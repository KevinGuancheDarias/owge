# Resolution — websocket-sync Rust↔Java parity

All six documented gaps (`01`–`06`) are **fixed**. Verified with
`scripts/ws_verify/compare_rest_sync.sh` (Rust + Java on the same `owge` DB):

```
SUMMARY: 19 match, 1 differ, 0 error (of 20 keys)
```

The single remaining `user_data_change` diff is `primaryResource` /
`secondaryResource` — **live resource-recalculation timing drift**, not a porting
bug: `UserStorageBo::trigger_resources_update` accrues resources per request, so
the two backends, hit at slightly different instants, read slightly different
amounts (the delta fluctuates run-to-run, e.g. 0.07 → 0.02). This was called out
as "not a bug" in `01-user_data_change.md`.

## What was changed (by task)

- **06 `unit_obtained_change`** — fixed the sqlx decode 500 (`UnitJoinedRow`
  `time`→`i32`, `primary/secondary_resource`→`u32`); then, once it returned 200,
  closed the revealed field gaps: nested `unit` now resolves `points`/`energy`/
  `hasToDisplayInRequirements`, its own `improvement` + inherited
  `speedImpactGroup`, and `ObtainedUnitDto` gained `storedUnits` (`[]`) + null
  omission on `sourcePlanet`/`targetPlanet`/`username`.
- **04 `unit_requirements_change`** — added `reached: bool` (Java default
  `false`), hydrated `upgrade.order` from `upgrades.order_number`, and added
  `skip_serializing_if` to `UnitDto.order`/`storage_capacity`.
- **02 `unit_type_change`** — replaced flat `parentId`/`shareMaxCountId` with a
  recursive catalog `UnitTypeDto` builder embedding nested `parent`,
  `shareMaxCount`, `speedImpactGroup`, `attackRule`, `criticalAttack` (reusing the
  existing `AttackRuleBo`/`CriticalAttackBo`/`SpeedImpactGroupBo` finders), with a
  depth guard. `imageUrl` resolution + null omission added on
  `SpeedImpactGroupDto` and the attack/critical entry `referenceName`.
- **03 `unit_unlocked_change`** — `UnitDto` gained `improvement` +
  `speedImpactGroup`; `UnlockedUnitFinder` enriches each unit (own improvement,
  *applicable* speed-impact group via the now-`pub(crate)` resolver).
- **05 `obtained_upgrades_change`** — nested `UpgradeDto` gained `improvement`
  (via `ImprovementBo::find_for_entity`) and `requirements` (via
  `RequirementBo::find_requirements`); the improvement's `unitTypesUpgrades[].
  unitType` is hydrated to the full catalog form, including
  `speedImpactGroup.requirementsGroups` (populated only on this path).
- **01 `user_data_change`** — `improvements` now serializes camelCase via a new
  `GroupedImprovementResponse` whose `unitTypesUpgrades` entries carry
  `{id, type, unitType, value}` (the `unitType` hydrated with **only**
  `attackRule`, matching Java's lazy-init shape on the aggregate path);
  `FactionDto` omits null `improvement`/`unitTypes` and prints `Float`s via the
  shortest-decimal serializer.

## Cross-cutting infrastructure

- `owge-business/src/dto/serde_helpers.rs` — shortest round-trip `f32`
  serialization (Jackson `Float` parity; avoids the `0.15`→`0.15000000596046448`
  f32→f64 widening).
- Broad `#[serde(skip_serializing_if = "Option::is_none")]` to match Java's
  global Jackson `Include.NON_NULL`.
- A reusable catalog `UnitTypeDto` builder with three hydration shapes, because
  **Java's nested-relation presence is Hibernate-lazy-init path-dependent** — the
  same `unitType` carries different relation sets in `unit_type_change`
  (full), the obtained-upgrade improvement (full + `requirementsGroups`), and the
  user-data improvement aggregate (`attackRule` only). Matched empirically per
  path.

## Verification

`cargo test -p owge-business --lib` → 5 passed (incl. the obtained-unit
single-join SQL-shape test). The failing **doctests** in `dto/user.rs` /
`model/user_storage.rs` are pre-existing illustrative `SqliteTemplate` examples,
unrelated to these changes.
