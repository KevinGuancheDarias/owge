//! Socket.io realtime server — the Rust port of `SocketIoService` (the netty
//! socket.io server) and `WebsocketConfiguration`.
//!
//! Served on its own listener (`OWGE_WS_PORT`, default 7474) at the engine.io
//! path `/socket.io`, exactly like the Java `SocketIOServer`. In production
//! nginx proxies `location /websocket/` to this port; the frontend connects
//! with `socket.io-client@2.x` (socket.io v4 protocol / engine.io v3), which is
//! why socketioxide is built with the `v4` feature.
//!
//! Protocol mirrored from `SocketIoService`:
//! * client emits `authentication` with the JSON string `{"value": <jwt>, ...}`;
//! * the server validates the token (game JWT, falling back to admin JWT),
//!   binds the `TokenUser` to the socket, joins a per-user room, and replies on
//!   the `authentication` event with a [`WebsocketMessage`] whose `value` is the
//!   list of `websocket_events_information` watermarks plus a synthetic
//!   `_universe_id:<n>` entry;
//! * thereafter the server pushes state deltas on `deliver_message` (see
//!   `owge_business::websocket::emitter`).

use std::collections::{HashMap, HashSet};
use std::sync::{Arc, Mutex};

use async_trait::async_trait;
use owge_business::bo::{ConfigurationBo, WebsocketEventsInformationBo};
use owge_business::dto::websocket::{WebsocketEventsInformationDto, WebsocketMessage};
use owge_business::jwt::{TokenUser, decode_token};
use owge_business::websocket::emitter::WebsocketEmitter;
use serde::Deserialize;
use serde_json::Value;
use socketioxide::SocketIo;
use socketioxide::extract::{Data, SocketRef, State};
use socketioxide::socket::Sid;

use crate::state::AppState;

const AUTHENTICATION: &str = "authentication";
const DELIVER_MESSAGE: &str = "deliver_message";

fn user_room(user_id: i64) -> String {
    format!("user:{user_id}")
}

/// Tracks which authenticated user each live socket belongs to, so we can
/// answer "is this user connected?" and clean up on disconnect. Mirrors the
/// `client.get(USER_TOKEN_KEY)` bookkeeping netty-socketio does internally.
#[derive(Clone, Default)]
pub struct WsRegistry(Arc<Mutex<RegInner>>);

#[derive(Default)]
struct RegInner {
    by_user: HashMap<i64, HashSet<Sid>>,
    by_sid: HashMap<Sid, i64>,
}

impl WsRegistry {
    fn add(&self, sid: Sid, user_id: i64) {
        let mut g = self.0.lock().unwrap();
        g.by_user.entry(user_id).or_default().insert(sid);
        g.by_sid.insert(sid, user_id);
    }

    fn remove(&self, sid: Sid) {
        let mut g = self.0.lock().unwrap();
        if let Some(user_id) = g.by_sid.remove(&sid) {
            if let Some(set) = g.by_user.get_mut(&user_id) {
                set.remove(&sid);
                if set.is_empty() {
                    g.by_user.remove(&user_id);
                }
            }
        }
    }

    fn is_connected(&self, user_id: i64) -> bool {
        let g = self.0.lock().unwrap();
        g.by_user
            .get(&user_id)
            .map(|s| !s.is_empty())
            .unwrap_or(false)
    }
}

/// [`WebsocketEmitter`] implementation over the live socketioxide handle.
pub struct WsEmitter {
    io: SocketIo,
    registry: WsRegistry,
}

#[async_trait]
impl WebsocketEmitter for WsEmitter {
    fn is_user_connected(&self, user_id: i64) -> bool {
        self.registry.is_connected(user_id)
    }

    async fn deliver(&self, user_id: i64, message: Value) {
        let _ = self
            .io
            .to(user_room(user_id))
            .emit(DELIVER_MESSAGE, &message)
            .await;
    }

    async fn broadcast_raw(&self, event: &str, payload: Value) {
        let _ = self.io.emit(event.to_string(), &payload).await;
    }
}

/// Build the socket.io tower layer + the [`WsEmitter`] that the business layer
/// pushes through. The layer is mounted on its own axum router in `main`.
pub fn build(state: AppState) -> (socketioxide::layer::SocketIoLayer, Arc<WsEmitter>) {
    let registry = WsRegistry::default();
    let (layer, io) = SocketIo::builder()
        .with_state(state)
        .with_state(registry.clone())
        .build_layer();

    io.ns("/", on_connect);

    let emitter = Arc::new(WsEmitter { io, registry });
    (layer, emitter)
}

async fn on_connect(socket: SocketRef) {
    tracing::debug!("socket.io client connected: {}", socket.id);
    socket.on(AUTHENTICATION, on_authentication);
    socket.on_disconnect(on_disconnect);
}

async fn on_disconnect(socket: SocketRef, State(registry): State<WsRegistry>) {
    registry.remove(socket.id);
}

#[derive(Deserialize)]
struct AuthPayload {
    value: Option<String>,
}

async fn on_authentication(
    socket: SocketRef,
    Data(raw): Data<String>,
    State(state): State<AppState>,
    State(registry): State<WsRegistry>,
) {
    let token = serde_json::from_str::<AuthPayload>(&raw)
        .ok()
        .and_then(|p| p.value)
        .unwrap_or_default();

    if token.is_empty() {
        send_auth_error(&socket, "invalid token sent from client");
        return;
    }

    let Some(user) = authenticate_token(&state, &token) else {
        send_auth_error(&socket, "Invalid credentials");
        return;
    };

    registry.add(socket.id, user.id);
    let _ = socket.join(user_room(user.id));

    match build_events_info(&state, user.id as i32).await {
        Ok(events_info) => {
            let value = serde_json::to_value(events_info).unwrap_or(Value::Null);
            let _ = socket.emit(AUTHENTICATION, &WebsocketMessage::ok(AUTHENTICATION, value));
        }
        Err(e) => {
            tracing::error!("failed to build websocket events info: {e}");
            send_auth_error(&socket, "Invalid credentials");
        }
    }
}

/// Try the game token config first, then the admin token config, returning the
/// first that validates — the equivalent of iterating the `authenticationFilters`.
fn authenticate_token(state: &AppState, token: &str) -> Option<TokenUser> {
    decode_token(&state.game_token, token)
        .or_else(|_| decode_token(&state.admin_token, token))
        .ok()
}

/// `findByUserId -> toDto` plus the synthetic `_universe_id:<n>` entry.
async fn build_events_info(
    state: &AppState,
    user_id: i32,
) -> owge_business::OwgeResult<Vec<WebsocketEventsInformationDto>> {
    let mut conn = state.db.acquire().await?;
    let mut info = WebsocketEventsInformationBo::find_by_user_id(&mut conn, user_id).await?;
    let universe_id = ConfigurationBo::find_value(&mut conn, "UNIVERSE_ID")
        .await
        .unwrap_or_default();
    info.push(WebsocketEventsInformationDto {
        event_name: format!("_universe_id:{universe_id}"),
        user_id: None,
        last_sent: None,
    });
    Ok(info)
}

fn send_auth_error(socket: &SocketRef, text: &str) {
    tracing::warn!("{text}");
    let _ = socket.emit(
        AUTHENTICATION,
        &WebsocketMessage::error(AUTHENTICATION, text),
    );
    let _ = socket.clone().disconnect();
}
