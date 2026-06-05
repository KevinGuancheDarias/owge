use serde::{Deserialize, Serialize};

/// Mirrors the `speed_impact_groups` table / Java `SpeedImpactGroup` entity.
///
/// The `can_*` columns are MySQL `enum('NONE','OWNED_ONLY','ANY')`; they map to
/// the Java `MissionSupportEnum` and are read here as `String`.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct SpeedImpactGroup {
    /// `smallint unsigned`.
    pub id: u16,
    pub name: String,
    /// `tinyint` (not the `tinyint(1)` boolean form, but used as a flag).
    pub is_fixed: i8,
    pub mission_explore: f64,
    pub mission_gather: f64,
    pub mission_establish_base: f64,
    pub mission_attack: f64,
    pub mission_conquest: f64,
    pub mission_counterattack: f64,
    /// `enum('NONE','OWNED_ONLY','ANY')`, nullable.
    pub can_explore: Option<String>,
    pub can_gather: Option<String>,
    pub can_establish_base: Option<String>,
    pub can_attack: Option<String>,
    pub can_counterattack: Option<String>,
    pub can_conquest: Option<String>,
    pub can_deploy: Option<String>,
    /// `bigint unsigned`, nullable — FK to `image_store`.
    pub image_id: Option<u64>,
}
