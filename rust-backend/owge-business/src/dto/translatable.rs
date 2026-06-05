//! Input DTOs for the admin translatable domain (`AdminTranslatableRestService`).
//!
//! The output shapes (`TranslatableDto` / `TranslatableTranslationDto`) already
//! live in [`crate::dto::tutorial`] and are reused as-is; this module only adds
//! the request-body deserialize structs the admin CRUD + translation endpoints
//! need.

use serde::Deserialize;

/// Request body for the translatable CRUD `POST ''` / `PUT '{id}'`.
///
/// Mirrors the writable part of `TranslatableDto`: the persisted `Translatable`
/// entity only has `name` and `default_lang_code` (the `translation` field is
/// `@Transient`). On create the entity default for `default_lang_code` is `en`.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TranslatableInput {
    pub name: String,
    #[serde(default = "default_lang_code")]
    pub default_lang_code: String,
}

fn default_lang_code() -> String {
    "en".to_string()
}

/// Request body for `POST/PUT admin/translatable/{id}/translations`, mirroring
/// the writable part of `TranslatableTranslationDto`. `id` is absent on create
/// and present when updating an existing translation row.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct TranslatableTranslationInput {
    #[serde(default)]
    pub id: Option<u64>,
    pub lang_code: String,
    pub value: String,
}
