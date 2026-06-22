# unit_unlocked_change — Embed per-unit `improvement` + `speedImpactGroup`, and stop emitting null scalars

## Summary

`GET /game/websocket-sync?keys=unit_unlocked_change` returns the array of units
the logged-in user has unlocked (Java: `UnitBo.findAllByUser` → `List<UnitDto>`;
Rust: `UnlockedUnitFinder::find_unlocked_by_user` → `Vec<UnitDto>`). Each element
is a **Unit** DTO (confirmed `UnitDto`, *not* `UnitType` nor `ObtainedUnit`).

The Rust `UnitDto` is missing two nested objects that Java always populates per
unit — `improvement` and `speedImpactGroup` — and it emits `order` /
`storageCapacity` as JSON `null` where Java's global `Include.NON_NULL` omits
them when the underlying column is null. The Rust `UnitDto` struct does not even
*declare* `improvement`/`speedImpactGroup` fields, so they can never appear in
the payload.

All building blocks needed for the fix already exist on the Rust side
(`ImprovementBo::find_for_entity`, `SpeedImpactGroupBo::find_by_id`, and the
private "applicable speed impact group" resolution in
`unit_interception_finder_bo.rs`); the gap is purely that `UnitDto` /
`UnlockedUnitFinder` do not wire them in.

## Observed difference

Harness diff (per element of the unlocked-units array):

```
[RUST MISSING FIELD] [].improvement       (java={"unitTypesUpgrades":[],"id":13})
[RUST MISSING FIELD] [].speedImpactGroup  (java={"canExplore":"ANY","canGather":"ANY",...})
[rust-only field]    [].order             (rust=null)
[rust-only field]    [].storageCapacity   (rust=null)
```

- `improvement` — the Unit's own `ImprovementDto` (the example
  `{"unitTypesUpgrades":[],"id":13}` is the shallow form of `ImprovementDto`,
  matching `rust-backend/owge-business/src/dto/improvement.rs`).
- `speedImpactGroup` — a `SpeedImpactGroupDto`; the `canExplore`/`canGather`/…
  keys in the example come from its `DtoWithMissionLimitation` base
  (`MissionSupportEnum` strings). The Rust `SpeedImpactGroupDto`
  (`rust-backend/owge-business/src/dto/speed_impact_group.rs`) already carries
  all of these fields.
- `order` / `storageCapacity` — flagged "rust-only" because Java omits them when
  null (NON_NULL), while Rust serializes `Option::None` as `null`.

## Root cause

### Java (reference behaviour — fields present)

- Sync key registered in
  `game-rest/src/main/java/com/kevinguanchedarias/owgejava/rest/game/UnitRestService.java:86`
  (`withHandler("unit_unlocked_change", this::findUnlocked)`), which calls
  `unitBo.findAllByUser(user)` (`UnitRestService.java:92-94`).
- `business/src/main/java/com/kevinguanchedarias/owgejava/business/UnitBo.java:139-157`
  (`findAllByUser`) initializes/resolves the `speedImpactGroup` per unit — using
  the unit's own group, else `speedImpactGroupFinderBo.findApplicable(user, unit)`
  (`UnitBo.java:148-154`) — then calls `toDto(units)`.
- `business/src/main/java/com/kevinguanchedarias/owgejava/dto/UnitDto.java`
  declares `improvement` (line 35) and `speedImpactGroup` (line 37). In
  `dtoFromEntity` (lines 49-81): `speedImpactGroup` is built from the entity at
  lines 59-62, and `improvement` is populated via
  `DtoWithImprovements.super.dtoFromEntity(entity)` at line 80.
- `business/src/main/java/com/kevinguanchedarias/owgejava/dto/DtoWithImprovements.java`
  (`dtoFromEntity`) constructs the nested `ImprovementDto` from
  `entity.getImprovement()`.
- Null omission: `game-rest/.../configuration/RestConfiguration.java:21-22` and
  `BootJacksonConfigurationService.java:33` set `Include.NON_NULL` globally, so
  `order`/`storageCapacity` (and any other null scalar) are dropped from the JSON.

### Rust (current behaviour — fields missing / null-emitted)

- Dispatch arm:
  `rust-backend/owge-business/src/websocket/sync.rs` `"unit_unlocked_change" =>`
  → `UnlockedUnitFinder::find_unlocked_by_user`.
- `rust-backend/owge-business/src/bo/unlocked/unlocked_unit_finder.rs:12-27` —
  runs `SELECT_UNIT` + the `unlocked_relation` join and maps each `UnitRow` into
  `UnitDto` via `Into`. It does **not** resolve `improvement` or
  `speedImpactGroup`.
- `rust-backend/owge-business/src/dto/unit.rs:10-42` — `UnitDto` has **no**
  `improvement` or `speed_impact_group` field (the doc comment at lines 3-7
  explicitly defers them). `order` (line 21) and `storage_capacity` (line 41)
  are `Option<_>` serialized **without** `skip_serializing_if`, so `None` →
  JSON `null`.
- `rust-backend/owge-business/src/bo/unit_bo.rs:43-87` — the `From<UnitRow>`
  impl and `SELECT_UNIT` do not touch improvement / speed-impact data.

## Affected code

- `rust-backend/owge-business/src/dto/unit.rs` — `UnitDto` struct (add
  `improvement` + `speed_impact_group`; add `skip_serializing_if` to `order` and
  `storage_capacity`).
- `rust-backend/owge-business/src/bo/unit_bo.rs` — `From<UnitRow>` (no longer a
  pure infallible mapping once nested objects are resolved) and/or a new async
  enrichment helper; `SELECT_UNIT` if the `speed_impact_group_id` /
  `improvement_id` columns are needed in `UnitRow`.
- `rust-backend/owge-business/src/bo/unlocked/unlocked_unit_finder.rs` — resolve
  the two nested objects per unit before returning.
- Reuse (do **not** reimplement):
  - `rust-backend/owge-business/src/bo/improvement_bo.rs:113` (`find_dto`) /
    `:210` (`find_for_entity`) for the `ImprovementDto`.
  - `rust-backend/owge-business/src/bo/speed_impact_group_bo.rs:91`
    (`find_by_id`) for the `SpeedImpactGroupDto`.
  - `rust-backend/owge-business/src/bo/unit_interception_finder_bo.rs:221`
    (`find_applicable_speed_impact_group`) / `:260`
    (`find_his_or_inherited_speed_impact_group`) for the applicable-group
    resolution — currently **private**; will need to be exposed (made
    `pub`/`pub(crate)`) or duplicated into a shared helper.

## Proposed fix (NOT implemented)

1. In `dto/unit.rs`, add to `UnitDto`:
   - `#[serde(skip_serializing_if = "Option::is_none")] pub improvement: Option<ImprovementDto>`
   - `#[serde(skip_serializing_if = "Option::is_none")] pub speed_impact_group: Option<SpeedImpactGroupDto>`
   and add `#[serde(skip_serializing_if = "Option::is_none")]` to the existing
   `order` and `storage_capacity` fields so null columns are omitted (NON_NULL
   parity). Verify against Java whether any *other* nullable `UnitDto` scalar is
   currently force-emitted as null by Rust; if so, give them the same treatment
   for full parity (out of scope for this key's diff, but note it).
3. Make the `From<UnitRow>` mapping the scalar-only base (leaving the two new
   fields `None`), then enrich asynchronously in `UnlockedUnitFinder` (the From
   impl can't issue queries). For each unlocked unit:
   - `improvement`: `ImprovementBo::find_for_entity(conn, "units", unit.id)`
     (resolves the unit's `improvement_id` and builds the nested DTO). Map the
     `NotFound("I18N_ERR_NULL_IMPROVEMENT")` case to `None` so units without an
     improvement omit the field, matching Java's
     `Hibernate.isInitialized(...) && != null` guard.
   - `speed_impact_group`: resolve the applicable group id with the logic from
     `unit_interception_finder_bo.rs` (own group → time-special swap rule →
     inherited from unit-type chain), exactly as Java's
     `UnitBo.findAllByUser` does (own group else `findApplicable`), then
     `SpeedImpactGroupBo::find_by_id(conn, group_id)`. Expose the currently
     private resolver (or factor it into a shared `SpeedImpactGroupFinderBo`-style
     helper) rather than duplicating the SQL.
4. Match Java's `setRequirementGroups(null)` (`UnitBo.java:150-154`): the Rust
   `SpeedImpactGroupDto` already omits `requirementsGroups`, so no extra work,
   but confirm the field stays absent in the emitted JSON.

## Acceptance criteria

- Re-run the harness (`rust-backend/scripts/ws_verify/compare_rest_sync.sh` /
  `rest_sync_diff.py`) against the same DB for key `unit_unlocked_change`.
- Key `unit_unlocked_change` reports ✅ no diff (no `[RUST MISSING FIELD]`, no
  `[rust-only field]`).
- Specifically verified per array element:
  - `improvement` present with the same shape as Java
    (`{"id":…, "unitTypesUpgrades":[…], …}`), and **absent** for units whose
    `improvement_id` is null.
  - `speedImpactGroup` present with the full `SpeedImpactGroupDto` shape
    (`canExplore`/`canGather`/… mission-limitation strings, `missionExplore` …,
    `isFixed`, `image`/`imageUrl`), resolving via own → swap-rule → inherited
    exactly as Java; `requirementsGroups` absent.
  - `order` and `storageCapacity` **omitted** (not `null`) when the column is
    null; present with the correct value when set.
- No regression on `unit_type_change` and `unit_obtained_change` keys.

## Risk & notes

- **Shared DTO with other keys.** `UnitDto` is the same DTO used by the admin
  unit CRUD (`UnitBo::find_all` / `find_by_id` in `unit_bo.rs:94-110`). Adding
  `improvement`/`speed_impact_group` as `Option` with `skip_serializing_if`
  keeps those endpoints unchanged when the fields aren't populated, but the
  enrichment must be done in the *sync finder*, not unconditionally in
  `From<UnitRow>` (which is sync and shared). Decide deliberately whether the
  admin CRUD should also embed these (Java's admin path differs); for this gap,
  only `find_unlocked_by_user` needs them.
- **Overlap with `unit_type_change` (key 02): partial, not full.** This is a
  *separate* DTO from `unit_type_change` — that key emits `UnitTypeDto`
  (`dto/unit_type.rs`), which does **not** currently embed `improvement` either,
  so the two share the *symptom* (deferred nested improvement) but not the
  struct. The genuinely **shared** pieces are the resolver functions:
  `ImprovementBo::find_for_entity` and the applicable-speed-impact-group logic.
  If key 02 is also fixed to embed `improvement`, both fixes should call the same
  `ImprovementBo` helper — coordinate so the improvement-resolution code is
  written once.
- **Overlap with obtained units (`unit_obtained_change`).** `ObtainedUnitDto`
  embeds a `unit` (`UnitDto`); once `UnitDto` carries `improvement`/
  `speedImpactGroup`, those keys' payloads may change too. Re-check the
  `unit_obtained_change` harness diff after this fix — it may resolve another
  gap or, conversely, require gating the new fields so obtained units don't
  start emitting them if Java doesn't. Confirm Java's obtained-unit path
  (`ObtainedUnitFinderBo.findCompletedAsDto`) before assuming parity.
- **Exposing private resolver.** `find_applicable_speed_impact_group` /
  `find_his_or_inherited_speed_impact_group` are currently private to
  `unit_interception_finder_bo.rs`. Prefer extracting them into a reusable
  location (a `SpeedImpactGroupFinderBo` mirroring the Java Bo) over a `pub`
  leak, to keep parity with the Java structure.
- **Ordering / determinism.** The Java swap-rule lookup is unordered; the Rust
  port already pins a deterministic order (`ORDER BY ats.id, r.id`). Preserve
  that when reusing the resolver so the chosen group is stable.
