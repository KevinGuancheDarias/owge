# bdd_parity — executable Gherkin specs run against BOTH backends

Implementation of `rust-backend/docs/BDD-PARITY-PLAN.md` (read that first —
all design decisions live there). Scenarios in `features/*.feature` are
executed black-box against the Java reference and the Rust port; each scenario
gets three verdicts: `JAVA_SPEC`, `RUST_SPEC` (the explicit Then steps) and
`PARITY` (implicit full table-state + websocket diff).

## Prerequisites

- The dev DB container `owge_backend_developer-db-1` running (db `owge`,
  root/1234).
- The `owge-java-compare:latest` image (built by ws_verify/mission_verify
  work; boots the Java backend on the DB's docker network).
- A built Rust backend: `cargo build` in `rust-backend/`.
- Node 14 via nvm + `/tmp/wsclient` with `socket.io-client@2.4.0`
  (`source ~/.nvm/nvm.sh && nvm use 14 && mkdir -p /tmp/wsclient &&
  cd /tmp/wsclient && npm install socket.io-client@2.4.0`).
- `python3` on PATH (stdlib only — used for the shared differ scripts in
  `../mission_verify/`).

## Run

```bash
./run_bdd_parity.sh                                  # everything, stop at first red
./run_bdd_parity.sh --keep-going                     # everything, full report
./run_bdd_parity.sh --feature special_location       # one feature (substring match)
./run_bdd_parity.sh --scenario "Establish base"      # one scenario (substring match)
./run_bdd_parity.sh --backend java                   # spec-validation mode (no Rust, no diff)
```

Artifacts land in `/tmp/bdd_parity_runs/<timestamp>/<feature>/<scenario>/{java,rust}/`
(driver log, `tables.json`, `ws_user<id>.jsonl`, diffs). Never in the repo.

The runner keeps **one backend awake at a time** (Java container paused during
the Rust pass, Rust killed during the Java pass and restarted fresh after every
restore) because both poll the same `scheduled_tasks` table and would steal
each other's nudged missions.

## Add a scenario

1. Write it in an existing/new `features/*.feature` using ONLY the step
   vocabulary in `src/steps/` (plan §6). Concrete ids, no business fluff
   (plan §2.5); scenario-created content ids in the reserved ranges: units
   ≥ 9100, time specials ≥ 900, special locations ≥ 500, missions ≥ 900000.
2. If a new step is genuinely needed, add it to the matching `src/steps/*.rs`
   module — Givens are deterministic DELETE-then-INSERT SQL, Whens go through
   REST + the scheduler nudge, Thens print ACTUAL rows on failure.
3. Validate against the reference first: `./run_bdd_parity.sh --scenario
   "<name>" --backend java`. Only then involve Rust.

## Crate layout

Standalone crate (own `[workspace]`), **never** add an `owge-*` dependency —
the backends stay black boxes (plan §2.2). `src/main.rs` = cucumber runner
(one backend pass per invocation, `OWGE_BDD_BACKEND=java|rust|none`);
`src/world.rs` = typed per-scenario state; `src/support/` = db/rest/ws/backend
plumbing; `src/steps/` = the step vocabulary.

`inventories/` holds the per-domain Java-reference behavior inventories
(endpoint surveys, behavior catalogs B1..Bn, draft scenarios, proposed steps)
produced during the 2026-07 fan-out — the source material for Phase 2/3
features.
