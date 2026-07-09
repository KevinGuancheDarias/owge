# upgrades — Java reference behavior inventory

Domain: LEVEL_UP mission registration/execution/cancel, `obtained_upgrades`,
and the requirement cascade around upgrade levels (`UPGRADE_LEVEL` /
`UPGRADE_LEVEL_LOWER_THAN`). Read the full call tree in `business/` +
`game-rest/`; Rust cross-checked against `rust-backend/owge-business` /
`rust-backend/owge-rest`.

## 1. Endpoints

| Verb + path | Controller file:line | Bo entry point |
|---|---|---|
| `GET game/upgrade/registerLevelUp?upgradeId=` | `game-rest/.../rest/game/UpgradeRestService.java:39-46` | `MissionBo.registerLevelUpAnUpgrade(userId, upgradeId)` (`business/.../MissionBo.java:112-163`) then `MissionBo.findRunningLevelUpMission` (`:285-297`) |
| `GET game/upgrade/cancelUpgrade` | `UpgradeRestService.java:48-52` | `MissionBo.cancelUpgradeMission(userId)` (`MissionBo.java:299-303`) |
| websocket sync `obtained_upgrades_change` | `UpgradeRestService.java:56, 60-64` (`findObtained`) | `ObtainedUpgradeRepository.findByUserId` + `ObtainedUpgradeBo.toDto` |
| websocket sync `running_upgrade_change` | `UpgradeRestService.java:57, 66-68` (`findRunningUpgrade`) | `MissionBo.findRunningLevelUpMission` |
| `GET game/upgradeType/` | `game-rest/.../rest/game/UpgradeTypeRestService.java:31-34` | `UpgradeTypeBo.findAll()` |
| websocket sync `upgrade_types_change` | `UpgradeTypeRestService.java:37-39` | `UpgradeTypeBo.findAll()` |

Out of scope but adjacent (admin content management, not the gameplay
flow this inventory targets): `AdminUpgradeRestService` /
`AdminUpgradeTypeRestService` (`game-rest/.../rest/admin/`) — full CRUD over
`Upgrade`/`UpgradeType` definitions via `CrudWithFullRestService`. Relevant
only as the source of `upgrades`/`upgrade_types` reference-data rows the
Given steps below read from.

**Note:** unlike `UnitRestService` (which has a plain "unlocked unit catalog"
sync handler), there is no player-facing "list all upgrades" endpoint —
`obtained_upgrades_change` already nests the full `UpgradeDto` inside each
`ObtainedUpgradeDto`, and a player only ever sees upgrades they have an
`obtained_upgrades` row for (created by the requirement cascade, §2 B7).

## 2. Behavior catalog

### `registerLevelUp`

**B1 — Happy path: register a LEVEL_UP mission.**
Trigger: `GET game/upgrade/registerLevelUp?upgradeId=X`, logged-in user has an
`obtained_upgrades` row for X with `available=1`, no other running LEVEL_UP
mission, under the mission-count cap, enough `primary_resource`/
`secondary_resource`.
Call chain: `MissionBo.registerLevelUpAnUpgrade` (`MissionBo.java:112-163`),
serialized per-user via `userLockUtilService.doInsideLockById` (`:118`, race
comment `:114-117` — without the lock two concurrent requests both pass
`checkUpgradeMissionDoesNotExists` and each insert a mission).
- `:119` `checkUpgradeMissionDoesNotExists` (see B4).
- `:120-121` load `obtained_upgrades` row, `checkUpgradeIsAvailable` (see B5/B10).
- `:124` `missionBaseService.checkMissionLimitNotReached` (see B6).
- `:125-128` `upgradeBo.calculateRequirementsAreMet(obtainedUpgrade)`
  (`UpgradeBo.java:103-120`): base cost (`upgrades.primary_resource` /
  `secondary_resource` / `time`) grown by `upgrades.level_effect` **once per
  currently-owned level** (loop `for i=1; i<nextLevel; i++` where
  `nextLevel=level+1`, i.e. exactly `level` iterations — level 0 pays base
  cost unchanged). `canRun` (`ResourceRequirementsPojo.java:27-31`) requires
  `user.primary_resource >= requiredPrimary && user.secondary_resource >=
  requiredSecondary` (no energy term for upgrades) else B9.
- `:129-135` `ZERO_UPGRADE_TIME` configuration row (default `"TRUE"`,
  `configurationBo.findOrSetDefault`): if `TRUE`, `requiredTime` is
  **hardcoded to `3D` seconds** (`:130`), else it is reduced by
  `improvementBo.findUserImprovement(user).getMoreUpgradeResearchSpeed()`
  via `computeImprovementValue(..., sum=false)` (`:133-134`).
- `:136-141` `objectRelationBo.findOne(UPGRADE, upgradeId)` → `MissionInformation.relation`;
  `MissionInformation.value = obtainedUpgrade.getLevel() + 1` (the **target**
  level, not a delta).
- `:143-149` build `Mission` (`type=LEVEL_UP`, no `source_planet`/`target_planet`).
- `:475-480` `attachRequirementsToMission`: `mission.primary_resource`,
  `.secondary_resource`, `.required_time` = the computed values;
  `.termination_date = missionTimeManagerBo.computeTerminationDate(requiredTime)`.
- `:151` `substractResources`: `user.primary_resource -= mission.primary_resource`;
  same for secondary (`:487-490`).
- `:153-155` save user, save mission, `missionSchedulerService.scheduleMission(mission)`
  (creates the `scheduled_tasks` row, `task_name='mission-run'`).
- `:156-161` **after commit**: `entityManager.refresh(mission)`,
  `emitRunningUpgrade(user)` → ws `running_upgrade_change` with the
  `RunningUpgradeDto` (`:169-171`, event constant `MissionBo.java:50`),
  `emitMissionCountChange(userId)` → ws `missions_count_change`
  (`business/.../mission/MissionEventEmitterBo.java:80-...`), and
  `userEventEmitterBo.emitUserData(user)` → ws `user_data_change`
  (`business/.../user/UserEventEmitterBo.java:23,51-53`) — reflects the
  resource deduction.
- REST response: `RunningUpgradeDto` with `missionsCount` overwritten by a
  **fresh** `missionRepository.countByUserIdAndResolvedFalse(userId)`
  (`UpgradeRestService.java:42-45`) — i.e. the count *includes* the mission
  just registered.

**B2 — LEVEL_UP mission fires and completes.**
Trigger: `scheduled_tasks` row for the mission becomes due; db-scheduler
invokes `MissionBo.runMission(missionId, LEVEL_UP)` (`:357-363`) →
`processLevelUpAnUpgrade(missionId)` (`:179-203`).
- `:181-182` if the mission row is gone (already cancelled/processed —
  race), logs `MISSION_NOT_FOUND` and returns — **no exception**, silent no-op.
- `:184` `upgrade = objectRelationBo.unboxObjectRelation(missionInformation.getRelation())`.
- `:187-189` `obtainedUpgrade.setLevel(missionInformation.getValue().intValue())`
  (the **target** level stored at registration time) then save. **DB effect:**
  `obtained_upgrades.level` updated; `available` is untouched here (still
  whatever it was — normally `1`, since B10 already rejected non-available
  upgrades at registration).
- `:190` `requirementBo.triggerLevelUpCompleted(user, upgrade.getId())` — the
  requirement cascade, see B7/B8/B11.
- `:191` `improvementBo.clearSourceCache(user, obtainedUpgradeBo)`
  (`ImprovementBo.java:137-148`) evicts the per-user/per-source improvement
  cache (`taggable-cache` + Spring `CacheEvict`) and emits ws
  `user_improvements_change` with the freshly recomputed
  `findUserImprovement(user)`.
- `:192` `improvementBo.triggerChange(userId, obtainedUpgrade.getUpgrade().getImprovement())`
  (`ImprovementBo.java:167-192`) — for each nonzero improvement field on
  *this upgrade's own* `improvement` row, fires a registered listener
  (`doTrigger`, `:392-...`), **independent of and in addition to**
  `clearSourceCache`'s emission:
  - `UNIT_IMPROVEMENTS` (any `unitTypesUpgrades` present) →
    `UnitTypeBo`'s listener (`UnitTypeBo.java:62-68`): if any of those
    unit-type improvements has `type=AMOUNT`, emits ws `unit_type_change`
    (`UnitTypeBo.java:31,155-157`).
  - `MORE_ENERGY` (`getMoreEnergyProduction() > 0`) →
    `UserEventEmitterBo`'s listener (`UserEventEmitterBo.java:33-38`): emits
    ws `user_max_energy_change` (via `doAfterCommit`).
  - Other branches (`MORE_PRIMARY_PRODUCTION`, `MORE_SECONDARY_PRODUCTION`,
    `MORE_CHARGE`, `MORE_MISSIONS`, `MORE_UPGRADE_RESEARCH_SPEED`,
    `MORE_UNIT_BUILD_SPEED`) have **no listeners currently registered**
    (`grep addChangeListener` finds only the two above) — the branches exist
    but are presently dead code paths.
- `:193` `missionRepository.delete(mission)` — the mission row (and its
  `mission_information` row, cascaded) is gone once LEVEL_UP completes; there
  is **no** "resolved=1, kept" terminal state for LEVEL_UP, unlike combat
  missions.
- `:194-199` **after commit**: `entityManager.refresh(obtainedUpgrade)`,
  ws `running_upgrade_change` → `null` (the running mission is gone),
  `obtainedUpgradeBo.emitObtainedChange(userId)` → ws `obtained_upgrades_change`
  with the full current list (`ObtainedUpgradeBo.java:71-73`),
  `emitMissionCountChange(userId)` → ws `missions_count_change`.

**B3 — `cancelUpgrade` while a LEVEL_UP mission is running.**
Trigger: `GET game/upgrade/cancelUpgrade`; a LEVEL_UP mission owned by the
caller exists.
`MissionBo.cancelUpgradeMission` (`:299-303`) →
`cancelMission(missionRepository.findOneByUserIdAndTypeCode(userId, "LEVEL_UP"))`
(private, `:380-411`):
- `:390-392` mission is `null` → `MissionNotFoundException` (extends
  `NotFoundException` → HTTP 404, `SgtGameRestExceptionHandler.java:32-35`).
  This is the "no LEVEL_UP mission running" case (there is no dedicated
  "nothing to cancel" message — same exception as a genuinely-missing
  mission id).
- `:393-395` ownership check via `userSessionService.findLoggedIn()`
  (always true here since the mission was looked up **by the caller's own
  userId** — the `else` branch at `:405-408`, throwing a generic
  `CommonException`, is dead for this call path but shared with other
  mission types where the mission is looked up by id alone).
- `:397-404` LEVEL_UP is not `BUILD_UNIT`, so the `else` branch runs: **full
  refund** — `missionUser.addtoPrimary(mission.getPrimaryResource())`,
  `addToSecondary(mission.getSecondaryResource())`, save, then
  `emitUserAfterCommit(missionUser.getId())` → after commit,
  `unitTypeBo.emitUserChange(userId)` (ws `unit_type_change` — fired
  unconditionally by the shared `cancelMission` code path even though a
  LEVEL_UP cancel never changes unit counts; it's vestigial for this mission
  type but real traffic) + `emitMissionCountChange(userId)` (ws
  `missions_count_change`).
- `:409` `missionRepository.delete(mission)`.
- `:410` `abortMissionJob(mission)` → `missionSchedulerService.abortMissionJob`
  (removes the `scheduled_tasks` row).
- Back in `cancelUpgradeMission` (`:302`): **synchronously** (not wrapped in
  `doAfterCommit`, unlike every other emission in this file) sends ws
  `running_upgrade_change` → `null`.
- REST response: literal string `"{}"` (`UpgradeRestService.java:49-52`).
- **No `obtained_upgrades_change` is emitted on cancel** — the level itself
  was never touched (only resources refunded), so the obtained-upgrade row
  is unchanged; only the running-mission and mission-count/user views update.

### Requirement cascade (triggered by B2's `:190`)

**B7 — Grant on first requirements-met (upgrade never obtained before).**
`RequirementBo.triggerLevelUpCompleted` (`:172-176`) →
`processRelationList(objectRelationBo.findByRequirementTypeAndSecondValue(UPGRADE_LEVEL, upgradeId), user)`
— **only relations gated by `UPGRADE_LEVEL` with `second_value = upgradeId`
are re-evaluated**; see B11 for the `UPGRADE_LEVEL_LOWER_THAN` gap.
`processRelationList` (`:281-310`) → per relation, `processRelation` (`:318-324`)
→ `checkRequirementsAreMet` (`:332-350`, ANDs every `requirements_information`
row on the relation; `UPGRADE_LEVEL` check at `:360-370` treats a missing
`obtained_upgrades` row as level 0) → if newly met,
`registerObtainedRelation` (`:404-443`):
- `:414` no-op if `unlocked_relation` already has `(user, relation)`.
- `:415-418` **insert** `unlocked_relation(user_id, relation_id)`.
- `:419-440` per `object_relations.object_description`:
  - `UPGRADE` (`:421-428`): if the target user already has an
    `obtained_upgrades` row for that upgrade id, flip `available=true`
    (`alterObtainedUpgradeAvailability`, `:479-482` — this is B8's
    "re-grant" path); else `registerObtainedUpgrade` (`:470-477`) **inserts**
    a new `obtained_upgrades` row with `level=0, available=true`. No
    websocket event fires directly for the UPGRADE case here — the caller
    (B2) fires `obtained_upgrades_change` once, after all cascade side
    effects, covering this insert.
  - `UNIT` / `TIME_SPECIAL` (`:429-434`): `emitUnlockedChange` → ws
    `unit_unlocked_change` / `time_special_unlocked_change`, full current
    unlocked list (`:503-510`, after commit).
  - `REQUIREMENT_GROUP` (`:435-436`): no direct action — group membership is
    handled by the master/slave pass below.
  - `SPEED_IMPACT_GROUP` (`:437-438`): ws `speed_impact_group_unlocked_change`.
- `:441` `requirementInternalEventEmitterService.doNotifyObtainedRelation` —
  fans out to `RequirementComplianceListener`s (`business/.../requirement/listener/`);
  no listener implementations found beyond the interface itself as of this
  inventory (`grep implements RequirementComplianceListener` only matches
  `TemporalUnitScheduleListener.java` — out of scope for upgrades).

**B8 — Re-grant (upgrade previously obtained, `available` was `false`,
requirements newly met again).** Same `registerObtainedRelation` path, but
since `obtainedUpgradeRepository.existsByUserIdAndUpgradeId` is true, only
`available` flips back to `true` (`:422-424`) — `level` is untouched (a
revoked-then-regranted upgrade keeps its prior level, it does not reset to 0).

**B9 — Revocation.** `unregisterLostRelation` (`:450-468`), reached from
`processRelation` when `checkRequirementsAreMet` now returns `false` for a
previously-unlocked relation (also reachable transitively any time
`triggerLevelUpCompleted` re-evaluates a relation, since the same
`processRelation` path handles both directions):
- `:451-454` delete the `unlocked_relation` row if present.
- `:457-459` if `object==UPGRADE` and the user has an `obtained_upgrades`
  row, `alterObtainedUpgradeAvailability(..., false)` — **the
  `obtained_upgrades` row is NOT deleted**, only `available` flips to
  `false`; `level` is preserved (so a subsequently re-granted upgrade
  resumes leveling from where it left off, per B8).
- `:460-464` `SPEED_IMPACT_GROUP` → re-emit; else (`UNIT`/`TIME_SPECIAL`) →
  `emitUnlockedChange` (full current list, item absent).
- `:465-467` `doNotifyLostRelation` fan-out.
- **No direct websocket event for the `UPGRADE` case** here either — same as
  B7, the caller's `obtained_upgrades_change` (B2) covers it. **Important
  parity implication:** if a scenario revokes an `UPGRADE` relation via a
  path that is *not* `triggerLevelUpCompleted` (e.g. a `HAVE_UNIT`-gated
  upgrade relation, revoked when the unit is lost), nothing calls
  `obtainedUpgradeBo.emitObtainedChange` afterward — the DB row updates but
  the affected user is not proactively pushed the new `available=false`
  state over websocket until their next full sync. Confirmed: none of
  `unregisterLostRelation`'s branches emit `obtained_upgrades_change`.

**B10 — Registration rejected: upgrade not available / never obtained.**
`checkUpgradeIsAvailable` (`MissionBo.java:454-459`): if
`obtainedUpgrade.isAvailable()` is false → `SgtMissionRegistrationException`
("Can't register mission, of type LEVEL_UP, when upgrade is not available!").
**If `obtainedUpgrade` is `null`** (user has no `obtained_upgrades` row at
all for the requested `upgradeId` — never unlocked, or unlocked and later
had the row's `available` flipped false but the row still exists so this
specific branch doesn't apply... the null case is specifically "never
unlocked"), `checkUpgradeIsAvailable(null)` calls `.isAvailable()` **on a
null reference** → uncaught `NullPointerException`. There is no
`@ExceptionHandler(NullPointerException.class)` in
`SgtGameRestExceptionHandler.java` — this falls through to Spring Boot's
default error handling (HTTP 500, generic body, no `GameBackendErrorPojo`
shape). This is a genuine crash-on-bad-input in the Java reference, not a
deliberate 4xx — see §6 open questions.

**B11 — `UPGRADE_LEVEL_LOWER_THAN` is never re-triggered by a level-up.**
`UpgradeLevelLowerThanRequirementSource.checkRequirementIsMet`
(`business/.../requirement/UpgradeLevelLowerThanRequirementSource.java:26-44`):
met iff `obtainedUpgrade != null && obtainedUpgrade.level < thirdValue`
(missing row = **not met**, opposite of `UPGRADE_LEVEL`'s "missing = level
0" convention). This requirement type is dispatched only through the
`default -> runRequirementSources(...)` branch of
`checkRequirementsAreMet`'s `switch` (`RequirementBo.java:343`), which is
only reached when *some* `processRelation`/`processRelationList` call
touches that specific relation. Grepping every `processRelationList`/
`findByRequirementType*` call site in `RequirementBo.java`
(`:152,163,174,194,205,213,224,230,246`) shows **none query
`UPGRADE_LEVEL_LOWER_THAN`** — only `triggerLevelUpCompleted` (`UPGRADE_LEVEL`
only), `triggerUnitBuildCompletedOrKilled`/`triggerUnitAmountChanged`
(`HAVE_UNIT`/`UNIT_AMOUNT`), `triggerSpecialLocation`
(`HAVE_SPECIAL_LOCATION`), `triggerTimeSpecialStateChange`
(`HAVE_SPECIAL_ENABLED`), `triggerFactionSelection`/`triggerHomeGalaxySelection`
(`BEEN_RACE`/`HOME_GALAXY`), and `triggerRelationChanged` (all users, one
specific relation, called from admin edits). **Consequence:** a relation
gated *purely* by `UPGRADE_LEVEL_LOWER_THAN(upgradeId, N)` never gets
re-evaluated when `upgradeId` levels past `N` — it stays unlocked forever
once granted (typically at upgrade level 0, since "obtained but not yet
leveled" is the usual intended use: "you get this while your upgrade is
still low-level"). It WILL be correctly re-evaluated as a side effect if the
same relation is *also* gated by another requirement type whose trigger
fires (e.g. also `HAVE_UNIT`), because `processRelation` evaluates the whole
`checkRequirementsAreMet` AND-chain when *anything* reprocesses that
relation. This is a Java-reference behavior, not a Rust bug — see §6.

### Points / max-level (explicitly checked, both absent)

**B12 — No max-level cap.** `upgrades` schema (`business/database/02_schema.sql:1093-1107`)
has no max-level column, and neither `registerLevelUpAnUpgrade` nor
`calculateRequirementsAreMet` reference any cap — an upgrade can be leveled
indefinitely as long as resources/mission-limit allow. Cost grows
geometrically forever (`level_effect` compounding, B1).

**B13 — `upgrades.points` is unused for scoring.** The column exists and is
serialized into `UpgradeDto.points` (`UpgradeDto.java:14,44`) but no
business code adds it to `user_storage.points`. The only points-mutation
call site in the whole codebase is `AttackMissionManagerBo.java:139,250`
(`userStorageBo.addPointsToUser`, driven by killed `units.points`, not
`upgrades.points`). Confirmed via `grep -rn "setPoints\|addPoints" business/src/main/java`.
Leveling an upgrade never changes `user_storage.points` or the ranking.

## 3. Draft Gherkin scenarios

Uses existing baseline content proven by `rust-backend/scripts/seed_levelup.sql`
and `seed_reqtrigger.sql` (both idempotent, both DBs): upgrade 1
"Reclutamiento" (cost 490/330, time 280, `level_effect=0.5`, `object_relations`
relation id 1); upgrades 91/95/96 ("Fervor Religioso"/"Lanzadera Ori"/"Armadura
de Guerra", relation ids 234/238/239) and relation 247 = `UNIT` 123 "Prior"
gated by `BEEN_RACE(8) + UPGRADE_LEVEL(91,1) + UPGRADE_LEVEL(95,1) +
UPGRADE_LEVEL(96,1)` (verified live 2026-07-09: `mission_types.id=1` for
`LEVEL_UP`; `object_relations` ids for upgrades 1/91/95/96 are 1/234/238/239).
No new reserved-id range was defined for `upgrades` in the plan's VERIFIED
notes; live baseline max `upgrades.id` is 143 — **recommend `upgrade id >= 200`**
for any scenario that must create a brand-new upgrade (mirrors the
`units >= 9100` pattern), though none of the scenarios below need one.

Steps marked `[NEW]` are not yet in the §6 catalog — see §4.

```gherkin
Feature: Upgrade level-up registration, completion, and cancel
  Reference: business/MissionBo.java registerLevelUpAnUpgrade /
  processLevelUpAnUpgrade / cancelUpgradeMission; business/RequirementBo.java
  triggerLevelUpCompleted.

  Background:
    Given the standard test universe
    And user 1 has 24000 primary resource and 16000 secondary resource   [NEW]

  Scenario: Registering a level-up deducts resources and starts the mission
    # Covers B1
    Given user 1 has obtained upgrade 1 at level 2 available             [NEW]
    When user 1 registers a LEVEL_UP mission for upgrade 1               [NEW]
    Then table missions has a row where user_id=1 and type=1
    And user 1 has primary resource 22897.5 and secondary resource 15257.5 [NEW]
    And user 1 received websocket event "running_upgrade_change" where value has upgrade id 1 [NEW]
    And user 1 received websocket event "missions_count_change"
    And user 1 received websocket event "user_data_change"

  Scenario: A second level-up cannot be registered while one is running
    # Covers B4 (checkUpgradeMissionDoesNotExists) via B1's precondition
    Given user 1 has obtained upgrade 1 at level 2 available             [NEW]
    And user 1 runs a fixed LEVEL_UP mission with id 900001 for upgrade 1 [NEW, W3-style]
    When user 1 registers a LEVEL_UP mission for upgrade 1               [NEW]
    Then the request fails with exception "SgtLevelUpMissionAlreadyRunningException" [NEW]

  Scenario: Completing a level-up bumps the level and unlocks a gated relation
    # Covers B2, B7 (grant), and the UPGRADE_LEVEL trigger scope of B11
    Given user 1 has faction 8
    And user 1 has obtained upgrade 95 at level 0 available              [NEW]
    And user 1 has obtained upgrade 91 at level 1 available              [NEW]
    And user 1 has obtained upgrade 96 at level 1 available              [NEW]
    And unit 123 exists gated by requirement BEEN_RACE with second value 8 [existing pattern, adapted]
    And table unlocked_relation has no row for user 1 and object UNIT reference 123
    When user 1 registers a LEVEL_UP mission for upgrade 95              [NEW]
    And the LEVEL_UP mission of user 1 completes
    Then user 1's obtained upgrade 95 is at level 1                      [NEW]
    And table unlocked_relation has a row for user 1 and object UNIT reference 123
    And user 1 received websocket event "obtained_upgrades_change" where some item has upgrade id 95 and level 1 [NEW]
    And user 1 received websocket event "unit_unlocked_change" where some item has id 123
    And user 1 received websocket event "user_improvements_change"       [only if upgrade 95 carries an improvement — verify against seed]
    And user 1 received websocket event "running_upgrade_change" with null value [NEW]

  Scenario: Cancelling a running level-up refunds resources and clears the mission
    # Covers B3
    Given user 1 has obtained upgrade 1 at level 2 available             [NEW]
    And user 1 registers a LEVEL_UP mission for upgrade 1                [NEW, reused as Given via When-as-setup — or seed directly]
    When user 1 cancels the running upgrade mission                     [NEW]
    Then user 1 has primary resource 24000 and secondary resource 16000  [NEW]
    And table missions has no row where user_id=1 and type=1
    And user 1 received websocket event "running_upgrade_change" with null value [NEW]
    And user 1 received websocket event "unit_type_change"
    And user 1 received websocket event "missions_count_change"

  Scenario: Cancelling with no running upgrade mission is a 404
    # Covers B3's MissionNotFoundException branch
    Given user 1 has no obtained upgrades
    When user 1 cancels the running upgrade mission                     [NEW]
    Then the request fails with HTTP status 404                         [NEW]

  Scenario: Losing a level-up requirement flips available=false but keeps the level
    # Covers B9 (revocation) — needs a relation that becomes unmet; simplest is
    # HAVE_SPECIAL_LOCATION-gated UPGRADE relation, reusing planet 1234 pattern
    # from special_location_unlock.feature — cross-feature reuse, verify shape.
    Given planet 1234 has special location 500 and no owner
    And upgrade 200 exists gated by requirement HAVE_SPECIAL_LOCATION with second value 500 [NEW, needs "upgrade exists gated by" Given]
    And planet 1234 is owned by user 1
    And user 1 has obtained upgrade 200 at level 3 available             [NEW]
    When user 1 leaves planet 1234
    Then user 1's obtained upgrade 200 is at level 3                     [NEW — level preserved]
    And user 1's obtained upgrade 200 is unavailable                     [NEW]
    And table unlocked_relation has no row for user 1 and object UPGRADE reference 200
```

`ZERO_UPGRADE_TIME` divergence probe (B1's `:130` vs the Rust `5.0` at
`rust-backend/owge-business/src/bo/mission_bo.rs:1079` — §6): add an explicit
`Then table missions has a row where user_id=1 and type=1 and required_time=3`
to the first scenario once the `[NEW]` step lands — Layer 1 will fail on Rust
today (`required_time=5`) even before Layer 2 catches it, which is the
better failure signal (a wrong-number assertion beats a silent table diff).

## 4. Proposed new steps

QUARANTINED — none implemented; all require review before landing in the
shared catalog.

| Step text | Why needed | Implementation notes |
|---|---|---|
| `user {u} has {p} primary resource and {s} secondary resource` (Given) | No existing Given sets `user_storage.primary_resource`/`secondary_resource` directly; every LEVEL_UP cost assertion needs a known starting balance. | `UPDATE user_storage SET primary_resource={p}, secondary_resource={s} WHERE id={u}`. |
| `user {u} has primary resource {p} and secondary resource {s}` (Then) | Symmetric read-side assertion; §6.4's generic escape hatch table whitelist excludes `user_storage`. | `SELECT primary_resource, secondary_resource FROM user_storage WHERE id={u}`, exact-match (resources here are always deterministic sums, no RNG). |
| `user {u} has obtained upgrade {upid} at level {lvl} available` / `... unavailable` (Given) | DELETE-then-INSERT `obtained_upgrades` fixture — the closest existing catalog entry (`user {u} has an obtained upgrade {upid} available`) is a **Then**, not a Given, and has no level parameter. | `DELETE FROM obtained_upgrades WHERE user_id={u} AND upgrade_id={upid}; INSERT INTO obtained_upgrades (user_id, upgrade_id, level, available) VALUES ({u},{upid},{lvl},{1|0})`. Register (u, upid) on the World for layer-2 filtering. |
| `user 1 has no obtained upgrades` (Given) | Needed for the "cancel with nothing running" / "never unlocked" negative scenarios. | `DELETE FROM obtained_upgrades WHERE user_id={u}`. |
| `user {u}'s obtained upgrade {upid} is at level {lvl}` (Then) | Parametrized level check; existing catalog only checks `available`. | `SELECT level FROM obtained_upgrades WHERE user_id={u} AND upgrade_id={upid}`. |
| `user {u}'s obtained upgrade {upid} is unavailable` (Then) | Negative-availability counterpart to the existing "available" Then. | `SELECT available FROM obtained_upgrades WHERE user_id={u} AND upgrade_id={upid}` = 0. |
| `user {u} registers a LEVEL_UP mission for upgrade {upid}` (When) | The existing W1 pattern (`§6.3`) is shaped for unit-involving missions with source/target planets and an involved-units array; LEVEL_UP has neither — it needs its own REST call shape. | `GET game/upgrade/registerLevelUp?upgradeId={upid}` with the minted JWT for `u`; then nudge+poll is only needed once a `When ... completes` step runs later (registration itself is synchronous — the mission exists immediately, only its *execution* is deferred). Assert 2xx per §6.3's rule. |
| `user {u} cancels the running upgrade mission` (When) | Same gap — `cancelUpgrade` has no body/params but is a distinct action from the generic "leaves planet"/"activates time special" synchronous steps already in §6.3. | `GET game/upgrade/cancelUpgrade`. |
| `user {u} runs a fixed LEVEL_UP mission with id {mid} for upgrade {upid}` (Given/When hybrid) | Needed for the "second registration rejected" scenario, which must guarantee a running mission exists without going through the full registration flow (so the test doesn't depend on B1 itself succeeding). | Mirrors W3 (§6.3): hand-insert `missions` + `mission_information` + `scheduled_tasks` rows with a fixed id in the `missions >= 900000` range, `type=1` (LEVEL_UP), matching the shape MissionBo builds at `:143-149`. |
| `the request fails with exception "{ExceptionSimpleName}"` (Then) | The step catalog has no failure-path assertion at all; every documented exception in §2 needs one. | REST call must be made expecting non-2xx; assert `response.json().exceptionType == name` (the field `SgtGameRestExceptionHandler` populates at `:68`, `GameBackendErrorPojo.exceptionType`). |
| `the request fails with HTTP status {code}` (Then) | Coarser variant for cases (like B10's raw NPE → 500) where there is no clean `GameBackendErrorPojo` body to match on. | Assert only `response.status() == code`; do not attempt to parse a body for the 500/NPE case. |
| `user {u} received websocket event "{name}" where value has upgrade id {id}` (Then) | `running_upgrade_change`'s payload is a single `RunningUpgradeDto` object (`{upgrade: {...}, level, missionId, ...}`), not a list — the existing "where some item has id X" predicate (§6.5) assumes `payload.value` is a list. | Poll for `payload.value.upgrade.id == id`. |
| `user {u} received websocket event "{name}" where some item has upgrade id {id} and level {lvl}` (Then) | `obtained_upgrades_change`'s list items are `ObtainedUpgradeDto` — top-level `id` is the surrogate `obtained_upgrades.id`, not the upgrade id; the existing "some item has id X" predicate would silently never match on upgrade id. | Poll for `any(item => item.upgrade.id == id && item.level == lvl)`. |
| `user {u} received websocket event "{name}" with null value` (Then) | `running_upgrade_change` legitimately delivers `payload.value == null` on completion/cancel (B2/B3) — distinct from "event never arrived" and from the list-based "no item has id X" pattern (§6.5), which doesn't apply to a scalar/null payload. | Poll for a `deliver` frame with matching `eventName` and `value === null`; must still wait-then-assert (not assert-absence) since the frame is expected to arrive. |
| `upgrade {upid} exists gated by requirement {REQ} with second value {sv}` (Given) | Symmetric to the existing `unit {uid} exists gated by ...` / `time special {tsid} exists gated by ...` steps (§6.2) but for `object_description='UPGRADE'` — needed for B9's revocation scenario and any `UPGRADE_LEVEL`-chained scenario that creates a brand-new upgrade rather than reusing baseline upgrade 1/91/95/96. | Same pattern: INSERT `upgrades` row (fixed id, copy defaults from an existing baseline upgrade), find-or-create `object_relations(UPGRADE, upid)`, INSERT `requirements_information`. |

## 5. Rust port status

`rust-backend/docs/UNPORTED-ENDPOINTS.md` has no `upgrade` entries — both
`game/upgrade` endpoints are implemented.

**`rust-backend/owge-rest/src/routes/game/upgrade.rs`** — both routes wired:
`registerLevelUp` (`:28,41-58`) and `cancelUpgrade` (`:29,62-69`), 1:1 against
`UpgradeRestService`. The file's own module doc (`:1-12`) explicitly claims
M3 parity and notes the frontend ignores both response bodies (driven by
`running_upgrade_change` sync instead) but ports the exact shapes anyway.

**`rust-backend/owge-business/src/bo/mission_bo.rs`** — `do_register_level_up`
(`:1003-1147`), `process_level_up_an_upgrade` (`:400-463`),
`find_running_level_up_mission` (`:231-261`), `cancel_upgrade_mission`
(`:268-324`), `count_unresolved_missions` (`:328-...`). Emission wiring for
completion lives in the caller, `run_non_unit_mission`
(`rust-backend/owge-business/src/bo/unit_mission_bo.rs:725-784`), which for
`MissionType::LevelUp` (`:773-779`) fires `evict_and_emit` (→
`user_improvements_change`), `emit_running_upgrade` (→ `running_upgrade_change`
null), `emit_obtained_upgrades` (→ `obtained_upgrades_change`),
`emit_mission_count_change` — matching B2's four ws events.

**`rust-backend/owge-business/src/bo/requirement_bo.rs`** —
`trigger_level_up_completed` (`:404-...`) queries only `UPGRADE_LEVEL`
relations (`:412`), matching B11's gap byte-for-byte (Rust reproduces the
same never-re-triggers-`UPGRADE_LEVEL_LOWER_THAN` behavior, confirmed by
grepping every `find_relations_with_code*` call site — none pass
`UPGRADE_LEVEL_LOWER_THAN`). `check_requirements_are_met`'s
`UPGRADE_LEVEL_LOWER_THAN` arm (`:656-659`) reproduces the Java
"row must exist AND level < third" quirk exactly (`obtained_upgrade_level`
returns `(exists, level)`, both checked). `register_obtained_relation`
(`:674-...`) and `unregister_lost_relation` (`:713-...`) mirror B7-B9,
including the level=0-on-first-grant insert (`:990`) and the
available-flag-only update on revoke (`:1005`) without deleting the row.

### Confirmed matching (verified by reading both sides)
- Cost/time exponential growth loop (`for _ in 0..current_level.max(0)`,
  `mission_bo.rs:1057-1061`) — matches B1's `level_effect` compounding.
- Mission-already-running check, resource check, mission-limit check, event
  names (`running_upgrade_change`, `missions_count_change`,
  `obtained_upgrades_change`, `unit_type_change`) — all confirmed identical
  strings via grep.
- `UPGRADE_LEVEL`/`UPGRADE_LEVEL_LOWER_THAN` requirement evaluation semantics.

### Suspected/confirmed divergences (see §6 for detail)
- **`ZERO_UPGRADE_TIME` collapsed time: Java `3` vs Rust `5`**
  (`mission_bo.rs:1079`, comment at `:1071` even says "3s" while the code sets
  `5.0`) — highest-confidence finding in this inventory, directly
  observable via `missions.required_time`/`termination_date`.
- **`cancel_upgrade_mission` never emits `unit_type_change`**
  (`mission_bo.rs:268-324` has no `UnitTypeEmitter` call, vs Java's
  `cancelMission` unconditionally firing it through `emitUser`, B3).
- **No Rust equivalent of `ImprovementBo.triggerChange`'s listener cascade**
  on level-up completion: `evict_and_emit` (`user_improvement_bo.rs:177-180`)
  only re-emits `user_improvements_change`; there is no analog of Java's
  per-field `doTrigger` that conditionally also emits `unit_type_change`
  (AMOUNT-type unit improvements) or `user_max_energy_change` (MORE_ENERGY)
  specifically off of *this upgrade's own* improvement row (B2's `:192`).
  Grepped `rust-backend/owge-business/src` for `user_max_energy_change` /
  an upgrade-triggered `unit_type_change` emission in the level-up path and
  found none — only `unit_mission_bo.rs`'s BUILD_UNIT branch emits
  `unit_type_change` (`:771`), not the LEVEL_UP branch (`:773-779`).
- **Registering a level-up for a never-obtained upgrade**: Java NPEs (500,
  B10); Rust returns a structured `OwgeError::NotFound` (`mission_bo.rs:1033-1037`,
  presumably a clean 404). This is Rust being *more correct*, not a bug to
  port — flagged in §6 as a spec-level question, not a parity target.

## 6. Open questions / suspected divergences

1. **`ZERO_UPGRADE_TIME` constant: 3 vs 5.** Confirmed by direct code
   reading, not yet run. `business/.../MissionBo.java:130` sets `3D`;
   `rust-backend/owge-business/src/bo/mission_bo.rs:1079` sets `5.0` (its own
   comment at `:1071` says "collapses the research time to 3s", contradicting
   the literal on the next line — looks like a copy-paste-then-drifted
   constant, not an intentional change). This is the single most
   actionable/cheapest bug this inventory found: it will fail on the very
   first LEVEL_UP scenario's `missions.required_time`/`termination_date` once
   the harness exists, with zero cascade complexity. Recommend fixing before
   or immediately after Phase 1 lands, independent of the BDD harness
   timeline.
2. **`cancel_upgrade_mission` missing `unit_type_change`.** Real gap or
   intentional simplification (the event is semantically vestigial for
   LEVEL_UP — no units ever move)? If a frontend view depends on
   `unit_type_change` firing on *every* mission cancel (not just BUILD_UNIT)
   to refresh some derived state, this is a real bug; if the frontend only
   reacts to it for unit-count changes, Rust's omission is harmless-but-still
   a parity diff the Layer 2 differ will flag. Needs a product-level call,
   not just a code-level one — flag to Kevin per Pitfall #11 in the plan
   rather than "fixing" either side unilaterally.
3. **`ImprovementBo.triggerChange`'s dead branches.** Only 2 of 8 possible
   `ImprovementChangeEnum` branches have registered listeners
   (`UNIT_IMPROVEMENTS` → `UnitTypeBo`, `MORE_ENERGY` → `UserEventEmitterBo`).
   Is this intentional (the other 5 improvement kinds don't need a push
   because their consuming views already refresh via `user_improvements_change`
   alone), or dead code from a removed feature? If intentional, Rust's
   `evict_and_emit`-only approach is actually already sufficient *except* for
   those two live branches — worth confirming with Kevin before porting the
   full generic listener mechanism (over-porting unused machinery isn't
   free either).
4. **B10 (NPE on registering a never-obtained upgrade): is Java's crash the
   spec, or is it an accident nobody noticed because the frontend only ever
   offers `upgradeId`s the player has an obtained-upgrade row for?** Per
   Pitfall #11, if this scenario is written and Java 500s, that is a
   `JAVA_SPEC` failure by construction (a crash is never "intended
   behavior") — this is a case where the harness should surface the bug
   report to Kevin rather than the scenario author encoding "expect a 500"
   as if it were correct. Rust's clean 404 is very likely the better
   behavior; the open question is whether Java should be patched to match
   (add a null check before `checkUpgradeIsAvailable`) rather than the
   scenario being written to pin the crash.
5. **B11 (`UPGRADE_LEVEL_LOWER_THAN` never re-triggered standalone) — is this
   working as designed?** The requirement type's own semantics ("granted
   while below level N") suggest it's meant to auto-revoke once the upgrade
   crosses N, which never happens via any current trigger path in either
   backend (Rust faithfully reproduces the gap). If no relation in the live
   content actually uses `UPGRADE_LEVEL_LOWER_THAN` as its *only* gate (i.e.
   it's always paired with another requirement whose trigger incidentally
   reprocesses it), this is theoretical; worth a quick
   `SELECT relation_id, COUNT(*) FROM requirements_information WHERE
   requirement_id=(SELECT id FROM requirements WHERE code='UPGRADE_LEVEL_LOWER_THAN')
   GROUP BY relation_id` cross-referenced against how many *other*
   requirement rows those same `relation_id`s have, to see whether live
   content is exposed to it. Flagged, not fixed, per this task's read-only
   scope.
5b. Live check performed (read-only, in-scope): `requirements.id=10` is
   `UPGRADE_LEVEL_LOWER_THAN`; a quick census of `requirements_information`
   for that `requirement_id` was not run in this pass — left as the first
   thing to check before writing the B11 scenario, since if zero rows
   reference it in live content the "revocation never happens" scenario has
   nothing to seed against baseline data (would need a fresh `[NEW]` gated
   relation, as sketched in §3's last scenario).
6. **`upgrade id` has no VERIFIED reserved range in the plan** (unlike
   units/time specials/special locations/missions). This inventory
   recommends `>= 200` (max live id is 143) but that is a **proposal**, not
   a verified fact like the plan's other ranges — confirm with Kevin or
   re-verify against DB state before the harness locks it in, especially if
   other inventories (units, time specials) also end up wanting new upgrade
   fixtures and could collide with each other's proposed ranges.
7. **`missionsCount` in the `registerLevelUp` response includes the
   just-created mission** (`UpgradeRestService.java:44`, counts *after*
   registration) — confirm Rust's `register_level_up` route
   (`upgrade.rs:48-57`) counts at the same point relative to commit (it
   does appear to, calling `count_unresolved_missions` after
   `register_level_up_an_upgrade` returns, but this wasn't traced through a
   live request in this pass).
