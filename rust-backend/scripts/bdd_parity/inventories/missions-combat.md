# missions-combat — Java reference behavior inventory

Scope: ATTACK and COUNTERATTACK unit missions, plus interception (speed-impact-group)
and unit-capture mechanics inside combat resolution. Java is the reference backend.
Produced for the OWGE BDD parity harness (`rust-backend/docs/BDD-PARITY-PLAN.md`).

All file:line citations are against the working tree at commit `b55e6afc` (2026-07-09).
Where an existing audit (`rust-backend/pending_migration/ATTACK_FEATURE_AUDIT.md`,
"the Part-0 audit") already did line-for-line Java/Rust comparison for the core combat
math, this document cites it rather than re-deriving the same table, and focuses the
new ground: the REST/registration layer, exceptions, interception, capture report
shape, and cascades that the Part-0 audit didn't cover (it starts from an
already-built `AttackInformation`, i.e. post-registration).

---

## 1. Endpoints

| Verb + path | Controller file:line | Bo entry point |
|---|---|---|
| `POST game/mission/attack` | `game-rest/src/main/java/com/kevinguanchedarias/owgejava/rest/game/MissionRestService.java:41-44` | `UnitMissionBo.myRegisterAttackMission` (`business/.../business/UnitMissionBo.java:127`) → `adminRegisterAttackMission:132` → `commonMissionRegister:304` |
| `POST game/mission/counterattack` | `MissionRestService.java:46-49` | `UnitMissionBo.myRegisterCounterattackMission` (`UnitMissionBo.java:138`) → `adminRegisterCounterattackMission:143` (target-planet-ownership guard) → `commonMissionRegister:304` |
| `POST game/mission/cancel?id={missionId}` | `MissionRestService.java:61-65` | `UnitMissionBo.myCancelMission` (`UnitMissionBo.java:188`) — applies to any unresolved unit mission including in-flight ATTACK/COUNTERATTACK, not combat-specific but listed because it's the only way to abort a combat mission before it fires |
| *(no HTTP path — async firing)* | `DbSchedulerRealizationJob` (fired by db-scheduler `OWGE_BACKGROUND`, task_name=`mission-run`) | `UnitMissionBo.runUnitMission(missionId, missionType)` (`UnitMissionBo.java:217`) → `doRunUnitMission:279` → interception gate → `AttackMissionProcessor.process` (`business/.../mission/processor/AttackMissionProcessor.java:46`) or, for COUNTERATTACK, `CounterattackMissionProcessor.process` (`CounterattackMissionProcessor.java:26`) which is a **pure delegate** to `AttackMissionProcessor.process` — COUNTERATTACK combat resolution is byte-identical code to ATTACK, only the registration-time guard differs (B3 below) |
| *(indirect — combat reused by other mission types)* | `AttackMissionProcessor.triggerAttackIfRequired` (`AttackMissionProcessor.java:54-62`), called from `GatherMissionProcessor`/`DeployMissionProcessor`/`EstablishBaseMissionProcessor`/`ExploreMissionProcessor`/`ReturnMissionProcessor` | Gated per mission type by `configuration` row `MISSION_<TYPE>_TRIGGER_ATTACK` (`AttackMissionManagerBo.isAttackTriggerEnabledForMission`, `business/.../mission/attack/AttackMissionManagerBo.java:71-74`) — out of scope for this inventory's Gherkin (belongs to each of those missions' own feature file) but material to combat parity because it's the SAME `processAttack` code path; noted in §6 open questions |

No separate GET/report endpoint is combat-specific: mission reports are read via the
generic `game/missionReport` sync source (`MissionReportBo`), out of this inventory's
scope.

---

## 2. Behavior catalog

### 2.1 Registration-time validation (shared by ATTACK and COUNTERATTACK)

Both `POST .../attack` and `POST .../counterattack` flow through
`UnitMissionBo.commonMissionRegister` (`UnitMissionBo.java:304-325`) →
`UnitMissionRegistrationBo.doCommonMissionRegister` (`business/.../mission/unit/registration/UnitMissionRegistrationBo.java:43-77`),
inside the source+target planet lock (`planetLockUtilService.doInsideLockById`,
`UnitMissionBo.java:319-324`).

**B1. User does not exist.**
Trigger: `missionInformation.userId` not present in `user_storage` (only reachable via
`adminRegisterAttackMission`/`adminRegisterCounterattackMission` directly — the `my*`
REST path always sets a real logged-in user id, so this is effectively an admin-API /
defensive check).
`MissionRegistrationUserExistsChecker.checkUserExists` (`.../checker/MissionRegistrationUserExistsChecker.java:19-23`).
Exception: `UserNotFoundException`. No DB writes, no websocket emission.

**B2. Counterattack target planet not owned by sender.**
Trigger: COUNTERATTACK only. `missionInformation.targetPlanetId` is not
`isOfUserProperty(userId, targetPlanetId)`.
`UnitMissionBo.adminRegisterCounterattackMission` (`UnitMissionBo.java:143-150`), same
check exists identically in Rust (`unit_mission_bo.rs:132-139`).
Exception: `SgtBackendInvalidInputException("TargetPlanet doesn't belong to sender
user, ...")`. Semantics: counterattack only makes sense when the "target" of the
counter-strike is a planet the counter-attacker itself owns (i.e., you're
counter-attacking units sitting on YOUR planet). No DB writes.

**B3. Source or target planet doesn't exist.**
`MissionRegistrationPlanetExistsChecker.checkPlanetExists`
(`.../checker/MissionRegistrationPlanetExistsChecker.java:18-22`), called for both
source and target in `MissionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits`
(`.../MissionRegistrationObtainedUnitLoader.java:45-46`).
Exception: `PlanetNotFoundException`.

**B4. Target planet not explored by the sender.**
`UnitMissionBo.commonMissionRegister` (`UnitMissionBo.java:314-318`) — skipped only for
`MissionType.EXPLORE`, so it always applies to ATTACK/COUNTERATTACK.
Exception: `SgtBackendInvalidInputException("Can't send this mission, because target
planet is not explored")`.

**B5. Mission limit reached.**
`MissionBaseService.checkMissionLimitNotReached`
(`business/.../mission/MissionBaseService.java:77-83`), called from
`UnitMissionBo.commonMissionRegister:309` before registration (skipped only for a DEPLOY
onto your own target planet — irrelevant to ATTACK/COUNTERATTACK, always checked).
Condition: `count(unresolved missions for user) + 1 >= findUserMaxAllowedMissions(user)`.
Exception: `SgtBackendInvalidInputException` with i18n key `I18N_ERR_MISSION_LIMIT_EXCEEDED`.

**B6. `involvedUnits` empty.**
`MissionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits`
(`.../MissionRegistrationObtainedUnitLoader.java:48-50`).
Exception: `SgtBackendInvalidInputException("involvedUnits can't be empty")`.

**B7. Same unit (id + expirationId) selected twice in the payload.**
`checkRepeatedUnitAndAdd` (`.../MissionRegistrationObtainedUnitLoader.java:88-94`), also
applied to nested `storedUnits`.
Exception: `SgtBackendInvalidInputException` i18n `I18N_ERR_REPEATED_UNIT`.

**B8. Missing `count` for a selected unit.**
`handleSelectedUnit` (`.../MissionRegistrationObtainedUnitLoader.java:104-106`).
Exception: `SgtBackendInvalidInputException("No count was specified for unit " + id)`.

**B9. Stored-unit total weight exceeds carrier capacity.**
`checkTotalHeight` (`.../MissionRegistrationObtainedUnitLoader.java:75-86`): for a
carrier unit with `storedUnits`, `sum(storedUnit.count * storedUnit.unit.storedWeight) >
carrier.count * carrier.unit.storageCapacity`.
Exception: `SgtBackendInvalidInputException` i18n `I18N_ERR_MAX_WEIGHT_OVERPASSED`.

**B10. Carrier can't store the selected stored-unit type (no `UNIT_STORES_UNIT` rule).**
`MissionRegistrationCanStoreUnitChecker.checkCanStoreUnit`
(`.../checker/MissionRegistrationCanStoreUnitChecker.java:34-38`), invoked per
`storedUnit` before `handleSelectedUnit` (`MissionRegistrationObtainedUnitLoader.java:58`).
Exception: `SgtBackendInvalidInputException` i18n `I18N_CANT_STORE_UNIT`.

**B11. Not enough units at the source (insufficient count).**
`ObtainedUnitBo.saveWithSubtraction` inner branch — not read line-for-line here (out of
this inventory's primary files) but reachable from
`MissionRegistrationObtainedUnitLoader.handleSelectedUnit:113`. Throws
`SgtBackendInvalidInputException("Can't not subtract because, obtainedUnit count is less
than the amount to subtract")` per `ObtainedUnitBo.java:159-160` (seen while reading
`saveWithSubtraction`/`saveWithChange` neighborhood, §"moveUnit" excerpt above).

**B12. Unit type doesn't support ATTACK/COUNTERATTACK mission type.**
`MissionRegistrationUnitTypeChecker.checkUnitsCanDoMission`
(`.../checker/MissionRegistrationUnitTypeChecker.java:22-48`), via
`unitTypeBo.canDoMission` → reflective `getCanAttack()`/`getCanCounterattack()` on
`UnitType`, resolved by `EntityCanDoMissionChecker.canDoMission`
(`business/.../mission/checker/EntityCanDoMissionChecker.java:28-44`): `ANY` → allowed,
`OWNED_ONLY` → allowed only if `targetPlanet` belongs to the sender, anything else
(including `NONE`/null) → denied.
Exception: `SgtBackendInvalidInputException` (hacker-flavored message,
`MissionRegistrationUnitTypeChecker.java:36-37`).

**B13. Unit's speed-impact-group doesn't support the mission (per-unit, non-carried only).**
Same file, `unitsThatCannotGoToMission` (`MissionRegistrationUnitTypeChecker.java:39-46`),
filters `ownerUnit == null` (i.e. carried/stored units are exempt — they inherit the
carrier's speed group implicitly since only the carrier moves).
Exception: `SgtBackendInvalidInputException` listing the offending unit names.

**B14. Cross-galaxy mission without an unlocked `SPEED_IMPACT_GROUP` relation.**
`CrossGalaxyMissionChecker.checkCrossGalaxy`
(`business/.../mission/checker/CrossGalaxyMissionChecker.java:26-60`): only evaluated
when `sourcePlanet.galaxy.id != targetPlanet.galaxy.id`; for each non-carried unit,
resolves its (unit-level or unit-type-level) `speedImpactGroup`, requires
`entityCanDoMissionChecker.canDoMission(...)` AND
`unlockedRelationBo.isUnlocked(user, objectRelation(SPEED_IMPACT_GROUP, speedGroup.id))`.
Exception: `SgtBackendInvalidInputException` ("This speed group doesn't support this
mission outside of the galaxy" OR "Don't try it.... you can't do cross galaxy missions").
Side note: a `null` `objectRelation` only logs a warning (`log.warn`, line 54) and treats
the check as passed — a latent gap if reference data is missing, not exception-worthy.

**B15. Successful registration (happy path) — DB effects.**
`UnitMissionRegistrationBo.doCommonMissionRegister` (lines 43-77):
- `missions` row inserted: `type` = 8 (ATTACK) or 9 (COUNTERATTACK)
  (`enumerations/MissionType.java:4-5`), `source_planet`, `target_planet`, `user_id`,
  `resolved=0`, `invisible` computed (see B16).
- `obtained_units` rows for the selected stacks are subtracted at the source (and
  deleted if fully consumed) via `saveWithSubtraction`, then the *new* mission-attached
  stacks are inserted/updated with `mission_id` = the new mission id and
  `target_planet` = destination (`MissionRegistrationUnitManager.manageUnitsRegistration`,
  not fully read but referenced at `UnitMissionRegistrationBo.java:56-58`).
- `scheduled_tasks` row inserted: `task_name='mission-run'`, `task_instance=<mission id>`
  (`MissionSchedulerService.scheduleMission`, called at `UnitMissionRegistrationBo.java:68`).
- Websocket: `unit_mission_change` to the sender (`missionEventEmitterBo.emitLocalMissionChangeAfterCommit`,
  line 69, post-commit — emits `unit_mission_change` + conditionally `enemy_mission_change`
  to the mission's target-planet owner iff `mission.invisible==false`,
  `MissionEventEmitterBo.emitLocalMissionChange:72-78`).
- Websocket: `unit_obtained_change` to the sender IF the sender owns the *source* planet
  (`obtainedUnitEventEmitter.emitObtainedUnitsAfterCommit(user)`, line 70-72).
- Websocket: if the source planet is itself enemy territory to the sender (i.e. units
  were dispatched from a foreign/captured stationing — `planetUtilService.isEnemyPlanet`,
  line 54) — `enemy_mission_change` to the source planet's owner PLUS
  `maybeUpdateMissionsVisibility` recompute for any sibling missions whose visibility
  changed (lines 73-76).

**B16. Mission-invisible flag (stealth).**
`MissionRegistrationInvisibleManager.handleDefineMissionAsInvisible`
(`.../MissionRegistrationInvisibleManager.java:20-24`): `mission.invisible = true` iff
**every** involved `ObtainedUnit`'s `(user, unit)` pair is `hiddenUnitBo.isHiddenUnit(...)`
(all-stealth stack). Affects B15's `enemy_mission_change` gating — an all-stealth attack
never notifies the target planet's owner of the incoming mission via
`emitLocalMissionChange` (though the *combat resolution itself*, once it fires, still
mutates and emits normally — invisibility only hides the in-flight mission, not the
outcome). `maybeUpdateMissionsVisibility` (lines 26-36) recomputes this flag for
*existing* sibling missions when a stack is drawn down enough to flip all-stealth
status, and re-saves only the missions whose flag actually changed.

### 2.2 Interception (pre-combat gate, runs before ANY unit-mission processor including ATTACK/COUNTERATTACK)

Entry: `UnitMissionBo.doRunUnitMission` (`UnitMissionBo.java:279-292`) calls
`MissionInterceptionManagerBo.loadInformation` (`business/.../mission/MissionInterceptionManagerBo.java:26-53`)
**before** dispatching to `missionProcessorMap.get(missionType)`. Skipped entirely only
for `MissionType.RETURN_MISSION` (line 33).

**B17. No interceptors apply → combat proceeds normally.**
`totalInterceptedUnits == 0`; `isMissionIntercepted=false`; processor dispatch proceeds
unchanged (`doRunUnitMission:281-287`).

**B18. Partial interception (some but not all involved units intercepted).**
`UnitInterceptionFinderBo.checkInterceptsSpeedImpactGroup`
(`business/.../speedimpactgroup/UnitInterceptionFinderBo.java:28-51`): for every
defender-side unit at the target planet with a non-empty
`unit.interceptableSpeedGroups` (`obtainedUnitFinderBo.findInvolvedInAttack`), every
involved attacker unit whose *applicable* speed-impact-group
(`speedImpactGroupBo.canIntercept`) is in that interceptor's set, not already claimed by
an earlier interceptor (`alreadyIntercepted`), and who is an enemy of the interceptor
(`allianceBo.areEnemies`) is captured into that interceptor's bucket (grouped by
interceptor **user**, first interceptor unit of that user wins the "representative"
slot, `interceptedMap`).
DB effects: intercepted `obtained_units` rows are **deleted**
(`MissionInterceptionManagerBo.deleteInterceptedUnits`, line 76-78, called at line 39 —
before the `involvedUnits` are re-read at line 40); combat then proceeds with the
*reduced* `involvedUnits` set (fewer/no stacks reach `buildAttackInformation`).
Report: original (pre-interception) `involvedUnits` + interception breakdown appended
to the *combat mission's own report* via
`maybeAppendDataToMissionReport` (`MissionInterceptionManagerBo.java:55-64`,
`UnitMissionBo.doRunUnitMission:284`) — attaches `interceptionInfo` key alongside the
normal `attackInformation` key in the SAME `mission_reports` row. Separately, one
`mission_reports` row per interceptor user is created via
`sendReportToInterceptorUsers` → `doSendReportToInterceptorUser`
(`UnitInterceptionFinderBo.java:53-66`): a report scoped to just that interceptor,
`involvedUnits=[interceptorUnit]`, `interceptionInfo=[thatInterceptorsEntry]`, saved
`isEnemy=true` via `missionReportBo.create(builder, true, interceptorUser)`.

**B19. Full interception (every involved unit intercepted).**
`isMissionIntercepted = totalInterceptedUnits == involvedUnits.size()`
(`MissionInterceptionManagerBo.java:37`). Combat processor is **never invoked**.
`UnitMissionBo.doRunUnitMission:288-291` calls
`MissionInterceptionManagerBo.handleMissionInterception`
(`MissionInterceptionManagerBo.java:66-74`):
- `mission.resolved = true` (mission ends here, no return-mission is scheduled for a
  fully-intercepted attack — the units are gone, not returning).
- A **new** standalone `mission_reports` row is created for the mission's own user via
  `reportFullMissionInterception` → `missionReportManagerBo.handleMissionReportSave`
  (line 69-71, 80-84) with `involvedUnits` = the original pre-interception set +
  `interceptionInfo`.
- Intercepted `obtained_units` rows deleted (again — idempotent no-op since B18 already
  deleted them during `loadInformation`; kept for the "full" code path's own safety, see
  open question §6).
- One `mission_reports` row per interceptor user (`sendReportToInterceptorUsers`, same
  as B18).
- Websocket: `missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission)`
  (`UnitMissionBo.java:290`) → `unit_mission_change` to the attacker (mission
  disappears from their running-missions list) — no `enemy_mission_change` to the
  interceptor here (that arrives via the interceptor's own `mission_reports` row /
  `mission_report_new` websocket event, not a mission-list event, since the
  interceptor's planet was never a mission source/target).
- **No `AttackInformation` is ever built** — no shuffle draw, no capture roll, no
  `unit_mission_change`/`unit_obtained_change` for the defender who would otherwise have
  fought. Requirement cascades (B27) do not fire because `AttackMissionProcessor.process`
  never runs.

### 2.3 Combat resolution (`AttackMissionProcessor.processAttack`, `AttackMissionManagerBo`)

Once interception has NOT fully consumed the mission, `AttackMissionProcessor.process`
(`business/.../mission/processor/AttackMissionProcessor.java:44-48`) is invoked, calling
`processAttack(mission, survivorsDoReturn=true, isTriggeredByEvent=false)` (line 64-93).
For a self-triggered attack the deep combat math (shuffle → per-attacker-stack
targeting → damage → survive/wipe → capture roll → points) is the Part-0-audited core;
**this inventory cites that audit rather than re-deriving it** — see
`ATTACK_FEATURE_AUDIT.md` items 1-25 for the byte-level Java/Rust comparison of each
sub-step. What follows is the outer envelope (report/emit/cascade) the audit did not
cover, plus the deterministic-RNG framing this harness depends on.

**B20. `AttackInformation` assembly (targeting universe).**
`AttackMissionManagerBo.buildAttackInformation` (`business/.../mission/attack/AttackMissionManagerBo.java:60-69`):
defenders = `obtainedUnitFinderBo.findInvolvedInAttack(targetPlanet)` (planet-resident /
DEPLOYED / CONQUEST≥10% stacks, excluding the attacking mission's own stack if it's
already sitting there), THEN the attacker's own mission-attached stacks
(`obtainedUnitRepository.findByMissionId`). Order is load-bearing for the shuffle (see
B21). = audit item 1.

**B21. Deterministic RNG gate.**
`AttackMissionManagerBo.startAttack` (lines 96-132): when `configuration` row
`ATTACK_DETERMINISTIC_RNG='TRUE'`, seeds `new Random(attackMission.getId())` once per
attack and threads it through `AttackInformation` (`startAttack:99-105`); OFF (default,
production) uses `Collections.shuffle(units)` / `Math.random()` (clock-seeded, not
reproducible). `AttackObtainedUnitBo.shuffleUnits(units, attackInformation)`
(`business/.../mission/attack/AttackObtainedUnitBo.java:61-72`) performs the shuffle —
explicit Fisher-Yates identical to `Collections.shuffle(list, rnd)` when deterministic,
tracing each `shuffle` draw. = audit item 5 (headline RNG divergence D1, now closed by
`JavaRandom` per `ATTACK_PARITY_PLAN.md` "Implementation status").

**B22. Enemy resolution / ally-not-attacked rule.**
`startAttack:107-109`: each user's `attackableUnits` = all units in the attack universe
belonging to a user `allianceBo.areEnemies(user, that unit's user)` returns true for.
`AllianceBo.areEnemies` (`business/.../business/AllianceBo.java:119-122`): **not
enemies** (mutually un-attackable) iff same user id, OR both have a non-null alliance
AND it's the same alliance id. A user with `alliance=NULL` is enemies with everyone
except themself. = audit item 6.

**B23. Per-stack targeting, attack-rule filter, critical-score sort, damage application.**
`doAttack`/`attackTarget`/`addPointsAndUpdateCount`
(`AttackMissionManagerBo.java:158-251`) — full formula parity confirmed by the Part-0
audit items 7-9, 13, 16-17 (D0 improvement-inheritance bug and its fix already resolved
per `ATTACK_PARITY_PLAN.md`). Key branches for this catalog:
  - **Partial kill / attacker survives, victim damaged not wiped**
    (`victimHealth > myAttack`, lines 191-206): attacker's `pendingAttack=0`,
    `noAttack=true` (this attacker stack stops targeting further victims this
    combat — one stack, one target, per combat pass); victim's shield/health split
    the damage `/2` unless `bypassShield`, with negative-shield rollover into health
    (lines 201-203).
  - **Victim wiped, attacker carries leftover damage forward**
    (else branch, lines 207-219): `pendingAttack = myAttack - victimHealth`, clamped to
    the attacker's `originalAttackValue` (can't exceed what it started the combat pass
    with, line 209-211) — this is what lets one attacker stack overkill through
    multiple victim stacks in the same pass. Victim `finalCount=0`,
    `obtained_units` row **deleted** (`obtainedUnitRepository.delete`, line 216),
    carrier-freeing (`maybeUnsetHolderUnit`, B24) and mission-emptying check
    (`deleteMissionIfRequired`, B25) both run.
  - **Full kill of the attacking stack itself** (attacker becomes the "victim" of a
    later target in the same pass, or a subsequent attacker in `doAttack`'s outer
    loop targets it): same wipe branch, same carrier/mission-delete cascade.

**B24. Carrier-freed-on-wipe.**
`maybeUnsetHolderUnit` (`AttackMissionManagerBo.java:223-233`): if the wiped unit's id
is in `attackInformation.unitsStoringUnits` (i.e. it was carrying other stacks), every
in-memory `AttackObtainedUnit` whose `ownerUnit.id` matches gets `ownerUnit=null`
(persisted later as `UPDATE obtained_units SET owner_unit_id=NULL` — audit item 11).
= a carried stack survives its carrier's death, un-stored, on the same planet.

**B25. Mission auto-deletion when its last stack dies.**
`deleteMissionIfRequired` (`AttackMissionManagerBo.java:260-270`): after a wipe, if
`!obtainedUnitRepository.existsByMission(wipedUnit.mission)`: if that mission IS the
attack mission itself, `attackInformation.removed=true` (own attack wiped out — see
B26); otherwise (a *different* in-flight mission's stack was killed as a bystander
victim, e.g. a DEPLOYED stack or another user's incoming mission caught in the crossfire)
the OTHER mission row is deleted outright and its owner added to
`usersWithDeletedMissions` (drives the B15-style emit fan-out in `startAttack:112-117`:
`unit_mission_change` + `user_data_change` to that owner, and that user's id is removed
from `usersWithChangedCounts` since "deleted" supersedes "changed count").

**B26. The attacking mission itself is wiped out (`attackInformation.removed=true`).**
Set in B25 when the ATTACK/COUNTERATTACK mission's own last stack dies mid-combat.
`AttackMissionProcessor.processAttack` (`AttackMissionProcessor.java:68`): survivors-do-return
is **skipped** (`survivorsDoReturn && !attackInformation.isRemoved()` — no return mission
registered, there's nothing left to return). `mission.resolved=true` still set (line 71)
— the mission row survives as a resolved/removed husk (its own report still gets
written) rather than being deleted, unlike B25's *other*-mission-wiped branch. Emits
`emitLocalMissionChangeAfterCommit` unconditionally when `removed` (line 87-89, "Maybe
useless?, should test" — Java's own comment, flagged verbatim as an open question §6).

**B27. Requirement re-triggers on unit loss (`HAVE_UNIT` / `UNIT_AMOUNT`).**
`AttackMissionProcessor.triggerUnitRequirementChange`
(`AttackMissionProcessor.java:95-104`), invoked once per **distinct**
`AttackObtainedUnit` (`.stream().distinct()`, line 90) after the report is built:
  - `finalCount == 0` (stack wiped) → `requirementBo.triggerUnitBuildCompletedOrKilled(user,
    unit)` (`business/.../business/RequirementBo.java:188-200`) — re-evaluates both
    `HAVE_UNIT` (does the user still have ANY of this unit) and `UNIT_AMOUNT` relations.
  - `finalCount != initialCount` but `> 0` (partial kill, stack survives reduced) →
    `requirementBo.triggerUnitAmountChanged(user, unit)`
    (`RequirementBo.java:202-208`) — re-evaluates only `UNIT_AMOUNT` relations whose
    `third_value <= new count`. Both paths run inside
    `userPlanetLockService.runLockedForUser` (locks every planet the user owns —
    relevant to the CLAUDE.md-documented lock-ordering discipline).
  - Cascade: any `UNIT_AMOUNT`-gated `object_relations` row whose threshold is crossed
    (in either direction — losing units can drop a user below a "have ≥N of unit X"
    unlock) is processed via `processRelationList`, which can insert/delete
    `unlocked_relation` rows and (for a requirement group with further gated content)
    recurse. This is the "unit-amount threshold on kill" case the plan's Phase 3
    explicitly calls out as unported-risk territory.

**B28. Points and survivor persistence.**
`AttackMissionManagerBo.updatePoints` (lines 134-156, audit items 9/14/22): for every
user in the attack, `userStorageBo.addPointsToUser(user, earnedPoints)` (`UPDATE
user_storage SET points = points + earned`); for every stack with
`finalCount != 0 && finalCount != initialCount` (survived but was hit),
`obtainedUnitBo.saveWithChange(unit, -killed)` — wrapped in try/catch for
`OwgeElementSideDeletedException` (log-and-skip if the row was already deleted earlier in
the same tx, line 143-148, audit item 15/D4). Post-commit, `alteredUsers` (union of users
with a changed-count stack, plus `usersWithChangedCounts`) get
`unitTypeBo.emitUserChange` + `obtainedUnitEventEmitter.emitObtainedUnits`
(lines 151-155).

**B29. Per-user post-combat websocket emit block.**
`startAttack:112-131` (audit item 21/D6): for `usersWithDeletedMissions` —
`unit_mission_change`, improvement-cache clear, `user_data_change`, and removal from
`usersWithChangedCounts` (dedup, a deleted-mission user's counts are already covered).
For the remaining `usersWithChangedCounts` — IF that user owns the target planet,
`unit_obtained_change` plus (conditionally, when there were also deleted missions or
more than one changed-counts user) `enemy_mission_change` to the target owner; then
unconditionally for every changed-counts user: improvement-cache clear,
`unit_mission_change`, `user_data_change`. Finally `attackEventEmitter.emitAttackEnd`
(line 131) fires the capture report fan-out (B31/B32).

**B30. Combat mission report (`mission_reports.json_body.attackInformation`).**
`AttackMissionProcessor.processAttack` (lines 72-81): one `UnitMissionReportBuilder`
(`business/.../builder/UnitMissionReportBuilder.java`) built via
`withAttackInformation(attackInformation)` (`UnitMissionReportBuilder.java:106-125`) —
shape: `{senderUser, sourcePlanet, targetPlanet, involvedUnits:[], attackInformation:
[{userInfo:{id,username}, earnedPoints, units:[{initialCount, finalCount,
obtainedUnit}]}]}` keyed per-user in `attackInformation.users` iteration order. This ONE
builder is persisted **N+1 times**, once per recipient, via
`missionReportManagerBo.handleMissionReportSave` (`MissionReportManagerBo.java:20-23`
overload): once per non-invoking involved user (`isEnemy=true`, line 76-78), and if
`isTriggeredByEvent` (only true when combat was triggered indirectly via
`triggerAttackIfRequired`, e.g. from GATHER/DEPLOY — NOT for a direct ATTACK/COUNTERATTACK
REST call) once more for the invoker (line 79-81). For a **direct** ATTACK/COUNTERATTACK
mission the invoker's own report row is instead attached via
`MissionReportManagerBo.handleMissionReportSave(mission, builder)` overload
(`MissionReportManagerBo.java:37-45`, called by... **actually not called from
processAttack for the direct path** — see open question §6, the invoker's report
attribution needs runtime verification). Each `MissionReportBo.create`/`.save` triggers,
post-commit, `mission_report_new` (`MissionReportBo.EMIT_NEW`,
`business/.../business/MissionReportBo.java:42,74,82-85`) to that specific report's
user, plus `mission_report_count_change` (`EMIT_COUNT_CHANGE`, lines 43,84,233-235).

**B31. Capture roll (per killed-stack, per attacker→victim pair).**
`HandleUnitCaptureListener.onAfterUnitKilledCalculation`
(`business/.../mission/attack/listener/HandleUnitCaptureListener.java:31-50`), fired from
`addPointsAndUpdateCount` (`AttackMissionManagerBo.java:249`) for EVERY killed-count
event, even a partial-stack kill (killed>0, stack not necessarily wiped) — capture is not
gated on "victim wiped". Rule lookup:
`unitRuleFinderService.findRule(UNIT_CAPTURE, attackerUnit, victimUnit)` OR (fallback)
`findRuleByActiveTimeSpecialsAndTargetUnit` — first match wins, `extra_args` format
`"<prob0-100>#<pct0-100>"`. If a rule matched with both extra-args present:
`captureProbabilityRoll() * 100 < prob` gates whether ANYTHING is captured; if it passes,
`captured = floor(captureAmountRoll() * floor(killed * pct * 0.01) + 1)` (**always at
least 1** once the probability gate passes, regardless of how small `killed*pct` is —
the `+1` is unconditional). = audit items 17-18 (D2/D8 edge notes).

**B32. Capture success — DB effects + report.**
`saveCaptured` (`HandleUnitCaptureListener.java:98-128`): a NEW `obtained_units` row is
built (`unit=victim's unit`, `user=attacker`, `count=captured`, `isFromCapture=true`,
`sourcePlanet`/`targetPlanet` = the attacker mission's own source/target, or the
attacker's own current planet if the attacker unit has no mission i.e. it was a
stationary defender counter-capturing) and persisted via `obtainedUnitBo.moveUnit`
(`ObtainedUnitBo.java:198-220` — merges into an existing matching stack at the
destination if the destination is attacker-owned, else creates/attaches to a DEPLOYED
mission at the foreign planet). The event is recorded into
`AttackInformation`'s per-request context (`addToContext`, in-memory only) for the
end-of-attack report fan-out (B33) — **capture does not, itself, emit a websocket event**
mid-combat; the recipient only learns via the capture report row + the normal
`unit_obtained_change`/`unit_mission_change` batch already covered by B28/B29 (since the
`obtained_units` mutation is folded into the same transaction).

**B33. Capture report fan-out.**
`HandleUnitCaptureListener.onAttackEnd` (lines 81-96), fired by
`attackEventEmitter.emitAttackEnd(attackInformation)` at the very end of `startAttack`
(`AttackMissionManagerBo.java:131`, i.e. AFTER B29's per-user list emit block, same
transaction): one NEW `mission_reports` row **per distinct captor user** (first-seen
order over the in-memory context list, `usersThatCaptured` = `.distinct()` over
`contextData` mapped to `captorUnit.user.user`), `involvedUnits=[]`,
`unitCaptureInformation=[{unit, oldOwner:{id,username}, capturedCount}, ...]` for every
capture event belonging to that captor this attack (`UnitMissionReportBuilder.withUnitCaptureInformation`,
`UnitMissionReportBuilder.java:152-164`), `isEnemy=false`
(`missionReportBo.create(builder, false, userThatCaptured)`, line 94) — this is a
SEPARATE `mission_reports` row from the combat report (B30), NOT merged into it, even
though both belong to the same mission/transaction. Triggers its own
`mission_report_new`/`mission_report_count_change` pair to the captor.

**B34. Capture probability/amount roll fails, or no capture rule exists.**
No `obtained_units` mutation beyond the normal kill, no capture report row, no context
entry. Silent — this is the majority-case branch (`Optional` chain in
`onAfterUnitKilledCalculation` short-circuits at whichever `.filter`/`.map` first fails:
no rule found, rule missing an extra-arg, probability roll fails).

**B35. Shield bypass.**
`AttackBypassShieldService.bypassShields`
(`business/.../mission/attack/AttackBypassShieldService.java:15-20`): true iff the
attacker unit's own `bypassShield` column is TRUE, OR the attacker's user has an active
time special granting rule type `TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD` targeting the
victim's unit (`activeTimeSpecialRuleFinderService.existsRuleMatchingUnitDestination`).
Consumed twice with **different** semantics — audit-confirmed but worth restating for
Gherkin authors: `attackTarget` (`AttackMissionManagerBo.java:184-186`) uses the
own-flag-OR-timespecial version to decide whether the DAMAGE split ignores shield;
`addPointsAndUpdateCount` (line 239) computes `healthForEachUnit` using ONLY the
attacker unit's own `bypassShield` column (NOT the time-special variant) — so a
time-special-granted bypass changes how damage is distributed to the victim's
shield/health pool but does NOT change the per-unit "how much damage kills one victim
unit" divisor used for the kill-count formula. This is a genuine, confirmed-in-source
asymmetry (not a bug per se — Java behaves this way both possible readings apply) worth
flagging as a scenario (own-flag bypass vs. time-special-only bypass should produce
different kill counts for an otherwise-identical fight).

### 2.4 Mission-run failure / retry (applies to ATTACK/COUNTERATTACK like any unit mission)

**B36. Transient failure during db-scheduler execution (e.g. lock timeout).**
`UnitMissionBo.runUnitMission` is `@Retryable(retryFor = CannotAcquireLockException.class,
...)` (`UnitMissionBo.java:216`) — retried in-process by Spring Retry before ever
reaching `MissionBaseService.retryMissionIfPossible`'s attempt-counter path
(`MissionBaseService.java:43-64`): under `MAX_ATTEMPTS=3`, on a non-lock exception the
mission's `attemps` is incremented, an error report is saved
(`buildCommonErrorReport`), and it's rescheduled; at `attemps>=3` a unit mission (which
ATTACK/COUNTERATTACK is, `MissionType.isUnitMission()`) instead has a return mission
registered and is marked resolved (survivors return home without ever having fought).

---

## 3. Draft Gherkin scenarios

All combat scenarios use the **W3 fixed-mission-id When variant** (§3 technique #2 /
§6.3 of the plan) so both backends seed `java.util.Random`/`JavaRandom` from the same
`mission.id` and the RNG trace + resulting tables are directly diffable. Concrete ids
per the VERIFIED 2026-07-09 ranges (missions ≥ 900000 — the live DB already has missions
up to id 900206 from prior harness runs, so new scenario ids below start at 901000 to
avoid collision). Units/rules below are existing baseline content confirmed live in the
dev DB 2026-07-09: unit 10 (X-302: attack 280, health 545, shield 0), unit 3 (Comandos
Stargate: attack 70, shield 40, health 110), unit 309 (Unas Salvaje, type 6: attack 90,
health 190, shield 0), unit 346 (Campo de Fuerza, declares `interceptable_speed_group`
{1}), unit 5 (Doctor Daniel Jackson, own `speed_impact_group_id=1`), rule 12
(UNIT_CAPTURE unit 3→unit 309). Mission type codes: ATTACK=8, COUNTERATTACK=9
(`MissionType.java:4-5`).

```gherkin
Feature: Attack combat resolution
  Reference: AttackMissionManagerBo / AttackMissionProcessor / AttackObtainedUnitBo.
  All scenarios run with configuration ATTACK_DETERMINISTIC_RNG=TRUE and the
  fixed-mission-id When variant so Java and Rust consume the same RNG sequence
  (mission.id-seeded java.util.Random / JavaRandom) and the RNG trace is diffable.

  Background:
    Given the standard test universe
    And user 1 has 0 units of id 10 on planet 1002

  Scenario: Symmetric 10v10 full mutual kill
    # B20-B23, B25, B28-B30. Mirrors seed_attack.sql.
    Given planet 1002 is owned by user 1
    And planet 1003 is owned by user 2
    And user 1 has 10 units of id 10 on planet 1002
    And user 2 has 10 units of id 10 on planet 1003
    When user 1 runs an ATTACK mission from planet 1002 to planet 1003 with 10 units of id 10 with fixed mission id 901001
    Then user 1 has 0 units of id 10 on planet 1002
    And user 2 has 0 units of id 10 on planet 1003
    And table missions has a row where id=901001 and resolved=1
    And user 1 received websocket event "unit_mission_change"
    And user 1 received websocket event "mission_report_new"
    And user 2 received websocket event "mission_report_new"

  Scenario: Asymmetric partial kill — survivor stack depends on shuffle order (seed-sensitive)
    # B20-B21, B23 (partial-kill branch), B27 (UNIT_AMOUNT re-trigger), B28.
    # Mirrors seed_attack_partialkill.sql / seed_attack_asymmetric.sql; the whole
    # point of this scenario is that it FAILS without the seeded-RNG shuffle (the
    # negative control: same seed => same survivor stack on both backends; a
    # DIFFERENT fixed mission id flips which stack survives).
    Given planet 1002 is owned by user 1
    And planet 1005 is owned by user 1
    And planet 1003 is owned by user 2
    And user 1 has 7 units of id 10 on planet 1002
    And user 1 has 2 units of id 10 on planet 1005
    And user 2 has 16 units of id 10 on planet 1003
    When user 1 runs an ATTACK mission from planet 1002 to planet 1003 with 7 units of id 10 and 2 units of id 10 from planet 1005 with fixed mission id 901002
    Then table obtained_units has a row where user_id=1 and unit_id=10 and count=1
    And user 2 has 0 units of id 10 on planet 1003

  Scenario: Counterattack on own planet — symmetric 10v10
    # B2 (registration guard), then identical combat resolution to ATTACK (B20-B30
    # via CounterattackMissionProcessor's pure delegation). Mirrors seed_counterattack.sql.
    Given planet 1002 is owned by user 1
    And planet 1003 is owned by user 1
    And planet 1004 is owned by user 2
    And user 1 has 10 units of id 10 on planet 1002
    And user 2 has 10 units of id 10 on planet 1003
    When user 1 runs a COUNTERATTACK mission from planet 1002 to planet 1003 with 10 units of id 10 with fixed mission id 901003
    Then table missions has a row where id=901003 and type=9 and resolved=1
    And user 2 has 0 units of id 10 on planet 1003

  Scenario: Counterattack rejected when target planet is not the sender's own
    # B2 — registration-time rejection, never reaches combat / no mission row created.
    Given planet 1002 is owned by user 1
    And planet 1003 is owned by user 2
    And user 1 has 5 units of id 10 on planet 1002
    And user 2 has 5 units of id 10 on planet 1003
    When user 1 runs a COUNTERATTACK mission from planet 1002 to planet 1003 with 5 units of id 10
    Then the request is rejected with SgtBackendInvalidInputException

  Scenario: Full interception — attack never reaches combat
    # B19. Mirrors seed_interception.sql exactly (units/planets/rule already proven).
    Given planet 1002 is owned by user 1
    And planet 1003 is owned by user 2
    And user 1 has 10 units of id 5 on planet 1002
    And user 2 has 5 units of id 346 on planet 1003
    When user 1 runs an ATTACK mission from planet 1002 to planet 1003 with 10 units of id 5 with fixed mission id 901004
    Then table missions has a row where id=901004 and resolved=1
    And table obtained_units has no row where user_id=1 and unit_id=5
    And user 2 has 5 units of id 346 on planet 1003
    And user 1 received websocket event "unit_mission_change"

  Scenario: Unit capture on kill — always-fires rule (deterministic amount)
    # B31-B33. Mirrors seed_capture.sql; rule 12 extra_args forced to '100#10' so
    # capture ALWAYS fires with captured=1 regardless of RNG (isolates the capture
    # BOOKKEEPING/report shape from the RNG-trace concern, which the partial-kill
    # scenario above already covers).
    Given planet 1002 is owned by user 1
    And planet 1003 is owned by user 2
    And user 1 has 10 units of id 3 on planet 1002
    And user 2 has 5 units of id 309 on planet 1003
    And capture rule 12 always captures 10 percent of kills
    When user 1 runs an ATTACK mission from planet 1002 to planet 1003 with 10 units of id 3 with fixed mission id 901005
    Then table obtained_units has a row where user_id=1 and unit_id=309 and is_from_capture=1 and count=1
    And user 1 received websocket event "mission_report_new"

  Scenario: Attacking mission fully wiped out — no return mission
    # B25-B26. Attacker sends a stack too weak to survive; the mission row is kept
    # (resolved=removed) rather than deleted, and no RETURN_MISSION is scheduled.
    Given planet 1002 is owned by user 1
    And planet 1003 is owned by user 2
    And user 1 has 1 units of id 10 on planet 1002
    And user 2 has 50 units of id 10 on planet 1003
    When user 1 runs an ATTACK mission from planet 1002 to planet 1003 with 1 units of id 10 with fixed mission id 901006
    Then table obtained_units has no row where user_id=1 and unit_id=10
    And table missions has no row where source_planet=1003 and target_planet=1002 and type=5
```

Note on the "the request is rejected with ..." Then and the multi-planet `with N units
of id U and M units of id U from planet P` When phrasing: these are NOT in the current
§6 catalog — see §4 below for the exact new-step proposals they require.

Coverage map (B-number → scenario):
B1/B5-B14 (registration validation) — not drafted above (they're generic to every unit
mission type, not combat-specific; recommend a SHARED `mission_registration_validation.feature`
covering B1/B4-B14 once across all mission types rather than duplicating per type, since
none of the checkers here special-case ATTACK/COUNTERATTACK). B2 — scenario 4. B15-B16 —
implicitly covered by every scenario's registration step (not separately asserted; would
need a `Then user X received websocket event "enemy_mission_change"` after registration,
before firing — a good Phase-3 addition). B17/B20-B23/B25/B27-B30 — scenarios 1-2. B18 —
not drafted (needs a partial-interception seed: some but not all involved units
intercepted; propose reusing `seed_interception.sql`'s shape with a smaller interceptor
stack). B19 — scenario 5. B24 (carrier-freed) — not drafted, needs a carrier+stored-unit
seed (none of the existing `seed_*.sql` files set up `owner_unit_id`; net-new fixture).
B31-B34 — scenario 6. B35 (bypass shield asymmetry) — not drafted, needs a unit with
`bypass_shield=1` or an active `TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD` rule (net-new
fixture; no existing seed exercises this). B26 — scenario 7. B36 — out of scope for
Gherkin (infra-retry behavior, not user-visible business logic; would need fault
injection the harness doesn't support).

---

## 4. Proposed new steps

QUARANTINED — none of these exist in `BDD-PARITY-PLAN.md` §6 yet.

| Step text | Why needed | Implementation notes |
|---|---|---|
| `user {u} runs a(n) {TYPE} mission from planet {src} to planet {dst} with {n} units of id {uid} and {m} units of id {uid2} from planet {src2}` | The asymmetric partial-kill scenario (the one case that REQUIRES seeded RNG) needs TWO stacks of possibly-the-same unit id from DIFFERENT source planets in one mission — §6.3's W1/W3 only carries one `(uid, n)` pair. | Extend the When step's REST body builder to accept a list of `(unitId, count, sourcePlanetId)` triples instead of a single one; for W3, INSERT one `obtained_units` row per triple with `mission_id`/`target_planet` set, exactly as `seed_attack_partialkill.sql` does (lines 91-95). Keep the single-unit form as sugar for the common case. |
| `capture rule {rid} always captures {pct} percent of kills` | Capture probability/amount are real RNG draws (`HandleUnitCaptureListener.java:46,48`); even under fixed-mission-id the Gherkin author would have to hand-compute the exact `JavaRandom` sequence to predict whether/how-much a capture fires. Forcing `extra_args='100#{pct}'` (prob=100 always passes, and the `+1` in the amount formula makes the result exactly 1 whenever `killed*pct/100 < 1`) makes the capture BOOKKEEPING (report shape, `is_from_capture` row, `moveUnit` destination logic) assertable at Layer 1 without depending on the trace. | `UPDATE rules SET extra_args='100#{pct}' WHERE id={rid};` — exactly the `seed_capture.sql` technique (line 53). Register the rule id on `context` for no particular filter purpose (rules aren't in the mission footprint dump) but document the mutation is production-config, not scenario data — a Given that mutates global reference data is unusual enough to flag for the harness README. |
| `table obtained_units has a row where {col}={v} and {col2}={v2} and ...` (generalize past the current 2-predicate examples) | The current §6.4 generic escape hatch is described as "a row where col=v and …" but every §6.4 usage example shows only named steps; capture/partial-kill scenarios need 3+ predicates (`user_id`, `unit_id`, `count`, `is_from_capture`) in one assertion. Confirm/extend the escape hatch's parser to accept an arbitrary `AND`-chain rather than a fixed arity. | Parse the step text with a generic `key=value(\s+and\s+key=value)*` grammar; reject columns not in the query's SELECT (already whitelisted per table, per §6.4). No new step name needed if the existing escape-hatch step's implementation already supports N predicates — flag as an implementation-completeness check, not strictly a new step. |
| `the request is rejected with {ExceptionSimpleName}` | Several registration-time Thens (B1-B14) need to assert the REST call itself failed with a specific exception type/4xx, not just "no mission was created". Neither §6.4 (DB) nor §6.5 (ws) covers a non-2xx REST response assertion — §6.3 only says "assert 2xx or fail the scenario", i.e. currently ANY non-2xx is already a hard scenario failure, which is backwards for negative-path scenarios. | Requires a `When` variant that expects failure (e.g. suffix `and it is rejected`) OR a paired `Then` that inspects a captured last-response object on `BddWorld` (status code + body) instead of always hard-failing on non-2xx. This is a bigger design gap than a single step — the current W1 contract ("assert 2xx or fail the scenario") actively prevents writing any negative-path registration scenario. Needs a decision, not just a step; flagged in §6 open questions too. |
| `table missions has no row where {predicates}` | B26's assertion ("no RETURN_MISSION was scheduled for the wiped attacker") is a negative existence check; §6.4's escape hatch as written only shows "has a row"/"has no row" for the NAMED `unlocked_relation` step, not generalized to the generic escape hatch. | Mirror the existing `has a row` / `has no row` pairing onto the generic escape-hatch step for every whitelisted table (`missions` is already whitelisted). |
| `capture rule {rid} from unit {a} to unit {b}` (Given, no forced determinism) | For scenarios that want to assert "no capture happens because no rule exists" (B34) without touching global `rules` data — currently there is no way to assert an ABSENCE of a capture rule as a Given (tests instead rely on ambient baseline data, which is fragile / not "concrete and deterministic" per §2.5). | `SELECT`-only Given that asserts (does not mutate) a precondition: fail scenario setup loudly if the expected rule row is missing/present unexpectedly. Lower priority — most B34 coverage can just pick two units with no baseline UNIT_CAPTURE rule between them and document that fact in the scenario's Given comment instead. |
| `unit {uid} declares owner unit {ownerUid} on planet {pid}` | B24 (carrier-freed-on-wipe) needs an `obtained_units` row with `owner_unit_id` set (a stored/carried unit) — no existing Given step or seed file constructs this relationship. | `UPDATE obtained_units SET owner_unit_id={carrierObtainedUnitId} WHERE ...` — needs the carrier's own `obtained_units.id`, which is DB-generated; either look it up by `(user, unit, planet)` immediately after inserting the carrier row within the same Given, or accept an explicit fixed id for both rows (deterministic-id INSERT rather than natural-key driven, breaking the "no autoincrement in Givens" convention for this one case — flag as a documented exception). |
| `user {u} has an active time special granting bypass shield against unit {uid}` | B35 (time-special shield bypass, vs. the unit's own `bypass_shield` column) has no existing fixture. | INSERT `active_time_specials` for a time special whose rule set includes a `TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD` rule (`AttackBypassShieldService.RULE_TYPE`) targeting `{uid}`; needs a reference time special + rule to exist in baseline or be created idempotently, mirroring the `unit {uid} exists gated by requirement ...` pattern from §6.2. |

---

## 5. Rust port status

Source: `rust-backend/pending_migration/ATTACK_FEATURE_AUDIT.md` (Part-0 audit, full
line-for-line Java/Rust comparison of the core combat math — items 1-25, verdict
"functionally complete, no `❌ missing`"), cross-checked here against the
registration/interception/report layers the audit did not cover, plus a live grep of
the current tree.

| Java file | Rust counterpart | Status |
|---|---|---|
| `UnitMissionBo.java` (registration `my*`/`admin*`, `runUnitMission`, `doRunUnitMission`) | `rust-backend/owge-business/src/bo/unit_mission_bo.rs` | ✅ ported — `my_register_attack_mission`/`my_register_counterattack_mission`/`my_cancel_mission` all present (`unit_mission_bo.rs:103-141,280+`); counterattack's target-ownership guard ported verbatim (`:132-139`). Module doc comment in `owge-rest/src/routes/game/mission.rs:14-23` is **stale** — it claims these endpoints "answer 501" pending a fuller `UnitMissionBo` port, but `unit_mission_bo.rs` already implements the full registration pipeline; the routes call it directly (`mission.rs:113-123,127-137`), not a 501 stub. Worth a doc fix, noted here so the harness author doesn't get misled into thinking attack/counterattack registration is unported. |
| `UnitMissionRegistrationBo.java` + all `checker/*` (B1-B14) | not found as a single `unit_mission_registration_bo.rs`... — actually present: `rust-backend/owge-business/src/bo/unit_mission_registration_bo.rs` (referenced by grep hits for `trigger_unit_build_completed_or_killed` at lines 432, 449) | Believed ✅ ported based on grep; **not read line-by-line in this pass** (out of the combat-specific budget) — flag as a gap in THIS inventory, not necessarily in Rust: recommend a follow-up audit pass over `unit_mission_registration_bo.rs` mirroring the granularity of `ATTACK_FEATURE_AUDIT.md`, specifically for B9/B10 (stored-unit weight/capability checks) and B14 (cross-galaxy) which are easy to under-port. |
| `MissionInterceptionManagerBo.java` | `rust-backend/owge-business/src/bo/mission_interception_manager_bo.rs` | ✅ ported per its own doc comment (`:1-30`) — `load_information`, `maybe_append_data_to_mission_report`, `handle_mission_interception` all present (lines 61, 107, 148) and documented as behavior-mirroring. Not independently line-diffed against Java in this pass; the doc comment's confidence is inherited, not re-verified — same caveat as special-location (this exact class of "confident comment, wrong behavior" is what produced `BUG-SPECIAL-LOCATION-UNLOCK.md`, so treat this ✅ as provisional until a scenario exercises it). |
| `UnitInterceptionFinderBo.java` | `rust-backend/owge-business/src/bo/unit_interception_finder_bo.rs` | ✅ ported, doc comment explicitly walks the same three conditions (interceptable-speed-group membership, enemy check, already-intercepted dedup) and calls out iteration-order preservation for the `HashMap` first-insert semantics (lines 19-23) — this is exactly the kind of subtle ordering bug class (cf. D2/D3 in the attack audit) worth a dedicated interception scenario to actually prove, not just trust the comment. |
| `AttackMissionManagerBo.java` core combat math | `rust-backend/owge-business/src/bo/attack_mission_manager_bo.rs` (1919 lines) | ✅ ported, per Part-0 audit items 1-25. Confirmed fixed since the audit: **D0** (improvement-inheritance `has_to_inherit_improvements` flag) and the Part-3 defender-`ORDER BY` bug — per `ATTACK_PARITY_PLAN.md` "Implementation status (2026-06-23)". Two RESIDUAL, audit-flagged, low-priority items NOT re-verified as fixed in this pass: **D2** (capture rule UNIT_TYPE×UNIT_TYPE lookup-order, only diverges with multiple overlapping type-chain capture rules) and **D4** (missing `OwgeElementSideDeletedException`-equivalent guard, audit judges net-inert). |
| `AttackMissionProcessor.java` / `CounterattackMissionProcessor.java` | `rust-backend/owge-business/src/bo/mission_processor/attack.rs`, `counterattack.rs` | ✅ ported — `counterattack.rs:16-23` is a literal one-line delegate to `attack::process`, matching Java's `CounterattackMissionProcessor.process` delegation exactly (both are "counterattack = attack" by construction, so a Rust bug in `attack.rs` automatically reproduces in counterattack on both backends — good, means B2 is the ONLY counterattack-specific surface worth separately testing). Requirement-cascade wiring (B27) confirmed present via grep: `attack_mission_manager_bo.rs:1422,1428` call `RequirementBo::trigger_unit_build_completed_or_killed`/`trigger_unit_amount_changed`. |
| `HandleUnitCaptureListener.java` (capture roll + report) | folded into `attack_mission_manager_bo.rs` (not a separate listener file — Rust has no listener/observer indirection here) | ✅ ported per audit items 17-20, with D2/D8 caveats above. Architecturally DIFFERENT from Java (Java uses the `AfterUnitKilledCalculationListener`/`AfterAttackEndListener` observer interfaces so third-party listeners could hook in; Rust inlines the logic) — behaviorally equivalent for the ONE listener that exists today, but if Java ever grows a second listener implementing those interfaces, Rust's inlined version would silently miss it. Worth a §6 open question. |
| `AttackBypassShieldService.java` | `attack_mission_manager_bo.rs:642-666` (per audit item 10) | ✅ ported, own-flag-OR-time-special logic identical. The B35 asymmetry (damage-split bypass vs. kill-divisor bypass using DIFFERENT flag combinations) is a property of the JAVA reference itself, so "ported" here means Rust reproduces the same asymmetry — not yet scenario-proven either side per §6 open questions. |
| `AttackEventEmitter.java` (listener dispatch) | inlined, no direct Rust equivalent needed (see `HandleUnitCaptureListener` row) | N/A — architectural difference, not a gap, given exactly one listener implementation exists on the Java side today. |
| `MissionReportBo.create`/`.save` (websocket emit: `mission_report_new`, `mission_report_count_change`) | not located by this pass's greps under `owge-business/src/bo/` with an obvious name (`mission_report_bo.rs` not confirmed to exist) | ⚠ **UNVERIFIED** — not checked in this pass. Given B30/B33's report fan-out is central to combat/capture/interception observability, recommend this be the FIRST thing a follow-up port-status check confirms, since a missing/wrong `mission_report_new` emission would silently fail every "user received websocket event mission_report_new" Then in §3 without an obvious DB-side symptom (the report ROW would still be correct in the table diff — only the websocket layer would be silently wrong, exactly the class of bug `ws_verify` was built to catch on the READ side; this is the WRITE-side analogue). |
| `rust-backend/docs/UNPORTED-ENDPOINTS.md` | — | Does not list any `game/mission/*` endpoint as unported (the file's full contents: `open/sponsor`, `open/websocket-sync/rule_change`, `open/websocket-sync/speed_group_change`, `admin/system/notify-updated-version`, `admin/system/run-hang-missions`, `admin/cache/drop-all`, `game/deliver-backdoor/ping-user`, `admin/users/{id}/suspicions` — none combat-related), corroborating that ATTACK/COUNTERATTACK registration+firing is believed fully ported. |

---

## 6. Open questions / suspected divergences

1. **Invoker's own combat report attribution (B30) needs runtime verification.**
   `AttackMissionProcessor.processAttack` (lines 72-93) builds ONE
   `UnitMissionReportBuilder` and explicitly saves it for every OTHER involved user
   (line 76-78) and, only `if (isTriggeredByEvent)`, ALSO for the invoker (line 79-81).
   For a direct player-initiated ATTACK/COUNTERATTACK, `isTriggeredByEvent=false`
   (`AttackMissionProcessor.java:47`), so by this reading the mission's OWN sender
   never gets a `mission_reports` row from THIS code path — yet the player obviously
   sees their own attack's outcome in practice. Either (a) `mission.setReport(...)` via
   a DIFFERENT path attaches a report the invoker can read through the `missions` table
   join rather than a personal `mission_reports` row, or (b) there's a report-save call
   this inventory's read missed. **This needs to be resolved by actually running a
   scenario against Java** (the harness's Phase-1 purpose exactly) rather than
   guessed from source reading — flagging rather than asserting an incorrect Then in
   §3's scenarios (none of the draft scenarios above assert "the ATTACKER received a
   mission_report_new", only that the DEFENDER/enemy side did, specifically to sidestep
   this ambiguity until verified).

2. **`emitLocalMissionChangeAfterCommit` on `removed` — Java's own doubt.**
   `AttackMissionProcessor.java:87-89` has the literal comment `// Maybe useless?,
   should test` on the emission that fires when the attack mission wiped itself out or
   left the target owner with deleted missions. Since this is the reference author's
   own flagged uncertainty, a Rust divergence here would be a case of "which one is
   actually right", not a straightforward port bug — treat per plan pitfall #11 (don't
   blindly match Java if the JAVA_SPEC verdict itself looks wrong; flag to Kevin).

3. **Full-interception's double-delete of intercepted units (B19).**
   `MissionInterceptionManagerBo.handleMissionInterception` (line 72) calls
   `deleteInterceptedUnits` again even though `loadInformation` (line 39) already
   deleted them when `totalInterceptedUnits > 0` (which is implied by `isMissionIntercepted
   == true`). The second delete is a no-op against an already-empty set in Java
   (`obtainedUnitRepository::deleteAll` on an already-deleted collection reference —
   behaves fine since the entities are still in-memory objects, JPA's `deleteAll` on
   detached/gone rows is idempotent). Confirm the Rust port also tolerates a
   delete-of-already-deleted without erroring (a SQL `DELETE ... WHERE id IN (...)`
   naturally would; a Rust port using `.rows_affected()` assertions could diverge here
   if it asserts >0).

4. **Interception vs. `MISSION_<TYPE>_TRIGGER_ATTACK`-triggered combat — does interception apply?**
   `UnitMissionBo.doRunUnitMission` runs interception ONCE, before dispatching to
   `missionProcessorMap.get(missionType)` — i.e. only for the mission's OWN top-level
   type (ATTACK/COUNTERATTACK/GATHER/etc.), never for the SECONDARY attack a
   GATHER/DEPLOY mission might trigger via `triggerAttackIfRequired`
   (`AttackMissionProcessor.java:54-62`) once it's already running INSIDE that other
   processor. Confirm this is intentional (a gather mission that lands on a hostile
   planet can't be "intercepted" separately from the attack it triggers — the
   interception gate only ever protects the mission's literal declared type) and that
   Rust's processor dispatch mirrors the same "interception checked once, at the
   top, only for the mission's own type" placement rather than re-checking inside the
   secondary attack trigger.

5. **B35's bypass-shield asymmetry — is it intentional or an accidental Java bug?**
   The damage-split bypass check (`attackTarget`, own-flag OR time-special) and the
   kill-divisor bypass check (`addPointsAndUpdateCount`, own-flag ONLY) read different
   inputs for what looks like it should be the same concept. Recommend Kevin confirm
   whether the kill-divisor omission of the time-special variant is deliberate (e.g.
   "shield bypass from a TIME SPECIAL only affects where damage lands, not how
   efficiently it kills" is a plausible design choice) before writing a Then that
   locks in the asymmetry as "correct" — per pitfall #11, don't canonize a possible
   Java bug without confirmation.

6. **The `git status`/registration-rejection Gherkin gap (§4, "the request is rejected
   with...").** The plan's §6.3 W1 contract currently states ANY non-2xx REST response
   is an automatic scenario failure ("Every When that creates a mission also asserts
   the POST returned 2xx — a 4xx/5xx is a scenario failure"). Taken literally, NONE of
   B1-B14's registration-validation branches are expressible as a passing Gherkin
   scenario today — every one of them is a 4xx by design. This needs a design decision
   (an explicit failure-expecting When/Then pair) before Phase 3's negative-path
   scenarios (which the plan's own roadmap calls for — "REQUIREMENT_GROUP", "max-planets
   edge", etc. are all necessarily negative-path) can be written at all, not just for
   combat. Flagging here because combat's registration checks (B1-B14) are the first
   place this gap was hit while producing this inventory, but the fix belongs in the
   harness's core When/Then vocabulary, not a combat-specific step.

7. **Rust `mission_report_bo` existence unverified (§5 last row).** Highest-priority
   follow-up before Phase 2 combat features are implemented: confirm whether Rust emits
   `mission_report_new`/`mission_report_count_change` at all, and if so, from where —
   this inventory's greps did not locate the emitting code with confidence in the time
   budget available.

8. **"Emergency deploy" mechanic — does not exist.** The task brief asked this
   inventory to check for it; `grep -rl "mergencyDeploy\|EMERGENCY_DEPLOY\|emergency"`
   across `business/src/main/java` returned no hits. There is no emergency-deploy
   mechanic in this codebase to catalog (possibly conflated with the DEPLOY mission
   type's `DeployMissionConfigurationEnum` restrictions, which are a DIFFERENT,
   already-cataloged mechanic in `MissionRegistrationCanDeployChecker.java` — not
   combat-specific, belongs in a `deploy.feature` inventory instead).
