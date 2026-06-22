# unit_type_change — Rust emits flat FK ids instead of hydrated nested objects

## Summary

The `unit_type_change` websocket-sync key is the single biggest parity gap
(~173 missing fields across the unit-type array). The Rust finder
(`UnitTypeBo::find_unit_types_with_user_info`) returns a *flattened* `UnitTypeDto`
that exposes related entities only as bare foreign-key ids (`parentId`,
`shareMaxCountId`) and **omits** the nested relation objects that Java embeds
(`parent`, `shareMaxCount`, `speedImpactGroup`, `attackRule`, `criticalAttack`).

The "Rust emits flat ids + nulls instead of hydrating nested objects" diagnosis
is **CONFIRMED for the relation objects** (`parent`, `shareMaxCount`,
`speedImpactGroup`, `attackRule`, `criticalAttack`) and the rust-only flat fields
(`parentId`, `shareMaxCountId`).

It is **CORRECTED for the scalar/per-user fields**: `image`, `imageUrl`,
`maxCount`, `computedMaxCount`, `userBuilt` and `used` ARE already resolved
correctly by the sync finder today (the DTO doc-comments in
`dto/unit_type.rs` are stale and claim otherwise). If the diff harness saw those
as `null`, that capture predates the current `find_unit_types_with_user_info`
implementation (it resolves the image-store join, runs the improvement engine for
`computedMaxCount`, and counts obtained units for `userBuilt`). They should be
re-checked when the harness is re-run; the work below is scoped to the nested
relation objects, with the scalars listed in acceptance criteria as a regression
guard.

## Observed difference

Per unit-type array element, Java returns and Rust is missing:

```
[].attackRule        java = {"id":1,"name":"Tropas","entries":[...]}        rust = (absent)
[].criticalAttack    java = {"id":5,"name":"Crítico ...","entries":[...]}   rust = (absent)
[].speedImpactGroup  java = {"id":..,"name":..,"canExplore":"ANY",...}      rust = (absent)
[].parent            java = {...full nested UnitType...}                    rust = (absent)
[].shareMaxCount     java = {...full nested UnitType...}                    rust = (absent)
```

Rust-only fields not present in Java JSON:

```
[].parentId          rust = 2          (Java carries the nested `parent` object instead)
[].shareMaxCountId   rust = 2          (Java carries the nested `shareMaxCount` object instead)
```

`[].image`, `[].imageUrl`, `[].maxCount`, `[].userBuilt`, `[].computedMaxCount`,
`[].used` exist on both sides; per the correction above, the current Rust finder
resolves them. (`inheritedImprovementUnitTypes` is declared on the Java DTO but
never populated in `dtoFromEntity`, so it is absent on both sides — out of scope.)

## Root cause

The Rust `UnitTypeDto` was deliberately built as a flat projection that defers
the nested-relation domains, and the finder never hydrates them. Per relation:

### parent / shareMaxCount (recursive UnitTypeDto)
- Java embeds the full nested `UnitTypeDto` recursively:
  `business/.../dto/UnitTypeDto.java:42-49` (`shareMaxCount = new UnitTypeDto(); shareMaxCount.dtoFromEntity(...)` and same for `parent`).
- Rust exposes only the FK id and no nested object:
  `rust-backend/owge-business/src/dto/unit_type.rs:28-30` (`share_max_count_id`, `parent_id`),
  populated from the SQL columns `ut.share_max_count` / `ut.parent_type` at
  `rust-backend/owge-business/src/bo/unit_type_bo.rs:77-78` and mapped at
  `unit_type_bo.rs:57-58`.

### speedImpactGroup
- Java embeds `SpeedImpactGroupDto`:
  `business/.../dto/UnitTypeDto.java:50-53`.
- The sync source pre-nulls its `requirementGroups` before mapping:
  `game-rest/.../rest/game/UnitTypeRestService.java:40`
  (`current.getSpeedImpactGroup().setRequirementGroups(null);`), so the emitted
  group has **no** `requirementsGroups` field.
- Rust selects neither the id nor the object: there is no
  `speed_impact_group` field on the Rust DTO at all
  (`rust-backend/owge-business/src/dto/unit_type.rs:17-51`), and the sync SELECT
  does not read `ut.speed_impact_group_id`
  (`rust-backend/owge-business/src/bo/unit_type_bo.rs:75-83`).

### attackRule
- Java embeds `AttackRuleDto` with its `entries`:
  `business/.../dto/UnitTypeDto.java:54-57` (`attackRule.dtoFromEntity(entity.getAttackRule())`,
  which itself loads `AttackRuleEntryDto` list).
- Rust: no field; FK `ut.attack_rule_id` not selected
  (`unit_type_bo.rs:75-83`).

### criticalAttack
- Java embeds `CriticalAttackDto` and **additionally** sets its `entries`
  separately (the Java `CriticalAttackDto.dtoFromEntity` only sets id/name, so
  `UnitTypeDto.dtoFromEntity` fills entries explicitly):
  `business/.../dto/UnitTypeDto.java:58-63`
  (`criticalAttack.setEntries(DtoUtilService.staticDtosFromEntities(...))`).
- Rust: no field; FK `ut.critical_attack_id` not selected
  (`unit_type_bo.rs:75-83`).

### Per-user / scalar fields (already correct — regression guard only)
- Java: `computedMaxCount = findUniTypeLimitByUser(user, current)`, `userBuilt`
  set only `if hasMaxCount(...)`, `used = isUsed(id)`:
  `game-rest/.../rest/game/UnitTypeRestService.java:42-46`;
  helpers at `business/.../business/UnitTypeBo.java:96-118,133,186`.
- Rust: same logic ported at
  `rust-backend/owge-business/src/bo/unit_type_bo.rs:107-174`
  (`computed_max_count`, `user_built` guarded by `has_max_count`, `used` from
  `EXISTS(...)`), and image url via
  `image_store_bo::compute_image_url` (`unit_type_bo.rs:45-47`).

## Affected code

Rust (to change):
- `rust-backend/owge-business/src/dto/unit_type.rs:6-10,17-51` — flat DTO, deferral note.
- `rust-backend/owge-business/src/bo/unit_type_bo.rs:23-83` (`UnitTypeRow`, `From`, `SELECT_DTO`) and `:97-179` (`find_unit_types_with_user_info`).

Rust (reuse, no change expected):
- `rust-backend/owge-business/src/dto/attack_rule.rs:19-36` — `AttackRuleDto` + `AttackRuleEntryDto` already exist.
- `rust-backend/owge-business/src/bo/attack_rule_bo.rs:43-67` — `AttackRuleBo::find_by_id` already loads rule + entries as DTO.
- `rust-backend/owge-business/src/dto/critical_attack.rs:12-31` — `CriticalAttackDto` + `CriticalAttackEntryDto` already exist.
- `rust-backend/owge-business/src/bo/critical_attack_bo.rs:28-62` — `CriticalAttackBo::find_by_id` already loads it with entries.
- `rust-backend/owge-business/src/dto/speed_impact_group.rs:8-31` — `SpeedImpactGroupDto` exists (no `requirementsGroups` field, which matches the sync source nulling it).
- `rust-backend/owge-business/src/bo/speed_impact_group_bo.rs:91-100` — `SpeedImpactGroupBo::find_by_id` already returns the DTO.

Java (reference):
- `business/.../dto/UnitTypeDto.java:22-63`
- `business/.../responses/UnitTypeResponse.java`
- `business/.../business/UnitTypeBo.java:96-118,133,186`
- `game-rest/.../rest/game/UnitTypeRestService.java:36-49`

Dispatch arm: `rust-backend/owge-business/src/websocket/sync.rs:64-67` (unchanged; same finder).

## Proposed fix (NOT implemented)

1. Extend `UnitTypeDto` (`dto/unit_type.rs`):
   - Remove the flat `parent_id` / `share_max_count_id` fields (they are not in
     the Java JSON).
   - Add nested optional fields, reusing existing DTOs:
     - `parent: Option<Box<UnitTypeDto>>` (recursive — `Box` to size the type)
     - `share_max_count: Option<Box<UnitTypeDto>>`
     - `speed_impact_group: Option<SpeedImpactGroupDto>`
     - `attack_rule: Option<AttackRuleDto>`
     - `critical_attack: Option<CriticalAttackDto>`
   - Keep `#[serde(rename_all = "camelCase")]`; the nested DTOs already serialize
     in camelCase.
   - Update the stale doc-comments (lines 6-10) — image/maxCount/computedMaxCount/
     userBuilt are resolved, not deferred.

2. Hydrate in `find_unit_types_with_user_info` (`bo/unit_type_bo.rs`):
   - Add `speed_impact_group_id`, `attack_rule_id`, `critical_attack_id` to
     `UnitTypeRow` and to `SELECT_DTO` (columns `ut.speed_impact_group_id`,
     `ut.attack_rule_id`, `ut.critical_attack_id`).
   - For each row, when the corresponding id is present, call the existing
     finders on the same `&mut MySqlConnection` (honoring the single-connection
     invariant):
     - `SpeedImpactGroupBo::find_by_id` → `speed_impact_group`
     - `AttackRuleBo::find_by_id` → `attack_rule`
     - `CriticalAttackBo::find_by_id` → `critical_attack`
   - For `parent` / `share_max_count`: build a nested `UnitTypeDto` from the
     referenced row. Match Java's `parent.dtoFromEntity` semantics, which calls
     the **basic** `dtoFromEntity` recursively — i.e. the nested unit type itself
     carries its own `attackRule`/`criticalAttack`/`speedImpactGroup`/`parent`/
     `shareMaxCount` (Java recurses fully). Confirm recursion depth against a live
     capture; if the live data only nests one level the simplest port is a helper
     that loads a `UnitTypeDto` by id (catalog form, no per-user fields) and
     recurses. Guard against cycles (parent chains are expected acyclic, but a
     depth cap or visited-set is prudent).
   - Note the per-user fields (`computedMaxCount`, `userBuilt`) belong to the
     top-level type only; nested parent/shareMaxCount in Java come from the plain
     `dtoFromEntity` and therefore carry the **catalog** `maxCount` /
     `computedMaxCount==null`-equivalent — match whatever the Java JSON shows for
     nested objects (verify against capture).

3. `find_all` / `find_by_id` (admin catalog reads) share `UnitTypeRow`; decide
   whether the admin endpoints also need the nested objects. The admin frontend
   form historically consumes nested `{id}` objects — keep their current behavior
   unless the admin parity task says otherwise. If `UnitTypeRow`/`From` are shared,
   factor the hydration so only the sync path builds nested objects (or hydrate in
   both if admin parity requires it).

## Acceptance criteria

- Re-run `rust-backend/scripts/ws_verify/compare_rest_sync.sh`; the
  `unit_type_change` key reports ✅ match (no `[RUST MISSING FIELD]` / `[rust-only
  field]` lines for that key).
- Each unit-type element emits these as **nested objects** matching Java byte-for-
  byte (modulo `lastSent`):
  - `attackRule` `{ id, name, entries:[{ id, target, referenceId, referenceName,
    canAttack }] }`
  - `criticalAttack` `{ id, name, entries:[{ id, target, referenceId,
    referenceName, value }] }`
  - `speedImpactGroup` `{ id, name, isFixed, missionExplore, missionGather,
    missionEstablishBase, missionAttack, missionConquest, missionCounterattack,
    canExplore, canGather, canEstablishBase, canAttack, canCounterattack,
    canConquest, canDeploy, image, imageUrl }` (no `requirementsGroups` — the sync
    source nulls it)
  - `parent` and `shareMaxCount` as fully-nested `UnitTypeDto` objects (same shape,
    recursively).
- The rust-only flat fields `parentId` and `shareMaxCountId` are **removed**.
- Regression guard (already passing, must stay): `image`, `imageUrl`, `maxCount`,
  `computedMaxCount`, `userBuilt` (present only when the type has a max),
  `used` resolve to the same values as Java.
- Relations that are NULL in the DB serialize as absent/`null` exactly as Java
  does (Java only sets the field when the entity getter is non-null).

## Risk & notes

- Medium-sized change. The heavy lifting (the three nested DTOs and their
  `find_by_id` finders for `attackRule`, `criticalAttack`, `speedImpactGroup`)
  **already exists** in the Rust codebase from the admin-endpoint ports, so this
  is mostly wiring + a recursive `parent`/`shareMaxCount` builder.
- New DTOs to build: **none**. Reuse `AttackRuleDto`, `CriticalAttackDto`,
  `SpeedImpactGroupDto`, and recurse `UnitTypeDto` for parent/shareMaxCount.
- N+1 query consideration: calling `find_by_id` per relation per unit-type row
  multiplies queries. The unit-type catalog is small (tens of rows), so this is
  acceptable for parity-first; a batched join can come later. Keep all calls on
  the single passed `&mut MySqlConnection` (single-connection invariant; no pool
  borrowing in the Bo layer).
- Recursion: confirm the exact nesting depth Java emits for `parent` /
  `shareMaxCount` from a live capture before implementing; add a cycle/depth guard
  to avoid infinite recursion if data is ever malformed.
- Dependency on the image-store URL helper (`compute_image_url`) already used by
  the unit-type finder and by `SpeedImpactGroupBo`/`AttackRuleBo` paths — no new
  dependency.
- `inheritedImprovementUnitTypes` is intentionally out of scope: Java declares it
  on `UnitTypeDto` but never populates it in `dtoFromEntity`, so it is absent on
  both sides.
