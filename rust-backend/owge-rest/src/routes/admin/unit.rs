//! `AdminUnitRestService` — `CrudWithFullRestService<Unit>` + the unit-specific
//! sub-resources (mapping `admin/unit`). `getObject()` is `UNIT`.
//!
//! Fully ported: the standard CRUD, the requirement read/write sub-resources
//! (`GET/POST/DELETE {id}/requirements`), and the improvement sub-resources
//! (`GET/PUT {id}/improvement` and `{id}/improvement/unitTypeImprovements`). The
//! requirement write side does NOT recompute unlocks, mirroring the Java trait
//! (which calls `addRequirementFromDto` directly, without `triggerRelationChanged`).

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::{RequirementBo, UnitBo};
use owge_business::dto::unit::UnitInput;
use owge_business::dto::{
    ImprovementDto, ImprovementUnitTypeDto, RequirementInformationDto, RequirementInformationInput,
    UnitDto,
};

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

/// `getObject()` — the `ObjectEnum` code this controller's relations live under.
const OBJECT_CODE: &str = "UNIT";

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/unit", get(find_all).post(save_new))
        .route(
            "/admin/unit/{id}",
            get(find_one).put(save_existing).delete(delete_one),
        )
        .route(
            "/admin/unit/{id}/requirements",
            get(find_requirements).post(save_requirement),
        )
        .route(
            "/admin/unit/{id}/requirements/{requirement_information_id}",
            axum::routing::delete(delete_requirement),
        )
        .route(
            "/admin/unit/{id}/improvement",
            get(find_improvement).put(save_improvement),
        )
        .route(
            "/admin/unit/{id}/improvement/unitTypeImprovements",
            get(find_unit_type_improvements).post(add_unit_type_improvement),
        )
        .route(
            "/admin/unit/{id}/improvement/unitTypeImprovements/{unit_type_improvement_id}",
            axum::routing::delete(delete_unit_type_improvement),
        )
        .route(
            "/admin/unit/{id}/criticalAttack",
            axum::routing::delete(unset_critical_attack),
        )
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<UnitDto>>> {
    Ok(Json(UnitBo::find_all(&state.db).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<UnitDto>> {
    UnitBo::find_by_id(&state.db, id)
        .await?
        .map(Json)
        .ok_or_else(|| ApiError(owge_business::OwgeError::NotFound(format!("No unit {id}"))))
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<UnitInput>,
) -> ApiResult<Json<UnitDto>> {
    Ok(Json(UnitBo::save_new(&state.db, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<UnitInput>,
) -> ApiResult<Json<UnitDto>> {
    Ok(Json(UnitBo::save_existing(&state.db, id, &input).await?))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    UnitBo::delete(&state.db, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `CrudWithRequirements` `GET '{id}/requirements'` — every requirement attached
/// to this unit's relation.
async fn find_requirements(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<RequirementInformationDto>>> {
    Ok(Json(
        RequirementBo::find_requirements(&state.db, OBJECT_CODE, id as i16).await?,
    ))
}

/// `POST '{id}/requirements'` — `RequirementBo.addRequirementFromDto` with the
/// relation forced to `(UNIT, id)`.
async fn save_requirement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<RequirementInformationInput>,
) -> ApiResult<Json<RequirementInformationDto>> {
    Ok(Json(
        RequirementBo::add_requirement_from_dto(&state.db, OBJECT_CODE, id as i16, &input).await?,
    ))
}

/// `DELETE '{id}/requirements/{requirementInformationId}'`.
async fn delete_requirement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((_id, requirement_information_id)): Path<(u16, i16)>,
) -> ApiResult<StatusCode> {
    RequirementBo::delete_requirement_information(&state.db, requirement_information_id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `CrudWithImprovements` `GET '{id}/improvement'`.
async fn find_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<ImprovementDto>> {
    Ok(Json(UnitBo::find_improvement(&state.db, id).await?))
}

/// `CrudWithImprovements` `PUT '{id}/improvement'`.
async fn save_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(dto): Json<ImprovementDto>,
) -> ApiResult<Json<ImprovementDto>> {
    Ok(Json(UnitBo::save_improvement(&state.db, id, &dto).await?))
}

/// `GET '{id}/improvement/unitTypeImprovements'`.
async fn find_unit_type_improvements(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<ImprovementUnitTypeDto>>> {
    Ok(Json(UnitBo::find_unit_type_improvements(&state.db, id).await?))
}

/// `POST '{id}/improvement/unitTypeImprovements'`.
async fn add_unit_type_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(dto): Json<ImprovementUnitTypeDto>,
) -> ApiResult<Json<ImprovementUnitTypeDto>> {
    Ok(Json(
        UnitBo::add_unit_type_improvement(&state.db, id, &dto).await?,
    ))
}

/// `DELETE '{id}/improvement/unitTypeImprovements/{unitTypeImprovementId}'`.
async fn delete_unit_type_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((id, unit_type_improvement_id)): Path<(u16, u16)>,
) -> ApiResult<StatusCode> {
    UnitBo::delete_unit_type_improvement(&state.db, id, unit_type_improvement_id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `AdminUnitRestService.unsetCriticalAttack` — `DELETE '{id}/criticalAttack'`.
async fn unset_critical_attack(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    UnitBo::unset_critical_attack(&state.db, id).await?;
    Ok(StatusCode::NO_CONTENT)
}
