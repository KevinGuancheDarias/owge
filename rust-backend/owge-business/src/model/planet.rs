use crate::dto::special_location::SpecialLocationDto;
use crate::dto::PlanetDto;
use serde::{Deserialize, Serialize};

/// Mirrors the `planets` table / Java `Planet` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct Planet {
    /// `bigint unsigned`.
    pub id: u64,
    pub name: String,
    #[sqlx(rename = "galaxy_id")]
    pub galaxy_id: u16,
    /// `int unsigned`.
    pub sector: u32,
    pub quadrant: u32,
    #[sqlx(rename = "planet_number")]
    pub planet_number: u16,
    /// `int` (signed, nullable) — the owning user id, or NULL if unowned.
    pub owner: Option<i32>,
    pub richness: u16,
    /// `tinyint` nullable, default 0.
    pub home: Option<i8>,
    #[sqlx(rename = "special_location_id")]
    pub special_location_id: Option<u16>,
}

#[derive(sqlx::FromRow)]
pub struct PlanetDtoRow {
    id: u64,
    name: String,
    sector: u32,
    quadrant: u32,
    planet_number: u16,
    owner_id: Option<i32>,
    owner_name: Option<String>,
    richness: u16,
    home: i8,
    galaxy_id: u16,
    galaxy_name: String,
}

impl From<PlanetDtoRow> for PlanetDto {
    fn from(r: PlanetDtoRow) -> Self {
        planet_dto_from_parts(
            r.id,
            r.name,
            r.sector,
            r.quadrant,
            r.planet_number,
            r.owner_id,
            r.owner_name,
            r.richness,
            r.home,
            r.galaxy_id,
            r.galaxy_name,
        )
    }
}

#[allow(clippy::too_many_arguments)]
fn planet_dto_from_parts(
    id: u64,
    name: String,
    sector: u32,
    quadrant: u32,
    planet_number: u16,
    owner_id: Option<i32>,
    owner_name: Option<String>,
    richness: u16,
    home: i8,
    galaxy_id: u16,
    galaxy_name: String,
) -> PlanetDto {
    PlanetDto {
        id,
        name: Some(name),
        sector,
        quadrant,
        planet_number,
        owner_id,
        owner_name,
        richness: Some(richness),
        home: Some(home != 0),
        galaxy_id,
        galaxy_name,
        // Only the navigation read fetches the planet's special location; other
        // callers leave it absent (matching Java, where it is populated only
        // when the lazy `Planet.specialLocation` is navigated).
        special_location: None,
    }
}

/// Navigation-query row: same fields as [`PlanetDtoRow`] plus `is_explored` and
/// the planet's special location (`sl_*`, joined from `special_locations` via
/// `planets.special_location_id`). The special-location columns are all `NULL`
/// when the planet has none.
#[derive(sqlx::FromRow)]
pub(crate) struct NavPlanetRow {
    id: u64,
    name: String,
    sector: u32,
    quadrant: u32,
    planet_number: u16,
    owner_id: Option<i32>,
    owner_name: Option<String>,
    richness: u16,
    // Nullable in the DB; Java omits `home` when it is NULL (Jackson `NON_NULL`),
    // so it is *not* coalesced to 0 here.
    home: Option<i8>,
    galaxy_id: u16,
    galaxy_name: String,
    is_explored: Option<bool>,
    sl_id: Option<u16>,
    sl_name: Option<String>,
    sl_description: Option<String>,
    sl_image_id: Option<u64>,
    sl_image_filename: Option<String>,
    sl_galaxy_id: Option<u16>,
    sl_galaxy_name: Option<String>,
    /// The special location's `improvement_id`. Read by `find_planets_at` to
    /// load the nested `improvement` for explored planets (the `From` impl is
    /// sync and cannot query, so it leaves `improvement` `None`).
    pub(crate) sl_improvement_id: Option<u16>,
}

impl From<NavPlanetRow> for PlanetDto {
    fn from(r: NavPlanetRow) -> Self {
        // The special location's `assignedPlanet` (`mappedBy = "specialLocation"`)
        // is the planet pointing back at it — i.e. this very planet `p` — so the
        // assigned-planet fields are the planet's own id/name.
        let special_location = r.sl_id.map(|id| SpecialLocationDto {
            id,
            name: r.sl_name.unwrap_or_default(),
            description: r.sl_description.unwrap_or_default(),
            image: r.sl_image_id,
            image_url: r
                .sl_image_filename
                .map(|f| crate::bo::image_store_bo::compute_image_url(&f)),
            // Loaded by `find_planets_at` for explored planets (see struct docs).
            improvement: None,
            galaxy_id: r.sl_galaxy_id,
            galaxy_name: r.sl_galaxy_name,
            assigned_planet_id: Some(r.id),
            assigned_planet_name: Some(r.name.clone()),
        });
        let home = r.home;
        let mut dto = planet_dto_from_parts(
            r.id,
            r.name,
            r.sector,
            r.quadrant,
            r.planet_number,
            r.owner_id,
            r.owner_name,
            r.richness,
            // Placeholder; overridden below so a SQL NULL stays `None` (omitted),
            // matching Java rather than coercing it to `false`.
            0,
            r.galaxy_id,
            r.galaxy_name,
        );
        dto.home = home.map(|h| h != 0);
        dto.special_location = special_location;
        if !r.is_explored.unwrap_or(false) {
            dto.clean_up_unexplored();
        }
        dto
    }
}
