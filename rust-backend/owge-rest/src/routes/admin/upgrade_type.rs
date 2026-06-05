//! `AdminUpgradeTypeRestService` — `CrudRestServiceTrait<UpgradeType>`
//! (mapping `admin/upgrade_type`).

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::UpgradeBo;
use owge_business::dto::upgrade::UpgradeTypeInput;
use owge_business::dto::UpgradeTypeDto;

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/upgrade_type", get(find_all).post(save_new))
        .route(
            "/admin/upgrade_type/{id}",
            get(find_one).put(save_existing).delete(delete_one),
        )
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<UpgradeTypeDto>>> {
    Ok(Json(UpgradeBo::find_upgrade_types(&state.db).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<UpgradeTypeDto>> {
    UpgradeBo::find_upgrade_type_by_id(&state.db, id)
        .await?
        .map(Json)
        .ok_or_else(|| {
            ApiError(owge_business::OwgeError::NotFound(format!(
                "No upgrade type {id}"
            )))
        })
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<UpgradeTypeInput>,
) -> ApiResult<Json<UpgradeTypeDto>> {
    Ok(Json(UpgradeBo::save_new_upgrade_type(&state.db, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<UpgradeTypeInput>,
) -> ApiResult<Json<UpgradeTypeDto>> {
    Ok(Json(
        UpgradeBo::save_existing_upgrade_type(&state.db, id, &input).await?,
    ))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    UpgradeBo::delete_upgrade_type(&state.db, id).await?;
    Ok(StatusCode::NO_CONTENT)
}
