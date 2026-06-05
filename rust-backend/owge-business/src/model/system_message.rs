use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};

/// Mirrors the `system_messages` table / Java `SystemMessage` entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct SystemMessage {
    /// `smallint unsigned`.
    pub id: u16,
    /// `text`.
    pub content: String,
    /// `datetime`.
    pub creation_date: NaiveDateTime,
}

/// Mirrors the `user_read_system_messages` table / Java `UserReadSystemMessage`
/// entity — the join row marking a message as read by a user.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct UserReadSystemMessage {
    /// `int unsigned`.
    pub id: u32,
    /// `int` (signed) — the reading user's id.
    pub user_id: i32,
    /// `smallint unsigned` — the read message's id.
    pub message_id: u16,
}
