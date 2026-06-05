//! Port of (the read-state-mutating side of) `MissionReportBo` — marking
//! mission reports as read.
//!
//! Reports themselves are produced by the mission engine (M3); only the
//! "mark as read" mutations and their `mission_report_change` count emission
//! belong to M2. The websocket emission is deferred to M4 (consistent with the
//! rest of the port), so these methods just perform the DB update.

use chrono::NaiveDateTime;
use sqlx::FromRow;

use crate::db::Db;
use crate::dto::{MissionReportDto, MissionReportResponse};
use crate::error::OwgeResult;

/// Java default page size (`MissionReportBo.DEFAULT_PAGE_SIZE`).
const DEFAULT_PAGE_SIZE: i64 = 15;

/// One `mission_reports` row joined with its (optional) owning `missions` row,
/// mirroring `MissionReportDto.dtoFromEntity` + `parseMission(findOneByReportId)`.
#[derive(FromRow)]
struct MissionReportRow {
    id: u64,
    json_body: Option<String>,
    report_date: Option<NaiveDateTime>,
    user_read_date: Option<NaiveDateTime>,
    is_enemy: Option<bool>,
    mission_id: Option<u64>,
    mission_date: Option<NaiveDateTime>,
}

pub struct MissionReportBo;

impl MissionReportBo {
    /// `findMissionReportsInformation(userId, page)` — the paginated "my reports"
    /// listing: a page of the user's reports (with mission join + parsed json
    /// bodies) plus the per-user / per-enemy unread counts.
    ///
    /// Note (per Java): when `page == 0` the response is flagged `requiresFlush`.
    pub async fn find_mission_reports_information(
        db: &Db,
        user_id: i32,
        page: i32,
    ) -> OwgeResult<MissionReportResponse> {
        let reports = Self::find_paginated_by_user_id(db, user_id, page).await?;
        let (user_unread, enemy_unread) = Self::find_unread_count(db, user_id).await?;
        Ok(MissionReportResponse {
            page,
            user_unread,
            enemy_unread,
            requires_flush: page == 0,
            reports,
        })
    }

    /// `findPaginatedByUserId` + `parseMission` + `parseJsonBody`: the page of
    /// reports for `userId`, newest first, with the owning mission's id/date
    /// joined in and `jsonBody` parsed into `parsedJson`.
    async fn find_paginated_by_user_id(
        db: &Db,
        user_id: i32,
        page: i32,
    ) -> OwgeResult<Vec<MissionReportDto>> {
        // Java: PageRequest.of(page, 15) ordered by id DESC. The LEFT JOIN to
        // missions mirrors `findOneByReportId` (mission whose report_id = report.id).
        let offset = (page as i64) * DEFAULT_PAGE_SIZE;
        let rows = sqlx::query_as::<_, MissionReportRow>(
            "SELECT mr.id AS id, mr.json_body AS json_body, mr.report_date AS report_date, \
                    mr.user_read_date AS user_read_date, mr.is_enemy AS is_enemy, \
                    m.id AS mission_id, m.termination_date AS mission_date \
             FROM mission_reports mr \
             LEFT JOIN missions m ON m.report_id = mr.id \
             WHERE mr.user_id = ? \
             ORDER BY mr.id DESC \
             LIMIT ? OFFSET ?",
        )
        .bind(user_id)
        .bind(DEFAULT_PAGE_SIZE)
        .bind(offset)
        .fetch_all(db)
        .await?;

        rows.into_iter()
            .map(|row| {
                // parseJsonBody: deserialize json_body into parsed_json, then null json_body.
                let parsed_json = match &row.json_body {
                    Some(body) => Some(serde_json::from_str(body)?),
                    None => None,
                };
                Ok(MissionReportDto {
                    id: Some(row.id),
                    json_body: None,
                    parsed_json,
                    mission_id: row.mission_id,
                    mission_date: row.mission_date,
                    report_date: row.report_date,
                    user_read_date: row.user_read_date,
                    is_enemy: row.is_enemy,
                })
            })
            .collect()
    }

    /// `MissionReportBo.toDto(MissionReport)` for a single row — the payload of
    /// the `mission_report_new` websocket event. Loads the report by id, joins
    /// its (optional) owning mission, and parses `jsonBody` into `parsedJson`
    /// (nulling `jsonBody`), exactly like the paginated path does per-row.
    ///
    /// Returns `None` if the report no longer exists (defensive; the emit is
    /// fired immediately after the row is inserted, so it normally exists).
    pub async fn find_by_id(db: &Db, report_id: u64) -> OwgeResult<Option<MissionReportDto>> {
        let row = sqlx::query_as::<_, MissionReportRow>(
            "SELECT mr.id AS id, mr.json_body AS json_body, mr.report_date AS report_date, \
                    mr.user_read_date AS user_read_date, mr.is_enemy AS is_enemy, \
                    m.id AS mission_id, m.termination_date AS mission_date \
             FROM mission_reports mr \
             LEFT JOIN missions m ON m.report_id = mr.id \
             WHERE mr.id = ?",
        )
        .bind(report_id)
        .fetch_optional(db)
        .await?;

        match row {
            None => Ok(None),
            Some(row) => {
                let parsed_json = match &row.json_body {
                    Some(body) => Some(serde_json::from_str(body)?),
                    None => None,
                };
                Ok(Some(MissionReportDto {
                    id: Some(row.id),
                    json_body: None,
                    parsed_json,
                    mission_id: row.mission_id,
                    mission_date: row.mission_date,
                    report_date: row.report_date,
                    user_read_date: row.user_read_date,
                    is_enemy: row.is_enemy,
                }))
            }
        }
    }

    /// `findUnreadCount` — `(userUnread, enemyUnread)`: unread report counts for
    /// non-enemy (`is_enemy = false`) and enemy (`is_enemy = true`) reports.
    pub async fn find_unread_count(db: &Db, user_id: i32) -> OwgeResult<(i64, i64)> {
        let enemy_unread = Self::count_unread(db, user_id, true).await?;
        let user_unread = Self::count_unread(db, user_id, false).await?;
        Ok((user_unread, enemy_unread))
    }

    /// `countByUserIdAndIsEnemyAndUserReadDateIsNull`.
    async fn count_unread(db: &Db, user_id: i32, is_enemy: bool) -> OwgeResult<i64> {
        let (count,): (i64,) = sqlx::query_as(
            "SELECT CAST(COUNT(*) AS SIGNED) FROM mission_reports \
             WHERE user_id = ? AND is_enemy = ? AND user_read_date IS NULL",
        )
        .bind(user_id)
        .bind(is_enemy)
        .fetch_one(db)
        .await?;
        Ok(count)
    }

    /// `markAsReadIfUserIsOwner(reportsIds, userId)` — set `user_read_date = now`
    /// for the owned reports in the list.
    pub async fn mark_as_read(db: &Db, user_id: i32, report_ids: &[u64]) -> OwgeResult<()> {
        if report_ids.is_empty() {
            return Ok(());
        }
        // Build an `IN (?, ?, ...)` list; sqlx/MySQL has no array binding.
        let placeholders = std::iter::repeat("?")
            .take(report_ids.len())
            .collect::<Vec<_>>()
            .join(", ");
        let sql = format!(
            "UPDATE mission_reports SET user_read_date = CURRENT_TIMESTAMP \
             WHERE user_id = ? AND id IN ({placeholders})"
        );
        let mut q = sqlx::query(&sql).bind(user_id);
        for id in report_ids {
            q = q.bind(id);
        }
        q.execute(db).await?;
        Ok(())
    }

    /// `markAsReadBeforeDate(userId, date)` — mark every still-unread report
    /// older than `date` as read.
    pub async fn mark_as_read_before_date(
        db: &Db,
        user_id: i32,
        date: NaiveDateTime,
    ) -> OwgeResult<()> {
        sqlx::query(
            "UPDATE mission_reports SET user_read_date = CURRENT_TIMESTAMP \
             WHERE user_id = ? AND user_read_date IS NULL AND report_date < ?",
        )
        .bind(user_id)
        .bind(date)
        .execute(db)
        .await?;
        Ok(())
    }
}
