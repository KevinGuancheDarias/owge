//! `AdminUnitTypeRestService` — `CrudRestServiceTrait<UnitType>` plus the two
//! bespoke `DELETE {id}/attackRule` and `DELETE {id}/criticalAttack` endpoints
//! that clear those foreign keys.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::UnitTypeBo;
use owge_business::dto::UnitTypeDto;
use owge_business::dto::unit_type::UnitTypeInput;

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/unit_type", get(find_all).post(save_new))
        .route(
            "/admin/unit_type/{id}",
            get(find_one).put(save_existing).delete(delete_one),
        )
        .route(
            "/admin/unit_type/{id}/attackRule",
            axum::routing::delete(unset_attack_rule),
        )
        .route(
            "/admin/unit_type/{id}/criticalAttack",
            axum::routing::delete(unset_critical_attack),
        )
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<UnitTypeDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(UnitTypeBo::find_all(&mut conn).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<UnitTypeDto>> {
    let mut conn = state.db.acquire().await?;
    UnitTypeBo::find_by_id(&mut conn, id)
        .await?
        .map(Json)
        .ok_or_else(|| {
            ApiError(owge_business::OwgeError::NotFound(format!(
                "No unit type {id}"
            )))
        })
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<UnitTypeInput>,
) -> ApiResult<Json<UnitTypeDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(UnitTypeBo::save_new(&mut conn, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<UnitTypeInput>,
) -> ApiResult<Json<UnitTypeDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        UnitTypeBo::save_existing(&mut conn, id, &input).await?,
    ))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    UnitTypeBo::delete(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

async fn unset_attack_rule(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    UnitTypeBo::unset_attack_rule(&mut conn, id).await?;
    // Java's `void` handler returns HTTP 200 with an empty body (not 204).
    Ok(StatusCode::OK)
}

async fn unset_critical_attack(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    UnitTypeBo::unset_critical_attack(&mut conn, id).await?;
    // Java's `void` handler returns HTTP 200 with an empty body (not 204).
    Ok(StatusCode::OK)
}
