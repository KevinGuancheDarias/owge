//! Port of (the read side of) the upgrade domain business objects:
//! `com.kevinguanchedarias.owgejava.business.UpgradeTypeBo` (the
//! `upgrade_types_change` sync payload) and
//! `...business.ObtainedUpgradeBo` together with
//! `UpgradeRestService.findObtained` (the `obtained_upgrades_change` sync
//! payload).
//!
//! The `running_upgrade_change` payload (`MissionBo.findRunningLevelUpMission`)
//! is driven directly by [`crate::bo::MissionBo::find_running_level_up_mission`]
//! (see `websocket/sync.rs`), not by this module.

use crate::bo::ImprovementBo;
use crate::db::Db;
use crate::dto::upgrade::{
    ObtainedUpgradeDto, UpgradeDto, UpgradeInput, UpgradeTypeDto, UpgradeTypeInput,
};
use crate::dto::{ImprovementDto, ImprovementUnitTypeDto};
use crate::error::{OwgeError, OwgeResult};

// --- upgrade_types ---------------------------------------------------------

#[derive(sqlx::FromRow)]
struct UpgradeTypeRow {
    id: u16,
    name: String,
}

impl From<UpgradeTypeRow> for UpgradeTypeDto {
    fn from(r: UpgradeTypeRow) -> Self {
        UpgradeTypeDto {
            id: r.id,
            name: r.name,
        }
    }
}

// --- obtained_upgrades (with embedded upgrade) -----------------------------

/// One `obtained_upgrades` row joined with its `upgrades`, the upgrade type, and
/// the upgrade image — exact SQL column types so sqlx never panics on
/// signedness/width.
#[derive(sqlx::FromRow)]
struct ObtainedUpgradeRow {
    id: u32,
    level: i16,
    available: i8,

    // --- upgrades ---
    upgrade_id: u16,
    upgrade_name: String,
    upgrade_description: Option<String>,
    upgrade_image: Option<u64>,
    upgrade_image_filename: Option<String>,
    upgrade_order: Option<u16>,
    upgrade_points: i32,
    upgrade_time: i32,
    upgrade_primary_resource: i32,
    upgrade_secondary_resource: i32,
    upgrade_type_id: Option<u16>,
    upgrade_type_name: Option<String>,
    upgrade_level_effect: f32,
    upgrade_cloned_improvements: i8,
}

impl From<ObtainedUpgradeRow> for ObtainedUpgradeDto {
    fn from(r: ObtainedUpgradeRow) -> Self {
        let image_url = r.upgrade_image_filename.map(|f| crate::bo::image_store_bo::compute_image_url(&f));
        let upgrade = UpgradeDto {
            id: r.upgrade_id,
            name: r.upgrade_name,
            description: r.upgrade_description,
            image: r.upgrade_image,
            image_url,
            order: r.upgrade_order,
            points: r.upgrade_points,
            // `Upgrade.time` is a Java `Long`; the column is `int` (signed).
            time: r.upgrade_time as i64,
            primary_resource: r.upgrade_primary_resource,
            secondary_resource: r.upgrade_secondary_resource,
            type_id: r.upgrade_type_id,
            type_name: r.upgrade_type_name,
            level_effect: r.upgrade_level_effect,
            cloned_improvements: r.upgrade_cloned_improvements != 0,
        };
        ObtainedUpgradeDto {
            id: r.id,
            level: r.level,
            available: r.available != 0,
            upgrade,
        }
    }
}

const SELECT_OBTAINED_DTO: &str = "\
    SELECT ou.id, ou.level, ou.available, \
           u.id AS upgrade_id, u.name AS upgrade_name, u.description AS upgrade_description, \
           u.image_id AS upgrade_image, i.filename AS upgrade_image_filename, \
           u.order_number AS upgrade_order, u.points AS upgrade_points, u.time AS upgrade_time, \
           u.primary_resource AS upgrade_primary_resource, u.secondary_resource AS upgrade_secondary_resource, \
           u.type AS upgrade_type_id, ut.name AS upgrade_type_name, \
           u.level_effect AS upgrade_level_effect, u.cloned_improvements AS upgrade_cloned_improvements \
    FROM obtained_upgrades ou \
    JOIN upgrades u ON u.id = ou.upgrade_id \
    LEFT JOIN upgrade_types ut ON ut.id = u.type \
    LEFT JOIN images_store i ON i.id = u.image_id ";

// --- upgrades (single, for the admin CRUD) ---------------------------------

/// One `upgrades` row joined with its type name and image filename — exact SQL
/// column types so sqlx never panics on signedness/width. `order_number`
/// (`smallint unsigned`, nullable) was added by migration `v0.11.0.sql`, so it
/// is read here and surfaced as [`UpgradeDto::order`].
#[derive(sqlx::FromRow)]
struct UpgradeRow {
    id: u16,
    name: String,
    description: Option<String>,
    image: Option<u64>,
    image_filename: Option<String>,
    order_number: Option<u16>,
    points: i32,
    time: i32,
    primary_resource: i32,
    secondary_resource: i32,
    type_id: Option<u16>,
    type_name: Option<String>,
    level_effect: f32,
    cloned_improvements: i8,
}

impl From<UpgradeRow> for UpgradeDto {
    fn from(r: UpgradeRow) -> Self {
        let image_url = r.image_filename.map(|f| crate::bo::image_store_bo::compute_image_url(&f));
        UpgradeDto {
            id: r.id,
            name: r.name,
            description: r.description,
            image: r.image,
            image_url,
            order: r.order_number,
            points: r.points,
            time: r.time as i64,
            primary_resource: r.primary_resource,
            secondary_resource: r.secondary_resource,
            type_id: r.type_id,
            type_name: r.type_name,
            level_effect: r.level_effect,
            cloned_improvements: r.cloned_improvements != 0,
        }
    }
}

const SELECT_UPGRADE_DTO: &str = "\
    SELECT u.id, u.name, u.description, u.image_id AS image, i.filename AS image_filename, \
           u.order_number, u.points, u.time, u.primary_resource, u.secondary_resource, \
           u.type AS type_id, ut.name AS type_name, \
           u.level_effect, u.cloned_improvements \
    FROM upgrades u \
    LEFT JOIN upgrade_types ut ON ut.id = u.type \
    LEFT JOIN images_store i ON i.id = u.image_id ";

pub struct UpgradeBo;

impl UpgradeBo {
    /// `UpgradeTypeBo.findAll()` -> DTOs — the `upgrade_types_change` sync
    /// payload (the whole upgrade-type catalog).
    pub async fn find_upgrade_types(db: &Db) -> OwgeResult<Vec<UpgradeTypeDto>> {
        let rows = sqlx::query_as::<_, UpgradeTypeRow>(
            "SELECT id, name FROM upgrade_types ORDER BY id",
        )
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `CrudRestServiceTrait.findOneById` for the admin upgrade-type CRUD.
    pub async fn find_upgrade_type_by_id(db: &Db, id: u16) -> OwgeResult<Option<UpgradeTypeDto>> {
        let row = sqlx::query_as::<_, UpgradeTypeRow>(
            "SELECT id, name FROM upgrade_types WHERE id = ?",
        )
        .bind(id)
        .fetch_optional(db)
        .await?;
        Ok(row.map(Into::into))
    }

    /// `CrudRestServiceTrait.saveNew` — insert; `upgrade_types.id` is AUTO_INCREMENT.
    pub async fn save_new_upgrade_type(
        db: &Db,
        input: &UpgradeTypeInput,
    ) -> OwgeResult<UpgradeTypeDto> {
        let result = sqlx::query("INSERT INTO upgrade_types (name) VALUES (?)")
            .bind(&input.name)
            .execute(db)
            .await?;
        let id = result.last_insert_id() as u16;
        Self::find_upgrade_type_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Upgrade type vanished right after insert".into()))
    }

    /// `CrudRestServiceTrait.saveExisting` — update by id.
    pub async fn save_existing_upgrade_type(
        db: &Db,
        id: u16,
        input: &UpgradeTypeInput,
    ) -> OwgeResult<UpgradeTypeDto> {
        let affected = sqlx::query("UPDATE upgrade_types SET name = ? WHERE id = ?")
            .bind(&input.name)
            .bind(id)
            .execute(db)
            .await?
            .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No upgrade type with id {id}")));
        }
        Self::find_upgrade_type_by_id(db, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No upgrade type with id {id}")))
    }

    /// `WithDeleteRestServiceTrait.delete`.
    pub async fn delete_upgrade_type(db: &Db, id: u16) -> OwgeResult<()> {
        sqlx::query("DELETE FROM upgrade_types WHERE id = ?")
            .bind(id)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `UpgradeRestService.findObtained(user)` ->
    /// `obtainedUpgradeRepository.findByUserId(userId)` mapped to
    /// `ObtainedUpgradeDto` — the `obtained_upgrades_change` sync payload.
    pub async fn find_obtained_dtos(db: &Db, user_id: i32) -> OwgeResult<Vec<ObtainedUpgradeDto>> {
        let rows = sqlx::query_as::<_, ObtainedUpgradeRow>(&format!(
            "{SELECT_OBTAINED_DTO} WHERE ou.user_id = ? ORDER BY ou.id"
        ))
        .bind(user_id)
        .fetch_all(db)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    // The `running_upgrade_change` sync payload is now driven directly by
    // `MissionBo::find_running_level_up_mission` (see `websocket/sync.rs`), which
    // returns the real `RunningUpgradeDto` once the LEVEL_UP mission engine is
    // ported; the former empty-list stub here has been removed.

    // --- admin upgrade CRUD (AdminUpgradeRestService) ----------------------

    /// `CrudRestServiceTrait.findAll()` — every upgrade, ordered by id.
    pub async fn find_all(db: &Db) -> OwgeResult<Vec<UpgradeDto>> {
        let rows =
            sqlx::query_as::<_, UpgradeRow>(&format!("{SELECT_UPGRADE_DTO} ORDER BY u.id"))
                .fetch_all(db)
                .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `WithReadRestServiceTrait.findOneById` for the admin upgrade CRUD.
    pub async fn find_one(db: &Db, id: u16) -> OwgeResult<Option<UpgradeDto>> {
        let row = sqlx::query_as::<_, UpgradeRow>(&format!("{SELECT_UPGRADE_DTO} WHERE u.id = ?"))
            .bind(id)
            .fetch_optional(db)
            .await?;
        Ok(row.map(Into::into))
    }

    /// Applies the `AdminUpgradeRestService.beforeConversion` defaults: `time`
    /// null/<5 -> 60, `levelEffect` -> 0.5, `clonedImprovements` -> false.
    fn defaults(input: &UpgradeInput) -> (i32, f32, bool) {
        let time = match input.time {
            Some(t) if t >= 5 => t,
            _ => 60,
        };
        let level_effect = input.level_effect.unwrap_or(0.5);
        let cloned = input.cloned_improvements.unwrap_or(false);
        (time as i32, level_effect, cloned)
    }

    /// `CrudRestServiceTrait.saveNew` — insert; `upgrades.id` is AUTO_INCREMENT.
    /// `typeId` is mandatory (`beforeSave` throws otherwise).
    pub async fn save_new(db: &Db, input: &UpgradeInput) -> OwgeResult<UpgradeDto> {
        let type_id = input.type_id.ok_or_else(|| {
            OwgeError::InvalidInput("I18N_ERR_UPGRADE_TYPE_IS_MANDATORY".into())
        })?;
        let (time, level_effect, cloned) = Self::defaults(input);
        let result = sqlx::query(
            "INSERT INTO upgrades (name, description, image_id, points, time, \
                    primary_resource, secondary_resource, type, level_effect, cloned_improvements) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        )
        .bind(&input.name)
        .bind(&input.description)
        .bind(input.image)
        .bind(input.points)
        .bind(time)
        .bind(input.primary_resource)
        .bind(input.secondary_resource)
        .bind(type_id)
        .bind(level_effect)
        .bind(cloned as i8)
        .execute(db)
        .await?;
        let id = result.last_insert_id() as u16;
        Self::find_one(db, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Upgrade vanished right after insert".into()))
    }

    /// `CrudRestServiceTrait.saveExisting` — update by id.
    pub async fn save_existing(db: &Db, id: u16, input: &UpgradeInput) -> OwgeResult<UpgradeDto> {
        let type_id = input.type_id.ok_or_else(|| {
            OwgeError::InvalidInput("I18N_ERR_UPGRADE_TYPE_IS_MANDATORY".into())
        })?;
        let (time, level_effect, cloned) = Self::defaults(input);
        let affected = sqlx::query(
            "UPDATE upgrades SET name = ?, description = ?, image_id = ?, points = ?, time = ?, \
                    primary_resource = ?, secondary_resource = ?, type = ?, level_effect = ?, \
                    cloned_improvements = ? WHERE id = ?",
        )
        .bind(&input.name)
        .bind(&input.description)
        .bind(input.image)
        .bind(input.points)
        .bind(time)
        .bind(input.primary_resource)
        .bind(input.secondary_resource)
        .bind(type_id)
        .bind(level_effect)
        .bind(cloned as i8)
        .bind(id)
        .execute(db)
        .await?
        .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No upgrade with id {id}")));
        }
        Self::find_one(db, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No upgrade with id {id}")))
    }

    /// `WithDeleteRestServiceTrait.delete`.
    pub async fn delete(db: &Db, id: u16) -> OwgeResult<()> {
        sqlx::query("DELETE FROM upgrades WHERE id = ?")
            .bind(id)
            .execute(db)
            .await?;
        Ok(())
    }

    /// `CrudWithImprovements` `GET {id}/improvement`.
    pub async fn find_improvement(db: &Db, id: u16) -> OwgeResult<ImprovementDto> {
        ImprovementBo::find_for_entity(db, "upgrades", id).await
    }

    /// `CrudWithImprovements` `PUT {id}/improvement`.
    pub async fn save_improvement(
        db: &Db,
        id: u16,
        dto: &ImprovementDto,
    ) -> OwgeResult<ImprovementDto> {
        ImprovementBo::save_for_entity(db, "upgrades", id, dto).await
    }

    /// `GET {id}/improvement/unitTypeImprovements`.
    pub async fn find_unit_type_improvements(
        db: &Db,
        id: u16,
    ) -> OwgeResult<Vec<ImprovementUnitTypeDto>> {
        ImprovementBo::find_unit_type_improvements_for_entity(db, "upgrades", id).await
    }

    /// `POST {id}/improvement/unitTypeImprovements`.
    pub async fn add_unit_type_improvement(
        db: &Db,
        id: u16,
        dto: &ImprovementUnitTypeDto,
    ) -> OwgeResult<ImprovementUnitTypeDto> {
        ImprovementBo::add_unit_type_improvement_for_entity(db, "upgrades", id, dto).await
    }

    /// `DELETE {id}/improvement/unitTypeImprovements/{utiId}`.
    pub async fn delete_unit_type_improvement(
        db: &Db,
        id: u16,
        unit_type_improvement_id: u16,
    ) -> OwgeResult<()> {
        ImprovementBo::delete_unit_type_improvement_for_entity(
            db,
            "upgrades",
            id,
            unit_type_improvement_id,
        )
        .await
    }
}
