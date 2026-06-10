//! `AllianceRestService` — the player-facing alliance endpoints under
//! `game/alliance`. All require a game user; logic lives in
//! [`AllianceBo`](AllianceBo).

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::{delete, get, post};
use axum::{Json, Router};
use owge_business::bo::AllianceBo;
use owge_business::dto::{
    AllianceDto, AllianceJoinRequestDto, JoinRequestIdBody, RequestJoinBody, SimpleUserData,
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
        .route(
            "/game/alliance/my-requests/{id}",
            delete(my_requests_delete),
        )
        .route("/game/alliance/requestJoin", post(request_join))
        .route(
            "/game/alliance/acceptJoinRequest",
            post(accept_join_request),
        )
        .route(
            "/game/alliance/rejectJoinRequest",
            post(reject_join_request),
        )
        .route("/game/alliance/leave", post(leave))
}

/// `findAll`.
async fn find_all(
    State(state): State<AppState>,
    _user: GameUser,
) -> ApiResult<Json<Vec<AllianceDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(AllianceBo::find_all(&mut conn).await?))
}

/// `members` — email is blanked (Java nulls it), improvements omitted.
async fn members(
    State(state): State<AppState>,
    _user: GameUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<SimpleUserData>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(AllianceBo::members(&mut conn, id).await?))
}

/// `save` — bound to both POST (create) and PUT (update); the body's `id`
/// distinguishes the two (id 0/absent = create).
async fn save(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(dto): Json<AllianceDto>,
) -> ApiResult<Json<AllianceDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        AllianceBo::save(&mut conn, dto, user.id as i32).await?,
    ))
}

/// `delete` — delete the invoker's own alliance.
async fn delete_by_user(
    State(state): State<AppState>,
    GameUser(user): GameUser,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    AllianceBo::delete_by_user(&mut conn, user.id as i32).await?;
    Ok(StatusCode::OK)
}

/// `listRequest` — join requests for the invoker's (owned) alliance.
async fn list_request(
    State(state): State<AppState>,
    GameUser(user): GameUser,
) -> ApiResult<Json<Vec<AllianceJoinRequestDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        AllianceBo::list_request(&mut conn, user.id as i32).await?,
    ))
}

/// `myRequests`.
async fn my_requests(
    State(state): State<AppState>,
    GameUser(user): GameUser,
) -> ApiResult<Json<Vec<AllianceJoinRequestDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        AllianceBo::my_requests(&mut conn, user.id as i32).await?,
    ))
}

/// `myRequestsDelete` — bare delete by id (no checks, matching Java).
async fn my_requests_delete(
    State(state): State<AppState>,
    _user: GameUser,
    Path(id): Path<u32>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    AllianceBo::delete_join_request_by_id(&mut conn, id).await?;
    Ok(StatusCode::OK)
}

/// `join` -> `requestJoin`.
async fn request_join(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(body): Json<RequestJoinBody>,
) -> ApiResult<Json<AllianceJoinRequestDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        AllianceBo::request_join(&mut conn, body.alliance_id, user.id as i32).await?,
    ))
}

/// `acceptRequest` -> `acceptJoin`.
async fn accept_join_request(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(body): Json<JoinRequestIdBody>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    AllianceBo::accept_join(&mut conn, body.join_request_id, user.id as i32).await?;
    Ok(StatusCode::OK)
}

/// `rejectRequest` -> `rejectJoin`.
async fn reject_join_request(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(body): Json<JoinRequestIdBody>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    AllianceBo::reject_join(&mut conn, body.join_request_id, user.id as i32).await?;
    Ok(StatusCode::OK)
}

/// `leave`.
async fn leave(State(state): State<AppState>, GameUser(user): GameUser) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    AllianceBo::leave(&mut conn, user.id as i32).await?;
    Ok(StatusCode::OK)
}
