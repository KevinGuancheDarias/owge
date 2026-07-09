//! Port of the write side of
//! `com.kevinguanchedarias.owgejava.business.ActiveTimeSpecialBo` —
//! specifically `ActiveTimeSpecialBo.activate(timeSpecialId)`, backing
//! `POST game/time_special/activate`.
//!
//! Java `activate`:
//!   1. `timeSpecialBo.findByIdOrDie(timeSpecialId)` — 404 if the time special
//!      does not exist.
//!   2. `objectRelationBo.findOne(TIME_SPECIAL, id)` + `checkIsUnlocked` — the
//!      relation must be in the user's `unlocked_relation` (else
//!      `SgtBackendTargetNotUnlocked`, a bare `CommonException` → HTTP 500).
//!   3. `findOneByTimeSpecial(id, userId)` — if a row already exists (ACTIVE or
//!      RECHARGE) the method logs a warning and returns it unchanged.
//!   4. otherwise insert an `active_time_specials` row (state ACTIVE,
//!      `activation_date = now`, `expiring_date = now + duration*1000`), clear
//!      the improvement source cache, register a `TIME_SPECIAL_EFFECT_END`
//!      scheduled task (`duration` seconds out), fire
//!      `requirementBo.triggerTimeSpecialStateChange`, and emit
//!      `time_special_change` / obtained-units websocket events.
//!
//! **Effect lifecycle (now fully ported).** In Java the expire/recharge are
//! *Quartz* jobs (`TIME_SPECIAL_EFFECT_END` / `TIME_SPECIAL_IS_READY`). The Rust
//! port runs them over the **same db-scheduler `scheduled_tasks` table** as
//! missions, via [`ActiveTimeSpecialBo::spawn_effect_poller`] (started in
//! `main`). `activate` writes the `TIME_SPECIAL_EFFECT_END` row; when it fires,
//! [`ActiveTimeSpecialBo::handle_effect_end`] moves the special to `RECHARGE`,
//! schedules `TIME_SPECIAL_IS_READY`, re-evaluates the requirement gates and
//! emits; when that fires, [`ActiveTimeSpecialBo::handle_is_ready`] deletes the
//! row so the special is usable again.
//!
//! The only remaining unported side effect is `improvementBo.clearSourceCache`
//! (Rust recomputes user improvements on demand, so no cache to clear).

use sqlx::{Connection, MySqlConnection};

use crate::bo::realtime_emitter::RequirementEmit;
use crate::bo::unlocked_relation_bo::UnlockedRelationBo;
use crate::db::Db;
use crate::dto::time_special::ActiveTimeSpecialDto;
use crate::error::{OwgeError, OwgeResult};
use crate::model::object_relation::object_enum;
use crate::model::time_special::ActiveTimeSpecial;

/// `task_name` for the effect-end scheduled task, matching the Java
/// `ScheduledTask` type string.
const EFFECT_END_TASK_NAME: &str = "TIME_SPECIAL_EFFECT_END";
/// `task_name` for the recharge-complete scheduled task.
const IS_READY_TASK_NAME: &str = "TIME_SPECIAL_IS_READY";
/// Poll cadence + claim parameters — same db-scheduler protocol as the mission
/// poller (`MissionSchedulerService`).
const POLL_INTERVAL: std::time::Duration = std::time::Duration::from_secs(3);
const STALE_PICK_MINUTES: i64 = 10;
const POLL_BATCH: i64 = 50;

pub struct ActiveTimeSpecialBo;

impl ActiveTimeSpecialBo {
    /// `ActiveTimeSpecialBo.activate(timeSpecialId)`. Returns the (possibly
    /// pre-existing) active time special as the `ActiveTimeSpecialDto` the
    /// controller emits (`activeTimeSpecialBo.toDto(...)`).
    pub async fn activate(
        conn: &mut MySqlConnection,
        user_id: i32,
        time_special_id: u16,
    ) -> OwgeResult<ActiveTimeSpecialDto> {
        // 1. findByIdOrDie — load the time special's `duration` (seconds); 404 if
        // it does not exist (NotFoundException → HTTP 404).
        let duration: u64 =
            sqlx::query_scalar::<_, u64>("SELECT duration FROM time_specials WHERE id = ?")
                .bind(time_special_id)
                .fetch_optional(&mut *conn)
                .await?
                .ok_or_else(|| {
                    OwgeError::NotFound(format!("No TimeSpecial with id {time_special_id}"))
                })?;

        // 2. checkIsUnlocked: the TIME_SPECIAL relation must be unlocked for the
        // user. Java throws SgtBackendTargetNotUnlocked, which extends
        // SgtBackendInvalidInputException since the D5 re-parenting → HTTP 400.
        let unlocked = UnlockedRelationBo::find_unlocked_reference_ids(
            &mut *conn,
            user_id,
            object_enum::TIME_SPECIAL,
        )
        .await?;
        if !unlocked.contains(&(time_special_id as i16)) {
            return Err(OwgeError::InvalidInput(
                "The target object relation has not been unlocked".to_string(),
            ));
        }

        // 3. findOneByTimeSpecial — already active/recharging? return unchanged.
        if let Some(existing) =
            Self::find_one_by_time_special(&mut *conn, time_special_id, user_id).await?
        {
            tracing::warn!("The specified time special, is already active, doing nothing");
            return Ok(Self::to_dto(existing));
        }

        // 4. insert the new ACTIVE row. `expiring_date = now + duration*1000ms`
        // (computeExpiringDate). `id` is AUTO_INCREMENT. The insert, the
        // effect-end scheduling and the requirement re-trigger run in one
        // transaction (Java is `@Transactional`): the trigger mutates
        // `unlocked_relation` and must commit atomically with the activation.
        let now = chrono::Utc::now().naive_utc();
        let expiring_date = now + chrono::Duration::seconds(duration as i64);
        let mut tx = conn.begin().await?;
        let result = sqlx::query(
            "INSERT INTO active_time_specials \
                 (user_id, time_special_id, state, activation_date, expiring_date, ready_date) \
             VALUES (?, ?, 'ACTIVE', ?, ?, NULL)",
        )
        .bind(user_id)
        .bind(time_special_id)
        .bind(now)
        .bind(expiring_date)
        .execute(&mut *tx)
        .await?;
        let new_id = result.last_insert_id();

        // improvementBo.clearSourceCache(user, this) is done post-commit below via
        // `UserImprovementBo::evict_and_emit` (after `tx.commit()`).

        // Register the TIME_SPECIAL_EFFECT_END task so the effect auto-expires.
        // In Java this is a Quartz job; here it is a `scheduled_tasks` row driven
        // by `spawn_effect_poller` (filtered to the TIME_SPECIAL_* task names,
        // distinct from the `mission-run` poller).
        Self::schedule_effect_end(&mut tx, new_id, duration).await?;

        // requirementBo.triggerTimeSpecialStateChange(user, timeSpecial) —
        // re-evaluate HAVE_SPECIAL_ENABLED-gated relations now that the special is
        // active. (The expire side will need the M4 TIME_SPECIAL_EFFECT_END runner
        // to fire the same trigger on deactivation.)
        let user = crate::bo::mission_bo::load_user_storage(&mut tx, user_id).await?;
        let mut req_emits = Vec::new();
        crate::bo::requirement_bo::RequirementBo::trigger_time_special_state_change(
            &mut tx,
            &user,
            time_special_id as i64,
            &mut req_emits,
        )
        .await?;
        // TemporalUnitsListener.onTimeSpecialActivated (BEFORE_COMMIT): grant the
        // special's temporal units in the same transaction as the activation.
        crate::bo::TemporalUnitsBo::grant_on_activation(&mut tx, user_id, time_special_id).await?;
        tx.commit().await?;

        // Requirement-trigger unlock pushes now that the special is ACTIVE.
        crate::bo::realtime_emitter::drain_requirement_emits(&mut *conn, &req_emits).await?;

        // improvementBo.clearSourceCache(user, this): the new ACTIVE special adds an
        // improvement source — evict the cache and push user_improvements_change.
        crate::bo::UserImprovementBo::evict_and_emit(&mut *conn, user_id).await?;
        // emitTimeSpecialChange(user) + emitIfActivationAffectingUnits — post-commit
        // websocket pushes (the applicationEventPublisher event remains unported;
        // requirement-trigger unlock emits are a follow-up).
        crate::bo::realtime_emitter::emit_time_special_change(&mut *conn, user_id).await?;
        Self::emit_if_activation_affecting_units(&mut *conn, user_id, time_special_id).await?;

        let new_active = ActiveTimeSpecial {
            id: new_id,
            user_id,
            time_special_id,
            state: "ACTIVE".to_string(),
            activation_date: now,
            expiring_date,
            ready_date: None,
        };
        Ok(Self::to_dto(new_active))
    }

    /// `emitIfActivationAffectingUnits` — when the activated special has a rule
    /// whose origin is this `TIME_SPECIAL` and whose destination is a `UNIT` or
    /// `UNIT_TYPE` (i.e. the special alters units, e.g. temporal-unit swaps), the
    /// user's obtained units changed shape, so re-emit `unit_obtained_change`.
    pub(crate) async fn emit_if_activation_affecting_units(
        conn: &mut MySqlConnection,
        user_id: i32,
        time_special_id: u16,
    ) -> OwgeResult<()> {
        let affects: i64 = sqlx::query_scalar(
            "SELECT COUNT(*) FROM rules \
              WHERE origin_type = 'TIME_SPECIAL' AND origin_id = ? \
                AND destination_type IN ('UNIT', 'UNIT_TYPE')",
        )
        .bind(time_special_id)
        .fetch_one(&mut *conn)
        .await?;
        if affects > 0 {
            crate::bo::ObtainedUnitEventEmitter::emit_obtained_units(&mut *conn, user_id).await?;
        }
        Ok(())
    }

    /// `findOneByTimeSpecial(timeSpecialId, userId)` — the user's row for this
    /// special (ACTIVE or RECHARGE), or `None`.
    async fn find_one_by_time_special(
        conn: &mut MySqlConnection,
        time_special_id: u16,
        user_id: i32,
    ) -> OwgeResult<Option<ActiveTimeSpecial>> {
        let row = sqlx::query_as::<_, ActiveTimeSpecial>(
            "SELECT id, user_id, time_special_id, state, activation_date, expiring_date, ready_date \
             FROM active_time_specials WHERE time_special_id = ? AND user_id = ?",
        )
        .bind(time_special_id)
        .bind(user_id)
        .fetch_optional(&mut *conn)
        .await?;
        Ok(row)
    }

    /// Insert the effect-end `scheduled_tasks` row, fired `duration` seconds out.
    /// `task_instance` is the `active_time_specials` id (matching the Java
    /// `ScheduledTask(type, content=activeId)` payload). Columns mirror the
    /// db-scheduler row written by `MissionSchedulerService`.
    async fn schedule_effect_end(
        conn: &mut MySqlConnection,
        active_id: u64,
        duration_seconds: u64,
    ) -> OwgeResult<()> {
        sqlx::query(
            "INSERT INTO scheduled_tasks \
                 (task_name, task_instance, task_data, execution_time, picked, version) \
             VALUES (?, ?, NULL, DATE_ADD(NOW(6), INTERVAL ? SECOND), 0, 1) \
             ON DUPLICATE KEY UPDATE \
                 execution_time = DATE_ADD(NOW(6), INTERVAL ? SECOND), \
                 picked = 0, picked_by = NULL, last_heartbeat = NULL, \
                 version = version + 1",
        )
        .bind(EFFECT_END_TASK_NAME)
        .bind(active_id.to_string())
        .bind(duration_seconds as i64)
        .bind(duration_seconds as i64)
        .execute(&mut *conn)
        .await?;
        Ok(())
    }

    /// Spawn the background poller for the time-special lifecycle tasks
    /// (`TIME_SPECIAL_EFFECT_END` → recharge, `TIME_SPECIAL_IS_READY` → ready, and
    /// `UNIT_EXPIRED` → temporal-unit removal), using the same db-scheduler claim
    /// protocol as
    /// [`MissionSchedulerService::spawn_poller`](crate::bo::MissionSchedulerService)
    /// over the same `scheduled_tasks` table, filtered to the two task names. In
    /// Java these are Quartz jobs registered in `ActiveTimeSpecialBo.init`.
    pub fn spawn_effect_poller(db: Db, worker_id: String) -> tokio::task::JoinHandle<()> {
        tokio::spawn(async move {
            tracing::info!("time-special effect poller started ({worker_id})");
            loop {
                if let Err(e) = Self::poll_effects_once(&db, &worker_id).await {
                    tracing::error!("time-special poller tick failed: {e}");
                }
                tokio::time::sleep(POLL_INTERVAL).await;
            }
        })
    }

    /// One poll tick: claim due `TIME_SPECIAL_EFFECT_END`/`TIME_SPECIAL_IS_READY`/
    /// `UNIT_EXPIRED` rows with a `version` CAS and dispatch each. Handler errors are logged and
    /// swallowed so the claimed row is still cleared (mirroring the mission poller).
    async fn poll_effects_once(db: &Db, worker_id: &str) -> OwgeResult<()> {
        // Acquire exactly ONE connection for this tick; all handlers share it.
        let mut conn = db.acquire().await?;

        let candidates: Vec<(String, String, i64)> = sqlx::query_as(
            "SELECT task_name, task_instance, version FROM scheduled_tasks \
             WHERE task_name IN (?, ?, ?) \
               AND execution_time <= NOW(6) \
               AND (picked = 0 \
                    OR (picked = 1 AND (last_heartbeat IS NULL \
                                        OR last_heartbeat < DATE_SUB(NOW(6), INTERVAL ? MINUTE)))) \
             ORDER BY execution_time \
             LIMIT ?",
        )
        .bind(EFFECT_END_TASK_NAME)
        .bind(IS_READY_TASK_NAME)
        .bind(crate::bo::temporal_units_bo::UNIT_EXPIRED_TASK_NAME)
        .bind(STALE_PICK_MINUTES)
        .bind(POLL_BATCH)
        .fetch_all(&mut *conn)
        .await?;

        for (task_name, task_instance, version) in candidates {
            let claimed = sqlx::query(
                "UPDATE scheduled_tasks \
                 SET picked = 1, picked_by = ?, last_heartbeat = NOW(6), version = version + 1 \
                 WHERE task_name = ? AND task_instance = ? AND version = ?",
            )
            .bind(worker_id)
            .bind(&task_name)
            .bind(&task_instance)
            .bind(version)
            .execute(&mut *conn)
            .await?;
            if claimed.rows_affected() != 1 {
                continue; // lost the CAS race to another worker/tick
            }

            match task_instance.parse::<u64>() {
                Ok(id) => {
                    let result = if task_name == EFFECT_END_TASK_NAME {
                        Self::handle_effect_end(&mut conn, id).await
                    } else if task_name == IS_READY_TASK_NAME {
                        Self::handle_is_ready(&mut conn, id).await
                    } else {
                        // UNIT_EXPIRED — task_instance is the temporal-info id.
                        crate::bo::TemporalUnitsBo::handle_unit_expired(&mut conn, id as u32).await
                    };
                    if let Err(e) = result {
                        tracing::error!("time-special {task_name}({id}) failed: {e}");
                    }
                }
                Err(_) => {
                    tracing::warn!("non-numeric {task_name} task_instance {task_instance}");
                }
            }
            Self::delete_task(&mut conn, &task_name, &task_instance).await?;
        }
        Ok(())
    }

    /// `TIME_SPECIAL_EFFECT_END` handler → `ActiveTimeSpecialBo.deactivate`: run the
    /// deactivation in its own transaction, then drain the queued post-commit emits.
    async fn handle_effect_end(conn: &mut MySqlConnection, active_id: u64) -> OwgeResult<()> {
        let mut tx = conn.begin().await?;
        let mut emits = Vec::new();
        Self::deactivate_in_tx(&mut tx, active_id, &mut emits).await?;
        tx.commit().await?;
        crate::bo::realtime_emitter::drain_requirement_emits(&mut *conn, &emits).await?;
        Ok(())
    }

    /// `ActiveTimeSpecialBo.deactivate(id)` — move an ACTIVE special to `RECHARGE`,
    /// set `ready_date = now + rechargeTime`, schedule `TIME_SPECIAL_IS_READY`,
    /// re-evaluate (lock) the special's `HAVE_SPECIAL_ENABLED`-gated relations now
    /// that it is no longer enabled, and **queue** the post-commit emits
    /// (`time_special_change`, `clearSourceCache`, `emitIfActivationAffectingUnits`)
    /// into `emits` for the caller to drain after its transaction commits.
    ///
    /// Runs entirely on the caller's `conn` so it composes inside a larger
    /// transaction — both the `TIME_SPECIAL_EFFECT_END` task path
    /// ([`handle_effect_end`](Self::handle_effect_end)) and the requirement-lost
    /// cascade (`RequirementBo`/`requirement_engine` `unregisterLostRelation` →
    /// `ActiveTimeSpecialBo.relationLost`).
    pub(crate) async fn deactivate_in_tx(
        conn: &mut MySqlConnection,
        active_id: u64,
        emits: &mut Vec<RequirementEmit>,
    ) -> OwgeResult<()> {
        let row: Option<(i32, u16, u64)> = sqlx::query_as(
            "SELECT a.user_id, a.time_special_id, ts.recharge_time \
               FROM active_time_specials a \
               JOIN time_specials ts ON ts.id = a.time_special_id \
              WHERE a.id = ?",
        )
        .bind(active_id)
        .fetch_optional(&mut *conn)
        .await?;
        let Some((user_id, time_special_id, recharge_time)) = row else {
            return Ok(()); // already gone (e.g. manual deactivate / account delete)
        };

        let now = chrono::Utc::now().naive_utc();
        let ready_date = now + chrono::Duration::seconds(recharge_time as i64);

        sqlx::query(
            "UPDATE active_time_specials SET state = 'RECHARGE', ready_date = ? WHERE id = ?",
        )
        .bind(ready_date)
        .bind(active_id)
        .execute(&mut *conn)
        .await?;
        Self::schedule_is_ready(conn, active_id, recharge_time).await?;
        // triggerTimeSpecialStateChange — the same trigger used on activate; with
        // the special now RECHARGE (not ACTIVE) it locks the gated relations.
        let user = crate::bo::mission_bo::load_user_storage(conn, user_id).await?;
        crate::bo::requirement_bo::RequirementBo::trigger_time_special_state_change(
            conn,
            &user,
            time_special_id as i64,
            emits,
        )
        .await?;
        // Queue the deactivate post-commit emits (clearSourceCache +
        // time_special_change + emitIfActivationAffectingUnits), drained by the caller.
        emits.push(RequirementEmit::ImprovementCache(user_id));
        emits.push(RequirementEmit::TimeSpecialChange(user_id));
        emits.push(RequirementEmit::TimeSpecialAffectingUnits {
            user_id,
            time_special_id,
        });
        Ok(())
    }

    /// `TIME_SPECIAL_IS_READY` handler: the recharge finished — delete the
    /// `active_time_specials` row (the special is usable again) and emit
    /// `time_special_change`.
    async fn handle_is_ready(conn: &mut MySqlConnection, active_id: u64) -> OwgeResult<()> {
        let user_id: Option<i32> =
            sqlx::query_scalar("SELECT user_id FROM active_time_specials WHERE id = ?")
                .bind(active_id)
                .fetch_optional(&mut *conn)
                .await?;
        let Some(user_id) = user_id else {
            return Ok(());
        };
        sqlx::query("DELETE FROM active_time_specials WHERE id = ?")
            .bind(active_id)
            .execute(&mut *conn)
            .await?;
        crate::bo::realtime_emitter::emit_time_special_change(&mut *conn, user_id).await?;
        Ok(())
    }

    /// Insert the `TIME_SPECIAL_IS_READY` scheduled task, fired `recharge_time`
    /// seconds out (same db-scheduler row shape as [`schedule_effect_end`]).
    async fn schedule_is_ready(
        conn: &mut MySqlConnection,
        active_id: u64,
        recharge_seconds: u64,
    ) -> OwgeResult<()> {
        sqlx::query(
            "INSERT INTO scheduled_tasks \
                 (task_name, task_instance, task_data, execution_time, picked, version) \
             VALUES (?, ?, NULL, DATE_ADD(NOW(6), INTERVAL ? SECOND), 0, 1) \
             ON DUPLICATE KEY UPDATE \
                 execution_time = DATE_ADD(NOW(6), INTERVAL ? SECOND), \
                 picked = 0, picked_by = NULL, last_heartbeat = NULL, \
                 version = version + 1",
        )
        .bind(IS_READY_TASK_NAME)
        .bind(active_id.to_string())
        .bind(recharge_seconds as i64)
        .bind(recharge_seconds as i64)
        .execute(&mut *conn)
        .await?;
        Ok(())
    }

    async fn delete_task(
        conn: &mut MySqlConnection,
        task_name: &str,
        task_instance: &str,
    ) -> OwgeResult<()> {
        sqlx::query("DELETE FROM scheduled_tasks WHERE task_name = ? AND task_instance = ?")
            .bind(task_name)
            .bind(task_instance)
            .execute(&mut *conn)
            .await?;
        Ok(())
    }

    /// `ActiveTimeSpecialDto.dtoFromEntity` — build the response DTO and compute
    /// `pendingMillis` (`calculatePendingMillis`): for ACTIVE it is the millis to
    /// `expiringDate`, for RECHARGE the millis to `readyDate`.
    fn to_dto(entity: ActiveTimeSpecial) -> ActiveTimeSpecialDto {
        let activation_date = entity.activation_date.and_utc().timestamp_millis();
        let expiring_date = entity.expiring_date.and_utc().timestamp_millis();
        let ready_date = entity.ready_date.map(|d| d.and_utc().timestamp_millis());
        let now = chrono::Utc::now().timestamp_millis();
        let pending_millis = if entity.state == "ACTIVE" {
            expiring_date - now
        } else {
            ready_date.unwrap_or(expiring_date) - now
        };
        ActiveTimeSpecialDto {
            id: entity.id,
            time_special: entity.time_special_id,
            state: entity.state,
            activation_date,
            expiring_date,
            ready_date,
            pending_millis,
        }
    }
}
