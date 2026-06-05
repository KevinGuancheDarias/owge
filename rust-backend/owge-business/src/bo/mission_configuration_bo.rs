//! Port of `business/mission/MissionConfigurationBo.java`.
//!
//! Holds the per-mission-type base time (in seconds) in the `configuration`
//! key/value table. The Java bean inserts every default at boot (`@PostConstruct
//! insertMissionBaseTimeIfMissing`); here we read lazily via
//! [`ConfigurationBo::find_or_set_default`], which inserts the default the first
//! time the key is read — same observable result without a boot hook.

use crate::bo::ConfigurationBo;
use crate::db::Db;
use crate::error::{OwgeError, OwgeResult};
use crate::model::mission::MissionType;

/// Configuration keys (mirrors the `MISSION_TIME_*_KEY` constants).
pub const MISSION_TIME_EXPLORE_KEY: &str = "MISSION_TIME_EXPLORE";
pub const MISSION_TIME_GATHER_KEY: &str = "MISSION_TIME_GATHER";
pub const MISSION_TIME_ESTABLISH_BASE_KEY: &str = "MISSION_TIME_ESTABLISH_BASE";
pub const MISSION_TIME_ATTACK_KEY: &str = "MISSION_TIME_ATTACK";
pub const MISSION_TIME_CONQUEST_KEY: &str = "MISSION_TIME_CONQUEST";
pub const MISSION_TIME_COUNTERATTACK_KEY: &str = "MISSION_TIME_COUNTERATTACK";
pub const MISSION_TIME_DEPLOY_KEY: &str = "MISSION_TIME_DEPLOY";

/// Defaults (mirrors the `DEFAULT_TIME_*` constants).
const DEFAULT_TIME_EXPLORE: &str = "60";
const DEFAULT_TIME_GATHER: &str = "900";
const DEFAULT_TIME_ESTABLISH_BASE: &str = "43200";
const DEFAULT_TIME_ATTACK: &str = "600";
const DEFAULT_TIME_CONQUEST: &str = "86400";
const DEFAULT_TIME_COUNTERATTACK: &str = "60";
const DEFAULT_TIME_DEPLOY: &str = "60";

pub struct MissionConfigurationBo;

impl MissionConfigurationBo {
    /// `findMissionBaseTimeByType` — the per-type config key + its default. Reads
    /// (and lazily inserts) the value via `ConfigurationBo::find_or_set_default`.
    /// Unsupported mission types raise `InvalidInput` (the Java
    /// `SgtBackendInvalidInputException`).
    pub async fn find_mission_base_time(db: &Db, mission_type: MissionType) -> OwgeResult<i64> {
        let (key, default) = match mission_type {
            MissionType::Explore => (MISSION_TIME_EXPLORE_KEY, DEFAULT_TIME_EXPLORE),
            MissionType::Gather => (MISSION_TIME_GATHER_KEY, DEFAULT_TIME_GATHER),
            MissionType::EstablishBase => {
                (MISSION_TIME_ESTABLISH_BASE_KEY, DEFAULT_TIME_ESTABLISH_BASE)
            }
            MissionType::Attack => (MISSION_TIME_ATTACK_KEY, DEFAULT_TIME_ATTACK),
            MissionType::Counterattack => {
                (MISSION_TIME_COUNTERATTACK_KEY, DEFAULT_TIME_COUNTERATTACK)
            }
            MissionType::Conquest => (MISSION_TIME_CONQUEST_KEY, DEFAULT_TIME_CONQUEST),
            MissionType::Deploy => (MISSION_TIME_DEPLOY_KEY, DEFAULT_TIME_DEPLOY),
            other => {
                return Err(OwgeError::InvalidInput(format!(
                    "Unsupported mission base time type, specified: {}",
                    other.code()
                )))
            }
        };
        let value = ConfigurationBo::find_or_set_default(db, key, default)
            .await?
            .value;
        value.trim().parse::<i64>().map_err(|_| {
            OwgeError::Common(format!(
                "Mission base time config {key} has a non-numeric value: {value}"
            ))
        })
    }
}
