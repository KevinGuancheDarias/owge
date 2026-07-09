# time-specials — Java reference behavior inventory

Sources read in full: `game-rest/.../rest/game/TimeSpecialRestService.java`,
`game-rest/.../rest/admin/AdminTimeSpecialRestService.java`,
`business/.../business/TimeSpecialBo.java`, `ActiveTimeSpecialBo.java`,
`ScheduledTasksManagerService.java` + `AbstractScheduledTasksManagerService.java` +
`QuartzScheduledTaskManagerService.java`, `pojo/ScheduledTask.java`,
`configurations/QuartzConfiguration.java` + `META-INF/quartz.properties` +
`META-INF/quartz-context.xml`, `business/requirement/TimeSpecialEnabledRequirementSourceBo.java`,
`TimeSpecialAvailableRequirementSourceBo.java`, `business/timespecial/UnlockableTimeSpecialService.java`,
`business/rule/timespecial/ActiveTimeSpecialRuleFinderService.java`, the three
`business/rule/type/timespecial/*ProviderBo.java` rule-type constants and their consumers
(`AttackBypassShieldService.java`, `SpeedImpactGroupFinderBo.java`, `HiddenUnitBo.java`,
`business/event/listener/timespecial/TemporalUnitsListener.java`,
`business/schedule/TemporalUnitScheduleListener.java`), `business/RequirementBo.java` (the
generic unlock/revoke cascade `activate`/`deactivate` drive), `entity/ActiveTimeSpecial.java`,
`entity/TimeSpecial.java`, `dto/ActiveTimeSpecialDto.java`, `dto/TimeSpecialDto.java`,
`enumerations/TimeSpecialStateEnum.java`, `enumerations/RequirementTypeEnum.java`,
`repository/ActiveTimeSpecialRepository.java`, `repository/TimeSpecialRepository.java`,
`rest/trait/WithReadRestServiceTrait.java`, `exceptionhandler/SgtGameRestExceptionHandler.java`,
`util/SpringRepositoryUtil.java`, `business/database/02_schema.sql` (`active_time_specials`,
`time_specials`, `QRTZ_*`, `scheduled_tasks`), the live dev DB (`SHOW TABLES LIKE 'QRTZ%'`,
`DESCRIBE QRTZ_TRIGGERS`/`QRTZ_SIMPLE_TRIGGERS`/`scheduled_tasks`, max ids), test suites
`ActiveTimeSpecialBoTest.java` / `TimeSpecialBoTest.java` (behavior confirmation, incl.
mock-verified emission counts), and the Rust port (`rust-backend/owge-business/src/bo/
active_time_special_bo.rs`, `time_special_bo.rs`, `active_time_special_rule_finder_bo.rs`,
`temporal_units_bo.rs`, `requirement_bo.rs`, `requirement_engine.rs`,
`rust-backend/owge-rest/src/routes/game/mod.rs`, `rust-backend/owge-rest/src/main.rs`).

## 1. Endpoints

| HTTP | Path | Controller | Bo entry point |
|---|---|---|---|
| GET | `game/time_special` | `WithReadRestServiceTrait.findAll` (default method, inherited by `TimeSpecialRestService`) — `WithReadRestServiceTrait.java:35-50` | `timeSpecialRepository.findAll()` → `TimeSpecialBo.toDto` per row (`TimeSpecialBo.java:100-109`) |
| GET | `game/time_special/{id}` | `WithReadRestServiceTrait.findOneById` — `WithReadRestServiceTrait.java:57-67` | `SpringRepositoryUtil.findByIdOrDie` (404 if absent) → `TimeSpecialBo.toDto` |
| POST | `game/time_special/activate` | `TimeSpecialRestService.activate` — `TimeSpecialRestService.java:46-49` | `ActiveTimeSpecialBo.activate` — `ActiveTimeSpecialBo.java:155-184` |
| (sync, not a route) | ws initial-sync handler `time_special_change` | `TimeSpecialRestService.findSyncHandlers` — `TimeSpecialRestService.java:72-88` | `activeTimeSpecialBo.findByUserWithCurrentStatus(user)` — `ActiveTimeSpecialBo.java:128-134` (`@TaggableCacheable`) |
| GET/POST/PUT/DELETE | `admin/time_special/**` | `AdminTimeSpecialRestService` (full CRUD trait) — `AdminTimeSpecialRestService.java:26-78` | `TimeSpecialBo.save`/`delete` (`TimeSpecialBo.java:65-98`) — admin-only, out of scope for player-facing scenarios but relevant to Given-step seeding (creates the `object_relations` + `requirements_information` rows) |

Not a REST endpoint but part of the surface the domain agent must know about: the two Quartz
job types `TIME_SPECIAL_EFFECT_END` and `TIME_SPECIAL_IS_READY` registered in
`ActiveTimeSpecialBo.init` (`ActiveTimeSpecialBo.java:85-100`, `@PostConstruct`) — these are the
scheduled transitions covered as B-numbers below, not endpoints.

`GET game/time_special` and `GET game/time_special/{id}` return **every** time special (not
scoped to "unlocked" ones) — `WithReadRestServiceTrait.findAll` calls
`config.getRepository().findAll()` unconditionally (`WithReadRestServiceTrait.java:42`); scoping
to "unlocked only" is specific to the `time_special_change` sync payload (see B3).

## 2. Behavior catalog

### `activate` (POST `game/time_special/activate`) — `ActiveTimeSpecialBo.activate`, `ActiveTimeSpecialBo.java:155-184`

- **B1. Time special does not exist.** `timeSpecialBo.findByIdOrDie(timeSpecialId)`
  (`ActiveTimeSpecialBo.java:157`) → `SpringRepositoryUtil.findByIdOrDie` throws `NotFoundException`
  → `SgtGameRestExceptionHandler.handleNotFoundResource` maps to **HTTP 404**
  (`SgtGameRestExceptionHandler.java:32-35`). No DB writes, no websocket.

- **B2. Relation exists but not unlocked for the caller.**
  `objectRelationBo.findOne(TIME_SPECIAL, id)` (`ActiveTimeSpecialBo.java:158-159`,
  `ObjectRelationBo.java:99-101`) finds the `object_relations` row (created when an admin first
  attached a requirement — see `RequirementBo.addRequirementFromDto`,
  `RequirementBo.java:256-273`, or `ObjectRelationBo.findObjectRelationOrCreate`,
  `ObjectRelationBo.java:118-129`). `objectRelationBo.checkIsUnlocked(loggedUser, relation)`
  (`ActiveTimeSpecialBo.java:161`, impl `ObjectRelationBo.java:191-205`) throws
  `SgtBackendTargetNotUnlocked` (a bare `CommonException`) if
  `unlocked_relation` has no row for `(user, relation)`. **No dedicated `@ExceptionHandler`**
  exists for `CommonException`/`SgtBackendTargetNotUnlocked` in
  `SgtGameRestExceptionHandler.java` (only `SgtBackendInvalidInputException`, `NotFoundException`,
  `AccessDeniedException`, `ProgrammingException` have handlers) — it falls through to the
  kevinsuite base `RestExceptionHandler`'s generic fallback, which in practice surfaces as
  **HTTP 500**.

- **B2a (edge case — Java-only NPE, see §6).** If the time special was *never* attached to any
  requirement by an admin, `objectRelationBo.findOne` returns `null` (no `object_relations` row
  exists at all for that `TIME_SPECIAL`/id pair — `ObjectRelationsRepository.findOneByObjectCodeAndReferenceId`
  returns `null`, not `Optional`). `checkIsUnlocked(user, relation)` then dereferences
  `relation.getId()` on a null relation (`ObjectRelationBo.java:191-192`) → unhandled
  `NullPointerException` → falls through to the generic exception path → **HTTP 500**, but with
  `exceptionType` = `NullPointerException`, not `CommonException`/`SgtBackendTargetNotUnlocked`.
  This is a genuine (if obscure) Java bug, not intended behavior — see §6 for the Rust divergence.

- **B3. Already ACTIVE or RECHARGE (idempotent no-op).**
  `findOneByTimeSpecial(timeSpecial.getId(), loggedUser.getId())` (`ActiveTimeSpecialBo.java:162`,
  `120-122`) queries `ActiveTimeSpecialRepository.findOneByTimeSpecialIdAndUserId` **without a
  state filter** — a row in **either** `ACTIVE` or `RECHARGE` state counts as "currently active"
  for this check. If found: logs `"The specified time special, is already active, doing
  nothing"` (`ActiveTimeSpecialBo.java:181`) and returns the **existing** row unchanged — no DB
  writes, no scheduled task, no `triggerTimeSpecialStateChange`, no websocket emission
  (test-confirmed: `ActiveTimeSpecialBoTest.activate_should_do_nothing_if_already_active`,
  `ActiveTimeSpecialBoTest.java:300-328`, asserts `never()` on `scheduledTasksManagerService`,
  `requirementBo`, `socketIoService`). **Note:** this means a special still recharging (state
  `RECHARGE`, not yet reached `readyDate`) also short-circuits `activate` silently — the caller
  gets HTTP 200 with the stale RECHARGE row back, not an error indicating "not ready yet". This
  is a real UX/API quirk worth an explicit scenario (B3 in §3).

- **B4. Successful activation.** (`ActiveTimeSpecialBo.java:163-179`)
  1. `INSERT active_time_specials` — `state='ACTIVE'`, `activation_date=now`,
     `expiring_date = now + duration*1000ms` (`computeExpiringDate`, `ActiveTimeSpecialBo.java:289-292`),
     `user = userSessionService.findLoggedInWithDetails()` (a **fresh** DB fetch, not the
     lighter `findLoggedIn()` used for the unlock check).
  2. `improvementBo.clearSourceCache(user, this)` (`ActiveTimeSpecialBo.java:172`) — evicts two
     cache entries and (regardless of cache hit/miss) **emits websocket event
     `user_improvements_change`** with the recomputed improvement payload
     (`ImprovementBo.java:137-148`, `socketIoService.sendMessage(user, "user_improvements_change",
     …)`) — this fires synchronously inside `activate`, not post-commit.
  3. Registers a `ScheduledTask("TIME_SPECIAL_EFFECT_END", newActive.getId())` for
     `timeSpecial.getDuration()` seconds out (`ActiveTimeSpecialBo.java:173-174`) — via Quartz,
     see the dedicated scheduler section below.
  4. `requirementBo.triggerTimeSpecialStateChange(user, timeSpecial)`
     (`ActiveTimeSpecialBo.java:175`, impl `RequirementBo.java:228-234`) — re-evaluates every
     `object_relations` row gated by a `HAVE_SPECIAL_ENABLED` requirement whose `second_value`
     equals this time special's id. For each such relation this may **grant** the unlock
     (`registerObtainedRelation`, `RequirementBo.java:412-443`) — inserting into
     `unlocked_relation` and, for `UNIT`/`TIME_SPECIAL` targets, emitting
     `unit_unlocked_change`/`time_special_unlocked_change` **after commit**
     (`transactionUtilService.doAfterCommit`, `RequirementBo.java:503-510`); `REQUIREMENT_GROUP`
     master/slave chains are also processed (`processRelationList`, `RequirementBo.java:281-310`).
     This is the domain-specific analogue of the special-location-unlock bug pattern (§6.1 of the
     parent plan) — a unit/time-special/upgrade/speed-impact-group gated by
     `HAVE_SPECIAL_ENABLED` on *this* time special becomes unlocked the instant it activates.
  5. `emitTimeSpecialChange(user)` (`ActiveTimeSpecialBo.java:176`, `245-247`) — **synchronous**
     (not post-commit) `socketIoService.sendMessage(user, "time_special_change",
     findByUserWithCurrentStatus(user))`, the full list of the user's *unlocked* time specials
     with per-item activation status (see B-payload note below).
  6. `emitIfActivationAffectingUnits(newActive)` (`ActiveTimeSpecialBo.java:177`, `279-287`) — if
     any `rules` row has `origin_type='TIME_SPECIAL', origin_id=<this id>` and
     `destination_type IN ('UNIT','UNIT_TYPE')`, emits `unit_obtained_change`
     (`ObtainedUnitEventEmitter.emitObtainedUnits`, `ObtainedUnitEventEmitter.java:32-33`,
     constant `UNIT_OBTAINED_CHANGE="unit_obtained_change"`, line 18) — covers both the
     `TIME_SPECIAL_IS_ENABLED_DO_HIDE`/`…SWAP_SPEED_IMPACT_GROUP` visibility/speed rules (which
     don't literally add units but change how existing ones are reported) and
     `TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS` (which does grant new `obtained_units` rows, see
     B9 below — the emit here is in addition to, not instead of, the temporal-units listener's
     own `emitObtainedUnits` call at `TemporalUnitsListener.java:91`, so under B9 the event is
     emitted **twice** in one activation — see §6 for whether this is a suppressible diff).
  7. `applicationEventPublisher.publishEvent(newActive)` (`ActiveTimeSpecialBo.java:178`) — an
     in-process Spring event, consumed **`BEFORE_COMMIT`** by
     `TemporalUnitsListener.onTimeSpecialActivated` (`TemporalUnitsListener.java:47-55`) — see B9.

- **B5. `activate` throws with an unlocked-but-not-yet-triggered relation (empty requirements
  list).** If the `object_relations` row for the time special has zero `requirements_information`
  rows, `checkIsUnlocked` still requires an explicit `unlocked_relation` row (nothing
  auto-unlocks a zero-requirement relation without some trigger having run once — see §6.2 Given
  step notes for how a scenario seeds this directly). Not a distinct code path, just a
  clarification for Given-step authors: **do not** assume "no requirements" ⇒ "auto-unlocked".

### `deactivate` — `TIME_SPECIAL_EFFECT_END` scheduled transition (private method,
`ActiveTimeSpecialBo.java:259-277`, registered as a Quartz handler at `ActiveTimeSpecialBo.java:88-91`)

- **B6. Effect-end fires for a still-existing row.**
  1. `activeTimeSpecial.setState(RECHARGE)`, `readyDate = now + rechargeTime*1000ms`
     (`computeExpiringDate` reused), `repository.save(...)` (`ActiveTimeSpecialBo.java:264-267`).
     **Note: the row is NOT deleted** — it transitions `ACTIVE → RECHARGE` in place, same row id.
  2. `improvementBo.clearSourceCache(user, this)` (line 269) — same `user_improvements_change`
     emission as B4.2 (the special stops contributing its improvement).
  3. Schedules `ScheduledTask("TIME_SPECIAL_IS_READY", id)` for `rechargeTime` seconds out (line 270).
  4. `requirementBo.triggerTimeSpecialStateChange(user, timeSpecial)` (line 271) — re-evaluates
     the same `HAVE_SPECIAL_ENABLED` relations; since the special is now `RECHARGE` (not
     `ACTIVE`), `TimeSpecialEnabledRequirementSourceBo.checkRequirementIsMet`
     (`TimeSpecialEnabledRequirementSourceBo.java:24-31`, filters
     `state == TimeSpecialStateEnum.ACTIVE`) now evaluates **false**, so any relation that had
     been granted in B4.4 is now **revoked** via `unregisterLostRelation`
     (`RequirementBo.java:450-468`) — deletes the `unlocked_relation` row and, for
     `UNIT`/`TIME_SPECIAL`, emits the corresponding `*_unlocked_change` (full current list, so the
     revoked item is simply absent) after commit.
  5. `emitTimeSpecialChange(user)` (line 272) — same payload shape as B4.5, now showing
     `state=RECHARGE`, `readyDate` set.
  6. `emitIfActivationAffectingUnits(activeTimeSpecial)` (line 273) — same rule check as B4.6; note
     it fires again on **deactivation** too (method name is misleading — it really means "affects
     unit visibility/count", checked on both transitions).

- **B7. Effect-end fires for a row deleted out-of-band.** `findById(id)` returns null (e.g., an
  admin manually deleted the `active_time_specials` row, or the user was deleted in the interim)
  → logs `"ActiveTimeSpecial was deleted outside"` (`ActiveTimeSpecialBo.java:275`) and returns —
  no save, no scheduling, no trigger, no websocket (test-confirmed:
  `ActiveTimeSpecialBoTest.time_special_effect_end_handler_should_do_nothing_if_side_deleted`,
  lines 127-146).

### `TIME_SPECIAL_IS_READY` scheduled transition (`ActiveTimeSpecialBo.java:92-100`)

- **B8. Recharge complete.** `findById(id)`; if present: `repository.delete(forDelete)` — the
  `active_time_specials` row is **deleted entirely** (the special becomes activatable again,
  since `findOneByTimeSpecial` will now return nothing) — then `emitTimeSpecialChange(forDelete.getUser())`
  (`ActiveTimeSpecialBo.java:96-99`). If absent (deleted out-of-band): silent no-op, only a debug
  log (no test assertion on output text for this branch, but
  `time_special_is_ready_handler_should_do_nothing_if_side_deleted_maybe_by_admin`,
  `ActiveTimeSpecialBoTest.java:231-247`, confirms no delete/no emit). **Unlike B6, this
  transition does NOT call `requirementBo.triggerTimeSpecialStateChange`** — the recharge-complete
  event does not itself change unlock state (a `HAVE_SPECIAL_ENABLED` gate cares only about
  ACTIVE vs not-ACTIVE, and the special was already non-ACTIVE throughout `RECHARGE`), so no
  `unlocked_relation` mutation and no `*_unlocked_change` here — only `time_special_change`.

### `relationLost` — cross-cutting cascade when the time special's OWN unlock is revoked
(`ActiveTimeSpecialBo.java:186-196`, implements `RequirementComplianceListener.relationLost`)

- **B9. A user loses eligibility for the time special itself while it is ACTIVE.** Fired by
  `RequirementInternalEventEmitterService.doNotifyLostRelation`
  (`RequirementInternalEventEmitterService.java:20-24`) whenever *any* trigger
  (`triggerUnitBuildCompletedOrKilled`, `triggerSpecialLocation`, `triggerFactionSelection`, …)
  causes `unregisterLostRelation` to run against a `TIME_SPECIAL` relation (e.g. the special was
  gated by `HAVE_SPECIAL_LOCATION` and the planet was lost, or by `HAVE_UNIT` and the qualifying
  unit was destroyed — this is the `HAVE_SPECIAL_AVAILABLE`-style gate, contrast with B6's
  `HAVE_SPECIAL_ENABLED`). If `ObjectEnum.TIME_SPECIAL.isObject(...)` and there is an `ACTIVE`
  row for `(timeSpecialId, user)`: calls the **same private `deactivate(id)`** as B6 — i.e. the
  time special is force-transitioned `ACTIVE → RECHARGE` (not deleted, not reset to "available
  immediately"), with all the same side effects (B6.1-B6.6). If the row is `RECHARGE` already or
  absent: no-op (test-confirmed `relationLost_should_do_nothing_if_not_active`,
  `ActiveTimeSpecialBoTest.java:339-353`). **Important: `deactivate()` (private, called from both
  the Quartz job B6 and this cascade) is not itself `@Transactional`** — only the *caller*
  matters: `relationLost` is `@Transactional` (`ActiveTimeSpecialBo.java:187`) so B9's `deactivate`
  runs inside the caller's transaction; but the Quartz job handler in `init()`
  (`ActiveTimeSpecialBo.java:88-91`) has **no** `@Transactional` wrapper at all — B6's
  `repository.save`, the `TIME_SPECIAL_IS_READY` scheduling, and
  `requirementBo.triggerTimeSpecialStateChange` (which is itself `@Transactional`, so gets its own
  transaction) run as **separate, non-atomic operations** when fired from the Quartz thread. See
  §6 — this is a real Java-reference atomicity gap that the Rust port closes (see §5), which is a
  legitimate topic for a parity scenario, not necessarily something to "fix" in Rust.

### `TemporalUnitsListener.onTimeSpecialActivated` — `BEFORE_COMMIT` grant of temporal units
(`TemporalUnitsListener.java:47-93`)

- **B10.** Runs inside the *same* transaction as B4 (Spring `@TransactionalEventListener(phase =
  BEFORE_COMMIT)`, fired by the `applicationEventPublisher.publishEvent(newActive)` at B4.7). For
  every `rules` row with `origin_type='TIME_SPECIAL', origin_id=<activated id>,
  type='TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS'` whose `destination_type='UNIT'` and
  `extra_args` has exactly 2 entries (`[duration_seconds, count]`,
  `TemporalUnitsListener.java:61,100-103`): builds `count` new `ObtainedUnit` rows for
  `destination_id`, sourced at `OwgeContextHolder`'s selected planet or else the user's home
  planet (`resolvePlanet`, lines 117-121), grouped by `duration`. Per distinct duration: inserts
  one `obtained_unit_temporal_information` row (`duration`, `expiration = now + duration`,
  `relationId` = the TIME_SPECIAL's `object_relations` id), stamps every grouped `ObtainedUnit`
  with that `expirationId`, schedules a `ScheduledTask("UNIT_EXPIRED", temporalInformation.getId())`
  for `duration` seconds out (`scheduleTask`, lines 95-98 — **this one goes through the same
  Quartz `ScheduledTasksManagerService`**, not a separate mechanism), saves the units, and
  (unconditionally when any group was processed) emits `unit_obtained_change`
  (`obtainedUnitEventEmitter.emitObtainedUnits(user)`, line 91) — this is the "twice" emission
  noted in B4.6.

### `UNIT_EXPIRED` scheduled transition — `TemporalUnitScheduleListener`
(`TemporalUnitScheduleListener.java:55-66`, `98-122`)

- **B11.** When a temporal-unit group's duration elapses: deletes the `obtained_units` rows
  tagged with that `expirationId`, emits `unit_obtained_change` (after commit, via
  `obtainedUnitEventEmitter.emitObtainedUnitsAfterCommit`, line 104), and — if any affected unit
  was mid-mission — cleans up now-unit-less missions and emits mission-count/enemy-mission
  events (lines 124-158). Distinct from the time-special lifecycle proper but reachable only via
  a prior B10, and driven through the **same Quartz scheduler** as `TIME_SPECIAL_EFFECT_END`/
  `TIME_SPECIAL_IS_READY` (different `task.getType()` string, `"UNIT_EXPIRED"`, same job store).
  `TemporalUnitScheduleListener` also implements `relationLost` (lines 68-76) — when the TIME_SPECIAL
  relation itself is lost, it force-deletes any outstanding temporal units granted by that
  special's `TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS` rule immediately (`doDeactivateTimeSpecial`,
  lines 78-96), independent of (and in addition to) B9's state transition on the
  `active_time_specials` row itself. Both listeners are registered as
  `RequirementComplianceListener` beans and are invoked in whatever order Spring injects the
  `List<RequirementComplianceListener>` — order is not guaranteed by the interface.

### Passive read-time effects while a special is ACTIVE (rule consumers — not endpoints, but
gate other domains' endpoints; documented here because they are pure functions of
`active_time_specials.state='ACTIVE'` + `rules`)

- **B12. `TIME_SPECIAL_IS_ENABLED_DO_HIDE` — unit invisibility.**
  `HiddenUnitBo.isHiddenUnit`/`isHiddenUnitInternal` (`HiddenUnitBo.java:34-52`) OR's a unit's own
  `isInvisible` flag with
  `activeTimeSpecialRuleFinderService.existsRuleMatchingUnitDestination(user, unit,
  "TIME_SPECIAL_IS_ENABLED_DO_HIDE")` (constant at
  `TimeSpecialIsActiveHideUnitsTypeProviderBo.java:9`). Consumed wherever obtained-unit DTOs are
  built for a viewer who isn't the owner (mission/scouting visibility) — out of this domain's
  endpoint list but the rule-matching machinery (`ActiveTimeSpecialRuleFinderService.java:32-54`)
  belongs here: it queries `active_time_specials` for `state='ACTIVE'` rows of the *unit-owning*
  user, joins their `rules` by `(origin_type='TIME_SPECIAL', origin_id=time_special_id)`, filters
  by rule type, then `RuleBo.isWantedUnitDestination` (`UNIT` exact match or `UNIT_TYPE`
  ancestry walk).
- **B13. `TIME_SPECIAL_IS_ENABLED_DO_SWAP_SPEED_IMPACT_GROUP` — speed group override.**
  `SpeedImpactGroupFinderBo.findApplicable` (`SpeedImpactGroupFinderBo.java:46-67`) — if the user
  has an ACTIVE special with this rule type and non-empty `extra_args`, the mission-speed
  calculation uses `speed_impact_groups` row `Integer.parseInt(extra_args.get(0))` instead of the
  unit's own/inherited group.
- **B14. `TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD` — combat shield bypass.**
  `AttackBypassShieldService.bypassShields` (`AttackBypassShieldService.java:10-20`) — OR's the
  source unit's own `bypassShield` flag with an ACTIVE-special rule of this type on the
  **attacking** user matching the **target** unit as destination.

All of B12-B14 recompute from the DB on every call in Java (backed by `@TaggableCacheable`,
evicted via the `ACTIVE_TIME_SPECIAL_BY_USER_CACHE_TAG`/`RULE_CACHE_TAG` tags whenever B4/B6/B8
run) — they are pure reads gated by `active_time_specials.state`, so any BDD scenario that
activates a special with one of these rule types and then exercises the gated feature
(scouting/attack/mission-speed scenarios, owned by other domain agents) is implicitly covering
this domain's B4/B6 correctness too.

### Scheduling mechanism — **Quartz, NOT db-scheduler** (critical for the nudge technique)

This is the one place time-specials structurally differs from the mission system the parent plan
document assumes as the default. **`ActiveTimeSpecialBo` uses
`ScheduledTasksManagerService`/`QuartzScheduledTaskManagerService`
(`business/QuartzScheduledTaskManagerService.java`), which is Spring's `SchedulerFactoryBean`
wired to a JDBC-persisted Quartz job store — not db-scheduler's `scheduled_tasks` table.**

- `META-INF/quartz.properties` (lines 1-7): `org.quartz.jobStore.class =
  org.springframework.scheduling.quartz.LocalDataSourceJobStore`,
  `driverDelegateClass = StdJDBCDelegate`, `tablePrefix = QRTZ_`, `isClustered = false`,
  `threadPool.threadCount = 1`. `META-INF/quartz-context.xml` (lines 10-14) wires the
  `SchedulerFactoryBean` to the app's own `dataSource` — Quartz state lives in the **same MySQL
  database** as everything else, in tables `QRTZ_JOB_DETAILS`, `QRTZ_TRIGGERS`,
  `QRTZ_SIMPLE_TRIGGERS`, `QRTZ_FIRED_TRIGGERS`, `QRTZ_LOCKS`, etc. (verified live:
  `SHOW TABLES LIKE 'QRTZ%'` returns 11 tables; all empty at rest, i.e. no leftover triggers in
  the current dev DB baseline).
- `QuartzScheduledTaskManagerService.doSchedule` (`QuartzScheduledTaskManagerService.java:88-113`):
  for each `registerEvent(task, deliverAfterSeconds)` call, generates a random UUID `jobName`,
  builds a `JobKey(jobName, task.getType())` and `TriggerKey("trigger_" + jobName,
  task.getType())` — **`TRIGGER_GROUP` and `JOB_GROUP` in the QRTZ tables equal the task type
  string** (`"TIME_SPECIAL_EFFECT_END"`, `"TIME_SPECIAL_IS_READY"`, or `"UNIT_EXPIRED"` for B10's
  temporal-unit expiry — all three share this one scheduler). `JobDataMap` carries
  `eventUuid` and `task` = `Gson.toJson(ScheduledTask)` (a JSON string of `{id,type,content}`,
  where `content` is the `active_time_specials.id` — deserializes as a `Double` on the Quartz
  side, hence `resolveTaskId`'s `Double`/`Long` branch, `ActiveTimeSpecialBo.java:294-300`).
  `SimpleTrigger.startAt(now + deliverAfterSeconds*1000)` — a one-shot trigger,
  `QRTZ_TRIGGERS.NEXT_FIRE_TIME` (bigint, epoch **millis**) is what actually drives firing.
- **Nudge technique for Java time-special scenarios (NEW — the §3/§6.3 mission-nudge technique
  does NOT apply here, it targets db-scheduler's `scheduled_tasks` table which Quartz never
  touches):**
  ```sql
  UPDATE QRTZ_TRIGGERS SET NEXT_FIRE_TIME = (UNIX_TIMESTAMP(NOW(3)) * 1000) - 1000
   WHERE TRIGGER_GROUP = 'TIME_SPECIAL_EFFECT_END';   -- or 'TIME_SPECIAL_IS_READY' / 'UNIT_EXPIRED'
  ```
  then poll `active_time_specials.state` (or, for `UNIT_EXPIRED`, poll for the temporal
  `obtained_units` rows to disappear) until it flips, with a timeout. Two caveats a driver
  implementer MUST verify empirically before relying on this (not verified in this pass — the
  QRTZ tables are empty at rest, so `SCHED_NAME` could not be read from a live row):
  1. **`SCHED_NAME`** is part of every QRTZ table's composite primary key; it must be included
     in the `WHERE` (or left unconstrained if only one scheduler instance ever runs against this
     DB, which is the case here — `isClustered=false`, one Java container). Confirm the actual
     value with `SELECT DISTINCT SCHED_NAME FROM QRTZ_TRIGGERS` right after the **first**
     activation in a session, rather than assuming Spring's default.
  2. **Signaling latency.** `LocalDataSourceJobStore`'s scheduler thread normally learns about a
     newly-due trigger either by waiting out its own `idleWaitTime` (Quartz default **30000ms**,
     not overridden in `quartz.properties`) or by an in-JVM `signalSchedulingChange()` call made
     by the Java API path (`scheduler.scheduleJob(...)`). A raw SQL `UPDATE` against
     `QRTZ_TRIGGERS` bypasses that signal entirely, so the running scheduler will not notice until
     its **next natural poll**, up to ~30s later (worse than db-scheduler's tighter mission-poll
     interval) — size the harness's poll-with-timeout accordingly (recommend ≥45s timeout for
     time-special scenarios, longer than the plan's default 40s mission timeout).
- **Rust does NOT reproduce Quartz at all.** The Rust port (`active_time_special_bo.rs:1-53`
  header comment, confirmed by code) deliberately **repurposes the shared db-scheduler
  `scheduled_tasks` table** for `TIME_SPECIAL_EFFECT_END`/`TIME_SPECIAL_IS_READY`/`UNIT_EXPIRED`,
  polled by its own dedicated poller (`ActiveTimeSpecialBo::spawn_effect_poller`,
  `active_time_special_bo.rs:247-257`, `POLL_INTERVAL = 3s`, started in
  `owge-rest/src/main.rs:60`) — same protocol/columns as the mission poller (claim via `version`
  CAS, `picked`/`picked_by`/`last_heartbeat`). **This means the existing §3/§6.3 mission-nudge
  SQL (`UPDATE scheduled_tasks SET execution_time = …`) DOES work for forcing Rust's time-special
  transitions, but NOT Java's** — any BDD scenario driving both backends through the same nudge
  step needs a **backend-conditional nudge** (or two named steps, one per backend, chosen by the
  runner's `OWGE_BDD_BACKEND`). This is the single most important asymmetry for whoever
  implements §6.3-equivalent time-special steps — propose it explicitly in §4 below.

## 3. Draft Gherkin scenarios

Reserved id ranges per the parent plan's VERIFIED note: units ≥9100, time specials ≥900,
missions ≥900000; users 1 (home 1002) / 2 (home 1004) exist. Time specials table max id is 688
live (verified `SELECT MAX(id) FROM time_specials` → 688), so ≥900 is clear. `requirements.code`
row ids: `HAVE_SPECIAL_AVAILABLE`=8, `HAVE_SPECIAL_ENABLED`=9 (verified live). All scenarios use
only §6 catalog steps plus the QUARANTINED steps proposed in §4 (marked `[NEW]`).

```gherkin
Feature: Time special activation, effect, and recharge lifecycle
  Reference: business/ActiveTimeSpecialBo.java, business/RequirementBo.java
  (triggerTimeSpecialStateChange), business/rule/timespecial/*, entities
  TimeSpecial/ActiveTimeSpecial. See time-specials.md for the full behavior
  catalog (B1-B14).

  Background:
    Given the standard test universe
    And time special 900 exists gated by requirement HAVE_SPECIAL_AVAILABLE with second value 9100
    # [NEW] concrete duration/rechargeTime control, see §4 step "time special {id} has duration
    # {d} seconds and recharge time {r} seconds"
    And time special 900 has duration 5 seconds and recharge time 5 seconds
    And unit 9100 exists gated by requirement HAVE_SPECIAL_ENABLED with second value 900
    And user 1 has an unlocked relation for object TIME_SPECIAL reference 900

  Scenario: Activating an unlocked time special grants HAVE_SPECIAL_ENABLED-gated unlocks
    # Covers B4 (successful activation + triggerTimeSpecialStateChange grant cascade)
    When user 1 activates time special 900
    Then table active_time_specials has a row where user_id=1 and time_special_id=900 and state=ACTIVE
    And table unlocked_relation has a row for user 1 and object UNIT reference 9100
    And user 1 received websocket event "time_special_change" where some item has id 900
    And user 1 received websocket event "unit_unlocked_change" where some item has id 9100
    And user 1 received websocket event "user_improvements_change"

  Scenario: Activating an already-active time special is a silent no-op
    # Covers B3
    Given user 1 activates time special 900
    When user 1 activates time special 900
    Then table active_time_specials has a row where user_id=1 and time_special_id=900 and state=ACTIVE
    And user 1 received no websocket event "user_improvements_change"

  Scenario: Activating a not-unlocked time special fails
    # Covers B2 — table unlocked_relation contains NOTHING for (user 2, time special 900) relation
    When user 2 activates time special 900
    Then the request failed

  Scenario: Effect end moves the special to RECHARGE and revokes the granted unlock
    # Covers B6 — requires the QRTZ nudge step (§4), NOT the mission_verify scheduled_tasks nudge
    Given user 1 activates time special 900
    When [NEW] the effect of time special 900 for user 1 ends
    Then table active_time_specials has a row where user_id=1 and time_special_id=900 and state=RECHARGE
    And table unlocked_relation has no row for user 1 and object UNIT reference 9100
    And user 1 received websocket event "unit_unlocked_change" where no item has id 9100
    And user 1 received websocket event "time_special_change" where some item has id 900

  Scenario: Recharge completion deletes the active_time_specials row
    # Covers B8
    Given user 1 activates time special 900
    And [NEW] the effect of time special 900 for user 1 ends
    When [NEW] the recharge of time special 900 for user 1 completes
    Then table active_time_specials has no row where user_id=1 and time_special_id=900
    And user 1 received websocket event "time_special_change"

  Scenario: Losing the special's own unlock while ACTIVE force-deactivates it (relationLost)
    # Covers B9 — cross-cutting cascade via a HAVE_SPECIAL_AVAILABLE-style unlock loss
    Given user 1 has 1 unit of id 9100 on planet 1002
    And user 1 activates time special 900
    When [NEW] user 1's unlocked relation for object TIME_SPECIAL reference 900 is revoked
    Then table active_time_specials has a row where user_id=1 and time_special_id=900 and state=RECHARGE
    And table unlocked_relation has no row for user 1 and object UNIT reference 9100
```

Every `Then table … / user … received websocket event …` step above is drawn verbatim from the
parent plan's §6.2/§6.4/§6.5 catalog (`table {t} has a row where …`, `table unlocked_relation has
a row for user {u} and object {OBJ} reference {rid}`, `user {u} received websocket event "{name}"
where some/no item has id {id}`, `user {u} received websocket event "{name}"`). `user {u}
activates time special {tsid}` is already listed (unimplemented) in §6.3 of the parent plan — it
needs no new spec, only an implementation. `time special {id} exists gated by requirement
{REQ_TYPE} with second value {sv}` follows the existing `unit {uid} exists gated by …` pattern
verbatim (§6.2 already documents the TIME_SPECIAL variant as "same pattern"). Everything marked
`[NEW]` is spec'd in §4.

## 4. Proposed new steps

| Step text | Why needed | Implementation notes |
|---|---|---|
| `time special {id} has duration {d} seconds and recharge time {r} seconds` | Given-step control over `time_specials.duration`/`recharge_time` — scenarios need short, deterministic windows (the catalog's default seed data may have arbitrarily long durations) so the effect/recharge nudge steps below have something to fire against without waiting out a real multi-hour window. | `UPDATE time_specials SET duration={d}, recharge_time={r} WHERE id={id}` (idempotent, no INSERT needed if the time special was already created by the existing "exists gated by …" step). |
| **`the effect of time special {tsid} for user {u} ends`** (QUARANTINE — the Quartz nudge) | This is this domain's equivalent of the parent plan's §3 mission-nudge technique #1, but Quartz's persistence (`QRTZ_*` tables, driven by `NEXT_FIRE_TIME` in epoch millis, keyed by `TRIGGER_GROUP`) is a completely different mechanism from db-scheduler's `scheduled_tasks.execution_time`. It MUST be implemented as a **backend-conditional** step (dispatch on `OWGE_BDD_BACKEND` inside the step body) because the two backends schedule this transition through different tables entirely (see §2 "Scheduling mechanism"). | Resolve `active_id` first: `SELECT id FROM active_time_specials WHERE time_special_id={tsid} AND user_id={u} AND state='ACTIVE'`. **Java branch:** `UPDATE QRTZ_TRIGGERS SET NEXT_FIRE_TIME = (UNIX_TIMESTAMP(NOW(3))*1000)-1000 WHERE TRIGGER_GROUP='TIME_SPECIAL_EFFECT_END'` (verify `SCHED_NAME` scoping per §2's open item before trusting this against a DB with concurrent scenarios), then poll `active_time_specials.state='RECHARGE'` for that `active_id`, timeout ≥45s (Quartz's 30s `idleWaitTime` plus margin — longer than the plan's default 40s mission timeout, so do NOT reuse that constant blindly). **Rust branch:** `UPDATE scheduled_tasks SET execution_time = DATE_SUB(NOW(6), INTERVAL 1 SECOND) WHERE task_name='TIME_SPECIAL_EFFECT_END' AND task_instance='{active_id}'`, poll the same way, timeout can stay at the mission-nudge default (Rust's poller ticks every 3s). Both branches converge on the same `Then` assertions (state row + emitted events), which is exactly the point — only the *nudge mechanism* differs, not the observed outcome. |
| **`the recharge of time special {tsid} for user {u} completes`** (QUARANTINE — the Quartz nudge, `TIME_SPECIAL_IS_READY` variant) | Same rationale as above, for B8. Needs its own step (not reusable with the effect-end one) because the target row transitions from `RECHARGE` to **deleted**, not `ACTIVE→RECHARGE`, so the poll predicate differs (poll for absence, not a state value). | Same `active_id` resolution (state filter `RECHARGE` this time). Java: `UPDATE QRTZ_TRIGGERS SET NEXT_FIRE_TIME=… WHERE TRIGGER_GROUP='TIME_SPECIAL_IS_READY'`, poll `NOT EXISTS (SELECT 1 FROM active_time_specials WHERE id={active_id})`. Rust: same `scheduled_tasks` nudge with `task_name='TIME_SPECIAL_IS_READY'`. |
| **`user {u}'s unlocked relation for object {OBJ} reference {rid} is revoked`** (QUARANTINE) | Needed for B9 (`relationLost` cascade) scenarios that want to force-lose an already-granted unlock **without** going through a full mission/requirement-trigger flow (e.g. simulate "an admin/other cascade revoked it") — a direct, backend-symmetric way to exercise `RequirementComplianceListener.relationLost` (Java) / `unregister_lost_relation` → `deactivate_in_tx` (Rust) in isolation. Distinct from the existing `Given user {u} has an unlocked relation for object {OBJ} reference {rid}` (§6.2), which only ever *adds* a row. | This is **not** a plain `DELETE FROM unlocked_relation` — that would silently desync from both backends' in-app cascade logic (neither backend's listener fires from a bare SQL delete). It must go through each backend's real revoke path. Simplest faithful option: reuse the domain's own trigger — e.g., if the scenario seeded the special as gated by `HAVE_UNIT` on some unit, have this step `DELETE FROM obtained_units WHERE …` for that unit and then invoke the backend's unit-kill/build-completed trigger endpoint (whatever "unit destroyed" flow already exists in another domain's When-step vocabulary) rather than inventing a bespoke revoke endpoint. **If no such indirect trigger is convenient, this step needs a real backend endpoint audit before being spec'd further — flag to Kevin; do not implement as raw SQL.** Left intentionally underspecified pending that decision — this row is the one on this page that most needs human sign-off before coding. |
| `time special {id} exists gated by requirement {REQ_TYPE} with second value {sv}` (not new — parametrize existing) | The parent plan's §6.2 table already lists `time special {tsid} exists gated by … HAVE_SPECIAL_LOCATION …` as "same pattern" as the unit variant, but this domain also needs the gate to be `HAVE_SPECIAL_AVAILABLE`/`HAVE_SPECIAL_ENABLED` (on some *other* time special/unit) to build B9-style chains. No new step — just confirming the existing template's `{REQ_TYPE}` parameter must not be hardcoded to `HAVE_SPECIAL_LOCATION` in the implementation. | Generalize the Given-step implementation to accept any `RequirementTypeEnum` name, not just `HAVE_SPECIAL_LOCATION`; validate against the live `requirements` table (`code` column) rather than a hardcoded switch. |

## 5. Rust port status

This domain is **exceptionally well ported** — more complete than most other domains audited so
far in this harness effort. Findings from `rust-backend/owge-business/src/bo/`:

- `active_time_special_bo.rs` — `activate` (B1-B5), `deactivate_in_tx`/`handle_effect_end` (B6-B7),
  `handle_is_ready` (B8), the shared db-scheduler poller (`spawn_effect_poller`,
  `poll_effects_once`) covering `TIME_SPECIAL_EFFECT_END`/`TIME_SPECIAL_IS_READY`/`UNIT_EXPIRED`
  all in one loop, and `emit_if_activation_affecting_units` (B4.6/B6.6) — all present, with the
  file's own header comment (`active_time_special_bo.rs:1-32`) explicitly noting "Effect lifecycle
  (now fully ported)".
- `time_special_bo.rs` — `find_all_dtos`/`find_dto_by_id` (GET list/by-id), `find_unlocked_dtos`/
  `find_user_status_dtos` (the `time_special_change` sync payload) — all present, doc comments
  cross-reference the exact Java methods.
- `active_time_special_rule_finder_bo.rs` — `exists_rule_matching_unit_destination` (B12/B14's
  shared rule-finding logic, `TIME_SPECIAL`→`rules` join + `UNIT`/`UNIT_TYPE` destination
  matching with ancestry walk) — ported, explicitly notes the Java `@TaggableCacheable` is
  replaced by recompute-on-demand ("no behavioural difference, only the cache optimisation is
  dropped").
- `temporal_units_bo.rs` — `grant_on_activation` (B10, `TemporalUnitsListener`) and
  `handle_unit_expired`/`on_time_special_relation_lost` (B11, `TemporalUnitScheduleListener`) —
  ported, with two **explicitly documented, deliberate divergences** in its header comment
  (`temporal_units_bo.rs:1-33`): (a) planet resolution always uses the home planet (Java prefers
  `OwgeContextHolder.selectedPlanetId` when present — the Rust activate route carries no selected
  planet context) and (b) `maybeTriggerClearImprovement` is reproduced as an unconditional
  improvement-cache evict+emit rather than Java's source-scoped one.
- `requirement_bo.rs`/`requirement_engine.rs` — `trigger_time_special_state_change`
  (`requirement_bo.rs:467-482`) mirrors `RequirementBo.triggerTimeSpecialStateChange` exactly
  (same `find_relations_with_code_and_second_value(HAVE_SPECIAL_ENABLED, …)` +
  `process_relation_list`), and B9's `relationLost` cascade is wired end-to-end:
  `unregister_lost_relation` (`requirement_bo.rs:713+`, `requirement_engine.rs:280+`) calls
  `Box::pin(ActiveTimeSpecialBo::deactivate_in_tx(...))` when the lost relation is a `TIME_SPECIAL`
  — matching Java's `RequirementComplianceListener.relationLost` fan-out.
- Consumers of the rule-finder are also ported: `attack_mission_manager_bo.rs` (B14, bypass
  shield), `unit_interception_finder_bo.rs` (B13, speed impact group swap), and B12's hide-unit
  rule appears in three separate call sites (`obtained_unit_bo.rs:299,427`,
  `unit_mission_registration_bo.rs:1135,1167`, `running_mission_finder_bo.rs:53-55,234-242`) —
  each documents the Java rule-type string it targets.
- Routes: `owge-rest/src/routes/game/mod.rs` wires `GET /game/time_special` (line 36),
  `GET /game/time_special/{id}` (line 37), `POST /game/time_special/activate` (line 63) to
  `time_special_list`/`time_special_by_id`/`time_special_activate` (lines 174-198, 440-453).
  `owge-rest/src/main.rs:60` starts `ActiveTimeSpecialBo::spawn_effect_poller` at boot.
- `rust-backend/docs/UNPORTED-ENDPOINTS.md` has **no mention of time_special** anywhere —
  consistent with this being fully ported (the doc appears to be an active-avoidance list for
  genuinely-missing routes, and this domain isn't on it).

## 6. Open questions / suspected divergences

1. **Stale documentation inside the Rust codebase itself (not a Java/Rust behavior divergence,
   but will mislead anyone reading the route file before this inventory).** The doc comment on
   `time_special_activate` (`owge-rest/src/routes/game/mod.rs:430-439`) says: *"PARTIAL (see
   `ActiveTimeSpecialBo::activate`): the activation row + DTO are faithful, but the
   `TIME_SPECIAL_EFFECT_END` task has no Rust runner yet (Java uses Quartz, not db-scheduler), so
   the effect does not auto-expire, and the `triggerTimeSpecialStateChange` requirement trigger +
   websocket emissions are not fired."* This is **no longer true** — `active_time_special_bo.rs`'s
   own header comment (lines 21-32) says the effect lifecycle is "now fully ported", and the code
   backs that up (`spawn_effect_poller` started in `main.rs:60`, `trigger_time_special_state_change`
   called from `activate` at `active_time_special_bo.rs:136-142`). The route file comment is
   leftover from an earlier development stage and should be corrected (or deleted) — flagged here
   rather than fixed, per this task's read-only constraint.
2. **B2a — Java has a latent NullPointerException that Rust does not reproduce.** Activating a
   time special with **no `object_relations` row at all** (never touched by the admin requirement
   UI) crashes Java with an unhandled NPE inside `ObjectRelationBo.checkIsUnlocked`
   (`ObjectRelationBo.java:191-192`, `relation` is `null`), surfacing as HTTP 500 with
   `exceptionType=NullPointerException`. Rust's `activate` (`active_time_special_bo.rs:80-90`)
   never looks up `object_relations` at all — it queries `unlocked_relation` directly via
   `UnlockedRelationBo::find_unlocked_reference_ids` and returns a clean `OwgeError::Common("The
   target object relation has not been unlocked")` (HTTP 500, `exceptionType=CommonException`)
   regardless of whether an `object_relations` row exists. Both end up HTTP 500 (so a
   status-code-only `Then` step wouldn't distinguish them), but the response body's
   `exceptionType`/`message` differ, and a Layer-2 diff of the response body would flag this.
   **Per pitfall #11 of the parent plan: this is Java being accidentally-wrong, not a spec to
   preserve — recommend a scenario that documents Rust's behavior as the intended one and flags
   Java's NPE path as a bug to fix upstream, not something the BDD suite should assert Rust must
   match.**
3. **Non-atomicity of the Quartz-triggered `deactivate` in Java vs. the Rust port's single
   transaction.** In Java, the `TIME_SPECIAL_EFFECT_END` Quartz handler
   (`ActiveTimeSpecialBo.java:88-91`) calls the private `deactivate(id)` method
   (`ActiveTimeSpecialBo.java:259-277`) with **no surrounding `@Transactional`** — each repository
   call (`repository.save`, the `requirementBo.triggerTimeSpecialStateChange` call which gets its
   *own* transaction since `RequirementBo` is class-level `@Transactional`) commits independently.
   If, say, the process crashes between `repository.save` (state→RECHARGE) and
   `triggerTimeSpecialStateChange`, Java would be left with a RECHARGE row whose
   `HAVE_SPECIAL_ENABLED`-gated unlocks were never revoked — a real (if narrow) consistency gap.
   The Rust port's `handle_effect_end` (`active_time_special_bo.rs:325-332`) explicitly wraps
   `deactivate_in_tx` in one transaction (`conn.begin()` … `tx.commit()`), closing this gap by
   construction. This is unlikely to be practically observable via the BDD harness (it would
   require injecting a mid-transaction crash), but is worth recording as a documented,
   intentional Rust improvement rather than a parity bug — no scenario should assert Java's
   crash-window behavior.
4. **The double `unit_obtained_change` emission noted in B4.6/B10.** When a time special has both
   a `TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS` rule (B10, emits once at
   `TemporalUnitsListener.java:91`) and thus also matches `emitIfActivationAffectingUnits`'s
   `destination_type IN ('UNIT','UNIT_TYPE')` check (B4.6, emits again at
   `ActiveTimeSpecialBo.java:283-286`), Java emits `unit_obtained_change` **twice** for one
   activation. Whether the Rust port reproduces this exact double-emission (both
   `TemporalUnitsBo::grant_on_activation` and `ActiveTimeSpecialBo::emit_if_activation_affecting_units`
   are called unconditionally from `activate`, `active_time_special_bo.rs:145,158`) was not traced
   down to a byte-for-byte confirmation in this pass — both code paths exist and both appear to run
   unconditionally, so parity is *plausible* but not verified end-to-end against a live rule-9
   fixture. **Layer-2's websocket diff (sorted multiset, per §5.5 of the parent plan) will not
   even flag a duplicate-vs-duplicate emission as different** (multiset equality), so this
   specific "is it double on both sides" question needs an explicit count-based `Then` step if
   anyone cares about it — not currently in the §6 catalog vocabulary (`user {u} received
   websocket event "{name}" exactly {n} times` would need to be added if this becomes a scenario;
   not proposed in §4 since no concrete scenario currently needs it).
5. **`SCHED_NAME` value unverified.** Flagged already in §2 — the QRTZ nudge step in §4 depends on
   knowing (or not needing to constrain by) `SCHED_NAME`; the live dev DB's `QRTZ_*` tables are
   currently empty so this could not be confirmed from a real row in this pass. Whoever implements
   the `[NEW]` nudge steps must do one live activation and inspect
   `SELECT DISTINCT SCHED_NAME FROM QRTZ_TRIGGERS` before shipping the step.
6. **B3's "RECHARGE also counts as already-active" quirk.** Calling `activate` while a special is
   `RECHARGE` (not yet `readyDate`) returns HTTP 200 with the stale RECHARGE row, not a 4xx
   "not ready yet" error — confirmed by both the Java code path (`findOneByTimeSpecial` has no
   state filter, `ActiveTimeSpecialBo.java:120-122,162`) and Rust's identical
   `find_one_by_time_special` (`active_time_special_bo.rs:197-211`, same unfiltered query) —
   this one **is** confirmed at-parity by direct code inspection, listed here only because it's a
   non-obvious API contract worth a named scenario (included in §3) rather than an implicit
   assumption.
