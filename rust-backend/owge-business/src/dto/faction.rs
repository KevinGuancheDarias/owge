//! Mirrors `FactionDto` (the admin-facing faction CRUD payload) plus the
//! faction sub-resource DTOs `FactionUnitTypeDto` and `FactionSpawnLocationDto`.
//!
//! The existing [`crate::model::Faction`] / public faction list only carries the
//! handful of fields the registration UI needs; the admin CRUD round-trips the
//! full `factions` row, so this module adds the wide [`FactionDto`] and its
//! create/update body [`FactionInput`].

use serde::{Deserialize, Serialize};

/// Mirrors `FactionDto`. Built directly from the `factions` row (with the
/// resource image filenames resolved to `*Url` fields).
///
/// `improvement` and `unitTypes` are always `null` here: `FactionDto` nulls
/// `unitTypes` in `dtoFromEntity`, and the improvement row is widened by the
/// improvement engine (out of scope this pass — see the admin route).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FactionDto {
    pub id: u16,
    pub hidden: bool,
    pub name: String,
    pub description: Option<String>,
    pub primary_resource_name: String,
    pub primary_resource_image: Option<u64>,
    pub primary_resource_image_url: Option<String>,
    pub secondary_resource_name: String,
    pub secondary_resource_image: Option<u64>,
    pub secondary_resource_image_url: Option<String>,
    pub energy_name: String,
    pub energy_image: Option<u64>,
    pub energy_image_url: Option<String>,
    /// `image`/`imageUrl` come from `CommonDtoWithImageStore` (the `image_id`
    /// FK on `factions`).
    pub image: Option<u64>,
    pub image_url: Option<String>,
    pub initial_primary_resource: u32,
    pub initial_secondary_resource: u32,
    pub initial_energy: u32,
    // Java `Float`s: print shortest round-trip decimal (not the f32->f64 tail).
    #[serde(serialize_with = "crate::dto::serde_helpers::serialize_f32")]
    pub primary_resource_production: f32,
    #[serde(serialize_with = "crate::dto::serde_helpers::serialize_f32")]
    pub secondary_resource_production: f32,
    pub max_planets: u8,
    pub cloned_improvements: bool,
    #[serde(
        skip_serializing_if = "Option::is_none",
        serialize_with = "crate::dto::serde_helpers::serialize_opt_f32"
    )]
    pub custom_primary_gather_percentage: Option<f32>,
    #[serde(
        skip_serializing_if = "Option::is_none",
        serialize_with = "crate::dto::serde_helpers::serialize_opt_f32"
    )]
    pub custom_secondary_gather_percentage: Option<f32>,
    /// Java nulls this in `dtoFromEntity`; `NON_NULL` then omits it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub improvement: Option<()>,
    /// Java nulls this in `dtoFromEntity`; `NON_NULL` then omits it.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub unit_types: Option<()>,
}

/// Admin create/update request body for a faction (`FactionDto` on the wire,
/// but only the writable scalar columns are consumed; `id` comes from the path
/// on update and is AUTO_INCREMENT on create). The `*ImageUrl` read-only fields
/// are ignored on input.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FactionInput {
    #[serde(default)]
    pub hidden: Option<bool>,
    pub name: String,
    #[serde(default)]
    pub description: Option<String>,
    pub primary_resource_name: String,
    #[serde(default)]
    pub primary_resource_image: Option<u64>,
    pub secondary_resource_name: String,
    #[serde(default)]
    pub secondary_resource_image: Option<u64>,
    pub energy_name: String,
    #[serde(default)]
    pub energy_image: Option<u64>,
    /// `image_id` FK from `CommonDtoWithImageStore`.
    #[serde(default)]
    pub image: Option<u64>,
    pub initial_primary_resource: u32,
    pub initial_secondary_resource: u32,
    pub initial_energy: u32,
    pub primary_resource_production: f32,
    pub secondary_resource_production: f32,
    pub max_planets: u8,
    #[serde(default)]
    pub cloned_improvements: bool,
    #[serde(default)]
    pub custom_primary_gather_percentage: Option<f32>,
    #[serde(default)]
    pub custom_secondary_gather_percentage: Option<f32>,
}

/// Mirrors `FactionUnitTypeDto` for `GET {factionId}/unitTypes` — `factionId` is
/// nulled out in the admin handler, matching the Java service.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FactionUnitTypeDto {
    pub id: u32,
    pub faction_id: Option<u16>,
    pub unit_type_id: u16,
    pub max_count: Option<u32>,
}

/// Mirrors `FactionSpawnLocationDto` for `GET {factionId}/spawn-locations`.
/// The Java DTO omits `id`/`factionId` (the `ConversionService` maps only the
/// galaxy + range columns).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct FactionSpawnLocationDto {
    pub galaxy_id: u16,
    pub sector_range_start: Option<u32>,
    pub sector_range_end: Option<u32>,
    pub quadrant_range_start: Option<u32>,
    pub quadrant_range_end: Option<u32>,
}

/// `PUT {factionId}/unitTypes` request body item — `UnitTypesOverride` (a
/// `UnitTypeDto` carrying `id` = the unit type and `overrideMaxCount`). Only
/// those two fields are consumed by `FactionBo.saveOverrides`.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FactionUnitTypeOverrideInput {
    /// The unit type id (`UnitTypeDto.id`).
    pub id: u16,
    #[serde(default)]
    pub override_max_count: Option<u32>,
}

/// `PUT {factionId}/spawn-locations` request body item — mirrors
/// `FactionSpawnLocationDto` on the write path.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct FactionSpawnLocationInput {
    pub galaxy_id: u16,
    #[serde(default)]
    pub sector_range_start: Option<u32>,
    #[serde(default)]
    pub sector_range_end: Option<u32>,
    #[serde(default)]
    pub quadrant_range_start: Option<u32>,
    #[serde(default)]
    pub quadrant_range_end: Option<u32>,
}
