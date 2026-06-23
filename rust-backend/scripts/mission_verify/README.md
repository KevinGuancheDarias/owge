# mission_verify — Rust vs Java mission-state parity harness

The database-state analogue of [`../ws_verify/`](../ws_verify/README.md) (which
diffs the websocket/REST sync payloads). Where `ws_verify` proves the two backends
*emit* the same events, `mission_verify` proves they *leave behind* the same
database state after running a mission — especially an **ATTACK** and the
RNG-driven combat it triggers.

It implements Parts 5–7 of `../../pending_migration/ATTACK_PARITY_PLAN.md`.

## What it does

For each scenario seed it runs the **same mission** on the Java backend and the
Rust backend, **sequentially**, against the **one shared `owge` dev DB**, using a
snapshot/restore so the two engines never touch the same rows at once, then diffs:

1. the resulting **table state** (`table_diff.py`), and
2. the **RNG draw trace** each backend emits (`trace_diff.py`).

## Topology: shared DB, sequential snapshot/restore

Both backends point at the single dev MySQL (`owge_backend_developer-db-1`, DB
`owge`). The two universes `sgalactica_java_13/14` the seed comments mention do
**not** exist here, so we isolate the engines on one DB per scenario:

```
1. set configuration.ATTACK_DETERMINISTIC_RNG = 'TRUE'
2. apply the scenario seed SQL
3. SNAPSHOT the in-scope tables   (mysqldump of just those tables)
4. run the mission on JAVA        -> dump table state + RNG trace
5. RESTORE the snapshot           (back to post-seed, pre-mission state)
6. run the mission on RUST        -> dump table state + RNG trace
7. table_diff.py  JAVA RUST   +   trace_diff.py  JAVA RUST
```

The Java backend is booted in a container on the DB's docker network exactly as
`ws_verify/compare_rest_sync.sh` does (context path `/game_api`, JWT minted HS256
from the `JWT_SECRET` config row, `OWGE_WS_SYNC_RATELIMIT_PER_MINUTE=0`); the Rust
backend runs natively (`OWGE_DB_JDBC_URL=mysql://root:1234@127.0.0.1:3306/owge
OWGE_SERVER_PORT=8080 ./target/debug/owge-rest`). The Java container is torn down
on exit (`trap`).

## Fixed-seed (same mission id) mode — the same-seed reproducibility proof

The RNG seed is `mission.id`. The default REST-POST path (below) creates the mission
on each backend **separately**, so Java's mission gets one auto-increment id and
Rust's gets another — **different seeds**, so `result`/`seed` can't be compared and
same-seed reproducibility stays untested. To close that, a seed file may declare on
its **first line**:

```sql
-- FIXED_MISSION_ID=900001
```

and then `INSERT` the `missions` row **with that fixed id** plus its db-scheduler
`scheduled_tasks` row (`task_name='mission-run'`, `task_instance='<id>'`,
`task_data=NULL` — Java uses `TaskWithoutDataDescriptor`, Rust parses
`task_instance` as the mission id, so a hand-inserted row fires on **both**). When
the runner sees the marker it **skips the REST POST entirely** and just nudges that
pre-seeded task's `execution_time` into the past to fire it. Because the snapshot is
taken **after** the seed, Java and Rust both restore and fire the **same** mission
id ⇒ the **same** `JavaRandom` seed ⇒ the RNG `result`/`seed` are comparable. On
this path the trace diff runs with **`--strict`** (compares every field including
`seed` and `result`), giving a bit-for-bit same-seed proof.

`scripts/seed_attack_partialkill.sql` is the canonical fixed-seed scenario: two
unequal attacker stacks (7× / 2× unit 10 from planets 1002 / 1005) vs a defender
force that leaves exactly **one** fractional survivor, so the shuffle order decides
which stack the survivor belongs to (its `source_planet`). Proven seed-sensitive by
re-running with two different fixed ids (900001 → survivor src 1005; 900007 →
survivor src 1002), and proven same-seed-identical (Java vs Rust, 9/9 tables +
strict trace) at each id.

## How a mission is triggered (default / REST-POST path)

The seeds set up units/planets only — not the mission row. The harness **creates**
the mission with a REST `POST game/mission/<verb>` to the backend under test
(mirroring the frontend; this exercises `buildAttackInformation`/`addUnit`
ordering identically to production and works the same on both backends), which
inserts the `missions` row **and** the db-scheduler `scheduled_tasks` row. It then
forces that task's `execution_time` into the past (the technique from
`ws_verify/README.md`) so the `OWGE_BACKGROUND` scheduler fires it immediately,
and polls until `missions.resolved = 1`.

> Alternative considered: seed the `missions` row directly. REST POST is used so
> the attacker-stack assembly order is the production order on both sides.

## Files

| File | Role |
|---|---|
| `dump_mission_state.sh <db> <out.json>` | Dump the mission-footprint tables to normalised JSON. |
| `table_diff.py JAVA.json RUST.json` | Normalise (strip surrogate ids/timestamps, parse report JSON) and deep-diff per table; `✅`/`🔴`. |
| `trace_diff.py JAVA.jsonl RUST.jsonl` | Align two RNG traces by `seq`, report the FIRST divergence. |
| `run_mission_parity.sh [seed.sql …]` | Orchestrate the whole pipeline; loop over all combat seeds if none given. |

## Tables in scope (the mission footprint)

`dump_mission_state.sh` dumps, filtered to the test users (`1,2`) / planets
(`1002,1003,1004,1005`) where the table has such a column:

- **obtained_units** — the unit stacks (counts, source/target planet, capture flag)
- **missions** — the mission rows (type, resources, planets, resolved)
- **mission_reports** — the combat report (`json_body` parsed for diffing)
- **mission_information** — per-mission relation values
- **user_storage** — only `points, primary_resource, secondary_resource, energy`
- **planets** — `owner, home` (conquest reassignment)
- **unlocked_relation** — unlocks
- **active_time_specials** — active time specials (`state`)
- **scheduled_tasks** — return-mission scheduling (names only; the per-row
  instance uuid is volatile)

### Normalisation (`table_diff.py`)

Surrogate ids and legitimately-differing timestamps are stripped before diffing;
RNG-derived combat numbers are **kept** (they are deterministic under
`ATTACK_DETERMINISTIC_RNG`):

- `obtained_units`: drop `id, mission_id, first_deployment_mission,
  expiration_id, owner_unit_id`.
- `missions`: drop `id, report_id, related_mission`.
- `mission_reports`: drop `id`; parse `json_body` and recursively drop
  `id/date/*Date/missionId/reportId` while keeping every combat number
  (counts, captured, killed, survivors, pointsGiven, …).
- `scheduled_tasks`: compare the multiset of `task_name` (drop `task_instance`).
- everything else: drop `id`.

Rows are sorted by a stable business key so DB row-order never causes a false
diff. The structural `norm`/`short` helpers are reused from
`../ws_verify/rest_sync_diff.py`.

## RNG trace schema (agreed with the combat-instrumentation agent, Part 4)

When `ATTACK_DETERMINISTIC_RNG=TRUE` and tracing is on, **both** backends emit one
JSON object per RNG draw, one per line, to their log/stderr. Identical fields on
both sides:

```json
{"seq": 0, "site": "shuffle",        "seed": 500,  "bound": 2,    "attacker": null, "victim": null, "killed": null, "result": 1}
{"seq": 1, "site": "capture_prob",   "seed": null, "bound": null, "attacker": 101,  "victim": 202,  "killed": null, "result": 0.4213}
{"seq": 2, "site": "capture_amount", "seed": null, "bound": null, "attacker": 101,  "victim": 202,  "killed": 3,    "result": 0.91}
```

| field | meaning |
|---|---|
| `seq` | monotonically increasing draw index (the alignment key) |
| `site` | `"shuffle"` \| `"capture_prob"` \| `"capture_amount"` |
| `seed` | the `JavaRandom` seed (mission id); set on the `shuffle` site, else `null` |
| `bound` | `nextInt` bound (shuffle swap), else `null` |
| `attacker` | attacking `obtained_units.id` (capture sites), else `null` |
| `victim` | victim `obtained_units.id` (capture sites), else `null` |
| `killed` | units killed feeding the capture-amount draw (`capture_amount` only) |
| `result` | the draw output — shuffle swap index, or `nextDouble()` for capture |

`trace_diff.py` aligns by `seq`, compares field by field, and prints the **first**
mismatch with both full lines. It is **robust to a missing/empty trace** (the
instrumentation is produced by a separate agent and may not be wired yet): if
neither side emits a trace it prints `no trace emitted yet` and exits 0 so the
table-diff path still gates; if only one side emits, it flags `🟡` and exits 0.

## Usage

```bash
# all combat/mission seeds
./run_mission_parity.sh

# one scenario (the asymmetric partial-kill case — only matches with seeded RNG)
./run_mission_parity.sh seed_attack_asymmetric.sql

# just dump + diff two states by hand
./dump_mission_state.sh owge /tmp/java.json
./dump_mission_state.sh owge /tmp/rust.json
python3 table_diff.py /tmp/java.json /tmp/rust.json --a-label Java --b-label Rust
```

Env overrides: `RUST_PORT` (8080), `JAVA_HOST_PORT` (8099), `DB_CONTAINER`,
`DB_NAME`, `DB_PASS`, `USERS` (`1,2`), `PLANETS`, `POLL_SECONDS` (40), `SEED_DIR`.

## Scenario seeds

Combat seeds live one level up in `../` (shared with the rest of the suite):
`seed_attack`, `seed_attack_asymmetric` (**new**), `seed_capture`,
`seed_conquest`, `seed_counterattack`, `seed_deploy`, `seed_gather`,
`seed_interception`. The non-combat seeds (`seed_build`, `seed_levelup`,
`seed_reqtrigger`, `seed_temporal_units`, `seed_deleteuser`) are skipped by the
default loop.

### `seed_attack_asymmetric.sql` — the case that REQUIRES seeded RNG

Attacker user 1 sends **two unequal stacks** of the same unit (7× from planet
1002, 2× from 1005) at defender 1003, whose force is sized to kill 8 of the 9
attackers — leaving **one** fractional survivor. `shuffleUnits` randomises which
stack absorbs the partial hit, so which stack the survivor belongs to (and which
`source_planet` it returns to) is RNG-dependent:

- **without** seeded RNG: Java's `Collections.shuffle` and Rust's xorshift pick
  different orders → `obtained_units` survivor rows diverge.
- **with** `ATTACK_DETERMINISTIC_RNG=TRUE` (mission-id-seeded `java.util.Random`
  in both): identical shuffle → identical survivor → `table_diff` matches.

## Status / caveats

- `ATTACK_DETERMINISTIC_RNG` and the RNG trace are produced by a **different
  agent's** combat-code changes (ATTACK_PARITY_PLAN Parts 2/4) which may not be
  wired yet. Until then the RNG trace will be empty and `trace_diff.py` reports
  `no trace emitted yet`; the **table-diff path works regardless** and will simply
  show combat divergences for any RNG-sensitive scenario (expected pre-seeding).
- `run_mission_parity.sh` sets `configuration.ATTACK_DETERMINISTIC_RNG='TRUE'`
  (upsert) itself, so it is ready the moment the flag is honoured.
- Seeds assume a clean test universe (they delete users 1,2). On a dev DB where
  users 1,2 own other rows (e.g. `obtained_upgrades`, extra planets) a seed may
  fail its `DELETE FROM user_storage`; clear those refs first or run against a
  fresh universe.
```
