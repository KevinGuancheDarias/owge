//! Mirrors `ObtainedUnitDto`
//! (`com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto`).
//!
//! The `unit_obtained_change` sync payload only carries *completed* obtained
//! units (those sitting on an owned planet, not in a running mission), so the
//! `mission` field is always null and is omitted here. Nested objects are kept
//! shallow: `unit` is a minimal embedded view and `sourcePlanet`/`targetPlanet`
//! reuse [`PlanetDto`]. `temporalInformation` / `storedUnits` belong to other
//! domains (time-special, stored-unit nesting) and are deferred.

use serde::Serialize;

use crate::dto::PlanetDto;

/// The `units` row embedded in an obtained unit, mirroring the scalar fields of
/// Java's `UnitDto` (`CommonDtoWithImageStore` + `UnitDto`). Field order and the
/// `Include.NON_NULL` null-omission match Jackson, so a fully-populated value
/// serialises equivalently to Java's report `unit` object.
///
/// The one deliberate omission is the deeply-nested `speedImpactGroup` (with its
/// `requirementsGroups` graph): the frontend report viewer reads only `name` and
/// `imageUrl` off a report unit (`reports-list.component`), and reproducing the
/// requirement-group graph inside every report is disproportionate. Add it here
/// if a consumer ever needs it.
///
/// Construct via [`ObtainedUnitUnitDto::reduced`] when only the combat/obtained
/// scalars are available (the `unit_obtained_change` sync path), or fill every
/// field for a report.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ObtainedUnitUnitDto {
    pub id: u16,
    pub name: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub description: Option<String>,
    /// `units.image_id` (the `images_store` FK) — Jackson serialises it as `image`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image: Option<u64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub image_url: Option<String>,
    pub has_to_display_in_requirements: bool,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub points: Option<u32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub time: Option<i32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub primary_resource: Option<u32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub secondary_resource: Option<u32>,
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
}

/// Compute the public image URL the way Java's `ImageStoreBo.computeImageUrl`
/// does: `/<OWGE_DYNAMIC_URL=dynamic>/<filename>`. Delegates to the single
/// source of truth in `image_store_bo` so the `OWGE_DYNAMIC_URL` segment stays
/// consistent across every embedded image URL.
pub fn compute_unit_image_url(filename: &str) -> String {
    crate::bo::image_store_bo::compute_image_url(filename)
}

impl ObtainedUnitUnitDto {
    /// The reduced view used where only the obtained-unit/combat scalars are at
    /// hand (the `unit_obtained_change` sync, which predates the full catalog
    /// join). Leaves the catalog-only fields `None` so they are omitted, matching
    /// that payload's existing shape.
    #[allow(clippy::too_many_arguments)]
    pub fn reduced(
        id: u16,
        name: String,
        type_id: Option<u16>,
        type_name: Option<String>,
        attack: Option<u16>,
        health: Option<u16>,
        shield: Option<u16>,
        charge: Option<u16>,
        is_unique: bool,
        can_fast_explore: bool,
        speed: Option<f64>,
        bypass_shield: bool,
        is_invisible: bool,
        stored_weight: u32,
        storage_capacity: Option<u32>,
    ) -> Self {
        Self {
            id,
            name,
            description: None,
            image: None,
            image_url: None,
            has_to_display_in_requirements: false,
            points: None,
            time: None,
            primary_resource: None,
            secondary_resource: None,
            energy: None,
            type_id,
            type_name,
            attack,
            health,
            shield,
            charge,
            is_unique,
            can_fast_explore,
            speed,
            cloned_improvements: false,
            bypass_shield,
            is_invisible,
            stored_weight,
            storage_capacity,
        }
    }
}

/// Mirrors the serialized `ObtainedUnitTemporalInformation` entity embedded in
/// `ObtainedUnitDto.temporalInformation`. Only present for temporal (time-special
/// granted) units. The frontend (`unit.util.ts`) derives the countdown's
/// `expirationDate` from `expiration` as `new Date(expiration * 1000)`, so
/// `expiration` is **epoch seconds** — matching Jackson's
/// `WRITE_DATES_AS_TIMESTAMPS` serialization of the entity's `Instant`.
/// `pendingMillis` mirrors the `@Transient` field the Java
/// `TemporalInformationUnitDataLoaderService` populates (millis until expiry).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct TemporalInformationDto {
    pub id: u32,
    pub duration: u32,
    /// Epoch **seconds** (Jackson `Instant` timestamp form). Fractional `.0` is
    /// numerically identical to Java's nanosecond-decimal output for a
    /// second-precision `timestamp` column.
    pub expiration: f64,
    pub relation_id: u16,
    pub pending_millis: i64,
}

#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ObtainedUnitDto {
    pub id: u64,
    pub unit: ObtainedUnitUnitDto,
    pub count: u64,
    pub source_planet: Option<PlanetDto>,
    pub target_planet: Option<PlanetDto>,
    pub user_id: i32,
    pub username: Option<String>,
    /// Present only for temporal (time-special granted) units; omitted otherwise
    /// (Jackson `Include.NON_NULL`).
    #[serde(skip_serializing_if = "Option::is_none")]
    pub temporal_information: Option<TemporalInformationDto>,
}
