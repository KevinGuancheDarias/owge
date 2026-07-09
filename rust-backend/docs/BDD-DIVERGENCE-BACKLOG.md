# BDD parity — divergence backlog

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

### D10 — time-special expiry scheduling: Rust `scheduled_tasks` row vs Java Quartz
`TIME_SPECIAL_EFFECT_END` row is B-only (Java schedules in QRTZ_* tables,
outside the dump). STRUCTURAL, matches the known design difference —
candidate for a documented differ suppression (needs Kevin's sign-off per
plan §5.4; alternative: dump+normalize QRTZ triggers into pseudo-rows).

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

### D16 — NEW: wall-clock precision class — Java emits epoch-millis precision, Rust whole seconds
`mission_report_new.reportDate` java=…801 vs rust=…000;
`time_special_change.activeTimeSpecialDto.{activationDate,expiringDate}`
java=…749 vs rust=…000. Java serializes the in-memory entity (millis); Rust
re-reads the DATETIME row (second-truncated). Also these keys are NOT in
`normalize_ws.py`'s DATEISH set (only terminationDate/startingDate/
creationDate/browsingDate), so pass-to-pass clock noise shows up as VALUE
diffs. NEEDS KEVIN'S RULING: (a) is millis precision contractual (→ Rust keeps
the in-memory timestamp through to the emit, cf. [[websocket-lastsent-millis-parity]]),
and (b) should reportDate/activationDate/expiringDate/readyDate/userReadDate
join DATEISH with a precision-preserving placeholder (<TS-NUM-MS> vs <TS-NUM-S>)?

### D18 — NEW (upgrades run 20260709_231926): socket `user_data_change` also carries `unitType.speedImpactGroup`
Same socket-vs-REST path-dependence as `user_improvements_change`: Java's
socket-pushed `user_data_change.improvements.unitTypesUpgrades[].unitType` has
the hydrated own `speedImpactGroup` while the REST websocket-sync response
(byte-proven earlier) does NOT. Fixed: `UserStorageBo::find_data_for_socket`
(socket emitter only; REST sync path unchanged). PENDING VERIFICATION.

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

### D17 — NEW: ws diff misalignment from intra-payload array ordering (extends D4)
`time_special_change.value[]`: java […,614,193,…] vs rust […,193,614,…] — one
transposition, and the element-by-element diff cascades into dozens of phantom
field diffs (this is most of the "247 diffs" in the time_specials activation
scenario; after sorting id-keyed arrays only ~30 real ones remain). Same
Kevin's-ruling class as D4: is array order contractual? If not, the
canonicalizer should sort id-keyed object arrays (the ws_verify REST harness
already did exactly that).

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
