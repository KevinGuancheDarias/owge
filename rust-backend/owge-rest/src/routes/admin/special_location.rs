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
use owge_business::dto::special_location::{
    ImprovementDto, SpecialLocationDto, SpecialLocationInput,
};
use owge_business::dto::ImprovementUnitTypeDto;

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
    Ok(Json(SpecialLocationBo::find_all(&state.db).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<SpecialLocationDto>> {
    SpecialLocationBo::find_by_id(&state.db, id)
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
    Ok(Json(SpecialLocationBo::save_new(&state.db, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<SpecialLocationInput>,
) -> ApiResult<Json<SpecialLocationDto>> {
    Ok(Json(
        SpecialLocationBo::save_existing(&state.db, id, &input).await?,
    ))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    SpecialLocationBo::delete(&state.db, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `CrudWithImprovementsRestServiceTrait` `GET '{id}/improvement'` — the special
/// location's improvement row as an `ImprovementDto`.
async fn find_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<ImprovementDto>> {
    Ok(Json(SpecialLocationBo::find_improvement(&state.db, id).await?))
}

/// `PUT '{id}/improvement'` — `ImprovementBo.createOrUpdateFromDto`.
async fn save_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(dto): Json<ImprovementDto>,
) -> ApiResult<Json<ImprovementDto>> {
    Ok(Json(
        SpecialLocationBo::save_improvement(&state.db, id, &dto).await?,
    ))
}

/// `GET '{id}/improvement/unitTypeImprovements'`.
async fn find_unit_type_improvements(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<ImprovementUnitTypeDto>>> {
    Ok(Json(
        SpecialLocationBo::find_unit_type_improvements(&state.db, id).await?,
    ))
}

/// `POST '{id}/improvement/unitTypeImprovements'`.
async fn add_unit_type_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(dto): Json<ImprovementUnitTypeDto>,
) -> ApiResult<Json<ImprovementUnitTypeDto>> {
    Ok(Json(
        SpecialLocationBo::add_unit_type_improvement(&state.db, id, &dto).await?,
    ))
}

/// `DELETE '{id}/improvement/unitTypeImprovements/{unitTypeImprovementId}'`.
async fn delete_unit_type_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((id, unit_type_improvement_id)): Path<(u16, u16)>,
) -> ApiResult<StatusCode> {
    SpecialLocationBo::delete_unit_type_improvement(&state.db, id, unit_type_improvement_id)
        .await?;
    Ok(StatusCode::NO_CONTENT)
}
