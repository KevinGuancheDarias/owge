//! `UnitRestService` — the player-facing unit endpoints under `game/unit`
//! (build, cancel, delete, the deprecated `findRunning`, and the per-unit
//! critical-attack lookup). All require a game user.
//!
//! ## Parity status (M3)
//! `build` (`MissionBo.registerBuildUnit`) and `cancel`
//! (`MissionBo.cancelBuildUnit`) are *upgrade/build mission* registration, which
//! is part of the mission engine and not yet ported as a Rust `Bo`
//! (`MissionBo` has no Rust equivalent on disk yet). They answer `501` until the
//! build-mission registration lands, rather than registering a build that can
//! never be scheduled/run. `findRunning` likewise depends on the running-mission
//! finder (M3). `delete` only needs `ObtainedUnitBo.save_with_subtraction`, and
//! `criticalAttack` only needs `UnitBo`/`CriticalAttackBo`; both are wired live
//! when those Bo methods are available and otherwise stubbed (see TODOs).

use axum::extract::State;
use axum::http::StatusCode;
use axum::routing::{get, post};
use axum::{Json, Router};

use axum::response::{IntoResponse, Response};
use serde_json::json;

use owge_business::bo::{CriticalAttackBo, MissionBo, MissionFinderBo, ObtainedUnitBo, UnitBo};
use owge_business::dto::{CriticalAttackInformationResponse, ObtainedUnitDto, UnitDto};

use crate::auth::GameUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/game/unit/findRunning", get(find_running))
        .route("/game/unit/build", post(build))
        .route("/game/unit/cancel", get(cancel))
        .route("/game/unit/delete", post(delete))
        .route("/game/unit/{unitId}/criticalAttack", get(critical_attack))
}

#[derive(serde::Deserialize)]
struct FindRunningQuery {
    /// Java param `planetId` is a boxed `Double`; the value is a planet id.
    /// `planets.id` = `bigint unsigned`, so parse as `u64`.
    #[serde(rename = "planetId")]
    planet_id: u64,
}

/// `findRunning` (deprecated since 0.9.0) -> `MissionFinderBo.findRunningUnitBuild`.
///
/// Returns the empty JSON string `""` when there is no running build, otherwise a
/// `DeprecationRestResponse<RunningUnitBuildDto>`: a `deprecated`
/// `{sinceVersion, useInstead}` map plus the `RunningUnitBuildDto` fields flattened
/// alongside it (Jackson `@JsonUnwrapped`).
async fn find_running(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    axum::extract::Query(q): axum::extract::Query<FindRunningQuery>,
) -> ApiResult<Response> {
    let mut conn = state.db.acquire().await?;
    let running =
        MissionFinderBo::find_running_unit_build(&mut conn, user.id as i32, q.planet_id).await?;
    match running {
        // Java `return "";` via StringHttpMessageConverter writes an EMPTY body
        // (text/plain, zero bytes), not the JSON string `""`.
        None => Ok(String::new().into_response()),
        Some(dto) => {
            // @JsonUnwrapped: merge the dto's fields with the `deprecated` map.
            let mut body = serde_json::to_value(&dto).map_err(owge_business::OwgeError::from)?;
            if let serde_json::Value::Object(map) = &mut body {
                map.insert(
                    "deprecated".to_string(),
                    json!({ "sinceVersion": "0.9.0", "useInstead": "/unit/build-missions" }),
                );
            }
            Ok(Json(body).into_response())
        }
    }
}

#[derive(serde::Deserialize)]
struct BuildQuery {
    /// `planets.id` = `bigint unsigned`.
    #[serde(rename = "planetId")]
    planet_id: u64,
    /// `units.id` = `smallint unsigned`; the Java param is boxed `Integer`.
    #[serde(rename = "unitId")]
    unit_id: u16,
    /// How many to build (`Long`); always positive.
    count: u64,
}

/// `build` -> `MissionBo.registerBuildUnit` (Java returns `void`/200). The
/// BUILD_UNIT registration: per-user + per-planet lock, unlock/resource/energy/
/// unit-type-limit checks, resource deduction, mission + obtained-unit, schedule.
async fn build(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    axum::extract::Query(q): axum::extract::Query<BuildQuery>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    MissionBo::register_build_unit(
        &mut conn,
        user.id as i32,
        q.planet_id,
        q.unit_id,
        q.count as i64,
    )
    .await?;
    Ok(StatusCode::OK)
}

#[derive(serde::Deserialize)]
struct CancelQuery {
    /// `missions.id` = `bigint unsigned`.
    #[serde(rename = "missionId")]
    mission_id: u64,
}

/// `cancel` -> `MissionBo.cancelBuildUnit`. Returns the Java `"OK"` body.
async fn cancel(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    axum::extract::Query(q): axum::extract::Query<CancelQuery>,
) -> ApiResult<Json<&'static str>> {
    let mut conn = state.db.acquire().await?;
    MissionBo::cancel_build_unit(&mut conn, user.id as i32, q.mission_id).await?;
    Ok(Json("OK"))
}

/// `delete` -> `ObtainedUnitBo.saveWithSubtraction(dto, true)` after stamping the
/// invoker as the owner. The Java body is an `ObtainedUnitDto`; only `id`,
/// `count` and the (server-stamped) `userId` matter to the subtract path, so we
/// accept the minimal shape and rebuild a DTO for the Bo call. Returns the Java
/// `"OK"` body.
#[derive(serde::Deserialize)]
struct DeleteRequest {
    /// `obtained_units.id` = `bigint unsigned`.
    id: u64,
    /// How many to subtract (`Long`); the full stack count deletes the row.
    count: u64,
}

async fn delete(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    Json(body): Json<DeleteRequest>,
) -> ApiResult<Json<&'static str>> {
    let mut conn = state.db.acquire().await?;
    // obtainedUnitDto.setUserId(loggedIn.id) — server-stamped owner.
    let dto = ObtainedUnitDto {
        id: body.id,
        // Only `id`/`count`/`userId` matter to `save_with_subtraction`; this
        // placeholder unit is never read.
        unit: UnitDto {
            id: 0,
            name: String::new(),
            description: None,
            image: None,
            image_url: None,
            order: None,
            has_to_display_in_requirements: false,
            points: None,
            time: None,
            primary_resource: None,
            secondary_resource: None,
            energy: None,
            type_id: None,
            type_name: None,
            attack: None,
            health: None,
            shield: None,
            charge: None,
            is_unique: false,
            can_fast_explore: false,
            speed: None,
            cloned_improvements: false,
            bypass_shield: false,
            is_invisible: false,
            stored_weight: 0,
            storage_capacity: None,
            improvement: None,
            speed_impact_group: None,
        },
        count: body.count,
        source_planet: None,
        target_planet: None,
        user_id: user.id as i32,
        username: None,
        temporal_information: None,
        stored_units: Vec::new(),
    };
    ObtainedUnitBo::save_with_subtraction(&mut conn, &dto, true).await?;
    Ok(Json("OK"))
}

/// `findCriticalAttackInformation` -> `UnitBo.findUsedCriticalAttack` +
/// `CriticalAttackBo.buildFullInformation`. Returns `[]` when the unit uses no
/// critical attack.
async fn critical_attack(
    State(state): State<AppState>,
    _user: GameUser,
    axum::extract::Path(unit_id): axum::extract::Path<u16>,
) -> ApiResult<Json<Vec<CriticalAttackInformationResponse>>> {
    let mut conn = state.db.acquire().await?;
    match UnitBo::find_used_critical_attack(&mut conn, unit_id).await? {
        Some(critical_attack_id) => Ok(Json(
            CriticalAttackBo::build_full_information(&mut conn, critical_attack_id).await?,
        )),
        None => Ok(Json(Vec::new())),
    }
}
