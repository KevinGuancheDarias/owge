//! Port of `CounterattackMissionProcessor`
//! (`...business.mission.processor.CounterattackMissionProcessor`).
//!
//! A counterattack is processed exactly like an attack — Java delegates to
//! `attackMissionProcessor.process(mission, involvedUnits)`.

use sqlx::MySqlConnection;

use crate::builder::UnitMissionReportBuilder;
use crate::error::OwgeResult;
use crate::model::mission::Mission;
use crate::model::obtained_unit::ObtainedUnit;

use super::attack;

pub async fn process(
    conn: &mut MySqlConnection,
    mission: &Mission,
    involved_units: &[ObtainedUnit],
    emits: &mut Vec<super::DeferredEmit>,
) -> OwgeResult<Option<UnitMissionReportBuilder>> {
    attack::process(conn, mission, involved_units, emits).await
}
