//! `AdminImageStoreRestService` — `admin/image_store`.
//!
//! Implements `WithReadRestServiceTrait` (`GET ''`, `GET '{id}'`) and
//! `WithDeleteRestServiceTrait` (`DELETE '{id}'`), plus the explicit metadata
//! update (`PUT '{id}'`) and the `POST ''` base64 upload
//! (`ImageStoreBo.save(base64, displayName)` → [`owge_business::bo::ImageStoreBo::save`]),
//! which decodes, magic-byte-validates, writes to disk and md5-dedupes the bytes.

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::ImageStoreBo;
use owge_business::dto::{ImageStoreDto, ImageStoreInput, ImageUploadInput};

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/image_store", get(find_all).post(upload_image))
        .route(
            "/admin/image_store/{id}",
            get(find_one).put(update_image).delete(delete_one),
        )
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<ImageStoreDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(ImageStoreBo::find_all(&mut conn).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u64>,
) -> ApiResult<Json<ImageStoreDto>> {
    let mut conn = state.db.acquire().await?;
    ImageStoreBo::find_by_id(&mut conn, id)
        .await?
        .map(Json)
        .ok_or_else(|| ApiError(owge_business::OwgeError::NotFound(format!("No image {id}"))))
}

async fn update_image(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u64>,
    Json(input): Json<ImageStoreInput>,
) -> ApiResult<Json<ImageStoreDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(ImageStoreBo::update(&mut conn, id, &input).await?))
}

async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u64>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    ImageStoreBo::delete(&mut conn, id).await?;
    Ok(StatusCode::NO_CONTENT)
}

/// `AdminImageStoreRestService.uploadImage` — `POST ''` with an `UploadImage`
/// `{ base64, displayName }` body. Decodes + mime-detects (png/gif/jpeg) the
/// payload, writes it under `OWGE_DYNAMIC_FILES_PATH`, md5-checksums the bytes
/// and dedupes by checksum (see `ImageStoreBo::save`).
async fn upload_image(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<ImageUploadInput>,
) -> ApiResult<Json<ImageStoreDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(ImageStoreBo::save(&mut conn, &input).await?))
}
