use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};
use sqlx::FromRow;
use sqlx_template::MysqlTemplate;

/// Mirrors the `user_storage` table / Java `UserStorage` entity — the per-game
/// player record (resources, faction, home planet, points, ...). The `id`
/// equals the external account-system user id (not auto-incremented).
#[derive(Debug, Clone, Serialize, Deserialize, FromRow, MysqlTemplate)]
#[table("user_storage")]
#[tp_select_one(by = "id")]
#[serde(rename_all = "camelCase")]
pub struct UserStorage {
    /// `int` (signed) — matches the external account-system user id.
    pub id: i32,
    pub username: String,
    pub email: String,
    /// `smallint unsigned`.
    pub alliance_id: Option<u16>,
    /// `smallint unsigned`.
    pub faction: u16,
    pub last_action: NaiveDateTime,
    pub home_planet: u64,
    pub primary_resource: Option<f64>,
    pub secondary_resource: Option<f64>,
    pub energy: f64,
    pub primary_resource_generation_per_second: Option<f64>,
    pub secondary_resource_generation_per_second: Option<f64>,
    pub has_skipped_tutorial: bool,
    pub points: f64,
    pub can_alter_twitch_state: bool,
    pub banned: bool,
}
