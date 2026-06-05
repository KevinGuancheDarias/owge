//! Port of `AdminUserBo` — admin-panel login. The caller is already
//! authenticated with a *game* token (the `/game/adminLogin` route lives under
//! the game-auth filter); this looks the user up in `admin_users`, validates,
//! and mints an *admin*-scoped HMAC token.

use chrono::Utc;
use jsonwebtoken::Algorithm;
use serde::Serialize;

use crate::db::Db;
use crate::error::{OwgeError, OwgeResult};
use crate::jwt::{self, TokenUser};
use crate::model::AdminUser;

pub struct AdminUserBo;

/// Mirror of `TokenPojo` — the admin login response.
#[derive(Debug, Serialize)]
pub struct TokenPojo {
    pub token: String,
    #[serde(rename = "userId")]
    pub user_id: i64,
}

/// Settings resolved once at boot from the `configuration` table
/// (`ADMIN_JWT_SECRET`, `ADMIN_JWT_ALGO`, `ADMIN_JWT_DURATION_SECONDS`).
#[derive(Debug, Clone)]
pub struct AdminJwtSettings {
    pub secret: String,
    pub algorithm: Algorithm,
    pub duration_seconds: i64,
}

/// Mirror of the Java `AdminUserDto` — note `canAddAdmins`/`enabled` serialize
/// as JSON booleans (the column is `tinyint`).
#[derive(Debug, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct AdminUserDto {
    pub id: u32,
    pub username: String,
    pub enabled: bool,
    pub can_add_admins: bool,
}

impl From<AdminUser> for AdminUserDto {
    fn from(a: AdminUser) -> Self {
        AdminUserDto {
            id: a.id,
            username: a.username,
            enabled: a.enabled,
            can_add_admins: a.can_add_admins != 0,
        }
    }
}

impl AdminUserBo {
    pub async fn find_by_id(db: &Db, id: i64) -> OwgeResult<Option<AdminUser>> {
        let row = sqlx::query_as::<_, AdminUser>(
            "SELECT id, username, enabled, can_add_admins FROM admin_users WHERE id = ?",
        )
        .bind(id as u32)
        .fetch_optional(db)
        .await?;
        Ok(row)
    }

    /// `AdminAdminsRestService.findAll` -> `adminUserBo.toDto(adminUserBo.findAll())`.
    pub async fn find_all(db: &Db) -> OwgeResult<Vec<AdminUserDto>> {
        let rows = sqlx::query_as::<_, AdminUser>(
            "SELECT id, username, enabled, can_add_admins FROM admin_users ORDER BY id",
        )
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `AdminUserBo.addAdmin(accountUserId, username)` — idempotent: if the admin
    /// already exists it is returned unchanged, otherwise a new enabled admin is
    /// inserted (`can_add_admins` defaults to `false`, matching the entity).
    pub async fn add_admin(db: &Db, id: i64, username: &str) -> OwgeResult<AdminUserDto> {
        if let Some(existing) = Self::find_by_id(db, id).await? {
            return Ok(existing.into());
        }
        sqlx::query(
            "INSERT INTO admin_users (id, username, enabled, can_add_admins) VALUES (?, ?, 1, 0)",
        )
        .bind(id as u32)
        .bind(username)
        .execute(db)
        .await?;
        Ok(AdminUserDto {
            id: id as u32,
            username: username.to_string(),
            enabled: true,
            can_add_admins: false,
        })
    }

    /// `AdminAdminsRestService.delete` -> `adminUserRepository.deleteById(id)`.
    pub async fn delete_by_id(db: &Db, id: i64) -> OwgeResult<()> {
        sqlx::query("DELETE FROM admin_users WHERE id = ?")
            .bind(id as u32)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `AdminUserBo.login()` — validate the already-authenticated game user as
    /// an admin and issue the admin token.
    pub async fn login(
        db: &Db,
        game_user: &TokenUser,
        settings: &AdminJwtSettings,
    ) -> OwgeResult<TokenPojo> {
        let admin = Self::find_by_id(db, game_user.id)
            .await?
            .ok_or_else(|| OwgeError::AccessDenied("ERR_NO_SUCH_USER".into()))?;
        if !admin.enabled {
            return Err(OwgeError::AccessDenied("ERR_USER_NOT_ENABLED".into()));
        }

        // Keep username in sync with the account system, like the Java login.
        if admin.username != game_user.username {
            sqlx::query("UPDATE admin_users SET username = ? WHERE id = ?")
                .bind(&game_user.username)
                .bind(admin.id)
                .execute(db)
                .await?;
        }

        let now = Utc::now().timestamp();
        let admin_token_user = TokenUser {
            id: admin.id as i64,
            username: game_user.username.clone(),
            email: game_user.email.clone(),
        };
        let token = jwt::build_hmac_token(
            &settings.secret,
            settings.algorithm,
            &admin_token_user,
            now,
            now + settings.duration_seconds,
        )?;

        Ok(TokenPojo {
            token,
            user_id: admin.id as i64,
        })
    }
}
