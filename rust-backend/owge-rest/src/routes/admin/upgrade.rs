//! `AdminUpgradeRestService` — `CrudWithFullRestService<Upgrade>` (OBJECT_CODE
//! `UPGRADE`). Fully ports the standard CRUD, the requirement read/write
//! sub-resources (`GET/POST/DELETE {id}/requirements`), and the improvement
//! sub-resources (`GET/PUT {id}/improvement` and `{id}/improvement/
//! unitTypeImprovements`). The requirement write side does NOT recompute unlocks
//! (mirroring the Java trait).

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::{RequirementBo, UpgradeBo};
use owge_business::dto::upgrade::UpgradeInput;
use owge_business::dto::{
    ImprovementDto, ImprovementUnitTypeDto, RequirementInformationDto, RequirementInformationInput,
    UpgradeDto,
};

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

/// `getObject()` -> `ObjectEnum.UPGRADE`.
const OBJECT_CODE: &str = "UPGRADE";

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/upgrade", get(find_all).post(save_new))
        .route(
            "/admin/upgrade/{id}",
            get(find_one).put(save_existing).delete(delete_one),
        )
        .route(
            "/admin/upgrade/{id}/requirements",
            get(find_requirements).post(save_requirement),
        )
        .route(
            "/admin/upgrade/{id}/requirements/{requirement_information_id}",
            axum::routing::delete(delete_requirement),
        )
        .route(
            "/admin/upgrade/{id}/improvement",
            get(find_improvement).put(save_improvement),
        )
        .route(
            "/admin/upgrade/{id}/improvement/unitTypeImprovements",
            get(find_unit_type_improvements).post(add_unit_type_improvement),
        )
        .route(
            "/admin/upgrade/{id}/improvement/unitTypeImprovements/{unit_type_improvement_id}",
            axum::routing::delete(delete_unit_type_improvement),
        )
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<UpgradeDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(UpgradeBo::find_all(&mut conn).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<UpgradeDto>> {
    let mut conn = state.db.acquire().await?;
    UpgradeBo::find_one(&mut conn, id)
        .await?
        .map(Json)
        .ok_or_else(|| {
            ApiError(owge_business::OwgeError::NotFound(format!(
                "No upgrade {id}"
            )))
        })
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<UpgradeInput>,
) -> ApiResult<Json<UpgradeDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(UpgradeBo::save_new(&mut conn, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<UpgradeInput>,
) -> ApiResult<Json<UpgradeDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(UpgradeBo::save_existing(&mut conn, id, &input).await?))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    UpgradeBo::delete(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `CrudWithRequirements` `GET {id}/requirements`.
async fn find_requirements(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<RequirementInformationDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        RequirementBo::find_requirements(&mut conn, OBJECT_CODE, id as i16).await?,
    ))
}

/// `POST {id}/requirements` — relation forced to `(UPGRADE, id)`.
async fn save_requirement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<RequirementInformationInput>,
) -> ApiResult<Json<RequirementInformationDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        RequirementBo::add_requirement_from_dto(&mut conn, OBJECT_CODE, id as i16, &input).await?,
    ))
}

/// `DELETE {id}/requirements/{requirementInformationId}`.
async fn delete_requirement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((_id, requirement_information_id)): Path<(u16, i16)>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    RequirementBo::delete_requirement_information(&mut conn, requirement_information_id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `CrudWithImprovements` `GET {id}/improvement`.
async fn find_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<ImprovementDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(UpgradeBo::find_improvement(&mut conn, id).await?))
}

/// `CrudWithImprovements` `PUT {id}/improvement`.
async fn save_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(dto): Json<ImprovementDto>,
) -> ApiResult<Json<ImprovementDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        UpgradeBo::save_improvement(&mut conn, id, &dto).await?,
    ))
}

/// `GET {id}/improvement/unitTypeImprovements`.
async fn find_unit_type_improvements(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<ImprovementUnitTypeDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        UpgradeBo::find_unit_type_improvements(&mut conn, id).await?,
    ))
}

/// `POST {id}/improvement/unitTypeImprovements`.
async fn add_unit_type_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(dto): Json<ImprovementUnitTypeDto>,
) -> ApiResult<Json<ImprovementUnitTypeDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        UpgradeBo::add_unit_type_improvement(&mut conn, id, &dto).await?,
    ))
}

/// `DELETE {id}/improvement/unitTypeImprovements/{unitTypeImprovementId}`.
async fn delete_unit_type_improvement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((id, unit_type_improvement_id)): Path<(u16, u16)>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    UpgradeBo::delete_unit_type_improvement(&mut conn, id, unit_type_improvement_id).await?;
    Ok(StatusCode::NO_CONTENT)
}
