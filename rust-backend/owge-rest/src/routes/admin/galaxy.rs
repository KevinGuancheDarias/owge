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
    let mut conn = state.db.acquire().await?;
    Ok(Json(GalaxyBo::find_all(&mut conn).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<GalaxyDto>> {
    let mut conn = state.db.acquire().await?;
    GalaxyBo::find_by_id(&mut conn, id)
        .await?
        .map(Json)
        .ok_or_else(|| {
            ApiError(owge_business::OwgeError::NotFound(format!(
                "No galaxy {id}"
            )))
        })
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<GalaxyInput>,
) -> ApiResult<Json<GalaxyDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(GalaxyBo::save_new(&mut conn, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<GalaxyInput>,
) -> ApiResult<Json<GalaxyDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(GalaxyBo::save_existing(&mut conn, id, &input).await?))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    GalaxyBo::delete(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

async fn has_players(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<bool>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(GalaxyBo::has_players(&mut conn, id).await?))
}
