//! Mirrors `AttackRuleDto` (which nests a list of `AttackRuleEntryDto`) and the
//! request body the admin `POST`/`PUT admin/attack-rule` endpoints accept.
//!
//! The Java DTO/entity model `id` and `referenceId` as `Integer`, but the
//! underlying columns are `smallint UNSIGNED`; `u16` is used here to match the
//! schema (the emitted JSON is a plain number either way).
//!
//! `target` is the Java enum `AttackableTargetEnum { UNIT_TYPE, UNIT }`,
//! persisted/serialized as its name string (`@Enumerated(EnumType.STRING)` and
//! Jackson default), so it is carried as a `String` ("UNIT" / "UNIT_TYPE").
//!
//! `referenceName` is a `@Transient` field on `AttackRuleEntry` that is never
//! populated on the read path (it has no `@PostLoad`/query backing it), so it is
//! always serialized as `null`, matching the Java behaviour.

use serde::{Deserialize, Serialize};

/// Mirrors `AttackRuleEntryDto`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AttackRuleEntryDto {
    pub id: u16,
    pub target: String,
    pub reference_id: u16,
    pub reference_name: Option<String>,
    pub can_attack: bool,
}

/// Mirrors `AttackRuleDto`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AttackRuleDto {
    pub id: u16,
    pub name: String,
    pub entries: Vec<AttackRuleEntryDto>,
}

/// Request body for `POST`/`PUT admin/attack-rule`. On update the `id` comes
/// from the path; on create it is generated. Each entry's `id` is ignored on
/// the write path (the Java `save` deletes and re-inserts all entries), so it is
/// optional here.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttackRuleInput {
    pub name: String,
    #[serde(default)]
    pub entries: Vec<AttackRuleEntryInput>,
}

/// A single entry inside an [`AttackRuleInput`].
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct AttackRuleEntryInput {
    pub target: String,
    pub reference_id: u16,
    #[serde(default)]
    pub can_attack: bool,
}
