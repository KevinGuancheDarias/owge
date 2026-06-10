//! Maps [`OwgeError`] to HTTP responses shaped like the Java backend's JSON
//! error bodies (`BackendErrorPojo { exceptionType, message }`), so existing
//! frontend error handling keeps working unchanged.

use axum::Json;
use axum::http::StatusCode;
use axum::response::{IntoResponse, Response};
use owge_business::OwgeError;
use serde_json::json;

/// Newtype so we can implement `IntoResponse` for the business error without
/// orphan-rule issues, and use `?` in handlers via `From`.
pub struct ApiError(pub OwgeError);

impl From<OwgeError> for ApiError {
    fn from(e: OwgeError) -> Self {
        ApiError(e)
    }
}

/// Lets handlers use `?` directly on raw sqlx results — most importantly the
/// per-request `state.db.acquire()` at the top of every DB-touching handler.
impl From<sqlx::Error> for ApiError {
    fn from(e: sqlx::Error) -> Self {
        ApiError(OwgeError::from(e))
    }
}

impl IntoResponse for ApiError {
    fn into_response(self) -> Response {
        let status = match &self.0 {
            OwgeError::NotFound(_) => StatusCode::NOT_FOUND,
            OwgeError::AccessDenied(_) => StatusCode::FORBIDDEN,
            OwgeError::Unauthorized(_) | OwgeError::Jwt(_) => StatusCode::UNAUTHORIZED,
            OwgeError::InvalidInput(_) => StatusCode::BAD_REQUEST,
            OwgeError::Conflict(_) => StatusCode::CONFLICT,
            OwgeError::Common(_) | OwgeError::Database(_) | OwgeError::Serialization(_) => {
                StatusCode::INTERNAL_SERVER_ERROR
            }
        };
        if status == StatusCode::INTERNAL_SERVER_ERROR {
            tracing::error!(error = %self.0, "request failed");
        }
        let body = match &self.0 {
            OwgeError::NotFound(_) => not_found_body(&self.0.to_string()),
            _ => json!({
                "exceptionType": self.0.exception_type(),
                "message": self.0.to_string(),
            }),
        };
        (status, Json(body)).into_response()
    }
}

/// Builds the JSON body for a 404, mirroring Java's `NotFoundException` wire
/// shape as observed on the live backend (v0.11.10):
///
/// ```json
/// {"message":"I18N_ERR_GENERIC_ITEM_NOT_FOUND","exceptionType":"NotFoundException",
///  "extra":{"affectedItem":{"id":999999}}}
/// ```
///
/// The Rust `OwgeError::NotFound` carries an ad-hoc string. Java's `findByIdOrDie`
/// always throws with the generic i18n key + an `affectedItem.id`, while a few
/// sites throw with a real i18n key and no affected item. We reproduce that
/// observable contract centrally so the frontend (which translates `message`)
/// keeps working, without touching ~86 call sites:
///
/// * a message that is already an i18n key (`^[A-Z0-9_]+$`) is emitted verbatim
///   with an empty `extra` (matches `new NotFoundException("SOME_KEY")`);
/// * otherwise (a developer-style "No X with id N" message) we emit the generic
///   key and lift the embedded id into `extra.affectedItem.id`
///   (matches `NotFoundException.fromAffected(type, id)`).
///
/// `developerHint`/`reporterAsString` are intentionally omitted: the frontend
/// never reads them, and `developerHint` embeds the deployed version so it can
/// never be universally bit-for-bit.
fn not_found_body(message: &str) -> serde_json::Value {
    let is_i18n_key = !message.is_empty()
        && message
            .bytes()
            .all(|b| b.is_ascii_uppercase() || b.is_ascii_digit() || b == b'_');
    if is_i18n_key {
        return json!({
            "message": message,
            "exceptionType": "NotFoundException",
            "extra": {},
        });
    }
    // Developer-style message: normalise to the generic key and recover the id.
    match extract_affected_id(message) {
        Some(id) => json!({
            "message": "I18N_ERR_GENERIC_ITEM_NOT_FOUND",
            "exceptionType": "NotFoundException",
            "extra": { "affectedItem": { "id": id } },
        }),
        None => json!({
            "message": "I18N_ERR_GENERIC_ITEM_NOT_FOUND",
            "exceptionType": "NotFoundException",
            "extra": {},
        }),
    }
}

/// Recovers the affected id embedded in a developer-style not-found message
/// (e.g. `"No user with id 5"`). Prefers the integer following `"id "`, falling
/// back to the first standalone integer in the string.
fn extract_affected_id(message: &str) -> Option<i64> {
    let parse_at = |rest: &str| {
        let digits: String = rest.chars().take_while(|c| c.is_ascii_digit()).collect();
        digits.parse::<i64>().ok()
    };
    if let Some(pos) = message.find("id ") {
        if let Some(id) = parse_at(&message[pos + 3..]) {
            return Some(id);
        }
    }
    // Fallback: first run of digits anywhere in the message.
    for (i, c) in message.char_indices() {
        if c.is_ascii_digit() {
            return parse_at(&message[i..]);
        }
    }
    None
}

/// Convenience alias for handler signatures.
pub type ApiResult<T> = Result<T, ApiError>;
