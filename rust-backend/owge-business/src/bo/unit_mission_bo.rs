//! Port of `business/UnitMissionBo.java` — the registration entry methods, the
//! mission cancel, and the unit-mission runner — plus the realization-job
//! dispatcher ([`MissionRunner`], the `DbSchedulerRealizationJob.execute`
//! analogue and the [`crate::bo::mission_scheduler_bo::MissionDispatch`] impl).
//!
//! ## Registration entry methods
//! Java's `my*` variants set the sender to the logged-in user and then call the
//! `adminRegister*` variant; here the authenticated [`UserStorage`] is passed in
//! by the REST layer, so a single `register_*` per mission type covers both (the
//! `my_*` / `admin_*` names are kept as thin wrappers for call-site parity). Each
//! does the type-specific precondition (counterattack ownership, conquest
//! own/home-planet guards, deploy self-target guard), the global mission-limit
//! check, the "target explored" guard (non-explore), then runs
//! `do_common_mission_register` under the source/target planet lock superset on a
//! pinned connection.
//!
//! ## Runner
//! [`UnitMissionBo::run_unit_mission`] loads the mission, resolves the planet
//! lock superset (source/target + every planet owned by their owners — the same
//! superset the Java `resolvePlanetsToLock` builds to avoid the user-lock
//! inversion deadlock), acquires the locks on one pinned connection, runs
//! interception + the matching processor + the report save under the lock, then
//! releases. Errors propagate to the dispatcher, which retries.
//!
//! ## sqlx signedness (load-bearing)
//! `missions.id` = `u64`; `source_planet`/`target_planet` = **signed** `i64`;
//! `user_id` = signed `i32`. `planets.id` = `u64`, `owner` = `Option<i32>`. See
//! the `Mission` / `Planet` models for the full map.

use std::collections::BTreeSet;

use crate::bo::emitter::unit_type_emitter::UnitTypeEmitter;
use crate::bo::mission_base_service_bo::MissionBaseService;
use crate::bo::mission_scheduler_bo::MissionDispatch;
use crate::bo::UserImprovementBo;
use crate::db::Db;
use crate::error::{OwgeError, OwgeResult};
use crate::lock::{self, planet_lock_key};
use crate::model::mission::{Mission, MissionType};
use crate::model::user_storage::UserStorage;
use crate::pojo::unit_mission_information::UnitMissionInformation;
use async_trait::async_trait;
use chrono::Utc;
use sqlx::{Connection, MySqlConnection};

/// Number of times the runner retries a mission whose body failed to acquire its
/// planet locks (`@Retryable(retryFor = CannotAcquireLockException.class)`).
const LOCK_RETRY_ATTEMPTS: u32 = 3;

pub struct UnitMissionBo;

impl UnitMissionBo {
    // ---- registration entry methods -------------------------------------------------

    pub async fn my_register_explore_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::admin_register_explore_mission(db, user, info).await
    }

    pub async fn admin_register_explore_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::common_mission_register(db, user, info, MissionType::Explore).await
    }

    pub async fn my_register_gather_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::admin_register_gather_mission(db, user, info).await
    }

    pub async fn admin_register_gather_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::common_mission_register(db, user, info, MissionType::Gather).await
    }

    pub async fn my_register_establish_base_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::admin_register_establish_base(db, user, info).await
    }

    pub async fn admin_register_establish_base(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::common_mission_register(db, user, info, MissionType::EstablishBase).await
    }

    pub async fn my_register_attack_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::admin_register_attack_mission(db, user, info).await
    }

    pub async fn admin_register_attack_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::common_mission_register(db, user, info, MissionType::Attack).await
    }

    pub async fn my_register_counterattack_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::admin_register_counterattack_mission(db, user, info).await
    }

    pub async fn admin_register_counterattack_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        // TargetPlanet must belong to the sender user.
        if !is_of_user_property(db, user.id, info.target_planet_id).await? {
            return Err(OwgeError::InvalidInput(
                "TargetPlanet doesn't belong to sender user, try again dear Hacker, maybe next \
                 time you have some luck"
                    .to_string(),
            ));
        }
        Self::common_mission_register(db, user, info, MissionType::Counterattack).await
    }

    pub async fn my_register_conquest_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::admin_register_conquest_mission(db, user, info).await
    }

    pub async fn admin_register_conquest_mission(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        if is_of_user_property(db, user.id, info.target_planet_id).await? {
            return Err(OwgeError::InvalidInput(
                "Doesn't make sense to conquest your own planet... unless your population hates \
                 you, and are going to organize a rebelion"
                    .to_string(),
            ));
        }
        if is_home_planet(db, info.target_planet_id).await? {
            return Err(OwgeError::InvalidInput(
                "Can't steal a home planet to a user, would you like a bandit to steal in your \
                 own home??!"
                    .to_string(),
            ));
        }
        Self::common_mission_register(db, user, info, MissionType::Conquest).await
    }

    pub async fn my_register_deploy(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        Self::admin_register_deploy(db, user, info).await
    }

    pub async fn admin_register_deploy(
        db: &Db,
        user: &UserStorage,
        info: UnitMissionInformation,
    ) -> OwgeResult<()> {
        if info.source_planet_id == Some(info.target_planet_id) {
            return Err(OwgeError::InvalidInput(
                "I18N_ERR_DEPLOY_ITSELF".to_string(),
            ));
        }
        Self::common_mission_register(db, user, info, MissionType::Deploy).await
    }

    /// `commonMissionRegister` — shared registration path: set the sender, apply
    /// the mission-limit + explored guards, then register under the planet lock.
    async fn common_mission_register(
        db: &Db,
        user: &UserStorage,
        mut info: UnitMissionInformation,
        mission_type: MissionType,
    ) -> OwgeResult<()> {
        info.mission_type = Some(mission_type);
        info.user_id = Some(user.id);

        let is_deploy = mission_type == MissionType::Deploy;
        // Deploying onto an already-owned planet does not count against the limit.
        let deploy_to_own =
            is_deploy && is_of_user_property(db, user.id, info.target_planet_id).await?;
        if !deploy_to_own {
            MissionBaseService::check_mission_limit_not_reached(db, user.id).await?;
        }

        // Non-explore missions require the target planet to be explored by the user.
        if mission_type != MissionType::Explore
            && !is_explored(db, user.id, info.target_planet_id).await?
        {
            return Err(OwgeError::InvalidInput(
                "Can't send this mission, because target planet is not explored ".to_string(),
            ));
        }

        // doInsideLockById(source, target) -> doCommonMissionRegister on a pinned conn.
        let mut keys = Vec::new();
        if let Some(source) = info.source_planet_id {
            keys.push(planet_lock_key(source as u64));
        }
        keys.push(planet_lock_key(info.target_planet_id as u64));

        let mut conn = db.acquire().await?;
        // Move owned copies into the locked body so the boxed future borrows only
        // `conn` (the HRTB factory must be valid for any connection lifetime).
        let owned_user = user.clone();
        let source_planet_id = info.source_planet_id;
        let (mission, req_emits) = run_locked(&mut conn, &keys, move |conn| {
            Box::pin(async move {
                crate::bo::unit_mission_registration_bo::UnitMissionRegistrationBo::do_common_mission_register(
                    conn, &info, mission_type, &owned_user, is_deploy,
                )
                .await
            })
        })
        .await?;
        drop(conn);

        // Requirement-trigger unlock pushes from the unit subtraction, after the
        // tx commit + lock release (Java doAfterCommit semantics).
        crate::bo::realtime_emitter::drain_requirement_emits(db, &req_emits).await?;

        // M4 emits, after commit + lock release (Java doCommonMissionRegister tail):
        // emitLocalMissionChangeAfterCommit + (if invoker owns the source planet)
        // emitObtainedUnitsAfterCommit, else (enemy source planet) emitEnemyMissionsChange.
        crate::bo::MissionEventEmitter::emit_local_mission_change(db, mission.id, user.id).await?;
        if let Some(src) = source_planet_id {
            let source_owner: Option<Option<i32>> =
                sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
                    .bind(src)
                    .fetch_optional(db)
                    .await?;
            match source_owner.flatten() {
                Some(o) if o == user.id => {
                    crate::bo::ObtainedUnitEventEmitter::emit_obtained_units(db, user.id).await?;
                }
                Some(o) => {
                    crate::bo::MissionEventEmitter::emit_enemy_missions_change(db, o).await?;
                }
                None => {}
            }
        }
        Ok(())
    }

    // ---- cancel ---------------------------------------------------------------------

    /// `myCancelMission` — cancel a player's own (non-return) mission and fly its
    /// units home with a duration reduced by however long it had already flown.
    ///
    /// `user_id` is the authenticated user (the Java side reads it from the
    /// session); the cancel is rejected if it does not own the mission.
    pub async fn my_cancel_mission(db: &Db, user_id: i32, mission_id: u64) -> OwgeResult<()> {
        let mut conn = db.acquire().await?;
        let mission = load_mission(&mut conn, mission_id).await?.ok_or_else(|| {
            OwgeError::NotFound(format!("No mission with id {mission_id} was found"))
        })?;
        if mission.user_id != Some(user_id) {
            return Err(OwgeError::InvalidInput(
                "You can't cancel other player missions".to_string(),
            ));
        }
        if MissionBaseService::is_of_type(&mission, MissionType::ReturnMission) {
            return Err(OwgeError::InvalidInput(
                "can't cancel return missions".to_string(),
            ));
        }

        // The cancel + return-registration run under the planet lock superset, like
        // a mission run (returnMissionRegistrationBo re-points obtained units).
        let keys = Self::resolve_lock_keys(&mut conn, &mission).await?;
        run_locked(&mut conn, &keys, move |conn| {
            Box::pin(async move {
                sqlx::query("UPDATE missions SET resolved = 1 WHERE id = ?")
                    .bind(mission.id)
                    .execute(&mut *conn)
                    .await?;

                // customRequiredTime = requiredTime - secondsAlreadyFlown.
                let now_millis = Utc::now().timestamp_millis();
                let termination_millis = mission
                    .termination_date
                    .map(|d| d.and_utc().timestamp_millis())
                    .unwrap_or(now_millis);
                let duration_seconds = if termination_millis >= now_millis {
                    ((termination_millis - now_millis) as f64) / 1000.0
                } else {
                    0.0
                };
                let custom = mission.required_time.unwrap_or(0.0) - duration_seconds;
                crate::bo::return_mission_registration_bo::ReturnMissionRegistrationBo::register_return_mission(
                    conn, &mission, Some(custom),
                )
                .await
            })
        })
        .await?;
        drop(conn);

        // M4: cancel registers a RETURN mission whose user is the canceller, so
        // their running-mission list changed. Java's ReturnMissionRegistrationBo
        // emits emitLocalMissionChangeAfterCommit(returnMission); the dominant
        // effect for the canceller is unit_mission_change. (The return mission's
        // enemy-side emit toward the original source-planet owner is Tier-2.)
        crate::bo::MissionEventEmitter::emit_unit_missions(db, user_id).await?;
        Ok(())
    }

    // ---- runner ---------------------------------------------------------------------

    /// `runUnitMission` — execute a due unit mission under the planet lock
    /// superset on one pinned connection. Retries the whole locked section a few
    /// times on a lock-acquisition conflict (the Java `@Retryable`).
    pub async fn run_unit_mission(
        db: &Db,
        mission_id: u64,
        mission_type: MissionType,
    ) -> OwgeResult<()> {
        let mut last_err = None;
        for attempt in 1..=LOCK_RETRY_ATTEMPTS {
            match Self::run_unit_mission_once(db, mission_id, mission_type).await {
                Ok(()) => return Ok(()),
                Err(e) if is_lock_conflict(&e) && attempt < LOCK_RETRY_ATTEMPTS => {
                    last_err = Some(e);
                    // Small jittered backoff, matching @Backoff(delay=500, max=750).
                    let backoff = 500 + (attempt as u64 * 100);
                    tokio::time::sleep(std::time::Duration::from_millis(backoff)).await;
                }
                Err(e) => return Err(e),
            }
        }
        Err(last_err.unwrap_or_else(|| {
            OwgeError::Conflict(format!("Exhausted lock retries for mission {mission_id}"))
        }))
    }

    async fn run_unit_mission_once(
        db: &Db,
        mission_id: u64,
        mission_type: MissionType,
    ) -> OwgeResult<()> {
        let mut conn = db.acquire().await?;
        let mission = load_mission(&mut conn, mission_id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No mission with id {mission_id}")))?;
        let keys = Self::resolve_lock_keys(&mut conn, &mission).await?;

        let db_for_processors = db.clone();
        run_locked(&mut conn, &keys, move |conn| {
            Box::pin(async move {
                do_run_unit_mission(conn, &db_for_processors, &mission, mission_type).await
            })
        })
        .await
    }

    /// `resolvePlanetsToLock` — source/target planets plus every planet owned by
    /// each of their owners, as the lock-key superset.
    async fn resolve_lock_keys(
        conn: &mut MySqlConnection,
        mission: &Mission,
    ) -> OwgeResult<Vec<String>> {
        let mut planet_ids: BTreeSet<u64> = BTreeSet::new();
        for planet_id in [mission.source_planet, mission.target_planet]
            .into_iter()
            .flatten()
        {
            add_planet_and_owner_planets(conn, &mut planet_ids, planet_id as u64).await?;
        }
        Ok(planet_ids.into_iter().map(planet_lock_key).collect())
    }
}

/// `doRunUnitMission` — interception check, then process + report save (or full
/// interception handling).
///
/// The whole fire runs in ONE transaction opened on the pinned, already-locked
/// `conn`. The named planet locks were acquired before `BEGIN` and are session-
/// scoped (NOT released by `BEGIN`/`COMMIT`), so they remain held across the
/// transaction. A mid-fire failure (a processor error, a bad decode, …) rolls
/// every mutation back instead of leaving a half-processed mission — e.g. units
/// moved but the mission left unresolved, or a report saved without the unit
/// state that produced it. The poller then deletes the claimed `scheduled_tasks`
/// row and `retry_mission_if_possible` reschedules, so the next attempt runs
/// against clean, consistent state. This mirrors Java's
/// `@Transactional(READ_COMMITTED)` around `doRunUnitMission`.
///
/// Processors still take their autonomous read-only snapshots through the `db`
/// pool (separate connection); those are committed-data reads and need not be
/// part of this transaction.
async fn do_run_unit_mission(
    conn: &mut MySqlConnection,
    db: &Db,
    mission: &Mission,
    mission_type: MissionType,
) -> OwgeResult<()> {
    let mut tx = conn.begin().await?;
    // Post-commit websocket emits the processors schedule while running in the tx.
    let mut emits: Vec<crate::bo::mission_processor::DeferredEmit> = Vec::new();

    let interception =
        crate::bo::mission_interception_manager_bo::MissionInterceptionManagerBo::load_information(
            &mut tx,
            mission,
            mission_type,
        )
        .await?;

    if !interception.is_mission_intercepted {
        let report_builder = crate::bo::mission_processor::dispatch(
            &mut tx,
            mission,
            &interception.involved_units,
            db,
            &mut emits,
        )
        .await?;
        let (report_builder, interceptor_pairs) =
            crate::bo::mission_interception_manager_bo::MissionInterceptionManagerBo::maybe_append_data_to_mission_report(
                &mut tx,
                mission,
                report_builder,
                &interception,
            )
            .await?;
        // Per-interceptor reports (partial-interception path).
        for (uid, rid) in interceptor_pairs {
            emits.push(crate::bo::mission_processor::DeferredEmit::MissionReport {
                user_id: uid,
                report_id: rid,
            });
        }
        if let Some(report_builder) = report_builder {
            let (report_user_id, report_id) =
                crate::bo::mission_report_manager_bo::MissionReportManagerBo::handle_mission_report_save(
                    &mut tx,
                    mission,
                    report_builder,
                )
                .await?;
            emits.push(crate::bo::mission_processor::DeferredEmit::MissionReport {
                user_id: report_user_id,
                report_id,
            });
        }
    } else {
        let ((report_user_id, report_id), interceptor_pairs) =
            crate::bo::mission_interception_manager_bo::MissionInterceptionManagerBo::handle_mission_interception(
                &mut tx,
                mission,
                &interception,
            )
            .await?;
        emits.push(crate::bo::mission_processor::DeferredEmit::MissionReport {
            user_id: report_user_id,
            report_id,
        });
        for (uid, rid) in interceptor_pairs {
            emits.push(crate::bo::mission_processor::DeferredEmit::MissionReport {
                user_id: uid,
                report_id: rid,
            });
        }
        // missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission): the
        // mission was fully intercepted and resolved without combat, so the owner's
        // running-mission list must refresh (the ATTACK processor never ran, so
        // `emit_after_run` emits nothing for this type).
        if let Some(owner) = mission.user_id {
            emits.push(
                crate::bo::mission_processor::DeferredEmit::LocalMissionChange {
                    mission_id: mission.id,
                    user_id: owner,
                },
            );
        }
    }

    tx.commit().await?;

    // M4 emits, after commit. Per-processor in Java; replicated by type here.
    emit_after_run(db, mission, mission_type).await?;
    // Drain the processor-scheduled deferred emits (explore/gather/attack/conquest).
    for emit in &emits {
        emit.run(db).await?;
    }
    Ok(())
}

/// Post-commit websocket emits for a freshly-run unit mission, for the simple,
/// data-free per-type emit sets (the processors whose only post-commit action is
/// `emitLocalMissionChangeAfterCommit` + an obtained-units refresh):
/// RETURN/DEPLOY/ESTABLISH_BASE.
///
/// The data-dependent processors (EXPLORE `planet_explored_event`, GATHER
/// `mission_gather_result`, ATTACK/COUNTERATTACK per-user emit set, CONQUEST
/// new/old-owner planet+mission emits) schedule their own [`DeferredEmit`]s while
/// running inside the tx; those are drained by the caller after commit.
async fn emit_after_run(db: &Db, mission: &Mission, mission_type: MissionType) -> OwgeResult<()> {
    let Some(owner) = mission.user_id else {
        return Ok(());
    };
    use crate::bo::{MissionEventEmitter, ObtainedUnitEventEmitter};
    match mission_type {
        MissionType::ReturnMission => {
            MissionEventEmitter::emit_local_mission_change(db, mission.id, owner).await?;
            ObtainedUnitEventEmitter::emit_obtained_units(db, owner).await?;
        }
        MissionType::Deploy => {
            ObtainedUnitEventEmitter::emit_obtained_units(db, owner).await?;
            MissionEventEmitter::emit_local_mission_change(db, mission.id, owner).await?;
        }
        MissionType::EstablishBase => {
            MissionEventEmitter::emit_local_mission_change(db, mission.id, owner).await?;
        }
        // EXPLORE/GATHER/ATTACK/COUNTERATTACK/CONQUEST schedule their own
        // processor-specific emits via the DeferredEmit queue (drained by the
        // caller), faithfully mirroring each Java processor's post-commit block.
        _ => {}
    }
    Ok(())
}

/// `addPlanetAndOwnerPlanets` — add the planet itself, and if it has an owner,
/// every planet that owner owns.
async fn add_planet_and_owner_planets(
    conn: &mut MySqlConnection,
    target: &mut BTreeSet<u64>,
    planet_id: u64,
) -> OwgeResult<()> {
    target.insert(planet_id);
    let owner: Option<Option<i32>> = sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
        .bind(planet_id)
        .fetch_optional(&mut *conn)
        .await?;
    if let Some(Some(owner_id)) = owner {
        let owned: Vec<u64> = sqlx::query_scalar("SELECT id FROM planets WHERE owner = ?")
            .bind(owner_id)
            .fetch_all(&mut *conn)
            .await?;
        target.extend(owned);
    }
    Ok(())
}

/// Acquire `keys` on `conn`, run `body` on the now-locked pinned connection
/// (mirroring the Java `planetLockUtilService.doInsideLock`), then release the
/// locks whether the body succeeded or failed (the `doInsideLock` `finally`).
///
/// `body` is a boxed-future factory tied to a single `'e` lifetime covering both
/// the connection borrow and any captured environment (the mission / info /
/// user the caller threads in). The locked critical section runs on the same
/// session — the named locks stay held until [`crate::lock::release`].
///
/// TODO(M3): the Java path wraps this in a `@Transactional(READ_COMMITTED)` so
/// the locked mutations commit atomically. Here the work runs in autocommit on
/// the pinned connection; opening an explicit transaction on the same session
/// (so the named locks, taken before `BEGIN`, remain held) is a parity follow-up.
pub(crate) async fn run_locked<F, T>(
    conn: &mut MySqlConnection,
    keys: &[String],
    body: F,
) -> OwgeResult<T>
where
    F: for<'c> FnOnce(
        &'c mut MySqlConnection,
    ) -> std::pin::Pin<
        Box<dyn std::future::Future<Output = OwgeResult<T>> + Send + 'c>,
    >,
{
    lock::acquire(conn, keys).await?;
    let body_result = body(conn).await;
    // Release on the same session regardless of outcome.
    let _ = lock::release(conn, keys).await;
    body_result
}

/// True when this error is a lock-acquisition conflict (the Rust analogue of
/// `CannotAcquireLockException`, raised by `lock::acquire`).
fn is_lock_conflict(err: &OwgeError) -> bool {
    matches!(err, OwgeError::Conflict(msg) if msg.contains("MySQL user-level locks"))
}

// --- small DB helpers (mirror the planetRepository / planetExplorationService calls) ---

async fn is_of_user_property(db: &Db, user_id: i32, planet_id: i64) -> OwgeResult<bool> {
    let owner: Option<Option<i32>> = sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
        .bind(planet_id)
        .fetch_optional(db)
        .await?;
    Ok(matches!(owner, Some(Some(o)) if o == user_id))
}

async fn is_home_planet(db: &Db, planet_id: i64) -> OwgeResult<bool> {
    let home: Option<Option<i8>> = sqlx::query_scalar("SELECT home FROM planets WHERE id = ?")
        .bind(planet_id)
        .fetch_optional(db)
        .await?;
    Ok(matches!(home, Some(Some(h)) if h != 0))
}

/// `PlanetExplorationService.isExplored(userId, planetId)` — the planet is the
/// user's property, or has an `explored_planets` row for the user.
async fn is_explored(db: &Db, user_id: i32, planet_id: i64) -> OwgeResult<bool> {
    if is_of_user_property(db, user_id, planet_id).await? {
        return Ok(true);
    }
    let count: i64 =
        sqlx::query_scalar("SELECT COUNT(*) FROM explored_planets WHERE user = ? AND planet = ?")
            .bind(user_id)
            .bind(planet_id)
            .fetch_one(db)
            .await?;
    Ok(count > 0)
}

async fn load_mission(conn: &mut MySqlConnection, mission_id: u64) -> OwgeResult<Option<Mission>> {
    Ok(
        sqlx::query_as::<_, Mission>(crate::bo::mission_base_service_bo::SELECT_MISSION)
            .bind(mission_id)
            .fetch_optional(&mut *conn)
            .await?,
    )
}

/// The realization-job analogue: the [`MissionDispatch`] the scheduler poller
/// drives. Mirrors `DbSchedulerRealizationJob.execute` — load the mission, skip
/// if missing/resolved, branch BUILD_UNIT/LEVEL_UP to [`crate::bo::mission_bo`]
/// vs unit missions to [`UnitMissionBo::run_unit_mission`], and on any error run
/// the retry path. **Never** returns an error (the scheduler must always delete
/// the claimed row).
#[derive(Clone)]
pub struct MissionRunner {
    db: Db,
}

impl MissionRunner {
    pub fn new(db: Db) -> Self {
        Self { db }
    }

    async fn execute(&self, mission_id: u64) -> OwgeResult<()> {
        let mut conn = self.db.acquire().await?;
        let mission = match load_mission(&mut conn, mission_id).await? {
            Some(m) => m,
            None => {
                tracing::debug!("mission {mission_id} not found, nothing to run");
                return Ok(());
            }
        };
        drop(conn);

        if mission.is_resolved() {
            return Ok(());
        }
        let Some(mission_type) = mission.mission_type() else {
            tracing::warn!(
                "mission {mission_id} has unknown type {}, skipping",
                mission.type_id
            );
            return Ok(());
        };

        tracing::debug!(
            "Executing mission id {mission_id} of type {}",
            mission_type.code()
        );
        let result =
            if mission_type == MissionType::BuildUnit || mission_type == MissionType::LevelUp {
                self.run_non_unit_mission(mission_id, mission_type).await
            } else {
                UnitMissionBo::run_unit_mission(&self.db, mission_id, mission_type).await
            };

        if let Err(e) = result {
            tracing::error!("Unexpected fatal exception when executing mission {mission_id}: {e}");
            // missionBaseService.retryMissionIfPossible(missionId, missionType)
            if let Err(retry_err) =
                MissionBaseService::retry_mission_if_possible(&self.db, mission_id, mission_type)
                    .await
            {
                tracing::error!("retry handling for mission {mission_id} also failed: {retry_err}");
            }
            // No extra emit here: Java `MissionBaseService.retryMissionIfPossible`
            // fires no websocket events of its own — the only emit on the failure
            // path is the rescheduled error report's `save → emitOneToUser`, which
            // `retry_mission_if_possible` already performs after committing.
        }
        Ok(())
    }

    /// BUILD_UNIT / LEVEL_UP completion under the appropriate planet lock.
    ///
    /// BUILD_UNIT locks the build planet (`mission_information.value`); LEVEL_UP
    /// has no planet, so it runs without a planet lock (the Java
    /// `processLevelUpAnUpgrade` takes no `planetLockUtilService` lock).
    async fn run_non_unit_mission(
        &self,
        mission_id: u64,
        mission_type: MissionType,
    ) -> OwgeResult<()> {
        let mut conn = self.db.acquire().await?;
        // Capture the owner before the run — BUILD_UNIT/LEVEL_UP delete the mission
        // row on completion, so it can't be read back afterwards for the emit.
        let owner: Option<i32> = sqlx::query_scalar("SELECT user_id FROM missions WHERE id = ?")
            .bind(mission_id)
            .fetch_optional(&mut *conn)
            .await?;
        let keys: Vec<String> = if mission_type == MissionType::BuildUnit {
            let planet: Option<f64> =
                sqlx::query_scalar("SELECT value FROM mission_information WHERE mission_id = ?")
                    .bind(mission_id)
                    .fetch_optional(&mut *conn)
                    .await?;
            planet
                .map(|p| vec![planet_lock_key(p as u64)])
                .unwrap_or_default()
        } else {
            Vec::new()
        };

        let req_emits = run_locked(&mut conn, &keys, move |conn| {
            Box::pin(async move {
                crate::bo::mission_bo::MissionBo::run_mission(conn, mission_id, mission_type).await
            })
        })
        .await?;
        drop(conn);

        // Requirement-trigger unlock pushes from the completion (drained after the
        // tx commit + lock release, matching Java doAfterCommit semantics).
        crate::bo::realtime_emitter::drain_requirement_emits(&self.db, &req_emits).await?;

        // M4 completion emits (Java MissionBo.processBuildUnit / processLevelUp).
        if let Some(owner) = owner {
            use crate::bo::{MissionEventEmitter, ObtainedUnitEventEmitter};
            match mission_type {
                MissionType::BuildUnit => {
                    // clearSourceCache(user, obtainedUnitImprovementCalculationService):
                    // the completed units may carry improvements.
                    UserImprovementBo::evict_and_emit(&self.db, owner).await?;
                    MissionEventEmitter::emit_unit_build_change(&self.db, owner).await?;
                    MissionEventEmitter::emit_mission_count_change(&self.db, owner).await?;
                    ObtainedUnitEventEmitter::emit_obtained_units(&self.db, owner).await?;
                    UnitTypeEmitter::emit_unit_type_change(&self.db, owner).await?;
                }
                MissionType::LevelUp => {
                    // clearSourceCache(user, obtainedUpgradeBo): the upgrade level rose.
                    UserImprovementBo::evict_and_emit(&self.db, owner).await?;
                    MissionEventEmitter::emit_running_upgrade(&self.db, owner).await?;
                    MissionEventEmitter::emit_obtained_upgrades(&self.db, owner).await?;
                    MissionEventEmitter::emit_mission_count_change(&self.db, owner).await?;
                }
                _ => {}
            }
        }
        Ok(())
    }
}

#[async_trait]
impl MissionDispatch for MissionRunner {
    async fn run_mission(&self, mission_id: u64) {
        // execute never propagates: like DbSchedulerRealizationJob.execute, it
        // handles its own retry/give-up so the scheduler row is always cleared.
        if let Err(e) = self.execute(mission_id).await {
            tracing::error!("mission {mission_id} dispatch failed fatally: {e}");
        }
    }
}
