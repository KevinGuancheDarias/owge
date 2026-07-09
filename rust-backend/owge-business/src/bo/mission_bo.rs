//! Port of the `MissionBo.runMission` completion paths for the two non-unit
//! mission types the realization job dispatches here: **BUILD_UNIT** and
//! **LEVEL_UP** (`business/MissionBo.java` `runMission` ->
//! `processBuildUnit` / `processLevelUpAnUpgrade`).
//!
//! Unlike the unit-mission runner, these complete on the per-build/per-user
//! lock superset rather than the source/target planet pair. The dispatcher
//! ([`crate::bo::mission_runner`]) acquires the planet lock (for BUILD_UNIT, the
//! build planet from `mission_information.value`) before calling in, so the work
//! here runs on the caller's pinned, locked connection.
//!
//! ## Parity scope
//! - `processBuildUnit`: land every obtained unit attached to the mission onto
//!   the build planet (`source_planet = planet`, merge identical stacks), then
//!   delete the mission.
//! - `processLevelUpAnUpgrade`: bump the user's `obtained_upgrades.level` for the
//!   mission's target upgrade to the mission's `value`, then delete the mission.
//!
//! The websocket emissions, requirement re-triggers, and improvement-cache
//! clears are M4/requirement-engine concerns and are left as `// TODO(M3)`.
//!
//! ## sqlx signedness (load-bearing)
//! `mission_information.relation_id` = `smallint unsigned` (`u16`), `value` =
//! `double` (`f64`); `object_relations.reference_id` = **signed** `smallint`
//! (`i16`); `obtained_upgrades.level` = signed `smallint` (`i16`),
//! `obtained_upgrades.upgrade_id` = `smallint unsigned` (`u16`).

use sqlx::{Connection, MySqlConnection};

use crate::bo::configuration_bo::ConfigurationBo;
use crate::bo::emitter::unit_type_emitter::UnitTypeEmitter;
use crate::bo::mission_base_service_bo::MissionBaseService;
use crate::bo::object_relation_bo::ObjectRelationBo;
use crate::bo::realtime_emitter::RequirementEmit;
use crate::bo::unlocked_relation_bo::UnlockedRelationBo;
use crate::bo::upgrade_bo::UpgradeBo;
use crate::bo::user_improvement_bo::UserImprovementBo;
use crate::bo::user_storage_bo::UserStorageBo;
use crate::bo::{MissionEventEmitter, UserEventEmitter};
use crate::dto::mission::RunningUpgradeDto;
use crate::dto::user_improvement::{ImprovementType, UserImprovementDto};
use crate::error::{OwgeError, OwgeResult};
use crate::lock::{planet_lock_key, user_lock_key};
use crate::model::mission::{Mission, MissionType};
use crate::model::object_relation::object_enum;
use crate::model::obtained_unit::ObtainedUnit;

pub struct MissionBo;

impl MissionBo {
    /// `runMission(missionId, missionType)` — dispatch the BUILD_UNIT / LEVEL_UP
    /// completion. Runs on the caller's pinned, locked `conn`.
    pub async fn run_mission(
        conn: &mut MySqlConnection,
        mission_id: u64,
        mission_type: MissionType,
    ) -> OwgeResult<Vec<RequirementEmit>> {
        // One transaction on the pinned, already-locked connection so a mid-fire
        // failure rolls back atomically (see `do_run_unit_mission` for the same
        // rationale — the planet lock is session-scoped and survives BEGIN/COMMIT).
        let mut tx = conn.begin().await?;
        // Requirement-trigger `*_unlocked_change` pushes, drained by the caller
        // after this tx commits (BUILD_UNIT completion / LEVEL_UP completion can
        // unlock units, time specials, etc.).
        let mut req_emits = Vec::new();
        match mission_type {
            MissionType::BuildUnit => {
                Self::process_build_unit(&mut tx, mission_id, &mut req_emits).await?
            }
            MissionType::LevelUp => {
                Self::process_level_up_an_upgrade(&mut tx, mission_id, &mut req_emits).await?
            }
            _ => {
                tracing::warn!(
                    "MissionBo.run_mission called for {} which is not BUILD_UNIT/LEVEL_UP",
                    mission_type.code()
                );
            }
        }
        tx.commit().await?;
        Ok(req_emits)
    }

    /// `MissionBo.registerBuildUnit` — register a BUILD_UNIT mission on a planet
    /// the invoker owns. Serialized by an OUTER user lock + inner planet lock: the
    /// per-user lock closes the cross-planet unique-unit race (two concurrent
    /// builds of the same unique unit on different planets would each take a
    /// different planet lock and both pass `checkIsUniqueBuilt`). The whole
    /// validation + persistence then runs in one transaction so a rejected build
    /// leaves no partial state.
    pub async fn register_build_unit(
        conn: &mut MySqlConnection,
        user_id: i32,
        planet_id: u64,
        unit_id: u16,
        count: i64,
    ) -> OwgeResult<()> {
        // planetCheckerService.myCheckIsOfUserProperty.
        let owner: Option<Option<i32>> =
            sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
                .bind(planet_id)
                .fetch_optional(&mut *conn)
                .await?;
        if !matches!(owner, Some(Some(o)) if o == user_id) {
            return Err(OwgeError::InvalidInput(
                "The target planet doesn't belong to the invoker".to_string(),
            ));
        }

        let keys = vec![user_lock_key(user_id), planet_lock_key(planet_id)];
        crate::bo::unit_mission_bo::run_locked(&mut *conn, &keys, move |conn| {
            Box::pin(async move {
                do_register_build_unit(conn, user_id, planet_id, unit_id, count).await
            })
        })
        .await?;

        // M4 (Java MissionBo build-register tail): emitMissionCountChange +
        // emitUnitBuildChange + unitTypeBo.emitUserChange + emitUserData.
        MissionEventEmitter::emit_unit_build_change(&mut *conn, user_id).await?;
        MissionEventEmitter::emit_mission_count_change(&mut *conn, user_id).await?;
        UnitTypeEmitter::emit_unit_type_change(&mut *conn, user_id).await?;
        UserEventEmitter::emit_user_data(&mut *conn, user_id).await?;
        Ok(())
    }

    /// `MissionBo.cancelBuildUnit` → `cancelMission`. Cancel a BUILD_UNIT mission
    /// the invoker owns: delete the in-build units, refund the spent resources,
    /// then delete the mission and abort its scheduled completion. No planet/user
    /// lock (matching Java's plain `@Transactional`); one transaction so the refund
    /// and the deletes are atomic.
    pub async fn cancel_build_unit(
        conn: &mut MySqlConnection,
        user_id: i32,
        mission_id: u64,
    ) -> OwgeResult<()> {
        let mut tx = conn.begin().await?;
        let mission = load_mission(&mut tx, mission_id).await?.ok_or_else(|| {
            OwgeError::NotFound(
                "The mission was not found, or was not passed to cancelMission()".to_string(),
            )
        })?;
        // The mission must belong to the invoker.
        if mission.user_id != Some(user_id) {
            return Err(OwgeError::InvalidInput(
                "unexpected executed condition!, maybe some dirty Kenpachi tried to cancel \
                 mission of other player!"
                    .to_string(),
            ));
        }
        let primary = mission.primary_resource.unwrap_or(0.0);
        let secondary = mission.secondary_resource.unwrap_or(0.0);

        // BUILD_UNIT: `missionCancelBuildService.cancel` deletes the in-build units
        // (the resource refund below applies to every cancellable type).
        if mission.mission_type() == Some(MissionType::BuildUnit) {
            sqlx::query("DELETE FROM obtained_units WHERE mission_id = ?")
                .bind(mission_id)
                .execute(&mut *tx)
                .await?;
        }

        // addToPrimary / addToSecondary — refund the reserved cost.
        sqlx::query(
            "UPDATE user_storage \
                SET primary_resource = primary_resource + ?, \
                    secondary_resource = secondary_resource + ? \
              WHERE id = ?",
        )
        .bind(primary)
        .bind(secondary)
        .bind(user_id)
        .execute(&mut *tx)
        .await?;

        // missionRepository.delete(mission) — clear the mission_information FK first.
        sqlx::query("DELETE FROM mission_information WHERE mission_id = ?")
            .bind(mission_id)
            .execute(&mut *tx)
            .await?;
        sqlx::query("DELETE FROM missions WHERE id = ?")
            .bind(mission_id)
            .execute(&mut *tx)
            .await?;

        // abortMissionJob — drop the scheduled completion task.
        sqlx::query(
            "DELETE FROM scheduled_tasks WHERE task_name = 'mission-run' AND task_instance = ?",
        )
        .bind(mission_id.to_string())
        .execute(&mut *tx)
        .await?;

        tx.commit().await?;

        MissionEventEmitter::emit_unit_build_change(&mut *conn, user_id).await?;
        MissionEventEmitter::emit_mission_count_change(&mut *conn, user_id).await?;
        UnitTypeEmitter::emit_unit_type_change(&mut *conn, user_id).await?;
        Ok(())
    }

    /// `MissionBo.registerLevelUpAnUpgrade` — register a LEVEL_UP mission for the
    /// invoker. Serialized by a per-user lock: the upgrade-mission-not-running
    /// check is a per-user check-then-insert, so two concurrent requests would
    /// otherwise both pass it and leave the user with several running LEVEL_UP
    /// missions (which then breaks `findRunningLevelUpMission`). The whole
    /// validation + persistence runs in one transaction so a rejected level-up
    /// leaves no partial state. Direct sibling of [`Self::register_build_unit`].
    pub async fn register_level_up_an_upgrade(
        conn: &mut MySqlConnection,
        user_id: i32,
        upgrade_id: u16,
    ) -> OwgeResult<()> {
        let keys = vec![user_lock_key(user_id)];
        crate::bo::unit_mission_bo::run_locked(&mut *conn, &keys, move |conn| {
            Box::pin(async move { do_register_level_up(conn, user_id, upgrade_id).await })
        })
        .await?;

        // M4 (Java MissionBo.registerLevelUpAnUpgrade tail): emitRunningUpgrade +
        // emitMissionCountChange + emitUserData.
        MissionEventEmitter::emit_running_upgrade(&mut *conn, user_id).await?;
        MissionEventEmitter::emit_mission_count_change(&mut *conn, user_id).await?;
        UserEventEmitter::emit_user_data(&mut *conn, user_id).await?;
        Ok(())
    }

    /// `MissionBo.findRunningLevelUpMission(userId)` — the running LEVEL_UP
    /// mission for the user as a [`RunningUpgradeDto`], or `None`. Drives the
    /// `running_upgrade_change` sync payload and the `registerLevelUp` response.
    pub async fn find_running_level_up_mission(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Option<RunningUpgradeDto>> {
        let Some(mission) = load_level_up_mission(&mut *conn, user_id).await? else {
            return Ok(None);
        };
        let info = load_mission_information(&mut *conn, mission.id)
            .await?
            .ok_or_else(|| {
                OwgeError::Common(format!(
                    "Level-up mission {} has no mission_information",
                    mission.id
                ))
            })?;
        let relation_id = info.relation_id.ok_or_else(|| {
            OwgeError::Common(format!(
                "Level-up mission {} mission_information has no relation_id",
                mission.id
            ))
        })?;
        let upgrade_id = resolve_upgrade_id(&mut *conn, relation_id).await?;
        let upgrade = UpgradeBo::find_one(&mut *conn, upgrade_id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No upgrade with id {upgrade_id}")))?;
        // missionInformation.getValue().intValue() — the level being researched.
        let level = info.value.unwrap_or(0.0) as i32;
        Ok(Some(RunningUpgradeDto::from_mission(
            &mission, upgrade, level,
        )))
    }

    /// `MissionBo.cancelUpgradeMission` → `cancelMission`. Cancel the invoker's
    /// running LEVEL_UP mission: refund the spent resources, then delete the
    /// mission and abort its scheduled completion. No lock (matching Java's plain
    /// `@Transactional`); one transaction so the refund and deletes are atomic.
    /// Unlike BUILD_UNIT there are no in-build units to drop.
    pub async fn cancel_upgrade_mission(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<()> {
        let mut tx = conn.begin().await?;
        // findOneByUserIdAndTypeCode(LEVEL_UP) — the invoker's running level-up.
        // cancelMission throws MissionNotFoundException when there is none.
        let mission = load_level_up_mission(&mut tx, user_id)
            .await?
            .ok_or_else(|| {
                // same I18N message the Java side now throws (D5 fix)
                OwgeError::NotFound("I18N_ERR_GENERIC_ITEM_NOT_FOUND".to_string())
            })?;
        // Ownership is implicit — the mission was queried by the invoker's id.
        let primary = mission.primary_resource.unwrap_or(0.0);
        let secondary = mission.secondary_resource.unwrap_or(0.0);

        // addToPrimary / addToSecondary — refund the reserved cost.
        sqlx::query(
            "UPDATE user_storage \
                SET primary_resource = primary_resource + ?, \
                    secondary_resource = secondary_resource + ? \
              WHERE id = ?",
        )
        .bind(primary)
        .bind(secondary)
        .bind(user_id)
        .execute(&mut *tx)
        .await?;

        // missionRepository.delete(mission) — clear the mission_information FK first.
        sqlx::query("DELETE FROM mission_information WHERE mission_id = ?")
            .bind(mission.id)
            .execute(&mut *tx)
            .await?;
        sqlx::query("DELETE FROM missions WHERE id = ?")
            .bind(mission.id)
            .execute(&mut *tx)
            .await?;

        // abortMissionJob — drop the scheduled completion task.
        sqlx::query(
            "DELETE FROM scheduled_tasks WHERE task_name = 'mission-run' AND task_instance = ?",
        )
        .bind(mission.id.to_string())
        .execute(&mut *tx)
        .await?;

        tx.commit().await?;

        // M4 (Java MissionBo.cancelUpgradeMission): RUNNING_UPGRADE_CHANGE -> null,
        // plus cancelMission's emitUser after-commit block (unitTypeBo.emitUserChange
        // + emitMissionCountChange).
        MissionEventEmitter::emit_running_upgrade(&mut *conn, user_id).await?;
        UnitTypeEmitter::emit_unit_type_change(&mut *conn, user_id).await?;
        MissionEventEmitter::emit_mission_count_change(&mut *conn, user_id).await?;
        Ok(())
    }

    /// `missionRepository.countByUserIdAndResolvedFalse` — the unresolved-mission
    /// count the `registerLevelUp` response carries in `missionsCount`.
    pub async fn count_unresolved_missions(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<i32> {
        let count: i64 =
            sqlx::query_scalar("SELECT COUNT(*) FROM missions WHERE user_id = ? AND resolved = 0")
                .bind(user_id)
                .fetch_one(&mut *conn)
                .await?;
        Ok(count as i32)
    }

    /// `processBuildUnit` — land the freshly-built units on the build planet, then
    /// delete the mission.
    async fn process_build_unit(
        conn: &mut MySqlConnection,
        mission_id: u64,
        req_emits: &mut Vec<RequirementEmit>,
    ) -> OwgeResult<()> {
        let Some(mission) = load_mission(conn, mission_id).await? else {
            tracing::debug!("mission {mission_id} not found (build unit)");
            return Ok(());
        };
        let user_id = mission.user_id.ok_or_else(|| {
            OwgeError::Common(format!(
                "Build-unit mission {mission_id} has no owning user"
            ))
        })?;

        // mission.getMissionInformation().getValue().longValue() — the build planet.
        let source_planet_id = load_mission_information_value(conn, mission_id)
            .await?
            .map(|v| v as u64)
            .ok_or_else(|| {
                OwgeError::Common(format!(
                    "Build-unit mission {mission_id} has no mission_information.value (build planet)"
                ))
            })?;

        let units = load_mission_units(conn, mission_id).await?;
        for unit in &units {
            // current.setSourcePlanet(sourcePlanet); obtainedUnitBo.moveUnit(current, userId, sourcePlanetId)
            crate::bo::mission_processor::move_unit_to_planet(
                conn,
                unit.id,
                user_id,
                source_planet_id,
            )
            .await?;
            // requirementBo.triggerUnitBuildCompletedOrKilled(user, current.getUnit())
            // — requirement engine re-trigger on unit-build completion.
            let user = load_user_storage(conn, user_id).await?;
            crate::bo::requirement_bo::RequirementBo::trigger_unit_build_completed_or_killed(
                conn,
                &user,
                unit.unit_id as i64,
                req_emits,
            )
            .await?;
        }

        delete_mission(conn, mission_id).await?;

        // M4 emits + improvement-cache eviction (evict_and_emit, emitUnitBuildChange,
        // emitMissionCountChange, emitObtainedUnits) are fired by the caller
        // `UnitMissionBo::run_non_unit_mission` AFTER this tx commits — this helper
        // runs on the borrowed `conn` and cannot emit committed state.
        Ok(())
    }

    /// `processLevelUpAnUpgrade` — bump the obtained upgrade's level, then delete
    /// the mission.
    async fn process_level_up_an_upgrade(
        conn: &mut MySqlConnection,
        mission_id: u64,
        req_emits: &mut Vec<RequirementEmit>,
    ) -> OwgeResult<()> {
        let Some(mission) = load_mission(conn, mission_id).await? else {
            tracing::debug!("mission {mission_id} not found (level up)");
            return Ok(());
        };
        let user_id = mission.user_id.ok_or_else(|| {
            OwgeError::Common(format!("Level-up mission {mission_id} has no owning user"))
        })?;

        let info = load_mission_information(conn, mission_id)
            .await?
            .ok_or_else(|| {
                OwgeError::Common(format!(
                    "Level-up mission {mission_id} has no mission_information"
                ))
            })?;
        // objectRelationBo.unboxObjectRelation(relation) — for an UPGRADE relation the
        // referenced entity id is object_relations.reference_id.
        let relation_id = info.relation_id.ok_or_else(|| {
            OwgeError::Common(format!(
                "Level-up mission {mission_id} mission_information has no relation_id"
            ))
        })?;
        let upgrade_id = resolve_upgrade_id(conn, relation_id).await?;
        // missionInformation.getValue().intValue() — the target level.
        let target_level = info.value.unwrap_or(0.0) as i16;

        // obtainedUpgrade.setLevel(level); obtainedUpgradeRepository.save(...)
        let updated = sqlx::query(
            "UPDATE obtained_upgrades SET level = ? WHERE user_id = ? AND upgrade_id = ?",
        )
        .bind(target_level)
        .bind(user_id)
        .bind(upgrade_id)
        .execute(&mut *conn)
        .await?;
        if updated.rows_affected() == 0 {
            return Err(OwgeError::NotFound(format!(
                "No obtained_upgrade for user {user_id} and upgrade {upgrade_id}"
            )));
        }

        // requirementBo.triggerLevelUpCompleted(user, upgrade.getId())
        // — requirement engine re-trigger on level-up completion.
        let user = load_user_storage(conn, user_id).await?;
        crate::bo::requirement_bo::RequirementBo::trigger_level_up_completed(
            conn,
            &user,
            upgrade_id as i64,
            req_emits,
        )
        .await?;
        delete_mission(conn, mission_id).await?;

        // M4 emits + improvement-cache eviction (evict_and_emit, RUNNING_UPGRADE_CHANGE,
        // emitObtainedUpgrades, emitMissionCountChange) are fired by the caller
        // `UnitMissionBo::run_non_unit_mission` AFTER this tx commits — this helper
        // runs on the borrowed `conn` and cannot emit committed state.
        Ok(())
    }
}

/// `object_relations.reference_id` for an UPGRADE relation = the upgrade id.
async fn resolve_upgrade_id(conn: &mut MySqlConnection, relation_id: u16) -> OwgeResult<u16> {
    // reference_id is a *signed* smallint; an upgrade id is a non-negative
    // smallint unsigned, so widen and re-narrow.
    let reference_id: Option<i16> =
        sqlx::query_scalar("SELECT reference_id FROM object_relations WHERE id = ?")
            .bind(relation_id)
            .fetch_optional(&mut *conn)
            .await?;
    let reference_id = reference_id
        .ok_or_else(|| OwgeError::NotFound(format!("No object_relation with id {relation_id}")))?;
    Ok(reference_id as u16)
}

/// One `mission_information` row (relation + value) by mission id.
struct MissionInformationRow {
    relation_id: Option<u16>,
    value: Option<f64>,
}

async fn load_mission_information(
    conn: &mut MySqlConnection,
    mission_id: u64,
) -> OwgeResult<Option<MissionInformationRow>> {
    let row: Option<(Option<u16>, Option<f64>)> =
        sqlx::query_as("SELECT relation_id, value FROM mission_information WHERE mission_id = ?")
            .bind(mission_id)
            .fetch_optional(&mut *conn)
            .await?;
    Ok(row.map(|(relation_id, value)| MissionInformationRow { relation_id, value }))
}

async fn load_mission_information_value(
    conn: &mut MySqlConnection,
    mission_id: u64,
) -> OwgeResult<Option<f64>> {
    Ok(load_mission_information(conn, mission_id)
        .await?
        .and_then(|info| info.value))
}

async fn load_mission_units(
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

/// Load the full `UserStorage` on the caller's connection — needed to drive the
/// requirement-trigger engine (which reads `faction` / `home_planet`).
pub(crate) async fn load_user_storage(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<crate::model::UserStorage> {
    sqlx::query_as::<_, crate::model::UserStorage>(
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

async fn load_mission(conn: &mut MySqlConnection, mission_id: u64) -> OwgeResult<Option<Mission>> {
    Ok(
        sqlx::query_as::<_, Mission>(crate::bo::mission_base_service_bo::SELECT_MISSION)
            .bind(mission_id)
            .fetch_optional(&mut *conn)
            .await?,
    )
}

async fn delete_mission(conn: &mut MySqlConnection, mission_id: u64) -> OwgeResult<()> {
    sqlx::query("DELETE FROM mission_information WHERE mission_id = ?")
        .bind(mission_id)
        .execute(&mut *conn)
        .await?;
    sqlx::query("DELETE FROM missions WHERE id = ?")
        .bind(mission_id)
        .execute(&mut *conn)
        .await?;
    Ok(())
}

// --- BUILD_UNIT registration (MissionBo.registerBuildUnit) -------------------

/// Unit cost scalars read for `calculateRequirements` / the build mission.
struct UnitCost {
    primary_resource: u32,
    secondary_resource: u32,
    time: i32,
    energy: Option<u16>,
    is_unique: bool,
    type_id: u16,
}

/// `doInsideLock(...)` body: validate the build, deduct resources, create the
/// BUILD_UNIT mission + its `mission_information`, attach the obtained unit, and
/// schedule completion — all in one transaction on the pinned, locked connection.
async fn do_register_build_unit(
    conn: &mut MySqlConnection,
    user_id: i32,
    planet_id: u64,
    unit_id: u16,
    count: i64,
) -> OwgeResult<()> {
    let mut tx = conn.begin().await?;

    // checkUnitBuildMissionDoesNotExists — only one running build per planet.
    let existing: Option<u64> = sqlx::query_scalar(
        "SELECT m.id FROM missions m \
           JOIN mission_information mi ON mi.mission_id = m.id \
          WHERE m.user_id = ? AND m.type = ? AND mi.value = ? AND m.resolved = 0 LIMIT 1",
    )
    .bind(user_id)
    .bind(MissionType::BuildUnit.value())
    .bind(planet_id as f64)
    .fetch_optional(&mut *tx)
    .await?;
    if existing.is_some() {
        return Err(OwgeError::InvalidInput(
            "I18N_ERR_BUILD_MISSION_ALREADY_PRESENT".to_string(),
        ));
    }

    // objectRelationBo.findOne(UNIT, unitId) + checkIsUnlocked.
    let relation_id = ObjectRelationBo::find_one(&mut tx, object_enum::UNIT, unit_id as i16)
        .await?
        .ok_or_else(|| OwgeError::NotFound(format!("No object relation for unit {unit_id}")))?;
    let unlocked =
        UnlockedRelationBo::find_unlocked_reference_ids(&mut tx, user_id, object_enum::UNIT)
            .await?;
    if !unlocked.contains(&(unit_id as i16)) {
        return Err(OwgeError::InvalidInput(
            "The specified unit is not unlocked for the invoker".to_string(),
        ));
    }

    let user = UserStorageBo::find_by_id(&mut tx, user_id)
        .await?
        .ok_or_else(|| OwgeError::NotFound(format!("No user with id {user_id}")))?;
    MissionBaseService::check_mission_limit_not_reached(&mut tx, user_id).await?;

    let unit = load_unit_cost(&mut tx, unit_id).await?;
    let final_count = if unit.is_unique { 1 } else { count };
    if final_count < 1 {
        return Err(OwgeError::InvalidInput(
            "Input can't be negative".to_string(),
        ));
    }

    // checkIsUniqueBuilt.
    if unit.is_unique {
        let owned: i64 = sqlx::query_scalar(
            "SELECT CAST(COALESCE(SUM(count), 0) AS SIGNED) FROM obtained_units \
              WHERE user_id = ? AND unit_id = ?",
        )
        .bind(user_id)
        .bind(unit_id)
        .fetch_one(&mut *tx)
        .await?;
        if owned > 0 {
            return Err(OwgeError::InvalidInput(format!(
                "Unit with id {unit_id} has been already build by user {user_id}"
            )));
        }
    }

    // calculateRequirements(unit, finalCount).
    let required_primary = unit.primary_resource as f64 * final_count as f64;
    let required_secondary = unit.secondary_resource as f64 * final_count as f64;
    let mut required_time = unit.time as f64 * final_count as f64;
    let required_energy = unit.energy.unwrap_or(0) as f64 * final_count as f64;

    // canRun(user): enough primary/secondary resources and energy headroom.
    let improvement = UserImprovementBo::find_user_improvement(&mut tx, user_id).await?;
    let primary = user.primary_resource.unwrap_or(0.0);
    let secondary = user.secondary_resource.unwrap_or(0.0);
    let available_energy = find_available_energy(
        &mut tx,
        user.faction,
        user_id,
        improvement.more_energy_production,
    )
    .await?;
    let can_run = primary >= required_primary
        && secondary >= required_secondary
        && (required_energy == 0.0 || available_energy >= required_energy);
    if !can_run {
        return Err(OwgeError::InvalidInput("No enough resources!".to_string()));
    }

    // moreUnitBuildSpeed reduces the build time (computeImprovementValue sum=false).
    required_time = compute_improvement_value(
        &mut tx,
        required_time,
        improvement.more_unit_build_speed,
        false,
    )
    .await?;

    // checkWouldReachUnitTypeLimit.
    check_would_reach_unit_type_limit(&mut tx, &user, unit.type_id, final_count, &improvement)
        .await?;

    // ZERO_BUILD_TIME (default TRUE) collapses the build time to 3s.
    if ConfigurationBo::find_or_set_default(&mut tx, "ZERO_BUILD_TIME", "TRUE")
        .await?
        .value
        == "TRUE"
    {
        required_time = 3.0;
    }

    // Create the mission + its mission_information (relation + build planet).
    let starting_date = chrono::Utc::now().naive_utc();
    let termination_date =
        crate::bo::mission_time_manager_bo::MissionTimeManagerBo::compute_termination_date(
            required_time,
        );
    let mission_result = sqlx::query(
        "INSERT INTO missions \
            (user_id, type, starting_date, required_time, termination_date, \
             primary_resource, secondary_resource, resolved, invisible) \
         VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0)",
    )
    .bind(user_id)
    .bind(MissionType::BuildUnit.value())
    .bind(starting_date)
    .bind(required_time)
    .bind(termination_date)
    .bind(required_primary)
    .bind(required_secondary)
    .execute(&mut *tx)
    .await?;
    let mission_id = mission_result.last_insert_id();

    sqlx::query(
        "INSERT INTO mission_information (mission_id, relation_id, value) VALUES (?, ?, ?)",
    )
    .bind(mission_id)
    .bind(relation_id)
    .bind(planet_id as f64)
    .execute(&mut *tx)
    .await?;

    // substractResources(user, mission).
    sqlx::query(
        "UPDATE user_storage \
            SET primary_resource = primary_resource - ?, \
                secondary_resource = secondary_resource - ? \
          WHERE id = ?",
    )
    .bind(required_primary)
    .bind(required_secondary)
    .bind(user_id)
    .execute(&mut *tx)
    .await?;

    // The obtained unit "in build" — attached to the mission, no planet until the
    // build completes (processBuildUnit lands it on the planet).
    sqlx::query(
        "INSERT INTO obtained_units (user_id, unit_id, count, mission_id, is_from_capture) \
         VALUES (?, ?, ?, ?, 0)",
    )
    .bind(user_id)
    .bind(unit_id)
    .bind(final_count)
    .bind(mission_id)
    .execute(&mut *tx)
    .await?;

    schedule_build_mission(&mut tx, mission_id, required_time).await?;

    tx.commit().await?;
    Ok(())
}

async fn load_unit_cost(conn: &mut MySqlConnection, unit_id: u16) -> OwgeResult<UnitCost> {
    let row: Option<(u32, u32, i32, Option<u16>, u8, u16)> = sqlx::query_as(
        "SELECT primary_resource, secondary_resource, time, energy, is_unique, type \
           FROM units WHERE id = ?",
    )
    .bind(unit_id)
    .fetch_optional(&mut *conn)
    .await?;
    let (primary_resource, secondary_resource, time, energy, is_unique, type_id) =
        row.ok_or_else(|| OwgeError::NotFound(format!("No unit with id {unit_id}")))?;
    Ok(UnitCost {
        primary_resource,
        secondary_resource,
        time,
        energy,
        is_unique: is_unique != 0,
        type_id,
    })
}

/// `UserEnergyServiceBo.findAvailableEnergy` = maxEnergy − consumedEnergy.
/// maxEnergy = faction.initialEnergy boosted by the `moreEnergyProduction`
/// improvement; consumedEnergy = Σ(count × unit.energy) over the user's units.
async fn find_available_energy(
    conn: &mut MySqlConnection,
    faction_id: u16,
    user_id: i32,
    more_energy_production: f64,
) -> OwgeResult<f64> {
    let initial_energy: Option<u16> =
        sqlx::query_scalar("SELECT initial_energy FROM factions WHERE id = ?")
            .bind(faction_id)
            .fetch_optional(&mut *conn)
            .await?;
    let max_energy = compute_improvement_value(
        &mut *conn,
        initial_energy.unwrap_or(0) as f64,
        more_energy_production,
        true,
    )
    .await?;
    // Σ(count × energy) is integer-valued; CAST so sqlx decodes i64 (SUM → DECIMAL).
    let consumed: i64 = sqlx::query_scalar(
        "SELECT CAST(COALESCE(SUM(ou.count * u.energy), 0) AS SIGNED) \
           FROM obtained_units ou JOIN units u ON u.id = ou.unit_id WHERE ou.user_id = ?",
    )
    .bind(user_id)
    .fetch_one(&mut *conn)
    .await?;
    Ok(max_energy - consumed as f64)
}

/// `ImprovementBo.computeImprovementValue(base, percentage, sum)` — apply the
/// percentage in `IMPROVEMENT_STEP`-sized increments (default 10), summing or
/// subtracting each step off the running value.
pub(crate) async fn compute_improvement_value(
    conn: &mut MySqlConnection,
    base: f64,
    input_percentage: f64,
    sum: bool,
) -> OwgeResult<f64> {
    let mut ret_val = base;
    let mut step: f64 = ConfigurationBo::find_or_set_default(&mut *conn, "IMPROVEMENT_STEP", "10")
        .await?
        .value
        .trim()
        .parse()
        .unwrap_or(10.0);
    let mut pending = input_percentage;
    while pending > 0.0 {
        if pending < step {
            step = pending;
        }
        let current = ret_val * (step / 100.0);
        if sum {
            ret_val += current;
        } else {
            ret_val -= current;
        }
        pending -= step;
    }
    Ok(ret_val)
}

/// `UnitTypeBo.checkWouldReachUnitTypeLimit` — reject when the user would exceed
/// the (faction-overridable, improvement-boosted) max count of the unit type.
///
/// Parity note: the share-count root is followed via `share_max_count`, and the
/// user's count is `countUnitsByUserAndUnitType` = units of the root type PLUS
/// units of any type whose `share_max_count` points at the root (the Java
/// `countByUserAndUnitType` + `countByUserAndSharedCountUnitType` sum).
async fn check_would_reach_unit_type_limit(
    conn: &mut MySqlConnection,
    user: &crate::model::UserStorage,
    type_id: u16,
    count: i64,
    improvement: &UserImprovementDto,
) -> OwgeResult<()> {
    let root_type = find_max_share_count_root(conn, type_id).await?;

    let faction_max: Option<u32> = sqlx::query_scalar(
        "SELECT max_count FROM factions_unit_types WHERE faction_id = ? AND unit_type_id = ?",
    )
    .bind(user.faction)
    .bind(root_type)
    .fetch_optional(&mut *conn)
    .await?
    .flatten();
    // unit_types.max_count is BIGINT (signed); factions_unit_types.max_count is INT UNSIGNED.
    let type_max: Option<i64> = sqlx::query_scalar("SELECT max_count FROM unit_types WHERE id = ?")
        .bind(root_type)
        .fetch_optional(&mut *conn)
        .await?
        .flatten();

    // hasMaxCount: a faction override > 0, or the type's own max_count > 0.
    let effective_max: Option<i64> = match faction_max {
        Some(m) if m > 0 => Some(m as i64),
        _ => type_max.filter(|&m| m > 0),
    };
    let Some(max) = effective_max else {
        return Ok(()); // no limit configured for this type
    };

    let owned: i64 = sqlx::query_scalar(
        "SELECT CAST(COALESCE(SUM(ou.count), 0) AS SIGNED) FROM obtained_units ou \
           JOIN units u ON u.id = ou.unit_id \
           JOIN unit_types ut ON ut.id = u.type \
          WHERE ou.user_id = ? AND (ut.id = ? OR ut.share_max_count = ?)",
    )
    .bind(user.id)
    .bind(root_type)
    .bind(root_type)
    .fetch_one(&mut *conn)
    .await?;
    let user_count = owned + count;

    let amount_improvement =
        improvement.find_unit_type_improvement(ImprovementType::Amount, root_type);
    let limit = compute_improvement_value(&mut *conn, max as f64, amount_improvement, true)
        .await?
        .floor() as i64;
    if user_count > limit {
        return Err(OwgeError::InvalidInput(
            "Nice try to buy over your possibilities!!!, try outside of Spain!".to_string(),
        ));
    }
    Ok(())
}

/// `UnitTypeBo.findMaxShareCountRoot` — follow the `share_max_count` chain to the
/// type whose count limit actually applies.
async fn find_max_share_count_root(conn: &mut MySqlConnection, type_id: u16) -> OwgeResult<u16> {
    let mut current = type_id;
    // Bounded walk (defensive against a cyclic share chain).
    for _ in 0..32 {
        let parent: Option<u16> =
            sqlx::query_scalar("SELECT share_max_count FROM unit_types WHERE id = ?")
                .bind(current)
                .fetch_optional(&mut *conn)
                .await?
                .flatten();
        match parent {
            Some(p) => current = p,
            None => break,
        }
    }
    Ok(current)
}

/// Inline the `scheduled_tasks` INSERT for the build mission (atomic with it),
/// mirroring the unit-registration scheduler: fire `required_time − DELAY_HANDLE`
/// seconds out.
async fn schedule_build_mission(
    conn: &mut MySqlConnection,
    mission_id: u64,
    required_time_seconds: f64,
) -> OwgeResult<()> {
    const DELAY_HANDLE: i64 = 2;
    let delay = required_time_seconds as i64 - DELAY_HANDLE;
    sqlx::query(
        "INSERT INTO scheduled_tasks \
             (task_name, task_instance, task_data, execution_time, picked, version) \
         VALUES ('mission-run', ?, NULL, DATE_ADD(NOW(6), INTERVAL ? SECOND), 0, 1) \
         ON DUPLICATE KEY UPDATE \
             execution_time = DATE_ADD(NOW(6), INTERVAL ? SECOND), \
             picked = 0, picked_by = NULL, last_heartbeat = NULL, version = version + 1",
    )
    .bind(mission_id.to_string())
    .bind(delay)
    .bind(delay)
    .execute(&mut *conn)
    .await?;
    Ok(())
}

// --- LEVEL_UP registration (MissionBo.registerLevelUpAnUpgrade) --------------

/// `missions` columns for the [`Mission`] model — the `SELECT_MISSION` column
/// list, but keyed by `user_id` + `type` (the running LEVEL_UP mission lookup,
/// `findOneByUserIdAndTypeCode`). There is at most one because registration
/// serializes on the user lock and completion/give-up delete the mission.
async fn load_level_up_mission(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<Option<Mission>> {
    Ok(sqlx::query_as::<_, Mission>(
        "SELECT id, user_id, type, termination_date, required_time, starting_date, \
                primary_resource, secondary_resource, required_energy, \
                source_planet, target_planet, related_mission, report_id, \
                attemps, resolved, invisible \
         FROM missions WHERE user_id = ? AND type = ? LIMIT 1",
    )
    .bind(user_id)
    .bind(MissionType::LevelUp.value())
    .fetch_optional(&mut *conn)
    .await?)
}

/// Upgrade cost scalars read for `calculateRequirementsAreMet`.
struct UpgradeCost {
    primary_resource: i32,
    secondary_resource: i32,
    time: i32,
    level_effect: f32,
}

async fn load_upgrade_cost(conn: &mut MySqlConnection, upgrade_id: u16) -> OwgeResult<UpgradeCost> {
    let row: Option<(i32, i32, i32, f32)> = sqlx::query_as(
        "SELECT primary_resource, secondary_resource, time, level_effect FROM upgrades WHERE id = ?",
    )
    .bind(upgrade_id)
    .fetch_optional(&mut *conn)
    .await?;
    let (primary_resource, secondary_resource, time, level_effect) =
        row.ok_or_else(|| OwgeError::NotFound(format!("No upgrade with id {upgrade_id}")))?;
    Ok(UpgradeCost {
        primary_resource,
        secondary_resource,
        time,
        level_effect,
    })
}

/// `doInsideLock(...)` body: validate the level-up, deduct resources, create the
/// LEVEL_UP mission + its `mission_information` — all in one transaction on the
/// pinned, locked connection.
async fn do_register_level_up(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: u16,
) -> OwgeResult<()> {
    let mut tx = conn.begin().await?;

    // checkUpgradeMissionDoesNotExists — only one running LEVEL_UP per user.
    // findOneByUserIdAndTypeCode has no resolved filter (the mission is deleted
    // on completion/give-up, so a lingering resolved row never exists).
    let existing: Option<u64> =
        sqlx::query_scalar("SELECT id FROM missions WHERE user_id = ? AND type = ? LIMIT 1")
            .bind(user_id)
            .bind(MissionType::LevelUp.value())
            .fetch_optional(&mut *tx)
            .await?;
    if existing.is_some() {
        return Err(OwgeError::InvalidInput(
            "There is already an upgrade going".to_string(),
        ));
    }

    // obtainedUpgradeRepository.findOneByUserIdAndUpgradeId + checkUpgradeIsAvailable.
    let obtained: Option<(i16, i8)> = sqlx::query_as(
        "SELECT level, available FROM obtained_upgrades WHERE user_id = ? AND upgrade_id = ?",
    )
    .bind(user_id)
    .bind(upgrade_id)
    .fetch_optional(&mut *tx)
    .await?;
    let (current_level, available) = obtained.ok_or_else(|| {
        OwgeError::NotFound(format!(
            "No obtained_upgrade for user {user_id} and upgrade {upgrade_id}"
        ))
    })?;
    if available == 0 {
        return Err(OwgeError::InvalidInput(
            "Can't register mission, of type LEVEL_UP, when upgrade is not available!".to_string(),
        ));
    }

    let user = UserStorageBo::find_by_id(&mut tx, user_id)
        .await?
        .ok_or_else(|| OwgeError::NotFound(format!("No user with id {user_id}")))?;
    MissionBaseService::check_mission_limit_not_reached(&mut tx, user_id).await?;

    // calculateRequirementsAreMet(obtainedUpgrade): the upgrade's base cost grown
    // by `levelEffect` once per already-owned level (i.e. `current_level` times,
    // since `nextLevel = level + 1` and the loop runs `1..nextLevel`).
    let cost = load_upgrade_cost(&mut tx, upgrade_id).await?;
    let mut required_primary = cost.primary_resource as f64;
    let mut required_secondary = cost.secondary_resource as f64;
    let mut required_time = cost.time as f64;
    let level_effect = cost.level_effect as f64;
    for _ in 0..current_level.max(0) {
        required_primary += required_primary * level_effect;
        required_secondary += required_secondary * level_effect;
        required_time += required_time * level_effect;
    }

    // canRun(user): upgrades carry no energy requirement, so only the primary /
    // secondary resource headroom is checked.
    let primary = user.primary_resource.unwrap_or(0.0);
    let secondary = user.secondary_resource.unwrap_or(0.0);
    if primary < required_primary || secondary < required_secondary {
        return Err(OwgeError::InvalidInput("No enough resources!".to_string()));
    }

    // ZERO_UPGRADE_TIME (default TRUE) collapses the research time to 3s;
    // otherwise the moreUpgradeResearchSpeed improvement reduces it
    // (computeImprovementValue sum=false).
    if ConfigurationBo::find_or_set_default(&mut tx, "ZERO_UPGRADE_TIME", "TRUE")
        .await?
        .value
        == "TRUE"
    {
        required_time = 3.0;
    } else {
        let improvement = UserImprovementBo::find_user_improvement(&mut tx, user_id).await?;
        required_time = compute_improvement_value(
            &mut tx,
            required_time,
            improvement.more_upgrade_research_speed,
            false,
        )
        .await?;
    }

    // objectRelationBo.findOne(UPGRADE, upgradeId); missionInformation.value = level + 1.
    let relation_id = ObjectRelationBo::find_one(&mut tx, object_enum::UPGRADE, upgrade_id as i16)
        .await?
        .ok_or_else(|| {
            OwgeError::NotFound(format!("No object relation for upgrade {upgrade_id}"))
        })?;
    let target_level = (current_level + 1) as f64;

    let starting_date = chrono::Utc::now().naive_utc();
    let termination_date =
        crate::bo::mission_time_manager_bo::MissionTimeManagerBo::compute_termination_date(
            required_time,
        );
    let mission_result = sqlx::query(
        "INSERT INTO missions \
            (user_id, type, starting_date, required_time, termination_date, \
             primary_resource, secondary_resource, resolved, invisible) \
         VALUES (?, ?, ?, ?, ?, ?, ?, 0, 0)",
    )
    .bind(user_id)
    .bind(MissionType::LevelUp.value())
    .bind(starting_date)
    .bind(required_time)
    .bind(termination_date)
    .bind(required_primary)
    .bind(required_secondary)
    .execute(&mut *tx)
    .await?;
    let mission_id = mission_result.last_insert_id();

    sqlx::query(
        "INSERT INTO mission_information (mission_id, relation_id, value) VALUES (?, ?, ?)",
    )
    .bind(mission_id)
    .bind(relation_id)
    .bind(target_level)
    .execute(&mut *tx)
    .await?;

    // substractResources(user, mission).
    sqlx::query(
        "UPDATE user_storage \
            SET primary_resource = primary_resource - ?, \
                secondary_resource = secondary_resource - ? \
          WHERE id = ?",
    )
    .bind(required_primary)
    .bind(required_secondary)
    .bind(user_id)
    .execute(&mut *tx)
    .await?;

    schedule_build_mission(&mut tx, mission_id, required_time).await?;

    tx.commit().await?;
    Ok(())
}
