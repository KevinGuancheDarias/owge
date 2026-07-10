//! Port of the mission `processor` package
//! (`com.kevinguanchedarias.owgejava.business.mission.processor.*`).
//!
//! Each Java processor is a Spring `@Service` implementing `MissionProcessor`,
//! whose `process(mission, involvedUnits)` runs the mission's effect and returns
//! a [`UnitMissionReportBuilder`] (or `null` when the mission produced no
//! report). The Rust port keeps one module per processor, each exposing a free
//! `process(conn, mission, involved_units) -> OwgeResult<Option<...>>`
//! function (no DI container — the connection comes from the caller). The
//! [`dispatch`] helper picks the right processor for a mission type, mirroring
//! `UnitMissionBo`'s `processors.stream().filter(supports)`.
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

use crate::bo::ImprovementBo;
use crate::bo::ObtainedUnitBo;
use crate::bo::emitter::unit_type_emitter::UnitTypeEmitter;
use crate::builder::UnitMissionReportBuilder;
use crate::dto::PlanetDto;
use crate::dto::obtained_unit::ObtainedUnitDto;
use crate::error::OwgeResult;
use crate::model::mission::{Mission, MissionType};
use crate::model::obtained_unit::ObtainedUnit;
use crate::model::planet::NavPlanetRow;
use sqlx::MySqlConnection;

/// `UnitMissionBo` dispatch — route a (unit) mission to its processor and return
/// the produced report builder, if any.
///
/// The non-unit/foreign mission types (`LEVEL_UP`, `BUILD_UNIT`, ...) are handled
/// by other subsystems, not this dispatch, and yield `None`.
pub async fn dispatch(
    conn: &mut MySqlConnection,
    mission: &Mission,
    involved_units: &[ObtainedUnit],
    emits: &mut Vec<DeferredEmit>,
) -> OwgeResult<Option<UnitMissionReportBuilder>> {
    let Some(mission_type) = mission.mission_type() else {
        return Ok(None);
    };
    match mission_type {
        MissionType::Explore => explore::process(conn, mission, involved_units, emits).await,
        MissionType::Gather => gather::process(conn, mission, involved_units, emits).await,
        MissionType::EstablishBase => {
            establish_base::process(conn, mission, involved_units, emits).await
        }
        MissionType::Attack => attack::process(conn, mission, involved_units, emits).await,
        MissionType::Counterattack => {
            counterattack::process(conn, mission, involved_units, emits).await
        }
        MissionType::Conquest => conquest::process(conn, mission, involved_units, emits).await,
        MissionType::Deploy => deploy::process(conn, mission, involved_units, emits).await,
        MissionType::ReturnMission => {
            return_mission::process(conn, mission, involved_units, emits).await
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
    let mut involved_dtos = involved_units_to_dtos(conn, involved_units).await?;
    for dto in involved_dtos.iter_mut() {
        enrich_unit_for_report(conn, dto).await?;
    }
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

/// Load a single planet DTO by id (joined with galaxy/owner names and its
/// special location) for MISSION payloads (`unit_mission_change` /
/// `enemy_mission_change`, report `json_body`, build-mission `sourcePlanet`):
/// the specialLocation is the SLIM shape Java emits on these paths —
/// `{id, name, description, assignedPlanetId, assignedPlanetName}` — because
/// the location's lazy `galaxy`/`image`/`improvement` associations are never
/// initialized in those transactions and `NON_NULL` drops them (R1/D19; the
/// rich shape observed on `planet_owned_change`/navigate stays in
/// `PlanetBo`). Returns `None` when the planet no longer exists. `home` is
/// left nullable (Java's Jackson `NON_NULL` omits it rather than coercing a
/// NULL to `false`); `is_explored` is hardcoded `1` since mission
/// source/target planets aren't masked on this path (the EXPLORE
/// target-planet masking is applied separately by the caller).
pub(crate) async fn load_planet_dto(
    conn: &mut MySqlConnection,
    planet_id: u64,
) -> OwgeResult<Option<PlanetDto>> {
    let Some(mut dto) = load_planet_dto_rich(&mut *conn, planet_id).await? else {
        return Ok(None);
    };
    if let Some(sl) = dto.special_location.as_mut() {
        sl.galaxy_id = None;
        sl.galaxy_name = None;
        sl.image = None;
        sl.image_url = None;
        sl.improvement = None;
    }
    Ok(Some(dto))
}

/// [`load_planet_dto`] with the RICH specialLocation (galaxy/image/improvement
/// hydrated) — kept for `planet_explored_event`, whose shape follows
/// `PlanetBo.toDto` (status quo; no scenario exercises a special-location
/// planet on the explore path yet).
pub(crate) async fn load_planet_dto_rich(
    conn: &mut MySqlConnection,
    planet_id: u64,
) -> OwgeResult<Option<PlanetDto>> {
    let row: Option<NavPlanetRow> = sqlx::query_as::<_, NavPlanetRow>(SELECT_PLANET_DTO)
        .bind(planet_id)
        .fetch_optional(&mut *conn)
        .await?;
    match row {
        Some(row) => {
            let sl_improvement_id = row.sl_improvement_id;
            let mut dto = PlanetDto::from(row);
            if let Some(sl) = dto.special_location.as_mut() {
                if let Some(improvement_id) = sl_improvement_id {
                    sl.improvement =
                        Some(ImprovementBo::find_dto(&mut *conn, Some(improvement_id)).await?);
                }
            }
            Ok(Some(dto))
        }
        None => Ok(None),
    }
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

/// Report-only enrichment: Java's report builder serializes the raw entity
/// graph inside the open transaction, so the unit's `speedImpactGroup` carries
/// its lazy `requirementsGroups` there — unlike every ws payload, where the
/// finder paths leave the groups out. Mirror that by upgrading the group to
/// the with-groups shape on DTOs bound for `UnitMissionReportBuilder` only.
pub(crate) async fn enrich_unit_for_report(
    conn: &mut MySqlConnection,
    dto: &mut ObtainedUnitDto,
) -> OwgeResult<()> {
    if let Some(group_id) = dto.unit.speed_impact_group.as_ref().map(|g| g.id) {
        dto.unit.speed_impact_group =
            crate::bo::SpeedImpactGroupBo::find_by_id_with_requirement_groups(&mut *conn, group_id)
                .await?;
    }
    Ok(())
}

/// Load one `obtained_units` row as a DTO (unit scalars + source/target planets).
pub(crate) async fn load_obtained_unit_dto(
    conn: &mut MySqlConnection,
    obtained_unit_id: u64,
) -> OwgeResult<Option<ObtainedUnitDto>> {
    ObtainedUnitBo::find_by_id(&mut *conn, obtained_unit_id).await
}

const SELECT_PLANET_DTO: &str = "\
    SELECT p.id, p.name, p.sector, p.quadrant, p.planet_number AS planet_number, \
           p.owner AS owner_id, o.username AS owner_name, p.richness, \
           p.home AS home, p.galaxy_id AS galaxy_id, g.name AS galaxy_name, \
           1 AS is_explored, \
           sl.id AS sl_id, sl.name AS sl_name, sl.description AS sl_description, \
           sl.image_id AS sl_image_id, sli.filename AS sl_image_filename, \
           sl.galaxy_id AS sl_galaxy_id, slg.name AS sl_galaxy_name, \
           sl.improvement_id AS sl_improvement_id \
    FROM planets p \
    JOIN galaxies g ON g.id = p.galaxy_id \
    LEFT JOIN user_storage o ON o.id = p.owner \
    LEFT JOIN special_locations sl ON sl.id = p.special_location_id \
    LEFT JOIN images_store sli ON sli.id = sl.image_id \
    LEFT JOIN galaxies slg ON slg.id = sl.galaxy_id \
    WHERE p.id = ?";

// --- obtained-unit DTO row (mirrors obtained_unit_bo's SELECT_DTO, single id) ---

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
/// owner-unit), re-home the owner's previously-deployed units there, and — like
/// Java's `maybeTriggerSpecialLocation(targetPlanet, owner)` at the end of the
/// helper — re-evaluate the NEW owner's `HAVE_SPECIAL_LOCATION` unlocks when the
/// planet carries a special location (grants the gated units/time specials/etc.;
/// see docs/BUG-SPECIAL-LOCATION-UNLOCK.md — this helper is reached only from the
/// conquest and establish-base processors, exactly as in Java, so the trigger
/// cannot mis-fire from Return/re-home paths).
///
/// Ordering matches Java: the new-owner grant fires here, BEFORE the old-owner
/// revoke that `conquest::process` runs after this helper returns.
///
/// The remaining websocket side effects (`emitPlanetOwnedChange`,
/// `emitEnemyMissionsChange`, `emitObtainedUnits`, `planetListBo.emitByChangedPlanet`)
/// are emitted post-commit via `DeferredEmit::ConquestSuccess` — the requirement
/// emits produced by the grant ride along as `DeferredEmit::Requirement`.
pub(crate) async fn define_planet_as_owned_by(
    conn: &mut MySqlConnection,
    owner_id: i32,
    involved_units: &[ObtainedUnit],
    target_planet_id: u64,
    emits: &mut Vec<DeferredEmit>,
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

    // maybeTriggerSpecialLocation(targetPlanet, owner) — Java PlanetBo.java:194:
    // grant the new owner every relation gated by HAVE_SPECIAL_LOCATION on this
    // planet's special location, inside the same firing transaction.
    if let Some(special_location_id) = find_planet_special_location(conn, target_planet_id).await? {
        let owner = crate::bo::mission_bo::load_user_storage(conn, owner_id).await?;
        let mut req_emits = Vec::new();
        crate::bo::requirement_bo::RequirementBo::trigger_special_location(
            conn,
            &owner,
            special_location_id as i64,
            &mut req_emits,
        )
        .await?;
        for req in req_emits {
            emits.push(DeferredEmit::Requirement(req));
        }
    }
    Ok(())
}

/// `planet.getSpecialLocation()` — the planet's special-location id, if any.
/// Shared by `define_planet_as_owned_by` (new-owner grant) and
/// `conquest::process` (old-owner revoke).
pub(crate) async fn find_planet_special_location(
    conn: &mut MySqlConnection,
    planet_id: u64,
) -> OwgeResult<Option<u16>> {
    let special: Option<Option<u16>> =
        sqlx::query_scalar("SELECT special_location_id FROM planets WHERE id = ?")
            .bind(planet_id)
            .fetch_optional(&mut *conn)
            .await?;
    Ok(special.flatten())
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
    /// `mission_report_count_change` to its recipient. `report_date` is the
    /// insert-time wall clock with the millis the DATETIME column truncates
    /// (D16 — Java emits the in-memory entity's precision).
    MissionReport {
        user_id: i32,
        report_id: u64,
        report_date: chrono::NaiveDateTime,
    },
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
    /// The mission's target planet — the deferred enemy-missions emit re-reads
    /// its owner at drain time (see the drain for why that matters on conquest).
    pub target_planet_id: Option<u64>,
    /// `attackInformation.getUsersWithDeletedMissions()`.
    pub users_with_deleted_missions: Vec<i32>,
    /// `attackInformation.getUsersWithChangedCounts()`.
    pub users_with_changed_counts: Vec<i32>,
    /// `updatePoints` altered users (stacks saved with `saveWithChange`) ∪ changed counts.
    pub altered_users: Vec<i32>,
}

impl DeferredEmit {
    /// Execute the emit against the (now-committed) DB.
    pub async fn run(&self, conn: &mut sqlx::MySqlConnection) -> OwgeResult<()> {
        use crate::bo::realtime_emitter;
        use crate::bo::user_event_emitter::UserEventEmitter;
        use crate::bo::{MissionEventEmitter, ObtainedUnitEventEmitter};
        match self {
            DeferredEmit::PlanetExplored { user_id, planet } => {
                realtime_emitter::send_planet_explored_event(
                    &mut *conn,
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
                realtime_emitter::send_gather_result(&mut *conn, *user_id, value).await
            }
            DeferredEmit::LocalMissionChange {
                mission_id,
                user_id,
            } => {
                MissionEventEmitter::emit_local_mission_change(&mut *conn, *mission_id, *user_id)
                    .await
            }
            DeferredEmit::ConquestSuccess {
                new_owner_id,
                target_planet_id,
                old_owner_id,
            } => {
                // PlanetBo.definePlanetAsOwnedBy (new owner) post-commit block.
                realtime_emitter::emit_planet_owned_change(&mut *conn, *new_owner_id).await?;
                MissionEventEmitter::emit_enemy_missions_change(&mut *conn, *new_owner_id).await?;
                ObtainedUnitEventEmitter::emit_obtained_units(&mut *conn, *new_owner_id).await?;
                // planetListBo.emitByChangedPlanet(targetPlanet): everyone whose
                // saved planet list contains this planet.
                for uid in find_planet_list_holders(&mut *conn, *target_planet_id).await? {
                    realtime_emitter::emit_planet_user_list_change(&mut *conn, uid).await?;
                }
                // ConquestMissionProcessor old-owner branch.
                if let Some(old) = old_owner_id {
                    realtime_emitter::emit_planet_owned_change(&mut *conn, *old).await?;
                    MissionEventEmitter::emit_enemy_missions_change(&mut *conn, *old).await?;
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
                    crate::bo::UserImprovementBo::evict_and_emit(&mut *conn, *uid).await?;
                }
                // updatePoints' doAfterCommit: per altered user, unit_type_change +
                // unit_obtained_change.
                for &uid in &a.altered_users {
                    UnitTypeEmitter::emit_unit_type_change(&mut *conn, uid).await?;
                    ObtainedUnitEventEmitter::emit_obtained_units(&mut *conn, uid).await?;
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
                    MissionEventEmitter::emit_unit_missions(&mut *conn, uid).await?;
                    UserEventEmitter::emit_user_data(&mut *conn, uid).await?;
                }
                for &uid in &changed {
                    if a.target_owner == Some(uid) {
                        ObtainedUnitEventEmitter::emit_obtained_units(&mut *conn, uid).await?;
                        if !deleted.is_empty() || changed.len() > 1 {
                            // Java wraps this one in doAfterCommit AND the lambda
                            // re-reads `targetPlanet.getOwner()` when it fires — so
                            // while the combat-time owner (`target_owner`) gates the
                            // branch, the RECIPIENT is the planet's owner after the
                            // whole mission tx committed. On a successful conquest
                            // definePlanetAsOwnedBy has reassigned the planet by
                            // then, and the frame goes to the NEW owner.
                            if let Some(planet_id) = a.target_planet_id {
                                if let Some(recipient) =
                                    planet_owner(&mut *conn, planet_id).await?
                                {
                                    MissionEventEmitter::emit_enemy_missions_change(
                                        &mut *conn, recipient,
                                    )
                                    .await?;
                                }
                            }
                        }
                    }
                    MissionEventEmitter::emit_unit_missions(&mut *conn, uid).await?;
                    UserEventEmitter::emit_user_data(&mut *conn, uid).await?;
                }
                // AttackMissionProcessor.processAttack conditional local change.
                if a.removed || (a.target_owner.is_some() && !deleted.is_empty()) {
                    MissionEventEmitter::emit_local_mission_change(
                        &mut *conn,
                        a.mission_id,
                        a.mission_user_id,
                    )
                    .await?;
                }
                Ok(())
            }
            DeferredEmit::Requirement(req) => req.run(&mut *conn).await,
            DeferredEmit::MissionReport {
                user_id,
                report_id,
                report_date,
            } => {
                // MissionReportBo.emitOneToUser: the new report, then the count.
                realtime_emitter::emit_mission_report_new(
                    &mut *conn,
                    *user_id,
                    *report_id,
                    *report_date,
                )
                .await?;
                realtime_emitter::emit_mission_report_count_change(&mut *conn, *user_id).await
            }
        }
    }
}

/// `planetListRepository.findUserIdByPlanetListPlanet(planet)` — the ids of users
/// whose saved planet list contains `planet_id`.
async fn find_planet_list_holders(
    conn: &mut sqlx::MySqlConnection,
    planet_id: u64,
) -> OwgeResult<Vec<i32>> {
    Ok(
        sqlx::query_scalar("SELECT DISTINCT user_id FROM planet_list WHERE planet_id = ?")
            .bind(planet_id)
            .fetch_all(&mut *conn)
            .await?,
    )
}

/// The planet's current owner, read fresh (post-commit `targetPlanet.getOwner()`).
async fn planet_owner(
    conn: &mut MySqlConnection,
    planet_id: u64,
) -> OwgeResult<Option<i32>> {
    let owner: Option<Option<i32>> = sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
        .bind(planet_id)
        .fetch_optional(&mut *conn)
        .await?;
    Ok(owner.flatten())
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
