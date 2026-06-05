//! Port of (the read side of) `SystemMessageBo`. Builds `SystemMessageUserDto`s
//! with the per-user `read` flag resolved via a join, matching
//! `SystemMessageBo.findReadByUser` / `translateToUser`.

use crate::db::Db;
use crate::dto::{SystemMessageDto, SystemMessageInput, SystemMessageUserDto};
use crate::error::{OwgeError, OwgeResult};

/// A system message row with the per-user read state, with exact SQL column
/// types so sqlx decode never panics on signedness/width.
#[derive(sqlx::FromRow)]
struct SystemMessageUserRow {
    id: u16,
    content: String,
    creation_date: chrono::NaiveDateTime,
    /// `EXISTS(...)` evaluates to `0`/`1` (signed) in MySQL.
    is_read: i64,
}

impl From<SystemMessageUserRow> for SystemMessageUserDto {
    fn from(r: SystemMessageUserRow) -> Self {
        SystemMessageUserDto {
            id: r.id,
            content: r.content,
            creation_date: r.creation_date,
            read: r.is_read != 0,
        }
    }
}

pub struct SystemMessageBo;

impl SystemMessageBo {
    /// `SystemMessageRestService.markAsRead` — record the given message ids as
    /// read for the user (idempotent per (user, message)).
    pub async fn mark_as_read(db: &Db, user_id: i32, message_ids: &[u16]) -> OwgeResult<()> {
        for &message_id in message_ids {
            let exists: i64 = sqlx::query_scalar(
                "SELECT COUNT(*) FROM user_read_system_messages WHERE user_id = ? AND message_id = ?",
            )
            .bind(user_id)
            .bind(message_id)
            .fetch_one(db)
            .await?;
            if exists == 0 {
                sqlx::query(
                    "INSERT INTO user_read_system_messages (user_id, message_id) VALUES (?, ?)",
                )
                .bind(user_id)
                .bind(message_id)
                .execute(db)
                .await?;
            }
        }
        Ok(())
    }

    /// `SystemMessageBo.findReadByUser(userId)` -> DTOs — the
    /// `system_message_change` sync payload. Messages are sorted by id DESC,
    /// matching the Java `Sort.by(Direction.DESC, "id")`.
    pub async fn find_read_by_user(
        db: &Db,
        user_id: i32,
    ) -> OwgeResult<Vec<SystemMessageUserDto>> {
        let rows = sqlx::query_as::<_, SystemMessageUserRow>(
            "SELECT sm.id, sm.content, sm.creation_date, \
                    EXISTS( \
                        SELECT 1 FROM user_read_system_messages ursm \
                        WHERE ursm.message_id = sm.id AND ursm.user_id = ? \
                    ) AS is_read \
             FROM system_messages sm \
             ORDER BY sm.id DESC",
        )
        .bind(user_id)
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `SystemMessageBo.save(SystemMessageDto)` — inserts a new system message
    /// (the id must be null, per `EntityUtil.requireNullId`; here the body has
    /// no id at all) and returns the persisted DTO.
    ///
    /// The Java path also emits the `system_message_change` websocket event to
    /// every user after commit; that emission is not yet ported here.
    pub async fn save(db: &Db, input: &SystemMessageInput) -> OwgeResult<SystemMessageDto> {
        let creation_date = input.effective_creation_date();
        let result = sqlx::query("INSERT INTO system_messages (content, creation_date) VALUES (?, ?)")
            .bind(&input.content)
            .bind(creation_date)
            .execute(db)
            .await?;
        let id = result.last_insert_id() as u16;
        let row = sqlx::query_as::<_, SystemMessageRow>(
            "SELECT id, content, creation_date FROM system_messages WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(db)
        .await?
        .ok_or_else(|| OwgeError::Common("System message vanished right after insert".into()))?;
        Ok(row.into())
    }
}

/// A bare `system_messages` row, for the admin create response.
#[derive(sqlx::FromRow)]
struct SystemMessageRow {
    id: u16,
    content: String,
    creation_date: chrono::NaiveDateTime,
}

impl From<SystemMessageRow> for SystemMessageDto {
    fn from(r: SystemMessageRow) -> Self {
        SystemMessageDto {
            id: r.id,
            content: r.content,
            creation_date: r.creation_date,
        }
    }
}
