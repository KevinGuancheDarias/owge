//! Port of (the read side of) `RequirementBo` — specifically
//! `findFactionUnitLevelRequirements`, the `unit_requirements_change` sync
//! payload.
//!
//! For the player's faction it finds the units gated by a `BEEN_RACE`
//! requirement on that faction (and flagged `display_in_requirements`), and for
//! each returns the `UPGRADE_LEVEL` requirements: which upgrade at which level
//! must be reached. Mirrors `RequirementBo.createUnitUpgradeRequirements`.

use sqlx::{Connection, MySqlConnection};

use crate::bo::ObjectRelationBo;
use crate::bo::realtime_emitter::RequirementEmit;
use crate::dto::requirement::{UnitUpgradeRequirement, UnitWithRequirementInfo};
use crate::dto::requirement_information::{
    ObjectRelationDto, RequirementDto, RequirementInformationDto, RequirementInformationInput,
};
use crate::dto::{UnitDto, UpgradeDto};
use crate::error::{OwgeError, OwgeResult};
use crate::model::object_relation::object_enum;
use crate::model::user_storage::UserStorage;

/// The valid `RequirementTypeEnum` codes (`requirements.code`).
const VALID_REQUIREMENT_CODES: &[&str] = &[
    "HAVE_SPECIAL_LOCATION",
    "HAVE_UNIT",
    "BEEN_RACE",
    "UPGRADE_LEVEL",
    "WORST_PLAYER",
    "UNIT_AMOUNT",
    "HOME_GALAXY",
    "HAVE_SPECIAL_AVAILABLE",
    "HAVE_SPECIAL_ENABLED",
    "UPGRADE_LEVEL_LOWER_THAN",
];

#[derive(sqlx::FromRow)]
struct UnitReqRow {
    relation_id: u16,
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

impl UnitReqRow {
    fn to_unit_dto(&self) -> UnitDto {
        UnitDto {
            id: self.id,
            name: self.name.clone(),
            description: self.description.clone(),
            image: self.image_id,
            image_url: self
                .image_filename
                .clone()
                .map(|f| crate::bo::image_store_bo::compute_image_url(&f)),
            order: self.order_number,
            has_to_display_in_requirements: self.display_in_requirements.unwrap_or(0) != 0,
            points: self.points,
            time: self.time.map(|v| v as u64),
            primary_resource: self.primary_resource.map(u64::from),
            secondary_resource: self.secondary_resource.map(u64::from),
            energy: self.energy,
            type_id: self.type_id,
            type_name: self.type_name.clone(),
            attack: self.attack,
            health: self.health,
            shield: self.shield,
            charge: self.charge,
            is_unique: self.is_unique != 0,
            can_fast_explore: self.can_fast_explore != 0,
            speed: self.speed,
            cloned_improvements: self.cloned_improvements != 0,
            bypass_shield: self.bypass_shield != 0,
            is_invisible: self.is_invisible != 0,
            stored_weight: self.stored_weight,
            storage_capacity: self.storage_capacity,
            improvement: None,
            speed_impact_group: None,
            attack_rule: None,
            critical_attack: None,
        }
    }
}

#[derive(sqlx::FromRow)]
struct UpgradeReqRow {
    level: Option<i32>,
    up_id: u16,
    up_name: String,
    up_description: Option<String>,
    up_image_id: Option<u64>,
    up_image_filename: Option<String>,
    up_points: i32,
    up_time: i32,
    up_primary_resource: i32,
    up_secondary_resource: i32,
    up_type_id: Option<u16>,
    up_type_name: Option<String>,
    up_level_effect: f32,
    up_cloned_improvements: i8,
    up_order: Option<u16>,
}

impl From<UpgradeReqRow> for UnitUpgradeRequirement {
    fn from(r: UpgradeReqRow) -> Self {
        UnitUpgradeRequirement {
            level: r.level.unwrap_or(0) as i64,
            upgrade: UpgradeDto {
                id: r.up_id,
                name: r.up_name,
                description: r.up_description,
                image: r.up_image_id,
                image_url: r
                    .up_image_filename
                    .map(|f| crate::bo::image_store_bo::compute_image_url(&f)),
                order: r.up_order, // `upgrades.order_number` (smallint unsigned)
                points: r.up_points,
                time: r.up_time as i64,
                primary_resource: r.up_primary_resource,
                secondary_resource: r.up_secondary_resource,
                type_id: r.up_type_id,
                type_name: r.up_type_name,
                level_effect: r.up_level_effect,
                cloned_improvements: r.up_cloned_improvements != 0,
                // Java leaves these lazy/uninitialized on the requirement read
                // path (Hibernate doesn't init them), so they are omitted here.
                improvement: None,
                requirements: None,
            },
            // Java never sets `reached` on this read path; always default false.
            reached: false,
        }
    }
}

#[derive(sqlx::FromRow)]
struct RequirementInfoRow {
    id: i16,
    rel_id: u16,
    object_description: String,
    reference_id: i16,
    req_id: i16,
    code: String,
    description: String,
    second_value: Option<i64>,
    third_value: Option<i64>,
}

impl From<RequirementInfoRow> for RequirementInformationDto {
    fn from(r: RequirementInfoRow) -> Self {
        RequirementInformationDto {
            id: r.id,
            relation: ObjectRelationDto {
                id: r.rel_id,
                object_code: r.object_description,
                reference_id: r.reference_id,
            },
            requirement: RequirementDto {
                id: r.req_id,
                code: r.code,
                description: r.description,
            },
            second_value: r.second_value,
            third_value: r.third_value,
        }
    }
}

pub struct RequirementBo;

impl RequirementBo {
    /// `RequirementInformationBo.findRequirements(objectEnum, referenceId)` — the
    /// admin `GET {id}/requirements` sub-resource: every requirement attached to
    /// the relation for a concrete entity.
    pub async fn find_requirements(
        conn: &mut MySqlConnection,
        object_description: &str,
        reference_id: i16,
    ) -> OwgeResult<Vec<RequirementInformationDto>> {
        let rows = sqlx::query_as::<_, RequirementInfoRow>(
            "SELECT ri.id, o.id AS rel_id, o.object_description, o.reference_id, \
                    r.id AS req_id, r.code, r.description, ri.second_value, ri.third_value \
             FROM requirements_information ri \
             JOIN object_relations o ON o.id = ri.relation_id \
             JOIN requirements r ON r.id = ri.requirement_id \
             WHERE o.object_description = ? AND o.reference_id = ? \
             ORDER BY ri.id",
        )
        .bind(object_description)
        .bind(reference_id)
        .fetch_all(&mut *conn)
        .await?;
        Ok(rows.into_iter().map(Into::into).collect())
    }

    /// `RequirementBo.addRequirementFromDto` — the `POST {id}/requirements` (and
    /// `POST {id}/requirement-group/{groupId}/requirement`) sub-resource: persist
    /// a `requirements_information` row against the relation `(object_code,
    /// reference_id)`, find-or-creating the relation. Validation mirrors the Java
    /// `ValidationUtil` chain (requirement code valid, second value present).
    ///
    /// NOTE: the trait method does **not** re-run the unlock engine
    /// (`triggerRelationChanged`); only the unused `RequirementInformationBo.save`
    /// does, so unlocks are intentionally not recomputed here. M4 cache eviction
    /// (`REQUIREMENT_INFORMATION_CACHE_TAG`) is deferred — see the caller TODOs.
    pub async fn add_requirement_from_dto(
        conn: &mut MySqlConnection,
        object_code: &str,
        reference_id: i16,
        input: &RequirementInformationInput,
    ) -> OwgeResult<RequirementInformationDto> {
        let code = input
            .requirement
            .as_ref()
            .and_then(|r| r.code.clone())
            .ok_or_else(|| OwgeError::InvalidInput("requirement.code must not be null".into()))?;
        if !VALID_REQUIREMENT_CODES.contains(&code.as_str()) {
            return Err(OwgeError::InvalidInput(format!(
                "requirement.code is not a valid requirement type: {code}"
            )));
        }
        if input.second_value.is_none() {
            return Err(OwgeError::InvalidInput(
                "secondValue must not be null".into(),
            ));
        }
        if reference_id <= 0 {
            return Err(OwgeError::InvalidInput(
                "relation.referenceId must be a positive number".into(),
            ));
        }

        let mut tx = conn.begin().await?;
        let relation_id =
            ObjectRelationBo::find_object_relation_or_create(&mut tx, object_code, reference_id)
                .await?;
        let requirement_id: i16 =
            sqlx::query_scalar("SELECT id FROM requirements WHERE code = ? LIMIT 1")
                .bind(&code)
                .fetch_optional(&mut *tx)
                .await?
                .ok_or_else(|| {
                    OwgeError::InvalidInput(format!("No requirement registered with code {code}"))
                })?;
        let result = sqlx::query(
            "INSERT INTO requirements_information (relation_id, requirement_id, second_value, third_value) \
             VALUES (?, ?, ?, ?)",
        )
        .bind(relation_id)
        .bind(requirement_id)
        .bind(input.second_value.map(|v| v as i32))
        .bind(input.third_value.map(|v| v as i32))
        .execute(&mut *tx)
        .await?;
        let new_id = result.last_insert_id() as i16;
        tx.commit().await?;

        // Re-read the saved row to build the response DTO (with the relation +
        // requirement objects, matching `dtoUtilService.dtoFromEntity`).
        let row = sqlx::query_as::<_, RequirementInfoRow>(
            "SELECT ri.id, o.id AS rel_id, o.object_description, o.reference_id, \
                    r.id AS req_id, r.code, r.description, ri.second_value, ri.third_value \
             FROM requirements_information ri \
             JOIN object_relations o ON o.id = ri.relation_id \
             JOIN requirements r ON r.id = ri.requirement_id \
             WHERE ri.id = ?",
        )
        .bind(new_id)
        .fetch_one(&mut *conn)
        .await?;
        Ok(row.into())
    }

    /// `RequirementInformationBo.delete` — `DELETE {id}/requirements/{reqInfoId}`:
    /// remove the `requirements_information` row by id.
    ///
    /// NOTE: the Java delete additionally `triggerRelationChanged` (recompute
    /// unlocks) and evicts caches; those are M4 and intentionally deferred, since
    /// the corresponding add path also does not recompute unlocks.
    pub async fn delete_requirement_information(
        conn: &mut MySqlConnection,
        id: i16,
    ) -> OwgeResult<()> {
        sqlx::query("DELETE FROM requirements_information WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?;
        // No-op in the Rust port: the Java REQUIREMENT_INFORMATION_CACHE_TAG and
        // REQUIREMENT_GROUP_CACHE_TAG taggable-caches (`RequirementInformationBo.delete`
        // calls `taggableCacheManager.evictByCacheTag(...)` for both) are not
        // replicated — requirement information is recomputed on demand, so there
        // is nothing to evict. The corresponding `triggerRelationChanged` (unlock
        // recomputation) is also intentionally deferred on the delete path to
        // match the symmetry with the add path (see the note above this method).
        Ok(())
    }

    /// `findFactionUnitLevelRequirements(factionBo.findByUser(userId))`.
    pub async fn find_faction_unit_level_requirements(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<UnitWithRequirementInfo>> {
        let faction_id: Option<u16> =
            sqlx::query_scalar("SELECT faction FROM user_storage WHERE id = ?")
                .bind(user_id)
                .fetch_optional(&mut *conn)
                .await?;
        let Some(faction_id) = faction_id else {
            return Ok(Vec::new());
        };

        // Units gated by BEEN_RACE on this faction, flagged for requirement display.
        let units = sqlx::query_as::<_, UnitReqRow>(
            "SELECT o.id AS relation_id, u.id, u.name, u.description, u.image_id, \
                    img.filename AS image_filename, u.order_number, \
                    u.display_in_requirements, u.points, u.time, u.primary_resource, \
                    u.secondary_resource, u.energy, u.type AS type_id, ut.name AS type_name, \
                    u.attack, u.health, u.shield, u.charge, u.is_unique, u.can_fast_explore, \
                    u.speed, u.cloned_improvements, u.bypass_shield, u.is_invisible, \
                    u.stored_weight, u.storage_capacity \
             FROM object_relations o \
             JOIN units u ON u.id = o.reference_id \
             LEFT JOIN unit_types ut ON ut.id = u.type \
             LEFT JOIN images_store img ON img.id = u.image_id \
             JOIN requirements_information ri ON ri.relation_id = o.id \
             JOIN requirements r ON r.id = ri.requirement_id AND r.code = 'BEEN_RACE' \
             WHERE o.object_description = ? AND ri.second_value = ? \
               AND COALESCE(u.display_in_requirements, 0) = 1 \
             GROUP BY o.id \
             ORDER BY u.order_number IS NULL, u.order_number, u.id",
        )
        .bind(object_enum::UNIT)
        .bind(faction_id)
        .fetch_all(&mut *conn)
        .await?;

        let mut result = Vec::with_capacity(units.len());
        for unit in units {
            let reqs = sqlx::query_as::<_, UpgradeReqRow>(
                "SELECT ri.third_value AS level, up.id AS up_id, up.name AS up_name, \
                        up.description AS up_description, up.image_id AS up_image_id, \
                        img.filename AS up_image_filename, up.points AS up_points, \
                        up.time AS up_time, up.primary_resource AS up_primary_resource, \
                        up.secondary_resource AS up_secondary_resource, up.type AS up_type_id, \
                        ut.name AS up_type_name, up.level_effect AS up_level_effect, \
                        up.cloned_improvements AS up_cloned_improvements, \
                        up.order_number AS up_order \
                 FROM requirements_information ri \
                 JOIN requirements r ON r.id = ri.requirement_id AND r.code = 'UPGRADE_LEVEL' \
                 JOIN upgrades up ON up.id = ri.second_value \
                 LEFT JOIN upgrade_types ut ON ut.id = up.type \
                 LEFT JOIN images_store img ON img.id = up.image_id \
                 WHERE ri.relation_id = ?",
            )
            .bind(unit.relation_id)
            .fetch_all(&mut *conn)
            .await?;
            result.push(UnitWithRequirementInfo {
                unit: unit.to_unit_dto(),
                requirements: reqs.into_iter().map(Into::into).collect(),
            });
        }
        Ok(result)
    }

    // -----------------------------------------------------------------------
    // Write-side triggers (M3): re-evaluate unlocks when unit/upgrade counts
    // change. Ports of `RequirementBo.triggerLevelUpCompleted` /
    // `triggerUnitBuildCompletedOrKilled` / `triggerUnitAmountChanged`. They run
    // inside the caller's transaction on a borrowed connection.
    //
    // NOTE: the Java `triggerUnit*` paths wrap their work in
    // `userPlanetLockService.runLockedForUser(user, ...)`. In the Rust port the
    // callers (build-unit completion, mission registration, attack resolution)
    // already hold the relevant planet/user lock for the whole transaction, so
    // the re-lock is a no-op here and is intentionally omitted (matching the
    // existing M3 lock handling elsewhere in the port).
    // -----------------------------------------------------------------------

    /// `RequirementBo.triggerLevelUpCompleted(user, upgradeId)` — re-evaluate
    /// every relation gated by an `UPGRADE_LEVEL` requirement on `upgrade_id`.
    pub async fn trigger_level_up_completed(
        conn: &mut MySqlConnection,
        user: &UserStorage,
        upgrade_id: i64,
        emits: &mut Vec<RequirementEmit>,
    ) -> OwgeResult<()> {
        let relations =
            find_relations_with_code_and_second_value(conn, UPGRADE_LEVEL, upgrade_id).await?;
        process_relation_list(conn, &relations, user, emits).await
    }

    /// `RequirementBo.triggerUnitBuildCompletedOrKilled(user, unit)` — re-evaluate
    /// the `HAVE_UNIT` relations for this unit, then the `UNIT_AMOUNT` ones
    /// (`triggerUnitAmountChanged`). Used after a unit is built, removed, or killed.
    pub async fn trigger_unit_build_completed_or_killed(
        conn: &mut MySqlConnection,
        user: &UserStorage,
        unit_id: i64,
        emits: &mut Vec<RequirementEmit>,
    ) -> OwgeResult<()> {
        let relations = find_relations_with_code_and_second_value(conn, HAVE_UNIT, unit_id).await?;
        process_relation_list(conn, &relations, user, emits).await?;
        Self::trigger_unit_amount_changed(conn, user, unit_id, emits).await
    }

    /// `RequirementBo.triggerUnitAmountChanged(user, unit)` — re-evaluate the
    /// `UNIT_AMOUNT` relations on this unit whose threshold (`third_value`) is at
    /// least the user's *current* total of the unit (the relations near the
    /// unlock boundary). Mirrors
    /// `findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual`.
    pub async fn trigger_unit_amount_changed(
        conn: &mut MySqlConnection,
        user: &UserStorage,
        unit_id: i64,
        emits: &mut Vec<RequirementEmit>,
    ) -> OwgeResult<()> {
        let count = count_units(conn, user.id, unit_id).await?;
        let relations = find_relations_unit_amount(conn, unit_id, count).await?;
        process_relation_list(conn, &relations, user, emits).await
    }

    /// `RequirementBo.triggerSpecialLocation(user, specialLocation)` — re-evaluate
    /// every relation gated by a `HAVE_SPECIAL_LOCATION` requirement whose
    /// `second_value` equals this special-location id (the special location was
    /// gained or lost by the user — conquest old-owner / planet-leave). Mirrors
    /// `processRelationList(findByRequirementTypeAndSecondValue(HAVE_SPECIAL_LOCATION,
    /// specialLocation.getId()), user)`.
    pub async fn trigger_special_location(
        conn: &mut MySqlConnection,
        user: &UserStorage,
        special_location_id: i64,
        emits: &mut Vec<RequirementEmit>,
    ) -> OwgeResult<()> {
        let relations = find_relations_with_code_and_second_value(
            conn,
            HAVE_SPECIAL_LOCATION,
            special_location_id,
        )
        .await?;
        process_relation_list(conn, &relations, user, emits).await
    }

    /// `RequirementBo.triggerTimeSpecialStateChange(user, timeSpecial)` —
    /// re-evaluate the `HAVE_SPECIAL_ENABLED` relations for this time special
    /// after it is activated (or its effect ends). Mirrors
    /// `processRelationList(findByRequirementTypeAndSecondValue(HAVE_SPECIAL_ENABLED,
    /// timeSpecialId), user)`.
    pub async fn trigger_time_special_state_change(
        conn: &mut MySqlConnection,
        user: &UserStorage,
        time_special_id: i64,
        emits: &mut Vec<RequirementEmit>,
    ) -> OwgeResult<()> {
        let relations =
            find_relations_with_code_and_second_value(conn, HAVE_SPECIAL_ENABLED, time_special_id)
                .await?;
        process_relation_list(conn, &relations, user, emits).await
    }
}

// ---------------------------------------------------------------------------
// Trigger engine — a port of the write side of `RequirementBo`
// (`processRelationList` / `processRelation` / `checkRequirementsAreMet` /
// `registerObtainedRelation` / `unregisterLostRelation`) plus the
// `ObjectRelationBo` relation-lookup queries the triggers above need.
//
// This mirrors the M2 engine in `requirement_engine.rs` (whose own
// `process_relation_list` is private); the logic is kept byte-for-byte
// equivalent so both entry families behave identically. The websocket
// `*_unlocked_change` emissions and internal requirement-event emitter remain
// M4 and are marked at their call sites, exactly as in `requirement_engine.rs`.
// ---------------------------------------------------------------------------

// Requirement codes (the `requirements.code` column / `RequirementTypeEnum`).
const UPGRADE_LEVEL: &str = "UPGRADE_LEVEL";
const HAVE_UNIT: &str = "HAVE_UNIT";
const UNIT_AMOUNT: &str = "UNIT_AMOUNT";
const BEEN_RACE: &str = "BEEN_RACE";
const HOME_GALAXY: &str = "HOME_GALAXY";
const HAVE_SPECIAL_LOCATION: &str = "HAVE_SPECIAL_LOCATION";
const HAVE_SPECIAL_AVAILABLE: &str = "HAVE_SPECIAL_AVAILABLE";
const HAVE_SPECIAL_ENABLED: &str = "HAVE_SPECIAL_ENABLED";
const UPGRADE_LEVEL_LOWER_THAN: &str = "UPGRADE_LEVEL_LOWER_THAN";

/// An `object_relations` row (exact column types).
#[derive(Clone, sqlx::FromRow)]
struct TriggerRelationRow {
    id: u16,
    object_description: String,
    reference_id: i16,
}

/// A `requirements_information` row joined with its `requirements.code`.
#[derive(sqlx::FromRow)]
struct TriggerReqRow {
    code: String,
    second_value: Option<i32>,
    third_value: Option<i32>,
}

/// `ObjectRelationBo.findByRequirementTypeAndSecondValue` — relations carrying a
/// requirement of `code` whose `second_value` equals `second_value`.
async fn find_relations_with_code_and_second_value(
    conn: &mut MySqlConnection,
    code: &str,
    second_value: i64,
) -> OwgeResult<Vec<TriggerRelationRow>> {
    let rows = sqlx::query_as::<_, TriggerRelationRow>(
        "SELECT DISTINCT o.id, o.object_description, o.reference_id \
         FROM object_relations o \
         JOIN requirements_information ri ON ri.relation_id = o.id \
         JOIN requirements r ON r.id = ri.requirement_id \
         WHERE r.code = ? AND ri.second_value = ?",
    )
    .bind(code)
    .bind(second_value as i32)
    .fetch_all(&mut *conn)
    .await?;
    Ok(rows)
}

/// `ObjectRelationBo.findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual`
/// for `UNIT_AMOUNT` — relations on this unit whose threshold is `>= count`.
async fn find_relations_unit_amount(
    conn: &mut MySqlConnection,
    unit_id: i64,
    count: i64,
) -> OwgeResult<Vec<TriggerRelationRow>> {
    let rows = sqlx::query_as::<_, TriggerRelationRow>(
        "SELECT DISTINCT o.id, o.object_description, o.reference_id \
         FROM object_relations o \
         JOIN requirements_information ri ON ri.relation_id = o.id \
         JOIN requirements r ON r.id = ri.requirement_id \
         WHERE r.code = ? AND ri.second_value = ? AND ri.third_value >= ?",
    )
    .bind(UNIT_AMOUNT)
    .bind(unit_id as i32)
    .bind(count)
    .fetch_all(&mut *conn)
    .await?;
    Ok(rows)
}

/// `RequirementBo.processRelationList`.
async fn process_relation_list(
    conn: &mut MySqlConnection,
    relations: &[TriggerRelationRow],
    user: &UserStorage,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    let mut affected_masters: Vec<u16> = Vec::new();
    for relation in relations {
        let is_slave_or_has_no_slaves;
        if relation.object_description == object_enum::REQUIREMENT_GROUP {
            if let Some(master_id) = find_master_id_by_slave(conn, relation.id).await? {
                if !affected_masters.contains(&master_id) {
                    affected_masters.push(master_id);
                }
            } else {
                tracing::warn!("Orphan group with id {}", relation.id);
            }
            is_slave_or_has_no_slaves = true;
        } else {
            is_slave_or_has_no_slaves = !exists_by_master(conn, relation.id).await?;
        }
        if is_slave_or_has_no_slaves {
            process_relation(conn, relation, user, emits).await?;
        }
    }
    for master_id in affected_masters {
        let Some(master) = find_relation_by_id(conn, master_id).await? else {
            continue;
        };
        let slave_ids = find_slave_ids_by_master(conn, master_id).await?;
        let mut any_slave_unlocked = false;
        for slave_id in slave_ids {
            if is_unlocked(conn, user.id, slave_id).await? {
                any_slave_unlocked = true;
                break;
            }
        }
        if any_slave_unlocked {
            register_obtained_relation(conn, &master, user, emits).await?;
        } else {
            unregister_lost_relation(conn, &master, user, emits).await?;
        }
    }
    Ok(())
}

/// `RequirementBo.processRelation`.
async fn process_relation(
    conn: &mut MySqlConnection,
    relation: &TriggerRelationRow,
    user: &UserStorage,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    if check_requirements_are_met(conn, relation.id, user).await? {
        register_obtained_relation(conn, relation, user, emits).await
    } else {
        unregister_lost_relation(conn, relation, user, emits).await
    }
}

/// `RequirementBo.checkRequirementsAreMet`.
async fn check_requirements_are_met(
    conn: &mut MySqlConnection,
    relation_id: u16,
    user: &UserStorage,
) -> OwgeResult<bool> {
    let reqs = sqlx::query_as::<_, TriggerReqRow>(
        "SELECT r.code, ri.second_value, ri.third_value \
         FROM requirements_information ri \
         JOIN requirements r ON r.id = ri.requirement_id \
         WHERE ri.relation_id = ?",
    )
    .bind(relation_id)
    .fetch_all(&mut *conn)
    .await?;
    for req in reqs {
        let second = req.second_value.unwrap_or(0);
        let third = req.third_value.unwrap_or(0) as i64;
        let met = match req.code.as_str() {
            UPGRADE_LEVEL => upgrade_level(conn, user.id, second).await? >= third,
            HAVE_UNIT => is_built_unit(conn, user.id, second).await?,
            UNIT_AMOUNT => count_units(conn, user.id, second as i64).await? >= third,
            BEEN_RACE => user.faction as i32 == second,
            HOME_GALAXY => home_galaxy_id(conn, user.home_planet).await? == Some(second),
            HAVE_SPECIAL_LOCATION => special_location_owned(conn, user.id, second).await?,
            HAVE_SPECIAL_AVAILABLE => time_special_unlocked(conn, user.id, second).await?,
            HAVE_SPECIAL_ENABLED => time_special_active(conn, user.id, second).await?,
            UPGRADE_LEVEL_LOWER_THAN => {
                let (exists, level) = obtained_upgrade_level(conn, user.id, second).await?;
                exists && level < third
            }
            other => {
                return Err(OwgeError::Common(format!(
                    "Not implemented requirement type: {other}"
                )));
            }
        };
        if !met {
            return Ok(false);
        }
    }
    Ok(true)
}

/// `RequirementBo.registerObtainedRelation`.
async fn register_obtained_relation(
    conn: &mut MySqlConnection,
    relation: &TriggerRelationRow,
    user: &UserStorage,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    if find_unlocked_id(conn, user.id, relation.id)
        .await?
        .is_some()
    {
        return Ok(()); // already unlocked: do nothing (matches Java)
    }
    sqlx::query("INSERT INTO unlocked_relation (user_id, relation_id) VALUES (?, ?)")
        .bind(user.id)
        .bind(relation.id)
        .execute(&mut *conn)
        .await?;
    // Mirrors `requirement_engine::register_obtained_relation` (Java switch):
    // UPGRADE → obtained_upgrades; UNIT/TIME_SPECIAL/SPEED_IMPACT_GROUP → emit.
    match relation.object_description.as_str() {
        object_enum::UPGRADE => {
            let upgrade_id = relation.reference_id as u16;
            if obtained_upgrade_exists(conn, user.id, upgrade_id).await? {
                set_obtained_upgrade_available(conn, user.id, upgrade_id, true).await?;
            } else {
                register_obtained_upgrade(conn, user.id, upgrade_id).await?;
            }
        }
        object_enum::UNIT => emits.push(RequirementEmit::UnitUnlocked(user.id)),
        object_enum::TIME_SPECIAL => emits.push(RequirementEmit::TimeSpecialUnlocked(user.id)),
        object_enum::SPEED_IMPACT_GROUP => {
            emits.push(RequirementEmit::SpeedImpactGroupUnlocked(user.id))
        }
        _ => {}
    }
    Ok(())
}

/// `RequirementBo.unregisterLostRelation`.
async fn unregister_lost_relation(
    conn: &mut MySqlConnection,
    relation: &TriggerRelationRow,
    user: &UserStorage,
    emits: &mut Vec<RequirementEmit>,
) -> OwgeResult<()> {
    let unlocked_id = find_unlocked_id(conn, user.id, relation.id).await?;
    if let Some(id) = unlocked_id {
        sqlx::query("DELETE FROM unlocked_relation WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?;
    }
    // Java if/else chain (see `requirement_engine::unregister_lost_relation`).
    let upgrade_exists = relation.object_description == object_enum::UPGRADE
        && obtained_upgrade_exists(conn, user.id, relation.reference_id as u16).await?;
    if upgrade_exists {
        set_obtained_upgrade_available(conn, user.id, relation.reference_id as u16, false).await?;
    } else if relation.object_description == object_enum::SPEED_IMPACT_GROUP {
        emits.push(RequirementEmit::SpeedImpactGroupUnlocked(user.id));
    } else if unlocked_id.is_some() {
        if relation.object_description == object_enum::UNIT {
            emits.push(RequirementEmit::UnitUnlocked(user.id));
        } else {
            emits.push(RequirementEmit::TimeSpecialUnlocked(user.id));
        }
    }
    // doNotifyLostRelation listener #1 (ActiveTimeSpecialBo.relationLost) — see the
    // matching note in `requirement_engine::unregister_lost_relation`. Box::pin
    // breaks the deactivate → trigger_time_special_state_change → unregister recursion.
    if relation.object_description == object_enum::TIME_SPECIAL {
        if let Some(active_id) =
            find_active_time_special_id(conn, user.id, relation.reference_id).await?
        {
            Box::pin(crate::bo::ActiveTimeSpecialBo::deactivate_in_tx(
                conn, active_id, emits,
            ))
            .await?;
        }
        // doNotifyLostRelation listener #2 (TemporalUnitScheduleListener.relationLost):
        // remove the special's temporal units, deferred post-commit (it pins its own
        // connection + planet locks). `relation.id` = the `object_relations` id stored
        // on the temporal-info rows.
        emits.push(RequirementEmit::TemporalUnitsRelationLost(relation.id));
    }
    Ok(())
}

/// The ACTIVE `active_time_specials.id` for `user_id` + `time_special_id`, if any
/// (`ActiveTimeSpecialBo.relationLost` `findOneByTimeSpecialIdAndUserId` + `ACTIVE`).
async fn find_active_time_special_id(
    conn: &mut MySqlConnection,
    user_id: i32,
    time_special_id: i16,
) -> OwgeResult<Option<u64>> {
    let id = sqlx::query_scalar::<_, u64>(
        "SELECT id FROM active_time_specials \
          WHERE user_id = ? AND time_special_id = ? AND state = 'ACTIVE' LIMIT 1",
    )
    .bind(user_id)
    .bind(time_special_id as u16)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(id)
}

// --- Query helpers (each maps a Java repository method) ---------------------

async fn find_relation_by_id(
    conn: &mut MySqlConnection,
    id: u16,
) -> OwgeResult<Option<TriggerRelationRow>> {
    let row = sqlx::query_as::<_, TriggerRelationRow>(
        "SELECT id, object_description, reference_id FROM object_relations WHERE id = ?",
    )
    .bind(id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(row)
}

async fn find_master_id_by_slave(
    conn: &mut MySqlConnection,
    slave_id: u16,
) -> OwgeResult<Option<u16>> {
    let id = sqlx::query_scalar::<_, u16>(
        "SELECT master_relation_id FROM object_relation__object_relation \
         WHERE slave_relation_id = ? LIMIT 1",
    )
    .bind(slave_id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(id)
}

async fn exists_by_master(conn: &mut MySqlConnection, master_id: u16) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM object_relation__object_relation WHERE master_relation_id = ?",
    )
    .bind(master_id)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}

async fn find_slave_ids_by_master(
    conn: &mut MySqlConnection,
    master_id: u16,
) -> OwgeResult<Vec<u16>> {
    let ids = sqlx::query_scalar::<_, u16>(
        "SELECT slave_relation_id FROM object_relation__object_relation WHERE master_relation_id = ?",
    )
    .bind(master_id)
    .fetch_all(&mut *conn)
    .await?;
    Ok(ids)
}

async fn find_unlocked_id(
    conn: &mut MySqlConnection,
    user_id: i32,
    relation_id: u16,
) -> OwgeResult<Option<i64>> {
    let id = sqlx::query_scalar::<_, i64>(
        "SELECT id FROM unlocked_relation WHERE user_id = ? AND relation_id = ? LIMIT 1",
    )
    .bind(user_id)
    .bind(relation_id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(id)
}

async fn is_unlocked(
    conn: &mut MySqlConnection,
    user_id: i32,
    relation_id: u16,
) -> OwgeResult<bool> {
    Ok(find_unlocked_id(conn, user_id, relation_id)
        .await?
        .is_some())
}

async fn upgrade_level(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: i32,
) -> OwgeResult<i64> {
    let level = sqlx::query_scalar::<_, i16>(
        "SELECT level FROM obtained_upgrades WHERE user_id = ? AND upgrade_id = ? LIMIT 1",
    )
    .bind(user_id)
    .bind(upgrade_id as u16)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(level.unwrap_or(0) as i64)
}

async fn obtained_upgrade_level(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: i32,
) -> OwgeResult<(bool, i64)> {
    let level = sqlx::query_scalar::<_, i16>(
        "SELECT level FROM obtained_upgrades WHERE user_id = ? AND upgrade_id = ? LIMIT 1",
    )
    .bind(user_id)
    .bind(upgrade_id as u16)
    .fetch_optional(&mut *conn)
    .await?;
    Ok((level.is_some(), level.unwrap_or(0) as i64))
}

async fn is_built_unit(conn: &mut MySqlConnection, user_id: i32, unit_id: i32) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM obtained_units ou \
         LEFT JOIN missions m ON m.id = ou.mission_id \
         LEFT JOIN mission_types mt ON mt.id = m.type \
         WHERE ou.user_id = ? AND ou.unit_id = ? \
           AND (ou.mission_id IS NULL OR mt.code != 'BUILD_UNIT')",
    )
    .bind(user_id)
    .bind(unit_id as u16)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}

async fn count_units(conn: &mut MySqlConnection, user_id: i32, unit_id: i64) -> OwgeResult<i64> {
    let count: i64 = sqlx::query_scalar(
        // SUM() yields DECIMAL even over an integer column; CAST so sqlx decodes i64.
        "SELECT CAST(COALESCE(SUM(count), 0) AS SIGNED) FROM obtained_units WHERE user_id = ? AND unit_id = ?",
    )
    .bind(user_id)
    .bind(unit_id as u16)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count)
}

async fn home_galaxy_id(conn: &mut MySqlConnection, home_planet: u64) -> OwgeResult<Option<i32>> {
    let galaxy = sqlx::query_scalar::<_, u16>("SELECT galaxy_id FROM planets WHERE id = ?")
        .bind(home_planet)
        .fetch_optional(&mut *conn)
        .await?;
    Ok(galaxy.map(|g| g as i32))
}

async fn special_location_owned(
    conn: &mut MySqlConnection,
    user_id: i32,
    special_location_id: i32,
) -> OwgeResult<bool> {
    let owner = sqlx::query_scalar::<_, Option<i32>>(
        "SELECT owner FROM planets WHERE special_location_id = ? LIMIT 1",
    )
    .bind(special_location_id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(matches!(owner, Some(Some(o)) if o == user_id))
}

async fn time_special_unlocked(
    conn: &mut MySqlConnection,
    user_id: i32,
    time_special_id: i32,
) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM unlocked_relation ur \
         JOIN object_relations o ON o.id = ur.relation_id \
         WHERE ur.user_id = ? AND o.object_description = ? AND o.reference_id = ?",
    )
    .bind(user_id)
    .bind(object_enum::TIME_SPECIAL)
    .bind(time_special_id as i16)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}

async fn time_special_active(
    conn: &mut MySqlConnection,
    user_id: i32,
    time_special_id: i32,
) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM active_time_specials \
         WHERE user_id = ? AND time_special_id = ? AND state = 'ACTIVE'",
    )
    .bind(user_id)
    .bind(time_special_id as u16)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}

async fn obtained_upgrade_exists(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: u16,
) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM obtained_upgrades WHERE user_id = ? AND upgrade_id = ?",
    )
    .bind(user_id)
    .bind(upgrade_id)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}

async fn register_obtained_upgrade(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: u16,
) -> OwgeResult<()> {
    sqlx::query(
        "INSERT INTO obtained_upgrades (user_id, upgrade_id, level, available) VALUES (?, ?, 0, 1)",
    )
    .bind(user_id)
    .bind(upgrade_id)
    .execute(&mut *conn)
    .await?;
    Ok(())
}

async fn set_obtained_upgrade_available(
    conn: &mut MySqlConnection,
    user_id: i32,
    upgrade_id: u16,
    available: bool,
) -> OwgeResult<()> {
    sqlx::query("UPDATE obtained_upgrades SET available = ? WHERE user_id = ? AND upgrade_id = ?")
        .bind(available as i8)
        .bind(user_id)
        .bind(upgrade_id)
        .execute(&mut *conn)
        .await?;
    Ok(())
}
