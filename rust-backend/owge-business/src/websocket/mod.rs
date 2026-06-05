//! Websocket realtime support.
//!
//! * [`sync`] — the **read side** of the sync protocol (`game/websocket-sync`),
//!   which lets the frontend hydrate its entire offline cache over HTTP (M1).
//! * [`emitter`] — the **push side** (M4): the process-global emit pipeline
//!   (`SocketIoService.sendMessage`) that bumps the per-user watermark and
//!   pushes `deliver_message` deltas through the socket.io server (owned by
//!   `owge-rest`).

pub mod emitter;
pub mod sync;

pub use sync::find_wanted_data;
