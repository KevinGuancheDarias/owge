//! `AdminRuleRestService` (`admin/rules`).
//!
//! Unlike the standard `CrudRestServiceTrait` admin controllers, this one is a
//! plain controller exposing:
//!   - `GET origin/{originType}/{id}` — rules by origin type + origin id,
//!   - `GET type/{type}` — rules by type,
//!   - `GET type-descriptor/{type}` / `GET item-type-descriptor/{itemType}` —
//!     descriptor lookups resolved from the Spring `RuleTypeProvider` /
//!     `RuleItemTypeProvider` bean registry (reimplemented in `RuleBo`; see below),
//!   - `DELETE {id}` — delete by id (Java returns `void` => 200, empty body),
//!   - `POST ''` — save (insert when the body has no id, update otherwise).
//!
//! The two `*-descriptor` endpoints resolve their answer from the rule
//! type/item-type provider registry (time-special + unit rule providers),
//! reimplemented in `RuleBo`.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::{get, post};
use axum::{Json, Router};
use owge_business::bo::RuleBo;
use owge_business::dto::rule::{
    RuleDto, RuleInput, RuleItemTypeDescriptorDto, RuleTypeDescriptorDto,
};

use crate::auth::AdminUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/rules", post(save))
        .route("/admin/rules/{id}", axum::routing::delete(delete_by_id))
        .route(
            "/admin/rules/origin/{origin_type}/{id}",
            get(find_by_origin),
        )
        .route("/admin/rules/type/{type}", get(find_by_type))
        .route(
            "/admin/rules/type-descriptor/{type}",
            get(find_type_descriptor),
        )
        .route(
            "/admin/rules/item-type-descriptor/{item_type}",
            get(find_item_type_descriptor),
        )
}

async fn find_by_origin(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((origin_type, id)): Path<(String, i64)>,
) -> ApiResult<Json<Vec<RuleDto>>> {
    Ok(Json(
        RuleBo::find_by_origin_type_and_origin_id(&state.db, &origin_type, id).await?,
    ))
}

async fn find_by_type(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(rule_type): Path<String>,
) -> ApiResult<Json<Vec<RuleDto>>> {
    Ok(Json(RuleBo::find_by_type(&state.db, &rule_type).await?))
}

async fn find_type_descriptor(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(rule_type): Path<String>,
) -> ApiResult<Json<RuleTypeDescriptorDto>> {
    Ok(Json(
        RuleBo::find_type_descriptor(&state.db, &rule_type).await?,
    ))
}

async fn find_item_type_descriptor(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(item_type): Path<String>,
) -> ApiResult<Json<RuleItemTypeDescriptorDto>> {
    Ok(Json(
        RuleBo::find_item_type_descriptor(&state.db, &item_type).await?,
    ))
}

async fn delete_by_id(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<StatusCode> {
    RuleBo::delete_by_id(&state.db, id).await?;
    // Java controller method returns `void` => Spring 200 with empty body.
    Ok(StatusCode::OK)
}

async fn save(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<RuleInput>,
) -> ApiResult<Json<RuleDto>> {
    Ok(Json(RuleBo::save(&state.db, &input).await?))
}
