use serde::{Deserialize, Serialize};

/// Mirrors `SpeedImpactGroupDto` (+ its `DtoWithMissionLimitation` base).
///
/// `imageUrl` comes from a join on `image_store`, so the `Bo` populates it when
/// building the DTO (no lazy entity navigation). `requirementsGroups` is omitted
/// until the requirement/unlock domain lands (M2).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SpeedImpactGroupDto {
    pub id: u16,
    pub name: String,
    pub is_fixed: bool,
    pub mission_explore: f64,
    pub mission_gather: f64,
    pub mission_establish_base: f64,
    pub mission_attack: f64,
    pub mission_conquest: f64,
    pub mission_counterattack: f64,
    // MissionSupportEnum values: "NONE" | "OWNED_ONLY" | "ANY" (default "ANY").
    pub can_explore: String,
    pub can_gather: String,
    pub can_establish_base: String,
    pub can_attack: String,
    pub can_counterattack: String,
    pub can_conquest: String,
    pub can_deploy: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_url: Option<String>,
    /// `SpeedImpactGroupDto.requirementsGroups` — populated on paths where Java's
    /// `@PostLoad` listener initialized the transient (e.g. embedded in an
    /// improvement's `unitType`); explicitly nulled by the `unit_type_change`
    /// rest service, so it stays `None` on the catalog/unit-type paths.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub requirements_groups: Option<Vec<crate::dto::RequirementGroupDto>>,
}

/// Admin create/update request body for a speed impact group. Mirrors
/// `SpeedImpactGroupDto` on the write path: the `MissionSupportEnum` strings
/// default to `ANY`, the mission multipliers to `0`, and `isFixed` to `false`,
/// exactly like the Java DTO field defaults.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SpeedImpactGroupInput {
    pub name: String,
    #[serde(default)]
    pub is_fixed: bool,
    #[serde(default)]
    pub mission_explore: f64,
    #[serde(default)]
    pub mission_gather: f64,
    #[serde(default)]
    pub mission_establish_base: f64,
    #[serde(default)]
    pub mission_attack: f64,
    #[serde(default)]
    pub mission_conquest: f64,
    #[serde(default)]
    pub mission_counterattack: f64,
    #[serde(default = "default_any")]
    pub can_explore: String,
    #[serde(default = "default_any")]
    pub can_gather: String,
    #[serde(default = "default_any")]
    pub can_establish_base: String,
    #[serde(default = "default_any")]
    pub can_attack: String,
    #[serde(default = "default_any")]
    pub can_counterattack: String,
    #[serde(default = "default_any")]
    pub can_conquest: String,
    #[serde(default = "default_any")]
    pub can_deploy: String,
    #[serde(default)]
    pub image: Option<u64>,
}

fn default_any() -> String {
    "ANY".to_string()
}
