//! Port of (the CRUD read/write side of) `SpecialLocationBo` and
//! `AdminSpecialLocationRestService` (`admin/special-location`, a
//! `CrudWithImprovementsRestServiceTrait<SpecialLocation>`).
//!
//! The standard CRUD plus the `GET {id}/improvement` sub-resource are ported.
//!
//! Divergences from the Java `SpecialLocationBo.save`/`delete`, which are out of
//! scope this pass:
//! - The Java `save` resolves the `galaxy_id` into a *random concrete planet*
//!   (`PlanetBo.findRandomPlanet`) and links that planet's `special_location`
//!   FK, persisting the planet's galaxy rather than the requested one. That
//!   planet-assignment side effect (and the matching `delete` that unlinks the
//!   planet) depends on the planet/random-selection engine and is **not** done
//!   here — the requested `galaxy_id` is persisted verbatim. See TODO below.
//! - `CrudWithImprovementsRestServiceTrait.beforeSave` lazily creates an empty
//!   `improvements` row when the entity has none; that is mirrored on insert.

use crate::bo::ImprovementBo;
use crate::dto::special_location::{ImprovementDto, SpecialLocationDto, SpecialLocationInput};
use crate::error::{OwgeError, OwgeResult};
use sqlx::MySqlConnection;

#[derive(sqlx::FromRow)]
struct SpecialLocationRow {
    id: u16,
    name: String,
    description: String,
    image_id: Option<u64>,
    image_filename: Option<String>,
    galaxy_id: Option<u16>,
    galaxy_name: Option<String>,
    assigned_planet_id: Option<u64>,
    assigned_planet_name: Option<String>,
}

impl From<SpecialLocationRow> for SpecialLocationDto {
    fn from(r: SpecialLocationRow) -> Self {
        let image_url = r
            .image_filename
            .map(|f| crate::bo::image_store_bo::compute_image_url(&f));
        SpecialLocationDto {
            id: r.id,
            name: r.name,
            description: r.description,
            image: r.image_id,
            image_url,
            // `beforeRequestEnd` nulls the improvement on every CRUD response.
            improvement: None,
            galaxy_id: r.galaxy_id,
            galaxy_name: r.galaxy_name,
            assigned_planet_id: r.assigned_planet_id,
            assigned_planet_name: r.assigned_planet_name,
        }
    }
}

const SELECT_SPECIAL_LOCATION_DTO: &str = "SELECT sl.id, sl.name, sl.description, \
        sl.image_id AS image_id, i.filename AS image_filename, \
        sl.galaxy_id AS galaxy_id, g.name AS galaxy_name, \
        p.id AS assigned_planet_id, p.name AS assigned_planet_name \
     FROM special_locations sl \
     LEFT JOIN images_store i ON i.id = sl.image_id \
     LEFT JOIN galaxies g ON g.id = sl.galaxy_id \
     LEFT JOIN planets p ON p.special_location_id = sl.id";

pub struct SpecialLocationBo;

impl SpecialLocationBo {
    /// `WithReadRestServiceTrait.findAll`.
    pub async fn find_all(conn: &mut MySqlConnection) -> OwgeResult<Vec<SpecialLocationDto>> {
        let rows = sqlx::query_as::<_, SpecialLocationRow>(&format!(
            "{SELECT_SPECIAL_LOCATION_DTO} ORDER BY sl.id"
        ))
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `WithReadRestServiceTrait.findOneById`.
    pub async fn find_by_id(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<Option<SpecialLocationDto>> {
        let row = sqlx::query_as::<_, SpecialLocationRow>(&format!(
            "{SELECT_SPECIAL_LOCATION_DTO} WHERE sl.id = ?"
        ))
        .bind(id)
        .fetch_optional(&mut *conn)
        .await?;
        Ok(row.map(Into::into))
    }

    /// `CrudRestServiceTrait.saveNew` — insert; `special_locations.id` is
    /// AUTO_INCREMENT. Mirrors `beforeSave` by creating an empty `improvements`
    /// row and linking it via `improvement_id`.
    pub async fn save_new(
        conn: &mut MySqlConnection,
        input: &SpecialLocationInput,
    ) -> OwgeResult<SpecialLocationDto> {
        let improvement_id = ImprovementBo::create_empty(&mut *conn).await?;
        let result = sqlx::query(
            "INSERT INTO special_locations \
                (name, description, image_id, galaxy_id, improvement_id, cloned_improvements) \
             VALUES (?, ?, ?, ?, ?, ?)",
        )
        .bind(&input.name)
        .bind(input.description.clone().unwrap_or_default())
        .bind(input.image)
        // TODO(special-location-planet): the Java save resolves galaxy_id into a
        // random planet and persists that planet's galaxy; here it is stored as-is.
        .bind(input.galaxy_id)
        .bind(improvement_id)
        .bind(i8::from(input.cloned_improvements))
        .execute(&mut *conn)
        .await?;
        let id = result.last_insert_id() as u16;
        Self::find_by_id(&mut *conn, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Special location vanished right after insert".into()))
    }

    /// `CrudRestServiceTrait.saveExisting` — update by id. The existing
    /// `improvement_id` is preserved (the improvement is edited via its own
    /// sub-resource, not the CRUD body).
    pub async fn save_existing(
        conn: &mut MySqlConnection,
        id: u16,
        input: &SpecialLocationInput,
    ) -> OwgeResult<SpecialLocationDto> {
        let affected = sqlx::query(
            "UPDATE special_locations SET name = ?, description = ?, image_id = ?, \
                    galaxy_id = ?, cloned_improvements = ? WHERE id = ?",
        )
        .bind(&input.name)
        .bind(input.description.clone().unwrap_or_default())
        .bind(input.image)
        .bind(input.galaxy_id)
        .bind(i8::from(input.cloned_improvements))
        .bind(id)
        .execute(&mut *conn)
        .await?
        .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!(
                "No special location with id {id}"
            )));
        }
        Self::find_by_id(&mut *conn, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No special location with id {id}")))
    }

    /// `WithDeleteRestServiceTrait.delete`.
    ///
    /// TODO(special-location-planet): the Java `delete` first unlinks the
    /// assigned planet's `special_location` FK; that side effect is skipped here.
    pub async fn delete(conn: &mut MySqlConnection, id: u16) -> OwgeResult<()> {
        sqlx::query("DELETE FROM special_locations WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?;
        Ok(())
    }

    /// Resolves a special location's `improvement_id`, distinguishing
    /// "location does not exist" from "location has no improvement".
    async fn resolve_improvement_id(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<Option<u16>> {
        let improvement_id: Option<Option<u16>> =
            sqlx::query_scalar("SELECT improvement_id FROM special_locations WHERE id = ?")
                .bind(id)
                .fetch_optional(&mut *conn)
                .await?;
        match improvement_id {
            None => Err(OwgeError::NotFound(format!(
                "No special location with id {id}"
            ))),
            Some(opt) => Ok(opt),
        }
    }

    /// `CrudWithImprovementsRestServiceTrait` `GET {id}/improvement` — the
    /// special location's improvement (incl. its `unitTypesUpgrades`). 404 when
    /// the location does not exist or has no improvement.
    pub async fn find_improvement(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<ImprovementDto> {
        let improvement_id = Self::resolve_improvement_id(&mut *conn, id).await?;
        ImprovementBo::find_dto(&mut *conn, improvement_id).await
    }

    /// `CrudWithImprovementsRestServiceTrait` `PUT {id}/improvement` —
    /// create-if-missing then update the special location's improvement.
    pub async fn save_improvement(
        conn: &mut MySqlConnection,
        id: u16,
        dto: &ImprovementDto,
    ) -> OwgeResult<ImprovementDto> {
        let improvement_id = match Self::resolve_improvement_id(&mut *conn, id).await? {
            Some(existing) => existing,
            None => {
                let new_id = ImprovementBo::create_empty(&mut *conn).await?;
                sqlx::query("UPDATE special_locations SET improvement_id = ? WHERE id = ?")
                    .bind(new_id)
                    .bind(id)
                    .execute(&mut *conn)
                    .await?;
                new_id
            }
        };
        ImprovementBo::update_from_dto(&mut *conn, improvement_id, dto).await
    }

    /// `GET {id}/improvement/unitTypeImprovements`.
    pub async fn find_unit_type_improvements(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<Vec<crate::dto::ImprovementUnitTypeDto>> {
        let improvement_id = Self::resolve_improvement_id(&mut *conn, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound("I18N_ERR_NULL_IMPROVEMENT".into()))?;
        ImprovementBo::load_unit_type_improvement_dtos(&mut *conn, improvement_id).await
    }

    /// `POST {id}/improvement/unitTypeImprovements`.
    pub async fn add_unit_type_improvement(
        conn: &mut MySqlConnection,
        id: u16,
        dto: &crate::dto::ImprovementUnitTypeDto,
    ) -> OwgeResult<crate::dto::ImprovementUnitTypeDto> {
        let improvement_id = Self::resolve_improvement_id(&mut *conn, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound("I18N_ERR_NULL_IMPROVEMENT".into()))?;
        ImprovementBo::add_unit_type_improvement(&mut *conn, improvement_id, dto).await
    }

    /// `DELETE {id}/improvement/unitTypeImprovements/{unitTypeImprovementId}`.
    pub async fn delete_unit_type_improvement(
        conn: &mut MySqlConnection,
        id: u16,
        unit_type_improvement_id: u16,
    ) -> OwgeResult<()> {
        let improvement_id = Self::resolve_improvement_id(&mut *conn, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound("I18N_ERR_NULL_IMPROVEMENT".into()))?;
        ImprovementBo::remove_unit_type_improvement(
            &mut *conn,
            improvement_id,
            unit_type_improvement_id,
        )
        .await
    }
}
