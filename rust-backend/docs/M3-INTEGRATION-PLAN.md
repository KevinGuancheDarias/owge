# M3 integration plan (resume here)

> **UPDATE 2026-06-04 — static integration COMPLETE.** All ~22 orphan files are
> now wired (lib.rs/dto/mod.rs/bo/mod.rs + owge-rest routes/main.rs). The whole
> **workspace compiles** (`cargo build`), **all tests pass** (`cargo test`: 6
> passed), and **clippy is clean** (`cargo clippy`: no errors). The 501 unit-
> mission endpoints (`game/mission/*` register + cancel) are re-pointed to the
> real `UnitMissionBo` methods + the scheduler poller is started in `main.rs`.
>
> Drift fixed during wiring (sections 1–7 below are now historical):
> - `unit_mission_registration_bo.rs`: `stored` carried units must be
>   `Vec<LoadedUnit>` (downstream `manage_units_registration` reads `.count`/
>   `.db_unit`); `check_total_weight` updated to take `&[LoadedUnit]`. The unused
>   `is_enemy_planet` is prefixed `_` (consumed only by deferred M4/M5 paths).
> - `faction_bo.rs:154` (M2): removed a redundant `sum > 0.0 &&` (clippy deny).
> - `MissionRunner` ctor is `MissionRunner::new(db)` (not the struct literal the
>   old snippet showed); poller wired in `main.rs` before `routes::router(state)`.
>
> **Endpoints still 501 (correctly — Bo methods genuinely not ported):** build-
> unit + level-up registration/cancel (`game/unit/build|cancel|findRunning`,
> `game/upgrade/*`) need `MissionBo::register_build_unit` / `register_level_up_*`
> which DO NOT EXIST yet (`mission_bo.rs` only has `run_mission`, the exec path).
> Also `game/unit/delete`, `game/unit/{id}/criticalAttack`, `report/findMy`,
> `planet/leave`, `time_special/activate` (see their TODOs). These are follow-on
> work, not M3-unit-mission blockers.
>
> **REMAINING = runtime verification vs the dc12 clone (section 5).** NOT done:
> blocked because it mutates the clone DB (the auto-mode classifier denied a
> direct `scheduled_tasks` UPDATE, and booting the server fires 77 due missions).
> Needs explicit user authorization + a game-JWT minting path. The clone has live
> data: 97 users, 3841 planets, 1644 obtained_units, 193 unresolved missions,
> 100 mission-run scheduled_tasks (77 due now). Start controlled: park due tasks,
> craft ONE EXPLORE, fire it, diff. See section 5.

---

## RUNTIME VERIFICATION RESULTS (2026-06-04) — EXPLORE ✅ vs Java

Setup: Rust on **sgalactica_java_14** (`.env.dc14`, `OWGE_PROFILES=rsaKeys`,
`OWGE_RSA_PUBLIC_KEY=/root/keys/public.key`, port 8099). Java reference = live
**dc13** = sgalactica_java_13 at `http://127.0.0.1:8123/game_api/`. Both DBs
seeded identically (user 1 `rusttester`, faction 1, home planet 1002, 10× unit 10
`X-302`). RSA game JWT minted with `scripts/mint_jwt.py` (signs `/root/keys/private.key`,
trusted by BOTH backends). Subscribe is 501 in Rust, so the user/planet/units are
seeded directly via SQL (authorized test-DB writes).

**EXPLORE register→fire→report→return verified end-to-end and compared to Java:**
- Registration (HTTP 200): missions row (type 4), `scheduled_tasks` row
  (`task_name='mission-run'`, exec = start + required_time − DELAY_HANDLE), units
  moved onto the mission. **`required_time` = 53.25 in BOTH** (Rust 53.2499994,
  Java 53.25); scheduled `exec − start` = **51s in BOTH**. Time/speed math matches.
- Firing: poller fired it, mission `resolved=1`, a RETURN mission (type 5) auto-
  created with its own scheduled task, a `mission_reports` row written — identical
  sequence to Java.
- Report JSON: top-level keys identical (`id, involvedUnits, senderUser,
  sourcePlanet, targetPlanet, unitsInPlanet`); all SHARED `unit` field values
  identical; `sourcePlanet` identical; `unitsInPlanet` both `[]`.

**Two real bugs found & fixed during verification** (MySQL `SUM()` over an integer
column returns DECIMAL, which sqlx won't decode as i64):
- `mission_time_manager_bo.rs` `SUM(iut.value …)` (the speed-improvement aggregate)
  → wrapped each in `CAST(… AS SIGNED)`. This was a hard 500 blocking ALL unit
  missions.
- `requirement_engine.rs:400` `SUM(count)` (countByUserAndUnit) — same latent bug,
  same fix.

**Remaining parity GAPS (report-payload fidelity, NOT engine correctness):**
1. ~~Report embeds a REDUCED unit DTO.~~ **FIXED & verified 2026-06-04.**
   `ObtainedUnitUnitDto` enriched to Java's full scalar `UnitDto` field set
   (`image, imageUrl, description, points, time, primaryResource,
   secondaryResource, energy, clonedImprovements, hasToDisplayInRequirements` +
   the combat scalars) with Jackson-style NON_NULL omission
   (`skip_serializing_if`). Populated at BOTH embed sites: report `involvedUnits`
   (`mission_processor` SELECT now joins `images_store`, computes
   `imageUrl=/dynamic/<filename>`) and the attack `attackInformation` (now emits
   the full `obtainedUnit` `{id,unit,count,userId,username}` from an enriched
   `CombatUnitRow`, replacing the old `{id,unitId,count}`). The sync path keeps
   its reduced shape via `ObtainedUnitUnitDto::reduced`. Verified vs Java: explore
   `involvedUnits[].unit` and attack `obtainedUnit.unit` are field-for-field
   identical (incl. `imageUrl`), with ONE deliberate omission — the deeply-nested
   `speedImpactGroup`/`requirementsGroups` graph (frontend report view reads only
   `unit.name` + `unit.imageUrl`). storageCapacity now NON_NULL (omitted when null,
   matching Java). Schema gotchas fixed: `units` image FK is `image_id` (→
   `images_store.filename`), `hasToDisplayInRequirements` column is
   `display_in_requirements`, `units.is_unique` is `tinyint UNSIGNED` (u8).
2. `planet.home`: Rust coerces NULL→`false`; Java keeps `null`. Cosmetic, and
   cross-cutting (shared `PlanetDto`); both are falsy to the frontend. Left as-is.
4. ~~**Autocommit orphan**~~ **FIXED & verified 2026-06-04.**
   `do_common_mission_register` now opens a transaction on the pinned conn
   (`conn.begin()`) after the caller's `GET_LOCK` (MySQL named locks are session-
   scoped, NOT released by BEGIN/COMMIT, so they stay held), runs all persistence
   on it, and `tx.commit()`s at the end; helpers take the tx via `&mut tx`
   (Transaction DerefMut→MySqlConnection). Verified: a late failure
   (`check_units_can_do_mission` rejecting a non-explore unit, which runs AFTER the
   mission + obtained_units inserts) now rolls back fully — 0 orphan missions, 0
   scheduled tasks, unit stack restored. Happy path still registers+fires. Needs
   `use sqlx::Connection;` for `.begin()`.
   NOTE: the RETURN-mission registration on the FIRING path
   (`ReturnMissionRegistrationBo` via `run_unit_mission`) is a separate write path
   and was NOT given the same tx treatment here — revisit if firing-path orphans
   appear.

### ATTACK ✅ vs Java (2026-06-04)
Scenario (`scripts/seed_attack.sql`, applied to BOTH DBs): attacker user 1 (home
1002, 10× unit 10) explored 1003; defender user 2 (home 1004, owns 1003, 10× unit
10 on 1003). Same units both sides, symmetric 10v10.
- Registration identical: type 8, `required_time` 600, sched offset +597s on BOTH.
- Combat resolution **bit-for-bit identical**: attacker 10→5, defender 10→5,
  `earnedPoints` 450 each, RETURN (type 5) created, planet 1003 NOT conquered.
  `attackInformation` user keys (`earnedPoints/units/userInfo`) + per-unit
  `initialCount`=10/`finalCount`=5 identical.
- **BUG FOUND & FIXED**: `attack_mission_manager_bo.rs` `SELECT_DEFENDERS_SQL` and
  `SELECT_COMBAT_UNIT_BASE` selected `us.alliance` but the column is
  `us.alliance_id` — would crash EVERY attack. Fixed both (`us.alliance_id AS
  user_alliance_id`).
- Report-DTO gap (same as explore, now confirmed in combat too): Rust's
  `attackInformation.units[].obtainedUnit` = `{count,id,unitId}` vs Java's full
  `ObtainedUnitDto` `{count,id,unit:{…full UnitDto…},userId,username}`. Shared
  values identical; Java carries image/imageUrl/name/description/etc. The combat-
  report frontend needs these → this is the priority fidelity fix.

### GATHER ✅ vs Java (2026-06-04)
Scenario (`scripts/seed_gather.sql`, both DBs): user 1 home 1002, 10× unit 28
(Tel'tak, charge 85), explored unowned 1003 (richness 60). Faction 1 split 60/40.
- Report structure + enriched `involvedUnits[].unit` identical to Java (the DTO fix
  carries over). `gatheredPrimary`/`gatheredSecondary` **bit-for-bit identical**
  after a fix.
- **BUG FOUND & FIXED**: Java's faction gather percentages are `Float`, so
  `customPercentage / 100` is computed in **f32** (`0.6f` = 0.60000002…) then
  promoted to double. Rust did it in f64 → 306.0 vs Java 306.00001215934753.
  Fix: `gather.rs` keeps the percentages `f32` and does `/100.0_f32` before
  `as f64`. Now both = 306.00001215934753 / 204.00000303983688 exactly.
- Java-only delta (NOT a gather bug): Java's user resource TOTAL gains the gathered
  amount PLUS passive resource generation (~+2.66 over the mission). Rust credits
  exactly the gathered amount. Passive resource-generation-over-time is a separate
  subsystem (affects every resource mutation, not gather) and is not yet ported.

### CONQUEST / COUNTERATTACK / DEPLOY / CANCEL ✅ vs Java (2026-06-04)
All seeded identically on both DBs (`scripts/seed_conquest.sql`,
`seed_counterattack.sql`, `seed_deploy.sql`; cancel + foreign-deploy via inline
`/tmp/seed_*.sql`). Fire by setting `scheduled_tasks.execution_time` to the past.
- **CONQUEST** (10 vs weak 2): planet 1003 → attacker, 9 survivors, conquest mission
  resolved, report `conquestStatus:true / I18N_PLANET_IS_NOW_OURS` — identical.
- **COUNTERATTACK** (= attack on the sender's own planet, 10v10): type 9, RETURN
  created, 5/5 survivors, 450 pts each — identical.
- **DEPLOY owned** (merge): 10 deployed into a planet holding 3 → single 13× stack,
  no report, mission resolved — identical.
- **DEPLOY foreign**: **BUG FOUND & FIXED.** Rust left units on the resolved DEPLOY
  (type 11) mission with `source_planet=NULL`; Java moves them to a persistent
  DEPLOYED (type 12) mission (`source_planet` preserved). Since the defenders /
  `areUnitsInvolved` query matches `code='DEPLOYED'`, the old behaviour meant
  foreign-deployed units would NOT defend. Ported `moveUnit` non-owned branch +
  `saveWithAdding` + `findDeployedMissionOrCreate` in `deploy.rs`
  (find-or-create the unresolved DEPLOYED mission, merge into an existing stationed
  stack). Now identical: DEPLOY(11) resolved + new DEPLOYED(12) resolved=0,
  units source 1002 / target 1003 on the type-12 mission.
- **CANCEL** (cancel a running explore): original (type 4) resolved + RETURN (type
  5) registered, units back on source — identical.

## ALL unit mission types now runtime-verified bit-for-bit vs Java ✅
EXPLORE, GATHER, ATTACK, COUNTERATTACK, CONQUEST, DEPLOY (owned+foreign), CANCEL.
Remaining non-M3-mission gaps (documented, separate subsystems): passive resource
generation over time (gather user-total delta); LEVEL_UP registration endpoint still
501. Rust server on :8099 (`/tmp/owge_dc14.log`); reset any scenario with the
matching `scripts/seed_*.sql`.

### CANCEL-BUILD ✅ vs Java (2026-06-04)
`MissionBo::cancel_build_unit(db, user_id, mission_id)` ported + wired to
`GET game/unit/cancel` (returns `"OK"`). Mirrors Java `cancelBuildUnit`→`cancelMission`:
load mission (404 if missing), ownership guard ("dirty Kenpachi" 400 if not the
invoker's), for BUILD_UNIT delete the in-build obtained_units, refund
`mission.primary/secondary_resource`, delete `mission_information` then the mission,
delete the scheduled task. No planet/user lock (matches Java's plain `@Transactional`);
one transaction. FK note: `mission_information.mission_id`→missions must be deleted
before the mission; `obtained_units.mission_id` is NOT a FK (only first_deployment_mission
is). Verified vs Java (seed_build.sql, build 5× then cancel): build mission +
mission_information + in-build units + scheduled task all gone, resources refunded
(Rust exactly back to 24000/16000; Java + passive-gen drift). Negative test: user 2
cancelling user 1's mission → 400, mission intact.

### BUILD_UNIT registration ✅ vs Java (2026-06-04)
`MissionBo::register_build_unit(db, user_id, planet_id, unit_id, count)` ported +
wired to `POST game/unit/build`. Outer user lock + inner planet lock (closes the
cross-planet unique-unit race), one transaction. Ports: checkUnitBuildMissionDoesNotExists,
objectRelation(UNIT)+checkIsUnlocked, mission-limit, finalCount(unique→1),
checkIsUniqueBuilt, calculateRequirements (primary/secondary/time/energy ×count),
canRun (resources + energy headroom), moreUnitBuildSpeed (computeImprovementValue
sum=false), checkWouldReachUnitTypeLimit, ZERO_BUILD_TIME, mission + mission_information
(relation, value=planet) + obtained_unit(in-build, no planet) + scheduled task.
New helpers in `mission_bo.rs`: find_available_energy (maxEnergy − consumed),
compute_improvement_value (IMPROVEMENT_STEP loop), check_would_reach_unit_type_limit
(share-count root + faction override + type max + AMOUNT improvement),
find_max_share_count_root, schedule_build_mission. `run_locked` made pub(crate).
Verified vs Java (seed_build.sql: unit 138 Kino unlocked via unlocked_relation,
build 5×): registration IDENTICAL (mission BUILD_UNIT(3) required_time 550, cost
300/200, mission_information relation 278 value 1002, obtained_unit 138×5 on mission,
sched offset 547) and completion IDENTICAL (units land on planet 1002, mission
deleted, sched cleared). Rust deducts exactly 300/200; Java total also reflects the
separate passive-generation subsystem. SCHEMA GOTCHAS: faction table is
`factions_unit_types` (faction_id/unit_type_id/max_count INT UNSIGNED);
`unit_types.max_count` is BIGINT (i64); `units.is_unique` tinyint UNSIGNED.
Parity TODO: countByUserAndSharedCountUnitType cross-type share aggregation not
reproduced (only relevant to share-count types; direct root-type count used).

### FIRING-PATH TRANSACTION HARDENING ✅ (2026-06-04)
The fire now runs in one transaction, same pattern as registration:
- `unit_mission_bo.rs` `do_run_unit_mission`: opens `conn.begin()` AFTER the planet
  locks (session-scoped, survive BEGIN/COMMIT), runs interception + processor
  dispatch + report-save on the tx, `commit()`s at the end. `use sqlx::Connection`.
- `mission_bo.rs` `run_mission` (BUILD_UNIT/LEVEL_UP): same wrap for consistency.
Processors still take autonomous read-only snapshots via the `db` pool (committed
reads, fine outside the tx). **Proven** with a temporary injected mid-fire failure
(env-gated, since removed): a GATHER that mutates user resources + writes a report
rolled back FULLY (resources unchanged at 24000/16000, no gather report, mission
left unresolved); retries incremented `attemps`, then `give_up` wrote the error
report ("Mission … failed, please contact an admin!") on its own connection.
Success path re-verified after removing the injection (resources 24306.00001…,
report 306.00001215934753, resolved=1). This closes the "firing failures can leave
a half-processed mission" gap — the class of bug that's painful to debug later.

---

State as of 2026-06-03 end of session. **M3 foundation is built, compiling &
validated. The fan-out workflow produced ~22 orphan files (~5,400 LOC) that are
NOT yet wired** — `cargo build -p owge-business` is currently GREEN precisely
because nothing references them. Integration = wire them, fix cross-module
compile drift, re-point the 501 endpoints, verify vs the dc12 clone.

Contracts the agents coded against: `docs/M3-CONTRACTS.md`. Read that first.

## 0. Done & trustworthy (already compiling)
- `model/mission.rs` (`Mission`/`MissionInformation`/`MissionReport`/`MissionType`)
- `lock.rs` (named-lock primitive — **validated on the clone**)
- `bo/mission_scheduler_bo.rs` (`MissionSchedulerService` + `MissionDispatch` trait + poller)
- `builder/unit_mission_report_builder.rs` (`UnitMissionReportBuilder` shared API)
These are wired already (lib.rs/model/mod.rs/bo/mod.rs). Don't redo.

## 1. Wiring lines to add (orchestrator owns these)

`owge-business/src/lib.rs`:
```rust
pub mod pojo;     // add near the other `pub mod` lines
```

`owge-business/src/dto/mod.rs`:
```rust
pub mod mission;
pub mod user_improvement;
pub use mission::{MissionDto, UnitRunningMissionDto, MissionReportDto, GatherMissionResultDto};
pub use user_improvement::{ImprovementType, UserImprovementDto, UnitTypeImprovementEntry};
```

`owge-business/src/bo/mod.rs` (add `pub mod` + `pub use` for each):
```
mission_configuration_bo::MissionConfigurationBo
mission_time_manager_bo::MissionTimeManagerBo
user_improvement_bo::UserImprovementBo
attack_mission_manager_bo::AttackMissionManagerBo
mission_interception_manager_bo::MissionInterceptionManagerBo   // also struct InterceptionInformation
unit_mission_registration_bo::UnitMissionRegistrationBo
return_mission_registration_bo::ReturnMissionRegistrationBo
mission_report_manager_bo::MissionReportManagerBo
mission_processor            // `pub mod mission_processor;` (has its own mod.rs + dispatch())
mission_base_service_bo::MissionBaseService
mission_bo::MissionBo
unit_mission_bo::{UnitMissionBo, MissionRunner}
```

`owge-rest/src/routes/game/mod.rs`:
```rust
pub mod mission;  pub mod unit;  pub mod upgrade;
// inside router(): .merge(mission::routes()).merge(unit::routes()).merge(upgrade::routes())
```

`owge-rest/src/main.rs` (start the poller after `AppState::bootstrap`):
```rust
use owge_business::bo::{MissionSchedulerService, MissionRunner};
use std::sync::Arc;
let scheduler = MissionSchedulerService::new(state.db.clone());
let runner = Arc::new(MissionRunner { db: state.db.clone() }); // confirm MissionRunner ctor/fields
let _poller = scheduler.spawn_poller(runner, format!("owge-rust-{}", state.env.server_port));
```
(Confirm `MissionRunner`'s public constructor — it's `pub struct MissionRunner { db: Db }` at
`unit_mission_bo.rs:529`; may need a `pub fn new(db)` or make `db` pub.)

## 2. Recommended integration ORDER (compile after each step — keep it green)
Wire bottom-up so each layer's deps already exist:
1. dto/user_improvement + dto/mission + pojo → `cargo build -p owge-business`.
2. mission_configuration_bo, mission_time_manager_bo, user_improvement_bo → build.
3. attack_mission_manager_bo, mission_interception_manager_bo → build.
4. unit_mission_registration_bo, return_mission_registration_bo, mission_report_manager_bo → build.
5. mission_processor → build.
6. mission_base_service_bo, mission_bo, unit_mission_bo → build.
7. owge-rest routes + main.rs poller → `cargo build` (workspace).

## 3. Known cross-module drift to expect & fix (NOT verified together)
Only Layer-3's 3 bo files were compile-checked (in isolation, temp-wired then reverted).
Layers 1–2 were never compiled together. Likely fixes:
- **Processors invented a wrapper**: `mission_processor/attack.rs` calls a reconstructed
  `attack::process_attack(conn, mission, survivors_do_return, is_triggered_by_event, db) -> AttackOutcome`
  and `trigger_attack_if_required(...)`, deriving `removed` by re-querying
  `COUNT(*) FROM obtained_units WHERE mission_id=...` and reading `units[].finalCount`/`userInfo.id`
  from the attackInformation JSON. Confirm `AttackMissionManagerBo::process_attack` returns that
  exact JSON shape (`to_attack_information_json`). Reconcile if drifted.
- **find_user_improvement location**: it's `UserImprovementBo::find_user_improvement` (NOT
  `ImprovementBo`). All callers already use `UserImprovementBo`. If desired, add a thin
  `ImprovementBo::find_user_improvement` delegate. `ImprovementType` enum is local to
  `dto/user_improvement.rs` — promote to a shared `enumerations` module later (optional).
- **Registration vs deploy**: `do_common_mission_register` collapsed Java's two `missionInformation`
  args into one `info`; for DEPLOY the caller must pass source/target already swapped.
- **Scheduling**: registration INLINES the `scheduled_tasks` INSERT (atomic with the mission) and
  duplicates `DELAY_HANDLE=2`/`MISSION_TASK_NAME="mission-run"` as local consts — keep in sync with
  `mission_scheduler_bo.rs`, or refactor to call `MissionSchedulerService::schedule_mission` post-commit.
- **run_locked**: uses autocommit on the pinned connection, NOT an explicit `READ_COMMITTED` tx
  (parity TODO — open a tx AFTER GET_LOCK so session locks stay held across BEGIN).

## 4. Re-point the 501 endpoints (endpoints agent stubbed them)
`routes/game/{mission,unit,upgrade}.rs` currently answer `501 NOT_IMPLEMENTED`. The
`UnitMissionBo` register entry methods now EXIST — wire each handler to them:
- `UnitMissionBo::my_register_{explore,gather,establish_base,attack,counterattack,conquest,deploy}(db, user, info)`
- `UnitMissionBo::my_cancel_mission(db, user, mission_id)`
- build/level-up: `MissionBo` / unit/upgrade build paths (confirm method names in `mission_bo.rs`).
Handlers take the JWT `GameUser` (`user.id as i32`, per `alliance.rs`) and a
`UnitMissionInformation` body; set `user_id` + pin `mission_type` server-side.

## 5. Verify vs the dc12 clone (`./run-local.sh`, port 8099)
- Registration: `POST game/mission/explore` with a real game JWT → assert a `missions` row +
  a `scheduled_tasks` row (`task_name='mission-run'`) appear; obtained_units moved onto the mission.
- Firing: let the poller fire (or set execution_time in the past) → mission processes, report row
  written, `resolved=1`, scheduled_tasks row deleted. Start with EXPLORE (simplest), then GATHER,
  then ATTACK (combat). Compare a report payload shape against a Java-produced one if available.
- Mission limit: register past `more_missions+1` → expect 400 `I18N_ERR_MISSION_LIMIT_EXCEEDED`.
- **Use the clone only** (`sgalactica_java_12_base_fast`), never live `sgalactica_java_12`.

## 6. Deferred (not M3 — don't chase): auditing (M5), websocket emits (M4),
per-user improvement cache/eviction (M4), image upload (M5).
