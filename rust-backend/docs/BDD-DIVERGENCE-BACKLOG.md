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
- TWO NEW findings from the fix wave itself:
  - **D13**: `time_specials :: not-unlocked rejection` — Java now 400
    `SgtBackendTargetNotUnlocked` "The target object relation has not been
    unlocked" (improved by the D5 re-parenting); Rust still answers 500
    there. Fix Rust's activate-not-unlocked error to InvalidInput(400) and
    update the scenario (currently pins the stale 500 → JAVA red).
  - **D14 (investigate first)**: `explore` scenario went red on BOTH backends —
    `planet_explored_event` not captured within 10 s. Passed in sweep 1 and
    validation; suspect ws-capture timing flake (the event fires synchronously
    at registration). RE-RUN before treating as real.

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

### D3 — mission report `json_body` field-shape gaps (establish-base report)
- `involvedUnits[0].storedUnits`: RUST-only (Java null-suppresses?).
- `involvedUnits[0].unit.improvement`: RUST-only.
- `senderUser.canAlterTwitchState`: JAVA-only.
- `targetPlanet.specialLocation.{galaxyId,galaxyName,image,imageUrl,improvement}`:
  RUST-only — Rust embeds the rich specialLocation DTO in reports where Java
  writes a slimmer shape. (Conquest reports diverge the same way, plus
  attackInformation subtree — see the conquest table.diff.)

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

### D9 — LEVEL_UP registration: Rust writes a `mission_information` row, Java doesn't
`{mission_id, relation_id=1, value=1}` B-only in all three upgrade-register
scenarios; missions table also shows an extra Rust row (type 12) —
investigate via the register scenario's table.diff (alignment noise makes the
condensed view misleading; read the full diff).

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
