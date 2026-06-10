//! `AdminUpgradeTypeRestService` — `CrudRestServiceTrait<UpgradeType>`
//! (mapping `admin/upgrade_type`).

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::UpgradeBo;
use owge_business::dto::UpgradeTypeDto;
use owge_business::dto::upgrade::UpgradeTypeInput;

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
    let mut conn = state.db.acquire().await?;
    Ok(Json(UpgradeBo::find_upgrade_types(&mut conn).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<UpgradeTypeDto>> {
    let mut conn = state.db.acquire().await?;
    UpgradeBo::find_upgrade_type_by_id(&mut conn, id)
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
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        UpgradeBo::save_new_upgrade_type(&mut conn, &input).await?,
    ))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<UpgradeTypeInput>,
) -> ApiResult<Json<UpgradeTypeDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        UpgradeBo::save_existing_upgrade_type(&mut conn, id, &input).await?,
    ))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    UpgradeBo::delete_upgrade_type(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}
