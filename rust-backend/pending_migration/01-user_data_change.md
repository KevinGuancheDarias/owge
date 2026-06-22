# user_data_change — improvements serialized in snake_case, null fields emitted, and f32 production widening

## Summary
The `user_data_change` payload diverges from Java in three independent ways: (1) the nested
`improvements` object (and its `unitTypesUpgrades` array entries) is serialized in **snake_case**
because the DTO actually serialized by `find_data` — `UserImprovementDto` — has no
`#[serde(rename_all = "camelCase")]`; (2) Rust emits `null` for fields Java omits (Java's Jackson
is globally `Include.NON_NULL`), which is why `factionDto.improvement` / `factionDto.unitTypes`
show up as "rust-only"; (3) the `*ResourceProduction` value diffs are a genuine `f32`→`f64`
JSON-widening artifact (Rust stores `f32`, serde_json widens to `f64` before formatting). The
`primaryResource`/`secondaryResource` value diff is **not a bug** — it is live per-request
resource recalculation timing.

## Observed difference
```
[rust-only field]    factionDto.improvement  (rust=null)
[rust-only field]    factionDto.unitTypes  (rust=null)
[VALUE DIFF]         factionDto.primaryResourceProduction  java=0.15 rust=0.15000000596046448
[VALUE DIFF]         factionDto.secondaryResourceProduction  java=0.09 rust=0.09000000357627869
[RUST MISSING FIELD] improvements.moreChargeCapacity  (java=0.0)
[RUST MISSING FIELD] improvements.moreEnergyProduction  (java=0.0)
[RUST MISSING FIELD] improvements.moreMissions  (java=1.0)
[RUST MISSING FIELD] improvements.morePrimaryResourceProduction  (java=35.0)
[RUST MISSING FIELD] improvements.moreSecondaryResourceProduction  (java=0.0)
[RUST MISSING FIELD] improvements.moreUnitBuildSpeed  (java=0.0)
[RUST MISSING FIELD] improvements.moreUpgradeResearchSpeed  (java=0.0)
[RUST MISSING FIELD] improvements.unitTypesUpgrades  (java=[...])
[rust-only field]    improvements.more_charge_capacity  (rust=0.0)
[rust-only field]    improvements.more_energy_production  (rust=0.0)
[rust-only field]    improvements.more_missions  (rust=1.0)
[rust-only field]    improvements.more_primary_resource_production  (rust=35.0)
[rust-only field]    improvements.unit_types_upgrades  (rust=[...])
[VALUE DIFF]         primaryResource  java=301941.10 rust=301941.17   (and secondaryResource similar)
```

## Root cause

### (a) snake_case `improvements.*` — CONFIRMED (with a correction to the hypothesis)
The hypothesis was "the DTO is missing `#[serde(rename_all = "camelCase")]`." That is correct, but
the offending struct is **not** the one the hypothesis implied. There are two distinct types:

- `UserImprovementDto` — the *runtime aggregate* (Java `GroupedImprovement`). This is the type
  actually placed into `UserData.improvements` and serialized for `user_data_change`.
  - `rust-backend/owge-business/src/dto/user_improvement.rs:64-78` — the struct is
    `#[derive(Debug, Clone, Default, Serialize)]` with **no `rename_all`**, so its fields
    (`more_primary_resource_production`, `more_missions`, `unit_types_upgrades`, …) serialize as
    raw snake_case. This is the bug.
  - Its nested entry `UnitTypeImprovementEntry` (`user_improvement.rs:54-62`) also has no
    `rename_all` *and* a structurally different shape than Java (it emits
    `improvement_type`/`unit_type_id`/`value`, whereas Java `ImprovementUnitTypeDto` emits
    `type` + nested `unitType{...}` + `value`).
- `GroupedImprovementWire` / `ImprovementUnitTypeWire` (`user_improvement.rs:171-197`) — these
  *do* carry `#[serde(rename_all = "camelCase")]` and the correct `type`/`unitType` shape, but
  they are only produced by `UserImprovementDto::to_wire()` (`user_improvement.rs:142-162`),
  which is used by the **socket** `user_improvements_change` event — **not** by the REST
  `user_data_change` path.

Wiring proof (the raw aggregate, not the wire form, is what `find_data` serializes):
- `rust-backend/owge-business/src/dto/user.rs:67` — `pub improvements: UserImprovementDto,`
- `rust-backend/owge-business/src/bo/user_storage_bo.rs:512` —
  `improvements: UserImprovementBo::find_user_improvement(&mut *conn, id).await?` (returns
  `UserImprovementDto`, `user_improvement_bo.rs:83-95`).
- Dispatch serializes `UserData` directly: `rust-backend/owge-business/src/websocket/sync.rs:57`.

Java reference (camelCase via Lombok `@Data` getters + default Jackson naming):
- `business/.../dto/AbstractImprovementDto.java:18-25` — fields `morePrimaryResourceProduction`,
  `moreSecondaryResourceProduction`, `moreEnergyProduction`, `moreChargeCapacity`, `moreMissions`,
  `moreUpgradeResearchSpeed`, `moreUnitBuildSpeed`, `unitTypesUpgrades`.
- `business/.../pojo/GroupedImprovement.java:20` — `GroupedImprovement extends AbstractImprovementDto`,
  the exact type held by `UserStorageDto.improvements` (`business/.../dto/UserStorageDto.java:22`).

Note: the diff only lists `unit_types_upgrades` at the top level (renamed) and does not expand the
nested entries because the harness sorts/compares array items by `id`; once the top-level array is
renamed to `unitTypesUpgrades`, the **nested entry shape** (`type` + `unitType{ id }`, not
`improvement_type`/`unit_type_id`) must also be corrected to match `ImprovementUnitTypeDto`.

### (b) `factionDto.improvement` / `factionDto.unitTypes` "rust-only" — null-inclusion policy gap
Both fields are intentionally `null` on the Java side (`FactionDto.dtoFromEntity` sets
`unitTypes = null` at `business/.../dto/FactionDto.java:46`, and `improvement` is only populated by
the improvement engine — left null here). They appear as "rust-only" purely because **Java omits
null fields** while **Rust emits `null`**:
- Java global config: `Include.NON_NULL` at
  `game-rest/.../configuration/BootJacksonConfigurationService.java:33` and
  `game-rest/.../configuration/RestConfiguration.java:21-22`.
- Rust `FactionDto` *does* have `#[serde(rename_all = "camelCase")]`
  (`rust-backend/owge-business/src/dto/faction.rs:18`) and the right names (`improvement`,
  `unit_types` → `unitTypes`), but the fields are plain `Option<()>` with **no**
  `skip_serializing_if`, so `None` serializes as `null`:
  - `faction.rs:47` — `pub improvement: Option<()>,`
  - `faction.rs:49` — `pub unit_types: Option<()>,`
- The harness does not strip nulls (`rest_sync_diff.py:79-86` `norm()` keeps null values), so a
  `null` Rust field with no Java counterpart key is reported as `[rust-only field]`.

This is a **broader serialization-policy gap**: serde does not emit `Include.NON_NULL` behaviour
by default. The same class of mismatch will recur on any DTO with `Option` fields that Java leaves
null. The targeted fix for this key is the two faction fields; the systemic fix is a project-wide
convention (see Risk & notes).

### (c) `*ResourceProduction` value diff `0.15` vs `0.15000000596046448` — f32 widening, REAL
- Java `FactionDto.primaryResourceProduction` / `secondaryResourceProduction` are `Float`
  (`business/.../dto/FactionDto.java:33-34`); Jackson serializes a Java `float` using the shortest
  round-trippable decimal, printing `0.15` / `0.09`.
- Rust `FactionDto.primary_resource_production` / `secondary_resource_production` are `f32`
  (`rust-backend/owge-business/src/dto/faction.rs:40-41`). serde_json has no `f32` JSON number type,
  so it **widens to `f64`** before formatting, exposing the binary32 rounding of `0.15`
  (= `0.15000000596046448`). This is a genuine on-the-wire difference, not a harness artifact.
- Correct parity requires the serialized form to print `0.15`. Two viable approaches:
  store/serialize these as the same width Java prints (keep `f32` but serialize via a helper that
  emits the shortest-round-trip `f32` representation, e.g. `ryu`/`format!("{}", x)` on the `f32`),
  or hold the column as the type whose default formatting matches. Plain `f32`→serde_json is what
  produces the long tail; that is the line to change.

### (d) `primaryResource` / `secondaryResource` value diff `301941.10` vs `301941.17` — NOT a bug
Both sides are `f64` (Java `Double` at `UserStorageDto.java:12-13`; Rust `Option<f64>` at
`user.rs:69-70`), so this is **not** a width issue. The resource amount is recomputed on every
authenticated request: `UserStorageBo::trigger_resources_update`
(`rust-backend/owge-business/src/bo/user_storage_bo.rs:200-248`) does
`primary_resource += (now - last_action) * per_sec` and rewrites `last_action = now`. The Java and
Rust backends were hit at slightly different wall-clock instants against the same DB, so each
accrued a slightly different amount before reading. The ~0.07 delta is consistent with a sub-second
elapsed-time difference. This is expected live drift and would vanish on a frozen clock / identical
request instant. Do not "fix" it.

## Affected code
- `rust-backend/owge-business/src/dto/user_improvement.rs:64-78` — `UserImprovementDto` (no `rename_all`) — root cause (a).
- `rust-backend/owge-business/src/dto/user_improvement.rs:54-62` — `UnitTypeImprovementEntry` (no `rename_all`; wrong nested shape vs `ImprovementUnitTypeDto`) — root cause (a).
- `rust-backend/owge-business/src/dto/user.rs:67` — `UserData.improvements: UserImprovementDto` (the serialized type).
- `rust-backend/owge-business/src/bo/user_storage_bo.rs:512` — populates `improvements`.
- `rust-backend/owge-business/src/dto/faction.rs:40-41` — `primary/secondary_resource_production: f32` — root cause (c).
- `rust-backend/owge-business/src/dto/faction.rs:47,49` — `improvement` / `unit_types: Option<()>` with no `skip_serializing_if` — root cause (b).
- `rust-backend/owge-business/src/bo/user_storage_bo.rs:200-248` — `trigger_resources_update` (explains (d); no change needed).
- Reference only: `business/.../dto/AbstractImprovementDto.java:18-25`, `business/.../pojo/GroupedImprovement.java:20`, `business/.../dto/UserStorageDto.java:12-22`, `business/.../dto/FactionDto.java:33-46`, `business/.../dto/ImprovementUnitTypeDto.java`, `game-rest/.../configuration/{BootJacksonConfigurationService.java:33,RestConfiguration.java:21-22}`.

## Proposed fix (NOT implemented)
1. **(a) camelCase the aggregate improvement DTO.** Add `#[serde(rename_all = "camelCase")]` to
   `UserImprovementDto` (`user_improvement.rs:64`). Also align `UnitTypeImprovementEntry` with
   Java `ImprovementUnitTypeDto`: serialize the type field as `type` and a nested `unitType { id }`
   rather than `improvement_type` / `unit_type_id`. Cleanest option: make
   `UserData.improvements` serialize through the existing `GroupedImprovementWire` /
   `ImprovementUnitTypeWire` shape (which already has the correct names + `type`/`unitType`), e.g.
   change `UserData.improvements` to hold the wire type, or `#[serde(serialize_with = ...)]` calling
   `to_wire()`. (Java additionally emits each entry's full `unitType` graph with
   `speedImpactGroup` nulled — confirm the frontend only needs `unitType.id`; the wire form already
   assumes so.)
2. **(b) Omit nulls to match `Include.NON_NULL`.** Add
   `#[serde(skip_serializing_if = "Option::is_none")]` to `FactionDto.improvement` and
   `FactionDto.unit_types` (`faction.rs:47,49`). Consider a project-wide convention so this is the
   default for `Option` fields Java leaves null (see notes).
3. **(c) Stop f32→f64 widening on production rates.** Make `primary_resource_production` /
   `secondary_resource_production` serialize as the shortest round-trippable `f32` value (e.g. a
   `serialize_with` helper that emits `format!("{}", x)` as a JSON number, or via `ryu` on the
   `f32`), so the wire shows `0.15` / `0.09` like Jackson. Keep the in-memory math width unchanged.
4. **(d)** No change — confirm via acceptance test that the residual `primaryResource` diff
   disappears (or is sub-cent) when both backends read at the same instant.

## Acceptance criteria
Re-run `rust-backend/scripts/ws_verify/compare_rest_sync.sh` (Rust + Java against the same DB);
`user_data_change` must show ✅ match (no `RUST MISSING FIELD`, no `rust-only field`, no
`VALUE DIFF`) except for the live-recalc `primaryResource`/`secondaryResource` drift, which must be
explained by request timing and not by type width. Specifically, after the fix the Rust payload must:
- Rename `improvements.more_primary_resource_production` → `morePrimaryResourceProduction` and
  likewise `moreSecondaryResourceProduction`, `moreEnergyProduction`, `moreChargeCapacity`,
  `moreMissions`, `moreUpgradeResearchSpeed`, `moreUnitBuildSpeed`, `unitTypesUpgrades`.
- Emit each `unitTypesUpgrades[]` entry with Java's field names (`type`, nested `unitType{ id, … }`,
  `value`), not `improvement_type` / `unit_type_id`.
- **Omit** `factionDto.improvement` and `factionDto.unitTypes` entirely (no `null` keys).
- Serialize `factionDto.primaryResourceProduction` = `0.15` and `secondaryResourceProduction` = `0.09`
  (no `0.15000000596046448` tail).
- Leave `primaryResource`/`secondaryResource` as `f64`; any remaining diff is timing-only.

## Risk & notes
- **f32→f64 is a wider pattern.** Any Rust DTO field typed `f32` that Java serializes from a
  `Float` will exhibit the same long-tail widening. `FactionDto` alone has several
  (`custom_primary_gather_percentage`, `custom_secondary_gather_percentage`, …), and other DTOs
  likely too. The improvement floats here are `f64` and Java sums them as `Float`→displayed as
  `35.0`/`1.0` (integer-valued, so they already match), but non-integer `f32` percentages elsewhere
  will not. Worth a sweep + a shared "float like Jackson" serializer helper rather than per-field
  fixes.
- **Null-inclusion is a systemic gap.** serde emits `null` for `Option::None`; Java omits it
  globally (`Include.NON_NULL`). Every `Option` field Java leaves null is a latent "rust-only field"
  diff. Recommend a repo-wide convention (e.g. always `skip_serializing_if = "Option::is_none"`,
  or a wrapper) rather than fixing keys one at a time.
- **`primaryResource` diff is timing, not a bug.** `trigger_resources_update` mutates resources per
  request based on elapsed wall-clock; the two backends were queried at different instants. Verify by
  observing the diff shrinks/grows with the gap between the two HTTP calls; do not change the math.
- Touching `UserData.improvements` to route through `GroupedImprovementWire` must not regress the
  socket `user_improvements_change` event, which already uses that wire form — keep them consistent.
