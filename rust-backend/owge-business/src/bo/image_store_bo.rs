//! Port of the read/update/delete side of `ImageStoreBo`, backing
//! `AdminImageStoreRestService` (`admin/image_store`).
//!
//! The controller mixes two traits: `WithReadRestServiceTrait` (`GET ''`,
//! `GET '{id}'`) and `WithDeleteRestServiceTrait` (`DELETE '{id}'`), plus an
//! explicit `POST ''` upload and `PUT '{id}'` metadata update.
//!
//! The base64 **upload** (`ImageStoreBo.save(base64, displayName)`, [`ImageStoreBo::save`])
//! decodes the (optionally `data:image/...;base64,`-prefixed) payload, validates
//! its type by magic bytes (PNG/GIF/JPEG only — replacing Java's Tika), writes
//! the file under a name-based (UUID v3) filename to the dynamic-files directory,
//! and dedupes by md5 checksum.

use std::sync::LazyLock;

use base64::Engine;

use crate::db::Db;
use crate::dto::{ImageStoreDto, ImageStoreInput, ImageUploadInput};
use crate::error::{OwgeError, OwgeResult};
use crate::model::ImageStore;

/// The path segment under which dynamic images are served, read once from the
/// environment (default `dynamic`), mirroring Spring's
/// `@Value("${OWGE_DYNAMIC_URL:dynamic}")` in `ImageStoreBo`.
static DYNAMIC_URL: LazyLock<String> =
    LazyLock::new(|| std::env::var("OWGE_DYNAMIC_URL").unwrap_or_else(|_| "dynamic".into()));

/// The on-disk directory dynamic images are written to, mirroring Spring's
/// `@Value("${OWGE_DYNAMIC_FILES_PATH:/var/owge_data/dynamic}")`.
static DYNAMIC_FILES_PATH: LazyLock<String> = LazyLock::new(|| {
    std::env::var("OWGE_DYNAMIC_FILES_PATH").unwrap_or_else(|_| "/var/owge_data/dynamic".into())
});

/// Builds the public image `url` from the stored `filename`, mirroring
/// `ImageStoreBo.computeImageUrl`: `<schemeAndHost>/<dynamicUrl>/<filename>`.
///
/// In Java `schemeAndHost` is *always* effectively empty — its ternary
/// (`hasLength(imageHost) ? "" : imageHost`) is inverted, so `OWGE_IMAGE_HOST`
/// is never applied — yielding the root-relative `"/<dynamicUrl>/<filename>"`
/// that nginx serves from its `location /dynamic/` block. We reproduce that
/// observable result (host-less, `dynamicUrl` from `OWGE_DYNAMIC_URL`) rather
/// than the dead `imageHost` branch.
///
/// This is the single source of truth for image URLs; every `*Bo` that embeds
/// an `imageUrl` calls it.
pub(crate) fn compute_image_url(filename: &str) -> String {
    format!("/{}/{}", &*DYNAMIC_URL, filename)
}

/// Detects the image type from its leading magic bytes and returns the file
/// extension Tika's `MimeType.forName(...).getExtension()` would yield for the
/// matching media type. Only PNG, GIF and JPEG are accepted, mirroring the
/// `MediaType.IMAGE_{PNG,GIF,JPEG}` guard in `ImageStoreBo.findValidExtension`.
fn find_valid_extension(data: &[u8]) -> OwgeResult<&'static str> {
    // PNG: 89 50 4E 47 0D 0A 1A 0A
    if data.starts_with(&[0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A]) {
        Ok(".png")
    // GIF: ascii "GIF8" (covers GIF87a / GIF89a)
    } else if data.starts_with(b"GIF8") {
        Ok(".gif")
    // JPEG: FF D8 FF
    } else if data.starts_with(&[0xFF, 0xD8, 0xFF]) {
        Ok(".jpg")
    } else {
        Err(OwgeError::InvalidInput("I18N_ERR_INVALID_IMAGE_TYPE".into()))
    }
}

/// Replicates `java.util.UUID.nameUUIDFromBytes(bytes).toString()`: a name-based
/// (version 3, MD5) UUID, formatted lowercase `8-4-4-4-12`.
fn name_uuid_from_bytes(data: &[u8]) -> String {
    let mut bytes = md5::compute(data).0;
    bytes[6] = (bytes[6] & 0x0f) | 0x30; // version 3
    bytes[8] = (bytes[8] & 0x3f) | 0x80; // IETF variant
    format!(
        "{:02x}{:02x}{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}{:02x}{:02x}{:02x}{:02x}",
        bytes[0], bytes[1], bytes[2], bytes[3], bytes[4], bytes[5], bytes[6], bytes[7],
        bytes[8], bytes[9], bytes[10], bytes[11], bytes[12], bytes[13], bytes[14], bytes[15],
    )
}

/// Writes the bytes under `<DYNAMIC_FILES_PATH>/<filename>`, but only if no file
/// already exists there (mirrors `ImageStoreBo.saveToDisk`).
fn save_to_disk(data: &[u8], filename: &str) -> OwgeResult<()> {
    let path = std::path::Path::new(&*DYNAMIC_FILES_PATH).join(filename);
    if !path.exists() {
        std::fs::write(&path, data)
            .map_err(|e| OwgeError::Common(format!("Couldn't save the file to disk: {e}")))?;
    }
    Ok(())
}

fn to_dto(row: ImageStore) -> ImageStoreDto {
    let url = compute_image_url(&row.filename);
    ImageStoreDto {
        id: row.id,
        checksum: row.checksum,
        filename: row.filename,
        display_name: row.display_name,
        description: row.description,
        url,
    }
}

pub struct ImageStoreBo;

impl ImageStoreBo {
    /// `ImageStoreBo.save(base64, displayName)` — `POST ''`. Decodes the
    /// (optionally `data:image/...;base64,`-prefixed) payload, validates its
    /// image type by magic bytes, writes it to disk under a name-based UUID
    /// filename, then dedupes by md5 checksum: an existing row with the same
    /// checksum is reused, otherwise a new row is inserted.
    pub async fn save(db: &Db, input: &ImageUploadInput) -> OwgeResult<ImageStoreDto> {
        let base64 = &input.base64;
        let parsed = if base64.starts_with("data:image/") {
            base64
                .split_once(',')
                .map(|(_, data)| data)
                .ok_or_else(|| OwgeError::InvalidInput("I18N_ERR_INVALID_IMAGE_TYPE".into()))?
        } else {
            base64.as_str()
        };
        let binary_data = base64::engine::general_purpose::STANDARD
            .decode(parsed)
            .map_err(|e| OwgeError::InvalidInput(format!("Invalid base64: {e}")))?;

        let extension = find_valid_extension(&binary_data)?;
        let filename = format!("{}{}", name_uuid_from_bytes(&binary_data), extension);
        save_to_disk(&binary_data, &filename)?;

        let checksum = format!("{:x}", md5::compute(&binary_data));

        // Reuse the existing image when one already has this checksum.
        if let Some(existing) = sqlx::query_as::<_, ImageStore>(
            "SELECT id, checksum, filename, display_name, description \
             FROM images_store WHERE checksum = ?",
        )
        .bind(&checksum)
        .fetch_optional(db)
        .await?
        {
            return Ok(to_dto(existing));
        }

        let display_name = format!("{}{}", input.display_name, extension);
        // The Java `ImageStore` entity defaults `description = ""` (field-level),
        // so a freshly uploaded image stores an empty string, not NULL.
        let id = sqlx::query(
            "INSERT INTO images_store (display_name, filename, checksum, description) \
             VALUES (?, ?, ?, '')",
        )
        .bind(&display_name)
        .bind(&filename)
        .bind(&checksum)
        .execute(db)
        .await?
        .last_insert_id();

        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Not able to save the entity".into()))
    }

    /// `ImageStoreBo.findAll()` — every image, ordered by id, with computed urls.
    pub async fn find_all(db: &Db) -> OwgeResult<Vec<ImageStoreDto>> {
        let rows = sqlx::query_as::<_, ImageStore>(
            "SELECT id, checksum, filename, display_name, description \
             FROM images_store ORDER BY id",
        )
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(to_dto).collect())
    }

    /// `WithReadRestServiceTrait.findOneById` — single image (or `None`).
    pub async fn find_by_id(db: &Db, id: u64) -> OwgeResult<Option<ImageStoreDto>> {
        let row = sqlx::query_as::<_, ImageStore>(
            "SELECT id, checksum, filename, display_name, description \
             FROM images_store WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(db)
        .await?;
        Ok(row.map(to_dto))
    }

    /// `ImageStoreBo.update(ImageStoreDto)` — `PUT '{id}'`. Only `displayName`
    /// and `description` are modifiable, and both must be non-empty
    /// (`ValidationUtil.requireNonEmptyString`).
    pub async fn update(db: &Db, id: u64, input: &ImageStoreInput) -> OwgeResult<ImageStoreDto> {
        if input.display_name.is_empty() {
            return Err(OwgeError::InvalidInput("displayName is required".into()));
        }
        if input.description.is_empty() {
            return Err(OwgeError::InvalidInput("description is required".into()));
        }
        let affected = sqlx::query(
            "UPDATE images_store SET display_name = ?, description = ? WHERE id = ?",
        )
        .bind(&input.display_name)
        .bind(&input.description)
        .bind(id)
        .execute(db)
        .await?
        .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No image with id {id}")));
        }
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No image with id {id}")))
    }

    /// `WithDeleteRestServiceTrait.delete`. The Java side also unlinks the file
    /// on disk; that is part of the filesystem work deferred with the upload.
    pub async fn delete(db: &Db, id: u64) -> OwgeResult<()> {
        sqlx::query("DELETE FROM images_store WHERE id = ?")
            .bind(id)
            .execute(db)
            .await?;
        Ok(())
    }
}
