//! Port of `AdminTranslatableRestService` + the parts of `TranslationBo` it
//! drives. `AdminTranslatableRestService` is a `CrudRestServiceTrait<Translatable>`
//! (full read/write/delete) plus three bespoke translation endpoints:
//! `GET {id}/translations`, `POST|PUT {id}/translations`, and
//! `DELETE {id}/translations/{translationId}`.
//!
//! The `Translatable.translation` field is `@Transient` and only populated by
//! `TranslatableListener` from the `X-Owge-Lang` request header. The CRUD reads
//! here mirror the tutorial port and leave `translation = None`.

use crate::db::Db;
use crate::dto::{TranslatableDto, TranslatableInput, TranslatableTranslationDto,
    TranslatableTranslationInput};
use crate::error::{OwgeError, OwgeResult};

#[derive(sqlx::FromRow)]
struct TranslatableRow {
    id: u32,
    name: String,
    default_lang_code: String,
}

impl From<TranslatableRow> for TranslatableDto {
    fn from(r: TranslatableRow) -> Self {
        TranslatableDto {
            id: r.id as u64,
            name: r.name,
            default_lang_code: r.default_lang_code,
            translation: None,
        }
    }
}

#[derive(sqlx::FromRow)]
struct TranslationRow {
    id: u32,
    lang_code: String,
    value: String,
}

impl From<TranslationRow> for TranslatableTranslationDto {
    fn from(r: TranslationRow) -> Self {
        TranslatableTranslationDto {
            id: r.id as u64,
            lang_code: r.lang_code,
            value: r.value,
        }
    }
}

pub struct TranslatableBo;

impl TranslatableBo {
    /// `CrudRestServiceTrait.findAll` — every translatable, ordered by id.
    pub async fn find_all(db: &Db) -> OwgeResult<Vec<TranslatableDto>> {
        let rows = sqlx::query_as::<_, TranslatableRow>(
            "SELECT id, name, default_lang_code FROM translatables ORDER BY id",
        )
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `WithReadRestServiceTrait.findOneById`.
    pub async fn find_by_id(db: &Db, id: u32) -> OwgeResult<Option<TranslatableDto>> {
        let row = sqlx::query_as::<_, TranslatableRow>(
            "SELECT id, name, default_lang_code FROM translatables WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(db)
        .await?;
        Ok(row.map(Into::into))
    }

    /// `CrudRestServiceTrait.saveNew` — insert; `translatables.id` is AUTO_INCREMENT.
    pub async fn save_new(db: &Db, input: &TranslatableInput) -> OwgeResult<TranslatableDto> {
        let result = sqlx::query(
            "INSERT INTO translatables (name, default_lang_code) VALUES (?, ?)",
        )
        .bind(&input.name)
        .bind(&input.default_lang_code)
        .execute(db)
        .await?;
        let id = result.last_insert_id() as u32;
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Translatable vanished right after insert".into()))
    }

    /// `CrudRestServiceTrait.saveExisting` — update by id.
    pub async fn save_existing(
        db: &Db,
        id: u32,
        input: &TranslatableInput,
    ) -> OwgeResult<TranslatableDto> {
        let affected = sqlx::query(
            "UPDATE translatables SET name = ?, default_lang_code = ? WHERE id = ?",
        )
        .bind(&input.name)
        .bind(&input.default_lang_code)
        .bind(id)
        .execute(db)
        .await?
        .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No translatable with id {id}")));
        }
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No translatable with id {id}")))
    }

    /// `WithDeleteRestServiceTrait.delete`.
    pub async fn delete(db: &Db, id: u32) -> OwgeResult<()> {
        sqlx::query("DELETE FROM translatables WHERE id = ?")
            .bind(id)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `TranslationBo.findTranslations(translatableId)` — every translation row
    /// for the given translatable.
    pub async fn find_translations(
        db: &Db,
        translatable_id: u32,
    ) -> OwgeResult<Vec<TranslatableTranslationDto>> {
        let rows = sqlx::query_as::<_, TranslationRow>(
            "SELECT id, lang_code, value FROM translatables_translations \
             WHERE translatable_id = ? ORDER BY id",
        )
        .bind(translatable_id)
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `TranslationBo.addTranslation` — create or update a single translation
    /// row for a translatable. Rejects a `lang_code` already used by another
    /// translation row of the same translatable
    /// (`ERR_I18N_LANG_CODE_ALREADY_SPECIFIED`).
    ///
    /// In the Java flow `addTranslation` also re-saves the parent `Translatable`
    /// (its `name`); here the parent is looked up to mirror `findByIdOrDie`
    /// (404 if missing) and the translation row is upserted.
    pub async fn add_translation(
        db: &Db,
        translatable_id: u32,
        input: &TranslatableTranslationInput,
    ) -> OwgeResult<TranslatableTranslationDto> {
        // findByIdOrDie(translatableRepository, id)
        if Self::find_by_id(db, translatable_id).await?.is_none() {
            return Err(OwgeError::NotFound(format!(
                "No translatable with id {translatable_id}"
            )));
        }

        // Reject a lang_code already specified by a *different* translation row.
        let existing_id: Option<u32> = sqlx::query_scalar(
            "SELECT id FROM translatables_translations \
             WHERE translatable_id = ? AND lang_code = ?",
        )
        .bind(translatable_id)
        .bind(&input.lang_code)
        .fetch_optional(db)
        .await?;
        if let Some(existing_id) = existing_id {
            if Some(existing_id as u64) != input.id {
                return Err(OwgeError::InvalidInput(
                    "ERR_I18N_LANG_CODE_ALREADY_SPECIFIED".into(),
                ));
            }
        }

        let id = match input.id {
            Some(id) => {
                let affected = sqlx::query(
                    "UPDATE translatables_translations \
                     SET translatable_id = ?, lang_code = ?, value = ? WHERE id = ?",
                )
                .bind(translatable_id)
                .bind(&input.lang_code)
                .bind(&input.value)
                .bind(id as u32)
                .execute(db)
                .await?
                .rows_affected();
                if affected == 0 {
                    // findById(...).orElse(new TranslatableTranslation()) -> insert
                    Self::insert_translation(db, translatable_id, input).await?
                } else {
                    id as u32
                }
            }
            None => Self::insert_translation(db, translatable_id, input).await?,
        };

        Self::find_translation_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Translation vanished right after save".into()))
    }

    async fn insert_translation(
        db: &Db,
        translatable_id: u32,
        input: &TranslatableTranslationInput,
    ) -> OwgeResult<u32> {
        let result = sqlx::query(
            "INSERT INTO translatables_translations (translatable_id, lang_code, value) \
             VALUES (?, ?, ?)",
        )
        .bind(translatable_id)
        .bind(&input.lang_code)
        .bind(&input.value)
        .execute(db)
        .await?;
        Ok(result.last_insert_id() as u32)
    }

    async fn find_translation_by_id(
        db: &Db,
        id: u32,
    ) -> OwgeResult<Option<TranslatableTranslationDto>> {
        let row = sqlx::query_as::<_, TranslationRow>(
            "SELECT id, lang_code, value FROM translatables_translations WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(db)
        .await?;
        Ok(row.map(Into::into))
    }

    /// `AdminTranslatableRestService.deleteTranslation`. The Java method calls
    /// `translatableRepository.deleteById(translationId)`, i.e. it deletes from
    /// the **`translatables`** table by the path `translationId` (matching the
    /// Java behaviour verbatim).
    pub async fn delete_translation(db: &Db, translation_id: u32) -> OwgeResult<()> {
        sqlx::query("DELETE FROM translatables WHERE id = ?")
            .bind(translation_id)
            .execute(db)
            .await?;
        Ok(())
    }
}
