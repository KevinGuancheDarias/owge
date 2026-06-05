//! `AdminConfigurationRestService` — bespoke (not a `CrudRestServiceTrait`).
//! Exposes the non-privileged engine settings to the admin UI.
//!
//! Routes mirror the Java `@RequestMapping("admin/configuration")`:
//! - `GET    admin/configuration`        -> findAll (non-privileged)
//! - `GET    admin/configuration/{name}` -> findOne (null when privileged)
//! - `POST   admin/configuration`        -> saveNew (409 when name in use)
//! - `PUT    admin/configuration/{name}` -> saveExisting
//! - `DELETE admin/configuration/{name}` -> delete (void / 200)

use axum::extract::{Path, State};
use axum::http::StatusCode;
use axum::routing::get;
use axum::{Json, Router};
use owge_business::bo::ConfigurationBo;
use owge_business::dto::{ConfigurationDto, ConfigurationInput};
use owge_business::OwgeError;

use crate::auth::AdminUser;
use crate::http_error::{ApiError, ApiResult};
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/admin/configuration", get(find_all).post(save_new))
        .route(
            "/admin/configuration/{name}",
            get(find_one).put(save_existing).delete(delete_one),
        )
}

async fn find_all(
    State(state): State<AppState>,
    _admin: AdminUser,
) -> ApiResult<Json<Vec<ConfigurationDto>>> {
    let rows = ConfigurationBo::find_all_non_privileged(&state.db).await?;
    Ok(Json(rows.into_iter().map(Into::into).collect()))
}

/// `findOne` returns `null` (JSON null) when the param is privileged, and 404
/// (`SgtBackendConfigurationNotFoundException`) when it does not exist at all.
async fn find_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(name): Path<String>,
) -> ApiResult<Json<Option<ConfigurationDto>>> {
    let configuration = ConfigurationBo::find(&state.db, &name).await?;
    if configuration.privileged != 0 {
        Ok(Json(None))
    } else {
        Ok(Json(Some(configuration.into())))
    }
}

async fn save_new(
    State(state): State<AppState>,
    _admin: AdminUser,
    Json(input): Json<ConfigurationInput>,
) -> ApiResult<Json<ConfigurationDto>> {
    if ConfigurationBo::find_opt(&state.db, &input.name).await?.is_some() {
        return Err(ApiError(OwgeError::InvalidInput("Key in use".into())));
    }
    let saved = ConfigurationBo::save(
        &state.db,
        &input.name,
        input.display_name.as_deref(),
        &input.value,
    )
    .await?;
    Ok(Json(saved.into()))
}

async fn save_existing(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(name): Path<String>,
    Json(input): Json<ConfigurationInput>,
) -> ApiResult<Json<ConfigurationDto>> {
    let existing = ConfigurationBo::find_opt(&state.db, &input.name).await?;
    match existing {
        None => Err(ApiError(OwgeError::NotFound(format!(
            "No configuration with name {}",
            input.name
        )))),
        Some(configuration) if configuration.privileged != 0 => Err(ApiError(OwgeError::NotFound(
            format!("No configuration with name {}", input.name),
        ))),
        Some(_) if name != input.name => Err(ApiError(OwgeError::InvalidInput(
            "Id field of the body and id of the path param, must match".into(),
        ))),
        Some(_) => {
            let saved = ConfigurationBo::save(
                &state.db,
                &input.name,
                input.display_name.as_deref(),
                &input.value,
            )
            .await?;
            Ok(Json(saved.into()))
        }
    }
}

/// The Java handler returns `void` (HTTP 200, empty body).
async fn delete_one(
    State(state): State<AppState>,
    _admin: AdminUser,
    Path(name): Path<String>,
) -> ApiResult<StatusCode> {
    ConfigurationBo::delete_one(&state.db, &name).await?;
    Ok(StatusCode::OK)
}
