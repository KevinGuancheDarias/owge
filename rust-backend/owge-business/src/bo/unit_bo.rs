//! Port of (the read side of) `UnitBo`. `find_unlocked_by_user` is the
//! `unit_unlocked_change` sync payload (`UnitBo.findAllByUser`): the units the
//! user has unlocked, resolved through the `unlocked_relation ->
//! object_relations(UNIT) -> units` indirection, joined to `unit_types` for the
//! type name.

use crate::bo::ImprovementBo;
use crate::dto::unit::UnitInput;
use crate::dto::{ImprovementDto, ImprovementUnitTypeDto, UnitDto};
use crate::error::{OwgeError, OwgeResult};
use sqlx::MySqlConnection;

#[derive(sqlx::FromRow)]
pub(crate) struct UnitRow {
    id: u16,
    name: String,
    description: Option<String>,
    image_id: Option<u64>,
    image_filename: Option<String>,
    order_number: Option<u16>,
    display_in_requirements: Option<i8>,
    points: Option<u32>,
    time: Option<i32>,
    primary_resource: Option<u32>,
    secondary_resource: Option<u32>,
    energy: Option<u16>,
    type_id: Option<u16>,
    type_name: Option<String>,
    attack: Option<u16>,
    health: Option<u16>,
    shield: Option<u16>,
    charge: Option<u16>,
    is_unique: u8,
    can_fast_explore: i8,
    speed: Option<f64>,
    cloned_improvements: i8,
    bypass_shield: i8,
    is_invisible: i8,
    stored_weight: u32,
    storage_capacity: Option<u32>,
}

impl From<UnitRow> for UnitDto {
    fn from(r: UnitRow) -> Self {
        UnitDto {
            id: r.id,
            name: r.name,
            description: r.description,
            image: r.image_id,
            image_url: r
                .image_filename
                .map(|f| crate::bo::image_store_bo::compute_image_url(&f)),
            order: r.order_number,
            has_to_display_in_requirements: r.display_in_requirements.unwrap_or(0) != 0,
            points: r.points,
            time: r.time.map(|v| v as u64),
            primary_resource: r.primary_resource.map(u64::from),
            secondary_resource: r.secondary_resource.map(u64::from),
            energy: r.energy,
            type_id: r.type_id,
            type_name: r.type_name,
            attack: r.attack,
            health: r.health,
            shield: r.shield,
            charge: r.charge,
            is_unique: r.is_unique != 0,
            can_fast_explore: r.can_fast_explore != 0,
            speed: r.speed,
            cloned_improvements: r.cloned_improvements != 0,
            bypass_shield: r.bypass_shield != 0,
            is_invisible: r.is_invisible != 0,
            stored_weight: r.stored_weight,
            storage_capacity: r.storage_capacity,
        }
    }
}

pub const SELECT_UNIT: &str = "\
    SELECT u.id, u.name, u.description, u.image_id, i.filename AS image_filename, \
           u.order_number, u.display_in_requirements, \
           u.points, u.time, u.primary_resource, u.secondary_resource, u.energy, \
           u.type AS type_id, ut.name AS type_name, u.attack, u.health, u.shield, u.charge, \
           u.is_unique, u.can_fast_explore, u.speed, u.cloned_improvements, u.bypass_shield, \
           u.is_invisible, u.stored_weight, u.storage_capacity \
    FROM units u \
    LEFT JOIN unit_types ut ON ut.id = u.type \
    LEFT JOIN images_store i ON i.id = u.image_id ";

pub struct UnitBo;

impl UnitBo {
    /// `CrudRestServiceTrait.findAll` for the admin unit CRUD — every unit,
    /// ordered by `order_number` (nulls last) then id.
    pub async fn find_all(conn: &mut MySqlConnection) -> OwgeResult<Vec<UnitDto>> {
        let rows = sqlx::query_as::<_, UnitRow>(&format!(
            "{SELECT_UNIT} ORDER BY u.order_number IS NULL, u.order_number, u.id"
        ))
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `WithReadRestServiceTrait.findOneById` for the admin unit CRUD.
    pub async fn find_by_id(conn: &mut MySqlConnection, id: u16) -> OwgeResult<Option<UnitDto>> {
        let row = sqlx::query_as::<_, UnitRow>(&format!("{SELECT_UNIT} WHERE u.id = ?"))
            .bind(id)
            .fetch_optional(&mut *conn)
            .await?;
        Ok(row.map(Into::into))
    }

    /// `CrudRestServiceTrait.saveNew` — insert; `units.id` is AUTO_INCREMENT.
    ///
    /// Mirrors `entityFromDto` + `AdminUnitRestService.beforeSave`: the scalar
    /// fields come straight from the body and the `type`/`speed_impact_group`/
    /// `critical_attack` relations are stored as their resolved ids. `typeId`
    /// is mandatory (`I18N_ERR_UNIT_TYPE_IS_MANDATORY`).
    ///
    /// `units.improvement_id` is `NOT NULL` (every unit owns an `improvements`
    /// row in the Java model, created by the `DtoWithImprovements` save path).
    /// The improvement *write* side is out of scope this pass, so we create an
    /// empty `improvements` row to satisfy the FK and link it.
    /// TODO(improvement-write): populate the improvement from the body
    /// `improvement` object once the improvement engine is ported.
    pub async fn save_new(conn: &mut MySqlConnection, input: &UnitInput) -> OwgeResult<UnitDto> {
        let type_id = input
            .type_id
            .ok_or_else(|| OwgeError::InvalidInput("I18N_ERR_UNIT_TYPE_IS_MANDATORY".into()))?;
        let improvement_id = sqlx::query("INSERT INTO improvements () VALUES ()")
            .execute(&mut *conn)
            .await?
            .last_insert_id() as u16;
        let result = sqlx::query(
            "INSERT INTO units \
                (name, description, image_id, order_number, display_in_requirements, \
                 points, time, primary_resource, secondary_resource, energy, type, \
                 attack, health, shield, charge, is_unique, can_fast_explore, speed, \
                 improvement_id, cloned_improvements, speed_impact_group_id, \
                 critical_attack_id, bypass_shield, is_invisible, stored_weight, \
                 storage_capacity) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        )
        .bind(&input.name)
        .bind(&input.description)
        .bind(input.image)
        .bind(input.order)
        .bind(input.has_to_display_in_requirements as i8)
        .bind(input.points)
        .bind(input.time)
        .bind(input.primary_resource)
        .bind(input.secondary_resource)
        .bind(input.energy)
        .bind(type_id)
        .bind(input.attack)
        .bind(input.health)
        .bind(input.shield)
        .bind(input.charge)
        .bind(input.is_unique as u8)
        .bind(input.can_fast_explore as i8)
        .bind(input.speed)
        .bind(improvement_id)
        .bind(input.cloned_improvements as i8)
        .bind(input.speed_impact_group_id())
        .bind(input.critical_attack_id())
        .bind(input.bypass_shield as i8)
        .bind(input.is_invisible as i8)
        .bind(input.stored_weight)
        .bind(input.storage_capacity)
        .execute(&mut *conn)
        .await?;
        let id = result.last_insert_id() as u16;
        Self::find_by_id(conn, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Unit vanished right after insert".into()))
    }

    /// `CrudRestServiceTrait.saveExisting` — update by id. The `improvement_id`
    /// link is left untouched (improvement writes are out of scope this pass).
    pub async fn save_existing(
        conn: &mut MySqlConnection,
        id: u16,
        input: &UnitInput,
    ) -> OwgeResult<UnitDto> {
        let type_id = input
            .type_id
            .ok_or_else(|| OwgeError::InvalidInput("I18N_ERR_UNIT_TYPE_IS_MANDATORY".into()))?;
        let affected = sqlx::query(
            "UPDATE units SET \
                name = ?, description = ?, image_id = ?, order_number = ?, \
                display_in_requirements = ?, points = ?, time = ?, primary_resource = ?, \
                secondary_resource = ?, energy = ?, type = ?, attack = ?, health = ?, \
                shield = ?, charge = ?, is_unique = ?, can_fast_explore = ?, speed = ?, \
                cloned_improvements = ?, speed_impact_group_id = ?, critical_attack_id = ?, \
                bypass_shield = ?, is_invisible = ?, stored_weight = ?, storage_capacity = ? \
             WHERE id = ?",
        )
        .bind(&input.name)
        .bind(&input.description)
        .bind(input.image)
        .bind(input.order)
        .bind(input.has_to_display_in_requirements as i8)
        .bind(input.points)
        .bind(input.time)
        .bind(input.primary_resource)
        .bind(input.secondary_resource)
        .bind(input.energy)
        .bind(type_id)
        .bind(input.attack)
        .bind(input.health)
        .bind(input.shield)
        .bind(input.charge)
        .bind(input.is_unique as u8)
        .bind(input.can_fast_explore as i8)
        .bind(input.speed)
        .bind(input.cloned_improvements as i8)
        .bind(input.speed_impact_group_id())
        .bind(input.critical_attack_id())
        .bind(input.bypass_shield as i8)
        .bind(input.is_invisible as i8)
        .bind(input.stored_weight)
        .bind(input.storage_capacity)
        .bind(id)
        .execute(&mut *conn)
        .await?
        .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No unit with id {id}")));
        }
        Self::find_by_id(conn, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No unit with id {id}")))
    }

    /// `WithDeleteRestServiceTrait.delete`.
    pub async fn delete(conn: &mut MySqlConnection, id: u16) -> OwgeResult<()> {
        sqlx::query("DELETE FROM units WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?;
        Ok(())
    }

    /// `CrudWithImprovements` `GET {id}/improvement`.
    pub async fn find_improvement(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<ImprovementDto> {
        ImprovementBo::find_for_entity(conn, "units", id).await
    }

    /// `CrudWithImprovements` `PUT {id}/improvement`.
    pub async fn save_improvement(
        conn: &mut MySqlConnection,
        id: u16,
        dto: &ImprovementDto,
    ) -> OwgeResult<ImprovementDto> {
        ImprovementBo::save_for_entity(conn, "units", id, dto).await
    }

    /// `GET {id}/improvement/unitTypeImprovements`.
    pub async fn find_unit_type_improvements(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<Vec<ImprovementUnitTypeDto>> {
        ImprovementBo::find_unit_type_improvements_for_entity(conn, "units", id).await
    }

    /// `POST {id}/improvement/unitTypeImprovements`.
    pub async fn add_unit_type_improvement(
        conn: &mut MySqlConnection,
        id: u16,
        dto: &ImprovementUnitTypeDto,
    ) -> OwgeResult<ImprovementUnitTypeDto> {
        ImprovementBo::add_unit_type_improvement_for_entity(conn, "units", id, dto).await
    }

    /// `DELETE {id}/improvement/unitTypeImprovements/{utiId}`.
    pub async fn delete_unit_type_improvement(
        conn: &mut MySqlConnection,
        id: u16,
        unit_type_improvement_id: u16,
    ) -> OwgeResult<()> {
        ImprovementBo::delete_unit_type_improvement_for_entity(
            conn,
            "units",
            id,
            unit_type_improvement_id,
        )
        .await
    }

    /// `UnitBo.findUsedCriticalAttack(int unitId)` — the critical attack id the
    /// unit actually uses: its own `critical_attack_id` if set, otherwise the
    /// first one found walking up its unit type's parent chain
    /// (`CriticalAttackBo.findUsedCriticalAttack(UnitType)`). Returns `None` when
    /// neither the unit nor any ancestor type defines one.
    ///
    /// `findByIdOrDie(unitId)` — a missing unit is a `NotFound`.
    pub async fn find_used_critical_attack(
        conn: &mut MySqlConnection,
        unit_id: u16,
    ) -> OwgeResult<Option<u16>> {
        let row: Option<(Option<u16>, Option<u16>)> =
            sqlx::query_as("SELECT critical_attack_id, type FROM units WHERE id = ?")
                .bind(unit_id)
                .fetch_optional(&mut *conn)
                .await?;
        let (unit_critical, type_id) =
            row.ok_or_else(|| OwgeError::NotFound(format!("No unit with id {unit_id}")))?;
        if let Some(critical) = unit_critical {
            return Ok(Some(critical));
        }
        // Walk the unit type parent chain (units.type may be NULL).
        let mut current = type_id;
        while let Some(tid) = current {
            let row: Option<(Option<u16>, Option<u16>)> = sqlx::query_as(
                "SELECT critical_attack_id, parent_type FROM unit_types WHERE id = ?",
            )
            .bind(tid)
            .fetch_optional(&mut *conn)
            .await?;
            let Some((type_critical, parent_type)) = row else {
                break;
            };
            if let Some(critical) = type_critical {
                return Ok(Some(critical));
            }
            current = parent_type;
        }
        Ok(None)
    }

    /// `AdminUnitRestService.unsetCriticalAttack` — clears the critical attack FK.
    pub async fn unset_critical_attack(conn: &mut MySqlConnection, id: u16) -> OwgeResult<()> {
        let affected = sqlx::query("UPDATE units SET critical_attack_id = NULL WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?
            .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No unit with id {id}")));
        }
        Ok(())
    }
}
