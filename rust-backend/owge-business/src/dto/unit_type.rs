//! Mirrors `UnitTypeDto` / `UnitTypeResponse`
//! (`com.kevinguanchedarias.owgejava.dto.UnitTypeDto`, extended by
//! `com.kevinguanchedarias.owgejava.responses.UnitTypeResponse`), the
//! `unit_type_change` sync payload.
//!
//! Java's `UnitTypeDto.dtoFromEntity` recursively embeds the nested relation
//! objects (`shareMaxCount`, `parent`, `speedImpactGroup`, `attackRule`,
//! `criticalAttack`), and `UnitTypeRestService.loadData` adds the per-user
//! `computedMaxCount` / `userBuilt` / `used` fields on top. Jackson runs with
//! `Include.NON_NULL`, so every nullable field below is `skip_serializing_if`'d
//! to match (a null relation/scalar is omitted, not emitted as `null`).
//! (`inheritedImprovementUnitTypes` is declared on the Java DTO but never
//! populated in `dtoFromEntity`, so it is absent on both sides — out of scope.)

use serde::{Deserialize, Serialize};

use crate::dto::{AttackRuleDto, CriticalAttackDto, SpeedImpactGroupDto};

/// JSON payload for one unit type. Field names are Jackson camelCase.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UnitTypeDto {
    pub id: u16,
    pub name: String,
    /// The image id (`UnitTypeDto.image`), or omitted when the type has no image.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image: Option<u64>,
    /// The resolved image URL (`UnitTypeDto.imageUrl`). Built from the image
    /// filename in the `Bo` query.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_url: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub max_count: Option<i64>,
    /// `UnitTypeDto.shareMaxCount` — the full nested (catalog) DTO, recursively.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub share_max_count: Option<Box<UnitTypeDto>>,
    /// `UnitTypeDto.parent` — the full nested (catalog) DTO, recursively.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub parent: Option<Box<UnitTypeDto>>,
    pub has_to_inherit_improvements: bool,

    // Mission limitation enums (DtoWithMissionLimitation). Serialized as the
    // enum names ("NONE" / "OWNED_ONLY" / "ANY"), exactly as Jackson does.
    pub can_explore: String,
    pub can_gather: String,
    pub can_establish_base: String,
    pub can_attack: String,
    pub can_counterattack: String,
    pub can_conquest: String,
    pub can_deploy: String,

    /// `UnitTypeDto.speedImpactGroup` (nested; `requirementsGroups` omitted, as
    /// the sync source nulls it).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub speed_impact_group: Option<SpeedImpactGroupDto>,
    /// `UnitTypeDto.attackRule` (nested, with its entries).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub attack_rule: Option<AttackRuleDto>,
    /// `UnitTypeDto.criticalAttack` (nested, with its entries).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub critical_attack: Option<CriticalAttackDto>,

    /// `UnitTypeResponse.computedMaxCount` — the per-user limit. Set only on the
    /// sync (per-user) path; absent on the catalog / nested forms.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub computed_max_count: Option<i64>,
    /// `UnitTypeResponse.userBuilt` — count the user currently has. Set only when
    /// the type has a max, on the sync path; absent otherwise.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_built: Option<i64>,
    /// `UnitTypeResponse.used` — whether any `unit` references this type. Set only
    /// on the sync path (top level); absent on the nested catalog forms.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub used: Option<bool>,
}

/// A nested object reference carrying only its `id`, matching the frontend's
/// `{ "id": N }` shape for related types in the admin form.
#[derive(Debug, Clone, Deserialize)]
pub struct IdRef {
    pub id: Option<u16>,
}

/// Admin create/update request body for a unit type
/// (`AdminUnitTypeRestService` via `CrudRestServiceTrait`). The flat scalar and
/// mission-limitation fields are copied directly; the nested object fields are
/// resolved to their foreign-key id in `beforeSave`, so here we accept the same
/// `{ "id": N }` JSON and keep only the id.
///
/// `image` is a bare `Long` id in `UnitTypeDto` (not a nested object); the other
/// relations (`speedImpactGroup`, `attackRule`, `criticalAttack`, `parent`,
/// `shareMaxCount`) are nested DTOs whose `id` is the only field used.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UnitTypeInput {
    pub name: String,
    #[serde(default)]
    pub image: Option<u64>,
    #[serde(default)]
    pub max_count: Option<i64>,
    #[serde(default)]
    pub has_to_inherit_improvements: bool,

    #[serde(default = "default_mission_limitation")]
    pub can_explore: String,
    #[serde(default = "default_mission_limitation")]
    pub can_gather: String,
    #[serde(default = "default_mission_limitation")]
    pub can_establish_base: String,
    #[serde(default = "default_mission_limitation")]
    pub can_attack: String,
    #[serde(default = "default_mission_limitation")]
    pub can_counterattack: String,
    #[serde(default = "default_mission_limitation")]
    pub can_conquest: String,
    #[serde(default = "default_mission_limitation")]
    pub can_deploy: String,

    #[serde(default)]
    pub speed_impact_group: Option<IdRef>,
    #[serde(default)]
    pub attack_rule: Option<IdRef>,
    #[serde(default)]
    pub critical_attack: Option<IdRef>,
    #[serde(default)]
    pub parent: Option<IdRef>,
    #[serde(default)]
    pub share_max_count: Option<IdRef>,
}

impl UnitTypeInput {
    /// Resolves a nested `{ "id": N }` relation to its bare id, treating an
    /// absent object or an object without an id as "not set" — matching the
    /// Java `beforeSave` guards (`getX() != null && getX().getId() != null`).
    pub fn speed_impact_group_id(&self) -> Option<u16> {
        self.speed_impact_group.as_ref().and_then(|r| r.id)
    }
    pub fn attack_rule_id(&self) -> Option<u16> {
        self.attack_rule.as_ref().and_then(|r| r.id)
    }
    pub fn critical_attack_id(&self) -> Option<u16> {
        self.critical_attack.as_ref().and_then(|r| r.id)
    }
    pub fn parent_id(&self) -> Option<u16> {
        self.parent.as_ref().and_then(|r| r.id)
    }
    pub fn share_max_count_id(&self) -> Option<u16> {
        self.share_max_count.as_ref().and_then(|r| r.id)
    }
}

fn default_mission_limitation() -> String {
    "ANY".to_string()
}
