//! Port of the running-build-finder half of
//! `com.kevinguanchedarias.owgejava.business.mission.MissionFinderBo`.
//!
//! `findRunningUnitBuild` powers the deprecated `game/unit/findRunning`
//! endpoint: the single BUILD_UNIT mission whose `mission_information.value`
//! equals the given planet id, assembled into a [`RunningUnitBuildDto`] from the
//! in-build unit, the mission, the build planet and the in-build stack count.
//!
//! `has_running_unit_build` is the boolean variant used by `planet/leave`
//! (PlanetBo) — true iff such a mission exists for the user/planet.
//!
//! ## sqlx signedness (load-bearing)
//! `missions.id` = `bigint unsigned` (`u64`); `missions.type` is the mission-type
//! id (`u16`); `mission_information.value` = `double` (`f64`);
//! `mission_information.relation_id` = `smallint unsigned` (`u16`);
//! `object_relations.reference_id` = **signed** `smallint` (`i16`); a unit id is a
//! non-negative `smallint unsigned` (`u16`); `planets.id` = `bigint unsigned`
//! (`u64`); `obtained_units.count` = `bigint unsigned` (`u64`).

use crate::bo::mission_base_service_bo::SELECT_MISSION;
use crate::bo::unit_bo::UnitBo;
use crate::db::Db;
use crate::dto::running_unit_build::RunningUnitBuildDto;
use crate::dto::PlanetDto;
use crate::error::{OwgeError, OwgeResult};
use crate::model::mission::{Mission, MissionType};

pub struct MissionFinderBo;

impl MissionFinderBo {
    /// `MissionFinderBo.findRunningUnitBuild(userId, planetId)` — the running
    /// BUILD_UNIT mission for the user whose `mission_information.value` equals
    /// `planetId`, as a [`RunningUnitBuildDto`], or `None`.
    pub async fn find_running_unit_build(
        db: &Db,
        user_id: i32,
        planet_id: u64,
    ) -> OwgeResult<Option<RunningUnitBuildDto>> {
        // findByUserIdAndTypeCodeAndMissionInformationValue(userId, BUILD_UNIT, planetId).
        let Some(mission_id) = find_build_mission_id(db, user_id, planet_id).await? else {
            return Ok(None);
        };

        let mission = sqlx::query_as::<_, Mission>(SELECT_MISSION)
            .bind(mission_id)
            .fetch_optional(db)
            .await?
            .ok_or_else(|| OwgeError::Common(format!("Mission {mission_id} vanished")))?;

        // missionInformation.getRelation() -> unboxObjectRelation: a UNIT
        // object_relation resolves to the units row via object_relations.reference_id.
        let unit_id = resolve_unit_id_for_mission(db, mission_id).await?;
        let unit = UnitBo::find_by_id(db, unit_id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No unit with id {unit_id}")))?;

        // SpringRepositoryUtil.findByIdOrDie(planetRepository, planetId.longValue()).
        let planet = find_planet_dto(db, planet_id)
            .await?
            .ok_or_else(|| OwgeError::NotFound(format!("No planet with id {planet_id}")))?;

        // obtainedUnitRepository.findByMissionId(mission.getId()).get(0).getCount().
        let count: u64 =
            sqlx::query_scalar("SELECT count FROM obtained_units WHERE mission_id = ? LIMIT 1")
                .bind(mission_id)
                .fetch_optional(db)
                .await?
                .ok_or_else(|| {
                    OwgeError::Common(format!(
                        "Build mission {mission_id} has no in-build obtained_units"
                    ))
                })?;

        Ok(Some(RunningUnitBuildDto::from_mission(
            &mission, unit, planet, count,
        )))
    }

    /// True iff a BUILD_UNIT mission for `user_id` has
    /// `mission_information.value == planet_id` (used by `planet/leave`).
    pub async fn has_running_unit_build(
        db: &Db,
        user_id: i32,
        planet_id: u64,
    ) -> OwgeResult<bool> {
        Ok(find_build_mission_id(db, user_id, planet_id).await?.is_some())
    }
}

/// The BUILD_UNIT mission id for the user whose `mission_information.value`
/// equals `planet_id`, if any.
async fn find_build_mission_id(
    db: &Db,
    user_id: i32,
    planet_id: u64,
) -> OwgeResult<Option<u64>> {
    Ok(sqlx::query_scalar(
        "SELECT m.id FROM missions m \
           JOIN mission_information mi ON mi.mission_id = m.id \
          WHERE m.user_id = ? AND m.type = ? AND mi.value = ? LIMIT 1",
    )
    .bind(user_id)
    .bind(MissionType::BuildUnit.value())
    .bind(planet_id as f64)
    .fetch_optional(db)
    .await?)
}

/// `objectRelationBo.unboxObjectRelation(missionInformation.getRelation())` for a
/// UNIT relation — the referenced unit id (`object_relations.reference_id`).
async fn resolve_unit_id_for_mission(db: &Db, mission_id: u64) -> OwgeResult<u16> {
    let reference_id: Option<i16> = sqlx::query_scalar(
        "SELECT o.reference_id FROM mission_information mi \
           JOIN object_relations o ON o.id = mi.relation_id \
          WHERE mi.mission_id = ?",
    )
    .bind(mission_id)
    .fetch_optional(db)
    .await?;
    let reference_id = reference_id.ok_or_else(|| {
        OwgeError::Common(format!(
            "Build mission {mission_id} mission_information has no relation"
        ))
    })?;
    Ok(reference_id as u16)
}

/// `PlanetBo.toDto` for a single planet by id (mirrors `PlanetBo`'s DTO SELECT).
async fn find_planet_dto(db: &Db, planet_id: u64) -> OwgeResult<Option<PlanetDto>> {
    let row = sqlx::query_as::<_, PlanetDtoRow>(
        "SELECT p.id, p.name, p.sector, p.quadrant, p.planet_number AS planet_number, \
                p.owner AS owner_id, o.username AS owner_name, p.richness, \
                COALESCE(p.home, 0) AS home, p.galaxy_id AS galaxy_id, g.name AS galaxy_name \
         FROM planets p \
         JOIN galaxies g ON g.id = p.galaxy_id \
         LEFT JOIN user_storage o ON o.id = p.owner \
         WHERE p.id = ?",
    )
    .bind(planet_id)
    .fetch_optional(db)
    .await?;
    Ok(row.map(Into::into))
}

/// A planet row joined with its galaxy and (optional) owner, with exact SQL
/// column types (mirrors `PlanetBo::PlanetRow`).
#[derive(sqlx::FromRow)]
struct PlanetDtoRow {
    id: u64,
    name: String,
    sector: u32,
    quadrant: u32,
    planet_number: u16,
    owner_id: Option<i32>,
    owner_name: Option<String>,
    richness: u16,
    home: i8,
    galaxy_id: u16,
    galaxy_name: String,
}

impl From<PlanetDtoRow> for PlanetDto {
    fn from(r: PlanetDtoRow) -> Self {
        PlanetDto {
            id: r.id,
            name: Some(r.name),
            sector: r.sector,
            quadrant: r.quadrant,
            planet_number: r.planet_number,
            owner_id: r.owner_id,
            owner_name: r.owner_name,
            richness: Some(r.richness),
            home: Some(r.home != 0),
            galaxy_id: r.galaxy_id,
            galaxy_name: r.galaxy_name,
        }
    }
}
