//! Realtime emit pipeline — the Rust port of `SocketIoService.sendMessage` and
//! the singleton socket registry it pushes through.
//!
//! Java's `SocketIoService` is a Spring singleton that holds the netty-socketio
//! server and is autowired into every `*EventEmitter`/`*Bo`. The Rust `*Bo`s are
//! stateless free functions over a pinned `&mut MySqlConnection`, so threading a
//! socket handle through every signature would be hugely invasive. Instead,
//! exactly as Spring keeps one bean, we keep one process-global
//! [`WebsocketEmitter`] (set once at startup by `owge-rest`, which owns the
//! socketioxide server). The business layer stays framework-agnostic behind the
//! trait.
//!
//! ## Semantics mirrored from `sendMessage`
//! * The `websocket_events_information` **watermark is persisted first, for
//!   every recipient, even offline ones** (broadcast = target `0` iterates all
//!   `user_storage` rows). This is what drives the frontend's delta-sync.
//! * The message *value* is computed **lazily and only if at least one of the
//!   recipients has a live socket** (Java's `Supplier`), then delivered on the
//!   `deliver_message` event.
//! * `sendOneTimeMessage` skips the watermark entirely.
//!
//! Emits must run **after** the surrounding DB transaction commits (Java uses
//! `transactionUtilService.doAfterCommit`): the finders here read through the
//! connection, which cannot see uncommitted writes from another transaction. Call
//! sites place the emit after `tx.commit()`.

use std::sync::{Arc, OnceLock};

use async_trait::async_trait;
use chrono::Utc;
use serde_json::Value;
use sqlx::MySqlConnection;

use crate::bo::WebsocketEventsInformationBo;
use crate::dto::websocket::WebsocketMessage;
use crate::error::OwgeResult;

/// Abstraction over the live socket.io server, implemented by `owge-rest` on
/// top of socketioxide. The business layer talks only to this trait.
#[async_trait]
pub trait WebsocketEmitter: Send + Sync {
    /// Is there at least one authenticated socket for this user id?
    fn is_user_connected(&self, user_id: i64) -> bool;

    /// Deliver an already-rendered `WebsocketMessage` JSON to every socket
    /// belonging to `user_id`, on the `deliver_message` event.
    async fn deliver(&self, user_id: i64, message: Value);

    /// Emit a raw socket.io event (the named `event`, with `payload`) to **all**
    /// connected clients. Used by `cache_clear`.
    async fn broadcast_raw(&self, event: &str, payload: Value);
}

static EMITTER: OnceLock<Arc<dyn WebsocketEmitter>> = OnceLock::new();

/// Register the process-global emitter (called once from `owge-rest::main`).
pub fn set_emitter(emitter: Arc<dyn WebsocketEmitter>) {
    let _ = EMITTER.set(emitter);
}

/// The registered emitter, if the socket.io server is up. `None` in unit tests
/// and before startup wiring — in which case watermarks are still persisted but
/// no live push happens (equivalent to "nobody connected").
pub fn emitter() -> Option<Arc<dyn WebsocketEmitter>> {
    EMITTER.get().cloned()
}

/// `sendMessage(targetUserId, eventName, supplier)` — persist the watermark for
/// every recipient, then push the lazily-computed value to connected sockets.
///
/// `target_user_id == 0` broadcasts: the watermark is bumped for **all** users
/// and the value is delivered to every connected client.
///
/// The connection is threaded into `value_fn` via an HRTB closure so the same
/// pinned `&mut MySqlConnection` can be reused for both the watermark writes
/// and the payload query — no second borrow or pool acquire needed.
pub async fn send_message<F>(
    conn: &mut MySqlConnection,
    target_user_id: i32,
    event_name: &str,
    value_fn: F,
) -> OwgeResult<()>
where
    F: for<'c> FnOnce(
        &'c mut MySqlConnection,
    ) -> std::pin::Pin<
        Box<dyn std::future::Future<Output = OwgeResult<Value>> + Send + 'c>,
    >,
{
    let now = Utc::now();
    let now_naive = now.naive_utc();
    // Epoch SECONDS on the socket path (matches Java's JavaTimeModule numeric
    // Instant + second-precision DATETIME column). The HTTP `websocket-sync`
    // endpoint keeps its own M1 format; the frontend treats lastSent as an
    // opaque equality token and re-baselines on each sync.
    let last_sent_secs = now.timestamp();

    // 1) Persist watermark(s) first — always, regardless of connectivity.
    let recipients: Vec<i32> = if target_user_id == 0 {
        let ids: Vec<(i32,)> = sqlx::query_as("SELECT id FROM user_storage")
            .fetch_all(&mut *conn)
            .await?;
        let ids: Vec<i32> = ids.into_iter().map(|(id,)| id).collect();
        for id in &ids {
            WebsocketEventsInformationBo::save(&mut *conn, event_name, *id, now_naive).await?;
        }
        ids
    } else {
        WebsocketEventsInformationBo::save(&mut *conn, event_name, target_user_id, now_naive)
            .await?;
        vec![target_user_id]
    };

    // 2) Deliver only if someone is connected (Supplier is evaluated lazily).
    let Some(em) = emitter() else {
        return Ok(());
    };
    let connected: Vec<i32> = recipients
        .into_iter()
        .filter(|id| em.is_user_connected(*id as i64))
        .collect();
    if connected.is_empty() {
        return Ok(());
    }
    let value = value_fn(&mut *conn).await?;
    let message = serde_json::to_value(WebsocketMessage::with_last_sent(
        event_name,
        value,
        last_sent_secs,
    ))?;
    for id in connected {
        em.deliver(id as i64, message.clone()).await;
    }
    Ok(())
}

/// `sendOneTimeMessage` — deliver without touching the watermark table. Skips
/// the value computation when the user is offline.
pub async fn send_one_time_message(
    target_user_id: i32,
    event_name: &str,
    value: Value,
) -> OwgeResult<()> {
    let Some(em) = emitter() else {
        return Ok(());
    };
    if !em.is_user_connected(target_user_id as i64) {
        return Ok(());
    }
    let message = serde_json::to_value(WebsocketMessage::ok(event_name, value))?;
    em.deliver(target_user_id as i64, message).await;
    Ok(())
}

/// `clearCache` push side — `server.getAllClients().forEach(sendEvent("cache_clear","null"))`.
/// (The watermark-table truncation is the caller's responsibility.)
pub async fn broadcast_cache_clear() -> OwgeResult<()> {
    if let Some(em) = emitter() {
        em.broadcast_raw("cache_clear", Value::String("null".into()))
            .await;
    }
    Ok(())
}
