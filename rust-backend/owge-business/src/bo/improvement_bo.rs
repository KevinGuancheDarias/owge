//! Port of `ImprovementBo.createOrUpdateFromDto`, the `CrudWithImprovements`
//! `find` (`GET {id}/improvement`), and `ImprovementUnitTypeBo` (the per
//! unit-type improvement sub-resources). These are shared by every
//! `CrudWithImprovements`/`CrudWithFull` entity (unit, faction, upgrade, special
//! location) — each entity's `*Bo` resolves its own `improvement_id` column and
//! then delegates here.
//!
//! Cache eviction (`IMPROVEMENT_CACHE_TAG_BY_USER`, the `user_improvements_change`
//! websocket emission) is M4 and is left as a TODO at the relevant call sites.

use sqlx::MySqlConnection;

use crate::dto::UnitTypeDto;
use crate::dto::improvement::{ImprovementDto, ImprovementUnitTypeDto};
use crate::error::{OwgeError, OwgeResult};

/// An `improvements` row, with exact column types so sqlx never panics on
/// signedness/width.
#[derive(sqlx::FromRow)]
struct ImprovementRow {
    id: u16,
    more_primary_resource_production: Option<i16>,
    more_secondary_resource_production: Option<i16>,
    more_energy_production: Option<i16>,
    more_charge_capacity: Option<i16>,
    more_missions_value: Option<i8>,
    more_upgrade_research_speed: Option<f32>,
    more_unit_build_speed: Option<f32>,
}

/// One `improvements_unit_types` row joined with its unit type's scalar columns.
#[derive(sqlx::FromRow)]
struct ImprovementUnitTypeRow {
    id: u16,
    r#type: String,
    value: i32,
    // --- unit_types (for the nested UnitTypeDto) ---
    unit_type_id: u16,
    unit_type_name: String,
    unit_type_image: Option<u64>,
    unit_type_image_filename: Option<String>,
    unit_type_max_count: Option<i64>,
    unit_type_share_max_count_id: Option<u16>,
    unit_type_parent_id: Option<u16>,
    unit_type_has_to_inherit_improvements: i8,
    unit_type_can_explore: String,
    unit_type_can_gather: String,
    unit_type_can_establish_base: String,
    unit_type_can_attack: String,
    unit_type_can_counterattack: String,
    unit_type_can_conquest: String,
    unit_type_can_deploy: String,
}

const SELECT_IMPROVEMENT_UNIT_TYPE: &str = "\
    SELECT iut.id, iut.type, iut.value, \
           ut.id AS unit_type_id, ut.name AS unit_type_name, \
           ut.image_id AS unit_type_image, img.filename AS unit_type_image_filename, \
           ut.max_count AS unit_type_max_count, \
           ut.share_max_count AS unit_type_share_max_count_id, \
           ut.parent_type AS unit_type_parent_id, \
           ut.has_to_inherit_improvements AS unit_type_has_to_inherit_improvements, \
           ut.can_explore AS unit_type_can_explore, ut.can_gather AS unit_type_can_gather, \
           ut.can_establish_base AS unit_type_can_establish_base, \
           ut.can_attack AS unit_type_can_attack, \
           ut.can_counterattack AS unit_type_can_counterattack, \
           ut.can_conquest AS unit_type_can_conquest, ut.can_deploy AS unit_type_can_deploy \
    FROM improvements_unit_types iut \
    JOIN unit_types ut ON ut.id = iut.unit_type_id \
    LEFT JOIN images_store img ON img.id = ut.image_id ";

impl From<ImprovementUnitTypeRow> for ImprovementUnitTypeDto {
    fn from(r: ImprovementUnitTypeRow) -> Self {
        let unit_type = UnitTypeDto {
            id: r.unit_type_id,
            name: r.unit_type_name,
            image: r.unit_type_image,
            image_url: r
                .unit_type_image_filename
                .map(|f| crate::bo::image_store_bo::compute_image_url(&f)),
            max_count: r.unit_type_max_count,
            share_max_count_id: r.unit_type_share_max_count_id,
            parent_id: r.unit_type_parent_id,
            has_to_inherit_improvements: r.unit_type_has_to_inherit_improvements != 0,
            can_explore: r.unit_type_can_explore,
            can_gather: r.unit_type_can_gather,
            can_establish_base: r.unit_type_can_establish_base,
            can_attack: r.unit_type_can_attack,
            can_counterattack: r.unit_type_can_counterattack,
            can_conquest: r.unit_type_can_conquest,
            can_deploy: r.unit_type_can_deploy,
            computed_max_count: r.unit_type_max_count,
            user_built: None,
            used: false,
        };
        ImprovementUnitTypeDto {
            id: Some(r.id),
            r#type: Some(r.r#type),
            unit_type_id: None,
            unit_type_name: None,
            unit_type: Some(unit_type),
            value: Some(r.value as i64),
        }
    }
}

pub struct ImprovementBo;

impl ImprovementBo {
    /// `CrudWithImprovements.find` — build the full `ImprovementDto` (including the
    /// nested `unitTypesUpgrades`) for an entity's `improvement_id`.
    /// `NotFound("I18N_ERR_NULL_IMPROVEMENT")` when the entity has no improvement.
    pub async fn find_dto(
        conn: &mut MySqlConnection,
        improvement_id: Option<u16>,
    ) -> OwgeResult<ImprovementDto> {
        let improvement_id = improvement_id
            .ok_or_else(|| OwgeError::NotFound("I18N_ERR_NULL_IMPROVEMENT".into()))?;
        let row = sqlx::query_as::<_, ImprovementRow>(
            "SELECT id, more_primary_resource_production, more_secondary_resource_production, \
                    more_energy_production, more_charge_capacity, more_missions_value, \
                    more_upgrade_research_speed, more_unit_build_speed \
             FROM improvements WHERE id = ?",
        )
        .bind(improvement_id)
        .fetch_optional(&mut *conn)
        .await?
        .ok_or_else(|| OwgeError::NotFound("I18N_ERR_NULL_IMPROVEMENT".into()))?;
        let unit_types_upgrades =
            Self::load_unit_type_improvement_dtos(&mut *conn, improvement_id).await?;
        Ok(ImprovementDto {
            id: row.id,
            more_primary_resource_production: row
                .more_primary_resource_production
                .map(|v| v as f32),
            more_secondary_resource_production: row
                .more_secondary_resource_production
                .map(|v| v as f32),
            more_energy_production: row.more_energy_production.map(|v| v as f32),
            more_charge_capacity: row.more_charge_capacity.map(|v| v as f32),
            more_missions: row.more_missions_value.map(|v| v as f32),
            more_upgrade_research_speed: row.more_upgrade_research_speed,
            more_unit_build_speed: row.more_unit_build_speed,
            unit_types_upgrades,
        })
    }

    /// `ImprovementBo.createOrUpdateFromDto` — update the `improvements` row's
    /// scalar columns from the DTO (the `unitTypesUpgrades` list is nulled by the
    /// controller and ignored here). The caller has already guaranteed the
    /// improvement row exists (`beforeSave` creates one when missing), so this
    /// only updates. Returns the refreshed `ImprovementDto`.
    ///
    /// The smallint columns store integral percentages; the DTO carries `Float`,
    /// matching `dtoUtilService.entityFromDto` which narrows back to the entity's
    /// declared field types (`Short`/`Byte`/`Float`).
    pub async fn update_from_dto(
        conn: &mut MySqlConnection,
        improvement_id: u16,
        dto: &ImprovementDto,
    ) -> OwgeResult<ImprovementDto> {
        sqlx::query(
            "UPDATE improvements SET \
                more_primary_resource_production = ?, more_secondary_resource_production = ?, \
                more_energy_production = ?, more_charge_capacity = ?, more_missions_value = ?, \
                more_upgrade_research_speed = ?, more_unit_build_speed = ? \
             WHERE id = ?",
        )
        .bind(dto.more_primary_resource_production.map(|v| v as i16))
        .bind(dto.more_secondary_resource_production.map(|v| v as i16))
        .bind(dto.more_energy_production.map(|v| v as i16))
        .bind(dto.more_charge_capacity.map(|v| v as i16))
        .bind(dto.more_missions.map(|v| v as i8))
        .bind(dto.more_upgrade_research_speed)
        .bind(dto.more_unit_build_speed)
        .bind(improvement_id)
        .execute(&mut *conn)
        .await?;
        // No further cache action needed here: `update_from_dto` is only called
        // from `save_for_entity`, which calls `UserImprovementBo::evict_all()`
        // after this returns (mirroring Java's `clearCacheEntries` at the caller
        // level). This is an admin definition edit, so there is no specific user
        // to emit `user_improvements_change` to — `evict_all()` is the correct
        // granularity and is already applied by the callers.
        Self::find_dto(&mut *conn, Some(improvement_id)).await
    }

    /// Resolves an entity's `improvement_id` column, distinguishing
    /// "entity does not exist" (`NotFound "No <entity> ..."`) from "entity exists
    /// but has no improvement" (`Ok(None)`). `table` must be a trusted constant
    /// (never user input) — it is interpolated into the SQL.
    pub async fn resolve_entity_improvement_id(
        conn: &mut MySqlConnection,
        table: &str,
        id: u16,
    ) -> OwgeResult<Option<u16>> {
        let improvement_id: Option<Option<u16>> =
            sqlx::query_scalar(&format!("SELECT improvement_id FROM {table} WHERE id = ?"))
                .bind(id)
                .fetch_optional(&mut *conn)
                .await?;
        match improvement_id {
            None => Err(OwgeError::NotFound(format!("No {table} with id {id}"))),
            Some(opt) => Ok(opt),
        }
    }

    /// `GET {id}/improvement` for an entity stored in `table` (with an
    /// `improvement_id` column).
    pub async fn find_for_entity(
        conn: &mut MySqlConnection,
        table: &str,
        id: u16,
    ) -> OwgeResult<ImprovementDto> {
        let improvement_id = Self::resolve_entity_improvement_id(&mut *conn, table, id).await?;
        Self::find_dto(&mut *conn, improvement_id).await
    }

    /// `PUT {id}/improvement` for an entity stored in `table` — create-if-missing
    /// then update, linking the new improvement via the entity's `improvement_id`.
    pub async fn save_for_entity(
        conn: &mut MySqlConnection,
        table: &str,
        id: u16,
        dto: &ImprovementDto,
    ) -> OwgeResult<ImprovementDto> {
        let improvement_id =
            match Self::resolve_entity_improvement_id(&mut *conn, table, id).await? {
                Some(existing) => existing,
                None => {
                    let new_id = Self::create_empty(&mut *conn).await?;
                    sqlx::query(&format!(
                        "UPDATE {table} SET improvement_id = ? WHERE id = ?"
                    ))
                    .bind(new_id)
                    .bind(id)
                    .execute(&mut *conn)
                    .await?;
                    new_id
                }
            };
        let result = Self::update_from_dto(&mut *conn, improvement_id, dto).await?;
        // clearCacheEntries: an improvement *definition* changed → any user holding
        // it has a stale aggregate.
        crate::bo::UserImprovementBo::evict_all();
        Ok(result)
    }

    /// `GET {id}/improvement/unitTypeImprovements` for an entity in `table`.
    pub async fn find_unit_type_improvements_for_entity(
        conn: &mut MySqlConnection,
        table: &str,
        id: u16,
    ) -> OwgeResult<Vec<ImprovementUnitTypeDto>> {
        let improvement_id = Self::resolve_entity_improvement_id(&mut *conn, table, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound("I18N_ERR_NULL_IMPROVEMENT".into()))?;
        Self::load_unit_type_improvement_dtos(&mut *conn, improvement_id).await
    }

    /// `POST {id}/improvement/unitTypeImprovements` for an entity in `table`.
    pub async fn add_unit_type_improvement_for_entity(
        conn: &mut MySqlConnection,
        table: &str,
        id: u16,
        dto: &ImprovementUnitTypeDto,
    ) -> OwgeResult<ImprovementUnitTypeDto> {
        let improvement_id = Self::resolve_entity_improvement_id(&mut *conn, table, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound("I18N_ERR_NULL_IMPROVEMENT".into()))?;
        let result = Self::add_unit_type_improvement(&mut *conn, improvement_id, dto).await?;
        crate::bo::UserImprovementBo::evict_all();
        Ok(result)
    }

    /// `DELETE {id}/improvement/unitTypeImprovements/{uti}` for an entity in `table`.
    pub async fn delete_unit_type_improvement_for_entity(
        conn: &mut MySqlConnection,
        table: &str,
        id: u16,
        unit_type_improvement_id: u16,
    ) -> OwgeResult<()> {
        let improvement_id = Self::resolve_entity_improvement_id(&mut *conn, table, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound("I18N_ERR_NULL_IMPROVEMENT".into()))?;
        Self::remove_unit_type_improvement(&mut *conn, improvement_id, unit_type_improvement_id)
            .await?;
        crate::bo::UserImprovementBo::evict_all();
        Ok(())
    }

    /// Inserts an empty `improvements` row and returns its AUTO_INCREMENT id —
    /// mirrors the `beforeSave` lazy `improvementRepository.save(new Improvement())`.
    pub async fn create_empty(conn: &mut MySqlConnection) -> OwgeResult<u16> {
        let result = sqlx::query("INSERT INTO improvements () VALUES ()")
            .execute(&mut *conn)
            .await?;
        Ok(result.last_insert_id() as u16)
    }

    /// `ImprovementUnitTypeBo.loadImprovementUnitTypes` + `convertEntireArray` —
    /// the per-unit-type improvements for an improvement, as DTOs.
    pub async fn load_unit_type_improvement_dtos(
        conn: &mut MySqlConnection,
        improvement_id: u16,
    ) -> OwgeResult<Vec<ImprovementUnitTypeDto>> {
        let rows = sqlx::query_as::<_, ImprovementUnitTypeRow>(&format!(
            "{SELECT_IMPROVEMENT_UNIT_TYPE} WHERE iut.improvement_id = ? ORDER BY iut.id"
        ))
        .bind(improvement_id)
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `ImprovementUnitTypeBo.add` — validate and insert a per-unit-type
    /// improvement for the given improvement, returning the saved DTO.
    ///
    /// Validation mirrors `checkIsValid`: the `type` must be a valid
    /// `ImprovementTypeEnum`, the `unitType` must exist, the `value` must be
    /// non-zero, and the `(type, unit_type)` pair must be unique within the
    /// improvement (`I18N_ERR_UNIT_IMPROVEMENT_DUPLICATED`). The `AMOUNT`-type
    /// "unit type must have a max count" check is omitted (a value-only check
    /// against the unit-type config — see the TODO).
    pub async fn add_unit_type_improvement(
        conn: &mut MySqlConnection,
        improvement_id: u16,
        dto: &ImprovementUnitTypeDto,
    ) -> OwgeResult<ImprovementUnitTypeDto> {
        let type_value = dto
            .r#type
            .as_deref()
            .filter(|t| is_valid_improvement_type(t))
            .ok_or_else(|| OwgeError::InvalidInput("I18N_ERR_INVALID_TYPE".into()))?;
        let unit_type_id = dto
            .resolved_unit_type_id()
            .ok_or_else(|| OwgeError::InvalidInput("I18N_ERR_INVALID_UNIT_TYPE".into()))?;
        let unit_type_exists: bool =
            sqlx::query_scalar::<_, i64>("SELECT COUNT(*) FROM unit_types WHERE id = ?")
                .bind(unit_type_id)
                .fetch_one(&mut *conn)
                .await?
                > 0;
        if !unit_type_exists {
            return Err(OwgeError::InvalidInput("I18N_ERR_INVALID_UNIT_TYPE".into()));
        }
        // TODO: the Java `checkValidUnitType` additionally rejects an `AMOUNT`
        // improvement on a unit type that has no max count; that needs the unit
        // type's `max_count` semantics and is omitted here.
        let value = dto
            .value
            .filter(|&v| v != 0)
            .ok_or_else(|| OwgeError::InvalidInput("I18N_ERR_INVALID_VALUE".into()))?;
        let duplicated: bool = sqlx::query_scalar::<_, i64>(
            "SELECT COUNT(*) FROM improvements_unit_types \
             WHERE improvement_id = ? AND type = ? AND unit_type_id = ?",
        )
        .bind(improvement_id)
        .bind(type_value)
        .bind(unit_type_id)
        .fetch_one(&mut *conn)
        .await?
            > 0;
        if duplicated {
            return Err(OwgeError::InvalidInput(
                "I18N_ERR_UNIT_IMPROVEMENT_DUPLICATED".into(),
            ));
        }
        let result = sqlx::query(
            "INSERT INTO improvements_unit_types (improvement_id, type, unit_type_id, value) \
             VALUES (?, ?, ?, ?)",
        )
        .bind(improvement_id)
        .bind(type_value)
        .bind(unit_type_id)
        .bind(value as i32)
        .execute(&mut *conn)
        .await?;
        let id = result.last_insert_id() as u16;
        // No further cache action here: `add_unit_type_improvement_for_entity`
        // (the only public caller) already calls `UserImprovementBo::evict_all()`
        // after this returns, matching Java's `clearCacheEntries` at the caller.
        let row = sqlx::query_as::<_, ImprovementUnitTypeRow>(&format!(
            "{SELECT_IMPROVEMENT_UNIT_TYPE} WHERE iut.id = ?"
        ))
        .bind(id)
        .fetch_one(&mut *conn)
        .await?;
        Ok(row.into())
    }

    /// `ImprovementUnitTypeBo.checkHasUnitTypeImprovementById` +
    /// `removeImprovementUnitType` — delete a per-unit-type improvement after
    /// verifying it belongs to the given improvement.
    pub async fn remove_unit_type_improvement(
        conn: &mut MySqlConnection,
        improvement_id: u16,
        unit_type_improvement_id: u16,
    ) -> OwgeResult<()> {
        let belongs: bool = sqlx::query_scalar::<_, i64>(
            "SELECT COUNT(*) FROM improvements_unit_types WHERE id = ? AND improvement_id = ?",
        )
        .bind(unit_type_improvement_id)
        .bind(improvement_id)
        .fetch_one(&mut *conn)
        .await?
            > 0;
        if !belongs {
            return Err(OwgeError::NotFound(
                "I18N_ERR_IMPROVEMENT_HAS_NO_SUCH_UNIT_IMPROVEMENT".into(),
            ));
        }
        sqlx::query("DELETE FROM improvements_unit_types WHERE id = ?")
            .bind(unit_type_improvement_id)
            .execute(&mut *conn)
            .await?;
        // No further cache action here: `delete_unit_type_improvement_for_entity`
        // (the only public caller) already calls `UserImprovementBo::evict_all()`
        // after this returns, matching Java's `clearCacheEntries` at the caller.
        Ok(())
    }
}

/// `EnumUtils.isValidEnum(ImprovementTypeEnum.class, ...)` — the
/// `improvements_unit_types.type` enum values.
fn is_valid_improvement_type(value: &str) -> bool {
    matches!(value, "ATTACK" | "DEFENSE" | "SHIELD" | "AMOUNT" | "SPEED")
}
