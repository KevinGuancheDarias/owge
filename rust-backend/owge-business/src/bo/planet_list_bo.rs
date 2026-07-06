//! Port of (the read side of) `PlanetListBo` — the user's saved/named planets
//! and the `planet_user_list_change` sync payload
//! (`PlanetListBo.findByUserId`).
//!
//! Java additionally runs `planetCleanerService.cleanUpUnexplored` over each
//! listed planet, which resets fields of still-unexplored planets
//! (`find_by_user_id` applies it via a post-pass, see below).

use crate::bo::ImprovementBo;
use crate::bo::PlanetBo;
use crate::dto::special_location::SpecialLocationDto;
use crate::dto::{PlanetDto, PlanetListDto};
use crate::error::OwgeResult;
use sqlx::MySqlConnection;

/// A `planet_list` row joined with the listing user, the planet, its galaxy,
/// the planet's (optional) owner, and its (optional) special location. Column
/// types are the exact schema types so sqlx decode never panics on
/// signedness/width. `home` is nullable — Java's Jackson `NON_NULL` omits it
/// when the column is SQL NULL rather than coercing it to `false`.
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
    home: Option<i8>,
    galaxy_id: u16,
    galaxy_name: String,
    sl_id: Option<u16>,
    sl_name: Option<String>,
    sl_description: Option<String>,
    sl_image_id: Option<u64>,
    sl_image_filename: Option<String>,
    sl_galaxy_id: Option<u16>,
    sl_galaxy_name: Option<String>,
    /// Read by `find_by_user_id` to load the nested `improvement` for planets
    /// that remain explored after the `cleanUpUnexplored` pass.
    sl_improvement_id: Option<u16>,
}

impl From<PlanetListRow> for PlanetListDto {
    fn from(r: PlanetListRow) -> Self {
        let special_location = r.sl_id.map(|id| SpecialLocationDto {
            id,
            name: r.sl_name.unwrap_or_default(),
            description: r.sl_description.unwrap_or_default(),
            image: r.sl_image_id,
            image_url: r
                .sl_image_filename
                .map(|f| crate::bo::image_store_bo::compute_image_url(&f)),
            improvement: None,
            galaxy_id: r.sl_galaxy_id,
            galaxy_name: r.sl_galaxy_name,
            assigned_planet_id: Some(r.id),
            assigned_planet_name: Some(r.name.clone()),
        });
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
                home: r.home.map(|h| h != 0),
                galaxy_id: r.galaxy_id,
                galaxy_name: r.galaxy_name,
                special_location,
            },
        }
    }
}

pub struct PlanetListBo;

impl PlanetListBo {
    /// `PlanetListRestService.add` — add/rename a named planet in the user's
    /// list (`planet_list` has no surrogate key; idempotent per (user, planet)).
    pub async fn add(
        conn: &mut MySqlConnection,
        user_id: i32,
        planet_id: u64,
        name: Option<&str>,
    ) -> OwgeResult<()> {
        let exists: i64 = sqlx::query_scalar(
            "SELECT COUNT(*) FROM planet_list WHERE user_id = ? AND planet_id = ?",
        )
        .bind(user_id)
        .bind(planet_id)
        .fetch_one(&mut *conn)
        .await?;
        if exists == 0 {
            sqlx::query("INSERT INTO planet_list (user_id, planet_id, name) VALUES (?, ?, ?)")
                .bind(user_id)
                .bind(planet_id)
                .bind(name)
                .execute(&mut *conn)
                .await?;
        } else {
            sqlx::query("UPDATE planet_list SET name = ? WHERE user_id = ? AND planet_id = ?")
                .bind(name)
                .bind(user_id)
                .bind(planet_id)
                .execute(&mut *conn)
                .await?;
        }
        Ok(())
    }

    /// `PlanetListRestService.delete`.
    pub async fn delete(
        conn: &mut MySqlConnection,
        user_id: i32,
        planet_id: u64,
    ) -> OwgeResult<()> {
        sqlx::query("DELETE FROM planet_list WHERE user_id = ? AND planet_id = ?")
            .bind(user_id)
            .bind(planet_id)
            .execute(&mut *conn)
            .await?;
        Ok(())
    }

    /// `PlanetListBo.findByUserId(userId)` -> DTOs — the
    /// `planet_user_list_change` sync payload.
    pub async fn find_by_user_id(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<PlanetListDto>> {
        let rows = sqlx::query_as::<_, PlanetListRow>(
            "SELECT pl.user_id AS user_id, u.username AS username, pl.name AS list_name, \
                    p.id, p.name, p.sector, p.quadrant, p.planet_number AS planet_number, \
                    p.owner AS owner_id, o.username AS owner_name, p.richness, \
                    p.home AS home, p.galaxy_id AS galaxy_id, g.name AS galaxy_name, \
                    sl.id AS sl_id, sl.name AS sl_name, sl.description AS sl_description, \
                    sl.image_id AS sl_image_id, sli.filename AS sl_image_filename, \
                    sl.galaxy_id AS sl_galaxy_id, slg.name AS sl_galaxy_name, \
                    sl.improvement_id AS sl_improvement_id \
             FROM planet_list pl \
             JOIN user_storage u ON u.id = pl.user_id \
             JOIN planets p ON p.id = pl.planet_id \
             JOIN galaxies g ON g.id = p.galaxy_id \
             LEFT JOIN user_storage o ON o.id = p.owner \
             LEFT JOIN special_locations sl ON sl.id = p.special_location_id \
             LEFT JOIN images_store sli ON sli.id = sl.image_id \
             LEFT JOIN galaxies slg ON slg.id = sl.galaxy_id \
             WHERE pl.user_id = ? \
             ORDER BY p.id",
        )
        .bind(user_id)
        .fetch_all(&mut *conn)
        .await?;
        // PlanetListBo.findByUserId maps each PlanetListDto.getPlanet through
        // planetCleanerService.cleanUpUnexplored(userId, planet): mask the fields
        // of any listed planet the user has not explored. For planets that
        // remain explored, the special location's (otherwise lazy)
        // `improvement` is loaded too (Java is not lazy on this path — see
        // `GalaxyBo::find_planets_at` / `PlanetBo::find_owned_dtos`).
        let mut dtos = Vec::with_capacity(rows.len());
        for row in rows {
            let sl_improvement_id = row.sl_improvement_id;
            let mut dto = PlanetListDto::from(row);
            if !PlanetBo::is_explored(&mut *conn, user_id, dto.planet.id).await? {
                dto.planet.clean_up_unexplored();
            } else if let Some(sl) = dto.planet.special_location.as_mut() {
                if let Some(improvement_id) = sl_improvement_id {
                    sl.improvement =
                        Some(ImprovementBo::find_dto(&mut *conn, Some(improvement_id)).await?);
                }
            }
            dtos.push(dto);
        }
        Ok(dtos)
    }
}
