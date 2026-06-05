use serde::Serialize;

use crate::dto::PlanetDto;

/// Mirrors `PlanetListDto`: a user's saved/named planet, flattening the
/// `PlanetUser` embedded key into `userId` + `username` and nesting the planet
/// info as a `PlanetDto`. `username` and the nested planet fields come from
/// joins, so the `Bo` populates them when building the DTO (no lazy nav).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanetListDto {
    pub user_id: i32,
    pub username: String,
    pub planet: PlanetDto,
    pub name: Option<String>,
}
