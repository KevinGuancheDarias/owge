//! Port of `ActiveTimeSpecialRuleFinderService`
//! (`business.rule.timespecial.ActiveTimeSpecialRuleFinderService`).
//!
//! Finds the rules that are *active because the associated time special is
//! ACTIVE for the user*, and tests whether such a rule targets a given unit.
//! Shared by:
//!   - `HiddenUnitBo` (rule type `TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS`) — mission /
//!     obtained-unit invisibility,
//!   - `AttackBypassShieldService` (rule type `TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD`).
//!
//! Java caches this (`@TaggableCacheable`); the Rust port recomputes from the DB
//! each call (consistent with the other recompute-on-demand ports — no
//! behavioural difference, only the cache optimisation is dropped).
//!
//! Schema: `active_time_specials.user_id` is signed `int` (`i32`),
//! `active_time_specials.time_special_id` is `smallint unsigned` (`u16`);
//! `rules.origin_id` / `rules.destination_id` are signed `smallint` (`i16`), and
//! `units.id` / `unit_types.id` / `unit_types.parent_type` are `smallint`.

use sqlx::MySqlConnection;

use crate::error::OwgeResult;

/// Java `ObjectEnum.TIME_SPECIAL.name()` — the origin type of a time-special rule.
const ORIGIN_TIME_SPECIAL: &str = "TIME_SPECIAL";

pub struct ActiveTimeSpecialRuleFinderBo;

impl ActiveTimeSpecialRuleFinderBo {
    /// `existsRuleMatchingUnitDestination(user, unit, wantedType)` — true iff the
    /// user has an ACTIVE time special whose rules include one of `wanted_type`
    /// that targets `unit` (`isWantedUnitDestination`).
    pub async fn exists_rule_matching_unit_destination(
        conn: &mut MySqlConnection,
        user_id: i32,
        unit_id: i64,
        unit_type_id: u16,
        wanted_type: &str,
    ) -> OwgeResult<bool> {
        // findActiveRules: for each ACTIVE time special of the user, gather the
        // `rules` rows with origin (TIME_SPECIAL, time_special_id) and keep those
        // whose `type` is the wanted one.
        let rules: Vec<(String, i16)> = sqlx::query_as(
            "SELECT r.destination_type, r.destination_id \
               FROM active_time_specials ats \
               JOIN rules r \
                 ON r.origin_type = ? AND r.origin_id = ats.time_special_id \
              WHERE ats.user_id = ? AND ats.state = 'ACTIVE' AND r.type = ?",
        )
        .bind(ORIGIN_TIME_SPECIAL)
        .bind(user_id)
        .bind(wanted_type)
        .fetch_all(&mut *conn)
        .await?;

        for (destination_type, destination_id) in rules {
            if Self::is_wanted_unit_destination(
                conn,
                &destination_type,
                destination_id as i64,
                unit_id,
                unit_type_id,
            )
            .await?
            {
                return Ok(true);
            }
        }
        Ok(false)
    }

    /// `RuleBo.isWantedUnitDestination` — a `UNIT` rule matches the exact unit id;
    /// a `UNIT_TYPE` rule matches if the unit's type (walking up `parent_type`)
    /// equals the rule destination; anything else never matches.
    async fn is_wanted_unit_destination(
        conn: &mut MySqlConnection,
        destination_type: &str,
        destination_id: i64,
        unit_id: i64,
        unit_type_id: u16,
    ) -> OwgeResult<bool> {
        match destination_type {
            "UNIT" => Ok(destination_id == unit_id),
            "UNIT_TYPE" => {
                // `unitTypeInheritanceFinderService.findUnitTypeMatchingCondition`
                // — walk the type's ancestry until one matches the destination id.
                let mut current: Option<u16> = Some(unit_type_id);
                while let Some(type_id) = current {
                    if type_id as i64 == destination_id {
                        return Ok(true);
                    }
                    current = sqlx::query_scalar("SELECT parent_type FROM unit_types WHERE id = ?")
                        .bind(type_id)
                        .fetch_optional(&mut *conn)
                        .await?
                        .flatten();
                }
                Ok(false)
            }
            _ => Ok(false),
        }
    }
}
