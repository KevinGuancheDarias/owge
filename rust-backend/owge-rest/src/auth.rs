//! Authentication extractors — the Rust equivalent of the Spring Security
//! `BootJwtAuthenticationFilter` chains. Instead of a servlet filter mutating a
//! `SecurityContext`, each protected handler asks for a [`GameUser`] or
//! [`AdminUser`] extractor, which verifies the `Authorization: Bearer <jwt>`
//! header against the matching scope's [`TokenConfig`].

use axum::extract::FromRequestParts;
use axum::http::request::Parts;
use owge_business::bo::UserStorageBo;
use owge_business::jwt::{self, TokenConfig, TokenUser};
use owge_business::OwgeError;

use crate::http_error::ApiError;
use crate::state::AppState;

/// A player authenticated via a `/game/**` token.
#[derive(Debug, Clone)]
pub struct GameUser(pub TokenUser);

/// An admin authenticated via an `/admin/**` token.
#[derive(Debug, Clone)]
pub struct AdminUser(pub TokenUser);

fn bearer_token(parts: &Parts) -> Result<&str, OwgeError> {
    let header = parts
        .headers
        .get(axum::http::header::AUTHORIZATION)
        .ok_or_else(|| {
            OwgeError::Unauthorized("HTTP Authorization header not found, or it's invalid".into())
        })?
        .to_str()
        .map_err(|_| OwgeError::Unauthorized("Malformed Authorization header".into()))?;
    header
        .strip_prefix("Bearer ")
        .ok_or_else(|| {
            OwgeError::Unauthorized("HTTP Authorization header not found, or it's invalid".into())
        })
}

fn authenticate(parts: &Parts, config: &TokenConfig) -> Result<TokenUser, ApiError> {
    let token = bearer_token(parts).map_err(ApiError)?;
    jwt::decode_token(config, token).map_err(ApiError)
}

impl FromRequestParts<AppState> for GameUser {
    type Rejection = ApiError;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        let user = authenticate(parts, &state.game_token)?;
        // `ResourceAutoUpdateEventHandler.doAfter`: after auth, for a subscribed
        // (existing) player, reject if banned then accrue passive resources for the
        // time elapsed since their last request. Skipped for accounts not yet
        // subscribed to this universe (no `user_storage` row).
        let user_id = user.id as i32;
        if UserStorageBo::exists(&state.db, user_id)
            .await
            .map_err(ApiError)?
        {
            if UserStorageBo::is_banned(&state.db, user_id)
                .await
                .map_err(ApiError)?
            {
                return Err(ApiError(OwgeError::AccessDenied("I18N_ERR_BANNED".into())));
            }
            UserStorageBo::trigger_resources_update(&state.db, user_id)
                .await
                .map_err(ApiError)?;
        }
        Ok(GameUser(user))
    }
}

impl FromRequestParts<AppState> for AdminUser {
    type Rejection = ApiError;

    async fn from_request_parts(
        parts: &mut Parts,
        state: &AppState,
    ) -> Result<Self, Self::Rejection> {
        Ok(AdminUser(authenticate(parts, &state.admin_token)?))
    }
}
