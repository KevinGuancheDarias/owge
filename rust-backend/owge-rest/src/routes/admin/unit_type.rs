//! `AdminUnitTypeRestService` — `CrudRestServiceTrait<UnitType>` plus the two
//! bespoke `DELETE {id}/attackRule` and `DELETE {id}/criticalAttack` endpoints
//! that clear those foreign keys.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::UnitTypeBo;
use owge_business::dto::unit_type::UnitTypeInput;
use owge_business::dto::UnitTypeDto;

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
    Ok(Json(UnitTypeBo::find_all(&state.db).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<UnitTypeDto>> {
    UnitTypeBo::find_by_id(&state.db, id)
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
    Ok(Json(UnitTypeBo::save_new(&state.db, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<UnitTypeInput>,
) -> ApiResult<Json<UnitTypeDto>> {
    Ok(Json(UnitTypeBo::save_existing(&state.db, id, &input).await?))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    UnitTypeBo::delete(&state.db, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

async fn unset_attack_rule(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    UnitTypeBo::unset_attack_rule(&state.db, id).await?;
    // Java's `void` handler returns HTTP 200 with an empty body (not 204).
    Ok(StatusCode::OK)
}

async fn unset_critical_attack(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    UnitTypeBo::unset_critical_attack(&state.db, id).await?;
    // Java's `void` handler returns HTTP 200 with an empty body (not 204).
    Ok(StatusCode::OK)
}
