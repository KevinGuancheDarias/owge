//! Port of `FactionBo` — the public selectable-faction list plus the admin
//! faction CRUD (`AdminFactionRestService`, `CrudWithImprovements`, object code
//! `FACTION`) and its read-only sub-resources (`unitTypes`, `spawn-locations`).

use crate::bo::ImprovementBo;
use crate::db::Db;
use crate::dto::faction::{
    FactionDto, FactionInput, FactionSpawnLocationDto, FactionSpawnLocationInput,
    FactionUnitTypeDto, FactionUnitTypeOverrideInput,
};
use crate::dto::{ImprovementDto, ImprovementUnitTypeDto};
use crate::error::{OwgeError, OwgeResult};

/// One `factions` row joined with its resource image filenames, with exact SQL
/// column types so sqlx never panics on signedness/width.
#[derive(sqlx::FromRow)]
struct FactionRow {
    id: u16,
    hidden: Option<i8>,
    name: String,
    description: Option<String>,
    primary_resource_name: String,
    primary_resource_image: Option<u64>,
    primary_resource_image_filename: Option<String>,
    secondary_resource_name: String,
    secondary_resource_image: Option<u64>,
    secondary_resource_image_filename: Option<String>,
    energy_name: String,
    energy_image: Option<u64>,
    energy_image_filename: Option<String>,
    image_id: Option<u64>,
    image_filename: Option<String>,
    initial_primary_resource: u32,
    initial_secondary_resource: u32,
    initial_energy: u32,
    primary_resource_production: f32,
    secondary_resource_production: f32,
    max_planets: u8,
    cloned_improvements: i8,
    custom_primary_gather_percentage: Option<f32>,
    custom_secondary_gather_percentage: Option<f32>,
}

impl From<FactionRow> for FactionDto {
    fn from(r: FactionRow) -> Self {
        let url = |f: Option<String>| f.map(|f| crate::bo::image_store_bo::compute_image_url(&f));
        FactionDto {
            id: r.id,
            hidden: r.hidden.unwrap_or(0) != 0,
            name: r.name,
            description: r.description,
            primary_resource_name: r.primary_resource_name,
            primary_resource_image: r.primary_resource_image,
            primary_resource_image_url: url(r.primary_resource_image_filename),
            secondary_resource_name: r.secondary_resource_name,
            secondary_resource_image: r.secondary_resource_image,
            secondary_resource_image_url: url(r.secondary_resource_image_filename),
            energy_name: r.energy_name,
            energy_image: r.energy_image,
            energy_image_url: url(r.energy_image_filename),
            image: r.image_id,
            image_url: url(r.image_filename),
            initial_primary_resource: r.initial_primary_resource,
            initial_secondary_resource: r.initial_secondary_resource,
            initial_energy: r.initial_energy,
            primary_resource_production: r.primary_resource_production,
            secondary_resource_production: r.secondary_resource_production,
            max_planets: r.max_planets,
            cloned_improvements: r.cloned_improvements != 0,
            custom_primary_gather_percentage: r.custom_primary_gather_percentage,
            custom_secondary_gather_percentage: r.custom_secondary_gather_percentage,
            improvement: None,
            unit_types: None,
        }
    }
}

#[derive(sqlx::FromRow)]
struct FactionUnitTypeRow {
    id: u32,
    unit_type_id: u16,
    max_count: Option<u32>,
}

#[derive(sqlx::FromRow)]
struct SpawnLocationRow {
    galaxy_id: u16,
    sector_range_start: Option<u32>,
    sector_range_end: Option<u32>,
    quadrant_range_start: Option<u32>,
    quadrant_range_end: Option<u32>,
}

/// Select list shared by `find_all`/`find_by_id`, joining `images_store` three
/// times for the resource image filenames plus once for the faction image.
const FACTION_SELECT: &str = "SELECT f.id, f.hidden, f.name, f.description, \
        f.primary_resource_name, f.primary_resource_image_id AS primary_resource_image, \
        pi.filename AS primary_resource_image_filename, \
        f.secondary_resource_name, f.secondary_resource_image_id AS secondary_resource_image, \
        si.filename AS secondary_resource_image_filename, \
        f.energy_name, f.energy_image_id AS energy_image, \
        ei.filename AS energy_image_filename, \
        f.image_id, im.filename AS image_filename, \
        f.initial_primary_resource, f.initial_secondary_resource, f.initial_energy, \
        f.primary_resource_production, f.secondary_resource_production, f.max_planets, \
        f.cloned_improvements, f.custom_primary_gather_percentage, \
        f.custom_secondary_gather_percentage \
     FROM factions f \
     LEFT JOIN images_store pi ON pi.id = f.primary_resource_image_id \
     LEFT JOIN images_store si ON si.id = f.secondary_resource_image_id \
     LEFT JOIN images_store ei ON ei.id = f.energy_image_id \
     LEFT JOIN images_store im ON im.id = f.image_id";

pub struct FactionBo;

impl FactionBo {
    /// All non-hidden factions (`hidden` = 0).
    pub async fn find_visible(db: &Db) -> OwgeResult<Vec<FactionDto>> {
        let rows = sqlx::query_as::<_, FactionRow>(&format!(
            "{FACTION_SELECT} WHERE f.hidden = 0 ORDER BY f.id"
        ))
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `CrudRestServiceTrait.findAll` — every faction as the wide admin DTO.
    pub async fn find_all(db: &Db) -> OwgeResult<Vec<FactionDto>> {
        let rows = sqlx::query_as::<_, FactionRow>(&format!("{FACTION_SELECT} ORDER BY f.id"))
            .fetch_all(db)
            .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `WithReadRestServiceTrait.findOneById`.
    pub async fn find_by_id(db: &Db, id: u16) -> OwgeResult<Option<FactionDto>> {
        let row = sqlx::query_as::<_, FactionRow>(&format!("{FACTION_SELECT} WHERE f.id = ?"))
            .bind(id)
            .fetch_optional(db)
            .await?;
        Ok(row.map(Into::into))
    }

    pub async fn find_by_id_or_die(db: &Db, id: u16) -> OwgeResult<FactionDto> {
        let row = Self::find_by_id(db, id).await?;
        let row = row.ok_or(OwgeError::NotFound(format!(
            "Faction with ID {id} not found"
        )))?;

        Ok(row)
    }

    /// `FactionBo.save` validation — the custom gather percentages may not sum to
    /// more than 100 (each defaults to 1 when null).
    fn validate_gather(input: &FactionInput) -> OwgeResult<()> {
        let primary = input.custom_primary_gather_percentage.unwrap_or(1.0);
        let secondary = input.custom_secondary_gather_percentage.unwrap_or(1.0);
        let sum = primary + secondary;
        if sum > 100.0 {
            return Err(OwgeError::InvalidInput(
                "No, dear hacker, custom primary percentage plus secondary CAN'T be higher than 100"
                    .into(),
            ));
        }
        Ok(())
    }

    /// `CrudRestServiceTrait.saveNew` — insert; `factions.id` is AUTO_INCREMENT.
    pub async fn save_new(db: &Db, input: &FactionInput) -> OwgeResult<FactionDto> {
        Self::validate_gather(input)?;
        let result = sqlx::query(
            "INSERT INTO factions (hidden, name, image_id, primary_resource_image_id, \
                    secondary_resource_image_id, energy_image_id, description, \
                    primary_resource_name, secondary_resource_name, energy_name, \
                    initial_primary_resource, initial_secondary_resource, initial_energy, \
                    primary_resource_production, secondary_resource_production, max_planets, \
                    cloned_improvements, custom_primary_gather_percentage, \
                    custom_secondary_gather_percentage) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        )
        .bind(input.hidden.map(|h| h as i8))
        .bind(&input.name)
        .bind(input.image)
        .bind(input.primary_resource_image)
        .bind(input.secondary_resource_image)
        .bind(input.energy_image)
        .bind(&input.description)
        .bind(&input.primary_resource_name)
        .bind(&input.secondary_resource_name)
        .bind(&input.energy_name)
        .bind(input.initial_primary_resource)
        .bind(input.initial_secondary_resource)
        .bind(input.initial_energy)
        .bind(input.primary_resource_production)
        .bind(input.secondary_resource_production)
        .bind(input.max_planets)
        .bind(input.cloned_improvements as i8)
        .bind(input.custom_primary_gather_percentage)
        .bind(input.custom_secondary_gather_percentage)
        .execute(db)
        .await?;
        let id = result.last_insert_id() as u16;
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Faction vanished right after insert".into()))
    }

    /// `CrudRestServiceTrait.saveExisting` — update by id.
    pub async fn save_existing(db: &Db, id: u16, input: &FactionInput) -> OwgeResult<FactionDto> {
        Self::validate_gather(input)?;
        let affected = sqlx::query(
            "UPDATE factions SET hidden = ?, name = ?, image_id = ?, \
                    primary_resource_image_id = ?, secondary_resource_image_id = ?, \
                    energy_image_id = ?, description = ?, primary_resource_name = ?, \
                    secondary_resource_name = ?, energy_name = ?, initial_primary_resource = ?, \
                    initial_secondary_resource = ?, initial_energy = ?, \
                    primary_resource_production = ?, secondary_resource_production = ?, \
                    max_planets = ?, cloned_improvements = ?, \
                    custom_primary_gather_percentage = ?, \
                    custom_secondary_gather_percentage = ? WHERE id = ?",
        )
        .bind(input.hidden.map(|h| h as i8))
        .bind(&input.name)
        .bind(input.image)
        .bind(input.primary_resource_image)
        .bind(input.secondary_resource_image)
        .bind(input.energy_image)
        .bind(&input.description)
        .bind(&input.primary_resource_name)
        .bind(&input.secondary_resource_name)
        .bind(&input.energy_name)
        .bind(input.initial_primary_resource)
        .bind(input.initial_secondary_resource)
        .bind(input.initial_energy)
        .bind(input.primary_resource_production)
        .bind(input.secondary_resource_production)
        .bind(input.max_planets)
        .bind(input.cloned_improvements as i8)
        .bind(input.custom_primary_gather_percentage)
        .bind(input.custom_secondary_gather_percentage)
        .bind(id)
        .execute(db)
        .await?
        .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No faction with id {id}")));
        }
        Self::find_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No faction with id {id}")))
    }

    /// `WithDeleteRestServiceTrait.delete`.
    pub async fn delete(db: &Db, id: u16) -> OwgeResult<()> {
        sqlx::query("DELETE FROM factions WHERE id = ?")
            .bind(id)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `AdminFactionRestService.findUnitTypesOverrides` — the faction's unit-type
    /// overrides (`factions_unit_types`). `factionId` is nulled out to match the
    /// Java service.
    pub async fn find_unit_type_overrides(
        db: &Db,
        faction_id: u16,
    ) -> OwgeResult<Vec<FactionUnitTypeDto>> {
        let rows = sqlx::query_as::<_, FactionUnitTypeRow>(
            "SELECT id, unit_type_id, max_count FROM factions_unit_types \
             WHERE faction_id = ? ORDER BY id",
        )
        .bind(faction_id)
        .fetch_all(db)
        .await?;
        Ok(rows
            .into_iter()
            .map(|r| FactionUnitTypeDto {
                id: r.id,
                faction_id: None,
                unit_type_id: r.unit_type_id,
                max_count: r.max_count,
            })
            .collect())
    }

    /// `FactionSpawnLocationBo.findByFaction` — the faction's spawn locations.
    pub async fn find_spawn_locations(
        db: &Db,
        faction_id: u16,
    ) -> OwgeResult<Vec<FactionSpawnLocationDto>> {
        let rows = sqlx::query_as::<_, SpawnLocationRow>(
            "SELECT galaxy_id, sector_range_start, sector_range_end, \
                    quadrant_range_start, quadrant_range_end \
             FROM faction_spawn_location WHERE faction_id = ? ORDER BY id",
        )
        .bind(faction_id)
        .fetch_all(db)
        .await?;
        Ok(rows
            .into_iter()
            .map(|r| FactionSpawnLocationDto {
                galaxy_id: r.galaxy_id,
                sector_range_start: r.sector_range_start,
                sector_range_end: r.sector_range_end,
                quadrant_range_start: r.quadrant_range_start,
                quadrant_range_end: r.quadrant_range_end,
            })
            .collect())
    }

    /// `FactionBo.saveOverrides` — `PUT {id}/unitTypes`: rewrite the faction's
    /// `factions_unit_types` from the override list (delete-all then insert).
    pub async fn save_overrides(
        db: &Db,
        faction_id: u16,
        overrides: &[FactionUnitTypeOverrideInput],
    ) -> OwgeResult<()> {
        let mut tx = db.begin().await?;
        sqlx::query("DELETE FROM factions_unit_types WHERE faction_id = ?")
            .bind(faction_id)
            .execute(&mut *tx)
            .await?;
        for override_item in overrides {
            sqlx::query(
                "INSERT INTO factions_unit_types (faction_id, unit_type_id, max_count) \
                 VALUES (?, ?, ?)",
            )
            .bind(faction_id)
            .bind(override_item.id)
            .bind(override_item.override_max_count)
            .execute(&mut *tx)
            .await?;
        }
        tx.commit().await?;
        Ok(())
    }

    /// `FactionSpawnLocationBo.saveSpawnLocations` — `PUT {id}/spawn-locations`:
    /// rewrite `faction_spawn_location` for the faction (delete-all then insert).
    pub async fn save_spawn_locations(
        db: &Db,
        faction_id: u16,
        spawn_locations: &[FactionSpawnLocationInput],
    ) -> OwgeResult<()> {
        let mut tx = db.begin().await?;
        sqlx::query("DELETE FROM faction_spawn_location WHERE faction_id = ?")
            .bind(faction_id)
            .execute(&mut *tx)
            .await?;
        for location in spawn_locations {
            sqlx::query(
                "INSERT INTO faction_spawn_location \
                    (faction_id, galaxy_id, sector_range_start, sector_range_end, \
                     quadrant_range_start, quadrant_range_end) \
                 VALUES (?, ?, ?, ?, ?, ?)",
            )
            .bind(faction_id)
            .bind(location.galaxy_id)
            .bind(location.sector_range_start)
            .bind(location.sector_range_end)
            .bind(location.quadrant_range_start)
            .bind(location.quadrant_range_end)
            .execute(&mut *tx)
            .await?;
        }
        tx.commit().await?;
        Ok(())
    }

    /// `CrudWithImprovements` `GET {id}/improvement`.
    pub async fn find_improvement(db: &Db, id: u16) -> OwgeResult<ImprovementDto> {
        ImprovementBo::find_for_entity(db, "factions", id).await
    }

    /// `CrudWithImprovements` `PUT {id}/improvement`.
    pub async fn save_improvement(
        db: &Db,
        id: u16,
        dto: &ImprovementDto,
    ) -> OwgeResult<ImprovementDto> {
        ImprovementBo::save_for_entity(db, "factions", id, dto).await
    }

    /// `GET {id}/improvement/unitTypeImprovements`.
    pub async fn find_unit_type_improvements(
        db: &Db,
        id: u16,
    ) -> OwgeResult<Vec<ImprovementUnitTypeDto>> {
        ImprovementBo::find_unit_type_improvements_for_entity(db, "factions", id).await
    }

    /// `POST {id}/improvement/unitTypeImprovements`.
    pub async fn add_unit_type_improvement(
        db: &Db,
        id: u16,
        dto: &ImprovementUnitTypeDto,
    ) -> OwgeResult<ImprovementUnitTypeDto> {
        ImprovementBo::add_unit_type_improvement_for_entity(db, "factions", id, dto).await
    }

    /// `DELETE {id}/improvement/unitTypeImprovements/{utiId}`.
    pub async fn delete_unit_type_improvement(
        db: &Db,
        id: u16,
        unit_type_improvement_id: u16,
    ) -> OwgeResult<()> {
        ImprovementBo::delete_unit_type_improvement_for_entity(
            db,
            "factions",
            id,
            unit_type_improvement_id,
        )
        .await
    }
}
