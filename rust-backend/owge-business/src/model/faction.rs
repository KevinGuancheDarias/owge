use serde::{Deserialize, Serialize};

/// Mirrors the `factions` table / Java `Faction` entity. Factions are the
/// player-selectable races; the open faction list feeds the registration UI.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct Faction {
    pub id: u32,
    /// `hidden` is a nullable `tinyint`; hidden factions are excluded from the
    /// public list.
    pub hidden: Option<i8>,
    pub name: String,
    #[serde(rename = "primaryResourceName")]
    pub primary_resource_name: String,
    #[serde(rename = "secondaryResourceName")]
    pub secondary_resource_name: String,
    pub description: Option<String>,
}
