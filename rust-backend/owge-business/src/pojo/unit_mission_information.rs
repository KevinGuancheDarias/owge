//! Mirrors `com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation` and
//! `com.kevinguanchedarias.owgejava.pojo.SelectedUnit` — the request body the
//! frontend sends when registering a unit-based mission (explore, gather,
//! attack, deploy, ...).
//!
//! These are *input* shapes: Jackson serializes/deserializes them in camelCase,
//! so the Rust side uses `#[serde(rename_all = "camelCase")]`. The numeric
//! widths follow the Java `Integer`/`Long` boxed types: `userId` is a signed
//! `int` (`i32`); the planet ids are signed `long` (`i64`, matching the
//! *signed* `missions.source_planet`/`target_planet` columns); `count` and
//! `expirationId`/`wantedTime` are `long` (`i64`).

use serde::{Deserialize, Serialize};

use crate::model::MissionType;

/// Required information to register an "unit based mission".
///
/// All fields are optional except `targetPlanetId` because the frontend body
/// omits server-derived values (`userId` is taken from the JWT, `missionType`
/// is set by the registering endpoint, `sourcePlanetId` may be absent for some
/// flows). The registration `Bo` fills in / validates these before persisting.
#[derive(Debug, Clone, Default, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct UnitMissionInformation {
    /// Signed `int` user id — usually overwritten from the authenticated user.
    #[serde(default)]
    pub user_id: Option<i32>,
    /// Signed `long` planet id (mirrors the signed `missions.source_planet`).
    #[serde(default)]
    pub source_planet_id: Option<i64>,
    /// Signed `long` planet id (mirrors the signed `missions.target_planet`).
    pub target_planet_id: i64,
    /// The mission type; set by the registering endpoint, not trusted from the
    /// raw body. Serialized as the enum *name* (matching `MissionType`'s serde).
    #[serde(default)]
    pub mission_type: Option<MissionType>,
    /// Optional caller-requested duration override, in seconds (`Long`).
    #[serde(default)]
    pub wanted_time: Option<i64>,
    /// The units the player wants to send on the mission.
    #[serde(default)]
    pub involved_units: Vec<SelectedUnit>,
}

/// Mirrors `SelectedUnit` — one entry of the units a player sends on a mission.
///
/// `storedUnits` is the recursive nesting used when a carrier unit transports
/// other units; it is optional and omitted for the common flat case.
#[derive(Debug, Clone, Serialize, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct SelectedUnit {
    /// `Integer` — the `units.id` (`smallint unsigned` in the DB, but the Java
    /// POJO boxes it as a signed `Integer`, so we keep `i32` for wire parity).
    pub id: i32,
    /// `Long` — how many of this unit to send.
    pub count: i64,
    /// `Long`, nullable — the obtained-unit expiration id, when the selection
    /// targets a specific time-special-limited stack.
    #[serde(default)]
    pub expiration_id: Option<i64>,
    /// Recursive nested selection (units carried inside this one).
    #[serde(default)]
    pub stored_units: Option<Vec<SelectedUnit>>,
}
