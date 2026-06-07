//! Port of (the read side of) `GalaxyBo` and the navigation read of `PlanetBo`,
//! backing `GET game/galaxy/navigate?galaxyId=&sector=&quadrant=`
//! (`GalaxyRestService.navigate`).
//!
//! The Java endpoint returns a `NavigationPojo { galaxies, planets }`:
//! - `galaxies` is every galaxy (`GalaxyBo.findAll`), and
//! - `planets` is every planet at the requested location
//!   (`PlanetBo.findByGalaxyAndSectorAndQuadrant`).
//!
//! `PlanetCleanerService.cleanUpUnexplored` (which blanks owner info on planets
//! the requesting user has not yet explored) depends on the mission/exploration
//! system and is **not** applied here yet — see the TODO on
//! [`GalaxyBo::find_planets_at`].

use crate::db::Db;
use crate::dto::{GalaxyDto, GalaxyInput, PlanetDto};
use crate::error::{OwgeError, OwgeResult};

#[derive(sqlx::FromRow)]
struct GalaxyRow {
    id: u16,
    name: String,
    sectors: u32,
    quadrants: u32,
    num_planets: u32,
    order_number: Option<u16>,
}

impl From<GalaxyRow> for GalaxyDto {
    fn from(r: GalaxyRow) -> Self {
        GalaxyDto {
            id: r.id,
            name: r.name,
            sectors: r.sectors,
            quadrants: r.quadrants,
            num_planets: r.num_planets,
            order_number: r.order_number,
        }
    }
}

/// A planet row at a navigated location joined with its galaxy and (optional)
/// owner, with exact SQL column types so sqlx never panics on signedness/width.
#[derive(sqlx::FromRow)]
struct NavPlanetRow {
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
        let mut ret_val = PlanetDto {
            id: r.id,
            name: Some(r.name),
            sector: r.sector,
            quadrant: r.quadrant,
            planet_number: r.planet_number,
            owner_id: r.owner_id,
            owner_name: r.owner_name,
            richness: Some(r.richness),
            home: Some(r.home != 0),
            galaxy_id: r.galaxy_id,
            galaxy_name: r.galaxy_name,
        };
        if !r.is_explored.unwrap_or(false) {
            ret_val.clean_up_unexplored();
        }

        ret_val
    }
}

pub struct GalaxyBo;

impl GalaxyBo {
    /// `GalaxyBo.findAll()` — every galaxy, ordered by id.
    pub async fn find_all(db: &Db) -> OwgeResult<Vec<GalaxyDto>> {
        let rows = sqlx::query_as::<_, GalaxyRow>(
            "SELECT id, name, sectors, quadrants, num_planets AS num_planets, \
                    order_number AS order_number \
             FROM galaxies ORDER BY id",
        )
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `WithReadRestServiceTrait.findOneById` for the admin galaxy CRUD.
    pub async fn find_by_id(db: &Db, id: u16) -> OwgeResult<Option<GalaxyDto>> {
        let row = sqlx::query_as::<_, GalaxyRow>(
            "SELECT id, name, sectors, quadrants, num_planets, order_number \
             FROM galaxies WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(db)
        .await?;
        Ok(row.map(Into::into))
    }

    /// `CrudRestServiceTrait.saveNew` — insert; `galaxies.id` is AUTO_INCREMENT.
    pub async fn save_new(db: &Db, input: &GalaxyInput) -> OwgeResult<GalaxyDto> {
        let result = sqlx::query(
            "INSERT INTO galaxies (name, sectors, quadrants, num_planets, order_number) \
             VALUES (?, ?, ?, ?, ?)",
        )
        .bind(&input.name)
        .bind(input.sectors)
        .bind(input.quadrants)
        .bind(input.num_planets)
        .bind(input.order_number)
        .execute(db)
        .await?;
        let id = result.last_insert_id() as u16;
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Galaxy vanished right after insert".into()))
    }

    /// `CrudRestServiceTrait.saveExisting` — update by id.
    pub async fn save_existing(db: &Db, id: u16, input: &GalaxyInput) -> OwgeResult<GalaxyDto> {
        let affected = sqlx::query(
            "UPDATE galaxies SET name = ?, sectors = ?, quadrants = ?, num_planets = ?, \
                    order_number = ? WHERE id = ?",
        )
        .bind(&input.name)
        .bind(input.sectors)
        .bind(input.quadrants)
        .bind(input.num_planets)
        .bind(input.order_number)
        .bind(id)
        .execute(db)
        .await?
        .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No galaxy with id {id}")));
        }
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No galaxy with id {id}")))
    }

    /// `WithDeleteRestServiceTrait.delete`.
    pub async fn delete(db: &Db, id: u16) -> OwgeResult<()> {
        sqlx::query("DELETE FROM galaxies WHERE id = ?")
            .bind(id)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `AdminGalaxiesRestService.hasPlayers` — any owned planet in the galaxy.
    pub async fn has_players(db: &Db, id: u16) -> OwgeResult<bool> {
        let count: i64 = sqlx::query_scalar(
            "SELECT COUNT(*) FROM planets WHERE galaxy_id = ? AND owner IS NOT NULL",
        )
        .bind(id)
        .fetch_one(db)
        .await?;
        Ok(count > 0)
    }

    /// `PlanetBo.findByGalaxyAndSectorAndQuadrant(galaxyId, sector, quadrant)` —
    /// the planets at a navigated location, as `PlanetDto`s.
    ///
    pub async fn find_planets_at(
        db: &Db,
        galaxy_id: u16,
        sector: u32,
        quadrant: u32,
        user: Option<i64>,
    ) -> OwgeResult<Vec<PlanetDto>> {
        let rows = sqlx::query_as::<_, NavPlanetRow>(
            "SELECT p.id, p.name, p.sector, p.quadrant, p.planet_number AS planet_number, \
                    p.owner AS owner_id, o.username AS owner_name, p.richness, ? as the_user, \
                    COALESCE(p.home, 0) AS home, p.galaxy_id AS galaxy_id, g.name AS galaxy_name, \
                (SELECT owner_id = the_user OR EXISTS(SELECT 1 FROM explored_planets WHERE `user` = the_user AND planet = p.id)) AS is_explored \
             FROM planets p \
             JOIN galaxies g ON g.id = p.galaxy_id \
             LEFT JOIN user_storage o ON o.id = p.owner \
             WHERE p.galaxy_id = ? AND p.sector = ? AND p.quadrant = ? \
             ORDER BY p.planet_number",
        )
        .bind(user.unwrap_or(0))
        .bind(galaxy_id)
        .bind(sector)
        .bind(quadrant)
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }
}
