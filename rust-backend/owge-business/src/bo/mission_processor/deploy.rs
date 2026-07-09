//! Port of `DeployMissionProcessor`
//! (`...business.mission.processor.DeployMissionProcessor`).
//!
//! Moves every unit involved in the deploy mission onto the target planet. When
//! the target is the user's own property the stacks simply land on it (merging
//! with any identical stack already there); otherwise they become a `DEPLOYED`
//! mission sitting at the foreign planet.
//!
//! Deploy produces no mission report (Java returns `null`).

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
    let target_planet_id = match mission.target_planet {
        Some(p) => p as u64,
        None => return Ok(None),
    };

    // missionUnitsFinderBo.findUnitsInvolved(missionId) -> moveUnit each to target.
    let units = find_units_involved(conn, mission.id).await?;
    let is_owned = super::is_planet_owned_by(conn, user_id, target_planet_id).await?;
    for ou in &units {
        if is_owned {
            super::move_unit_to_planet(conn, ou.id, user_id, target_planet_id).await?;
        } else {
            // moveUnit "not my planet" branch: the stack becomes part of a
            // persistent DEPLOYED mission at the foreign planet (merging into an
            // existing identical DEPLOYED stack when one is already there).
            move_unit_to_foreign_planet(conn, user_id, ou, target_planet_id).await?;
        }
    }

    // mission.setResolved(true) — persisted here since deploy produces no report
    // (the report-save path that normally flips `resolved` is not invoked).
    sqlx::query("UPDATE missions SET resolved = 1 WHERE id = ?")
        .bind(mission.id)
        .execute(&mut *conn)
        .await?;

    // requirementBo.triggerUnitBuildCompletedOrKilled(user, alteredUnits' units) —
    // re-evaluate HAVE_UNIT / UNIT_AMOUNT relations for each deployed unit.
    // Java gates this (and emitObtainedUnits) behind `user == targetPlanet.owner`:
    // foreign deploys park units under a DEPLOYED mission and trigger nothing.
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

    // TODO(M3/M4): set the deployed mission `invisible` when every involved unit is
    // hidden (hiddenUnitBo.isHiddenUnit), obtainedUnitEventEmitter.emitObtainedUnits,
    // missionEventEmitterBo emits.
    Ok(None)
}

/// `ObtainedUnitBo.moveUnit` non-owned branch + `saveWithAdding` +
/// `MissionFinderBo.findDeployedMissionOrCreate`. The stack joins a persistent
/// DEPLOYED mission (type 12) at the foreign planet: if an identical DEPLOYED
/// stack of the same unit/expiration is already stationed there it absorbs the
/// count (and this copy is deleted); otherwise the stack is attached to the
/// user's existing unresolved DEPLOYED mission for that planet, creating one when
/// none exists. `source_planet` (the origin) is preserved, mirroring Java.
async fn move_unit_to_foreign_planet(
    conn: &mut MySqlConnection,
    user_id: i32,
    ou: &ObtainedUnit,
    planet_id: u64,
) -> OwgeResult<()> {
    // saveWithAdding: an existing DEPLOYED stack of the same unit/expiration at the
    // planet absorbs this count.
    let existing: Option<u64> = match ou.expiration_id {
        None => {
            sqlx::query_scalar(
                "SELECT ou.id FROM obtained_units ou \
               JOIN missions m ON m.id = ou.mission_id \
               JOIN mission_types mt ON mt.id = m.type \
              WHERE ou.user_id = ? AND ou.unit_id = ? AND ou.target_planet = ? \
                AND mt.code = 'DEPLOYED' AND ou.expiration_id IS NULL AND ou.id <> ? LIMIT 1",
            )
            .bind(user_id)
            .bind(ou.unit_id)
            .bind(planet_id)
            .bind(ou.id)
            .fetch_optional(&mut *conn)
            .await?
        }
        Some(exp) => {
            sqlx::query_scalar(
                "SELECT ou.id FROM obtained_units ou \
               JOIN missions m ON m.id = ou.mission_id \
               JOIN mission_types mt ON mt.id = m.type \
              WHERE ou.user_id = ? AND ou.unit_id = ? AND ou.target_planet = ? \
                AND mt.code = 'DEPLOYED' AND ou.expiration_id = ? AND ou.id <> ? LIMIT 1",
            )
            .bind(user_id)
            .bind(ou.unit_id)
            .bind(planet_id)
            .bind(exp)
            .bind(ou.id)
            .fetch_optional(&mut *conn)
            .await?
        }
    };

    if let Some(existing_id) = existing {
        sqlx::query("UPDATE obtained_units SET count = count + ? WHERE id = ?")
            .bind(ou.count)
            .bind(existing_id)
            .execute(&mut *conn)
            .await?;
        sqlx::query("DELETE FROM obtained_units WHERE id = ?")
            .bind(ou.id)
            .execute(&mut *conn)
            .await?;
        return Ok(());
    }

    let deployed_mission_id =
        find_or_create_deployed_mission(conn, user_id, ou.source_planet, planet_id).await?;
    sqlx::query(
        "UPDATE obtained_units \
            SET target_planet = ?, mission_id = ?, owner_unit_id = NULL \
          WHERE id = ?",
    )
    .bind(planet_id)
    .bind(deployed_mission_id)
    .bind(ou.id)
    .execute(&mut *conn)
    .await?;
    Ok(())
}

/// `MissionFinderBo.findDeployedMissionOrCreate` — the user's unresolved DEPLOYED
/// (type 12) mission for `target_planet`, or a freshly inserted one (no
/// time/schedule — a DEPLOYED mission is the persistent "units stationed here"
/// marker, never fired). `pub(crate)` so the combat manager's unit-capture path
/// can station captured units the same way `moveUnit` does.
pub(crate) async fn find_or_create_deployed_mission(
    conn: &mut MySqlConnection,
    user_id: i32,
    source_planet: Option<u64>,
    target_planet: u64,
) -> OwgeResult<u64> {
    let existing: Option<u64> = sqlx::query_scalar(
        "SELECT m.id FROM missions m JOIN mission_types mt ON mt.id = m.type \
          WHERE m.user_id = ? AND mt.code = 'DEPLOYED' AND m.target_planet = ? \
            AND m.resolved = 0 LIMIT 1",
    )
    .bind(user_id)
    .bind(target_planet as i64)
    .fetch_optional(&mut *conn)
    .await?;
    if let Some(id) = existing {
        return Ok(id);
    }

    let type_id: u16 = sqlx::query_scalar("SELECT id FROM mission_types WHERE code = 'DEPLOYED'")
        .fetch_one(&mut *conn)
        .await?;
    let starting_date = chrono::Utc::now().naive_utc();
    let result = sqlx::query(
        "INSERT INTO missions \
            (user_id, type, starting_date, source_planet, target_planet, resolved, invisible) \
         VALUES (?, ?, ?, ?, ?, 0, 0)",
    )
    .bind(user_id)
    .bind(type_id)
    .bind(starting_date)
    .bind(source_planet.map(|p| p as i64))
    .bind(target_planet as i64)
    .execute(&mut *conn)
    .await?;
    Ok(result.last_insert_id())
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
