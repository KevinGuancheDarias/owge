use serde::{Deserialize, Serialize};

/// Mirrors the `images_store` table / Java `ImageStore` entity.
///
/// The Java entity exposes a `@Transient` `url` derived from `filename`; it is
/// not a column and is therefore not part of this row struct (it is computed in
/// the DTO mapping, see `ImageStoreBo::compute_image_url`).
#[derive(Debug, Clone, Serialize, Deserialize, sqlx::FromRow)]
pub struct ImageStore {
    /// `bigint unsigned`, AUTO_INCREMENT.
    pub id: u64,
    /// `char(32)`, not null.
    pub checksum: String,
    /// `varchar(500)`, not null.
    pub filename: String,
    /// `varchar(50)`, nullable.
    #[sqlx(rename = "display_name")]
    pub display_name: Option<String>,
    /// `varchar(200)`, nullable.
    pub description: Option<String>,
}
