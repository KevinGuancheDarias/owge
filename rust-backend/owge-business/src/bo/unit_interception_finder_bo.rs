//! Port of `UnitInterceptionFinderBo`
//! (`com.kevinguanchedarias.owgejava.business.speedimpactgroup.UnitInterceptionFinderBo`).
//!
//! Given an in-flight mission and its involved (attacking) units, this resolves
//! which of those units are *intercepted* by active interceptors defending the
//! target planet. An interceptor is any unit involved in the defence of the
//! target planet (`ObtainedUnitFinderBo.findInvolvedInAttack`) whose `units` row
//! declares one or more `interceptable_speed_group`s. An involved unit is
//! intercepted by such a defender when:
//!   * the defender's interceptable speed-impact-group set contains the
//!     *applicable* speed impact group of the involved unit
//!     (`SpeedImpactGroupBo.canIntercept` →
//!     `SpeedImpactGroupFinderBo.findApplicable`), and
//!   * the defender and the involved unit's owners are enemies
//!     (`AllianceBo.areEnemies`), and
//!   * the involved unit has not already been intercepted by an earlier defender
//!     (`alreadyIntercepted` set — each involved unit is intercepted at most once).
//!
//! Results are grouped by interceptor *user* (`interceptedMap` keyed by the
//! interceptor's user id); the recorded `interceptor_unit` is the first defender
//! unit of that user that intercepted anything, matching the Java `HashMap`
//! first-insert semantics. Iteration order over defenders and involved units is
//! preserved so the grouping is bit-for-bit equivalent.
//!
//! ## sqlx signedness
//! `obtained_units` columns decode at the [`ObtainedUnit`] model types;
//! `user_storage.alliance_id` is `smallint unsigned` (`u16`), `units` ids and
//! `interceptable_speed_group.speed_impact_group_id` are `smallint unsigned`
//! (`u16`), `active_time_specials.user_id` is signed `int` (`i32`).
//!
//! ## Parity scope
//! The detection, grouping and (via [`MissionInterceptionManagerBo`]) stack
//! removal + report persistence are ported, as is `sendReportToInterceptorUsers`
//! (M4): each interceptor user gets its own `mission_reports` row plus a
//! `mission_report_new` / `mission_report_count_change` push (the push is
//! deferred post-commit by the firing path via
//! [`crate::bo::mission_processor::DeferredEmit::MissionReport`]).

use sqlx::MySqlConnection;

use crate::builder::UnitMissionReportBuilder;
use crate::error::OwgeResult;
use crate::model::mission::Mission;
use crate::model::obtained_unit::ObtainedUnit;

/// Rust counterpart of `InterceptedUnitsInformation` (pojo).
///
/// The Java pojo carries the full `UserStorage` and `ObtainedUnit` entities; here
/// we keep the fields actually consumed downstream: the interceptor user's id and
/// username (for the report's `interceptorUser`), the interceptor obtained-unit
/// stack, and the set of intercepted obtained-unit stacks.
#[derive(Debug, Clone)]
pub struct InterceptedUnitsInformation {
    /// `interceptorUser.getId()` — the grouping key.
    pub interceptor_user_id: i32,
    /// `interceptorUser.getUsername()` — embedded into the interception report.
    pub interceptor_username: String,
    /// `interceptorUnit` — the (first) defender stack of this user that intercepted.
    pub interceptor_unit: ObtainedUnit,
    /// `interceptedUnits` — the involved stacks this user intercepted (in the
    /// order they were intercepted; deduplicated across all defenders).
    pub intercepted_units: Vec<ObtainedUnit>,
}

/// A defender involved at the target planet, enriched with its owner's id/username
/// and alliance — the candidate interceptor row.
#[derive(Debug, Clone, sqlx::FromRow)]
struct DefenderRow {
    /// `obtained_units.id`.
    id: u64,
    /// `obtained_units.user_id` (signed int).
    user_id: i32,
    /// `obtained_units.unit_id` (smallint unsigned).
    unit_id: u16,
    /// `obtained_units.count`.
    count: u64,
    /// `obtained_units.source_planet`.
    source_planet: Option<u64>,
    /// `obtained_units.target_planet`.
    target_planet: Option<u64>,
    /// `obtained_units.mission_id`.
    mission_id: Option<u64>,
    /// `obtained_units.first_deployment_mission`.
    first_deployment_mission: Option<u64>,
    /// `obtained_units.is_from_capture` (tinyint).
    is_from_capture: i8,
    /// `obtained_units.expiration_id`.
    expiration_id: Option<u32>,
    /// `obtained_units.owner_unit_id`.
    owner_unit_id: Option<u64>,
    /// `user_storage.username`.
    username: String,
    /// `user_storage.alliance_id` (smallint unsigned).
    alliance_id: Option<u16>,
}

impl DefenderRow {
    fn to_obtained_unit(&self) -> ObtainedUnit {
        ObtainedUnit {
            id: self.id,
            user_id: self.user_id,
            unit_id: self.unit_id,
            count: self.count,
            source_planet: self.source_planet,
            target_planet: self.target_planet,
            mission_id: self.mission_id,
            first_deployment_mission: self.first_deployment_mission,
            is_from_capture: self.is_from_capture,
            expiration_id: self.expiration_id,
            owner_unit_id: self.owner_unit_id,
        }
    }
}

pub struct UnitInterceptionFinderBo;

impl UnitInterceptionFinderBo {
    /// `checkInterceptsSpeedImpactGroup` — resolve the interceptions of the
    /// mission's involved units by the active interceptors at the target planet.
    ///
    /// Returns one [`InterceptedUnitsInformation`] per interceptor *user* (only
    /// for users that intercepted at least one unit), in first-interception order.
    pub async fn check_intercepts_speed_impact_group(
        conn: &mut MySqlConnection,
        mission: &Mission,
        involved_units: &[ObtainedUnit],
    ) -> OwgeResult<Vec<InterceptedUnitsInformation>> {
        let Some(target_planet_id) = mission.target_planet.map(|p| p as u64) else {
            return Ok(Vec::new());
        };

        // findInvolvedInAttack(targetPlanet), keeping only defenders whose unit
        // declares interceptable speed groups.
        let defenders = Self::find_involved_in_attack(conn, target_planet_id).await?;

        // alreadyIntercepted: an involved unit is intercepted at most once.
        let mut already_intercepted: std::collections::HashSet<u64> =
            std::collections::HashSet::new();
        // interceptedMap keyed by interceptor user id, insertion-ordered.
        let mut order: Vec<i32> = Vec::new();
        let mut intercepted_map: std::collections::HashMap<i32, InterceptedUnitsInformation> =
            std::collections::HashMap::new();

        for defender in &defenders {
            let interceptable_groups =
                Self::find_interceptable_group_ids(conn, defender.unit_id).await?;
            if interceptable_groups.is_empty() {
                continue;
            }
            for involved in involved_units {
                if already_intercepted.contains(&involved.id) {
                    continue;
                }
                // canIntercept(defender.interceptableSpeedGroups, involved.user, involved)
                let can_intercept =
                    Self::can_intercept(conn, &interceptable_groups, involved).await?;
                if !can_intercept {
                    continue;
                }
                // allianceBo.areEnemies(defender.user, involved.user)
                let involved_alliance = Self::find_user_alliance(conn, involved.user_id).await?;
                if !are_enemies(
                    defender.user_id,
                    defender.alliance_id,
                    involved.user_id,
                    involved_alliance,
                ) {
                    continue;
                }
                let interceptor_user_id = defender.user_id;
                let entry = intercepted_map
                    .entry(interceptor_user_id)
                    .or_insert_with(|| {
                        order.push(interceptor_user_id);
                        InterceptedUnitsInformation {
                            interceptor_user_id,
                            interceptor_username: defender.username.clone(),
                            interceptor_unit: defender.to_obtained_unit(),
                            intercepted_units: Vec::new(),
                        }
                    });
                entry.intercepted_units.push(involved.clone());
                already_intercepted.insert(involved.id);
            }
        }

        Ok(order
            .into_iter()
            .map(|user_id| intercepted_map.remove(&user_id).expect("present"))
            .collect())
    }

    /// `SpeedImpactGroupBo.canIntercept` — the involved unit's *applicable* speed
    /// impact group is one of the defender's interceptable groups.
    async fn can_intercept(
        conn: &mut MySqlConnection,
        interceptable_group_ids: &[u16],
        involved: &ObtainedUnit,
    ) -> OwgeResult<bool> {
        // determineTargetUnit(involved): a stored unit (owner_unit_id set) resolves
        // to its carrier's unit; otherwise the stack's own unit.
        let target_unit_id = match involved.owner_unit_id {
            Some(owner_unit_id) => {
                match Self::find_unit_by_obtained_unit_id(conn, owner_unit_id).await? {
                    Some(unit_id) => unit_id,
                    None => involved.unit_id,
                }
            }
            None => involved.unit_id,
        };
        let applicable =
            Self::find_applicable_speed_impact_group(conn, involved.user_id, target_unit_id)
                .await?;
        Ok(matches!(applicable, Some(group_id) if interceptable_group_ids.contains(&group_id)))
    }

    /// `SpeedImpactGroupFinderBo.findApplicable(user, unit)` — the speed impact
    /// group swapped in by an active `TIME_SPECIAL_IS_ENABLED_DO_SWAP_SPEED_IMPACT_GROUP`
    /// rule for one of the user's active time specials, else the unit's own group,
    /// else the nearest ancestor unit type's group (`findHisOrInherited`).
    async fn find_applicable_speed_impact_group(
        conn: &mut MySqlConnection,
        user_id: i32,
        unit_id: u16,
    ) -> OwgeResult<Option<u16>> {
        // The swap rule: for each ACTIVE time special of the user, the rules with
        // origin TIME_SPECIAL/<timeSpecialId>, type
        // TIME_SPECIAL_IS_ENABLED_DO_SWAP_SPEED_IMPACT_GROUP, take the first whose
        // extra_args[0] parses to a speed impact group id. Java's
        // `findByOriginTypeAndOriginId` is unordered; we mirror its effective
        // result by taking the first such rule ordered by (active_time_special.id,
        // rules.id) for determinism.
        let swap_arg: Option<Option<String>> = sqlx::query_scalar(
            "SELECT r.extra_args \
               FROM active_time_specials ats \
               JOIN rules r ON r.origin_type = 'TIME_SPECIAL' \
                           AND r.origin_id = ats.time_special_id \
              WHERE ats.user_id = ? AND ats.state = 'ACTIVE' \
                AND r.type = 'TIME_SPECIAL_IS_ENABLED_DO_SWAP_SPEED_IMPACT_GROUP' \
                AND r.extra_args IS NOT NULL AND r.extra_args <> '' \
              ORDER BY ats.id, r.id \
              LIMIT 1",
        )
        .bind(user_id)
        .fetch_optional(&mut *conn)
        .await?;
        if let Some(extra_args) = swap_arg.flatten() {
            // extra_args are '#'-delimited; the first segment is the group id.
            let first = extra_args.split('#').next().unwrap_or("");
            if let Ok(group_id) = first.parse::<u16>() {
                return Ok(Some(group_id));
            }
        }

        Self::find_his_or_inherited_speed_impact_group(conn, unit_id).await
    }

    /// `SpeedImpactGroupFinderBo.findHisOrInherited(unit)` — the unit's own
    /// `speed_impact_group_id`, else the nearest ancestor unit type with one set.
    async fn find_his_or_inherited_speed_impact_group(
        conn: &mut MySqlConnection,
        unit_id: u16,
    ) -> OwgeResult<Option<u16>> {
        let row: Option<(Option<u16>, Option<u16>)> =
            sqlx::query_as("SELECT speed_impact_group_id, type FROM units WHERE id = ?")
                .bind(unit_id)
                .fetch_optional(&mut *conn)
                .await?;
        let Some((own_group, unit_type_id)) = row else {
            return Ok(None);
        };
        if let Some(group_id) = own_group {
            return Ok(Some(group_id));
        }
        // Walk the unit_type parent chain for the first type with a group set.
        let mut current = unit_type_id;
        let mut visited: std::collections::HashSet<u16> = std::collections::HashSet::new();
        while let Some(type_id) = current {
            if !visited.insert(type_id) {
                break; // guard against a malformed cycle
            }
            let type_row: Option<(Option<u16>, Option<u16>)> = sqlx::query_as(
                "SELECT speed_impact_group_id, parent_type FROM unit_types WHERE id = ?",
            )
            .bind(type_id)
            .fetch_optional(&mut *conn)
            .await?;
            match type_row {
                Some((Some(group_id), _)) => return Ok(Some(group_id)),
                Some((None, parent)) => current = parent,
                None => return Ok(None),
            }
        }
        Ok(None)
    }

    /// `obtainedUnitRepository.findUnitByOuId` — the `unit_id` of an obtained unit.
    async fn find_unit_by_obtained_unit_id(
        conn: &mut MySqlConnection,
        obtained_unit_id: u64,
    ) -> OwgeResult<Option<u16>> {
        Ok(
            sqlx::query_scalar("SELECT unit_id FROM obtained_units WHERE id = ?")
                .bind(obtained_unit_id)
                .fetch_optional(&mut *conn)
                .await?,
        )
    }

    /// The `speed_impact_group_id`s a unit declares as interceptable
    /// (`unit.getInterceptableSpeedGroups()`).
    async fn find_interceptable_group_ids(
        conn: &mut MySqlConnection,
        unit_id: u16,
    ) -> OwgeResult<Vec<u16>> {
        Ok(sqlx::query_scalar(
            "SELECT speed_impact_group_id FROM interceptable_speed_group WHERE unit_id = ?",
        )
        .bind(unit_id)
        .fetch_all(&mut *conn)
        .await?)
    }

    /// `user_storage.alliance_id` for a user.
    async fn find_user_alliance(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<Option<u16>> {
        let alliance: Option<Option<u16>> =
            sqlx::query_scalar("SELECT alliance_id FROM user_storage WHERE id = ?")
                .bind(user_id)
                .fetch_optional(&mut *conn)
                .await?;
        Ok(alliance.flatten())
    }

    /// `ObtainedUnitFinderBo.findInvolvedInAttack(targetPlanet)` — defenders
    /// involved at the target planet: units sitting on the planet with no mission,
    /// DEPLOYED units targeting the planet, and CONQUEST units toward the planet
    /// with >=10% of their required time elapsed. Each is enriched with its owner's
    /// username and alliance for the enemy check. Unlike the combat manager's
    /// defender query, the attacking mission's own stacks are *not* excluded here
    /// (matching Java) — they cannot match these clauses for an in-flight ATTACK
    /// mission anyway, and the enemy check rejects same-user pairs.
    async fn find_involved_in_attack(
        conn: &mut MySqlConnection,
        target_planet_id: u64,
    ) -> OwgeResult<Vec<DefenderRow>> {
        Ok(sqlx::query_as::<_, DefenderRow>(SELECT_DEFENDERS_SQL)
            .bind(target_planet_id) // sourcePlanet, mission NULL
            .bind(target_planet_id) // DEPLOYED targetPlanet
            .bind(target_planet_id) // CONQUEST targetPlanet
            .fetch_all(&mut *conn)
            .await?)
    }

    /// `sendReportToInterceptorUsers` — write a per-interceptor interception
    /// report and notify each interceptor user.
    ///
    /// Java `doSendReportToInterceptorUser`: for each grouped interception,
    /// `UnitMissionReportBuilder.create(interceptorUser, sourcePlanet,
    /// targetPlanet, [interceptorUnit]).withInterceptionInformation([info])` then
    /// `missionReportBo.create(builder, true, interceptorUser)` — i.e. an
    /// `is_enemy = true` report owned by the interceptor, NOT linked to the
    /// in-flight mission. Returns one `(user_id, report_id)` per report so the
    /// firing path can emit `mission_report_new` + `mission_report_count_change`
    /// after commit.
    pub async fn send_report_to_interceptor_users(
        conn: &mut MySqlConnection,
        mission: &Mission,
        interceptions: &[InterceptedUnitsInformation],
    ) -> OwgeResult<Vec<(i32, u64)>> {
        use crate::bo::mission_interception_manager_bo::MissionInterceptionManagerBo;
        use crate::bo::mission_processor::{involved_units_to_dtos, load_planet_dto};
        use crate::bo::mission_report_manager_bo::MissionReportManagerBo;

        // sourcePlanet / targetPlanet are shared across every interceptor report.
        let source_planet = match mission.source_planet {
            Some(id) => load_planet_dto(conn, id as u64).await?,
            None => None,
        };
        let target_planet = match mission.target_planet {
            Some(id) => load_planet_dto(conn, id as u64).await?,
            None => None,
        };

        let mut pairs = Vec::with_capacity(interceptions.len());
        for info in interceptions {
            // involvedUnits = [interceptorUnit].
            let involved_dtos =
                involved_units_to_dtos(conn, std::slice::from_ref(&info.interceptor_unit)).await?;
            // withInterceptionInformation(List.of(info)) — just this interceptor's block.
            let interception_json = MissionInterceptionManagerBo::build_interception_info_json(
                conn,
                std::slice::from_ref(info),
            )
            .await?;
            let builder = UnitMissionReportBuilder::create_with(
                info.interceptor_user_id,
                &info.interceptor_username,
                source_planet.as_ref(),
                target_planet.as_ref(),
                &involved_dtos,
            )
            .with_interception_information(interception_json);
            // missionReportBo.create(builder, true, interceptorUser): an enemy
            // report owned by the interceptor, not linked to the mission.
            let inserted = MissionReportManagerBo::handle_mission_report_save_for_users(
                conn,
                &builder,
                /* is_enemy = */ true,
                &[info.interceptor_user_id],
            )
            .await?;
            pairs.extend(inserted);
        }
        Ok(pairs)
    }
}

/// `AllianceBo.areEnemies` — different users, and at least one without an alliance
/// or in a different alliance.
fn are_enemies(
    source_id: i32,
    source_alliance: Option<u16>,
    target_id: i32,
    target_alliance: Option<u16>,
) -> bool {
    source_id != target_id
        && (source_alliance.is_none()
            || target_alliance.is_none()
            || source_alliance != target_alliance)
}

/// Defenders involved at the target planet (ObtainedUnitFinderBo.findInvolvedInAttack),
/// enriched with owner username + alliance. `?1/?2/?3` = target planet id.
const SELECT_DEFENDERS_SQL: &str = "\
    SELECT ou.id AS id, ou.user_id AS user_id, ou.unit_id AS unit_id, ou.count AS count, \
           ou.source_planet AS source_planet, ou.target_planet AS target_planet, \
           ou.mission_id AS mission_id, ou.first_deployment_mission AS first_deployment_mission, \
           ou.is_from_capture AS is_from_capture, ou.expiration_id AS expiration_id, \
           ou.owner_unit_id AS owner_unit_id, \
           us.username AS username, us.alliance_id AS alliance_id \
    FROM obtained_units ou \
    JOIN user_storage us ON us.id = ou.user_id \
    LEFT JOIN missions m ON m.id = ou.mission_id \
    LEFT JOIN mission_types mt ON mt.id = m.type \
    WHERE ( \
            (ou.mission_id IS NULL AND ou.source_planet = ?) \
            OR (ou.target_planet = ? AND mt.code = 'DEPLOYED') \
            OR (ou.target_planet = ? AND mt.code = 'CONQUEST' \
                AND m.required_time * 0.1 < TIME_TO_SEC(TIMEDIFF(UTC_TIMESTAMP(), m.starting_date))) \
          )";
