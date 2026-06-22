//! Runtime user-improvement aggregate — the Rust counterpart of
//! `ImprovementBo.findUserImprovement(UserStorage)`.
//!
//! The Java side computes a [`UserImprovementDto`] (its `GroupedImprovement`) by
//! reducing every registered `ImprovementSource.calculateImprovement(user)`. In
//! this codebase there are exactly three sources:
//!   1. `ObtainedUpgradeBo` — each unlocked upgrade's improvement **multiplied by
//!      its level** (`improvementBo.multiplyValues(upgrade.improvement, level)`).
//!   2. `ActiveTimeSpecialBo` — each `ACTIVE` time special's improvement (as-is).
//!   3. `ObtainedUnitImprovementCalculationService` — each non-building obtained
//!      unit's unit improvement (as-is).
//!
//! There is **no faction-base improvement source** (the faction only contributes
//! base resource production, applied elsewhere in `UserStorageBo`), so it is not
//! summed here — matching the Java aggregate exactly.
//!
//! After reducing the sources, `findUserImprovement` adds `1.0` to `moreMissions`
//! (the always-available extra mission slot).
//!
//! ## Caching (port of `ImprovementBo`'s `@Cacheable` per user)
//! Computing a user's aggregate is two multi-join queries and now runs on the
//! hot path (every authenticated request via the resource update, mission
//! registration, gather, the `user_data` emitter). We cache the **per-user
//! aggregate** in process — the Rust analog of Java's `findUserImprovement`
//! `@Cacheable(key=#user.id)` — using a [`moka`] concurrent cache. We drop
//! Java's per-source sub-caching: a full recompute is cheap, so any change just
//! evicts the whole user. [`UserImprovementBo::evict`] is called at every
//! improvement-changing site (the Rust analog of every `clearSourceCache` call);
//! a short TTL + capacity bound backstop any missed eviction and bound memory.
//! One backend process per universe (matching Java's in-process cache), so there
//! is no cross-instance coherency concern.

use std::sync::{Arc, LazyLock};
use std::time::Duration;

use moka::future::Cache;

use crate::dto::user_improvement::{ImprovementType, UserImprovementDto};
use crate::error::OwgeResult;
use sqlx::MySqlConnection;

/// Per-user aggregate improvement cache. Key = `user_storage.id`; value = the
/// shared aggregate. TTL bounds staleness from any missed eviction; the capacity
/// cap bounds memory in a large universe (moka evicts least-recently-used).
static IMPROVEMENT_CACHE: LazyLock<Cache<i32, Arc<UserImprovementDto>>> = LazyLock::new(|| {
    Cache::builder()
        .max_capacity(50_000)
        .time_to_live(Duration::from_secs(60))
        .build()
});

/// The flat `improvements` columns this aggregate sums. Decoded at the exact
/// MySQL types: the `more_*` columns are `smallint`/`tinyint`/`float`.
#[derive(sqlx::FromRow)]
struct FlatImprovementRow {
    /// Level multiplier for the obtained-upgrade source; `1` for the as-is
    /// sources (active time special / obtained unit).
    multiplier: i64,
    more_primary_resource_production: Option<i16>,
    more_secondary_resource_production: Option<i16>,
    more_energy_production: Option<i16>,
    more_charge_capacity: Option<i16>,
    more_missions_value: Option<i8>,
    more_upgrade_research_speed: Option<f32>,
    more_unit_build_speed: Option<f32>,
}

/// One `improvements_unit_types` row, with the multiplier of its owning source.
#[derive(sqlx::FromRow)]
struct UnitTypeImprovementRow {
    source_id: u32,
    r#type: String,
    unit_type_id: u16,
    value: i32,
    multiplier: i64,
}

pub struct UserImprovementBo;

impl UserImprovementBo {
    /// `ImprovementBo.findUserImprovement` — the user's aggregate improvement,
    /// served from the per-user cache (computing + storing on a miss). On a cache
    /// miss the computation runs on the provided `conn`.
    pub async fn find_user_improvement(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<UserImprovementDto> {
        if let Some(cached) = IMPROVEMENT_CACHE.get(&user_id).await {
            return Ok((*cached).clone());
        }
        let dto = Self::compute_user_improvement(conn, user_id).await?;
        IMPROVEMENT_CACHE
            .insert(user_id, Arc::new(dto.clone()))
            .await;
        Ok(dto)
    }

    /// Connection-taking variant — same as `find_user_improvement`, kept as an
    /// alias so callers that hold `&mut MySqlConnection` can call either name.
    #[inline]
    pub async fn find_user_improvement_on_conn(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<UserImprovementDto> {
        Self::find_user_improvement(conn, user_id).await
    }

    /// Build the `user_data_change` serialization form of the aggregate: the
    /// seven `more*` floats plus `unitTypesUpgrades` whose entries carry the full
    /// catalog `unitType` (Java `GroupedImprovement` → `ImprovementUnitTypeDto`).
    pub async fn find_user_improvement_response(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<crate::dto::GroupedImprovementResponse> {
        let aggregate = Self::find_user_improvement(&mut *conn, user_id).await?;
        let mut unit_types_upgrades = Vec::with_capacity(aggregate.unit_types_upgrades.len());
        for entry in &aggregate.unit_types_upgrades {
            // The aggregate's load path initializes only `attackRule` on the
            // embedded unitType (see `find_catalog_attack_rule_only`).
            let unit_type =
                crate::bo::UnitTypeBo::find_catalog_attack_rule_only(&mut *conn, entry.unit_type_id)
                    .await?;
            unit_types_upgrades.push(crate::dto::ImprovementUnitTypeDto {
                id: entry.id.map(|v| v as u16),
                r#type: Some(entry.improvement_type.code().to_string()),
                unit_type_id: None,
                unit_type_name: None,
                unit_type,
                value: Some(entry.value),
            });
        }
        Ok(crate::dto::GroupedImprovementResponse {
            more_primary_resource_production: aggregate.more_primary_resource_production,
            more_secondary_resource_production: aggregate.more_secondary_resource_production,
            more_energy_production: aggregate.more_energy_production,
            more_charge_capacity: aggregate.more_charge_capacity,
            more_missions: aggregate.more_missions,
            more_upgrade_research_speed: aggregate.more_upgrade_research_speed,
            more_unit_build_speed: aggregate.more_unit_build_speed,
            unit_types_upgrades,
        })
    }

    /// Evict a single user's cached aggregate — call after committing any change
    /// that alters their improvements (the Rust analog of every Java
    /// `clearSourceCache(user, source)`): time-special activate/expire, build-unit
    /// completion, level-up completion, attack unit losses, unit delete.
    pub async fn evict(user_id: i32) {
        IMPROVEMENT_CACHE.invalidate(&user_id).await;
    }

    /// Evict the user's cached aggregate **and** push `user_improvements_change`
    /// with the freshly-recomputed value — the Rust analog of Java's
    /// `clearSourceCache(user, source)`, which evicts then emits. Call at every
    /// per-user improvement-change site, after the change commits.
    pub async fn evict_and_emit(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<()> {
        Self::evict(user_id).await;
        crate::bo::realtime_emitter::emit_user_improvements(conn, user_id).await
    }

    /// Evict every user's cached aggregate — the analog of `clearCacheEntries`,
    /// used when an improvement *definition* changes (admin edits an upgrade /
    /// time special / unit improvement), which can affect any user.
    pub fn evict_all() {
        IMPROVEMENT_CACHE.invalidate_all();
    }

    /// Compute the aggregate from the DB (the uncached path), reducing every
    /// improvement source for `user_id` into a [`UserImprovementDto`].
    async fn compute_user_improvement(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<UserImprovementDto> {
        let mut aggregate = UserImprovementDto::default();

        // --- Flat improvement values, one row per (source, improvement) with the
        // source's level multiplier already attached. ---
        let flat_rows = sqlx::query_as::<_, FlatImprovementRow>(FLAT_IMPROVEMENTS_SQL)
            .bind(user_id) // obtained_upgrades
            .bind(user_id) // active_time_specials
            .bind(user_id) // obtained_units
            .fetch_all(&mut *conn)
            .await?;
        for row in &flat_rows {
            let m = row.multiplier as f64;
            aggregate.more_primary_resource_production +=
                row.more_primary_resource_production.unwrap_or(0) as f64 * m;
            aggregate.more_secondary_resource_production +=
                row.more_secondary_resource_production.unwrap_or(0) as f64 * m;
            aggregate.more_energy_production += row.more_energy_production.unwrap_or(0) as f64 * m;
            aggregate.more_charge_capacity += row.more_charge_capacity.unwrap_or(0) as f64 * m;
            aggregate.more_missions += row.more_missions_value.unwrap_or(0) as f64 * m;
            aggregate.more_upgrade_research_speed +=
                row.more_upgrade_research_speed.unwrap_or(0.0) as f64 * m;
            aggregate.more_unit_build_speed += row.more_unit_build_speed.unwrap_or(0.0) as f64 * m;
        }

        // --- Per-unit-type improvements, summed by (type, unit_type_id) with the
        // source multiplier applied. ---
        let unit_type_rows =
            sqlx::query_as::<_, UnitTypeImprovementRow>(UNIT_TYPE_IMPROVEMENTS_SQL)
                .bind(user_id) // obtained_upgrades
                .bind(user_id) // active_time_specials
                .bind(user_id) // obtained_units
                .fetch_all(&mut *conn)
                .await?;
        for row in unit_type_rows {
            let Some(improvement_type) = ImprovementType::from_code(&row.r#type) else {
                continue;
            };
            aggregate.add_unit_type_improvement_with_id(
                Some(row.source_id),
                improvement_type,
                row.unit_type_id,
                row.value as i64 * row.multiplier,
            );
        }

        // `findUserImprovement` always adds the extra mission slot.
        aggregate.more_missions += 1.0;
        Ok(aggregate)
    }
}

/// Union of the three improvement sources' flat `improvements` rows, each tagged
/// with its level multiplier (`level` for unlocked upgrades, `1` otherwise).
///
/// Source filters mirror the Java repositories:
///   - `obtained_upgrades`: `ObtainedUpgradeRepository.findByUserId` (all rows).
///   - `active_time_specials`: only `state = 'ACTIVE'`
///     (`findByUserIdAndState(..., ACTIVE)`).
///   - `obtained_units`: `findByUserAndNotBuilding` — excludes stacks attached to
///     a `BUILD_UNIT` mission.
const FLAT_IMPROVEMENTS_SQL: &str = "\
    SELECT ou.level AS multiplier, \
           i.more_primary_resource_production, i.more_secondary_resource_production, \
           i.more_energy_production, i.more_charge_capacity, i.more_missions_value, \
           i.more_upgrade_research_speed, i.more_unit_build_speed \
      FROM obtained_upgrades ou \
      JOIN upgrades u ON u.id = ou.upgrade_id \
      JOIN improvements i ON i.id = u.improvement_id \
     WHERE ou.user_id = ? \
    UNION ALL \
    SELECT 1 AS multiplier, \
           i.more_primary_resource_production, i.more_secondary_resource_production, \
           i.more_energy_production, i.more_charge_capacity, i.more_missions_value, \
           i.more_upgrade_research_speed, i.more_unit_build_speed \
      FROM active_time_specials ats \
      JOIN time_specials ts ON ts.id = ats.time_special_id \
      JOIN improvements i ON i.id = ts.improvement_id \
     WHERE ats.user_id = ? AND ats.state = 'ACTIVE' \
    UNION ALL \
    SELECT 1 AS multiplier, \
           i.more_primary_resource_production, i.more_secondary_resource_production, \
           i.more_energy_production, i.more_charge_capacity, i.more_missions_value, \
           i.more_upgrade_research_speed, i.more_unit_build_speed \
      FROM obtained_units obu \
      JOIN units un ON un.id = obu.unit_id \
      JOIN improvements i ON i.id = un.improvement_id \
      LEFT JOIN missions m ON m.id = obu.mission_id \
      LEFT JOIN mission_types mt ON mt.id = m.type \
     WHERE obu.user_id = ? AND (mt.code IS NULL OR mt.code <> 'BUILD_UNIT')";

/// Same three sources, joined to their `improvements_unit_types` rows.
const UNIT_TYPE_IMPROVEMENTS_SQL: &str = "\
    SELECT iut.id AS source_id, iut.type AS `type`, iut.unit_type_id, iut.value, ou.level AS multiplier \
      FROM obtained_upgrades ou \
      JOIN upgrades u ON u.id = ou.upgrade_id \
      JOIN improvements_unit_types iut ON iut.improvement_id = u.improvement_id \
     WHERE ou.user_id = ? \
    UNION ALL \
    SELECT iut.id AS source_id, iut.type AS `type`, iut.unit_type_id, iut.value, 1 AS multiplier \
      FROM active_time_specials ats \
      JOIN time_specials ts ON ts.id = ats.time_special_id \
      JOIN improvements_unit_types iut ON iut.improvement_id = ts.improvement_id \
     WHERE ats.user_id = ? AND ats.state = 'ACTIVE' \
    UNION ALL \
    SELECT iut.id AS source_id, iut.type AS `type`, iut.unit_type_id, iut.value, 1 AS multiplier \
      FROM obtained_units obu \
      JOIN units un ON un.id = obu.unit_id \
      JOIN improvements_unit_types iut ON iut.improvement_id = un.improvement_id \
      LEFT JOIN missions m ON m.id = obu.mission_id \
      LEFT JOIN mission_types mt ON mt.id = m.type \
     WHERE obu.user_id = ? AND (mt.code IS NULL OR mt.code <> 'BUILD_UNIT')";
