//! Websocket wire envelopes — port of `pojo/WebsocketMessage` and
//! `dto/WebsocketEventsInformationDto`.
//!
//! `WebsocketMessage<T>` is what the server pushes on the `deliver_message`
//! (and `authentication`) socket.io events; the frontend reads `eventName`,
//! `status` and `value`, and persists `lastSent` as the per-event watermark.
//!
//! **`lastSent` format.** Java serialises an `Instant` via netty-socketio's
//! `JacksonJsonSupport` as a numeric timestamp on the socket path; with the
//! second-precision `last_sent` DATETIME column this is whole **epoch seconds**
//! (verified against dc13: `1780603500`). The Rust socket path matches that
//! (`timestamp()`). The separate HTTP `websocket-sync` endpoint keeps its own
//! M1 format (epoch millis); the frontend treats `lastSent` as an opaque
//! equality token and re-baselines on each sync, so the per-path difference is
//! harmless (Java itself differs between its socket and HTTP paths).

use serde::Serialize;
use serde_json::Value;

/// `WebsocketMessage<T>` — the realtime envelope. `status` is always `"ok"`
/// (or `"error"` on the auth-failure path). `lastSent` is epoch seconds.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WebsocketMessage {
    pub event_name: String,
    /// Java's websocket mapper is NON_NULL, so a null payload drops the key
    /// entirely (e.g. `running_upgrade_change` with `() -> null` on completion
    /// / cancel) — mirror that instead of sending `"value":null`.
    #[serde(skip_serializing_if = "Value::is_null")]
    pub value: Value,
    pub status: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_sent: Option<i64>,
}

impl WebsocketMessage {
    /// `new WebsocketMessage<>(eventName, value)` — status "ok", no lastSent.
    pub fn ok(event_name: impl Into<String>, value: Value) -> Self {
        Self {
            event_name: event_name.into(),
            value,
            status: "ok".into(),
            last_sent: None,
        }
    }

    /// `new WebsocketMessage<>(information, value)` — carries the watermark's
    /// `lastSent` so the client can update its event-information cache.
    pub fn with_last_sent(
        event_name: impl Into<String>,
        value: Value,
        last_sent_secs: i64,
    ) -> Self {
        Self {
            event_name: event_name.into(),
            value,
            status: "ok".into(),
            last_sent: Some(last_sent_secs),
        }
    }

    /// `new WebsocketMessage<>(event, text, "error")`.
    pub fn error(event_name: impl Into<String>, text: impl Into<String>) -> Self {
        Self {
            event_name: event_name.into(),
            value: Value::String(text.into()),
            status: "error".into(),
            last_sent: None,
        }
    }
}

/// `WebsocketEventsInformationDto` — one per `(eventName, userId)` watermark,
/// returned as a list inside the `authentication` reply so the client can
/// decide which events changed since it last synced.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct WebsocketEventsInformationDto {
    pub event_name: String,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub user_id: Option<i32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub last_sent: Option<i64>,
}
