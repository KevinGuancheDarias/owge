//! Port of the mission `processor` package
//! (`com.kevinguanchedarias.owgejava.business.mission.processor.*`).
//!
//! Each Java processor is a Spring `@Service` implementing `MissionProcessor`,
//! whose `process(mission, involvedUnits)` runs the mission's effect and returns
//! a [`UnitMissionReportBuilder`] (or `null` when the mission produced no
//! report). The Rust port keeps one module per processor, each exposing a free
//! `process(conn, mission, involved_units, db) -> OwgeResult<Option<...>>`
//! function (no DI container — dependencies are explicit `conn`/`db` args, as the
//! contract requires). The [`dispatch`] helper picks the right processor for a
//! mission type, mirroring `UnitMissionBo`'s `processors.stream().filter(supports)`.
//!
//! Shared, read-only helpers live here so every processor builds the same
//! report base (`UnitMissionReportBuilder.create(user, source, target, units)`)
//! without re-deriving the SQL.

pub mod attack;
pub mod conquest;
pub mod counterattack;
pub mod deploy;
pub mod establish_base;
pub mod explore;
pub mod gather;
pub mod return_mission;

use crate::bo::emitter::unit_type_emitter::UnitTypeEmitter;
use crate::builder::UnitMissionReportBuilder;
use crate::dto::obtained_unit::{ObtainedUnitDto, ObtainedUnitUnitDto};
use crate::dto::PlanetDto;
use crate::error::OwgeResult;
use crate::model::mission::{Mission, MissionType};
use crate::model::obtained_unit::ObtainedUnit;
use crate::model::planet::PlanetDtoRow;
use sqlx::MySqlConnection;

/// `UnitMissionBo` dispatch — route a (unit) mission to its processor and return
/// the produced report builder, if any. `db` is threaded for the processors that
/// need pool-side autonomous reads; the locked-section mutations run on `conn`.
///
/// The non-unit/foreign mission types (`LEVEL_UP`, `BUILD_UNIT`, ...) are handled
/// by other subsystems, not this dispatch, and yield `None`.
pub async fn dispatch(
    conn: &mut MySqlConnection,
    mission: &Mission,
    involved_units: &[ObtainedUnit],
    db: &crate::db::Db,
    emits: &mut Vec<DeferredEmit>,
) -> OwgeResult<Option<UnitMissionReportBuilder>> {
    let Some(mission_type) = mission.mission_type() else {
        return Ok(None);
    };
    match mission_type {
        MissionType::Explore => explore::process(conn, mission, involved_units, db, emits).await,
        MissionType::Gather => gather::process(conn, mission, involved_units, db, emits).await,
        MissionType::EstablishBase => {
            establish_base::process(conn, mission, involved_units, db, emits).await
        }
        MissionType::Attack => attack::process(conn, mission, involved_units, db, emits).await,
        MissionType::Counterattack => {
            counterattack::process(conn, mission, involved_units, db, emits).await
        }
        MissionType::Conquest => conquest::process(conn, mission, involved_units, db, emits).await,
        MissionType::Deploy => deploy::process(conn, mission, involved_units, db, emits).await,
        MissionType::ReturnMission => {
            return_mission::process(conn, mission, involved_units, db, emits).await
        }
        _ => Ok(None),
    }
}

/// Build the shared report base
/// (`UnitMissionReportBuilder.create(user, sourcePlanet, targetPlanet, involvedUnits)`)
/// for `mission`, resolving the sender user, the source/target planet DTOs and the
/// involved-unit DTOs from the database. Used by every report-producing processor.
pub(crate) async fn create_report_base(
    conn: &mut MySqlConnection,
    mission: &Mission,
    involved_units: &[ObtainedUnit],
) -> OwgeResult<UnitMissionReportBuilder> {
    let (user_id, username) = load_sender_user(conn, mission).await?;
    let source_planet = match mission.source_planet {
        Some(id) => load_planet_dto(conn, id as u64).await?,
        None => None,
    };
    let target_planet = match mission.target_planet {
        Some(id) => load_planet_dto(conn, id as u64).await?,
        None => None,
    };
    let involved_dtos = involved_units_to_dtos(conn, involved_units).await?;
    Ok(UnitMissionReportBuilder::create_with(
        user_id,
        &username,
        source_planet.as_ref(),
        target_planet.as_ref(),
        &involved_dtos,
    ))
}

/// `mission.getUser()` reduced to the `{id, username}` the report embeds.
pub(crate) async fn load_sender_user(
    conn: &mut MySqlConnection,
    mission: &Mission,
) -> OwgeResult<(i32, String)> {
    let user_id = mission.user_id.unwrap_or_default();
    let username: Option<String> =
        sqlx::query_scalar("SELECT username FROM user_storage WHERE id = ?")
            .bind(user_id)
            .fetch_optional(&mut *conn)
            .await?;
    Ok((user_id, username.unwrap_or_default()))
}

/// Load a single planet DTO by id (joined with galaxy/owner names), matching
/// `PlanetBo.toDto`. Returns `None` when the planet no longer exists.
pub(crate) async fn load_planet_dto(
    conn: &mut MySqlConnection,
    planet_id: u64,
) -> OwgeResult<Option<PlanetDto>> {
    let row: Option<PlanetDtoRow> = sqlx::query_as::<_, PlanetDtoRow>(SELECT_PLANET_DTO)
        .bind(planet_id)
        .fetch_optional(&mut *conn)
        .await?;
    Ok(row.map(Into::into))
}

/// Convert the mission's involved `ObtainedUnit` rows into `ObtainedUnitDto`s for
/// the report's `involvedUnits` block. Each is enriched with its unit/planet
/// scalars; stacks whose `units` row vanished are skipped.
pub(crate) async fn involved_units_to_dtos(
    conn: &mut MySqlConnection,
    involved_units: &[ObtainedUnit],
) -> OwgeResult<Vec<ObtainedUnitDto>> {
    let mut out = Vec::with_capacity(involved_units.len());
    for ou in involved_units {
        if let Some(dto) = load_obtained_unit_dto(conn, ou.id).await? {
            out.push(dto);
        }
    }
    Ok(out)
}

/// Load one `obtained_units` row as a DTO (unit scalars + source/target planets).
pub(crate) async fn load_obtained_unit_dto(
    conn: &mut MySqlConnection,
    obtained_unit_id: u64,
) -> OwgeResult<Option<ObtainedUnitDto>> {
    let row: Option<ObtainedUnitDtoRow> = sqlx::query_as::<_, ObtainedUnitDtoRow>(&format!(
        "{SELECT_OBTAINED_UNIT_DTO} WHERE ou.id = ?"
    ))
    .bind(obtained_unit_id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(row.map(Into::into))
}

const SELECT_PLANET_DTO: &str = "\
    SELECT p.id, p.name, p.sector, p.quadrant, p.planet_number AS planet_number, \
           p.owner AS owner_id, o.username AS owner_name, p.richness, \
           COALESCE(p.home, 0) AS home, p.galaxy_id AS galaxy_id, g.name AS galaxy_name \
    FROM planets p \
    JOIN galaxies g ON g.id = p.galaxy_id \
    LEFT JOIN user_storage o ON o.id = p.owner \
    WHERE p.id = ?";

// --- obtained-unit DTO row (mirrors obtained_unit_bo's SELECT_DTO, single id) ---

#[derive(sqlx::FromRow)]
struct ObtainedUnitDtoRow {
    id: u64,
    count: u64,
    user_id: i32,
    username: Option<String>,

    unit_id: u16,
    unit_name: String,
    unit_type_id: Option<u16>,
    unit_type_name: Option<String>,
    unit_attack: Option<u16>,
    unit_health: Option<u16>,
    unit_shield: Option<u16>,
    unit_charge: Option<u16>,
    unit_is_unique: u8,
    unit_can_fast_explore: i8,
    unit_speed: Option<f64>,
    unit_bypass_shield: i8,
    unit_is_invisible: i8,
    unit_stored_weight: u32,
    unit_storage_capacity: Option<u32>,
    unit_description: Option<String>,
    unit_image: Option<u64>,
    unit_image_filename: Option<String>,
    unit_points: Option<u32>,
    unit_time: Option<i32>,
    unit_primary_resource: Option<u32>,
    unit_secondary_resource: Option<u32>,
    unit_energy: Option<u16>,
    unit_cloned_improvements: i8,
    unit_has_to_display_in_requirements: i8,

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

impl From<ObtainedUnitDtoRow> for ObtainedUnitDto {
    fn from(r: ObtainedUnitDtoRow) -> Self {
        let unit = ObtainedUnitUnitDto {
            id: r.unit_id,
            name: r.unit_name,
            description: r.unit_description,
            image: r.unit_image,
            image_url: r
                .unit_image_filename
                .as_deref()
                .map(crate::dto::obtained_unit::compute_unit_image_url),
            has_to_display_in_requirements: r.unit_has_to_display_in_requirements != 0,
            points: r.unit_points,
            time: r.unit_time,
            primary_resource: r.unit_primary_resource,
            secondary_resource: r.unit_secondary_resource,
            energy: r.unit_energy,
            type_id: r.unit_type_id,
            type_name: r.unit_type_name,
            attack: r.unit_attack,
            health: r.unit_health,
            shield: r.unit_shield,
            charge: r.unit_charge,
            is_unique: r.unit_is_unique != 0,
            can_fast_explore: r.unit_can_fast_explore != 0,
            speed: r.unit_speed,
            cloned_improvements: r.unit_cloned_improvements != 0,
            bypass_shield: r.unit_bypass_shield != 0,
            is_invisible: r.unit_is_invisible != 0,
            stored_weight: r.unit_stored_weight,
            storage_capacity: r.unit_storage_capacity,
        };
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
        ObtainedUnitDto {
            id: r.id,
            unit,
            count: r.count,
            source_planet,
            target_planet,
            user_id: r.user_id,
            username: r.username,
            temporal_information: None,
        }
    }
}

const SELECT_OBTAINED_UNIT_DTO: &str = "\
    SELECT ou.id, ou.count, ou.user_id AS user_id, usr.username AS username, \
           u.id AS unit_id, u.name AS unit_name, u.type AS unit_type_id, ut.name AS unit_type_name, \
           u.attack AS unit_attack, u.health AS unit_health, u.shield AS unit_shield, u.charge AS unit_charge, \
           u.is_unique AS unit_is_unique, u.can_fast_explore AS unit_can_fast_explore, u.speed AS unit_speed, \
           u.bypass_shield AS unit_bypass_shield, u.is_invisible AS unit_is_invisible, \
           u.stored_weight AS unit_stored_weight, u.storage_capacity AS unit_storage_capacity, \
           u.description AS unit_description, u.image_id AS unit_image, uimg.filename AS unit_image_filename, \
           u.points AS unit_points, u.time AS unit_time, u.primary_resource AS unit_primary_resource, \
           u.secondary_resource AS unit_secondary_resource, u.energy AS unit_energy, \
           u.cloned_improvements AS unit_cloned_improvements, \
           u.display_in_requirements AS unit_has_to_display_in_requirements, \
           sp.id AS sp_id, sp.name AS sp_name, sp.sector AS sp_sector, sp.quadrant AS sp_quadrant, \
           sp.planet_number AS sp_planet_number, sp.owner AS sp_owner_id, spo.username AS sp_owner_name, \
           sp.richness AS sp_richness, sp.home AS sp_home, sp.galaxy_id AS sp_galaxy_id, spg.name AS sp_galaxy_name, \
           tp.id AS tp_id, tp.name AS tp_name, tp.sector AS tp_sector, tp.quadrant AS tp_quadrant, \
           tp.planet_number AS tp_planet_number, tp.owner AS tp_owner_id, tpo.username AS tp_owner_name, \
           tp.richness AS tp_richness, tp.home AS tp_home, tp.galaxy_id AS tp_galaxy_id, tpg.name AS tp_galaxy_name \
    FROM obtained_units ou \
    JOIN units u ON u.id = ou.unit_id \
    LEFT JOIN unit_types ut ON ut.id = u.type \
    LEFT JOIN images_store uimg ON uimg.id = u.image_id \
    JOIN user_storage usr ON usr.id = ou.user_id \
    LEFT JOIN planets sp ON sp.id = ou.source_planet \
    LEFT JOIN galaxies spg ON spg.id = sp.galaxy_id \
    LEFT JOIN user_storage spo ON spo.id = sp.owner \
    LEFT JOIN planets tp ON tp.id = ou.target_planet \
    LEFT JOIN galaxies tpg ON tpg.id = tp.galaxy_id \
    LEFT JOIN user_storage tpo ON tpo.id = tp.owner ";

// --- shared mutation helpers used by several processors ---

/// `attackMissionManagerBo.isAttackTriggerEnabledForMission(missionType)` —
/// reads `MISSION_<CODE>_TRIGGER_ATTACK` (default `FALSE`).
pub(crate) async fn is_attack_trigger_enabled(
    conn: &mut MySqlConnection,
    mission_type: MissionType,
) -> OwgeResult<bool> {
    let name = format!("MISSION_{}_TRIGGER_ATTACK", mission_type.code());
    let value: Option<String> =
        sqlx::query_scalar("SELECT value FROM configuration WHERE name = ?")
            .bind(&name)
            .fetch_optional(&mut *conn)
            .await?;
    let value = match value {
        Some(v) => v,
        None => {
            // findOrSetDefault writes the default the first time it is read.
            sqlx::query(
                "INSERT INTO configuration (name, value, privileged) VALUES (?, 'FALSE', 0)",
            )
            .bind(&name)
            .execute(&mut *conn)
            .await?;
            "FALSE".to_string()
        }
    };
    Ok(value.eq_ignore_ascii_case("true"))
}

/// `obtainedUnitRepository.areUnitsInvolved(userId, alliance, relatedPlanetId)` —
/// whether any defender (other than the invoker's own alliance) is involved at the
/// target planet, the precondition `triggerAttackIfRequired` checks before firing
/// an event-triggered attack.
///
/// The Java query (`ObtainedUnitRepository.areUnitsInvolved`) matches obtained
/// units involved-in-attack at the planet whose owner is *not* the invoker and is
/// not in the invoker's alliance. We replicate it against the
/// involved-in-attack definition used by the combat manager.
pub(crate) async fn are_units_involved(
    conn: &mut MySqlConnection,
    invoker_user_id: i32,
    invoker_alliance_id: Option<u16>,
    target_planet_id: u64,
) -> OwgeResult<bool> {
    let count: i64 = sqlx::query_scalar(ARE_UNITS_INVOLVED_SQL)
        .bind(target_planet_id) // sourcePlanet, mission NULL
        .bind(target_planet_id) // DEPLOYED targetPlanet
        .bind(target_planet_id) // CONQUEST targetPlanet
        .bind(invoker_user_id)
        .bind(invoker_alliance_id)
        .bind(invoker_alliance_id)
        .fetch_one(&mut *conn)
        .await?;
    Ok(count > 0)
}

const ARE_UNITS_INVOLVED_SQL: &str = "\
    SELECT COUNT(*) \
      FROM obtained_units ou \
      JOIN user_storage us ON us.id = ou.user_id \
      LEFT JOIN missions m ON m.id = ou.mission_id \
      LEFT JOIN mission_types mt ON mt.id = m.type \
     WHERE ( \
             (ou.mission_id IS NULL AND ou.source_planet = ?) \
             OR (ou.target_planet = ? AND mt.code = 'DEPLOYED') \
             OR (ou.target_planet = ? AND mt.code = 'CONQUEST' \
                 AND m.required_time * 0.1 < TIME_TO_SEC(TIMEDIFF(UTC_TIMESTAMP(), m.starting_date))) \
           ) \
       AND ou.user_id <> ? \
       AND (us.alliance_id IS NULL OR ? IS NULL OR us.alliance_id <> ?)";

/// `planetBo.hasMaxPlanets(user)` — the user already owns >= faction.maxPlanets.
pub(crate) async fn has_max_planets(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<bool> {
    // factions.max_planets is `tinyint UNSIGNED` -> u8 (decode at literal type).
    let row: Option<(u8,)> = sqlx::query_as(
        "SELECT f.max_planets \
           FROM user_storage us JOIN factions f ON f.id = us.faction \
          WHERE us.id = ?",
    )
    .bind(user_id)
    .fetch_optional(&mut *conn)
    .await?;
    let faction_max = row.map(|r| r.0).unwrap_or(0);
    let owned: i64 = sqlx::query_scalar("SELECT COUNT(*) FROM planets WHERE owner = ?")
        .bind(user_id)
        .fetch_one(&mut *conn)
        .await?;
    Ok(owned >= faction_max as i64)
}

/// `planetBo.isHomePlanet(planet)` — the planet's `home` flag is set.
pub(crate) async fn is_home_planet(conn: &mut MySqlConnection, planet_id: u64) -> OwgeResult<bool> {
    let home: Option<Option<i8>> = sqlx::query_scalar("SELECT home FROM planets WHERE id = ?")
        .bind(planet_id)
        .fetch_optional(&mut *conn)
        .await?;
    Ok(matches!(home, Some(Some(h)) if h != 0))
}

/// `planetBo.definePlanetAsOwnedBy(owner, involvedUnits, targetPlanet)` — set the
/// planet's owner, land the involved units on it (clearing their mission/target/
/// owner-unit), and re-home the owner's previously-deployed units there.
///
/// The Java `definePlanetAsOwnedBy` also calls `maybeTriggerSpecialLocation`, but
/// that special-location requirement trigger is intentionally NOT fired here: it is
/// centralized in `conquest::process` (the only caller for which the conquered
/// planet has special-location semantics and a former owner). This helper is also
/// reached by the Return / re-home path, where re-evaluating HAVE_SPECIAL_LOCATION
/// for the mover would be wrong, so firing it here would double-fire / mis-fire.
///
/// The remaining websocket side effects (`emitPlanetOwnedChange`,
/// `emitEnemyMissionsChange`, `emitObtainedUnits`, `planetListBo.emitByChangedPlanet`)
/// are emitted post-commit via `DeferredEmit::ConquestSuccess` — no persisted state
/// effect beyond what is written here.
pub(crate) async fn define_planet_as_owned_by(
    conn: &mut MySqlConnection,
    owner_id: i32,
    involved_units: &[ObtainedUnit],
    target_planet_id: u64,
) -> OwgeResult<()> {
    sqlx::query("UPDATE planets SET owner = ? WHERE id = ?")
        .bind(owner_id)
        .bind(target_planet_id)
        .execute(&mut *conn)
        .await?;
    for ou in involved_units {
        sqlx::query(
            "UPDATE obtained_units \
                SET source_planet = ?, target_planet = NULL, mission_id = NULL, owner_unit_id = NULL \
              WHERE id = ?",
        )
        .bind(target_planet_id)
        .bind(ou.id)
        .execute(&mut *conn)
        .await?;
    }

    // Re-home the owner's previously DEPLOYED units sitting on this planet, then
    // delete their now-empty deployed missions (Java moveUnit + missionRepository.delete).
    let deployed: Vec<(u64, Option<u64>)> = sqlx::query_as(
        "SELECT ou.id, ou.mission_id \
           FROM obtained_units ou \
           JOIN missions m ON m.id = ou.mission_id \
           JOIN mission_types mt ON mt.id = m.type \
          WHERE ou.user_id = ? AND ou.target_planet = ? AND mt.code = 'DEPLOYED'",
    )
    .bind(owner_id)
    .bind(target_planet_id)
    .fetch_all(&mut *conn)
    .await?;
    for (ou_id, mission_id) in deployed {
        move_unit_to_planet(conn, ou_id, owner_id, target_planet_id).await?;
        if let Some(mission_id) = mission_id {
            sqlx::query("DELETE FROM missions WHERE id = ?")
                .bind(mission_id)
                .execute(&mut *conn)
                .await?;
        }
    }
    Ok(())
}

/// `ObtainedUnitBo.moveUnit(unit, userId, planetId)` — the path taken when the
/// destination planet is the user's own property (`isOfUserProperty`): the stack
/// lands on the planet (`source_planet = planet`, `mission/target/owner_unit`
/// cleared), merging into an existing identical stack when one is already there
/// (`saveWithAdding`).
///
/// Parity scope: this ports the owned-planet branch (used by Return and by
/// `definePlanetAsOwnedBy`'s re-home loop and by Deploy when the target is owned).
/// The "not my planet" branch (creating/attaching a `DEPLOYED` mission) is ported
/// in the deploy processor with a `// TODO(M3)` where the deployed-mission finder
/// is required.
pub(crate) async fn move_unit_to_planet(
    conn: &mut MySqlConnection,
    obtained_unit_id: u64,
    user_id: i32,
    planet_id: u64,
) -> OwgeResult<()> {
    let is_owned = is_planet_owned_by(conn, user_id, planet_id).await?;
    if !is_owned {
        // The caller is responsible for the non-owned (deployed) branch; here we
        // only land it on the planet as a fallback so state stays consistent.
        // TODO(M3): non-owned moveUnit (DEPLOYED mission creation) — see deploy.rs.
        sqlx::query("UPDATE obtained_units SET source_planet = ?, target_planet = ? WHERE id = ?")
            .bind(planet_id)
            .bind(planet_id)
            .bind(obtained_unit_id)
            .execute(&mut *conn)
            .await?;
        return Ok(());
    }

    // Identify the moving stack's (unit_id, expiration_id) to find a merge target.
    let row: Option<(u16, Option<u32>, u64)> =
        sqlx::query_as("SELECT unit_id, expiration_id, count FROM obtained_units WHERE id = ?")
            .bind(obtained_unit_id)
            .fetch_optional(&mut *conn)
            .await?;
    let Some((unit_id, expiration_id, count)) = row else {
        return Ok(());
    };

    // saveWithAdding: an existing stack of the same unit on this planet, not in a
    // mission, with matching expiration, absorbs the count.
    let existing: Option<u64> = match expiration_id {
        None => {
            sqlx::query_scalar(
                "SELECT id FROM obtained_units \
              WHERE user_id = ? AND unit_id = ? AND source_planet = ? \
                AND mission_id IS NULL AND expiration_id IS NULL AND owner_unit_id IS NULL \
                AND id <> ? LIMIT 1",
            )
            .bind(user_id)
            .bind(unit_id)
            .bind(planet_id)
            .bind(obtained_unit_id)
            .fetch_optional(&mut *conn)
            .await?
        }
        Some(exp) => {
            sqlx::query_scalar(
                "SELECT id FROM obtained_units \
              WHERE user_id = ? AND unit_id = ? AND source_planet = ? \
                AND mission_id IS NULL AND expiration_id = ? AND owner_unit_id IS NULL \
                AND id <> ? LIMIT 1",
            )
            .bind(user_id)
            .bind(unit_id)
            .bind(planet_id)
            .bind(exp)
            .bind(obtained_unit_id)
            .fetch_optional(&mut *conn)
            .await?
        }
    };

    if let Some(existing_id) = existing {
        sqlx::query("UPDATE obtained_units SET count = count + ? WHERE id = ?")
            .bind(count)
            .bind(existing_id)
            .execute(&mut *conn)
            .await?;
        sqlx::query("DELETE FROM obtained_units WHERE id = ?")
            .bind(obtained_unit_id)
            .execute(&mut *conn)
            .await?;
    } else {
        sqlx::query(
            "UPDATE obtained_units \
                SET source_planet = ?, target_planet = NULL, mission_id = NULL, owner_unit_id = NULL \
              WHERE id = ?",
        )
        .bind(planet_id)
        .bind(obtained_unit_id)
        .execute(&mut *conn)
        .await?;
    }
    Ok(())
}

// ---------------------------------------------------------------------------
// Deferred (post-commit) websocket emits
// ---------------------------------------------------------------------------

/// A websocket emit a processor schedules while it runs inside the firing
/// transaction, to be executed by `unit_mission_bo::do_run_unit_mission` AFTER
/// the transaction commits.
///
/// Mirrors the Java processors' post-commit emit blocks
/// (`transactionUtilService.doAfterCommit(...)` / `emit*AfterCommit`): those
/// observe committed state and so cannot run on the borrowed `conn`. The
/// processors push these into a `Vec`; the firing path drains them once the tx
/// has committed and the planet locks have been released.
pub enum DeferredEmit {
    /// `PlanetExplorationService.defineAsExplored` — one-time `planet_explored_event`
    /// with the freshly-explored planet DTO (only when newly explored).
    PlanetExplored {
        user_id: i32,
        planet: Box<PlanetDto>,
    },
    /// `GatherMissionProcessor` — one-time `mission_gather_result`.
    GatherResult {
        user_id: i32,
        primary: f64,
        secondary: f64,
    },
    /// `AttackMissionManagerBo.startAttack` + `updatePoints` + `AttackMissionProcessor`
    /// per-user emit block (see [`AttackEmit`]).
    Attack(AttackEmit),
    /// `ConquestMissionProcessor` success branch — `PlanetBo.definePlanetAsOwnedBy`
    /// (new owner) plus the old-owner emits.
    ConquestSuccess {
        new_owner_id: i32,
        target_planet_id: u64,
        old_owner_id: Option<i32>,
    },
    /// `missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission)`.
    LocalMissionChange { mission_id: u64, user_id: i32 },
    /// A requirement-trigger `*_unlocked_change` push collected while a processor
    /// re-evaluated unlocks inside the firing tx (see
    /// [`crate::bo::realtime_emitter::RequirementEmit`]).
    Requirement(crate::bo::realtime_emitter::RequirementEmit),
    /// `MissionReportBo.emitOneToUser` — a report row was inserted (owner or
    /// enemy) inside the firing tx; after commit, push `mission_report_new` +
    /// `mission_report_count_change` to its recipient.
    MissionReport { user_id: i32, report_id: u64 },
}

/// Data `AttackMissionManagerBo` collects during combat that drives the per-user
/// emit block of `AttackMissionManagerBo.startAttack` / `updatePoints` and the
/// conditional `AttackMissionProcessor.emitLocalMissionChangeAfterCommit`.
pub struct AttackEmit {
    pub mission_id: u64,
    pub mission_user_id: i32,
    /// `attackInformation.isRemoved()` — the attack mission ran out of units.
    pub removed: bool,
    /// `targetPlanet.getOwner()` at combat time (pre-conquest reassignment).
    pub target_owner: Option<i32>,
    /// `attackInformation.getUsersWithDeletedMissions()`.
    pub users_with_deleted_missions: Vec<i32>,
    /// `attackInformation.getUsersWithChangedCounts()`.
    pub users_with_changed_counts: Vec<i32>,
    /// `updatePoints` altered users (stacks saved with `saveWithChange`) ∪ changed counts.
    pub altered_users: Vec<i32>,
}

impl DeferredEmit {
    /// Execute the emit against the (now-committed) DB.
    pub async fn run(&self, db: &crate::db::Db) -> OwgeResult<()> {
        use crate::bo::realtime_emitter;
        use crate::bo::user_event_emitter::UserEventEmitter;
        use crate::bo::{MissionEventEmitter, ObtainedUnitEventEmitter};
        match self {
            DeferredEmit::PlanetExplored { user_id, planet } => {
                realtime_emitter::send_planet_explored_event(
                    *user_id,
                    serde_json::to_value(planet.as_ref())?,
                )
                .await
            }
            DeferredEmit::GatherResult {
                user_id,
                primary,
                secondary,
            } => {
                let value = serde_json::json!({
                    "primaryResource": primary,
                    "secondaryResource": secondary,
                });
                realtime_emitter::send_gather_result(*user_id, value).await
            }
            DeferredEmit::LocalMissionChange {
                mission_id,
                user_id,
            } => MissionEventEmitter::emit_local_mission_change(db, *mission_id, *user_id).await,
            DeferredEmit::ConquestSuccess {
                new_owner_id,
                target_planet_id,
                old_owner_id,
            } => {
                // PlanetBo.definePlanetAsOwnedBy (new owner) post-commit block.
                realtime_emitter::emit_planet_owned_change(db, *new_owner_id).await?;
                MissionEventEmitter::emit_enemy_missions_change(db, *new_owner_id).await?;
                ObtainedUnitEventEmitter::emit_obtained_units(db, *new_owner_id).await?;
                // planetListBo.emitByChangedPlanet(targetPlanet): everyone whose
                // saved planet list contains this planet.
                for uid in find_planet_list_holders(db, *target_planet_id).await? {
                    realtime_emitter::emit_planet_user_list_change(db, uid).await?;
                }
                // ConquestMissionProcessor old-owner branch.
                if let Some(old) = old_owner_id {
                    realtime_emitter::emit_planet_owned_change(db, *old).await?;
                    MissionEventEmitter::emit_enemy_missions_change(db, *old).await?;
                }
                Ok(())
            }
            DeferredEmit::Attack(a) => {
                use std::collections::HashSet;
                // clearSourceCache(user, obtainedUnitImprovementCalculationService)
                // for every user whose unit counts changed or were wiped.
                let affected: HashSet<i32> = a
                    .altered_users
                    .iter()
                    .chain(&a.users_with_deleted_missions)
                    .chain(&a.users_with_changed_counts)
                    .copied()
                    .collect();
                for uid in &affected {
                    crate::bo::UserImprovementBo::evict_and_emit(db, *uid).await?;
                }
                // updatePoints' doAfterCommit: per altered user, unit_type_change +
                // unit_obtained_change.
                for &uid in &a.altered_users {
                    UnitTypeEmitter::emit_unit_type_change(db, uid).await?;
                    ObtainedUnitEventEmitter::emit_obtained_units(db, uid).await?;
                }
                // startAttack per-user block. The deleted-missions loop removes its
                // users from the changed-counts set first.
                let deleted: HashSet<i32> = a.users_with_deleted_missions.iter().copied().collect();
                let changed: Vec<i32> = a
                    .users_with_changed_counts
                    .iter()
                    .copied()
                    .filter(|u| !deleted.contains(u))
                    .collect();
                for &uid in &a.users_with_deleted_missions {
                    MissionEventEmitter::emit_unit_missions(db, uid).await?;
                    UserEventEmitter::emit_user_data(db, uid).await?;
                }
                for &uid in &changed {
                    if a.target_owner == Some(uid) {
                        ObtainedUnitEventEmitter::emit_obtained_units(db, uid).await?;
                        if !deleted.is_empty() || changed.len() > 1 {
                            MissionEventEmitter::emit_enemy_missions_change(db, uid).await?;
                        }
                    }
                    MissionEventEmitter::emit_unit_missions(db, uid).await?;
                    UserEventEmitter::emit_user_data(db, uid).await?;
                }
                // AttackMissionProcessor.processAttack conditional local change.
                if a.removed || (a.target_owner.is_some() && !deleted.is_empty()) {
                    MissionEventEmitter::emit_local_mission_change(
                        db,
                        a.mission_id,
                        a.mission_user_id,
                    )
                    .await?;
                }
                Ok(())
            }
            DeferredEmit::Requirement(req) => req.run(db).await,
            DeferredEmit::MissionReport { user_id, report_id } => {
                // MissionReportBo.emitOneToUser: the new report, then the count.
                realtime_emitter::emit_mission_report_new(db, *user_id, *report_id).await?;
                realtime_emitter::emit_mission_report_count_change(db, *user_id).await
            }
        }
    }
}

/// `planetListRepository.findUserIdByPlanetListPlanet(planet)` — the ids of users
/// whose saved planet list contains `planet_id`.
async fn find_planet_list_holders(db: &crate::db::Db, planet_id: u64) -> OwgeResult<Vec<i32>> {
    Ok(
        sqlx::query_scalar("SELECT DISTINCT user_id FROM planet_list WHERE planet_id = ?")
            .bind(planet_id)
            .fetch_all(db)
            .await?,
    )
}

/// `planetRepository.isOfUserProperty(userId, planetId)`.
pub(crate) async fn is_planet_owned_by(
    conn: &mut MySqlConnection,
    user_id: i32,
    planet_id: u64,
) -> OwgeResult<bool> {
    let owner: Option<Option<i32>> = sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
        .bind(planet_id)
        .fetch_optional(&mut *conn)
        .await?;
    Ok(matches!(owner, Some(Some(o)) if o == user_id))
}
