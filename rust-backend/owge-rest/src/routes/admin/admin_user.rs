//! `AdminAdminsRestService` (`admin/admin-user`). Not a `CrudRestServiceTrait`:
//! it exposes only `GET ''` (list), `PUT '{id}'` (add admin) and
//! `DELETE '{id}'`.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::admin_user_bo::AdminUserDto;
use owge_business::bo::AdminUserBo;
use serde::Deserialize;

use crate::auth::AdminUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/admin-user", get(find_all))
        .route("/admin/admin-user/{id}", axum::routing::put(add).delete(delete_one))
}

#[derive(Deserialize)]
struct AddBody {
    username: String,
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<AdminUserDto>>> {
    Ok(Json(AdminUserBo::find_all(&state.db).await?))
}

/// `PUT '{id}'` — `adminUserBo.addAdmin(id, body.username)`. The `{id}` is the
/// account-system user id to grant admin to.
async fn add(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<i64>,
    Json(body): Json<AddBody>,
) -> ApiResult<Json<AdminUserDto>> {
    Ok(Json(AdminUserBo::add_admin(&state.db, id, &body.username).await?))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<i64>,
) -> ApiResult<StatusCode> {
    AdminUserBo::delete_by_id(&state.db, id).await?;
    // Java method returns `void` => Spring 200 with empty body.
    Ok(StatusCode::OK)
}
