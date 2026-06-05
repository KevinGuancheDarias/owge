//! Port of the *save* side of the mission report subsystem:
//! `MissionReportManagerBo` (`...business.mission.report.MissionReportManagerBo`)
//! together with the `create`/`save` helpers it leans on in `MissionReportBo`
//! (`...business.MissionReportBo`).
//!
//! When a mission finishes, its processor returns a built
//! [`UnitMissionReportBuilder`]; this Bo persists it. Java's flow (single-arg
//! `handleMissionReportSave`) is:
//!   1. insert an empty `mission_reports` row owned by the mission's user to
//!      obtain the AUTO_INCREMENT id,
//!   2. set `report_date = now`, `json_body = builder.withId(id).buildJson()`,
//!      `is_enemy = false`,
//!   3. link the mission to the report (`mission.setReport(report)`).
//!
//! The contract additionally requires this Bo to write the link back onto the
//! `missions` row (`report_id`) and flip `resolved = 1`, which in Java is done
//! by the surrounding mission-execution transaction flushing the managed
//! `Mission` entity.
//!
//! The per-affected-user enemy-report duplication
//! (`handleMissionReportSave(mission, builder, isEnemy, users)`) is ported as
//! [`MissionReportManagerBo::handle_mission_report_save_for_users`]: each enemy
//! user gets its own `mission_reports` row carrying the *same* payload with
//! `is_enemy = true`. Note that in Java these enemy rows are *not* linked back
//! onto the mission (only the owner's report is); we keep that parity.
//!
//! **sqlx signedness** (roadmap §0.3): `mission_reports.id` is `bigint UNSIGNED`
//! (`u64`), `user_id` is signed `int` (`i32`), `is_enemy` is `tinyint(1)`
//! (`bool`). `missions.report_id` is `bigint UNSIGNED`, `missions.resolved` is a
//! bare `tinyint`.

use chrono::Utc;
use sqlx::MySqlConnection;

use crate::builder::UnitMissionReportBuilder;
use crate::error::{OwgeError, OwgeResult};
use crate::model::mission::Mission;

pub struct MissionReportManagerBo;

impl MissionReportManagerBo {
    /// Java `handleMissionReportSave(Mission, UnitMissionReportBuilder)` — saves
    /// the owner's (non-enemy) report, links it onto the mission's `report_id`,
    /// and marks the mission `resolved`.
    ///
    /// Returns `(owner_user_id, report_id)` so the caller can emit
    /// `mission_report_new` + `mission_report_count_change` post-commit
    /// (Java `MissionReportBo.save` → `emitOneToUser`).
    pub async fn handle_mission_report_save(
        conn: &mut MySqlConnection,
        mission: &Mission,
        report: UnitMissionReportBuilder,
    ) -> OwgeResult<(i32, u64)> {
        // Java: missionReport.setUser(mission.getUser()). A user-bound mission
        // always has an owner; bail loudly rather than write a NULL FK.
        let user_id = mission.user_id.ok_or_else(|| {
            OwgeError::Common(format!(
                "Cannot save a mission report for mission {} which has no owning user",
                mission.id
            ))
        })?;

        let report_id =
            Self::insert_report(conn, user_id, report, /* is_enemy = */ false).await?;

        // Java relies on the managed Mission entity being flushed with
        // report = savedReport and resolved = true; we write both explicitly.
        sqlx::query("UPDATE missions SET report_id = ?, resolved = 1 WHERE id = ?")
            .bind(report_id)
            .bind(mission.id)
            .execute(&mut *conn)
            .await?;

        Ok((user_id, report_id))
    }

    /// Java `handleMissionReportSave(Mission, builder, isEnemy, List<UserStorage>)`
    /// — duplicate the same report payload to each affected (enemy) user. Unlike
    /// the owner path, these rows are not linked back onto the mission and do
    /// not touch `resolved`.
    ///
    /// Returns one `(user_id, report_id)` per inserted row so the caller can
    /// emit `mission_report_new` + `mission_report_count_change` to each
    /// recipient post-commit (Java routes every report through
    /// `MissionReportBo.create` → `save` → `emitOneToUser`).
    pub async fn handle_mission_report_save_for_users(
        conn: &mut MySqlConnection,
        report: &UnitMissionReportBuilder,
        is_enemy: bool,
        user_ids: &[i32],
    ) -> OwgeResult<Vec<(i32, u64)>> {
        // The builder's payload is identical for every recipient (only `id`
        // differs, and the frontend keys off the surrounding row id). Build the
        // JSON once and reuse it; `withId` is purely cosmetic here.
        let json_body = report.build_json()?;
        let now = Utc::now().naive_utc();
        let mut pairs = Vec::with_capacity(user_ids.len());
        for &user_id in user_ids {
            let result = sqlx::query(
                "INSERT INTO mission_reports (json_body, user_id, report_date, is_enemy) \
                 VALUES (?, ?, ?, ?)",
            )
            .bind(&json_body)
            .bind(user_id)
            .bind(now)
            .bind(is_enemy)
            .execute(&mut *conn)
            .await?;
            pairs.push((user_id, result.last_insert_id()));
        }
        Ok(pairs)
    }

    /// Insert one `mission_reports` row and return its AUTO_INCREMENT id.
    ///
    /// Mirrors Java's two-step `save` then `withId(id).buildJson()`: the report
    /// payload embeds its own row id, so we insert an empty body first to obtain
    /// the id, then update the body with the id baked in.
    async fn insert_report(
        conn: &mut MySqlConnection,
        user_id: i32,
        report: UnitMissionReportBuilder,
        is_enemy: bool,
    ) -> OwgeResult<u64> {
        let now = Utc::now().naive_utc();
        let result = sqlx::query(
            "INSERT INTO mission_reports (json_body, user_id, report_date, is_enemy) \
             VALUES ('{}', ?, ?, ?)",
        )
        .bind(user_id)
        .bind(now)
        .bind(is_enemy)
        .execute(&mut *conn)
        .await?;
        let report_id = result.last_insert_id();

        let json_body = report.with_id(report_id).build_json()?;
        sqlx::query("UPDATE mission_reports SET json_body = ? WHERE id = ?")
            .bind(json_body)
            .bind(report_id)
            .execute(&mut *conn)
            .await?;

        Ok(report_id)
    }
}
