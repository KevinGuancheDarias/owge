//! `MissionRestService` — the player-facing unit-mission registration endpoints
//! under `game/mission` (explore, gather, establishBase, attack, counterattack,
//! conquest, deploy, cancel). All require a game user; logic lives in the
//! business `UnitMissionBo`.
//!
//! Each registration endpoint takes a [`UnitMissionInformation`] body (the
//! frontend omits `userId`, which is taken from the JWT, and `missionType`,
//! which the endpoint pins). The Java `myRegister*` methods do exactly two
//! things before delegating to the shared `commonMissionRegister`: set the
//! sender to the logged-in user, then call the per-type `adminRegister*`
//! validator. We preserve that split by setting `user_id` from the `GameUser`
//! extractor here and delegating to the matching `UnitMissionBo::my_register_*`.
//!
//! ## Parity status (M3)
//! The high-level `UnitMissionBo` wrapper — which acquires the source/target
//! **planet locks**, runs the mission-limit / planet-explored / per-type
//! pre-checks, and calls `UnitMissionRegistrationBo::do_common_mission_register`
//! on the locked connection — is not yet ported as a Rust `Bo`
//! (`do_common_mission_register` exists, but only the inner, lock-naive entry).
//! Per the M3 contract we keep these handlers thin and delegate to the expected
//! `UnitMissionBo` API; until that Bo lands they answer `501` rather than
//! duplicating the lock/check business logic into the REST layer (which would
//! break the layering rule). See `NOTES` in the agent report.

use axum::extract::State;
use axum::http::StatusCode;
use axum::routing::post;
use axum::{Json, Router};

use owge_business::bo::{UnitMissionBo, UserStorageBo};
use owge_business::model::{MissionType, UserStorage};
use owge_business::pojo::unit_mission_information::UnitMissionInformation;
use owge_business::OwgeError;

use crate::auth::GameUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/game/mission/explorePlanet", post(explore_planet))
        .route("/game/mission/gather", post(gather))
        .route("/game/mission/establishBase", post(establish_base))
        .route("/game/mission/attack", post(attack))
        .route("/game/mission/counterattack", post(counterattack))
        .route("/game/mission/conquest", post(conquest))
        .route("/game/mission/deploy", post(deploy))
        .route("/game/mission/cancel", post(cancel))
}

/// Set the sender to the authenticated user and pin the mission type, mirroring
/// `myRegister` + `commonMissionRegister`'s `setMissionType`.
fn prepare(mut info: UnitMissionInformation, user_id: i32, mission_type: MissionType) -> UnitMissionInformation {
    info.user_id = Some(user_id);
    info.mission_type = Some(mission_type);
    info
}

/// Resolve the `UserStorage` for the authenticated game user (the registration
/// `Bo` methods take the entity, not just the id, mirroring Java's
/// `userStorageBo.findLoggedIn()`).
async fn load_user(state: &AppState, user_id: i32) -> ApiResult<UserStorage> {
    UserStorageBo::find_by_id(&state.db, user_id)
        .await?
        .ok_or_else(|| OwgeError::NotFound(format!("No user with id {user_id}")).into())
}

/// `explorePlanet` -> `UnitMissionBo.myRegisterExploreMission`.
async fn explore_planet(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(info): Json<UnitMissionInformation>,
) -> ApiResult<StatusCode> {
    let stored = load_user(&state, user.id as i32).await?;
    let info = prepare(info, user.id as i32, MissionType::Explore);
    UnitMissionBo::my_register_explore_mission(&state.db, &stored, info).await?;
    Ok(StatusCode::OK)
}

/// `gather` -> `UnitMissionBo.myRegisterGatherMission`.
async fn gather(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(info): Json<UnitMissionInformation>,
) -> ApiResult<StatusCode> {
    let stored = load_user(&state, user.id as i32).await?;
    let info = prepare(info, user.id as i32, MissionType::Gather);
    UnitMissionBo::my_register_gather_mission(&state.db, &stored, info).await?;
    Ok(StatusCode::OK)
}

/// `establishBase` -> `UnitMissionBo.myRegisterEstablishBaseMission`.
async fn establish_base(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(info): Json<UnitMissionInformation>,
) -> ApiResult<StatusCode> {
    let stored = load_user(&state, user.id as i32).await?;
    let info = prepare(info, user.id as i32, MissionType::EstablishBase);
    UnitMissionBo::my_register_establish_base_mission(&state.db, &stored, info).await?;
    Ok(StatusCode::OK)
}

/// `attack` -> `UnitMissionBo.myRegisterAttackMission`.
async fn attack(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(info): Json<UnitMissionInformation>,
) -> ApiResult<StatusCode> {
    let stored = load_user(&state, user.id as i32).await?;
    let info = prepare(info, user.id as i32, MissionType::Attack);
    UnitMissionBo::my_register_attack_mission(&state.db, &stored, info).await?;
    Ok(StatusCode::OK)
}

/// `counterattack` -> `UnitMissionBo.myRegisterCounterattackMission` (validates
/// the target planet belongs to the sender before registering).
async fn counterattack(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(info): Json<UnitMissionInformation>,
) -> ApiResult<StatusCode> {
    let stored = load_user(&state, user.id as i32).await?;
    let info = prepare(info, user.id as i32, MissionType::Counterattack);
    UnitMissionBo::my_register_counterattack_mission(&state.db, &stored, info).await?;
    Ok(StatusCode::OK)
}

/// `conquest` -> `UnitMissionBo.myRegisterConquestMission` (rejects conquering
/// your own / a home planet before registering).
async fn conquest(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(info): Json<UnitMissionInformation>,
) -> ApiResult<StatusCode> {
    let stored = load_user(&state, user.id as i32).await?;
    let info = prepare(info, user.id as i32, MissionType::Conquest);
    UnitMissionBo::my_register_conquest_mission(&state.db, &stored, info).await?;
    Ok(StatusCode::OK)
}

/// `deploy` -> `UnitMissionBo.myRegisterDeploy` (rejects deploying to the source
/// planet before registering).
async fn deploy(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(info): Json<UnitMissionInformation>,
) -> ApiResult<StatusCode> {
    let stored = load_user(&state, user.id as i32).await?;
    let info = prepare(info, user.id as i32, MissionType::Deploy);
    UnitMissionBo::my_register_deploy(&state.db, &stored, info).await?;
    Ok(StatusCode::OK)
}

#[derive(serde::Deserialize)]
struct CancelQuery {
    /// `missions.id` is `bigint unsigned`; the Java param is boxed `Long` but the
    /// id is always positive, so `u64` matches the column.
    id: u64,
}

/// `cancel` -> `UnitMissionBo.myCancelMission` — marks the mission resolved and
/// registers a return for the in-flight units (rejecting other players' missions
/// and RETURN_MISSIONs). Returns the Java `"OK"` body.
///
async fn cancel(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    axum::extract::Query(q): axum::extract::Query<CancelQuery>,
) -> ApiResult<Json<&'static str>> {
    UnitMissionBo::my_cancel_mission(&state.db, user.id as i32, q.id).await?;
    Ok(Json("OK"))
}
