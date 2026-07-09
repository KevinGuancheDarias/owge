# Bug: special-location unlocks never granted to the new planet owner

**Status:** FIXED 2026-07-09 — the "Proposed fix" below was applied verbatim
(`define_planet_as_owned_by` now fires the new-owner trigger and takes an
`emits` parameter; both processor call sites updated; misleading comment
rewritten). Reproduced RED and verified GREEN by the BDD parity harness
(`bdd_parity/features/special_location_unlock.feature`): after the fix,
`unlocked_relation` matches Java exactly (17 rows both) on the establish-base
and conquest scenarios and all `unit_unlocked_change`/`time_special_unlocked_change`
assertions pass on both backends. **Data repair for affected players is still
pending** (see "Consequences").
**Severity:** high — persistent data bug, player-visible, matches live user reports
**Reported symptom:** units or time specials (heroes) not appearing after conquering
or establishing a base on a planet whose special location should unlock them.

## Root cause

The Rust port never fires the `HAVE_SPECIAL_LOCATION` requirement re-evaluation for
the **new owner** of a planet. It only ever fires the *revoke* direction, so the
engine can take these unlocks away but can never grant them.

### Java reference behavior (3 trigger sites)

| # | Site | Direction | File |
|---|------|-----------|------|
| 1 | `PlanetBo.definePlanetAsOwnedBy` → `maybeTriggerSpecialLocation(targetPlanet, owner)` | **grant** to new owner (conquest **and** establish base both route through this helper) | `business/.../business/PlanetBo.java:194` |
| 2 | `ConquestMissionProcessor.process` → `triggerSpecialLocation(oldOwner, ...)` | revoke from old owner | `business/.../mission/processor/ConquestMissionProcessor.java:75` |
| 3 | `PlanetBo.doLeavePlanet` → `maybeTriggerSpecialLocation(planet, user)` | revoke from leaving owner | `business/.../business/PlanetBo.java:146` |

`triggerSpecialLocation` (`RequirementBo.java:223`) runs `processRelationList` over
every relation carrying a `HAVE_SPECIAL_LOCATION` requirement with
`second_value = specialLocation.id`, which inserts/deletes `unlocked_relation` rows
and cascades to obtained upgrades, unit/time-special/speed-impact-group websocket
emits, and REQUIREMENT_GROUP masters.

### Rust port state

| Java site | Rust status |
|-----------|-------------|
| 1 — grant to new owner | **MISSING.** `define_planet_as_owned_by` (`rust-backend/owge-business/src/bo/mission_processor/mod.rs:304`) deliberately skips it. Neither caller compensates: `conquest.rs` only triggers the old owner, `establish_base.rs` triggers nothing. The `DeferredEmit::ConquestSuccess` post-commit handler (`mod.rs:549`) only pushes websocket events, no requirement work. |
| 2 — revoke from old owner | ✅ ported (`conquest.rs:81-100`) |
| 3 — revoke on leave planet | ✅ ported (`planet_bo.rs:150`, `leave_planet`) |

The doc comment on `define_planet_as_owned_by` (`mod.rs:293-298`) that justifies the
omission is **factually wrong on both counts**:

- It claims the trigger is "centralized in `conquest::process`" — but that call is
  the *old-owner revoke*, not the new-owner grant.
- It claims the Java helper "is also reached by the Return / re-home path" where the
  trigger would misfire — Java's `definePlanetAsOwnedBy` is called **only** from
  `ConquestMissionProcessor` and `EstablishBaseMissionProcessor`; the Return path
  uses `ObtainedUnitBo.moveUnit`, not this helper.

## Consequences

- Conquering or establishing a base on a special-location planet never inserts the
  winner's `unlocked_relation` rows → gated units / time specials / upgrades /
  speed-impact groups never appear.
- This is persisted state, not a missed websocket push: reload does not help. The
  content stays locked until some *other* trigger for the same relation happens to
  re-run (or the Java backend processes an equivalent event).
- Affected players need a data repair after the code fix: re-run the trigger for
  every currently-owned special-location planet (or equivalently re-run
  `process_relation_list` over all `HAVE_SPECIAL_LOCATION` relations per owner).

## Verified NOT broken (checked during the same review)

- The trigger engine itself (`requirement_bo.rs`: `process_relation_list` →
  `check_requirements_are_met` → `register_obtained_relation` /
  `unregister_lost_relation`) is a faithful port: REQUIREMENT_GROUP master/slave
  handling, `HAVE_SPECIAL_LOCATION` ownership check (same semantics as Java's
  `checkSpecialLocationRequirement`), obtained-upgrade registration/availability
  flip, time-special deactivation + temporal-unit removal on loss.
- Websocket emits (`unit_unlocked_change`, `time_special_unlocked_change`,
  `speed_impact_group_unlocked_change`) exist and are drained post-commit via
  `DeferredEmit::Requirement`.
- Java's `relationObtained` internal listener is a no-op default that nothing
  overrides, so the Rust register path is not missing a listener side effect.

## Proposed fix

Fire the new-owner trigger inside `define_planet_as_owned_by`, matching Java's
placement (end of the helper: after the ownership `UPDATE` and the deployed-unit
re-homing loop, inside the same firing transaction):

1. Add an `emits: &mut Vec<DeferredEmit>` (or `&mut Vec<RequirementEmit>`)
   parameter to `define_planet_as_owned_by`.
2. At the end of the helper: read `planets.special_location_id`; if present, load
   the new owner's `UserStorage` and call
   `RequirementBo::trigger_special_location(conn, &owner, special_location_id, ...)`,
   pushing the resulting `RequirementEmit`s as `DeferredEmit::Requirement`.
3. Update the two call sites (`conquest.rs:73`, `establish_base.rs:52`) to pass
   `emits` through.
4. Rewrite the misleading comment block at `mod.rs:293-298`.

Ordering note (conquest): Java triggers the **new owner first** (inside
`definePlanetAsOwnedBy`), **then the old owner** (processor body). The Rust call
sites already invoke the old-owner trigger *after* the helper call, so putting the
new-owner trigger inside the helper reproduces Java's order naturally.

Test idea: extend the mission-verify / ws-sync harness seed with a special-location
planet gating a unit and a time special; run conquest and establish-base against it
on both backends and diff `unlocked_relation` + emitted events. (The rich seed from
commit 89fd4771 is the natural place to add this.)

Changelog: user-facing (`game-frontend/CHANGELOG.md`) entry under the in-progress
version once fixed, `Fix:` prefix.

## Wider context

This is another instance of the port diverging from the Java reference behind a
confident-but-wrong comment (compare the deploy-registration crash f5956ba6 and the
eight websocket-sync gaps fixed in 4e710856). A systematic Java-vs-Rust behavioral
diff (like the ws-sync compare harness, but for state-mutating flows) would likely
surface more of these.
