//! DTOs for the tutorial domain, mirroring the Java `TutorialSectionEntryDto`,
//! `TutorialSectionAvailableHtmlSymbolDto`, `TranslatableDto` and
//! `TranslatableTranslationDto` JSON shapes consumed by the frontend.

use serde::{Deserialize, Serialize};

/// Mirrors `TranslatableTranslationDto`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TranslatableTranslationDto {
    pub id: u64,
    pub lang_code: String,
    pub value: String,
}

/// Mirrors `TranslatableDto`. The `translation` field is `@Transient` on the
/// Java entity and is not populated when building tutorial entries, so it is
/// always serialized as `null` here (matching the Java JSON).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TranslatableDto {
    pub id: u64,
    pub name: String,
    pub default_lang_code: String,
    pub translation: Option<TranslatableTranslationDto>,
}

/// Mirrors `TutorialSectionAvailableHtmlSymbolDto`. `sectionFrontendPath` comes
/// from the joined `tutorial_sections.frontend_router_path`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TutorialSectionAvailableHtmlSymbolDto {
    pub id: u32,
    pub name: String,
    pub identifier: String,
    pub section_frontend_path: Option<String>,
}

/// Mirrors `TutorialSectionEntryDto`. `event` is the enum name string
/// (`CLICK` / `ANY_KEY_OR_CLICK`), matching Jackson's default enum rendering.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TutorialSectionEntryDto {
    pub id: u64,
    pub order: Option<i32>,
    pub event: String,
    pub html_symbol: Option<TutorialSectionAvailableHtmlSymbolDto>,
    pub text: Option<TranslatableDto>,
}

/// Mirrors `TutorialSectionDto` (extends `CommonDto`, hence `id`/`name`/
/// `description`). `availableHtmlSymbols` is built from the joined
/// `tutorial_sections_available_html_symbols` rows; when the section has none
/// the Java DTO leaves the list `null`, so it is an `Option` here.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TutorialSectionDto {
    pub id: u16,
    pub name: String,
    pub description: Option<String>,
    pub frontend_router_path: String,
    pub available_html_symbols: Option<Vec<TutorialSectionAvailableHtmlSymbolDto>>,
}

/// Admin create/update body for a tutorial entry
/// (`AdminTutorialSectionRestService.addUpdateEntry`, a `TutorialSectionEntryDto`).
/// Only `htmlSymbol.id`, `text.id`, `order` and `event` are read; `id` is absent
/// on create.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TutorialSectionEntryInput {
    #[serde(default)]
    pub id: Option<u32>,
    #[serde(default)]
    pub order: Option<u16>,
    pub event: String,
    pub html_symbol: TutorialEntryRefInput,
    pub text: TutorialEntryRefInput,
}

/// A `{ id }`-bearing nested object in the entry body (the html symbol / text).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TutorialEntryRefInput {
    pub id: u32,
}
