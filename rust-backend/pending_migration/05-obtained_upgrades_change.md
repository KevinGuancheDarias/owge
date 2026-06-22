# obtained_upgrades_change — nested `upgrade` DTO is missing `improvement` and `requirements`

## Summary
The `GET /game/websocket-sync?keys=obtained_upgrades_change` payload is an array of
obtained-upgrade objects, each embedding a nested `upgrade` object. On the Java side that
nested `UpgradeDto` is fully hydrated: it carries an `improvement` object (the upgrade's
production/build/research bonuses) and a `requirements` array (the requirement-information
list). The Rust port emits the nested `upgrade` with only its scalar/catalog fields and
**omits both `improvement` and `requirements` entirely**. The omission is deliberate and
documented in the Rust DTO — these two nested objects were deferred pending the improvement
engine and requirement-system ports. The diagnosis in the task brief is **CONFIRMED**.

## Observed difference
Per array element (from the `ws_verify` harness, Rust vs Java on the same DB):

```
[RUST MISSING FIELD] [].upgrade.improvement
    (java={"morePrimaryResourceProduction":35.0,"moreSecondaryResourceProduction":0.0,...})
[RUST MISSING FIELD] [].upgrade.requirements
    (java=[{"id":1,"relation":{"id":1,"objectCode":"UPGRADE","referenceId":1},...}])
```

Both servers return HTTP 200; the top-level array, `id`, `level`, `available`, and the
nested `upgrade`'s scalar fields all match. Only the two nested sub-objects on `upgrade`
are absent in Rust.

## Root cause

### Rust side — the two nested objects are never modeled or queried
- `rust-backend/owge-business/src/dto/upgrade.rs:46-65` — `UpgradeDto` declares only scalar
  fields. There is **no `improvement` field and no `requirements` field**. The module
  doc-comment (lines 1-10) and the struct doc-comment (lines 38-43) state explicitly that
  `improvement` (improvement engine, "M2") and `requirements` (requirement system, "M3")
  are "deferred and not emitted yet".
- `rust-backend/owge-business/src/dto/upgrade.rs:92-99` — `ObtainedUpgradeDto` embeds
  `upgrade: UpgradeDto`, so it inherits that gap.
- `rust-backend/owge-business/src/bo/upgrade_bo.rs:96-108` — `SELECT_OBTAINED_DTO` joins only
  `obtained_upgrades`, `upgrades`, `upgrade_types`, and `images_store`. It does **not** join
  `improvements` and does **not** read any requirement data, so the source rows never carry
  the data needed for the two nested objects.
- `rust-backend/owge-business/src/bo/upgrade_bo.rs:65-92` — `impl From<ObtainedUpgradeRow> for
  ObtainedUpgradeDto` builds the `UpgradeDto` from scalar columns only; there is no place to
  populate `improvement`/`requirements`.
- `rust-backend/owge-business/src/bo/upgrade_bo.rs:239-250` — `UpgradeBo::find_obtained_dtos`
  runs the above SQL and maps rows straight into DTOs; it is the function wired to the key.
- `rust-backend/owge-business/src/websocket/sync.rs:80-82` — the `obtained_upgrades_change`
  dispatch arm calls `UpgradeBo::find_obtained_dtos`.

### Java side — both nested objects are hydrated during entity→DTO mapping
- Registration: `game-rest/.../rest/game/UpgradeRestService.java:54-63` registers
  `obtained_upgrades_change` → `findObtained`, which calls
  `obtainedUpgradeRepository.findByUserId(...)` then `obtainedUpgradeBo.toDto(list)`.
- `business/.../dto/ObtainedUpgradeDto.java:19-25` — `dtoFromEntity` creates a `UpgradeDto`
  and calls `upgrade.dtoFromEntity(entity.getUpgrade())`.
- `business/.../dto/UpgradeDto.java:25-41` — `UpgradeDto.dtoFromEntity`:
  - calls `DtoWithImprovements.super.dtoFromEntity(entity)` (line 33), which populates
    `improvement`;
  - maps `entity.getRequirements()` into `List<RequirementInformationDto>` (lines 34-40).
- `business/.../dto/DtoWithImprovements.java:24-29` — the default `dtoFromEntity` checks
  `Hibernate.isInitialized(entity.getImprovement())`, and if so builds an `ImprovementDto`
  from `entity.getImprovement()`. The improvement is the `@ManyToOne` on
  `Upgrade.improvement` mapped to column `upgrades.improvement_id`
  (`business/.../entity/Upgrade.java:59-61`).
- `business/.../dto/ImprovementDto.java` + `business/.../dto/AbstractImprovementDto.java:27-35`
  — `ImprovementDto` = `id` + the seven `more*` floats (mapped from `improvements` columns
  `more_primary_resource_production`, `more_secondary_resource_production`,
  `more_energy_production`, `more_charge_capacity`, `more_missions_value`,
  `more_upgrade_research_speed`, `more_unit_build_speed`; see
  `business/.../entity/ImprovementBase.java:29-48`) plus a `unitTypesUpgrades` list.
- The `requirements` come from a `@Transient` field on the entity
  (`business/.../entity/Upgrade.java:66-67`) that is filled by a JPA `@PostLoad` listener,
  `business/.../entity/listener/UpgradeListener.java:25-28`, which calls
  `requirementInformationBo.findRequirements(ObjectEnum.UPGRADE, upgrade.getId())`. Each
  `RequirementInformation` becomes a `RequirementInformationDto`
  (`business/.../dto/RequirementInformationDto.java`: `id`, `relation` (`ObjectRelationDto`),
  `requirement` (`RequirementDto`), `secondValue`, `thirdValue`).

So Java emits both nested objects as a side effect of normal entity hydration + the
`@PostLoad` requirement listener; Rust's hand-written SQL DTO path was intentionally built
without them.

## Affected code
- `rust-backend/owge-business/src/dto/upgrade.rs` — `UpgradeDto` (no `improvement`,
  no `requirements`); module/struct doc-comments declaring the deferral.
- `rust-backend/owge-business/src/bo/upgrade_bo.rs` — `SELECT_OBTAINED_DTO`,
  `ObtainedUpgradeRow`, `From<ObtainedUpgradeRow>`, `find_obtained_dtos` (and the parallel
  admin `SELECT_UPGRADE_DTO`/`UpgradeRow`/`find_all`/`find_one` path, which has the same gap
  and should be fixed consistently if `upgrade_types_change`/admin upgrade endpoints are in
  scope — they share `UpgradeDto`).
- New/shared Rust DTOs required: an `ImprovementDto` and a requirement-information DTO chain
  (`RequirementInformationDto` → `ObjectRelationDto`, `RequirementDto`).

## Proposed fix (NOT implemented)
1. Add to Rust `UpgradeDto` an `improvement: Option<ImprovementDto>` and a
   `requirements: Option<Vec<RequirementInformationDto>>` (both `Option` to match Jackson's
   null-vs-present behaviour; Java emits `improvement` only when initialized and `requirements`
   only when non-null). Match Jackson camelCase and the seven `more*` field names exactly,
   including the Java quirk `moreMissions` (DTO) ← `more_missions_value` (column) ←
   `moreMisions` (entity field).
2. Introduce a shared `ImprovementDto` (id + the seven `more*` floats + `unitTypesUpgrades`)
   built from the `improvements` row. Hydrate it by extending `SELECT_OBTAINED_DTO` with a
   `LEFT JOIN improvements imp ON imp.id = u.improvement_id` and reading the `more_*` columns
   into `ObtainedUpgradeRow`; map a `None` improvement (null `improvement_id`) to an absent
   field. Note schema column widths: most `more_*` columns are `smallint`/`tinyint`, two are
   `float` (`02_schema.sql:289-296`) — read into the right Rust types and surface as Jackson
   floats. If `unitTypesUpgrades` must be populated, a second query against
   `improvements_unit_types` is needed (defer if the harness shows Java emits `[]`/null here).
3. Introduce a shared requirement-information DTO chain and a query mirroring
   `requirementInformationBo.findRequirements(UPGRADE, upgradeId)` — i.e. the
   requirement-information rows for object code `UPGRADE` and `referenceId = upgrade.id`,
   each with its `relation` (`object_relations`) and `requirement` (`requirements`) joined.
   Because each obtained upgrade needs its own requirement list, either batch-load per
   distinct upgrade id and stitch in `find_obtained_dtos`, or add a secondary per-upgrade
   query. Reuse the same requirement DTO/query work from `unit_requirements_change` (key 04).
4. Populate both in the `From<ObtainedUpgradeRow>` / `find_obtained_dtos` mapping.

## Acceptance criteria
- Re-run the `rust-backend/scripts/ws_verify/` harness against the same DB for key
  `obtained_upgrades_change`.
- The key reports ✅ match (no `[RUST MISSING FIELD]` / value diffs).
- Each array element's nested `upgrade.improvement` is present with all seven `more*` fields
  (values and JSON shape matching Java, including `0.0` floats), and `unitTypesUpgrades`
  matching Java.
- Each array element's nested `upgrade.requirements` is present as an array whose elements
  match Java's `RequirementInformationDto` shape (`id`, `relation{ id, objectCode,
  referenceId, ... }`, `requirement`, `secondValue`, `thirdValue`).
- Absent/null handling matches Java: `improvement` omitted when the upgrade has no
  `improvement_id`; `requirements` omitted/null when there are none.

## Risk & notes
- **Shared with key 01 (`user_data_change`, ImprovementDto):** the `ImprovementDto` /
  `AbstractImprovementDto` serde model (seven `more*` floats + `unitTypesUpgrades`, with the
  `moreMissions`/`more_missions_value`/`moreMisions` naming quirk) is the same shape needed
  by the user-improvements work. Build one canonical Rust `ImprovementDto` and reuse it for
  both keys to avoid divergence in field names, null-vs-zero handling, and Jackson casing.
- **Shared with key 04 (`unit_requirements_change`, requirement DTOs):** the
  `RequirementInformationDto` → `ObjectRelationDto`/`RequirementDto` chain and the
  "find requirements for (objectCode, referenceId)" query are the same machinery used by the
  unit-requirements key (it goes through `RequirementBo`/`requirementInformationBo.findRequirements`
  on the Java side). Reuse the requirement DTOs and the finder rather than duplicating them.
- The Java requirement load is a JPA `@PostLoad` side effect (`UpgradeListener`,
  `REQUIRES_NEW` tx) keyed on `ObjectEnum.UPGRADE` — the Rust port must replicate that
  object-code/reference-id lookup explicitly in SQL; there is no FK column on `upgrades` for it.
- Per the single-connection invariant, any added queries must take `&mut MySqlConnection`
  threaded through `find_obtained_dtos`, not open new pool connections.
- The admin `UpgradeDto` path (`SELECT_UPGRADE_DTO`/`find_all`/`find_one`) shares the same
  `UpgradeDto`; adding the fields there changes admin endpoint output too — verify that is
  intended / matches Java's `AdminUpgradeRestService` output, or scope the change carefully.
