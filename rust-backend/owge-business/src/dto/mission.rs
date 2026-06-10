//! Mission-related output DTOs, mirroring:
//! - `com.kevinguanchedarias.owgejava.dto.MissionDto`
//! - `com.kevinguanchedarias.owgejava.dto.AbstractRunningMissionDto`
//!   (flattened into the concrete running-mission DTO below)
//! - `com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto`
//! - `com.kevinguanchedarias.owgejava.dto.MissionReportDto`
//! - `com.kevinguanchedarias.owgejava.dto.mission.GatherMissionResultDto`
//!
//! All are response payloads, so every struct is Jackson-camelCase
//! (`#[serde(rename_all = "camelCase")]`). The `MissionType` field is emitted as
//! its *code* string (e.g. `"EXPLORE"`) to match Jackson's default enum
//! serialization (`name()`), which differs from the Rust enum's variant name.

use chrono::{NaiveDateTime, Utc};
use serde::Serialize;
use serde_json::Value;

use crate::dto::upgrade::UpgradeDto;
use crate::dto::{ObtainedUnitDto, PlanetDto, SimpleUserData};
use crate::model::{Mission, MissionType};

/// `AbstractRunningMissionDto.NEVER_ENDING_MISSION_SYMBOL`.
pub const NEVER_ENDING_MISSION_SYMBOL: i64 = -1;

/// `AbstractRunningMissionDto.INTENTIONAL_DELAY_MS`.
const INTENTIONAL_DELAY_MS: i64 = 2000;

/// Recomputes the millis pending until a mission's termination date, mirroring
/// `AbstractRunningMissionDto.recalculatePendingMillis()`:
/// `terminationDate.toEpochMilli() - now() + INTENTIONAL_DELAY_MS`, or
/// `NEVER_ENDING_MISSION_SYMBOL` when there is no termination date.
pub fn recalculate_pending_millis(termination_date: Option<NaiveDateTime>) -> i64 {
    match termination_date {
        None => NEVER_ENDING_MISSION_SYMBOL,
        Some(td) => {
            let now_ms = Utc::now().timestamp_millis();
            td.and_utc().timestamp_millis() - now_ms + INTENTIONAL_DELAY_MS
        }
    }
}

/// Serializes an optional [`MissionType`] as its code, or `null`.
fn serialize_opt_mission_type<S>(value: &Option<MissionType>, s: S) -> Result<S::Ok, S::Error>
where
    S: serde::Serializer,
{
    match value {
        Some(v) => s.serialize_str(v.code()),
        None => s.serialize_none(),
    }
}

/// Mirrors `MissionDto` — the minimal mission view (`{id, terminationDate,
/// resolved, invisible}`).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct MissionDto {
    pub id: u64,
    pub termination_date: Option<NaiveDateTime>,
    pub resolved: bool,
    pub invisible: bool,
}

impl From<&Mission> for MissionDto {
    fn from(m: &Mission) -> Self {
        MissionDto {
            id: m.id,
            termination_date: m.termination_date,
            resolved: m.resolved != 0,
            invisible: m.invisible != 0,
        }
    }
}

/// Mirrors `UnitRunningMissionDto`, with the `AbstractRunningMissionDto` base
/// fields flattened in (Jackson serializes the inheritance flat).
///
/// The Java constructor populates `involvedUnits`, `sourcePlanet`,
/// `targetPlanet` and a trimmed `user` from the entity graph. In the Rust port
/// those nested DTOs are built by the calling `Bo` (no lazy entity navigation),
/// so this struct just holds the assembled values. Use
/// [`UnitRunningMissionDto::from_mission`] to fill the base fields off a
/// [`Mission`] and the recomputed `pendingMillis`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UnitRunningMissionDto {
    // --- AbstractRunningMissionDto fields ---
    pub mission_id: u64,
    pub required_primary: Option<f64>,
    pub required_secondary: Option<f64>,
    pub required_time: Option<f64>,
    pub pending_millis: i64,
    #[serde(serialize_with = "serialize_opt_mission_type")]
    pub r#type: Option<MissionType>,
    pub missions_count: Option<i32>,
    pub termination_date: Option<NaiveDateTime>,

    // --- UnitRunningMissionDto fields ---
    pub involved_units: Option<Vec<ObtainedUnitDto>>,
    pub invisible: bool,
    pub source_planet: Option<PlanetDto>,
    pub target_planet: Option<PlanetDto>,
    pub user: Option<SimpleUserData>,
}

impl UnitRunningMissionDto {
    /// Builds the base (`AbstractRunningMissionDto`) portion from a [`Mission`],
    /// mirroring `AbstractRunningMissionDto(Mission)`: copies the resource/time
    /// fields, sets `type` from the mission's discriminant and recomputes
    /// `pendingMillis`. Nested DTOs default to absent; the caller fills them.
    pub fn from_mission(mission: &Mission) -> Self {
        UnitRunningMissionDto {
            mission_id: mission.id,
            required_primary: mission.primary_resource,
            required_secondary: mission.secondary_resource,
            required_time: mission.required_time,
            pending_millis: recalculate_pending_millis(mission.termination_date),
            r#type: mission.mission_type(),
            missions_count: None,
            termination_date: mission.termination_date,
            involved_units: None,
            invisible: mission.invisible != 0,
            source_planet: None,
            target_planet: None,
            user: None,
        }
    }

    /// Mirrors `nullifyInvolvedUnitsPlanets()` — drops the source/target planet
    /// of every involved unit (used when hiding mission origin/target details).
    pub fn nullify_involved_units_planets(&mut self) -> &mut Self {
        if let Some(units) = self.involved_units.as_mut() {
            for u in units.iter_mut() {
                u.source_planet = None;
                u.target_planet = None;
            }
        }
        self
    }

    /// Recomputes `pendingMillis` from the current `terminationDate`
    /// (`recalculatePendingMillis()`), e.g. right before emitting to a client.
    pub fn recalculate_pending_millis(&mut self) {
        self.pending_millis = recalculate_pending_millis(self.termination_date);
    }
}

/// Mirrors `RunningUpgradeDto` (extends `AbstractRunningMissionDto`) — the
/// running LEVEL_UP mission view returned by `registerLevelUp` and emitted on
/// `running_upgrade_change`. The base running-mission fields are flattened in,
/// as with [`UnitRunningMissionDto`].
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RunningUpgradeDto {
    // --- AbstractRunningMissionDto fields ---
    pub mission_id: u64,
    pub required_primary: Option<f64>,
    pub required_secondary: Option<f64>,
    pub required_time: Option<f64>,
    pub pending_millis: i64,
    #[serde(serialize_with = "serialize_opt_mission_type")]
    pub r#type: Option<MissionType>,
    pub missions_count: Option<i32>,
    pub termination_date: Option<NaiveDateTime>,

    // --- RunningUpgradeDto fields ---
    pub upgrade: UpgradeDto,
    /// `mission.getMissionInformation().getValue().intValue()` — the level being
    /// researched (current level + 1).
    pub level: i32,
}

impl RunningUpgradeDto {
    /// Builds from the running LEVEL_UP [`Mission`], its target [`UpgradeDto`] and
    /// the level being researched, mirroring `new RunningUpgradeDto(upgrade,
    /// mission)`. `missionsCount` defaults to absent (the REST controller sets it
    /// on the `registerLevelUp` response; the sync emit leaves it null).
    pub fn from_mission(mission: &Mission, upgrade: UpgradeDto, level: i32) -> Self {
        RunningUpgradeDto {
            mission_id: mission.id,
            required_primary: mission.primary_resource,
            required_secondary: mission.secondary_resource,
            required_time: mission.required_time,
            pending_millis: recalculate_pending_millis(mission.termination_date),
            r#type: mission.mission_type(),
            missions_count: None,
            termination_date: mission.termination_date,
            upgrade,
            level,
        }
    }
}

/// Mirrors `MissionReportDto`. `parsedJson` is the deserialized `jsonBody`;
/// dates are millis-epoch on the wire (Jackson serializes `java.util.Date` as a
/// number), modeled here as `NaiveDateTime` and serialized by the caller's
/// chosen format — kept naive to match the foundation entities.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct MissionReportDto {
    pub id: Option<u64>,
    pub json_body: Option<String>,
    pub parsed_json: Option<Value>,
    pub mission_id: Option<u64>,
    pub mission_date: Option<NaiveDateTime>,
    pub report_date: Option<NaiveDateTime>,
    pub user_read_date: Option<NaiveDateTime>,
    pub is_enemy: Option<bool>,
}

/// Mirrors `dto.mission.GatherMissionResultDto` (`{primaryResource,
/// secondaryResource}`) — the gathered amounts a gather mission produced.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct GatherMissionResultDto {
    pub primary_resource: Option<f64>,
    pub secondary_resource: Option<f64>,
}
