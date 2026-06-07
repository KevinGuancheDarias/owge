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
            r.id, r.name, r.sector, r.quadrant, r.planet_number,
            r.owner_id, r.owner_name, r.richness, r.home, r.galaxy_id, r.galaxy_name,
        )
    }
}

#[allow(clippy::too_many_arguments)]
fn planet_dto_from_parts(
    id: u64, name: String, sector: u32, quadrant: u32, planet_number: u16,
    owner_id: Option<i32>, owner_name: Option<String>, richness: u16, home: i8,
    galaxy_id: u16, galaxy_name: String,
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
    }
}

/// Navigation-query row: same fields as [`PlanetDtoRow`] plus `is_explored`.
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
    home: i8,
    galaxy_id: u16,
    galaxy_name: String,
    is_explored: Option<bool>,
}

impl From<NavPlanetRow> for PlanetDto {
    fn from(r: NavPlanetRow) -> Self {
        let mut dto = planet_dto_from_parts(
            r.id, r.name, r.sector, r.quadrant, r.planet_number,
            r.owner_id, r.owner_name, r.richness, r.home, r.galaxy_id, r.galaxy_name,
        );
        if !r.is_explored.unwrap_or(false) {
            dto.clean_up_unexplored();
        }
        dto
    }
}
