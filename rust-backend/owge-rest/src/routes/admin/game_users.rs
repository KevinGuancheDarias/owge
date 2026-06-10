//! `AdminGameUsersRestService` (`admin/users`).
//!
//! The user-listing reads (`with-suspicions`, `{id}`) and the cascading
//! `DELETE {id}` (`UserDeleteService.deleteAccount`) are ported. Only
//! `GET {id}/suspicions` (which embeds full `AuditDto`s) depends on the
//! auditing/anti-cheat subsystem, which is **dropped from the plan** (disabled in
//! live Java too), so it answers `501 NOT_IMPLEMENTED` permanently.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::UserStorageBo;
use owge_business::dto::SimpleUserData;

use crate::auth::AdminUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/users/with-suspicions", get(find_with_suspicions))
        .route("/admin/users/{id}", get(find_by_id).delete(delete_user))
        .route("/admin/users/{id}/suspicions", get(find_user_suspicions))
}

async fn find_with_suspicions(
    State(_): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<()>>> {
    todo!("Won't implement, but check how costly is to remove")
}

async fn find_by_id(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<i32>,
) -> ApiResult<Json<SimpleUserData>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(UserStorageBo::find_simple_by_id(&mut conn, id).await?))
}

// `UserDeleteService.deleteAccount`: cascade-delete the user across planets,
// obtained units/upgrades, missions, reports, audits, suspicions, … then the
// `user_storage` row. The Java controller returns void -> 200 empty body.
async fn delete_user(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<i32>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    UserStorageBo::delete_account(&mut conn, id).await?;
    Ok(StatusCode::OK)
}

// NOT PORTED (permanent 501): `SuspicionDto` embeds a full `AuditDto`; the
// auditing/anti-cheat subsystem is dropped from the plan (disabled in live Java
// too), so this endpoint intentionally stays `501 NOT_IMPLEMENTED`.
async fn find_user_suspicions(_admin: AdminUser, Path(_id): Path<i32>) -> StatusCode {
    StatusCode::NOT_IMPLEMENTED
}
