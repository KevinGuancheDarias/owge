use serde::Serialize;

/// Mirrors `SimpleUserDataDto` (a Java record `{id, username, email}`).
#[derive(Debug, Clone, Serialize)]
pub struct SimpleUserDataDto {
    pub id: i32,
    pub username: String,
    pub email: String,
}

/// Mirrors `SimpleUserDataWithSuspicionsCountsDto` (record `{user, suspicionsCount}`).
#[derive(Debug, Clone, Serialize)]
pub struct SimpleUserDataWithSuspicionsCountsDto {
    pub user: SimpleUserDataDto,
    #[serde(rename = "suspicionsCount")]
    pub suspicions_count: i64,
}

/// Mirrors `UserStorageDto` (the `user_data_change` sync payload). The nested
/// faction/home-planet/alliance/improvements objects are populated as their
/// domains land; the directly-stored and computed-resource fields are complete.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct UserStorageDto {
    pub id: i32,
    pub username: String,
    pub email: String,
    pub primary_resource: Option<f64>,
    pub secondary_resource: Option<f64>,
    pub consumed_energy: Option<f64>,
    pub primary_resource_generation_per_second: Option<f64>,
    pub secondary_resource_generation_per_second: Option<f64>,
    pub max_energy: Option<f64>,
    pub has_skipped_tutorial: bool,
    pub can_alter_twitch_state: bool,
    /// Java's `findData` never sets the `computed*` fields, so they stay `null`
    /// and are omitted by the global `NON_NULL` inclusion. We mirror that:
    /// always `None`, omitted from the JSON.
    #[serde(skip_serializing_if = "Option::is_none")]
    pub computed_primary_resource_generation_per_second: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub computed_secondary_resource_generation_per_second: Option<f64>,
    #[serde(skip_serializing_if = "Option::is_none")]
    pub computed_max_energy: Option<f64>,
}
