//! OWGE core game engine — Rust port of the Java `business` Spring Boot library.
//!
//! Mirrors the layering of the Java engine: entities (`model`), data access +
//! game logic (`bo`, the "business objects"), authentication primitives
//! (`jwt`), and shared infrastructure (`config`, `db`, `error`).
//!
//! The web concerns (routing, middleware, websocket wiring) live in the
//! sibling `owge-rest` binary crate, exactly as `game-rest` depends on the
//! `owgejava-backend` jar.

pub mod bo;
pub mod builder;
pub mod config;
pub mod db;
pub mod dto;
pub mod error;
pub mod jwt;
pub mod lock;
pub mod model;
pub mod pojo;
pub mod websocket;

pub use error::{OwgeError, OwgeResult};
