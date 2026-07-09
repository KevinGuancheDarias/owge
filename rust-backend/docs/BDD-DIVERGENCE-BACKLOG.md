# BDD parity — divergence backlog

Every RUST_SPEC / PARITY red from `bdd_parity` runs gets an entry here with
the artifact path that proves it. Java is the default spec (plan §9.11) —
entries marked JAVA-SUSPECT need Kevin's ruling on which side is right.

## From the Phase-1 run (special_location_unlock, 2026-07-09, artifacts `/tmp/bdd_parity_runs/20260709_172100`)

### D1 — Rust never grants HAVE_SPECIAL_LOCATION unlocks to a new planet owner ⭐ the Phase-1 target
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
