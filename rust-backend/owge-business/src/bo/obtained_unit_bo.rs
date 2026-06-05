//! Port of (the read side of) `ObtainedUnitBo` / `ObtainedUnitFinderBo`
//! (`com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo`,
//! `...unit.ObtainedUnitFinderBo`).
//!
//! Provides the `unit_obtained_change` sync payload:
//! `ObtainedUnitFinderBo.findCompletedAsDto(user)` ->
//! `obtainedUnitRepository.findDeployedInUserOwnedPlanets(userId)` filtered to
//! the stacks that are *not* stored inside another unit (`ownerUnit == null`),
//! i.e. units on an owned planet and not part of a running mission.

use crate::bo::active_time_special_rule_finder_bo::ActiveTimeSpecialRuleFinderBo;
use crate::db::Db;
use crate::dto::obtained_unit::{ObtainedUnitDto, ObtainedUnitUnitDto};
use crate::dto::PlanetDto;
use crate::error::{OwgeError, OwgeResult};

/// One `obtained_units` row joined with its `units`, owning user, and (optional)
/// source/target planets — exact SQL column types so sqlx never panics on
/// signedness/width.
#[derive(sqlx::FromRow)]
struct ObtainedUnitRow {
    id: u64,
    count: u64,
    user_id: i32,
    username: Option<String>,

    // --- units ---
    unit_id: u16,
    unit_name: String,
    unit_type_id: Option<u16>,
    unit_type_name: Option<String>,
    unit_attack: Option<u16>,
    unit_health: Option<u16>,
    unit_shield: Option<u16>,
    unit_charge: Option<u16>,
    unit_is_unique: u8, // units.is_unique is `tinyint UNSIGNED`
    unit_can_fast_explore: i8,
    unit_speed: Option<f64>,
    unit_bypass_shield: i8,
    unit_is_invisible: i8,
    unit_stored_weight: u32,
    unit_storage_capacity: Option<u32>,

    // --- source planet (nullable join) ---
    sp_id: Option<u64>,
    sp_name: Option<String>,
    sp_sector: Option<u32>,
    sp_quadrant: Option<u32>,
    sp_planet_number: Option<u16>,
    sp_owner_id: Option<i32>,
    sp_owner_name: Option<String>,
    sp_richness: Option<u16>,
    sp_home: Option<i8>,
    sp_galaxy_id: Option<u16>,
    sp_galaxy_name: Option<String>,

    // --- temporal information (nullable join; present for time-special units) ---
    // `expiration` is a `TIMESTAMP` column → sqlx decodes it as `DateTime<Utc>`.
    ti_id: Option<u32>,
    ti_duration: Option<u32>,
    ti_expiration: Option<chrono::DateTime<chrono::Utc>>,
    ti_relation_id: Option<u16>,

    // --- target planet (nullable join) ---
    tp_id: Option<u64>,
    tp_name: Option<String>,
    tp_sector: Option<u32>,
    tp_quadrant: Option<u32>,
    tp_planet_number: Option<u16>,
    tp_owner_id: Option<i32>,
    tp_owner_name: Option<String>,
    tp_richness: Option<u16>,
    tp_home: Option<i8>,
    tp_galaxy_id: Option<u16>,
    tp_galaxy_name: Option<String>,
}

impl From<ObtainedUnitRow> for ObtainedUnitDto {
    fn from(r: ObtainedUnitRow) -> Self {
        let unit = ObtainedUnitUnitDto::reduced(
            r.unit_id,
            r.unit_name,
            r.unit_type_id,
            r.unit_type_name,
            r.unit_attack,
            r.unit_health,
            r.unit_shield,
            r.unit_charge,
            r.unit_is_unique != 0,
            r.unit_can_fast_explore != 0,
            r.unit_speed,
            r.unit_bypass_shield != 0,
            r.unit_is_invisible != 0,
            r.unit_stored_weight,
            r.unit_storage_capacity,
        );
        let source_planet = r.sp_id.map(|id| PlanetDto {
            id,
            name: Some(r.sp_name.unwrap_or_default()),
            sector: r.sp_sector.unwrap_or_default(),
            quadrant: r.sp_quadrant.unwrap_or_default(),
            planet_number: r.sp_planet_number.unwrap_or_default(),
            owner_id: r.sp_owner_id,
            owner_name: r.sp_owner_name,
            richness: Some(r.sp_richness.unwrap_or_default()),
            home: Some(r.sp_home.unwrap_or(0) != 0),
            galaxy_id: r.sp_galaxy_id.unwrap_or_default(),
            galaxy_name: r.sp_galaxy_name.unwrap_or_default(),
        });
        let target_planet = r.tp_id.map(|id| PlanetDto {
            id,
            name: Some(r.tp_name.unwrap_or_default()),
            sector: r.tp_sector.unwrap_or_default(),
            quadrant: r.tp_quadrant.unwrap_or_default(),
            planet_number: r.tp_planet_number.unwrap_or_default(),
            owner_id: r.tp_owner_id,
            owner_name: r.tp_owner_name,
            richness: Some(r.tp_richness.unwrap_or_default()),
            home: Some(r.tp_home.unwrap_or(0) != 0),
            galaxy_id: r.tp_galaxy_id.unwrap_or_default(),
            galaxy_name: r.tp_galaxy_name.unwrap_or_default(),
        });
        // TemporalInformationUnitDataLoaderService.addInformationToDto: when the
        // stack has an `expiration_id`, attach the temporal info and compute
        // `pendingMillis` (millis from now to expiration). `expiration` goes on the
        // wire as epoch seconds (Jackson `Instant` timestamp form).
        let temporal_information = match (r.ti_id, r.ti_expiration) {
            (Some(id), Some(expiration)) => {
                let expiration_ms = expiration.timestamp_millis();
                Some(crate::dto::obtained_unit::TemporalInformationDto {
                    id,
                    duration: r.ti_duration.unwrap_or_default(),
                    expiration: expiration.timestamp() as f64,
                    relation_id: r.ti_relation_id.unwrap_or_default(),
                    pending_millis: expiration_ms - chrono::Utc::now().timestamp_millis(),
                })
            }
            _ => None,
        };
        ObtainedUnitDto {
            id: r.id,
            unit,
            count: r.count,
            source_planet,
            target_planet,
            user_id: r.user_id,
            username: r.username,
            temporal_information,
        }
    }
}

const SELECT_DTO: &str = "\
    SELECT ou.id, ou.count, ou.user_id AS user_id, usr.username AS username, \
           u.id AS unit_id, u.name AS unit_name, u.type AS unit_type_id, ut.name AS unit_type_name, \
           u.attack AS unit_attack, u.health AS unit_health, u.shield AS unit_shield, u.charge AS unit_charge, \
           u.is_unique AS unit_is_unique, u.can_fast_explore AS unit_can_fast_explore, u.speed AS unit_speed, \
           u.bypass_shield AS unit_bypass_shield, u.is_invisible AS unit_is_invisible, \
           u.stored_weight AS unit_stored_weight, u.storage_capacity AS unit_storage_capacity, \
           sp.id AS sp_id, sp.name AS sp_name, sp.sector AS sp_sector, sp.quadrant AS sp_quadrant, \
           sp.planet_number AS sp_planet_number, sp.owner AS sp_owner_id, spo.username AS sp_owner_name, \
           sp.richness AS sp_richness, sp.home AS sp_home, sp.galaxy_id AS sp_galaxy_id, spg.name AS sp_galaxy_name, \
           tp.id AS tp_id, tp.name AS tp_name, tp.sector AS tp_sector, tp.quadrant AS tp_quadrant, \
           tp.planet_number AS tp_planet_number, tp.owner AS tp_owner_id, tpo.username AS tp_owner_name, \
           tp.richness AS tp_richness, tp.home AS tp_home, tp.galaxy_id AS tp_galaxy_id, tpg.name AS tp_galaxy_name, \
           ti.id AS ti_id, ti.duration AS ti_duration, ti.expiration AS ti_expiration, ti.relation_id AS ti_relation_id \
    FROM obtained_units ou \
    JOIN units u ON u.id = ou.unit_id \
    LEFT JOIN unit_types ut ON ut.id = u.type \
    JOIN user_storage usr ON usr.id = ou.user_id \
    LEFT JOIN obtained_unit_temporal_information ti ON ti.id = ou.expiration_id \
    LEFT JOIN planets sp ON sp.id = ou.source_planet \
    LEFT JOIN galaxies spg ON spg.id = sp.galaxy_id \
    LEFT JOIN user_storage spo ON spo.id = sp.owner \
    LEFT JOIN planets tp ON tp.id = ou.target_planet \
    LEFT JOIN galaxies tpg ON tpg.id = tp.galaxy_id \
    LEFT JOIN user_storage tpo ON tpo.id = tp.owner ";

pub struct ObtainedUnitBo;

impl ObtainedUnitBo {
    /// `ObtainedUnitFinderBo.findCompletedAsDto(user)` — the
    /// `unit_obtained_change` sync payload. Units on an owned planet
    /// (`source_planet IS NOT NULL`) that are not in a running mission
    /// (`mission_id IS NULL`) and are not stored inside another unit
    /// (`owner_unit_id IS NULL`).
    ///
    /// After building each DTO, applies `HiddenUnitBo.defineHidden`: the
    /// embedded `unit.is_invisible` is set to `true` if the DB flag is set OR
    /// if the user has an active time-special rule of type
    /// `TIME_SPECIAL_IS_ENABLED_DO_HIDE` targeting this unit (by exact unit id
    /// or by unit-type ancestry). Ports `HiddenUnitBo.isHiddenUnitInternal`.
    pub async fn find_completed_dtos(db: &Db, user_id: i32) -> OwgeResult<Vec<ObtainedUnitDto>> {
        let rows = sqlx::query_as::<_, ObtainedUnitRow>(&format!(
            "{SELECT_DTO} \
             WHERE ou.user_id = ? \
               AND ou.source_planet IS NOT NULL \
               AND ou.mission_id IS NULL \
               AND ou.owner_unit_id IS NULL \
             ORDER BY ou.id"
        ))
        .bind(user_id)
        .fetch_all(db)
        .await?;

        // Acquire a single connection for all per-unit time-special rule checks.
        let mut conn = db.acquire().await?;

        let mut dtos = Vec::with_capacity(rows.len());
        for row in rows {
            // `HiddenUnitBo.isHiddenUnitInternal`: is_invisible = DB flag OR
            // an ACTIVE time-special DO_HIDE rule matches this unit.
            let db_invisible = row.unit_is_invisible != 0;
            let unit_id = row.unit_id as i64;
            let unit_type_id = row.unit_type_id.unwrap_or(0);
            let is_invisible = if db_invisible {
                true
            } else {
                ActiveTimeSpecialRuleFinderBo::exists_rule_matching_unit_destination(
                    &mut conn,
                    user_id,
                    unit_id,
                    unit_type_id,
                    "TIME_SPECIAL_IS_ENABLED_DO_HIDE",
                )
                .await?
            };

            let mut dto: ObtainedUnitDto = row.into();
            dto.unit.is_invisible = is_invisible;
            dtos.push(dto);
        }
        Ok(dtos)
    }

    /// `ObtainedUnitBo.saveWithSubtraction(ObtainedUnitDto, handleImprovements)`
    /// — the `game/unit/delete` path: subtract `dto.count` from the obtained
    /// unit identified by `dto.id`, deleting the stack when the count reaches
    /// zero. `findByIdOrDie(dto.id)` — a missing stack is a `NotFound`.
    ///
    /// Subtraction bounds mirror the Java `SgtBackendInvalidInputException`
    /// messages: `count > stack.count` is rejected; `count < stack.count`
    /// decrements (`saveWithChange(-count)`); `count == stack.count` deletes the
    /// row. (`dto.count` is unsigned here, so the Java `count < 0` "dear hacker"
    /// branch is unreachable.)
    ///
    /// `planet_check` matches the requested signature and the REST `delete`
    /// passing `true`; the Java `saveWithSubtraction(dto, true)` path loads the
    /// stack purely by id and performs no ownership SQL, so the flag carries no
    /// extra validation here (faithful to Java behaviour).
    ///
    /// Mirrors Java `ObtainedUnitBo.saveWithSubtraction`: on a partial subtract OR
    /// a full delete (but NOT the error branches), fire
    /// `requirementBo.triggerUnitBuildCompletedOrKilled(user, unit)` so the
    /// HAVE_UNIT / UNIT_AMOUNT relations gated on this unit are re-evaluated. The
    /// whole operation runs in one transaction (Java is `@Transactional`); the
    /// trigger mutates `unlocked_relation` and must commit atomically with the
    /// stack change.
    ///
    /// The improvement source cache eviction + `user_improvements_change` emit
    /// (`improvementBo.clearSourceCache(user, obtainedUnitImprovementCalculationService)`)
    /// is wired below (unconditional, matching the `handleImprovements = true` path
    /// from the REST `delete` endpoint). The user-data / unit-type / obtained-unit
    /// websocket events (`emitUserData`, `emitUserChange`, `emitObtainedUnits`) are
    /// M4 remainders deferred with the rest of the event-emitter work.
    pub async fn save_with_subtraction(
        db: &Db,
        dto: &ObtainedUnitDto,
        _planet_check: bool,
    ) -> OwgeResult<()> {
        let row: Option<(u64, u16)> =
            sqlx::query_as("SELECT count, unit_id FROM obtained_units WHERE id = ?")
                .bind(dto.id)
                .fetch_optional(db)
                .await?;
        let (stack_count, unit_id) = row.ok_or_else(|| {
            OwgeError::NotFound(format!("No obtained unit with id {}", dto.id))
        })?;
        let subtraction_count = dto.count;
        if subtraction_count > stack_count {
            return Err(OwgeError::InvalidInput(
                "Can't not subtract because, obtainedUnit count is less than the amount to subtract"
                    .to_string(),
            ));
        }
        let mut tx = db.begin().await?;
        if subtraction_count < stack_count {
            // saveWithChange(obtainedUnit, -subtractionCount).
            sqlx::query("UPDATE obtained_units SET count = count - ? WHERE id = ?")
                .bind(subtraction_count)
                .bind(dto.id)
                .execute(&mut *tx)
                .await?;
        } else {
            sqlx::query("DELETE FROM obtained_units WHERE id = ?")
                .bind(dto.id)
                .execute(&mut *tx)
                .await?;
        }
        // requirementBo.triggerUnitBuildCompletedOrKilled(user, unit) — re-evaluate
        // HAVE_UNIT then UNIT_AMOUNT relations for this unit.
        let user = crate::bo::mission_bo::load_user_storage(&mut tx, dto.user_id).await?;
        let mut req_emits = Vec::new();
        crate::bo::requirement_bo::RequirementBo::trigger_unit_build_completed_or_killed(
            &mut tx,
            &user,
            unit_id as i64,
            &mut req_emits,
        )
        .await?;
        tx.commit().await?;
        // Requirement-trigger unlock pushes from the removed units (after commit).
        crate::bo::realtime_emitter::drain_requirement_emits(db, &req_emits).await?;
        // clearSourceCache: removing units changes the obtained-unit improvement source.
        crate::bo::UserImprovementBo::evict_and_emit(db, dto.user_id).await?;
        Ok(())
    }
}
