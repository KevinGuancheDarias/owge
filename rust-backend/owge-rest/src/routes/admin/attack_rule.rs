//! `AdminAttackRuleRestService` (`admin/attack-rule`).
//!
//! Not a `CrudRestServiceTrait`: it exposes only `POST ''` (save new),
//! `PUT '{id}'` (save existing) and `DELETE '{id}'`. There are no `GET`
//! endpoints in the Java controller, so none are exposed here.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::{delete, post};
use axum::{Json, Router};
use owge_business::bo::AttackRuleBo;
use owge_business::dto::{AttackRuleDto, AttackRuleInput};

use crate::auth::AdminUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/attack-rule", post(save_new))
        .route(
            "/admin/attack-rule/{id}",
            delete(delete_one).put(save_existing),
        )
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<AttackRuleInput>,
) -> ApiResult<Json<AttackRuleDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(AttackRuleBo::save_new(&mut conn, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<AttackRuleInput>,
) -> ApiResult<Json<AttackRuleDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        AttackRuleBo::save_existing(&mut conn, id, &input).await?,
    ))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    AttackRuleBo::delete(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}
