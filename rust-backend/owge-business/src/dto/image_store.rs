use serde::{Deserialize, Serialize};

/// Mirrors `ImageStoreDto`. Built from the `images_store` row, plus the
/// computed `url` (`ImageStoreBo.computeImageUrl`).
#[derive(Debug, Clone, Serialize)]
#[serde(rename_all = "camelCase")]
pub struct ImageStoreDto {
    pub id: u64,
    pub checksum: String,
    pub filename: String,
    pub display_name: Option<String>,
    pub description: Option<String>,
    pub url: String,
}

/// Admin update request body for an image. The Java `updateImage` endpoint
/// takes the whole `ImageStoreDto` but only `displayName` and `description` are
/// modifiable (`ImageStoreBo.update` validates both are non-empty); the `id`
/// comes from the path.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ImageStoreInput {
    pub display_name: String,
    pub description: String,
}

/// Upload request body for `POST admin/image_store`, mirroring the Java
/// `UploadImage` pojo (`{ displayName, base64 }`). The `base64` may be a bare
/// base64 string or a `data:image/...;base64,<data>` data URL.
#[derive(Debug, Clone, Deserialize)]
#[serde(rename_all = "camelCase")]
pub struct ImageUploadInput {
    pub display_name: String,
    pub base64: String,
}
