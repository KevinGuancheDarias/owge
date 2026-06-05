//! Mirrors the `obtained_units` table / Java `ObtainedUnit` entity
//! (`com.kevinguanchedarias.owgejava.entity.ObtainedUnit`).
//!
//! An obtained unit is a stack of `count` instances of a `Unit` owned by a
//! user, optionally sitting on a planet (`source_planet`) and/or tied to a
//! running `mission`.

use serde::{Deserialize, Serialize};

#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct ObtainedUnit {
    /// `bigint unsigned`.
    pub id: u64,
    /// `int` (signed) — the owning user id.
    #[sqlx(rename = "user_id")]
    pub user_id: i32,
    /// `smallint unsigned`.
    #[sqlx(rename = "unit_id")]
    pub unit_id: u16,
    /// `bigint unsigned`.
    pub count: u64,
    /// `bigint unsigned`, nullable.
    #[sqlx(rename = "source_planet")]
    pub source_planet: Option<u64>,
    /// `bigint unsigned`, nullable.
    #[sqlx(rename = "target_planet")]
    pub target_planet: Option<u64>,
    /// `bigint unsigned`, nullable — the running mission this stack belongs to.
    #[sqlx(rename = "mission_id")]
    pub mission_id: Option<u64>,
    /// `bigint unsigned`, nullable.
    #[sqlx(rename = "first_deployment_mission")]
    pub first_deployment_mission: Option<u64>,
    /// `tinyint` (not the JPA boolean default — stored as a number).
    #[sqlx(rename = "is_from_capture")]
    pub is_from_capture: i8,
    /// `int unsigned`, nullable.
    #[sqlx(rename = "expiration_id")]
    pub expiration_id: Option<u32>,
    /// `bigint unsigned`, nullable — set when this stack is stored inside
    /// another obtained unit.
    #[sqlx(rename = "owner_unit_id")]
    pub owner_unit_id: Option<u64>,
}

/// Mirrors the `obtained_unit_temporal_information` table / Java
/// `ObtainedUnitTemporalInformation` (a Spring-Data-JDBC entity).
///
/// One row is created each time a time special with a
/// `TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS` rule is activated: it groups the
/// granted [`ObtainedUnit`]s (via their `expiration_id`) and records when they
/// expire so the `UNIT_EXPIRED` scheduled task can remove them. `expiration` is a
/// `timestamp` (UTC, second precision); `relation_id` points at the
/// `object_relations` row for the originating `TIME_SPECIAL`.
#[derive(Debug, Clone, sqlx::FromRow)]
pub struct ObtainedUnitTemporalInformation {
    /// `int unsigned` — matches `obtained_units.expiration_id`.
    pub id: u32,
    /// `int unsigned` — the lifetime in **seconds** (Java types it `Long`, but
    /// the column is `INT UNSIGNED` and the value comes from the rule's
    /// `duration` extra-arg).
    pub duration: u32,
    /// `timestamp` — when the granted units expire (sqlx decodes a MySQL
    /// `TIMESTAMP` column as `DateTime<Utc>`, distinct from a `DATETIME`'s
    /// `NaiveDateTime`).
    pub expiration: chrono::DateTime<chrono::Utc>,
    /// `smallint unsigned` — the `object_relations` id of the source TIME_SPECIAL.
    #[sqlx(rename = "relation_id")]
    pub relation_id: u16,
}
