use serde::{Deserialize, Serialize};

/// Mirrors the `admin_users` table / Java `AdminUser` entity. The `id` matches
/// the game (account-system) user id — it is not auto-incremented.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct AdminUser {
    /// `int unsigned` — sqlx requires the Rust type's signedness to match the
    /// column, so this is `u32`, not `i64`.
    pub id: u32,
    pub username: String,
    /// `enabled` is `tinyint(1)` — a disabled admin is rejected at login.
    pub enabled: bool,
    /// `can_add_admins` is `tinyint unsigned`.
    #[serde(rename = "canAddAdmins")]
    pub can_add_admins: u8,
}
