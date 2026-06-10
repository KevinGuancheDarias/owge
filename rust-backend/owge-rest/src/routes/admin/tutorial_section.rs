//! `AdminTutorialSectionRestService` — `WithReadRestServiceTrait<TutorialSection>`
//! (read-only CRUD: `GET ''` / `GET '{id}'`) plus the bespoke entry/symbol
//! endpoints (`availableHtmlSymbols`, `entries` GET/POST/PUT, `entries/{id}`
//! DELETE).

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::TutorialBo;
use owge_business::dto::{
    TutorialSectionAvailableHtmlSymbolDto, TutorialSectionDto, TutorialSectionEntryDto,
    TutorialSectionEntryInput,
};

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/tutorial_section", get(find_all))
        .route(
            "/admin/tutorial_section/availableHtmlSymbols",
            get(find_html_symbols),
        )
        .route(
            "/admin/tutorial_section/entries",
            get(find_entries)
                .post(add_update_entry)
                .put(add_update_entry),
        )
        .route(
            "/admin/tutorial_section/entries/{entryId}",
            axum::routing::delete(delete_entry),
        )
        .route("/admin/tutorial_section/{id}", get(find_one))
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<TutorialSectionDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(TutorialBo::find_all_sections(&mut conn).await?))
}

async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(id): Path<u16>,
) -> ApiResult<Json<TutorialSectionDto>> {
    let mut conn = state.db.acquire().await?;
    TutorialBo::find_section_by_id(&mut conn, id)
        .await?
        .map(Json)
        .ok_or_else(|| {
            ApiError(owge_business::OwgeError::NotFound(format!(
                "No tutorial section {id}"
            )))
        })
}

async fn find_html_symbols(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<TutorialSectionAvailableHtmlSymbolDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(
        TutorialBo::find_available_html_symbols(&mut conn).await?,
    ))
}

async fn find_entries(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<TutorialSectionEntryDto>>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(TutorialBo::find_entries(&mut conn).await?))
}

async fn add_update_entry(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<TutorialSectionEntryInput>,
) -> ApiResult<Json<TutorialSectionEntryDto>> {
    let mut conn = state.db.acquire().await?;
    Ok(Json(TutorialBo::add_update_entry(&mut conn, &input).await?))
}

async fn delete_entry(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(entry_id): Path<u32>,
) -> ApiResult<StatusCode> {
    let mut conn = state.db.acquire().await?;
    TutorialBo::delete_entry(&mut conn, entry_id).await?;
    Ok(StatusCode::NO_CONTENT)
}
