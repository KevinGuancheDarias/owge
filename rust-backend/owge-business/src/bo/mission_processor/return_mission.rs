//! Port of `ReturnMissionProcessor`
//! (`...business.mission.processor.ReturnMissionProcessor`).
//!
//! Lands every unit attached to the (already-finished outbound) mission back onto
//! its source planet. Return missions produce no report (Java returns `null`).

use sqlx::MySqlConnection;

use crate::builder::UnitMissionReportBuilder;
use crate::error::OwgeResult;
use crate::model::mission::Mission;
use crate::model::obtained_unit::ObtainedUnit;

pub async fn process(
    conn: &mut MySqlConnection,
    mission: &Mission,
    _involved_units: &[ObtainedUnit],
    emits: &mut Vec<super::DeferredEmit>,
) -> OwgeResult<Option<UnitMissionReportBuilder>> {
    let user_id = mission.user_id.unwrap_or_default();
    let source_planet_id = match mission.source_planet {
        Some(p) => p as u64,
        None => return Ok(None),
    };

    // obtainedUnitRepository.findByMissionId(mission) -> moveUnit each home.
    let units = find_units_involved(conn, mission.id).await?;
    let is_owned = super::is_planet_owned_by(conn, user_id, source_planet_id).await?;
    for ou in &units {
        if is_owned {
            super::move_unit_to_planet(conn, ou.id, user_id, source_planet_id).await?;
        } else {
            // Returning to a planet lost mid-flight: Java calls the same
            // moveUnit as everywhere else, whose non-owned branch parks the
            // stack under a DEPLOYED marker mission at the (now foreign)
            // planet — exactly like a foreign deploy (bdd scenario "Units
            // returning to a planet lost mid-flight park as a DEPLOYED
            // stack"; the old fallback here left the stack mission-less).
            super::deploy::move_unit_to_foreign_planet(conn, user_id, ou, source_planet_id)
                .await?;
        }
    }

    // mission.setResolved(true) — persisted here since the mission produces no report.
    sqlx::query("UPDATE missions SET resolved = 1 WHERE id = ?")
        .bind(mission.id)
        .execute(&mut *conn)
        .await?;

    // requirementBo.triggerUnitBuildCompletedOrKilled(user, unit) +
    // triggerUnitAmountChanged(user, unit) per returned unit. The former already
    // calls the latter, so one call per distinct unit re-evaluates HAVE_UNIT then
    // UNIT_AMOUNT (idempotently). Java gates the triggers on the source planet
    // still belonging to the user (the async block checks planetOwner == user);
    // a return parked on a lost planet triggers nothing.
    if is_owned {
        let user = crate::bo::mission_bo::load_user_storage(conn, user_id).await?;
        let mut seen: std::collections::HashSet<u16> = std::collections::HashSet::new();
        let mut req_emits = Vec::new();
        for ou in &units {
            if seen.insert(ou.unit_id) {
                crate::bo::requirement_bo::RequirementBo::trigger_unit_build_completed_or_killed(
                    conn,
                    &user,
                    ou.unit_id as i64,
                    &mut req_emits,
                )
                .await?;
            }
        }
        for req in req_emits {
            emits.push(super::DeferredEmit::Requirement(req));
        }
    }

    // TODO(M3/M4): missionEventEmitterBo.emitLocalMissionChangeAfterCommit,
    // obtainedUnitEventEmitter.emitObtainedUnits.
    Ok(None)
}

/// `obtainedUnitRepository.findByMissionId(missionId)`.
async fn find_units_involved(
    conn: &mut MySqlConnection,
    mission_id: u64,
) -> OwgeResult<Vec<ObtainedUnit>> {
    Ok(sqlx::query_as::<_, ObtainedUnit>(
        "SELECT id, user_id, unit_id, count, source_planet, target_planet, \
                mission_id, first_deployment_mission, is_from_capture, \
                expiration_id, owner_unit_id \
         FROM obtained_units WHERE mission_id = ?",
    )
    .bind(mission_id)
    .fetch_all(&mut *conn)
    .await?)
}
