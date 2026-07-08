//! Port of the **temporal-units** feature — time-special-granted units that
//! auto-expire. Two Java collaborators are folded into this one Bo:
//!
//! * `TemporalUnitsListener.onTimeSpecialActivated`
//!   (`business.event.listener.timespecial`) — fires `BEFORE_COMMIT` of the
//!   `active_time_specials` insert: for every
//!   `TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS` rule on the activated special it
//!   grants the configured `count` of the rule's destination `UNIT` to the user,
//!   grouped by `duration`, recording an `obtained_unit_temporal_information` row
//!   per duration and a `UNIT_EXPIRED` scheduled task that removes them when the
//!   duration elapses. Ported as [`TemporalUnitsBo::grant_on_activation`], called
//!   from [`ActiveTimeSpecialBo::activate`](crate::bo::ActiveTimeSpecialBo) inside
//!   the activation transaction.
//!
//! * `TemporalUnitScheduleListener` (`business.schedule`) — the `UNIT_EXPIRED`
//!   task handler ([`TemporalUnitsBo::handle_unit_expired`]) and the
//!   `RequirementComplianceListener.relationLost` hook
//!   ([`TemporalUnitsBo::on_time_special_relation_lost`]) that force-removes a
//!   special's temporal units when its unlock relation is lost. Both delete the
//!   expired units under the same per-planet MySQL named locks the mission engine
//!   uses (`aggressiveLockAcquire`), then clean up any missions left unit-less and
//!   emit the obtained-unit / mission websocket deltas.
//!
//! **Divergence from Java, by design:**
//! * Resolve-planet uses the user's home planet. Java prefers
//!   `OwgeContextHolder.selectedPlanetId`, but the Rust activate route carries no
//!   selected-planet context, so the `.orElse(homePlanet)` fallback always applies.
//! * `maybeTriggerClearImprovement` is reproduced as an unconditional
//!   [`UserImprovementBo::evict_and_emit`](crate::bo::UserImprovementBo) on the
//!   affected user (the Rust port has no per-source improvement cache to scope).
//! * `aggressiveLockAcquire`'s unbounded self-recursion (re-lock when the planet
//!   set shifts under the lock) is a bounded retry loop here — same invariant
//!   (delete only while holding the locks for the units' actual planet set),
//!   without risking an unbounded stack.

use std::collections::BTreeMap;

use sqlx::{Connection, MySqlConnection};

use crate::bo::ObjectRelationBo;
use crate::error::{OwgeError, OwgeResult};
use crate::lock;
use crate::model::object_relation::object_enum;
use crate::model::rule::Rule;

/// The rule type that grants temporal units
/// (`TimeSpecialIsActiveTemporalUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS_ID`).
const TEMPORAL_UNITS_RULE: &str = "TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS";

/// `task_name` of the expiry task (`TemporalUnitScheduleListener.TASK_NAME`).
pub const UNIT_EXPIRED_TASK_NAME: &str = "UNIT_EXPIRED";

/// What to emit after the expiry/deletion transaction commits (the Java
/// `transactionUtilService.doAfterCommit` / `emitAfterCommit` calls). Collected
/// while the units are deleted under lock, drained by the caller post-commit.
#[derive(Default)]
struct ExpiryEmits {
    /// Units were actually deleted for this user → re-emit `unit_obtained_change`
    /// and evict the improvement cache.
    affected_user: Option<i32>,
    /// At least one running mission was touched → re-emit the owner's
    /// `unit_mission_change` + `missions_count_change`.
    missions_affected: bool,
    /// Owners of the target planets of the affected missions (excluding the
    /// temporal-unit owner) → re-emit each one's `enemy_mission_change`.
    enemy_users: Vec<i32>,
}

pub struct TemporalUnitsBo;

impl TemporalUnitsBo {
    // ── grant side (TemporalUnitsListener) ──────────────────────────────────

    /// `TemporalUnitsListener.onTimeSpecialActivated` → `handleRules`. Runs inside
    /// the caller's activation transaction (`conn`), so the granted units commit
    /// atomically with the `active_time_specials` row.
    ///
    /// For every `TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS` rule on `time_special_id`
    /// whose destination is an existing `UNIT` and whose `extra_args` are
    /// `[duration, count]`, grant `count` of that unit to `user_id`, grouping all
    /// units that share a `duration` under one
    /// `obtained_unit_temporal_information` row + one `UNIT_EXPIRED` task.
    ///
    /// Does not emit here: the surrounding [`ActiveTimeSpecialBo::activate`]
    /// already evicts the improvement cache and (via
    /// `emit_if_activation_affecting_units`, which matches every `TIME_SPECIAL →
    /// UNIT` rule) re-emits `unit_obtained_change` after commit, covering the
    /// listener's `maybeTriggerClearImprovement` + `emitObtainedUnits`.
    pub(crate) async fn grant_on_activation(
        conn: &mut MySqlConnection,
        user_id: i32,
        time_special_id: u16,
    ) -> OwgeResult<()> {
        let rules = Self::find_temporal_unit_rules(conn, time_special_id).await?;
        if rules.is_empty() {
            return Ok(());
        }

        // resolvePlanet: selected planet (unwired) else home planet.
        let home_planet: u64 =
            sqlx::query_scalar("SELECT home_planet FROM user_storage WHERE id = ?")
                .bind(user_id)
                .fetch_one(&mut *conn)
                .await?;

        // The object_relations id of the source TIME_SPECIAL (recorded on each
        // temporal-info row so relationLost can find them).
        let relation_id = ObjectRelationBo::find_one(
            conn,
            object_enum::TIME_SPECIAL,
            time_special_id as i16,
        )
        .await?
        .ok_or_else(|| {
            OwgeError::Common(format!(
                "No object relation for TIME_SPECIAL {time_special_id} while granting temporal units"
            ))
        })?;

        // Group (unit_id, count) by duration, mirroring Java's Map<Long, Set<OU>>.
        let mut by_duration: BTreeMap<u32, Vec<(u16, u64)>> = BTreeMap::new();
        for rule in rules {
            if rule.extra_args.len() != 2 || rule.destination_type != object_enum::UNIT {
                continue;
            }
            let unit_id = rule.destination_id;
            let exists: Option<u16> = sqlx::query_scalar("SELECT id FROM units WHERE id = ?")
                .bind(unit_id)
                .fetch_optional(&mut *conn)
                .await?;
            let Some(unit_id) = exists else {
                tracing::warn!(
                    "Unit with id {unit_id} doesn't exist for temporal-units rule {}",
                    rule.id
                );
                continue;
            };
            let (Ok(duration), Ok(count)) = (
                rule.extra_args[0].parse::<u32>(),
                rule.extra_args[1].parse::<u64>(),
            ) else {
                tracing::warn!(
                    "Non-numeric extra_args {:?} for temporal-units rule {}",
                    rule.extra_args,
                    rule.id
                );
                continue;
            };
            by_duration
                .entry(duration)
                .or_default()
                .push((unit_id, count));
        }

        for (duration, units) in by_duration {
            let now = chrono::Utc::now().naive_utc();
            let expiration = now + chrono::Duration::seconds(duration as i64);
            let result = sqlx::query(
                "INSERT INTO obtained_unit_temporal_information (duration, expiration, relation_id) \
                 VALUES (?, ?, ?)",
            )
            .bind(duration)
            .bind(expiration)
            .bind(relation_id)
            .execute(&mut *conn)
            .await?;
            let temporal_id = result.last_insert_id();

            for (unit_id, count) in units {
                sqlx::query(
                    "INSERT INTO obtained_units \
                         (user_id, unit_id, count, source_planet, is_from_capture, expiration_id) \
                     VALUES (?, ?, ?, ?, 0, ?)",
                )
                .bind(user_id)
                .bind(unit_id)
                .bind(count)
                .bind(home_planet)
                .bind(temporal_id)
                .execute(&mut *conn)
                .await?;
            }

            Self::schedule_unit_expired(conn, temporal_id, duration).await?;
        }
        Ok(())
    }

    /// `ruleBo.findByOriginTypeAndOriginIdAndType(TIME_SPECIAL, id, TEMPORAL_UNITS)`.
    /// Returns the parsed [`RuleDto`](crate::dto::rule::RuleDto)s (extra_args split
    /// on `#`).
    ///
    /// Public because `owge-wiki-gen` reuses it to list the units a time
    /// special grants temporarily, with the exact activation-time rule lookup.
    pub async fn find_temporal_unit_rules(
        conn: &mut MySqlConnection,
        time_special_id: u16,
    ) -> OwgeResult<Vec<crate::dto::rule::RuleDto>> {
        let rows = sqlx::query_as::<_, Rule>(
            "SELECT id, type, origin_type, origin_id, destination_type, destination_id, extra_args \
             FROM rules \
             WHERE origin_type = ? AND origin_id = ? AND type = ? ORDER BY id",
        )
        .bind(object_enum::TIME_SPECIAL)
        .bind(time_special_id as i16)
        .bind(TEMPORAL_UNITS_RULE)
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// Insert (or refresh) the `UNIT_EXPIRED` scheduled task firing `duration`
    /// seconds out, `task_instance` = the temporal-info id. Same db-scheduler row
    /// shape as the mission / time-special tasks.
    async fn schedule_unit_expired(
        conn: &mut MySqlConnection,
        temporal_id: u64,
        duration_seconds: u32,
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
        .bind(UNIT_EXPIRED_TASK_NAME)
        .bind(temporal_id.to_string())
        .bind(duration_seconds as i64)
        .bind(duration_seconds as i64)
        .execute(&mut *conn)
        .await?;
        Ok(())
    }

    // ── expiry side (TemporalUnitScheduleListener) ──────────────────────────

    /// `UNIT_EXPIRED` task handler. `expiration_id` is the
    /// `obtained_unit_temporal_information` id. No-op if the row is already gone
    /// (`existsById` guard), else delete the units under their planet locks and
    /// fire the post-commit emits.
    pub async fn handle_unit_expired(
        conn: &mut MySqlConnection,
        expiration_id: u32,
    ) -> OwgeResult<()> {
        let exists: Option<u32> =
            sqlx::query_scalar("SELECT id FROM obtained_unit_temporal_information WHERE id = ?")
                .bind(expiration_id)
                .fetch_optional(&mut *conn)
                .await?;
        if exists.is_none() {
            return Ok(());
        }
        Self::aggressive_lock_and_delete(conn, expiration_id).await
    }

    /// `TemporalUnitScheduleListener.relationLost` (listener #2): a lost
    /// `TIME_SPECIAL` unlock relation force-removes that special's temporal units.
    /// Java path: find the special's `TEMPORAL_UNITS` rule → its object relation →
    /// the `obtained_unit_temporal_information` rows by `relation_id` →
    /// `deleteOnDemand` each.
    ///
    /// `relation_id` is the `object_relations` id of the lost `TIME_SPECIAL`
    /// relation (already known by the caller in the requirement engine), so this
    /// skips Java's rule→relation re-derivation and looks the temporal rows up
    /// directly. Runs after the caller's transaction has committed (it locks +
    /// opens its own transactions per expiry), so it is invoked from the
    /// post-commit emit drain, not inside the requirement-engine transaction.
    pub async fn on_time_special_relation_lost(
        conn: &mut MySqlConnection,
        relation_id: u16,
    ) -> OwgeResult<()> {
        let ids: Vec<u32> = sqlx::query_scalar(
            "SELECT id FROM obtained_unit_temporal_information WHERE relation_id = ?",
        )
        .bind(relation_id)
        .fetch_all(&mut *conn)
        .await?;
        for expiration_id in ids {
            Self::aggressive_lock_and_delete(&mut *conn, expiration_id).await?;
        }
        Ok(())
    }

    /// `aggressiveLockAcquire(expirationId, doDeleteExpiredOrOnDemand)`. Acquire the
    /// planet locks for the units' current planet set, re-read it under the lock,
    /// and only delete if it is unchanged (else release + retry with the new set).
    /// No-op when the units are on no planet (Java's `if (!planetIds.isEmpty())`
    /// guard — leaves the temporal-info row, matching Java).
    async fn aggressive_lock_and_delete(
        conn: &mut MySqlConnection,
        expiration_id: u32,
    ) -> OwgeResult<()> {
        for _ in 0..lock::MAX_LOCK_ATTEMPTS {
            let planet_ids = Self::find_planet_ids_on_conn(&mut *conn, expiration_id).await?;
            if planet_ids.is_empty() {
                return Ok(());
            }
            let keys: Vec<String> = planet_ids
                .iter()
                .map(|&id| lock::planet_lock_key(id))
                .collect();

            let outcome = crate::bo::unit_mission_bo::run_locked(conn, &keys, {
                let expected = planet_ids.clone();
                move |conn| {
                    Box::pin(async move {
                        // Re-read under lock; only proceed if the planet set held.
                        let inner = Self::find_planet_ids_on_conn(conn, expiration_id).await?;
                        if inner == expected {
                            Ok(Some(Self::do_delete_expired(conn, expiration_id).await?))
                        } else {
                            Ok(None)
                        }
                    })
                }
            })
            .await?;

            if let Some(emits) = outcome {
                Self::drain_expiry_emits(conn, emits).await?;
                return Ok(());
            }
            // Planet set shifted under the lock; retry with the fresh set.
        }
        Err(OwgeError::Conflict(format!(
            "Could not stably lock planets for temporal-unit expiry {expiration_id}"
        )))
    }

    /// `doDeleteExpiredOrOnDemand` — delete the units, the temporal-info row, and
    /// any now-unit-less missions, all in one transaction on the locked `conn`;
    /// return what to emit after commit. (`maybeTriggerClearImprovement` +
    /// `emitObtainedUnitsAfterCommit` + `handleAffectedMissions`.)
    async fn do_delete_expired(
        conn: &mut MySqlConnection,
        expiration_id: u32,
    ) -> OwgeResult<ExpiryEmits> {
        // findByExpirationId: the stacks to remove, with their user + mission.
        let ous: Vec<(u64, i32, Option<u64>)> = sqlx::query_as(
            "SELECT id, user_id, mission_id FROM obtained_units WHERE expiration_id = ?",
        )
        .bind(expiration_id)
        .fetch_all(&mut *conn)
        .await?;

        let mut tx = conn.begin().await?;
        let mut emits = ExpiryEmits::default();

        if !ous.is_empty() {
            let user_id = ous[0].1;
            emits.affected_user = Some(user_id);

            sqlx::query("DELETE FROM obtained_units WHERE expiration_id = ?")
                .bind(expiration_id)
                .execute(&mut *tx)
                .await?;

            // handleAffectedMissions: the distinct missions the deleted units were
            // part of.
            let mission_ids: Vec<u64> = {
                let mut v: Vec<u64> = ous.iter().filter_map(|&(_, _, m)| m).collect();
                v.sort_unstable();
                v.dedup();
                v
            };
            if !mission_ids.is_empty() {
                emits.missions_affected = true;
                // usersOwningPlanetsOfTargetMissions(owner, missions): enemy owners
                // of the missions' target planets (excluding the unit owner).
                emits.enemy_users =
                    Self::enemy_owners_of_missions(&mut tx, user_id, &mission_ids).await?;
                // deleteNonUnitsLeftMissions: drop missions that now have no units.
                for mission_id in &mission_ids {
                    let still_used: Option<u64> = sqlx::query_scalar(
                        "SELECT id FROM obtained_units WHERE mission_id = ? LIMIT 1",
                    )
                    .bind(mission_id)
                    .fetch_optional(&mut *tx)
                    .await?;
                    if still_used.is_none() {
                        sqlx::query("DELETE FROM missions WHERE id = ?")
                            .bind(mission_id)
                            .execute(&mut *tx)
                            .await?;
                    }
                }
            }
        }

        // deleteById(expirationId) — always, after the units (Java orders it last).
        sqlx::query("DELETE FROM obtained_unit_temporal_information WHERE id = ?")
            .bind(expiration_id)
            .execute(&mut *tx)
            .await?;

        tx.commit().await?;
        Ok(emits)
    }

    /// `usersOwningPlanetsOfTargetMissions` — distinct owners of the target planets
    /// of `mission_ids`, excluding `owner_id` (and null owners).
    async fn enemy_owners_of_missions(
        conn: &mut MySqlConnection,
        owner_id: i32,
        mission_ids: &[u64],
    ) -> OwgeResult<Vec<i32>> {
        let placeholders = std::iter::repeat_n("?", mission_ids.len())
            .collect::<Vec<_>>()
            .join(", ");
        let sql = format!(
            "SELECT DISTINCT p.owner FROM missions m \
               JOIN planets p ON p.id = m.target_planet \
              WHERE m.id IN ({placeholders}) AND p.owner IS NOT NULL AND p.owner <> ?"
        );
        let mut q = sqlx::query_scalar::<_, i32>(&sql);
        for id in mission_ids {
            q = q.bind(id);
        }
        q = q.bind(owner_id);
        Ok(q.fetch_all(&mut *conn).await?)
    }

    /// Fire the post-commit websocket emits collected by [`do_delete_expired`].
    async fn drain_expiry_emits(conn: &mut MySqlConnection, emits: ExpiryEmits) -> OwgeResult<()> {
        if let Some(user_id) = emits.affected_user {
            // maybeTriggerClearImprovement + emitObtainedUnitsAfterCommit.
            crate::bo::UserImprovementBo::evict_and_emit(&mut *conn, user_id).await?;
            crate::bo::ObtainedUnitEventEmitter::emit_obtained_units(&mut *conn, user_id).await?;
            if emits.missions_affected {
                crate::bo::MissionEventEmitter::emit_unit_missions(&mut *conn, user_id).await?;
                crate::bo::MissionEventEmitter::emit_mission_count_change(&mut *conn, user_id)
                    .await?;
            }
        }
        for enemy_id in emits.enemy_users {
            crate::bo::MissionEventEmitter::emit_enemy_missions_change(&mut *conn, enemy_id)
                .await?;
        }
        Ok(())
    }

    /// Same query as `find_planet_ids_by_expiration` but on the provided
    /// connection (the under-lock re-read in `aggressiveLockAcquire` and the
    /// initial pre-lock read).
    async fn find_planet_ids_on_conn(
        conn: &mut MySqlConnection,
        expiration_id: u32,
    ) -> OwgeResult<Vec<u64>> {
        let ids = sqlx::query_scalar::<_, u64>(
            "SELECT DISTINCT source_planet FROM obtained_units \
              WHERE expiration_id = ? AND source_planet IS NOT NULL \
             UNION \
             SELECT DISTINCT target_planet FROM obtained_units \
              WHERE expiration_id = ? AND target_planet IS NOT NULL",
        )
        .bind(expiration_id)
        .bind(expiration_id)
        .fetch_all(&mut *conn)
        .await?;
        Self::sorted_unique(ids)
    }

    fn sorted_unique(mut ids: Vec<u64>) -> OwgeResult<Vec<u64>> {
        ids.sort_unstable();
        ids.dedup();
        Ok(ids)
    }
}
