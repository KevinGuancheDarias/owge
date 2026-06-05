//! Mirrors the `unit_types` table / Java `UnitType` entity
//! (`com.kevinguanchedarias.owgejava.entity.UnitType`, plus the
//! `can_*` mission-limitation columns from `EntityWithMissionLimitation`).

use serde::{Deserialize, Serialize};

/// A row of the `unit_types` catalog table. Column types match the schema
/// exactly so sqlx never panics on signedness/width.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct UnitType {
    /// `smallint unsigned`.
    pub id: u16,
    pub name: String,
    /// `smallint unsigned` nullable.
    #[sqlx(rename = "attack_rule_id")]
    pub attack_rule_id: Option<u16>,
    /// `bigint` (signed) nullable.
    #[sqlx(rename = "max_count")]
    pub max_count: Option<i64>,
    /// `smallint unsigned` nullable — the unit type this one shares its count with.
    #[sqlx(rename = "share_max_count")]
    pub share_max_count: Option<u16>,
    /// `bigint unsigned` nullable.
    #[sqlx(rename = "image_id")]
    pub image_id: Option<u64>,
    /// `smallint unsigned` nullable.
    #[sqlx(rename = "parent_type")]
    pub parent_type: Option<u16>,
    /// `enum('NONE','OWNED_ONLY','ANY')` — stored as text.
    #[sqlx(rename = "can_explore")]
    pub can_explore: String,
    #[sqlx(rename = "can_gather")]
    pub can_gather: String,
    #[sqlx(rename = "can_establish_base")]
    pub can_establish_base: String,
    #[sqlx(rename = "can_attack")]
    pub can_attack: String,
    #[sqlx(rename = "can_counterattack")]
    pub can_counterattack: String,
    #[sqlx(rename = "can_conquest")]
    pub can_conquest: String,
    #[sqlx(rename = "can_deploy")]
    pub can_deploy: String,
    /// `smallint unsigned` nullable.
    #[sqlx(rename = "speed_impact_group_id")]
    pub speed_impact_group_id: Option<u16>,
    /// `smallint unsigned` nullable.
    #[sqlx(rename = "critical_attack_id")]
    pub critical_attack_id: Option<u16>,
    /// `tinyint(1)` — read as `i8`, the DTO maps it to `bool`.
    #[sqlx(rename = "has_to_inherit_improvements")]
    pub has_to_inherit_improvements: i8,
}
