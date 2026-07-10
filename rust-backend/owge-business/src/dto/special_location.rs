use serde::{Deserialize, Serialize};

// `ImprovementDto` now lives in the shared `dto::improvement` module so that
// unit/faction/upgrade can reuse it; re-exported here for the existing
// `dto::special_location::ImprovementDto` import sites.
pub use crate::dto::improvement::ImprovementDto;

/// Mirrors `SpecialLocationDto` (`CommonDtoWithImageStore` + `DtoWithImprovements`).
///
/// `image`/`imageUrl` come from the joined `images_store` row; `galaxyId`/
/// `galaxyName` and `assignedPlanetId`/`assignedPlanetName` from the joined
/// galaxy / assigned planet. The CRUD trait's `beforeRequestEnd` nulls the
/// `improvement` field on every CRUD response (the populated improvement is
/// fetched via `GET {id}/improvement`) — and Java's mapper is NON_NULL
/// everywhere, so a nulled/unset field is OMITTED from the wire, never
/// `null`. The mission-payload SLIM shape (R1/D19) relies on that: only
/// `{id, name, description, assignedPlanetId, assignedPlanetName}` survive.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct SpecialLocationDto {
    pub id: u16,
    pub name: String,
    pub description: String,
    /// Bare image-store id (Java `image`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_url: Option<String>,
    /// Always unset (→ omitted) on CRUD responses (see struct docs).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub improvement: Option<ImprovementDto>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub galaxy_id: Option<u16>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub galaxy_name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub assigned_planet_id: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub assigned_planet_name: Option<String>,
}

/// Admin create/update request body for a special location.
///
/// `id` comes from the path on update and is AUTO_INCREMENT on create. Mirrors
/// `SpecialLocationDto` on the write side; `galaxyId` is the only relation
/// (`beforeSave` builds a `Galaxy` stub from it).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SpecialLocationInput {
    pub name: String,
    #[serde(default)]
    pub description: Option<String>,
    /// Bare image-store id (Java `image`).
    #[serde(default)]
    pub image: Option<u64>,
    #[serde(default)]
    pub galaxy_id: Option<u16>,
    #[serde(default)]
    pub cloned_improvements: bool,
}
