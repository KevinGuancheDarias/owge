//! Port of `RunningMissionFinderBo`
//! (`com.kevinguanchedarias.owgejava.business.mission.RunningMissionFinderBo`)
//! and the `findBuildMissions` method of `MissionFinderBo`.
//!
//! Provides the finder functions backing the four mission-related websocket
//! events: `unit_mission_change`, `enemy_mission_change`,
//! `missions_count_change`, and `unit_build_mission_change`.
//!
//! ## Parity scope
//! - [`RunningMissionFinderBo::find_user_running_missions`]: user's own running
//!   missions (resolved=0), with involved units (obtained via mission_id),
//!   planets nullified on the involved unit DTOs.
//! - [`RunningMissionFinderBo::find_enemy_running_missions`]: missions targeting
//!   a planet owned by the user, resolved=0 AND invisible=0 AND user != target
//!   owner. Involved-unit planets nullified; source_planet+user hidden when user
//!   has not explored the source planet.
//! - [`RunningMissionFinderBo::find_build_missions`]: all BUILD_UNIT missions
//!   for the user, resolved=0, each assembled as a `RunningUnitBuildDto`.
//! - [`RunningMissionFinderBo::count_user_running_missions`]: delegates to
//!   `MissionBo::count_unresolved_missions`.
//!
//! ## EXPLORE planetCleaner / hiddenUnit / handleInvisible
//!
//! ### `cleanUpUnexplored` (EXPLORE missions, `find_user_running_missions`)
//! Java's `findUserRunningMissions` calls
//! `planetCleanerService.cleanUpUnexplored(userId, dto.getTargetPlanet())` for
//! every EXPLORE mission. `PlanetCleanerService.cleanUpUnexplored` checks
//! `planetExplorationService.isExplored(userId, planet.getId())`; if not
//! explored it nullifies these fields on the `PlanetDto`:
//!   - `name` → null
//!   - `richness` → null
//!   - `home` → null
//!   - `ownerId` → null
//!   - `ownerName` → null
//!   - `specialLocation` → null
//!
//! The `isExplored` check is already ported (`is_planet_explored_by_user` in
//! this file). **Porting `cleanUpUnexplored` requires making `name`, `richness`,
//! and `home` optional on [`crate::dto::planet::PlanetDto`]** (they are
//! currently non-optional `String`/`u16`/`bool`). `owner_id` and `owner_name`
//! are already `Option`. `special_location` is deferred entirely (see the
//! `specialLocation` TODO in `planet.rs`). Until the DTO fields are widened to
//! `Option`, nullification cannot be implemented without a breaking DTO change
//! that affects all planet-DTO consumers. See `TODO(cleanUpUnexplored)` inline.
//!
//! ### `hiddenUnitBo.defineHidden` + `ObtainedUnitUtil.handleInvisible` (enemy missions)
//! Java's `findEnemyRunningMissions` calls, in order:
//!   1. `hiddenUnitBo.defineHidden(involvedUnits, involvedUnitDtos)` — for each
//!      involved unit, sets `dto.getUnit().setIsInvisible(isHiddenUnit(user, unit))`
//!      where `isHiddenUnit` returns:
//!      `unit.getIsInvisible() == true`   (units table `is_invisible` column)
//!      **OR** `activeTimeSpecialRuleFinderService.existsRuleMatchingUnitDestination(
//!          owner_user, unit, TIME_SPECIAL_IS_ENABLED_DO_HIDE)` —
//!      i.e. does the *attacker's* user have any ACTIVE time-special whose
//!      rule `type='TIME_SPECIAL_IS_ENABLED_DO_HIDE'` targets this unit's id
//!      (destination_type='UNIT', destination_id=unit.id) or any ancestor
//!      unit-type of the unit (destination_type='UNIT_TYPE', matched by
//!      recursively walking `unit_types.parent_id`).
//!      Tables involved: `active_time_specials` (user_id, state='ACTIVE'),
//!      `rules` (type, origin_type='TIME_SPECIAL', origin_id=time_special_id,
//!      destination_type, destination_id), `unit_types` (id, parent_id for
//!      ancestry walk).
//!   2. `ObtainedUnitUtil.handleInvisible(involvedUnitDtos)` — for each DTO
//!      whose `unit.isInvisible == true`, sets `dto.unit = null` and
//!      `dto.count = null`.
//!
//! **Porting `handleInvisible` requires making `unit` and `count` optional on
//! [`crate::dto::obtained_unit::ObtainedUnitDto`]** (currently non-optional).
//! **Porting the time-special branch of `defineHidden`** requires a new
//! SQL join across `active_time_specials` + `rules` + recursive `unit_types`
//! parent walk. The `units.is_invisible` part of `defineHidden` is already
//! available in `ObtainedUnitUnitDto.is_invisible` (loaded by
//! `involved_units_to_dtos` from the `units` table). Neither half can be wired
//! without first widening the DTO. See `TODO(handleInvisible)` inline.
//!
//! ## sqlx signedness (load-bearing)
//! `missions.id` = `bigint unsigned` (`u64`); `missions.user_id` = signed
//! `int` (`i32`); `missions.type` = `smallint unsigned` (`u16`);
//! `missions.source_planet`/`target_planet` = **signed** `bigint` (`i64`);
//! `missions.resolved`/`invisible` = `tinyint` (`i8`);
//! `mission_information.value` = `double` (`f64`);
//! `object_relations.reference_id` = signed `smallint` (`i16`) (unit id is
//! treated as `u16` after widening); `obtained_units.count` = `bigint
//! unsigned` (`u64`); `planets.id` = `bigint unsigned` (`u64`).

use sqlx::MySqlConnection;

use crate::bo::mission_bo::MissionBo;
use crate::bo::mission_base_service_bo::SELECT_MISSION;
use crate::db::Db;
use crate::dto::mission::UnitRunningMissionDto;
use crate::dto::running_unit_build::RunningUnitBuildDto;
use crate::dto::PlanetDto;
use crate::error::{OwgeError, OwgeResult};
use crate::model::mission::{Mission, MissionType};
use crate::model::obtained_unit::ObtainedUnit;

pub struct RunningMissionFinderBo;

impl RunningMissionFinderBo {
    /// `findUserRunningMissions(userId)` — all running (resolved=0) missions
    /// for the user, with involved-unit DTOs attached and their source/target
    /// planets nullified.
    ///
    /// Mirrors `missionRepository.findByUserIdAndResolvedFalse(userId)` ->
    /// `UnitRunningMissionDto::new(mission)` -> load involved units ->
    /// `nullifyInvolvedUnitsPlanets()`.
    ///
    /// TODO(cleanUpUnexplored): For EXPLORE missions Java calls
    /// `planetCleanerService.cleanUpUnexplored(userId, dto.targetPlanet)` which
    /// nullifies `name`/`richness`/`home`/`ownerId`/`ownerName`/`specialLocation`
    /// on the planet DTO when the user has NOT explored that planet.
    /// `is_planet_explored_by_user` (this file) already provides the DB check.
    /// Blocked on: `PlanetDto.name`/`richness`/`home` must become `Option<_>`
    /// (edit `owge-business/src/dto/planet.rs`); once widened, add:
    /// ```text
    /// if dto.r#type == Some(MissionType::Explore) {
    ///     if let Some(ref mut tp) = dto.target_planet {
    ///         let mut conn = db.acquire().await?;
    ///         if !is_planet_explored_by_user(&mut conn, user_id, tp.id).await? {
    ///             tp.name = None;
    ///             tp.richness = None;
    ///             tp.home = None;
    ///             tp.owner_id = None;
    ///             tp.owner_name = None;
    ///             // tp.special_location = None; (once special_location is added)
    ///         }
    ///     }
    /// }
    /// ```
    pub async fn find_user_running_missions(
        db: &Db,
        user_id: i32,
    ) -> OwgeResult<Vec<UnitRunningMissionDto>> {
        let missions = load_user_missions(db, user_id).await?;
        let mut out = Vec::with_capacity(missions.len());
        for mission in &missions {
            let mut dto = UnitRunningMissionDto::from_mission(mission);

            // Load source/target planet DTOs from the mission itself.
            let mut conn = db.acquire().await?;
            if let Some(sp_id) = mission.source_planet {
                dto.source_planet = load_planet_dto_conn(&mut conn, sp_id as u64).await?;
            }
            if let Some(tp_id) = mission.target_planet {
                dto.target_planet = load_planet_dto_conn(&mut conn, tp_id as u64).await?;
            }
            // Load user field on the DTO from the mission's user_id.
            if let Some(uid) = mission.user_id {
                dto.user = load_user_storage_dto(&mut conn, uid).await?;
            }

            // obtainedUnitRepository.findByMissionId(missionId) -> ObtainedUnitFinderBo.findCompletedAsDto
            // Simplified: load the obtained-unit DTOs directly (no completion filter on
            // mission-attached units — Java does filter but mission units are always
            // "complete" in the involved-units sense; this matches the pattern in
            // mission_base_service_bo::build_common_error_report).
            let units = load_mission_units_conn(&mut conn, mission.id).await?;
            let dtos =
                crate::bo::mission_processor::involved_units_to_dtos(&mut conn, &units).await?;
            dto.involved_units = Some(dtos);

            // nullifyInvolvedUnitsPlanets()
            dto.nullify_involved_units_planets();

            // cleanUpUnexplored (EXPLORE missions): Java's RunningMissionFinderBo
            // calls planetCleanerService.cleanUpUnexplored on the target planet of
            // every EXPLORE mission, masking name/richness/home/owner for a planet
            // the user has not yet explored (the mission is in flight to explore it).
            if dto.r#type == Some(MissionType::Explore) {
                if let Some(tp) = dto.target_planet.as_mut() {
                    if !is_planet_explored_by_user(&mut conn, user_id, tp.id).await? {
                        tp.clean_up_unexplored();
                    }
                }
            }

            out.push(dto);
        }
        Ok(out)
    }

    /// `findEnemyRunningMissions(user)` — missions targeting a planet owned by
    /// `user_id`, resolved=0, invisible=0, and launched by a different user.
    ///
    /// Mirrors `missionRepository.findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot`:
    /// - load the user's owned planet ids
    /// - query missions whose `target_planet` is one of those, resolved=0,
    ///   invisible=0, user_id != user_id arg
    /// - build DTO + nullify involved-unit planets
    /// - if the user has NOT explored the source planet: set source_planet=None
    ///   and user=None on the DTO
    ///
    /// See `TODO(handleInvisible)` inline for the deferred
    /// `hiddenUnitBo.defineHidden` + `ObtainedUnitUtil.handleInvisible` logic.
    pub async fn find_enemy_running_missions(
        db: &Db,
        user_id: i32,
    ) -> OwgeResult<Vec<UnitRunningMissionDto>> {
        // planetRepository.findByOwnerId(user.getId())
        let planet_ids = load_user_planet_ids(db, user_id).await?;
        if planet_ids.is_empty() {
            return Ok(Vec::new());
        }

        // findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot
        let missions = load_enemy_missions(db, user_id, &planet_ids).await?;
        let mut out = Vec::with_capacity(missions.len());
        for mission in &missions {
            let mut conn = db.acquire().await?;

            // Build base DTO.
            let mut dto = UnitRunningMissionDto::from_mission(mission);

            // Fill source/target planet DTOs.
            if let Some(sp_id) = mission.source_planet {
                dto.source_planet = load_planet_dto_conn(&mut conn, sp_id as u64).await?;
            }
            if let Some(tp_id) = mission.target_planet {
                dto.target_planet = load_planet_dto_conn(&mut conn, tp_id as u64).await?;
            }
            if let Some(uid) = mission.user_id {
                dto.user = load_user_storage_dto(&mut conn, uid).await?;
            }

            // Load involved units and nullify their planets.
            let units = load_mission_units_conn(&mut conn, mission.id).await?;
            let dtos =
                crate::bo::mission_processor::involved_units_to_dtos(&mut conn, &units).await?;
            dto.involved_units = Some(dtos);
            dto.nullify_involved_units_planets();

            // TODO(handleInvisible): Java calls hiddenUnitBo.defineHidden then
            // ObtainedUnitUtil.handleInvisible on the involved units here.
            //
            // defineHidden sets unit.isInvisible = true when either:
            //   (a) units.is_invisible = 1  — already in ObtainedUnitUnitDto.is_invisible
            //   (b) the attacker's user has an ACTIVE time-special with a rule
            //       of type 'TIME_SPECIAL_IS_ENABLED_DO_HIDE' that targets this
            //       unit (by UNIT id) or any ancestor UNIT_TYPE (recursive
            //       parent walk on unit_types.parent_id).
            //       SQL shape:
            //         SELECT r.destination_type, r.destination_id
            //         FROM active_time_specials ats
            //         JOIN rules r ON r.origin_type = 'TIME_SPECIAL'
            //                     AND r.origin_id = ats.time_special_id
            //                     AND r.type = 'TIME_SPECIAL_IS_ENABLED_DO_HIDE'
            //         WHERE ats.user_id = <attacker_user_id>
            //           AND ats.state = 'ACTIVE'
            //       Then for each row: if destination_type='UNIT' check
            //       destination_id == unit.id; if 'UNIT_TYPE' walk the
            //       unit_types parent chain until a match or root.
            //
            // handleInvisible: for each involved-unit DTO where unit.is_invisible
            // is true, set dto.unit = null and dto.count = null.
            //
            // Blocked on: ObtainedUnitDto.unit and .count must become Option<_>
            // (edit owge-business/src/dto/obtained_unit.rs + add
            // #[serde(skip_serializing_if="Option::is_none")] to match Jackson's
            // Include.NON_NULL). Once widened, implement a helper
            // `apply_handle_invisible(units: &mut Vec<ObtainedUnitDto>)` in this
            // file that performs steps (a)+(b) above, then call it here.

            // planetExplorationService.isExplored(user, sourcePlanet): if not explored,
            // hide source_planet and user fields.
            if let Some(sp_id) = mission.source_planet {
                let explored =
                    is_planet_explored_by_user(&mut conn, user_id, sp_id as u64).await?;
                if !explored {
                    dto.source_planet = None;
                    dto.user = None;
                }
            }

            out.push(dto);
        }
        Ok(out)
    }

    /// `findBuildMissions(userId)` — all BUILD_UNIT missions for the user
    /// (resolved=0), each assembled as a [`RunningUnitBuildDto`].
    ///
    /// Mirrors `MissionFinderBo.findBuildMissions`: query BUILD_UNIT missions by
    /// user_id+type resolved=0, then for each: unbox the unit from
    /// `mission_information` + `object_relations`, load the build planet from
    /// `mission_information.value`, load the in-build count from `obtained_units`.
    pub async fn find_build_missions(
        db: &Db,
        user_id: i32,
    ) -> OwgeResult<Vec<RunningUnitBuildDto>> {
        let mission_ids = load_build_mission_ids(db, user_id).await?;
        let mut out = Vec::with_capacity(mission_ids.len());
        for mission_id in mission_ids {
            let mut conn = db.acquire().await?;
            let mission = sqlx::query_as::<_, Mission>(SELECT_MISSION)
                .bind(mission_id)
                .fetch_optional(&mut *conn)
                .await?;
            let Some(mission) = mission else {
                continue;
            };

            // missionInformation.getRelation() -> unboxObjectRelation -> unit id
            let unit_id = resolve_unit_id_for_mission(&mut conn, mission_id).await?;
            let unit = crate::bo::unit_bo::UnitBo::find_by_id(db, unit_id)
                .await?
                .ok_or_else(|| OwgeError::NotFound(format!("No unit with id {unit_id}")))?;

            // missionInformation.getValue().longValue() -> build planet id
            let planet_id: Option<f64> = sqlx::query_scalar(
                "SELECT value FROM mission_information WHERE mission_id = ?",
            )
            .bind(mission_id)
            .fetch_optional(&mut *conn)
            .await?;
            let planet_id = planet_id
                .map(|v| v as u64)
                .ok_or_else(|| {
                    OwgeError::Common(format!(
                        "Build mission {mission_id} has no mission_information.value"
                    ))
                })?;

            let planet = load_planet_dto_conn(&mut conn, planet_id)
                .await?
                .ok_or_else(|| {
                    OwgeError::NotFound(format!("No planet with id {planet_id}"))
                })?;

            // obtainedUnitRepository.findByMissionId(mission.getId()).get(0).getCount() or 0
            let count: u64 =
                sqlx::query_scalar("SELECT count FROM obtained_units WHERE mission_id = ? LIMIT 1")
                    .bind(mission_id)
                    .fetch_optional(&mut *conn)
                    .await?
                    .unwrap_or(0);

            out.push(RunningUnitBuildDto::from_mission(&mission, unit, planet, count));
        }
        Ok(out)
    }

    /// `countUserRunningMissions(userId)` — delegates to
    /// `MissionBo::count_unresolved_missions`.
    pub async fn count_user_running_missions(db: &Db, user_id: i32) -> OwgeResult<i32> {
        MissionBo::count_unresolved_missions(db, user_id).await
    }
}

// ---------------------------------------------------------------------------
// SQL helpers
// ---------------------------------------------------------------------------

const SELECT_MISSION_COLS: &str = "\
    SELECT id, user_id, type, termination_date, required_time, starting_date, \
           primary_resource, secondary_resource, required_energy, \
           source_planet, target_planet, related_mission, report_id, \
           attemps, resolved, invisible \
    FROM missions";

/// All resolved=0 missions for the user.
async fn load_user_missions(db: &Db, user_id: i32) -> OwgeResult<Vec<Mission>> {
    Ok(sqlx::query_as::<_, Mission>(&format!(
        "{SELECT_MISSION_COLS} WHERE user_id = ? AND resolved = 0"
    ))
    .bind(user_id)
    .fetch_all(db)
    .await?)
}

/// Planet ids owned by the user. `planets.id` is `bigint unsigned`; cast to a
/// SIGNED value so it decodes as `i64` and binds against `missions.target_planet`
/// (a **signed** `bigint`).
async fn load_user_planet_ids(db: &Db, user_id: i32) -> OwgeResult<Vec<i64>> {
    Ok(
        sqlx::query_scalar("SELECT CAST(id AS SIGNED) FROM planets WHERE owner = ?")
            .bind(user_id)
            .fetch_all(db)
            .await?,
    )
}

/// Enemy missions: target_planet IN user_planets, resolved=0, invisible=0,
/// user_id != user_id arg. Constructs the IN-list dynamically.
async fn load_enemy_missions(
    db: &Db,
    user_id: i32,
    planet_ids: &[i64],
) -> OwgeResult<Vec<Mission>> {
    if planet_ids.is_empty() {
        return Ok(Vec::new());
    }
    // Build ? placeholders for the IN list.
    let placeholders = planet_ids
        .iter()
        .map(|_| "?")
        .collect::<Vec<_>>()
        .join(", ");
    let sql = format!(
        "{SELECT_MISSION_COLS} \
         WHERE target_planet IN ({placeholders}) \
           AND resolved = 0 AND invisible = 0 AND user_id != ?"
    );
    let mut q = sqlx::query_as::<_, Mission>(&sql);
    for pid in planet_ids {
        q = q.bind(*pid);
    }
    q = q.bind(user_id);
    Ok(q.fetch_all(db).await?)
}

/// All BUILD_UNIT mission ids for the user (resolved=0).
async fn load_build_mission_ids(db: &Db, user_id: i32) -> OwgeResult<Vec<u64>> {
    Ok(sqlx::query_scalar(
        "SELECT m.id FROM missions m \
          WHERE m.user_id = ? AND m.type = ? AND m.resolved = 0",
    )
    .bind(user_id)
    .bind(MissionType::BuildUnit.value())
    .fetch_all(db)
    .await?)
}

/// `object_relations.reference_id` for the UNIT relation on a BUILD_UNIT mission.
async fn resolve_unit_id_for_mission(
    conn: &mut MySqlConnection,
    mission_id: u64,
) -> OwgeResult<u16> {
    let reference_id: Option<i16> = sqlx::query_scalar(
        "SELECT o.reference_id FROM mission_information mi \
           JOIN object_relations o ON o.id = mi.relation_id \
          WHERE mi.mission_id = ?",
    )
    .bind(mission_id)
    .fetch_optional(&mut *conn)
    .await?;
    let reference_id = reference_id.ok_or_else(|| {
        OwgeError::Common(format!(
            "Build mission {mission_id} mission_information has no relation"
        ))
    })?;
    Ok(reference_id as u16)
}

/// Load the obtained-unit stacks attached to a mission.
async fn load_mission_units_conn(
    conn: &mut MySqlConnection,
    mission_id: u64,
) -> OwgeResult<Vec<ObtainedUnit>> {
    Ok(sqlx::query_as::<_, ObtainedUnit>(
        "SELECT id, user_id, unit_id, count, source_planet, target_planet, \
                mission_id, first_deployment_mission, is_from_capture, \
                expiration_id, owner_unit_id \
         FROM obtained_units WHERE mission_id = ?",
    )
    .bind(mission_id)
    .fetch_all(&mut *conn)
    .await?)
}

/// Load a planet DTO by id on an existing connection.
async fn load_planet_dto_conn(
    conn: &mut MySqlConnection,
    planet_id: u64,
) -> OwgeResult<Option<PlanetDto>> {
    crate::bo::mission_processor::load_planet_dto(conn, planet_id).await
}

/// A minimal `UserStorageDto` (id + username + email) for the `user` field on
/// `UnitRunningMissionDto` — mirrors what Java's `UserStorageDto` constructor
/// copies from the entity: id, username, email (other fields null/zero).
async fn load_user_storage_dto(
    conn: &mut MySqlConnection,
    user_id: i32,
) -> OwgeResult<Option<crate::dto::UserStorageDto>> {
    let row: Option<(i32, String, String)> = sqlx::query_as(
        "SELECT id, username, email FROM user_storage WHERE id = ?",
    )
    .bind(user_id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(row.map(|(id, username, email)| crate::dto::UserStorageDto {
        id,
        username,
        email,
        primary_resource: None,
        secondary_resource: None,
        consumed_energy: None,
        primary_resource_generation_per_second: None,
        secondary_resource_generation_per_second: None,
        max_energy: None,
        has_skipped_tutorial: false,
        can_alter_twitch_state: false,
        computed_primary_resource_generation_per_second: None,
        computed_secondary_resource_generation_per_second: None,
        computed_max_energy: None,
    }))
}

/// `PlanetExplorationService.isExplored(user, planet)` — true when the user
/// has an explored_planets row for the given planet.
///
/// Java: `exploredPlanetsRepository.findOneByUserAndPlanet(user, planet) != null`.
/// Column names in `explored_planets`: `user` (int, signed) and `planet`
/// (bigint unsigned).
async fn is_planet_explored_by_user(
    conn: &mut MySqlConnection,
    user_id: i32,
    planet_id: u64,
) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(
        "SELECT COUNT(*) FROM explored_planets WHERE `user` = ? AND planet = ?",
    )
    .bind(user_id)
    .bind(planet_id)
    .fetch_one(&mut *conn)
    .await?;
    Ok(count > 0)
}
