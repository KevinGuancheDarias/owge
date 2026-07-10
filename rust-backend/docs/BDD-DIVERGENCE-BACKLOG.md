# BDD parity — divergence backlog

## SWEEP 3 (post D16/D17/D10/D19, 2026-07-10 17:35, artifacts `/tmp/bdd_parity_runs/20260710_173518`)

37 scenarios: JAVA_SPEC 37 ✅ · RUST_SPEC 37 ✅ · **PARITY 29 ✅ / 8 🔴**
(sweep 2 was 18✅/19🔴 — 11 scenarios flipped today; explore/gather 5/5,
upgrades 8/8, time_specials 3/3, deploy family stays 7/7 green).

The 8 remaining reds (4–16 diff lines each) decompose into FIVE classes:
- **R1 specialLocation rich-vs-slim** (the old D3 leftover, now reproduced):
  Rust emits the RICH specialLocation (galaxyId/galaxyName/image/…) inside
  `mission_report_new.parsedJson.targetPlanet`, `unit_mission_change`/
  `enemy_mission_change` targetPlanet and the mission_reports TABLE json_body;
  Java emits the slim shape there. Opposite direction on conquest
  `unit_obtained_change.value[].sourcePlanet.specialLocation` (JAVA-only).
  Affects all 4 special_location scenarios + establish-changed-owner.
- **R2 requirementsGroups path-pair (D2 remainder)**: `unit_unlocked_change`
  unit.improvement…speedImpactGroup.requirementsGroups is JAVA-only (Java DEEP
  here — establish/compound/leave), while `time_special_unlocked_change`
  …unitType.{parent,shareMaxCount,speedImpactGroup}.requirementsGroups is
  RUST-only (Rust over-emits). Same nested graph, opposite hydration per event.
- **R3 missing Rust emits on ownership-change paths**: FRAMECOUNT java>rust —
  `enemy_mission_change` 1v0, `planet_owned_change` 1v0, `unit_obtained_change`
  2v1 (establish-base grant, compound probe, cancel-auto-return). Part of the
  definePlanetAsOwnedBy emit block not fully ported.
- **R4 unit_build shapes**: `unit_build_mission_change.value[].unit.
  {improvement,speedImpactGroup}` JAVA-only; plus `unit_type_change` java 1
  rust 2 on registration — likely the dedup removal now DOUBLE-emitting on the
  build path (Rust queues via requirement trigger AND the M4 block; Java once).
- **R5 conquest unit_mission_change count j=1 r=0** (+ empty myUnitMissions):
  at emit time Java still counts the resolving conquest mission, Rust doesn't —
  emit-ordering/timing relative to mission resolution.

storedUnits on enemy_mission_change (RUST-only, establish-changed-owner +
conquest ws_user2) rides with R1/R2's finder — same `involved_units` builder.

## SWEEP 2 (post-fix wave, 2026-07-09 22:02, artifacts `/tmp/bdd_parity_runs/20260709_220239`)

37 scenarios: JAVA_SPEC 35 ✅ · RUST_SPEC 36 ✅ · PARITY 18 ✅ / 19 🔴.
vs sweep 1 (36 scenarios: JAVA 36 · RUST 28 · PARITY 18✅/18🔴):
- ALL 8 original RUST_SPEC reds fixed (D5 ×7, D6/D7 via the fix wave).
- Deploy family: 4 scenarios flipped to FULL three-verdict parity (D8 + date
  format + canonicalizer).
- Remaining 19 PARITY reds are concentrated in TWO classes: the D2/D3/D11
  report/unlock payload shapes (special_location ×4, reports/gather/explore
  ×4, upgrades completion, unit_build ×2, time_specials payloads) and D9
  (upgrade-register table rows) + D10 (Quartz structural, time_specials).
- TWO NEW findings from the fix wave itself (BOTH RESOLVED 2026-07-09 night):
  - **D13 — ✅ FIXED**: `time_specials :: not-unlocked rejection` — Java 400
    `SgtBackendTargetNotUnlocked` "The target object relation has not been
    unlocked" (after the D5 re-parenting) vs Rust 500. Fixed:
    `active_time_special_bo.rs` activate now returns `InvalidInput` (400, same
    message) and the scenario asserts 400 + message. Verified FULL
    three-verdict parity (`/tmp/bdd_parity_runs`, time_specials run
    2026-07-09 ~22:4x).
  - **D14 — ✅ ROOT-CAUSED, NOT A FLAKE, FIXED**: `explore` red on BOTH
    backends because the run **baseline itself was dirty**: a run killed
    during the 19:1x deadlock window (pre-`7aa8bcb7` fix) leaked
    `explored_planets (user 1, planet 1234)` — the Gather/travel Givens'
    row — and since the runner dumps the CURRENT db as each run's baseline,
    every later baseline (incl. sweep 2) carried it. Both backends then
    correctly skipped `defineAsExplored` (already explored) → no
    `planet_explored_event`, while the scenario's `explored_planets` table
    assertion still passed against the leaked row. Fixed by deleting the row
    from the live db and adding `Given user 1 has not explored planet 1234`
    to the explore scenario so the precondition is forced. Verified: explore
    JAVA ✅ RUST ✅. **LESSON: baseline = current-db snapshot; any crashed
    run can poison ALL later runs — scenarios must force their
    preconditions with explicit Givens, and a dirty-baseline audit
    (diff baseline.sql vs known-good) belongs in any both-backends-red
    investigation.**

Every RUST_SPEC / PARITY red from `bdd_parity` runs gets an entry here with
the artifact path that proves it. Java is the default spec (plan §9.11) —
entries marked JAVA-SUSPECT need Kevin's ruling on which side is right.

## From the Phase-1 run (special_location_unlock, 2026-07-09, artifacts `/tmp/bdd_parity_runs/20260709_172100`)

### D1 — ✅ FIXED 2026-07-09 — Rust never granted HAVE_SPECIAL_LOCATION unlocks to a new planet owner ⭐ the Phase-1 target
Fixed in `define_planet_as_owned_by` (new-owner trigger + emits param); verified
green by the harness, `unlocked_relation` now byte-matches (17 rows both).
Player data repair still pending (BUG doc "Consequences").
- Scenarios: "Establish base grants…" and "Conquest transfers…" — JAVA ✅ RUST 🔴 PARITY 🔴.
- `unlocked_relation`: Java grants the gated UNIT/TIME_SPECIAL relations to the
  new owner, Rust doesn't (table diff row count 17 vs 15); the
  `unit_unlocked_change` / `time_special_unlocked_change` events are
  correspondingly missing on Rust.
- Known, documented: `BUG-SPECIAL-LOCATION-UNLOCK.md` — fix per its "Proposed
  fix" section (task #8). Revoke on leave works (third scenario RUST_SPEC ✅).

### D2 — Rust omits `requirementsGroups` inside nested `speedImpactGroup` DTOs
- Seen in BOTH the `unit_unlocked_change` ws payload (leave scenario, PARITY 🔴)
  and `mission_reports.json_body → involvedUnits[].unit.speedImpactGroup`
  (establish-base scenario). Java serializes the full requirements groups of a
  speed impact group wherever it's embedded; Rust drops the field.
- Same root cause suspected in both paths (shared DTO serialization).
- **REPORT PATH FIXED 2026-07-09 night**: it is PATH-DEPENDENT, not universal —
  Java's ws `unit_mission_change` frames have speedImpactGroup WITHOUT
  requirementsGroups; only the report json (raw entity graph serialized by the
  builder's own mapper) carries them. Rust now enriches report-bound DTOs only
  (`mission_processor::enrich_unit_for_report` on the `create_report_base` +
  `explore_planet_units` paths; first attempt inside `involved_units_to_dtos`
  leaked into the ws finder and was moved out). `unit_unlocked_change` ws path
  still open — verify which side Java actually emits there before "fixing".

### D3 — mission report `json_body` field-shape gaps (establish-base report)
- ✅ `involvedUnits[0].storedUnits`: RUST-only — FIXED: Java's report path maps
  the entity WITHOUT the UnitDataLoader chain so storedUnits stays null and
  NON_NULL drops it (the loader-backed unit_obtained_change path emits `[]` —
  keep that one). Rust `strip_units` now removes it.
- ✅ `involvedUnits[0].unit.improvement`: RUST-only — FIXED: Java
  `obtainedUnitToDto` explicitly nulls it; `strip_units` now removes it.
- ✅ `senderUser.canAlterTwitchState`: JAVA-only — FIXED: Java's fresh
  UserStorageDto has the `= false` field initializer (non-null survives
  NON_NULL); Rust `with_sender_user` now emits it.
- ⏳ `targetPlanet.specialLocation.{galaxyId,galaxyName,image,imageUrl,improvement}`:
  RUST-only — Rust embeds the rich specialLocation DTO in reports where Java
  writes a slimmer shape. (Conquest reports diverge the same way, plus
  attackInformation subtree — see the conquest table.diff.) STILL OPEN — not
  exercised by explore/gather (planet 1234 has no SL); fix when re-running the
  special_location / establish-base scenarios.
- All three ✅ verified: explore + gather `mission_reports` TABLE now ✅ match
  (run 20260709_224450).

### D4 — requirements ordering inside `requirementsGroups[].requirements[]`
- `planet_owned_change` (leave scenario): identical items, different order
  (ids 3936/3938 swapped). Frames are diffed as sorted multisets of whole
  frames, so intra-payload array order still bites.
- Candidate for a justified normalization (compare requirements as sets) —
  needs Kevin's call: is requirement order contractual for the frontend?

## From the FULL SWEEP (36 scenarios, 2026-07-09, artifacts `/tmp/bdd_parity_runs/20260709_182651`)

Scoreboard: JAVA_SPEC 36/36 ✅ · RUST_SPEC 28/36 (8 🔴) · PARITY 18/36 (18 🔴).

### D5 — ✅ RESOLVED 2026-07-09 (Kevin's ruling: Rust behavior is the contract; Java fixed) ⭐
TWO Java root causes, both fixed and verified green on all 7 scenarios:
1. `SgtMissionRegistrationException` / `SgtLevelUpMissionAlreadyRunningException` /
   `SgtBackendTargetNotUnlocked` extended `CommonException`, which
   `SgtGameRestExceptionHandler` doesn't map → generic 500. Re-parented to
   `SgtBackendInvalidInputException` → 400 + real message.
2. Prose `NotFoundException` messages ("nice try, dirty hacker!", "The mission
   was not found…") CRASHED the handler itself — `handleGameException`'s
   doc-url builder requires the `I18N_ERR` prefix and threw
   `ProgrammingException` → Spring fell back to a raw servlet 500. Fixed via
   `NotFoundException.fromAffected(...)` / I18N message → proper 404.
Features updated to assert the proper statuses+messages; changelog entry added.

### (original D5 report, for history) — error-response divergence class (7 RUST_SPEC reds)
Rust returns properly mapped errors — 400 `SgtBackendInvalidInputException` with
the real business message ("No enough resources!", "There is already an upgrade
going", "Can't register mission, of type LEVEL_UP, when upgrade is not
available!", "The specified unit is not unlocked for the invoker") or 404
`NotFoundException` — where Java swallows the SAME business exceptions into
generic 500s (`{"message":"Unexpected server error"}` or raw servlet errors).
Affects: unit build rejections (2), upgrade rejections (4), deploy
units-not-held (1). The features currently assert Java's observed 500s, so
Rust goes red. Ruling needed: fix Java's exception mapping (likely kevinsuite
handler coverage) and flip the specs to the proper statuses, or accept Java's
500s as the contract and degrade Rust to match. Rust's behavior is clearly the
intended design.

### D6 — ✅ RESOLVED 2026-07-09 — ZERO_UPGRADE_TIME drift (was static finding #3)
`upgrades :: ZERO_UPGRADE_TIME…` red on Rust: missions.required_time = 5 vs
Java 3. One-line fix in `mission_bo.rs:1079` (its own comment says 3s).

### D7 — ✅ RESOLVED 2026-07-09 — Rust cancel_upgrade_mission missing `unit_type_change` emit (was static finding #4)
`upgrades :: Cancelling a running level-up…` red: the event never arrives on Rust.

### D8 — ✅ RESOLVED 2026-07-09 — deploy path: Rust emitted `unit_obtained_change` TWICE (Java once)
Fixed: `emit_after_run` + `deploy.rs` now gate the emit AND the requirement
trigger behind target ownership like Java's DeployMissionProcessor. Together
with the LocalDateTime-as-ISO Java fix (`9782f81f`) and the canonicalizing ws
normalizer, all four deploy scenarios reached full three-verdict parity
(sweep 2).
All four deploy scenarios PARITY-red with tables CLEAN — pure ws divergence:
java 1× vs rust 2× `unit_obtained_change` (plus payload diffs in
`unit_mission_change`). Note the inventory predicted MISSING emits here;
reality is a duplicate — check `deploy.rs` DeferredEmit wiring.

### D9 — ✅ ROOT-CAUSED + FIXED — the "B-only mission_information row" was a scenario RACE, not a Rust write
Java writes the identical row on registration (MissionBo L139). The diff was a
race: ZERO_UPGRADE_TIME collapses required_time to 3 s and DELAY_HANDLE is 2 s,
so the level-up task is due at **+1 s** and auto-fires MID-SCENARIO on
whichever backend's scheduler polls first — in the sweeps Java completed (and
HARD-DELETED the mission) before its dump while Rust hadn't fired yet.
Harness fix: `user_registers_level_up` now FREEZES the task (+1 h) so a
"running" mission stays running until an explicit completes/cancel step
(the completion nudge rewinds execution_time, so it is transparent).
Verified: upgrades feature now has ZERO table diffs (run 20260709_231926).
- **JAVA-SUSPECT spin-off**: Java's completion delete left the
  `mission_information` row ORPHANED in the dump despite
  `cascade = CascadeType.ALL` — and the table has NO FK in `02_schema.sql`.
  Production Java universes likely accumulate orphaned rows; check a live DB.
- Footgun: freezing via inline `CAST(id AS CHAR)` subquery hits MySQL 1267
  (collation mix vs `task_instance` utf8mb4_unicode_ci) — bind the id as a
  parameter instead.

### D10 — ✅ RULED + FIXED 2026-07-10: Rust `scheduled_tasks` row vs Java Quartz — Quartz REMOVED from Java
`TIME_SPECIAL_EFFECT_END` row was B-only (Java scheduled in QRTZ_* tables,
outside the dump). Investigation established: Java's Quartz was JDBC-backed
(`LocalDataSourceJobStore`, `QRTZ_` prefix, same datasource) and served
exactly THREE one-time event types — `TIME_SPECIAL_EFFECT_END`,
`TIME_SPECIAL_IS_READY` (ActiveTimeSpecialBo) and `UNIT_EXPIRED`
(TemporalUnitsListener/TemporalUnitScheduleListener); no cron jobs; the
`cancelEvent` API had zero callers; `registerEvent` return value unused.
- **KEVIN'S RULING (2026-07-10)**: rather than suppress or cross-map the
  differ, REMOVE Quartz from Java — it is legacy from before the db-scheduler
  lib was added; there is no good reason to keep two schedulers.
- **FIX (Java)**: `DbSchedulerTasksManagerService` replaces
  `QuartzScheduledTaskManagerService` — same `ScheduledTasksManagerService`
  interface, `mission-run` pattern (`TaskWithoutDataDescriptor`,
  `task_instance` = domain entity id, `task_data` NULL — byte-compatible with
  the rows Rust already writes); 3 dispatch beans in
  `DbSchedulerConfiguration`; `TemporalUnitScheduleListener` hard `(Double)`
  Gson cast made Long-tolerant. DELETED: Quartz service + config +
  `quartz-context.xml` + `quartz.properties` + `spring-boot-starter-quartz`
  + the `SchedulerFactoryBeanCustomizer` bean. Migration
  `migrations/v1.0.0.sql` rebuilds in-flight events FROM THE DOMAIN TABLES
  (`active_time_specials.expiring_date`/`ready_date`,
  `obtained_unit_temporal_information.expiration` — no qrtz blob parsing) and
  drops the 11 `qrtz_*` tables; `02_schema.sql` no longer creates them.
- **Rust: zero changes.** The table diff now compares the scheduling rows FOR
  REAL on both sides — stronger than suppression (a backend that fails to
  schedule an expiry goes red).
- **VERIFIED (run `20260710_153410`)**: with the Quartz-free image + D16 + D17,
  the ENTIRE time_specials feature is at FULL three-verdict parity (3/3
  ✅✅✅), scheduled_tasks table included.
- Footgun for posterity: the first rebuilt image kept the deleted
  `QuartzScheduledTaskManagerService.class` — `target/classes` is COPY'd into
  the image build and incremental `mvn install` re-packaged the orphan
  (`ClassNotFoundException: org.quartz.Scheduler` at boot). `mvn clean` +
  wipe `game-rest/target` before any compare-image rebuild that deletes
  classes.

### D11 — report/event payload shapes across explore/gather/establish/conquest/cancel-return
`mission_reports.json_body` + `mission_report_new`/`planet_explored_event`/
`mission_gather_result`/`enemy_mission_change`/`unit_mission_change` payload
diffs — same D2 (`requirementsGroups` omission) and D3 (field-shape gaps)
classes extended to more mission types. Also upgrade completion shows unlock-
list payload diffs (`unit_unlocked_change`/`time_special_unlocked_change`)
with the unlocked_relation TABLE matching — serialization-only.
Progress 2026-07-09 night:
- ✅ `mission_report_new` RUST-only `missionId`/`missionDate` — Java's single-row
  `toDto` never runs `parseMission` (that's the paginated `mission_report_change`
  path only); Rust `MissionReportBo::find_by_id` no longer joins the mission.
- ✅ `unit_mission_change` frame count (Java 2 / Rust 1 after a unit mission
  resolves) — Java's `doRegisterReturnMission` ends with
  `emitLocalMissionChangeAfterCommit(returnMission)`; the Rust omission was
  deliberate ("redundant") but the count is observable. `register_return_mission`
  now returns the new mission id and all five processor call sites queue
  `DeferredEmit::LocalMissionChange` (cancel path already matches with its own
  single emit; `MissionBaseService::give_up` still lacks it — rare path, open).
- ✅ ws `user_improvements_change` — Rust emitted the slim math-side wire shape
  (`unitTypeId`-only entries, a deliberate departure); Java emits the same
  GroupedImprovement as `user_data_change` (id + full hydrated `unitType`).
  Emitter now uses `find_user_improvement_response` (byte-parity proven on the
  REST sync path).

### D15 — ✅ ROOT-CAUSED + FIXED (cancel-resolved-explore `required_time` drift: java 5 vs rust 3.025)
FIRST HYPOTHESIS ("Rust fires missions early") was WRONG. Actual cause: the
driver's registration nudge rewinds only the `scheduled_tasks` row, NOT
`missions.termination_date` — so when the cancel runs on the "already
resolved" explore, both backends compute remaining-time against a termination
still ~30 s in the future, and the cancel-return's `required_time` equals the
WALL-CLOCK elapsed between registration and cancel: nondeterministic across
passes by construction (java pass took ~5 s, rust ~3 s — neither is wrong).
Two real defects underneath, both fixed:
1. Rust kept fractional seconds where Java truncates
   (`(long)((term-now)/1000D)`) — `my_cancel_mission` now integer-divides.
2. Harness: `fire_and_await_mission` now also rewinds the mission's own
   `termination_date` into the past (a genuinely resolved mission's
   termination IS past), so cancel-after-resolve deterministically clamps
   remaining to 0 → cancel-return reuses the full `required_time` on BOTH
   backends.
LESSON for scenario authors: any assertion downstream of remaining-time math
must pin the mission's wall-clock rows, not just the scheduler row.

### D16 — ✅ RULED + FIXED 2026-07-10: wall-clock precision class — millis are contractual
`mission_report_new.reportDate` java=…801 vs rust=…000;
`time_special_change.activeTimeSpecialDto.{activationDate,expiringDate}`
java=…749 vs rust=…000. Java serializes the in-memory entity (millis); Rust
re-reads the DATETIME row (second-truncated).
- **KEVIN'S RULING (2026-07-10): millis are contractual — Rust must send
  millis.** Precise scope, verified in frontend + Java code:
  - `pendingMillis` is the ONLY date-ish field with a functional client
    contract: user machines keep imprecise clocks, so every countdown is
    `browserComputedTerminationDate = new Date(Date.now() + pendingMillis)`
    (`owge-core date.util.ts` → `widget-countdown`); `terminationDate` itself
    is ignored. That is also why Java recomputes `pendingMillis` on cached
    responses: `RunningMissionFinderBo.findUserRunningMissions` is
    `@TaggableCacheable`, and the `unit_mission_change`/`enemy_mission_change`
    sync handlers (`MissionRestService.findSyncHandlers`) re-wrap every cache
    read with `MissionRestUtil.mutateRecalculatePendingMillis` →
    `AbstractRunningMissionDto.recalculatePendingMillis()` =
    `terminationDate.toEpochMilli() - now + 2000`.
  - `reportDate` is a DISPLAY date: epoch-millis number fed to Angular's
    `| date` pipe (`reports-list` `normalizedDate = reportDate ?? missionDate`);
    sub-second precision is not functionally consumed.
    `activationDate`/`expiringDate`/`readyDate` are consumed NOWHERE in the
    frontend (time-special UI runs off `pendingMillis` + `state`).
  - Therefore the sub-second-precision fix below is justified by WIRE PARITY
    with Java (Java emits the in-memory entity's millis and Java is the spec,
    plan §9.11) — not by a frontend need on the date fields themselves.
- **FIX (a) Rust emits**: the in-memory insert-time value is now threaded to
  the emit instead of re-reading the truncated DATETIME —
  `MissionReportManagerBo::handle_mission_report_save{,_for_users}` return
  `report_date`, carried via `DeferredEmit::MissionReport` into
  `emit_mission_report_new` (overrides the re-read DTO); activation passes the
  fresh `ActiveTimeSpecial` to `emit_time_special_change_with_fresh` →
  `find_user_status_dtos_with_fresh` patches only that row (matching Java's
  Hibernate-session-cache semantics: other rows stay DB-truncated on BOTH
  sides).
- **FIX (b) canonicalizer**: reportDate/activationDate/expiringDate/readyDate/
  userReadDate joined DATEISH; placeholders now preserve the precision class
  (`<TS-NUM-MS>`/`<TS-NUM-S>`, `<TS-STR-MS>`/`<TS-STR-S>`) so a truncation
  regression stays red while clock noise is suppressed. Known ~1/1000 flake:
  a real-millis clock landing exactly on …000 normalizes to `<TS-NUM-S>`.
- **VERIFIED** (run `20260710_111731`): `time_special_change`
  activationDate/expiringDate both normalize `<TS-NUM-MS>` on both backends;
  the scenario's remaining reds are ONLY D17 (requirements[] ordering: the
  value[] 614/193 transposition PLUS requirementsGroups[11].requirements[]
  3936/3938 transposed inside ts-59/ts-60 improvement and
  user_improvements_change — the WHOLE residual diff is that one class) + D10
  (structural task row).
- **VERIFIED report path** (run `20260710_111935`): raw frames java
  `reportDate:…976` / rust `…299` — both true millis, both `<TS-NUM-MS>` —
  and the ENTIRE `missions_explore_gather_cancel` feature (5 scenarios)
  flipped to FULL three-verdict parity ✅✅✅.
- **Remainders (not covered by scenarios yet)**: the deactivate→RECHARGE path
  (`ready_date` in-memory millis) and `handle_is_ready` re-emit still re-read
  truncated rows — same fix pattern applies when B6-B11 expiry scenarios land
  (would need `RequirementEmit::TimeSpecialChange` to carry the fresh row).
  Java `terminationDate` ISO strings observed second-precision on the wire
  (no hidden mission-path divergence). Alternative root fix if ever desired:
  widen the date columns to DATETIME(3) by migration (both backends would
  then round-trip millis) — NOT done, prod schema decision.

### D18 — NEW (upgrades run 20260709_231926): socket `user_data_change` also carries `unitType.speedImpactGroup`
Same socket-vs-REST path-dependence as `user_improvements_change`: Java's
socket-pushed `user_data_change.improvements.unitTypesUpgrades[].unitType` has
the hydrated own `speedImpactGroup` while the REST websocket-sync response
(byte-proven earlier) does NOT. Fixed: `UserStorageBo::find_data_for_socket`
(socket emitter only; REST sync path unchanged). PENDING VERIFICATION.

### D19 — ✅ FIXED 2026-07-10: upgrades feature 8/8 FULL parity (run `20260710_172904`)
Six fixes, each verified red→green:
1. `obtained_upgrades_change` — improvement hydrated via the SHALLOW builder
   (`find_for_entity_shallow`): Java's payload carries
   `unitTypesUpgrades[].unitType.speedImpactGroup` WITHOUT requirementsGroups
   (`running_upgrade_change` keeps the deep shape — path-dependent as usual).
2. `running_upgrade_change` — Rust's `find_running_level_up_mission` now
   hydrates `upgrade.improvement` (deep) + `upgrade.requirements` like Java's
   session-managed entity.
3. Envelope — `WebsocketMessage.value` skips `Value::Null` (Java's NON_NULL
   mapper drops the key; `() -> null` on completion/cancel).
4. Emit multiplicity — `drain_requirement_emits` dedup REMOVED: Java emits one
   `*_unlocked_change` per (un)registered relation (observed ×5
   time_special_unlocked_change on completion); Kevin's D11-class ruling.
5. `unit_unlocked_change` unit `speedImpactGroup` — NOT hydration-flavored
   after all: Java serializes the unit's OWN `speed_impact_group_id` FK only
   (unit 1 = NULL → key absent); Rust wrongly applied the gameplay
   inheritance resolution (`find_applicable_speed_impact_group`) in
   `UnlockedUnitFinder`. Now uses the own FK (shallow group via `find_by_id`).
   The establish-base/leave `requirementsGroups` variance remains a D2-remainder
   question for those scenarios.
6. `unit_type_change` on completion — ported Java's
   `improvementBo.triggerChange` → `UNIT_IMPROVEMENTS` listener: emitted when
   the leveled upgrade's improvement has a type-AMOUNT unitTypesUpgrades entry.
Plus two HARNESS lessons:
- `user_data_change.value.{primaryResource,secondaryResource}` are normalized
  (per-second wall-clock accrual, unassertable — ws_verify tolerated class);
  scoped to that event so upgrade/unit PRICES still diff.
- **FRESH JVM PER SCENARIO**: Java's in-memory taggable caches survive DB
  restores — the Completing scenario's improvement aggregate leaked into the
  later Cancelling/ZERO scenarios' `user_data_change` as a deterministic
  false red (+35/+40, exactly one improvement-multiple). The runner now kills
  and re-boots the Java container before every scenario's restore (Rust
  already restarted per scenario; also removes the paused-JVM restore
  deadlock hazard).

#### (original entry, for history)
### D19 — NEW: upgrades-completion unlock-frame class (the remaining D11 chunk)
From `Completing a level-up bumps…` ws diff:
- `obtained_upgrades_change.value[].upgrade.improvement.unitTypesUpgrades[].unitType.speedImpactGroup.requirementsGroups`
  RUST-only ×126 (+1 on `parent`) — here Rust OVER-emits: this payload wants
  the SHALLOW catalog shape (opposite direction from the report path; exactly
  the ws_verify "lazy-init path-dependence" catalog already handled for
  `unit_obtained_change` — check which builder obtained_upgrades_change uses).
- `unit_unlocked_change.value[].speedImpactGroup` RUST-only ×1 (unit-level).
- FRAME-COUNT `time_special_unlocked_change`: java **5** rust 1 — Java emits
  one frame per requirement re-trigger; Rust dedups/coalesces.
- FRAME-COUNT `unit_type_change`: java 1 rust 0 — Rust misses the emit on
  level-up completion.
- `running_upgrade_change.value.upgrade.{improvement,requirements}` JAVA-only
  (registration scenarios) — Rust's running-upgrade payload embeds a slimmer
  upgrade.
- `user_data_change.{primaryResource,secondaryResource}` VALUE — live
  per-second accrual drift, unassertable (same tolerated class as ws_verify);
  candidate for a justified normalization of these two keys.

### D17 — ✅ RULED + FIXED 2026-07-10: ws diff misalignment from intra-payload array ordering (extends D4)
`time_special_change.value[]`: java […,614,193,…] vs rust […,193,614,…] — one
transposition, and the element-by-element diff cascades into dozens of phantom
field diffs (most of the "247 diffs" in the time_specials activation scenario;
same class one level deeper: `requirementsGroups[].requirements[]` 3936/3938).
Neither side's order is specified: Java's `findUnlocked` →
`findByUserIdAndObjectType` has NO ORDER BY (accidental `unlocked_relation`
scan order = grant-insertion order) and `RequirementGroup.requirements` is
`@Transient` (code-filled, not even PK order); Rust does `ORDER BY id`.
- **KEVIN'S RULING (2026-07-10): array order is NOT contractual — sort in the
  canonicalizer (ws_verify precedent) — EXCEPT report lists and planet_list,
  where order IS the contract.**
- **FIX**: `normalize_ws.py` sorts (by `repr(id)`, both sides) any array whose
  EVERY element is an object carrying an `id`; content/shape/length/missing
  elements still diff normally. Exemptions keep positional comparison:
  `ORDERED_VALUE_EVENTS = {planet_user_list_change, mission_report_change,
  mission_report_new}` (whole value subtree) and `ORDERED_KEYS = {reports}`
  (paginated report responses).
- **VERIFIED by replaying the captured frames of runs `20260710_111731`,
  `20260710_111935`, `20260709_232524`, `20260709_231926`**: time_specials
  activation ws → byte-IDENTICAL (its residual was pure ordering); all 5
  explore/gather scenarios stay identical (no masking regression); the
  surviving upgrades diffs are exactly the enumerated D19 classes
  (running_upgrade_change shape, user_data_change accrual drift,
  completion-scenario unlock frame counts) — none of them ordering.
- Time_specials activation scenario PARITY verdict still 🔴 pending D10 only
  (structural scheduled_tasks row in the TABLE diff).

## From the inventory wave (static analysis — not yet reproduced by a scenario)

Confirmed-by-reading, highest confidence first; each has full detail in
`rust-backend/scripts/bdd_parity/inventories/<file>.md`:

1. **missions-travel**: `return_mission.rs` "destination not owned" fallback
   nulls `mission_id`/`target_planet` instead of the DEPLOYED-merge path that
   `deploy.rs` implements for the identical Java branch.
2. **missions-travel**: establish_base/deploy/return processors skip the
   post-commit `unit_mission_change`/`enemy_mission_change`/
   `unit_obtained_change` emits (TODO(M3/M4) comments); conquest is wired.
3. **upgrades**: `ZERO_UPGRADE_TIME` = 3 s in Java vs 5.0 in Rust (Rust's own
   comment says 3 s — drift). Cheapest fix in the backlog.
4. **upgrades**: Rust `cancel_upgrade_mission` never emits `unit_type_change`;
   no `ImprovementBo.triggerChange` listener cascade equivalent.
5. **explore/gather**: Rust's general unit-type mission-support check drops
   Java's `OWNED_ONLY` ownership refinement (cross-galaxy path has it).
6. **explore/gather**: `is_planet_explored_by_user` omits Java's ownership
   OR-branch.
7. **planet-user-misc**: tutorial visited-entries and system-message reads —
   Java inserts duplicate rows on repeat, Rust dedups. Needs the row-COUNT
   Then step to reproduce.
8. **alliance** (JAVA-SUSPECT): Java's alliance delete hits an FK error when
   pending join requests exist; Rust deletes them first. Rust is arguably
   correct — Kevin's ruling needed.
9. **time-specials**: Java Quartz-triggered deactivate is non-atomic; Rust's
   single-transaction port closes the gap (behavioral difference only under
   crash timing — probably accept).
10. **combat**: shield-bypass asymmetry (damage-split uses own-flag OR
    time-special, kill-divisor own-flag only) — faithfully ported per audit,
    but flagged as possibly unintended in Java (JAVA-SUSPECT).

## Stale doc comments found (hygiene, not behavior)
- `owge-rest/src/routes/game/mission.rs` header claims attack/counterattack
  answer 501 — they're fully wired.
- `owge-rest/src/routes/game/unit.rs` header claims build/cancel answer 501 —
  ported.
- `owge-rest/src/routes/game/mod.rs:297-299` claims `twitch_state_change`
  deferred — implemented three lines below.
- `owge-rest/src/routes/game/mod.rs:430-439` "PARTIAL/not fired" comment on
  time-special sync — implementation is complete.
