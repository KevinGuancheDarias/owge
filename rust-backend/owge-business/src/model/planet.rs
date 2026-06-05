use serde::{Deserialize, Serialize};

/// Mirrors the `planets` table / Java `Planet` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct Planet {
    /// `bigint unsigned`.
    pub id: u64,
    pub name: String,
    #[sqlx(rename = "galaxy_id")]
    pub galaxy_id: u16,
    /// `int unsigned`.
    pub sector: u32,
    pub quadrant: u32,
    #[sqlx(rename = "planet_number")]
    pub planet_number: u16,
    /// `int` (signed, nullable) — the owning user id, or NULL if unowned.
    pub owner: Option<i32>,
    pub richness: u16,
    /// `tinyint` nullable, default 0.
    pub home: Option<i8>,
    #[sqlx(rename = "special_location_id")]
    pub special_location_id: Option<u16>,
}
