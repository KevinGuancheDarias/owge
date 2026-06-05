//! `AdminGalaxiesRestService` — `CrudRestServiceTrait<Galaxy>` + `has-players`.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::GalaxyBo;
use owge_business::dto::{GalaxyDto, GalaxyInput};

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/galaxy", get(find_all).post(save_new))
        .route(
            "/admin/galaxy/{id}",
            get(find_one).put(save_existing).delete(delete_one),
        )
        .route("/admin/galaxy/{id}/has-players", get(has_players))
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<GalaxyDto>>> {
    Ok(Json(GalaxyBo::find_all(&state.db).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<GalaxyDto>> {
    GalaxyBo::find_by_id(&state.db, id)
        .await?
        .map(Json)
        .ok_or_else(|| ApiError(owge_business::OwgeError::NotFound(format!("No galaxy {id}"))))
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<GalaxyInput>,
) -> ApiResult<Json<GalaxyDto>> {
    Ok(Json(GalaxyBo::save_new(&state.db, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<GalaxyInput>,
) -> ApiResult<Json<GalaxyDto>> {
    Ok(Json(GalaxyBo::save_existing(&state.db, id, &input).await?))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    GalaxyBo::delete(&state.db, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

async fn has_players(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<bool>> {
    Ok(Json(GalaxyBo::has_players(&state.db, id).await?))
}
