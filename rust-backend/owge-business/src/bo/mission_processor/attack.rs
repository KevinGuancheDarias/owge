//! Port of `AttackMissionProcessor`
//! (`...business.mission.processor.AttackMissionProcessor`).
//!
//! Runs the combat core ([`AttackMissionManagerBo::process_attack`]), saves the
//! attack report(s), optionally registers the survivors' return mission, and
//! exposes the helpers the gather/establish-base/conquest processors lean on
//! (`triggerAttackIfRequired`, `processAttack`).
//!
//! The combat manager owns `AttackInformation` (its shape is private); the
//! processor consumes the `attackInformation` JSON it returns and derives the
//! two facts the surrounding processors need that are not in the JSON —
//! whether the attack mission was wiped out (`removed`) — from the persisted
//! state (the attack mission has no `obtained_units` left).

use serde_json::Value;
use sqlx::MySqlConnection;

use crate::bo::attack_mission_manager_bo::AttackMissionManagerBo;
use crate::bo::mission_report_manager_bo::MissionReportManagerBo;
use crate::bo::return_mission_registration_bo::ReturnMissionRegistrationBo;
use crate::builder::UnitMissionReportBuilder;
use crate::error::OwgeResult;
use crate::model::mission::{Mission, MissionType};
use crate::model::obtained_unit::ObtainedUnit;

use super::create_report_base;

/// The outcome of `AttackMissionProcessor.processAttack`, reduced to what the
/// callers consume: the `attackInformation` JSON (also embedded in the report),
/// whether the attack mission itself was removed, and the built report builder.
pub struct AttackOutcome {
    pub attack_information: Value,
    pub removed: bool,
    pub report_builder: UnitMissionReportBuilder,
}

impl AttackOutcome {
    /// Whether `user_id` has any surviving unit (`finalCount > 0`) in the result.
    pub fn user_has_survivors(&self, user_id: i32) -> bool {
        self.users_iter()
            .find(|u| u["userInfo"]["id"].as_i64() == Some(user_id as i64))
            .map(|u| {
                u["units"].as_array().is_some_and(|units| {
                    units
                        .iter()
                        .any(|unit| unit["finalCount"].as_i64().unwrap_or(0) > 0)
                })
            })
            .unwrap_or(false)
    }

    /// Whether `user_id` participated in the battle at all.
    pub fn contains_user(&self, user_id: i32) -> bool {
        self.users_iter()
            .any(|u| u["userInfo"]["id"].as_i64() == Some(user_id as i64))
    }

    /// The set of (user_id, alliance_id) survivors, used for the alliance-defeated
    /// check. `alliance_id` is read per-user from the DB by the caller, so this
    /// only exposes the per-user survivor flag.
    fn users_iter(&self) -> impl Iterator<Item = &Value> {
        self.attack_information
            .as_array()
            .map(|a| a.iter())
            .into_iter()
            .flatten()
    }

    /// The participating user ids (for the alliance-defeated / report fan-out).
    pub fn participating_user_ids(&self) -> Vec<i32> {
        self.users_iter()
            .filter_map(|u| u["userInfo"]["id"].as_i64().map(|v| v as i32))
            .collect()
    }
}

/// `process(mission, involvedUnits)` — `processAttack(mission, true, false)`.
pub async fn process(
    conn: &mut MySqlConnection,
    mission: &Mission,
    _involved_units: &[ObtainedUnit],
    emits: &mut Vec<super::DeferredEmit>,
) -> OwgeResult<Option<UnitMissionReportBuilder>> {
    let outcome = process_attack(
        conn, mission, /* survivors_do_return = */ true,
        /* is_triggered_by_event = */ false, emits,
    )
    .await?;
    Ok(Some(outcome.report_builder))
}

/// `triggerAttackIfRequired(mission, user, targetPlanet)` — when the mission type
/// has attack-on-arrival enabled and enemy units are involved at the target, run
/// an event-triggered attack. Returns `true` if the mission should continue
/// (i.e. the attack did not wipe out the mission).
pub async fn trigger_attack_if_required(
    conn: &mut MySqlConnection,
    mission: &Mission,
    mission_type: MissionType,
    emits: &mut Vec<super::DeferredEmit>,
) -> OwgeResult<bool> {
    let user_id = mission.user_id.unwrap_or_default();
    let target_planet_id = match mission.target_planet {
        Some(p) => p as u64,
        None => return Ok(true),
    };
    if !super::is_attack_trigger_enabled(conn, mission_type).await? {
        return Ok(true);
    }
    let alliance_id = find_user_alliance(conn, user_id).await?;
    if !super::are_units_involved(conn, user_id, alliance_id, target_planet_id).await? {
        return Ok(true);
    }
    let outcome = process_attack(
        conn, mission, /* survivors_do_return = */ false,
        /* is_triggered_by_event = */ true, emits,
    )
    .await?;
    Ok(!outcome.removed)
}

/// `processAttack(mission, survivorsDoReturn, isTriggeredByEvent)`.
///
/// Runs the combat core, persists the result, optionally registers the survivors'
/// return mission, builds the attack report and saves it to the invoker and to
/// every other affected user.
pub async fn process_attack(
    conn: &mut MySqlConnection,
    mission: &Mission,
    survivors_do_return: bool,
    is_triggered_by_event: bool,
    emits: &mut Vec<super::DeferredEmit>,
) -> OwgeResult<AttackOutcome> {
    let invoker_id = mission.user_id.unwrap_or_default();

    // The combat manager loads its own involved units (it re-enriches the
    // attacker stacks itself), so we pass the mission's current stacks.
    let involved_units = find_units_involved(conn, mission.id).await?;
    let (attack_information, emit_data) =
        AttackMissionManagerBo::process_attack(conn, mission, &involved_units).await?;

    // `attackInformation.isRemoved()` — the attack mission ran out of units; the
    // combat manager flags this when its mission becomes empty. Derive it from the
    // persisted state: no obtained_units remain attached to the attack mission.
    let remaining: i64 =
        sqlx::query_scalar("SELECT COUNT(*) FROM obtained_units WHERE mission_id = ?")
            .bind(mission.id)
            .fetch_one(&mut *conn)
            .await?;
    let removed = remaining == 0;

    if survivors_do_return && !removed {
        // registerReturnMission emits emitLocalMissionChangeAfterCommit(returnMission).
        let return_id =
            ReturnMissionRegistrationBo::register_return_mission(conn, mission, None).await?;
        emits.push(super::DeferredEmit::LocalMissionChange {
            mission_id: return_id,
            user_id: invoker_id,
        });
    }

    // mission.setResolved(true) is persisted by the report save path below.

    let report_builder = create_report_base(conn, mission, &[])
        .await?
        .with_attack_information(attack_information.clone());

    // handleMissionReportSave(mission, builder, true, <affected non-invoker users>).
    let affected: Vec<i32> = participating_user_ids(&attack_information)
        .into_iter()
        .filter(|&id| id != invoker_id)
        .collect();
    if !affected.is_empty() {
        let enemy_builder = create_report_base(conn, mission, &[])
            .await?
            .with_attack_information(attack_information.clone());
        let enemy_pairs = MissionReportManagerBo::handle_mission_report_save_for_users(
            conn,
            &enemy_builder,
            /* is_enemy = */ true,
            &affected,
        )
        .await?;
        for (uid, rid, rdate) in enemy_pairs {
            emits.push(super::DeferredEmit::MissionReport {
                user_id: uid,
                report_id: rid,
                report_date: rdate,
            });
        }
    }

    // For an event-triggered attack the invoker also gets its own (non-enemy)
    // report saved immediately, since the mission it belongs to is not an ATTACK
    // mission whose normal flow would persist the builder.
    if is_triggered_by_event {
        let invoker_builder = create_report_base(conn, mission, &[])
            .await?
            .with_attack_information(attack_information.clone());
        let (report_user_id, report_id, report_date) =
            MissionReportManagerBo::handle_mission_report_save(conn, mission, invoker_builder)
                .await?;
        emits.push(super::DeferredEmit::MissionReport {
            user_id: report_user_id,
            report_id,
            report_date,
        });
    }

    // Requirement-trigger `*_unlocked_change` pushes from the combat unit-count
    // changes (`AttackMissionProcessor.triggerUnitRequirementChange`), drained with
    // the rest of the post-commit emits.
    for req in emit_data.requirement_emits {
        emits.push(super::DeferredEmit::Requirement(req));
    }

    // Per-captor capture reports (`emitAttackEnd` / HandleUnitCaptureListener).
    for (uid, rid, rdate) in emit_data.capture_report_pairs {
        emits.push(super::DeferredEmit::MissionReport {
            user_id: uid,
            report_id: rid,
            report_date: rdate,
        });
    }

    // Schedule the post-commit per-user websocket emit block
    // (`AttackMissionManagerBo.startAttack` + `updatePoints` doAfterCommit +
    // `AttackMissionProcessor.emitLocalMissionChangeAfterCommit`).
    emits.push(super::DeferredEmit::Attack(super::AttackEmit {
        mission_id: mission.id,
        mission_user_id: invoker_id,
        removed,
        target_owner: emit_data.target_owner,
        users_with_deleted_missions: emit_data.users_with_deleted_missions,
        users_with_changed_counts: emit_data.users_with_changed_counts,
        altered_users: emit_data.altered_users,
    }));

    // NOT PORTED (deliberate): auditBo.nonRequestAudit(ATTACK_INTERACTION, ...) —
    // audit side effect, no combat-state impact; auditing is disabled in the live
    // Java deployment, so it is an intentional no-op in the port.

    Ok(AttackOutcome {
        attack_information,
        removed,
        report_builder,
    })
}

/// The participating user ids from the `attackInformation` JSON.
fn participating_user_ids(attack_information: &Value) -> Vec<i32> {
    attack_information
        .as_array()
        .map(|a| {
            a.iter()
                .filter_map(|u| u["userInfo"]["id"].as_i64().map(|v| v as i32))
                .collect()
        })
        .unwrap_or_default()
}

/// `user_storage.alliance_id` for the invoker.
async fn find_user_alliance(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<Option<u16>> {
    let alliance: Option<Option<u16>> =
        sqlx::query_scalar("SELECT alliance_id FROM user_storage WHERE id = ?")
            .bind(user_id)
            .fetch_optional(&mut *conn)
            .await?;
    Ok(alliance.flatten())
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
         FROM obtained_units WHERE mission_id = ? \
         ORDER BY id",
    )
    .bind(mission_id)
    .fetch_all(&mut *conn)
    .await?)
}
