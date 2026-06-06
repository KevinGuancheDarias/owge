//! `rest/open/*` — unauthenticated endpoints.

use axum::extract::State;
use axum::routing::get;
use axum::{Json, Router};
use chrono::Utc;
use owge_business::bo::{ConfigurationBo, RuleBo, SpeedImpactGroupBo};
use owge_business::dto::{RuleWithRelatedUnitsDto, SpeedImpactGroupDto};
use serde::Serialize;

use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/open/clock", get(clock))
        .route("/open/configuration", get(configuration))
        .route("/open/websocket-sync/rule_change", get(rule_change))
        .route(
            "/open/websocket-sync/speed_group_change",
            get(speed_group_change),
        )
}

/// `ClockRestService.currentTime` — returns the server time. Jackson serialises
/// `new Date()` as epoch milliseconds, so we return a bare JSON number too.
async fn clock() -> Json<i64> {
    Json(Utc::now().timestamp_millis())
}

/// Subset of `ConfigurationDto` exposed by the open configuration endpoint.
#[derive(Serialize)]
struct ConfigurationDto {
    name: String,
    #[serde(rename = "displayName")]
    display_name: Option<String>,
    value: String,
}

/// `ConfigurationRestService.findUnprivilege` — the non-privileged settings the
/// frontend needs before login.
async fn configuration(State(state): State<AppState>) -> ApiResult<Json<Vec<ConfigurationDto>>> {
    let configs = ConfigurationBo::find_public(&state.db).await?;
    let dtos = configs
        .into_iter()
        .map(|c| ConfigurationDto {
            name: c.name,
            display_name: c.display_name,
            value: c.value,
        })
        .collect();
    Ok(Json(dtos))
}

/// `OpenWebsocketSyncRestService.findRules` — all rules plus the units they reference.
async fn rule_change(
    State(state): State<AppState>,
) -> ApiResult<Json<RuleWithRelatedUnitsDto>> {
    Ok(Json(RuleBo::find_all_with_related_units(&state.db).await?))
}

/// `OpenWebsocketSyncRestService.findSpeedImpactGroups` — all speed impact groups.
async fn speed_group_change(
    State(state): State<AppState>,
) -> ApiResult<Json<Vec<SpeedImpactGroupDto>>> {
    Ok(Json(SpeedImpactGroupBo::find_all_dtos(&state.db).await?))
}
