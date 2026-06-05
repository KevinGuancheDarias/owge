use serde::{Deserialize, Serialize};

/// Mirrors the `object_relations` table / Java `ObjectRelation` entity — the
/// generic indirection layer. A relation ties an `object_description` (the
/// `ObjectEnum` name: `UNIT`, `UPGRADE`, `TIME_SPECIAL`, `SPEED_IMPACT_GROUP`,
/// `REQUIREMENT_GROUP`) to a concrete entity's `reference_id`.
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct ObjectRelation {
    /// `smallint unsigned`.
    pub id: u16,
    #[sqlx(rename = "object_description")]
    pub object_description: String,
    /// `smallint` (signed) — the id of the concrete referenced entity.
    #[sqlx(rename = "reference_id")]
    pub reference_id: i16,
}

/// The `ObjectEnum` discriminator values stored in `object_description`.
pub mod object_enum {
    pub const UNIT: &str = "UNIT";
    pub const UPGRADE: &str = "UPGRADE";
    pub const TIME_SPECIAL: &str = "TIME_SPECIAL";
    pub const SPEED_IMPACT_GROUP: &str = "SPEED_IMPACT_GROUP";
    pub const REQUIREMENT_GROUP: &str = "REQUIREMENT_GROUP";
}
