//! Port of `ExploreMissionProcessor`
//! (`...business.mission.processor.ExploreMissionProcessor`).
//!
//! Marks the target planet explored for the user (if not already), reads the
//! units present at the planet for the report, registers the return mission, and
//! builds the explore report.

use sqlx::MySqlConnection;

use crate::bo::return_mission_registration_bo::ReturnMissionRegistrationBo;
use crate::builder::UnitMissionReportBuilder;
use crate::dto::obtained_unit::ObtainedUnitDto;
use crate::error::OwgeResult;
use crate::model::mission::Mission;
use crate::model::obtained_unit::ObtainedUnit;

use super::create_report_base;

pub async fn process(
    conn: &mut MySqlConnection,
    mission: &Mission,
    involved_units: &[ObtainedUnit],
    emits: &mut Vec<super::DeferredEmit>,
) -> OwgeResult<Option<UnitMissionReportBuilder>> {
    let user_id = mission.user_id.unwrap_or_default();
    let target_planet_id = mission.target_planet.map(|p| p as u64);

    if let Some(target_planet_id) = target_planet_id {
        if !is_explored(conn, user_id, target_planet_id).await? {
            define_as_explored(conn, user_id, target_planet_id).await?;
            // PlanetExplorationService.defineAsExplored emits planet_explored_event
            // with the planet DTO — only on a *new* exploration.
            if let Some(planet) = super::load_planet_dto(conn, target_planet_id).await? {
                emits.push(super::DeferredEmit::PlanetExplored {
                    user_id,
                    planet: Box::new(planet),
                });
            }
        }
    }

    let units_in_planet = match target_planet_id {
        Some(target_planet_id) => explore_planet_units(conn, mission.id, target_planet_id).await?,
        None => Vec::new(),
    };

    // returnMissionRegistrationBo.registerReturnMission(mission, null) — which
    // itself emits emitLocalMissionChangeAfterCommit(returnMission).
    let return_id = ReturnMissionRegistrationBo::register_return_mission(conn, mission, None).await?;
    emits.push(super::DeferredEmit::LocalMissionChange {
        mission_id: return_id,
        user_id: mission.user_id.unwrap_or_default(),
    });

    let builder = create_report_base(conn, mission, involved_units)
        .await?
        .with_explored_information(&units_in_planet);

    // mission.setResolved(true) is persisted centrally by the report save path.
    Ok(Some(builder))
}

/// `PlanetExplorationService.isExplored` — the planet is the user's property, or
/// has an `explored_planets` row for the user.
async fn is_explored(conn: &mut MySqlConnection, user_id: i32, planet_id: u64) -> OwgeResult<bool> {
    let is_owned = super::is_planet_owned_by(conn, user_id, planet_id).await?;
    if is_owned {
        return Ok(true);
    }
    let count: i64 =
        sqlx::query_scalar("SELECT COUNT(*) FROM explored_planets WHERE user = ? AND planet = ?")
            .bind(user_id)
            .bind(planet_id)
            .fetch_one(&mut *conn)
            .await?;
    Ok(count > 0)
}

/// `PlanetExplorationService.defineAsExplored` — persist the `explored_planets`
/// row (the `planet_explored_event` websocket emission is M4, TODO).
async fn define_as_explored(
    conn: &mut MySqlConnection,
    user_id: i32,
    planet_id: u64,
) -> OwgeResult<()> {
    sqlx::query("INSERT INTO explored_planets (user, planet) VALUES (?, ?)")
        .bind(user_id)
        .bind(planet_id)
        .execute(&mut *conn)
        .await?;
    // TODO(M3/M4): socketIoService.sendMessage(user, PLANET_EXPLORED_EVENT, ...).
    Ok(())
}

/// `ObtainedUnitBo.explorePlanetUnits` -> `findByExplorePlanet(missionId, planetId)`:
/// units sitting on the planet with no mission, plus units DEPLOYED at the planet
/// belonging to some other mission. `hiddenUnitBo.defineHidden` /
/// `ObtainedUnitUtil.handleInvisible` are the M4 visibility pass (TODO).
async fn explore_planet_units(
    conn: &mut MySqlConnection,
    explore_mission_id: u64,
    planet_id: u64,
) -> OwgeResult<Vec<ObtainedUnitDto>> {
    let ids: Vec<u64> = sqlx::query_scalar(
        "SELECT ou.id FROM obtained_units ou \
         LEFT JOIN missions oum ON oum.id = ou.mission_id \
         LEFT JOIN mission_types mt_oum ON mt_oum.id = oum.type \
         WHERE (ou.mission_id IS NULL AND ou.source_planet = ?) \
            OR (ou.target_planet = ? AND ou.mission_id IS NOT NULL \
                AND ou.mission_id <> ? AND mt_oum.code = 'DEPLOYED') \
         ORDER BY ou.id",
    )
    .bind(planet_id)
    .bind(planet_id)
    .bind(explore_mission_id)
    .fetch_all(&mut *conn)
    .await?;
    // TODO(M3/M4): hiddenUnitBo.defineHidden + ObtainedUnitUtil.handleInvisible.
    let mut units = Vec::with_capacity(ids.len());
    for id in ids {
        if let Some(mut dto) = super::load_obtained_unit_dto(conn, id).await? {
            super::enrich_unit_for_report(conn, &mut dto).await?;
            units.push(dto);
        }
    }
    Ok(units)
}
