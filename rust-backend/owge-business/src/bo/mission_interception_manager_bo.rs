//! Port of `MissionInterceptionManagerBo`
//! (`com.kevinguanchedarias.owgejava.business.mission.MissionInterceptionManagerBo`)
//! and the `MissionInterceptionInformation` / `InterceptedUnitsInformation` pojos.
//!
//! Before a unit mission is processed, the engine checks whether the units in
//! flight are intercepted (by defensive interceptors keyed on speed-impact
//! groups). Fully-intercepted missions are resolved into an interception report
//! and never reach their target; partially-intercepted ones lose the intercepted
//! stacks and proceed with what remains.
//!
//! The interceptor detection itself lives in
//! [`UnitInterceptionFinderBo`](crate::bo::unit_interception_finder_bo) — this Bo
//! drives it: `load_information` computes the intercepted stacks, deletes them
//! when any were intercepted and re-reads the surviving involved set;
//! `maybe_append_data_to_mission_report` attaches the original units +
//! interception breakdown to a partially-intercepted mission's report; and
//! `handle_mission_interception` writes the full-interception report (via
//! [`MissionReportManagerBo`](crate::bo::mission_report_manager_bo)) and deletes
//! the intercepted stacks.
//!
//! ## sqlx signedness
//! `obtained_units` columns are decoded at their literal MySQL types (see the
//! `ObtainedUnit` model); `missions.id` is `bigint unsigned` (`u64`).

use serde_json::{Value, json};
use sqlx::MySqlConnection;

use crate::bo::unit_interception_finder_bo::{
    InterceptedUnitsInformation, UnitInterceptionFinderBo,
};
use crate::builder::UnitMissionReportBuilder;
use crate::error::OwgeResult;
use crate::model::mission::{Mission, MissionType};
use crate::model::obtained_unit::ObtainedUnit;

/// Rust counterpart of `MissionInterceptionInformation`.
#[derive(Debug, Clone)]
pub struct InterceptionInformation {
    /// `isMissionIntercepted` — every in-flight unit was intercepted.
    pub is_mission_intercepted: bool,
    /// The units still involved in the mission after interception (the survivors
    /// re-read after intercepted stacks were deleted).
    pub involved_units: Vec<ObtainedUnit>,
    /// `originallyInvolved` — the involved units before any interception removal,
    /// kept for the report (`withInvolvedUnits`).
    pub originally_involved: Vec<ObtainedUnit>,
    /// `totalInterceptedUnits` — number of intercepted stacks.
    pub total_intercepted_units: usize,
    /// `interceptedUnits` — the per-interceptor breakdown (empty for RETURN
    /// missions and when nothing was intercepted).
    pub intercepted_units: Vec<InterceptedUnitsInformation>,
}

pub struct MissionInterceptionManagerBo;

impl MissionInterceptionManagerBo {
    /// `loadInformation` — determine the interception state for a mission.
    ///
    /// For `RETURN_MISSION` interception never applies (units coming home are not
    /// intercepted), matching the Java early-out.
    pub async fn load_information(
        conn: &mut MySqlConnection,
        mission: &Mission,
        mission_type: MissionType,
    ) -> OwgeResult<InterceptionInformation> {
        let mut involved_units = Self::find_units_involved(conn, mission.id).await?;
        let originally_involved = involved_units.clone();

        if mission_type == MissionType::ReturnMission {
            return Ok(InterceptionInformation {
                is_mission_intercepted: false,
                involved_units,
                originally_involved,
                total_intercepted_units: 0,
                intercepted_units: Vec::new(),
            });
        }

        let intercepted_units = UnitInterceptionFinderBo::check_intercepts_speed_impact_group(
            conn,
            mission,
            &involved_units,
        )
        .await?;
        let total_intercepted_units: usize = intercepted_units
            .iter()
            .map(|info| info.intercepted_units.len())
            .sum();
        let is_mission_intercepted = total_intercepted_units == involved_units.len();
        if total_intercepted_units > 0 {
            Self::delete_intercepted_units(conn, &intercepted_units).await?;
            involved_units = Self::find_units_involved(conn, mission.id).await?;
        }

        Ok(InterceptionInformation {
            is_mission_intercepted,
            involved_units,
            originally_involved,
            total_intercepted_units,
            intercepted_units,
        })
    }

    /// `maybeAppendDataToMissionReport` — when some (but not all) units were
    /// intercepted, the surviving mission's report records the *original* unit set
    /// plus the interception breakdown, and the interceptor users are notified.
    pub async fn maybe_append_data_to_mission_report(
        conn: &mut MySqlConnection,
        mission: &Mission,
        report_builder: Option<UnitMissionReportBuilder>,
        interception_information: &InterceptionInformation,
    ) -> OwgeResult<(Option<UnitMissionReportBuilder>, Vec<(i32, u64)>)> {
        if interception_information.total_intercepted_units != 0 {
            if let Some(builder) = report_builder {
                // reportBuilder.withInvolvedUnits(originallyInvolved): the report
                // shows the units as they were before interception removal.
                let originally_involved_dtos = Self::obtained_units_to_report_dtos(
                    conn,
                    &interception_information.originally_involved,
                )
                .await?;
                let interception_json = Self::build_interception_info_json(
                    conn,
                    &interception_information.intercepted_units,
                )
                .await?;
                let builder = builder
                    .with_involved_units(&originally_involved_dtos)
                    .with_interception_information(interception_json);
                // sendReportToInterceptorUsers(...): per-interceptor reports; the
                // returned (user, report) pairs are emitted post-commit by the caller.
                let interceptor_pairs = UnitInterceptionFinderBo::send_report_to_interceptor_users(
                    conn,
                    mission,
                    &interception_information.intercepted_units,
                )
                .await?;
                return Ok((Some(builder), interceptor_pairs));
            }
            return Ok((None, Vec::new()));
        }
        Ok((report_builder, Vec::new()))
    }

    /// `handleMissionInterception` — the mission was *fully* intercepted: mark it
    /// resolved, write the full-interception report, delete the intercepted stacks
    /// and notify the interceptor users.
    pub async fn handle_mission_interception(
        conn: &mut MySqlConnection,
        mission: &Mission,
        interception_information: &InterceptionInformation,
    ) -> OwgeResult<((i32, u64), Vec<(i32, u64)>)> {
        // `mission.setResolved(true)` — Java sets it on the entity; the report save
        // below also flips `resolved`, but set it eagerly so the state is correct
        // even if there were no rows to report on.
        sqlx::query("UPDATE missions SET resolved = 1 WHERE id = ?")
            .bind(mission.id)
            .execute(&mut *conn)
            .await?;

        // reportFullMissionInterception(mission, originallyInvolved, interceptedUnits)
        // is built and saved BEFORE the intercepted stacks are deleted (so the
        // report can still load their data).
        let owner_pair = Self::report_full_mission_interception(
            conn,
            mission,
            &interception_information.originally_involved,
            &interception_information.intercepted_units,
        )
        .await?;

        Self::delete_intercepted_units(conn, &interception_information.intercepted_units).await?;
        // sendReportToInterceptorUsers(...): per-interceptor reports; pairs emitted
        // post-commit by the caller.
        let interceptor_pairs = UnitInterceptionFinderBo::send_report_to_interceptor_users(
            conn,
            mission,
            &interception_information.intercepted_units,
        )
        .await?;
        Ok((owner_pair, interceptor_pairs))
    }

    /// `reportFullMissionInterception` — build the owner-facing full-interception
    /// report (involved units + interception breakdown) and persist it via the
    /// report manager (which also links it onto the mission and flips `resolved`).
    async fn report_full_mission_interception(
        conn: &mut MySqlConnection,
        mission: &Mission,
        involved: &[ObtainedUnit],
        intercepted_units: &[InterceptedUnitsInformation],
    ) -> OwgeResult<(i32, u64)> {
        let builder =
            crate::bo::mission_processor::create_report_base(conn, mission, involved).await?;
        let interception_json = Self::build_interception_info_json(conn, intercepted_units).await?;
        let builder = builder.with_interception_information(interception_json);
        crate::bo::mission_report_manager_bo::MissionReportManagerBo::handle_mission_report_save(
            conn, mission, builder,
        )
        .await
    }

    /// `deleteInterceptedUnits` — delete every intercepted obtained-unit stack
    /// (`obtainedUnitRepository.deleteAll`), across all interceptors.
    async fn delete_intercepted_units(
        conn: &mut MySqlConnection,
        intercepted_units: &[InterceptedUnitsInformation],
    ) -> OwgeResult<()> {
        for interception in intercepted_units {
            for unit in &interception.intercepted_units {
                sqlx::query("DELETE FROM obtained_units WHERE id = ?")
                    .bind(unit.id)
                    .execute(&mut *conn)
                    .await?;
            }
        }
        Ok(())
    }

    /// Build the `interceptionInfo` report block
    /// (`UnitMissionReportBuilder.withInterceptionInformation`): a list of
    /// `{ interceptorUser, interceptorUnit, units }` where `interceptorUser` is the
    /// username, and the units are stripped obtained-unit DTOs (no
    /// source/target planet, no mission).
    ///
    /// `pub(crate)` so the per-interceptor report builder
    /// ([`crate::bo::unit_interception_finder_bo::UnitInterceptionFinderBo::send_report_to_interceptor_users`])
    /// can serialise a single interceptor's block.
    pub(crate) async fn build_interception_info_json(
        conn: &mut MySqlConnection,
        intercepted_units: &[InterceptedUnitsInformation],
    ) -> OwgeResult<Value> {
        let mut entries = Vec::with_capacity(intercepted_units.len());
        for info in intercepted_units {
            let interceptor_unit =
                Self::obtained_unit_to_stripped_value(conn, info.interceptor_unit.id).await?;
            let mut units = Vec::with_capacity(info.intercepted_units.len());
            for unit in &info.intercepted_units {
                units.push(Self::obtained_unit_to_stripped_value(conn, unit.id).await?);
            }
            entries.push(json!({
                "interceptorUser": info.interceptor_username,
                "interceptorUnit": interceptor_unit,
                "units": units,
            }));
        }
        Ok(Value::Array(entries))
    }

    /// Convert a list of obtained-unit stacks to the report DTO list used by
    /// `withInvolvedUnits` (each stripped of source/target planet + mission).
    async fn obtained_units_to_report_dtos(
        conn: &mut MySqlConnection,
        units: &[ObtainedUnit],
    ) -> OwgeResult<Vec<crate::dto::obtained_unit::ObtainedUnitDto>> {
        let mut out = Vec::with_capacity(units.len());
        for unit in units {
            if let Some(dto) =
                crate::bo::mission_processor::load_obtained_unit_dto(conn, unit.id).await?
            {
                out.push(dto);
            }
        }
        Ok(out)
    }

    /// Load one obtained-unit DTO and strip the per-unit source/target/mission
    /// references the report omits (mirrors the builder's `obtainedUnitToDto`).
    /// Returns `null` JSON when the stack's row has already vanished.
    async fn obtained_unit_to_stripped_value(
        conn: &mut MySqlConnection,
        obtained_unit_id: u64,
    ) -> OwgeResult<Value> {
        let dto =
            crate::bo::mission_processor::load_obtained_unit_dto(conn, obtained_unit_id).await?;
        let mut value = match dto {
            Some(dto) => serde_json::to_value(&dto)?,
            None => return Ok(Value::Null),
        };
        if let Value::Object(ref mut map) = value {
            map.remove("sourcePlanet");
            map.remove("targetPlanet");
            map.remove("mission");
        }
        Ok(value)
    }

    /// `MissionUnitsFinderBo.findUnitsInvolved` — the obtained-unit stacks attached
    /// to a mission (`findByMissionId`). Decoded at the `obtained_units` column
    /// types (see the `ObtainedUnit` model for the signedness map).
    async fn find_units_involved(
        conn: &mut MySqlConnection,
        mission_id: u64,
    ) -> OwgeResult<Vec<ObtainedUnit>> {
        Ok(sqlx::query_as::<_, ObtainedUnit>(
            "SELECT id, user_id, unit_id, count, source_planet, target_planet, \
                    mission_id, first_deployment_mission, is_from_capture, \
                    expiration_id, owner_unit_id \
             FROM obtained_units WHERE mission_id = ?",
        )
        .bind(mission_id)
        .fetch_all(&mut *conn)
        .await?)
    }
}
