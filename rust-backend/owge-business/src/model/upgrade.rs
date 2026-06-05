//! Persistence entities for the upgrade domain — the Rust counterpart of the
//! Java JPA entities `com.kevinguanchedarias.owgejava.entity.Upgrade`,
//! `...entity.UpgradeType`, and `...entity.ObtainedUpgrade`.
//!
//! Field types mirror the exact MySQL column types in
//! `business/database/02_schema.sql` (plus migration `v0.11.0.sql`, which adds
//! `upgrades.order_number`), so sqlx never panics on signedness/width.

use serde::{Deserialize, Serialize};

/// Mirrors the `upgrade_types` table / Java `UpgradeType` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct UpgradeType {
    /// `smallint unsigned`.
    pub id: u16,
    pub name: String,
}

/// Mirrors the `upgrades` table / Java `Upgrade` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct Upgrade {
    /// `smallint unsigned`.
    pub id: u16,
    pub name: String,
    pub description: Option<String>,
    /// `smallint unsigned`, nullable — added by migration `v0.11.0.sql`
    /// (`@Column(name = "order_number")`).
    #[sqlx(rename = "order_number")]
    pub order_number: Option<u16>,
    /// `int` (signed), default 0.
    pub points: i32,
    /// `bigint unsigned`, nullable — FK to `images_store`.
    #[sqlx(rename = "image_id")]
    pub image_id: Option<u64>,
    /// `int` (signed), default 60.
    pub time: i32,
    #[sqlx(rename = "primary_resource")]
    pub primary_resource: i32,
    #[sqlx(rename = "secondary_resource")]
    pub secondary_resource: i32,
    /// `smallint unsigned`, nullable — FK to `upgrade_types` (`Null means invisible`).
    #[sqlx(rename = "type")]
    pub type_id: Option<u16>,
    #[sqlx(rename = "level_effect")]
    pub level_effect: f32,
    /// `smallint unsigned`, nullable — FK to `improvements`.
    #[sqlx(rename = "improvement_id")]
    pub improvement_id: Option<u16>,
    /// `tinyint(1)` — read as `i8`, maps to bool.
    #[sqlx(rename = "cloned_improvements")]
    pub cloned_improvements: i8,
}

/// Mirrors the `obtained_upgrades` table / Java `ObtainedUpgrade` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct ObtainedUpgrade {
    /// `int unsigned`.
    pub id: u32,
    /// `int` (signed) — FK to `user_storage`.
    #[sqlx(rename = "user_id")]
    pub user_id: i32,
    /// `smallint unsigned` — FK to `upgrades`.
    #[sqlx(rename = "upgrade_id")]
    pub upgrade_id: u16,
    /// `smallint` (signed).
    pub level: i16,
    /// `tinyint` — read as `i8`, maps to bool.
    pub available: i8,
}
