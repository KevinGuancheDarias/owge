//! Port of `WebsocketEventsInformationBo` — maintains the per-user, per-event
//! "last sent" watermark in `websocket_events_information`, used by the
//! frontend delta-sync to avoid re-sending unchanged data.

use chrono::NaiveDateTime;

use crate::dto::websocket::WebsocketEventsInformationDto;
use crate::error::OwgeResult;
use sqlx::MySqlConnection;

pub struct WebsocketEventsInformationBo;

impl WebsocketEventsInformationBo {
    /// Upsert the watermark for `(event_name, user_id)`. The table has no unique
    /// key in the base schema, so we emulate save-or-update explicitly.
    pub async fn save(
        conn: &mut MySqlConnection,
        event_name: &str,
        user_id: i32,
        last_sent: NaiveDateTime,
    ) -> OwgeResult<()> {
        let updated = sqlx::query(
            "UPDATE websocket_events_information SET last_sent = ? \
             WHERE event_name = ? AND user_id = ?",
        )
        .bind(last_sent)
        .bind(event_name)
        .bind(user_id)
        .execute(&mut *conn)
        .await?
        .rows_affected();

        if updated == 0 {
            sqlx::query(
                "INSERT INTO websocket_events_information (event_name, user_id, last_sent) \
                 VALUES (?, ?, ?)",
            )
            .bind(event_name)
            .bind(user_id)
            .bind(last_sent)
            .execute(&mut *conn)
            .await?;
        }
        Ok(())
    }

    /// `findByUserId` -> `toDto` — every watermark belonging to `user_id`,
    /// shaped as the [`WebsocketEventsInformationDto`] list the `authentication`
    /// reply carries. `lastSent` is rendered as epoch millis (see the dto docs).
    pub async fn find_by_user_id(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<WebsocketEventsInformationDto>> {
        let rows: Vec<(String, NaiveDateTime)> = sqlx::query_as(
            "SELECT event_name, last_sent FROM websocket_events_information WHERE user_id = ?",
        )
        .bind(user_id)
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows
            .into_iter()
            .map(|(event_name, last_sent)| WebsocketEventsInformationDto {
                event_name,
                user_id: Some(user_id),
                // Epoch SECONDS — matches Java's socket handshake, where the
                // `Instant` is serialised by netty's JavaTimeModule as a numeric
                // timestamp and the DATETIME column truncates to whole seconds.
                last_sent: Some(last_sent.and_utc().timestamp()),
            })
            .collect())
    }
}
