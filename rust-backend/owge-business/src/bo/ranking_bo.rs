//! Port of (the read side of) `RankingBo`, backing `GET game/ranking`
//! (`RankingRestService.findAll`).
//!
//! Despite the legacy `ranking` table, `RankingBo.findRanking()` builds the
//! ranking live: it reads every user (`findAllByOrderByPointsDesc`), assigns a
//! 1-based position by points descending, and embeds each user's faction
//! (name/description/image) and alliance (id/name when present).

use crate::dto::ranking::{RankingEntryDto, RankingFactionDto};
use crate::error::OwgeResult;
use sqlx::MySqlConnection;

/// One user joined with its faction (+ faction image) and optional alliance,
/// with exact SQL column types so sqlx never panics on signedness/width.
#[derive(sqlx::FromRow)]
struct RankingRow {
    user_id: i32,
    username: String,
    /// `user_storage.points` — `double`.
    points: f64,
    alliance_id: Option<u16>,
    alliance_name: Option<String>,
    faction_name: String,
    faction_description: Option<String>,
    /// `factions.image_id` — `bigint unsigned`, nullable.
    faction_image: Option<u64>,
    /// `images_store.filename`, nullable (no image / unmatched join).
    faction_image_filename: Option<String>,
}

impl From<RankingRow> for RankingFactionDto {
    fn from(r: RankingRow) -> Self {
        let image_url = r
            .faction_image_filename
            .map(|f| crate::bo::image_store_bo::compute_image_url(&f));
        RankingFactionDto {
            id: None,
            name: r.faction_name,
            description: r.faction_description,
            image: r.faction_image,
            image_url,
        }
    }
}

pub struct RankingBo;

impl RankingBo {
    /// `RankingBo.findRanking()` — every user ordered by points descending, with
    /// a 1-based `position`, faction info and (optional) alliance info.
    pub async fn find_ranking(conn: &mut MySqlConnection) -> OwgeResult<Vec<RankingEntryDto>> {
        let rows = sqlx::query_as::<_, RankingRow>(
            "SELECT u.id AS user_id, u.username, u.points, \
                    a.id AS alliance_id, a.name AS alliance_name, \
                    f.name AS faction_name, f.description AS faction_description, \
                    f.image_id AS faction_image, i.filename AS faction_image_filename \
             FROM user_storage u \
             JOIN factions f ON f.id = u.faction \
             LEFT JOIN images_store i ON i.id = f.image_id \
             LEFT JOIN alliances a ON a.id = u.alliance_id \
             ORDER BY u.points DESC",
        )
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows
            .into_iter()
            .enumerate()
            .map(|(idx, r)| {
                let position = (idx as i32) + 1;
                let points = r.points;
                let user_id = r.user_id;
                let username = r.username.clone();
                let alliance_id = r.alliance_id;
                let alliance_name = r.alliance_name.clone();
                RankingEntryDto {
                    position,
                    points,
                    user_id,
                    username,
                    alliance_id,
                    alliance_name,
                    faction: r.into(),
                }
            })
            .collect())
    }
}
