//! Port of `ConquestMissionProcessor`
//! (`...business.mission.processor.ConquestMissionProcessor`).
//!
//! Runs the attack at the target, then decides whether the conquest succeeds
//! (old owner and their alliance defeated, user under the planet cap, not a home
//! planet). On success the planet changes hands; on failure the survivors return
//! and the report explains why.

use sqlx::MySqlConnection;

use crate::bo::mission_report_manager_bo::MissionReportManagerBo;
use crate::bo::return_mission_registration_bo::ReturnMissionRegistrationBo;
use crate::builder::UnitMissionReportBuilder;
use crate::db::Db;
use crate::error::OwgeResult;
use crate::model::mission::Mission;
use crate::model::obtained_unit::ObtainedUnit;

use super::{attack, create_report_base};

/// `GlobalConstants.MAX_PLANETS_MESSAGE`.
const MAX_PLANETS_MESSAGE: &str = "I18N_MAX_PLANETS_EXCEEDED";

pub async fn process(
    conn: &mut MySqlConnection,
    mission: &Mission,
    involved_units: &[ObtainedUnit],
    db: &Db,
    emits: &mut Vec<super::DeferredEmit>,
) -> OwgeResult<Option<UnitMissionReportBuilder>> {
    let user_id = mission.user_id.unwrap_or_default();
    let target_planet_id = match mission.target_planet {
        Some(p) => p as u64,
        None => return Ok(None),
    };

    let mut builder = create_report_base(conn, mission, involved_units).await?;
    let max_planets = super::has_max_planets(conn, user_id).await?;

    // Capture the old owner before the attack (the attack never reassigns owner).
    let old_owner = find_planet_owner(conn, target_planet_id).await?;

    let outcome = attack::process_attack(
        conn,
        mission,
        /* survivors_do_return = */ false,
        /* is_triggered_by_event = */ false,
        db,
        emits,
    )
    .await?;

    let (is_old_owner_defeated, is_alliance_defeated) = match old_owner {
        None => (true, true),
        Some(owner_id) => {
            let defeated = calculate_is_old_owner_defeated(&outcome, owner_id);
            let alliance_defeated =
                calculate_is_alliance_defeated(conn, &outcome, owner_id, defeated).await?;
            (defeated, alliance_defeated)
        }
    };

    let is_home = super::is_home_planet(conn, target_planet_id).await?;
    let failed = !is_old_owner_defeated || !is_alliance_defeated || max_planets || is_home;
    let mut units_returning = false;

    if failed {
        if !outcome.removed {
            ReturnMissionRegistrationBo::register_return_mission(conn, mission, None).await?;
            units_returning = true;
        }
        builder = append_conquest_information(
            builder,
            max_planets,
            is_old_owner_defeated,
            is_alliance_defeated,
        );
    } else {
        super::define_planet_as_owned_by(conn, user_id, involved_units, target_planet_id).await?;
        builder = builder.with_conquest_information(true, "I18N_PLANET_IS_NOW_OURS");
        // ConquestMissionProcessor.process (Java lines 74-85): when the planet has a
        // special location and there was an old owner, re-evaluate that (former)
        // owner's HAVE_SPECIAL_LOCATION unlocks, then cancel their BUILD_UNIT mission
        // on the planet, then save the enemy report. Centralizes the special-location
        // trigger that `define_planet_as_owned_by`'s comment refers to (it is NOT
        // fired there — Return/re-home also call that helper and must not trigger it).
        if let Some(owner_id) = old_owner {
            // maybeTriggerSpecialLocation(targetPlanet, oldOwner) — only when the
            // planet actually carries a special location.
            if let Some(special_location_id) =
                find_planet_special_location(conn, target_planet_id).await?
            {
                let old_owner_user = crate::bo::mission_bo::load_user_storage(conn, owner_id).await?;
                let mut req_emits = Vec::new();
                crate::bo::requirement_bo::RequirementBo::trigger_special_location(
                    conn,
                    &old_owner_user,
                    special_location_id as i64,
                    &mut req_emits,
                )
                .await?;
                for req in req_emits {
                    emits.push(super::DeferredEmit::Requirement(req));
                }
            }
            // findUnitBuildMission(targetPlanet).ifPresent(missionCancelBuildService::cancel):
            // cancel the old owner's unresolved BUILD_UNIT mission on this planet
            // (refunds resources, deletes the in-build units, the mission, and its
            // scheduled task). `old_owner` satisfies cancelBuildUnit's ownership guard.
            //
            // NOTE: the Java cancel joins the firing tx (Propagation.MANDATORY); the
            // Rust `MissionBo::cancel_build_unit` opens its own transaction on `db`.
            // The conquest firing tx (on `conn`) commits before the deferred emits run,
            // so this runs the cancel against `db` while that tx is still open. It
            // operates on the old owner's distinct rows (their build mission + user
            // resources) and acquires no planet lock, so it does not contend with the
            // conquest tx's `planet_lock_<target>`. This is the emit/tx-threading gap
            // noted in risks_or_deviations.
            if let Some(build_mission_id) =
                find_old_owner_build_mission(conn, owner_id, target_planet_id).await?
            {
                crate::bo::mission_bo::MissionBo::cancel_build_unit(db, owner_id, build_mission_id)
                    .await?;
            }
            let enemy_builder = create_report_base(conn, mission, involved_units)
                .await?
                .with_conquest_information(true, "I18N_YOUR_PLANET_WAS_CONQUISTED");
            let pairs = MissionReportManagerBo::handle_mission_report_save_for_users(
                conn,
                &enemy_builder,
                /* is_enemy = */ true,
                &[owner_id],
            )
            .await?;
            for (uid, rid) in pairs {
                emits.push(super::DeferredEmit::MissionReport {
                    user_id: uid,
                    report_id: rid,
                });
            }
        }
        // PlanetBo.definePlanetAsOwnedBy (new owner) + old-owner post-commit emits.
        emits.push(super::DeferredEmit::ConquestSuccess {
            new_owner_id: user_id,
            target_planet_id,
            old_owner_id: old_owner,
        });
    }

    // emitLocalMissionChangeAfterCommit(mission) unless the survivors are returning
    // (Java: `if (!areUnitsHavingToReturn)`).
    if !units_returning {
        emits.push(super::DeferredEmit::LocalMissionChange {
            mission_id: mission.id,
            user_id,
        });
    }

    // mission.setResolved(true) is persisted by the report save path.
    Ok(Some(builder))
}

/// `calculateIsOldOwnerDefeated` — the old owner has no surviving stack.
fn calculate_is_old_owner_defeated(outcome: &attack::AttackOutcome, owner_id: i32) -> bool {
    !outcome.contains_user(owner_id) || !outcome.user_has_survivors(owner_id)
}

/// `calculateIsAllianceDefeated` — the old owner is defeated and every
/// participant in the old owner's alliance has no surviving stack.
async fn calculate_is_alliance_defeated(
    conn: &mut MySqlConnection,
    outcome: &attack::AttackOutcome,
    owner_id: i32,
    is_old_owner_defeated: bool,
) -> OwgeResult<bool> {
    if !is_old_owner_defeated {
        return Ok(false);
    }
    let Some(owner_alliance) = find_user_alliance(conn, owner_id).await? else {
        // oldOwner.getAlliance() == null -> alliance considered defeated.
        return Ok(true);
    };
    for participant_id in outcome.participating_user_ids() {
        let participant_alliance = find_user_alliance(conn, participant_id).await?;
        if participant_alliance == Some(owner_alliance) && outcome.user_has_survivors(participant_id)
        {
            return Ok(false);
        }
    }
    Ok(true)
}

/// `appendConquestInformation`.
fn append_conquest_information(
    builder: UnitMissionReportBuilder,
    max_planets: bool,
    is_old_owner_defeated: bool,
    is_alliance_defeated: bool,
) -> UnitMissionReportBuilder {
    if max_planets {
        builder.with_conquest_information(false, MAX_PLANETS_MESSAGE)
    } else if !is_old_owner_defeated {
        builder.with_conquest_information(false, "I18N_OWNER_NOT_DEFEATED")
    } else if !is_alliance_defeated {
        builder.with_conquest_information(false, "I18N_ALLIANCE_NOT_DEFEATED")
    } else {
        builder.with_conquest_information(false, "I18N_CANT_CONQUER_HOME_PLANET")
    }
}

/// `targetPlanet.getSpecialLocation()` — the planet's `special_location_id`
/// (`smallint UNSIGNED` -> `u16`), or `None` when the planet has none.
async fn find_planet_special_location(
    conn: &mut MySqlConnection,
    planet_id: u64,
) -> OwgeResult<Option<u16>> {
    let special: Option<Option<u16>> =
        sqlx::query_scalar("SELECT special_location_id FROM planets WHERE id = ?")
            .bind(planet_id)
            .fetch_optional(&mut *conn)
            .await?;
    Ok(special.flatten())
}

/// `missionRepository.findOneByResolvedFalseAndTypeCodeAndMissionInformationValue(
/// BUILD_UNIT, planet.getId())` constrained to the old owner — the unresolved
/// BUILD_UNIT mission whose `mission_information.value` equals the planet id. Runs
/// on the firing `conn` so it sees the conquest tx's consistent view.
async fn find_old_owner_build_mission(
    conn: &mut MySqlConnection,
    owner_id: i32,
    planet_id: u64,
) -> OwgeResult<Option<u64>> {
    let id: Option<u64> = sqlx::query_scalar(
        "SELECT m.id FROM missions m \
           JOIN mission_types mt ON mt.id = m.type \
           JOIN mission_information mi ON mi.mission_id = m.id \
          WHERE m.user_id = ? AND m.resolved = 0 \
            AND mt.code = 'BUILD_UNIT' AND mi.value = ? LIMIT 1",
    )
    .bind(owner_id)
    .bind(planet_id as f64)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(id)
}

async fn find_planet_owner(
    conn: &mut MySqlConnection,
    planet_id: u64,
) -> OwgeResult<Option<i32>> {
    let owner: Option<Option<i32>> =
        sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
            .bind(planet_id)
            .fetch_optional(&mut *conn)
            .await?;
    Ok(owner.flatten())
}

async fn find_user_alliance(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<Option<u16>> {
    let alliance: Option<Option<u16>> =
        sqlx::query_scalar("SELECT alliance_id FROM user_storage WHERE id = ?")
            .bind(user_id)
            .fetch_optional(&mut *conn)
            .await?;
    Ok(alliance.flatten())
}
