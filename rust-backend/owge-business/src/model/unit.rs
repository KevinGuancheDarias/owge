use serde::{Deserialize, Serialize};

/// Mirrors the `units` table / Java `Unit` entity (the unit catalog).
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct Unit {
    pub id: u16,
    pub order_number: Option<u16>,
    pub name: String,
    pub display_in_requirements: Option<i8>,
    pub attack_rule_id: Option<u16>,
    pub image_id: Option<u64>,
    pub points: Option<u32>,
    pub description: Option<String>,
    /// `int` (signed) base build time in seconds.
    pub time: Option<i32>,
    pub primary_resource: Option<u32>,
    pub secondary_resource: Option<u32>,
    pub energy: Option<u16>,
    #[sqlx(rename = "type")]
    pub type_id: Option<u16>,
    pub attack: Option<u16>,
    pub health: Option<u16>,
    pub shield: Option<u16>,
    pub charge: Option<u16>,
    /// `tinyint unsigned`.
    pub is_unique: u8,
    pub can_fast_explore: i8,
    pub speed: Option<f64>,
    pub improvement_id: u16,
    pub cloned_improvements: i8,
    pub speed_impact_group_id: Option<u16>,
    pub critical_attack_id: Option<u16>,
    pub bypass_shield: i8,
    pub is_invisible: i8,
    pub stored_weight: u32,
    pub storage_capacity: Option<u32>,
}
