//! `AdminSystemMessageRestService` — a bespoke admin controller (not a
//! `CrudRestServiceTrait`) exposing only `POST admin/system-message`
//! (`create`), which delegates to `SystemMessageBo.save` and returns the
//! persisted `SystemMessageDto`.

use axum::extract::State;
use axum::routing::post;
use axum::{Json, Router};
use owge_business::bo::SystemMessageBo;
use owge_business::dto::{SystemMessageDto, SystemMessageInput};

use crate::auth::AdminUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new().route("/admin/system-message", post(create))
}

async fn create(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<SystemMessageInput>,
) -> ApiResult<Json<SystemMessageDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(SystemMessageBo::save(&mut conn, &input).await?))
}
