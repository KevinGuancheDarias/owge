//! Mirrors `CriticalAttackInformationResponse`
//! (`com.kevinguanchedarias.owgejava.responses.CriticalAttackInformationResponse`).
//!
//! The per-unit `GET game/unit/{unitId}/criticalAttack` payload: one entry per
//! either an explicit `UNIT`-targeted critical rule or per unit type (each unit
//! type gets either its matching rule's value or the default `1.0`). The list is
//! sorted by descending value the same way Java does (`b.value*1000 -
//! a.value*1000`).

use serde::Serialize;

/// Mirrors `CriticalAttackInformationResponse`. `target` is the Java
/// `AttackableTargetEnum` (`UNIT` / `UNIT_TYPE`) serialised as its name; `value`
/// is a Java `float`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct CriticalAttackInformationResponse {
    pub target: String,
    /// Jackson serialises the boxed `Number` (a unit id or unit-type id) as a
    /// plain number.
    pub target_id: u32,
    pub target_name: String,
    pub value: f32,
}
