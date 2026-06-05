//! Port of `TrackBrowserRestService`'s persistence — appends a row to the
//! `track_browser` table (frontend telemetry of client-side warn/error events).
//! There is no read side; the admin reads this table directly.

use chrono::Utc;

use crate::db::Db;
use crate::error::OwgeResult;

pub struct TrackBrowserBo;

impl TrackBrowserBo {
    /// `TrackBrowser.builder().method(method).jsonContent(content).createdAt(now)`.
    /// `method` is `warn` or `error`; the column is `varchar(8)`.
    pub async fn track(db: &Db, method: &str, content: &str) -> OwgeResult<()> {
        sqlx::query(
            "INSERT INTO track_browser (method, json_content, created_at) VALUES (?, ?, ?)",
        )
        .bind(method)
        .bind(content)
        .bind(Utc::now().naive_utc())
        .execute(db)
        .await?;
        Ok(())
    }
}
