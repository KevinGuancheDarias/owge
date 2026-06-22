//! Mirrors `CriticalAttackDto` and `CriticalAttackEntryDto`.
//!
//! The Java DTO doubles as both the response and the request body
//! (`AdminCriticalAttackRestService.save(CriticalAttackDto)`), so a single
//! shape carries the `id` (set from the path on update) and the nested
//! `entries`. On input the ids are optional (generated on insert).

use serde::{Deserialize, Serialize};

/// Mirrors `CriticalAttackDto`. Built from a `critical_attack` row plus its
/// `critical_attack_entries`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CriticalAttackDto {
    pub id: u16,
    pub name: String,
    pub entries: Vec<CriticalAttackEntryDto>,
}

/// Mirrors `CriticalAttackEntryDto`. `referenceName` is part of the Java DTO but
/// is never populated by the admin save/toDto path (it stays null), so it is
/// emitted as `null` to match the JSON exactly.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CriticalAttackEntryDto {
    pub id: u32,
    pub target: String,
    pub reference_id: u32,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub reference_name: Option<String>,
    #[serde(serialize_with = "crate::dto::serde_helpers::serialize_f32")]
    pub value: f32,
}

/// Admin create/update request body, mirroring the inbound `CriticalAttackDto`.
/// `id` is ignored on create and taken from the path on update.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CriticalAttackInput {
    #[serde(default)]
    pub name: Option<String>,
    #[serde(default)]
    pub entries: Vec<CriticalAttackEntryInput>,
}

/// A nested entry in the create/update body.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct CriticalAttackEntryInput {
    pub target: String,
    pub reference_id: u32,
    pub value: f32,
}
