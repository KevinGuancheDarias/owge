# unit-build — Java reference behavior inventory

Domain: unit building and unit management — `BUILD_UNIT` mission (register/
complete/cancel), obtained-unit queries, unit deletion/disband, unit-type
build limits, energy/resource accounting, requirement re-triggers on unit
count change, and (tangentially, same "obtained unit management" surface)
temporal/expirable units granted by time-special activation.

All file paths relative to repo root `/home/kevin/projects/owge`. DB facts
verified 2026-07-09 against `owge_backend_developer-db-1` (db `owge`).

## 1. Endpoints

| Verb + path | Controller | Bo entry point |
|---|---|---|
| `GET /game/unit/findRunning` (deprecated since 0.9.0) | `game-rest/.../rest/game/UnitRestService.java:46-54` | `MissionFinderBo.findRunningUnitBuild` (`business/.../business/mission/MissionFinderBo.java:64-74`) |
| `POST /game/unit/build` | `UnitRestService.java:56-61` | `MissionBo.registerBuildUnit` (`business/.../business/MissionBo.java:211-275`) |
| `GET /game/unit/cancel` | `UnitRestService.java:63-67` | `MissionBo.cancelBuildUnit` (`MissionBo.java:347-350`) → `cancelMission` (`MissionBo.java:380-411`) |
| `POST /game/unit/delete` | `UnitRestService.java:69-74` | `ObtainedUnitBo.saveWithSubtraction(ObtainedUnitDto, boolean)` (`business/.../business/unit/obtained/ObtainedUnitBo.java:171-183`) |
| `GET /game/unit/{unitId}/criticalAttack` | `UnitRestService.java:76-82` | `UnitBo.findUsedCriticalAttack` (`business/.../business/UnitBo.java:133-136`) + `CriticalAttackBo.buildFullInformation` |
| websocket sync sources (`SyncSource.findSyncHandlers`, not classic REST — pulled on connect / on-demand refresh) | `UnitRestService.java:84-115` | `unit_unlocked_change`→`UnitBo.findAllByUser` (`UnitBo.java:138-157`); `unit_build_mission_change`→`MissionFinderBo.findBuildMissions` (`MissionFinderBo.java:82-92`); `unit_obtained_change`→`ObtainedUnitFinderBo.findCompletedAsDto` (`business/.../business/unit/ObtainedUnitFinderBo.java:43-45`); `unit_requirements_change`→`RequirementBo.findFactionUnitLevelRequirements` |

Not in `UnitRestService` but part of the same domain (fired by the
`db-scheduler` job, not a REST call): `MissionBo.processBuildUnit`
(`MissionBo.java:305-345`), dispatched from
`DbSchedulerRealizationJob.execute` (`business/.../job/DbSchedulerRealizationJob.java:34-64`)
via `MissionBo.runMission` (`MissionBo.java:356-363`).

Temporal-unit expiration (time-special-driven, not build-driven, but same
`obtained_units`/websocket surface — in scope per task brief):
`business/.../business/event/listener/timespecial/TemporalUnitsListener.java`
(grant on activation) and
`business/.../business/schedule/TemporalUnitScheduleListener.java`
(expire / on-demand revoke).

## 2. Behavior catalog

### `POST /game/unit/build` — `MissionBo.registerBuildUnit` (`MissionBo.java:211-275`)

Locking: outer `userLockUtilService.doInsideLockById([userId])` then inner
`planetLockUtilService.doInsideLockById([planetId])` (`MissionBo.java:217-218`,
comment explains the two-lock rationale: the user lock closes a cross-planet
race on `checkIsUniqueBuilt` that the planet lock alone can't).

- **B1 — happy path.** Preconditions: planet owned by invoker, unit unlocked,
  under mission-limit, resources+energy sufficient, unit-type limit not
  breached. Effects (all inside the locked block, one `@Transactional(READ_COMMITTED)`):
  - `object_relations` lookup for `(UNIT, unitId)` → `relation`
    (`MissionBo.java:220-221`).
  - `missions` INSERT: `type=BUILD_UNIT`, `starting_date=now(UTC)`,
    `primary_resource`/`secondary_resource` = cost, `required_time` per B10/B11,
    `termination_date` via `MissionTimeManagerBo.computeTerminationDate`
    (`MissionBo.java:240-250`, `attachRequirementsToMission` at `MissionBo.java:475-480`).
  - `mission_information` INSERT: `relation_id`=the unit's object_relation,
    `value`=`planetId` (double) (`MissionBo.java:236-238`).
  - `user_storage` UPDATE: `primary_resource -= cost`, `secondary_resource -= cost`
    (`substractResources`, `MissionBo.java:252`, `487-490`). **Energy is NOT
    decremented on `user_storage`** — there is no energy column subtracted;
    see B13b.
  - `obtained_units` INSERT: one row, `count=finalCount`, `unit_id`, `user_id`,
    `mission_id=<new mission>`, `source_planet=NULL`, `target_planet=NULL`
    ("in build" state) (`MissionBo.java:257-262`).
  - `missionSchedulerService.scheduleMission(mission)` → db-scheduler INSERT
    `scheduled_tasks(task_name='mission-run', task_instance=<mission id>,
    execution_time = now + required_time - 2s)` (`MissionSchedulerService.java:38-44`,
    `DELAY_HANDLE=2`).
  - Websocket, all in `transactionUtilService.doAfterCommit` (`MissionBo.java:266-273`):
    `missions_count_change` (`emitMissionCountChange`), `unit_build_mission_change`
    (`missionEventEmitterBo.emitUnitBuildChange`), `unit_type_change`
    (`unitTypeBo.emitUserChange`), `user_data_change`
    (`userEventEmitterBo.emitUserData`).

- **B2 — planet not owned by invoker.** `planetCheckerService.myCheckIsOfUserProperty(planetId)`
  (`MissionBo.java:212`, `business/.../business/planet/PlanetCheckerService.java:16-21`)
  → `SgtBackendInvalidInputException` ("does NOT belong to the user") → HTTP 400
  (`SgtGameRestExceptionHandler.java:27-30`). Checked *before* acquiring any lock.

- **B3 — a BUILD_UNIT mission is already running on that planet.**
  `checkUnitBuildMissionDoesNotExists` (`MissionBo.java:219`, `441-445`) calls
  `missionFinderBo.findRunningUnitBuild(userId, planetId)`; if non-null →
  `SgtBackendUnitBuildAlreadyRunningException("I18N_ERR_BUILD_MISSION_ALREADY_PRESENT")`
  → 400. One running build **per planet**, not per user (a user can build on
  two different owned planets concurrently).

- **B4 — unit not unlocked for the invoker.** `checkUnlockedUnit`
  (`MissionBo.java:222`, `466-468`) → `ObjectRelationBo.checkIsUnlocked`
  (`business/.../business/ObjectRelationBo.java:201-205`): no
  `unlocked_relation` row for `(userId, relationId)` →
  `SgtBackendTargetNotUnlocked` → 400 (falls through the generic
  `handleGameException` default `BAD_REQUEST`, `SgtGameRestExceptionHandler.java:53-56`).
  **Check is a pure existence check against `unlocked_relation`** — it does
  NOT re-derive `BEEN_RACE`/`UPGRADE_LEVEL`/etc. requirements at build time.

- **B4b — unit id has no `object_relations` row (edge case).**
  `ObjectRelationBo.findOne` returns `null` on no match (`ObjectRelationBo.java:99-101`,
  no exception); `checkUnlockedUnit(userId, relation)` then calls
  `relation.getId()` on that `null` → **uncaught `NullPointerException`** → 500,
  not a clean 4xx. Only reachable if `unitId` exists in `units` but was never
  given an `object_relations(UNIT, unitId)` row (shouldn't happen via the admin
  UI, but the REST param is a raw `Integer` with no existence check first).
  Flag for §6 open questions — does Rust reproduce a 500 here or return a
  clean 404?

- **B5 — user's concurrent-mission cap reached.**
  `missionBaseService.checkMissionLimitNotReached(user)` (`MissionBo.java:224`,
  `business/.../business/mission/MissionBaseService.java:77-83`, cap =
  `improvementBo.findUserImprovement(user).getMoreMissions() + 1`,
  `MissionBaseService.java:103-105`) → `SgtBackendInvalidInputException
  I18N_ERR_MISSION_LIMIT_EXCEEDED` → 400. Counts **all** unresolved missions of
  every type, not just BUILD_UNIT.

- **B6 — unique unit already built.** `unitBo.checkIsUniqueBuilt` (`MissionBo.java:227`,
  `UnitBo.java:107-113`): if `unit.isUnique` and
  `obtainedUnitRepository.countByUserAndUnit(user, unit) > 0` →
  `SgtBackendInvalidInputException` → 400.

- **B7 — unique unit forces count=1.** `finalCount = unit.isUnique ? 1 :
  count` (`MissionBo.java:226`) — the client-requested `count` is silently
  clamped for unique units (not rejected).

- **B7b — non-positive count.** `unitBo.calculateRequirements` (`UnitBo.java:85-96`)
  throws `SgtBackendInvalidInputException("Input can't be negative")` when
  `finalCount < 1` — reachable if a unique unit is already forced to 1 this
  can't trigger via B7's path, but a non-unique `count<=0` request hits it.

- **B8 — insufficient primary/secondary resources.**
  `ResourceRequirementsPojo.canRun` (`business/.../pojo/ResourceRequirementsPojo.java:27-31`)
  false when `user.primary < requiredPrimary` or `user.secondary <
  requiredSecondary` → `SgtMissionRegistrationException("No enough
  resources!")` (`MissionBo.java:229-231`) → 400 (`SgtMissionRegistrationException
  extends CommonException`, default handler).

- **B9 — insufficient energy.** Same `canRun` check, energy branch:
  `requiredEnergy>0 && userEnergyServiceBo.findAvailableEnergy(user) <
  requiredEnergy` → same exception/status as B8.
  `findAvailableEnergy = findMaxEnergy(user) - findConsumedEnergy(user)`
  (`business/.../business/user/UserEnergyServiceBo.java:20-36`);
  `findConsumedEnergy` = `SUM(obtained_units.count * unit.energy)` over **all**
  the user's `obtained_units` rows including ones currently mid-build (their
  row already exists with `mission_id` set at this point in a *different*,
  earlier build) — i.e. energy reserved by an in-flight build already counts
  against a second build's energy check.

- **B10 — `ZERO_BUILD_TIME` config (default `'TRUE'`).**
  `configurationBo.findOrSetDefault("ZERO_BUILD_TIME","TRUE")` (`MissionBo.java:243-245`):
  when `'TRUE'`, `requiredTime` is forced to `3` (seconds) regardless of the
  unit's real build time — **the dev/test default makes builds resolve in ~3s**,
  important for the harness's nudge-and-poll technique (§9 below).

- **B11 — build-speed improvement reduces time.**
  `improvementBo.computeImprovementValue(requiredTime,
  improvement.getMoreUnitBuildSpeed(), false)` (`MissionBo.java:232-234`) is
  applied **before** the B10 zero-time override (so if `ZERO_BUILD_TIME=TRUE`
  the improvement has no visible effect — it computes then gets overwritten).

- **B12 — unit-type max-count limit.**
  `unitTypeBo.checkWouldReachUnitTypeLimit(user, unit.type.id, finalCount)`
  (`MissionBo.java:235`, `business/.../business/UnitTypeBo.java:171-198`):
  walks to the `shareMaxCount` root type (`findMaxShareCountRoot`,
  `UnitTypeBo.java:200-202`), sums the user's current count across every type
  sharing that root (`countUnitsByUserAndUnitType` =
  `obtained_units` owned-by-type + shared-count-type,
  `UnitTypeBo.java:186-189`) plus the requested `count`; if that exceeds
  `findUniTypeLimitByUser` (faction/type max-count improved by
  `AMOUNT`-type improvements, `UnitTypeBo.java:96-105`) →
  `SgtBackendInvalidInputException` → 400. Baseline data: only unit **types**
  `1` (Tropas, max 10), `33` (Héroes, max 20), `49` (Unidad Suprema, max 250)
  have a non-null `max_count`; units **10**/**11** (types 10/30) have no limit
  configured, so B12 is not exercisable with those baseline units — a new
  unit ≥9100 with `type_id=1` is needed to test it (see §3).

- **B13 — DB effects summary (happy path).** `missions` (type=3/BUILD_UNIT —
  confirm via `mission_types`), `mission_information`, `obtained_units`
  (mission-attached, no planet), `user_storage.primary_resource`/
  `secondary_resource` decremented, `scheduled_tasks` row for `mission-run`.
  **B13b — energy accounting nuance:** `user_storage` has no energy-balance
  column that build touches; "energy" is always the derived
  `findMaxEnergy - findConsumedEnergy` and becomes consumed **the instant**
  the `obtained_units` row is inserted (even though the unit isn't "complete"
  yet) — that's exactly why `registerBuildUnit`'s afterCommit block includes
  `userEventEmitterBo.emitUserData` (energy visibly drops at *registration*,
  not at completion).

- **B14 — mission-limit / already-running checks happen inside the lock**, so
  two concurrent build requests on the *same* planet are serialized by the
  planet lock and the second sees B3; two concurrent requests for the *same
  unique unit* on *different* planets are serialized by the outer user lock
  and the second sees B6 (this is the documented reason for the two-level
  lock, `MissionBo.java:213-216`).

### `processBuildUnit` (mission completion — fired by `mission-run` scheduled task, `MissionBo.java:305-345`)

- **B15 — happy path.** Acquires the **build (source) planet** lock
  (`planetLockUtilService.doInsideLockById([sourcePlanetId])`,
  `MissionBo.java:309-311`). For every `obtained_units` row attached to the
  mission (`obtainedUnitRepository.findByMissionId`, `MissionBo.java:319`):
  sets `sourcePlanet` then `obtainedUnitBo.moveUnit(current, userId,
  sourcePlanetId)` (`MissionBo.java:323-324`, `ObtainedUnitBo.java:197-220`).
  `moveUnit` on a planet the user owns takes the **merge branch**
  (`ObtainedUnitBo.java:202-207`): `saveWithAdding` looks for an existing
  stack `(user, unit, sourcePlanet=target, expirationId IS NULL,
  mission IS NULL)` — if found, `saveWithChange` does an in-place `count +=`
  UPDATE on the *existing* row (`ObtainedUnitBo.java:190-195`) and the
  in-build row is deleted (`ObtainedUnitBo.java:123-131` — `existingOne !=
  null` branch); if not found, the in-build row itself gets
  `mission=NULL, targetPlanet=NULL, ownerUnit=NULL` and becomes the stack.
  **Stacking rule: identical `(user, unit, planet)` obtained-unit stacks merge
  by count; a build never creates a second row for a unit already sitting
  loose on the destination planet.**
- **B16 — requirement cascade.** For each unit landed,
  `requirementBo.triggerUnitBuildCompletedOrKilled(user, unit)`
  (`MissionBo.java:325`, `business/.../business/RequirementBo.java:188-216`)
  re-evaluates, under a per-user/per-planet lock
  (`userPlanetLockService.runLockedForUser`):
  - `HAVE_UNIT` requirements whose `second_value == unit.id` (grant/keep
    unlocked whatever is gated on merely owning ≥1 of the unit).
  - `UNIT_AMOUNT` requirements: `count =
    obtainedUnitRepository.countByUserAndUnit(user, unit)` (post-build total,
    all planets), then any relation gated by
    `UNIT_AMOUNT(second_value=unit.id, third_value<=count)` is (re)processed —
    **this is the cascade that unlocks content gated on "own N of unit X"** the
    moment a build completes and crosses the threshold.
- **B17 — mission row deleted, not resolved.** `missionRepository.delete(mission)`
  (`MissionBo.java:327`) — **BUILD_UNIT missions are hard-deleted on
  completion; `missions.resolved` is never set to `1` for this mission type.**
  This diverges from the harness plan's generic "poll `missions.resolved=1`"
  completion technique (§3 technique #1 of `BDD-PARITY-PLAN.md`) — for
  BUILD_UNIT the correct completion signal is **mission row absence** (or
  `obtained_units.mission_id IS NULL` for the built stack). Flagged in §6.
- **B18 — websocket emits.** `doAfterCommit` (`MissionBo.java:328-334`):
  `unit_build_mission_change` (`emitUnitBuildChange`), `missions_count_change`;
  plus, only if any landed unit had a non-null `improvement`,
  `improvementBo.clearSourceCache(user, obtainedUnitImprovementCalculationService)`.
  Separately, **`unit_obtained_change` is emitted 500ms later, asynchronously,
  outside the commit-synchronous batch**:
  `asyncRunnerBo.runAsyncWithoutContextDelayed(() ->
  obtainedUnitEventEmitter.emitObtainedUnits(user), 500)`
  (`MissionBo.java:335-338`) — a `Then` asserting this event needs the
  harness's settle-wait (§5.3 of the plan) to be ≥500ms, not just the
  post-commit window.

### `GET /game/unit/cancel` — `MissionBo.cancelBuildUnit` → `cancelMission` (`MissionBo.java:347-350`, `380-420`)

- **B19 — ownership check.** `cancelMission` compares `mission.user ==
  loggedInUser` (`MissionBo.java:393-395`); mismatch → generic `CommonException`
  ("unexpected executed condition! ... dirty Kenpachi ...",
  `MissionBo.java:405-407`) → 400 (`CommonException` has no specific handler,
  falls to the base RestExceptionHandler's default mapping).
- **B20 — mission not found.** `cancelMission(Long)` → `missionRepository.findById(...).orElse(null)`
  → `cancelMission(null)` → `MissionNotFoundException` ("The mission was not
  found, or was not passed to cancelMission()", `MissionBo.java:390-392`) →
  404 (`MissionNotFoundException extends NotFoundException`,
  `SgtGameRestExceptionHandler.java:32-35`).
- **B21 — BUILD_UNIT cancel branch → `MissionCancelBuildService.cancel`**
  (`business/.../business/mission/cancel/MissionCancelBuildService.java:28-40`,
  dispatched at `MissionBo.java:396-398` via `missionTypeBo.resolve(mission) ==
  BUILD_UNIT`):
  - `obtainedUnitModificationBo.deleteByMissionId(mission.id)` — **hard
    `DELETE FROM obtained_units WHERE mission_id=?`**
    (`ObtainedUnitModificationBo.java:24-28`), i.e. the in-build stack is
    dropped entirely, not decremented via `saveWithSubtraction`. Because this
    bypasses `saveWithSubtraction`, **`RequirementBo.triggerUnitBuildCompletedOrKilled`
    is NOT called on cancel** — correct, since the units never existed for the
    player, but worth noting as an asymmetry vs. B31 (disband via `/delete`
    DOES trigger it).
  - `improvementBo.clearCacheEntries(obtainedUnitImprovementCalculationService)`
    (`ObtainedUnitModificationBo.java:27`) — unconditional cache clear (unlike
    B18's landed-unit-improvement conditional).
  - Refund: `missionUser.addtoPrimary/addToSecondary(mission.primary/secondaryResource)`
    (`MissionCancelBuildService.java:32-33`) — **full refund of the reserved
    cost**, no penalty. **No energy refund logic exists nor is needed** (B13b:
    energy was never explicitly deducted, so deleting the obtained_units row
    alone frees it).
  - Websocket, `doAfterCommit` (`MissionCancelBuildService.java:35-39`,
    `42-45`): `unit_build_mission_change` (payload =
    `missionFinderBo.findBuildMissions(userId)`, i.e. the refreshed running-build
    list), `unit_type_change` (`unitTypeBo.emitUserChange`),
    `missions_count_change`. **No `user_data_change` is emitted here** despite
    `primary_resource`/`secondary_resource` changing — divergence risk, see §6.
- **B22 — mission row + scheduler cleanup (outer `cancelMission`, applies to
  every cancellable mission type, not just BUILD_UNIT).**
  `missionRepository.delete(mission)` then
  `abortMissionJob(mission)` → `scheduler.cancel(...)`
  (`MissionBo.java:409-410`, `MissionSchedulerService.java:52-59`) — swallows
  `TaskInstanceNotFoundException` with a `log.warn` if the scheduled task
  already fired/was already gone (no error surfaced to the caller).
- **B23 — non-BUILD_UNIT branch (for contrast, not this domain's focus):**
  plain refund + `emitUserAfterCommit` (`unitTypeBo.emitUserChange` +
  `emitMissionCountChange`) with **no** `obtained_units` deletion — confirms
  the `obtained_units`-hard-delete behavior (B21) is BUILD_UNIT-specific.

### `POST /game/unit/delete` — `ObtainedUnitBo.saveWithSubtraction` (disband/scrap)

- **B24 — full removal.** `saveWithSubtraction(dto, handleImprovements=true)`
  (`ObtainedUnitBo.java:171-183`) → loads the entity by id
  (`findByIdOrDie`), calls `saveWithSubtraction(entity, dto.count, true)`
  (`ObtainedUnitBo.java:148-169`): if `subtractionCount == obtainedUnit.count`
  → `repository.delete(obtainedUnit)` (hard delete) THEN
  `requirementBo.triggerUnitBuildCompletedOrKilled(user, unit)`
  (`ObtainedUnitBo.java:165-167`) — re-evaluates `HAVE_UNIT`/`UNIT_AMOUNT`
  after the loss (can **revoke** relations whose `UNIT_AMOUNT` threshold is no
  longer met, or whose `HAVE_UNIT` no longer holds if this was the last one —
  see `RequirementBo` `processRelationList`/revocation path, not read in
  depth here but is the general relation-loss mechanism used elsewhere, e.g.
  `docs/BUG-SPECIAL-LOCATION-UNLOCK.md`'s `relationLost`).
- **B25 — partial removal.** `0 < subtractionCount < count` →
  `saveWithChange(obtainedUnit, -subtractionCount)` (UPDATE `count -=`,
  `ObtainedUnitBo.java:190-195`, cache-tag evicted manually since a
  `@Modifying` update bypasses the entity listener) — **also** triggers
  `triggerUnitBuildCompletedOrKilled` (`ObtainedUnitBo.java:161-163`), i.e.
  even a partial disband re-checks `UNIT_AMOUNT` thresholds (correct: the
  count dropped).
- **B26 — negative count.** `subtractionCount < 0` →
  `SgtBackendInvalidInputException` ("... you can go cry if you want",
  `ObtainedUnitBo.java:155-157`) → 400.
- **B27 — over-subtraction.** `subtractionCount > count` →
  `SgtBackendInvalidInputException` (`ObtainedUnitBo.java:158-160`) → 400.
- **B28 — server-stamped owner.** `UnitRestService.delete` sets
  `obtainedUnitDto.setUserId(loggedIn.id)` (`UnitRestService.java:70-72`)
  **before** calling the Bo — the client cannot disband another user's stack
  by spoofing `userId` in the request body (the `id` still has to resolve to
  a real `obtained_units` row, but ownership is asserted server-side... note:
  ⚠ the Bo itself does not appear to re-verify `entity.user.id ==
  dto.userId` inside `saveWithSubtraction` — the server-stamped `dto.userId`
  is only used for the *cache tag* and for downstream emit targeting
  (`ObtainedUnitBo.java:176-183`); the entity to mutate is looked up purely by
  `dto.id` via `findByIdOrDie`. This means a user who knows another user's
  `obtained_units.id` can still delete/shrink it via this endpoint — **flag as
  a possible authorization gap**, not confirmed exploitable without checking
  `findByIdOrDie`'s exact query, but worth a scenario (§6).
- **B29 — cache/improvement.** `handleImprovements=true` (always, from this
  endpoint) → `doAfterCommit(improvementBo.clearSourceCache(user,
  obtainedUnitImprovementCalculationService))` (`ObtainedUnitBo.java:152-154`).
- **B30 — websocket emits — NOT wrapped in `doAfterCommit`.**
  (`ObtainedUnitBo.java:177-183`, executed synchronously inside the
  `@Transactional` method body, unlike almost every other emit path in this
  file): `user_data_change` (`userEventEmitterBo.emitUserData`) **only if**
  `unitBeforeDeletion.unit.energy > 0`; `unit_type_change`
  (`unitTypeBo.emitUserChange`); `unit_obtained_change`
  (`obtainedUnitEventEmitter.emitObtainedUnits`). Because these calls are not
  deferred to after-commit, they run before the surrounding transaction
  actually commits to the DB — a `Then` reading DB state at the same instant
  the ws frame lands could theoretically race ahead of the commit. Documented
  as an observation, not asserted to be a live bug (see §6).

### `GET /game/unit/findRunning` (deprecated) — `MissionFinderBo.findRunningUnitBuild`

- **B31.** No running BUILD_UNIT mission for `(userId, planetId)` → REST
  returns the **empty string body** (`UnitRestService.java:49-50`;
  `Object` return of `""` is written by Spring's `StringHttpMessageConverter`
  as a zero-byte `text/plain` body, NOT the JSON string `"\"\""`). Otherwise
  wraps `RunningUnitBuildDto` in `DeprecationRestResponse<>("0.9.0",
  "/unit/build-missions", retVal)` (`UnitRestService.java:52-53`).

### `GET /game/unit/{unitId}/criticalAttack`

- **B32.** `unitBo.findUsedCriticalAttack(unitId)` = `unit.criticalAttack` if
  set, else `criticalAttackBo.findUsedCriticalAttack(unit.type)` (type-level
  default) (`UnitBo.java:133-136`); if still null, endpoint returns `[]`
  (`UnitRestService.java:78-81`), else the full
  `CriticalAttackInformationResponse` list.

### Websocket sync handlers (pulled by `SyncHandlerBuilder`, not classic REST — full-list-push semantics per plan §6.5/§9.5)

- **B33 — `unit_unlocked_change`.** `UnitBo.findAllByUser` — unboxes
  `unlocked_relation` rows of object type `UNIT` to `Unit` entities
  (`UnitBo.java:139-142`), detaches + sets `isInvisible` (hidden-unit rule)
  and resolves a `speedImpactGroup` per unit if none is directly configured
  (`UnitBo.java:143-155`). This is the payload the special-location-unlock
  canonical scenario (`BDD-PARITY-PLAN.md` §2.5/§6.1) asserts against for the
  UNIT side.
- **B34 — `unit_build_mission_change`.** `MissionFinderBo.findBuildMissions(userId)`
  — **every** unresolved BUILD_UNIT mission for the user across **all**
  planets, each entry carrying `(unit, mission, planet, count)` (count = the
  first `obtained_units` row's count for that mission, or `0` if somehow none
  exists) (`MissionFinderBo.java:82-92`). Full-list push: cancelling one build
  re-sends the list minus that entry, not a delta.
- **B35 — `unit_obtained_change`.** `ObtainedUnitFinderBo.findCompletedAsDto(user)`
  — `obtainedUnitRepository.findDeployedInUserOwnedPlanets(user.id)`, i.e.
  **only units actually sitting on a planet the user owns** — excludes
  in-build (`mission_id` set, no planet), in-transit/deployed-elsewhere, and
  "stored" rows (`ownerUnit != null`, filtered explicitly at
  `ObtainedUnitFinderBo.java:48`). `@TaggableCacheable` tagged by
  `Unit.UNIT_CACHE_TAG` + per-user obtained-unit tag
  (`ObtainedUnitFinderBo.java:39-42`) — a bulk/`@Modifying` write elsewhere
  that skips the entity listener must evict this tag manually (per
  `CLAUDE.md` "Concurrency & caching"); `saveWithChange`
  (`ObtainedUnitBo.java:190-195`) is the example that does so correctly.
- **B36 — `unit_requirements_change`.**
  `requirementBo.findFactionUnitLevelRequirements(factionBo.findByUser(...))`
  filtered to `unit.hasToDisplayInRequirements`, with `improvement`,
  `speedImpactGroup`, and each requirement's nested `upgrade.requirements`
  stripped before serialization (`UnitRestService.java:104-115`) — a
  presentation-layer trim, not a data-completeness signal.

### Temporal / expirable units (time-special-driven grant + expiry — same `obtained_units` surface)

Not reachable via `UnitRestService`; included because the task brief names it
explicitly and it shares tables/events with the rest of this domain.

- **B37 — grant on time-special activation.**
  `TemporalUnitsListener.onTimeSpecialActivated`
  (`business/.../business/event/listener/timespecial/TemporalUnitsListener.java:47-93`,
  `@TransactionalEventListener(phase = BEFORE_COMMIT)`): for every
  `TIME_SPECIAL_IS_ACTIVE_TEMPORAL_UNITS` rule on the activated time special
  with exactly 2 extra args `(durationSeconds, count)` and a `UNIT`
  destination, groups by duration, then per duration group: INSERT
  `obtained_unit_temporal_information(duration, expiration=now+duration,
  relation_id=<the time special's own object_relation id>)`
  (`TemporalUnitsListener.java:73-82`), stamp `expiration_id` on each new
  in-memory `ObtainedUnit(unit, user, count, source_planet=
  OwgeContextHolder-selected-planet-or-home)` and `INSERT` them
  (`TemporalUnitsListener.java:84-86`, `100-115`), possibly trigger an
  improvement-cache clear (`unitImprovementUtilService.maybeTriggerClearImprovement`),
  and register a `UNIT_EXPIRED` scheduled task keyed by the new
  `obtained_unit_temporal_information.id`, firing `duration` seconds out
  (`TemporalUnitsListener.java:95-98`). Emits `unit_obtained_change`
  synchronously (not `doAfterCommit` — this listener itself already runs
  `BEFORE_COMMIT`) if any group was granted (`TemporalUnitsListener.java:90-92`).
- **B38 — expiry (natural).**
  `TemporalUnitScheduleListener` task handler `UNIT_EXPIRED`
  (`business/.../business/schedule/TemporalUnitScheduleListener.java:55-66`):
  guarded by `obtained_unit_temporal_information` still existing (idempotent
  against a duplicate fire), acquires an "aggressive" planet lock over every
  planet currently holding a unit with that `expiration_id`
  (re-checks the planet set under lock and retries if it changed mid-acquire —
  `aggressiveLockAcquire`, `TemporalUnitScheduleListener.java:110-122` — guards
  against a unit moving planets between the lock-target computation and the
  lock itself), then `doDeleteExpiredOrOrDemand`
  (`TemporalUnitScheduleListener.java:98-108`): hard-deletes every
  `obtained_units` row with that `expiration_id`, conditionally clears the
  improvement cache, emits `unit_obtained_change` after commit, handles
  affected missions (`handleAffectedMissions`,
  `TemporalUnitScheduleListener.java:124-136`: deletes any mission left with
  zero remaining `obtained_units`, emits `unit_mission_change` +
  `missions_count_change` to the temporal-unit owner, and
  `enemy_mission_change` to any *other* user who owned a target planet of one
  of those missions), then deletes the
  `obtained_unit_temporal_information` row itself.
- **B39 — expiry (on-demand, early revoke).**
  `TemporalUnitScheduleListener.relationLost`
  (`RequirementComplianceListener` override,
  `TemporalUnitScheduleListener.java:68-76`): when a user loses the
  `unlocked_relation` for a `TIME_SPECIAL`, if that time special has an
  `ACTIVE` `active_time_specials` row for the user, force-deactivate it
  (`doDeactivateTimeSpecial`, `TemporalUnitScheduleListener.java:78-88`) and
  immediately run the **same** `doDeleteExpiredOrOrDemand` path for every
  `obtained_unit_temporal_information` row tied to that time special's
  relation — i.e. losing the unlock yanks the granted temporal units
  immediately rather than waiting for the natural timer. This is the same
  general "relation lost → cascade" mechanism documented for special
  locations in `docs/BUG-SPECIAL-LOCATION-UNLOCK.md`; worth checking whether
  the Rust port's `relation_lost` handling (if any) covers this listener too,
  not just the special-location one.

## 3. Draft Gherkin scenarios

Reserved id ranges per `BDD-PARITY-PLAN.md` §6.1 VERIFIED note: units ≥9100,
time specials ≥900, missions ≥900000. Baseline facts used below (verified
live 2026-07-09): unit **10** (X-302, cost 9000/6000, time 1500, energy 67,
type 10, not unique), unit **11** (BC-303, cost 27000/18000, time 3750,
energy 80, type 30, not unique); relation **22** = `object_relations(UNIT,10)`,
relation **23** = `object_relations(UNIT,11)`; both gated by `BEEN_RACE`+
`UPGRADE_LEVEL` requirements which the `Given` step for unlocking bypasses
(it inserts `unlocked_relation` directly, matching `checkIsUnlocked`'s pure
existence check, B4). User 1 home planet 1002, user 2 home planet 1004.
`ZERO_BUILD_TIME` defaults to `'TRUE'` (B10) — every build resolves in ~3s
without needing the execution_time nudge, but the nudge technique still
applies for determinism.

Only §6 catalog steps are used except where a `Given`/`Then` is proposed new
in §4 (marked `*NEW*`).

```gherkin
Feature: Unit build registration, completion, and cancellation

  Background:
    Given the standard test universe
    And user 1 has an unlocked relation for object UNIT reference 10

  Scenario: Registering a build deducts resources and schedules the mission
    # Covers B1, B13, B13b, B14, B15
    When user 1 runs a BUILD_UNIT mission from planet 1002 to planet 1002 with 5 units of id 10
    Then table missions has a row where user_id=1 and type=BUILD_UNIT
    And user 1 received websocket event "unit_build_mission_change" where some item has id 10
    And user 1 received websocket event "unit_type_change"
    And user 1 received websocket event "user_data_change"
    And user 1 received websocket event "missions_count_change"

  Scenario: Build completes and lands the units on the planet, merging with an existing stack
    # Covers B15 (merge branch), B16 (UNIT_AMOUNT/HAVE_UNIT re-trigger), B17, B18
    Given user 1 has 3 units of id 10 on planet 1002
    When user 1 runs a BUILD_UNIT mission from planet 1002 to planet 1002 with 5 units of id 10
    And the BUILD_UNIT mission of user 1 completes
    Then user 1 has 8 units of id 10 on planet 1002
    And table missions has no row where user_id=1 and type=BUILD_UNIT
    And user 1 received websocket event "unit_obtained_change" where some item has id 10

  Scenario: Cancelling a build refunds resources and removes the in-build units
    # Covers B19, B21, B22, B25 (websocket set), B26 (open question: no user_data_change)
    When user 1 runs a BUILD_UNIT mission from planet 1002 to planet 1002 with 5 units of id 10
    And user 1 cancels their build mission on planet 1002       # *NEW* — see §4
    Then user 1 has 0 units of id 10 on planet 1002
    And table missions has no row where user_id=1 and type=BUILD_UNIT
    And user 1 received websocket event "unit_build_mission_change"
    And user 1 received websocket event "unit_type_change"

  Scenario: Cannot register a second build on the same planet while one is running
    # Covers B3
    Given user 1 runs a BUILD_UNIT mission from planet 1002 to planet 1002 with 1 units of id 10
    When user 1 attempts to build 1 units of id 10 on planet 1002 and expects an error   # *NEW*
    Then the last build attempt of user 1 failed with error "I18N_ERR_BUILD_MISSION_ALREADY_PRESENT"   # *NEW*

  Scenario: Cannot build a unit that is not unlocked
    # Covers B4
    Given user 1 has no unlocked relation for object UNIT reference 11   # *NEW* (negative of existing Given)
    When user 1 attempts to build 1 units of id 11 on planet 1002 and expects an error
    Then the last build attempt of user 1 failed with error "SgtBackendTargetNotUnlocked"

  Scenario: Disbanding units fully removes the stack and re-triggers requirement checks
    # Covers B24, B29, B30
    Given user 1 has 5 units of id 10 on planet 1002
    When user 1 disbands all units of id 10 on planet 1002   # *NEW*
    Then user 1 has 0 units of id 10 on planet 1002
    And user 1 received websocket event "unit_obtained_change"
    And user 1 received websocket event "unit_type_change"
```

Not yet drafted (needs a fresh unit ≥9100 with `type_id=1`, max_count 10, per
B12): a unit-type-limit scenario. Also not drafted: the temporal-unit
grant/expire flow (B37-B39) — `rust-backend/scripts/seed_temporal_units.sql`
is the ready-made `Given` block (activates time special 648 → grants unit 491
expiring in 21601s) but needs a `*NEW*` "user {u} activates time special
{tsid}" `When` (already listed as a catalog step in
`BDD-PARITY-PLAN.md` §6.3) plus a `*NEW*` `Then` for
`obtained_unit_temporal_information` existence and the `UNIT_EXPIRED`
scheduled-task row — left for Phase 2/3 per the roadmap.

## 4. Proposed new steps

QUARANTINED — none of these exist in `BDD-PARITY-PLAN.md` §6 yet.

| Step text | Why needed | Implementation notes |
|---|---|---|
| `user {u} cancels their build mission on planet {pid}` | §6.3 has no cancel-build `When`; `GET /game/unit/cancel` needs the mission id, which the scenario doesn't know ahead of time (autoincrement, per §9 pitfall 9) | Look up the unresolved BUILD_UNIT mission for `(u, pid)` via SQL (mirrors `MissionFinderBo.findRunningUnitBuild`), then REST `GET game/unit/cancel?missionId=<found>`. Assert 2xx. |
| `user {u} attempts to build {n} units of id {uid} on planet {pid} and expects an error` | Every existing `When` in §6.3 asserts 2xx (`BDD-PARITY-PLAN.md` §6.3 closing paragraph); negative scenarios (B2-B9, B12) need a variant that captures the error instead of failing the scenario | POST `game/unit/build`, do NOT assert 2xx; store `(status, exception_type, message)` on `World` for the next `Then`. |
| `the last build attempt of user {u} failed with error "{marker}"` | Pairs with the step above | Assert the stored response's `exceptionType` (Java: `GameBackendErrorPojo.exceptionType` = `e.getClass().getSimpleName()`, e.g. `SgtBackendTargetNotUnlocked`) OR message contains `{marker}` for the `I18N_*` string cases (`MissionBo.java:219,224,443`). Needs a Rust equivalent error-shape check — Rust errors currently surface via `OwgeError` variants (`InvalidInput`/`NotFound`/`Common`), not necessarily the same `exceptionType` string; this step's implementation needs a mapping table, not a blind string compare (§6 open question). |
| `user {u} has no unlocked relation for object {OBJ} reference {rid}` | Negative Given, complement of the existing "has an unlocked relation" step (`BDD-PARITY-PLAN.md` §6.2) | `DELETE FROM unlocked_relation WHERE user_id=? AND relation_id=(SELECT id FROM object_relations WHERE object_description=? AND reference_id=?)`. |
| `user {u} disbands all units of id {uid} on planet {pid}` | §6.3 has no disband/delete `When`; `POST /game/unit/delete` needs the target `obtained_units.id`, not `(unit, planet)` | Resolve the row id by `(user, unit, planet, mission IS NULL)` via SQL, then POST `game/unit/delete` with `{id, count: <full count>}`. A partial-count variant (`disbands {n} units of id {uid} on planet {pid}`) covers B25. |
| `user {u} activates time special {tsid}` | Already named in `BDD-PARITY-PLAN.md` §6.3 table but not yet implemented; needed for B37-B39 | REST activate endpoint (find exact route — not investigated in this pass, likely `TimeSpecialRestService` in `game-rest`), synchronous per the plan's note. |
| `table obtained_unit_temporal_information has a row where relation_id={rid}` | Generic escape hatch (`BDD-PARITY-PLAN.md` §6.4) doesn't list this table in its whitelist | Add `obtained_unit_temporal_information` and `scheduled_tasks` (filtered to `task_name` IN `('mission-run','UNIT_EXPIRED')`) to the `Then` whitelist for temporal-unit scenarios. |

## 5. Rust port status

All 5 `UnitRestService` REST routes exist in
`rust-backend/owge-rest/src/routes/game/unit.rs:31-38` and are wired to real
`owge-business` calls (NOT stubbed) — the file's own header comment (lines
5-14) claiming `build`/`cancel` "answer `501`" is **stale**; the code at
`unit.rs:95-110` and `120-128` calls `MissionBo::register_build_unit` /
`MissionBo::cancel_build_unit` directly. `rust-backend/docs/UNPORTED-ENDPOINTS.md`
(15 lines total) lists no `game/unit/*` route, consistent with "ported."

- `register_build_unit` (`rust-backend/owge-business/src/bo/mission_bo.rs:91-125`,
  validation in `do_register_build_unit` at `mission_bo.rs:577-`): mirrors
  B2-B14 closely — planet-ownership check (`mission_bo.rs:99-108`), the
  same user+planet lock pair via `run_locked` (`mission_bo.rs:110-116`),
  `checkUnitBuildMissionDoesNotExists` (`mission_bo.rs:586-601`), unlock
  check via `UnlockedRelationBo::find_unlocked_reference_ids`
  (`mission_bo.rs:607-614` — returns `InvalidInput`, NOT a distinct
  not-found/not-unlocked error type; contrast Java's dedicated
  `SgtBackendTargetNotUnlocked`, see §6), mission-limit
  (`MissionBaseService::check_mission_limit_not_reached`), unique-unit check
  (`mission_bo.rs:629-644`), resource+energy `canRun`
  (`mission_bo.rs:646-668`), `checkWouldReachUnitTypeLimit`
  (`mission_bo.rs:679-681`), `ZERO_BUILD_TIME` (`mission_bo.rs:684-689`),
  mission/mission_information/obtained_units INSERTs and
  `user_storage` decrement (`mission_bo.rs:695-`), then the 4-event emit tail
  (`mission_bo.rs:118-124`) matching B15's websocket set.
- `cancel_build_unit` (`mission_bo.rs:132-200`): ownership check, BUILD_UNIT
  branch hard-deletes `obtained_units` by `mission_id`
  (`mission_bo.rs:156-161`), refunds primary/secondary
  (`mission_bo.rs:163-174`), deletes `mission_information`+`missions`
  (`mission_bo.rs:177-184`), deletes the `scheduled_tasks` row directly by
  `task_name='mission-run' AND task_instance=?` (`mission_bo.rs:187-192`) —
  **note:** Java's `abortMissionJob` goes through the db-scheduler `Scheduler.cancel`
  API and swallows a not-found race (B22); the Rust port does a raw `DELETE`
  which is naturally idempotent (0 rows affected, no error) — likely
  equivalent in observable effect but worth a parity scenario for the "task
  already fired concurrently with cancel" race. Emits
  `unit_build_mission_change` + `missions_count_change` + `unit_type_change`
  (`mission_bo.rs:196-198`) — **matches Java's B21 omission of
  `user_data_change`** (both backends skip it on cancel; if this is a bug it's
  a shared one, see §6).
- `process_build_unit` (`mission_bo.rs:342-396`): moves units via
  `crate::bo::mission_processor::move_unit_to_planet` (not read in this pass —
  verify its merge/stacking semantics match `ObtainedUnitBo.moveUnit`'s B15
  exactly, e.g. same 3-branch dispatch), triggers
  `RequirementBo::trigger_unit_build_completed_or_killed` per unit
  (`mission_bo.rs:380-386`, matches B16), deletes the mission
  (`mission_bo.rs:389`, matches B17's hard-delete-not-resolved behavior — good,
  this means the harness's "poll mission row absence" note in §6 applies
  identically to both backends). Comment at `mission_bo.rs:391-394` confirms
  emits (`emitUnitBuildChange`, `emitMissionCountChange`, `emitObtainedUnits`)
  are fired by the **caller** (`UnitMissionBo::run_non_unit_mission`, not
  inspected in this pass) after the tx commits — worth confirming the 500ms
  async delay on `unit_obtained_change` (B18) is or isn't replicated; if Rust
  emits it synchronously post-commit instead of delayed, that is a **timing**
  difference only (both eventually emit the same payload) and the harness's
  ws settle-wait already accounts for it, but it's worth noting Rust may not
  reproduce the *specific* 500ms delay if a scenario ever asserted ordering.
- `ObtainedUnitBo::save_with_subtraction` — referenced from
  `unit.rs:195` (`delete` route) as already wired ("live when those Bo methods
  are available", per the file's own header) — not traced into
  `owge-business` in this pass; recommend a follow-up read of
  `owge-business/src/bo/obtained_unit_bo.rs` (if that's its path) to verify
  B24-B30 parity, especially the B28 authorization-gap question and B30's
  synchronous (not after-commit) emit ordering.
- Temporal units (B37-B39): not searched in this pass — `TemporalUnitsListener`/
  `TemporalUnitScheduleListener` are Spring event-listener-driven in Java with
  no direct REST trigger; find the Rust equivalent (likely inside the
  time-special activation Bo) before drafting §3's temporal scenario.
- `findRunning`/`criticalAttack` (`unit.rs:54-78`, `202-214`): both wired to
  real Bo calls (`MissionFinderBo::find_running_unit_build`,
  `UnitBo::find_used_critical_attack` + `CriticalAttackBo::build_full_information`);
  `find_running`'s empty-body-vs-JSON-empty-string nuance (B31) is explicitly
  handled (`unit.rs:63-65` comment + code matches the Java
  `StringHttpMessageConverter` behavior).

## 6. Open questions / suspected divergences

1. **BUILD_UNIT mission completion never sets `missions.resolved=1`** (B17;
   confirmed identical in Rust, `mission_bo.rs:389`) — the harness's generic
   "nudge `execution_time`, poll `missions.resolved=1`" technique
   (`BDD-PARITY-PLAN.md` §3 technique #1) does not work for this mission type.
   The `When` step implementation for BUILD_UNIT completion must poll for
   **mission row absence** instead (or `obtained_units.mission_id IS NULL`
   for the built stack). Same caveat likely applies to LEVEL_UP (`MissionBo.java:193`
   also does `missionRepository.delete`, never sets `resolved`) — worth
   checking whether *any* mission type in this codebase actually sets
   `resolved=1`, or whether that column is legacy/write-only elsewhere
   (e.g. `unit_mission` combat flows) before the harness's generic technique
   is trusted for other features.
2. **`user_data_change` is not emitted after build cancellation** in either
   backend (Java `MissionCancelBuildService.java:35-45` / Rust
   `mission_bo.rs:196-198`) despite `primary_resource`/`secondary_resource`
   changing. Both backends agree, so `PARITY` would pass, but `JAVA_SPEC`
   might still be "wrong" per the reference spec (§9 pitfall 11: "when Java
   and Rust are both wrong, flag it, don't silently match"). Worth a scenario
   that explicitly asserts absence-then-flags-to-Kevin rather than baking the
   omission in as intended behavior.
3. **B4b NPE on a unit id with no `object_relations` row** — is this
   reachable in practice (are all `units` rows guaranteed a matching
   `object_relations(UNIT, id)` row by an admin-side trigger/constraint, or is
   this a latent 500 waiting for a malformed `unitId` param)? And does the
   Rust port's `ObjectRelationBo::find_one` + `.ok_or_else(NotFound(...))`
   (`mission_bo.rs:604-606`) mean Rust returns a **clean 404** where Java
   would 500 — i.e. Rust is arguably *more correct* here, which would fail
   `JAVA_SPEC` if asserted, or just show up as a `PARITY` diff on the error
   response shape if a scenario ever manufactures this state.
4. **B4 error-type mapping**: Java throws a distinct `SgtBackendTargetNotUnlocked`
   for "not unlocked" vs `SgtBackendInvalidInputException` for most other
   build-time rejections; Rust's `do_register_build_unit` returns
   `OwgeError::InvalidInput` for the not-unlocked case too
   (`mission_bo.rs:610-614`) — same HTTP status (400) but the error-type/
   `exceptionType` string a scenario might assert on will differ. The
   proposed `*NEW*` "failed with error" step (§4) needs an explicit
   Java-exceptionType ↔ Rust-OwgeError-variant mapping table rather than a
   literal string compare, or scenarios must assert on the `I18N_*` message
   body instead (more stable across the two type systems).
5. **B28 possible authorization gap in `/game/unit/delete`**: from reading
   `ObtainedUnitBo.saveWithSubtraction(ObtainedUnitDto, boolean)`
   (`ObtainedUnitBo.java:171-183`) the entity to mutate is loaded purely by
   `dto.getId()` (`findByIdOrDie`), and `dto.userId` (server-stamped) is only
   used for cache-tag/emit targeting, not as a WHERE-clause ownership filter
   — this needs verifying against `findByIdOrDie`'s actual query (not read in
   this pass) before treating it as a confirmed bug; if confirmed, it's a
   spec-level issue per §9 pitfall 11 (flag to Kevin, don't just port the
   hole into Rust). A scenario "user 2 attempts to delete user 1's obtained
   unit id" would settle it either way and should be added regardless of the
   outcome.
6. **B30 / delete-endpoint emits are not wrapped in `doAfterCommit`**
   (`ObtainedUnitBo.java:177-183`) — every other write path in this domain
   defers websocket emission to after-commit; this one doesn't. Not
   necessarily observable as a bug (the method itself is the outermost
   `@Transactional` boundary for a REST call, so "before commit" here likely
   still means "after the DB write is durable from the caller's perspective"
   modulo isolation level), but it's an inconsistency worth a targeted
   `Then` if the harness ever needs strict emission-vs-commit ordering
   guarantees for this endpoint.
7. **`move_unit_to_planet`** (Rust, referenced at `mission_bo.rs:370-376`) was
   not traced in this pass — confirm its 3-way branch (own-planet merge /
   deployed-mission-append / foreign-planet-with-deploy) matches
   `ObtainedUnitBo.moveUnit`'s exact branch conditions
   (`ObtainedUnitBo.java:197-220`) before trusting B15 parity claims above.
8. **Temporal units (B37-B39) have no traced Rust counterpart** in this pass
   — needs its own follow-up read (likely under a time-special activation Bo)
   before Phase 2/3 scenario-writing for `seed_temporal_units.sql`.
9. **Unit-type-limit (B12) has no baseline-unit test fixture** — units 10/11
   belong to types (10, 30) with no configured `max_count`; a scenario needs
   either a new unit ≥9100 on type 1 (max_count 10) or type 33/49, or a
   `Given` that temporarily sets a `max_count`/`share_max_count` on a fresh
   unit type ≥ some reserved range (no reserved range for unit_types is
   documented in `BDD-PARITY-PLAN.md` — flag to Kevin if one is needed).
