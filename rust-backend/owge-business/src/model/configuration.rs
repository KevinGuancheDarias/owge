use serde::{Deserialize, Serialize};

/// Mirrors the `configuration` table and the Java `Configuration` entity.
/// Keyed by `name`; holds arbitrary string-valued engine settings (JWT
/// secrets, durations, feature toggles, ...).
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct Configuration {
    pub name: String,
    #[serde(rename = "displayName")]
    pub display_name: Option<String>,
    pub value: String,
    /// `privileged` is `tinyint`; non-zero means the value is not exposed to
    /// the open/public configuration endpoint.
    pub privileged: i8,
}
