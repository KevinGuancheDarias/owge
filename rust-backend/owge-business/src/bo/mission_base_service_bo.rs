//! Port of `business/mission/MissionBaseService.java`.
//!
//! The shared, type-agnostic mission helpers used by the dispatch/retry layer:
//! [`MissionBaseService::retry_mission_if_possible`] (the catch-handler the
//! realization job runs when a mission body throws), the per-user mission-count
//! cap ([`MissionBaseService::check_mission_limit_not_reached`]), and the small
//! [`MissionBaseService::is_of_type`] predicate.
//!
//! ## Parity with `retryMissionIfPossible`
//! On each failed attempt the mission's `attemps` counter is bumped; once it
//! reaches `MAX_ATTEMPTS` (3) the engine gives up:
//!
//! - **unit missions** register a return mission (units fly home) and the
//!   mission is marked `resolved`;
//! - **BUILD_UNIT** deletes the mission's obtained units and the mission;
//! - **LEVEL_UP** deletes the mission.
//!
//! Below the cap it bumps `attemps`, recomputes the termination date, saves a
//! common error report, re-schedules the run, and persists the mission.
//!
//! ## sqlx signedness (load-bearing)
//! `missions.id`/`report_id` = `u64`; `missions.attemps` = `tinyint unsigned`
//! (`u8`); `missions.resolved` = bare `tinyint` (`i8`); `missions.user_id` =
//! signed `int` (`i32`); `missions.type` = `smallint unsigned` (`u16`). See the
//! `Mission` model for the full map.

use sqlx::MySqlConnection;

use crate::builder::UnitMissionReportBuilder;
use crate::error::{OwgeError, OwgeResult};
use crate::model::mission::{Mission, MissionType};

/// `MissionBaseService.MAX_ATTEMPTS` — give up after this many failed runs.
const MAX_ATTEMPTS: u8 = 3;
/// `MissionSchedulerService.DELAY_HANDLE` — fire 2s before the nominal end.
const DELAY_HANDLE: i64 = 2;
pub struct MissionBaseService;

impl MissionBaseService {
    /// `retryMissionIfPossible` — re-arm (or finally give up on) a mission that
    /// threw during execution.
    ///
    /// Runs on the caller's pinned `conn`, mirroring the Java `@Transactional`
    /// boundary of `retryMissionIfPossible`. The realization job calls this
    /// *after* the failed mission body's locked section is gone, so it does not
    /// need the planet locks (the return-mission registration it may do re-points
    /// obtained units by mission id, which is safe to run unlocked here for the
    /// retry/give-up path — matching the Java service's own transaction).
    /// Returns the instant the failed execution should be RETRIED at, or `None`
    /// when the mission was given up (max attempts). Mirrors the hotfixed Java
    /// `retryMissionIfPossible` (17f266a2): the retry must NOT be scheduled
    /// from inside the failing execution — the runner still holds the claimed
    /// `scheduled_tasks` row and unconditionally clears it after the run, so an
    /// inner reschedule was silently wiped and the retry lost. The caller
    /// (the scheduler loop) re-arms the claimed row with the returned instant.
    pub async fn retry_mission_if_possible(
        conn: &mut MySqlConnection,
        mission_id: u64,
        mission_type: MissionType,
    ) -> OwgeResult<Option<chrono::NaiveDateTime>> {
        let mission = load_mission(&mut *conn, mission_id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No mission with id {mission_id}")))?;

        if mission.attemps >= MAX_ATTEMPTS {
            // (17f266a2) the player is only notified when the mission is
            // definitively given up; attempts that will still be retried used
            // to save this report too, alarming players over failures that
            // succeeded on a later attempt. Java saves the report BEFORE the
            // per-type give-up actions.
            let report = build_common_error_report(&mut *conn, &mission, mission_type).await?;
            let (report_user_id, report_id, report_date) =
                crate::bo::mission_report_manager_bo::MissionReportManagerBo::handle_mission_report_save(
                    &mut *conn, &mission, report,
                )
                .await?;
            Self::give_up(&mut *conn, &mission, mission_type).await?;
            // handleMissionReportSave's emitOneToUser fires after Java's
            // REQUIRES_NEW commit; here the give-up work has flushed on conn.
            crate::bo::realtime_emitter::emit_mission_report_new(
                &mut *conn,
                report_user_id,
                report_id,
                report_date,
            )
            .await?;
            crate::bo::realtime_emitter::emit_mission_report_count_change(
                &mut *conn,
                report_user_id,
            )
            .await?;
            Ok(None)
        } else {
            Ok(Some(Self::reschedule(&mut *conn, mission).await?))
        }
    }

    /// Final give-up branch of `retryMissionIfPossible`.
    async fn give_up(
        conn: &mut MySqlConnection,
        mission: &Mission,
        mission_type: MissionType,
    ) -> OwgeResult<()> {
        if mission_type.is_unit_mission() {
            // returnMissionRegistrationBo.registerReturnMission(mission, null) — units fly home.
            let return_id = crate::bo::return_mission_registration_bo::ReturnMissionRegistrationBo::register_return_mission(
                conn, mission, None,
            )
            .await?;
            sqlx::query("UPDATE missions SET resolved = 1 WHERE id = ?")
                .bind(mission.id)
                .execute(&mut *conn)
                .await?;
            // Java's doRegisterReturnMission ends with
            // emitLocalMissionChangeAfterCommit(returnMission); Rust's
            // register_return_mission leaves the emit to its caller, and this
            // caller had none (the old "rare path, open" backlog item). Same
            // direct-emit pattern as the reschedule branch below.
            crate::bo::MissionEventEmitter::emit_local_mission_change(
                &mut *conn,
                return_id,
                mission.user_id.unwrap_or_default(),
            )
            .await?;
        } else if mission_type == MissionType::BuildUnit {
            // obtainedUnitModificationBo.deleteByMissionId(mission.getId()); then delete the mission.
            sqlx::query("DELETE FROM obtained_units WHERE mission_id = ?")
                .bind(mission.id)
                .execute(&mut *conn)
                .await?;
            delete_mission(conn, mission.id).await?;
        } else if mission_type == MissionType::LevelUp {
            delete_mission(conn, mission.id).await?;
        } else {
            return Err(OwgeError::Common(
                "Should never ever happen: retry give-up for a non unit/build/level-up mission"
                    .to_string(),
            ));
        }
        Ok(())
    }

    /// Below-the-cap branch of `retryMissionIfPossible` (hotfixed shape,
    /// 17f266a2): bump attempts + termination only — NO error report (the
    /// player is only alarmed on the final give-up) and NO scheduling from in
    /// here (the runner re-arms its claimed row with the returned instant,
    /// `MissionSchedulerService.computeExecutionTime`).
    async fn reschedule(
        conn: &mut MySqlConnection,
        mut mission: Mission,
    ) -> OwgeResult<chrono::NaiveDateTime> {
        mission.attemps += 1;
        mission.termination_date = Some(
            crate::bo::mission_time_manager_bo::MissionTimeManagerBo::compute_termination_date(
                mission.required_time.unwrap_or(0.0),
            ),
        );

        // missionRepository.save(mission) — persist the bumped attempts / new date.
        sqlx::query("UPDATE missions SET attemps = ?, termination_date = ? WHERE id = ?")
            .bind(mission.attemps)
            .bind(mission.termination_date)
            .bind(mission.id)
            .execute(&mut *conn)
            .await?;

        // Instant.now().plusSeconds(requiredTime - DELAY_HANDLE)
        Ok(chrono::Utc::now().naive_utc()
            + chrono::Duration::seconds(mission.required_time.unwrap_or(0.0) as i64 - DELAY_HANDLE))
    }

    /// `isOfType` — is this mission of the given type?
    pub fn is_of_type(mission: &Mission, mission_type: MissionType) -> bool {
        mission.mission_type() == Some(mission_type)
    }

    /// `checkMissionLimitNotReached` — throws when registering one more mission
    /// would meet/exceed the user's max-allowed missions
    /// (`findUserImprovement(user).getMoreMissions() + 1`).
    pub async fn check_mission_limit_not_reached(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<()> {
        let running: i64 =
            sqlx::query_scalar("SELECT COUNT(*) FROM missions WHERE user_id = ? AND resolved = 0")
                .bind(user_id)
                .fetch_one(&mut *conn)
                .await?;
        let max_allowed = find_user_max_allowed_missions(&mut *conn, user_id).await?;
        if running + 1 >= max_allowed {
            return Err(OwgeError::InvalidInput(
                "I18N_ERR_MISSION_LIMIT_EXCEEDED".to_string(),
            ));
        }
        Ok(())
    }
}

/// `findUserMaxAllowedMissions` — `findUserImprovement(user).getMoreMissions().intValue() + 1`.
async fn find_user_max_allowed_missions(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<i64> {
    // `find_user_improvement` already adds the base +1 mission slot to
    // `more_missions`; Java does `.intValue() + 1` on the same aggregate, so add
    // one more here for the registration-headroom slot.
    let improvement = crate::bo::user_improvement_bo::UserImprovementBo::find_user_improvement(
        &mut *conn, user_id,
    )
    .await?;
    Ok(improvement.more_missions as i64 + 1)
}

/// `buildCommonErrorReport` — the placeholder error report saved on a retry.
async fn build_common_error_report(
    conn: &mut MySqlConnection,
    mission: &Mission,
    mission_type: MissionType,
) -> OwgeResult<UnitMissionReportBuilder> {
    let (user_id, username) = crate::bo::mission_processor::load_sender_user(conn, mission).await?;
    let mut builder = UnitMissionReportBuilder::create()
        .with_sender_user(user_id, &username)
        .with_id(mission.id);

    if mission_type.is_unit_mission() {
        // withSourcePlanet / withTargetPlanet / withInvolvedUnits(findByMissionId).
        if let Some(source) = mission.source_planet {
            if let Some(dto) =
                crate::bo::mission_processor::load_planet_dto(conn, source as u64).await?
            {
                builder = builder.with_source_planet(&dto);
            }
        }
        if let Some(target) = mission.target_planet {
            if let Some(dto) =
                crate::bo::mission_processor::load_planet_dto(conn, target as u64).await?
            {
                builder = builder.with_target_planet(&dto);
            }
        }
        let units = load_mission_units(conn, mission.id).await?;
        let dtos = crate::bo::mission_processor::involved_units_to_dtos(conn, &units).await?;
        builder = builder.with_involved_units(&dtos);
    }

    builder = builder.with_error_information(&format!(
        "Mission with id {} failed, please contact an admin!",
        mission.id
    ));
    Ok(builder)
}

async fn load_mission_units(
    conn: &mut MySqlConnection,
    mission_id: u64,
) -> OwgeResult<Vec<crate::model::obtained_unit::ObtainedUnit>> {
    Ok(
        sqlx::query_as::<_, crate::model::obtained_unit::ObtainedUnit>(
            "SELECT id, user_id, unit_id, count, source_planet, target_planet, \
                mission_id, first_deployment_mission, is_from_capture, \
                expiration_id, owner_unit_id \
         FROM obtained_units WHERE mission_id = ?",
        )
        .bind(mission_id)
        .fetch_all(&mut *conn)
        .await?,
    )
}

async fn load_mission(conn: &mut MySqlConnection, mission_id: u64) -> OwgeResult<Option<Mission>> {
    Ok(sqlx::query_as::<_, Mission>(SELECT_MISSION)
        .bind(mission_id)
        .fetch_optional(&mut *conn)
        .await?)
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

pub(crate) const SELECT_MISSION: &str = "\
    SELECT id, user_id, type, termination_date, required_time, starting_date, \
           primary_resource, secondary_resource, required_energy, \
           source_planet, target_planet, related_mission, report_id, \
           attemps, resolved, invisible \
    FROM missions WHERE id = ?";
