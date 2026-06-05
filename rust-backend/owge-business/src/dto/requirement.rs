use serde::Serialize;

use crate::dto::{UnitDto, UpgradeDto};

/// Mirrors `UnitWithRequirementInformation` — a unit plus the upgrade-level
/// requirements that gate it for the player's faction (the
/// `unit_requirements_change` sync payload).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UnitWithRequirementInfo {
    pub unit: UnitDto,
    pub requirements: Vec<UnitUpgradeRequirement>,
}

/// Mirrors `UnitUpgradeRequirements` — "needs `upgrade` at `level`".
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UnitUpgradeRequirement {
    pub level: i64,
    pub upgrade: UpgradeDto,
}
