//! Domain error type mirroring the OWGE Java exception hierarchy
//! (`NotFoundException`, `AccessDeniedException`,
//! `UserLoginException`, `CommonException`, JWT/auth exceptions).
//!
//! The `owge-rest` crate maps these to HTTP responses shaped like the Java
//! backend's JSON error bodies.

use thiserror::Error;

pub type OwgeResult<T> = Result<T, OwgeError>;

#[derive(Debug, Error)]
pub enum OwgeError {
    /// Maps to HTTP 404 — `NotFoundException`.
    #[error("{0}")]
    NotFound(String),

    /// Maps to HTTP 403 — `AccessDeniedException` / auth failures.
    #[error("{0}")]
    AccessDenied(String),

    /// Maps to HTTP 401 — invalid/expired token, missing Authorization header.
    #[error("{0}")]
    Unauthorized(String),

    /// Maps to HTTP 400 — `SgtBackendInvalidInputException`.
    #[error("{0}")]
    InvalidInput(String),

    /// Maps to HTTP 409 — uniqueness / state conflicts.
    #[error("{0}")]
    Conflict(String),

    /// Maps to HTTP 500 — `CommonException` and unexpected failures.
    #[error("{0}")]
    Common(String),

    #[error(transparent)]
    Database(#[from] sqlx::Error),

    #[error(transparent)]
    Jwt(#[from] jsonwebtoken::errors::Error),

    #[error(transparent)]
    Serialization(#[from] serde_json::Error),
}

impl OwgeError {
    /// The `exceptionType` field the Java backend reports in its JSON error
    /// bodies (`BackendErrorPojo.exceptionType`). Kept compatible so existing
    /// frontend error handling continues to match.
    pub fn exception_type(&self) -> &'static str {
        match self {
            OwgeError::NotFound(_) => "NotFoundException",
            OwgeError::AccessDenied(_) => "AccessDeniedException",
            OwgeError::Unauthorized(_) => "InvalidAuthorizationHeader",
            OwgeError::InvalidInput(_) => "SgtBackendInvalidInputException",
            OwgeError::Conflict(_) => "SgtBackendUniqueException",
            OwgeError::Common(_) => "CommonException",
            OwgeError::Database(_) => "CommonException",
            OwgeError::Serialization(_) => "CommonException",
            OwgeError::Jwt(_) => "InvalidAuthorizationHeader",
        }
    }
}
