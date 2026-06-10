//! `AdminSpecialLocationRestService` — `CrudWithImprovementsRestServiceTrait<SpecialLocation>`
//! (mapping `admin/special-location`). `getObject()` is `SPECIAL_LOCATION`.
//!
//! The standard CRUD plus the improvement sub-resources (`GET/PUT
//! {id}/improvement` and `{id}/improvement/unitTypeImprovements`) are ported.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::SpecialLocationBo;
use owge_business::dto::ImprovementUnitTypeDto;
use owge_business::dto::special_location::{
    ImprovementDto, SpecialLocationDto, SpecialLocationInput,
};

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

// `getObject()` for this controller is `SPECIAL_LOCATION`. It is a
// `CrudWithImprovements` (not `CrudWithRequirements`), so there is no
// `{id}/requirements` sub-resource and the object code is not used at runtime.

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/special-location", get(find_all).post(save_new))
        .route(
            "/admin/special-location/{id}",
            get(find_one).put(save_existing).delete(delete_one),
        )
        .route(
            "/admin/special-location/{id}/improvement",
            get(find_improvement).put(save_improvement),
        )
        .route(
            "/admin/special-location/{id}/improvement/unitTypeImprovements",
            get(find_unit_type_improvements).post(add_unit_type_improvement),
        )
        .route(
            "/admin/special-location/{id}/improvement/unitTypeImprovements/{unit_type_improvement_id}",
            axum::routing::delete(delete_unit_type_improvement),
        )
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<SpecialLocationDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(SpecialLocationBo::find_all(&mut conn).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<SpecialLocationDto>> {
    let mut conn = state.db.acquire().await?;
    SpecialLocationBo::find_by_id(&mut conn, id)
        .await?
        .map(Json)
        .ok_or_else(|| {
            ApiError(owge_business::OwgeError::NotFound(format!(
                "No special location {id}"
            )))
        })
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<SpecialLocationInput>,
) -> ApiResult<Json<SpecialLocationDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(SpecialLocationBo::save_new(&mut conn, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<SpecialLocationInput>,
) -> ApiResult<Json<SpecialLocationDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        SpecialLocationBo::save_existing(&mut conn, id, &input).await?,
    ))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    SpecialLocationBo::delete(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `CrudWithImprovementsRestServiceTrait` `GET '{id}/improvement'` — the special
/// location's improvement row as an `ImprovementDto`.
async fn find_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<ImprovementDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        SpecialLocationBo::find_improvement(&mut conn, id).await?,
    ))
}

/// `PUT '{id}/improvement'` — `ImprovementBo.createOrUpdateFromDto`.
async fn save_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(dto): Json<ImprovementDto>,
) -> ApiResult<Json<ImprovementDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        SpecialLocationBo::save_improvement(&mut conn, id, &dto).await?,
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
        SpecialLocationBo::find_unit_type_improvements(&mut conn, id).await?,
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
        SpecialLocationBo::add_unit_type_improvement(&mut conn, id, &dto).await?,
    ))
}

/// `DELETE '{id}/improvement/unitTypeImprovements/{unitTypeImprovementId}'`.
async fn delete_unit_type_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((id, unit_type_improvement_id)): Path<(u16, u16)>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    SpecialLocationBo::delete_unit_type_improvement(&mut conn, id, unit_type_improvement_id)
        .await?;
    Ok(StatusCode::NO_CONTENT)
}
