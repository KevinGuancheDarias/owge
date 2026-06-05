use serde::Serialize;

/// Mirrors `RankingEntry` (record). `position` is 1-based and assigned in the
/// `Bo` after ordering users by points descending.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RankingEntryDto {
    pub position: i32,
    /// `user_storage.points` — `double` in the schema.
    pub points: f64,
    pub user_id: i32,
    pub username: String,
    /// `alliances.id` — `smallint unsigned`, null when the user has no alliance.
    pub alliance_id: Option<u16>,
    pub alliance_name: Option<String>,
    pub faction: RankingFactionDto,
}

/// Mirrors the `CommonDtoWithImageStore<Integer, Faction>` nested in
/// `RankingEntry.faction`. In Java only `name`, `description`, `image` and
/// `imageUrl` are populated (`id` stays null), so we keep `id` absent.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct RankingFactionDto {
    /// Always `null` to match `RankingBo.findRanking` (id is never set there).
    pub id: Option<u16>,
    pub name: String,
    pub description: Option<String>,
    /// `factions.image_id` — `bigint unsigned`, the image-store id.
    pub image: Option<u64>,
    pub image_url: Option<String>,
}
