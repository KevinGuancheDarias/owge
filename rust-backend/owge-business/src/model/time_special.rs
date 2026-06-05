//! Persistence entities for the time-special domain — the Rust counterpart of
//! the Java JPA entities
//! `com.kevinguanchedarias.owgejava.entity.TimeSpecial` and
//! `...entity.ActiveTimeSpecial`.
//!
//! Field types mirror the exact MySQL column types in
//! `business/database/02_schema.sql` (`time_specials`, `active_time_specials`),
//! so sqlx never panics on signedness/width.

use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};

/// Mirrors the `time_specials` table / Java `TimeSpecial` entity
/// (extends `CommonEntityWithImageStore`).
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct TimeSpecial {
    /// `smallint unsigned`.
    pub id: u16,
    pub name: String,
    pub description: Option<String>,
    /// `bigint unsigned`, nullable — FK to `images_store`.
    #[sqlx(rename = "image_id")]
    pub image_id: Option<u64>,
    /// `bigint unsigned` — duration in seconds of the time special.
    pub duration: u64,
    /// `bigint unsigned` — time (seconds) to wait before reuse.
    #[sqlx(rename = "recharge_time")]
    pub recharge_time: u64,
    /// `smallint unsigned`, nullable — FK to `improvements`.
    #[sqlx(rename = "improvement_id")]
    pub improvement_id: Option<u16>,
    /// `tinyint(1)` — read as `i8`, maps to bool.
    #[sqlx(rename = "cloned_improvements")]
    pub cloned_improvements: i8,
}

/// Mirrors the `active_time_specials` table / Java `ActiveTimeSpecial` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct ActiveTimeSpecial {
    /// `bigint unsigned`.
    pub id: u64,
    /// `int` (signed) — FK to `user_storage`.
    #[sqlx(rename = "user_id")]
    pub user_id: i32,
    /// `smallint unsigned` — FK to `time_specials`.
    #[sqlx(rename = "time_special_id")]
    pub time_special_id: u16,
    /// `enum('ACTIVE','RECHARGE')` — read as the raw string.
    pub state: String,
    /// `datetime`.
    #[sqlx(rename = "activation_date")]
    pub activation_date: NaiveDateTime,
    /// `datetime`.
    #[sqlx(rename = "expiring_date")]
    pub expiring_date: NaiveDateTime,
    /// `datetime`, nullable — set once the special enters the RECHARGE state.
    #[sqlx(rename = "ready_date")]
    pub ready_date: Option<NaiveDateTime>,
}
