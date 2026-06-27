//! Port of (the read side of) `PlanetBo`. Builds `PlanetDto`s with the owner
//! and galaxy names resolved via joins, matching `PlanetBo.toDto`.

use crate::bo::mission_finder_bo::MissionFinderBo;
use crate::bo::realtime_emitter;
use crate::dto::PlanetDto;
use crate::error::{OwgeError, OwgeResult};
use sqlx::{Connection, MySqlConnection};

/// A planet row joined with its galaxy and (optional) owner, with exact SQL
/// column types so sqlx decode never panics on signedness/width.
#[derive(sqlx::FromRow)]
struct PlanetRow {
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

impl From<PlanetRow> for PlanetDto {
    fn from(r: PlanetRow) -> Self {
        PlanetDto {
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
            special_location: None,
        }
    }
}

const SELECT_DTO: &str = "\
    SELECT p.id, p.name, p.sector, p.quadrant, p.planet_number AS planet_number, \
           p.owner AS owner_id, o.username AS owner_name, p.richness, \
           COALESCE(p.home, 0) AS home, p.galaxy_id AS galaxy_id, g.name AS galaxy_name \
    FROM planets p \
    JOIN galaxies g ON g.id = p.galaxy_id \
    LEFT JOIN user_storage o ON o.id = p.owner ";

pub struct PlanetBo;

impl PlanetBo {
    /// `planetRepository.findByOwnerId(userId)` -> DTOs — the
    /// `planet_owned_change` sync payload.
    pub async fn find_owned_dtos(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<PlanetDto>> {
        let rows = sqlx::query_as::<_, PlanetRow>(&format!(
            "{SELECT_DTO} WHERE p.owner = ? ORDER BY COALESCE(p.home,0) DESC, p.id"
        ))
        .bind(user_id)
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    pub async fn find_by_id(conn: &mut MySqlConnection, id: u64) -> OwgeResult<Option<PlanetDto>> {
        let row = sqlx::query_as::<_, PlanetRow>(&format!("{SELECT_DTO} WHERE p.id = ?"))
            .bind(id)
            .fetch_optional(&mut *conn)
            .await?;
        Ok(row.map(Into::into))
    }

    pub async fn find_by_id_or_die(conn: &mut MySqlConnection, id: u64) -> OwgeResult<PlanetDto> {
        Self::find_by_id(&mut *conn, id)
            .await?
            .ok_or(OwgeError::NotFound("Planet not found".to_string()))
    }

    /// `PlanetBo.canLeavePlanet(invokerId, planetId)`.
    ///
    /// True iff the planet is **not** a home planet, **is** owned by the invoker,
    /// the invoker has **no** units stationed on it (`obtained_units` with the
    /// planet as source and `mission IS NULL`), and there is **no** running
    /// BUILD_UNIT mission targeting the planet.
    pub async fn can_leave_planet(
        conn: &mut MySqlConnection,
        user_id: i32,
        planet_id: u64,
    ) -> OwgeResult<bool> {
        // !isHomePlanet(planetId): planetRepository.findOneByIdAndHomeTrue(planetId) != null
        let is_home: bool =
            sqlx::query_scalar("SELECT COUNT(*) > 0 FROM planets WHERE id = ? AND home = 1")
                .bind(planet_id)
                .fetch_one(&mut *conn)
                .await?;
        if is_home {
            return Ok(false);
        }

        // planetRepository.isOfUserProperty(invokerId, planetId)
        let is_owner: bool =
            sqlx::query_scalar("SELECT COUNT(*) > 0 FROM planets WHERE id = ? AND owner = ?")
                .bind(planet_id)
                .bind(user_id)
                .fetch_one(&mut *conn)
                .await?;
        if !is_owner {
            return Ok(false);
        }

        // !obtainedUnitRepository.hasUnitsInPlanet(invokerId, planetId)
        let has_units: bool = sqlx::query_scalar(
            "SELECT COUNT(*) > 0 FROM obtained_units \
             WHERE user_id = ? AND source_planet = ? AND mission_id IS NULL",
        )
        .bind(user_id)
        .bind(planet_id)
        .fetch_one(&mut *conn)
        .await?;
        if has_units {
            return Ok(false);
        }

        // missionFinderBo.findRunningUnitBuild(invokerId, planetId) == null
        if MissionFinderBo::has_running_unit_build(&mut *conn, user_id, planet_id).await? {
            return Ok(false);
        }

        Ok(true)
    }

    /// `PlanetBo.doLeavePlanet(invokerId, planetId)` — relinquish a (non-home)
    /// owned planet: clears `planets.owner`, then (Java) fires the
    /// special-location requirement trigger and emits `planet_owned_change`.
    ///
    /// Mirrors the Java guard exactly: if `canLeavePlanet` is false it throws
    /// `SgtBackendInvalidInputException("ERR_I18N_CAN_NOT_LEAVE_PLANET")`
    /// -> `InvalidInput` (HTTP 400).
    pub async fn leave_planet(
        conn: &mut MySqlConnection,
        user_id: i32,
        planet_id: u64,
    ) -> OwgeResult<()> {
        if !Self::can_leave_planet(&mut *conn, user_id, planet_id).await? {
            return Err(OwgeError::InvalidInput(
                "ERR_I18N_CAN_NOT_LEAVE_PLANET".into(),
            ));
        }

        // var planet = findById(planetId); var user = planet.getOwner();
        // Capture the planet's special location AND its owner *before* clearing it
        // (Java reads `planet.getOwner()` / `planet.getSpecialLocation()` first).
        // canLeavePlanet already proved the invoker is the owner, so the former
        // owner is `user_id`; we still read it to mirror the Java `var user`.
        let (former_owner, special_location_id): (Option<i32>, Option<u16>) =
            sqlx::query_as("SELECT owner, special_location_id FROM planets WHERE id = ?")
                .bind(planet_id)
                .fetch_one(&mut *conn)
                .await?;

        // planet.setOwner(null); planetRepository.save(planet);
        sqlx::query("UPDATE planets SET owner = NULL WHERE id = ?")
            .bind(planet_id)
            .execute(&mut *conn)
            .await?;

        // maybeTriggerSpecialLocation(planet, user): if the planet has a
        // special_location, RequirementBo.triggerSpecialLocation re-evaluates the
        // relations gated by HAVE_SPECIAL_LOCATION for the (former) owner. The
        // resulting `*_unlocked_change` pushes are collected and drained after the
        // trigger's writes (this method runs on the conn, no surrounding tx, so
        // we wrap the trigger in its own savepoint transaction for a consistent view).
        let mut req_emits = Vec::new();
        if let (Some(former_owner_id), Some(special_location_id)) =
            (former_owner, special_location_id)
        {
            let mut tx = conn.begin().await?;
            let user = crate::bo::mission_bo::load_user_storage(&mut tx, former_owner_id).await?;
            crate::bo::requirement_bo::RequirementBo::trigger_special_location(
                &mut tx,
                &user,
                special_location_id as i64,
                &mut req_emits,
            )
            .await?;
            tx.commit().await?;
        }

        // Java: transactionUtilService.doAfterCommit(() -> planetListBo.emitByChangedPlanet(planet))
        // PlanetListBo.emitByChangedPlanet: for each user who has this planet in their
        // planet_list, emit planet_user_list_change. The UPDATE above auto-commits (no
        // surrounding tx), so we emit immediately after it.
        let list_holders: Vec<i32> =
            sqlx::query_scalar("SELECT DISTINCT user_id FROM planet_list WHERE planet_id = ?")
                .bind(planet_id)
                .fetch_all(&mut *conn)
                .await?;
        for holder_id in list_holders {
            realtime_emitter::emit_planet_user_list_change(&mut *conn, holder_id).await?;
        }

        // Java: emitPlanetOwnedChange(invokerId) ->
        // transactionUtilService.doAfterCommit(() ->
        //   socketIoService.sendMessage(userId, "planet_owned_change", () -> ...))
        realtime_emitter::emit_planet_owned_change(&mut *conn, user_id).await?;

        // Drain the special-location requirement triggers' `*_unlocked_change`
        // pushes (Java fires these via doAfterCommit inside the trigger).
        realtime_emitter::drain_requirement_emits(&mut *conn, &req_emits).await?;

        Ok(())
    }

    /// `PlanetExplorationService.isExplored(userId, planetId)` — true when the user
    /// owns the planet or has an `explored_planets` row for it. `explored_planets`
    /// keys the user with column `user` (signed `int`) and `planet`.
    pub async fn is_explored(
        conn: &mut MySqlConnection,
        user_id: i32,
        planet_id: u64,
    ) -> OwgeResult<bool> {
        let found: bool = sqlx::query_scalar(
            "SELECT EXISTS(SELECT 1 FROM planets WHERE id = ? AND owner = ?) \
             OR EXISTS(SELECT 1 FROM explored_planets WHERE `user` = ? AND planet = ?)",
        )
        .bind(planet_id)
        .bind(user_id)
        .bind(user_id)
        .bind(planet_id)
        .fetch_one(&mut *conn)
        .await?;
        Ok(found)
    }
}
