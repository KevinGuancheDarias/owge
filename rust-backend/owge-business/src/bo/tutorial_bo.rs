//! Port of (the read side of) `TutorialSectionBo`. Builds the
//! `TutorialSectionEntryDto` list for the `tutorial_entries_change` sync, with
//! the html symbol (and its section frontend path) and the `text` translatable
//! resolved via joins instead of lazy entity navigation.

use crate::dto::{
    TranslatableDto, TutorialSectionAvailableHtmlSymbolDto, TutorialSectionDto,
    TutorialSectionEntryDto, TutorialSectionEntryInput,
};
use crate::error::{OwgeError, OwgeResult};
use sqlx::MySqlConnection;

/// A tutorial entry joined with its html symbol (+ section path) and its text
/// translatable, with exact SQL column types so sqlx never panics on
/// signedness/width.
#[derive(sqlx::FromRow)]
struct TutorialSectionEntryRow {
    // tutorial_sections_entries
    id: u32,
    order_num: Option<u16>,
    event: String,
    // tutorial_sections_available_html_symbols (NOT NULL FK -> always present)
    symbol_id: u32,
    symbol_name: String,
    symbol_identifier: String,
    section_frontend_path: Option<String>,
    // translatables (NOT NULL FK -> always present)
    text_id: u32,
    text_name: String,
    text_default_lang_code: String,
}

impl From<TutorialSectionEntryRow> for TutorialSectionEntryDto {
    fn from(r: TutorialSectionEntryRow) -> Self {
        TutorialSectionEntryDto {
            id: r.id as u64,
            order: r.order_num.map(|o| o as i32),
            event: r.event,
            html_symbol: Some(TutorialSectionAvailableHtmlSymbolDto {
                id: r.symbol_id,
                name: r.symbol_name,
                identifier: r.symbol_identifier,
                section_frontend_path: r.section_frontend_path,
            }),
            // `translation` is @Transient on the Java entity and is not
            // populated here, so it serializes as null (matching Java JSON).
            text: Some(TranslatableDto {
                id: r.text_id as u64,
                name: r.text_name,
                default_lang_code: r.text_default_lang_code,
                translation: None,
            }),
        }
    }
}

/// A `tutorial_sections` row (id is `smallint UNSIGNED`).
#[derive(sqlx::FromRow)]
struct TutorialSectionRow {
    id: u16,
    name: String,
    description: Option<String>,
    frontend_router_path: String,
}

/// A `tutorial_sections_available_html_symbols` row joined with its (optional)
/// owning section's frontend path.
#[derive(sqlx::FromRow)]
struct HtmlSymbolRow {
    id: u32,
    name: String,
    identifier: String,
    section_frontend_path: Option<String>,
}

impl From<HtmlSymbolRow> for TutorialSectionAvailableHtmlSymbolDto {
    fn from(r: HtmlSymbolRow) -> Self {
        TutorialSectionAvailableHtmlSymbolDto {
            id: r.id,
            name: r.name,
            identifier: r.identifier,
            section_frontend_path: r.section_frontend_path,
        }
    }
}

const SELECT_ENTRY_DTO: &str = "\
    SELECT e.id AS id, e.order_num AS order_num, e.event AS event, \
           s.id AS symbol_id, s.name AS symbol_name, s.identifier AS symbol_identifier, \
           sec.frontend_router_path AS section_frontend_path, \
           t.id AS text_id, t.name AS text_name, t.default_lang_code AS text_default_lang_code \
    FROM tutorial_sections_entries e \
    JOIN tutorial_sections_available_html_symbols s ON s.id = e.section_available_html_symbol_id \
    LEFT JOIN tutorial_sections sec ON sec.id = s.tutorial_section_id \
    JOIN translatables t ON t.id = e.text_id ";

pub struct TutorialBo;

impl TutorialBo {
    /// `TutorialRestService.addVisitedEntry` — mark a tutorial entry visited for
    /// the user (idempotent per (user, entry)). Feeds `visited_tutorial_entry_change`.
    pub async fn add_visited_entry(
        conn: &mut MySqlConnection,
        user_id: i32,
        entry_id: u32,
    ) -> OwgeResult<()> {
        let exists: i64 = sqlx::query_scalar(
            "SELECT COUNT(*) FROM visited_tutorial_entries WHERE user_id = ? AND entry_id = ?",
        )
        .bind(user_id)
        .bind(entry_id)
        .fetch_one(&mut *conn)
        .await?;
        if exists == 0 {
            sqlx::query("INSERT INTO visited_tutorial_entries (user_id, entry_id) VALUES (?, ?)")
                .bind(user_id)
                .bind(entry_id)
                .execute(&mut *conn)
                .await?;
        }
        Ok(())
    }

    /// `tutorialSectionBo.findEntries()` -> DTOs ordered by `order` ascending —
    /// the `tutorial_entries_change` sync payload. Tutorial entries are global
    /// (not per-user), so no `user_id` is needed.
    pub async fn find_entries(
        conn: &mut MySqlConnection,
    ) -> OwgeResult<Vec<TutorialSectionEntryDto>> {
        let rows = sqlx::query_as::<_, TutorialSectionEntryRow>(&format!(
            "{SELECT_ENTRY_DTO} ORDER BY e.order_num ASC"
        ))
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `WithReadRestServiceTrait.findAll` for the admin tutorial-section CRUD —
    /// every section hydrated with its available html symbols
    /// (`TutorialSectionBo.findAllHydrated`).
    pub async fn find_all_sections(
        conn: &mut MySqlConnection,
    ) -> OwgeResult<Vec<TutorialSectionDto>> {
        let rows = sqlx::query_as::<_, TutorialSectionRow>(
            "SELECT id, name, description, frontend_router_path AS frontend_router_path \
             FROM tutorial_sections ORDER BY id",
        )
        .fetch_all(&mut *conn)
        .await?;
        let mut sections = Vec::with_capacity(rows.len());
        for row in rows {
            sections.push(Self::hydrate_section(&mut *conn, row).await?);
        }
        Ok(sections)
    }

    /// `WithReadRestServiceTrait.findOneById` — one section hydrated with its
    /// available html symbols (`TutorialSectionBo.findOneHydratedById`).
    pub async fn find_section_by_id(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<Option<TutorialSectionDto>> {
        let row = sqlx::query_as::<_, TutorialSectionRow>(
            "SELECT id, name, description, frontend_router_path \
             FROM tutorial_sections WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(&mut *conn)
        .await?;
        match row {
            Some(row) => Ok(Some(Self::hydrate_section(&mut *conn, row).await?)),
            None => Ok(None),
        }
    }

    async fn hydrate_section(
        conn: &mut MySqlConnection,
        row: TutorialSectionRow,
    ) -> OwgeResult<TutorialSectionDto> {
        // The symbol DTO carries the owning section's frontend path, which here
        // is this section's own path (`s.tutorial_section_id = sec.id`).
        let symbols = sqlx::query_as::<_, HtmlSymbolRow>(
            "SELECT s.id AS id, s.name AS name, s.identifier AS identifier, \
                    sec.frontend_router_path AS section_frontend_path \
             FROM tutorial_sections_available_html_symbols s \
             LEFT JOIN tutorial_sections sec ON sec.id = s.tutorial_section_id \
             WHERE s.tutorial_section_id = ? ORDER BY s.id",
        )
        .bind(row.id)
        .fetch_all(&mut *conn)
        .await?;
        let available_html_symbols = if symbols.is_empty() {
            None
        } else {
            Some(symbols.into_iter().map(Into::into).collect())
        };
        Ok(TutorialSectionDto {
            id: row.id,
            name: row.name,
            description: row.description,
            frontend_router_path: row.frontend_router_path,
            available_html_symbols,
        })
    }

    /// `tutorialSectionBo.findAvailableHtmlSymbols()` — every html symbol, with
    /// its (optional) owning section's frontend path.
    pub async fn find_available_html_symbols(
        conn: &mut MySqlConnection,
    ) -> OwgeResult<Vec<TutorialSectionAvailableHtmlSymbolDto>> {
        let rows = sqlx::query_as::<_, HtmlSymbolRow>(
            "SELECT s.id AS id, s.name AS name, s.identifier AS identifier, \
                    sec.frontend_router_path AS section_frontend_path \
             FROM tutorial_sections_available_html_symbols s \
             LEFT JOIN tutorial_sections sec ON sec.id = s.tutorial_section_id \
             ORDER BY s.id",
        )
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `tutorialSectionBo.addUpdateEntry(pojo)` — insert (no id) or update an
    /// entry, then return it hydrated as a `TutorialSectionEntryDto`.
    pub async fn add_update_entry(
        conn: &mut MySqlConnection,
        input: &TutorialSectionEntryInput,
    ) -> OwgeResult<TutorialSectionEntryDto> {
        let id = match input.id {
            // Update when the row exists; an unknown id falls through to insert
            // (Java: `findById(...).orElse(new TutorialSectionEntry())`).
            Some(id) if Self::entry_exists(&mut *conn, id).await? => {
                sqlx::query(
                    "UPDATE tutorial_sections_entries \
                     SET order_num = ?, section_available_html_symbol_id = ?, event = ?, text_id = ? \
                     WHERE id = ?",
                )
                .bind(input.order)
                .bind(input.html_symbol.id)
                .bind(&input.event)
                .bind(input.text.id)
                .bind(id)
                .execute(&mut *conn)
                .await?;
                id
            }
            _ => {
                let result = sqlx::query(
                    "INSERT INTO tutorial_sections_entries \
                     (order_num, section_available_html_symbol_id, event, text_id) \
                     VALUES (?, ?, ?, ?)",
                )
                .bind(input.order)
                .bind(input.html_symbol.id)
                .bind(&input.event)
                .bind(input.text.id)
                .execute(&mut *conn)
                .await?;
                result.last_insert_id() as u32
            }
        };
        Self::find_entry_by_id(&mut *conn, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Tutorial entry vanished right after save".into()))
    }

    /// `entryRepository.deleteById(entryId)`.
    pub async fn delete_entry(conn: &mut MySqlConnection, entry_id: u32) -> OwgeResult<()> {
        sqlx::query("DELETE FROM tutorial_sections_entries WHERE id = ?")
            .bind(entry_id)
            .execute(&mut *conn)
            .await?;
        Ok(())
    }

    async fn entry_exists(conn: &mut MySqlConnection, id: u32) -> OwgeResult<bool> {
        let count: i64 =
            sqlx::query_scalar("SELECT COUNT(*) FROM tutorial_sections_entries WHERE id = ?")
                .bind(id)
                .fetch_one(&mut *conn)
                .await?;
        Ok(count > 0)
    }

    async fn find_entry_by_id(
        conn: &mut MySqlConnection,
        id: u32,
    ) -> OwgeResult<Option<TutorialSectionEntryDto>> {
        let row = sqlx::query_as::<_, TutorialSectionEntryRow>(&format!(
            "{SELECT_ENTRY_DTO} WHERE e.id = ?"
        ))
        .bind(id)
        .fetch_optional(&mut *conn)
        .await?;
        Ok(row.map(Into::into))
    }

    /// `tutorialSectionBo.findVisitedIdsByUser(userId)` -> the visited entry ids
    /// for the `visited_tutorial_entry_change` sync.
    pub async fn find_visited_ids_by_user(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<u32>> {
        let ids = sqlx::query_scalar::<_, u32>(
            "SELECT entry_id FROM visited_tutorial_entries WHERE user_id = ?",
        )
        .bind(user_id)
        .fetch_all(&mut *conn)
        .await?;
        Ok(ids)
    }
}
