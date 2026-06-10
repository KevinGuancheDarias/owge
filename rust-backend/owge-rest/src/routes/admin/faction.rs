//! `AdminFactionRestService` — `CrudWithImprovementsRestServiceTrait<Faction>` +
//! `WithImageRestServiceTrait` + the faction-specific sub-resources (mapping
//! `admin/faction`). `getObject()` is `FACTION`.
//!
//! Fully ported: the standard CRUD, the improvement sub-resources (`GET/PUT
//! {id}/improvement` and `{id}/improvement/unitTypeImprovements`), and the
//! faction-specific `unitTypes` / `spawn-locations` read+write sub-resources.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::FactionBo;
use owge_business::dto::faction::{
    FactionDto, FactionInput, FactionSpawnLocationDto, FactionSpawnLocationInput,
    FactionUnitTypeDto, FactionUnitTypeOverrideInput,
};
use owge_business::dto::{ImprovementDto, ImprovementUnitTypeDto};

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/faction", get(find_all).post(save_new))
        .route(
            "/admin/faction/{id}",
            get(find_one).put(save_existing).delete(delete_one),
        )
        .route(
            "/admin/faction/{id}/improvement",
            get(find_improvement).put(save_improvement),
        )
        .route(
            "/admin/faction/{id}/improvement/unitTypeImprovements",
            get(find_unit_type_improvements).post(add_unit_type_improvement),
        )
        .route(
            "/admin/faction/{id}/improvement/unitTypeImprovements/{unit_type_improvement_id}",
            axum::routing::delete(delete_unit_type_improvement),
        )
        .route(
            "/admin/faction/{id}/unitTypes",
            get(find_unit_types).put(save_unit_types),
        )
        .route(
            "/admin/faction/{id}/spawn-locations",
            get(find_spawn_locations).put(save_spawn_locations),
        )
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<FactionDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(FactionBo::find_all(&mut conn).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<FactionDto>> {
    let mut conn = state.db.acquire().await?;
    FactionBo::find_by_id(&mut conn, id)
        .await?
        .map(Json)
        .ok_or_else(|| {
            ApiError(owge_business::OwgeError::NotFound(format!(
                "No faction {id}"
            )))
        })
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<FactionInput>,
) -> ApiResult<Json<FactionDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(FactionBo::save_new(&mut conn, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<FactionInput>,
) -> ApiResult<Json<FactionDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(FactionBo::save_existing(&mut conn, id, &input).await?))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    FactionBo::delete(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `AdminFactionRestService.findUnitTypesOverrides` — `GET '{id}/unitTypes'`.
async fn find_unit_types(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<FactionUnitTypeDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        FactionBo::find_unit_type_overrides(&mut conn, id).await?,
    ))
}

/// `AdminFactionRestService.saveUnitTypes` — `PUT '{id}/unitTypes'`
/// (`FactionBo.saveOverrides`).
async fn save_unit_types(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(overrides): Json<Vec<FactionUnitTypeOverrideInput>>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    FactionBo::save_overrides(&mut conn, id, &overrides).await?;
    Ok(StatusCode::OK)
}

/// `AdminFactionRestService.findSpawnLocations` — `GET '{id}/spawn-locations'`.
async fn find_spawn_locations(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<FactionSpawnLocationDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(FactionBo::find_spawn_locations(&mut conn, id).await?))
}

/// `AdminFactionRestService.saveSpawnLocations` — `PUT '{id}/spawn-locations'`
/// (`FactionSpawnLocationBo.saveSpawnLocations`).
async fn save_spawn_locations(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(spawn_locations): Json<Vec<FactionSpawnLocationInput>>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    FactionBo::save_spawn_locations(&mut conn, id, &spawn_locations).await?;
    Ok(StatusCode::OK)
}

/// `CrudWithImprovements` `GET '{id}/improvement'`.
async fn find_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<ImprovementDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(FactionBo::find_improvement(&mut conn, id).await?))
}

/// `CrudWithImprovements` `PUT '{id}/improvement'`.
async fn save_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(dto): Json<ImprovementDto>,
) -> ApiResult<Json<ImprovementDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        FactionBo::save_improvement(&mut conn, id, &dto).await?,
    ))
}

/// `GET '{id}/improvement/unitTypeImprovements'`.
async fn find_unit_type_improvements(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<ImprovementUnitTypeDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        FactionBo::find_unit_type_improvements(&mut conn, id).await?,
    ))
}

/// `POST '{id}/improvement/unitTypeImprovements'`.
async fn add_unit_type_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(dto): Json<ImprovementUnitTypeDto>,
) -> ApiResult<Json<ImprovementUnitTypeDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        FactionBo::add_unit_type_improvement(&mut conn, id, &dto).await?,
    ))
}

/// `DELETE '{id}/improvement/unitTypeImprovements/{unitTypeImprovementId}'`.
async fn delete_unit_type_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((id, unit_type_improvement_id)): Path<(u16, u16)>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    FactionBo::delete_unit_type_improvement(&mut conn, id, unit_type_improvement_id).await?;
    Ok(StatusCode::NO_CONTENT)
}
