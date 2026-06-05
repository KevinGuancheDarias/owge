use serde::{Deserialize, Serialize};

use crate::model::Configuration;

/// Mirrors `ConfigurationDto` — the admin-facing view of a `configuration` row.
/// Note: `privileged` is intentionally **not** exposed (the Java DTO only copies
/// `name`, `displayName` and `value`).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ConfigurationDto {
    pub name: String,
    pub display_name: Option<String>,
    pub value: String,
}

impl From<Configuration> for ConfigurationDto {
    fn from(c: Configuration) -> Self {
        ConfigurationDto {
            name: c.name,
            display_name: c.display_name,
            value: c.value,
        }
    }
}

/// Admin create/update request body. The Java controller binds the whole
/// `ConfigurationDto` (so `name` travels in the body, not only the path).
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ConfigurationInput {
    pub name: String,
    #[serde(default)]
    pub display_name: Option<String>,
    pub value: String,
}
