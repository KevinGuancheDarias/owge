use serde::{Deserialize, Serialize};

use crate::dto::{ImprovementDto, SpeedImpactGroupDto};

/// Mirrors `UnitDto` (the unit catalog DTO). Core stats + the unit type
/// (id/name via join) are populated; the nested improvement / speedImpactGroup
/// / attackRule / criticalAttack / requirements / interceptableSpeedGroups
/// objects land with the improvement + requirement engines (their `*Dto`s reuse
/// the same id-shallow approach used elsewhere in M1/M2).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UnitDto {
    pub id: u16,
    pub name: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// `image` is the bare image-store id in the Java DTO.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image: Option<u64>,
    /// `imageUrl` from `CommonDtoWithImageStore` (`ImageStore.getUrl()` =
    /// `/dynamic/<filename>`). Omitted when the unit has no image, matching
    /// Java's global `NON_NULL` inclusion.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_url: Option<String>,
    // Java serializes with Jackson `Include.NON_NULL`, so every nullable field
    // is omitted (not emitted as `null`) when absent. Mirror that here with
    // `skip_serializing_if` on every Option field for websocket-sync parity.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub order: Option<u16>,
    pub has_to_display_in_requirements: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub points: Option<u32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub time: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub primary_resource: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub secondary_resource: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub energy: Option<u16>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub type_id: Option<u16>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub type_name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attack: Option<u16>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub health: Option<u16>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub shield: Option<u16>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub charge: Option<u16>,
    pub is_unique: bool,
    pub can_fast_explore: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub speed: Option<f64>,
    pub cloned_improvements: bool,
    pub bypass_shield: bool,
    pub is_invisible: bool,
    pub stored_weight: u32,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub storage_capacity: Option<u32>,
    /// `UnitDto.improvement` (the unit's own improvement). Resolved on the
    /// per-user read paths (`findAllByUser`); absent when the unit has none or
    /// on paths that don't hydrate it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub improvement: Option<ImprovementDto>,
    /// `UnitDto.speedImpactGroup` — the applicable group (own → time-special
    /// swap → inherited). Absent on paths that don't hydrate it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub speed_impact_group: Option<SpeedImpactGroupDto>,
}

/// A shallow `{ "id": N }` reference, matching how the admin frontend serializes
/// the nested `speedImpactGroup` / `criticalAttack` objects in the unit body
/// (the Java `beforeSave` only ever reads their `.getId()`).
#[derive(Debug, Clone, Deserialize)]
pub struct IdRef {
    pub id: Option<u16>,
}

/// Admin create/update request body for a unit
/// (`AdminUnitRestService`, a `CrudWithFullRestService<Unit>`).
///
/// Mirrors `UnitDto` on the write side. `id` comes from the path on update and
/// is AUTO_INCREMENT on create. The relations `speedImpactGroup` /
/// `criticalAttack` are accepted as shallow `{id}` objects (the Java
/// `beforeSave` resolves them by id); `attackRule`, `improvement`,
/// `requirements` and `interceptableSpeedGroups` are handled by their own
/// sub-resources / engines and are ignored here. `typeId` is mandatory
/// (`I18N_ERR_UNIT_TYPE_IS_MANDATORY`).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UnitInput {
    pub name: String,
    #[serde(default)]
    pub description: Option<String>,
    /// `image` is the bare image-store id in the Java DTO.
    #[serde(default)]
    pub image: Option<u64>,
    #[serde(default)]
    pub order: Option<u16>,
    #[serde(default)]
    pub has_to_display_in_requirements: bool,
    #[serde(default)]
    pub points: Option<u32>,
    #[serde(default)]
    pub time: Option<i32>,
    #[serde(default)]
    pub primary_resource: Option<u32>,
    #[serde(default)]
    pub secondary_resource: Option<u32>,
    #[serde(default)]
    pub energy: Option<u16>,
    #[serde(default)]
    pub type_id: Option<u16>,
    #[serde(default)]
    pub attack: Option<u16>,
    #[serde(default)]
    pub health: Option<u16>,
    #[serde(default)]
    pub shield: Option<u16>,
    #[serde(default)]
    pub charge: Option<u16>,
    #[serde(default)]
    pub is_unique: bool,
    #[serde(default)]
    pub can_fast_explore: bool,
    #[serde(default)]
    pub speed: Option<f64>,
    #[serde(default)]
    pub cloned_improvements: bool,
    #[serde(default)]
    pub bypass_shield: bool,
    #[serde(default)]
    pub is_invisible: bool,
    #[serde(default = "default_stored_weight")]
    pub stored_weight: u32,
    #[serde(default)]
    pub storage_capacity: Option<u32>,
    #[serde(default)]
    pub speed_impact_group: Option<IdRef>,
    #[serde(default)]
    pub critical_attack: Option<IdRef>,
}

fn default_stored_weight() -> u32 {
    1
}

impl UnitInput {
    /// The resolved `speed_impact_group_id` FK (the nested object's id, if any).
    pub fn speed_impact_group_id(&self) -> Option<u16> {
        self.speed_impact_group.as_ref().and_then(|r| r.id)
    }

    /// The resolved `critical_attack_id` FK (the nested object's id, if any).
    pub fn critical_attack_id(&self) -> Option<u16> {
        self.critical_attack.as_ref().and_then(|r| r.id)
    }
}
