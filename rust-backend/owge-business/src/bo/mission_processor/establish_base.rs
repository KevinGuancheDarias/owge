//! Port of `EstablishBaseMissionProcessor`
//! (`...business.mission.processor.EstablishBaseMissionProcessor`).
//!
//! Optionally triggers a defensive attack; if the mission survives, either claims
//! the (unowned, under-cap) target planet for the user, or registers the return
//! mission and reports why the base could not be established.

use sqlx::MySqlConnection;

use crate::bo::return_mission_registration_bo::ReturnMissionRegistrationBo;
use crate::builder::UnitMissionReportBuilder;
use crate::error::OwgeResult;
use crate::model::mission::{Mission, MissionType};
use crate::model::obtained_unit::ObtainedUnit;

use super::{attack, create_report_base};

/// `GlobalConstants.MAX_PLANETS_MESSAGE`.
const MAX_PLANETS_MESSAGE: &str = "I18N_MAX_PLANETS_EXCEEDED";

pub async fn process(
    conn: &mut MySqlConnection,
    mission: &Mission,
    involved_units: &[ObtainedUnit],
    emits: &mut Vec<super::DeferredEmit>,
) -> OwgeResult<Option<UnitMissionReportBuilder>> {
    let user_id = mission.user_id.unwrap_or_default();
    let target_planet_id = match mission.target_planet {
        Some(p) => p as u64,
        None => return Ok(None),
    };

    if !attack::trigger_attack_if_required(conn, mission, MissionType::EstablishBase, emits).await?
    {
        return Ok(None);
    }

    let mut builder = create_report_base(conn, mission, involved_units).await?;

    let planet_owner = find_planet_owner(conn, target_planet_id).await?;
    let has_max_planets = super::has_max_planets(conn, user_id).await?;

    if planet_owner.is_some() || has_max_planets {
        // registerReturnMission emits emitLocalMissionChangeAfterCommit(returnMission).
        let return_id =
            ReturnMissionRegistrationBo::register_return_mission(conn, mission, None).await?;
        emits.push(super::DeferredEmit::LocalMissionChange {
            mission_id: return_id,
            user_id,
        });
        if planet_owner.is_some() {
            builder = builder.with_establish_base_information(false, "I18N_ALREADY_HAS_OWNER");
        } else {
            builder = builder.with_establish_base_information(false, MAX_PLANETS_MESSAGE);
        }
    } else {
        builder = builder.with_establish_base_information(true, "");
        super::define_planet_as_owned_by(conn, user_id, involved_units, target_planet_id, emits)
            .await?;
    }

    // mission.setResolved(true) is persisted by the report save path.
    // TODO(M3/M4): missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission).
    Ok(Some(builder))
}

/// `targetPlanet.getOwner()` — the planet's owner id, if any.
async fn find_planet_owner(conn: &mut MySqlConnection, planet_id: u64) -> OwgeResult<Option<i32>> {
    let owner: Option<Option<i32>> = sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
        .bind(planet_id)
        .fetch_optional(&mut *conn)
        .await?;
    Ok(owner.flatten())
}
