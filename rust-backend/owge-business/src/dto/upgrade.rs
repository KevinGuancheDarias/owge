//! DTOs for the upgrade domain, mirroring
//! `com.kevinguanchedarias.owgejava.dto.UpgradeTypeDto`,
//! `...dto.UpgradeDto`, and `...dto.ObtainedUpgradeDto`.
//!
//! Field names are Jackson camelCase. As with the other ported catalog DTOs
//! (`UnitTypeDto`, `ObtainedUnitDto`), the heavy nested objects driven by other
//! domains — `improvement` (improvement engine, M2) and `requirements`
//! (requirement system, M3) — are deferred and not emitted yet; the related
//! `type` is exposed via `typeId` / `typeName`.

use serde::{Deserialize, Serialize};

/// JSON payload for one upgrade type — the `upgrade_types_change` sync payload.
/// Mirrors `UpgradeTypeDto` (just `id` + `name`).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UpgradeTypeDto {
    pub id: u16,
    pub name: String,
}

/// Admin create/update request body for an upgrade type
/// (`AdminUpgradeTypeRestService`, a `CrudRestServiceTrait<UpgradeType>`).
/// The `id` comes from the path on update and is AUTO_INCREMENT on create.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UpgradeTypeInput {
    pub name: String,
}

/// JSON payload for one upgrade, mirroring `UpgradeDto` (which extends
/// `CommonDtoWithImageStore` / `CommonDto`). Embedded inside [`ObtainedUpgradeDto`].
///
/// `improvement` and `requirements` are omitted until the improvement engine
/// (M2) and requirement system (M3) are ported, matching how the other ported
/// DTOs keep nested objects shallow.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UpgradeDto {
    pub id: u16,
    pub name: String,
    pub description: Option<String>,
    /// The image id (`CommonDtoWithImageStore.image`), or null when no image.
    pub image: Option<u64>,
    /// The resolved image URL (`CommonDtoWithImageStore.imageUrl`). Built from
    /// the image filename in the `Bo` query.
    pub image_url: Option<String>,
    /// `UpgradeDto.order` (`order_number`).
    pub order: Option<u16>,
    pub points: i32,
    pub time: i64,
    pub primary_resource: i32,
    pub secondary_resource: i32,
    pub type_id: Option<u16>,
    pub type_name: Option<String>,
    pub level_effect: f32,
    pub cloned_improvements: bool,
}

/// Admin create/update request body for an upgrade
/// (`AdminUpgradeRestService`, a `CrudWithFullRestService<Upgrade>`). The `id`
/// comes from the path on update and is AUTO_INCREMENT on create.
///
/// Defaults mirror `AdminUpgradeRestService.beforeConversion`: `clonedImprovements`
/// -> false, `levelEffect` -> 0.5, `time` (null or < 5) -> 60. `typeId` is
/// mandatory (`beforeSave` throws `I18N_ERR_UPGRADE_TYPE_IS_MANDATORY` otherwise).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UpgradeInput {
    pub name: String,
    #[serde(default)]
    pub description: Option<String>,
    #[serde(default)]
    pub image: Option<u64>,
    #[serde(default)]
    pub points: i32,
    #[serde(default)]
    pub time: Option<i64>,
    #[serde(default)]
    pub primary_resource: i32,
    #[serde(default)]
    pub secondary_resource: i32,
    #[serde(default)]
    pub type_id: Option<u16>,
    #[serde(default)]
    pub level_effect: Option<f32>,
    #[serde(default)]
    pub cloned_improvements: Option<bool>,
}

/// JSON payload for one obtained upgrade — part of the `obtained_upgrades_change`
/// sync payload. Mirrors `ObtainedUpgradeDto`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ObtainedUpgradeDto {
    pub id: u32,
    pub level: i16,
    pub available: bool,
    pub upgrade: UpgradeDto,
}
