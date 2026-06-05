//! Persistence entities for the tutorial domain — Rust counterparts of the
//! Java JPA entities `TutorialSectionEntry`, `TutorialSectionAvailableHtmlSymbol`
//! and `VisitedTutorialSectionEntry`.

use serde::{Deserialize, Serialize};

/// Mirrors the `tutorial_sections_entries` table / Java `TutorialSectionEntry`
/// entity. `event` is the MySQL `enum('CLICK','ANY_KEY_OR_CLICK')` column.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct TutorialSectionEntry {
    /// `int unsigned` (Java maps it to `Long`).
    pub id: u32,
    /// `order_num` — `smallint unsigned` nullable.
    #[sqlx(rename = "order_num")]
    pub order_num: Option<u16>,
    /// `section_available_html_symbol_id` — `int unsigned` NOT NULL.
    #[sqlx(rename = "section_available_html_symbol_id")]
    pub section_available_html_symbol_id: u32,
    /// `enum('CLICK','ANY_KEY_OR_CLICK')`.
    pub event: String,
    /// `text_id` — `int unsigned` NOT NULL.
    #[sqlx(rename = "text_id")]
    pub text_id: u32,
}

/// Mirrors the `tutorial_sections_available_html_symbols` table / Java
/// `TutorialSectionAvailableHtmlSymbol` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct TutorialSectionAvailableHtmlSymbol {
    /// `int unsigned`.
    pub id: u32,
    pub name: String,
    pub identifier: String,
    /// `tutorial_section_id` — `smallint unsigned` nullable.
    #[sqlx(rename = "tutorial_section_id")]
    pub tutorial_section_id: Option<u16>,
}

/// Mirrors the `visited_tutorial_entries` table / Java
/// `VisitedTutorialSectionEntry` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct VisitedTutorialSectionEntry {
    /// `bigint` (signed).
    pub id: i64,
    /// `user_id` — `int` (signed) NOT NULL.
    #[sqlx(rename = "user_id")]
    pub user_id: i32,
    /// `entry_id` — `int unsigned` NOT NULL.
    #[sqlx(rename = "entry_id")]
    pub entry_id: u32,
}
