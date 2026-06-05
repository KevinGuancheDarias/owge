//! `DebugRestService` (`admin/debug`). A trivial health/probe endpoint that
//! returns the string `"OK"` — used by the admin frontend to verify the admin
//! token works.

use axum::routing::get;
use axum::Router;

use crate::auth::AdminUser;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new().route("/admin/debug", get(say_ok))
}

/// Java returns a bare `String`, which Spring serializes as `text/plain` body
/// `OK` (no JSON quoting); axum does the same for a `&'static str` return.
async fn say_ok(_admin: AdminUser) -> &'static str {
    "OK"
}
