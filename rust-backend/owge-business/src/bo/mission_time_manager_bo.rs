//! Port of `business/mission/MissionTimeManagerBo.java`.
//!
//! Computes how long a unit mission takes and stamps the `required_time` /
//! `termination_date` onto the [`Mission`]. The base per-type time comes from
//! [`MissionConfigurationBo`]; it is then lengthened by a speed/penalty/move-cost
//! model when the involved units are not all "fixed speed". The slowest
//! non-fixed unit's speed (improved by that unit type's `SPEED` improvement) sets
//! the pace.
//!
//! All the `MISSION_SPEED_*` tuning knobs are read (and lazily defaulted) from the
//! `configuration` table. The Java side reads them via `ConfigurationBo`; here the
//! reads run on the **caller's pinned connection** (`conn`) so the whole
//! calculation stays on the locked mission connection, matching the contract's
//! preference for `&mut MySqlConnection` in mission-processor call paths.

use chrono::{NaiveDateTime, Utc};
use sqlx::MySqlConnection;

use crate::bo::mission_configuration_bo::MissionConfigurationBo;
use crate::error::OwgeResult;
use crate::model::mission::{Mission, MissionType};
use crate::model::obtained_unit::ObtainedUnit;

/// The few `units`/`speed_impact_groups` columns the time math needs for one
/// involved unit, decoded at their literal MySQL types.
#[derive(sqlx::FromRow)]
struct UnitSpeedRow {
    /// `units.speed` (`double`, nullable).
    speed: Option<f64>,
    /// `units.type` (`smallint unsigned`, nullable) — the unit type id.
    #[sqlx(rename = "type")]
    type_id: Option<u16>,
    /// `speed_impact_groups.is_fixed` (`tinyint`), NULL when no group.
    is_fixed: Option<i8>,
}

/// Coordinates of a planet involved in the mission (`planets` scalar columns).
#[derive(sqlx::FromRow, Clone, Copy)]
struct PlanetCoords {
    galaxy_id: u16,
    sector: u32,
    quadrant: u32,
    planet_number: u16,
}

pub struct MissionTimeManagerBo;

impl MissionTimeManagerBo {
    /// `computeTerminationDate` — now (UTC) + `requiredTime` whole seconds. Java
    /// uses `requiredTime.intValue()`, i.e. truncation toward zero.
    pub fn compute_termination_date(required_time: f64) -> NaiveDateTime {
        Utc::now().naive_utc() + chrono::Duration::seconds(required_time as i64)
    }

    /// `calculateRequiredTime` — the per-type base time (no speed adjustment).
    pub async fn calculate_required_time(
        conn: &mut MySqlConnection,
        mission_type: MissionType,
    ) -> OwgeResult<f64> {
        Ok(MissionConfigurationBo::find_mission_base_time(&mut *conn, mission_type).await? as f64)
    }

    /// `handleCustomDuration` — bump the mission to a custom duration when it is
    /// longer than the already-computed `required_time`.
    pub fn handle_custom_duration(mission: &mut Mission, custom: Option<i64>) {
        let current = mission.required_time.unwrap_or(0.0);
        if let Some(custom) = custom {
            if (custom as f64) > current {
                mission.required_time = Some(custom as f64);
                mission.termination_date = Some(Self::compute_termination_date(custom as f64));
            }
        }
    }

    /// `handleMissionTimeCalculation` — when not every involved unit is a fixed
    /// speed-impact-group unit, find the slowest non-fixed unit, apply that unit
    /// type's `SPEED` improvement, and recompute `required_time` /
    /// `termination_date` from the speed/penalty/move-cost model.
    ///
    /// Mirrors the Java guard exactly: only units with a positive speed *and* that
    /// are not in a fixed speed-impact-group participate in the slowest-speed
    /// search; if none qualify the mission keeps its base time.
    pub async fn handle_mission_time_calculation(
        conn: &mut MySqlConnection,
        units: &[ObtainedUnit],
        mission: &mut Mission,
        mission_type: MissionType,
    ) -> OwgeResult<()> {
        // Resolve each involved unit's speed/type/fixed-flag.
        let mut rows: Vec<UnitSpeedRow> = Vec::with_capacity(units.len());
        for unit in units {
            let row = sqlx::query_as::<_, UnitSpeedRow>(
                "SELECT u.speed AS speed, u.type AS `type`, sig.is_fixed AS is_fixed \
                 FROM units u \
                 LEFT JOIN speed_impact_groups sig ON sig.id = u.speed_impact_group_id \
                 WHERE u.id = ?",
            )
            .bind(unit.unit_id)
            .fetch_one(&mut *conn)
            .await?;
            rows.push(row);
        }

        // `allUnitsHaveFixedSpeedImpactGroup` — every unit is in a fixed group.
        let all_fixed =
            !rows.is_empty() && rows.iter().all(|r| matches!(r.is_fixed, Some(f) if f != 0));
        if all_fixed {
            return Ok(());
        }

        // Lowest speed among units that have a positive speed and are not in a
        // fixed speed-impact-group.
        let lowest_speed = rows
            .iter()
            .filter(|r| {
                matches!(r.speed, Some(s) if s > 0.0) && !matches!(r.is_fixed, Some(f) if f != 0)
            })
            .filter_map(|r| r.speed)
            .fold(None::<f64>, |acc, s| {
                Some(match acc {
                    Some(a) if a <= s => a,
                    _ => s,
                })
            });
        let Some(lowest_speed) = lowest_speed else {
            return Ok(());
        };

        // The unit type of the (first) unit whose speed equals the lowest speed.
        let unit_type_id = rows
            .iter()
            .find(|r| matches!(r.speed, Some(s) if s == lowest_speed))
            .and_then(|r| r.type_id);

        // SPEED improvement for that unit type (with parent inheritance), as a
        // rational (`findAsRational` = value / 100).
        let speed_improvement_pct = match (mission.user_id, unit_type_id) {
            (Some(user_id), Some(unit_type_id)) => {
                Self::find_speed_improvement(conn, user_id, unit_type_id).await?
            }
            _ => 0.0,
        };
        let speed_with_improvement =
            lowest_speed + (lowest_speed * (speed_improvement_pct / 100.0));

        // Base time for the type (read on the same connection).
        let mission_type_time = Self::find_mission_base_time_conn(conn, mission_type).await?;

        // Source/target coordinates (signed FK columns → unsigned planet ids).
        let (Some(source_id), Some(target_id)) = (mission.source_planet, mission.target_planet)
        else {
            // No planets to measure between — keep the base time.
            mission.required_time = Some(mission_type_time);
            mission.termination_date = Some(Self::compute_termination_date(mission_type_time));
            return Ok(());
        };
        let source = Self::load_planet_coords(conn, source_id as u64).await?;
        let target = Self::load_planet_coords(conn, target_id as u64).await?;

        let required_time = Self::calculate_time_using_speed(
            conn,
            mission_type,
            mission_type_time,
            speed_with_improvement,
            source,
            target,
        )
        .await?;
        mission.required_time = Some(required_time);
        mission.termination_date = Some(Self::compute_termination_date(required_time));
        Ok(())
    }

    /// `calculateTimeUsingSpeed`.
    async fn calculate_time_using_speed(
        conn: &mut MySqlConnection,
        mission_type: MissionType,
        mission_type_time: f64,
        lowest_unit_speed: f64,
        source: PlanetCoords,
        target: PlanetCoords,
    ) -> OwgeResult<f64> {
        let mut divisor = Self::find_mission_type_divisor(conn, mission_type).await?;
        if divisor == 0 {
            divisor = 1;
        }
        let left_multiplier =
            Self::find_speed_left_multiplier(conn, mission_type, source, target).await?;
        let move_cost = Self::calculate_move_cost(conn, mission_type, source, target).await?;
        let ret = mission_type_time
            + ((left_multiplier as f64 * move_cost as f64) * (100.0 - lowest_unit_speed))
                / divisor as f64;
        Ok(mission_type_time.max(ret))
    }

    /// `findMissionTypeDivisor` — `MISSION_SPEED_DIVISOR_<TYPE>`, default `1`.
    async fn find_mission_type_divisor(
        conn: &mut MySqlConnection,
        mission_type: MissionType,
    ) -> OwgeResult<i64> {
        let name = format!("MISSION_SPEED_DIVISOR_{}", mission_type.code());
        let value = Self::config_or_default(conn, &name, "1").await?;
        Ok(value.trim().parse::<i64>().unwrap_or(1))
    }

    /// `findSpeedLeftMultiplier` — the "mission penalty" based on how far apart
    /// the source/target are (same quadrant, different galaxy/sector/quadrant).
    async fn find_speed_left_multiplier(
        conn: &mut MySqlConnection,
        mission_type: MissionType,
        source: PlanetCoords,
        target: PlanetCoords,
    ) -> OwgeResult<i64> {
        let mission_type_name = mission_type.code();
        let prefix = "MISSION_SPEED_";
        let (suffix, default_multiplier): (&str, i64) = if source.quadrant == target.quadrant
            && source.sector == target.sector
            && source.galaxy_id == target.galaxy_id
        {
            ("_SAME_Q", 50)
        } else if source.galaxy_id != target.galaxy_id {
            ("_DIFF_G", 2000)
        } else if source.sector != target.sector {
            ("_DIFF_S", 200)
        } else {
            ("_DIFF_Q", 100)
        };
        let name = format!("{prefix}{mission_type_name}{suffix}");
        let value = Self::config_or_default(conn, &name, &default_multiplier.to_string()).await?;
        Ok(value.trim().parse::<i64>().unwrap_or(default_multiplier))
    }

    /// `calculateMoveCost` — distance-weighted cost between the two planets.
    async fn calculate_move_cost(
        conn: &mut MySqlConnection,
        mission_type: MissionType,
        source: PlanetCoords,
        target: PlanetCoords,
    ) -> OwgeResult<f32> {
        let mission_type_name = mission_type.code();
        let prefix = "MISSION_SPEED_";
        let position_in_quadrant =
            (source.planet_number as i64 - target.planet_number as i64).unsigned_abs() as f32;
        let quadrants = (source.quadrant as i64 - target.quadrant as i64).unsigned_abs() as f32;
        let sectors = (source.sector as i64 - target.sector as i64).unsigned_abs() as f32;

        let planet_diff = Self::config_f32(
            conn,
            &format!("{prefix}{mission_type_name}_P_MOVE_COST"),
            0.01,
        )
        .await?;
        let quadrant_diff = Self::config_f32(
            conn,
            &format!("{prefix}{mission_type_name}_Q_MOVE_COST"),
            0.02,
        )
        .await?;
        let sector_diff = Self::config_f32(
            conn,
            &format!("{prefix}{mission_type_name}_S_MOVE_COST"),
            0.03,
        )
        .await?;
        let galaxy_diff = Self::config_f32(
            conn,
            &format!("{prefix}{mission_type_name}_G_MOVE_COST"),
            0.15,
        )
        .await?;

        let galaxy_component = if source.galaxy_id != target.galaxy_id {
            galaxy_diff
        } else {
            0.0
        };
        Ok((position_in_quadrant * planet_diff)
            + (quadrants * quadrant_diff)
            + (sectors * sector_diff)
            + galaxy_component)
    }

    /// `findUnitTypeImprovement(SPEED, unitType)` for one user, honouring parent
    /// inheritance, returning the raw summed percentage (not yet rationalised).
    async fn find_speed_improvement(
        conn: &mut MySqlConnection,
        user_id: i32,
        unit_type_id: u16,
    ) -> OwgeResult<f64> {
        // Recompute the SPEED slice of the user-improvement aggregate directly on
        // the locked `conn`, summing the same three sources
        // `UserImprovementBo::find_user_improvement` sums (unlocked upgrades ×
        // level, active time specials, non-building obtained units), restricted to
        // `SPEED` and the unit type's inheritance chain. This keeps the whole time
        // calculation on the caller's pinned connection instead of taking a second
        // pool connection for the `&Db`-based aggregate.
        let chain = Self::resolve_unit_type_inheritance_chain(conn, unit_type_id).await?;
        if chain.is_empty() {
            return Ok(0.0);
        }
        let placeholders = std::iter::repeat("?")
            .take(chain.len())
            .collect::<Vec<_>>()
            .join(", ");
        // NOTE: MySQL `SUM()` over an integer column yields a DECIMAL, which sqlx
        // will not decode into i64 — wrap each aggregate in CAST(... AS SIGNED)
        // (the improvement values and levels are integer-valued).
        let sql = format!(
            "SELECT CAST(COALESCE(SUM(iut.value * ou.level), 0) AS SIGNED) \
               FROM obtained_upgrades ou \
               JOIN upgrades u ON u.id = ou.upgrade_id \
               JOIN improvements_unit_types iut ON iut.improvement_id = u.improvement_id \
              WHERE ou.user_id = ? AND iut.type = 'SPEED' AND iut.unit_type_id IN ({placeholders}) \
            UNION ALL \
            SELECT CAST(COALESCE(SUM(iut.value), 0) AS SIGNED) \
               FROM active_time_specials ats \
               JOIN time_specials ts ON ts.id = ats.time_special_id \
               JOIN improvements_unit_types iut ON iut.improvement_id = ts.improvement_id \
              WHERE ats.user_id = ? AND ats.state = 'ACTIVE' AND iut.type = 'SPEED' \
                AND iut.unit_type_id IN ({placeholders}) \
            UNION ALL \
            SELECT CAST(COALESCE(SUM(iut.value), 0) AS SIGNED) \
               FROM obtained_units obu \
               JOIN units un ON un.id = obu.unit_id \
               JOIN improvements_unit_types iut ON iut.improvement_id = un.improvement_id \
               LEFT JOIN missions m ON m.id = obu.mission_id \
               LEFT JOIN mission_types mt ON mt.id = m.type \
              WHERE obu.user_id = ? AND (mt.code IS NULL OR mt.code <> 'BUILD_UNIT') \
                AND iut.type = 'SPEED' AND iut.unit_type_id IN ({placeholders})"
        );
        let mut query = sqlx::query_scalar::<_, i64>(&sql).bind(user_id);
        for id in &chain {
            query = query.bind(*id);
        }
        query = query.bind(user_id);
        for id in &chain {
            query = query.bind(*id);
        }
        query = query.bind(user_id);
        for id in &chain {
            query = query.bind(*id);
        }
        let totals: Vec<i64> = query.fetch_all(&mut *conn).await?;
        Ok(totals.into_iter().sum::<i64>() as f64)
    }

    /// Walk the `unit_types.parent_type` chain while `has_to_inherit_improvements`
    /// is set, mirroring `findUnitTypeImprovement`'s recursion into the parent.
    /// The starting type is always included; a parent is included only when the
    /// *child* declared inheritance.
    async fn resolve_unit_type_inheritance_chain(
        conn: &mut MySqlConnection,
        unit_type_id: u16,
    ) -> OwgeResult<Vec<u16>> {
        let mut chain = Vec::new();
        let mut current = Some(unit_type_id);
        // Guard against pathological cycles in the data.
        let mut guard = 0;
        while let Some(id) = current {
            if chain.contains(&id) || guard > 64 {
                break;
            }
            chain.push(id);
            guard += 1;
            let row: Option<(i8, Option<u16>)> = sqlx::query_as(
                "SELECT has_to_inherit_improvements, parent_type FROM unit_types WHERE id = ?",
            )
            .bind(id)
            .fetch_optional(&mut *conn)
            .await?;
            current = match row {
                Some((inherit, parent)) if inherit != 0 => parent,
                _ => None,
            };
        }
        Ok(chain)
    }

    /// `MissionConfigurationBo.findMissionBaseTimeByType`, run on the caller's
    /// connection (so it joins the locked transaction).
    async fn find_mission_base_time_conn(
        conn: &mut MySqlConnection,
        mission_type: MissionType,
    ) -> OwgeResult<f64> {
        let (key, default) = match mission_type {
            MissionType::Explore => ("MISSION_TIME_EXPLORE", "60"),
            MissionType::Gather => ("MISSION_TIME_GATHER", "900"),
            MissionType::EstablishBase => ("MISSION_TIME_ESTABLISH_BASE", "43200"),
            MissionType::Attack => ("MISSION_TIME_ATTACK", "600"),
            MissionType::Counterattack => ("MISSION_TIME_COUNTERATTACK", "60"),
            MissionType::Conquest => ("MISSION_TIME_CONQUEST", "86400"),
            MissionType::Deploy => ("MISSION_TIME_DEPLOY", "60"),
            other => {
                return Err(crate::error::OwgeError::InvalidInput(format!(
                    "Unsupported mission base time type, specified: {}",
                    other.code()
                )));
            }
        };
        let value = Self::config_or_default(conn, key, default).await?;
        Ok(value
            .trim()
            .parse::<i64>()
            .unwrap_or_else(|_| default.parse().unwrap()) as f64)
    }

    async fn load_planet_coords(
        conn: &mut MySqlConnection,
        planet_id: u64,
    ) -> OwgeResult<PlanetCoords> {
        let row = sqlx::query_as::<_, PlanetCoords>(
            "SELECT galaxy_id, sector, quadrant, planet_number FROM planets WHERE id = ?",
        )
        .bind(planet_id)
        .fetch_one(&mut *conn)
        .await?;
        Ok(row)
    }

    /// `ConfigurationBo.findOrSetDefault` on the caller's connection — read or
    /// insert-and-return the default value string.
    async fn config_or_default(
        conn: &mut MySqlConnection,
        name: &str,
        default: &str,
    ) -> OwgeResult<String> {
        let existing: Option<String> =
            sqlx::query_scalar("SELECT value FROM configuration WHERE name = ?")
                .bind(name)
                .fetch_optional(&mut *conn)
                .await?;
        if let Some(value) = existing {
            return Ok(value);
        }
        sqlx::query("INSERT INTO configuration (name, value, privileged) VALUES (?, ?, 0)")
            .bind(name)
            .bind(default)
            .execute(&mut *conn)
            .await?;
        Ok(default.to_string())
    }

    async fn config_f32(conn: &mut MySqlConnection, name: &str, default: f32) -> OwgeResult<f32> {
        let value = Self::config_or_default(conn, name, &default.to_string()).await?;
        Ok(value.trim().parse::<f32>().unwrap_or(default))
    }
}
