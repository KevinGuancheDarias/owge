# missions-travel — Java reference behavior inventory

Domain: **ESTABLISH_BASE, CONQUEST, DEPLOY, RETURN_MISSION** — the travel/ownership unit
missions. ATTACK/COUNTERATTACK/GATHER/EXPLORE combat math is out of scope except where
ESTABLISH_BASE/CONQUEST call into `AttackMissionProcessor` as a dependency (cited, not
re-derived — combat math itself is owned by the attack-parity work, see
`docs/attack-parity-deterministic-rng` memory).

Sources read in full: `game-rest/.../rest/game/MissionRestService.java`,
`game-rest/.../rest/game/PlanetRestService.java`,
`business/.../business/UnitMissionBo.java`,
`business/.../business/mission/unit/registration/UnitMissionRegistrationBo.java` and every
class under `business/.../business/mission/unit/registration/` (`MissionRegistrationObtainedUnitLoader`,
`MissionRegistrationPreparer`, `MissionRegistrationAuditor`, `MissionRegistrationUnitManager`,
`MissionRegistrationInvisibleManager`, `MissionRegistrationOrphanMissionEraser`, and the
`checker/` sub-package: `MissionRegistrationCanDeployChecker`, `MissionRegistrationCanStoreUnitChecker`,
`MissionRegistrationPlanetExistsChecker`, `MissionRegistrationUnitTypeChecker`,
`MissionRegistrationUserExistsChecker`), `business/.../business/mission/unit/registration/returns/ReturnMissionRegistrationBo.java`,
`business/.../business/mission/checker/{CrossGalaxyMissionChecker,EntityCanDoMissionChecker}.java`,
`business/.../business/mission/processor/{EstablishBaseMissionProcessor,ConquestMissionProcessor,
DeployMissionProcessor,ReturnMissionProcessor,AttackMissionProcessor,MissionProcessor}.java`,
`business/.../business/mission/{MissionBaseService,MissionEventEmitterBo,MissionTimeManagerBo,
MissionFinderBo,MissionInterceptionManagerBo}.java`, `business/.../business/mission/report/MissionReportManagerBo.java`,
`business/.../business/{MissionSchedulerService,MissionReportBo,PlanetBo,RequirementBo}.java`,
`business/.../business/unit/ObtainedUnitEventEmitter.java`,
`business/.../business/unit/obtained/ObtainedUnitBo.java`,
`business/.../job/DbSchedulerRealizationJob.java`,
`business/.../enumerations/MissionType.java`, `business/.../GlobalConstants.java`,
`business/.../business/planet/PlanetExplorationService.java`,
`business/.../business/planet/PlanetUtilService.java`, `business/.../business/unit/HiddenUnitBo.java`
(`isHiddenUnit`), plus the Rust port: `rust-backend/owge-rest/src/routes/game/mission.rs`,
`rust-backend/owge-rest/src/routes/game/mod.rs` (planet leave), `rust-backend/owge-business/src/bo/unit_mission_bo.rs`,
`unit_mission_registration_bo.rs`, `return_mission_registration_bo.rs`, `planet_bo.rs`,
`mission_processor/{mod,establish_base,conquest,deploy,return_mission,attack}.rs`, and
`docs/UNPORTED-ENDPOINTS.md`. Live dev-DB ids verified via `docker exec owge_backend_developer-db-1
mysql -uroot -p1234 owge -e "..."` (SELECT-only).

## 1. Endpoints

| HTTP | Path | Controller | Bo entry point |
|---|---|---|---|
| POST | `game/mission/establishBase` | `MissionRestService.establishBase` — `MissionRestService.java:36-39` | `UnitMissionBo.myRegisterEstablishBaseMission` → `adminRegisterEstablishBase` — `UnitMissionBo.java:116-124` |
| POST | `game/mission/conquest` | `MissionRestService.conquest` — `MissionRestService.java:51-54` | `UnitMissionBo.myRegisterConquestMission` → `adminRegisterConquestMission` — `UnitMissionBo.java:152-169` |
| POST | `game/mission/deploy` | `MissionRestService.deploy` — `MissionRestService.java:56-59` | `UnitMissionBo.myRegisterDeploy` → `adminRegisterDeploy` — `UnitMissionBo.java:171-185` |
| POST | `game/mission/cancel?id=` | `MissionRestService.cancel` — `MissionRestService.java:61-65` | `UnitMissionBo.myCancelMission` — `UnitMissionBo.java:187-207` (source of RETURN_MISSION auto-registration on cancel) |
| POST | `game/planet/leave?planetId=` | `PlanetRestService.leave` — `PlanetRestService.java:29-33` | `PlanetBo.doLeavePlanet` — `PlanetBo.java:137-149` (ownership side effect used by/relevant to conquest/establish-base) |
| *(none)* | RETURN_MISSION has **no REST endpoint** — it is only ever auto-registered server-side | — | `ReturnMissionRegistrationBo.registerReturnMission` — `ReturnMissionRegistrationBo.java:33-39`, called from `EstablishBaseMissionProcessor`, `ConquestMissionProcessor`, `AttackMissionProcessor`, `MissionBaseService.retryMissionIfPossible`, `UnitMissionBo.myCancelMission`, `MissionInterceptionManagerBo` (indirectly, via full-interception short-circuit) |
| *(background)* | db-scheduler `mission-run` task fires due missions | `DbSchedulerRealizationJob.execute` (whole file) | `UnitMissionBo.runUnitMission` — `UnitMissionBo.java:215-224` → `missionProcessorMap.get(type).process(...)` |

All three REST endpoints are thin: they set `missionType`, then delegate to the shared
`UnitMissionBo.commonMissionRegister` (`UnitMissionBo.java:304-325`) →
`UnitMissionRegistrationBo.doCommonMissionRegister` (`UnitMissionRegistrationBo.java:42-77`) pipeline.
Per-type differences are only the guard clauses in each `adminRegister*` method (§2 B23-B25) and the
`missionType` value threaded through.

## 2. Behavior catalog

### A. Shared registration pipeline (`commonMissionRegister` → `doCommonMissionRegister`)

Applies to ESTABLISH_BASE, CONQUEST, DEPLOY alike (also EXPLORE/GATHER/ATTACK/COUNTERATTACK, out
of scope). Executed inside `planetLockUtilService.doInsideLockById([sourcePlanetId, targetPlanetId], ...)`
(`UnitMissionBo.java:319-324`) — DB effects below all happen under that lock.

- **B1. User must exist.** `MissionRegistrationUserExistsChecker.checkUserExists` (`.java:19-23`)
  — `UserNotFoundException` if `user_storage` row missing. (Effectively unreachable via REST since
  the JWT-authenticated user always exists; reachable via the admin `adminRegister*` paths.)
- **B2. Mission-limit guard.** `UnitMissionBo.commonMissionRegister` (`.java:307-310`): calls
  `missionBaseService.checkMissionLimitNotReached(user)` **unless** this is a DEPLOY mission whose
  target planet is already the user's own property (`planetRepository.isOfUserProperty`). If
  `countByUserIdAndResolvedFalse(user) + 1 >= factionImprovement.moreMissions + 1`
  (`MissionBaseService.java:77-83`, `findUserMaxAllowedMissions` L103-105) throws
  `SgtBackendInvalidInputException("I18N_ERR_MISSION_LIMIT_EXCEEDED")`.
- **B3. Target must be explored (non-EXPLORE types).** `UnitMissionBo.java:314-318`:
  `planetExplorationService.isExplored(userId, targetPlanetId)` (`PlanetExplorationService.java:29-32`
  — true if owned by user OR an `explored_planets` row exists) else
  `SgtBackendInvalidInputException("Can't send this mission, because target planet is not explored")`.
- **B4. Global DEPLOY kill-switch.** `MissionRegistrationCanDeployChecker.checkDeployedAllowed`
  (`.java:30-35`): if `missionType==DEPLOY` and `configuration` row `findDeployMissionConfiguration()`
  resolves to `DISALLOWED`, throws `SgtBackendInvalidInputException`.
- **B5. Source/target planet must exist.** `MissionRegistrationPlanetExistsChecker.checkPlanetExists`
  (`.java:18-22`), called for both source and target
  (`MissionRegistrationObtainedUnitLoader.java:45-46`) — `PlanetNotFoundException` per missing id.
- **B6. `involvedUnits` must be non-empty.** `MissionRegistrationObtainedUnitLoader.java:48-50` —
  `SgtBackendInvalidInputException("involvedUnits can't be empty")`.
- **B7. No repeated `(unitId, expirationId)` across top-level units AND their `storedUnits`.**
  `checkRepeatedUnitAndAdd` (`.java:88-94`) — `SgtBackendInvalidInputException("I18N_ERR_REPEATED_UNIT")`.
- **B8. Each selected unit needs an explicit `count`.** `handleSelectedUnit` (`.java:104-106`) —
  `SgtBackendInvalidInputException("No count was specified for unit " + id)` if `count == null`.
- **B9. Obtained-unit resolution, ownership-aware.** `handleSelectedUnit` (`.java:107-109`) calls
  `ObtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission` (`ObtainedUnitBo.java:232-252`):
  if the source planet is **not** the user's property, looks up a `DEPLOYED`-mission stack
  (`findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed`) — i.e. this is how a user launches a
  new travel mission *from* units they already have parked on someone else's planet; else looks up a
  mission-less stack on their own planet. `NotFoundException("...nice try, dirty hacker!")` if absent.
- **B10. Deploy-after-deploy rule.** `MissionRegistrationCanDeployChecker.checkUnitCanDeploy`
  (`.java:44-57`), only for the top-level selected unit (`checkCanDeploy=true` arg,
  `MissionRegistrationObtainedUnitLoader.java:110-112`; **not** applied to nested `storedUnits`):
  if `configuration` is `ONLY_ONCE_RETURN_SOURCE`/`ONLY_ONCE_RETURN_DEPLOYED`, the target isn't the
  user's own planet, the source stack's current mission type is `DEPLOYED`, and the requested mission
  is itself `DEPLOY` → `SgtBackendInvalidInputException("You can't do a deploy mission after a deploy
  mission")`. (A DEPLOYED stack can still be redirected via ESTABLISH_BASE/CONQUEST/ATTACK — only
  DEPLOY→DEPLOY is blocked here.)
- **B11. Stored-unit carry rule.** `MissionRegistrationCanStoreUnitChecker.checkCanStoreUnit`
  (`.java:34-38`) — for each `storedUnits[]` entry, requires a `UNIT_STORES_UNIT` rule between the
  carrier and cargo unit types, else `SgtBackendInvalidInputException("I18N_CANT_STORE_UNIT")`.
- **B12. Total stored weight cap.** `checkTotalHeight` (`MissionRegistrationObtainedUnitLoader.java:75-86`)
  — `sum(storedUnit.count * storedWeight) > carrierCount * storageCapacity` →
  `SgtBackendInvalidInputException("I18N_ERR_MAX_WEIGHT_OVERPASSED")`.
- **B13. Source stack subtraction, with orphan-DEPLOYED marking.**
  `ObtainedUnitBo.saveWithSubtraction` (`.java:148-169`): subtracting more than available →
  `SgtBackendInvalidInputException`; subtracting negative → same; exact-count subtraction deletes the
  row and returns `null`. `MissionRegistrationObtainedUnitLoader.handleSelectedUnit` (`.java:113-118`):
  when the subtraction empties a stack **whose current mission is `DEPLOYED`**, that mission id is
  collected into `deletedMissions`, then after the whole `involvedUnits` loop
  `MissionRegistrationOrphanMissionEraser.doMarkAsDeletedTheOrphanMissions` (`.java:19-27`) re-queries
  `obtained_units` per candidate mission id and sets `missions.resolved=1` for any that now have zero
  rows. **This is the exact Java-side counterpart of the historical Rust crash** fixed in commit
  `83a0ab9a` (`mark_orphan_missions_resolved` — see §5/§6): registering a DEPLOY (or any travel
  mission) that fully empties a pre-existing DEPLOYED stack must resolve that now-empty DEPLOYED
  "mission" row at **registration** time, not execution time.
- **B14. Mission row persisted + audit.** `MissionRegistrationPreparer.prepareMission`
  (`.java:24-42`) inserts a `missions` row (`starting_date=now UTC`, `required_time` from
  `MissionTimeManagerBo.calculateRequiredTime` — a per-`MissionType` config value, `termination_date`
  computed from it). `MissionRegistrationAuditor.auditMissionRegistration` (`.java:20-34`) writes an
  `AuditActionEnum.REGISTER_MISSION` audit row (targeted at the target-planet owner if it isn't the
  invoker), and for DEPLOY additionally audits `USER_INTERACTION`/`"DEPLOY"` against every **other**
  user who currently has units in-or-headed-to the target planet
  (`obtainedUnitFinderBo.findInPlanetOrInMissionToPlanet`).
- **B15. Unit-type / mission-type support gate.** `MissionRegistrationUnitTypeChecker.checkUnitsCanDoMission`
  (`.java:22-48`) via `EntityCanDoMissionChecker.canDoMission` (`.java:28-44`, reflective
  `getCan<MissionType>()` on the unit type, `MissionSupportEnum.ANY|OWNED_ONLY|NONE` —
  `OWNED_ONLY` requires the **target** planet to already belong to the invoker) →
  `SgtBackendInvalidInputException` naming the offending unit type if any unit fails.
- **B16. Speed-impact-group per-mission-type gate.** Same file, `.java:39-47`: for units with no
  `ownerUnit` (i.e. not a carried/stored unit), resolves the applicable `SpeedImpactGroup` and
  re-runs `EntityCanDoMissionChecker.canDoMission` against it; failing units are named in the
  exception message.
- **B17. Cross-galaxy gate.** `CrossGalaxyMissionChecker.checkCrossGalaxy` (`.java:26-38`) — only
  when `sourcePlanet.galaxy != targetPlanet.galaxy`: for each top-level unit's `SpeedImpactGroup`
  (falling back to its unit type's group), `doCheckSpeedImpactIfNotNull` (`.java:40-60`) requires (a)
  `EntityCanDoMissionChecker.canDoMission` for that group+missionType and (b) the invoking user has an
  `unlocked_relation` row for that `SPEED_IMPACT_GROUP`'s `object_relations` entry — else
  `SgtBackendInvalidInputException("Don't try it.... you can't do cross galaxy missions...")`.
- **B18. Required-time (re)computation.** `MissionTimeManagerBo.handleMissionTimeCalculation`
  (`.java:44-68`) recomputes `mission.required_time`/`termination_date` from the slowest non-fixed-speed
  unit's speed (adjusted by the user's SPEED improvement) and per-mission-type/route
  `MISSION_SPEED_*` configuration (same-quadrant/diff-quadrant/diff-sector/diff-galaxy multipliers +
  move-cost-per-hop). Skipped entirely if every involved unit has a **fixed** speed-impact group.
  `handleCustomDuration` (`.java:70-75`) then overrides `required_time` upward if the caller passed a
  larger `wantedTime`.
- **B19. Invisible-mission flag.** `MissionRegistrationInvisibleManager.handleDefineMissionAsInvisible`
  (`.java:20-24`): `missions.invisible = true` iff **every** involved unit is hidden for its owner
  (`HiddenUnitBo.isHiddenUnit`, cache-backed).
- **B20. Scheduling + websocket emits on commit.** `missionSchedulerService.scheduleMission`
  (`MissionSchedulerService.java:38-44`) inserts the db-scheduler row (`mission-run` task,
  `execution_time = now + required_time - 2s`). Then (`UnitMissionRegistrationBo.java:69-76`):
  - `missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission)` (`MissionEventEmitterBo.java:31-34`)
    → always emits `unit_mission_change` to the invoker (`.java:59-66`), and additionally
    `enemy_mission_change` to the **target** planet's owner iff the mission is not `invisible`
    (`.java:72-78`, `44-49`).
  - `obtainedUnitEventEmitter.emitObtainedUnitsAfterCommit(user)` (`ObtainedUnitEventEmitter.java:28-30`,
    `UNIT_OBTAINED_CHANGE` = `"unit_obtained_change"`) — **only** if the invoker currently owns the
    **source** planet (`UnitMissionRegistrationBo.java:70-72`) — i.e. launching a mission out of a
    foreign-planet DEPLOYED stack does **not** emit `unit_obtained_change` to the invoker.
  - If the source planet is enemy-owned (`isEnemyPlanet`, `PlanetUtilService.java:10-12`):
    `missionRegistrationInvisibleManager.maybeUpdateMissionsVisibility(alteredVisibilityMissions)`
    (`.java:26-36`) recomputes `.invisible` for every **other** mission the moved stacks belonged to
    (their visibility may have flipped now that a chunk of the stack left), and
    `missionEventEmitterBo.emitEnemyMissionsChange(sourcePlanet.owner)` notifies that foreign owner.

### B. Type-specific registration guards

- **B21. CONQUEST — reject own planet.** `UnitMissionBo.adminRegisterConquestMission` (`.java:159-163`):
  `planetBo.myIsOfUserProperty(targetPlanetId)` → `SgtBackendInvalidInputException`
  ("...unless your population hates you...").
- **B22. CONQUEST — reject home planet.** Same method (`.java:164-167`):
  `planetBo.isHomePlanet(targetPlanetId)` → `SgtBackendInvalidInputException`
  ("...would you like a bandit to steal in your own home??!"). Checked at **registration** time; the
  processor (B33) re-checks it at **execution** time too (home ownership can't change in between in
  practice, but the guard is defense-in-depth / shared with the max-planets-style failure path).
- **B23. DEPLOY — reject same-planet deploy.** `UnitMissionBo.adminRegisterDeploy` (`.java:178-183`):
  `sourcePlanetId.equals(targetPlanetId)` → `SgtBackendInvalidInputException("I18N_ERR_DEPLOY_ITSELF")`.

### C. Mission dispatch (background)

- **B24. `DbSchedulerRealizationJob.execute`** (whole file): loads the mission; no-ops silently if
  already `resolved`; routes unit-mission types to `UnitMissionBo.runUnitMission`. On **any**
  exception from the processor: catches, calls `MissionBaseService.retryMissionIfPossible` (below),
  emits `unit_mission_change` (`emitUnitMissions`) + `enemy_mission_change` (`emitEnemyMissionsChange`)
  to the mission's user regardless of outcome, and (if `PessimisticLockingFailureException`) logs
  InnoDB/process diagnostics.
- **B25. Retry ladder.** `MissionBaseService.retryMissionIfPossible` (`.java:42-64`): attempts
  `< 3` → increments `attemps`, recomputes `termination_date`, saves an error `MissionReport`
  (`buildCommonErrorReport`, `.java:85-95`), reschedules via `MissionSchedulerService.scheduleMission`.
  Attempts `>= 3` for a **unit mission** → `returnMissionRegistrationBo.registerReturnMission(mission,
  null)` (§H, B43) and `mission.resolved = true` — i.e. a mission that keeps throwing eventually
  self-heals into a RETURN_MISSION rather than being stuck forever.
- **B26. Planet-lock superset acquisition.** `UnitMissionBo.resolvePlanetsToLock`
  (`.java:226-251`): locks source + target planet **and every other planet owned by either owner**,
  in one acquisition, specifically to avoid a nested per-user `RequirementBo →
  UserPlanetLockService.runLockedForUser` lock-ordering deadlock. Retried up to
  `CannotAcquireLockException` via `@Retryable` (`.java:216`).

### D. Interception (applies to ESTABLISH_BASE / CONQUEST / DEPLOY, **not** RETURN_MISSION)

`MissionInterceptionManagerBo.loadInformation` (`.java:26-53`) runs before every non-RETURN_MISSION
processor:

- **B27. Full interception.** If **all** involved units are intercepted
  (`unitInterceptionFinderBo.checkInterceptsSpeedImpactGroup`), the mission short-circuits entirely:
  `handleMissionInterception` (`.java:66-74`) marks `resolved=true`, saves a
  `reportFullMissionInterception` report (`InterceptionInformation` only — no
  establish/conquest/deploy-specific report content), deletes the intercepted `obtained_units` rows,
  and notifies interceptor users (`sendReportToInterceptorUsers`). `missionEventEmitterBo
  .emitLocalMissionChangeAfterCommit(mission)` fires (`UnitMissionBo.java:290`). The type-specific
  processor (`EstablishBaseMissionProcessor.process` etc.) is **never invoked**.
- **B28. Partial interception.** Some but not all units intercepted: those units are deleted, the
  processor still runs on the survivors, and `maybeAppendDataToMissionReport`
  (`MissionInterceptionManagerBo.java:55-64`) folds interception info + the original full involved-unit
  list into the processor's report, plus a separate `sendReportToInterceptorUsers` notification.

### E. ESTABLISH_BASE execution (`EstablishBaseMissionProcessor.process`, `.java:34-60`)

- **B29. Defensive-attack trigger.** `AttackMissionProcessor.triggerAttackIfRequired`
  (`AttackMissionProcessor.java:54-62`): if attack-trigger-on-establish is config-enabled AND the
  target planet currently has visible units belonging to the invoker's enemies (from the invoker's
  perspective — `obtainedUnitRepository.areUnitsInvolved`), runs a full `processAttack` first
  (`survivorsDoReturn=false`, `isTriggeredByEvent=true`). If that attack removes the whole mission
  (`isRemoved`), `EstablishBaseMissionProcessor.process` returns `null` — the establish-base outcome
  never happens, only the attack report exists.
- **B30. Target already owned (mid-flight change or was never free).** `.java:41-49`: if
  `targetPlanet.getOwner() != null`, registers a RETURN_MISSION (B-D reuse) and reports
  `"I18N_ALREADY_HAS_OWNER"`. This is the "target changed owner mid-flight" case explicitly named in
  the task brief: nothing at registration time prevented sending the mission to an unowned planet that
  another player's concurrent ESTABLISH_BASE/CONQUEST claims first.
- **B31. Faction max-planets reached.** `.java:42-48`: `planetBo.hasMaxPlanets(user)`
  (`PlanetBo.java:130-135`, `userPlanets >= faction.maxPlanets`) → return mission registered, report
  `GlobalConstants.MAX_PLANETS_MESSAGE` (`"I18N_MAX_PLANETS_EXCEEDED"`, `GlobalConstants.java:8`).
  Checked **only at execution time**, never at registration — a user can register several
  simultaneous ESTABLISH_BASE missions that will all fail this check if they resolve after the user
  hits the cap.
- **B32. Success.** `.java:50-52`: `builder.withEstablishBaseInformation(true)`,
  `planetBo.definePlanetAsOwnedBy(user, involvedUnits, targetPlanet)` (§F). `mission.resolved=true`;
  `missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission)` fires in **every** branch of this
  processor (`.java:55`) — success, already-owned, and max-planets alike.

### F. CONQUEST execution (`ConquestMissionProcessor.process`, `.java:44-93`)

- **B33. Always runs a real attack.** `.java:53`: `attackMissionProcessor.processAttack(mission,
  survivorsDoReturn=false, isTriggeredByEvent=false)` against whatever is currently on the target
  planet, unconditionally (unlike establish-base's conditional trigger).
- **B34. Old-owner/alliance defeat calculation.** `.java:57-63`: if `oldOwner == null`, both
  `isOldOwnerDefeated` and `isAllianceDefeated` are trivially `true` (B-number: **conquest of an
  unowned planet auto-passes the defeat checks**, `.java:57-60`). Else
  `calculateIsOldOwnerDefeated` (`.java:104-108`, old owner has zero surviving units post-attack) and
  `calculateIsAllianceDefeated` (`.java:95-102`, old owner defeated AND every alliance-mate present in
  the attack also has zero survivors — `oldOwner.alliance==null` short-circuits to `true`).
- **B35. Failure branches.** `isFailedConquest` (`.java:110-112`) = `!oldOwnerDefeated ||
  !allianceDefeated || maxPlanets || isHomePlanet(targetPlanet)` (home-planet re-checked here even
  though B22 already blocked it at registration — defense in depth against a home-status flip). On
  failure: if the attack didn't wipe the mission (`!attackInformation.isRemoved()`), registers a
  RETURN_MISSION for the survivors; report reason picked in priority order maxPlanets →
  ownerNotDefeated → allianceNotDefeated → cantConquerHomePlanet (`appendConquestInformation`,
  `.java:114-126`).
- **B36. Success.** `.java:71-87`: `planetBo.definePlanetAsOwnedBy(...)` (§F), report
  `"I18N_PLANET_IS_NOW_OURS"`. If the planet had a special location **and** an old owner, re-triggers
  `requirementBo.triggerSpecialLocation(oldOwner, specialLocation)` (revoke path — this is the
  conquest half of the special-location unlock bug scenario in §6.1 of the plan doc). If there was an
  old owner: `planetBo.emitPlanetOwnedChange(oldOwner)`, cancels the old owner's unresolved
  `BUILD_UNIT` mission on that planet if any (`findUnitBuildMission` +
  `missionCancelBuildService.cancel`), `missionEventEmitterBo.emitEnemyMissionsChange(oldOwner)`, and
  saves a **second**, enemy-flagged `MissionReport` to the old owner
  (`"I18N_YOUR_PLANET_WAS_CONQUISTED"`).
- **B37. Local-mission-change emit is conditional here** (unlike establish-base's unconditional
  emit): `.java:89-91`, `emitLocalMissionChangeAfterCommit` fires **unless** the survivors are
  returning (i.e. it fires on success and on "attack wiped the mission" failure, but not on
  "survivors are heading home" failure — that path's `unit_mission_change` comes from the RETURN_MISSION
  registration's own emit, B41, instead).

### G. DEPLOY execution (`DeployMissionProcessor.process`, `.java:40-69` + `ObtainedUnitBo.moveUnit`, `.java:197-220`)

Deploy produces **no** `MissionReport` (`process` returns `null`).

- **B38. Per-unit `moveUnit` — lands on own planet.** If the target planet is (now) the user's
  property: `saveWithAdding` merges into an existing mission-less stack of the same
  unit+expiration on that planet (or inserts fresh), and the moved row's `mission`/`target_planet`/
  `owner_unit` are cleared (`.java:201-207`) — it becomes an ordinary planet-resident stack, not a
  mission.
- **B39. Per-unit `moveUnit` — already part of a DEPLOYED mission, staying foreign.** If not owned
  and the unit's *current* mission is already `DEPLOYED`: it's just re-saved as-is (`.java:208-209`)
  — this is the "unit was already parked here, nothing to attach" fast path (reachable when the same
  underlying stack is re-targeted at its current DEPLOYED planet, a no-op merge).
- **B40. Per-unit `moveUnit` — not owned, not already DEPLOYED: create-or-merge into a DEPLOYED
  mission for (user, target planet).** `.java:210-217`, via `saveWithAdding` (merges into an existing
  **DEPLOYED** stack of the same unit+expiration already sitting on that planet, if one exists) then
  `MissionFinderBo.findDeployedMissionOrCreate` (`.java:37-61`): if none exists, inserts a new
  `DEPLOYED` mission (`type=12`, `resolved` defaults false, no schedule — it's a passive marker,
  never fired by the scheduler) and attaches the unit to it; if one already exists (same user +
  target planet + unresolved), the unit is appended to that mission instead — **stacks merge into
  one DEPLOYED mission per (user, target planet), never duplicate.**
- **B41. Deployed-mission `invisible` recompute.** `.java:50-55`: after moving all units, if the
  (first altered unit's) mission is non-null, recomputes its `invisible` flag from
  `HiddenUnitBo.isHiddenUnit` across **all** of that DEPLOYED mission's current involved units (not
  just the ones just moved — picks up units other missions may have added to the same DEPLOYED stack
  concurrently).
- **B42. Post-commit emits, conditional on final ownership.** `.java:57-67`, inside
  `transactionUtilService.doAfterCommit`: refreshes each altered unit entity; **only if** the user
  now owns the mission's `target_planet` (i.e. the B38 "landed at home" branch happened for at least
  the representative unit — checked via `mission.getTargetPlanet().getOwner()`, note: this re-checks
  against the *original* `mission` target, not per-unit outcome):
  `obtainedUnitEventEmitter.emitObtainedUnits(user)` (`unit_obtained_change`) and — in a **new**
  `REQUIRES_NEW` transaction — `requirementBo.triggerUnitBuildCompletedOrKilled(user, units)`
  (HAVE_UNIT / UNIT_AMOUNT re-evaluation, `RequirementBo.java:188-200`). Regardless of ownership,
  `missionEventEmitterBo.emitLocalMissionChange(mission, user.getId())` always fires
  (`unit_mission_change` + conditional `enemy_mission_change`, `.java:66`).

### H. RETURN_MISSION registration (`ReturnMissionRegistrationBo`, `.java:33-61`) — always auto

- **B43. Auto-registration shape.** `doRegisterReturnMission` (`.java:42-60`): new `missions` row
  copying `source_planet`/`target_planet`/`user`/`invisible` from the **origin** mission (not
  swapped — RETURN_MISSION's `source_planet` stays the origin mission's source, i.e. "home"), with
  `required_time` = caller-supplied override or the origin's own `required_time` (both
  `EstablishBaseMissionProcessor`/`ConquestMissionProcessor` pass `null` → reuse the same duration
  the outbound trip took; `myCancelMission`, B44, computes an explicit remaining-time override
  instead), `related_mission` FK back to the origin. **All** `obtained_units` currently linked to the
  origin mission are re-parented (`mission_id`) onto the new RETURN_MISSION row — this is why a
  RETURN_MISSION always has exactly the survivors of whatever mission spawned it. Scheduled
  immediately (`missionSchedulerService.scheduleMission`); `emitLocalMissionChangeAfterCommit` fires
  for the new RETURN_MISSION.
- **B44. `myCancelMission` → RETURN_MISSION with adjusted duration.** `UnitMissionBo.java:187-207`:
  only the mission's own user may cancel (`SgtBackendInvalidInputException` otherwise); a
  RETURN_MISSION itself cannot be cancelled (`"can't cancel return missions"`); else marks the
  cancelled mission `resolved=true` and registers a return with `requiredTime = originMission
  .requiredTime - max(0, secondsRemainingUntilTermination)` — i.e. the return trip is shortened by
  however much of the outbound trip had already elapsed (floor 0).

### I. RETURN_MISSION execution (`ReturnMissionProcessor.process`, `.java:38-63`)

- **B45. Moves every involved unit home via `moveUnit(..., mission.getSourcePlanet())`.** Same
  3-way branch as DEPLOY (B38-B40), landing on `mission.source_planet` — the **original outbound
  mission's** source planet, i.e. truly "home", even through however many hops the origin mission
  itself represented. If the user no longer owns that planet by the time the return lands, the
  fallback is the same DEPLOYED-mission-creation path as DEPLOY's foreign branch (moveUnit doesn't
  distinguish "returning" from "deploying" — it only looks at current ownership of the destination).
- **B46. `mission.resolved=true` + unconditional `emitLocalMissionChangeAfterCommit`.** `.java:47-48`
  — unlike DEPLOY (B42, conditional `unit_obtained_change`) and unlike CONQUEST (B37, conditional on
  survivors-returning), RETURN_MISSION always emits `unit_mission_change`/`enemy_mission_change`
  immediately.
- **B47. Delayed (500 ms) async post-processing.** `.java:49-60`, `asyncRunnerBo
  .runAsyncWithoutContextDelayed`: **only if** the mission's `source_planet` is still owned by the
  returning user, re-triggers `requirementBo.triggerUnitBuildCompletedOrKilled` +
  `triggerUnitAmountChanged` per distinct returned unit type. `obtainedUnitEventEmitter
  .emitObtainedUnits(user)` (`unit_obtained_change`) fires **unconditionally** in this same delayed
  block, regardless of final ownership — this differs from DEPLOY, where the `unit_obtained_change`
  emit itself (not just the requirement re-trigger) is gated on ownership (B42). Being on a detached
  async thread, this emit is **not** part of the firing transaction/commit ordering the same way the
  other emits are — a scenario asserting on it needs the settle-wait (§9.6 of the plan doc) to cover
  at least 500 ms + emission latency.

### J. Ownership side effects (`PlanetBo`)

- **B48. `definePlanetAsOwnedBy`** (`.java:177-200`), called from B32 and B36 (establish-base/conquest
  success) — never from Return or from Deploy-to-own-planet (those use the narrower `moveUnit`
  directly): sets `targetPlanet.owner = newOwner`; every unit in `involvedUnits` (the units that
  performed the mission) lands directly on the planet (`source_planet=target`, `target_planet=null`,
  `mission=null`) — no separate `moveUnit` call for these, they're written inline. **Separately**,
  re-homes any of the **new owner's own** pre-existing DEPLOYED units already sitting on this planet
  (`findByUserIdAndTargetPlanetAndMissionTypeCode(owner, target, DEPLOYED)`) via `moveUnit`, then
  deletes their now-empty `DEPLOYED` mission row outright (not via the orphan-eraser path — deleted
  unconditionally once re-homed). `maybeTriggerSpecialLocation(target, owner)` grants
  `HAVE_SPECIAL_LOCATION`-gated relations to the **new** owner if the planet has a
  `special_location_id` (`RequirementBo.triggerSpecialLocation`, `.java:222-226` →
  `processRelationList`, `.java:281-310` → `registerObtainedRelation`, `.java:412-443`, which emits
  `unit_unlocked_change`/`time_special_unlocked_change`/`speed_impact_group_unlocked_change` as
  applicable, `.java:502-510`). Finally emits `planet_owned_change` (to new owner),
  `enemy_mission_change` (to new owner — their own missions targeting/leaving this planet may have
  changed visibility), `unit_obtained_change` (to new owner), and (post-commit)
  `planetListBo.emitByChangedPlanet(target)`.
- **B49. `doLeavePlanet` (`game/planet/leave`)** (`.java:137-149`): guarded by `canLeavePlanet`
  (`.java:163-167`) = not home planet, invoker is current owner, invoker has **no** mission-less
  units sitting on the planet, and no running `BUILD_UNIT` mission there — else
  `SgtBackendInvalidInputException("ERR_I18N_CAN_NOT_LEAVE_PLANET")`. On success: `owner=NULL`;
  `maybeTriggerSpecialLocation(planet, formerOwner)` re-evaluates (revokes) that user's
  `HAVE_SPECIAL_LOCATION` unlocks if the planet had one; emits `planet_owned_change` and (post-commit)
  `planetListBo.emitByChangedPlanet`. Note the units-present guard means a planet can only be
  abandoned once it's fully empty of the owner's mission-less stock — a DEPLOYED stack belonging to
  *another* user sitting on it does **not** block leaving (only the *owner's own* mission-less units
  count, per `obtainedUnitRepository.hasUnitsInPlanet`).

## 3. Draft Gherkin scenarios

Concrete ids used (verified live 2026-07-09, see queries in the sources note): user 1 home = planet
1002 (galaxy 1), user 1 also owns planet 1003 (galaxy 1, non-home); user 2 home = planet 1004
(galaxy 1); planet 1234 = unowned, no special location, galaxy 1 (the plan's canonical target);
planet 2282 = unowned, galaxy 2 (cross-galaxy target); unit 10 = X-302 (speed 7, `speed_impact_group_id`
2); unit 11 = BC-303 (speed 7, no fixed group). Missions table currently at max id ~900206 live — any
fixed-mission-id (`W3`) scenario in this domain should pick ids ≥ 901000 to stay clear of prior
harness runs. Steps marked `*NEW*` are not yet in the §6 catalog of the plan doc — see §4.

```gherkin
Feature: Establish base
  Background:
    Given the standard test universe
    And user 1 has 5 units of id 10 on planet 1002

  Scenario: Establishing a base on a free planet succeeds
    # Covers B1,B3,B5-B9,B13-B20,B29(no-op, no defenders),B32,B48
    When user 1 runs an ESTABLISH_BASE mission from planet 1002 to planet 1234 with 5 units of id 10
    Then planet 1234 is owned by user 1
    And user 1 has 5 units of id 10 on planet 1234
    And user 1 received websocket event "unit_mission_change"
    And user 1 received websocket event "planet_owned_change"

  Scenario: Establishing a base on an already-owned planet returns the mission
    # Covers B30, B43 (return auto-registration), B32's unconditional emit
    Given planet 1234 is owned by user 2
    When user 1 runs an ESTABLISH_BASE mission from planet 1002 to planet 1234 with 5 units of id 10
    Then planet 1234 is owned by user 2
    And user 1 has 5 units of id 10 on planet 1002
    And user 1 received websocket event "unit_mission_change"

Feature: Conquest
  Background:
    Given the standard test universe
    And user 1 has 5 units of id 10 on planet 1002

  Scenario: Conquering an unowned planet auto-passes the defeat checks
    # Covers B33, B34 (oldOwner==null branch), B35(not taken), B36, B48
    When user 1 runs a CONQUEST mission from planet 1002 to planet 1234 with 5 units of id 10
    Then planet 1234 is owned by user 1
    And user 1 received websocket event "planet_owned_change"

  Scenario: Conquest against a defended planet fails and the survivors return
    # Covers B35 (failure, not-defeated branch), B43 (return registration),
    # B37 (LocalMissionChange NOT fired directly, comes via the return registration instead)
    Given planet 1234 is owned by user 2
    And user 2 has 5 units of id 11 on planet 1234
    When user 1 runs a CONQUEST mission from planet 1002 to planet 1234 with 5 units of id 10
    Then planet 1234 is owned by user 2

Feature: Deploy
  Background:
    Given the standard test universe
    And user 1 has 5 units of id 10 on planet 1002

  Scenario: Deploying to your own planet lands the stack directly, no DEPLOYED mission
    # Covers B4, B23(not-triggered: different planets), B38
    When user 1 runs a DEPLOY mission from planet 1002 to planet 1003 with 5 units of id 10
    Then user 1 has 5 units of id 10 on planet 1003
    And table missions has no row where user_id=1 and type_code=DEPLOYED and target_planet=1003

  Scenario: Deploying to a foreign planet creates a DEPLOYED stack
    # Covers B40, B41 (invisible recompute), B42 (unit_obtained_change withheld since not owner)
    When user 1 runs a DEPLOY mission from planet 1002 to planet 1234 with 5 units of id 10
    Then table missions has a row where user_id=1 and type_code=DEPLOYED and target_planet=1234
    And user 1 received websocket event "unit_mission_change"

  Scenario: A second deploy to the same foreign planet merges into the existing DEPLOYED mission
    # Covers B40 (merge branch) — no second DEPLOYED mission row created
    Given user 1 has 3 units of id 11 on planet 1002
    And user 1 runs a DEPLOY mission from planet 1002 to planet 1234 with 5 units of id 10
    When user 1 runs a DEPLOY mission from planet 1002 to planet 1234 with 3 units of id 11
    Then user 1 has 5 units of id 10 on planet 1234
    And user 1 has 3 units of id 11 on planet 1234
    # *NEW* step needed: assert exactly one DEPLOYED mission row for (user, target) — see §4

  Scenario: Redeploying an emptied DEPLOYED stack does not crash registration
    # Covers B13 (orphan-DEPLOYED marking at registration time) — this is the exact shape of the
    # historical Rust crash (commit 83a0ab9a): DEPLOY that fully empties a DEPLOYED stack tied to
    # an existing DEPLOYED mission, then a further DEPLOY re-using the same (user,target) pair.
    Given user 1 runs a DEPLOY mission from planet 1002 to planet 1234 with 5 units of id 10
    # the 5 units of id 10 on planet 1234 are now a DEPLOYED stack
    When user 1 runs a DEPLOY mission from planet 1234 to planet 1002 with 5 units of id 10
    Then user 1 has 5 units of id 10 on planet 1002
    And table missions has a row where user_id=1 and type_code=DEPLOYED and target_planet=1234 and resolved=1

Feature: Return mission
  Background:
    Given the standard test universe
    And user 1 has 5 units of id 10 on planet 1002

  Scenario: Cancelling an in-flight establish-base mission auto-registers a return
    # Covers B44 (cancel -> return with adjusted duration), B43
    Given user 1 runs an ESTABLISH_BASE mission from planet 1002 to planet 1234 with 5 units of id 10
    # *NEW* step needed: "user {u} cancels their latest mission" (POST game/mission/cancel) — see §4
    When user 1 cancels their latest mission
    Then table missions has a row where user_id=1 and type_code=RETURN_MISSION and target_planet=1234

  Scenario: A completed return mission lands units back home
    # Covers B45 (own-planet branch), B46, B47
    Given user 1 runs a DEPLOY mission from planet 1002 to planet 1234 with 5 units of id 10
    # *NEW* step needed: "user {u} runs a DEPLOY mission ... and it is immediately recalled" or reuse
    # cancel on the DEPLOYED mission — deploy has no direct cancel path today; see §6 open question.
    When the RETURN_MISSION mission of user 1 completes
    Then user 1 has 5 units of id 10 on planet 1002

Feature: Leave planet
  Background:
    Given the standard test universe
    And planet 1234 is owned by user 1

  Scenario: Leaving a planet you fully occupy relinquishes it
    # Covers B49 success path
    When user 1 leaves planet 1234
    Then planet 1234 has no owner
    And user 1 received websocket event "planet_owned_change"

  Scenario: Leaving is refused while your own units still sit on the planet
    # Covers B49 guard (canLeavePlanet == false)
    Given user 1 has 1 unit of id 10 on planet 1234
    When user 1 leaves planet 1234
    Then planet 1234 is owned by user 1
```

Coverage map (B-number → scenario): B1/B3/B5-B9/B13-B20/B29/B32/B48 → "Establishing a base on a free
planet succeeds". B30/B43 → "already-owned planet". B33/B34/B36/B48 → "auto-passes defeat checks".
B35/B37/B43 → "defended planet fails". B4/B23/B38 → "own planet, no DEPLOYED mission". B40/B41/B42 →
"foreign planet creates DEPLOYED stack". B40 (merge) → "second deploy merges". B13 → "redeploying an
emptied stack". B44/B43 → "cancel auto-registers return". B45/B46/B47 → "completed return lands
home". B49 → both "leave planet" scenarios. Not covered by a scenario above (would need combat/RNG
or config-flip fixtures, left to Phase 3 per the plan's roadmap): B2 (mission-limit), B10-B12
(deploy-after-deploy / stored-unit rules), B17 (cross-galaxy speed-impact unlock), B21/B22 (own/home
conquest registration guards — trivial REST 4xx checks, low value as parity scenarios), B24-B28
(scheduler retry/interception — needs a forced-failure or interceptor fixture), B31 (max-planets —
needs faction cap manipulation), B39 (already-DEPLOYED-staying-foreign fast path).

## 4. Proposed new steps

These are **quarantined** — not yet in the plan doc's §6 catalog. Do not wire them into the driver
until reconciled with Kevin / added to `BDD-PARITY-PLAN.md` §6.

| Step text | Why needed | Implementation notes |
|---|---|---|
| `user {u} cancels their latest mission` | §6.3 has no step for `POST game/mission/cancel`; needed to exercise B44 (cancel → adjusted-duration return) without waiting for a mission to fail 3x. | Find the user's most recently created unresolved mission (`ORDER BY id DESC LIMIT 1` scoped to `context.created_missions`, not a blind DB query) and `POST game/mission/cancel?id=<id>` with the user's JWT. Assert 2xx like W1. |
| `table missions has a row where user_id={u} and type_code={type} and target_planet={pid}` (and `and resolved={0\|1}`) | The existing generic escape hatch (§6.4) is scoped to a table+column whitelist that does not include `missions`, and `type_code` requires a join to `mission_types` — worth a dedicated step rather than stretching the generic one. | `SELECT m.* FROM missions m JOIN mission_types mt ON mt.id=m.type WHERE m.user_id=? AND mt.code=? AND m.target_planet=? [AND m.resolved=?]`. Reuse for all DEPLOYED-mission-row assertions in §3. |
| `table missions has exactly one row where user_id={u} and type_code=DEPLOYED and target_planet={pid}` | Distinguishing "merged into existing DEPLOYED mission" (B40) from "created a second one" needs a count assertion, not just existence. | Same query as above with `COUNT(*)`; assert `=1`. |
| `the DEPLOY mission of user {u} to planet {pid} is recalled` | RETURN_MISSION scenarios for DEPLOY specifically need a way to trigger a return without a REST cancel endpoint (DEPLOY has none — see §6 open question). Placeholder until that's resolved with Kevin. | Either (a) confirm no such player action exists and drop this draft scenario, or (b) if one is found, wire it the same way as `cancel`. |
| `user {u} received no websocket event "{name}" for planet {pid}` (payload-scoped negative) | Several B-numbers (B42, B47) hinge on a conditional emit (`unit_obtained_change` gated on final ownership) — the existing negative step (§6.5) only checks item-membership inside a list payload, not "this whole event class didn't fire for this planet's context." | Likely unnecessary if the existing `received no websocket event "{name}"` (whole-event-class absence) step already suffices — flagged here only if scenario-writing surfaces a real need; prefer the existing step first. |

## 5. Rust port status

`grep`s run against `rust-backend/owge-rest/src/routes/` and `rust-backend/owge-business/src/`;
`docs/UNPORTED-ENDPOINTS.md` does **not** list any of `establishBase`/`conquest`/`deploy`/`cancel`/
`game/planet/leave` — all four REST endpoints plus leave-planet are routed and call into a Bo layer
(no `501`s despite a stale module-doc comment in `mission.rs:1-22` claiming otherwise — the comment
predates the current `UnitMissionBo` port and should be corrected separately).

| Java flow | Rust location | Status |
|---|---|---|
| `establishBase` route + `myRegister*`/`adminRegister*` | `owge-rest/src/routes/game/mission.rs:100-110`, `owge-business/src/bo/unit_mission_bo.rs` | **Ported.** Route → `UnitMissionBo::my_register_establish_base_mission`. |
| `conquest` route + guards | `mission.rs` (conquest fn), `unit_mission_bo.rs` | **Ported**, including the own-planet/home-planet registration guards (B21/B22) per the module doc comment. |
| `deploy` route + self-target guard | `mission.rs` (deploy fn), `unit_mission_bo.rs` | **Ported**, including B23 (self-target guard) per the module doc comment. |
| `cancel` route | `mission.rs` (cancel fn) → `UnitMissionBo::my_cancel_mission` | **Ported** (module doc explicitly cites B44's shape: "marks resolved and registers a return... rejecting other players' missions and RETURN_MISSIONs"). |
| `game/planet/leave` | `owge-rest/src/routes/game/mod.rs:415-426` → `PlanetBo::leave_planet`, `owge-business/src/bo/planet_bo.rs:96-149` (`can_leave_planet`/`leave_planet`) | **Ported**, including the `canLeavePlanet` guard chain (B49) — code comments cross-reference the exact Java lines. |
| `doCommonMissionRegister` full pipeline (B1-B20 checks) | `owge-business/src/bo/unit_mission_registration_bo.rs` (1224 lines) | **Ported in depth** — `check_deploy_allowed`, `check_unit_can_deploy` (ONLY_ONCE_* rule), `check_cross_galaxy`, `check_can_store_unit`, `mark_orphan_missions_resolved` (B13, the exact function whose `u64`/`i64` COUNT(*) decode bug was fixed in commit `83a0ab9a`) all present with file:line-cited comments back to the Java source. |
| `EstablishBaseMissionProcessor` | `owge-business/src/bo/mission_processor/establish_base.rs` | **Partially ported.** B29 (attack trigger), B30 (already-owned), B31 (max-planets), B32 (success + `define_planet_as_owned_by`) all present. **Gap:** `missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission)` is a `TODO(M3/M4)` (`establish_base.rs:56`) — the Rust processor never emits `unit_mission_change`/`enemy_mission_change` after an establish-base mission resolves, in **any** branch (Java emits it unconditionally, B32). |
| `ConquestMissionProcessor` | `owge-business/src/bo/mission_processor/conquest.rs` | **Ported, most thoroughly of the four.** B33-B37 all present, including the special-location re-trigger, old-owner BUILD_UNIT cancellation, and the enemy report — with an explicit code comment flagging its own tx-threading risk (`cancel_build_unit` runs against a fresh `db` acquire while the conquest tx is still open, `conquest.rs:104-113`) as a noted-but-accepted deviation, not a silent gap. `DeferredEmit::LocalMissionChange` **is** pushed (both success and non-return-failure paths, `conquest.rs:151-156`) — this is the one processor of the four that ports the local-mission-change emit at all. |
| `DeployMissionProcessor` + `ObtainedUnitBo.moveUnit` | `owge-business/src/bo/mission_processor/deploy.rs`, `mission_processor/mod.rs:363-` (`move_unit_to_planet`) | **Partially ported.** B38 (own-planet landing) and B40/B41-shape (foreign DEPLOYED creation/merge, `move_unit_to_foreign_planet`, `deploy.rs:84-152`) are present and match Java closely. **Gaps (explicit `TODO(M3/M4)` at `deploy.rs:71-73`):** the DEPLOYED mission's `invisible` recompute (B41) is not set; `obtainedUnitEventEmitter.emitObtainedUnits` (`unit_obtained_change`, B42) is not emitted; `missionEventEmitterBo`'s `unit_mission_change`/`enemy_mission_change` emit (B42) is not emitted. The requirement re-trigger (HAVE_UNIT/UNIT_AMOUNT) **is** ported (`deploy.rs:51-69`). |
| `ReturnMissionProcessor` + auto-registration | `owge-business/src/bo/mission_processor/return_mission.rs`, `owge-business/src/bo/return_mission_registration_bo.rs` | **Partially ported, with a suspected correctness bug — see §6.** B45's own-planet branch is ported; the requirement re-trigger (B47's sync half) is ported. **Gaps (explicit `TODO(M3/M4)` at `return_mission.rs:76-77`):** `emitLocalMissionChangeAfterCommit` (B46) and `emitObtainedUnits` (part of B47) are not emitted. **Divergence, not just a gap:** the "destination not owned" fallback (`return_mission.rs:32-45`) sets `mission_id = NULL, target_planet = NULL` directly instead of routing through the DEPLOYED-mission creation/merge path that `deploy.rs`'s `move_unit_to_foreign_planet` implements for the identical Java `moveUnit` non-owned branch — see §6. |
| `move_unit_to_planet` shared helper | `mission_processor/mod.rs:363-` | Its own doc comment admits the gap: "The 'not my planet' branch (creating/attaching a DEPLOYED mission) is ported in the deploy processor... TODO(M3): non-owned moveUnit (DEPLOYED mission creation) — see deploy.rs" (`mod.rs:371-373`) — i.e. the helper is **known, by its own authors, to only cover the owned-planet branch**, and callers must each hand-implement the non-owned branch. `deploy.rs` does; `return_mission.rs` doesn't (§6). |
| `PlanetBo.definePlanetAsOwnedBy` | `mission_processor/mod.rs:304-350` | **Ported**, including the re-home-existing-DEPLOYED-units loop and mission deletion (B48's core state mutation). The websocket/unlock side (special-location grant, `planet_owned_change`, etc.) is deferred to `DeferredEmit::ConquestSuccess` per its own doc comment (`mod.rs:300-303`) — not independently verified for establish-base's call site in this pass (establish_base.rs calls `super::define_planet_as_owned_by` directly, not through a `ConquestSuccess`-shaped emit — worth double-checking establish-base's post-commit emit set separately once the `unit_mission_change` TODO above is resolved). |
| Interception (B27/B28) | `owge-business/src/bo/mission_interception_manager_bo.rs` | Present as a file; not read in this pass (out of primary scope — flagged for the interception/combat-domain agent rather than re-derived here). |

## 6. Open questions / suspected divergences

1. **RETURN_MISSION landing on an unowned-by-invoker planet: suspected Rust bug, not just a gap.**
   Java's `moveUnit` (`ObtainedUnitBo.java:197-220`) has exactly three branches regardless of which
   processor calls it (Deploy or Return): owned → land; not-owned-but-already-DEPLOYED → re-save
   as-is; not-owned-and-not-DEPLOYED → `saveWithAdding` + `findDeployedMissionOrCreate`, i.e. **the
   returning stack becomes a parked `DEPLOYED` mission at the destination**, exactly like a foreign
   deploy would. Rust's `deploy.rs` correctly implements this three-way branch via
   `move_unit_to_foreign_planet`. Rust's `return_mission.rs` (`.rs:32-45`) instead has its own ad hoc
   fallback that sets `mission_id = NULL, target_planet = NULL, owner_unit_id = NULL` — i.e. the
   returned units become a bare mission-less row with **no** DEPLOYED-mission linkage, sitting on a
   planet the user doesn't own. This looks unreachable through normal player-facing queries (which
   generally scope "units on planet X" to the planet's owner) and diverges from what
   `findDeployedMissionOrCreate`/`moveUnit` produce in Java. **This is exactly the shape of bug the
   BDD harness's Layer-2 `obtained_units`/`missions` table diff is built to catch** — recommend a
   Phase-3 scenario: user 1's DEPLOY mission is in flight to planet 1234, user 2 conquers 1234 before
   it lands, then assert whether the arriving stack becomes a DEPLOYED mission (Java) or an orphaned
   row (Rust). Flag to Kevin before assuming Java is "the" spec here — worth confirming Java's
   behavior is actually intended (a returning stack parking itself as a foreign DEPLOYED stack on a
   planet it never asked to land on is a slightly surprising design) rather than reflexively porting
   it — per plan §9 pitfall 11, don't blindly copy Java if it turns out to be the wrong default either.
2. **Missing `unit_mission_change`/`enemy_mission_change` emits for ESTABLISH_BASE, DEPLOY, RETURN_MISSION in Rust** (all three have explicit `TODO(M3/M4)` markers, §5) — every scenario in §3 touching these three mission types should assert on `unit_mission_change` specifically (as several already do) to make the harness catch this as soon as it's wired up; today it would show as `RUST_SPEC` red across the board for those assertions, which is the expected/wanted state until the TODOs land.
3. **Missing `unit_obtained_change` emit for DEPLOY and (half of) RETURN_MISSION in Rust** — same TODO markers; layer-2 ws-frame diff will catch this without a dedicated `Then`, but an explicit assertion is cheap and documents intent.
4. **DEPLOYED mission `invisible` flag never recomputed in Rust's deploy processor** (B41) — no direct player-visible symptom found in this pass (the flag mainly affects `enemy_mission_change` payload filtering elsewhere), but it's a real state divergence the layer-2 `missions` table diff should catch once wired.
5. **`EstablishBaseMissionProcessor`'s call to `define_planet_as_owned_by` vs. `ConquestSuccess`-shaped deferred emit** — conquest routes its post-success emits through a dedicated `DeferredEmit::ConquestSuccess` variant; establish-base calls the same underlying `define_planet_as_owned_by` state-mutation function directly but (per the `unit_mission_change` TODO) has no equivalent deferred-emit wiring at all yet. Once the TODO lands, worth confirming establish-base's emit set matches conquest's rather than being independently (and possibly incompletely) reinvented.
6. **No REST-reachable way to recall an in-flight DEPLOY mission before it resolves**, in either backend — `myCancelMission` works on any non-RETURN_MISSION mission generically (B44), so `POST game/mission/cancel?id=<deployMissionId>` should work for a DEPLOY exactly like it does for ESTABLISH_BASE/CONQUEST; this wasn't independently verified in this pass (the draft §3 scenario left it as an open placeholder). Worth a quick confirmation read of whether `myCancelMission`'s `missionBaseService.isOfType(mission, RETURN_MISSION)` guard is the *only* type restriction (it is, per `UnitMissionBo.java:194-195`) before finalizing that scenario.
7. **`checkMissionLimitNotReached`'s DEPLOY exemption (B2) interacts with the mission-limit config** (`faction.moreMissions` improvement) in a way not exercised by any draft scenario above — a scenario at exactly the mission cap, deploying to one's own planet (exempted) vs. to a foreign planet (not exempted), would be a good Phase-3 addition; not drafted here since it needs a config/improvement fixture not yet in the §6 Given catalog.
8. **Audit-table (`AuditActionEnum`) side effects were catalogued (B14) but not verified against the Rust port** — audits aren't websocket/DB-diffable in the same way (no player-facing read path was checked in this pass), and Rust audit-log parity is plausibly a lower-priority concern; flagged rather than investigated further.
