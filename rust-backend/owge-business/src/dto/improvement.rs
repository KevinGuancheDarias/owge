//! Shared improvement DTOs — `ImprovementDto` (`AbstractImprovementDto` + `id`)
//! and the nested `ImprovementUnitTypeDto`. These back the `GET/PUT
//! {id}/improvement` and `{id}/improvement/unitTypeImprovements` sub-resources
//! of every `CrudWithImprovements`/`CrudWithFull` entity (unit, faction,
//! upgrade, special location), so they live in one shared module rather than
//! being duplicated per entity.

use serde::{Deserialize, Serialize};

use crate::dto::UnitTypeDto;

/// Mirrors `ImprovementDto` (`id` + `AbstractImprovementDto` floats + the nested
/// `unitTypesUpgrades` list). The stored `improvements` columns are
/// smallint/float and are widened to `Float` in the Java DTO.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ImprovementDto {
    /// Ignored on the `PUT` body (the entity owns its improvement id); defaults
    /// to 0 when absent.
    #[serde(default)]
    pub id: u16,
    // Java serializes these as `Float` with `Include.NON_NULL`: omitted when
    // null, and printed as the shortest round-trippable decimal otherwise.
    #[serde(
        default,
        skip_serializing_if = "Option::is_none",
        serialize_with = "crate::dto::serde_helpers::serialize_opt_f32"
    )]
    pub more_primary_resource_production: Option<f32>,
    #[serde(
        default,
        skip_serializing_if = "Option::is_none",
        serialize_with = "crate::dto::serde_helpers::serialize_opt_f32"
    )]
    pub more_secondary_resource_production: Option<f32>,
    #[serde(
        default,
        skip_serializing_if = "Option::is_none",
        serialize_with = "crate::dto::serde_helpers::serialize_opt_f32"
    )]
    pub more_energy_production: Option<f32>,
    #[serde(
        default,
        skip_serializing_if = "Option::is_none",
        serialize_with = "crate::dto::serde_helpers::serialize_opt_f32"
    )]
    pub more_charge_capacity: Option<f32>,
    /// Java `moreMissions` (from `more_missions_value`).
    #[serde(
        default,
        skip_serializing_if = "Option::is_none",
        serialize_with = "crate::dto::serde_helpers::serialize_opt_f32"
    )]
    pub more_missions: Option<f32>,
    #[serde(
        default,
        skip_serializing_if = "Option::is_none",
        serialize_with = "crate::dto::serde_helpers::serialize_opt_f32"
    )]
    pub more_upgrade_research_speed: Option<f32>,
    #[serde(
        default,
        skip_serializing_if = "Option::is_none",
        serialize_with = "crate::dto::serde_helpers::serialize_opt_f32"
    )]
    pub more_unit_build_speed: Option<f32>,
    /// The per-unit-type improvements (`improvements_unit_types` rows). Nulled by
    /// the controller on the `PUT` body and ignored there.
    #[serde(default)]
    pub unit_types_upgrades: Vec<ImprovementUnitTypeDto>,
}

/// Mirrors `ImprovementUnitTypeDto`. `dtoFromEntity` populates `id`, `type`,
/// the nested `unitType`, and `value`; the deprecated `unitTypeId`/
/// `unitTypeName` stay `null` (the frontend sends `unitTypeId` on `POST` but the
/// DTO read side never sets it). We expose all four fields to match the Jackson
/// JSON shape exactly.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ImprovementUnitTypeDto {
    #[serde(default)]
    pub id: Option<u16>,
    #[serde(default)]
    pub r#type: Option<String>,
    /// Deprecated since 0.9.0; only consumed on `POST` input — null on output.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub unit_type_id: Option<u16>,
    /// Deprecated since 0.9.0 — always null on output.
    #[serde(default, skip_serializing_if = "Option::is_none")]
    pub unit_type_name: Option<String>,
    /// The resolved unit type (`UnitTypeDto`), populated on output. The POST
    /// input form carries only `unitTypeId`/`type`/`value`, so this is never
    /// read from the request body (`UnitTypeDto` is serialize-only).
    #[serde(default, skip_serializing_if = "Option::is_none", skip_deserializing)]
    pub unit_type: Option<UnitTypeDto>,
    #[serde(default)]
    pub value: Option<i64>,
}

impl ImprovementUnitTypeDto {
    /// The unit-type id this DTO targets, resolved from either the nested
    /// `unitType` object or the deprecated `unitTypeId` field (matching the
    /// frontend, which may send either form).
    pub fn resolved_unit_type_id(&self) -> Option<u16> {
        self.unit_type
            .as_ref()
            .map(|ut| ut.id)
            .or(self.unit_type_id)
    }
}
