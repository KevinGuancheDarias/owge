//! `AllianceRestService` — the player-facing alliance endpoints under
//! `game/alliance`. All require a game user; logic lives in
//! [`AllianceBo`](owge_business::bo::AllianceBo).

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::{delete, get, post};
use axum::{Json, Router};
use owge_business::bo::AllianceBo;
use owge_business::dto::{
    AllianceDto, AllianceJoinRequestDto, JoinRequestIdBody, RequestJoinBody, UserStorageDto,
};

use crate::auth::GameUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route(
            "/game/alliance",
            get(find_all).post(save).put(save).delete(delete_by_user),
        )
        .route("/game/alliance/{id}/members", get(members))
        .route("/game/alliance/listRequest", get(list_request))
        .route("/game/alliance/my-requests", get(my_requests))
        .route("/game/alliance/my-requests/{id}", delete(my_requests_delete))
        .route("/game/alliance/requestJoin", post(request_join))
        .route("/game/alliance/acceptJoinRequest", post(accept_join_request))
        .route("/game/alliance/rejectJoinRequest", post(reject_join_request))
        .route("/game/alliance/leave", post(leave))
}

/// `findAll`.
async fn find_all(State(state): State<AppState>, _user: GameUser) -> ApiResult<Json<Vec<AllianceDto>>> {
    Ok(Json(AllianceBo::find_all(&state.db).await?))
}

/// `members` — email is blanked (Java nulls it), improvements omitted.
async fn members(
    State(state): State<AppState>,
    _user: GameUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<UserStorageDto>>> {
    Ok(Json(AllianceBo::members(&state.db, id).await?))
}

/// `save` — bound to both POST (create) and PUT (update); the body's `id`
/// distinguishes the two (id 0/absent = create).
async fn save(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(dto): Json<AllianceDto>,
) -> ApiResult<Json<AllianceDto>> {
    Ok(Json(AllianceBo::save(&state.db, dto, user.id as i32).await?))
}

/// `delete` — delete the invoker's own alliance.
async fn delete_by_user(
    State(state): State<AppState>,
    GameUser(user): GameUser,
) -> ApiResult<StatusCode> {
    AllianceBo::delete_by_user(&state.db, user.id as i32).await?;
    Ok(StatusCode::OK)
}

/// `listRequest` — join requests for the invoker's (owned) alliance.
async fn list_request(
    State(state): State<AppState>,
    GameUser(user): GameUser,
) -> ApiResult<Json<Vec<AllianceJoinRequestDto>>> {
    Ok(Json(AllianceBo::list_request(&state.db, user.id as i32).await?))
}

/// `myRequests`.
async fn my_requests(
    State(state): State<AppState>,
    GameUser(user): GameUser,
) -> ApiResult<Json<Vec<AllianceJoinRequestDto>>> {
    Ok(Json(AllianceBo::my_requests(&state.db, user.id as i32).await?))
}

/// `myRequestsDelete` — bare delete by id (no checks, matching Java).
async fn my_requests_delete(
    State(state): State<AppState>,
    _user: GameUser,
    Path(id): Path<u32>,
) -> ApiResult<StatusCode> {
    AllianceBo::delete_join_request_by_id(&state.db, id).await?;
    Ok(StatusCode::OK)
}

/// `join` -> `requestJoin`.
async fn request_join(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(body): Json<RequestJoinBody>,
) -> ApiResult<Json<AllianceJoinRequestDto>> {
    Ok(Json(
        AllianceBo::request_join(&state.db, body.alliance_id, user.id as i32).await?,
    ))
}

/// `acceptRequest` -> `acceptJoin`.
async fn accept_join_request(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(body): Json<JoinRequestIdBody>,
) -> ApiResult<StatusCode> {
    AllianceBo::accept_join(&state.db, body.join_request_id, user.id as i32).await?;
    Ok(StatusCode::OK)
}

/// `rejectRequest` -> `rejectJoin`.
async fn reject_join_request(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(body): Json<JoinRequestIdBody>,
) -> ApiResult<StatusCode> {
    AllianceBo::reject_join(&state.db, body.join_request_id, user.id as i32).await?;
    Ok(StatusCode::OK)
}

/// `leave`.
async fn leave(State(state): State<AppState>, GameUser(user): GameUser) -> ApiResult<StatusCode> {
    AllianceBo::leave(&state.db, user.id as i32).await?;
    Ok(StatusCode::OK)
}
