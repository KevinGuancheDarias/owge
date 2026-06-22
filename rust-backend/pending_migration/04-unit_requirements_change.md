# unit_requirements_change — missing `reached` flag + unhydrated `upgrade.order` (and null Unit-DTO fields)

## Summary

The `unit_requirements_change` websocket-sync key (`GET /game/websocket-sync?keys=unit_requirements_change`)
returns an array of `{ unit, requirements }` objects, where each requirement is
`{ level, upgrade, reached }`. The Rust port (`RequirementBo::find_faction_unit_level_requirements`,
`UnitUpgradeRequirement` DTO) diverges from Java in three ways, producing **224 diffs** in the
ws_verify harness:

1. **Missing `reached` boolean** — Java's `UnitUpgradeRequirements` POJO has a `boolean reached`
   field that Jackson always serializes (default `false`); the Rust `UnitUpgradeRequirement` DTO
   has no such field at all.
2. **Unhydrated `upgrade.order`** — Java resolves the upgrade's `order` (int) via
   `UpgradeDto.dtoFromEntity` → `entity.getOrder()` (mapped from `upgrades.order_number`). Rust
   hardcodes `order: None` based on an **incorrect comment** claiming the `upgrades` table has no
   `order_number` column. It does.
3. **`unit.order` / `unit.storageCapacity` emitted as `null` (rust-only)** — this is the shared
   Unit-DTO serialization gap (see Risk & notes), not specific to this key: Java uses global
   Jackson `Include.NON_NULL` and omits null fields, while the Rust `UnitDto` serializes
   `order`/`storage_capacity` without `skip_serializing_if`, emitting `"order":null` etc.

## Observed difference

Harness output for key `unit_requirements_change`:

```
[RUST MISSING FIELD] [].requirements[].reached        (java=false)
[RUST EMPTY/NULL]    [].requirements[].upgrade.order  java=1 rust=null
[rust-only field]    [].unit.order             (rust=null)
[rust-only field]    [].unit.storageCapacity   (rust=null)
```

## Root cause

### Sub-issue 1 — missing `reached` (CONFIRMED, with nuance)

Java POJO **declares** the field and Jackson serializes it because it is a primitive `boolean`
(`Include.NON_NULL` does not suppress primitives):

- `business/src/main/java/com/kevinguanchedarias/owgejava/pojo/UnitUpgradeRequirements.java:8`
  — `private boolean reached = false;` (getter `isReached()` → JSON key `reached`).

**Nuance — Java never computes `reached`.** The builder leaves it at its `false` default:

- `business/.../business/RequirementBo.java:484-500`
  (`createUnitUpgradeRequirements`) sets only `level` and `upgrade`; it **never calls
  `setReached(...)`**. So in this payload `reached` is **always `false`** (matching the harness
  `java=false`). The Javadoc ("If true, means the user got the upgrade to the required level")
  describes an *intended* meaning that is not actually wired up on this read path.

Rust side: the DTO has no field at all, so the key is simply absent:

- `rust-backend/owge-business/src/dto/requirement.rs:18-21` — `UnitUpgradeRequirement { level, upgrade }`.
- `rust-backend/owge-business/src/bo/requirement_bo.rs:122-145` — `From<UpgradeReqRow>` never produces a `reached`.

➡️ Parity requires adding a `reached: bool` field defaulting to `false`. Computing the *real*
satisfied-state would be a behavior change beyond Java parity (see Risk & notes).

### Sub-issue 2 — `upgrade.order` null (CONFIRMED; Rust comment is factually wrong)

Java resolves it:

- `business/.../dto/UpgradeDto.java:13` — `private Integer order;`
- `business/.../dto/UpgradeDto.java:42-43` — `loadData(...)` does `order = entity.getOrder();`
- `business/.../entity/Upgrade.java:37-38` — `@Column(name = "order_number") private Integer order;`
- `business/database/02_schema.sql:1093-1095` — `upgrades` table has
  `` `order_number` smallint UNSIGNED DEFAULT NULL ``.
- Hydration call site: `RequirementBo.java:494-496` —
  `upgradeDto.dtoFromEntity(upgradeBo.findById(current.getSecondValue().intValue()))` loads the
  full `Upgrade` entity, so `order` is populated from the DB.

Rust does NOT select the column and hardcodes null:

- `rust-backend/owge-business/src/bo/requirement_bo.rs:351-364` — the `UpgradeReqRow` SELECT lists
  `up.id, up.name, up.description, up.image_id, ..., up.cloned_improvements` but **omits
  `up.order_number`**.
- `rust-backend/owge-business/src/bo/requirement_bo.rs:104-120` — `UpgradeReqRow` has no `up_order` field.
- `rust-backend/owge-business/src/bo/requirement_bo.rs:134` —
  `order: None, // `upgrades` has no order_number column` — **this comment is incorrect**; the
  column exists (schema line 1095). This is the precise root cause: missing SELECT column + a
  wrong assumption baked into a hardcoded `None`.

(`upgrade.order` serializes fine when present — `UpgradeDto.order` is the relevant field; check its
serde attrs in `rust-backend/owge-business/src/dto/upgrade.rs` to ensure it isn't `skip_serializing_if`'d
so it can emit the resolved int rather than being dropped.)

### Sub-issue 3 — `unit.order` / `unit.storageCapacity` rust-only `null` (shared Unit-DTO gap)

These ARE selected and hydrated correctly in Rust:

- `rust-backend/owge-business/src/bo/requirement_bo.rs:326-332` selects `u.order_number` and
  `u.storage_capacity`.
- `rust-backend/owge-business/src/bo/requirement_bo.rs:79,99` — `to_unit_dto` sets
  `order: self.order_number` and `storage_capacity: self.storage_capacity`.

The diff is purely a **null-omission serialization mismatch**:

- Java omits null fields globally: `game-rest/.../configuration/RestConfiguration.java:21-22`
  (`mapper.setSerializationInclusion(Include.NON_NULL)` + `setDefaultPropertyInclusion(NON_NULL)`),
  also `BootJacksonConfigurationService.java:33`.
- Rust `UnitDto.order` (`rust-backend/owge-business/src/dto/unit.rs:21`) and `storage_capacity`
  (`unit.rs:41`) have **no** `#[serde(skip_serializing_if = "Option::is_none")]`, so when the DB
  value is NULL they serialize as `"order":null` / `"storageCapacity":null`, which Java never emits.

➡️ This is NOT unique to `unit_requirements_change`; the same `UnitDto` powers
`unit_type_change` and `unit_unlocked_change`. Fix it once on the shared DTO, not per call site.

## Affected code

Java (reference, do not modify):
- `business/.../pojo/UnitUpgradeRequirements.java:8`
- `business/.../business/RequirementBo.java:484-500`
- `business/.../dto/UpgradeDto.java:13,42-43`
- `business/.../entity/Upgrade.java:37-38`
- `business/database/02_schema.sql:1093-1095`
- `game-rest/.../configuration/RestConfiguration.java:21-22`

Rust (to change):
- `rust-backend/owge-business/src/dto/requirement.rs:18-21` — add `reached` to `UnitUpgradeRequirement`.
- `rust-backend/owge-business/src/bo/requirement_bo.rs:104-120` — add `up_order` to `UpgradeReqRow`.
- `rust-backend/owge-business/src/bo/requirement_bo.rs:122-145` — set `order` and `reached` in `From`.
- `rust-backend/owge-business/src/bo/requirement_bo.rs:351-364` — add `up.order_number` to SELECT.
- `rust-backend/owge-business/src/dto/unit.rs:21,41` — add `skip_serializing_if = "Option::is_none"` to
  `order` and `storage_capacity` (shared with other unit keys).

## Proposed fix (NOT implemented)

1. **Add `reached`** to `UnitUpgradeRequirement`:
   ```rust
   pub struct UnitUpgradeRequirement {
       pub level: i64,
       pub upgrade: UpgradeDto,
       pub reached: bool,   // Java parity: always false on this read path
   }
   ```
   Set `reached: false` in `From<UpgradeReqRow>` to match Java's never-set default. Do **not**
   attempt to compute real satisfaction unless Java behavior is intentionally being upgraded.

2. **Hydrate `upgrade.order`**: add `up.order_number AS up_order` to the `UpgradeReqRow` SELECT
   (`requirement_bo.rs:352-358`), add `up_order: Option<u16>` to the struct, and set
   `order: r.up_order` in the `From` impl (replacing the hardcoded `None` and removing the
   incorrect comment).

3. **Fix shared Unit-DTO null omission**: add
   `#[serde(skip_serializing_if = "Option::is_none")]` to `UnitDto.order` and
   `UnitDto.storage_capacity` (`dto/unit.rs:21,41`). Verify this is consistent with whatever the
   sibling `unit_type_change` / `unit_unlocked_change` migration tasks decide (ideally a single
   coordinated change to `UnitDto`).

4. Confirm `UpgradeDto.order` in `rust-backend/owge-business/src/dto/upgrade.rs` is serialized
   (not `skip_serializing_if`'d away in a manner that would drop the now-resolved int).

## Acceptance criteria

- Re-run the ws_verify harness (`scripts/ws_verify`, e.g. the one-liner in
  `scripts/ws_verify/README.md:135-146`, or the REST `keys=unit_requirements_change` diff in
  `scripts/ws_verify/rest_sync_diff.py`) against the SAME DB; the `unit_requirements_change` key
  shows ✅ **zero diffs**.
- Every element of `[].requirements[]` includes `"reached": false` (matching Java).
- `[].requirements[].upgrade.order` is the resolved integer (e.g. `1`) wherever `upgrades.order_number`
  is non-null, and absent/`null` parity-matched where the DB value is null.
- `[].unit.order` and `[].unit.storageCapacity` no longer appear as rust-only `null` fields (omitted
  when the DB value is null, matching Java `Include.NON_NULL`).
- No regression in `unit_type_change` / `unit_unlocked_change` from the shared `UnitDto` change.

## Risk & notes

- **`reached` correctness.** Java does NOT compute `reached` on this path — it ships the literal
  default `false`. The harness `java=false` confirms this. The minimal/correct parity fix is to add
  the field defaulting to `false`. Computing the *true* satisfied-state (compare the user's obtained
  upgrade level vs `level`) would be a **behavior change** that diverges from Java and would break
  the bit-for-bit harness; do not do it under the parity goal. If the game actually relies on
  `reached` being meaningful, that is a separate cross-cutting bug to raise with the project owner,
  not part of this port task.
- **Shared Unit-DTO hydration/serialization.** Sub-issue 3 is the recurring `UnitDto` null-omission
  gap shared with `unit_type_change` and `unit_unlocked_change`. Prefer one coordinated edit to
  `dto/unit.rs` so the fix lands consistently across all unit keys; verify against those sibling
  payloads before/after.
- **`upgrades.order_number` type.** Schema declares it `smallint UNSIGNED`. Mapping it as
  `Option<u16>` in `UpgradeReqRow` matches the `UnitReqRow.order_number` precedent; Java exposes it
  as `Integer`, serialized as a JSON number, so `u16`→JSON-number is parity-safe.
