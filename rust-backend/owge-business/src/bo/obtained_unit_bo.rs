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
use crate::bo::obtained_unit_entity::obtained_unit::Entity;
use crate::bo::obtained_unit_entity::{
    galaxy, obtained_unit, planet, temporal_information, unit, unit_type, user_storage,
};
use crate::bo::{ImprovementBo, SpeedImpactGroupBo, UnitInterceptionFinderBo};
use crate::db;
use crate::dto::obtained_unit::{ObtainedUnitDto, TemporalInformationDto};
use crate::dto::{MissionDto, PlanetDto, UnitDto};
use crate::error::{OwgeError, OwgeResult};
use crate::model::seaorm::image_store::ImageSimple;
use sea_orm::{
    ColumnTrait, DerivePartialModel, EntityTrait, JoinType, QueryFilter, QueryOrder, QuerySelect,
    RelationTrait, Select, SelectModel, Selector,
};
use sqlx::{Connection, MySqlConnection};

/// `unit_types.name`, `LEFT JOIN`ed in to populate `UnitDto.type_name`
/// (`units.type` is nullable, hence `Option`).
#[derive(DerivePartialModel)]
#[sea_orm(entity = "unit_type::Entity", from_query_result)]
struct UnitTypeNameRow {
    name: String,
}

/// The `units` row joined in for an obtained-unit stack — exactly the columns
/// the old hand-written `SELECT_DTO` hand-picked (this sync payload predates
/// the full catalog join; see `ObtainedUnitUnitDto`'s/`reduced`'s doc comments
/// for why fields like `description`/`image`/`points` aren't here), now
/// expressed as a SeaORM partial model that decodes straight off the join
/// instead of a hand-aliased SQL string + matching `FromRow` struct.
#[derive(DerivePartialModel)]
#[sea_orm(entity = "unit::Entity", from_query_result)]
struct UnitJoinedRow {
    id: u16,
    name: String,
    description: Option<String>,
    order_number: Option<u16>,
    type_id: Option<u16>,
    // Match the actual `units` column types so SeaORM's generated `unit_*`
    // aliases decode: primary/secondary_resource are INT UNSIGNED (-> u32),
    // time is signed INT (-> i32). Declaring these as u64 made sqlx reject the
    // decode and 500'd the whole `unit_obtained_change` sync key.
    primary_resource: Option<u32>,
    secondary_resource: Option<u32>,
    time: Option<i32>,
    points: Option<u32>,
    energy: Option<u16>,
    display_in_requirements: Option<i8>,
    improvement_id: u16,
    attack_rule_id: Option<u16>,
    critical_attack_id: Option<u16>,

    #[sea_orm(nested)]
    image: Option<ImageSimple>,

    attack: Option<u16>,
    health: Option<u16>,
    shield: Option<u16>,
    charge: Option<u16>,
    is_unique: u8,
    can_fast_explore: i8,
    speed: Option<f64>,
    bypass_shield: i8,
    is_invisible: i8,
    stored_weight: u32,
    storage_capacity: Option<u32>,
    #[sea_orm(nested)]
    unit_type: Option<UnitTypeNameRow>,
}

/// `user_storage.username` — reused both for the obtained-unit's owning user
/// (`INNER JOIN`, always present) and for a planet's owner (`LEFT JOIN`,
/// optional; `PlanetDto.owner_id` comes from `planets.owner` directly, not
/// from this row).
#[derive(DerivePartialModel)]
#[sea_orm(entity = "user_storage::Entity", from_query_result)]
struct UsernameRow {
    username: String,
}

/// `galaxies.name`, joined in for a planet's `PlanetDto.galaxy_name`.
#[derive(DerivePartialModel)]
#[sea_orm(entity = "galaxy::Entity", from_query_result)]
struct GalaxyNameRow {
    name: String,
}

/// One side of the `obtained_units` -> `planets` -> `galaxies`/`user_storage`
/// graph, joined in as the unit's source planet (table aliases `sp` /
/// `sp_galaxy` / `sp_owner` — see `find_completed_dtos`). `TargetPlanetRow`
/// below is identical except for which aliases its nested fields read from:
/// `#[sea_orm(nested, alias = "...")]` is resolved at compile time, so the two
/// sides of this doubly-joined graph can't share one partial-model type.
#[derive(DerivePartialModel)]
#[sea_orm(entity = "planet::Entity", from_query_result)]
struct SourcePlanetRow {
    id: u64,
    name: String,
    sector: u32,
    quadrant: u32,
    planet_number: u16,
    owner: Option<i32>,
    richness: u16,
    home: Option<i8>,
    galaxy_id: u16,
    #[sea_orm(nested, alias = "sp_galaxy")]
    galaxy: GalaxyNameRow,
    #[sea_orm(nested, alias = "sp_owner")]
    owner_info: Option<UsernameRow>,
}

/// The unit's target planet — see [`SourcePlanetRow`] (table aliases `tp` /
/// `tp_galaxy` / `tp_owner`).
#[derive(DerivePartialModel)]
#[sea_orm(entity = "planet::Entity", from_query_result)]
struct TargetPlanetRow {
    id: u64,
    name: String,
    sector: u32,
    quadrant: u32,
    planet_number: u16,
    owner: Option<i32>,
    richness: u16,
    home: Option<i8>,
    galaxy_id: u16,
    #[sea_orm(nested, alias = "tp_galaxy")]
    galaxy: GalaxyNameRow,
    #[sea_orm(nested, alias = "tp_owner")]
    owner_info: Option<UsernameRow>,
}

/// `obtained_unit_temporal_information`, `LEFT JOIN`ed on `expiration_id`
/// (present only for time-special granted unit stacks).
#[derive(DerivePartialModel)]
#[sea_orm(entity = "temporal_information::Entity", from_query_result)]
struct TemporalInformationRow {
    id: u32,
    duration: u32,
    expiration: chrono::DateTime<chrono::Utc>,
    relation_id: u16,
}

/// One `obtained_units` row with every nested relation (`unit` -> `unit_type`,
/// owning user, optional source/target planets -> galaxy/owner, optional
/// temporal information) decoded straight off a single joined query — the
/// direct SeaORM equivalent of the old `ObtainedUnitRow { unit: UnitRow,
/// source_planet: Option<PlanetDtoRow>, ... }` shape that plain `sqlx::FromRow`
/// couldn't decode (nested structs need unique column names, and this query
/// joins `planets`/`galaxies`/`user_storage` *twice*). `DerivePartialModel`'s
/// `#[sea_orm(nested, alias = "...")]` generates the column aliasing for us.
#[derive(DerivePartialModel)]
#[sea_orm(entity = "obtained_unit::Entity", from_query_result)]
struct ObtainedUnitJoinedRow {
    id: u64,
    count: u64,
    user_id: i32,
    mission_id: Option<u64>,
    #[sea_orm(nested)]
    unit: UnitJoinedRow,
    #[sea_orm(nested)]
    owner: UsernameRow,
    #[sea_orm(nested, alias = "sp")]
    source_planet: Option<SourcePlanetRow>,
    #[sea_orm(nested, alias = "tp")]
    target_planet: Option<TargetPlanetRow>,
    #[sea_orm(nested)]
    temporal_information: Option<TemporalInformationRow>,
}

/// The SLIM specialLocation for the planet carrying it (assigned planet = the
/// planet pointing at the location) — the shape Java's NON_NULL mapper leaves
/// when the location's lazy galaxy/image/improvement were never initialized,
/// which is always the case on the `unit_obtained_change` payload (R-class/D19).
async fn load_slim_special_location(
    conn: &mut MySqlConnection,
    planet_id: u64,
) -> OwgeResult<Option<crate::dto::special_location::SpecialLocationDto>> {
    let row: Option<(u16, String, Option<String>, u64, Option<String>)> = sqlx::query_as(
        "SELECT sl.id, sl.name, sl.description, p.id, p.name \
           FROM planets p \
           JOIN special_locations sl ON sl.id = p.special_location_id \
          WHERE p.id = ?",
    )
    .bind(planet_id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(row.map(|(id, name, description, assigned_planet_id, assigned_planet_name)| {
        crate::dto::special_location::SpecialLocationDto {
            id,
            name,
            description: description.unwrap_or_default(),
            image: None,
            image_url: None,
            improvement: None,
            galaxy_id: None,
            galaxy_name: None,
            assigned_planet_id: Some(assigned_planet_id),
            assigned_planet_name,
        }
    }))
}

/// Mirrors `model::planet::planet_dto_from_parts` — both `SourcePlanetRow` and
/// `TargetPlanetRow` decode to the same [`PlanetDto`] shape.
#[allow(clippy::too_many_arguments)]
fn planet_dto_from_joined_parts(
    id: u64,
    name: String,
    sector: u32,
    quadrant: u32,
    planet_number: u16,
    owner_id: Option<i32>,
    richness: u16,
    home: Option<i8>,
    galaxy_id: u16,
    galaxy_name: String,
    owner_name: Option<String>,
) -> PlanetDto {
    PlanetDto {
        id,
        name: Some(name),
        sector,
        quadrant,
        planet_number,
        owner_id,
        owner_name,
        richness: Some(richness),
        home: home.map(|h| h != 0),
        galaxy_id,
        galaxy_name,
        special_location: None,
    }
}

impl From<SourcePlanetRow> for PlanetDto {
    fn from(r: SourcePlanetRow) -> Self {
        planet_dto_from_joined_parts(
            r.id,
            r.name,
            r.sector,
            r.quadrant,
            r.planet_number,
            r.owner,
            r.richness,
            r.home,
            r.galaxy_id,
            r.galaxy.name,
            r.owner_info.map(|o| o.username),
        )
    }
}

impl From<TargetPlanetRow> for PlanetDto {
    fn from(r: TargetPlanetRow) -> Self {
        planet_dto_from_joined_parts(
            r.id,
            r.name,
            r.sector,
            r.quadrant,
            r.planet_number,
            r.owner,
            r.richness,
            r.home,
            r.galaxy_id,
            r.galaxy.name,
            r.owner_info.map(|o| o.username),
        )
    }
}

/// Mirrors `unit_bo::UnitRow`'s `From` impl, but only populating the scalar
/// columns the old `SELECT_DTO` selected — see [`UnitJoinedRow`]'s doc comment.
fn unit_dto_from_joined_row(r: UnitJoinedRow) -> UnitDto {
    UnitDto {
        id: r.id,
        name: r.name,
        description: r.description,
        image: r.image.as_ref().map(|i| i.id),
        image_url: r.image.as_ref().map(|i| format!("/dynamic/{}", i.filename)),
        order: r.order_number,
        has_to_display_in_requirements: r.display_in_requirements.unwrap_or(0) != 0,
        points: r.points,
        time: r.time.map(|v| v as u64),
        primary_resource: r.primary_resource.map(|v| v as u64),
        secondary_resource: r.secondary_resource.map(|v| v as u64),
        energy: r.energy,
        type_id: r.type_id,
        type_name: r.unit_type.map(|ut| ut.name),
        attack: r.attack,
        health: r.health,
        shield: r.shield,
        charge: r.charge,
        is_unique: r.is_unique != 0,
        can_fast_explore: r.can_fast_explore != 0,
        speed: r.speed,
        cloned_improvements: false,
        bypass_shield: r.bypass_shield != 0,
        is_invisible: r.is_invisible != 0,
        stored_weight: r.stored_weight,
        storage_capacity: r.storage_capacity,
        improvement: None,
        speed_impact_group: None,
        attack_rule: None,
        critical_attack: None,
    }
}

impl ObtainedUnitJoinedRow {
    async fn to_dto(self, conn: &mut MySqlConnection) -> OwgeResult<ObtainedUnitDto> {
        let db_invisible = self.unit.is_invisible != 0;
        let unit_id = self.unit.id as i64;
        let unit_type_id = self.unit.type_id.unwrap_or(0);
        let is_invisible = if db_invisible {
            true
        } else {
            ActiveTimeSpecialRuleFinderBo::exists_rule_matching_unit_destination(
                &mut *conn,
                self.user_id,
                unit_id,
                unit_type_id,
                "TIME_SPECIAL_IS_ENABLED_DO_HIDE",
            )
            .await?
        };

        // TemporalInformationUnitDataLoaderService.addInformationToDto: when the
        // stack has an `expiration_id`, attach the temporal info and compute
        // `pendingMillis` (millis from now to expiration). `expiration` goes on the
        // wire as epoch seconds (Jackson `Instant` timestamp form).
        let temporal_information = self.temporal_information.map(|ti| {
            let expiration_ms = ti.expiration.timestamp_millis();
            TemporalInformationDto {
                id: ti.id,
                duration: ti.duration,
                expiration: ti.expiration.timestamp() as f64,
                relation_id: ti.relation_id,
                pending_millis: expiration_ms - chrono::Utc::now().timestamp_millis(),
            }
        });

        // The nested unit mirrors Java's `UnitDto.dtoFromEntity`: its own
        // improvement, and the entity's effective speedImpactGroup — which the
        // JPA getter resolves as own-else-inherited from the unit-type chain
        // (no user-specific time-special swap on this path) — plus its own
        // `attackRule`/`criticalAttack`.
        let own_improvement_id = self.unit.improvement_id;
        let unit_id_u16 = self.unit.id;
        let attack_rule_id = self.unit.attack_rule_id;
        let critical_attack_id = self.unit.critical_attack_id;
        let mut unit = unit_dto_from_joined_row(self.unit);
        unit.is_invisible = is_invisible;
        // Uses the *shallow* improvement shape: on this load path (a unit's own
        // improvement) Java's embedded `unitType` doesn't carry the nested
        // `speedImpactGroup.requirementsGroups` — see
        // `ImprovementBo::find_dto_shallow`'s doc for why.
        unit.improvement = match ImprovementBo::find_dto_shallow(&mut *conn, Some(own_improvement_id)).await {
            Ok(improvement) => Some(improvement),
            Err(OwgeError::NotFound(_)) => None,
            Err(e) => return Err(e),
        };
        unit.speed_impact_group = match UnitInterceptionFinderBo::find_his_or_inherited_speed_impact_group(
            &mut *conn, unit_id_u16,
        )
        .await?
        {
            Some(group_id) => SpeedImpactGroupBo::find_by_id(&mut *conn, group_id).await?,
            None => None,
        };
        unit.attack_rule = match attack_rule_id {
            Some(id) => crate::bo::AttackRuleBo::find_by_id(&mut *conn, id).await?,
            None => None,
        };
        unit.critical_attack = match critical_attack_id {
            Some(id) => crate::bo::CriticalAttackBo::find_by_id(&mut *conn, id).await?,
            None => None,
        };

        // `ObtainedUnitDto.mission` — `DtoUtilService.staticDtoFromEntity(MissionDto.class,
        // entity.getMission())`: present only when this stack is attached to a
        // running mission (the `unit_mission_change` involved-units path); the
        // `unit_obtained_change` completed-units query never has a mission_id.
        let mission = match self.mission_id {
            Some(mission_id) => load_mission_dto(&mut *conn, mission_id).await?,
            None => None,
        };

        Ok(ObtainedUnitDto {
            id: self.id,
            unit,
            count: self.count,
            source_planet: self.source_planet.map(Into::into),
            target_planet: self.target_planet.map(Into::into),
            mission,
            user_id: self.user_id,
            username: Some(self.owner.username),
            temporal_information,
            stored_units: Some(Vec::new()),
        })
    }
}

/// `DtoUtilService.staticDtoFromEntity(MissionDto.class, mission)` for the
/// owning mission of an obtained-unit stack. Returns `None` if the mission row
/// is somehow gone (defensive; `mission_id` is a live FK in practice).
async fn load_mission_dto(
    conn: &mut MySqlConnection,
    mission_id: u64,
) -> OwgeResult<Option<MissionDto>> {
    let row: Option<(Option<chrono::NaiveDateTime>, i8, i8)> = sqlx::query_as(
        "SELECT termination_date, resolved, invisible FROM missions WHERE id = ?",
    )
    .bind(mission_id)
    .fetch_optional(&mut *conn)
    .await?;
    Ok(row.map(|(termination_date, resolved, invisible)| MissionDto {
        id: mission_id,
        termination_date,
        resolved: resolved != 0,
        invisible: invisible != 0,
    }))
}

pub struct ObtainedUnitBo;

impl ObtainedUnitBo {
    /// `ObtainedUnitFinderBo.findCompletedAsDto(user)` — the
    /// `unit_obtained_change` sync payload. Units on an owned planet
    /// (`source_planet IS NOT NULL`) that are not in a running mission
    /// (`mission_id IS NULL`) and are not stored inside another unit
    /// (`owner_unit_id IS NULL`).
    ///
    /// Issues a single joined query built by SeaORM and executed via
    /// [`db::sea_all`] on the caller's pinned connection — the join graph
    /// mirrors the old hand-written `SELECT_DTO`:
    /// ```text
    /// obtained_units
    ///  ├─ JOIN units                         ─ LEFT JOIN unit_types
    ///  ├─ JOIN user_storage      (owner)
    ///  ├─ LEFT JOIN obtained_unit_temporal_information
    ///  ├─ LEFT JOIN planets AS sp            ─ LEFT JOIN galaxies AS sp_galaxy
    ///  │                                     ─ LEFT JOIN user_storage AS sp_owner
    ///  └─ LEFT JOIN planets AS tp            ─ LEFT JOIN galaxies AS tp_galaxy
    ///                                        ─ LEFT JOIN user_storage AS tp_owner
    /// ```
    ///
    /// After building each DTO, applies `HiddenUnitBo.defineHidden`: the
    /// embedded `unit.is_invisible` is set to `true` if the DB flag is set OR
    /// if the user has an active time-special rule of type
    /// `TIME_SPECIAL_IS_ENABLED_DO_HIDE` targeting this unit (by exact unit id
    /// or by unit-type ancestry). Ports `HiddenUnitBo.isHiddenUnitInternal`.
    pub async fn find_completed_dtos(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Vec<ObtainedUnitDto>> {
        let filters = Entity::find()
            .filter(obtained_unit::Column::UserId.eq(user_id))
            .filter(obtained_unit::Column::SourcePlanet.is_not_null())
            .filter(obtained_unit::Column::MissionId.is_null())
            .filter(obtained_unit::Column::OwnerUnitId.is_null())
            .order_by_asc(obtained_unit::Column::Id);

        let rows = db::sea_all(&mut *conn, Self::with_full_joins(filters)).await?;

        let mut dtos = Vec::with_capacity(rows.len());
        for row in rows {
            // `HiddenUnitBo.isHiddenUnitInternal`: is_invisible = DB flag OR
            // an ACTIVE time-special DO_HIDE rule matches this unit.

            let mut dto = row.to_dto(&mut *conn).await?;
            // Java's PlanetDto mapping carries the planet's specialLocation in
            // the SLIM shape on this payload (`unit_obtained_change`); the
            // joined row here doesn't fetch it, so hydrate it per planet
            // (R-class/D19; galaxy/image/improvement stay lazy-unset in Java
            // and are omitted by NON_NULL).
            if let Some(sp) = dto.source_planet.as_mut() {
                sp.special_location = load_slim_special_location(&mut *conn, sp.id).await?;
            }
            if let Some(tp) = dto.target_planet.as_mut() {
                tp.special_location = load_slim_special_location(&mut *conn, tp.id).await?;
            }
            dtos.push(dto);
        }
        Ok(dtos)
    }

    pub async fn find_by_id(
        conn: &mut MySqlConnection,
        id: u64,
    ) -> OwgeResult<Option<ObtainedUnitDto>> {
        let filters = Entity::find()
            .filter(obtained_unit::Column::Id.eq(id))
            .limit(1);

        match db::sea_one(&mut *conn, Self::with_full_joins(filters)).await? {
            Some(row) => Ok(Some(row.to_dto(&mut *conn).await?)),
            None => Ok(None),
        }
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
        conn: &mut MySqlConnection,
        dto: &ObtainedUnitDto,
        _planet_check: bool,
    ) -> OwgeResult<()> {
        let row: Option<(u64, u16)> =
            sqlx::query_as("SELECT count, unit_id FROM obtained_units WHERE id = ?")
                .bind(dto.id)
                .fetch_optional(&mut *conn)
                .await?;
        let (stack_count, unit_id) =
            row.ok_or_else(|| OwgeError::NotFound(format!("No obtained unit with id {}", dto.id)))?;
        let subtraction_count = dto.count;
        if subtraction_count > stack_count {
            return Err(OwgeError::InvalidInput(
                "Can't not subtract because, obtainedUnit count is less than the amount to subtract"
                    .to_string(),
            ));
        }
        let mut tx = conn.begin().await?;
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
        crate::bo::realtime_emitter::drain_requirement_emits(&mut *conn, &req_emits).await?;
        // clearSourceCache: removing units changes the obtained-unit improvement source.
        crate::bo::UserImprovementBo::evict_and_emit(&mut *conn, dto.user_id).await?;
        Ok(())
    }

    fn with_full_joins(select: Select<Entity>) -> Selector<SelectModel<ObtainedUnitJoinedRow>> {
        select
            .join(JoinType::InnerJoin, obtained_unit::Relation::Unit.def())
            .join(JoinType::LeftJoin, unit::Relation::UnitType.def())
            .join(JoinType::InnerJoin, obtained_unit::Relation::Owner.def())
            .join(JoinType::LeftJoin, unit::Relation::ImageStore.def())
            .join(
                JoinType::LeftJoin,
                obtained_unit::Relation::TemporalInformation.def(),
            )
            .join_as(
                JoinType::LeftJoin,
                obtained_unit::Relation::SourcePlanet.def(),
                "sp",
            )
            .join_as(
                JoinType::LeftJoin,
                planet::Relation::Galaxy.def().from_alias("sp"),
                "sp_galaxy",
            )
            .join_as(
                JoinType::LeftJoin,
                planet::Relation::Owner.def().from_alias("sp"),
                "sp_owner",
            )
            .join_as(
                JoinType::LeftJoin,
                obtained_unit::Relation::TargetPlanet.def(),
                "tp",
            )
            .join_as(
                JoinType::LeftJoin,
                planet::Relation::Galaxy.def().from_alias("tp"),
                "tp_galaxy",
            )
            .join_as(
                JoinType::LeftJoin,
                planet::Relation::Owner.def().from_alias("tp"),
                "tp_owner",
            )
            .into_partial_model::<ObtainedUnitJoinedRow>()
    }
}

#[cfg(test)]
mod sql_shape_tests {
    use super::*;
    use sea_orm::DbBackend;

    /// Confirms `find_completed_dtos` builds exactly **one** SQL query that
    /// performs every join itself (no lazy-loading / N+1 fan-out) — the
    /// "single round trip doing all the joins" the refactor was for.
    #[test]
    fn find_completed_dtos_query_is_a_single_joined_select() {
        let sql = Entity::find()
            .filter(obtained_unit::Column::UserId.eq(1))
            .filter(obtained_unit::Column::SourcePlanet.is_not_null())
            .filter(obtained_unit::Column::MissionId.is_null())
            .filter(obtained_unit::Column::OwnerUnitId.is_null())
            .order_by_asc(obtained_unit::Column::Id)
            .join(JoinType::InnerJoin, obtained_unit::Relation::Unit.def())
            .join(JoinType::LeftJoin, unit::Relation::UnitType.def())
            .join(JoinType::InnerJoin, obtained_unit::Relation::Owner.def())
            .join(
                JoinType::LeftJoin,
                obtained_unit::Relation::TemporalInformation.def(),
            )
            .join_as(
                JoinType::LeftJoin,
                obtained_unit::Relation::SourcePlanet.def(),
                "sp",
            )
            .join_as(
                JoinType::LeftJoin,
                planet::Relation::Galaxy.def().from_alias("sp"),
                "sp_galaxy",
            )
            .join_as(
                JoinType::LeftJoin,
                planet::Relation::Owner.def().from_alias("sp"),
                "sp_owner",
            )
            .join_as(
                JoinType::LeftJoin,
                obtained_unit::Relation::TargetPlanet.def(),
                "tp",
            )
            .join_as(
                JoinType::LeftJoin,
                planet::Relation::Galaxy.def().from_alias("tp"),
                "tp_galaxy",
            )
            .join_as(
                JoinType::LeftJoin,
                planet::Relation::Owner.def().from_alias("tp"),
                "tp_owner",
            )
            .into_partial_model::<ObtainedUnitJoinedRow>()
            .into_statement(DbBackend::MySql)
            .to_string();

        println!("{sql}");

        // One statement, one `SELECT`, one `FROM` — i.e. a single round trip.
        assert_eq!(sql.matches("SELECT").count(), 1);
        assert_eq!(sql.matches(" FROM ").count(), 1);
        assert!(!sql.contains(';'), "expected a single statement: {sql}");

        // Every joined table/alias from the doc-comment diagram is present.
        for needle in [
            "INNER JOIN `units`",
            "LEFT JOIN `unit_types`",
            "INNER JOIN `user_storage`",
            "LEFT JOIN `obtained_unit_temporal_information`",
            "LEFT JOIN `planets` AS `sp`",
            "LEFT JOIN `galaxies` AS `sp_galaxy`",
            "LEFT JOIN `user_storage` AS `sp_owner`",
            "LEFT JOIN `planets` AS `tp`",
            "LEFT JOIN `galaxies` AS `tp_galaxy`",
            "LEFT JOIN `user_storage` AS `tp_owner`",
        ] {
            assert!(sql.contains(needle), "missing `{needle}` in: {sql}");
        }

        // Spot-check the nested column aliasing `DerivePartialModel` generates
        // for the doubly-joined sides — this is the exact "magic" the old
        // hand-written `SELECT_DTO` + `FromRow` couldn't pull off.
        for needle in [
            "AS `unit_id`",
            "AS `unit_unit_type_name`",
            "AS `owner_username`",
            "AS `source_planet_id`",
            "AS `source_planet_galaxy_name`",
            "AS `source_planet_owner_info_username`",
            "AS `target_planet_id`",
            "AS `target_planet_galaxy_name`",
            "AS `target_planet_owner_info_username`",
            "AS `temporal_information_id`",
        ] {
            assert!(sql.contains(needle), "missing `{needle}` in: {sql}");
        }
    }
}
