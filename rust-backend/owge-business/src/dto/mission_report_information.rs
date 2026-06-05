//! Port of `com.kevinguanchedarias.owgejava.responses.MissionReportResponse` —
//! the paginated "my mission reports" payload returned by
//! `MissionReportBo.findMissionReportsInformation` (behind
//! `ReportRestService.findMy`).
//!
//! Response payload, so it is Jackson-camelCase. The reports themselves reuse
//! the already-ported [`MissionReportDto`].

use serde::Serialize;

use crate::dto::MissionReportDto;

/// Mirrors `MissionReportResponse { page, userUnread, enemyUnread,
/// requiresFlush, reports }`.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct MissionReportResponse {
    pub page: i32,
    pub user_unread: i64,
    pub enemy_unread: i64,
    pub requires_flush: bool,
    pub reports: Vec<MissionReportDto>,
}
