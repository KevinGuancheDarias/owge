//! Port of `ObtainedUnitEventEmitter`
//! (`com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter`).
//!
//! Three emitters:
//! * `emit_obtained_units`  — `unit_obtained_change`
//! * `emit_unit_type_change` — `unit_type_change`
//! * `emit_side_changes`    — composite: optionally `user_data_change`, then
//!                            `unit_type_change` + `unit_obtained_change`.
//!
//! The caller passes `any_unit_has_energy: bool` to `emit_side_changes`, mirroring
//! Java's `isOneUnitHavingEnergy` predicate (unit.energy != null && energy > 0).
//! Computing that predicate is a call-site concern; the emitter only acts on it.

use crate::bo::emitter::unit_type_emitter::UnitTypeEmitter;
use crate::error::OwgeResult;
use crate::websocket::emitter;
use sqlx::MySqlConnection;

pub struct ObtainedUnitEventEmitter;

impl ObtainedUnitEventEmitter {
    /// Emits `unit_obtained_change` — the list of completed obtained-unit DTOs.
    ///
    /// Java: `ObtainedUnitEventEmitter.emitObtainedUnits` ->
    /// `socketIoService.sendMessage(user, UNIT_OBTAINED_CHANGE, () -> obtainedUnitFinderBo.findCompletedAsDto(user))`.
    pub async fn emit_obtained_units(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(conn, user_id, "unit_obtained_change", |conn| {
            Box::pin(async move {
                Ok(serde_json::to_value(
                    crate::bo::ObtainedUnitBo::find_completed_dtos(&mut *conn, user_id).await?,
                )?)
            })
        })
        .await
    }

    /// Emits the bundle of side-effects caused by a unit alteration.
    ///
    /// Java: `ObtainedUnitEventEmitter.emitSideChanges(List<ObtainedUnit>)`.
    /// * If `any_unit_has_energy` — emit `user_data_change` (energy totals changed).
    /// * Always emit `unit_type_change` + `unit_obtained_change`.
    ///
    /// The caller is responsible for determining `any_unit_has_energy`
    /// (i.e. whether any affected unit has `energy > 0`).
    pub async fn emit_side_changes(
        conn: &mut MySqlConnection,
        user_id: i32,
        any_unit_has_energy: bool,
    ) -> OwgeResult<()> {
        if any_unit_has_energy {
            crate::bo::user_event_emitter::UserEventEmitter::emit_user_data(conn, user_id).await?;
        }
        UnitTypeEmitter::emit_unit_type_change(conn, user_id).await?;
        Self::emit_obtained_units(conn, user_id).await?;
        Ok(())
    }
}
