//! Port of (the read side of) `PlanetListBo` — the user's saved/named planets
//! and the `planet_user_list_change` sync payload
//! (`PlanetListBo.findByUserId`).
//!
//! Java additionally runs `planetCleanerService.cleanUpUnexplored` over each
//! listed planet, which resets fields of still-unexplored planets. That depends
//! on the exploration/mission state, so the cleanup is deferred (see TODO);
//! the raw joined rows are returned for now, matching the explored-planet case.

use crate::bo::PlanetBo;
use crate::db::Db;
use crate::dto::{PlanetDto, PlanetListDto};
use crate::error::OwgeResult;

/// A `planet_list` row joined with the listing user, the planet, its galaxy and
/// the planet's (optional) owner. Column types are the exact schema types so
/// sqlx decode never panics on signedness/width.
#[derive(sqlx::FromRow)]
struct PlanetListRow {
    user_id: i32,
    username: String,
    list_name: Option<String>,
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

impl From<PlanetListRow> for PlanetListDto {
    fn from(r: PlanetListRow) -> Self {
        PlanetListDto {
            user_id: r.user_id,
            username: r.username,
            name: r.list_name,
            planet: PlanetDto {
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
            },
        }
    }
}

pub struct PlanetListBo;

impl PlanetListBo {
    /// `PlanetListRestService.add` — add/rename a named planet in the user's
    /// list (`planet_list` has no surrogate key; idempotent per (user, planet)).
    pub async fn add(db: &Db, user_id: i32, planet_id: u64, name: Option<&str>) -> OwgeResult<()> {
        let exists: i64 = sqlx::query_scalar(
            "SELECT COUNT(*) FROM planet_list WHERE user_id = ? AND planet_id = ?",
        )
        .bind(user_id)
        .bind(planet_id)
        .fetch_one(db)
        .await?;
        if exists == 0 {
            sqlx::query("INSERT INTO planet_list (user_id, planet_id, name) VALUES (?, ?, ?)")
                .bind(user_id)
                .bind(planet_id)
                .bind(name)
                .execute(db)
                .await?;
        } else {
            sqlx::query("UPDATE planet_list SET name = ? WHERE user_id = ? AND planet_id = ?")
                .bind(name)
                .bind(user_id)
                .bind(planet_id)
                .execute(db)
                .await?;
        }
        Ok(())
    }

    /// `PlanetListRestService.delete`.
    pub async fn delete(db: &Db, user_id: i32, planet_id: u64) -> OwgeResult<()> {
        sqlx::query("DELETE FROM planet_list WHERE user_id = ? AND planet_id = ?")
            .bind(user_id)
            .bind(planet_id)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `PlanetListBo.findByUserId(userId)` -> DTOs — the
    /// `planet_user_list_change` sync payload.
    pub async fn find_by_user_id(db: &Db, user_id: i32) -> OwgeResult<Vec<PlanetListDto>> {
        let rows = sqlx::query_as::<_, PlanetListRow>(
            "SELECT pl.user_id AS user_id, u.username AS username, pl.name AS list_name, \
                    p.id, p.name, p.sector, p.quadrant, p.planet_number AS planet_number, \
                    p.owner AS owner_id, o.username AS owner_name, p.richness, \
                    COALESCE(p.home, 0) AS home, p.galaxy_id AS galaxy_id, g.name AS galaxy_name \
             FROM planet_list pl \
             JOIN user_storage u ON u.id = pl.user_id \
             JOIN planets p ON p.id = pl.planet_id \
             JOIN galaxies g ON g.id = p.galaxy_id \
             LEFT JOIN user_storage o ON o.id = p.owner \
             WHERE pl.user_id = ? \
             ORDER BY p.id",
        )
        .bind(user_id)
        .fetch_all(db)
        .await?;
        // PlanetListBo.findByUserId maps each PlanetListDto.getPlanet through
        // planetCleanerService.cleanUpUnexplored(userId, planet): mask the fields
        // of any listed planet the user has not explored.
        let mut dtos: Vec<PlanetListDto> = rows.into_iter().map(Into::into).collect();
        for dto in dtos.iter_mut() {
            if !PlanetBo::is_explored(db, user_id, dto.planet.id).await? {
                dto.planet.clean_up_unexplored();
            }
        }
        Ok(dtos)
    }
}
