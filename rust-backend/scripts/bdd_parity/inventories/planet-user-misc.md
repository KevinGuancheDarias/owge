# planet-user-misc — Java reference behavior inventory

Scope: `PlanetRestService`, `PlanetListRestService`, `UserRestService`,
`TutorialRestService`, `ReportRestService`, `SystemMessageRestService`, plus a
brief pass on `TwitchStateRestService`, `TrackBrowserRestService`,
`AdminLoginRestService`, `DeliverBackdoorRestService`, `WebsocketSyncRestService`.

Ownership note: `PlanetBo.definePlanetAsOwnedBy` (the "gain a planet" side —
conquest/establish-base) and the unlock-grant/revoke mechanics of
`RequirementBo.triggerSpecialLocation` are owned by the missions-travel agent
(see `docs/BDD-PARITY-PLAN.md` §6.1 / `docs/BUG-SPECIAL-LOCATION-UNLOCK.md`).
This document only covers the "leave a planet" endpoint surface and
cross-references that shared machinery where `doLeavePlanet` calls into it.

All controllers live under
`game-rest/src/main/java/com/kevinguanchedarias/owgejava/rest/game/`.

## 1. Endpoints

### PlanetRestService (`PlanetRestService.java`)

| Verb + path | controller:line | Bo entry point |
|---|---|---|
| POST `game/planet/leave` | PlanetRestService.java:29-33 | `PlanetBo.doLeavePlanet` (PlanetBo.java:138) |
| (sync) `planet_owned_change` | PlanetRestService.java:36-39 | `planetRepository.findByOwnerId` + `PlanetBo.toDto` |

### PlanetListRestService (`PlanetListRestService.java`)

| Verb + path | controller:line | Bo entry point |
|---|---|---|
| POST `game/planet-list` | PlanetListRestService.java:30-33 | `PlanetListBo.myAdd` (PlanetListBo.java:57) |
| DELETE `game/planet-list/{planetId}` | PlanetListRestService.java:35-38 | `PlanetListBo.myDelete` (PlanetListBo.java:70) |
| (sync) `planet_user_list_change` | PlanetListRestService.java:41-44 | `PlanetListBo.findByUserId` |

### UserRestService (`UserRestService.java`)

| Verb + path | controller:line | Bo entry point |
|---|---|---|
| GET `game/user/exists` | UserRestService.java:34-38 | `AuditBo.creteCookieIfMissing` + `UserStorageBo.exists` |
| GET `game/user/subscribe` | UserRestService.java:46-49 | `UserStorageBo.subscribe` (UserStorageBo.java:107) — **mutating despite GET verb** |
| (sync) `user_data_change` | UserRestService.java:51-61 | `UserStorageBo.findById` + `UserEventEmitterBo.findData` |

### TutorialRestService (`TutorialRestService.java`)

| Verb + path | controller:line | Bo entry point |
|---|---|---|
| GET `game/tutorial/entries` | TutorialRestService.java:33-36 | `TutorialSectionBo.findEntries` (read-only) |
| POST `game/tutorial/visited-entries` | TutorialRestService.java:42-45 | `TutorialSectionBo.addVisitedEntry` (TutorialSectionBo.java:116) |
| (sync) `tutorial_entries_change` / `visited_tutorial_entry_change` | TutorialRestService.java:47-51 | `findEntries` / `findVisitedIdsByUser` |

### ReportRestService (`ReportRestService.java`)

| Verb + path | controller:line | Bo entry point |
|---|---|---|
| GET `game/report/findMy` (deprecated since 0.9.6) | ReportRestService.java:30-34 | `MissionReportBo.findMissionReportsInformation` (read-only) |
| POST `game/report/mark-as-read` | ReportRestService.java:36-39 | `MissionReportBo.markAsRead` (MissionReportBo.java:206) |
| POST `game/report/mark-as-read-before-date/{date}` | ReportRestService.java:41-44 | `MissionReportBo.markAsReadBeforeDate` (MissionReportBo.java:212) |
| (sync) `mission_report_change` | ReportRestService.java:46-50 | `MissionReportBo.findMissionReportsInformation(user,0)` |

### SystemMessageRestService (`SystemMessageRestService.java`)

| Verb + path | controller:line | Bo entry point |
|---|---|---|
| POST `game/system-message/mark-as-read` | SystemMessageRestService.java:37-40 | `SystemMessageBo.markAsRead` (SystemMessageBo.java:86) |
| (sync) `system_message_change` | SystemMessageRestService.java:31-35 | `SystemMessageBo.findReadByUser` |

### Brief: other controllers

| Verb + path | controller:line | Bo entry point |
|---|---|---|
| GET/PUT `game/twitch-state` | TwitchStateRestService.java:35-53 | `ConfigurationBo.findOrSetDefault` / `saveByKeyAndValue` |
| POST `game/track-browser/{warn,error}` | TrackBrowserRestService.java:21-29 | `trackBrowserRepository.save` |
| POST `game/adminLogin` | AdminLoginRestService.java:28-31 | `AdminUserBo.login` (AdminUserBo.java:90) |
| GET `game/deliver-backdoor/ping-user` | DeliverBackdoorRestService.java:23-30 | `SocketIoService.sendMessage` |
| GET `game/websocket-sync` | WebsocketSyncRestService.java:35-47 | `AuditBo.doBestEffortAudit` + `WebsocketSyncService.findWantedData` |

## 2. Behavior catalog

### PlanetRestService.leave -> PlanetBo.doLeavePlanet (PlanetBo.java:137-149)

**B1 — success.** Preconditions (`canLeavePlanet`, PlanetBo.java:163-167, all must hold):
not the invoker's home planet (`planetRepository.findOneByIdAndHomeTrue`), planet's
`owner` = invoker (`planetRepository.isOfUserProperty`), invoker has **no** units
stationed on that planet with `mission_id IS NULL`
(`ObtainedUnitRepository.hasUnitsInPlanet`, query at
`ObtainedUnitRepository.java:133-134`: `user_id=? AND source_planet=? AND mission IS NULL`),
and no running `BUILD_UNIT` mission on that planet
(`MissionFinderBo.findRunningUnitBuild`).
Effect: `planet.owner = NULL`, `planetRepository.save` (PlanetBo.java:144-145).
Cascade: `maybeTriggerSpecialLocation` (PlanetBo.java:207-211) — if
`planet.special_location_id IS NOT NULL`, calls
`RequirementBo.triggerSpecialLocation(former_owner, specialLocation)`
(RequirementBo.java:222-226), which re-evaluates every relation gated by
`HAVE_SPECIAL_LOCATION` on that special location for the former owner —
**this is the revoke half of the special-location unlock mechanism** owned by
the missions-travel agent; see `docs/BUG-SPECIAL-LOCATION-UNLOCK.md`. Emits
after commit: `planetListBo.emitByChangedPlanet(planet)` (PlanetBo.java:147 →
PlanetListBo.java:82-84, pushes `planet_user_list_change` to every user who has
this planet bookmarked) and `emitPlanetOwnedChange(invokerId)`
(PlanetBo.java:148, 158-161 → `planet_owned_change` to the invoker with
**their full remaining owned-planets list**, i.e. this planet will be absent).

**B2 — reject: home planet.** `canLeavePlanet` false because
`isHomePlanet(planetId)` true → throws `SgtBackendInvalidInputException`
message `ERR_I18N_CAN_NOT_LEAVE_PLANET` (PlanetBo.java:139-141), mapped to
HTTP 400 (`SgtGameRestExceptionHandler.java:27-30`). No DB/ws effect.

**B3 — reject: not owner.** Same exception; `isOfUserProperty` false.

**B4 — reject: has stationed units.** Same exception;
`hasUnitsInPlanet` true.

**B5 — reject: running unit build.** Same exception;
`findRunningUnitBuild` non-null.

**B6 — no special location.** Same success path as B1 but
`maybeTriggerSpecialLocation` is a no-op (`planet.special_location_id IS NULL`)
— `RequirementBo.triggerSpecialLocation` is never called, so no
`unlocked_relation` change and no `*_unlocked_change` event. (Confirmed by
`PlanetBoTest.doLeavePlanet_should_work_arguments`, PlanetBoTest.java:308-313:
`timesTriggerSpecialLocation` is 0 for `null` special location, 1 otherwise.)

### PlanetListRestService.add -> PlanetListBo.myAdd (PlanetListBo.java:57-64)

**B7 — success, new bookmark.** `user = userSessionService.findLoggedInWithReference()`
(a lazy reference, no ownership check on the *planet* — any existing planet id
works, owned or not). Builds `PlanetList{planetUser=(user,planet), name}` and
`repository.save(...)`. Because `PlanetList`'s `@EmbeddedId` is always
non-null and the entity has no `@Version`, Spring Data JPA's `isNew()` check
returns false, so `save()` performs `entityManager.merge(...)` — an **upsert**
on the composite PK `(user_id, planet_id)`: a repeat `add` for the same
`(user, planet)` with a different `name` **overwrites the name in place**
(no duplicate row is possible — the PK forbids it). Emits (synchronous, not
`doAfterCommit`) `emitChangeToUser` → `planet_user_list_change` to the caller
with their full list (PlanetListBo.java:100-102).

**B8 — reject: planet does not exist.** `SpringRepositoryUtil.findByIdOrDie`
(PlanetListRestService is not directly involved; called inline in `myAdd` via
`planetRepository`) throws a `NotFoundException` subtype → HTTP 404
(`SgtGameRestExceptionHandler.java:32-35`). No DB/ws effect.

### PlanetListRestService.delete -> PlanetListBo.myDelete (PlanetListBo.java:70-74)

**B9 — success.** `repository.deleteById(new PlanetUser(user, planetRepository.getReferenceById(planetId)))`.
`getReferenceById` returns an unvalidated lazy proxy (no existence check on
the *planet* row itself). Emits `planet_user_list_change` to the caller
unconditionally after the delete call returns (PlanetListBo.java:73).

**B10 — delete of a non-existent bookmark.** Spring Data's
`SimpleJpaRepository.deleteById` on a missing composite-id row throws
`EmptyResultDataAccessException` (not caught anywhere in this call chain) —
falls through to the base `RestExceptionHandler` (from the `kevinsuite`
dependency, not in this repo) rather than `SgtGameRestExceptionHandler`'s
explicit handlers; exact resulting HTTP status is **unverified** — flagged in
§6.

### UserRestService.exists (UserRestService.java:34-38) — read + side effect

Read-only DB-wise (`userStorageBo.exists`), but **always** has an HTTP side
effect: `AuditBo.creteCookieIfMissing` (AuditBo.java:92-99) sets a
`CONTROL_COOKIE_NAME` cookie (random double value, 7-day max-age, path
`/game_api`) on the response **iff the request has none yet**. This is not a
DB mutation and is invisible to the table-diff layer; a scenario would need a
raw HTTP header assertion to see it (not in the current step catalog — see §4).

### UserRestService.subscribe -> UserStorageBo.subscribe (UserStorageBo.java:107-140)

`@GetMapping` but fully mutating — do not treat as read-only.

**B11 — reject: faction does not exist.** `factionBo.exists(factionId)` false
→ `SgtFactionNotFoundException("No such faction")` (extends `CommonException`,
**not** one of the explicitly `@ExceptionHandler`-mapped types in
`SgtGameRestExceptionHandler` — falls to the base handler; exact HTTP status
unverified, see §6). No DB/ws effect.

**B12 — no-op: user already subscribed.** `userStorageRepository.existsById(user.getId())`
true → returns `false` immediately, no writes.

**B13 — success.** Picks `selectedPlanet = planetBo.findRandomPlanet(spawnGalaxy)`
(`factionSpawnLocationBo.determineSpawnGalaxy(faction)` — random galaxy row
from `faction_spawn_location`, or null for universe-wide); sets
`user.faction`, `user.homePlanet`, `primaryResource`/`secondaryResource`/`energy`
from the faction's initial values, `lastAction=now`; `userStorageRepository.save(user)`;
then `selectedPlanet.owner=user; selectedPlanet.home=true; planetRepository.save(selectedPlanet)`.
Cascades: `requirementBo.triggerFactionSelection(user)` (RequirementBo.java:150-153,
processes every relation gated by `BEEN_RACE`) and
`requirementBo.triggerHomeGalaxySelection(user)` (RequirementBo.java:161-164,
gated by `HOME_GALAXY`) — both may grant `unlocked_relation` rows and emit
`*_unlocked_change` events (cross-domain requirement-engine machinery, not
detailed further here). If `user.getId() > 0`, audits `SUBSCRIBE_TO_WORLD`
(AuditBo, DB row in the audit table). Returns `true`.
**Notably `subscribe()` itself never calls `planetBo.emitPlanetOwnedChange`,
`planetListBo.emitByChangedPlanet`, or emits `user_data_change`** — the only
websocket traffic from a successful subscribe is whatever the two
`requirementBo.trigger*` calls happen to unlock. Confirmed intentional in the
Rust port (`user_storage_bo.rs` mirrors exactly this: insert + planet update +
two trigger calls, no extra emission) — **not a divergence**, but a real gap
callers must know about when writing `Then` assertions (don't expect
`planet_owned_change` after subscribe).

**B14 — reject: universe full.** `planetBo.findRandomPlanet` finds zero
candidate planets (`SgtBackendUniverseIsFull`, extends `CommonException`,
same unmapped-handler caveat as B11) — thrown *after* the faction-exists and
already-subscribed checks but *before* any write.

### TutorialSectionBo.addVisitedEntry (TutorialSectionBo.java:115-124)

**B15 — success, first visit.** Inserts a new `VisitedTutorialSectionEntry(user, entry)`
row — `visited_tutorial_entries.id` is a plain `@GeneratedValue(IDENTITY)`
surrogate key with **no unique constraint on `(user_id, entry_id)`**
(`business/database/02_schema.sql:1184-1188`: only a bare PK on `id`). Emits
after commit: `visited_tutorial_entry_change` → `findVisitedIdsByUser(userId)`
(full list, may contain duplicates — see B16).

**B16 — repeat visit of the same entry.** Java has **no dedup guard** —
`addVisitedEntry` called twice for the same `(user, entry)` inserts a
**second row**. `findVisitedIdsByUser` (TutorialSectionBo.java:106-109) maps
every row's `entry.id` without `distinct()`, so the emitted
`visited_tutorial_entry_change` list literally contains the id twice.
**⚠ Confirmed divergence from the Rust port** — see §6.

**B17 — reject: entry does not exist.** `entryRepository.findById(entryId).get()`
(TutorialSectionBo.java:119) — `Optional.get()` on empty throws
`NoSuchElementException`, an *unchecked*, unhandled exception — no
`@ExceptionHandler` catches it, likely surfaces as a generic 500. Not caught
anywhere in this call chain.

### MissionReportBo.markAsRead (MissionReportBo.java:205-209)

**B18 — success.** `missionReportRepository.markAsReadIfUserIsOwner(reportsIds, userId)`
— bulk `@Modifying` JPQL `UPDATE MissionReport rp SET rp.userReadDate =
CURRENT_TIMESTAMP WHERE rp.user.id = :userId AND rp.id IN :reportsIds`
(MissionReportRepository.java:30-32). Report ids in the list that belong to a
*different* user are silently excluded by the `WHERE rp.user.id = :userId`
clause (no error, no partial-failure signal). Emits `emitCountChange(userId)`
(MissionReportBo.java:233-235) → `mission_report_count_change` with
`{enemyUnread, userUnread}` counts (`findUnreadCount`, MissionReportBo.java:191-197).
Bulk `@Modifying` update bypasses entity listeners — per
`CLAUDE.md` "Concurrency & caching", any by-user read cache tag on mission
reports would need manual eviction; no `@TaggableCacheEvictByTag` is present
on this Bo, so this is likely fine (no cache to go stale) but worth a spot
check if caching is ever added here.

**B19 — reject: empty list.** No guard against `reportsIds == null`/empty;
JPQL `IN ()` with an empty collection is invalid HQL and would throw at
translation time in Java (unverified in this file alone — flagged §6). The
Rust port explicitly special-cases empty (`mission_report_bo.rs:182-184`,
early return `Ok(())`), which is itself worth checking against Java's actual
behavior.

### MissionReportBo.markAsReadBeforeDate (MissionReportBo.java:211-215)

**B20 — success.** `missionReportRepository.markAsReadBeforeDate(userId, Date.from(date))`
— bulk update `WHERE rp.user.id=:userId AND rp.userReadDate IS NULL AND
rp.reportDate < :date` (MissionReportRepository.java:34-36) — idempotent by
construction (`userReadDate IS NULL` guard prevents re-touching already-read
rows). Emits `emitCountChange(userId)`, same as B18.

### SystemMessageBo.markAsRead (SystemMessageBo.java:85-94)

**B21 — success.** For each id in `messages`,
`repository.findAllById(messages)` (silently drops ids that don't exist —
same "silent partial success" pattern as B18) then **unconditionally inserts**
a new `UserReadSystemMessage(message, user)` row —
`user_read_system_messages.id` is `@GeneratedValue(IDENTITY)` with **no
unique constraint on `(user_id, message_id)`** (`02_schema.sql:1145-1149`,
only a PK on `id`). Emits after commit: `emitChangeToUser(user.getId())`
(SystemMessageBo.java:93 → 110-117) → `system_message_change` to that single
user with the **full message list**, `isRead` computed per-message via
`userReadRepository.existsByMessageIdAndUserId` (so a duplicate read-row
doesn't change the emitted payload's `isRead` flag — the divergence is
DB-row-count-only, not payload-visible through this endpoint).

**B22 — repeat mark-as-read of the same message.** Same no-dedup pattern as
B16: a second `mark-as-read` call for a message id the user already marked
read inserts a **second** `user_read_system_messages` row.
**⚠ Confirmed divergence from the Rust port** — see §6.

### TwitchStateRestService.defineState (TwitchStateRestService.java:44-53)

**B23 — success (authorized).** Requires
`userSessionService.findLoggedInWithDetails().getCanAlterTwitchState() == true`
(`user_storage.can_alter_twitch_state`). `configurationBo.saveByKeyAndValue("TWITCH_STATE", status)`
(ConfigurationBo.java:110-112, upserts the `configuration` row — not wrapped
in `doAfterCommit`, method has no `@Transactional` at all, so the config save
and the websocket send below happen back-to-back with no transactional
boundary). Broadcasts **immediately, not after commit**:
`socketIoService.sendMessage(null, "twitch_state_change", () -> statusBool)`
— `null` target user = broadcast to **every connected user** (unlike every
other emitter in this doc, which targets one user or a filtered list).

**B24 — reject: unauthorized.** `SgtBackendInvalidInputException("You can't
get out of Matrix, the system rules your live!")` → HTTP 400. No DB/ws
effect.

### TrackBrowserRestService.warn / .error (TrackBrowserRestService.java:21-29)

**B25 — success (both).** Inserts one `TrackBrowser{method, jsonContent, createdAt=now}`
row (TrackBrowserRestService.java:31-39). No validation, no websocket, no
cascade. Client-side error/warning telemetry sink — not real game state.

### AdminLoginRestService.login -> AdminUserBo.login (AdminUserBo.java:90-103)

**B26 — success, no username drift.** Requires an existing, enabled
`AdminUser` row matching the caller's game-JWT `sub`
(`authenticationBo.findTokenUser()`); returns a freshly minted admin JWT
(`ADMIN_JWT_SECRET`/`ADMIN_JWT_ALGO`/`ADMIN_JWT_DURATION_SECONDS` config rows).
No DB write.

**B27 — success, username drift.** If the token's username differs from the
stored `AdminUser.username`, updates and saves it before minting the token
(AdminUserBo.java:98-101) — a genuine (small) DB mutation on an otherwise
read-leaning endpoint.

**B28 — reject: no such admin / disabled.** `AccessDeniedException` (`ERR_NO_SUCH_USER`
/ `ERR_USER_NOT_ENABLED`) → HTTP 403. No DB/ws effect.

### DeliverBackdoorRestService.pingUser (DeliverBackdoorRestService.java:23-30)

**B29.** Sends a `"ping"` websocket message with payload `"Hello World"` to
`targetUser` — constructs a bare `UserStorage` with only `id` set (**no DB
existence check** — pinging a nonexistent user id is not an error, the
message is simply undeliverable). Debug/ops tool, not game logic; see §3/§5
for the "skip" recommendation.

### Read-only endpoints (one line each; payload parity covered elsewhere by `ws_verify`)

- `game/tutorial/entries` — all tutorial entries ordered by `order` (TutorialRestService.java:33-36).
- `game/report/findMy` (deprecated) — paginated + unread-count report response for the caller (ReportRestService.java:30-34).
- `game/twitch-state` GET — current `TWITCH_STATE` config flag (TwitchStateRestService.java:35-38).
- `game/websocket-sync` — REST hydration path for arbitrary sync keys; has the same "best-effort LOGIN audit" side effect pattern as `game/user/exists` (WebsocketSyncRestService.java:41-45), swallowing `RuntimeException` if the user isn't yet subscribed to this universe.
- Sync handlers (`findSyncHandlers` on every `SyncSource` controller above) are pure reads driven by the websocket sync mechanism, not separate REST endpoints.

## 3. Draft Gherkin scenarios

Using only §6 catalog steps where they exist (`the standard test universe`,
`planet {pid} is owned by user {u}`, `user {u} leaves planet {pid}`,
`planet {pid} has no owner`, `user {u} received websocket event "{name}"` /
`… where no item has id {id}` / `user {u} received no websocket event "{name}"`,
the generic `table {t} has a row where …` escape hatch). Everywhere a step
doesn't exist yet, it is marked `[NEW]` and defined in §4. Ids use the
VERIFIED reserved ranges (units ≥9100, time specials ≥900, special locations
≥500, missions ≥900000) where relevant; planet-list/tutorial/report/system-message
ids have **no** reserved range yet — proposed in §6.

```gherkin
Feature: Leaving a planet
  Reference: PlanetBo.doLeavePlanet (PlanetBo.java:137). Covers B1-B6.

  Background:
    Given the standard test universe

  Scenario: Leaving a non-home, unit-free, special-location-free planet succeeds
    Given planet 1234 is owned by user 1
    When user 1 leaves planet 1234
    Then planet 1234 has no owner
    And user 1 received websocket event "planet_owned_change" where no item has id 1234
    # Covers B1, B6.

  Scenario: Leaving a special-location planet revokes the gated unlock
    Given planet 1234 has special location 500 and no owner
    And unit 9100 exists gated by requirement HAVE_SPECIAL_LOCATION with second value 500
    And planet 1234 is owned by user 1
    And user 1 has an unlocked relation for object UNIT reference 9100
    When user 1 leaves planet 1234
    Then planet 1234 has no owner
    And table unlocked_relation has no row for user 1 and object UNIT reference 9100
    And user 1 received websocket event "unit_unlocked_change" where no item has id 9100
    # Covers B1, cross-references the special-location revoke path
    # (missions-travel agent's domain — see special_location_unlock.feature).

  Scenario: Cannot leave the home planet
    When user 1 leaves planet 1002
    Then planet 1002 is owned by user 1
    # Covers B2. [NEW] needs a `Then` for "the leave attempt failed with
    # ERR_I18N_CAN_NOT_LEAVE_PLANET" to assert the 400, see §4.

  Scenario: Cannot leave a planet with stationed units
    Given planet 1234 is owned by user 1
    And user 1 has 5 units of id 10 on planet 1234
    When user 1 leaves planet 1234
    Then planet 1234 is owned by user 1
    # Covers B4.
```

```gherkin
Feature: Planet list bookmarks
  Reference: PlanetListBo.myAdd / .myDelete (PlanetListBo.java:57, 70). Covers B7, B9.

  Background:
    Given the standard test universe

  Scenario: Adding a planet to the list and renaming it
    When user 1 adds planet 1234 to their planet list named "Outpost" [NEW]
    Then table planet_list has a row where user_id=1 and planet_id=1234 and name=Outpost
    And user 1 received websocket event "planet_user_list_change" where some item has id 1234
    When user 1 adds planet 1234 to their planet list named "Renamed" [NEW]
    Then table planet_list has a row where user_id=1 and planet_id=1234 and name=Renamed
    # Second add is the merge/upsert path (B7) — proves it renames in place,
    # not a duplicate row (impossible anyway: composite PK).

  Scenario: Removing a bookmarked planet
    Given user 1 adds planet 1234 to their planet list named "Outpost" [NEW as Given]
    When user 1 removes planet 1234 from their planet list [NEW]
    Then table planet_list has no row where user_id=1 and planet_id=1234
    And user 1 received websocket event "planet_user_list_change" where no item has id 1234
    # Covers B9.
```

```gherkin
Feature: Tutorial visited entries
  Reference: TutorialSectionBo.addVisitedEntry (TutorialSectionBo.java:115). Covers B15, B16.

  Background:
    Given the standard test universe
    And tutorial entry 900 exists [NEW — see §6 open question on missing id range]

  Scenario: Visiting an entry records it once
    When user 1 visits tutorial entry 900 [NEW]
    Then table visited_tutorial_entries has 1 row where user_id=1 and entry_id=900 [NEW: row-count variant]
    And user 1 received websocket event "visited_tutorial_entry_change" where some item has id 900

  Scenario: Re-visiting the same entry — divergence probe
    Given user 1 visits tutorial entry 900 [NEW as Given]
    When user 1 visits tutorial entry 900 again [NEW]
    Then table visited_tutorial_entries has 2 rows where user_id=1 and entry_id=900 [NEW]
    # JAVA_SPEC expected green (Java has no dedup guard, §2 B16).
    # RUST_SPEC expected RED today: Rust's add_visited_entry (tutorial_bo.rs:107-120)
    # checks COUNT(*) first and no-ops on a repeat — it will assert 1 row, not 2.
    # This scenario is this inventory's equivalent of the special-location
    # canonical example: intentionally red on Rust until Kevin decides whether
    # Java's no-dedup behavior is the intended spec (see §6).
```

```gherkin
Feature: System message read receipts
  Reference: SystemMessageBo.markAsRead (SystemMessageBo.java:86). Covers B21, B22.

  Background:
    Given the standard test universe
    And system message 900 exists [NEW]

  Scenario: Marking a message read twice — divergence probe
    When user 1 marks system message 900 as read [NEW]
    And user 1 marks system message 900 as read [NEW]
    Then table user_read_system_messages has 2 rows where user_id=1 and message_id=900 [NEW]
    # Same shape as the tutorial divergence probe above — Java always inserts,
    # Rust's system_message_bo.rs:41-58 dedups via a COUNT(*) guard.
```

```gherkin
Feature: Mission report read state
  Reference: MissionReportBo.markAsRead / markAsReadBeforeDate. Covers B18, B20.

  Background:
    Given the standard test universe
    And mission report 900000 exists for user 1 unread [NEW — reuses the missions≥900000 range]

  Scenario: Marking a report read updates the unread count
    When user 1 marks reports [900000] as read [NEW]
    Then table mission_reports has a row where id=900000 and user_read_date is not null [NEW]
    And user 1 received websocket event "mission_report_count_change"

  Scenario: Marking another user's report id is silently ignored
    Given mission report 900001 exists for user 2 unread [NEW]
    When user 1 marks reports [900001] as read [NEW]
    Then table mission_reports has a row where id=900001 and user_read_date is null [NEW]
    # Covers B18's "silently drops ids owned by someone else" branch.
```

## 4. Proposed new steps

QUARANTINED — none of these exist in `docs/BDD-PARITY-PLAN.md` §6 yet.

| Step text | Why needed | Implementation notes |
|---|---|---|
| `Given tutorial entry {eid} exists` | No tutorial content in the baseline dev DB (`tutorial_sections_entries` is empty — `MAX(id)` NULL). | Needs the whole FK chain: a `tutorial_sections_available_html_symbols` row, a `translations` row for `text_id`, then the entry itself with a fixed id. Copy the shape from `TutorialSectionBo.addUpdateEntry` (TutorialSectionBo.java:131-139). Propose reserving tutorial entry ids ≥900 to match the time-special convention. |
| `Given system message {mid} exists` | `system_messages` table is empty in baseline. | `INSERT INTO system_messages (id, content, creation_date) VALUES (?, 'test', NOW())`. Propose reserving ids ≥900. |
| `Given mission report {rid} exists for user {u} unread` / `… read` | `mission_reports` table is empty in baseline; needed to test mark-as-read without running a full mission. | Direct `INSERT INTO mission_reports (id, json_body, user_id, report_date, is_enemy, user_read_date)`; reuse the missions ≥900000 id range since these ids share the "test-authored, no real mission behind them" flavor — flag to Kevin whether report ids should get their own range instead. |
| `Given user {u} adds planet {pid} to their planet list named "{name}"` (as a Given, i.e. pre-seed via the endpoint, not raw SQL) | `planet_list` seeding is simplest by calling the real endpoint since there's no interesting precondition to bypass — but the §5.1 Given-phase runs before ws-capture starts, so calling the real `PlanetListBo.add` here is fine and keeps parity between the Given-seeded state and what the endpoint actually writes. | Implement as a thin wrapper over the `When` step of the same name (§below) that does not register ws capture. |
| `When user {u} adds planet {pid} to their planet list named "{name}"` | Exercises `PlanetListRestService.add` / `PlanetListBo.myAdd`. | POST `game/planet-list` with `{planetId, name}`. |
| `When user {u} removes planet {pid} from their planet list` | Exercises delete. | DELETE `game/planet-list/{pid}`. |
| `When user {u} visits tutorial entry {eid}` (and a distinguishable `… again` alias, or just call twice) | Exercises `TutorialRestService.addVisitedEntry`; needed twice in the divergence-probe scenario. | POST `game/tutorial/visited-entries` with a bare JSON `entryId` body. |
| `When user {u} marks reports [{ids}] as read` | Exercises `ReportRestService.markAsRead`. | POST `game/report/mark-as-read` with a JSON array body. |
| `When user {u} marks reports before "{date}" as read` | Exercises the date-cutoff variant. | POST `game/report/mark-as-read-before-date/{date}` (ISO-8601 in the path). |
| `When user {u} marks system message {mid} as read` | Exercises `SystemMessageRestService.markAsRead`. | POST `game/system-message/mark-as-read` with a JSON array body (even for one id). |
| `When user {u} subscribes to faction {fid}` | Exercises `UserStorageBo.subscribe` — currently no When step touches user creation at all; every existing Given assumes users 1/2 already exist. Needed for faction/spawn-galaxy/universe-full scenarios (B11-B14). | GET `game/user/subscribe?factionId={fid}` for the JWT-authenticated user. Needs a **new, not-yet-subscribed** test user id — propose reserving user ids ≥9000 (mirrors the units≥9100 convention) since 1/2 are permanently seeded. |
| `Then table planet_list has a row where user_id={u} and planet_id={pid} and name={name}` / `has no row where …` | `planet_list` is not in the §6.4 whitelist (`unlocked_relation, obtained_units, obtained_upgrades, missions, planets, active_time_specials`). | Extend the generic `table {t} has a row …` whitelist to include `planet_list`, `visited_tutorial_entries`, `user_read_system_messages`, `mission_reports`, `system_messages`, `user_storage`. Composite-PK tables (`planet_list`) need the generic step to support multiple `and`-clauses, which the plan's syntax already implies. |
| `Then table {t} has {n} rows where {col}={v} and …` | The row-COUNT variant, not just existence — **required** to catch the visited-entry / system-message-read no-dedup divergence (B16, B22), since a plain "has a row" assertion can't distinguish 1 row from 2. This is the single most important new step in this document. | `SELECT COUNT(*) FROM {t} WHERE …` against the same whitelist as above. |
| `Then table mission_reports has a row where id={rid} and user_read_date is not null` (and `is null`) | Needed to assert the mark-as-read effect; the existing whitelist doesn't cover NULL-vs-not-null predicates, only `col=value`. | Extend the generic step's predicate grammar to accept `is null` / `is not null` alongside `=`. |
| `Then the last request failed with "{ERR_CODE}"` | Several behaviors here (B2-B5, B8, B11, B14, B17, B24, B28) are pure-rejection branches with no DB/ws footprint — the only observable difference between "rejected correctly" and "silently succeeded" is the HTTP response. Nothing in §6 asserts HTTP status/body today. | The `When` step implementation (`support/rest.rs`) must stash the last response's status + parsed error body (`GameBackendErrorPojo.message`/`exceptionType`) on `BddWorld`; this `Then` reads it back. Also needs a decision on how strictly to match Rust's error shape (`OwgeError` variants) against Java's `GameBackendErrorPojo` — likely status-code-only matching plus a looser message substring check, since exact wording parity is not the point of this harness. |
| `Then user {u}'s HTTP response set an audit cookie` (or: drop it) | `game/user/exists` and `game/websocket-sync` have a cookie side effect (§2) that's invisible to every existing assertion layer. | Low priority — likely **not worth building**; the audit cookie is dev-analytics plumbing, not game state. Recommend explicitly marking it out-of-scope rather than building a step for it (see §6). |

## 5. Rust port status

All mutating endpoints in scope are ported; routes registered in
`rust-backend/owge-rest/src/routes/game/mod.rs`:

| Java endpoint | Rust route | Rust fn | Notes |
|---|---|---|---|
| `POST game/planet/leave` | `POST /game/planet/leave` (mod.rs:62) | `planet_leave` (mod.rs:420) → `PlanetBo::leave_planet` (`planet_bo.rs:150`) | Faithful: `can_leave_planet` (planet_bo.rs:96-137) mirrors all four `canLeavePlanet` checks including the exact `hasUnitsInPlanet` query shape (`mission_id IS NULL AND source_planet=?`). Special-location trigger wrapped in its own savepoint tx. |
| `POST game/planet-list` | `POST /game/planet-list` (mod.rs:39) | `planet_list_add` (mod.rs:218) → `PlanetListBo::add` (`planet_list_bo.rs:92`) | Faithful upsert semantics (exists-check then INSERT or UPDATE name) — matches Java's JPA-merge behavior. |
| `DELETE game/planet-list/{planetId}` | `DELETE /game/planet-list/{planetId}` (mod.rs:41) | `planet_list_delete` → `PlanetListBo::delete` (`planet_list_bo.rs:124`) | Unconditional `DELETE … WHERE`; no error on a no-op delete, unlike Java's `EmptyResultDataAccessException` path (B10) — **potential divergence**, see §6. |
| `GET game/user/exists` | `GET /game/user/exists` (mod.rs:32) | `user_exists` (mod.rs:93) → `UserStorageBo::exists` | **No cookie side effect** — Rust's handler never touches response cookies (§2, §6). |
| `GET game/user/subscribe` | `GET /game/user/subscribe` (mod.rs:61) | `user_subscribe` (mod.rs:395) → `UserStorageBo::subscribe` (`user_storage_bo.rs:44`) | Faithful, including the "subscribe itself emits nothing besides the two requirement triggers" behavior — confirmed not a divergence. |
| `POST game/tutorial/visited-entries` | `POST /game/tutorial/visited-entries` (mod.rs:48) | `tutorial_add_visited` (mod.rs:257) → `TutorialBo::add_visited_entry` (`tutorial_bo.rs:102`) | **Divergence**: Rust dedups via a `SELECT COUNT(*)` guard (tutorial_bo.rs:107-114); Java always inserts (B16). |
| `POST game/report/mark-as-read` | `POST /game/report/mark-as-read` (mod.rs:55) | `report_mark_as_read` (mod.rs:328) → `MissionReportBo::mark_as_read` (`mission_report_bo.rs:177`) | Faithful `UPDATE … WHERE user_id=? AND id IN (...)`; explicit empty-list early-return not present in the Java source read here (B19 — worth checking Java's actual empty-list behavior). Emits `mission_report_count_change` via `realtime_emitter::emit_mission_report_count_change`, matching `emitCountChange`. |
| `POST game/report/mark-as-read-before-date/{date}` | mod.rs:57 | `report_mark_as_read_before_date` (mod.rs:345) → `mark_as_read_before_date` (`mission_report_bo.rs:204`) | Faithful, same `userReadDate IS NULL` idempotency guard as Java. |
| `POST game/system-message/mark-as-read` | mod.rs:45 | `system_message_mark_read` (mod.rs:246) → `SystemMessageBo::mark_as_read` (`system_message_bo.rs:36`) | **Divergence**: same dedup-guard pattern as tutorial (B22). |
| `GET/PUT game/twitch-state` | mod.rs:52 | `twitch_state_get`/`twitch_state_put` (mod.rs:291, 300) | PUT emits `twitch_state_change` via `realtime_emitter::emit_twitch_state_change` (mod.rs:319) — the adjacent doc comment ("emission is deferred to M4", mod.rs:298-299) looks **stale**; the emission is actually present. Flagged as a documentation hygiene issue, not a behavior gap — see §6. |
| `POST game/track-browser/{warn,error}` | mod.rs:49-50 | `track_browser_warn`/`_error` → `TrackBrowserBo::track` | Faithful, trivial. |
| `POST game/adminLogin` | mod.rs:30 | `admin_login` (mod.rs:69) → `AdminUserBo::login` | Present; not deep-audited here (out of primary scope — admin surface belongs to a different inventory). |
| `GET game/deliver-backdoor/ping-user` | **not ported** | — | Listed in `rust-backend/docs/UNPORTED-ENDPOINTS.md`. Confirmed by route-table grep (`owge-rest/src/routes/game/mod.rs` has no `deliver-backdoor` route). |
| `GET game/websocket-sync` | mod.rs:33 | `websocket_sync` (mod.rs:112) → `websocket::find_wanted_data` | Present; the Java "best-effort LOGIN audit, swallow if user not yet subscribed" nuance not independently verified against Rust here — low priority (audit-log side channel, not game state). |

`rust-backend/docs/UNPORTED-ENDPOINTS.md` additionally lists (out of this
domain, noted for completeness): `open/sponsor`, `open/websocket-sync/rule_change`,
`open/websocket-sync/speed_group_change`, three `admin/*` endpoints, and
`admin/users/{id}/suspicions`.

## 6. Open questions / suspected divergences

1. **Confirmed, high-value divergence — no-dedup vs dedup on repeat
   read/visit actions.** Both `TutorialSectionBo.addVisitedEntry`
   (TutorialSectionBo.java:115-124) and `SystemMessageBo.markAsRead`
   (SystemMessageBo.java:85-94) insert a fresh row on **every** call with no
   existence check, and neither `visited_tutorial_entries` nor
   `user_read_system_messages` has a unique constraint on `(user_id,
   entry_id/message_id)` (`business/database/02_schema.sql:1145-1149,
   1184-1188`) — confirmed by reading the schema directly, not inferred.
   The Rust ports of both (`tutorial_bo.rs:107-120`, `system_message_bo.rs:41-58`)
   guard with a `SELECT COUNT(*)` and skip the insert on a repeat. Net effect:
   a user who double-clicks "mark all read" or replays a tutorial-entry event
   accumulates duplicate rows in Java but not in Rust. This is exactly the
   shape of bug this harness exists to catch (row-count divergence, not
   visible in either backend's DTO payload) — recommend it becomes one of the
   first Phase-3 scenarios (the "divergence probe" scenarios drafted in §3
   are ready to run as soon as the row-count `Then` step (§4) exists). Open
   question for Kevin: **which behavior is the intended spec?** Unbounded
   duplicate-row growth in Java looks like an unintentional bug (no product
   reason to store the same "user read message N" fact twice), but per §9
   pitfall #11 of the plan, Java is the default spec until told otherwise —
   don't silently "fix" the scenario to match Rust.

2. **`PlanetListBo.myDelete` on a non-existent bookmark.** Java's
   `repository.deleteById` throws `EmptyResultDataAccessException` on a
   missing composite-id row (unverified exact HTTP status — falls through to
   the base `kevinsuite` `RestExceptionHandler`, not
   `SgtGameRestExceptionHandler`'s explicit handlers, since
   `EmptyResultDataAccessException` isn't in its `@ExceptionHandler` list).
   Rust's `PlanetListBo::delete` (`planet_list_bo.rs:124-135`) issues an
   unconditional `DELETE … WHERE user_id=? AND planet_id=?` with no
   existence check — a no-op delete returns `Ok(())`/200, not an error. If
   Java genuinely 4xx/5xxs here, this is a real divergence on the "delete
   twice" / "delete something you never added" path. Needs a live HTTP probe
   against the Java container to confirm the actual status code before
   writing a `Then` for it.

3. **`SgtFactionNotFoundException` / `SgtBackendUniverseIsFull` HTTP status
   unverified.** Both extend `CommonException` but neither is one of the four
   types `SgtGameRestExceptionHandler` explicitly maps
   (`SgtGameRestExceptionHandler.java:27-46`: `SgtBackendInvalidInputException`
   → 400, `NotFoundException` → 404, `AccessDeniedException` → 403,
   `ProgrammingException` → 500). They fall to the base `RestExceptionHandler`
   from the `kevinsuite` dependency (source not in this repo, per
   `CLAUDE.md`'s "Critical external dependency: kevinsuite-java" — it's
   fetched from JitPack, not vendored). Needs either reading that jar's
   decompiled source or a live probe (`POST game/user/subscribe?factionId=999999`
   against a running Java container) to pin down the actual status before
   any `Then the last request failed with …` step can assert it reliably.

4. **`MissionReportBo.markAsRead` with an empty `reportsIds` list.** JPQL
   `UPDATE … WHERE rp.id IN :reportsIds` with an empty bound collection is
   known to throw at Hibernate parameter-binding time in some JPA provider
   versions (empty-`IN`-clause is invalid ANSI SQL and providers differ on
   whether they special-case it). Not verified against this specific
   Hibernate version in this repo. The Rust port explicitly early-returns on
   an empty list (`mission_report_bo.rs:182-184`) — if that early-return was
   added *because* someone observed the Java 500, that's a deliberate
   compensating fix, not a divergence; if it was speculative, it might itself
   be untested against the real Java behavior. Worth a quick empirical check.

5. **`game/user/exists` and `game/websocket-sync` audit-cookie / best-effort
   LOGIN audit side effects have no Rust equivalent and no test-harness
   visibility.** Recommend explicitly scoping these **out** of the BDD suite
   (they're dev-analytics plumbing, not game state a player or the mission
   system observes) rather than building HTTP-header assertion machinery for
   them — flagging here so the decision is recorded rather than silently
   dropped.

6. **Stale doc comment in the Rust port, not a behavior bug.**
   `owge-rest/src/routes/game/mod.rs:297-299` says the `twitch_state_change`
   websocket emission "is deferred to M4", but the code three lines below
   (mod.rs:319-321) actually calls
   `realtime_emitter::emit_twitch_state_change`. Either the comment predates
   the emission being implemented and was never updated, or there's a subtler
   gap (e.g. the emission fires but doesn't carry the websocket-sync
   "watermark for all users" semantics Java's `null`-target broadcast implies
   — PlanetRestService.java's sibling emitters all target a single user or a
   filtered list; `TwitchStateRestService` is the *only* true broadcast in
   this whole domain). Worth a quick diff-read of `emit_twitch_state_change`
   against `SocketIoService.sendMessage(null, …)`'s broadcast semantics before
   trusting the comment is simply outdated.

7. **No reserved id range exists yet for tutorial entries, system messages,
   or mission reports** (baseline dev DB has zero rows in all three tables —
   confirmed via `SELECT MAX(id)` returning `NULL` for each). §4's proposed
   Given steps assume ids ≥900 (tutorial/system-message, matching the
   time-special convention) and reuse the missions ≥900000 range for report
   ids, but this needs Kevin's sign-off before it becomes a second "VERIFIED"
   block in `BDD-PARITY-PLAN.md` §6.1's style. Likewise, exercising
   `UserStorageBo.subscribe` (B11-B14) needs a **never-subscribed** user id —
   proposed ≥9000, but users 1/2 are the only ones the plan currently
   guarantees exist/don't-exist state for.
