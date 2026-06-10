//! `AdminCriticalAttackRestService` — `admin/critical-attack`.
//!
//! Unlike most admin controllers this is **not** a full `CrudRestServiceTrait`:
//! it declares only `POST ''` (save new), `PUT '{id}'` (save existing) and
//! `DELETE '{id}'`. There is no `GET` route, matching the Java controller.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::post;
use axum::{Json, Router};
use owge_business::bo::CriticalAttackBo;
use owge_business::dto::{CriticalAttackDto, CriticalAttackInput};

use crate::auth::AdminUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/critical-attack", post(save_new))
        .route(
            "/admin/critical-attack/{id}",
            axum::routing::put(save_existing).delete(delete_one),
        )
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<CriticalAttackInput>,
) -> ApiResult<Json<CriticalAttackDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(CriticalAttackBo::save_new(&mut conn, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<CriticalAttackInput>,
) -> ApiResult<Json<CriticalAttackDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        CriticalAttackBo::save_existing(&mut conn, id, &input).await?,
    ))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    CriticalAttackBo::delete(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}
