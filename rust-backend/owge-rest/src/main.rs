//! OWGE REST — runnable web app (Rust port of `game-rest`).
//!
//! Boots the DB pool + JWT material, builds the axum router, applies CORS, and
//! serves. The socket.io realtime server (milestone M4) will be layered onto
//! this same listener via `socketioxide`.

mod auth;
mod http_error;
mod routes;
mod socketio;
mod state;

use std::sync::Arc;
use std::time::Duration;

use axum::http::{HeaderName, Method};
use owge_business::bo::{MissionRunner, MissionSchedulerService};
use owge_business::config::EnvConfig;
use tower_http::cors::{Any, CorsLayer};
use tower_http::trace::TraceLayer;
use tracing_subscriber::EnvFilter;

use crate::state::AppState;

#[tokio::main]
async fn main() {
    tracing_subscriber::fmt()
        .with_env_filter(EnvFilter::try_from_default_env().unwrap_or_else(|_| "info".into()))
        .init();

    if let Err(e) = run().await {
        tracing::error!("fatal: {e}");
        std::process::exit(1);
    }
}

async fn run() -> owge_business::OwgeResult<()> {
    let env = EnvConfig::from_env()?;
    let bind = format!("{}:{}", env.server_host, env.server_port);
    let cors = build_cors(&env);

    let state = AppState::bootstrap(env).await?;
    tracing::info!(
        game_method = ?state.game_token.method,
        "OWGE backend booted; admin tokens use {:?}",
        state.admin_jwt.algorithm
    );

    // Mission scheduler poller — the db-scheduler analogue that fires due missions
    // (`DbSchedulerRealizationJob` -> `MissionRunner`). Started after bootstrap and
    // kept alive for the process lifetime.
    let scheduler = MissionSchedulerService::new(state.db.clone());
    let runner = Arc::new(MissionRunner::new(state.db.clone()));
    let worker_id = format!("owge-rust-{}", state.env.server_port);
    let _poller = scheduler.spawn_poller(runner, worker_id);

    // Time-special lifecycle poller — the analogue of Java's Quartz
    // TIME_SPECIAL_EFFECT_END / TIME_SPECIAL_IS_READY jobs, over the same
    // `scheduled_tasks` table (so an activated special auto-expires + recharges).
    let _ts_poller = owge_business::bo::ActiveTimeSpecialBo::spawn_effect_poller(
        state.db.clone(),
        format!("owge-rust-ts-{}", state.env.server_port),
    );

    // Socket.io realtime server on its own listener (OWGE_WS_PORT). Build the
    // tower layer + emitter, register the emitter globally so the business layer
    // can push `deliver_message` deltas, then serve the engine.io endpoint.
    let ws_bind = format!("{}:{}", state.env.ws_host, state.env.ws_port);
    let ws_cors = build_cors(&state.env);
    let (ws_layer, ws_emitter) = socketio::build(state.clone());
    owge_business::websocket::emitter::set_emitter(ws_emitter);
    let ws_app = axum::Router::new().layer(ws_layer).layer(ws_cors);
    let ws_listener = tokio::net::TcpListener::bind(&ws_bind)
        .await
        .map_err(|e| owge_business::OwgeError::Common(format!("ws bind {ws_bind} failed: {e}")))?;
    tracing::info!("socket.io listening on ws://{ws_bind} (path /socket.io)");
    tokio::spawn(async move {
        if let Err(e) = axum::serve(ws_listener, ws_app).await {
            tracing::error!("socket.io server error: {e}");
        }
    });

    // Serve every REST route under OWGE_CONTEXT_PATH (e.g. `/game_api`) when set,
    // so the same nginx reverse proxy that fronts the Java backend (which keeps
    // the `/game_api` prefix) can front this one unchanged. Empty => serve at root.
    let context_path = state.env.context_path.clone();
    let routed = routes::router(state);
    let routed = if context_path.is_empty() {
        routed
    } else {
        tracing::info!("serving REST under context path {context_path}");
        axum::Router::new().nest(&context_path, routed)
    };
    let app = routed
        .layer(cors)
        .layer(TraceLayer::new_for_http());

    let listener = tokio::net::TcpListener::bind(&bind)
        .await
        .map_err(|e| owge_business::OwgeError::Common(format!("bind {bind} failed: {e}")))?;
    tracing::info!("listening on http://{bind}");

    axum::serve(listener, app)
        .await
        .map_err(|e| owge_business::OwgeError::Common(format!("server error: {e}")))?;
    Ok(())
}

/// CORS roughly matching the Java `CorsFilterConfiguration` — permissive in dev,
/// origin-restricted when `OWGE_CORS_ORIGINS` is set.
fn build_cors(env: &EnvConfig) -> CorsLayer {
    let layer = CorsLayer::new()
        .allow_methods([
            Method::GET,
            Method::POST,
            Method::PUT,
            Method::DELETE,
            Method::OPTIONS,
        ])
        .allow_headers([
            HeaderName::from_static("authorization"),
            HeaderName::from_static("content-type"),
        ])
        .max_age(Duration::from_secs(3600));

    if env.cors_origins.iter().any(|o| o == "*") {
        layer.allow_origin(Any)
    } else {
        let origins = env
            .cors_origins
            .iter()
            .filter_map(|o| o.parse().ok())
            .collect::<Vec<_>>();
        layer.allow_origin(origins)
    }
}
