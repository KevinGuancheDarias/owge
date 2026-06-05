//! HTTP route wiring. Mirrors the `rest/{open,game,admin}` controller packages.
//! Controllers stay thin — every handler delegates to a `*Bo` in
//! `owge-business`.

pub mod admin;
pub mod game;
pub mod open;

use axum::Router;

use crate::state::AppState;

/// Assemble the full application router. New controller groups are mounted here
/// as their milestones land (M1 game read endpoints, M2 admin CRUD, ...).
pub fn router(state: AppState) -> Router {
    Router::new()
        .merge(open::routes())
        .merge(game::routes())
        .merge(admin::routes())
        .with_state(state)
}
