//! `com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto`
//! (extends `AbstractRunningMissionDto`).
//!
//! The running BUILD_UNIT mission view returned by the deprecated
//! `game/unit/findRunning` endpoint and the `unit_build_mission_change` sync
//! payload. The `AbstractRunningMissionDto` base fields are flattened in (Jackson
//! serializes the inheritance flat), mirroring [`crate::dto::mission::RunningUpgradeDto`].

use chrono::NaiveDateTime;
use serde::Serialize;

use crate::dto::mission::recalculate_pending_millis;
use crate::dto::{PlanetDto, UnitDto};
use crate::model::mission::{Mission, MissionType};

/// Serializes an optional [`MissionType`] as its code, or `null` (matching
/// Jackson's `name()` enum serialization).
fn serialize_opt_mission_type<S>(value: &Option<MissionType>, s: S) -> Result<S::Ok, S::Error>
where
    S: serde::Serializer,
{
    match value {
        Some(v) => s.serialize_str(v.code()),
        None => s.serialize_none(),
    }
}

/// Mirrors `RunningUnitBuildDto`. `count` is `Long` in Java; the in-build stack's
/// `obtained_units.count` is `bigint unsigned` in this port, hence `u64`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RunningUnitBuildDto {
    // --- AbstractRunningMissionDto fields ---
    pub mission_id: u64,
    pub required_primary: Option<f64>,
    pub required_secondary: Option<f64>,
    pub required_time: Option<f64>,
    pub pending_millis: i64,
    #[serde(serialize_with = "serialize_opt_mission_type")]
    pub r#type: Option<MissionType>,
    /// Java's `AbstractRunningMissionDto.missionsCount` is `@JsonInclude(NON_NULL)`;
    /// `findRunningUnitBuild` leaves it null, so omit it rather than emit `null`.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub missions_count: Option<i32>,
    pub termination_date: Option<NaiveDateTime>,

    // --- RunningUnitBuildDto fields ---
    pub count: u64,
    pub unit: UnitDto,
    /// The build planet (`PlanetDto` of `mission_information.value`).
    pub source_planet: PlanetDto,
}

impl RunningUnitBuildDto {
    /// Builds from the running BUILD_UNIT [`Mission`], its target [`UnitDto`], the
    /// build [`PlanetDto`] and the in-build stack `count`, mirroring
    /// `new RunningUnitBuildDto(unit, mission, planet, count)`. `missionsCount`
    /// defaults to absent (Java leaves it null on this path).
    pub fn from_mission(
        mission: &Mission,
        unit: UnitDto,
        source_planet: PlanetDto,
        count: u64,
    ) -> Self {
        RunningUnitBuildDto {
            mission_id: mission.id,
            required_primary: mission.primary_resource,
            required_secondary: mission.secondary_resource,
            required_time: mission.required_time,
            pending_millis: recalculate_pending_millis(mission.termination_date),
            r#type: mission.mission_type(),
            missions_count: None,
            termination_date: mission.termination_date,
            count,
            unit,
            source_planet,
        }
    }
}
