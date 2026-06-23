//! Port of `AttackMissionManagerBo`
//! (`com.kevinguanchedarias.owgejava.business.mission.attack.AttackMissionManagerBo`)
//! together with the helpers it leans on: `AttackObtainedUnitBo` (per-stack combat
//! stat computation + shuffle), the recursion-based `AttackRuleBo.findAttackRule`/
//! `canAttack`, and `CriticalAttackBo.findApplicableCriticalEntry`/
//! `findUsedCriticalAttack`.
//!
//! This is the combat core. Given the attacking mission and its units, it loads
//! every defender involved at the target planet, runs the shuffle-targeting attack
//! loop (critical multipliers, shield bypass, kill counts as
//! `floor(usedAttack / healthPerUnit)`, leftover carry-over), accrues points,
//! persists the surviving `obtained_units` counts (deleting wiped stacks and
//! freeing carriers / deleting emptied missions), and finally returns the
//! `attackInformation` JSON the report viewer consumes
//! (`UnitMissionReportBuilder.withAttackInformation`).
//!
//! ## sqlx signedness (load-bearing)
//! All `obtained_units`/`units`/`planets` columns are decoded at their literal
//! MySQL types (see `docs/M3-CONTRACTS.md`); the enriched row below documents each.
//!
//! ## Parity notes
//! - Websocket emissions and cache evictions (`emitUnitMissions`, `emitUserData`,
//!   `emitObtainedUnits`, `clearSourceCache`) are M4 and are left as `// TODO(M3)`
//!   — they do not affect persisted state.
//! - The `UNIT_CAPTURE` mechanic (`HandleUnitCaptureListener`, fired from
//!   `AttackEventEmitter.emitAfterUnitKilledCalculation` / `emitAttackEnd`) IS
//!   ported here: per kill, the `UNIT_CAPTURE` rule between attacker and victim
//!   units (own rule, then the captor's active time-special rules) is rolled, and
//!   on success the captured units are stationed for the captor via the `moveUnit`
//!   path (`is_from_capture = 1`) and a per-captor capture report is written at the
//!   end of the battle.
//! - `AttackBypassShieldService` also consults active time-special rules
//!   (`TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD`); this IS ported via the shared
//!   `ActiveTimeSpecialRuleFinderBo`, so a stack bypasses shields when its own
//!   `bypass_shield` flag is set OR such a rule (owned by the attacker) targets the
//!   defender unit. (Note: Java's `addPointsAndUpdateCount` health-per-unit divisor
//!   intentionally consults ONLY the own flag, so that site is left unchanged.)
//! - The unit-type parent chain for attack-rule / critical-rule / improvement
//!   inheritance is resolved by walking `unit_types.parent_type` via SQL.

use std::collections::{HashMap, HashSet};
use std::time::{SystemTime, UNIX_EPOCH};

use serde_json::{Value, json};
use sqlx::MySqlConnection;

use crate::bo::configuration_bo::ConfigurationBo;
use crate::bo::java_random::JavaRandom;
use crate::bo::mission_report_manager_bo::MissionReportManagerBo;
use crate::bo::realtime_emitter::RequirementEmit;
use crate::builder::UnitMissionReportBuilder;
use crate::dto::user_improvement::{ImprovementType, UserImprovementDto};
use crate::error::{OwgeError, OwgeResult};
use crate::model::UserStorage;
use crate::model::mission::Mission;
use crate::model::obtained_unit::ObtainedUnit;

/// An `obtained_units` row enriched with the unit catalog scalars and the owning
/// user's alliance — everything the combat loop needs without further round-trips.
/// Every column is decoded at its literal MySQL type.
#[derive(Debug, Clone, sqlx::FromRow)]
struct CombatUnitRow {
    // --- obtained_units ---
    id: u64,
    user_id: i32,
    count: u64,
    mission_id: Option<u64>,
    owner_unit_id: Option<u64>,

    // --- units (combat-relevant) ---
    unit_id: u16,
    unit_type_id: Option<u16>,
    unit_attack: Option<u16>,
    unit_health: Option<u16>,
    unit_shield: Option<u16>,
    unit_points: Option<u32>,
    unit_attack_rule_id: Option<u16>,
    unit_critical_attack_id: Option<u16>,
    unit_bypass_shield: i8,

    // --- units (report/DTO catalog scalars; not used by the combat loop) ---
    unit_name: String,
    unit_type_name: Option<String>,
    unit_description: Option<String>,
    unit_image: Option<u64>,
    unit_image_filename: Option<String>,
    unit_charge: Option<u16>,
    unit_time: Option<i32>,
    unit_primary_resource: Option<u32>,
    unit_secondary_resource: Option<u32>,
    unit_energy: Option<u16>,
    unit_speed: Option<f64>,
    unit_is_unique: u8,
    unit_can_fast_explore: i8,
    unit_is_invisible: i8,
    unit_stored_weight: u32,
    unit_storage_capacity: Option<u32>,
    unit_has_to_display_in_requirements: i8,
    unit_cloned_improvements: i8,

    // --- owning user ---
    username: String,
    user_alliance_id: Option<u16>,
}

impl CombatUnitRow {
    /// Build the embedded report unit DTO from the cached catalog scalars (works
    /// even after the row's `obtained_units` record was deleted by combat).
    fn to_unit_dto(&self) -> crate::dto::obtained_unit::ObtainedUnitUnitDto {
        crate::dto::obtained_unit::ObtainedUnitUnitDto {
            id: self.unit_id,
            name: self.unit_name.clone(),
            description: self.unit_description.clone(),
            image: self.unit_image,
            image_url: self
                .unit_image_filename
                .as_deref()
                .map(crate::dto::obtained_unit::compute_unit_image_url),
            has_to_display_in_requirements: self.unit_has_to_display_in_requirements != 0,
            points: self.unit_points,
            time: self.unit_time,
            primary_resource: self.unit_primary_resource,
            secondary_resource: self.unit_secondary_resource,
            energy: self.unit_energy,
            type_id: self.unit_type_id,
            type_name: self.unit_type_name.clone(),
            attack: self.unit_attack,
            health: self.unit_health,
            shield: self.unit_shield,
            charge: self.unit_charge,
            is_unique: self.unit_is_unique != 0,
            can_fast_explore: self.unit_can_fast_explore != 0,
            speed: self.unit_speed,
            cloned_improvements: self.unit_cloned_improvements != 0,
            bypass_shield: self.unit_bypass_shield != 0,
            is_invisible: self.unit_is_invisible != 0,
            stored_weight: self.unit_stored_weight,
            storage_capacity: self.unit_storage_capacity,
        }
    }
}

/// Runtime per-stack combat state — the Rust counterpart of `AttackObtainedUnit`.
struct AttackObtainedUnit {
    row: CombatUnitRow,
    initial_count: u64,
    final_count: u64,
    pending_attack: f64,
    no_attack: bool,
    available_shield: f64,
    available_health: f64,
    total_shield: f64,
    total_health: f64,
}

/// Per-user combat accumulator — the Rust counterpart of `AttackUserInformation`.
struct AttackUserInformation {
    user_id: i32,
    username: String,
    alliance_id: Option<u16>,
    earned_points: f64,
    /// Indices (into `AttackInformation.units`) of this user's own stacks.
    unit_indices: Vec<usize>,
    /// Indices of the stacks this user may attack (enemies), computed up front.
    attackable_indices: Vec<usize>,
    /// The owner's aggregated improvements (`AttackUserInformation.userImprovement`).
    improvement: UserImprovementDto,
}

/// The whole-battle state — the Rust counterpart of `AttackInformation`.
struct AttackInformation {
    attack_mission_id: u64,
    /// All combat stacks (attackers + defenders), in `units` order.
    units: Vec<AttackObtainedUnit>,
    /// `user_id` -> accumulator.
    users: HashMap<i32, AttackUserInformation>,
    /// Stable user ordering for deterministic report output.
    user_order: Vec<i32>,
    /// Ids of obtained units that hold other units inside them.
    units_storing_units: HashSet<u64>,
    /// obtained_unit ids whose stack was fully wiped (to delete + carrier-free).
    deleted_unit_ids: Vec<u64>,
    /// obtained_unit ids detached from a destroyed carrier (owner_unit_id -> NULL).
    detached_unit_ids: Vec<u64>,
    /// mission ids that became empty and must be deleted.
    emptied_mission_ids: HashSet<u64>,
    /// whether the attack mission itself ran out of units.
    removed: bool,
    /// `usersWithDeletedMissions` — users whose (non-attack) mission was emptied
    /// and deleted; drives the post-commit `emitUnitMissions`/`emitUserData` block.
    users_with_deleted_missions: HashSet<i32>,
    /// `usersWithChangedCounts` — users whose stack count changed during combat.
    users_with_changed_counts: HashSet<i32>,
    /// `updatePoints` altered users (a stack persisted with `saveWithChange`).
    altered_users: HashSet<i32>,
    /// `targetPlanet.getOwner()` captured at combat time (pre-conquest reassign).
    target_owner: Option<i32>,
    /// `UnitCaptureContext` accumulator (`HandleUnitCaptureListener`) — one entry
    /// per successful capture, drained into per-captor reports at `emitAttackEnd`.
    capture_contexts: Vec<CaptureContext>,
    /// xorshift64 state driving the capture probability/amount rolls (the Rust
    /// stand-in for `Math.random()`, kept separate from the shuffle PRNG). Used only
    /// when deterministic mode is OFF.
    rng_state: u64,
    /// DETERMINISTIC MODE (`ATTACK_DETERMINISTIC_RNG = TRUE`): a single
    /// `java.util.Random` seeded from the attack `mission.id`, consumed in Java's
    /// exact draw order (shuffle, then capture prob/amount per pair). `None` keeps
    /// the legacy clock-seeded xorshift path unchanged. See `ATTACK_PARITY_PLAN.md`.
    det_rng: Option<JavaRandom>,
    /// The i64 attack seed (`mission.id`), emitted on every `@@RNG@@` trace line.
    det_seed: i64,
    /// Per-attack trace sequence counter (starts at 0, increments each draw).
    trace_seq: u64,
    /// `(captor_user_id, report_id)` for each capture report written at
    /// `emitAttackEnd`, surfaced so the processor can emit `mission_report_new` +
    /// `mission_report_count_change` post-commit.
    capture_report_pairs: Vec<(i32, u64)>,
}

impl AttackInformation {
    /// Emit one `@@RNG@@ ` trace line per primitive draw (deterministic mode only)
    /// and bump `trace_seq`. Compact single-line JSON, no spaces; the harness greps
    /// stderr for the `@@RNG@@ ` prefix and aligns the two backends by `seq`.
    fn trace_draw(
        &mut self,
        site: &str,
        bound: Option<i32>,
        attacker: Option<i64>,
        victim: Option<i64>,
        killed: Option<i64>,
        result: &Value,
    ) {
        let line = json!({
            "seq": self.trace_seq,
            "site": site,
            "seed": self.det_seed,
            "bound": bound,
            "attacker": attacker,
            "victim": victim,
            "killed": killed,
            "result": result,
        });
        eprintln!("@@RNG@@ {line}");
        self.trace_seq += 1;
    }

    /// One deterministic `nextInt(bound)` draw for the explicit Fisher-Yates shuffle,
    /// traced as `site="shuffle"`. Caller guarantees deterministic mode is on.
    fn det_shuffle_next(&mut self, bound: i32) -> usize {
        let j = self
            .det_rng
            .as_mut()
            .expect("deterministic mode")
            .next_int_bound(bound);
        self.trace_draw("shuffle", Some(bound), None, None, None, &json!(j));
        j as usize
    }

    /// One deterministic `nextDouble()` capture draw, traced as `site` with the
    /// attacker/victim obtained_unit ids (and `killed` for the amount draw).
    fn det_capture_next(
        &mut self,
        site: &str,
        attacker: i64,
        victim: i64,
        killed: Option<i64>,
    ) -> f64 {
        let d = self
            .det_rng
            .as_mut()
            .expect("deterministic mode")
            .next_double();
        self.trace_draw(site, None, Some(attacker), Some(victim), killed, &json!(d));
        d
    }
}

/// One successful unit capture — the Rust counterpart of `UnitCaptureContext`,
/// reduced to what the `unitCaptureInformation` report entry needs.
struct CaptureContext {
    /// The user whose units did the capturing (the report owner).
    captor_user_id: i32,
    /// The captured (victim) unit's catalog DTO (`entry.unit`).
    victim_unit_dto: crate::dto::obtained_unit::ObtainedUnitUnitDto,
    /// The victim's owner before the capture (`entry.oldOwner` = `{id, username}`).
    victim_old_owner_id: i32,
    victim_old_owner_username: String,
    /// How many units were captured (`entry.capturedCount`).
    captured_count: u64,
}

/// The post-commit emit inputs `AttackMissionManagerBo` collects during combat,
/// handed back to the processor to schedule the per-user websocket emits.
pub struct AttackEmitData {
    pub users_with_deleted_missions: Vec<i32>,
    pub users_with_changed_counts: Vec<i32>,
    pub altered_users: Vec<i32>,
    pub target_owner: Option<i32>,
    /// Requirement-trigger `*_unlocked_change` pushes from the combat unit-count
    /// changes (`AttackMissionProcessor.triggerUnitRequirementChange`), drained by
    /// the processor after the firing tx commits.
    pub requirement_emits: Vec<RequirementEmit>,
    /// `(captor_user_id, report_id)` for each capture report written at
    /// `emitAttackEnd`; the processor emits `mission_report_new` +
    /// `mission_report_count_change` for each after commit.
    pub capture_report_pairs: Vec<(i32, u64)>,
}

pub struct AttackMissionManagerBo;

impl AttackMissionManagerBo {
    /// Entry point used by the attack/counterattack/conquest processors.
    ///
    /// Loads the defenders at the target planet, merges them with `involved_units`
    /// (the attacking mission's stacks), runs the combat loop, persists the result,
    /// and returns the `attackInformation` JSON.
    pub async fn process_attack(
        conn: &mut MySqlConnection,
        mission: &Mission,
        involved_units: &[ObtainedUnit],
    ) -> OwgeResult<(Value, AttackEmitData)> {
        let mut info = Self::build_attack_information(conn, mission, involved_units).await?;
        let mut requirement_emits = Vec::new();
        Self::start_attack(conn, mission, &mut info, &mut requirement_emits).await?;
        let emit_data = AttackEmitData {
            users_with_deleted_missions: info.users_with_deleted_missions.iter().copied().collect(),
            users_with_changed_counts: info.users_with_changed_counts.iter().copied().collect(),
            altered_users: info.altered_users.iter().copied().collect(),
            target_owner: info.target_owner,
            requirement_emits,
            capture_report_pairs: info.capture_report_pairs.clone(),
        };
        Ok((Self::to_attack_information_json(&info), emit_data))
    }

    /// `buildAttackInformation` — assemble every combat stack involved at the
    /// target planet plus the attacker's stacks, computing each one's combat stats.
    async fn build_attack_information(
        conn: &mut MySqlConnection,
        mission: &Mission,
        involved_units: &[ObtainedUnit],
    ) -> OwgeResult<AttackInformation> {
        // DETERMINISTIC MODE gate: `configuration` row name='ATTACK_DETERMINISTIC_RNG'
        // value 'TRUE' (case-insensitive, anything else = false; absent = false —
        // parsed like Java `Boolean.parseBoolean`). When ON we build exactly ONE
        // `JavaRandom` seeded from the attack `mission.id` and consume it in Java's
        // draw order; when OFF the legacy clock-seeded xorshift path is untouched.
        let deterministic = ConfigurationBo::find_or_set_default(
            &mut *conn,
            "ATTACK_DETERMINISTIC_RNG",
            "FALSE",
        )
        .await?
        .value
        .eq_ignore_ascii_case("true");
        let det_seed = mission.id as i64;
        let det_rng = if deterministic {
            Some(JavaRandom::new(det_seed))
        } else {
            None
        };

        let mut info = AttackInformation {
            attack_mission_id: mission.id,
            units: Vec::new(),
            users: HashMap::new(),
            user_order: Vec::new(),
            units_storing_units: HashSet::new(),
            deleted_unit_ids: Vec::new(),
            detached_unit_ids: Vec::new(),
            emptied_mission_ids: HashSet::new(),
            removed: false,
            users_with_deleted_missions: HashSet::new(),
            users_with_changed_counts: HashSet::new(),
            altered_users: HashSet::new(),
            target_owner: None,
            capture_contexts: Vec::new(),
            capture_report_pairs: Vec::new(),
            rng_state: SystemTime::now()
                .duration_since(UNIX_EPOCH)
                .map(|d| d.as_nanos() as u64)
                .unwrap_or(0x2545_F491_4F6C_DD1D)
                ^ 0x9E37_79B9_7F4A_7C15
                | 1,
            det_rng,
            det_seed,
            trace_seq: 0,
        };

        // --- Defenders involved at the target planet, excluding this mission's
        // own stacks (`!attackMission.equals(unit.getMission())`). Mirrors
        // ObtainedUnitFinderBo.findInvolvedInAttack:
        //  * units sitting on the planet with no mission (sourcePlanet = planet,
        //    mission NULL),
        //  * DEPLOYED units whose targetPlanet is the planet,
        //  * CONQUEST units toward the planet with >=10% of required time elapsed.
        if let Some(target_planet_id) = mission.target_planet.map(|p| p as u64) {
            let defenders = sqlx::query_as::<_, CombatUnitRow>(SELECT_DEFENDERS_SQL)
                .bind(target_planet_id) // sourcePlanet, mission NULL
                .bind(target_planet_id) // DEPLOYED targetPlanet
                .bind(target_planet_id) // CONQUEST targetPlanet
                .bind(mission.id) // exclude the attack mission's own stacks
                .fetch_all(&mut *conn)
                .await?;
            for row in defenders {
                Self::add_unit(conn, &mut info, row).await?;
            }
        }

        // --- The attacker's stacks (findByMissionId). The caller already loaded
        // them; re-enrich each with its unit/user scalars so combat math matches.
        for ou in involved_units {
            if let Some(row) = Self::load_combat_unit_row(conn, ou.id).await? {
                Self::add_unit(conn, &mut info, row).await?;
            }
        }

        Ok(info)
    }

    /// `addUnit` — register one obtained unit (its computed combat stack) under its
    /// owning user, computing total attack/shield/health with the owner's
    /// improvements applied.
    async fn add_unit(
        conn: &mut MySqlConnection,
        info: &mut AttackInformation,
        row: CombatUnitRow,
    ) -> OwgeResult<()> {
        let user_id = row.user_id;
        if !info.users.contains_key(&user_id) {
            // `improvementBo.findUserImprovement(userEntity)`. The combat loop only
            // consults the per-unit-type ATTACK/SHIELD/DEFENSE improvements, so we
            // aggregate exactly those on the locked connection rather than the full
            // `UserImprovementBo::find_user_improvement` (which takes a pool and
            // would not see this transaction's uncommitted mutations).
            let improvement = Self::load_unit_type_improvements(conn, user_id).await?;
            info.users.insert(
                user_id,
                AttackUserInformation {
                    user_id,
                    username: row.username.clone(),
                    alliance_id: row.user_alliance_id,
                    earned_points: 0.0,
                    unit_indices: Vec::new(),
                    attackable_indices: Vec::new(),
                    improvement,
                },
            );
            info.user_order.push(user_id);
        }
        // Improvement inheritance walks the parent chain ONLY while each level's
        // `has_to_inherit_improvements` is TRUE (Java `GroupedImprovement
        // .findUnitTypeImprovement`, `pojo/GroupedImprovement.java:74-86`), stopping
        // at the first non-inheriting level. This is distinct from the rule/critical
        // chain (`unit_type_chain`), which inherits unconditionally.
        let improvement_chain =
            Self::unit_type_improvement_chain(conn, row.unit_type_id).await?;
        let stack = {
            let user = info.users.get(&user_id).expect("just inserted");
            Self::create_combat_stack(&row, &user.improvement, &improvement_chain)
        };
        if let Some(owner_unit_id) = row.owner_unit_id {
            info.units_storing_units.insert(owner_unit_id);
        }
        let idx = info.units.len();
        info.units.push(stack);
        info.users
            .get_mut(&user_id)
            .expect("present")
            .unit_indices
            .push(idx);
        Ok(())
    }

    /// `AttackObtainedUnitBo.create` — compute the stack's total attack, shield and
    /// health, each boosted by the owner's `ATTACK`/`SHIELD`/`DEFENSE` improvement
    /// for the unit's type (as a rational: percentage / 100).
    fn create_combat_stack(
        row: &CombatUnitRow,
        improvement: &UserImprovementDto,
        unit_type_chain: &[u16],
    ) -> AttackObtainedUnit {
        let initial_count = row.count;
        let count = initial_count as f64;

        let attack_imp = as_rational(
            improvement
                .find_unit_type_improvement_for_chain(ImprovementType::Attack, unit_type_chain),
        );
        let shield_imp = as_rational(
            improvement
                .find_unit_type_improvement_for_chain(ImprovementType::Shield, unit_type_chain),
        );
        let defense_imp = as_rational(
            improvement
                .find_unit_type_improvement_for_chain(ImprovementType::Defense, unit_type_chain),
        );

        let mut total_attack = count * row.unit_attack.unwrap_or(0) as f64;
        total_attack += total_attack * attack_imp;
        let mut total_shield = count * row.unit_shield.unwrap_or(0) as f64;
        total_shield += total_shield * shield_imp;
        let mut total_health = count * row.unit_health.unwrap_or(0) as f64;
        total_health += total_health * defense_imp;

        AttackObtainedUnit {
            row: row.clone(),
            initial_count,
            final_count: initial_count,
            pending_attack: total_attack,
            no_attack: false,
            available_shield: total_shield,
            available_health: total_health,
            total_shield,
            total_health,
        }
    }

    /// `startAttack` — shuffle, compute each user's attackable (enemy) set, run the
    /// attack loop, then persist points + surviving counts.
    async fn start_attack(
        conn: &mut MySqlConnection,
        mission: &Mission,
        info: &mut AttackInformation,
        req_emits: &mut Vec<RequirementEmit>,
    ) -> OwgeResult<()> {
        let target_planet_id = mission.target_planet.map(|p| p as u64);
        // `attackObtainedUnitBo.shuffleUnits(units)` — Collections.shuffle. We
        // shuffle an index permutation so the per-user index lists stay valid.
        // To avoid pulling in a new crate, a clock-seeded xorshift Fisher-Yates
        // stands in for `java.util.Random` (combat targeting only needs an
        // unbiased random order, not reproducibility).
        let mut order: Vec<usize> = (0..info.units.len()).collect();
        if info.det_rng.is_some() {
            // DETERMINISTIC: explicit Fisher-Yates over the index permutation so each
            // `nextInt` draw is traced. The permutation array has the SAME length and
            // initial order as Java's shuffled `units` list (defenders in
            // category order, then the attacker's stacks), so the swap indices match
            // Java's `Collections.shuffle(units, rnd)` byte-for-byte. Mirrors JDK
            // `Collections.shuffle`: for i = n down to 2, swap(i-1, nextInt(i)).
            let n = order.len();
            for i in (2..=n).rev() {
                let j = info.det_shuffle_next(i as i32);
                order.swap(i - 1, j);
            }
        } else {
            // NON-DETERMINISTIC (unchanged): clock-seeded xorshift Fisher-Yates.
            shuffle_indices(&mut order);
        }

        // Per user, the indices of enemy stacks they may attack.
        for user_id in info.user_order.clone() {
            let (uid, ualliance) = {
                let u = &info.users[&user_id];
                (u.user_id, u.alliance_id)
            };
            let attackable: Vec<usize> = order
                .iter()
                .copied()
                .filter(|&i| {
                    let target = &info.units[i];
                    are_enemies(
                        uid,
                        ualliance,
                        target.row.user_id,
                        target.row.user_alliance_id,
                    )
                })
                .collect();
            info.users
                .get_mut(&user_id)
                .expect("present")
                .attackable_indices = attackable;
        }

        // Capture the target planet's owner (pre-conquest reassignment) for the
        // post-commit emit block (`targetPlanet.getOwner()`).
        if let Some(planet_id) = target_planet_id {
            let owner: Option<Option<i32>> =
                sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
                    .bind(planet_id)
                    .fetch_optional(&mut *conn)
                    .await?;
            info.target_owner = owner.flatten();
        }

        Self::do_attack(conn, info, &order).await?;
        Self::update_points(conn, info).await?;
        Self::trigger_unit_requirement_changes(conn, info, req_emits).await?;

        // `attackEventEmitter.emitAttackEnd(attackInformation)` — write one capture
        // report per user that captured anything (the captured units themselves were
        // already stationed during `do_attack`).
        info.capture_report_pairs = Self::create_capture_reports(conn, mission, info).await?;

        // The per-user websocket emits (emitUnitMissions / emitUserData /
        // emitObtainedUnits / emitEnemyMissionsChange) are scheduled post-commit by
        // the caller from the collected `users_with_*` / `target_owner` state.
        Ok(())
    }

    /// `doAttack` — every stack attacks, in shuffled order, the enemy stacks it is
    /// allowed to hit (filtered by attack rules), ordered by descending critical
    /// score, until its attack is exhausted (`noAttack`).
    async fn do_attack(
        conn: &mut MySqlConnection,
        info: &mut AttackInformation,
        order: &[usize],
    ) -> OwgeResult<()> {
        for &attacker_idx in order {
            // Snapshot the attacker's attack-rule context and attackable set.
            let (attacker_user_id, attack_rule_id, attacker_unit_type) = {
                let a = &info.units[attacker_idx];
                (a.row.user_id, a.row.unit_attack_rule_id, a.row.unit_type_id)
            };
            let attackable_indices = info.users[&attacker_user_id].attackable_indices.clone();

            // Resolve this attacker's effective attack rule once (recursion up the
            // unit-type parent chain when the unit has no own rule).
            let effective_rule_id = match attack_rule_id {
                Some(id) => Some(id),
                None => Self::find_attack_rule(conn, attacker_unit_type).await?,
            };
            let rule_entries = match effective_rule_id {
                Some(id) => Self::load_attack_rule_entries(conn, id).await?,
                None => Vec::new(),
            };

            // Build (target_idx, score) for every attackable enemy that this rule
            // permits, then sort by descending score (Java sorts `b.score - a.score`).
            let mut scored: Vec<(usize, f32)> = Vec::new();
            for &target_idx in &attackable_indices {
                if target_idx == attacker_idx {
                    continue;
                }
                let can = Self::can_attack(conn, &rule_entries, &info.units[target_idx]).await?;
                if can {
                    let score = Self::find_critical_score(
                        conn,
                        &info.units[attacker_idx],
                        &info.units[target_idx],
                    )
                    .await?;
                    scored.push((target_idx, score));
                }
            }
            scored.sort_by(|a, b| b.1.partial_cmp(&a.1).unwrap_or(std::cmp::Ordering::Equal));

            for (target_idx, score) in scored {
                if info.units[target_idx].final_count != 0 {
                    Self::attack_target(conn, info, attacker_idx, target_idx, score).await?;
                }
                if info.units[attacker_idx].no_attack {
                    break;
                }
            }
        }
        Ok(())
    }

    /// `attackTarget` — apply one attacker→target hit: compute kills, drain shield
    /// and health, carry over leftover attack, and on a wipe delete the stack and
    /// free its carrier / empty its mission.
    async fn attack_target(
        conn: &mut MySqlConnection,
        info: &mut AttackInformation,
        source_idx: usize,
        target_idx: usize,
        score: f32,
    ) -> OwgeResult<()> {
        let original_attack_value = info.units[source_idx].pending_attack;
        let my_attack = original_attack_value * score as f64;
        // `attackBypassShieldService.bypassShields(source, target)` — the source
        // unit's own `bypassShield` flag OR an ACTIVE time-special rule of type
        // `TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD` (owned by the SOURCE's user) that
        // targets the TARGET unit. Note: `addPointsAndUpdateCount` below mirrors
        // Java in consulting ONLY the own flag (no time-special), so that site is
        // intentionally left as the bare `unit_bypass_shield` read.
        let bypass_shield = Self::bypass_shields(conn, info, source_idx, target_idx).await?;

        let victim_health = {
            let t = &info.units[target_idx];
            if bypass_shield {
                t.available_health
            } else {
                t.available_health + t.available_shield
            }
        };

        let killed_count =
            Self::add_points_and_update_count(info, my_attack, source_idx, target_idx);
        // `attackEventEmitter.emitAfterUnitKilledCalculation` — roll the capture
        // rule for this attacker→victim pair (runs before the wipe DELETE below,
        // matching Java's call order inside `addPointsAndUpdateCount`).
        Self::maybe_capture(conn, info, source_idx, target_idx, killed_count).await?;

        if victim_health > my_attack {
            // Target survives; attacker is spent.
            {
                let s = &mut info.units[source_idx];
                s.pending_attack = 0.0;
                s.no_attack = true;
            }
            let t = &mut info.units[target_idx];
            if bypass_shield {
                t.available_health -= my_attack;
            } else {
                let attack_distributed = my_attack / 2.0;
                t.available_shield -= attack_distributed;
                t.available_health -= attack_distributed;
            }
            if t.available_shield < 0.0 {
                t.available_health += t.available_shield;
            }
            // `if (!initialCount.equals(finalCount)) usersWithChangedCounts.add(user)`.
            let changed = t.initial_count != t.final_count;
            let target_user_id = t.row.user_id;
            if changed {
                info.users_with_changed_counts.insert(target_user_id);
            }
        } else {
            // Target wiped; carry over leftover attack (clamped to original).
            {
                let s = &mut info.units[source_idx];
                s.pending_attack = my_attack - victim_health;
                if s.pending_attack > original_attack_value {
                    s.pending_attack = original_attack_value;
                }
            }
            let (target_ou_id, target_mission_id, target_user_id) = {
                let t = &mut info.units[target_idx];
                t.available_health = 0.0;
                t.available_shield = 0.0;
                t.final_count = 0;
                (t.row.id, t.row.mission_id, t.row.user_id)
            };
            Self::maybe_unset_holder_unit(info, target_ou_id);
            // `obtainedUnitRepository.delete(target)`.
            sqlx::query("DELETE FROM obtained_units WHERE id = ?")
                .bind(target_ou_id)
                .execute(&mut *conn)
                .await?;
            info.deleted_unit_ids.push(target_ou_id);
            Self::delete_mission_if_required(conn, info, target_mission_id, target_user_id).await?;
            // `usersWithChangedCounts.add(target.getUser())` (wipe always counts).
            info.users_with_changed_counts.insert(target_user_id);
        }
        Ok(())
    }

    /// `AttackBypassShieldService.bypassShields(source, target)` — true when the
    /// SOURCE combat unit's own `bypassShield` flag is set, OR an ACTIVE
    /// time-special rule of type `TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD` owned by
    /// the SOURCE's user targets the TARGET unit
    /// (`ActiveTimeSpecialRuleFinderService.existsRuleMatchingUnitDestination`).
    async fn bypass_shields(
        conn: &mut MySqlConnection,
        info: &AttackInformation,
        source_idx: usize,
        target_idx: usize,
    ) -> OwgeResult<bool> {
        if info.units[source_idx].row.unit_bypass_shield != 0 {
            return Ok(true);
        }
        let source_user_id = info.units[source_idx].row.user_id;
        let (target_unit_id, target_unit_type) = {
            let t = &info.units[target_idx];
            (t.row.unit_id, t.row.unit_type_id)
        };
        // A unit always has a type in practice; a missing type id can only fail to
        // match a `UNIT_TYPE` rule (the helper walks `parent_type` from this id).
        crate::bo::active_time_special_rule_finder_bo::ActiveTimeSpecialRuleFinderBo::exists_rule_matching_unit_destination(
            conn,
            source_user_id,
            target_unit_id as i64,
            target_unit_type.unwrap_or(0),
            "TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD",
        )
        .await
    }

    /// `maybeUnsetHolderUnit` — when the wiped stack was holding other stacks,
    /// detach those carried stacks (`ownerUnit = null`) and persist it.
    fn maybe_unset_holder_unit(info: &mut AttackInformation, target_ou_id: u64) {
        if info.units_storing_units.contains(&target_ou_id) {
            let mut detached = Vec::new();
            for stack in info.units.iter_mut() {
                if stack.row.owner_unit_id == Some(target_ou_id) {
                    stack.row.owner_unit_id = None;
                    detached.push(stack.row.id);
                    // The carried stacks survive; their persisted owner_unit_id is
                    // cleared in update_points (see the carrier-free UPDATE there).
                }
            }
            info.detached_unit_ids.extend(detached);
        }
    }

    /// `addPointsAndUpdateCount` — `killedCount = floor(usedAttack / healthPerUnit)`
    /// (clamped to the remaining count), decrement the victim's `finalCount`, and
    /// award the attacker `killedCount * victimUnit.points`.
    fn add_points_and_update_count(
        info: &mut AttackInformation,
        used_attack: f64,
        source_idx: usize,
        victim_idx: usize,
    ) -> u64 {
        let (health_for_each_unit, killed_count, victim_points) = {
            let victim = &info.units[victim_idx];
            let source_bypass = info.units[source_idx].row.unit_bypass_shield != 0;
            let denom = victim.initial_count as f64;
            let health_for_each_unit = if source_bypass {
                victim.total_health / denom
            } else {
                (victim.total_health + victim.total_shield) / denom
            };
            let mut killed = (used_attack / health_for_each_unit).floor() as i64;
            let remaining = victim.final_count as i64;
            if killed > remaining {
                killed = remaining;
            }
            (
                health_for_each_unit,
                killed.max(0) as u64,
                victim.row.unit_points.unwrap_or(0) as f64,
            )
        };
        let _ = health_for_each_unit;

        {
            let victim = &mut info.units[victim_idx];
            victim.final_count = victim.final_count.saturating_sub(killed_count);
        }
        let attacker_user_id = info.units[source_idx].row.user_id;
        if let Some(user) = info.users.get_mut(&attacker_user_id) {
            user.earned_points += killed_count as f64 * victim_points;
        }
        killed_count
    }

    /// `HandleUnitCaptureListener.onAfterUnitKilledCalculation` — roll the
    /// `UNIT_CAPTURE` rule for this attacker→victim pair and, on success, station
    /// the captured units for the captor and record the capture for the report.
    async fn maybe_capture(
        conn: &mut MySqlConnection,
        info: &mut AttackInformation,
        source_idx: usize,
        victim_idx: usize,
        killed: u64,
    ) -> OwgeResult<()> {
        let (captor_user_id, attacker_unit_id, attacker_unit_type, attacker_ou_id) = {
            let a = &info.units[source_idx];
            (a.row.user_id, a.row.unit_id, a.row.unit_type_id, a.row.id)
        };
        let (victim_unit_id, victim_unit_type, victim_ou_id) = {
            let v = &info.units[victim_idx];
            (v.row.unit_id, v.row.unit_type_id, v.row.id)
        };

        // findRule(UNIT_CAPTURE, attackerUnit, victimUnit)
        //   .or(findRuleByActiveTimeSpecialsAndTargetUnit(captor, victimUnit))
        let Some(extra_args) = Self::find_capture_rule(
            conn,
            attacker_unit_id,
            attacker_unit_type,
            victim_unit_id,
            victim_unit_type,
            captor_user_id,
        )
        .await?
        else {
            return Ok(());
        };

        // `.filter(rule -> hasExtraArg(rule, 0) && hasExtraArg(rule, 1))`
        let parts: Vec<&str> = extra_args.split('#').collect();
        if parts.len() < 2 {
            return Ok(());
        }
        // `Long.parseLong(args.get(0|1))` — malformed args yield no capture here.
        let (Ok(probability), Ok(percentage)) = (parts[0].parse::<i64>(), parts[1].parse::<i64>())
        else {
            return Ok(());
        };

        // `.filter(args -> Math.random() * 100 < probability)`. In deterministic mode
        // the double comes from the shared `JavaRandom` (traced); otherwise from the
        // legacy clock-seeded xorshift. The trace ids are the attacker/victim
        // obtained_unit ids (`row.id`).
        let capture_prob = if info.det_rng.is_some() {
            info.det_capture_next(
                "capture_prob",
                attacker_ou_id as i64,
                victim_ou_id as i64,
                None,
            )
        } else {
            next_unit_f64(&mut info.rng_state)
        };
        if !(capture_prob * 100.0 < probability as f64) {
            return Ok(());
        }

        // captured = floor(Math.random() * floor(killed * (percentage * 0.01))) + 1
        let scaled = (killed as f64 * (percentage as f64 * 0.01)).floor();
        let capture_amount = if info.det_rng.is_some() {
            info.det_capture_next(
                "capture_amount",
                attacker_ou_id as i64,
                victim_ou_id as i64,
                Some(killed as i64),
            )
        } else {
            next_unit_f64(&mut info.rng_state)
        };
        let captured = (capture_amount * scaled + 1.0).floor();
        if captured < 1.0 {
            return Ok(());
        }
        Self::save_captured(conn, info, source_idx, victim_idx, captured as u64).await
    }

    /// `UnitRuleFinderService.findRule` for `UNIT_CAPTURE`, returning the matching
    /// rule's `extra_args` (the first existing rule wins; a `NULL`-args match — like
    /// no match — yields `None`, since Java applies the args filter *after* the
    /// lookup and never falls through). Lookup order: UNIT→UNIT, UNIT→UNIT_TYPE
    /// chain, UNIT_TYPE chain→UNIT, UNIT_TYPE chain × UNIT_TYPE chain, then the
    /// captor's ACTIVE time-specials → the victim unit/type.
    async fn find_capture_rule(
        conn: &mut MySqlConnection,
        attacker_unit_id: u16,
        attacker_unit_type: Option<u16>,
        victim_unit_id: u16,
        victim_unit_type: Option<u16>,
        captor_user_id: i32,
    ) -> OwgeResult<Option<String>> {
        let attacker_chain = Self::unit_type_chain(conn, attacker_unit_type).await?;
        let victim_chain = Self::unit_type_chain(conn, victim_unit_type).await?;

        // 1. unit vs unit
        if let Some(extra) = Self::lookup_rule(
            conn,
            "UNIT",
            attacker_unit_id as i64,
            "UNIT",
            victim_unit_id as i64,
        )
        .await?
        {
            return Ok(extra);
        }
        // 2. unit vs unit-type (up the victim's type chain)
        for &t in &victim_chain {
            if let Some(extra) =
                Self::lookup_rule(conn, "UNIT", attacker_unit_id as i64, "UNIT_TYPE", t as i64)
                    .await?
            {
                return Ok(extra);
            }
        }
        // 3. unit-type (up the attacker's chain) vs unit
        for &f in &attacker_chain {
            if let Some(extra) =
                Self::lookup_rule(conn, "UNIT_TYPE", f as i64, "UNIT", victim_unit_id as i64)
                    .await?
            {
                return Ok(extra);
            }
        }
        // 4. unit-type vs unit-type. Java `unitTypeVsUnitTypeOptional` recurses the
        // VICTIM (to) parent chain to exhaustion at each attacker level BEFORE
        // stepping the ATTACKER (from) chain — `(f,t)` then `(f, t.parent...)` then
        // `(f.parent, t)`. The attacker-outer × victim-inner loop this replaced gave
        // a different first match when several overlapping type-chain capture rules
        // exist (D2). Replicate the recursion order exactly via depth-first walk
        // over the precomputed chains (duplicate probes just re-hit the same row, so
        // the first present rule still wins identically to Java).
        if let Some(extra) =
            Self::unit_type_vs_unit_type_capture(conn, &attacker_chain, 0, &victim_chain, 0).await?
        {
            return Ok(extra);
        }
        // 5. the captor's ACTIVE time-specials vs the victim unit / unit-type
        let time_special_ids: Vec<u64> = sqlx::query_scalar(
            "SELECT time_special_id FROM active_time_specials \
              WHERE user_id = ? AND state = 'ACTIVE'",
        )
        .bind(captor_user_id)
        .fetch_all(&mut *conn)
        .await?;
        for ts_id in time_special_ids {
            if let Some(extra) = Self::lookup_rule(
                conn,
                "TIME_SPECIAL",
                ts_id as i64,
                "UNIT",
                victim_unit_id as i64,
            )
            .await?
            {
                return Ok(extra);
            }
            for &t in &victim_chain {
                if let Some(extra) =
                    Self::lookup_rule(conn, "TIME_SPECIAL", ts_id as i64, "UNIT_TYPE", t as i64)
                        .await?
                {
                    return Ok(extra);
                }
            }
        }
        Ok(None)
    }

    /// Byte-exact port of Java `UnitRuleFinderService.unitTypeVsUnitTypeOptional`'s
    /// recursion order for the UNIT_TYPE×UNIT_TYPE capture-rule probe: try
    /// `(from[fi], to[ti])`, then exhaust the victim (to) chain
    /// (`unitTypeVsUnitTypeOptional(from[fi], to.parent)`), then step the attacker
    /// (from) chain (`unitTypeVsUnitTypeOptional(from.parent, to[ti])`). The chains
    /// are `[self, parent, ...]`, so `chain[idx + 1]` is "parent". Returning the
    /// first present rule reproduces Java's `Optional::or` short-circuit (fixes D2).
    fn unit_type_vs_unit_type_capture<'a>(
        conn: &'a mut MySqlConnection,
        from_chain: &'a [u16],
        fi: usize,
        to_chain: &'a [u16],
        ti: usize,
    ) -> std::pin::Pin<
        Box<dyn std::future::Future<Output = OwgeResult<Option<Option<String>>>> + Send + 'a>,
    > {
        Box::pin(async move {
            if fi >= from_chain.len() || ti >= to_chain.len() {
                return Ok(None);
            }
            // try (from[fi], to[ti])
            if let Some(extra) = Self::lookup_rule(
                conn,
                "UNIT_TYPE",
                from_chain[fi] as i64,
                "UNIT_TYPE",
                to_chain[ti] as i64,
            )
            .await?
            {
                return Ok(Some(extra));
            }
            // .or -> recurse the victim (to) chain first
            if let Some(found) =
                Self::unit_type_vs_unit_type_capture(conn, from_chain, fi, to_chain, ti + 1).await?
            {
                return Ok(Some(found));
            }
            // .or -> then step the attacker (from) chain
            if let Some(found) =
                Self::unit_type_vs_unit_type_capture(conn, from_chain, fi + 1, to_chain, ti).await?
            {
                return Ok(Some(found));
            }
            Ok(None)
        })
    }

    /// One `rules` lookup for `type = 'UNIT_CAPTURE'`. The outer `Option` is row
    /// presence; the inner `Option<String>` is the (nullable) `extra_args` column.
    async fn lookup_rule(
        conn: &mut MySqlConnection,
        origin_type: &str,
        origin_id: i64,
        destination_type: &str,
        destination_id: i64,
    ) -> OwgeResult<Option<Option<String>>> {
        Ok(sqlx::query_scalar::<_, Option<String>>(
            "SELECT extra_args FROM rules \
              WHERE type = 'UNIT_CAPTURE' AND origin_type = ? AND origin_id = ? \
                AND destination_type = ? AND destination_id = ? LIMIT 1",
        )
        .bind(origin_type)
        .bind(origin_id)
        .bind(destination_type)
        .bind(destination_id)
        .fetch_optional(&mut *conn)
        .await?)
    }

    /// `HandleUnitCaptureListener.saveCaptured` — build the captured stack, record
    /// the capture context for the report, and station the units via `moveUnit`.
    async fn save_captured(
        conn: &mut MySqlConnection,
        info: &mut AttackInformation,
        source_idx: usize,
        victim_idx: usize,
        captured: u64,
    ) -> OwgeResult<()> {
        let (captor_user_id, captor_ou_id, captor_mission_id) = {
            let a = &info.units[source_idx];
            (a.row.user_id, a.row.id, a.row.mission_id)
        };
        let victim_unit_id = info.units[victim_idx].row.unit_id;

        // Java: source/target come from the captor unit's mission when it has one,
        // otherwise the captured units land on the captor unit's own planet.
        let (source_planet, dest_planet): (Option<u64>, Option<u64>) = if let Some(mission_id) =
            captor_mission_id
        {
            let row: Option<(Option<i64>, Option<i64>)> =
                sqlx::query_as("SELECT source_planet, target_planet FROM missions WHERE id = ?")
                    .bind(mission_id)
                    .fetch_optional(&mut *conn)
                    .await?;
            match row {
                Some((s, t)) => (s.map(|v| v as u64), t.map(|v| v as u64)),
                None => (None, None),
            }
        } else {
            let sp: Option<Option<u64>> =
                sqlx::query_scalar("SELECT source_planet FROM obtained_units WHERE id = ?")
                    .bind(captor_ou_id)
                    .fetch_optional(&mut *conn)
                    .await?;
            (None, sp.flatten())
        };
        // Java dereferences `targetPlanet.getId()`; without a destination there is
        // nowhere to station the units, so skip (cannot happen for attack stacks,
        // which always carry a mission with a target planet).
        let Some(dest_planet) = dest_planet else {
            return Ok(());
        };

        // Record the capture for the end-of-battle report (`addToContext`).
        let (victim_unit_dto, victim_old_owner_id, victim_old_owner_username) = {
            let v = &info.units[victim_idx];
            (v.row.to_unit_dto(), v.row.user_id, v.row.username.clone())
        };
        info.capture_contexts.push(CaptureContext {
            captor_user_id,
            victim_unit_dto,
            victim_old_owner_id,
            victim_old_owner_username,
            captured_count: captured,
        });

        // `obtainedUnitBo.moveUnit(captured, captorUserId, destPlanetId)`.
        Self::station_captured_units(
            conn,
            captor_user_id,
            victim_unit_id,
            captured,
            source_planet,
            dest_planet,
        )
        .await
    }

    /// `ObtainedUnitBo.moveUnit` for a freshly captured (transient, mission-less)
    /// stack: merge into the captor's matching stack when one already exists,
    /// otherwise insert it — on the captor's own planet it lands directly
    /// (`source_planet = planet`), on a foreign planet it is attached to a
    /// `DEPLOYED` mission (created when absent), preserving the origin planet.
    async fn station_captured_units(
        conn: &mut MySqlConnection,
        user_id: i32,
        unit_id: u16,
        count: u64,
        source_planet: Option<u64>,
        dest_planet: u64,
    ) -> OwgeResult<()> {
        if crate::bo::mission_processor::is_planet_owned_by(conn, user_id, dest_planet).await? {
            // Owned-planet branch (saveWithAdding at the planet, no mission).
            let existing: Option<u64> = sqlx::query_scalar(
                "SELECT id FROM obtained_units \
                  WHERE user_id = ? AND unit_id = ? AND source_planet = ? \
                    AND mission_id IS NULL AND expiration_id IS NULL AND owner_unit_id IS NULL \
                  LIMIT 1",
            )
            .bind(user_id)
            .bind(unit_id)
            .bind(dest_planet)
            .fetch_optional(&mut *conn)
            .await?;
            if let Some(existing_id) = existing {
                sqlx::query("UPDATE obtained_units SET count = count + ? WHERE id = ?")
                    .bind(count)
                    .bind(existing_id)
                    .execute(&mut *conn)
                    .await?;
            } else {
                sqlx::query(
                    "INSERT INTO obtained_units (user_id, unit_id, count, source_planet, is_from_capture) \
                     VALUES (?, ?, ?, ?, 1)",
                )
                .bind(user_id)
                .bind(unit_id)
                .bind(count)
                .bind(dest_planet)
                .execute(&mut *conn)
                .await?;
            }
        } else {
            // Foreign-planet branch (DEPLOYED mission, origin planet preserved).
            let existing: Option<u64> = sqlx::query_scalar(
                "SELECT ou.id FROM obtained_units ou \
                   JOIN missions m ON m.id = ou.mission_id \
                   JOIN mission_types mt ON mt.id = m.type \
                  WHERE ou.user_id = ? AND ou.unit_id = ? AND ou.target_planet = ? \
                    AND mt.code = 'DEPLOYED' AND ou.expiration_id IS NULL LIMIT 1",
            )
            .bind(user_id)
            .bind(unit_id)
            .bind(dest_planet)
            .fetch_optional(&mut *conn)
            .await?;
            if let Some(existing_id) = existing {
                sqlx::query("UPDATE obtained_units SET count = count + ? WHERE id = ?")
                    .bind(count)
                    .bind(existing_id)
                    .execute(&mut *conn)
                    .await?;
            } else {
                let deployed_mission_id =
                    crate::bo::mission_processor::deploy::find_or_create_deployed_mission(
                        conn,
                        user_id,
                        source_planet,
                        dest_planet,
                    )
                    .await?;
                sqlx::query(
                    "INSERT INTO obtained_units \
                        (user_id, unit_id, count, source_planet, target_planet, mission_id, is_from_capture) \
                     VALUES (?, ?, ?, ?, ?, ?, 1)",
                )
                .bind(user_id)
                .bind(unit_id)
                .bind(count)
                .bind(source_planet.map(|v| v as i64))
                .bind(dest_planet)
                .bind(deployed_mission_id)
                .execute(&mut *conn)
                .await?;
            }
        }
        Ok(())
    }

    /// `HandleUnitCaptureListener.onAttackEnd` — group the captures by captor and
    /// write each captor a `unitCaptureInformation` report (sender = the captor,
    /// source/target = the attack mission's planets, `involvedUnits = []`). These
    /// reports are the captor's own (`is_enemy = false`) and are not linked back
    /// onto the mission (`MissionReportBo.create`).
    async fn create_capture_reports(
        conn: &mut MySqlConnection,
        mission: &Mission,
        info: &AttackInformation,
    ) -> OwgeResult<Vec<(i32, u64)>> {
        if info.capture_contexts.is_empty() {
            return Ok(Vec::new());
        }
        let source_planet = match mission.source_planet {
            Some(id) => crate::bo::mission_processor::load_planet_dto(conn, id as u64).await?,
            None => None,
        };
        let target_planet = match mission.target_planet {
            Some(id) => crate::bo::mission_processor::load_planet_dto(conn, id as u64).await?,
            None => None,
        };

        // Distinct captor ids, in first-seen order (deterministic report fan-out).
        let mut captors: Vec<i32> = Vec::new();
        for ctx in &info.capture_contexts {
            if !captors.contains(&ctx.captor_user_id) {
                captors.push(ctx.captor_user_id);
            }
        }

        let mut pairs = Vec::with_capacity(captors.len());
        for captor_id in captors {
            let username = info
                .users
                .get(&captor_id)
                .map(|u| u.username.clone())
                .unwrap_or_default();
            let entries: Vec<Value> = info
                .capture_contexts
                .iter()
                .filter(|c| c.captor_user_id == captor_id)
                .map(|c| {
                    json!({
                        "unit": c.victim_unit_dto,
                        "oldOwner": { "id": c.victim_old_owner_id, "username": c.victim_old_owner_username },
                        "capturedCount": c.captured_count,
                    })
                })
                .collect();
            let builder = UnitMissionReportBuilder::create_with(
                captor_id,
                &username,
                source_planet.as_ref(),
                target_planet.as_ref(),
                &[],
            )
            .with_unit_capture_information(Value::Array(entries));
            let inserted = MissionReportManagerBo::handle_mission_report_save_for_users(
                conn,
                &builder,
                /* is_enemy = */ false,
                &[captor_id],
            )
            .await?;
            pairs.extend(inserted);
        }
        Ok(pairs)
    }

    /// `deleteMissionIfRequired` — once the obtained unit is gone, if its mission
    /// has no remaining units delete the mission (or, for the attack mission
    /// itself, flag `removed`).
    async fn delete_mission_if_required(
        conn: &mut MySqlConnection,
        info: &mut AttackInformation,
        mission_id: Option<u64>,
        mission_user_id: i32,
    ) -> OwgeResult<()> {
        let Some(mission_id) = mission_id else {
            return Ok(());
        };
        let still_has: i64 =
            sqlx::query_scalar("SELECT COUNT(*) FROM obtained_units WHERE mission_id = ?")
                .bind(mission_id)
                .fetch_one(&mut *conn)
                .await?;
        if still_has == 0 {
            if info.attack_mission_id == mission_id {
                info.removed = true;
            } else {
                sqlx::query("DELETE FROM missions WHERE id = ?")
                    .bind(mission_id)
                    .execute(&mut *conn)
                    .await?;
                // emptied_mission_ids drives the websocket emissions; the mission's
                // owner gets `usersWithDeletedMissions.add(mission.getUser())`.
                info.emptied_mission_ids.insert(mission_id);
                info.users_with_deleted_missions.insert(mission_user_id);
            }
        }
        Ok(())
    }

    /// `updatePoints` — award each user their earned points and persist every
    /// stack whose count changed (`saveWithChange(-killed)`), then persist carrier
    /// detachment for any freed stacks.
    async fn update_points(
        conn: &mut MySqlConnection,
        info: &mut AttackInformation,
    ) -> OwgeResult<()> {
        for user_id in info.user_order.clone() {
            let (earned, indices) = {
                let u = &info.users[&user_id];
                (u.earned_points, u.unit_indices.clone())
            };
            if earned != 0.0 {
                // `userStorageBo.addPointsToUser`.
                sqlx::query("UPDATE user_storage SET points = points + ? WHERE id = ?")
                    .bind(earned)
                    .bind(user_id)
                    .execute(&mut *conn)
                    .await?;
            }
            for idx in indices {
                let stack = &info.units[idx];
                // Only stacks that survived with a changed count are saved here;
                // fully-wiped stacks were already DELETEd in attack_target.
                if stack.final_count != 0 && stack.initial_count != stack.final_count {
                    let killed = stack.initial_count - stack.final_count;
                    let stack_id = stack.row.id;
                    sqlx::query("UPDATE obtained_units SET count = count - ? WHERE id = ?")
                        .bind(killed)
                        .bind(stack_id)
                        .execute(&mut *conn)
                        .await?;
                    // `saveWithChange` succeeded → alteredUsers.add(user).
                    info.altered_users.insert(user_id);
                }
            }
        }

        // `alteredUsers.addAll(usersWithChangedCounts)`.
        let changed: Vec<i32> = info.users_with_changed_counts.iter().copied().collect();
        info.altered_users.extend(changed);

        // Persist any carrier detachment recorded by maybe_unset_holder_unit:
        // the surviving stacks whose carrier was destroyed (owner_unit_id -> NULL).
        let deleted: HashSet<u64> = info.deleted_unit_ids.iter().copied().collect();
        for &detached_id in &info.detached_unit_ids {
            if deleted.contains(&detached_id) {
                continue; // the stack itself was later wiped — nothing to update.
            }
            sqlx::query("UPDATE obtained_units SET owner_unit_id = NULL WHERE id = ?")
                .bind(detached_id)
                .execute(&mut *conn)
                .await?;
        }
        // TODO(M3/M4): unitTypeBo.emitUserChange + emitObtainedUnits for altered users.
        Ok(())
    }

    /// `AttackMissionProcessor.triggerUnitRequirementChange`, applied over every
    /// combat stack (`attackInformation.getUnits().stream().distinct()`): when a
    /// stack was fully wiped re-run `triggerUnitBuildCompletedOrKilled` (HAVE_UNIT
    /// + UNIT_AMOUNT); when it merely shrank re-run `triggerUnitAmountChanged`
    /// (UNIT_AMOUNT only). Runs after all persistence (Java fires it from the
    /// processor once combat resolution has saved survivor counts).
    async fn trigger_unit_requirement_changes(
        conn: &mut MySqlConnection,
        info: &AttackInformation,
        req_emits: &mut Vec<RequirementEmit>,
    ) -> OwgeResult<()> {
        for stack in &info.units {
            let user_id = stack.row.user_id;
            let unit_id = stack.row.unit_id as i64;
            if stack.final_count == 0 {
                let user = load_user_storage(conn, user_id).await?;
                crate::bo::requirement_bo::RequirementBo::trigger_unit_build_completed_or_killed(
                    conn, &user, unit_id, req_emits,
                )
                .await?;
            } else if stack.final_count != stack.initial_count {
                let user = load_user_storage(conn, user_id).await?;
                crate::bo::requirement_bo::RequirementBo::trigger_unit_amount_changed(
                    conn, &user, unit_id, req_emits,
                )
                .await?;
            }
        }
        Ok(())
    }

    // --- Attack-rule resolution (recursion up the unit-type parent chain) ---

    /// `AttackRuleBo.findAttackRule(UnitType)` — the nearest ancestor unit type
    /// with an `attack_rule_id`.
    async fn find_attack_rule(
        conn: &mut MySqlConnection,
        unit_type_id: Option<u16>,
    ) -> OwgeResult<Option<u16>> {
        let mut current = unit_type_id;
        while let Some(id) = current {
            let row: Option<(Option<u16>, Option<u16>)> =
                sqlx::query_as("SELECT attack_rule_id, parent_type FROM unit_types WHERE id = ?")
                    .bind(id)
                    .fetch_optional(&mut *conn)
                    .await?;
            match row {
                Some((Some(rule), _)) => return Ok(Some(rule)),
                Some((None, parent)) => current = parent,
                None => return Ok(None),
            }
        }
        Ok(None)
    }

    async fn load_attack_rule_entries(
        conn: &mut MySqlConnection,
        attack_rule_id: u16,
    ) -> OwgeResult<Vec<AttackRuleEntryRow>> {
        Ok(sqlx::query_as::<_, AttackRuleEntryRow>(
            "SELECT target, reference_id, can_attack \
             FROM attack_rule_entries WHERE attack_rule_id = ?",
        )
        .bind(attack_rule_id)
        .fetch_all(&mut *conn)
        .await?)
    }

    /// `AttackRuleBo.canAttack` — first matching entry (by UNIT id, then by
    /// UNIT_TYPE up the parent chain) decides; default `true` when none matches or
    /// there is no rule.
    async fn can_attack(
        conn: &mut MySqlConnection,
        entries: &[AttackRuleEntryRow],
        target: &AttackObtainedUnit,
    ) -> OwgeResult<bool> {
        for entry in entries {
            // isUnitMatchingEntry
            if entry.target == "UNIT" && target.row.unit_id == entry.reference_id {
                return Ok(entry.can_attack != 0);
            }
            // isUnitTypeMatchingEntry
            if entry.target == "UNIT_TYPE"
                && Self::unit_type_chain_contains(conn, target.row.unit_type_id, entry.reference_id)
                    .await?
            {
                return Ok(entry.can_attack != 0);
            }
        }
        Ok(true)
    }

    /// `findCriticalScore` — the critical multiplier the attacker applies to the
    /// target (its unit's own critical attack, else its type's, else `1.0`).
    async fn find_critical_score(
        conn: &mut MySqlConnection,
        attacker: &AttackObtainedUnit,
        target: &AttackObtainedUnit,
    ) -> OwgeResult<f32> {
        let critical_attack_id = match attacker.row.unit_critical_attack_id {
            Some(id) => Some(id),
            None => Self::find_used_critical_attack(conn, attacker.row.unit_type_id).await?,
        };
        let Some(critical_attack_id) = critical_attack_id else {
            return Ok(1.0);
        };
        let entries = sqlx::query_as::<_, CriticalEntryRow>(
            "SELECT target, reference_id, value \
             FROM critical_attack_entries WHERE critical_attack_id = ?",
        )
        .bind(critical_attack_id)
        .fetch_all(&mut *conn)
        .await?;
        for entry in entries {
            // findApplicableCriticalEntry
            if entry.target == "UNIT" && target.row.unit_id as u32 == entry.reference_id {
                return Ok(entry.value);
            }
            if entry.target == "UNIT_TYPE" {
                let matches = match target.row.unit_type_id {
                    Some(t) => {
                        Self::unit_type_chain_contains(conn, Some(t), entry.reference_id as u16)
                            .await?
                    }
                    None => false,
                };
                if matches {
                    return Ok(entry.value);
                }
            }
        }
        Ok(1.0)
    }

    /// `CriticalAttackBo.findUsedCriticalAttack` — nearest ancestor unit type with
    /// a `critical_attack_id`.
    async fn find_used_critical_attack(
        conn: &mut MySqlConnection,
        unit_type_id: Option<u16>,
    ) -> OwgeResult<Option<u16>> {
        let mut current = unit_type_id;
        while let Some(id) = current {
            let row: Option<(Option<u16>, Option<u16>)> = sqlx::query_as(
                "SELECT critical_attack_id, parent_type FROM unit_types WHERE id = ?",
            )
            .bind(id)
            .fetch_optional(&mut *conn)
            .await?;
            match row {
                Some((Some(crit), _)) => return Ok(Some(crit)),
                Some((None, parent)) => current = parent,
                None => return Ok(None),
            }
        }
        Ok(None)
    }

    /// Whether `reference_id` is in the unit type's parent chain (`findUnitTypeMatchingRule`).
    async fn unit_type_chain_contains(
        conn: &mut MySqlConnection,
        unit_type_id: Option<u16>,
        reference_id: u16,
    ) -> OwgeResult<bool> {
        let chain = Self::unit_type_chain(conn, unit_type_id).await?;
        Ok(chain.contains(&reference_id))
    }

    /// The unit-type id chain to sum improvements over, honouring
    /// `has_to_inherit_improvements` per level (Java `GroupedImprovement
    /// .findUnitTypeImprovement`): always include the unit's own type, then keep
    /// climbing to the parent only while the *current* level's
    /// `has_to_inherit_improvements` flag is TRUE — stopping at the first level that
    /// does not inherit. So a non-inheriting child does NOT pick up its parent's
    /// ATTACK/SHIELD/DEFENSE bonus, matching Java (fixes D0).
    async fn unit_type_improvement_chain(
        conn: &mut MySqlConnection,
        unit_type_id: Option<u16>,
    ) -> OwgeResult<Vec<u16>> {
        let mut chain = Vec::new();
        let mut current = unit_type_id;
        while let Some(id) = current {
            if chain.contains(&id) {
                break; // guard against a malformed cycle
            }
            chain.push(id);
            // `has_to_inherit_improvements` (i8 / tinyint) and `parent_type`.
            let row: Option<(i8, Option<u16>)> = sqlx::query_as(
                "SELECT has_to_inherit_improvements, parent_type FROM unit_types WHERE id = ?",
            )
            .bind(id)
            .fetch_optional(&mut *conn)
            .await?;
            match row {
                // Recurse to the parent ONLY when this level inherits.
                Some((inherit, parent)) if inherit != 0 => current = parent,
                _ => break,
            }
        }
        Ok(chain)
    }

    /// The full `[self, parent, grandparent, ...]` unit-type id chain, used both
    /// for rule matching and improvement inheritance.
    async fn unit_type_chain(
        conn: &mut MySqlConnection,
        unit_type_id: Option<u16>,
    ) -> OwgeResult<Vec<u16>> {
        let mut chain = Vec::new();
        let mut current = unit_type_id;
        while let Some(id) = current {
            if chain.contains(&id) {
                break; // guard against a malformed cycle
            }
            chain.push(id);
            let parent: Option<Option<u16>> =
                sqlx::query_scalar("SELECT parent_type FROM unit_types WHERE id = ?")
                    .bind(id)
                    .fetch_optional(&mut *conn)
                    .await?;
            current = parent.flatten();
        }
        Ok(chain)
    }

    // --- enrichment helpers ---

    /// `ImprovementBo.findUserImprovement` restricted to the per-unit-type
    /// improvements combat needs (ATTACK/SHIELD/DEFENSE), summed by
    /// `(type, unit_type_id)` from the user's three improvement sources (unlocked
    /// upgrades × level, active time specials, non-building obtained units). Runs
    /// on the caller's connection so it joins the locked transaction.
    async fn load_unit_type_improvements(
        conn: &mut MySqlConnection,
        user_id: i32,
    ) -> OwgeResult<UserImprovementDto> {
        let rows = sqlx::query_as::<_, UnitTypeImprovementRow>(USER_UNIT_TYPE_IMPROVEMENTS_SQL)
            .bind(user_id) // obtained_upgrades
            .bind(user_id) // active_time_specials
            .bind(user_id) // obtained_units
            .fetch_all(&mut *conn)
            .await?;
        let mut aggregate = UserImprovementDto::default();
        for row in rows {
            if let Some(improvement_type) = ImprovementType::from_code(&row.r#type) {
                aggregate.add_unit_type_improvement(
                    improvement_type,
                    row.unit_type_id,
                    row.value as i64 * row.multiplier,
                );
            }
        }
        Ok(aggregate)
    }

    /// Load the enriched combat row for a single obtained unit id.
    async fn load_combat_unit_row(
        conn: &mut MySqlConnection,
        obtained_unit_id: u64,
    ) -> OwgeResult<Option<CombatUnitRow>> {
        Ok(sqlx::query_as::<_, CombatUnitRow>(&format!(
            "{SELECT_COMBAT_UNIT_BASE} WHERE ou.id = ?"
        ))
        .bind(obtained_unit_id)
        .fetch_optional(&mut *conn)
        .await?)
    }

    /// `UnitMissionReportBuilder.withAttackInformation` shape — a list of per-user
    /// objects `{ userInfo, earnedPoints, units: [{ initialCount, finalCount,
    /// obtainedUnit }] }`. The frontend reads this verbatim, so the keys match the
    /// Java map exactly.
    fn to_attack_information_json(info: &AttackInformation) -> Value {
        let mut users_json = Vec::new();
        // Java builds `attackInformation` by iterating `AttackInformation.users`,
        // a HashMap<Integer, ...>; for the small user ids in play its iteration is
        // ascending by key. Combat itself uses insertion order (`user_order`) for
        // shuffle-input parity, but the persisted REPORT must mirror Java's
        // ascending-by-user-id array order, so sort a copy here (this does not
        // affect any RNG draw or combat outcome — report serialisation only).
        let mut ordered_users = info.user_order.clone();
        ordered_users.sort_unstable();
        for user_id in &ordered_users {
            let user = &info.users[user_id];
            let mut units_json = Vec::new();
            for &idx in &user.unit_indices {
                let stack = &info.units[idx];
                // `obtainedUnit.count` mirrors the ObtainedUnit ENTITY's count at
                // report-build time, which is NOT always the final count: Java
                // updates the entity via `saveWithChange` only for SURVIVING stacks
                // (final > 0); a fully-wiped stack (final == 0) is `delete`d, so its
                // entity count is never decremented and the report carries the
                // ORIGINAL (initial) count. Match that here.
                let report_count = if stack.final_count == 0 {
                    stack.initial_count
                } else {
                    stack.final_count
                };
                units_json.push(json!({
                    "initialCount": stack.initial_count,
                    "finalCount": stack.final_count,
                    "obtainedUnit": {
                        "id": stack.row.id,
                        "unit": stack.row.to_unit_dto(),
                        "count": report_count,
                        "userId": stack.row.user_id,
                        "username": stack.row.username,
                    },
                }));
            }
            users_json.push(json!({
                "userInfo": { "id": user.user_id, "username": user.username },
                "earnedPoints": user.earned_points,
                "units": units_json,
            }));
        }
        Value::Array(users_json)
    }
}

/// Fisher-Yates shuffle with a clock-seeded xorshift64 PRNG — a dependency-free
/// stand-in for `Collections.shuffle` (`java.util.Random`).
fn shuffle_indices(items: &mut [usize]) {
    let mut state = SystemTime::now()
        .duration_since(UNIX_EPOCH)
        .map(|d| d.as_nanos() as u64)
        .unwrap_or(0x9E37_79B9_7F4A_7C15)
        | 1;
    let mut next = || {
        state ^= state << 13;
        state ^= state >> 7;
        state ^= state << 17;
        state
    };
    let len = items.len();
    for i in (1..len).rev() {
        let j = (next() % (i as u64 + 1)) as usize;
        items.swap(i, j);
    }
}

/// `ImprovementBo.findAsRational` — percentage / 100.
fn as_rational(input_percentage: f64) -> f64 {
    input_percentage / 100.0
}

/// Advance an xorshift64 state and map it to a `[0, 1)` double — the
/// dependency-free stand-in for `Math.random()` used by the unit-capture rolls.
fn next_unit_f64(state: &mut u64) -> f64 {
    *state ^= *state << 13;
    *state ^= *state >> 7;
    *state ^= *state << 17;
    (*state >> 11) as f64 / (1u64 << 53) as f64
}

/// Load the full `UserStorage` on the caller's connection — needed to drive the
/// requirement-trigger engine (which reads `faction` / `home_planet`).
async fn load_user_storage(conn: &mut MySqlConnection, user_id: i32) -> OwgeResult<UserStorage> {
    sqlx::query_as::<_, UserStorage>(
        "SELECT id, username, email, alliance_id, faction, last_action, home_planet, \
                primary_resource, secondary_resource, energy, \
                primary_resource_generation_per_second, secondary_resource_generation_per_second, \
                has_skipped_tutorial, points, can_alter_twitch_state, banned \
         FROM user_storage WHERE id = ?",
    )
    .bind(user_id)
    .fetch_optional(&mut *conn)
    .await?
    .ok_or_else(|| OwgeError::NotFound(format!("No user_storage with id {user_id}")))
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

/// One `improvements_unit_types` row from a user's improvement sources, with the
/// source-level multiplier (`obtained_upgrades.level`, else `1`).
#[derive(sqlx::FromRow)]
struct UnitTypeImprovementRow {
    r#type: String,
    unit_type_id: u16,
    /// `improvements_unit_types.value` = `int`.
    value: i32,
    multiplier: i64,
}

#[derive(sqlx::FromRow)]
struct AttackRuleEntryRow {
    target: String,
    /// `attack_rule_entries.reference_id` = `smallint unsigned`.
    reference_id: u16,
    can_attack: i8,
}

#[derive(sqlx::FromRow)]
struct CriticalEntryRow {
    target: String,
    /// `critical_attack_entries.reference_id` = `int unsigned`.
    reference_id: u32,
    value: f32,
}

/// Base enriched-row projection shared by the per-id and the defenders queries.
const SELECT_COMBAT_UNIT_BASE: &str = "\
    SELECT ou.id AS id, ou.user_id AS user_id, ou.count AS count, \
           ou.mission_id AS mission_id, ou.owner_unit_id AS owner_unit_id, \
           u.id AS unit_id, u.type AS unit_type_id, u.attack AS unit_attack, \
           u.health AS unit_health, u.shield AS unit_shield, u.points AS unit_points, \
           u.attack_rule_id AS unit_attack_rule_id, \
           u.critical_attack_id AS unit_critical_attack_id, \
           u.bypass_shield AS unit_bypass_shield, \
           u.name AS unit_name, ut.name AS unit_type_name, u.description AS unit_description, \
           u.image_id AS unit_image, uimg.filename AS unit_image_filename, \
           u.charge AS unit_charge, u.time AS unit_time, u.primary_resource AS unit_primary_resource, \
           u.secondary_resource AS unit_secondary_resource, u.energy AS unit_energy, \
           u.speed AS unit_speed, u.is_unique AS unit_is_unique, \
           u.can_fast_explore AS unit_can_fast_explore, u.is_invisible AS unit_is_invisible, \
           u.stored_weight AS unit_stored_weight, u.storage_capacity AS unit_storage_capacity, \
           u.display_in_requirements AS unit_has_to_display_in_requirements, \
           u.cloned_improvements AS unit_cloned_improvements, \
           us.username AS username, us.alliance_id AS user_alliance_id \
    FROM obtained_units ou \
    JOIN units u ON u.id = ou.unit_id \
    LEFT JOIN unit_types ut ON ut.id = u.type \
    LEFT JOIN images_store uimg ON uimg.id = u.image_id \
    JOIN user_storage us ON us.id = ou.user_id ";

/// Defenders involved at the target planet (ObtainedUnitFinderBo.findInvolvedInAttack),
/// excluding the attack mission's own stacks. `?1/?2/?3` = target planet id;
/// `?4` = attack mission id.
///
/// **Ordering (Part 3, load-bearing for shuffle parity).** Java assembles defenders
/// as three *separate* queries concatenated in this exact sequence:
///   1. `findBySourcePlanetIdAndMissionIsNull` (planet-resident, no mission)
///   2. `findByTargetPlanetIdAndMissionTypeCode(DEPLOYED)`
///   3. `findByTargetPlanetIdWhereReferencePercentageTimePassed(CONQUEST >= 10%)`
/// — none of which carry an explicit `ORDER BY`, so each returns rows in `ou.id`
/// order. The previous single OR-query interleaved the three categories by raw scan
/// order, which differs from Java's category-grouped order and so produced a
/// different shuffle input list. We restore Java's order with an explicit category
/// rank (`cat`) plus `ou.id`. The categories are mutually exclusive (category 0
/// requires `mission_id IS NULL`; 1/2 require a typed mission), so the `CASE` is
/// unambiguous.
const SELECT_DEFENDERS_SQL: &str = "\
    SELECT ou.id AS id, ou.user_id AS user_id, ou.count AS count, \
           ou.mission_id AS mission_id, ou.owner_unit_id AS owner_unit_id, \
           u.id AS unit_id, u.type AS unit_type_id, u.attack AS unit_attack, \
           u.health AS unit_health, u.shield AS unit_shield, u.points AS unit_points, \
           u.attack_rule_id AS unit_attack_rule_id, \
           u.critical_attack_id AS unit_critical_attack_id, \
           u.bypass_shield AS unit_bypass_shield, \
           u.name AS unit_name, ut.name AS unit_type_name, u.description AS unit_description, \
           u.image_id AS unit_image, uimg.filename AS unit_image_filename, \
           u.charge AS unit_charge, u.time AS unit_time, u.primary_resource AS unit_primary_resource, \
           u.secondary_resource AS unit_secondary_resource, u.energy AS unit_energy, \
           u.speed AS unit_speed, u.is_unique AS unit_is_unique, \
           u.can_fast_explore AS unit_can_fast_explore, u.is_invisible AS unit_is_invisible, \
           u.stored_weight AS unit_stored_weight, u.storage_capacity AS unit_storage_capacity, \
           u.display_in_requirements AS unit_has_to_display_in_requirements, \
           u.cloned_improvements AS unit_cloned_improvements, \
           us.username AS username, us.alliance_id AS user_alliance_id \
    FROM obtained_units ou \
    JOIN units u ON u.id = ou.unit_id \
    LEFT JOIN unit_types ut ON ut.id = u.type \
    LEFT JOIN images_store uimg ON uimg.id = u.image_id \
    JOIN user_storage us ON us.id = ou.user_id \
    LEFT JOIN missions m ON m.id = ou.mission_id \
    LEFT JOIN mission_types mt ON mt.id = m.type \
    WHERE ( \
            (ou.mission_id IS NULL AND ou.source_planet = ?) \
            OR (ou.target_planet = ? AND mt.code = 'DEPLOYED') \
            OR (ou.target_planet = ? AND mt.code = 'CONQUEST' \
                AND m.required_time * 0.1 < TIME_TO_SEC(TIMEDIFF(UTC_TIMESTAMP(), m.starting_date))) \
          ) \
      AND (ou.mission_id IS NULL OR ou.mission_id <> ?) \
    ORDER BY CASE \
            WHEN ou.mission_id IS NULL THEN 0 \
            WHEN mt.code = 'DEPLOYED' THEN 1 \
            ELSE 2 END, ou.id";

/// Per-unit-type improvements for a user, summed by the three sources (matches
/// `UserImprovementBo`'s `UNIT_TYPE_IMPROVEMENTS_SQL`). `?1/?2/?3` = user id.
const USER_UNIT_TYPE_IMPROVEMENTS_SQL: &str = "\
    SELECT iut.type AS `type`, iut.unit_type_id, iut.value, ou.level AS multiplier \
      FROM obtained_upgrades ou \
      JOIN upgrades u ON u.id = ou.upgrade_id \
      JOIN improvements_unit_types iut ON iut.improvement_id = u.improvement_id \
     WHERE ou.user_id = ? \
    UNION ALL \
    SELECT iut.type AS `type`, iut.unit_type_id, iut.value, 1 AS multiplier \
      FROM active_time_specials ats \
      JOIN time_specials ts ON ts.id = ats.time_special_id \
      JOIN improvements_unit_types iut ON iut.improvement_id = ts.improvement_id \
     WHERE ats.user_id = ? AND ats.state = 'ACTIVE' \
    UNION ALL \
    SELECT iut.type AS `type`, iut.unit_type_id, iut.value, 1 AS multiplier \
      FROM obtained_units obu \
      JOIN units un ON un.id = obu.unit_id \
      JOIN improvements_unit_types iut ON iut.improvement_id = un.improvement_id \
      LEFT JOIN missions m ON m.id = obu.mission_id \
      LEFT JOIN mission_types mt ON mt.id = m.type \
     WHERE obu.user_id = ? AND (mt.code IS NULL OR mt.code <> 'BUILD_UNIT')";
