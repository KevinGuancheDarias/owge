# BDD Parity Harness — executable Gherkin specs that run against BOTH backends

**Status:** design document, nothing implemented yet (2026-07-07).
**Verified against repo + dev DB 2026-07-09:** all referenced harness assets,
endpoints, table names, config rows and docker images exist; §6.1 ids and §9
environment state were corrected/confirmed from the live baseline (see the
"VERIFIED" notes inline).
**Audience:** the implementer (human or model). This document is deliberately
verbose and self-contained: read it top to bottom before writing any code, and
treat every "MUST"/"MUST NOT" as a hard rule. Where a decision was already made,
the alternative is named and the reason it was rejected is given — do not
re-litigate those choices.

---

## 1. Goal and motivation

The Rust port (`rust-backend/`) keeps diverging from the Java reference
(`business/` + `game-rest/`) in state-mutating flows. Known instances, all found
*after* users hit them or by hand-built one-off harnesses:

- The special-location unlock bug (`BUG-SPECIAL-LOCATION-UNLOCK.md`): the new
  planet owner never receives `HAVE_SPECIAL_LOCATION` unlocks on conquest or
  establish-base. Hidden behind a confident-but-wrong code comment.
- The deploy-registration crash on redeploying an emptied DEPLOYED stack
  (commit f5956ba6).
- Eight websocket-sync payload gaps (commit 4e710856), found by the `ws_verify`
  harness.
- Combat divergences (stack ordering, D0/D2 rounding), found by the
  `mission_verify` harness + deterministic RNG.

The plan: **write the game's behavior once, as Gherkin scenarios, and execute
each scenario against both backends, asserting (a) each backend does what the
scenario says, and (b) both backends leave behind byte-identical database state
and websocket traffic.** The Gherkin suite becomes:

1. the parity regression suite (catches Rust-vs-Java divergence),
2. the executable specification of intended behavior (catches "both are wrong"),
3. the safety net for the planned SeaORM migration of the Rust internals — once
   the suite is green, the Rust data layer can be rewritten freely because the
   suite only observes black-box outcomes.

### The canonical example (from the bug that motivated this)

```gherkin
Scenario: Establishing a base on a special-location planet unlocks the gated unit
  Given planet 1234 has special location 1
  And unit 55 exists gated by requirement HAVE_SPECIAL_LOCATION with second value 1
  And user 1 has 5 units of id 10 on planet 1002
  When user 1 runs an ESTABLISH_BASE mission from planet 1002 to planet 1234
  Then planet 1234 is owned by user 1
  And table unlocked_relation contains a row for user 1 and the relation of UNIT 55
  And user 1 received websocket event "unit_unlocked_change" containing unit id 55
```

Run against Java today: passes. Run against Rust today: the last two `Then`
steps fail. That failure report — *before any user files a bug* — is the entire
point of this project.

---

## 2. Non-negotiable design decisions (already made — do not revisit)

### 2.1 ONE driver, steps implemented ONCE, backends as black boxes

The step definitions are implemented exactly once, in an external test driver.
There is **no** Cucumber-JVM code in the Java backend and **no** `cucumber`
crate in the Rust backend.

*Why:* if steps were implemented twice (Java steps + Rust steps), the two step
implementations could themselves diverge, recreating the parity problem one
level up where nothing can detect it. Every step is naturally black-box anyway:
`Given` = SQL writes, `When` = REST calls / scheduler nudges, `Then` = SQL reads
+ captured websocket frames. The backends need zero test code.

### 2.2 Driver language: Rust, runner: the `cucumber` crate (cucumber-rs)

**(REVISED 2026-07-09 by Kevin — supersedes the original Python/behave choice.)**
The test suite will grow large; a statically typed driver is easier to read
and maintain at that scale, and Rust has first-class raw-SQL support via
`sqlx` (plain runtime `query()`/`fetch_all()` — do NOT use the compile-time
`query!` macros; the harness doesn't need them and they'd couple builds to a
live DB). Stack: `cucumber` crate (async, tokio) for Gherkin execution with a
typed `World` struct instead of an untyped context; `sqlx` (mysql) for
Given/Then SQL; `reqwest` for When REST calls; `jsonwebtoken` for HS256 JWT
minting (replaces wrapping `mint_jwt.py`); `serde_json` for ws-frame
predicates. The feature files are plain Gherkin — nothing about them is
runner-specific.

The reusable Python diff utilities (`table_diff.py`, `trace_diff.py`,
`rest_sync_diff.py`, `dump_mission_state.sh`) are NOT ported: they were always
invoked by the runner as subprocesses (§5.4), never imported in-process, so
the driver language doesn't affect them. `python3` must merely be on PATH.
Likewise `ws_capture.js` remains a Node 14 subprocess.

**The black-box guardrail (the reason Rust was originally rejected):** a Rust
driver tempts in-process access to `owge-business`/`owge-rest`. This is made
structurally impossible: the driver is a **standalone crate with its own
`[workspace]`** (NOT a member of the rust-backend workspace), and it MUST
NEVER declare a path dependency on any `owge-*` crate. Depending on `sqlx`
(which the backend also happens to use) is fine — that's a shared third-party
library, not backend internals. Both backends stay pure black boxes: SQL in,
REST in, SQL + websocket frames out.

Rejected alternatives: **Java/Cucumber-JVM** — this machine has no local
JDK/maven (docker-only builds), making driver iteration painfully slow, and
the in-process-temptation argument applies equally. **Node (cucumber-js)** —
frontend is pinned to Node 14, too old for current cucumber-js. **Python 3 +
behave** (the original choice) — dynamic typing reads and maintains poorly at
the suite size this plan targets; rejected 2026-07-09.

### 2.3 Sequential runs against ONE shared database, snapshot/restore between

Both backends point at the single dev MySQL. Per scenario: seed once, snapshot,
run against Java, capture outcomes, restore snapshot, run against Rust, capture
outcomes, diff. This is precisely the proven `mission_verify` topology (see its
README, "Topology" section) — reuse it, do not invent a two-database scheme.

*Why:* byte-identical starting state is guaranteed by construction, and only one
MySQL, one JWT config, one docker network need to exist.

### 2.4 Two assertion layers per scenario: explicit `Then` + implicit full diff

- **Layer 1 — explicit:** the scenario's `Then` steps run against *each* backend
  independently. They encode intended behavior. If both backends are wrong the
  same way, layer 1 still fails. Verdict per backend: `JAVA_SPEC` pass/fail,
  `RUST_SPEC` pass/fail.
- **Layer 2 — implicit:** after each backend run, the driver dumps the full
  mission-footprint table state (normalized) and the full captured websocket
  stream (normalized), and after both runs diffs Java-vs-Rust. This catches
  divergences the scenario author never thought to assert. Verdict: `PARITY`
  pass/fail.

A scenario only counts green when all three verdicts pass. Layer 2 is **always
on** for every scenario — it is not opt-in, because its whole value is catching
the unknown unknowns. (Example: a scenario written to test conquest resource
looting would still have caught the special-location bug via the
`unlocked_relation` table diff.)

### 2.5 Gherkin style: concrete IDs, concrete tables, no business-fluff

Steps say `planet 1234`, `unit 55`, `table unlocked_relation contains …`.
Parity testing wants precision; these scenarios are for engineers and diff
tools, not product stakeholders. Do NOT abstract IDs behind names like "the
hero planet" unless a step alias maps the name to a fixed ID in one place.

---

## 3. Prior art you MUST read and reuse (do not reimplement)

All paths relative to `rust-backend/scripts/`. **Beware:** the READMEs of these
harnesses reference `/public/owge/...` — that path does not exist on the current
machine; the repo is at `/home/kevin/projects/owge`. Treat every `/public/owge`
in old docs as the repo root.

| Asset | What it gives the BDD harness |
|---|---|
| `mission_verify/README.md` | The whole execution topology: shared-DB sequential snapshot/restore, how the Java backend is booted in a container on the DB's docker network (context path `/game_api`, JWT minted HS256 from the `JWT_SECRET` configuration row, `OWGE_WS_SYNC_RATELIMIT_PER_MINUTE=0`), how the Rust backend runs natively (`OWGE_DB_JDBC_URL=mysql://root:1234@127.0.0.1:3306/owge OWGE_SERVER_PORT=8080 ./target/debug/owge-rest`), teardown traps. |
| `mission_verify/run_mission_parity.sh` | Working reference implementation of the per-scenario pipeline (seed → snapshot → Java → restore → Rust → diff). The BDD driver generalizes this; steal its mechanics (mysqldump table snapshot, restore, backend health polling). |
| `mission_verify/dump_mission_state.sh` | Dumps the mission-footprint tables to normalized JSON, filtered to test users/planets. Layer-2 state capture calls this as-is (env `USERS`/`PLANETS` widen the filter). |
| `mission_verify/table_diff.py` | The normalized table differ (strips surrogate ids/timestamps, parses `mission_reports.json_body`, sorts rows by stable business keys). Layer-2 state diff calls this as-is. Its normalization rules are documented in the mission_verify README §"Normalisation". |
| `mission_verify/trace_diff.py` | RNG-trace differ. Only relevant for combat scenarios; wire it in for those. |
| `ws_verify/ws_capture.js` | The socket.io capture client (Node 14 + `socket.io-client@2.4.0`, run from `/tmp/wsclient` or with the node_modules symlink — see ws_verify README "Prerequisites"). Emits one JSON line per received event, already normalizing `lastSent` → `"<TS>"` and sorting the authentication payload. Layer-2 websocket capture shells out to this, one process per captured user, started before the `When` and stopped after the last `Then`. |
| `ws_verify/rest_sync_diff.py` | Structural `norm`/`short`/`diff` helpers, imported by `table_diff.py`. |
| `mint_jwt.py` | Mints the HS256 JWT for a given user id (`--id 1 --username … --email …`) from the `JWT_SECRET` configuration row. The Rust driver reimplements this with the `jsonwebtoken` crate (§2.2) — use `mint_jwt.py` as the **reference for the exact claim shape** (and as a debugging cross-check: both must produce tokens the backends accept). |
| `seed_*.sql` (in `scripts/`) | Existing scenario seeds (attack, conquest, deploy, gather, …). These become the `Given`-step building blocks / get ported into feature Backgrounds over time (§8 Phase 2). |
| The rich websocket seed (committed in 89fd4771, `ws_verify/seed_user1_full_compare.sql`) | The maximal "user 1 exercises every payload path" baseline. Useful as the Background for read-side scenarios. |
| `../pending_migration/ATTACK_PARITY_PLAN.md` | Context on the deterministic-RNG design (`ATTACK_DETERMINISTIC_RNG` configuration row, mission-id-seeded `JavaRandom`, the RNG trace schema). |
| `../docs/BUG-SPECIAL-LOCATION-UNLOCK.md` | The first bug this harness must be able to catch; §6.1 turns it into the first feature file. |

Key techniques already proven by those harnesses, which the BDD steps rely on:

1. **Firing a due mission deterministically** (no waiting for real delays):
   after the REST POST creates the mission + its db-scheduler row, force the
   task's `execution_time` into the past and poll `missions.resolved = 1`:
   ```sql
   UPDATE scheduled_tasks SET execution_time = DATE_SUB(NOW(), INTERVAL 1 SECOND)
    WHERE task_name = 'mission-run' AND task_instance = '<mission_id>';
   ```
   Both backends' `OWGE_BACKGROUND` schedulers pick it up within their poll
   interval. (Technique documented in both harness READMEs.)
2. **Same RNG seed on both backends** (combat scenarios only): the RNG seed is
   `mission.id`, so REST-created missions get different auto-increment ids per
   backend. The `FIXED_MISSION_ID` seeding technique (mission_verify README §
   "Fixed-seed mode") inserts the `missions` + `scheduled_tasks` rows by hand
   with a fixed id so both backends fire the *same* mission id ⇒ same seed.
   The BDD equivalent is a dedicated `When` variant (§6.3, step W3).
3. **`ATTACK_DETERMINISTIC_RNG='TRUE'`** must be upserted into `configuration`
   before any combat scenario (the runner does this globally).
4. **Normalization over synchronization** for time: don't try to control the
   clock; strip/replace all date-ish fields in both the table differ and the ws
   capture (already implemented in `table_diff.py` / `ws_capture.js`).

---

## 4. Directory layout

```
rust-backend/scripts/bdd_parity/
├── README.md                  # quickstart: prerequisites, how to run, how to add a scenario
├── Cargo.toml                 # OWN [workspace] — standalone, NOT a rust-backend workspace member
│                              # deps: cucumber, tokio, sqlx (mysql), reqwest, jsonwebtoken,
│                              #       serde/serde_json, anyhow. NEVER an owge-* path dep (§2.2).
├── run_bdd_parity.sh          # the §5.1/§7 runner (loops backends, restores DB, diffs)
├── features/
│   ├── special_location_unlock.feature      # §6.1 — the first feature, written below
│   ├── conquest.feature                     # Phase 2
│   ├── establish_base.feature               # Phase 2
│   ├── deploy.feature                       # Phase 2
│   └── ...
└── src/
    ├── main.rs                # #[tokio::main]: builds the cucumber runner, .before/.after
    │                          # hooks = the §5 lifecycle; reads OWGE_BDD_BACKEND
    ├── world.rs               # BddWorld: typed scenario state (backend handle, captured_users,
    │                          # ws_procs, created_missions, registered planet/unit filters)
    ├── steps/
    │   ├── given_state.rs     # all Given step defs (SQL seeding)
    │   ├── when_actions.rs    # all When step defs (REST + scheduler nudge)
    │   ├── then_db.rs         # Then defs asserting table state
    │   └── then_ws.rs         # Then defs asserting captured websocket frames
    └── support/
        ├── backends.rs        # Backend abstraction: base_url, ws path, boot/health/teardown
        ├── db.rs              # sqlx pool/queries + snapshot/restore (shells to mysqldump/mysql)
        ├── ws.rs              # ws_capture.js subprocess management + frame parsing
        ├── rest.rs            # JWT minting (jsonwebtoken, claims per mint_jwt.py) + reqwest helpers
        └── artifacts.rs       # per-run artifact dirs, dump/diff subprocess invocation, verdicts
```

Conventions:

- Everything under `bdd_parity/` is committed (plus `Cargo.lock`; `target/` is
  git-ignored). Artifacts go to
  `/tmp/bdd_parity_runs/<timestamp>/<feature>/<scenario>/{java,rust}/…` — never
  into the repo.
- `support/db.rs` uses sqlx runtime queries (never `query!` macros, §2.2) for
  reads/asserts and Given seeding; snapshot/restore **shells out to
  `docker exec <db-container> mysqldump/mysql`** exactly as the existing shell
  harnesses do (dump/restore through a driver is a reimplementation trap —
  don't).
- The cucumber runner is a **binary target** (`cargo run --`), not a
  `cargo test` harness — the shell runner invokes it once per backend pass
  with `OWGE_BDD_BACKEND` set.
- No ORM, no migrations framework. Keep it boring.

---

## 5. Execution model — what one scenario run actually does

This is the heart of the design. Implement it in `src/main.rs` (cucumber
`.before`/`.after` hooks) + `src/support/`, with per-scenario state in
`BddWorld`. The driver executes each scenario **twice** via the "backend axis"
described below.

### 5.1 The backend axis

cucumber-rs has no native "run each scenario against N targets" concept.
Implement it the simple way: **the runner script loops**. A top-level
`run_bdd_parity.sh`:

```
1. global setup:
   a. ensure DB container is up; upsert configuration rows:
      ATTACK_DETERMINISTIC_RNG='TRUE', (ws rate-limit row if applicable)
   b. boot the Java backend container (mission_verify technique), health-poll
   c. build (cargo build) + start the Rust backend natively, health-poll
2. for each feature/scenario selected:
   a. RESET  : restore the clean-universe baseline (§5.2)
   b. SEED   : run the driver for this scenario with OWGE_BDD_PHASE=seed
               → executes ONLY the Given steps (via a phase guard in the step
                 code), writing the scenario state into the DB
   c. SNAP   : mysqldump snapshot of the in-scope tables (post-seed state)
   d. JAVA   : run the driver with OWGE_BDD_BACKEND=java (full scenario; Given
               steps are no-ops in this phase — state already present),
               lifecycle §5.3
   e. RESTORE: restore the §5.2c snapshot
   f. RUST   : run the driver with OWGE_BDD_BACKEND=rust, lifecycle §5.3
   g. DIFF   : layer-2 diff of the java/ vs rust/ artifacts (§5.4)
   h. record verdicts (JAVA_SPEC, RUST_SPEC, PARITY)
3. summary table + non-zero exit if any verdict failed
```

**Simplification permitted for Phase 1:** instead of the seed-phase trick
(2b/2c), make Given steps **idempotent deterministic SQL** (DELETE-then-INSERT
with fixed ids, exactly like the existing `seed_*.sql` files) and simply run
them in both the JAVA and RUST passes, with the RESET(a) restore before *each*
pass. Then the pipeline is: RESET → driver(java) → RESET → driver(rust) → DIFF.
This is byte-identical-by-construction **iff** Givens are deterministic (fixed
ids, no NOW() except in columns the differ strips). Start with this; introduce
the snapshot-after-seed refinement only if a Given legitimately can't be made
deterministic.

### 5.2 Database reset — the baseline

- The **baseline** is a full mysqldump of the clean dev universe (schema +
  `04_insert_data.sql`-level content + the rich seed of 89fd4771), taken once at
  global setup into the run's artifact dir, restored before each backend pass.
- Restore = `mysql < baseline.sql` via docker exec. Full-DB restore is O(seconds)
  on this dataset and removes an entire class of "table X wasn't in the
  snapshot" bugs. Only optimize to table-scoped snapshots (mission_verify
  style) if runtime becomes a real problem.
- Existing caveat inherited from mission_verify README: seeds that
  `DELETE FROM user_storage` for users 1/2 fail if stray FK rows reference
  them. The baseline restore makes this moot — another reason to prefer
  full-DB restore.
- **db-scheduler hygiene:** after every restore, clear leftover
  `scheduled_tasks` rows not part of the seed, so a stale task from a previous
  pass can't fire mid-scenario:
  `DELETE FROM scheduled_tasks WHERE task_name='mission-run';` (baseline should
  contain none, but be defensive).
- Both backends CACHE. Java has taggable-cache with by-user tags; the Rust side
  may cache too. After a restore that happened *behind a running backend's
  back*, in-memory caches are stale. Mitigations, in order of preference:
  1. Scenario steps only ever assert DB rows + ws frames caused by the
     scenario's own `When` (which runs *after* the restore and mutates through
     the backend, evicting/refreshing what it touches). This usually suffices.
  2. If a cache-staleness false-negative is observed (symptom: a `Then` reads a
     REST/ws payload showing pre-restore data), add a global "cache clear"
     hook: cheapest is restarting the Rust process (fast) and, for Java,
     hitting an admin endpoint or accepting a container restart per scenario
     (slow — measure first). Document what was needed in the harness README.

### 5.3 The driver lifecycle within one backend pass

In `src/main.rs` hooks + `BddWorld` (pseudocode; `context` = the World):

```
global setup (once per driver invocation, e.g. lazy static / before-first-scenario):
    read OWGE_BDD_BACKEND (java|rust) → context.backend  (base_url, ws_path,
        jwt cache, sqlx pool from support/backends.rs)
    create artifact dir /tmp/bdd_parity_runs/<run>/<feature>/<scenario>/<backend>/

World::new / .before hook (per scenario):
    context.captured_users = set()      # user ids whose ws traffic we record
    context.ws_procs = {}               # user_id -> ws_capture.js Child
    context.created_missions = []       # mission ids created by When steps

  # NOTE: DB reset happens OUTSIDE the driver (runner script §5.1) so both
  # backends see identical state. Do NOT reset inside the before hook.

Given steps:
    pure SQL via support/db.rs. Register interesting ids on the World
    (users involved → auto-added to captured_users; planets/units → added to
    the layer-2 dump filter set).

Just before the FIRST When step of the scenario (implement as a helper the
When steps call):
    for each user in captured_users: start ws_capture.js
        (base_url, ws_path per backend; JWT minted per user; duration = long,
         killed explicitly later; stdout → artifacts/<backend>/ws_user<id>.jsonl)
    wait for each capture's "authentication" line (connection is live) before
    proceeding — otherwise events can be missed.

When steps:
    REST calls / scheduler nudges (§6.3). Poll for completion
    (missions.resolved=1) with a hard timeout (default 40 s, like
    mission_verify's POLL_SECONDS). On timeout: fail the scenario loudly with
    the mission row + scheduled_tasks row dumped into the failure message.

Then steps:
    - DB assertions: SELECT via support/db.rs, assert, and on failure print the
      actual rows (not just "assertion failed") — failure messages are the
      product here.
    - WS assertions: read the capture files written so far (flush-safe: read
      the file, don't talk to the process), filter kind=="deliver", match
      eventName + payload predicate (§6.4).
      IMPORTANT: allow a settle wait — websocket emissions are post-commit and
      async; a Then may run before the frame lands. Implement ws assertions as
      "poll the capture file up to N seconds (default 10) for a matching
      frame", not a single read.

.after hook (per scenario):
    sleep the settle window (2 s) → kill all ws_capture procs
    LAYER-2 CAPTURE:
      - dump_mission_state.sh <db> artifacts/<backend>/tables.json
        with USERS/PLANETS env widened to everything the scenario registered
      - ws captures are already in artifacts/<backend>/ws_user<id>.jsonl
      - if combat: copy the RNG trace from the backend log to
        artifacts/<backend>/rng_trace.jsonl
    record JAVA_SPEC / RUST_SPEC verdict from the cucumber scenario result
    (runner reads the driver's summary output / process exit code per pass)
```

### 5.4 Layer-2 diff (runner script, after both passes)

```
table_diff.py  java/tables.json  rust/tables.json  --a-label Java --b-label Rust
for each user captured:
    normalize + sort deliver lines (the ws_verify one-liner technique:
      grep '"kind":"deliver"' | sort) and diff java vs rust
if combat scenario: trace_diff.py java/rng_trace.jsonl rust/rng_trace.jsonl
```

PARITY verdict = all three diffs empty. Any diff output goes verbatim into the
scenario's report block.

**Known-acceptable diff suppressions** (encode in the differ config, with a
comment justifying each): none initially. Every suppression added later MUST
cite a reasoned decision ("Java emits X twice due to double doAfterCommit —
accepted, see issue #N"). Unexplained suppressions are how parity rots. Note:
`ws_capture.js` already normalizes `lastSent`; `table_diff.py` already strips
surrogate ids/dates — those are normalizations, not suppressions, and live in
the shared tools.

### 5.5 Ordering semantics for websocket assertions

Frames are compared as **sorted multisets, not sequences** (the ws_verify
`| sort` technique). Rationale: post-commit emission order is not contractual
in either backend (async executors), and sequence-comparison would produce
flaky diffs. If a scenario genuinely needs ordering ("report event arrives
after unit change"), write an explicit `Then` for it rather than tightening
the global diff.

---

## 6. The step catalog

The initial vocabulary. Keep it SMALL and COMPOSABLE — resist adding a bespoke
step per scenario; prefer parameterized steps + `Background`. Every step's
implementation notes name the exact tables/endpoints so the implementer does
not need to reverse-engineer them.

### 6.1 The first feature file — write this one first, verbatim

`features/special_location_unlock.feature`:

```gherkin
Feature: Special-location unlocks
  Acquiring a planet that carries a special location must grant the relations
  gated by HAVE_SPECIAL_LOCATION on that special location; losing the planet
  must revoke them. Reference: Java PlanetBo.definePlanetAsOwnedBy /
  doLeavePlanet + ConquestMissionProcessor; see
  docs/BUG-SPECIAL-LOCATION-UNLOCK.md.

  Background:
    Given the standard test universe
    And planet 1234 has special location 500 and no owner
    And unit 9100 exists gated by requirement HAVE_SPECIAL_LOCATION with second value 500
    And time special 900 exists gated by requirement HAVE_SPECIAL_LOCATION with second value 500
    And user 1 has 5 units of id 10 on planet 1002
    And user 1 has explored planet 1234

  Scenario: Establish base grants the unlocks to the new owner
    When user 1 runs an ESTABLISH_BASE mission from planet 1002 to planet 1234 with 5 units of id 10
    Then planet 1234 is owned by user 1
    And table unlocked_relation has a row for user 1 and object UNIT reference 9100
    And table unlocked_relation has a row for user 1 and object TIME_SPECIAL reference 900
    And user 1 received websocket event "unit_unlocked_change" where some item has id 9100
    And user 1 received websocket event "time_special_unlocked_change" where some item has id 900

  Scenario: Conquest transfers the unlocks from old owner to new owner
    Given planet 1234 is owned by user 2
    And user 2 has an unlocked relation for object UNIT reference 9100
    And user 2 has an unlocked relation for object TIME_SPECIAL reference 900
    And user 2 has 1 unit of id 11 on planet 1234
    When user 1 runs a CONQUEST mission from planet 1002 to planet 1234 with 5 units of id 10
    Then planet 1234 is owned by user 1
    And table unlocked_relation has a row for user 1 and object UNIT reference 9100
    And table unlocked_relation has no row for user 2 and object UNIT reference 9100
    And user 1 received websocket event "unit_unlocked_change" where some item has id 9100
    And user 2 received websocket event "unit_unlocked_change" where no item has id 9100

  Scenario: Leaving the planet revokes the unlocks
    Given planet 1234 is owned by user 1
    And user 1 has an unlocked relation for object UNIT reference 9100
    When user 1 leaves planet 1234
    Then planet 1234 has no owner
    And table unlocked_relation has no row for user 1 and object UNIT reference 9100
    And user 1 received websocket event "unit_unlocked_change" where no item has id 9100
```

Expected initial result: Java passes everything; Rust fails the establish-base
and conquest **grant** assertions (the documented bug) and passes the revoke
scenario. That asymmetry IS the acceptance test for the harness itself: build
the harness until it reports exactly this.

(**VERIFIED 2026-07-09 against the live dev DB** — the ids above are real:
the plan's original indicative ids collided with baseline content, so they
were replaced. Baseline maxima: `units` 9001, `time_specials` 688,
`special_locations` 202, `planets` 8681. Reserved scenario ranges therefore:
units ≥ 9100, time specials ≥ 900, special locations ≥ 500, missions ≥ 900000.
Do NOT create special location 1 — it exists and is already assigned to planet
1074; unit 55 exists with 5 requirement rows; time special 7 exists with 1.
Confirmed good as-is: user 1 (home planet 1002) and user 2 (home 1004) exist;
planet 1234 exists in galaxy 1, owner NULL, special_location_id NULL — the
perfect target; units 10 (X-302) and 11 (BC-303) are baseline ships with
speed 7. `JWT_SECRET` and `ATTACK_DETERMINISTIC_RNG` configuration rows both
exist. The users table is `user_storage`, not `users`.)

### 6.2 Given steps (`src/steps/given_state.rs`) — SQL seeding

All Givens are deterministic DELETE-then-INSERT with explicit ids (§5.1). Key
tables involved (schema in `business/database/02_schema.sql`):

| Step | Implementation notes |
|---|---|
| `the standard test universe` | No-op marker (the baseline restore already happened). Exists so features read complete. |
| `planet {pid} has special location {slid} and no owner` | Ensure `special_locations` row `{slid}` exists (INSERT IGNORE with a name + galaxy null); `UPDATE planets SET special_location_id={slid}, owner=NULL WHERE id={pid}`. The planet must exist in the baseline galaxy — pick from seeded galaxies. |
| `unit {uid} exists gated by requirement HAVE_SPECIAL_LOCATION with second value {sv}` | 1) INSERT the `units` row (copy a baseline unit's column defaults; fixed id). 2) find-or-create `object_relations` row (`object_description='UNIT'`, `reference_id={uid}`). 3) INSERT `requirements_information` (relation_id, requirement_id of code `HAVE_SPECIAL_LOCATION` from `requirements`, second_value={sv}, third_value NULL). This mirrors what the admin panel writes. |
| `time special {tsid} exists gated by …` | Same pattern against `time_specials` + object_description `TIME_SPECIAL`. |
| `user {u} has {n} units of id {uid} on planet {pid}` | DELETE existing `obtained_units` for (u, uid, pid); INSERT one stack: `user_id, unit_id, count, source_planet={pid}, target_planet NULL, mission_id NULL`, **and `is_from_capture=0` — the column is NOT NULL with no default** (copy the INSERT shape from `seed_deploy.sql`). Register user u for ws capture; register pid/uid in the layer-2 filter. |
| `planet {pid} is owned by user {u}` | `UPDATE planets SET owner={u} WHERE id={pid}`. Registers u for capture. |
| `user {u} has an unlocked relation for object {OBJ} reference {rid}` | Find the `object_relations` id for (OBJ, rid); INSERT `unlocked_relation (user_id, relation_id)`. |

General rules for Givens:

- Never use `NOW()`/autoincrement-dependent logic in a way the differ can see.
- Each Given registers what it touched on `context` (users → ws capture,
  users/planets → the `USERS`/`PLANETS` filter of the layer-2 dump).
- If a Given needs a row the baseline lacks (a requirement code, an object row),
  it creates it idempotently — but audit first whether the baseline already has
  it (`requirements` and `objects` are reference data seeded by
  `04_insert_data.sql`; they DO exist).

### 6.3 When steps (`src/steps/when_actions.rs`) — REST + scheduler

| Step | Implementation notes |
|---|---|
| `user {u} runs a(n) {TYPE} mission from planet {src} to planet {dst} with {n} units of id {uid}` | (W1) Mint JWT for u (`support/rest.rs`, claims per `mint_jwt.py`). POST to the backend's mission endpoint — the same `game/mission/<verb>` routes the frontend uses; find the exact verb per type in `game-rest/.../rest/game/` (Java) and `owge-rest/src/` (Rust); they are already proven equal by mission_verify's REST-POST path. Body: source/target planet ids + involved units `[{id: uid, count: n}]`. Then (W2) read back the created mission id, nudge its `scheduled_tasks.execution_time` into the past (§3 technique #1), and poll `missions.resolved=1` (timeout → loud failure). Record mission id on context. |
| `user {u} runs … with fixed mission id {mid}` | (W3) Combat-RNG variant: skip the REST POST; INSERT the `missions` row with id {mid} + `mission_information` + the db-scheduler row (`task_name='mission-run'`, `task_instance='{mid}'`, `task_data=NULL`) exactly as the `FIXED_MISSION_ID` seeds do (mission_verify README §fixed-seed — copy the INSERT shapes from `seed_attack_partialkill.sql`), then nudge+poll. Use ONLY when the scenario asserts RNG-derived numbers; W1 is closer to production. |
| `user {u} leaves planet {pid}` | REST: the leave-planet endpoint (`PlanetBo.doLeavePlanet` — find the route in `game-rest` `PlanetRestService` and its Rust twin). Synchronous — no scheduler nudge needed. |
| `user {u} activates time special {tsid}` | REST activate endpoint; synchronous. |
| `the {TYPE} mission of user {u} completes` | For multi-mission scenarios: nudge+poll the *latest unresolved* mission of that type for u. |

The FIRST When in a scenario triggers ws-capture startup (§5.3). Every When
that creates a mission also asserts the POST returned 2xx — a 4xx/5xx is a
scenario failure with the response body in the message, not a silent skip.

### 6.4 Then steps — DB (`src/steps/then_db.rs`)

| Step | Implementation notes |
|---|---|
| `planet {pid} is owned by user {u}` / `planet {pid} has no owner` | `SELECT owner FROM planets WHERE id={pid}`. |
| `table unlocked_relation has a row for user {u} and object {OBJ} reference {rid}` (and `has no row`) | JOIN `unlocked_relation` → `object_relations` on relation_id, filter object_description/reference_id/user_id. |
| `table {t} has a row where {col}={v} and …` | Generic escape hatch, restricted to a whitelist of tables (`unlocked_relation`, `obtained_units`, `obtained_upgrades`, `missions`, `planets`, `active_time_specials`). Use sparingly; prefer named steps. |
| `user {u} has {n} units of id {uid} on planet {pid}` (as Then) | SUM(count) over matching `obtained_units`. |
| `user {u} has an obtained upgrade {upid} available` | `obtained_upgrades` row with `available=1`. |

On failure, print the full actual rows of the queried table slice.

### 6.5 Then steps — websocket (`src/steps/then_ws.rs`)

| Step | Implementation notes |
|---|---|
| `user {u} received websocket event "{name}"` | Poll u's capture file (§5.3) for a `deliver` frame with `payload.eventName == name`. |
| `user {u} received websocket event "{name}" where some item has id {id}` | Same + predicate: `payload.value` is a list and `any(item["id"] == id)`. |
| `… where no item has id {id}` | The event must have arrived (list push semantics: these events carry the FULL current list, e.g. `unit_unlocked_change` = all unlocked units — that's why "no item has id X" is meaningful and why it must wait for the frame first). |
| `user {u} received no websocket event "{name}"` | Negative: after the settle window only (assert in after-settle phase, or implement as "wait settle, then assert absence"). |

Payload shape reference: each deliver line is
`{"kind":"deliver","payload":{"eventName":…,"value":…,"lastSent":"<TS>"}}` (see
ws_verify README).

### 6.6 Vocabulary v2 — consolidated from the 2026-07-09 inventory wave

The eight `bdd_parity/inventories/*.md` files each carry a quarantined
"proposed new steps" section. Consolidated result (dedup'd; the per-domain
files keep the full implementation notes — read the relevant one before
implementing a step from this list):

**DESIGN DECISION — negative paths.** Five inventories independently hit the
same wall: W1's "non-2xx fails the scenario" contract makes every
registration-validation branch inexpressible. Resolution: each mutating When
gets an `attempts` variant (`user {u} attempts …`) that stores
`(http_status, body)` on the World instead of asserting, paired with:
- `Then the request is rejected with HTTP status {code}` (strict), and
- `Then the request is rejected with error containing "{marker}"` (loose body
  match — Java's `GameBackendErrorPojo.exceptionType`/message vs Rust's
  `OwgeError` shapes differ by design; exact error-body parity is NOT a goal
  of this harness, HTTP status equality is).
The plain (asserting) When remains the default for happy paths.

**Generic table escape hatch (§6.4) — extend, it's load-bearing:**
`table {t} has a row where <preds>` / `has no row where <preds>` /
`has {n} rows where <preds>`; predicates are an `and`-chain supporting `=`,
`is null`, `is not null`; `missions` gets a `type_code` pseudo-column (join
`mission_types`). Whitelist grows to: missions, planet_list,
visited_tutorial_entries, user_read_system_messages, mission_reports,
system_messages, user_storage, obtained_unit_temporal_information,
explored_planets, alliances, alliance_join_requests. The **row-COUNT variant
is the single most important addition** — it is the only way to catch the
confirmed Java-dups-vs-Rust-dedups divergences (tutorial visited entries,
system-message reads).

**Accepted new Givens:** `configuration "{name}" is "{value}"` (upsert);
`user {u} has {p} primary resource and {s} secondary resource`;
`user {u} has obtained upgrade {upid} at level {lvl} available|unavailable`;
`user {u} has no unlocked relation for object {OBJ} reference {rid}`;
`user {u} has explored planet {pid}`; `alliance {aid} exists owned by user {u}`;
`user {u} is a member of alliance {aid}`; generalize every `… exists gated by
requirement …` Given to ANY requirement code (validate against `requirements`)
and add the UPGRADE variant; `time special {id} has duration {d} seconds and
recharge time {r} seconds`; combat extras (capture-rule forcing via
`rules.extra_args`, `owner_unit_id` carrier linkage — see missions-combat.md).
New id reservations: **users ≥ 9000**, tutorial entries / system messages
≥ 900, reports ≥ 900000 (shared with missions).

**Accepted new Whens:** `user {u} cancels their latest mission` (+ cross-user
rejection variant); `the {TYPE} mission of user {u} completes` (nudge+poll,
already in §6.3); `user {u} cancels their build mission on planet {pid}`;
`user {u} disbands {n|all} units of id {uid} on planet {pid}`;
`user {u} registers a LEVEL_UP mission for upgrade {upid}` and
`user {u} cancels the running upgrade mission` (GET-shaped, no unit body);
`user {u} activates time special {tsid}`; multi-stack W1 extension
(`… and {m} units of id {uid2} from planet {src2}`) for asymmetric combat;
alliance/planet-list/tutorial/report/system-message Whens (see
planet-user-misc.md, alliance.md); `user {u} subscribes to faction {fid}`
(GET that mutates!).

**Accepted new ws Thens (non-list payloads):** `… where the payload has id
{id}` (single-object values like planet_explored_event);
`… where value has upgrade id {id}` (running_upgrade_change);
`… where some item has upgrade id {id} and level {lvl}`
(obtained_upgrades_change — top-level item id is the surrogate!);
`… with null value` (running_upgrade_change on completion/cancel).

**Backend-conditional nudges (time-specials only):** `the effect of time
special {tsid} for user {u} ends` and `the recharge of time special {tsid} for
user {u} completes` — Java drives these through **Quartz** (`QRTZ_TRIGGERS.
NEXT_FIRE_TIME`, ≥45 s poll budget), Rust through `scheduled_tasks`; the step
body dispatches on the backend. Full SQL in time-specials.md §4. This is the
one sanctioned deviation from "steps don't know the backend" — the observed
outcome stays backend-agnostic, only the nudge mechanism differs.

**Special completion semantics:** BUILD_UNIT missions are hard-DELETED on
completion (never `resolved=1`) — their completion step polls for the mission
row's disappearance + the obtained_units grant, not for `resolved`.

**Rejected/parked:** audit-cookie assertion (dev plumbing, out of scope);
`deliver-backdoor` scenarios (unported debug tool); a raw-SQL unlock-revoke
Given (must go through a real backend path — needs Kevin's sign-off on which;
see time-specials.md §4 last row).

---

## 7. Runner, reporting, and exit criteria

`run_bdd_parity.sh [--feature f] [--scenario name-substring] [--backend java|rust|both] [--keep-artifacts]`

- `--backend java` alone is the "is the spec right?" mode (validate new
  scenarios against the reference before ever involving Rust).
- Output per scenario, exactly this shape (grep-able, diff-able):

```
▶ special_location_unlock :: Establish base grants the unlocks to the new owner
  JAVA_SPEC  ✅
  RUST_SPEC  🔴  Then table unlocked_relation has a row for user 1 and object UNIT reference 55
                 actual rows for user 1: []
  PARITY     🔴  table unlocked_relation: Java has 2 rows Rust has 0
                 ws user 1: Java emitted unit_unlocked_change, Rust did not
```

- Summary: matrix of scenario × (JAVA_SPEC, RUST_SPEC, PARITY); exit 1 on any
  red. Artifacts dir path printed at the end.
- Runtime budget: a scenario is restore + 2 backend passes + polling — expect
  ~30–90 s each. That is fine for a nightly/per-change suite; do NOT
  prematurely parallelize (parallel = two DBs = new topology = new bugs).

---

## 8. Phased roadmap

**Phase 0 — skeleton (goal: the pipeline exists).**
`bdd_parity/` scaffolding, `support/` modules, runner script that can restore
the baseline, boot/talk to both backends, run one trivial scenario
(`Given user 1 has 5 units… / Then user 1 has 5 units…` — no When) with both
layers wired. Deliverable: `run_bdd_parity.sh` prints the §7 report for the
trivial scenario, all green.

**Phase 1 — the special-location feature (goal: harness catches a real bug).**
Implement §6.1 exactly. Acceptance: Java all green; Rust red on the two grant
scenarios with the `unlocked_relation` assertion + PARITY diff, green on
revoke. THEN fix the bug per `BUG-SPECIAL-LOCATION-UNLOCK.md` §"Proposed fix"
and watch the suite go green — that closes the loop and proves the harness
end-to-end. (Order matters: harness first, fix second — the red run is the
harness's own regression test.)

**Phase 2 — port the existing seeds into features.**
One feature per mission processor, translating `seed_conquest.sql`,
`seed_deploy.sql`, `seed_gather.sql`, `seed_capture.sql`,
`seed_counterattack.sql`, `seed_interception.sql`, `seed_build.sql`,
`seed_levelup.sql`, `seed_reqtrigger.sql`, `seed_temporal_units.sql`,
`seed_deleteuser.sql` into Given-vocabulary + explicit Thens (the seeds'
comments say what they exercise). Combat scenarios use the fixed-mission-id
When (W3) + trace diff. Deliverable: mission_verify's coverage is a subset of
the BDD suite; mission_verify can then be frozen (keep it — it's the low-level
debugging tool).

**Phase 3 — divergence-hunting scenarios.**
Scenarios for flows with NO current coverage: leave planet, upgrade level-up
requirement cascades (`UPGRADE_LEVEL_LOWER_THAN` revocation!), time-special
activate/expire cascade (`HAVE_SPECIAL_ENABLED`), REQUIREMENT_GROUP
master/slave unlocks, unit amount thresholds on kill (UNIT_AMOUNT), alliance
cases in conquest, max-planets edge, home-planet conquest rejection. Each is a
place the port has plausibly diverged; the layer-2 diff will do the finding.

**Phase 4 — gate the SeaORM migration.**
Rule: no SeaORM refactor lands unless the full suite is green before AND after.
The suite is the migration's contract; that's the payoff of black-box design.

---

## 9. Pitfalls & environment notes for the implementer

1. **Stale `/public/owge` paths** in older harness docs/scripts — the repo root
   is `/home/kevin/projects/owge`. Fix path variables, not the docs' logic.
2. **Node 14 only for `ws_capture.js`**, via `source ~/.nvm/nvm.sh && nvm use
   14`; `socket.io-client@2.4.0` lives in `/tmp/wsclient/node_modules` (re-`npm
   install socket.io-client@2.4.0` there if the machine was cleaned). The
   driver just needs `node` resolvable — wrap capture startup in a shell
   that sources nvm.
   **VERIFIED 2026-07-09:** `/tmp/wsclient` WAS cleaned — recreate it first.
   nvm has v14.21.3 installed. `python3` is available (needed only for the
   shared differ scripts, which are stdlib-only — behave/PyMySQL are NOT
   needed since the §2.2 revision; `mint_jwt.py` needs PyJWT but is now only
   a debugging cross-check, not part of the pipeline). Already in place: the
   dev DB container
   `owge_backend_developer-db-1` (port 3306, db `owge`, root/1234), the
   `owge-java-compare:latest` image for booting the Java backend, and a built
   `rust-backend/target/debug/owge-rest`.
3. **No local mvn/JDK** — the Java backend runs as a container (mission_verify
   boots it; reuse that). Rust builds natively with cargo.
4. **WSL2: NEVER search/scan under `/mnt`** (it hydrates a OneDrive). Keep all
   artifacts under `/tmp`.
5. **Websocket events are full-list pushes**, not deltas (`unit_unlocked_change`
   carries ALL currently-unlocked units). Assertions and diffs must treat them
   as such (§6.5).
6. **Post-commit async emission** — always poll-with-timeout for ws frames
   (§5.3), never assert on a single immediate read; always settle-wait before
   killing captures.
7. **db-scheduler poll interval** — the nudge technique's latency is the
   scheduler's polling cadence; the 40 s mission-resolution timeout accounts
   for it. If a poll times out, FIRST check the backend actually claimed the
   task (`scheduled_tasks.picked`), and that the `execution_time` UPDATE
   matched a row (`ROW_COUNT()`).
8. **JWT**: minted from the `JWT_SECRET` row of `configuration`; token per
   user id; Java context path `/game_api`, ws path `/websocket/socket.io`;
   Rust ws path `/socket.io` (ports per env, see harness READMEs).
9. **Auto-increment drift is normal** — REST-created missions get different ids
   per backend pass; the differs already strip surrogate ids. Never assert a
   raw autoincrement id in a Then; assert business keys.
10. **One scenario = one universe-state story.** If you find yourself wanting a
    scenario to depend on the previous scenario's end state, split it and seed
    the intermediate state via Givens instead — scenarios must stay
    independently runnable (the reset guarantees it; don't fight it).
11. **When Java and Rust are BOTH wrong** (spec-level bug in the reference):
    the JAVA_SPEC verdict fails. Do not "fix" the scenario to match Java
    blindly — flag it to Kevin; the Java behavior is the *default* spec, not an
    infallible one.
12. **The Rust backend caches per-connection/user state**; if REST reads in a
    Then look stale after external SQL writes, see §5.2 cache mitigation
    ladder before blaming the backend.

---

## 10. Relationship to the existing harnesses (keep, freeze, or fold?)

- `ws_verify` — **keep as-is.** It is the low-level tool for read-side payload
  parity and is already at full parity on the rich seed; the BDD harness covers
  the *event-emission* side of writes instead.
- `mission_verify` — **keep until Phase 2 completes**, then freeze (no new
  seeds; new coverage goes into features). Its `dump_mission_state.sh` /
  `table_diff.py` / `trace_diff.py` become shared libraries of the BDD harness
  and stay maintained.
- `BUG-SPECIAL-LOCATION-UNLOCK.md` — its "Proposed fix" section is executed in
  Phase 1, *after* the feature file reproduces it red.
```
