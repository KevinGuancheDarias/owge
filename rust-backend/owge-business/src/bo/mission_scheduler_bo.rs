//! Port of `MissionSchedulerService` + the kagkarlsson **db-scheduler** runner
//! (`DbSchedulerConfiguration` / `DbSchedulerRealizationJob`).
//!
//! The Java engine schedules each mission as a one-time db-scheduler task in the
//! `scheduled_tasks` table (`task_name = "mission-run"`, `task_instance =
//! <missionId>`, fired at `now + requiredTime − 2s`). A background poller claims
//! due rows with optimistic concurrency (`version` CAS), runs the mission, and
//! deletes the row. We re-implement just that protocol over the *same* table so
//! existing universes' pending rows stay compatible.
//!
//! The mission body itself never propagates errors to the scheduler: like
//! `DbSchedulerRealizationJob.execute`, the dispatcher catches failures and
//! re-schedules via the retry path, so a claimed row is always deleted after a
//! run.

use std::sync::Arc;
use std::time::Duration;

use crate::db::Db;
use crate::error::OwgeResult;

/// `task_name` for every mission row (Java `BASIC_ONE_TIME_TASK`).
pub const MISSION_TASK_NAME: &str = "mission-run";
/// `MissionSchedulerService.DELAY_HANDLE` — fire 2s before the nominal end so
/// the mission resolves right at its termination instant.
const DELAY_HANDLE: i64 = 2;
/// db-scheduler default poll cadence.
const POLL_INTERVAL: Duration = Duration::from_secs(3);
/// A picked row whose worker died (no heartbeat) is reclaimed after this long.
const STALE_PICK_MINUTES: i64 = 10;
/// Max rows claimed per poll tick.
const POLL_BATCH: i64 = 50;

/// Dispatches a single due mission. Implemented by the realization layer
/// (`DbSchedulerRealizationJob` analogue) which owns the `Bo` graph; kept as a
/// trait so the scheduler module has no dependency on the mission processors.
#[async_trait::async_trait]
pub trait MissionDispatch: Send + Sync {
    /// Run the mission with this id. Must **not** return an error for a
    /// mission-level failure — handle retry/giving-up internally (mirroring
    /// `DbSchedulerRealizationJob.execute`).
    async fn run_mission(&self, mission_id: u64);
}

#[derive(Clone)]
pub struct MissionSchedulerService {
    db: Db,
}

impl MissionSchedulerService {
    pub fn new(db: Db) -> Self {
        Self { db }
    }

    /// `scheduleMission` — insert (or re-arm, for a retry) the `scheduled_tasks`
    /// row. `execution_time = NOW(6) + (requiredTime − 2)s`.
    pub async fn schedule_mission(
        &self,
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
        .execute(&self.db)
        .await?;
        Ok(())
    }

    /// `abortMissionJob` — cancel a pending mission row (no-op if already gone).
    pub async fn abort_mission_job(&self, mission_id: u64) -> OwgeResult<()> {
        sqlx::query("DELETE FROM scheduled_tasks WHERE task_name = ? AND task_instance = ?")
            .bind(MISSION_TASK_NAME)
            .bind(mission_id.to_string())
            .execute(&self.db)
            .await?;
        Ok(())
    }

    /// Spawn the background poller. Returns the `JoinHandle` so `main` can keep
    /// it alive; the loop runs until the process exits.
    pub fn spawn_poller(
        &self,
        dispatch: Arc<dyn MissionDispatch>,
        worker_id: String,
    ) -> tokio::task::JoinHandle<()> {
        let db = self.db.clone();
        tokio::spawn(async move {
            tracing::info!("mission scheduler poller started ({worker_id})");
            loop {
                if let Err(e) = poll_once(&db, &dispatch, &worker_id).await {
                    tracing::error!("mission poller tick failed: {e}");
                }
                tokio::time::sleep(POLL_INTERVAL).await;
            }
        })
    }
}

/// One poll tick: find due/stale rows, claim each with a `version` CAS, and
/// dispatch the winners. Each winner is run inline (then its row deleted); the
/// dispatcher does its own per-mission concurrency control via planet locks.
async fn poll_once(db: &Db, dispatch: &Arc<dyn MissionDispatch>, worker_id: &str) -> OwgeResult<()> {
    let candidates: Vec<(String, i64)> = sqlx::query_as(
        "SELECT task_instance, version FROM scheduled_tasks \
         WHERE task_name = ? \
           AND execution_time <= NOW(6) \
           AND (picked = 0 \
                OR (picked = 1 AND (last_heartbeat IS NULL \
                                    OR last_heartbeat < DATE_SUB(NOW(6), INTERVAL ? MINUTE)))) \
         ORDER BY execution_time \
         LIMIT ?",
    )
    .bind(MISSION_TASK_NAME)
    .bind(STALE_PICK_MINUTES)
    .bind(POLL_BATCH)
    .fetch_all(db)
    .await?;

    for (task_instance, version) in candidates {
        let claimed = sqlx::query(
            "UPDATE scheduled_tasks \
             SET picked = 1, picked_by = ?, last_heartbeat = NOW(6), version = version + 1 \
             WHERE task_name = ? AND task_instance = ? AND version = ?",
        )
        .bind(worker_id)
        .bind(MISSION_TASK_NAME)
        .bind(&task_instance)
        .bind(version)
        .execute(db)
        .await?;

        if claimed.rows_affected() != 1 {
            // Another worker (or tick) won the CAS race; skip.
            continue;
        }

        let mission_id: u64 = match task_instance.parse() {
            Ok(id) => id,
            Err(_) => {
                tracing::warn!("non-numeric mission task_instance {task_instance}, deleting");
                delete_row(db, &task_instance).await?;
                continue;
            }
        };

        // The dispatcher swallows mission-level errors (retry handled inside),
        // so the row is always cleared after the run.
        dispatch.run_mission(mission_id).await;
        delete_row(db, &task_instance).await?;
    }
    Ok(())
}

async fn delete_row(db: &Db, task_instance: &str) -> OwgeResult<()> {
    sqlx::query("DELETE FROM scheduled_tasks WHERE task_name = ? AND task_instance = ?")
        .bind(MISSION_TASK_NAME)
        .bind(task_instance)
        .execute(db)
        .await?;
    Ok(())
}
