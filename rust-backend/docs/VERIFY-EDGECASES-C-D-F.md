# Bit-for-bit verification runbook — registration / combat / conquest edge cases

How to runtime-verify the three **firing-path** behavioural ports from the
M2/M3 edge-case work that were *not* covered by the read-path smoke test:

- **C** — mission-registration checkers (`checkUnitCanDeploy` ONLY_ONCE, cross-galaxy, mission-invisibility) in `unit_mission_registration_bo.rs`
- **D** — combat shield-bypass time-special rule in `attack_mission_manager_bo.rs`
- **F** — conquest special-location requirement trigger + old-owner BUILD_UNIT cancel in `conquest.rs` / `requirement_bo.rs`

The read-path changes (B hidden-unit sync, E unit-type limits, G/H syncs,
A cleanUpUnexplored) were already runtime-smoke-tested (HTTP 200, no sqlx panics,
E proven via `computedMaxCount 15` vs raw `maxCount 10`). This doc is only the
remaining firing-path closeout.

## Method (same as every prior session's verification)

Diff the **DB state** (and, where shaped, the HTTP response) produced by the same
seeded scenario on two backends:

- **Java reference (dc13)** = `sgalactica_java_13`, REST at `http://127.0.0.1:8123/game_api/`.
- **Rust (dc14)** = `sgalactica_java_14`, run the built binary:
  ```bash
  cd /public/owge/rust-backend
  source .env.dc14 && OWGE_DYNAMIC_FILES_PATH=/tmp/owge_dynamic_dc14 \
    OWGE_SERVER_PORT=8100 OWGE_WS_PORT=7475 nohup target/debug/owge-rest &
  ```
  (Use spare ports 8100/7475 if a `:8099` instance is already winning the
  db-scheduler race. Kill your own instance when done.)

DB access: `mysql -h127.0.0.1 -uOWGEu4 -pStrongestOneill sgalactica_java_1{3,4}`.

Game JWT: `python3 scripts/mint_jwt.py --id <N> --username rusttester --email rust@test.local`
(RS256 over `/root/keys/private.key`; trusted by **both** backends). Admin token:
`POST /game/adminLogin` with that JWT.

General loop per scenario:
1. Apply the **identical seed SQL** to *both* `sgalactica_java_13` and `_14`
   (test-DB writes are authorized). FK-clean leftovers first (order:
   `mission_information → scheduled_tasks → obtained_units → missions →
   unlocked_relation → mission_reports → websocket_events_information →
   explored_planets` for the test users).
2. Drive the action via the REST API on each backend (register / activate), OR
   seed the row directly when the registration path isn't what's under test.
3. **Fire** scheduled missions by forcing the task due:
   `UPDATE scheduled_tasks SET execution_time = NOW() - INTERVAL 1 MINUTE WHERE task_name='mission-run';`
4. `diff` the resulting rows on both DBs. They must match (modulo the
   already-documented deltas: passive-resource drift, shuffle-PRNG kill counts,
   doAfterCommit watermark presence).

---

## C — registration checkers

These are **negative** tests (the port should now *reject* what Java rejects).

### C1 — deploy ONLY_ONCE
1. On both DBs: `INSERT/UPDATE configuration SET value='ONLY_ONCE_RETURN_SOURCE' WHERE name='DEPLOYMENT_CONFIG'` (insert the row if absent).
2. Seed a user whose unit is **currently on a DEPLOYED mission** at a planet the
   user does **not** own (an `obtained_units` row with `mission_id` → a `missions`
   row of type `DEPLOYED`, target = a foreign planet).
3. `POST /game/mission/deploy` (a second deploy of that same unit to another
   foreign planet).
4. **Expect on both:** HTTP 400, body message `"You can't do a deploy mission after a deploy mission"`, and **no** new mission/scheduled_task row created.
5. Control: set `DEPLOYMENT_CONFIG='FREEDOM'`, repeat → both allow it.
6. Also: `DEPLOYMENT_CONFIG='DISALLOWED'` + any `POST /game/mission/deploy` → both 400 `"The deployment mission is globally disabled"`.

### C2 — cross-galaxy
1. Seed source planet and target planet in **different galaxies**.
2. Pick a unit whose resolved speed-impact-group (unit override, else its type's)
   either (a) has `can_<mission> = 'NONE'`/null for the attempted mission, or
   (b) is **not** in the user's `unlocked_relation` for `SPEED_IMPACT_GROUP`.
3. `POST /game/mission/<type>` source→target.
4. **Expect on both:** 400 with
   - `"This speed group doesn't support this mission outside of the galaxy"` for case (a), or
   - `"Don't try it.... you can't do cross galaxy missions, and you know it"` for case (b).
5. Control: same-galaxy source/target → both allow (the cross-galaxy guard is skipped).

### C3 — mission invisibility (DO_HIDE)
1. Seed a user with an **ACTIVE** `active_time_specials` row whose `time_special`
   has a `rules` row of type `TIME_SPECIAL_IS_ENABLED_DO_HIDE` whose destination
   (UNIT or UNIT_TYPE) matches the unit being sent.
2. Register any unit mission with only such units.
3. **Expect on both:** the created `missions` row has `invisible = 1`.
4. Control: no DO_HIDE rule / no active special → `invisible = 0` (unless a unit
   has the static `units.is_invisible` flag).

---

## D — combat shield-bypass time-special rule

1. Seed an attacker user with an **ACTIVE** time special carrying a
   `TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD` rule whose destination matches the
   **defender** unit (or its type).
2. Seed a defender stack on the target planet whose unit has a non-zero `shield`
   and whose attacker unit's own `bypass_shield` flag is **0** (so the bypass can
   only come from the rule).
3. To make combat deterministic (the shuffle PRNG differs Java vs Rust), use a
   1-attacker vs 1-defender setup so target selection is forced.
4. Register + fire the attack on both.
5. **Expect on both:** the defender's damage is computed against `health` **only**
   (shield ignored) — i.e. identical `attackInformation` `finalCount` / survivor
   counts and identical `earnedPoints`. Re-run with the rule removed → damage is
   computed against `health + shield` (fewer/no kills), and both backends agree.
6. Sanity: confirm the kill-count **divisor** path (`addPointsAndUpdateCount`) is
   unaffected by the rule — Java deliberately uses only the own-flag there, and
   the Rust port matches (this is the one site D left as the bare flag).

---

## F — conquest special-location trigger + old-owner build cancel

1. Seed: a **special-location planet** (`planets.special_location_id` non-null)
   owned by user B, with:
   - an `object_relations`/`requirements` chain gated by `HAVE_SPECIAL_LOCATION`
     with `second_value` = that special-location id, currently **unlocked** for
     user B (`unlocked_relation` row), and
   - an in-progress **BUILD_UNIT** mission for user B on that planet (a `missions`
     row type BUILD_UNIT + `mission_information.value` = planet id + a
     `scheduled_tasks` row + in-build `obtained_units`).
2. User A conquers the planet (seed an attacking CONQUEST mission A→planet strong
   enough to win; fire it).
3. **Expect on both DBs after firing:**
   - `planets.owner` → user A;
   - user B's `unlocked_relation` row for that `HAVE_SPECIAL_LOCATION` relation is
     **removed** (the special-location requirement re-evaluated and is no longer met);
   - user B's BUILD_UNIT mission + its `mission_information` + `scheduled_tasks`
     row + in-build `obtained_units` are **gone**, and user B's primary/secondary
     resources are **refunded** by the build cost (modulo passive-gen drift).
4. **Leave variant:** give a user a (rare) special-location planet they can leave
   (no units, no running build, not home), `POST /game/planet/leave`, and confirm
   the same `HAVE_SPECIAL_LOCATION` unlock re-evaluation happens on both.

### Known acceptable deltas (do not treat as failures)
- The Rust `cancel_build_unit` commits in its **own** transaction (Java uses
  `Propagation.MANDATORY` inside the conquest tx). End state is identical in the
  normal firing path (no post-process rollback); only atomicity-under-rollback
  differs.
- doAfterCommit websocket watermarks may be present on Rust but absent on dc13
  (the long-documented ambient-tx gating); compare **DB state**, check Rust
  watermarks separately.
- Combat kill counts can differ by the shuffle-PRNG (xorshift vs `Collections.shuffle`);
  use 1v1 setups to avoid it.
