use serde::{Deserialize, Serialize};

/// Mirrors `GalaxyDto`. Built directly from the `galaxies` row.
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct GalaxyDto {
    pub id: u16,
    pub name: String,
    pub sectors: u32,
    pub quadrants: u32,
    pub num_planets: u32,
    pub order_number: Option<u16>,
}

/// Admin create/update request body for a galaxy (the `id` comes from the path
/// on update and is generated on create).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct GalaxyInput {
    pub name: String,
    pub sectors: u32,
    pub quadrants: u32,
    #[serde(default = "default_num_planets")]
    pub num_planets: u32,
    #[serde(default)]
    pub order_number: Option<u16>,
}

fn default_num_planets() -> u32 {
    20
}
