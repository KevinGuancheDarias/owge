//! Port of the mission **registration** pipeline
//! (`business/mission/unit/registration/`): `UnitMissionRegistrationBo` plus its
//! collaborators — `MissionRegistrationPreparer`,
//! `MissionRegistrationObtainedUnitLoader`, `MissionRegistrationUnitManager`,
//! `MissionRegistrationInvisibleManager`, `MissionRegistrationOrphanMissionEraser`,
//! `MissionRegistrationAuditor`, and the `checker/*` validators.
//!
//! The Java side spreads this across a dozen `@Service` beans; here it is one
//! file whose private `async fn`s mirror those beans, all running on the
//! caller's pinned, locked `&mut MySqlConnection` (the contract's preference for
//! mutation-in-a-locked-section work). The single public entry point is
//! [`UnitMissionRegistrationBo::do_common_mission_register`].
//!
//! ## What it does (parity with `doCommonMissionRegister`)
//! 1. Check the user exists; check DEPLOY is globally allowed.
//! 2. Load & validate the selected `obtained_units` from the source planet,
//!    subtracting the requested counts (deleting the stack when it hits zero),
//!    and mark orphaned DEPLOYED missions as resolved.
//! 3. Insert the `missions` row (base time from `MissionTimeManagerBo`).
//! 4. Create the per-mission `obtained_units` copies (carrier/stored nesting),
//!    pointing at the new mission.
//! 5. Check the unit *types* / speed groups can do this mission, and the
//!    cross-galaxy rule.
//! 6. Compute the speed-adjusted required time + termination date, apply any
//!    caller-requested custom duration, define mission invisibility, persist, and
//!    schedule the run.
//!
//! ## sqlx signedness (load-bearing)
//! `missions.id`/`obtained_units.id` = `u64`; `missions.source_planet`/
//! `target_planet` = **signed** `i64`; `missions.user_id` = signed `i32`;
//! `missions.type` = `smallint unsigned` (`u16`); `obtained_units.unit_id` =
//! `u16`, its `count` = `u64`, planet/mission/owner ids = `Option<u64>`. See the
//! `Mission` / `ObtainedUnit` models for the full map.

use sqlx::{Connection, MySqlConnection};

use crate::bo::realtime_emitter::RequirementEmit;
use crate::error::{OwgeError, OwgeResult};
use crate::lock::{planet_lock_key, user_lock_key};
use crate::model::mission::{Mission, MissionType};
use crate::model::obtained_unit::ObtainedUnit;
use crate::model::user_storage::UserStorage;
use crate::pojo::unit_mission_information::{SelectedUnit, UnitMissionInformation};

/// `MissionSchedulerService.DELAY_HANDLE` — fire 2s before the nominal end. Kept
/// in sync with `mission_scheduler_bo` so the inline schedule insert below
/// matches `schedule_mission` exactly.
const DELAY_HANDLE: i64 = 2;
/// `scheduled_tasks.task_name` for a mission row (see `mission_scheduler_bo`).
const MISSION_TASK_NAME: &str = "mission-run";

/// One selected obtained-unit stack after it has been loaded from the DB and
/// subtracted: the source `obtained_units` row that was decremented, the count
/// being sent, and the (recursively loaded) units it carries.
struct LoadedUnit {
    db_unit: ObtainedUnit,
    count: u64,
    stored: Vec<LoadedUnit>,
}

pub struct UnitMissionRegistrationBo;

impl UnitMissionRegistrationBo {
    /// `doCommonMissionRegister` — register one unit-based mission and return the
    /// persisted [`Mission`].
    ///
    /// Runs entirely on `conn`, which the caller must already hold the relevant
    /// planet (and, for unique flows, user) locks on. `is_deploy` toggles the
    /// extra deploy auditing (a deliberate no-op — auditing is dropped, disabled in live Java).
    ///
    /// All of the persistence (unit subtraction, mission insert, per-mission
    /// obtained-units, time/schedule) runs inside a single transaction opened on
    /// `conn`. The MySQL named locks the caller acquired with `GET_LOCK` are
    /// session-scoped and are NOT released by `BEGIN`/`COMMIT`, so they remain
    /// held across this transaction. If any step fails, the transaction is rolled
    /// back (the `Transaction` is dropped before `commit`), so a mid-registration
    /// failure can no longer leave an orphan mission without its scheduled task —
    /// matching the Java `@Transactional` boundary on `doCommonMissionRegister`.
    pub async fn do_common_mission_register(
        conn: &mut MySqlConnection,
        info: &UnitMissionInformation,
        mission_type: MissionType,
        user: &UserStorage,
        is_deploy: bool,
    ) -> OwgeResult<(Mission, Vec<RequirementEmit>)> {
        let mut tx = conn.begin().await?;
        // Requirement-trigger `*_unlocked_change` pushes from the unit subtraction
        // (`saveWithSubtraction` re-evaluates HAVE_UNIT / UNIT_AMOUNT relations),
        // returned to the caller to drain after commit + lock release.
        let mut req_emits = Vec::new();

        check_user_exists(&mut tx, user.id).await?;
        check_deploy_allowed(&mut tx, mission_type).await?;

        let source_planet_id = info.source_planet_id;
        let target_planet_id = info.target_planet_id;
        check_planet_exists(&mut tx, target_planet_id).await?;
        if let Some(source) = source_planet_id {
            check_planet_exists(&mut tx, source).await?;
        }

        // (2) load + subtract the selected units from the source planet.
        let loaded = load_and_subtract_units(&mut tx, info, mission_type, &mut req_emits).await?;

        // (3) insert the missions row (base time only, refined below).
        let mut mission = prepare_and_insert_mission(&mut tx, info, mission_type, user).await?;

        // isEnemyPlanet: source planet owned by somebody other than the invoker.
        let _is_enemy_planet = is_enemy_planet(&mut tx, user, mission.source_planet).await?;

        // NOT PORTED (deliberate): MissionRegistrationAuditor.auditMissionRegistration
        // (audit REGISTER_MISSION / USER_INTERACTION). Auditing is disabled in the
        // live Java deployment too, so it is an intentional no-op in the port.
        let _ = is_deploy;

        // (4) materialise the per-mission obtained_units (carrier + stored).
        let mission_units = manage_units_registration(&mut tx, &loaded, &mission).await?;

        // (5) unit-type / speed-group + cross-galaxy checks.
        check_units_can_do_mission(&mut tx, &mission_units, user, &mission, mission_type).await?;
        check_cross_galaxy(&mut tx, mission_type, &mission_units, &mission).await?;

        // (6) speed-adjusted time, custom duration, invisibility, persist, schedule.
        crate::bo::mission_time_manager_bo::MissionTimeManagerBo::handle_mission_time_calculation(
            &mut tx,
            &mission_units,
            &mut mission,
            mission_type,
        )
        .await?;
        crate::bo::mission_time_manager_bo::MissionTimeManagerBo::handle_custom_duration(
            &mut mission,
            info.wanted_time,
        );
        handle_define_mission_as_invisible(&mut tx, &mut mission, &mission_units).await?;
        persist_mission_after_time(&mut tx, &mission).await?;
        schedule_mission(&mut tx, mission.id, mission.required_time.unwrap_or(0.0)).await?;

        tx.commit().await?;

        // M4 emits (emitLocalMissionChangeAfterCommit + obtained-units + enemy-
        // missions refresh) are fired by the caller `UnitMissionBo::common_mission_register`
        // AFTER this tx commits and the lock is released — this helper runs on the
        // borrowed `conn` and cannot emit committed state.

        Ok((mission, req_emits))
    }
}

/// `MissionRegistrationUserExistsChecker.checkUserExists`.
async fn check_user_exists(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<()> {
    let exists: Option<i64> = sqlx::query_scalar("SELECT id FROM user_storage WHERE id = ?")
        .bind(user_id)
        .fetch_optional(&mut *conn)
        .await?;
    if exists.is_none() {
        return Err(OwgeError::NotFound(format!("No user with id {user_id}")));
    }
    Ok(())
}

/// `MissionRegistrationPlanetExistsChecker.checkPlanetExists`.
async fn check_planet_exists(conn: &mut MySqlConnection, planet_id: i64) -> OwgeResult<()> {
    let exists: Option<u64> = sqlx::query_scalar("SELECT id FROM planets WHERE id = ?")
        .bind(planet_id)
        .fetch_optional(&mut *conn)
        .await?;
    if exists.is_none() {
        return Err(OwgeError::NotFound(format!(
            "No such planet with id {planet_id}"
        )));
    }
    Ok(())
}

/// `DeployMissionConfigurationEnum` — the universe-wide DEPLOY policy stored in
/// the `DEPLOYMENT_CONFIG` configuration row.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
enum DeployMissionConfiguration {
    Freedom,
    OnlyOnceReturnDeployed,
    OnlyOnceReturnSource,
    Disallowed,
}

/// `ConfigurationBo.findDeployMissionConfiguration` — read `DEPLOYMENT_CONFIG`
/// (inserting the `FREEDOM` default when absent, like `findOrSetDefault`) and
/// parse it to the enum; any unparseable value logs a warning and defaults to
/// `FREEDOM`. Run on the registration connection so it sees the same transaction
/// as the rest of the pipeline (the pooled `ConfigurationBo` helper would use a
/// different connection).
async fn find_deploy_mission_configuration(
    conn: &mut MySqlConnection,
) -> OwgeResult<DeployMissionConfiguration> {
    let existing: Option<String> =
        sqlx::query_scalar("SELECT value FROM configuration WHERE name = ?")
            .bind("DEPLOYMENT_CONFIG")
            .fetch_optional(&mut *conn)
            .await?;
    let value = match existing {
        Some(v) => v,
        None => {
            sqlx::query("INSERT INTO configuration (name, value, privileged) VALUES (?, ?, 0)")
                .bind("DEPLOYMENT_CONFIG")
                .bind("FREEDOM")
                .execute(&mut *conn)
                .await?;
            "FREEDOM".to_string()
        }
    };
    Ok(match value.as_str() {
        "FREEDOM" => DeployMissionConfiguration::Freedom,
        "ONLY_ONCE_RETURN_DEPLOYED" => DeployMissionConfiguration::OnlyOnceReturnDeployed,
        "ONLY_ONCE_RETURN_SOURCE" => DeployMissionConfiguration::OnlyOnceReturnSource,
        "DISALLOWED" => DeployMissionConfiguration::Disallowed,
        other => {
            tracing::warn!(
                "Invalid value '{other}' for DEPLOYMENT_CONFIG, please check \
                 DeployMissionConfigurationEnum for valid values, Defaulting to FREEDOM"
            );
            DeployMissionConfiguration::Freedom
        }
    })
}

/// `MissionRegistrationCanDeployChecker.checkDeployedAllowed` — the *global*
/// DEPLOY toggle. `findDeployMissionConfiguration` reads `DEPLOYMENT_CONFIG`;
/// only the `DISALLOWED` value blocks registration here.
async fn check_deploy_allowed(
    conn: &mut MySqlConnection,
    mission_type: MissionType,
) -> OwgeResult<()> {
    if mission_type != MissionType::Deploy {
        return Ok(());
    }
    if find_deploy_mission_configuration(conn).await? == DeployMissionConfiguration::Disallowed {
        return Err(OwgeError::InvalidInput(
            "The deployment mission is globally disabled".to_string(),
        ));
    }
    Ok(())
}

/// `isOfUserProperty(userId, planetId)` — does this user own this planet?
async fn is_of_user_property(
    conn: &mut MySqlConnection,
    user_id: i32,
    planet_id: i64,
) -> OwgeResult<bool> {
    let owner: Option<Option<i32>> = sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
        .bind(planet_id)
        .fetch_optional(&mut *conn)
        .await?;
    Ok(matches!(owner, Some(Some(o)) if o == user_id))
}

/// `PlanetUtilService.isEnemyPlanet` — the planet has an owner that is not the
/// invoker. (Operates on the mission's source planet, matching the Java caller.)
async fn is_enemy_planet(
    conn: &mut MySqlConnection,
    user: &UserStorage,
    source_planet: Option<i64>,
) -> OwgeResult<bool> {
    let Some(planet_id) = source_planet else {
        return Ok(false);
    };
    let owner: Option<Option<i32>> = sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
        .bind(planet_id)
        .fetch_optional(&mut *conn)
        .await?;
    Ok(matches!(owner, Some(Some(o)) if o != user.id))
}

/// `MissionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits` — validate
/// no repeated units, load each selected stack from the source planet, subtract
/// the requested count (deleting at zero), recurse into stored/carried units,
/// enforce carrier weight, and resolve orphaned DEPLOYED missions.
async fn load_and_subtract_units(
    conn: &mut MySqlConnection,
    info: &UnitMissionInformation,
    mission_type: MissionType,
    req_emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<Vec<LoadedUnit>> {
    if info.involved_units.is_empty() {
        return Err(OwgeError::InvalidInput(
            "involvedUnits can't be empty".to_string(),
        ));
    }
    let user_id = info.user_id.ok_or_else(|| {
        OwgeError::InvalidInput("userId is required to register a mission".to_string())
    })?;
    let source_planet_id = info.source_planet_id.ok_or_else(|| {
        OwgeError::InvalidInput("sourcePlanetId is required to register a mission".to_string())
    })?;

    // (unitId, expirationId) uniqueness across the whole selection.
    let mut seen: Vec<(i32, Option<i64>)> = Vec::new();
    // Mission ids that lost their last unit (candidate orphan DEPLOYED missions).
    let mut emptied_deployed_missions: Vec<u64> = Vec::new();

    let is_deployed_source = !is_of_user_property(conn, user_id, source_planet_id).await?;

    let mut result = Vec::with_capacity(info.involved_units.len());
    for current in &info.involved_units {
        check_repeated_unit(&mut seen, current)?;
        let db_unit = handle_selected_unit(
            conn,
            user_id,
            source_planet_id,
            is_deployed_source,
            current,
            true,
            mission_type,
            info.target_planet_id,
            &mut emptied_deployed_missions,
            req_emits,
        )
        .await?;

        // Stored / carried units (recursive nesting, one level as the frontend
        // sends it). Each must be storable in the carrier and obeys the same load.
        let mut stored = Vec::new();
        if let Some(stored_units) = &current.stored_units {
            for stored_unit in stored_units {
                check_repeated_unit(&mut seen, stored_unit)?;
                check_can_store_unit(conn, current.id, stored_unit.id).await?;
                let stored_db = handle_selected_unit(
                    conn,
                    user_id,
                    source_planet_id,
                    is_deployed_source,
                    stored_unit,
                    false,
                    mission_type,
                    info.target_planet_id,
                    &mut emptied_deployed_missions,
                    req_emits,
                )
                .await?;
                stored.push(LoadedUnit {
                    db_unit: stored_db,
                    count: stored_unit.count as u64,
                    stored: Vec::new(),
                });
            }
        }

        check_total_weight(conn, &db_unit, current.count, &stored).await?;
        result.push(LoadedUnit {
            db_unit,
            count: current.count as u64,
            stored,
        });
    }

    mark_orphan_missions_resolved(conn, &emptied_deployed_missions).await?;
    Ok(result)
}

/// `checkRepeatedUnitAndAdd`.
fn check_repeated_unit(
    seen: &mut Vec<(i32, Option<i64>)>,
    selected: &SelectedUnit,
) -> OwgeResult<()> {
    let key = (selected.id, selected.expiration_id);
    if seen.contains(&key) {
        return Err(OwgeError::InvalidInput(
            "I18N_ERR_REPEATED_UNIT".to_string(),
        ));
    }
    seen.push(key);
    Ok(())
}

/// `MissionRegistrationObtainedUnitLoader.handleSelectedUnit` — find the source
/// obtained-unit stack (`findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission`),
/// optionally check it can deploy, then subtract the requested count
/// (`saveWithSubtraction`). When the subtraction empties a stack tied to a
/// DEPLOYED mission, that mission is queued for orphan resolution.
#[allow(clippy::too_many_arguments)]
async fn handle_selected_unit(
    conn: &mut MySqlConnection,
    user_id: i32,
    source_planet_id: i64,
    is_deployed_source: bool,
    selected: &SelectedUnit,
    check_can_deploy: bool,
    mission_type: MissionType,
    target_planet_id: i64,
    emptied_deployed_missions: &mut Vec<u64>,
    req_emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<ObtainedUnit> {
    if selected.count <= 0 {
        return Err(OwgeError::InvalidInput(format!(
            "No count was specified for unit {}",
            selected.id
        )));
    }
    let db_unit = find_source_obtained_unit(
        conn,
        user_id,
        selected,
        source_planet_id,
        is_deployed_source,
    )
    .await?;

    if check_can_deploy {
        // MissionRegistrationCanDeployChecker.checkUnitCanDeploy — block a
        // DEPLOY-after-DEPLOY when the deploy configuration is ONLY_ONCE_* and the
        // current obtained unit is on a DEPLOYED mission targeting a planet the
        // user doesn't own.
        check_unit_can_deploy(conn, user_id, target_planet_id, mission_type, &db_unit).await?;
    }

    let subtraction = selected.count as u64;
    if subtraction > db_unit.count {
        return Err(OwgeError::InvalidInput(
            "Can't not subtract because, obtainedUnit count is less than the amount to subtract"
                .to_string(),
        ));
    }
    if subtraction == db_unit.count {
        // saveWithSubtraction returns null → the stack is deleted.
        sqlx::query("DELETE FROM obtained_units WHERE id = ?")
            .bind(db_unit.id)
            .execute(&mut *conn)
            .await?;
        // saveWithSubtraction delete branch: triggerUnitBuildCompletedOrKilled is
        // fired AFTER the delete (so the re-count reflects the removed stack).
        let user = load_user_storage(conn, user_id).await?;
        crate::bo::requirement_bo::RequirementBo::trigger_unit_build_completed_or_killed(
            conn,
            &user,
            db_unit.unit_id as i64,
            req_emits,
        )
        .await?;
        if let Some(mission_id) = db_unit.mission_id {
            if mission_is_deployed(conn, mission_id).await? {
                emptied_deployed_missions.push(mission_id);
            }
        }
    } else {
        // saveWithSubtraction change branch: Java fires the trigger BEFORE
        // saveWithChange(-subtraction), so the re-count still sees the old (larger)
        // count at trigger time — replicated faithfully by triggering first.
        let user = load_user_storage(conn, user_id).await?;
        crate::bo::requirement_bo::RequirementBo::trigger_unit_build_completed_or_killed(
            conn,
            &user,
            db_unit.unit_id as i64,
            req_emits,
        )
        .await?;
        // saveWithChange(-subtraction): decrement the surviving stack in place.
        sqlx::query("UPDATE obtained_units SET count = count - ? WHERE id = ?")
            .bind(subtraction)
            .bind(db_unit.id)
            .execute(&mut *conn)
            .await?;
        // No-op in the Rust port: Java evicts a by-user `@TaggableCacheEvictByTag`
        // here because the `@Modifying` update bypasses entity listeners. That
        // generic cache is not replicated (Rust recomputes), and this is a unit
        // *split* (a new on-mission stack + decremented origin) so the user's total
        // unit count — and thus the moka improvement cache — is unchanged.
    }

    Ok(db_unit)
}

/// Load the full `UserStorage` on the caller's connection — needed to drive the
/// requirement-trigger engine (which reads `faction` / `home_planet`).
async fn load_user_storage(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<UserStorage> {
    sqlx::query_as::<_, UserStorage>(
        "SELECT id, username, email, alliance_id, faction, last_action, home_planet, \
                primary_resource, secondary_resource, energy, \
                primary_resource_generation_per_second, secondary_resource_generation_per_second, \
                has_skipped_tutorial, points, can_alter_twitch_state, banned \
         FROM user_storage WHERE id = ?",
    )
    .bind(user_id)
    .fetch_optional(&mut *conn)
    .await?
    .ok_or_else(|| OwgeError::NotFound(format!("No user_storage with id {user_id}")))
}

/// `ObtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission`.
/// `isDeployedSource` (== not the user's own planet) selects the target-planet /
/// DEPLOYED-mission variant; otherwise the source-planet / no-mission variant.
async fn find_source_obtained_unit(
    conn: &mut MySqlConnection,
    user_id: i32,
    selected: &SelectedUnit,
    planet_id: i64,
    is_deployed_source: bool,
) -> OwgeResult<ObtainedUnit> {
    const COLS: &str = "id, user_id, unit_id, count, source_planet, target_planet, \
                        mission_id, first_deployment_mission, is_from_capture, \
                        expiration_id, owner_unit_id";
    let unit_id = selected.id;
    let row: Option<ObtainedUnit> = match (is_deployed_source, selected.expiration_id) {
        (true, None) => {
            sqlx::query_as::<_, ObtainedUnit>(&format!(
                "SELECT {COLS} FROM obtained_units ou \
                 WHERE ou.user_id = ? AND ou.unit_id = ? AND ou.target_planet = ? \
                   AND ou.mission_id IN (SELECT m.id FROM missions m \
                        JOIN mission_types mt ON mt.id = m.type WHERE mt.code = 'DEPLOYED') \
                 LIMIT 1"
            ))
            .bind(user_id)
            .bind(unit_id)
            .bind(planet_id)
            .fetch_optional(&mut *conn)
            .await?
        }
        (false, None) => {
            sqlx::query_as::<_, ObtainedUnit>(&format!(
                "SELECT {COLS} FROM obtained_units ou \
                 WHERE ou.user_id = ? AND ou.unit_id = ? AND ou.source_planet = ? \
                   AND ou.expiration_id IS NULL AND ou.mission_id IS NULL \
                 LIMIT 1"
            ))
            .bind(user_id)
            .bind(unit_id)
            .bind(planet_id)
            .fetch_optional(&mut *conn)
            .await?
        }
        (true, Some(expiration_id)) => {
            sqlx::query_as::<_, ObtainedUnit>(&format!(
                "SELECT {COLS} FROM obtained_units ou \
                 WHERE ou.user_id = ? AND ou.unit_id = ? AND ou.target_planet = ? \
                   AND ou.expiration_id = ? \
                   AND ou.mission_id IN (SELECT m.id FROM missions m \
                        JOIN mission_types mt ON mt.id = m.type WHERE mt.code = 'DEPLOYED') \
                 LIMIT 1"
            ))
            .bind(user_id)
            .bind(unit_id)
            .bind(planet_id)
            .bind(expiration_id)
            .fetch_optional(&mut *conn)
            .await?
        }
        (false, Some(expiration_id)) => {
            sqlx::query_as::<_, ObtainedUnit>(&format!(
                "SELECT {COLS} FROM obtained_units ou \
                 WHERE ou.user_id = ? AND ou.unit_id = ? AND ou.source_planet = ? \
                   AND ou.expiration_id = ? AND ou.mission_id IS NULL \
                 LIMIT 1"
            ))
            .bind(user_id)
            .bind(unit_id)
            .bind(planet_id)
            .bind(expiration_id)
            .fetch_optional(&mut *conn)
            .await?
        }
    };
    row.ok_or_else(|| {
        OwgeError::NotFound(format!(
            "No obtainedUnit for unit with id {unit_id} was found in planet {planet_id}, \
             nice try, dirty hacker!"
        ))
    })
}

/// Is this mission a DEPLOYED mission? (used to detect orphaned deploy stacks).
async fn mission_is_deployed(conn: &mut MySqlConnection, mission_id: u64) -> OwgeResult<bool> {
    let code: Option<String> = sqlx::query_scalar(
        "SELECT mt.code FROM missions m JOIN mission_types mt ON mt.id = m.type WHERE m.id = ?",
    )
    .bind(mission_id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(code.as_deref() == Some(MissionType::Deployed.code()))
}

/// `MissionRegistrationCanDeployChecker.checkUnitCanDeploy` — when the universe
/// deploy policy is one of the `ONLY_ONCE_*` modes, you can't chain a DEPLOY
/// mission onto a unit that is currently on a DEPLOYED mission at a planet you
/// don't own (i.e. you must return/run another mission first).
async fn check_unit_can_deploy(
    conn: &mut MySqlConnection,
    user_id: i32,
    target_planet_id: i64,
    mission_type: MissionType,
    db_unit: &ObtainedUnit,
) -> OwgeResult<()> {
    // resolve(currentObtainedUnit.getMission()) == DEPLOYED ?
    let unit_mission_is_deployed = match db_unit.mission_id {
        Some(mission_id) => mission_is_deployed(conn, mission_id).await?,
        None => false,
    };
    let is_of_user_property = is_of_user_property(conn, user_id, target_planet_id).await?;
    let deploy_configuration = find_deploy_mission_configuration(conn).await?;
    let is_only_once = deploy_configuration == DeployMissionConfiguration::OnlyOnceReturnSource
        || deploy_configuration == DeployMissionConfiguration::OnlyOnceReturnDeployed;
    if is_only_once
        && !is_of_user_property
        && unit_mission_is_deployed
        && mission_type == MissionType::Deploy
    {
        return Err(OwgeError::InvalidInput(
            "You can't do a deploy mission after a deploy mission".to_string(),
        ));
    }
    Ok(())
}

/// `MissionRegistrationOrphanMissionEraser.doMarkAsDeletedTheOrphanMissions` —
/// mark each candidate mission resolved iff it no longer has any obtained units.
async fn mark_orphan_missions_resolved(
    conn: &mut MySqlConnection,
    candidate_mission_ids: &[u64],
) -> OwgeResult<()> {
    for &mission_id in candidate_mission_ids {
        let remaining: Option<u64> =
            sqlx::query_scalar("SELECT COUNT(*) FROM obtained_units WHERE mission_id = ?")
                .bind(mission_id)
                .fetch_one(&mut *conn)
                .await?;
        if remaining.unwrap_or(0) == 0 {
            sqlx::query("UPDATE missions SET resolved = 1 WHERE id = ?")
                .bind(mission_id)
                .execute(&mut *conn)
                .await?;
        }
    }
    Ok(())
}

/// `MissionRegistrationCanStoreUnitChecker.checkCanStoreUnit` — the carrier unit
/// must have a `UNIT_STORES_UNIT` rule allowing it to store the inner unit.
async fn check_can_store_unit(
    conn: &mut MySqlConnection,
    carrier_unit_id: i32,
    stored_unit_id: i32,
) -> OwgeResult<()> {
    // The rule lives in the generic `rules` table keyed by the
    // UnitStoresUnitRuleTypeProviderBo provider id, origin = carrier unit,
    // destination = stored unit.
    let exists: Option<i64> = sqlx::query_scalar(
        "SELECT id FROM rules \
         WHERE type = 'UNIT_STORES_UNIT' \
           AND origin_type = 'UNIT' AND destination_type = 'UNIT' \
           AND origin_id = ? AND destination_id = ? LIMIT 1",
    )
    .bind(carrier_unit_id)
    .bind(stored_unit_id)
    .fetch_optional(&mut *conn)
    .await?;
    if exists.is_none() {
        return Err(OwgeError::InvalidInput("I18N_CANT_STORE_UNIT".to_string()));
    }
    Ok(())
}

/// `MissionRegistrationObtainedUnitLoader.checkTotalHeight` — the carried weight
/// must not exceed the carrier's `storageCapacity * count`.
async fn check_total_weight(
    conn: &mut MySqlConnection,
    carrier: &ObtainedUnit,
    carrier_count: i64,
    stored: &[LoadedUnit],
) -> OwgeResult<()> {
    if stored.is_empty() {
        return Ok(());
    }
    let storage_capacity: Option<u32> =
        sqlx::query_scalar("SELECT storage_capacity FROM units WHERE id = ?")
            .bind(carrier.unit_id)
            .fetch_optional(&mut *conn)
            .await?
            .flatten();
    let max_supported = carrier_count.max(0) as i128 * storage_capacity.unwrap_or(0) as i128;

    let mut stored_weight: i128 = 0;
    for stored_unit in stored {
        let weight: Option<u32> =
            sqlx::query_scalar("SELECT stored_weight FROM units WHERE id = ?")
                .bind(stored_unit.db_unit.unit_id)
                .fetch_optional(&mut *conn)
                .await?;
        stored_weight += weight.unwrap_or(0) as i128 * stored_unit.count as i128;
    }
    if stored_weight > max_supported {
        return Err(OwgeError::InvalidInput(
            "I18N_ERR_MAX_WEIGHT_OVERPASSED".to_string(),
        ));
    }
    Ok(())
}

/// `MissionRegistrationPreparer.prepareMission` + `missionRepository.saveAndFlush`
/// — insert the `missions` row with the per-type base time. The speed-adjusted
/// time and termination date are refined later by `MissionTimeManagerBo`.
async fn prepare_and_insert_mission(
    conn: &mut MySqlConnection,
    info: &UnitMissionInformation,
    mission_type: MissionType,
    user: &UserStorage,
) -> OwgeResult<Mission> {
    let starting_date = chrono::Utc::now().naive_utc();
    // calculateRequiredTime — base time read on the pinned connection.
    let required_time = find_mission_base_time(conn, mission_type).await?;
    let termination_date =
        crate::bo::mission_time_manager_bo::MissionTimeManagerBo::compute_termination_date(
            required_time,
        );

    let result = sqlx::query(
        "INSERT INTO missions \
            (user_id, type, starting_date, required_time, termination_date, \
             source_planet, target_planet, attemps, resolved, invisible) \
         VALUES (?, ?, ?, ?, ?, ?, ?, 1, 0, 0)",
    )
    .bind(user.id)
    .bind(mission_type.value())
    .bind(starting_date)
    .bind(required_time)
    .bind(termination_date)
    .bind(info.source_planet_id)
    .bind(info.target_planet_id)
    .execute(&mut *conn)
    .await?;
    let mission_id = result.last_insert_id();

    Ok(Mission {
        id: mission_id,
        user_id: Some(user.id),
        type_id: mission_type.value(),
        termination_date: Some(termination_date),
        required_time: Some(required_time),
        starting_date,
        primary_resource: None,
        secondary_resource: None,
        required_energy: None,
        source_planet: info.source_planet_id,
        target_planet: Some(info.target_planet_id),
        related_mission: None,
        report_id: None,
        attemps: 1,
        resolved: 0,
        invisible: 0,
    })
}

/// `MissionConfigurationBo.findMissionBaseTimeByType` on the pinned connection
/// (mirror of `MissionTimeManagerBo::find_mission_base_time_conn`).
async fn find_mission_base_time(
    conn: &mut MySqlConnection,
    mission_type: MissionType,
) -> OwgeResult<f64> {
    let (key, default) = match mission_type {
        MissionType::Explore => ("MISSION_TIME_EXPLORE", "60"),
        MissionType::Gather => ("MISSION_TIME_GATHER", "900"),
        MissionType::EstablishBase => ("MISSION_TIME_ESTABLISH_BASE", "43200"),
        MissionType::Attack => ("MISSION_TIME_ATTACK", "600"),
        MissionType::Counterattack => ("MISSION_TIME_COUNTERATTACK", "60"),
        MissionType::Conquest => ("MISSION_TIME_CONQUEST", "86400"),
        MissionType::Deploy => ("MISSION_TIME_DEPLOY", "60"),
        other => {
            return Err(OwgeError::InvalidInput(format!(
                "Unsupported mission base time type, specified: {}",
                other.code()
            )));
        }
    };
    let existing: Option<String> =
        sqlx::query_scalar("SELECT value FROM configuration WHERE name = ?")
            .bind(key)
            .fetch_optional(&mut *conn)
            .await?;
    let value = match existing {
        Some(v) => v,
        None => {
            sqlx::query("INSERT INTO configuration (name, value, privileged) VALUES (?, ?, 0)")
                .bind(key)
                .bind(default)
                .execute(&mut *conn)
                .await?;
            default.to_string()
        }
    };
    Ok(value
        .trim()
        .parse::<i64>()
        .unwrap_or_else(|_| default.parse().unwrap()) as f64)
}

/// `MissionRegistrationUnitManager.manageUnitsRegistration` — create one
/// `obtained_units` row per selected stack (and per stored stack), pointing at
/// the new mission, and return them re-read as [`ObtainedUnit`] for the downstream
/// checks/time math.
async fn manage_units_registration(
    conn: &mut MySqlConnection,
    loaded: &[LoadedUnit],
    mission: &Mission,
) -> OwgeResult<Vec<ObtainedUnit>> {
    // Source/target as unsigned for the obtained_units columns (which are
    // `bigint unsigned`, unlike the signed missions.* FK columns).
    let source_planet = mission.source_planet.map(|p| p as u64);
    let target_planet = mission.target_planet.map(|p| p as u64);

    let mut result = Vec::new();
    for unit in loaded {
        let carrier_id = configure_obtained_unit(
            conn,
            unit.count,
            mission,
            &unit.db_unit,
            None,
            source_planet,
            target_planet,
        )
        .await?;
        result.push(read_obtained_unit(conn, carrier_id).await?);

        for stored in &unit.stored {
            let stored_id = configure_obtained_unit(
                conn,
                stored.count,
                mission,
                &stored.db_unit,
                Some(carrier_id),
                source_planet,
                target_planet,
            )
            .await?;
            result.push(read_obtained_unit(conn, stored_id).await?);
        }
    }
    Ok(result)
}

/// `MissionRegistrationUnitManager.configureObtainedUnit` — insert one
/// per-mission obtained-unit copy and return its new id.
#[allow(clippy::too_many_arguments)]
async fn configure_obtained_unit(
    conn: &mut MySqlConnection,
    count: u64,
    mission: &Mission,
    db_unit: &ObtainedUnit,
    owner_unit_id: Option<u64>,
    source_planet: Option<u64>,
    target_planet: Option<u64>,
) -> OwgeResult<u64> {
    let result = sqlx::query(
        "INSERT INTO obtained_units \
            (user_id, unit_id, count, mission_id, expiration_id, \
             source_planet, target_planet, owner_unit_id, is_from_capture) \
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, 0)",
    )
    .bind(db_unit.user_id)
    .bind(db_unit.unit_id)
    .bind(count)
    .bind(mission.id)
    .bind(db_unit.expiration_id)
    .bind(source_planet)
    .bind(target_planet)
    .bind(owner_unit_id)
    .execute(&mut *conn)
    .await?;
    Ok(result.last_insert_id())
}

async fn read_obtained_unit(conn: &mut MySqlConnection, id: u64) -> OwgeResult<ObtainedUnit> {
    Ok(sqlx::query_as::<_, ObtainedUnit>(
        "SELECT id, user_id, unit_id, count, source_planet, target_planet, \
                mission_id, first_deployment_mission, is_from_capture, \
                expiration_id, owner_unit_id \
         FROM obtained_units WHERE id = ?",
    )
    .bind(id)
    .fetch_one(&mut *conn)
    .await?)
}

/// `MissionRegistrationUnitTypeChecker.checkUnitsCanDoMission` — every involved
/// unit *type* (and each non-stored unit's speed-impact-group) must permit this
/// mission type against the target planet.
async fn check_units_can_do_mission(
    conn: &mut MySqlConnection,
    units: &[ObtainedUnit],
    _user: &UserStorage,
    _mission: &Mission,
    mission_type: MissionType,
) -> OwgeResult<()> {
    // The full Java check resolves UnitTypeBo.canDoMission + EntityCanDoMissionChecker
    // against the per-user/per-planet mission-support matrix
    // (`<missionType>_mission_support` columns on unit_types/speed_impact_groups,
    // plus requirement gating). Port the column-level guard here; the
    // requirement/per-planet refinements come with the requirement-engine
    // integration.
    let column = mission_support_column(mission_type);
    let Some(column) = column else {
        return Ok(());
    };

    for unit in units {
        // Unit type support.
        let type_supports: Option<Option<String>> = sqlx::query_scalar(&format!(
            "SELECT ut.{column} FROM units u \
             JOIN unit_types ut ON ut.id = u.type WHERE u.id = ?"
        ))
        .bind(unit.unit_id)
        .fetch_optional(&mut *conn)
        .await?;
        if matches!(type_supports, Some(Some(ref v)) if v == "NONE") {
            return Err(OwgeError::InvalidInput(
                "At least one unit type doesn't support the specified mission.... don't try it \
                 dear hacker, you can't defeat the system, but don't worry nobody can"
                    .to_string(),
            ));
        }

        // Speed-impact-group support (only for carrier/non-stored units).
        if unit.owner_unit_id.is_none() {
            let group_supports: Option<Option<String>> = sqlx::query_scalar(&format!(
                "SELECT sig.{column} FROM units u \
                 JOIN speed_impact_groups sig ON sig.id = u.speed_impact_group_id WHERE u.id = ?"
            ))
            .bind(unit.unit_id)
            .fetch_optional(&mut *conn)
            .await?;
            if matches!(group_supports, Some(Some(ref v)) if v == "NONE") {
                return Err(OwgeError::InvalidInput(
                    "At least one unit speed group doesn't support the specified mission"
                        .to_string(),
                ));
            }
        }
    }
    Ok(())
}

/// Maps a mission type to the `*_mission_support` column on `unit_types` /
/// `speed_impact_groups`. `None` for types without a per-type support gate.
fn mission_support_column(mission_type: MissionType) -> Option<&'static str> {
    Some(match mission_type {
        MissionType::Explore => "can_explore",
        MissionType::Gather => "can_gather",
        MissionType::EstablishBase => "can_establish_base",
        MissionType::Attack => "can_attack",
        MissionType::Counterattack => "can_counterattack",
        MissionType::Conquest => "can_conquest",
        MissionType::Deploy => "can_deploy",
        _ => return None,
    })
}

/// `CrossGalaxyMissionChecker.checkCrossGalaxy` — when the source and target are
/// in different galaxies, every involved unit must allow cross-galaxy travel.
async fn check_cross_galaxy(
    conn: &mut MySqlConnection,
    mission_type: MissionType,
    units: &[ObtainedUnit],
    mission: &Mission,
) -> OwgeResult<()> {
    let (Some(source), Some(target)) = (mission.source_planet, mission.target_planet) else {
        return Ok(());
    };
    let source_galaxy: Option<u16> =
        sqlx::query_scalar("SELECT galaxy_id FROM planets WHERE id = ?")
            .bind(source)
            .fetch_optional(&mut *conn)
            .await?;
    let target_galaxy: Option<u16> =
        sqlx::query_scalar("SELECT galaxy_id FROM planets WHERE id = ?")
            .bind(target)
            .fetch_optional(&mut *conn)
            .await?;
    if source_galaxy == target_galaxy {
        return Ok(());
    }
    // The invoking user (units.get(0).getUser()). All involved obtained_units share
    // the mission's user_id; resolve it once from the mission.
    let Some(user_id) = mission.user_id else {
        return Ok(());
    };
    // For each non-stored unit (ownerUnit == null): resolve the speed group
    // (unit override, else the unit type's group) and run doCheckSpeedImpactIfNotNull.
    for unit in units {
        if unit.owner_unit_id.is_some() {
            continue;
        }
        let speed_group_id = resolve_unit_speed_group(conn, unit.unit_id).await?;
        if let Some(speed_group_id) = speed_group_id {
            do_check_speed_impact(conn, speed_group_id, user_id, target, mission_type).await?;
        }
    }
    Ok(())
}

/// `unit.getSpeedImpactGroup() ?? unit.getType().getSpeedImpactGroup()` — the
/// unit's own speed-impact-group override, falling back to its type's group.
async fn resolve_unit_speed_group(
    conn: &mut MySqlConnection,
    unit_id: u16,
) -> OwgeResult<Option<u16>> {
    let unit_group: Option<u16> =
        sqlx::query_scalar("SELECT speed_impact_group_id FROM units WHERE id = ?")
            .bind(unit_id)
            .fetch_optional(&mut *conn)
            .await?
            .flatten();
    if unit_group.is_some() {
        return Ok(unit_group);
    }
    let type_group: Option<u16> = sqlx::query_scalar(
        "SELECT ut.speed_impact_group_id FROM units u \
         JOIN unit_types ut ON ut.id = u.type WHERE u.id = ?",
    )
    .bind(unit_id)
    .fetch_optional(&mut *conn)
    .await?
    .flatten();
    Ok(type_group)
}

/// `CrossGalaxyMissionChecker.doCheckSpeedImpactIfNotNull` — the speed group must
/// (a) support this mission type out of the galaxy (`canDoMission`) and (b) be
/// unlocked for the user.
async fn do_check_speed_impact(
    conn: &mut MySqlConnection,
    speed_group_id: u16,
    user_id: i32,
    target_planet_id: i64,
    mission_type: MissionType,
) -> OwgeResult<()> {
    // (a) EntityCanDoMissionChecker.canDoMission against the speed group.
    if !speed_group_can_do_mission(
        conn,
        speed_group_id,
        user_id,
        target_planet_id,
        mission_type,
    )
    .await?
    {
        return Err(OwgeError::InvalidInput(
            "This speed group doesn't support this mission outside of the galaxy".to_string(),
        ));
    }
    // (b) objectRelationBo.findOne(SPEED_IMPACT_GROUP, id) + unlockedRelationBo.isUnlocked.
    let relation_exists: Option<i64> = sqlx::query_scalar(
        "SELECT 1 FROM object_relations WHERE object_description = ? AND reference_id = ? LIMIT 1",
    )
    .bind(crate::model::object_relation::object_enum::SPEED_IMPACT_GROUP)
    .bind(speed_group_id as i16)
    .fetch_optional(&mut *conn)
    .await?;
    if relation_exists.is_none() {
        tracing::warn!(
            "Unexpected null objectRelation for SPEED_IMPACT_GROUP with id {speed_group_id}"
        );
        return Ok(());
    }
    // isUnlocked: the user's unlocked SPEED_IMPACT_GROUP reference ids must contain it.
    let unlocked: Option<i64> = sqlx::query_scalar(
        "SELECT 1 \
         FROM unlocked_relation ur \
         JOIN object_relations o ON o.id = ur.relation_id \
         WHERE ur.user_id = ? AND o.object_description = ? AND o.reference_id = ? LIMIT 1",
    )
    .bind(user_id)
    .bind(crate::model::object_relation::object_enum::SPEED_IMPACT_GROUP)
    .bind(speed_group_id as i16)
    .fetch_optional(&mut *conn)
    .await?;
    if unlocked.is_none() {
        return Err(OwgeError::InvalidInput(
            "Don't try it.... you can't do cross galaxy missions, and you know it".to_string(),
        ));
    }
    Ok(())
}

/// `EntityCanDoMissionChecker.canDoMission` for a `speed_impact_groups` row:
/// read the `can_<mission>` support enum — `ANY` => true, `OWNED_ONLY` => the
/// target planet is owned by the user, otherwise (`NONE`/null/unmapped) => false.
async fn speed_group_can_do_mission(
    conn: &mut MySqlConnection,
    speed_group_id: u16,
    user_id: i32,
    target_planet_id: i64,
    mission_type: MissionType,
) -> OwgeResult<bool> {
    let Some(column) = mission_support_column(mission_type) else {
        return Ok(false);
    };
    let support: Option<Option<String>> = sqlx::query_scalar(&format!(
        "SELECT {column} FROM speed_impact_groups WHERE id = ?"
    ))
    .bind(speed_group_id)
    .fetch_optional(&mut *conn)
    .await?;
    let support = support.flatten();
    Ok(match support.as_deref() {
        Some("ANY") => true,
        Some("OWNED_ONLY") => is_of_user_property(conn, user_id, target_planet_id).await?,
        _ => false,
    })
}

/// `MissionRegistrationInvisibleManager.handleDefineMissionAsInvisible` — the
/// mission is invisible iff every involved unit is a hidden/invisible unit.
async fn handle_define_mission_as_invisible(
    conn: &mut MySqlConnection,
    mission: &mut Mission,
    units: &[ObtainedUnit],
) -> OwgeResult<()> {
    if units.is_empty() {
        mission.invisible = 0;
        return Ok(());
    }
    // HiddenUnitBo.isHiddenUnit operates per the unit's *owning user* — all the
    // involved obtained_units share the mission's user_id.
    let owner_user_id = mission.user_id;
    let mut all_hidden = true;
    for unit in units {
        if !is_hidden_unit(conn, owner_user_id, unit.unit_id).await? {
            all_hidden = false;
            break;
        }
    }
    mission.invisible = if all_hidden { 1 } else { 0 };
    Ok(())
}

/// `HiddenUnitBo.isHiddenUnit` — the unit is hidden iff its static `is_invisible`
/// flag is set OR the owning user has an ACTIVE time special whose
/// `TIME_SPECIAL_IS_ENABLED_DO_HIDE` rules target this unit (or its type).
async fn is_hidden_unit(
    conn: &mut MySqlConnection,
    owner_user_id: Option<i32>,
    unit_id: u16,
) -> OwgeResult<bool> {
    let is_invisible: Option<i8> =
        sqlx::query_scalar("SELECT is_invisible FROM units WHERE id = ?")
            .bind(unit_id)
            .fetch_optional(&mut *conn)
            .await?;
    if is_invisible.unwrap_or(0) != 0 {
        return Ok(true);
    }
    // Only the rule branch needs the owning user; without one (defensive) it can't
    // have an active time special, so the unit is not hidden.
    let Some(user_id) = owner_user_id else {
        return Ok(false);
    };
    let unit_type_id: Option<u16> = sqlx::query_scalar("SELECT type FROM units WHERE id = ?")
        .bind(unit_id)
        .fetch_optional(&mut *conn)
        .await?
        .flatten();
    let Some(unit_type_id) = unit_type_id else {
        return Ok(false);
    };
    crate::bo::active_time_special_rule_finder_bo::ActiveTimeSpecialRuleFinderBo::exists_rule_matching_unit_destination(
        conn,
        user_id,
        unit_id as i64,
        unit_type_id,
        "TIME_SPECIAL_IS_ENABLED_DO_HIDE",
    )
    .await
}

/// `missionRepository.save(mission)` after the time/invisibility refinement —
/// persist the recomputed `required_time`, `termination_date` and `invisible`.
async fn persist_mission_after_time(
    conn: &mut MySqlConnection,
    mission: &Mission,
) -> OwgeResult<()> {
    sqlx::query(
        "UPDATE missions SET required_time = ?, termination_date = ?, invisible = ? WHERE id = ?",
    )
    .bind(mission.required_time)
    .bind(mission.termination_date)
    .bind(mission.invisible)
    .bind(mission.id)
    .execute(&mut *conn)
    .await?;
    Ok(())
}

/// `MissionSchedulerService.scheduleMission`, run inline on the registration
/// connection so the `scheduled_tasks` row commits atomically with the mission.
/// Identical SQL to `mission_scheduler_bo::MissionSchedulerService::schedule_mission`.
async fn schedule_mission(
    conn: &mut MySqlConnection,
    mission_id: u64,
    required_time_seconds: f64,
) -> OwgeResult<()> {
    let delay = required_time_seconds as i64 - DELAY_HANDLE;
    sqlx::query(
        "INSERT INTO scheduled_tasks \
             (task_name, task_instance, task_data, execution_time, picked, version) \
         VALUES (?, ?, NULL, DATE_ADD(NOW(6), INTERVAL ? SECOND), 0, 1) \
         ON DUPLICATE KEY UPDATE \
             execution_time = DATE_ADD(NOW(6), INTERVAL ? SECOND), \
             picked = 0, picked_by = NULL, last_heartbeat = NULL, \
             version = version + 1",
    )
    .bind(MISSION_TASK_NAME)
    .bind(mission_id.to_string())
    .bind(delay)
    .bind(delay)
    .execute(&mut *conn)
    .await?;
    Ok(())
}

/// Suppress an unused-import warning for the lock-key helpers, which the
/// orchestrator's caller uses to build the planet/user lock superset before
/// invoking [`UnitMissionRegistrationBo::do_common_mission_register`]. Re-exported
/// here so the registration module advertises the keys it expects to run under.
#[allow(dead_code)]
fn _lock_keys_doc(planet_id: u64, user_id: i32) -> (String, String) {
    (planet_lock_key(planet_id), user_lock_key(user_id))
}
