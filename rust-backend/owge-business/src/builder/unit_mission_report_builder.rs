//! Port of `UnitMissionReportBuilder`
//! (`com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder`).
//!
//! The Java builder accumulates a `Map<String, Object>` and serialises it to the
//! `mission_reports.json_body` payload the frontend's report viewer consumes.
//! The Rust port accumulates a `serde_json::Map` the same way; the `with_*`
//! methods mirror the Java ones one-for-one. Jackson is configured
//! `Include.NON_NULL`, so we simply never insert null values (and `build_json`
//! omits absent keys).
//!
//! This is the shared report API every mission processor and the combat manager
//! builds against; its method set is frozen (see `docs/M3-CONTRACTS.md`).

use serde::Serialize;
use serde_json::{Map, Value};

use crate::dto::obtained_unit::ObtainedUnitDto;
use crate::dto::PlanetDto;
use crate::error::{OwgeError, OwgeResult};

#[derive(Default)]
pub struct UnitMissionReportBuilder {
    map: Map<String, Value>,
}

impl UnitMissionReportBuilder {
    pub fn create() -> Self {
        Self { map: Map::new() }
    }

    /// `create(user, sourcePlanet, targetPlanet, selectedUnits)` convenience.
    pub fn create_with(
        sender_user_id: i32,
        sender_username: &str,
        source_planet: Option<&PlanetDto>,
        target_planet: Option<&PlanetDto>,
        involved_units: &[ObtainedUnitDto],
    ) -> Self {
        let mut b = Self::create()
            .with_sender_user(sender_user_id, sender_username)
            .with_involved_units(involved_units);
        if let Some(sp) = source_planet {
            b = b.with_source_planet(sp);
        }
        if let Some(tp) = target_planet {
            b = b.with_target_planet(tp);
        }
        b
    }

    fn put(mut self, key: &str, value: Value) -> Self {
        if !value.is_null() {
            self.map.insert(key.to_string(), value);
        }
        self
    }

    /// Insert any pre-built JSON under `key` (escape hatch for the complex
    /// combat/interception/capture sub-objects whose shape the producing module
    /// owns).
    pub fn with_raw(self, key: &str, value: Value) -> Self {
        self.put(key, value)
    }

    pub fn with_id(self, id: u64) -> Self {
        self.put("id", Value::from(id))
    }

    /// `withSenderUser` — Java embeds only `{id, username}`.
    pub fn with_sender_user(self, user_id: i32, username: &str) -> Self {
        self.put(
            "senderUser",
            serde_json::json!({ "id": user_id, "username": username }),
        )
    }

    pub fn with_source_planet(self, planet: &PlanetDto) -> Self {
        self.put("sourcePlanet", to_value(planet))
    }

    pub fn with_target_planet(self, planet: &PlanetDto) -> Self {
        self.put("targetPlanet", to_value(planet))
    }

    pub fn with_involved_units(self, units: &[ObtainedUnitDto]) -> Self {
        self.put("involvedUnits", strip_units(units))
    }

    pub fn with_explored_information(self, units_in_planet: &[ObtainedUnitDto]) -> Self {
        self.put("unitsInPlanet", strip_units(units_in_planet))
    }

    pub fn with_gather_information(self, primary: f64, secondary: f64) -> Self {
        self.put("gatheredPrimary", Value::from(primary))
            .put("gatheredSecondary", Value::from(secondary))
    }

    pub fn with_establish_base_information(self, status: bool, status_str: &str) -> Self {
        self.put("establishBaseStatus", Value::from(status))
            .put("establishBaseStatusStr", Value::from(status_str))
    }

    pub fn with_conquest_information(self, status: bool, status_str: &str) -> Self {
        self.put("conquestStatus", Value::from(status))
            .put("conquestStatusStr", Value::from(status_str))
    }

    pub fn with_error_information(self, error_text: &str) -> Self {
        self.put("errorText", Value::from(error_text))
    }

    /// `withAttackInformation` — the attack sub-object is built by the combat
    /// manager (it owns `AttackInformation`'s shape) and passed in as JSON.
    pub fn with_attack_information(self, attack_information: Value) -> Self {
        self.put("attackInformation", attack_information)
    }

    pub fn with_interception_information(self, interception_info: Value) -> Self {
        self.put("interceptionInfo", interception_info)
    }

    pub fn with_unit_capture_information(self, unit_capture_information: Value) -> Self {
        self.put("unitCaptureInformation", unit_capture_information)
    }

    /// The accumulated map (Java `build()`).
    pub fn build(self) -> Map<String, Value> {
        self.map
    }

    /// Serialise to the `json_body` string (Java `buildJson()`).
    pub fn build_json(&self) -> OwgeResult<String> {
        serde_json::to_string(&self.map)
            .map_err(|e| OwgeError::Common(format!("Could not convert report to JSON: {e}")))
    }
}

fn to_value<T: Serialize>(value: &T) -> Value {
    serde_json::to_value(value).unwrap_or(Value::Null)
}

/// Mirror the Java `obtainedUnitToDto` cleanup: drop the per-unit
/// source/target/mission references so the embedded snapshot is compact.
fn strip_units(units: &[ObtainedUnitDto]) -> Value {
    let cleaned: Vec<Value> = units
        .iter()
        .map(|u| {
            let mut v = to_value(u);
            if let Value::Object(ref mut o) = v {
                o.remove("sourcePlanet");
                o.remove("targetPlanet");
                o.remove("mission");
            }
            v
        })
        .collect();
    Value::Array(cleaned)
}
