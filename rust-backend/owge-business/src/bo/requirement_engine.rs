//! Port of the **write** side of `RequirementBo` — the requirement-trigger
//! engine that adds/removes rows from `unlocked_relation` (and the dependent
//! `obtained_upgrades`) when a player's state changes.
//!
//! This is the M2 shared infrastructure the porting roadmap flags as
//! rework-prone, so it is ported faithfully against the Java source
//! (`RequirementBo.processRelationList`/`processRelation`/
//! `checkRequirementsAreMet`/`registerObtainedRelation`/`unregisterLostRelation`).
//!
//! ## What is ported
//! - The two subscribe triggers ([`trigger_faction_selection`],
//!   [`trigger_home_galaxy_selection`]) plus the generic engine they ride on, so
//!   the remaining triggers (level-up, unit build, special location, time
//!   special) only need a thin wrapper when M3 lands.
//! - All requirement-type checks: `UPGRADE_LEVEL`, `HAVE_UNIT`, `UNIT_AMOUNT`,
//!   `BEEN_RACE`, `HOME_GALAXY`, `HAVE_SPECIAL_LOCATION`, and the three
//!   `RequirementSource` ones (`HAVE_SPECIAL_AVAILABLE`, `HAVE_SPECIAL_ENABLED`,
//!   `UPGRADE_LEVEL_LOWER_THAN`).
//! - Requirement-group OR-semantics: a *master* relation unlocks when **any** of
//!   its requirement-group *slaves* is unlocked.
//! - The `obtained_upgrades` side effects of (un)locking an `UPGRADE` relation.
//!
//! ## Deferred (matching the rest of the port)
//! - The websocket `*_unlocked_change` emissions and the internal
//!   requirement-event emitter are **M4**; their call sites are marked.
//!
//! Every function takes a `&mut MySqlConnection` so the whole trigger runs inside
//! the caller's transaction (e.g. `UserStorageBo::subscribe`).

use sqlx::MySqlConnection;

use crate::bo::realtime_emitter::RequirementEmit;
use crate::error::{OwgeError, OwgeResult};
use crate::model::object_relation::object_enum;
use crate::model::UserStorage;

// Requirement codes (the `requirements.code` column / `RequirementTypeEnum`).
const UPGRADE_LEVEL: &str = "UPGRADE_LEVEL";
const HAVE_UNIT: &str = "HAVE_UNIT";
const UNIT_AMOUNT: &str = "UNIT_AMOUNT";
const BEEN_RACE: &str = "BEEN_RACE";
const HOME_GALAXY: &str = "HOME_GALAXY";
const HAVE_SPECIAL_LOCATION: &str = "HAVE_SPECIAL_LOCATION";
const HAVE_SPECIAL_AVAILABLE: &str = "HAVE_SPECIAL_AVAILABLE";
const HAVE_SPECIAL_ENABLED: &str = "HAVE_SPECIAL_ENABLED";
const UPGRADE_LEVEL_LOWER_THAN: &str = "UPGRADE_LEVEL_LOWER_THAN";

/// An `object_relations` row (engine-local, exact column types).
#[derive(Clone, sqlx::FromRow)]
struct RelationRow {
    id: u16,
    object_description: String,
    reference_id: i16,
}

/// A `requirements_information` row joined with its `requirements.code`.
#[derive(sqlx::FromRow)]
struct ReqRow {
    code: String,
    second_value: Option<i32>,
    third_value: Option<i32>,
}

/// `RequirementBo.triggerFactionSelection` — re-evaluate every relation gated by
/// a `BEEN_RACE` requirement for this user. Any resulting `*_unlocked_change`
/// pushes are appended to `emits` for the caller to drain after commit.
pub async fn trigger_faction_selection(
    conn: &mut MySqlConnection,
    user: &UserStorage,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    let relations = find_relations_with_code(conn, BEEN_RACE).await?;
    process_relation_list(conn, &relations, user, emits).await
}

/// `RequirementBo.triggerHomeGalaxySelection` — re-evaluate every relation gated
/// by a `HOME_GALAXY` requirement for this user.
pub async fn trigger_home_galaxy_selection(
    conn: &mut MySqlConnection,
    user: &UserStorage,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    let relations = find_relations_with_code(conn, HOME_GALAXY).await?;
    process_relation_list(conn, &relations, user, emits).await
}

/// `RequirementBo.triggerSpecialLocation` — re-evaluate every relation gated by a
/// `HAVE_SPECIAL_LOCATION` requirement whose `second_value` equals this special
/// location id. Kept symmetric with the `RequirementBo::trigger_special_location`
/// in `requirement_bo.rs` (the conquest/planet-leave callers use that one); this
/// wrapper exists so the two trigger engines stay in lockstep.
pub async fn trigger_special_location(
    conn: &mut MySqlConnection,
    user: &UserStorage,
    special_location_id: i64,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    let relations =
        find_relations_with_code_and_second_value(conn, HAVE_SPECIAL_LOCATION, special_location_id)
            .await?;
    process_relation_list(conn, &relations, user, emits).await
}

/// `ObjectRelationBo.findByRequirementTypeAndSecondValue` — relations carrying a
/// requirement of `code` whose `second_value` equals `second_value`.
async fn find_relations_with_code_and_second_value(
    conn: &mut MySqlConnection,
    code: &str,
    second_value: i64,
) -> OwgeResult<Vec<RelationRow>> {
    let rows = sqlx::query_as::<_, RelationRow>(
        "SELECT DISTINCT o.id, o.object_description, o.reference_id \
         FROM object_relations o \
         JOIN requirements_information ri ON ri.relation_id = o.id \
         JOIN requirements r ON r.id = ri.requirement_id \
         WHERE r.code = ? AND ri.second_value = ?",
    )
    .bind(code)
    .bind(second_value as i32)
    .fetch_all(&mut *conn)
    .await?;
    Ok(rows)
}

/// `RequirementBo.processRelationList` — process each relation, deferring
/// requirement-group *masters* to a second pass that applies OR-semantics over
/// their slaves.
async fn process_relation_list(
    conn: &mut MySqlConnection,
    relations: &[RelationRow],
    user: &UserStorage,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    let mut affected_masters: Vec<u16> = Vec::new();
    for relation in relations {
        let is_slave_or_has_no_slaves;
        if relation.object_description == object_enum::REQUIREMENT_GROUP {
            if let Some(master_id) = find_master_id_by_slave(conn, relation.id).await? {
                if !affected_masters.contains(&master_id) {
                    affected_masters.push(master_id);
                }
            } else {
                tracing::warn!("Orphan group with id {}", relation.id);
            }
            is_slave_or_has_no_slaves = true;
        } else {
            is_slave_or_has_no_slaves = !exists_by_master(conn, relation.id).await?;
        }
        if is_slave_or_has_no_slaves {
            process_relation(conn, relation, user, emits).await?;
        }
    }
    for master_id in affected_masters {
        let Some(master) = find_relation_by_id(conn, master_id).await? else {
            continue;
        };
        let slave_ids = find_slave_ids_by_master(conn, master_id).await?;
        let mut any_slave_unlocked = false;
        for slave_id in slave_ids {
            if is_unlocked(conn, user.id, slave_id).await? {
                any_slave_unlocked = true;
                break;
            }
        }
        if any_slave_unlocked {
            register_obtained_relation(conn, &master, user, emits).await?;
        } else {
            unregister_lost_relation(conn, &master, user, emits).await?;
        }
    }
    Ok(())
}

/// `RequirementBo.processRelation` — (un)lock a single relation by whether its
/// requirements are met.
async fn process_relation(
    conn: &mut MySqlConnection,
    relation: &RelationRow,
    user: &UserStorage,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    if check_requirements_are_met(conn, relation.id, user).await? {
        register_obtained_relation(conn, relation, user, emits).await
    } else {
        unregister_lost_relation(conn, relation, user, emits).await
    }
}

/// `RequirementBo.checkRequirementsAreMet` — all of a relation's requirements
/// must hold (an empty requirement set is trivially met).
async fn check_requirements_are_met(
    conn: &mut MySqlConnection,
    relation_id: u16,
    user: &UserStorage,
) -> OwgeResult<bool> {
    let reqs = sqlx::query_as::<_, ReqRow>(
        "SELECT r.code, ri.second_value, ri.third_value \
         FROM requirements_information ri \
         JOIN requirements r ON r.id = ri.requirement_id \
         WHERE ri.relation_id = ?",
    )
    .bind(relation_id)
    .fetch_all(&mut *conn)
    .await?;
    for req in reqs {
        let second = req.second_value.unwrap_or(0);
        let third = req.third_value.unwrap_or(0) as i64;
        let met = match req.code.as_str() {
            UPGRADE_LEVEL => upgrade_level(conn, user.id, second).await? >= third,
            HAVE_UNIT => is_built_unit(conn, user.id, second).await?,
            UNIT_AMOUNT => count_units(conn, user.id, second).await? >= third,
            BEEN_RACE => user.faction as i32 == second,
            HOME_GALAXY => home_galaxy_id(conn, user.home_planet).await? == Some(second),
            HAVE_SPECIAL_LOCATION => special_location_owned(conn, user.id, second).await?,
            HAVE_SPECIAL_AVAILABLE => time_special_unlocked(conn, user.id, second).await?,
            HAVE_SPECIAL_ENABLED => time_special_active(conn, user.id, second).await?,
            UPGRADE_LEVEL_LOWER_THAN => {
                let (exists, level) = obtained_upgrade_level(conn, user.id, second).await?;
                exists && level < third
            }
            other => {
                return Err(OwgeError::Common(format!(
                    "Not implemented requirement type: {other}"
                )));
            }
        };
        if !met {
            return Ok(false);
        }
    }
    Ok(true)
}

/// `RequirementBo.registerObtainedRelation` — idempotently insert the
/// `unlocked_relation` row and apply the per-object-type side effects.
async fn register_obtained_relation(
    conn: &mut MySqlConnection,
    relation: &RelationRow,
    user: &UserStorage,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    if find_unlocked_id(conn, user.id, relation.id).await?.is_some() {
        return Ok(()); // already unlocked: do nothing (matches Java)
    }
    sqlx::query("INSERT INTO unlocked_relation (user_id, relation_id) VALUES (?, ?)")
        .bind(user.id)
        .bind(relation.id)
        .execute(&mut *conn)
        .await?;
    // Java `switch (object)`: UPGRADE touches obtained_upgrades (no socket push);
    // UNIT / TIME_SPECIAL / SPEED_IMPACT_GROUP schedule a doAfterCommit
    // `*_unlocked_change`; REQUIREMENT_GROUP does nothing.
    match relation.object_description.as_str() {
        object_enum::UPGRADE => {
            let upgrade_id = relation.reference_id as u16;
            if obtained_upgrade_exists(conn, user.id, upgrade_id).await? {
                set_obtained_upgrade_available(conn, user.id, upgrade_id, true).await?;
            } else {
                register_obtained_upgrade(conn, user.id, upgrade_id).await?;
            }
        }
        object_enum::UNIT => emits.push(RequirementEmit::UnitUnlocked(user.id)),
        object_enum::TIME_SPECIAL => emits.push(RequirementEmit::TimeSpecialUnlocked(user.id)),
        object_enum::SPEED_IMPACT_GROUP => {
            emits.push(RequirementEmit::SpeedImpactGroupUnlocked(user.id))
        }
        _ => {}
    }
    // TODO: requirementInternalEventEmitterService.doNotifyObtainedRelation — the
    // `relationObtained` listeners are all no-ops in Java (default method), so
    // there is nothing behavioural to port here.
    Ok(())
}

/// `RequirementBo.unregisterLostRelation` — remove the `unlocked_relation` row
/// (if any) and reverse the per-object-type side effects.
async fn unregister_lost_relation(
    conn: &mut MySqlConnection,
    relation: &RelationRow,
    user: &UserStorage,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    let unlocked_id = find_unlocked_id(conn, user.id, relation.id).await?;
    if let Some(id) = unlocked_id {
        sqlx::query("DELETE FROM unlocked_relation WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?;
    }
    // Java if/else chain (preserved exactly, including the quirk that an UPGRADE
    // relation with no obtained_upgrade row falls through to the time-special emit):
    //   if (UPGRADE && obtainedUpgradeExists) alterAvailability(false);
    //   else if (SPEED_IMPACT_GROUP)          emitUnlockedSpeedImpactGroups;
    //   else if (unlockedRelation != null)    emitUnlockedChange(UNIT ? unit : timeSpecial);
    let upgrade_exists = relation.object_description == object_enum::UPGRADE
        && obtained_upgrade_exists(conn, user.id, relation.reference_id as u16).await?;
    if upgrade_exists {
        set_obtained_upgrade_available(conn, user.id, relation.reference_id as u16, false).await?;
    } else if relation.object_description == object_enum::SPEED_IMPACT_GROUP {
        emits.push(RequirementEmit::SpeedImpactGroupUnlocked(user.id));
    } else if unlocked_id.is_some() {
        if relation.object_description == object_enum::UNIT {
            emits.push(RequirementEmit::UnitUnlocked(user.id));
        } else {
            emits.push(RequirementEmit::TimeSpecialUnlocked(user.id));
        }
    }
    // doNotifyLostRelation listener #1 (ActiveTimeSpecialBo.relationLost): a lost
    // TIME_SPECIAL unlock relation force-deactivates the special when the user has
    // it ACTIVE. Box::pin breaks the async recursion
    // (deactivate_in_tx → trigger_time_special_state_change → process_relation_list →
    // unregister_lost_relation); it terminates because the special leaves ACTIVE.
    if relation.object_description == object_enum::TIME_SPECIAL {
        if let Some(active_id) =
            find_active_time_special_id(conn, user.id, relation.reference_id).await?
        {
            Box::pin(crate::bo::ActiveTimeSpecialBo::deactivate_in_tx(
                conn, active_id, emits,
            ))
            .await?;
        }
        // doNotifyLostRelation listener #2 (TemporalUnitScheduleListener.relationLost):
        // remove the special's temporal units. Deferred post-commit (the removal
        // pins its own connection + planet locks); `relation.id` is the
        // `object_relations` id stored on the temporal-info rows.
        emits.push(RequirementEmit::TemporalUnitsRelationLost(relation.id));
    }
    Ok(())
}

/// The ACTIVE `active_time_specials.id` for `user_id` + `time_special_id`, if any —
/// `ActiveTimeSpecialBo.relationLost`'s `findOneByTimeSpecialIdAndUserId` filtered
/// to the `ACTIVE` state.
async fn find_active_time_special_id(
    conn: &mut MySqlConnection,
    user_id: i32,
    time_special_id: i16,
) -> OwgeResult<Option<u64>> {
    let id = sqlx::query_scalar::<_, u64>(
        "SELECT id FROM active_time_specials \
          WHERE user_id = ? AND time_special_id = ? AND state = 'ACTIVE' LIMIT 1",
    )
    .bind(user_id)
    .bind(time_special_id as u16)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(id)
}

// ---------------------------------------------------------------------------
// Query helpers (each maps a Java repository method).
// ---------------------------------------------------------------------------

/// `objectRelationsRepository.findByRequirementsRequirementCode(code)` — the
/// distinct relations that carry at least one requirement of the given code.
async fn find_relations_with_code(
    conn: &mut MySqlConnection,
    code: &str,
) -> OwgeResult<Vec<RelationRow>> {
    let rows = sqlx::query_as::<_, RelationRow>(
        "SELECT DISTINCT o.id, o.object_description, o.reference_id \
         FROM object_relations o \
         JOIN requirements_information ri ON ri.relation_id = o.id \
         JOIN requirements r ON r.id = ri.requirement_id \
         WHERE r.code = ?",
    )
    .bind(code)
    .fetch_all(&mut *conn)
    .await?;
    Ok(rows)
}

async fn find_relation_by_id(
    conn: &mut MySqlConnection,
    id: u16,
) -> OwgeResult<Option<RelationRow>> {
    let row = sqlx::query_as::<_, RelationRow>(
        "SELECT id, object_description, reference_id FROM object_relations WHERE id = ?",
    )
    .bind(id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(row)
}

/// `objectRelationToObjectRelationBo.findBySlave(...).getMaster()`.
async fn find_master_id_by_slave(
    conn: &mut MySqlConnection,
    slave_id: u16,
) -> OwgeResult<Option<u16>> {
    let id = sqlx::query_scalar::<_, u16>(
        "SELECT master_relation_id FROM object_relation__object_relation \
         WHERE slave_relation_id = ? LIMIT 1",
    )
    .bind(slave_id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(id)
}

/// `objectRelationToObjectRelationBo.isMaster(relation)`.
async fn exists_by_master(conn: &mut MySqlConnection, master_id: u16) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM object_relation__object_relation WHERE master_relation_id = ?",
    )
    .bind(master_id)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}

/// `objectRelationToObjectRelationBo.findByMasterId(...).map(getSlave)`.
async fn find_slave_ids_by_master(
    conn: &mut MySqlConnection,
    master_id: u16,
) -> OwgeResult<Vec<u16>> {
    let ids = sqlx::query_scalar::<_, u16>(
        "SELECT slave_relation_id FROM object_relation__object_relation WHERE master_relation_id = ?",
    )
    .bind(master_id)
    .fetch_all(&mut *conn)
    .await?;
    Ok(ids)
}

/// `unlockedRelationBo.findOneByUserIdAndRelationId(...)` — the row id, or None.
async fn find_unlocked_id(
    conn: &mut MySqlConnection,
    user_id: i32,
    relation_id: u16,
) -> OwgeResult<Option<i64>> {
    let id = sqlx::query_scalar::<_, i64>(
        "SELECT id FROM unlocked_relation WHERE user_id = ? AND relation_id = ? LIMIT 1",
    )
    .bind(user_id)
    .bind(relation_id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(id)
}

/// `unlockedRelationBo.isUnlocked(user, relation)`.
async fn is_unlocked(
    conn: &mut MySqlConnection,
    user_id: i32,
    relation_id: u16,
) -> OwgeResult<bool> {
    Ok(find_unlocked_id(conn, user_id, relation_id).await?.is_some())
}

/// The user's level for an upgrade (`0` if none) — `checkUpgradeLevelRequirement`.
async fn upgrade_level(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: i32,
) -> OwgeResult<i64> {
    let level = sqlx::query_scalar::<_, i16>(
        "SELECT level FROM obtained_upgrades WHERE user_id = ? AND upgrade_id = ? LIMIT 1",
    )
    .bind(user_id)
    .bind(upgrade_id as u16)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(level.unwrap_or(0) as i64)
}

/// `(exists, level)` for an upgrade — used by `UPGRADE_LEVEL_LOWER_THAN`.
async fn obtained_upgrade_level(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: i32,
) -> OwgeResult<(bool, i64)> {
    let level = sqlx::query_scalar::<_, i16>(
        "SELECT level FROM obtained_upgrades WHERE user_id = ? AND upgrade_id = ? LIMIT 1",
    )
    .bind(user_id)
    .bind(upgrade_id as u16)
    .fetch_optional(&mut *conn)
    .await?;
    Ok((level.is_some(), level.unwrap_or(0) as i64))
}

/// `obtainedUnitRepository.isBuiltUnit(user, unit)` — owns at least one of the
/// unit that is not currently a pending `BUILD_UNIT` mission.
async fn is_built_unit(
    conn: &mut MySqlConnection,
    user_id: i32,
    unit_id: i32,
) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM obtained_units ou \
         LEFT JOIN missions m ON m.id = ou.mission_id \
         LEFT JOIN mission_types mt ON mt.id = m.type \
         WHERE ou.user_id = ? AND ou.unit_id = ? \
           AND (ou.mission_id IS NULL OR mt.code != 'BUILD_UNIT')",
    )
    .bind(user_id)
    .bind(unit_id as u16)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}

/// `obtainedUnitRepository.countByUserAndUnit(user, unit)`.
async fn count_units(conn: &mut MySqlConnection, user_id: i32, unit_id: i32) -> OwgeResult<i64> {
    let count: i64 = sqlx::query_scalar(
        // SUM() yields DECIMAL even over an integer column; CAST so sqlx decodes i64.
        "SELECT CAST(COALESCE(SUM(count), 0) AS SIGNED) FROM obtained_units WHERE user_id = ? AND unit_id = ?",
    )
    .bind(user_id)
    .bind(unit_id as u16)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count)
}

/// `checkBeenGalaxyRequirement` — the galaxy of the user's home planet.
async fn home_galaxy_id(
    conn: &mut MySqlConnection,
    home_planet: u64,
) -> OwgeResult<Option<i32>> {
    let galaxy = sqlx::query_scalar::<_, u16>("SELECT galaxy_id FROM planets WHERE id = ?")
        .bind(home_planet)
        .fetch_optional(&mut *conn)
        .await?;
    Ok(galaxy.map(|g| g as i32))
}

/// `checkSpecialLocationRequirement` — the planet holding the special location is
/// owned by the user.
async fn special_location_owned(
    conn: &mut MySqlConnection,
    user_id: i32,
    special_location_id: i32,
) -> OwgeResult<bool> {
    let owner = sqlx::query_scalar::<_, Option<i32>>(
        "SELECT owner FROM planets WHERE special_location_id = ? LIMIT 1",
    )
    .bind(special_location_id)
    .fetch_optional(&mut *conn)
    .await?;
    // None planet -> false; planet with NULL owner -> false; else owner == user.
    Ok(matches!(owner, Some(Some(o)) if o == user_id))
}

/// `TimeSpecialAvailableRequirementSourceBo` — the `TIME_SPECIAL` relation is in
/// the user's `unlocked_relation`.
async fn time_special_unlocked(
    conn: &mut MySqlConnection,
    user_id: i32,
    time_special_id: i32,
) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM unlocked_relation ur \
         JOIN object_relations o ON o.id = ur.relation_id \
         WHERE ur.user_id = ? AND o.object_description = ? AND o.reference_id = ?",
    )
    .bind(user_id)
    .bind(object_enum::TIME_SPECIAL)
    .bind(time_special_id as i16)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}

/// `TimeSpecialEnabledRequirementSourceBo` — the user has the time special in the
/// `ACTIVE` state.
async fn time_special_active(
    conn: &mut MySqlConnection,
    user_id: i32,
    time_special_id: i32,
) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM active_time_specials \
         WHERE user_id = ? AND time_special_id = ? AND state = 'ACTIVE'",
    )
    .bind(user_id)
    .bind(time_special_id as u16)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}

async fn obtained_upgrade_exists(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: u16,
) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM obtained_upgrades WHERE user_id = ? AND upgrade_id = ?",
    )
    .bind(user_id)
    .bind(upgrade_id)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}

/// `registerObtainedUpgrade` — a fresh `obtained_upgrades` row at level 0,
/// available.
async fn register_obtained_upgrade(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: u16,
) -> OwgeResult<()> {
    sqlx::query(
        "INSERT INTO obtained_upgrades (user_id, upgrade_id, level, available) VALUES (?, ?, 0, 1)",
    )
    .bind(user_id)
    .bind(upgrade_id)
    .execute(&mut *conn)
    .await?;
    Ok(())
}

/// `alterObtainedUpgradeAvailability`.
async fn set_obtained_upgrade_available(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: u16,
    available: bool,
) -> OwgeResult<()> {
    sqlx::query("UPDATE obtained_upgrades SET available = ? WHERE user_id = ? AND upgrade_id = ?")
        .bind(available as i8)
        .bind(user_id)
        .bind(upgrade_id)
        .execute(&mut *conn)
        .await?;
    Ok(())
}
