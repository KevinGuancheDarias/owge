//! Persistence entities for the mission engine — the Rust counterpart of the
//! Java `Mission`, `MissionInformation`, and `MissionReport` JPA entities
//! (`com.kevinguanchedarias.owgejava.entity`), over the `missions`,
//! `mission_information`, and `mission_reports` tables.
//!
//! **sqlx signedness is load-bearing** (see roadmap §0.3): `missions.id` is
//! `bigint UNSIGNED` (`u64`) but `missions.source_planet`/`target_planet` are
//! *signed* `bigint` (`i64`) even though they reference the unsigned
//! `planets.id`; `missions.user_id` is signed `int` (`i32`). Decode each column
//! at its literal column type or sqlx panics at runtime.

use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};

/// Mirrors the `missions` table / Java `Mission` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct Mission {
    /// `bigint unsigned`.
    pub id: u64,
    /// `int` (signed, nullable) — the owning user id, `NULL` for a core mission.
    #[sqlx(rename = "user_id")]
    pub user_id: Option<i32>,
    /// `smallint unsigned` — FK to `mission_types.id`; the value equals the
    /// [`MissionType`] discriminant (see the seed in `04_insert_data.sql`).
    #[sqlx(rename = "type")]
    pub type_id: u16,
    #[sqlx(rename = "termination_date")]
    pub termination_date: Option<NaiveDateTime>,
    #[sqlx(rename = "required_time")]
    pub required_time: Option<f64>,
    #[sqlx(rename = "starting_date")]
    pub starting_date: NaiveDateTime,
    #[sqlx(rename = "primary_resource")]
    pub primary_resource: Option<f64>,
    #[sqlx(rename = "secondary_resource")]
    pub secondary_resource: Option<f64>,
    #[sqlx(rename = "required_energy")]
    pub required_energy: Option<f64>,
    /// `bigint` **signed**, nullable — references the unsigned `planets.id`.
    #[sqlx(rename = "source_planet")]
    pub source_planet: Option<i64>,
    /// `bigint` **signed**, nullable — references the unsigned `planets.id`.
    #[sqlx(rename = "target_planet")]
    pub target_planet: Option<i64>,
    /// `bigint unsigned`, nullable.
    #[sqlx(rename = "related_mission")]
    pub related_mission: Option<u64>,
    /// `bigint unsigned`, nullable.
    #[sqlx(rename = "report_id")]
    pub report_id: Option<u64>,
    /// `tinyint unsigned`, default 1.
    pub attemps: u8,
    /// `tinyint` (stored as a number, not the JPA boolean default).
    pub resolved: i8,
    /// `tinyint` (stored as a number).
    pub invisible: i8,
}

impl Mission {
    /// The strongly-typed mission type, resolved from the `type` discriminant.
    pub fn mission_type(&self) -> Option<MissionType> {
        MissionType::from_value(self.type_id)
    }

    pub fn is_resolved(&self) -> bool {
        self.resolved != 0
    }
}

/// Mirrors the `mission_information` table / Java `MissionInformation` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct MissionInformation {
    /// `bigint unsigned`.
    pub id: u64,
    /// `bigint unsigned`.
    #[sqlx(rename = "mission_id")]
    pub mission_id: u64,
    /// `smallint unsigned`, nullable — the `object_relations` relation id.
    #[sqlx(rename = "relation_id")]
    pub relation_id: Option<u16>,
    pub value: Option<f64>,
}

/// Mirrors the `mission_reports` table / Java `MissionReport` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct MissionReport {
    /// `bigint unsigned`.
    pub id: u64,
    /// `mediumtext` — the JSON report payload.
    #[sqlx(rename = "json_body")]
    pub json_body: String,
    /// `int` (signed).
    #[sqlx(rename = "user_id")]
    pub user_id: i32,
    #[sqlx(rename = "report_date")]
    pub report_date: Option<NaiveDateTime>,
    /// `tinyint(1)` — decodes as `bool`.
    #[sqlx(rename = "is_enemy")]
    pub is_enemy: Option<bool>,
    #[sqlx(rename = "user_read_date")]
    pub user_read_date: Option<NaiveDateTime>,
}

/// Port of `com.kevinguanchedarias.owgejava.enumerations.MissionType`.
///
/// The discriminant is the `mission_types.id` value (verified against the seed
/// data), so [`MissionType::from_value`] maps a `missions.type` column straight
/// to the enum and [`MissionType::value`] back.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash, Serialize, Deserialize)]
pub enum MissionType {
    LevelUp = 1,
    BroadcastMessage = 2,
    BuildUnit = 3,
    Explore = 4,
    ReturnMission = 5,
    Gather = 6,
    EstablishBase = 7,
    Attack = 8,
    Counterattack = 9,
    Conquest = 10,
    Deploy = 11,
    Deployed = 12,
}

impl MissionType {
    pub fn value(self) -> u16 {
        self as u16
    }

    pub fn from_value(value: u16) -> Option<MissionType> {
        Some(match value {
            1 => MissionType::LevelUp,
            2 => MissionType::BroadcastMessage,
            3 => MissionType::BuildUnit,
            4 => MissionType::Explore,
            5 => MissionType::ReturnMission,
            6 => MissionType::Gather,
            7 => MissionType::EstablishBase,
            8 => MissionType::Attack,
            9 => MissionType::Counterattack,
            10 => MissionType::Conquest,
            11 => MissionType::Deploy,
            12 => MissionType::Deployed,
            _ => return None,
        })
    }

    /// The enum *name* as stored in `mission_types.code` (e.g. `ESTABLISH_BASE`).
    pub fn code(self) -> &'static str {
        match self {
            MissionType::LevelUp => "LEVEL_UP",
            MissionType::BroadcastMessage => "BROADCAST_MESSAGE",
            MissionType::BuildUnit => "BUILD_UNIT",
            MissionType::Explore => "EXPLORE",
            MissionType::ReturnMission => "RETURN_MISSION",
            MissionType::Gather => "GATHER",
            MissionType::EstablishBase => "ESTABLISH_BASE",
            MissionType::Attack => "ATTACK",
            MissionType::Counterattack => "COUNTERATTACK",
            MissionType::Conquest => "CONQUEST",
            MissionType::Deploy => "DEPLOY",
            MissionType::Deployed => "DEPLOYED",
        }
    }

    pub fn from_code(code: &str) -> Option<MissionType> {
        Some(match code {
            "LEVEL_UP" => MissionType::LevelUp,
            "BROADCAST_MESSAGE" => MissionType::BroadcastMessage,
            "BUILD_UNIT" => MissionType::BuildUnit,
            "EXPLORE" => MissionType::Explore,
            "RETURN_MISSION" => MissionType::ReturnMission,
            "GATHER" => MissionType::Gather,
            "ESTABLISH_BASE" => MissionType::EstablishBase,
            "ATTACK" => MissionType::Attack,
            "COUNTERATTACK" => MissionType::Counterattack,
            "CONQUEST" => MissionType::Conquest,
            "DEPLOY" => MissionType::Deploy,
            "DEPLOYED" => MissionType::Deployed,
            _ => return None,
        })
    }

    /// `MissionType.isUnitMission()` — value in `[4, 11]` (EXPLORE..DEPLOY).
    pub fn is_unit_mission(self) -> bool {
        let v = self.value();
        (4..=11).contains(&v)
    }
}
