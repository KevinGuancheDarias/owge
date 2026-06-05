use serde::{Deserialize, Serialize};

/// Mirrors the `galaxies` table / Java `Galaxy` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct Galaxy {
    /// `smallint unsigned`.
    pub id: u16,
    pub name: String,
    /// `int unsigned`.
    pub sectors: u32,
    pub quadrants: u32,
    #[sqlx(rename = "num_planets")]
    pub num_planets: u32,
    /// `smallint unsigned`, nullable.
    #[sqlx(rename = "order_number")]
    pub order_number: Option<u16>,
}
