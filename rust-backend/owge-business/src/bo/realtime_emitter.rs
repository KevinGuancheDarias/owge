//! Realtime-emit wrappers for requirement/unlock, time-special, planet, and
//! one-time / broadcast events.
//!
//! These are the Rust ports of:
//! * `RequirementInternalEventEmitterService` (unit_unlocked_change,
//!   unit_requirements_change, speed_impact_group_unlocked_change)
//! * `ImprovementBo.clearSourceCache` / `emitUserImprovement` side-effects
//!   (time_special_change indirectly via requirement triggers)
//! * `PlanetBo` / `PlanetListBo` emit hooks (planet_owned_change,
//!   planet_user_list_change)
//! * `SocketIoService` miscellaneous one-time / broadcast events:
//!   twitch_state_change, planet_explored_event, mission_gather_result,
//!   account_deleted, warn_message
//!
//! All `emit_*` wrappers follow the canonical shape (see M4-CONTRACTS.md):
//! ```text
//! emitter::send_message(conn, user_id, "<event>", |conn| Box::pin(async move { Ok(to_value(finder)?) })).await
//! ```
//! They must be called **after** the surrounding DB transaction commits.

use crate::bo::unlocked::unlocked_unit_finder::UnlockedUnitFinder;
use crate::error::OwgeResult;
use crate::websocket::emitter;
use serde_json::Value;
use sqlx::MySqlConnection;

// ─── requirement / unlock emitters ──────────────────────────────────────────

/// Emits `unit_unlocked_change` — the units currently unlocked for `user_id`.
///
/// Java: every `RequirementComplianceListener` implementation that listens on
/// the `unit_unlocked_change` event calls
/// `socketIoService.sendMessage(userId, "unit_unlocked_change", () -> unitBo.findAllByUser(userId))`.
/// Rust finder: `UnitBo::find_unlocked_by_user`.
pub async fn emit_unit_unlocked_change(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "unit_unlocked_change", |conn| {
        Box::pin(async move {
            Ok(serde_json::to_value(
                UnlockedUnitFinder::find_unlocked_by_user(&mut *conn, user_id).await?,
            )?)
        })
    })
    .await
}

/// Emits `unit_requirements_change` — the faction-unit-level requirements for
/// `user_id`.
///
/// Java: `socketIoService.sendMessage(userId, "unit_requirements_change", () ->
/// requirementBo.findFactionUnitLevelRequirements(factionBo.findByUser(userId)))`.
/// Rust finder: `RequirementBo::find_faction_unit_level_requirements`.
pub async fn emit_unit_requirements_change(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "unit_requirements_change", |conn| {
        Box::pin(async move {
            Ok(serde_json::to_value(
                crate::bo::RequirementBo::find_faction_unit_level_requirements(&mut *conn, user_id)
                    .await?,
            )?)
        })
    })
    .await
}

/// Emits `speed_impact_group_unlocked_change` — the speed-impact groups
/// unlocked for `user_id`.
///
/// Java: `socketIoService.sendMessage(userId, "speed_impact_group_unlocked_change",
/// () -> unlockedSpeedImpactGroupService.findCrossGalaxyUnlocked(userId))`.
/// Rust finder: `SpeedImpactGroupBo::find_cross_galaxy_unlocked`.
pub async fn emit_speed_impact_group_unlocked_change(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<()> {
    emitter::send_message(
        conn,
        user_id,
        "speed_impact_group_unlocked_change",
        |conn| {
            Box::pin(async move {
                Ok(serde_json::to_value(
                    crate::bo::SpeedImpactGroupBo::find_cross_galaxy_unlocked(&mut *conn, user_id)
                        .await?,
                )?)
            })
        },
    )
    .await
}

/// Emits `time_special_unlocked_change` — the time specials currently unlocked
/// for `user_id`.
///
/// Java: `RequirementBo.emitUnlockedChange` for a `TIME_SPECIAL` relation →
/// `socketIoService.sendMessage(userId, "time_special_unlocked_change",
/// () -> unlockableTimeSpecialService.findUnlocked(userId))`.
/// Rust finder: `TimeSpecialBo::find_unlocked_dtos`. The frontend routes both
/// `time_special_change` and `time_special_unlocked_change` through the same
/// handler, so the payload carries the per-user activation status (a superset of
/// Java's plain `dtoFromEntity`, which the frontend recomputes anyway).
pub async fn emit_time_special_unlocked_change(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "time_special_unlocked_change", |conn| {
        Box::pin(async move {
            Ok(serde_json::to_value(
                crate::bo::TimeSpecialBo::find_unlocked_dtos(&mut *conn, user_id).await?,
            )?)
        })
    })
    .await
}

/// Emits `user_improvements_change` — the user's recomputed improvement aggregate.
///
/// Java: `ImprovementBo.clearSourceCache` / `emitUserImprovement` →
/// `socketIoService.sendMessage(user, "user_improvements_change",
/// () -> findUserImprovement(user))`. The finder is cached, so this is cheap; it
/// must be called **after** the matching cache eviction (see
/// [`UserImprovementBo::evict_and_emit`](crate::bo::UserImprovementBo::evict_and_emit)).
pub async fn emit_user_improvements(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "user_improvements_change", |conn| {
        Box::pin(async move {
            // Same GroupedImprovement Java serializes on user_data_change (id +
            // catalog unitType per entry — not a slim shape), except the socket
            // path additionally carries each unitType's own speedImpactGroup
            // (lazy-init path-dependence; see find_catalog_by_id_for_socket_aggregate).
            let response = crate::bo::UserImprovementBo::find_user_improvement_response_for_socket(
                &mut *conn, user_id,
            )
            .await?;
            Ok(serde_json::to_value(response)?)
        })
    })
    .await
}

// ─── requirement-trigger post-commit collector ───────────────────────────────

/// A websocket emit the requirement-trigger engine
/// (`requirement_engine` / `requirement_bo`) schedules while running inside the
/// caller's transaction, to be fired AFTER that transaction commits.
///
/// Mirrors `RequirementBo.registerObtainedRelation` / `unregisterLostRelation`,
/// whose `emitUnlockedChange` / `emitUnlockedSpeedImpactGroups` push happens via
/// `transactionUtilService.doAfterCommit(...)`. The engine cannot observe its own
/// uncommitted writes through the emit finders (which read via the connection), so
/// it records what to emit and the call site drains it once committed.
///
/// The three object types Java's `registerObtainedRelation`/`unregisterLostRelation`
/// emit for (`UPGRADE` and `REQUIREMENT_GROUP` produce no socket push), plus the
/// post-commit side effects of the **time-special deactivation cascade**: when a
/// lost `TIME_SPECIAL` relation force-deactivates an ACTIVE special
/// (`doNotifyLostRelation` → `ActiveTimeSpecialBo.relationLost` → `deactivate`),
/// that `deactivate` queues its own `time_special_change` / improvement-cache /
/// affecting-units emits here so they fire after the outer transaction commits.
#[derive(Clone, Copy, PartialEq, Eq, Hash, Debug)]
pub enum RequirementEmit {
    /// `unit_unlocked_change` for `user_id` (UNIT relation (un)locked).
    UnitUnlocked(i32),
    /// `time_special_unlocked_change` for `user_id` (TIME_SPECIAL relation (un)locked).
    TimeSpecialUnlocked(i32),
    /// `speed_impact_group_unlocked_change` for `user_id` (SPEED_IMPACT_GROUP (un)locked).
    SpeedImpactGroupUnlocked(i32),
    /// `time_special_change` for `user_id` (a special's state changed — used by the
    /// deactivation cascade; mirrors `ActiveTimeSpecialBo.emitTimeSpecialChange`).
    TimeSpecialChange(i32),
    /// `improvementBo.clearSourceCache` — evict the user's improvement cache and
    /// re-emit `user_improvements_change` (a deactivated special drops a source).
    ImprovementCache(i32),
    /// `emitIfActivationAffectingUnits` — re-emit `unit_obtained_change` when the
    /// (de)activated special has a `TIME_SPECIAL → UNIT/UNIT_TYPE` rule.
    TimeSpecialAffectingUnits { user_id: i32, time_special_id: u16 },
    /// `TemporalUnitScheduleListener.relationLost` (listener #2): a lost
    /// `TIME_SPECIAL` relation force-removes that special's temporal units. The
    /// `u16` is the lost `object_relations` id (`relation_id` on the
    /// `obtained_unit_temporal_information` rows). Deferred to post-commit because
    /// the removal pins its own connection and locks the units' planets.
    TemporalUnitsRelationLost(u16),
}

impl RequirementEmit {
    /// Fire this single emit against the (now-committed) DB.
    pub async fn run(self, conn: &mut MySqlConnection) -> OwgeResult<()> {
        match self {
            RequirementEmit::UnitUnlocked(u) => emit_unit_unlocked_change(conn, u).await,
            RequirementEmit::TimeSpecialUnlocked(u) => {
                emit_time_special_unlocked_change(conn, u).await
            }
            RequirementEmit::SpeedImpactGroupUnlocked(u) => {
                emit_speed_impact_group_unlocked_change(conn, u).await
            }
            RequirementEmit::TimeSpecialChange(u) => emit_time_special_change(conn, u).await,
            RequirementEmit::ImprovementCache(u) => {
                crate::bo::UserImprovementBo::evict_and_emit(conn, u).await
            }
            RequirementEmit::TimeSpecialAffectingUnits {
                user_id,
                time_special_id,
            } => {
                crate::bo::ActiveTimeSpecialBo::emit_if_activation_affecting_units(
                    conn,
                    user_id,
                    time_special_id,
                )
                .await
            }
            RequirementEmit::TemporalUnitsRelationLost(relation_id) => {
                crate::bo::TemporalUnitsBo::on_time_special_relation_lost(conn, relation_id).await
            }
        }
    }
}

/// Drain a batch of [`RequirementEmit`]s after the producing transaction has
/// committed. Java schedules one `doAfterCommit` callback per (un)locked
/// relation, so several relations of the same kind would push the same full-list
/// payload repeatedly; since the payload is an idempotent snapshot, we emit each
/// distinct `(event, user)` once (same final client state, fewer messages).
pub async fn drain_requirement_emits(
    conn: &mut MySqlConnection,
    emits: &[RequirementEmit],
) -> OwgeResult<()> {
    let mut seen = std::collections::HashSet::new();
    for emit in emits {
        if seen.insert(*emit) {
            emit.run(&mut *conn).await?;
        }
    }
    Ok(())
}

// ─── time-special emitter ────────────────────────────────────────────────────

/// Emits `time_special_change` — the user's time-special status list.
///
/// Java: `socketIoService.sendMessage(userId, "time_special_change",
/// () -> activeTimeSpecialBo.findByUserWithCurrentStatus(user))`.
/// Rust finder: `TimeSpecialBo::find_user_status_dtos`.
pub async fn emit_time_special_change(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "time_special_change", |conn| {
        Box::pin(async move {
            Ok(serde_json::to_value(
                crate::bo::TimeSpecialBo::find_user_status_dtos(&mut *conn, user_id).await?,
            )?)
        })
    })
    .await
}

// ─── planet emitters ─────────────────────────────────────────────────────────

/// Emits `planet_owned_change` — the planets owned by `user_id`.
///
/// Java: `socketIoService.sendMessage(userId, "planet_owned_change",
/// () -> planetBo.toDto(planetRepository.findByOwnerId(userId)))`.
/// Rust finder: `PlanetBo::find_owned_dtos`.
pub async fn emit_planet_owned_change(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "planet_owned_change", |conn| {
        Box::pin(async move {
            Ok(serde_json::to_value(
                crate::bo::PlanetBo::find_owned_dtos(&mut *conn, user_id).await?,
            )?)
        })
    })
    .await
}

/// Emits `planet_user_list_change` — the user's saved/named planet list.
///
/// Java: `socketIoService.sendMessage(userId, "planet_user_list_change",
/// () -> planetListBo.findByUserId(userId))`.
/// Rust finder: `PlanetListBo::find_by_user_id`.
pub async fn emit_planet_user_list_change(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "planet_user_list_change", |conn| {
        Box::pin(async move {
            Ok(serde_json::to_value(
                crate::bo::PlanetListBo::find_by_user_id(&mut *conn, user_id).await?,
            )?)
        })
    })
    .await
}

// ─── broadcast ───────────────────────────────────────────────────────────────

/// Broadcasts `twitch_state_change` to every connected client (target user 0).
///
/// Java: `socketIoService.sendMessage(null, "twitch_state_change", () -> statusJson)`.
/// The `null` user resolves to `sendMessage(0, …)` in Java, which broadcasts.
/// `status_json` is the already-serialised Twitch state value.
pub async fn emit_twitch_state_change(
    conn: &mut MySqlConnection,
    status_json: Value,
) -> OwgeResult<()> {
    emitter::send_message(conn, 0, "twitch_state_change", |_conn| {
        Box::pin(async move { Ok(status_json) })
    })
    .await
}

// ─── one-time events (no watermark) ──────────────────────────────────────────

/// Sends `planet_explored_event` (watermarked — the frame carries `lastSent`).
///
/// Java: `PlanetExplorationService.defineAsExplored` →
/// `socketIoService.sendMessage(user, "planet_explored_event", () -> planetDto)`
/// — the regular watermarked `sendMessage`, NOT `sendOneTimeMessage` (that one
/// is only used for `account_deleted`).
/// `planet_json` is the serialised `PlanetDto` of the newly-explored planet.
pub async fn send_planet_explored_event(
    conn: &mut MySqlConnection,
    user_id: i32,
    planet_json: Value,
) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "planet_explored_event", |_conn| {
        Box::pin(async move { Ok(planet_json) })
    })
    .await
}

/// Sends `mission_gather_result` (watermarked — the frame carries `lastSent`).
///
/// Java: `GatherMissionProcessor` →
/// `socketIoService.sendMessage(user, "mission_gather_result", () -> resultJson)`
/// (after commit) — the regular watermarked `sendMessage`.
/// `result_json` is the serialised gather-mission result object.
pub async fn send_gather_result(
    conn: &mut MySqlConnection,
    user_id: i32,
    result_json: Value,
) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "mission_gather_result", |_conn| {
        Box::pin(async move { Ok(result_json) })
    })
    .await
}

/// Sends `account_deleted` as a one-time push with a null payload (no watermark).
///
/// Java: `socketIoService.sendOneTimeMessage(userId, "account_deleted", () -> null)`.
pub async fn send_account_deleted(user_id: i32) -> OwgeResult<()> {
    emitter::send_one_time_message(user_id, "account_deleted", Value::Null).await
}

// ─── warning message ─────────────────────────────────────────────────────────

/// Emits `warn_message` to `user_id` with the given i18n text string.
///
/// Java: `SocketIoService.sendWarning(user, i18nWarningText)` ->
/// `sendMessage(user, "warn_message", () -> i18nWarningText)`.
/// Uses `send_message` (not one-time) so the warning is persisted in the
/// watermark table and delivered to the client on reconnect.
pub async fn send_warning(
    conn: &mut MySqlConnection,
    user_id: i32,
    i18n_text: &str,
) -> OwgeResult<()> {
    let text = i18n_text.to_owned();
    emitter::send_message(conn, user_id, "warn_message", |_conn| {
        Box::pin(async move { Ok(Value::String(text)) })
    })
    .await
}

// ─── mission-report emitters ─────────────────────────────────────────────────

/// Emits `mission_report_new` (`MissionReportBo.EMIT_NEW`) — the freshly-saved
/// report DTO, to its owning user.
///
/// Java: `MissionReportBo.emitOneToUser` →
/// `socketIoService.sendMessage(user, "mission_report_new", () -> toDto(report))`.
/// Uses `send_message` (with watermark), matching Java's `sendMessage(UserStorage, …)`.
pub async fn emit_mission_report_new(
    conn: &mut MySqlConnection,
    user_id: i32,
    report_id: u64,
) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "mission_report_new", |conn| {
        Box::pin(async move {
            Ok(serde_json::to_value(
                crate::bo::MissionReportBo::find_by_id(&mut *conn, report_id).await?,
            )?)
        })
    })
    .await
}

/// Emits `mission_report_count_change` (`MissionReportBo.EMIT_COUNT_CHANGE`) —
/// the per-user / per-enemy unread counts.
///
/// Java: `MissionReportBo.emitCountChange` →
/// `socketIoService.sendMessage(userId, "mission_report_count_change",
/// () -> findUnreadCount(userId, null))`. `findUnreadCount(userId, null)`
/// returns a fresh `MissionReportResponse` with only the two counts set, so the
/// wire payload (Jackson `NON_NULL`) is `{page:0, userUnread, enemyUnread,
/// requiresFlush:false}` (`reports` is null → omitted).
pub async fn emit_mission_report_count_change(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<()> {
    emitter::send_message(conn, user_id, "mission_report_count_change", |conn| {
        Box::pin(async move {
            let (user_unread, enemy_unread) =
                crate::bo::MissionReportBo::find_unread_count(&mut *conn, user_id).await?;
            Ok(serde_json::json!({
                "page": 0,
                "userUnread": user_unread,
                "enemyUnread": enemy_unread,
                "requiresFlush": false,
            }))
        })
    })
    .await
}
