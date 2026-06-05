use serde::{Deserialize, Serialize};

/// Mirrors the `special_locations` table / Java `SpecialLocation` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct SpecialLocation {
    /// `smallint unsigned`.
    pub id: u16,
    pub name: String,
    /// `image_id`, `bigint unsigned`, nullable.
    #[sqlx(rename = "image_id")]
    pub image_id: Option<u64>,
    /// `text NOT NULL`.
    pub description: String,
    /// `galaxy_id`, `smallint unsigned`, nullable.
    #[sqlx(rename = "galaxy_id")]
    pub galaxy_id: Option<u16>,
    /// `improvement_id`, `smallint unsigned`, nullable.
    #[sqlx(rename = "improvement_id")]
    pub improvement_id: Option<u16>,
    /// `cloned_improvements`, `tinyint NOT NULL`.
    #[sqlx(rename = "cloned_improvements")]
    pub cloned_improvements: i8,
}
