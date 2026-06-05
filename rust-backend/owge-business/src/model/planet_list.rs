use serde::{Deserialize, Serialize};

/// Mirrors the `planet_list` table / Java `PlanetList` entity (whose primary
/// key is the embedded `PlanetUser` = `(user_id, planet_id)`).
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct PlanetList {
    /// `int` (signed) — the owning user id.
    pub user_id: i32,
    /// `bigint unsigned` — the listed planet id.
    pub planet_id: u64,
    /// `varchar(150)` nullable — the user-given name for the planet.
    pub name: Option<String>,
}
