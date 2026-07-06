//! Port of (the read side of) `UnitTypeBo`
//! (`com.kevinguanchedarias.owgejava.business.UnitTypeBo`) and the
//! `UnitTypeRestService.loadData` builder — the `unit_type_change` sync payload.
//!
//! `UnitTypeBo.findUnitTypesWithUserInfo` maps every `unit_type` row to a
//! `UnitTypeResponse`, resolving the image URL plus the per-user
//! `computedMaxCount` / `userBuilt` / `used` fields. `computedMaxCount` runs the
//! improvement engine (`findUniTypeLimitByUser`) and `userBuilt` counts the
//! user's obtained units (`countUnitsByUserAndUnitType`), exactly as in Java;
//! `used` and the catalog fields are fully resolved.

use crate::bo::{AttackRuleBo, CriticalAttackBo, SpeedImpactGroupBo, UserImprovementBo};
use crate::bo::mission_bo::{compute_improvement_value, load_user_storage};
use crate::dto::UnitTypeDto;
use crate::dto::unit_type::UnitTypeInput;
use crate::dto::user_improvement::ImprovementType;
use crate::error::{OwgeError, OwgeResult};
use sqlx::MySqlConnection;

/// Guard against pathological `parent` / `shareMaxCount` chains (Java recurses
/// the full chain; real data is shallow, but a cycle would loop forever).
const MAX_NESTED_DEPTH: u8 = 16;

/// A `unit_types` row joined with its image and a `used` existence flag, with
/// exact SQL column types so sqlx decode never panics on signedness/width.
#[derive(sqlx::FromRow)]
struct UnitTypeRow {
    id: u16,
    name: String,
    image: Option<u64>,
    image_filename: Option<String>,
    max_count: Option<i64>,
    share_max_count_id: Option<u16>,
    parent_id: Option<u16>,
    speed_impact_group_id: Option<u16>,
    attack_rule_id: Option<u16>,
    critical_attack_id: Option<u16>,
    has_to_inherit_improvements: i8,
    can_explore: String,
    can_gather: String,
    can_establish_base: String,
    can_attack: String,
    can_counterattack: String,
    can_conquest: String,
    can_deploy: String,
    /// `EXISTS(SELECT 1 FROM units ...)` -> `tinyint(1)` read as `i8`.
    used: i8,
}

/// Build the **catalog** form of a unit type (Java `UnitTypeDto.dtoFromEntity`):
/// scalars + the nested `speedImpactGroup` / `attackRule` / `criticalAttack`
/// objects and the recursively-nested `parent` / `shareMaxCount`. The per-user
/// fields (`computedMaxCount` / `userBuilt` / `used`) are left `None` here; the
/// sync path sets them on the top-level type only.
async fn build_catalog_dto(
    conn: &mut MySqlConnection,
    r: UnitTypeRow,
    depth: u8,
    with_req_groups: bool,
) -> OwgeResult<UnitTypeDto> {
    let image_url = r
        .image_filename
        .as_deref()
        .map(crate::bo::image_store_bo::compute_image_url);

    // `with_req_groups` mirrors Java's path-dependent `requirementsGroups`: the
    // `unit_type_change` rest service nulls it (false here), but it is present
    // where the entity is reached with the transient initialized (e.g. embedded
    // in an improvement's `unitType` — `find_catalog_by_id`, true).
    let speed_impact_group = match r.speed_impact_group_id {
        Some(id) if with_req_groups => {
            SpeedImpactGroupBo::find_by_id_with_requirement_groups(conn, id).await?
        }
        Some(id) => SpeedImpactGroupBo::find_by_id(conn, id).await?,
        None => None,
    };
    let attack_rule = match r.attack_rule_id {
        Some(id) => AttackRuleBo::find_by_id(conn, id).await?,
        None => None,
    };
    let critical_attack = match r.critical_attack_id {
        Some(id) => CriticalAttackBo::find_by_id(conn, id).await?,
        None => None,
    };
    let parent = build_nested_by_id(conn, r.parent_id, depth, with_req_groups).await?;
    let share_max_count = build_nested_by_id(conn, r.share_max_count_id, depth, with_req_groups).await?;

    Ok(UnitTypeDto {
        id: r.id,
        name: r.name,
        image: r.image,
        image_url,
        max_count: r.max_count,
        share_max_count,
        parent,
        has_to_inherit_improvements: r.has_to_inherit_improvements != 0,
        can_explore: r.can_explore,
        can_gather: r.can_gather,
        can_establish_base: r.can_establish_base,
        can_attack: r.can_attack,
        can_counterattack: r.can_counterattack,
        can_conquest: r.can_conquest,
        can_deploy: r.can_deploy,
        speed_impact_group,
        attack_rule,
        critical_attack,
        computed_max_count: None,
        user_built: None,
        used: None,
    })
}

/// Recursively load a nested `parent` / `shareMaxCount` by id (boxed to break
/// the async recursion cycle; depth-guarded against malformed cyclic data).
async fn build_nested_by_id(
    conn: &mut MySqlConnection,
    id: Option<u16>,
    depth: u8,
    with_req_groups: bool,
) -> OwgeResult<Option<Box<UnitTypeDto>>> {
    let Some(id) = id else { return Ok(None) };
    if depth >= MAX_NESTED_DEPTH {
        return Ok(None);
    }
    let row = sqlx::query_as::<_, UnitTypeRow>(&format!("{SELECT_DTO} WHERE ut.id = ?"))
        .bind(id)
        .fetch_optional(&mut *conn)
        .await?;
    match row {
        Some(r) => Ok(Some(Box::new(
            Box::pin(build_catalog_dto(conn, r, depth + 1, with_req_groups)).await?,
        ))),
        None => Ok(None),
    }
}

const SELECT_DTO: &str = "\
    SELECT ut.id, ut.name, ut.image_id AS image, i.filename AS image_filename, \
           ut.max_count, ut.share_max_count AS share_max_count_id, \
           ut.parent_type AS parent_id, ut.speed_impact_group_id, \
           ut.attack_rule_id, ut.critical_attack_id, ut.has_to_inherit_improvements, \
           ut.can_explore, ut.can_gather, ut.can_establish_base, ut.can_attack, \
           ut.can_counterattack, ut.can_conquest, ut.can_deploy, \
           EXISTS(SELECT 1 FROM units u WHERE u.type = ut.id) AS used \
    FROM unit_types ut \
    LEFT JOIN images_store i ON i.id = ut.image_id ";

pub struct UnitTypeBo;

impl UnitTypeBo {
    /// `UnitTypeBo.findUnitTypesWithUserInfo(userId)` -> DTOs — the
    /// `unit_type_change` sync payload (whole catalog, with per-user info).
    ///
    /// Per type it resolves, exactly as Java does:
    /// - `computedMaxCount` = `findUniTypeLimitByUser(user, type)` — 0 when the
    ///   type has no max, else `floor(computeImprovementValue(findMaxCount,
    ///   AMOUNT improvement for the type, sum=true))`. Always set (never null).
    /// - `userBuilt` = `countUnitsByUserAndUnitType(user, type)` ONLY when the
    ///   type `hasMaxCount`; left null otherwise.
    pub async fn find_unit_types_with_user_info(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<UnitTypeDto>> {
        let rows = sqlx::query_as::<_, UnitTypeRow>(&format!("{SELECT_DTO} ORDER BY ut.id"))
            .fetch_all(&mut *conn)
            .await?;

        // Load the user (faction) once — `findUniTypeLimitByUser` /
        // `hasMaxCount` / `findMaxCount` all key off `user.getFaction()`.
        let user = load_user_storage(&mut *conn, user_id).await?;
        let improvement =
            UserImprovementBo::find_user_improvement_on_conn(&mut *conn, user_id).await?;

        let mut out = Vec::with_capacity(rows.len());
        for r in rows {
            let type_id = r.id;
            let type_max = r.max_count;
            let used_flag = r.used != 0;
            let mut dto = build_catalog_dto(&mut *conn, r, 0, false).await?;
            // `used` is a top-level (sync) field only; nested catalog forms omit it.
            dto.used = Some(used_flag);

            // findMaxCount(faction, type): the faction override row's max_count
            // if present, else unit_types.max_count.
            // factions_unit_types.max_count is INT UNSIGNED; unit_types.max_count
            // is BIGINT (signed).
            let faction_max: Option<u32> = sqlx::query_scalar(
                "SELECT max_count FROM factions_unit_types \
                  WHERE faction_id = ? AND unit_type_id = ?",
            )
            .bind(user.faction)
            .bind(type_id)
            .fetch_optional(&mut *conn)
            .await?
            .flatten();
            let max_count_value: i64 = match faction_max {
                Some(m) => m as i64,
                None => type_max.unwrap_or(0),
            };

            // hasMaxCount(faction, type): a faction override max_count > 0, OR
            // unit_types.max_count present and > 0.
            let has_max_count = faction_max.map(|m| m > 0).unwrap_or(false)
                || type_max.map(|m| m > 0).unwrap_or(false);

            // findUniTypeLimitByUser: 0 when !hasMaxCount, else the
            // improvement-boosted, floored max count.
            let computed_max_count: i64 = if has_max_count {
                let amount_improvement =
                    improvement.find_unit_type_improvement(ImprovementType::Amount, type_id);
                compute_improvement_value(
                    &mut *conn,
                    max_count_value as f64,
                    amount_improvement,
                    true,
                )
                .await?
                .floor() as i64
            } else {
                0
            };
            dto.computed_max_count = Some(computed_max_count);

            // countUnitsByUserAndUnitType only when hasMaxCount: SUM over the
            // user's obtained units of this type PLUS units whose type's
            // share_max_count points at this type.
            if has_max_count {
                let user_built: i64 = sqlx::query_scalar(
                    "SELECT CAST(COALESCE(SUM(ou.count), 0) AS SIGNED) FROM obtained_units ou \
                       JOIN units u ON u.id = ou.unit_id \
                       JOIN unit_types ut ON ut.id = u.type \
                      WHERE ou.user_id = ? AND (ut.id = ? OR ut.share_max_count = ?)",
                )
                .bind(user.id)
                .bind(type_id)
                .bind(type_id)
                .fetch_one(&mut *conn)
                .await?;
                dto.user_built = Some(user_built);
            }

            out.push(dto);
        }
        Ok(out)
    }

    /// `CrudRestServiceTrait.findAll` for the admin unit-type CRUD — every row,
    /// ordered by id. Same catalog read as the sync payload but without per-user
    /// info (the admin list does not need it).
    pub async fn find_all(conn: &mut MySqlConnection) -> OwgeResult<Vec<UnitTypeDto>> {
        let rows = sqlx::query_as::<_, UnitTypeRow>(&format!("{SELECT_DTO} ORDER BY ut.id"))
            .fetch_all(&mut *conn)
            .await?;
        let mut out = Vec::with_capacity(rows.len());
        for r in rows {
            out.push(build_catalog_dto(&mut *conn, r, 0, false).await?);
        }
        Ok(out)
    }

    /// The **catalog** form of one unit type by id (Java `dtoFromEntity`: nested
    /// relations, no per-user fields). Used to hydrate the `unitType` embedded in
    /// improvement `unitTypesUpgrades` entries. `None` when the id is absent.
    ///
    /// Includes nested `speedImpactGroup.requirementsGroups` — matches the load
    /// path Java reaches through an *upgrade's* improvement
    /// (`obtained_upgrades_change`). Use [`Self::find_catalog_by_id_shallow`] for
    /// a *unit's own* improvement, where that nested list stays uninitialized.
    pub async fn find_catalog_by_id(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<Option<UnitTypeDto>> {
        Ok(build_nested_by_id(conn, Some(id), 0, true).await?.map(|b| *b))
    }

    /// Like [`Self::find_catalog_by_id`], but every nested `speedImpactGroup`
    /// (including on `parent`/`shareMaxCount`) omits `requirementsGroups` — the
    /// shape Java emits for the `unitType` embedded in a *unit's own*
    /// improvement (`unit_obtained_change`/`unit_unlocked_change`): on that load
    /// path Java resolves `speedImpactGroup`/`attackRule`/`criticalAttack`/
    /// `parent`/`shareMaxCount` eagerly, but not the deeper requirement-group
    /// graph.
    pub async fn find_catalog_by_id_shallow(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<Option<UnitTypeDto>> {
        Ok(build_nested_by_id(conn, Some(id), 0, false)
            .await?
            .map(|b| *b))
    }

    /// Catalog form for the `unitType` embedded in a user's aggregate
    /// improvement (`GroupedImprovement.unitTypesUpgrades[].unitType`, the
    /// `user_data_change` payload): full `parent`/`shareMaxCount` (recursively,
    /// **with** their own nested `speedImpactGroup.requirementsGroups` — they
    /// go through the ordinary catalog builder) plus `attackRule`/
    /// `criticalAttack`, but the top-level entry's **own** `speedImpactGroup`
    /// is never hydrated.
    ///
    /// Matched empirically against the running Java backend across five unit
    /// types with different FK combinations (some with `attackRule` only,
    /// some with `parent`+`criticalAttack`+`attackRule`, one with every
    /// relation set): in every case the entry's own `speedImpactGroup` was
    /// absent despite a non-null `speed_impact_group_id`, while a *nested*
    /// `parent`/`shareMaxCount` on the very same response carried a fully
    /// populated `speedImpactGroup` (including `requirementsGroups`). This
    /// mirrors the already-documented "path-dependent nested-relation" pattern
    /// (see `pending_migration/RESOLUTION.md`) one level deeper than
    /// previously known — the earlier "attackRule only" shape only *looked*
    /// right because the older/thinner seed data's unit types never had a
    /// `parent`/`shareMaxCount`/`criticalAttack` FK to reveal the gap.
    pub async fn find_catalog_by_id_for_aggregate(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<Option<UnitTypeDto>> {
        let Some(r) = sqlx::query_as::<_, UnitTypeRow>(&format!("{SELECT_DTO} WHERE ut.id = ?"))
            .bind(id)
            .fetch_optional(&mut *conn)
            .await?
        else {
            return Ok(None);
        };
        let image_url = r
            .image_filename
            .as_deref()
            .map(crate::bo::image_store_bo::compute_image_url);
        let attack_rule = match r.attack_rule_id {
            Some(rule_id) => AttackRuleBo::find_by_id(&mut *conn, rule_id).await?,
            None => None,
        };
        let critical_attack = match r.critical_attack_id {
            Some(id) => CriticalAttackBo::find_by_id(&mut *conn, id).await?,
            None => None,
        };
        let parent = build_nested_by_id(conn, r.parent_id, 0, true).await?;
        let share_max_count = build_nested_by_id(conn, r.share_max_count_id, 0, true).await?;
        Ok(Some(UnitTypeDto {
            id: r.id,
            name: r.name,
            image: r.image,
            image_url,
            max_count: r.max_count,
            share_max_count,
            parent,
            has_to_inherit_improvements: r.has_to_inherit_improvements != 0,
            can_explore: r.can_explore,
            can_gather: r.can_gather,
            can_establish_base: r.can_establish_base,
            can_attack: r.can_attack,
            can_counterattack: r.can_counterattack,
            can_conquest: r.can_conquest,
            can_deploy: r.can_deploy,
            speed_impact_group: None,
            attack_rule,
            critical_attack,
            computed_max_count: None,
            user_built: None,
            used: None,
        }))
    }

    /// `WithReadRestServiceTrait.findOneById` for the admin unit-type CRUD.
    pub async fn find_by_id(
        conn: &mut MySqlConnection,
        id: u16,
    ) -> OwgeResult<Option<UnitTypeDto>> {
        let row = sqlx::query_as::<_, UnitTypeRow>(&format!("{SELECT_DTO} WHERE ut.id = ?"))
            .bind(id)
            .fetch_optional(&mut *conn)
            .await?;
        match row {
            Some(r) => Ok(Some(build_catalog_dto(&mut *conn, r, 0, false).await?)),
            None => Ok(None),
        }
    }

    /// `CrudRestServiceTrait.saveNew` — insert; `unit_types.id` is AUTO_INCREMENT.
    /// Mirrors `entityFromDto` + `beforeSave`: the scalar/limitation fields come
    /// straight from the body, the relations are stored as their resolved ids.
    pub async fn save_new(
        conn: &mut MySqlConnection,
        input: &UnitTypeInput,
    ) -> OwgeResult<UnitTypeDto> {
        let result = sqlx::query(
            "INSERT INTO unit_types \
                (name, image_id, max_count, has_to_inherit_improvements, \
                 can_explore, can_gather, can_establish_base, can_attack, \
                 can_counterattack, can_conquest, can_deploy, \
                 speed_impact_group_id, attack_rule_id, critical_attack_id, \
                 parent_type, share_max_count) \
             VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
        )
        .bind(&input.name)
        .bind(input.image)
        .bind(input.max_count)
        .bind(input.has_to_inherit_improvements as i8)
        .bind(&input.can_explore)
        .bind(&input.can_gather)
        .bind(&input.can_establish_base)
        .bind(&input.can_attack)
        .bind(&input.can_counterattack)
        .bind(&input.can_conquest)
        .bind(&input.can_deploy)
        .bind(input.speed_impact_group_id())
        .bind(input.attack_rule_id())
        .bind(input.critical_attack_id())
        .bind(input.parent_id())
        .bind(input.share_max_count_id())
        .execute(&mut *conn)
        .await?;
        let id = result.last_insert_id() as u16;
        Self::find_by_id(conn, id)
            .await?
            .ok_or_else(|| OwgeError::Common("Unit type vanished right after insert".into()))
    }

    /// `CrudRestServiceTrait.saveExisting` — update by id.
    pub async fn save_existing(
        conn: &mut MySqlConnection,
        id: u16,
        input: &UnitTypeInput,
    ) -> OwgeResult<UnitTypeDto> {
        let affected = sqlx::query(
            "UPDATE unit_types SET \
                name = ?, image_id = ?, max_count = ?, has_to_inherit_improvements = ?, \
                can_explore = ?, can_gather = ?, can_establish_base = ?, can_attack = ?, \
                can_counterattack = ?, can_conquest = ?, can_deploy = ?, \
                speed_impact_group_id = ?, attack_rule_id = ?, critical_attack_id = ?, \
                parent_type = ?, share_max_count = ? \
             WHERE id = ?",
        )
        .bind(&input.name)
        .bind(input.image)
        .bind(input.max_count)
        .bind(input.has_to_inherit_improvements as i8)
        .bind(&input.can_explore)
        .bind(&input.can_gather)
        .bind(&input.can_establish_base)
        .bind(&input.can_attack)
        .bind(&input.can_counterattack)
        .bind(&input.can_conquest)
        .bind(&input.can_deploy)
        .bind(input.speed_impact_group_id())
        .bind(input.attack_rule_id())
        .bind(input.critical_attack_id())
        .bind(input.parent_id())
        .bind(input.share_max_count_id())
        .bind(id)
        .execute(&mut *conn)
        .await?
        .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No unit type with id {id}")));
        }
        Self::find_by_id(conn, id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No unit type with id {id}")))
    }

    /// `WithDeleteRestServiceTrait.delete`.
    pub async fn delete(conn: &mut MySqlConnection, id: u16) -> OwgeResult<()> {
        sqlx::query("DELETE FROM unit_types WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?;
        Ok(())
    }

    /// `AdminUnitTypeRestService.unsetAttackRule` — clears the attack rule FK.
    pub async fn unset_attack_rule(conn: &mut MySqlConnection, id: u16) -> OwgeResult<()> {
        let affected = sqlx::query("UPDATE unit_types SET attack_rule_id = NULL WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?
            .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No unit type with id {id}")));
        }
        Ok(())
    }

    /// `AdminUnitTypeRestService.unsetCriticalAttack` — clears the critical
    /// attack FK.
    pub async fn unset_critical_attack(conn: &mut MySqlConnection, id: u16) -> OwgeResult<()> {
        let affected = sqlx::query("UPDATE unit_types SET critical_attack_id = NULL WHERE id = ?")
            .bind(id)
            .execute(&mut *conn)
            .await?
            .rows_affected();
        if affected == 0 {
            return Err(OwgeError::NotFound(format!("No unit type with id {id}")));
        }
        Ok(())
    }
}
