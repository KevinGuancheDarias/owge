//! Port of `business/mission/unit/registration/returns/ReturnMissionRegistrationBo.java`.
//!
//! When a unit mission ends (or is aborted) the units must fly home: a new
//! `RETURN_MISSION` is registered that swaps source/target back and re-points the
//! origin mission's obtained units onto it. The Java `registerReturnMission` wraps
//! `doRegisterReturnMission` in a planet lock; here the caller (the mission runner)
//! already holds the planet superset on the pinned `conn`, so this is the inner
//! `doRegisterReturnMission` body run directly on that connection.
//!
//! ## sqlx signedness
//! `missions.id` = `u64`; `related_mission` = `Option<u64>`; `source_planet`/
//! `target_planet` = **signed** `i64`; `user_id` = signed `i32`; `invisible` =
//! `i8`. The new mission inherits the origin's planets/user/invisibility.

use sqlx::MySqlConnection;

use crate::error::OwgeResult;
use crate::model::mission::{Mission, MissionType};

/// `MissionSchedulerService.DELAY_HANDLE` — fire 2s before the nominal end.
const DELAY_HANDLE: i64 = 2;
/// `scheduled_tasks.task_name` for a mission row.
const MISSION_TASK_NAME: &str = "mission-run";

pub struct ReturnMissionRegistrationBo;

impl ReturnMissionRegistrationBo {
    /// `doRegisterReturnMission` — register the return for `source_mission`.
    ///
    /// `custom_duration` overrides the required time (`customRequiredTime`); when
    /// `None`, the origin mission's `required_time` is reused. Runs on the caller's
    /// pinned, planet-locked `conn` (the Java method's planet-lock wrapper is the
    /// caller's responsibility here).
    pub async fn register_return_mission(
        conn: &mut MySqlConnection,
        source_mission: &Mission,
        custom_duration: Option<f64>,
    ) -> OwgeResult<u64> {
        let starting_date = chrono::Utc::now().naive_utc();
        // customRequiredTime ?? originMission.requiredTime
        let required_time = custom_duration
            .or(source_mission.required_time)
            .unwrap_or(0.0);
        let termination_date =
            crate::bo::mission_time_manager_bo::MissionTimeManagerBo::compute_termination_date(
                required_time,
            );
        // returnMission.setInvisible(Boolean.TRUE.equals(originMission.getInvisible()))
        let invisible: i8 = if source_mission.invisible != 0 { 1 } else { 0 };

        // missionRepository.saveAndFlush(returnMission)
        let result = sqlx::query(
            "INSERT INTO missions \
                (user_id, type, starting_date, required_time, termination_date, \
                 source_planet, target_planet, related_mission, attemps, resolved, invisible) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, 1, 0, ?)",
        )
        .bind(source_mission.user_id)
        .bind(MissionType::ReturnMission.value())
        .bind(starting_date)
        .bind(required_time)
        .bind(termination_date)
        .bind(source_mission.source_planet)
        .bind(source_mission.target_planet)
        .bind(source_mission.id)
        .bind(invisible)
        .execute(&mut *conn)
        .await?;
        let return_mission_id = result.last_insert_id();

        // obtainedUnits.forEach(current -> current.setMission(returnMission)); saveAll(...)
        // Re-point every obtained unit that belonged to the origin mission onto the
        // return mission (`findByMissionId(originMission.getId())`).
        sqlx::query("UPDATE obtained_units SET mission_id = ? WHERE mission_id = ?")
            .bind(return_mission_id)
            .bind(source_mission.id)
            .execute(&mut *conn)
            .await?;

        // missionSchedulerService.scheduleMission(returnMission) — inlined on the
        // registration connection so the scheduled_tasks row commits atomically.
        schedule_mission(conn, return_mission_id, required_time).await?;

        // Java emits emitLocalMissionChangeAfterCommit(returnMission) here — an
        // EXTRA unit_mission_change frame on top of the end-of-run emit (the
        // frames are duplicates content-wise, but the count is observable on the
        // wire, so parity keeps both). This runs on the borrowed `conn` (no
        // post-commit hook), so processor callers queue the equivalent
        // `DeferredEmit::LocalMissionChange` for the returned mission id;
        // my_cancel_mission's own post-commit emit_unit_missions already matches
        // Java's single frame on the cancel path.
        Ok(return_mission_id)
    }
}

/// `MissionSchedulerService.scheduleMission`, inlined on the caller's connection
/// (identical SQL to `mission_scheduler_bo`).
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
