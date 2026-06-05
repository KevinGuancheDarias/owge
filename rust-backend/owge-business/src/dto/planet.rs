use serde::Serialize;

/// Mirrors `PlanetDto`. `ownerName` and `galaxyName` come from joins, so the
/// `Bo` populates them when building the DTO (no lazy entity navigation).
///
/// `name`, `richness`, `home`, `ownerId` and `ownerName` are `Option` because
/// `PlanetCleanerService.cleanUpUnexplored` nulls them for planets the viewing
/// user has not explored. Java's global Jackson `NON_NULL` omits null fields, so
/// they carry `skip_serializing_if` to reproduce the wire shape (the field is
/// absent, not `null`, for an unexplored planet).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct PlanetDto {
    pub id: u64,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub name: Option<String>,
    pub sector: u32,
    pub quadrant: u32,
    pub planet_number: u16,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner_id: Option<i32>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub owner_name: Option<String>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub richness: Option<u16>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub home: Option<bool>,
    pub galaxy_id: u16,
    pub galaxy_name: String,
    // specialLocation is omitted until the special-location domain lands (M2).
}

impl PlanetDto {
    /// `PlanetCleanerService.cleanUpUnexplored` — mask the fields a player may
    /// not see for a planet they have not explored.
    pub fn clean_up_unexplored(&mut self) {
        self.name = None;
        self.richness = None;
        self.home = None;
        self.owner_id = None;
        self.owner_name = None;
    }
}
