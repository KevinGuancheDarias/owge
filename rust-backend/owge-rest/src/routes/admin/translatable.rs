//! `AdminTranslatableRestService` — `CrudRestServiceTrait<Translatable>` plus the
//! bespoke `{id}/translations` endpoints.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::TranslatableBo;
use owge_business::dto::{
    TranslatableDto, TranslatableInput, TranslatableTranslationDto, TranslatableTranslationInput,
};

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/translatable", get(find_all).post(save_new))
        .route(
            "/admin/translatable/{id}",
            get(find_one).put(save_existing).delete(delete_one),
        )
        .route(
            "/admin/translatable/{id}/translations",
            get(find_translations)
                .post(add_translation)
                .put(add_translation),
        )
        .route(
            "/admin/translatable/{id}/translations/{translation_id}",
            axum::routing::delete(delete_translation),
        )
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<TranslatableDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(TranslatableBo::find_all(&mut conn).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u32>,
) -> ApiResult<Json<TranslatableDto>> {
    let mut conn = state.db.acquire().await?;
    TranslatableBo::find_by_id(&mut conn, id)
        .await?
        .map(Json)
        .ok_or_else(|| {
            ApiError(owge_business::OwgeError::NotFound(format!(
                "No translatable {id}"
            )))
        })
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<TranslatableInput>,
) -> ApiResult<Json<TranslatableDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(TranslatableBo::save_new(&mut conn, &input).await?))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u32>,
    Json(input): Json<TranslatableInput>,
) -> ApiResult<Json<TranslatableDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        TranslatableBo::save_existing(&mut conn, id, &input).await?,
    ))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u32>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    TranslatableBo::delete(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

async fn find_translations(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u32>,
) -> ApiResult<Json<Vec<TranslatableTranslationDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        TranslatableBo::find_translations(&mut conn, id).await?,
    ))
}

async fn add_translation(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u32>,
    Json(input): Json<TranslatableTranslationInput>,
) -> ApiResult<Json<TranslatableTranslationDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        TranslatableBo::add_translation(&mut conn, id, &input).await?,
    ))
}

async fn delete_translation(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path((_id, translation_id)): Path<(u32, u32)>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    TranslatableBo::delete_translation(&mut conn, translation_id).await?;
    Ok(StatusCode::NO_CONTENT)
}
