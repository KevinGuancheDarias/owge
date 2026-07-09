# alliance — Java reference behavior inventory

Sources read in full: `game-rest/.../rest/game/AllianceRestService.java`,
`business/.../business/AllianceBo.java`, `business/.../business/AllianceJoinRequestBo.java`,
`business/.../repository/AllianceRepository.java`, `AllianceJoinRequestRepository.java`,
`business/.../entity/Alliance.java`, `AllianceJoinRequest.java`, `UserStorage.java` (alliance FK),
`business/.../dto/AllianceDto.java`, `AllianceJoinRequestDto.java`,
`business/database/02_schema.sql` (`alliances`, `alliance_join_request`), the live dev schema
(`SHOW CREATE TABLE`), `AllianceBoTest.java` / `AllianceJoinRequestBoTest.java` (behavior
confirmation), and the Rust port (`rust-backend/owge-rest/src/routes/game/alliance.rs`,
`rust-backend/owge-business/src/bo/alliance_bo.rs`, `user_storage_bo.rs` delete-cascade).

## 1. Endpoints

| HTTP | Path | Controller | Bo entry point |
|---|---|---|---|
| GET | `game/alliance` | `AllianceRestService.findAll` — L50-53 | `allianceRepository.findAll()` (no Bo) |
| GET | `game/alliance/{id}/members` | `AllianceRestService.members` — L61-68 | `allianceRepository.findMembers(id)` (no Bo) |
| POST | `game/alliance` (create) | `AllianceRestService.save` — L76-82 | `AllianceBo.save` — `AllianceBo.java` L83-108 |
| PUT | `game/alliance` (update) | `AllianceRestService.save` — L76-82 | `AllianceBo.save` — L83-108 |
| DELETE | `game/alliance` | `AllianceRestService.delete` — L84-87 | `AllianceBo.deleteByUser` — L203-212 (→ `AllianceBo.delete` L67-71) |
| GET | `game/alliance/listRequest` | `AllianceRestService.listRequest` — L89-100 | inline in controller (no Bo call) + `allianceJoinRequestRepository.findByAlliance` |
| GET | `game/alliance/my-requests` | `AllianceRestService.myRequests` — L108-113 | `allianceJoinRequestRepository.findByUserId` (bypasses `AllianceJoinRequestBo.findByUserId`, same query) |
| DELETE | `game/alliance/my-requests/{id}` | `AllianceRestService.myRequestsDelete` — L115-118 | `allianceJoinRequestRepository.deleteById(id)` (no Bo, **no ownership check** — see B7) |
| POST | `game/alliance/requestJoin` | `AllianceRestService.join` — L120-125 | `AllianceBo.requestJoin` — L133-147 |
| POST | `game/alliance/acceptJoinRequest` | `AllianceRestService.acceptRequest` — L127-131 | `AllianceBo.acceptJoin` — L149-165 |
| POST | `game/alliance/rejectJoinRequest` | `AllianceRestService.rejectRequest` — L133-137 | `AllianceBo.rejectJoin` — L173-177 |
| POST | `game/alliance/leave` | `AllianceRestService.leave` — L139-142 | `AllianceBo.leave` — L185-193 |

11 route mappings (`save` handles both POST and PUT under one `@RequestMapping`), matching the
domain brief's "~11 endpoints".

Not user-reachable through this controller but part of the alliance surface: `AllianceBo.areEnemies`
(`AllianceBo.java` L119-122, used by `AttackMissionManagerBo.java:108` and
`UnitInterceptionFinderBo.java:39` — combat's ally-can't-attack-ally / interception check; owned by
the combat agent, cross-referenced only) and the `UserDeleteListener` cascade
(`AllianceJoinRequestBo.doDeleteUser` L88-93, order 1; `AllianceBo.doDeleteUser` L254-261, order 2).

## 2. Behavior catalog

### `findAll` (GET `game/alliance`)
- **B1.** Returns every row of `alliances` via `allianceRepository.findAll()`
  (`AllianceRestService.java:52`), mapped to `AllianceDto`. No auth-scoped filtering, no
  preconditions, no exceptions, no DB writes, no websocket.

### `members` (GET `game/alliance/{id}/members`)
- **B2.** `allianceRepository.findMembers(id)` — JPQL `SELECT a.users FROM Alliance a WHERE a.id
  = ?1` (`AllianceRepository.java:21-22`), i.e. every `user_storage` row with
  `alliance_id = {id}`. Controller then blanks two fields per returned `UserStorageDto`:
  `setImprovements(null)` and `setEmail(null)` (`AllianceRestService.java:64-67`) — members
  listing never leaks email/improvements. Unknown `{id}` → empty list, **not** a 404 (JPQL join
  against a non-existent alliance id just yields nothing).

### `save` (POST/PUT `game/alliance`)
- **B3 (create, id null).** Controller `checkPost` (`BaseRestServiceTrait.java:17-21`) is a no-op
  here since a create body has no id. `AllianceBo.save` (`AllianceBo.java:84-99`):
  1. `DISABLED_FEATURE_ALLIANCE` config (default `"FALSE"`) — if `TRUE`, throws
     `SgtBackendInvalidInputException("You can't not create an alliance, while the idea is nice,
     it's not possible")` (L85-88). **Live dev DB currently has this row set to `TRUE`**
     (verified via `SELECT name,value FROM configuration WHERE name='DISABLED_FEATURE_ALLIANCE'`)
     — any create-alliance scenario's `Given` MUST flip it to `FALSE` first.
  2. Loads the invoker via `findByIdOrDie` (L91); if `creator.getAlliance() != null` throws
     `"You already have an alliance, leave it first"` (L92-94).
  3. Else: `alliance.setOwner(creator)`, `INSERT INTO alliances` (id is `AUTO_INCREMENT`), sets
     `retVal.getOwner().setAlliance(retVal)` in memory, `auditBo.doAudit(JOIN_ALLIANCE)` (no
     `invokerId`/`relatedId` args — self-audit), then `UPDATE user_storage SET alliance_id=<new>`
     for the owner (L95-99). **`alliances.owner_id` has a UNIQUE constraint** (live schema
     `SHOW CREATE TABLE`) — DB-enforces "one owned alliance per user" independent of the
     application check.
- **B4 (update, id present).** `findById(alliance.getId())` (generic not-found if missing, via
  `WithNameBo`/`SpringRepositoryUtil` conventions used elsewhere — `AllianceBo.java:101`);
  `checkInvokerIsOwner` throws `"You are NOT the owner of that alliance, try hacking the owner
  account"` if the invoker isn't `storedAlliance.owner` (L102, L232-237). Only `name` and
  `description` are copied from the incoming DTO (L103-104) — **`image` and `owner` are never
  updated on this path**, even if the request body sets them.

### `delete` (DELETE `game/alliance`) → `deleteByUser`
- **B5.** Reloads the full user (`findByIdOrDie`, L205). If `user.getAlliance() == null` throws
  `"You don't have any alliance"` (L207-209). Else `checkInvokerIsOwner(alliance, user)` — throws
  the same "try hacking the owner account" message if the caller is a member but not the owner
  (L210) — **members cannot delete the alliance**, only the owner, and there is no separate
  "kick member" endpoint anywhere in this surface. On success calls `AllianceBo.delete`
  (L211 → L67-71):
  - `defineAllianceByAllianceId(alliance.getId())` → bulk
    `UPDATE user_storage SET alliance_id = NULL WHERE alliance_id = {old}` for **every** member,
    not just the owner (`UserStorageRepository.defineAllianceByAllianceId`, repo L36).
  - `repository.delete(alliance)` → `DELETE FROM alliances WHERE id = {id}`.
  - **This path does NOT delete `alliance_join_request` rows for the alliance.** The
    `alliance_join_request.alliance_id` FK (`CONSTRAINT alliance_join_request_ibfk_1 ... REFERENCES
    alliances(id)`, live schema) has no `ON DELETE CASCADE`. If any pending join requests exist
    against this alliance when the owner deletes it, the `DELETE FROM alliances` statement will
    fail with a DB foreign-key violation (surfaces as an unhandled
    `DataIntegrityViolationException`/500, not a friendly `SgtBackendInvalidInputException`).
    Contrast with the user-delete cascade (`AllianceBo.doDeleteUser`, L254-261) which explicitly
    calls `allianceJoinRequestRepository.deleteByAlliance(alliance)` before deleting the alliance
    row. **This is a real Java-reference bug/edge case** — see §6.

### `listRequest` (GET `game/alliance/listRequest`)
- **B6.** Loads the caller with `findLoggedInWithDetails()` (fresh DB fetch,
  `UserSessionService.java:19-34`). If `user.getAlliance() == null` throws
  `"You don't have any alliance"` (`AllianceRestService.java:94`); else if
  `!user.getAlliance().getOwner().getId().equals(user.getId())` throws
  `"You are not the owner of the alliance"` (L95-96) — **note this is an inline check with
  different wording than `AllianceBo.checkInvokerIsOwner`'s "try hacking the owner account"**,
  even though the semantics are identical (invoker must be alliance owner). Returns
  `allianceJoinRequestRepository.findByAlliance(alliance)` — all pending requests for that
  alliance, across all requesting users.

### `myRequests` (GET `game/alliance/my-requests`)
- **B7.** `allianceJoinRequestRepository.findByUserId(user.getId())` — every join request the
  caller has ever made, regardless of alliance or ownership. No preconditions.

### `myRequestsDelete` (DELETE `game/alliance/my-requests/{id}`)
- **B8.** `allianceJoinRequestRepository.deleteById(id)` (`AllianceRestService.java:117`) — **no
  ownership/authorship check whatsoever.** Any authenticated user can delete *any* pending join
  request by guessing/enumerating its id, not just their own — a genuine authorization gap in the
  Java reference (contrast with `acceptJoin`/`rejectJoin`, both of which gate on
  "invoker owns the request's *alliance*"; this endpoint gates on nothing). Deleting a
  non-existent id throws Spring Data's `EmptyResultDataAccessException` (unhandled → 500), not a
  friendly validation error.

### `join` → `requestJoin` (POST `game/alliance/requestJoin`)
- **B9.** Controller `checkMapEntry(body, "allianceId")` throws `"No value for key allianceId"`
  if missing (`BaseRestServiceTrait.java:33-36`). `AllianceBo.requestJoin`
  (`AllianceBo.java:133-147`):
  1. `findByIdOrDie(allianceId)` — not-found if the alliance doesn't exist.
  2. `findByIdOrDie(invokerId)` for the user.
  3. If `user.getAlliance() != null` throws `"You are already in an alliance, nice try!"`
     (L136-138).
  4. **Builds the `AllianceJoinRequest` via its Lombok `@Builder` and saves it DIRECTLY through
     `allianceJoinRequestRepository.save(...)` (L142-146) — it does NOT call
     `AllianceJoinRequestBo.save(...)`.** `AllianceJoinRequestBo.save` (L62-73) has its own
     duplicate-request guard (`existsByUserAndAlliance` → throws `"You already have a join
     request for this alliance"`) — **that guard is dead code on this REST path**; confirmed by
     grep (only test code calls `AllianceJoinRequestBo.save` directly) and by
     `AllianceBoTest.requestJoin_should_work` asserting the repository is called, not the Bo.
     **A user CAN spam unlimited duplicate join requests to the same alliance.**
     `request_date` still gets populated because `AllianceJoinRequest.requestDate` has a
     `@Builder.Default = LocalDateTime.now(ZoneOffset.UTC)` (`AllianceJoinRequest.java:47-48`) —
     the NOT NULL column is satisfied incidentally by the entity default, not by Bo logic.

### `acceptRequest` → `acceptJoin` (POST `game/alliance/acceptJoinRequest`)
- **B10.** `checkMapEntry(body,"joinRequestId")`. `AllianceBo.acceptJoin` (L149-165):
  1. `findByIdOrDie(joinRequestId)` — not-found if bad id.
  2. `checkInvokerIsOwner(request.getAlliance(), invoker)` — throws "try hacking the owner
     account" if the caller isn't the target alliance's owner.
  3. `checkIsLimitReached(request)` (L263-275) — throws `"I18N_ERR_ALLIANCE_IS_FULL"` if the
     alliance is at capacity. Capacity = `min(ALLIANCE_MAX_SIZE config (default 15),
     max(2, userCount * ALLIANCE_MAX_SIZE_PERCENTAGE config% (default 7)))`, compared against
     `userStorageRepository.countByAlliance(alliance)` (current member count, **not** pending
     request count).
  4. **Branch A — target user still has no alliance** (`request.getUser().getAlliance() ==
     null`): sets the user's alliance, for every *existing* member emits
     `auditBo.nonRequestAudit(USER_INTERACTION, "JOIN_ALLIANCE", newUser, existingMemberId)`
     (one audit row per existing member, L157-158), `auditBo.doAudit(ACCEPT_JOIN_ALLIANCE, null,
     newUserId)`, `UPDATE user_storage SET alliance_id=...` for the joiner, then
     `allianceJoinRequestRepository.deleteByUser(request.getUser())` — **deletes ALL of that
     user's pending join requests, not just this one** (they may have requested several
     alliances; accepting one cancels the rest).
  5. **Branch B — target user already has an alliance** (joined elsewhere between requesting and
     being accepted — the exact race the domain brief calls out): silently
     `allianceJoinRequestRepository.delete(request)` (just this one row), **no audit, no
     `user_storage` write, no error surfaced** — the accepting owner gets a normal 200 OK with no
     indication the accept was a no-op.

### `rejectRequest` → `rejectJoin` (POST `game/alliance/rejectJoinRequest`)
- **B11.** `checkMapEntry(body,"joinRequestId")`. `findByIdOrDie`; `checkInvokerIsOwner` (same
  "try hacking" message); `allianceJoinRequestRepository.delete(request)`. No capacity check
  (rejecting never needs room).

### `leave` (POST `game/alliance/leave`)
- **B12.** `AllianceBo.leave` (L185-193): `userRef = userStorageRepository.getReferenceById(userId)`
  — a **lazy proxy**, no upfront existence check (per the persistence-gotcha pattern in
  `CLAUDE.md` — dereferencing later throws `EntityNotFoundException` if the row was deleted
  earlier in the same transaction, not relevant to a standalone call but worth the harness
  knowing). `isOwnerOfAnAlliance(userId)` (`repository.findOneByOwnerId(userId).isPresent()`,
  L245-247) — if true, throws `"You can't leave your own alliance"` (L188-190): **the owner must
  use `delete`, never `leave`.** This check is purely "do I own *some* alliance", independent of
  whether `userRef.alliance` is currently set. Otherwise unconditionally
  `userRef.setAlliance(null); save(userRef)` — **idempotent**: calling `leave` while already
  without an alliance succeeds silently (no exception for "you're not in an alliance").

### Cascades on user deletion (`UserDeleteListener`, not REST-reachable directly)
- **B13.** Order 1 — `AllianceJoinRequestBo.doDeleteUser` (L88-93): if the deleted user does
  **not** own their own alliance, `repository.deleteByUser(user)` removes all their pending join
  requests. If they DO own an alliance, this is skipped (order-2 step below handles it via the
  alliance-scoped cascade instead — avoids deleting the owner's own join requests twice, though
  an owner realistically has none).
- **B14.** Order 2 — `AllianceBo.doDeleteUser` (L254-261): `repository.findOneByOwnerId(user.getId())`
  — if the deleted user owned an alliance: `userStorageRepository.defineAllianceByAllianceId(alliance,
  null)` (unset every member), `allianceJoinRequestRepository.deleteByAlliance(alliance)` (**this
  cascade DOES clean up join requests before deleting** — unlike B5/`deleteByUser`'s REST path),
  `repository.delete(alliance)`. If the deleted user didn't own an alliance, this is a no-op
  (their own membership in someone else's alliance, if any, is just left dangling on the
  about-to-be-deleted `user_storage` row itself, which is fine since that row is being deleted
  too).

### Websocket
- **No websocket events are emitted anywhere on the alliance mutation paths** — confirmed by
  grepping every class that references both "alliance" and `EventEmitter`/`SocketIoService`
  (`UserStorageBo`, `AttackMissionManagerBo`, `ConquestMissionProcessor`,
  `AttackMissionProcessor`, `UserEventEmitterBo`): all of those hits are either combat's
  ally-filtering logic or `UserEventEmitterBo` embedding the user's *current* `AllianceDto` inside
  a general user-sync payload (`UserEventEmitterBo.java:67`, `userDto.setAlliance(...)`) that
  fires for unrelated reasons (e.g. login/points sync) — not triggered *by* any alliance endpoint.
  Practically: joining/leaving/accepting an alliance produces zero websocket traffic; the
  frontend must re-fetch (`findAll`/`members`/`listRequest`/`myRequests`) to observe the change.
  For BDD scenarios this means **no `Then user X received websocket event ...` assertions apply
  to this domain** — Layer 2 (table diff) is the only meaningful assertion layer here besides
  explicit `Then` DB checks.

## 3. Draft Gherkin scenarios

Reserved ids: `alliances`/`alliance_join_request` are **empty in the baseline** (`MAX(id) IS
NULL` on both, verified live) and have no reserved range in the BDD plan's VERIFIED notes (only
units/time specials/special locations/missions are reserved). Scenarios below pick alliance ids
≥ 900 and join-request ids ≥ 900 to stay consistent with the plan's existing "reserved scenario
range" convention (§6.1 VERIFIED note) — **flagged in §6 as needing Kevin's confirmation**, since
nothing collides today regardless. Users 1 and 2 exist in the baseline
(`rusttester`/`rusttester2`, both `alliance_id = NULL`). Every scenario below needs steps not yet
in the §6 catalog (marked `†`, defined in §4) because the catalog currently only covers the
special-location feature.

```gherkin
Feature: Alliances
  Reference: AllianceRestService / AllianceBo / AllianceJoinRequestBo.

  Background:
    Given the standard test universe
    And configuration "DISABLED_FEATURE_ALLIANCE" is "FALSE"          # † new — B3 precondition

  Scenario: Creating an alliance sets the creator as owner
    When user 1 creates an alliance with id 900 named "Test Alliance"  # † new (B3)
    Then alliance 900 has owner user 1                                 # † new
    And user 1 has alliance 900                                        # † new

  Scenario: Creating an alliance while already in one is rejected
    Given alliance 900 exists owned by user 2                          # † new
    And user 1 has alliance 900                                        # † new
    When user 1 creates an alliance with id 901 named "Second"         # † new (B3)
    Then the request is rejected with an error containing "already have an alliance"  # † new

  Scenario: Alliance creation is rejected while the feature is disabled
    Given configuration "DISABLED_FEATURE_ALLIANCE" is "TRUE"          # † new
    When user 1 creates an alliance with id 902 named "Nope"           # † new (B3)
    Then the request is rejected with an error containing "not possible"  # † new

  Scenario: Only the owner may edit the alliance
    Given alliance 900 exists owned by user 1
    And user 2 has alliance 900                                        # † new
    When user 2 edits alliance 900 setting name "Hijacked"             # † new (B4)
    Then the request is rejected with an error containing "try hacking the owner account"
    And alliance 900 has name "Test Alliance"                          # † new (unchanged)

  Scenario: Owner deletes the alliance and every member is released
    Given alliance 900 exists owned by user 1
    And user 2 has alliance 900
    When user 1 deletes their alliance                                 # † new (B5)
    Then alliance 900 does not exist                                   # † new
    And user 1 has no alliance                                         # † new
    And user 2 has no alliance                                         # † new

  Scenario: A non-owner member cannot delete the alliance
    Given alliance 900 exists owned by user 1
    And user 2 has alliance 900
    When user 2 deletes their alliance                                 # † new (B5)
    Then the request is rejected with an error containing "try hacking the owner account"
    And alliance 900 has owner user 1                                  # † new

  Scenario: Requesting to join an alliance while already in one is rejected
    Given alliance 900 exists owned by user 2
    And user 1 has alliance 900
    When user 1 sends a join request to alliance 900 with id 900       # † new (B9)
    Then the request is rejected with an error containing "already in an alliance"

  Scenario: Owner accepts a join request and the requester joins
    Given alliance 900 exists owned by user 1
    And user 2 has a pending join request 900 to alliance 900          # † new
    When user 1 accepts join request 900                               # † new (B10)
    Then user 2 has alliance 900                                       # † new
    And table alliance_join_request has no row with id 900             # generic escape hatch (§6.4 catalog)

  Scenario: Accepting is rejected when the alliance is full
    Given alliance 900 exists owned by user 1
    And configuration "ALLIANCE_MAX_SIZE" is "1"                       # † new
    And user 2 has alliance 900
    And user 3 exists                                                  # † new — needs a 3rd user (see §4)
    And user 3 has a pending join request 900 to alliance 900          # † new
    When user 1 accepts join request 900                               # † new (B10)
    Then the request is rejected with an error containing "I18N_ERR_ALLIANCE_IS_FULL"
    And user 3 has no alliance                                         # † new

  Scenario: Accepting a request for a user who joined another alliance meanwhile is a silent no-op
    Given alliance 900 exists owned by user 1
    And alliance 901 exists owned by user 3                            # † new
    And user 2 has a pending join request 900 to alliance 900
    And user 2 has alliance 901                                        # user 2 joined 901 after requesting 900
    When user 1 accepts join request 900                               # † new (B10)
    Then table alliance_join_request has no row with id 900
    And user 2 has alliance 901                                        # unchanged — proves the silent-noop branch

  Scenario: Rejecting a join request removes it without side effects
    Given alliance 900 exists owned by user 1
    And user 2 has a pending join request 900 to alliance 900
    When user 1 rejects join request 900                               # † new (B11)
    Then table alliance_join_request has no row with id 900
    And user 2 has no alliance

  Scenario: The alliance owner cannot leave (must delete instead)
    Given alliance 900 exists owned by user 1
    When user 1 leaves their alliance                                  # † new (B12)
    Then the request is rejected with an error containing "can't leave your own alliance"
    And user 1 has alliance 900

  Scenario: A member can leave freely
    Given alliance 900 exists owned by user 1
    And user 2 has alliance 900
    When user 2 leaves their alliance                                  # † new (B12)
    Then user 2 has no alliance
    And alliance 900 has owner user 1                                  # alliance itself untouched
```

Coverage: B1 (implicit, `findAll` is read-only and not separately scenario'd — low risk, mostly a
straight passthrough), B2 similarly read-only, B3/B4/B5/B9/B10/B11/B12 all covered above, B13/B14
(user-delete cascade) intentionally **not** drafted here — they belong to a `user_deletion.feature`
owned by whichever agent inventories account/user deletion, not duplicated here. B6/B7/B8
(`listRequest`/`myRequests`/`myRequestsDelete`) are read/delete-by-id passthroughs with the
authorization gap noted in §6 — worth one dedicated scenario once §4's steps land, omitted from
this draft to keep it to the state-mutating core.

## 4. Proposed new steps

All QUARANTINED — none of these exist in the BDD-PARITY-PLAN.md §6 catalog today; the catalog only
serves the special-location feature so far.

| Step text | Why needed | Implementation notes |
|---|---|---|
| `configuration "{name}" is "{value}"` | B3 needs `DISABLED_FEATURE_ALLIANCE=FALSE` (baseline has it `TRUE`, verified live); B10's capacity scenario needs `ALLIANCE_MAX_SIZE`/`ALLIANCE_MAX_SIZE_PERCENTAGE` overrides. Generic — useful far beyond this domain. | `INSERT INTO configuration (name, value) VALUES (?, ?) ON DUPLICATE KEY UPDATE value = VALUES(value)` (mirrors `ConfigurationBo.findOrSetDefault`'s upsert semantics). Register nothing on `context` — global state, not scenario-scoped, so the runner should probably reset known config rows as part of §5.2 baseline restore rather than leave scenarios responsible for cleanup. |
| `alliance {aid} exists owned by user {u}` | Every alliance scenario needs a pre-seeded alliance without going through `save` (which has its own preconditions/config toggle). | `INSERT INTO alliances (id, name, description, owner_id) VALUES ({aid}, 'Test Alliance', NULL, {u})`; also `UPDATE user_storage SET alliance_id={aid} WHERE id={u}` (Java's `save` always sets the owner's own `alliance_id` too — the owner is implicitly a member). Register `{u}` for ws capture (harmless, no ws events exist here, but keeps the convention uniform) and `{aid}` in the layer-2 filter. |
| `user {u} has alliance {aid}` (Given) | Seed a plain member (non-owner) into an alliance. | `UPDATE user_storage SET alliance_id={aid} WHERE id={u}`. |
| `user {u} has a pending join request {rid} to alliance {aid}` (Given) | Seed `alliance_join_request` rows directly, bypassing `requestJoin`'s own preconditions, for accept/reject scenarios. | `INSERT INTO alliance_join_request (id, alliance_id, user_id, request_date) VALUES ({rid}, {aid}, {u}, UTC_TIMESTAMP())`. |
| `user {n} exists` | §2.5/plan note: only users 1 and 2 exist in the baseline; several alliance scenarios (capacity limits, "joined another alliance meanwhile") genuinely need a 3rd distinct user. | **Needs a design decision, not just SQL**: `user_storage` requires non-null `faction`, `home_planet` (FK to an existing `planets` row), `energy`, `has_skipped_tutorial`, `points`, `can_alter_twitch_state`, `banned` (live `SHOW CREATE TABLE user_storage`). Cheapest option: reserve a fixed synthetic user id (propose ≥ 900, mirroring the other reserved ranges) and a fixed *dedicated* home-planet id for it (NOT 1234 — that's reserved for the special-location feature), copying column defaults from user 1's row. This needs Kevin to pick the reserved user-id range and a spare planet id before implementation — flagged in §6. |
| `alliance {aid} has owner user {u}` (Then) | Assert ownership after create/delete/leave flows. | `SELECT owner_id FROM alliances WHERE id={aid}`. |
| `alliance {aid} has name "{name}"` (Then) | Assert `save` update-path field scoping (name/description only, not image/owner). | `SELECT name FROM alliances WHERE id={aid}`. |
| `alliance {aid} does not exist` (Then) | Assert delete succeeded. | `SELECT COUNT(*) FROM alliances WHERE id={aid}` = 0. |
| `user {u} has no alliance` / `user {u} has alliance {aid}` (Then) | The core state assertion for join/leave/accept/reject/delete flows. | `SELECT alliance_id FROM user_storage WHERE id={u}`. |
| `user {u} creates an alliance with id {aid} named "{name}"` (When) | REST `POST game/alliance` with `{id: null, name, description: null}` — needs a fixed resulting id for deterministic assertions, but the endpoint auto-increments; either accept the auto id and have the step read it back (`SELECT id FROM alliances WHERE owner_id={u} ORDER BY id DESC LIMIT 1`) and bind it to `{aid}` as an alias, or (simpler, matching W3's fixed-id philosophy for missions) insert directly for the *seed* half of table-driven scenarios and reserve this step for **create-success scenarios only** where the resulting id doesn't need to be a specific reserved value. | Mint JWT for `{u}`; `POST /game_api/game/alliance` body `{"name": "{name}"}`; assert 2xx; capture the response `id`. |
| `user {u} edits alliance {aid} setting name "{name}"` (When) | REST `PUT game/alliance` update path (B4). | `PUT /game_api/game/alliance` body `{"id": {aid}, "name": "{name}", "description": ...}`. |
| `user {u} deletes their alliance` (When) | REST `DELETE game/alliance` (B5). | `DELETE /game_api/game/alliance`, no body. |
| `user {u} sends a join request to alliance {aid} with id {rid}` (When) | REST `POST requestJoin` (B9). Needs the `{rid}` alias because the endpoint returns the auto-generated id and scenarios want to reference it in a later `Then`/accept step within the same scenario — bind `{rid}` from the response, don't try to force a specific PK via REST. | `POST .../requestJoin` body `{"allianceId": {aid}}`; capture response `.id` bound to alias `{rid}` for later steps. |
| `user {u} accepts join request {rid}` / `user {u} rejects join request {rid}` (When) | REST `acceptJoinRequest`/`rejectJoinRequest` (B10/B11). | `POST .../acceptJoinRequest` / `.../rejectJoinRequest` body `{"joinRequestId": {rid}}`. |
| `user {u} leaves their alliance` (When) | REST `leave` (B12). | `POST .../leave`, no body. |
| `the request is rejected with an error containing "{fragment}"` (Then) | Generic negative-outcome assertion — the plan's existing Then catalog has no error-message step at all (special-location feature only asserts success paths). Needed for essentially every exception in this domain (B3/B4/B5/B9/B10/B11/B12 all have `SgtBackendInvalidInputException` branches). | Requires the When step to capture the REST response status+body on the `World` even on non-2xx (today's plan text says "Every When that creates a mission also asserts the POST returned 2xx — a 4xx/5xx is a scenario failure" — that convention needs an explicit opt-out/variant for negative-path scenarios, e.g. a `When` step suffix like "...(expecting failure)" or a separate assertion step that consumes the last captured error instead of asserting 2xx up front). This is a **cross-cutting proposal**, not alliance-specific — flag to whoever owns `src/steps/when_actions.rs` conventions. |
| `table alliance_join_request has no row with id {rid}` (Then) | Reuses the existing generic escape hatch pattern (§6.4) but that whitelist (`unlocked_relation, obtained_units, obtained_upgrades, missions, planets, active_time_specials`) does not include `alliance_join_request`, `alliances`, or `user_storage`. | Add `alliance_join_request`, `alliances`, and (very carefully — it's a huge table) a narrow, id-scoped `user_storage` accessor to the whitelist, OR prefer the named steps above (`user {u} has no alliance` etc.) and only extend the whitelist for `alliance_join_request`/`alliances`. |

## 5. Rust port status

**Fully ported**, and unusually well-documented for it — `rust-backend/owge-business/src/bo/alliance_bo.rs`
opens with a doc comment explicitly noting the dead-code discovery from §2 above (`request_join`
inserts directly, matching Java's bypass of the duplicate-check Bo method) and that auditing is
"dropped (not ported); the live port disables it". Endpoint-by-endpoint:

| Endpoint | Rust route | Rust Bo fn | Notes |
|---|---|---|---|
| `findAll` | `alliance.rs:44-50` (`find_all`) | `AllianceBo::find_all` (`alliance_bo.rs:58-65`) | Matches. |
| `members` | `alliance.rs:53-60` | `AllianceBo::members` (`alliance_bo.rs:87-98`) | Doc comment explicitly matches Java's email-blank/improvements-omit (`alliance_bo.rs:84-86`). |
| `save` (POST/PUT) | `alliance.rs:64-73` | `AllianceBo::save` (`alliance_bo.rs:103-178`) | **Divergence found**: Rust's create/update branch is `dto.id == 0` (`alliance_bo.rs:121`) vs Java's `alliance.getId() == null` (`AllianceBo.java:90`) — a body that explicitly sends `id: 0` behaves as *create* in both (0 isn't a valid alliance id, `smallint unsigned`, and Java's DTO `id` field is a boxed `Integer` so `0` ≠ `null` there — **if the frontend or a scenario ever sends `id:0` explicitly rather than omitting it, Java would attempt the *update* path and 404/not-found on alliance 0, while Rust would silently treat it as *create*.** Flagged in §6. |
| `delete` | `alliance.rs:76-83` | `AllianceBo::delete_by_user` (`alliance_bo.rs:183-210`) | **Rust does NOT replicate the Java bug from B5**: it explicitly runs `DELETE FROM alliance_join_request WHERE alliance_id = ?` (`alliance_bo.rs:200-203`) before `DELETE FROM alliances`, inside the same transaction. So on this exact input (pending join requests present at delete time) **Java throws a raw FK-constraint 500 and Rust succeeds cleanly** — a guaranteed PARITY-layer divergence once such a scenario exists. This needs a call from Kevin per pitfall §9.11 ("when Java and Rust are both wrong... don't fix the scenario to match Java blindly, flag it") — here it's the *opposite* shape: Rust is arguably more correct, Java has the bug. |
| `listRequest` | `alliance.rs:86-94` | `AllianceBo::list_request` (`alliance_bo.rs:214-226`) | Matches, including the ownership check (`alliance.check_owner`, not shown but referenced). |
| `myRequests` | `alliance.rs:97-105` | `AllianceBo::my_requests` (`alliance_bo.rs:229-234`) | Matches. |
| `myRequestsDelete` | `alliance.rs:108-116` | `AllianceBo::delete_join_request_by_id` (`alliance_bo.rs:238-244`) | Rust's own doc comment says "bare delete by id (no checks, matching Java)" (`alliance.rs:107`) — **the B8 authorization gap is deliberately preserved for parity**, not fixed. Confirmed intentional, not an oversight. |
| `requestJoin` | `alliance.rs:119-128` | `AllianceBo::request_join` (`alliance_bo.rs:249-274`) | Matches, including the duplicate-check bypass (module doc comment, `alliance_bo.rs:1-9`). |
| `acceptJoinRequest` | `alliance.rs:131-139` | `AllianceBo::accept_join` (`alliance_bo.rs:280-314`) | Matches both branches (target-user-still-free vs already-elsewhere), including `deleteByUser`-on-accept (`alliance_bo.rs:301-304`) and the silent-no-op branch (`alliance_bo.rs:305-311`). Audit calls correctly omitted per the module doc comment. |
| `rejectJoinRequest` | `alliance.rs:142-150` | `AllianceBo::reject_join` (`alliance_bo.rs:318-331`) | Matches. |
| `leave` | `alliance.rs:153-157` | `AllianceBo::leave` (`alliance_bo.rs:335-350`) | Matches, including the idempotent-if-already-alliance-less behavior. |
| `areEnemies` (cross-ref, combat-owned) | n/a (inlined) | `are_enemies` free fn, duplicated in `attack_mission_manager_bo.rs:1779` and `unit_interception_finder_bo.rs:428` | Ported as two separate free functions rather than one shared `AllianceBo::are_enemies`, matching Java's `AllianceBo.areEnemies` logic exactly but not its single-source-of-truth structure — cosmetic, not a behavior risk, since both copies were checked to implement the same predicate. |
| User-delete cascade (B13/B14) | n/a | `user_storage_bo.rs` inline (lines ~335-370, commented per-listener with Java order annotations) | Matches Java's order-1 (`AllianceJoinRequestBo`) then order-2 (`AllianceBo`) sequencing, including the "only clean up own join requests if not the alliance owner" nuance. |

`rust-backend/docs/UNPORTED-ENDPOINTS.md` — no alliance entries at all (searched, zero hits),
consistent with this being a fully-ported surface. `rust-backend/docs/PORTING-ROADMAP.md:219`
independently lists `AllianceRestService` as done ("12 endpoints, ownership checks, join-request
lifecycle" — their count of 12 vs this inventory's 11 route mappings likely counts POST/PUT to
`game/alliance` as two).

## 6. Open questions / suspected divergences

1. **Java bug, not a Rust gap**: deleting your own alliance while pending join requests exist
   against it throws a raw DB FK-constraint error in Java (`AllianceBo.delete`, `AllianceBo.java:67-71`
   never purges `alliance_join_request`), while the Rust port proactively cleans those rows up first
   (`alliance_bo.rs:200-203`) and succeeds. Per plan §9.11, this needs Kevin's call: should the BDD
   spec assert Java's crash (freezing the bug as "spec"), assert Rust's clean success (implicitly
   flagging Java as needing a fix — add `deleteByAlliance` to `AllianceBo.delete`, mirroring
   `doDeleteUser`), or should this scenario be marked `@known-divergence` and excluded from Layer 1
   until Java is patched? Recommend the middle option (fix Java to match Rust's more-correct
   behavior) since it's a one-line change (`allianceJoinRequestRepository.deleteByAlliance(alliance);`
   before `repository.delete(alliance);` in `AllianceBo.delete`).
2. **`save`'s create/update branch condition differs in representation, not (yet) in observed
   behavior**: Java branches on `alliance.getId() == null` (boxed `Integer`, so JSON omitting `id`
   or sending `null` explicitly → create); Rust branches on `dto.id == 0` (`u16`, so JSON must omit
   `id` entirely and rely on serde's `Default`/`0`, or Rust's DTO needs to distinguish
   absent-vs-zero). If the frontend ever sends `{"id": 0, ...}` explicitly the two backends would
   disagree (Java: attempt update of alliance 0 → not-found error; Rust: silently create). Needs a
   scenario once §4's `alliances exists owned by user` seeding is available: `POST` with an
   explicit `id: 0` body, assert both backends the same way — this is exactly the kind of case
   Layer 2 (full-diff) would catch even if nobody thought to write it as an explicit `Then`.
3. **`myRequestsDelete` (B8) authorization gap is present in BOTH backends by design** (Rust's own
   comment says "no checks, matching Java"). This is a real product security issue (IDOR — any
   logged-in user can delete any other user's pending join request by numeric id) independent of
   parity. Not the BDD harness's job to fix, but worth flagging to Kevin outside this inventory
   since both backends agree — Layer 1/2 parity testing will never surface it (parity tests
   equivalence, not correctness against a security baseline).
4. **`listRequest`'s inline ownership-check error message** ("You are not the owner of the
   alliance") differs textually from `AllianceBo.checkInvokerIsOwner`'s message ("You are NOT the
   owner of that alliance, try hacking the owner account") despite being semantically the same
   check. If the BDD `Then ... error containing "..."` steps match on substrings (as drafted in §3),
   this is harmless as long as each scenario uses the exact fragment the corresponding endpoint
   actually produces — but a copy-paste of the wrong fragment between `listRequest` and the other
   ownership-gated endpoints would produce a false failure. Worth a inventory-wide note when
   scenarios are finalized.
5. **Reserved id range for `alliances`/`alliance_join_request`/synthetic third+ users** is not
   specified anywhere in `BDD-PARITY-PLAN.md`'s VERIFIED notes (only units/time
   specials/special-locations/missions are reserved). This inventory's §3/§4 provisionally proposes
   ≥ 900 for both tables (currently empty, so nothing collides today) and flags that a synthetic
   3rd+ test user needs its own reserved id range *and* a dedicated spare home-planet id (distinct
   from 1234, which is reserved for the special-location feature) — needs Kevin's sign-off before
   implementation, per the task's own instruction to propose rather than assume.
6. **No websocket coverage in this domain** (see §2 "Websocket" note) means every alliance scenario
   leans entirely on Layer 1 explicit DB `Then`s + Layer 2 table diff — there is no ws-frame Layer 2
   check possible here, which is fine but worth calling out so nobody wonders why alliance
   `.feature` files never start a `ws_capture.js` process.
7. **`checkPost` interaction with `save`**: `BaseRestServiceTrait.checkPost` (games-rest,
   L17-21) rejects a POST body that carries a non-null `id`. This wasn't traced in the Rust route
   (`alliance.rs:64-73` binds POST and PUT to the same `save` handler with no analogous guard
   visible in the excerpt read) — worth a quick follow-up read of the full `save` Rust handler
   (only the routing + Bo call were read here, not whether there's a POST-with-id guard) before
   writing the corresponding scenario, since Java would reject `POST` + `id` present with
   `"Post request can't contain an id"` and it's unclear whether Rust replicates that specific
   guard or just falls through to the `dto.id == 0` branch logic in item 2 above.
