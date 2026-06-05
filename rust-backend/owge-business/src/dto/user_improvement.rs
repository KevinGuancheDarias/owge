//! Runtime aggregate of a user's improvements — the Rust counterpart of the Java
//! `GroupedImprovement` pojo (which extends `AbstractImprovementDto`).
//!
//! Built by [`crate::bo::user_improvement_bo::UserImprovementBo::find_user_improvement`]
//! from the user's improvement *sources* (unlocked upgrades × level, active time
//! specials, and non-building obtained units). It exposes the summed flat
//! improvement values plus the per-unit-type improvements, and the
//! `find_unit_type_improvement` lookup the time-math and combat code uses.
//!
//! The flat values are kept as `f64` (the Java fields are `Float`, summed); the
//! mission time math widens the unit-type SPEED improvement to a `double` rational
//! (see `ImprovementBo.findAsRational`).

/// Port of `enumerations.ImprovementTypeEnum` — the `improvements_unit_types.type`
/// values. Lives here (rather than a shared `enumerations` module) because no such
/// module exists yet in the Rust tree; promote it when one is introduced.
#[derive(Debug, Clone, Copy, PartialEq, Eq, Hash)]
pub enum ImprovementType {
    Attack,
    Defense,
    Shield,
    Speed,
    Amount,
}

impl ImprovementType {
    /// The stored enum string (`ATTACK`, `DEFENSE`, ...).
    pub fn code(self) -> &'static str {
        match self {
            ImprovementType::Attack => "ATTACK",
            ImprovementType::Defense => "DEFENSE",
            ImprovementType::Shield => "SHIELD",
            ImprovementType::Speed => "SPEED",
            ImprovementType::Amount => "AMOUNT",
        }
    }

    pub fn from_code(code: &str) -> Option<ImprovementType> {
        Some(match code {
            "ATTACK" => ImprovementType::Attack,
            "DEFENSE" => ImprovementType::Defense,
            "SHIELD" => ImprovementType::Shield,
            "SPEED" => ImprovementType::Speed,
            "AMOUNT" => ImprovementType::Amount,
            _ => return None,
        })
    }
}

/// One summed per-unit-type improvement entry, keyed by `(type, unit_type_id)`
/// (mirrors `ImprovementUnitTypeDto` reduced inside `GroupedImprovement`).
#[derive(Debug, Clone)]
pub struct UnitTypeImprovementEntry {
    /// The improvement type (`ATTACK`/`DEFENSE`/`SHIELD`/`AMOUNT`/`SPEED`).
    pub improvement_type: ImprovementType,
    /// The targeted `unit_types.id`.
    pub unit_type_id: u16,
    /// The accumulated value (`improvements_unit_types.value` summed).
    pub value: i64,
}

/// Mirrors `GroupedImprovement` — the full sum of a user's improvements.
#[derive(Debug, Clone, Default)]
pub struct UserImprovementDto {
    pub more_primary_resource_production: f64,
    pub more_secondary_resource_production: f64,
    pub more_energy_production: f64,
    pub more_charge_capacity: f64,
    /// `moreMissions` (`more_missions_value`); `findUserImprovement` adds a base
    /// `1.0` after summing the sources (the "always one extra mission slot").
    pub more_missions: f64,
    pub more_upgrade_research_speed: f64,
    pub more_unit_build_speed: f64,
    /// Per-unit-type improvements, summed by `(type, unit_type_id)`.
    pub unit_types_upgrades: Vec<UnitTypeImprovementEntry>,
}

impl UserImprovementDto {
    /// `GroupedImprovement.findUnitTypeImprovement` — the summed value of a unit
    /// type improvement for a given unit type, **including** the value inherited
    /// from the parent unit type when `has_to_inherit_improvements` is set.
    ///
    /// Inheritance is resolved by the caller (it needs the `unit_types` parent
    /// chain), so this overload takes the already-resolved chain of unit-type ids
    /// to sum over. The single-id form [`find_unit_type_improvement`] is the
    /// direct (non-inherited) lookup for code that has no chain.
    ///
    /// Returns the raw summed percentage (e.g. `25` for +25%); callers apply
    /// `ImprovementBo.findAsRational` (`/ 100`) before using it.
    pub fn find_unit_type_improvement_for_chain(
        &self,
        improvement_type: ImprovementType,
        unit_type_chain: &[u16],
    ) -> f64 {
        self.unit_types_upgrades
            .iter()
            .filter(|e| {
                e.improvement_type == improvement_type && unit_type_chain.contains(&e.unit_type_id)
            })
            .map(|e| e.value)
            .sum::<i64>() as f64
    }

    /// Direct (non-inherited) `findUnitTypeImprovement` for a single unit type id.
    /// Use [`find_unit_type_improvement_for_chain`](Self::find_unit_type_improvement_for_chain)
    /// when parent inheritance must be honoured.
    pub fn find_unit_type_improvement(
        &self,
        improvement_type: ImprovementType,
        unit_type_id: u16,
    ) -> f64 {
        self.find_unit_type_improvement_for_chain(improvement_type, &[unit_type_id])
    }

    /// `GroupedImprovement.addToType` — accumulate a per-unit-type improvement,
    /// merging into an existing `(type, unit_type_id)` entry when present.
    pub fn add_unit_type_improvement(
        &mut self,
        improvement_type: ImprovementType,
        unit_type_id: u16,
        value: i64,
    ) {
        if let Some(existing) = self.unit_types_upgrades.iter_mut().find(|e| {
            e.improvement_type == improvement_type && e.unit_type_id == unit_type_id
        }) {
            existing.value += value;
        } else {
            self.unit_types_upgrades.push(UnitTypeImprovementEntry {
                improvement_type,
                unit_type_id,
                value,
            });
        }
    }

    /// Build the `user_improvements_change` websocket value (the Jackson shape of
    /// `GroupedImprovement` extending `AbstractImprovementDto`).
    pub fn to_wire(&self) -> GroupedImprovementWire {
        GroupedImprovementWire {
            more_primary_resource_production: self.more_primary_resource_production,
            more_secondary_resource_production: self.more_secondary_resource_production,
            more_energy_production: self.more_energy_production,
            more_charge_capacity: self.more_charge_capacity,
            more_missions: self.more_missions,
            more_upgrade_research_speed: self.more_upgrade_research_speed,
            more_unit_build_speed: self.more_unit_build_speed,
            unit_types_upgrades: self
                .unit_types_upgrades
                .iter()
                .map(|e| ImprovementUnitTypeWire {
                    improvement_type: e.improvement_type.code(),
                    unit_type_id: e.unit_type_id,
                    unit_type: UnitTypeRefWire { id: e.unit_type_id },
                    value: e.value,
                })
                .collect(),
        }
    }
}

/// Wire shape of `GroupedImprovement` for the `user_improvements_change` socket
/// event. The frontend's `ImprovementUtil.findUnitTypeImprovement` reads each
/// unit-type entry's `type`, `value` and **`unitType.id`** (the legacy
/// `unitTypeId`/`unitTypeName` are deprecated), so each entry carries a minimal
/// nested `unitType { id }`; the full `UnitTypeDto` graph + `id`/`unitTypeName`
/// Java also emits are unused by the consumer and omitted.
#[derive(serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct GroupedImprovementWire {
    pub more_primary_resource_production: f64,
    pub more_secondary_resource_production: f64,
    pub more_energy_production: f64,
    pub more_charge_capacity: f64,
    pub more_missions: f64,
    pub more_upgrade_research_speed: f64,
    pub more_unit_build_speed: f64,
    pub unit_types_upgrades: Vec<ImprovementUnitTypeWire>,
}

#[derive(serde::Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ImprovementUnitTypeWire {
    #[serde(rename = "type")]
    pub improvement_type: &'static str,
    pub unit_type_id: u16,
    pub unit_type: UnitTypeRefWire,
    pub value: i64,
}

#[derive(serde::Serialize)]
pub struct UnitTypeRefWire {
    pub id: u16,
}
