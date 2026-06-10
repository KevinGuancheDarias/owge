use crate::{OwgeError, OwgeResult};
use chrono::NaiveDateTime;
use serde::{Deserialize, Serialize};

/// Mirrors the `alliances` table / Java `Alliance` entity. Although the Java
/// entity types `id` as `Integer`, the DB column is `smallint unsigned`, so we
/// use `u16` (like `Galaxy`). `owner_id` is the external account / `user_storage`
/// id (`int`).
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct Alliance {
    /// `smallint unsigned`.
    pub id: u16,
    pub name: String,
    pub description: Option<String>,
    /// `char(36)` image id, nullable.
    pub image: Option<String>,
    #[sqlx(rename = "owner_id")]
    pub owner_id: i32,
}

impl Alliance {
    pub fn check_owner(&self, invoker_id: i32) -> OwgeResult<()> {
        if self.owner_id == invoker_id {
            return Ok(());
        }
        Err(OwgeError::InvalidInput(
            "You are not the owner of the alliance".into(),
        ))
    }
}

/// Mirrors the `alliance_join_request` table / Java `AllianceJoinRequest`
/// entity.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct AllianceJoinRequest {
    /// `int unsigned`.
    pub id: u32,
    /// `smallint unsigned`.
    #[sqlx(rename = "alliance_id")]
    pub alliance_id: u16,
    /// `int` — the `user_storage` id.
    #[sqlx(rename = "user_id")]
    pub user_id: i32,
    #[sqlx(rename = "request_date")]
    pub request_date: NaiveDateTime,
}
