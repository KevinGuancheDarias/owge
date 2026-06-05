//! Websocket message wrapper for `unit_mission_change`.
//!
//! Mirrors `com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage`
//! (a Lombok `@Builder` with `count` + `myUnitMissions`).

use serde::Serialize;

use crate::dto::UnitRunningMissionDto;

/// The payload pushed on the `unit_mission_change` socket event.
///
/// Jackson serializes the `myUnitMissions` field in camelCase; `count` stays
/// as-is. `#[serde(rename_all = "camelCase")]` covers both.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct MissionWebsocketMessage {
    /// `RunningMissionFinderBo.countUserRunningMissions(userId)`.
    pub count: i32,
    /// `RunningMissionFinderBo.findUserRunningMissions(userId)`.
    pub my_unit_missions: Vec<UnitRunningMissionDto>,
}
