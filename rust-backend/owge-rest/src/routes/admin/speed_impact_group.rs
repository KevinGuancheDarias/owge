//! `AdminSpeedImpactGroupRestService` (`admin/speed-impact-group`) —
//! `CrudRestServiceTrait` + `CrudWithRequirementGroupsRestServiceTrait`.
//! `getObject()` is `SPEED_IMPACT_GROUP`.
//!
//! The standard CRUD and the requirement-group sub-resources
//! (`GET/POST {id}/requirement-group`, `DELETE {id}/requirement-group/{groupId}`,
//! `POST {id}/requirement-group/{groupId}/requirement`, and `DELETE
//! .../requirement/{reqInfoId}`) are ported via `RequirementGroupBo`. The
//! `REQUIREMENT_GROUP_CACHE_TAG` eviction is M4 (see the Bo TODOs).

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::{RequirementBo, RequirementGroupBo, SpeedImpactGroupBo};
use owge_business::dto::{
    RequirementGroupDto, RequirementGroupInput, RequirementInformationDto,
    RequirementInformationInput, SpeedImpactGroupDto, SpeedImpactGroupInput,
};

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

/// `getObject()` -> `ObjectEnum.SPEED_IMPACT_GROUP`.
const OBJECT_CODE: &str = "SPEED_IMPACT_GROUP";

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/speed-impact-group", get(find_all).post(save_new))
        .route(
            "/admin/speed-impact-group/{id}",
            get(find_one).put(save_existing).delete(delete_one),
        )
        .route(
            "/admin/speed-impact-group/{id}/requirement-group",
            get(find_requirement_groups).post(add_requirement_group),
        )
        .route(
            "/admin/speed-impact-group/{id}/requirement-group/{group_id}",
            axum::routing::delete(delete_requirement_group),
        )
        .route(
            "/admin/speed-impact-group/{id}/requirement-group/{group_id}/requirement",
            axum::routing::post(add_group_requirement),
        )
        .route(
            "/admin/speed-impact-group/{id}/requirement-group/{group_id}/requirement/{requirement_information_id}",
            axum::routing::delete(delete_group_requirement),
        )
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<SpeedImpactGroupDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(SpeedImpactGroupBo::find_all_dtos(&mut conn).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<SpeedImpactGroupDto>> {
    let mut conn = state.db.acquire().await?;
    SpeedImpactGroupBo::find_by_id(&mut conn, id)
        .await?
        .map(Json)
        .ok_or_else(|| {
            ApiError(owge_business::OwgeError::NotFound(format!(
                "No speed impact group {id}"
            )))
        })
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<SpeedImpactGroupInput>,
) -> ApiResult<Json<SpeedImpactGroupDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(SpeedImpactGroupBo::save_new(&mut conn, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<SpeedImpactGroupInput>,
) -> ApiResult<Json<SpeedImpactGroupDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        SpeedImpactGroupBo::save_existing(&mut conn, id, &input).await?,
    ))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    SpeedImpactGroupBo::delete(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `GET {id}/requirement-group` — the requirement groups attached to this speed
/// impact group, each with its requirements.
async fn find_requirement_groups(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<Vec<RequirementGroupDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        RequirementGroupBo::find_groups(&mut conn, OBJECT_CODE, id as i16).await?,
    ))
}

/// `POST {id}/requirement-group` — `RequirementGroupBo.add`.
async fn add_requirement_group(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
    Json(input): Json<RequirementGroupInput>,
) -> ApiResult<Json<RequirementGroupDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        RequirementGroupBo::add(
            &mut conn,
            OBJECT_CODE,
            id as i16,
            input.name.as_deref(),
            &input.requirements,
        )
        .await?,
    ))
}

/// `DELETE {id}/requirement-group/{groupId}`.
async fn delete_requirement_group(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((_id, group_id)): Path<(u16, u16)>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    RequirementGroupBo::delete(&mut conn, group_id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `POST {id}/requirement-group/{requirementGroupId}/requirement` — add a
/// requirement to the group (relation forced to `(REQUIREMENT_GROUP, groupId)`).
async fn add_group_requirement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((_id, group_id)): Path<(u16, u16)>,
    Json(input): Json<RequirementInformationInput>,
) -> ApiResult<Json<RequirementInformationDto>> {
    let mut conn = state.db.acquire().await?;
    let dto = RequirementBo::add_requirement_from_dto(
        &mut conn,
        "REQUIREMENT_GROUP",
        group_id as i16,
        &input,
    )
    .await?;
    // No-op in the Rust port: the Java REQUIREMENT_GROUP taggable-cache is not
    // replicated — requirement group data is recomputed on demand, so there is
    // nothing to evict.
    Ok(Json(dto))
}

/// `DELETE {id}/requirement-group/{requirementGroupId}/requirement/{requirementInformationId}`.
async fn delete_group_requirement(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((_id, _group_id, requirement_information_id)): Path<(u16, u16, i16)>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    RequirementBo::delete_requirement_information(&mut conn, requirement_information_id).await?;
    // No-op in the Rust port: the Java REQUIREMENT_GROUP taggable-cache is not
    // replicated — requirement group data is recomputed on demand, so there is
    // nothing to evict.
    Ok(StatusCode::NO_CONTENT)
}
