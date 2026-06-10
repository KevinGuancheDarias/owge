//! Port of `WebsocketSyncService.findWantedData` + the `SyncSource` registry.
//!
//! The Java side discovers `SyncSource` beans and merges their
//! `findSyncHandlers()` maps into one `event-name -> handler` registry. Rust
//! has no bean scanning, so the registry is the explicit [`dispatch`] match
//! below — the single shared file the per-domain modules feed into. Each domain
//! contributes a `Bo` function returning a serializable DTO; adding a sync key
//! is a one-line arm here plus that function.

use chrono::{SubsecRound, Utc};
use serde_json::{Map, Value};
use sqlx::MySqlConnection;

use crate::bo::unlocked::unlocked_unit_finder::UnlockedUnitFinder;
use crate::bo::{
    MissionBo, MissionReportBo, ObtainedUnitBo, PlanetBo, PlanetListBo, RequirementBo,
    RunningMissionFinderBo, SpeedImpactGroupBo, SystemMessageBo, TimeSpecialBo, TutorialBo,
    UnitTypeBo, UpgradeBo, UserStorageBo, WebsocketEventsInformationBo,
};
use crate::dto::mission_websocket::MissionWebsocketMessage;
use crate::error::OwgeResult;

/// `findWantedData(keys)` — for each requested key with a known handler, run it
/// for the logged-in user, record the watermark, and return
/// `{ key: { data, lastSent } }`.
pub async fn find_wanted_data(
    conn: &mut MySqlConnection,
    user_id: i32,
    keys: &[String],
) -> OwgeResult<Map<String, Value>> {
    let mut out = Map::new();
    // Instant.now().truncatedTo(SECONDS) in the Java service.
    let now = Utc::now().trunc_subsecs(0);
    for key in keys {
        if let Some(data) = dispatch(&mut *conn, user_id, key).await? {
            WebsocketEventsInformationBo::save(&mut *conn, key, user_id, now.naive_utc()).await?;
            let mut pair = Map::new();
            pair.insert("data".to_string(), data);
            // lastSent: epoch millis (server clock); see PORTING-ROADMAP parity note.
            pair.insert("lastSent".to_string(), Value::from(now.timestamp_millis()));
            out.insert(key.clone(), Value::Object(pair));
        }
    }
    Ok(out)
}

/// The handler registry. Returns `None` for unknown keys (the Java service
/// simply filters those out). Each arm mirrors one controller's
/// `withHandler("<key>", ...)` registration.
async fn dispatch(
    conn: &mut MySqlConnection,
    user_id: i32,
    key: &str,
) -> OwgeResult<Option<Value>> {
    let value = match key {
        // UserRestService
        "user_data_change" => to_value(UserStorageBo::find_data(&mut *conn, user_id).await?)?,
        // PlanetRestService
        "planet_owned_change" => to_value(PlanetBo::find_owned_dtos(&mut *conn, user_id).await?)?,
        // PlanetListRestService
        "planet_user_list_change" => {
            to_value(PlanetListBo::find_by_user_id(&mut *conn, user_id).await?)?
        }
        // UnitTypeRestService
        "unit_type_change" => {
            to_value(UnitTypeBo::find_unit_types_with_user_info(&mut *conn, user_id).await?)?
        }
        // UnitRestService
        "unit_obtained_change" => {
            to_value(ObtainedUnitBo::find_completed_dtos(&mut *conn, user_id).await?)?
        }
        "unit_unlocked_change" => {
            to_value(UnlockedUnitFinder::find_unlocked_by_user(&mut *conn, user_id).await?)?
        }
        "unit_requirements_change" => to_value(
            RequirementBo::find_faction_unit_level_requirements(&mut *conn, user_id).await?,
        )?,
        // UpgradeTypeRestService / UpgradeRestService
        "upgrade_types_change" => to_value(UpgradeBo::find_upgrade_types(&mut *conn).await?)?,
        "obtained_upgrades_change" => {
            to_value(UpgradeBo::find_obtained_dtos(&mut *conn, user_id).await?)?
        }
        "running_upgrade_change" => {
            to_value(MissionBo::find_running_level_up_mission(&mut *conn, user_id).await?)?
        }
        // TimeSpecialRestService
        "time_special_change" => {
            to_value(TimeSpecialBo::find_user_status_dtos(&mut *conn, user_id).await?)?
        }
        // SystemMessageRestService
        "system_message_change" => {
            to_value(SystemMessageBo::find_read_by_user(&mut *conn, user_id).await?)?
        }
        // TutorialRestService
        "tutorial_entries_change" => to_value(TutorialBo::find_entries(&mut *conn).await?)?,
        "visited_tutorial_entry_change" => {
            to_value(TutorialBo::find_visited_ids_by_user(&mut *conn, user_id).await?)?
        }
        // SpeedImpactRestService
        "speed_impact_group_unlocked_change" => {
            to_value(SpeedImpactGroupBo::find_cross_galaxy_unlocked(&mut *conn, user_id).await?)?
        }
        // Mission-system sync keys (M3 finders, M4-wired here as both the HTTP
        // sync source and the socket emit value).
        "unit_mission_change" => {
            let count =
                RunningMissionFinderBo::count_user_running_missions(&mut *conn, user_id).await?;
            let my_unit_missions =
                RunningMissionFinderBo::find_user_running_missions(&mut *conn, user_id).await?;
            to_value(MissionWebsocketMessage {
                count,
                my_unit_missions,
            })?
        }
        "enemy_mission_change" => to_value(
            RunningMissionFinderBo::find_enemy_running_missions(&mut *conn, user_id).await?,
        )?,
        "missions_count_change" => to_value(
            RunningMissionFinderBo::count_user_running_missions(&mut *conn, user_id).await?,
        )?,
        "unit_build_mission_change" => {
            to_value(RunningMissionFinderBo::find_build_missions(&mut *conn, user_id).await?)?
        }
        "mission_report_change" => {
            // MissionReportBo.emitToUser → findMissionReportsInformation(userId, 0).
            to_value(
                MissionReportBo::find_mission_reports_information(&mut *conn, user_id, 0).await?,
            )?
        }
        _ => return Ok(None),
    };
    Ok(Some(value))
}

fn to_value<T: serde::Serialize>(v: T) -> OwgeResult<Value> {
    Ok(serde_json::to_value(v)?)
}
