# missions-explore-gather-cancel — Java reference behavior inventory

Domain: EXPLORE, GATHER, mission CANCEL, running-mission queries, mission
count/slot limits, and the generic unit-mission lifecycle (registration →
scheduling → resolution → return) as it applies to those two mission types.
All line numbers are as of the repo state read for this inventory
(commit `b55e6afc`, 2026-07-09).

## 1. Endpoints

| HTTP | Path | Controller | Bo entry point |
|---|---|---|---|
| POST | `game/mission/explorePlanet` | `MissionRestService.java:26-29` | `UnitMissionBo.myRegisterExploreMission` (`UnitMissionBo.java:84-88`) |
| POST | `game/mission/gather` | `MissionRestService.java:31-34` | `UnitMissionBo.myRegisterGatherMission` (`UnitMissionBo.java:104-108`) |
| POST | `game/mission/cancel?id=` | `MissionRestService.java:61-65` | `UnitMissionBo.myCancelMission` (`UnitMissionBo.java:187-207`) |
| — (websocket sync handlers, not REST) | `missions_count_change`, `unit_mission_change`, `enemy_mission_change` | `MissionRestService.java:67-79` (`findSyncHandlers`) | `RunningMissionFinderBo.countUserRunningMissions` / `.findUserRunningMissions` / `.findEnemyRunningMissions` |

`MissionRestService` has no dedicated "my running missions" REST GET — running
missions are pushed via the `SyncSource`/websocket-sync mechanism
(`findSyncHandlers`, `MissionRestService.java:67-75`), driven by the same
`RunningMissionFinderBo` used by `MissionEventEmitterBo`. `explorePlanet` and
`gather` both return `void` (200 with empty body); `cancel` returns the literal
JSON string `"OK"` (`MissionRestService.java:64`).

## 2. Behavior catalog

### Registration — shared pipeline (`myRegisterExploreMission` / `myRegisterGatherMission`)

Both `explorePlanet` and `gather` funnel through the identical shared pipeline;
only the `MissionType` argument differs. Path: `UnitMissionBo.myRegister*`
(sets `userId` from session, `UnitMissionBo.java:300-302`) → `adminRegister*`
→ `commonMissionRegister` (`UnitMissionBo.java:304-325`) → planet-lock section
→ `UnitMissionRegistrationBo.doCommonMissionRegister`
(`UnitMissionRegistrationBo.java:42-77`).

**B1. Mission-slot-limit enforcement.**
Trigger: every `commonMissionRegister` call except a DEPLOY onto a
self-owned target planet (`UnitMissionBo.java:307-310`) — EXPLORE/GATHER
always hit this check since they are never DEPLOY.
Precondition/logic: `MissionBaseService.checkMissionLimitNotReached`
(`MissionBaseService.java:77-83`) computes
`missionRepository.countByUserIdAndResolvedFalse(user.getId()) + 1 >=
findUserMaxAllowedMissions(user)` and throws
`SgtBackendInvalidInputException("I18N_ERR_MISSION_LIMIT_EXCEEDED")` if true.
`findUserMaxAllowedMissions` = `improvementBo.findUserImprovement(user)
.getMoreMissions().intValue() + 1` (`MissionBaseService.java:103-105`) — i.e.
base 1 concurrent mission + the `moreMissions` improvement (upgrades/factions
that grant `+N moreMissions`). **Important:** `countByUserIdAndResolvedFalse`
counts ALL unresolved missions regardless of type, including in-flight
`RETURN_MISSION` rows (`MissionType.RETURN_MISSION` has `isUnitMission()==true`,
`MissionType.java:22-24`, so a unit's trip home also occupies a mission slot)
and `LEVEL_UP`/`BUILD_UNIT` missions. No DB write; pure read+throw. No
websocket emission on this rejection path (exception propagates to the REST
layer as an error response before any commit).

**B2. Target-planet-explored precondition (non-EXPLORE only).**
Trigger: any mission type other than EXPLORE (`UnitMissionBo.java:314-318`).
GATHER requires `planetExplorationService.isExplored(userId,
targetPlanetId)` to be true, else throws `SgtBackendInvalidInputException
("Can't send this mission, because target planet is not explored ")`.
EXPLORE is explicitly exempt (`missionType != MissionType.EXPLORE`) — you can
explore an unexplored planet by definition. `isExplored` = owned by user OR
has an `explored_planets` row (`PlanetExplorationService.java:29-32`).

**B3. Planet-lock section + `doCommonMissionRegister`.**
`planetLockUtilService.doInsideLockById([sourcePlanetId, targetPlanetId], …)`
(`UnitMissionBo.java:319-324`) wraps the rest of registration in MySQL named
locks (`planet_lock_<id>`). Inside the lock:
1. `checkUserExists` (`MissionRegistrationUserExistsChecker.java:19-23`) —
   throws `UserNotFoundException` if the user id doesn't exist in
   `user_storage`.
2. `checkDeployedAllowed` — no-op for EXPLORE/GATHER (only gates
   `MissionType.DEPLOY`, `MissionRegistrationCanDeployChecker.java:30-35`).
3. `checkAndLoadObtainedUnits` (`MissionRegistrationObtainedUnitLoader.java:39-73`):
   - **B4.** Throws `SgtBackendInvalidInputException("involvedUnits can't be
     empty")` if the request has no selected units
     (`MissionRegistrationObtainedUnitLoader.java:48-50`) — EXPLORE/GATHER
     always need at least one unit.
   - **B5.** Throws `SgtBackendInvalidInputException("I18N_ERR_REPEATED_UNIT")`
     if the same `(unitId, expirationId)` appears twice, at either the
     top level or nested in `storedUnits`
     (`MissionRegistrationObtainedUnitLoader.java:88-94`).
   - **B6.** For each selected unit: loads the source `obtained_units` stack
     (`ObtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission`),
     throws `SgtBackendInvalidInputException` ("No count was specified…") if
     `count` is null (`:104-106`), then
     `checkUnitCanDeploy` — a no-op for non-DEPLOY missions
     (`MissionRegistrationCanDeployChecker.java:44-56`, the guard only fires
     when `missionInformation.getMissionType() == MissionType.DEPLOY`) — then
     `obtainedUnitBo.saveWithSubtraction` decrements/deletes the source stack.
     If subtraction empties a stack that belonged to a `DEPLOYED` mission, that
     mission id is queued for orphan-resolution
     (`:115-118`, `MissionRegistrationOrphanMissionEraser`).
   - **B7.** Carrier-weight check: for units with `storedUnits`, throws
     `SgtBackendInvalidInputException("I18N_ERR_MAX_WEIGHT_OVERPASSED")` when
     `sum(storedUnit.storedWeight * count) > carrier.storageCapacity * carrierCount`
     (`MissionRegistrationObtainedUnitLoader.java:75-86`).
4. `missionRepository.saveAndFlush(prepareMission(...))` inserts the `missions`
   row: `starting_date=now(UTC)`, `required_time = calculateRequiredTime(type)`
   (base config `MISSION_TIME_EXPLORE` default 60s / `MISSION_TIME_GATHER`
   default 900s, `MissionRegistrationPreparer.java:24-42`,
   `MissionTimeManagerBo.java:37-39`), `termination_date =
   now + required_time`, `user`, `source_planet`, `target_planet`.
5. `auditMissionRegistration` (`MissionRegistrationAuditor.java:19-34`) — audit
   log write only, no game-state effect (see §6 for the "audit disabled in
   Java prod" pitfall shared with the Rust port).
6. `manageUnitsRegistration` (`MissionRegistrationUnitManager.java:27-59`)
   inserts one `obtained_units` row per selected/stored unit, `mission_id` =
   the new mission, `source_planet`/`target_planet` copied from the mission,
   `owner_unit_id` set for stored (carried) units.
7. **B8. Unit-type / speed-group mission-support check.**
   `checkUnitsCanDoMission` (`MissionRegistrationUnitTypeChecker.java:22-48`)
   throws `SgtBackendInvalidInputException` if any involved unit's *type*
   doesn't support the mission (`unitTypeBo.canDoMission` →
   `EntityCanDoMissionChecker.canDoMission`,
   `EntityCanDoMissionChecker.java:28-44`: reflectively calls
   `getCanExplore()`/`getCanGather()` on the type; `ANY` → true,
   `OWNED_ONLY` → `planetRepository.isOfUserProperty(user, targetPlanet)`,
   anything else (including `NONE`/null) → false), **or** if any non-stored
   unit's resolved speed-impact-group (`SpeedImpactGroupFinderBo.findApplicable`)
   fails the same `ANY`/`OWNED_ONLY`/else check
   (`MissionRegistrationUnitTypeChecker.java:39-47`) — note this speed-group
   check ALSO applies the `OWNED_ONLY` ownership refinement, not just a
   NONE/not-NONE test.
8. **B9. Cross-galaxy check.** `checkCrossGalaxy`
   (`CrossGalaxyMissionChecker.java:26-38`) — only runs when
   `sourcePlanet.galaxy != targetPlanet.galaxy`. For each non-stored
   (`ownerUnit == null`) unit, resolves its speed-impact-group (unit override,
   else the unit type's group) and requires (a) `EntityCanDoMissionChecker
   .canDoMission` on that group for "outside the galaxy" (same `ANY`/
   `OWNED_ONLY` logic) and (b) the group's `SPEED_IMPACT_GROUP` object
   relation to be `unlockedRelationBo.isUnlocked` for the user, else
   `SgtBackendInvalidInputException("Don't try it.... you can't do cross
   galaxy missions, and you know it")` (`CrossGalaxyMissionChecker.java:40-60`).
9. `obtainedUnitRepository.saveAll(...)`.
10. **B10. Speed-adjusted mission duration.**
    `handleMissionTimeCalculation` (`MissionTimeManagerBo.java:44-68`): unless
    every involved unit has a *fixed* speed-impact-group, finds the lowest
    `unit.speed` among eligible units, looks up the improvement-adjusted speed
    (`moreSpeed` for that unit's type), and recomputes `required_time` via
    `calculateTimeUsingSpeed` — `missionTypeTime + (leftMultiplier * moveCost *
    (100 - speed)) / divisor`, floored at `missionTypeTime`
    (`MissionTimeManagerBo.java:77-85`). `leftMultiplier`
    ("mission penalty") and `moveCost` are driven by
    `MISSION_SPEED_<TYPE>_{SAME_Q,DIFF_Q,DIFF_S,DIFF_G}` and
    `MISSION_SPEED_<TYPE>_{P,Q,S,G}_MOVE_COST` configuration rows
    (defaults 50/100/200/2000 and 0.01/0.02/0.03/0.15,
    `MissionTimeManagerBo.java:87-142`) — **treat these as normalize-not-assert
    inputs** for parity scenarios: the exact numeric duration depends on planet
    number/quadrant/sector/galaxy deltas and live config, not a fixed constant.
11. `handleCustomDuration` (`MissionTimeManagerBo.java:70-75`) — if the request
    supplied `wantedTime` and it's greater than the computed `required_time`,
    it overrides (players can voluntarily slow down a mission — never speeds
    it up).
12. `handleDefineMissionAsInvisible`
    (`MissionRegistrationInvisibleManager.java:20-24`) — mission `invisible` =
    true iff every involved unit is a "hidden unit" for its user
    (`HiddenUnitBo.isHiddenUnit`: unit's own `is_invisible` flag OR an active
    `TIME_SPECIAL_IS_ENABLED_DO_HIDE` rule targeting the unit/its type).
13. `missionRepository.save(mission)` persists the refined time/invisibility;
    `missionSchedulerService.scheduleMission(mission)` inserts/updates the
    `scheduled_tasks` row (`task_name='mission-run'`, `task_instance=<id>`,
    `execution_time = now + required_time - DELAY_HANDLE(2s)`).
14. **B11. Post-commit websocket emissions (registration).**
    `missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission)`
    (`UnitMissionRegistrationBo.java:69`) → after commit, emits
    `unit_mission_change` to the invoker (always,
    `MissionEventEmitterBo.java:72-78`, via `emitUnitMissions`) and
    `enemy_mission_change` to the **target** planet's owner iff the mission is
    not invisible (`MissionEventEmitterBo.java:44-49,72-77`). Then, if the
    invoker owns the **source** planet, `emitObtainedUnitsAfterCommit(user)`
    fires `obtained_units_change`-style refresh
    (`UnitMissionRegistrationBo.java:70-72`); else (source planet belongs to
    someone else — e.g. gathering from a DEPLOYED stack at a foreign planet)
    `missionRegistrationInvisibleManager.maybeUpdateMissionsVisibility(...)`
    runs and `missionEventEmitterBo.emitEnemyMissionsChange(sourcePlanetOwner)`
    fires (`UnitMissionRegistrationBo.java:73-76`).

### EXPLORE-specific resolution (`ExploreMissionProcessor.process`, `ExploreMissionProcessor.java:29-44`)

Runs when the scheduled task fires (`UnitMissionBo.runUnitMission` →
`doRunUnitMission` → `missionProcessorMap.get(EXPLORE).process(...)`), inside
the source/target planet lock superset, `@Transactional(READ_COMMITTED)`.

**B12. Mark planet explored (first time only).**
If `!planetExplorationService.isExplored(user, targetPlanet)`,
`defineAsExplored` inserts an `explored_planets` row
(`user`, `planet`) and **synchronously** sends websocket event
`planet_explored_event` (constant `PlanetExplorationService.PLANET_EXPLORED_EVENT`,
`PlanetExplorationService.java:18,34-42`) carrying the target planet's
`PlanetDto` — note this emit is NOT wrapped in `doAfterCommit`, unlike almost
every other emission in this domain (`PlanetExplorationService.java:39-42`).
If the planet was already explored (e.g. re-exploring an owned/previously
explored planet), this step and its emission are skipped entirely.

**B13. Units-in-planet report payload.**
`obtainedUnitBo.explorePlanetUnits(mission, targetPlanet)`
(`ObtainedUnitBo.java:87-94`) queries `findByExplorePlanet(missionId,
planetId)` (`ObtainedUnitRepository.java:41`) — units sitting at the target
planet with no mission, PLUS units belonging to a `DEPLOYED` mission targeting
that planet (excluding the exploring mission's own units, implicit via the
join). `hiddenUnitBo.defineHidden` + `ObtainedUnitUtil.handleInvisible` then
null out `unit`/`count` on DTOs for hidden/invisible units before they reach
the report.

**B14. Return-mission auto-registration.**
`returnMissionRegistrationBo.registerReturnMission(mission, null)`
(`ExploreMissionProcessor.java:38`) — ALWAYS runs for EXPLORE (unconditional,
unlike GATHER's attack-trigger gate). See "Return mission" behavior below.

**B15. Report build + resolve.**
`UnitMissionReportBuilder.create(...).withExploredInformation(unitsInPlanet)`
(`ExploreMissionProcessor.java:39-41`, `UnitMissionReportBuilder.java:85-88`
puts key `unitsInPlanet`) then `mission.setResolved(true)`
(`ExploreMissionProcessor.java:42`) — the mission row is NOT deleted (unlike
BUILD_UNIT/LEVEL_UP paths in `MissionBo`); it persists with `resolved=1` until
the nightly `deleteOldMissions` job removes rows older than 60 days
(`MissionBo.java:93-103`). Report saved via
`MissionReportManagerBo.handleMissionReportSave(mission, builder)`
(`UnitMissionBo.java:286`, `MissionReportManagerBo.java:37-45`) → inserts
`mission_reports` (`json_body` = the built map, `is_enemy=false`,
`user_id=mission.user`), links `missions.report_id`, and (via
`MissionReportBo.save`, `MissionReportBo.java:69-76`) after commit emits
`mission_report_new` to the mission's user plus `mission_report_count_change`
(`MissionReportBo.java:82-85, 233-235`).

### GATHER-specific resolution (`GatherMissionProcessor.process`, `GatherMissionProcessor.java:34-74`)

**B16. Defensive-attack trigger gate.**
`attackMissionProcessor.triggerAttackIfRequired(mission, user, targetPlanet)`
(`GatherMissionProcessor.java:40`, `AttackMissionProcessor.java:54-62`) — if
configuration `MISSION_GATHER_TRIGGER_ATTACK` (default `"FALSE"`,
`AttackMissionManagerBo.java:71-74`) is `"TRUE"` **and**
`obtainedUnitRepository.areUnitsInvolved(userId, userAlliance, targetPlanetId)`
is true — i.e. there exists a `DEPLOYED`-mission `obtained_units` row at the
target planet whose owner is neither this user nor (if set) in the same
alliance (`ObtainedUnitRepository.java:132-133`) — then a full combat resolves
first (`processAttack(mission, survivorsDoReturn=false,
isTriggeredByEvent=true)`, `AttackMissionProcessor.java:64-93`). **If the
attacking gather stack is wiped (`result.isRemoved()`), `continueMission =
false` and `GatherMissionProcessor.process` returns `null`** — no gather
report is built, no resources are credited, `mission.setResolved(true)` was
already set inside `processAttack` (`AttackMissionProcessor.java:71`), and the
combat's own attack report (built + saved by `processAttack` itself,
`AttackMissionProcessor.java:72-81`) is the only report produced. If the
gathering stack survives the triggered combat, gather proceeds normally below
with the (possibly reduced) surviving units — note `involvedUnits` passed into
`GatherMissionProcessor.process` was captured **before** the trigger-attack
ran (`UnitMissionBo.doRunUnitMission`, `UnitMissionBo.java:279-292`, passes
`interceptionInformation.getInvolvedUnits()` once); casualties from the
triggered attack are NOT re-queried, so the gather math below can overcount
units killed by the defensive combat — **flag as an open question (§6)**.

**B17. Return-mission auto-registration (conditional).**
Only reached if B16's `continueMission == true`.
`returnMissionRegistrationBo.registerReturnMission(mission, null)`
(`GatherMissionProcessor.java:42`).

**B18. Gathered-amount math.**
`gathered = Σ (ObjectUtils.firstNonNull(unit.charge, 0) * unit.count)` over
`involvedUnits` (`GatherMissionProcessor.java:43-45`) — units with no `charge`
(most non-cargo ship types) contribute 0.
`withPlanetRichness = gathered * targetPlanet.findRationalRichness()`
(`Planet.java:49-50`: `richness / 100.0`, an int 0-100 richness column
producing a 0.0-1.0 multiplier) (`GatherMissionProcessor.java:46`).
`withUserImprovement = withPlanetRichness + withPlanetRichness *
improvementBo.findAsRational(groupedImprovement.getMoreChargeCapacity())`
(`GatherMissionProcessor.java:47-49`) — the `moreChargeCapacity` improvement
(from upgrades/units) as a percentage bonus.
Faction split: if `faction.customPrimaryGatherPercentage` AND
`customSecondaryGatherPercentage` are both non-null and `> 0`,
`primary = withUserImprovement * (customPrimary/100)`, `secondary =
withUserImprovement * (customSecondary/100)` (**Java computes `customPrimary /
100` in `Float` (f32) arithmetic before widening to `double`** —
`GatherMissionProcessor.java:54-56`, a precision detail the Rust port
explicitly replicates, see §5); otherwise a flat 50/50 split
(`GatherMissionProcessor.java:57-59`). `user.addtoPrimary(primary)` /
`addToSecondary(secondary)` credit the user's `user_storage.primary_resource`
/ `secondary_resource` (`GatherMissionProcessor.java:61-62`).

**B19. Report + websocket.**
`UnitMissionReportBuilder...withGatherInformation(primary, secondary)`
(`GatherMissionProcessor.java:63-65`, puts keys `gatheredPrimary`/
`gatheredSecondary`) — saved the same way as B15. Additionally, **immediately**
after building (still inside the transaction, deferred via
`transactionUtilService.doAfterCommit`), sends websocket event
`mission_gather_result` to the user with `{primaryResource, secondaryResource}`
(`GatherMissionProcessor.java:66-68`, `GatherMissionResultDto`) — this is
IN ADDITION TO the generic `mission_report_new` emitted by the report save;
GATHER is the only mission type in this domain with a dedicated
"result" push event. `mission.setResolved(true)`
(`GatherMissionProcessor.java:69`).

### Interception (shared, applies to any unresolved unit mission except RETURN_MISSION)

**B20.** Before a unit mission's processor runs, `MissionInterceptionManagerBo
.loadInformation` (`MissionInterceptionManagerBo.java:26-53`) checks
`unitInterceptionFinderBo.checkInterceptsSpeedImpactGroup` — if EVERY involved
unit is intercepted, the EXPLORE/GATHER processor never runs at all;
`handleMissionInterception` (`MissionInterceptionManagerBo.java:66-74`) marks
the mission resolved, deletes the intercepted `obtained_units`, builds/saves a
report with only `interceptionInfo` (no `unitsInPlanet`/`gatheredPrimary`
keys), and notifies interceptor users
(`unitInterceptionFinderBo.sendReportToInterceptorUsers`). If only *some*
units are intercepted, the processor still runs with the surviving subset, and
`maybeAppendDataToMissionReport` (`MissionInterceptionManagerBo.java:55-64`)
appends `interceptionInfo` to whatever report the processor produced.

### Return mission (`ReturnMissionRegistrationBo.doRegisterReturnMission`, `ReturnMissionRegistrationBo.java:42-60`)

**B21.** Always: a new `missions` row of type `RETURN_MISSION`, `source_planet`
= origin's source, `target_planet` = origin's target (i.e. NOT flipped in the
DB columns — the "return" semantics are purely in `MissionType` +
`related_mission`), `related_mission = origin.id`, `invisible` copied from the
origin, `required_time` = `customRequiredTime` if given else the origin's
`required_time` (so EXPLORE/GATHER's normal auto-return reuses the SAME
duration as the outbound trip — no separate "return speed" calc). ALL
`obtained_units` rows still pointing at the origin mission
(`findByMissionId(originMission.getId())`) are re-pointed
(`current.setMission(returnMission)`) — this is how units "become" the return
trip; no new `obtained_units` rows are created.
`missionSchedulerService.scheduleMission(returnMission)` schedules its
`scheduled_tasks` row. `missionEventEmitterBo
.emitLocalMissionChangeAfterCommit(returnMission)` — emits `unit_mission_change`
to the mission owner (always) and `enemy_mission_change` to the **origin's
source planet owner** if not invisible and the owner differs from the mission
user (usually a no-op since the source planet is normally the user's own home,
`MissionEventEmitterBo.java:44-49`).

Return-mission RESOLUTION itself (units arriving home) is out of this
inventory's direct scope but relevant to lifecycle completeness: it is
dispatched through the same `runUnitMission` path with
`MissionType.RETURN_MISSION`, handled by a `ReturnMissionProcessor` (not read
in depth here — outside EXPLORE/GATHER/CANCEL domain) which is exempt from the
interception check (`MissionInterceptionManagerBo.java:33,42-45`:
`!missionType.equals(RETURN_MISSION)` guards the intercept-check branch, so
returning units cannot be intercepted).

### CANCEL (`UnitMissionBo.myCancelMission`, `UnitMissionBo.java:187-207`)

**B22. Ownership / type guards.**
- 404 `NotFoundException` if no mission with that id exists
  (`UnitMissionBo.java:190-191`).
- `SgtBackendInvalidInputException("You can't cancel other player missions")`
  if the mission's user differs from the session user
  (`UnitMissionBo.java:192-193`) — note this reads `mission.getUser()` and
  compares to `userSessionService.findLoggedIn()`, NOT the REST-supplied id
  (no id spoofing surface).
- `SgtBackendInvalidInputException("can't cancel return missions")` if
  `missionBaseService.isOfType(mission, MissionType.RETURN_MISSION)`
  (`UnitMissionBo.java:194-196`) — units already flying home cannot be
  re-cancelled; there is no "cancel the cancel."

**B23. Cancel effect — no resolved-state guard.**
`mission.setResolved(true); missionRepository.save(mission);`
(`UnitMissionBo.java:197-198`) then computes
`durationMillis = max(0, (terminationMillis - nowMillis) / 1000)` — how many
seconds are LEFT until the mission would have completed naturally
(`UnitMissionBo.java:199-204`) — and calls
`returnMissionRegistrationBo.registerReturnMission(mission,
mission.getRequiredTime() - durationMillis)`
(`UnitMissionBo.java:205`) — i.e. the return trip's duration is the time
ALREADY SPENT flying, not the time remaining (a unit cancelled 5s into a 60s
mission returns in ~5s, not 55s) — this models "turn back now, you're only
this far out." **Critically, `myCancelMission` never checks whether the
mission is already resolved** — cancelling an EXPLORE/GATHER mission that has
ALREADY completed (and already auto-registered its own return mission per
B14/B17/B21, which re-points the origin mission's `obtained_units` away to the
return mission) re-runs `registerReturnMission` a SECOND time against a
mission that now has zero `obtained_units` rows still pointing at it
(`findByMissionId` returns empty) — producing a second, unit-less
`RETURN_MISSION` row that gets scheduled and eventually fires and resolves
with no units to move. This is a same-both-backends spec question, not a
Rust-vs-Java divergence — see §6.

**B24. No resource refund for unit missions.**
Unlike the *other* (private) `MissionBo.cancelMission` used for
`BUILD_UNIT`/`LEVEL_UP` cancellation — which refunds
`primaryResource`/`secondaryResource` to the user
(`MissionBo.java:396-404`) — `UnitMissionBo.myCancelMission` (used for
EXPLORE/GATHER/ATTACK/etc.) does NOT touch `user_storage` resources at all;
EXPLORE/GATHER never charged resources up front (only time), so there is
nothing to refund; the "cost" already paid is the elapsed flight time, which
is exactly what B23's return-duration calc gives back proportionally.

**B25. Websocket.** No explicit emission from `myCancelMission` itself; the
only emission is the one `registerReturnMission` triggers for the new
`RETURN_MISSION` (B21): `unit_mission_change` to the canceller (updates their
running-mission list — the cancelled mission is `resolved=1` so it drops out,
replaced by the new return mission) and (usually skipped, same-owner check)
`enemy_mission_change`.

### Running-mission queries / mission-count sync (`RunningMissionFinderBo`)

**B26.** `countUserRunningMissions(userId)` = `missions.count(user_id=?,
resolved=0)` (`RunningMissionFinderBo.java:53-56`), cached with
`@TaggableCacheable(tags = MISSION_BY_USER_CACHE_TAG:#userId)` — pushed as
`missions_count_change` (`MissionRestService.java:69-70`,
`MissionEventEmitterBo.emitMissionCountChange`,
`MissionEventEmitterBo.java:80-82`).

**B27.** `findUserRunningMissions(userId)` (`RunningMissionFinderBo.java:63-80`)
— every unresolved mission for the user, each with its involved units
attached (`obtainedUnitFinderBo.findCompletedAsDto`) and, for EXPLORE
missions specifically, `planetCleanerService.cleanUpUnexplored(userId,
targetPlanet)` (`:75-77`) nulls the target planet's `name`/`richness`/`home`/
owner/`specialLocation` fields if the user still hasn't explored it (i.e. an
in-flight EXPLORE mission's own client can't peek the destination's identity
before arrival). `nullifyInvolvedUnitsPlanets()` strips
source/target-planet refs from the involved-unit DTOs (redundant with the
parent mission DTO). Cached, tagged by both `MISSION_BY_USER_CACHE_TAG` and
`OBTAINED_UNIT_CACHE_TAG_BY_USER`.

**B28.** `findEnemyRunningMissions(user)` — missions targeting a planet the
user owns, `resolved=0 AND invisible=0 AND user != askingUser`
(`RunningMissionFinderBo.java:36-51`); if the asking user hasn't explored the
mission's SOURCE planet, the DTO's `sourcePlanet` and `user` fields are
nulled (`:43-46`) — you can see an incoming EXPLORE/GATHER mission is en
route to your planet without necessarily knowing who sent it or from where,
unless you've explored their launch point. `hiddenUnitBo.defineHidden` +
`ObtainedUnitUtil.handleInvisible` (`:47-48`) then strip unit identity/count
for hidden units.

## 3. Draft Gherkin scenarios

IDs per VERIFIED 2026-07-09 ranges (units ≥9100, missions ≥900000; user 1 home
1002, user 2 home 1004, planet 1234 unowned in galaxy 1). Reused the shape of
`rust-backend/scripts/seed_gather.sql` (user 1 = 'rusttester', unit 28 charge
85 on planet 1002, planet 1003 richness 60, unowned, explored by user 1) for
the GATHER scenario's numeric expectations. Steps use ONLY the §6 catalog
vocabulary of `BDD-PARITY-PLAN.md` where it exists; steps not yet in the
catalog are marked `[NEW]` and listed in §4.

```gherkin
Feature: Explore, gather, and mission cancel
  Reference: business/mission/processor/ExploreMissionProcessor.java,
  business/mission/processor/GatherMissionProcessor.java,
  business/UnitMissionBo.java#myCancelMission.

  Background:
    Given the standard test universe
    And user 1 has 5 units of id 10 on planet 1002

  Scenario: Explore mission discovers an unowned, unexplored planet
    # Covers B1-B15 (registration pipeline + explore resolution), B21 (auto return)
    When user 1 runs an EXPLORE mission from planet 1002 to planet 1234 with 5 units of id 10
    Then table missions has a row where user_id=1 and type=EXPLORE and resolved=1
    And table explored_planets has a row where user=1 and planet=1234
    And table missions has a row where user_id=1 and type=RETURN_MISSION and related_mission is the explore mission id   # [NEW]
    And user 1 received websocket event "planet_explored_event" where some item has id 1234   # [NEW payload predicate: planet id, not list membership]
    And user 1 received websocket event "mission_report_new"
    And user 1 received websocket event "unit_mission_change"

  Scenario: Gather mission credits resources per faction split and richness
    # Covers B1-B11 (registration), B16 (no trigger-attack: MISSION_GATHER_TRIGGER_ATTACK=FALSE default,
    # no DEPLOYED units at target), B17-B19 (gather math + report + mission_gather_result), B21 (auto return)
    Given user 1 has explored planet 1003
    And user 1 has 10 units of id 28 on planet 1002
    When user 1 runs a GATHER mission from planet 1002 to planet 1003 with 10 units of id 28
    Then table user_storage has a row where id=1   # [NEW: assert primary_resource/secondary_resource delta, not absolute value — normalize against improvements]
    And table missions has a row where user_id=1 and type=GATHER and resolved=1
    And user 1 received websocket event "mission_gather_result"
    And user 1 received websocket event "mission_report_new"

  Scenario: Registering a mission beyond the concurrent-mission limit is rejected
    # Covers B1 (mission-slot-limit enforcement)
    Given user 1 has explored planet 1234
    And user 1 has fixed mission id 900001 for user 1   # [NEW helper Given: pre-seed N unresolved missions to saturate the slot count]
    When user 1 runs a GATHER mission from planet 1002 to planet 1234 with 5 units of id 10
    Then the mission registration is rejected with error "I18N_ERR_MISSION_LIMIT_EXCEEDED"   # [NEW Then]

  Scenario: Cancelling a running explore mission before it resolves flies the units home early
    # Covers B22 (ownership/type guard - allowed), B23 (cancel effect), B25 (websocket)
    Given user 1 has explored planet 1234
    When user 1 runs an EXPLORE mission from planet 1002 to planet 1234 with 5 units of id 10
    And user 1 cancels their latest mission   # [NEW When step]
    Then table missions has a row where user_id=1 and type=EXPLORE and resolved=1
    And table missions has a row where user_id=1 and type=RETURN_MISSION and related_mission is the explore mission id   # [NEW]
    And user 1 received websocket event "unit_mission_change"

  Scenario: Cancelling another player's mission is rejected
    # Covers B22 (ownership guard)
    Given planet 1234 is owned by user 2
    And user 2 has explored planet 1002
    When user 2 runs an EXPLORE mission from planet 1234 to planet 1002 with 1 unit of id 11
    And user 1 cancels user 2's latest mission   # [NEW When step, deliberately cross-user to hit the guard]
    Then the cancel is rejected with error "You can't cancel other player missions"   # [NEW Then]

  Scenario: Cancelling a return mission is rejected
    # Covers B22 (RETURN_MISSION guard)
    Given user 1 has explored planet 1234
    When user 1 runs an EXPLORE mission from planet 1002 to planet 1234 with 5 units of id 10
    And the EXPLORE mission of user 1 completes
    And user 1 cancels their latest mission
    Then the cancel is rejected with error "can't cancel return missions"   # [NEW Then]
```

## 4. Proposed new steps

QUARANTINED — none of these are implemented; they name a need surfaced by §3.

| Step text | Why needed | Implementation notes |
|---|---|---|
| `user {u} has explored planet {pid}` | Given: seed `explored_planets` directly, needed by every GATHER/return/cancel scenario that must skip B2's explored-precondition without an extra EXPLORE round-trip. | `INSERT INTO explored_planets (user, planet) VALUES (?, ?)`, idempotent (delete-then-insert or `INSERT IGNORE` on a unique key if one exists — check schema). |
| `table missions has a row where user_id={u} and type={TYPE} and resolved={0\|1}` | Then: the generic mission-row assertion the plan's §6.4 "escape hatch" table doesn't cover types outside a whitelist; `missions` should be added to that whitelist, or a named step added. | JOIN `missions` → `mission_types` on `type`, filter `code={TYPE}`, `resolved`. |
| `table missions has a row where user_id={u} and type=RETURN_MISSION and related_mission is the {TYPE} mission id` | Then: asserts the auto-return-mission linkage (B14/B17/B21/B23) without hardcoding an autoincrement id (Pitfall #9). | Requires the driver to remember the `When` step's created mission id on `BddWorld` (already planned per §5.3 `created_missions`); resolve "the {TYPE} mission id" to the last created mission of that type this scenario. |
| `user {u} received websocket event "{name}" where some item has id {id}` applied to a **non-list** payload (e.g. `planet_explored_event`, whose `value` is a single `PlanetDto`, not a list) | Then: §6.5's existing predicate assumes `payload.value` is a list (`any(item["id"]==id)`); `planet_explored_event` and `mission_gather_result` are single-object payloads. | Add a predicate variant: `payload.value.id == id` (object) vs the existing list form; auto-detect by `isinstance(value, list)` in the driver, or add a distinct step `... where the payload has id {id}`. |
| `the mission registration is rejected with error "{code}"` | Then: no existing step asserts a REST call FAILED with a specific error code/message (the whole catalog assumes registration success then asserts state). B1/B2/B4-B9/B22 are all rejection-path behaviors with no coverage. | The `When` step must capture the HTTP status + body instead of asserting 2xx-or-fail-loud (contradicts §6.3's "every When that creates a mission also asserts the POST returned 2xx" default) — needs a `When`/`Then` PAIR variant, e.g. `user {u} attempts a(n) {TYPE} mission ... ` (non-asserting When) + this Then, mirroring how negative-path testing usually forks the happy-path step. |
| `user {u} has fixed mission id {mid} for user {u}` (or simpler: `user {u} has {n} other unresolved missions`) | Given: seed N filler unresolved missions to deterministically saturate B1's mission-limit check without registering N real missions through the REST path (which is slow and itself under test). | `INSERT INTO missions (id, user_id, type, resolved, ...) VALUES (?, ?, <EXPLORE code>, 0, ...)` with fixed ids ≥900000, matching the `FIXED_MISSION_ID` seed pattern (§6.3 W3) but without the `scheduled_tasks` row (never meant to fire). |
| `user {u} cancels their latest mission` / `user {u} cancels user {u2}'s latest mission` | When: POST `game/mission/cancel?id=` needs the target mission id resolved from "the last mission `When`-created this scenario for user X", including the cross-user case that deliberately targets someone else's mission id (to test B22's ownership guard). | Resolve via `BddWorld.created_missions` (§5.3) filtered by user; the cross-user variant takes the *other* user's last created mission id but sends the request as the *acting* user's JWT. |
| `the cancel is rejected with error "{message}"` | Then: negative-path counterpart to the cancel `When`, distinct from the registration-rejection Then above because cancel's exceptions are `SgtBackendInvalidInputException`/`NotFoundException` with the exact strings quoted in `UnitMissionBo.java:190-196` (not `I18N_*` codes like the registration ones). | Same status/body capture mechanism as the registration-rejection variant; message string match (not code match) since `myCancelMission`'s exceptions aren't `withDeveloperHintDoc`/I18N-coded like `checkMissionLimitNotReached`'s is. |
| `the {TYPE} mission of user {u} completes` | Already named in the plan's §6.3 catalog table (nudge+poll the latest unresolved mission of that type) — flagging here only because the cancel-a-return-mission scenario (§3) is the first concrete use case in THIS domain: it must complete the EXPLORE leg first so the auto-registered RETURN_MISSION exists to then attempt (and reject) cancelling. | No new implementation beyond what §6.3 already specifies; noted for completeness. |

## 5. Rust port status

All three endpoints are ROUTED and largely implemented — `explorePlanet`,
`gather`, and `cancel` are NOT in `rust-backend/docs/UNPORTED-ENDPOINTS.md`.

| Java piece | Rust file | Status |
|---|---|---|
| `MissionRestService.explorePlanet`/`.gather`/`.cancel` | `rust-backend/owge-rest/src/routes/game/mission.rs:42-49,74-97,178-186` | Routed; thin handlers delegating to `UnitMissionBo`, per file header comment (`mission.rs:1-23`) noting the M3 "planet-lock + pre-check" wrapper WAS since implemented in `UnitMissionBo` (the header's caveat about handlers answering `501` appears STALE — `common_mission_register` in `unit_mission_bo.rs:196-271` fully implements the lock/limit/explored pipeline; worth a doc-comment fix, not a behavior gap). |
| `UnitMissionBo.myRegisterExploreMission`/`myRegisterGatherMission` + `commonMissionRegister` | `rust-backend/owge-business/src/bo/unit_mission_bo.rs:55-85,196-271` | Ported: mission-limit check (`MissionBaseService::check_mission_limit_not_reached`, called at `:210`), explored-precondition (`:214-220`), planet-lock (`run_locked`, `:233-241`), post-commit emits (`:243-269`). |
| `UnitMissionRegistrationBo.doCommonMissionRegister` | `rust-backend/owge-business/src/bo/unit_mission_registration_bo.rs:79-147` | Ported end-to-end: user-exists, deploy-allowed, unit load+subtract+weight check, mission insert, unit-type/speed-group check, cross-galaxy check, time calc, invisibility, schedule. Auditing (B_step in §2's `auditMissionRegistration`) deliberately dropped (`:111-114`, matches Java prod's audit being disabled). |
| `MissionBaseService.checkMissionLimitNotReached` (B1) | `rust-backend/owge-business/src/bo/mission_base_service_bo.rs:167-183,186-199` | Ported faithfully: same `running + 1 >= max_allowed` formula, same `+1` base slot. |
| `MissionRegistrationUnitTypeChecker.checkUnitsCanDoMission` (B8) | `rust-backend/owge-business/src/bo/unit_mission_registration_bo.rs:884-937` | **PARTIAL / likely divergence** — see §6 item 1. |
| `CrossGalaxyMissionChecker` (B9) | `rust-backend/owge-business/src/bo/unit_mission_registration_bo.rs:956-1079` | Ported, including the `OWNED_ONLY` ownership refinement (correctly, unlike B8's general-path check). |
| `MissionTimeManagerBo` (B10-B11) | `rust-backend/owge-business/src/bo/mission_time_manager_bo.rs` (not read in full for this inventory; referenced from `unit_mission_registration_bo.rs:124-134`) | Present, invoked at the right points; formula not independently re-verified against Java in this pass — flag for a follow-up numeric parity check (§6 item 4). |
| `MissionRegistrationInvisibleManager` (B12 mission-invisible flag) | `rust-backend/owge-business/src/bo/unit_mission_registration_bo.rs:1108-1170` | Ported, including the `TIME_SPECIAL_IS_ENABLED_DO_HIDE` rule branch. |
| `ExploreMissionProcessor` (B12-B15) | `rust-backend/owge-business/src/bo/mission_processor/explore.rs` | Ported. `planet_explored_event` emission is real (`explore.rs:33-38`, `DeferredEmit::PlanetExplored`) — the file's own comment at `explore.rs:86` ("TODO(M3/M4): socketIoService.sendMessage(...)") is STALE relative to the actual code, which already schedules the emit via the `emits` deferred queue (post-commit, not synchronous like Java — see §6 item 3). `hiddenUnitBo.defineHidden`/`ObtainedUnitUtil.handleInvisible` on B13's `unitsInPlanet` are explicitly TODO (`explore.rs:113`, blocked on DTO widening per `running_mission_finder_bo.rs:67-74`). |
| `GatherMissionProcessor` (B16-B19) | `rust-backend/owge-business/src/bo/mission_processor/gather.rs` | Ported, including the `f32`-precision faction-percentage division (`gather.rs:62-69`, an explicit, well-documented bit-for-bit-parity choice) and the `mission_gather_result` deferred emit (`gather.rs:92-98`). |
| `AttackMissionProcessor.triggerAttackIfRequired` (B16 gate) | `rust-backend/owge-business/src/bo/mission_processor/attack.rs:96-118`, config lookup `mod.rs:193-206` | Ported; same `MISSION_<TYPE>_TRIGGER_ATTACK` config key/default-FALSE, same `are_units_involved` DEPLOYED/alliance semantics. |
| `MissionInterceptionManagerBo` (B20) | `rust-backend/owge-business/src/bo/mission_interception_manager_bo.rs` (invoked from `unit_mission_bo.rs:421-495`) | Present and wired into `do_run_unit_mission`; not independently re-read line-by-line in this pass. |
| `ReturnMissionRegistrationBo.doRegisterReturnMission` (B21) | `rust-backend/owge-business/src/bo/return_mission_registration_bo.rs:34-91` | Ported. Deliberately DROPS its own `emitLocalMissionChangeAfterCommit(returnMission)` call (comment at `:84-89`), relying on every call site to already refresh the relevant running-mission list — see §6 item 2 for the one path (`my_cancel_mission`) where this substitution isn't a byte-for-byte match. |
| `UnitMissionBo.myCancelMission` (B22-B25) | `rust-backend/owge-business/src/bo/unit_mission_bo.rs:280-336` | Ported: same three guards (not-found, not-owner, is-return-mission) in the same order, same duration-left formula (`:310-320`), runs under the same lock superset. Emits `emit_unit_missions` unconditionally after (`:334`) — see §6 item 2 re: whether this is a faithful substitute for Java's implicit `emitLocalMissionChangeAfterCommit(returnMission)`. **No resolved-state guard, matching Java's B23 gap exactly** (shared behavior, not a divergence). |
| `RunningMissionFinderBo` (B26-B28) | `rust-backend/owge-business/src/bo/running_mission_finder_bo.rs` | `count_user_running_missions`/`find_user_running_missions`/`find_enemy_running_missions`/`find_build_missions` all present. Explicit, well-documented TODOs: `cleanUpUnexplored`'s planet-DTO nullification is only PARTIALLY done (name/richness/home via `clean_up_unexplored()`, called at `:167-173`, but `special_location` nulling is blocked on a separate DTO TODO per the file header, `:39-44`); `hiddenUnitBo.defineHidden`/`ObtainedUnitUtil.handleInvisible` for `find_enemy_running_missions` are ENTIRELY TODO (`:228-257`, blocked on making `ObtainedUnitDto.unit`/`.count` optional) — so enemy-mission-list responses currently leak hidden/invisible unit identity+count in Rust where Java would suppress it. **This is a live parity gap worth its own scenario** (not written into §3 since it's outside the EXPLORE/GATHER/CANCEL happy path, but any scenario using `find_enemy_running_missions` as a `Then` on a hidden unit would currently show Java/Rust diverge). |
| `PlanetExplorationService.isExplored` (used by B2, B12) | Java: `PlanetExplorationService.java:25-32`. Rust: `unit_mission_bo.rs:630-641` (registration path) is faithful (checks ownership OR explored_planets); but `running_mission_finder_bo.rs:497-509`'s `is_planet_explored_by_user` (used for B27's cleanUpUnexplored and B28's source-planet reveal) is **NOT** — see §6 item 5. | Split status: correct in the registration path, incomplete in the running-mission-query path. |

## 6. Open questions / suspected divergences

1. **Rust's general (non-cross-galaxy) unit-type/speed-group mission-support
   check silently drops the `OWNED_ONLY` ownership refinement.**
   Java's `EntityCanDoMissionChecker.canDoMission`
   (`EntityCanDoMissionChecker.java:28-44`) is used for BOTH the general
   `checkUnitsCanDoMission` path (B8,
   `MissionRegistrationUnitTypeChecker.java:35,39-40`) AND the cross-galaxy
   path (B9, `CrossGalaxyMissionChecker.java:47`) — in both cases,
   `MissionSupportEnum.OWNED_ONLY` means "only if the invoking user owns the
   TARGET planet." Rust's cross-galaxy path replicates this correctly
   (`speed_group_can_do_mission`, `unit_mission_registration_bo.rs:1084-1106`,
   explicitly branches `Some("OWNED_ONLY") =>
   is_of_user_property(...)`). But Rust's general-path
   `check_units_can_do_mission` (`unit_mission_registration_bo.rs:884-937`)
   only tests `column == "NONE"` (reject) vs. anything else (accept) — an
   `OWNED_ONLY`-configured unit type or speed group would be WRONGLY ALLOWED
   by Rust to travel to an unowned/enemy target planet where Java would
   reject it. This is squarely in the EXPLORE/GATHER domain if any unit type
   used for exploring/gathering is configured `OWNED_ONLY` for
   `can_explore`/`can_gather` (a plausible content choice — e.g. a "colony
   scout" unit type that may only gather from your own planets). **Needs a
   scenario**: a unit type with `can_gather=OWNED_ONLY` sent to gather from an
   unowned planet — Java should reject (`SgtBackendInvalidInputException`),
   Rust currently would not. Recommend checking `unit_types`/
   `speed_impact_groups` seed data for any `OWNED_ONLY` value on
   `can_explore`/`can_gather` to confirm this is reachable with real content,
   not just a theoretical config value.

2. **GATHER's overcounted-casualties-after-trigger-attack gap (B16) is Java's
   own behavior, not a Rust divergence** — flagging it here because it's the
   kind of thing layer-2 (full-DB diff) parity testing could mask if BOTH
   backends replicate the same overcount identically (need to verify Rust's
   `gather.rs` also captures `involved_units` before, not after, calling
   `attack::trigger_attack_if_required` — a quick read of `gather.rs:31-49`
   shows the units passed to `process(...)` are the caller-supplied
   `involved_units` slice, and the gather-amount sum loop (`:41-49`) uses that
   same pre-attack slice — so Rust reproduces the same overcount as Java.
   Confirmed NOT a divergence, but worth a dedicated Gherkin scenario per
   pitfall #11 ("when both are wrong, flag it, don't silently match") since
   this looks like a genuine spec-level bug: a gather mission whose escort
   dies in the triggered defensive combat still gets credited resources as if
   its full pre-combat charge capacity survived.

3. **`myCancelMission`'s implicit websocket emit vs. Rust's explicit
   `emit_unit_missions` call — worth a targeted parity scenario, not
   obviously wrong.** Java's cancel path emits ONLY through
   `registerReturnMission` → `emitLocalMissionChangeAfterCommit(returnMission)`
   → `emitLocalMissionChange` → `emitUnitMissions(userId)` +
   conditionally `emitEnemyMissionsChange(returnMission)`
   (`MissionEventEmitterBo.java:31-34,44-49,72-78`). Rust's
   `ReturnMissionRegistrationBo::register_return_mission` deliberately drops
   its own emit (comment, `return_mission_registration_bo.rs:84-89`), and
   `my_cancel_mission` compensates with an explicit
   `MissionEventEmitter::emit_unit_missions(&mut *conn, user_id)`
   (`unit_mission_bo.rs:334`) — this covers the dominant `unit_mission_change`
   effect but appears to SKIP the `enemy_mission_change` half Java's path
   would send (to the return mission's target-planet owner, which is
   `originMission.getTargetPlanet().getOwner()` — for GATHER this could be a
   planet owned by a different player, e.g. gathering FROM a planet someone
   else owns, unlike EXPLORE which is more commonly unowned targets). If a
   `GATHER` mission targets an enemy-owned planet and is then cancelled,
   Java would push `enemy_mission_change` to that owner (their view of the
   incoming mission should disappear/update) while Rust currently would not.
   **Needs a scenario**: user 1 gathers from a planet owned by user 2,
   cancels mid-flight, assert user 2 receives `enemy_mission_change`.

4. **`MissionTimeManagerBo`'s speed-adjusted duration formula
   (`calculateTimeUsingSpeed`, B10) was read on the Java side in full
   (`MissionTimeManagerBo.java:77-142`) but the Rust
   `mission_time_manager_bo.rs` file was only referenced, not read, in this
   pass** (time budget) — flag for a follow-up inventory pass or a dedicated
   numeric-parity scenario (e.g. two planets with a known planet-number/
   quadrant/sector delta, asserting `missions.required_time` matches between
   backends within a normalize-friendly tolerance, per the plan's "normalize
   over synchronize" rule for time fields).

5. **`running_mission_finder_bo.rs`'s `is_planet_explored_by_user` omits the
   ownership branch of `PlanetExplorationService.isExplored`.** Java:
   `planetRepository.isOfUserProperty(userId, planetId) ||
   exploredPlanetRepository.findOneByUserIdAndPlanetId(...) != null`
   (`PlanetExplorationService.java:29-32`). Rust
   (`running_mission_finder_bo.rs:497-509`) only runs the `explored_planets`
   COUNT query — no ownership OR-branch, and its own doc comment
   (`:491-496`) even mis-describes the Java method it's porting (claims it's
   just `findOneByUserAndPlanet(...) != null`, omitting the ownership
   disjunct entirely — likely how the gap was introduced). Used at
   `running_mission_finder_bo.rs:169` (B27 cleanUpUnexplored on an EXPLORE
   mission's target — low practical impact, a user's own EXPLORE mission
   target is essentially never already the user's own property) and `:262`
   (B28 enemy-mission source-planet reveal — a source planet the exploring/
   gathering user owns would trivially already be "explored" by ownership in
   Java, so this could matter if a source planet somehow both belongs to the
   viewing user and lacks an `explored_planets` row — an edge case, but
   possible if ownership changed after exploration bookkeeping, e.g. via
   conquest). Low-priority but cheap to fix and cheap to write a scenario
   for; noting rather than escalating.

6. **Cancelling an already-resolved mission (B23) creates a phantom
   unit-less `RETURN_MISSION` — confirmed present in BOTH backends
   identically** (Java: no resolved-state guard,
   `UnitMissionBo.java:187-207`; Rust: same,
   `unit_mission_bo.rs:280-336`) — per pitfall #11, this is a
   spec-level question for Kevin, not something to "fix" unilaterally in
   either backend to make a scenario pass. Recommend a scenario that
   DELIBERATELY exercises this (cancel a mission twice, or cancel after it
   auto-resolved) so layer-1 `JAVA_SPEC` captures the CURRENT intended
   behavior (even if it's a wart) and any future intentional fix shows up as
   a deliberate spec change to the Gherkin, not a silent parity drift.
