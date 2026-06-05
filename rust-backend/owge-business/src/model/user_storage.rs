use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};

/// Mirrors the `user_storage` table / Java `UserStorage` entity — the per-game
/// player record (resources, faction, home planet, points, ...). The `id`
/// equals the external account-system user id (not auto-incremented).
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct UserStorage {
    /// `int` (signed) — matches the external account-system user id.
    pub id: i32,
    pub username: String,
    pub email: String,
    /// `smallint unsigned`.
    #[serde(rename = "allianceId")]
    pub alliance_id: Option<u16>,
    /// `smallint unsigned`.
    pub faction: u16,
    #[serde(rename = "lastAction")]
    pub last_action: NaiveDateTime,
    #[serde(rename = "homePlanet")]
    pub home_planet: u64,
    #[serde(rename = "primaryResource")]
    pub primary_resource: Option<f64>,
    #[serde(rename = "secondaryResource")]
    pub secondary_resource: Option<f64>,
    pub energy: f64,
    #[serde(rename = "primaryResourceGenerationPerSecond")]
    pub primary_resource_generation_per_second: Option<f64>,
    #[serde(rename = "secondaryResourceGenerationPerSecond")]
    pub secondary_resource_generation_per_second: Option<f64>,
    #[sqlx(rename = "has_skipped_tutorial")]
    #[serde(rename = "hasSkippedTutorial")]
    pub has_skipped_tutorial: bool,
    pub points: f64,
    #[sqlx(rename = "can_alter_twitch_state")]
    #[serde(rename = "canAlterTwitchState")]
    pub can_alter_twitch_state: bool,
    pub banned: bool,
}
