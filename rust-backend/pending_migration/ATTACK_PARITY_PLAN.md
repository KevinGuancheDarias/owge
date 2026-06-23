# Attack parity plan — bit-for-bit Java vs Rust combat

**Goal.** Guarantee the Rust attack engine produces the *exact same* mission-table
state as Java for the same units attacking the same planets, and prove the Rust
port is not missing any attack feature. Achieved by (B) porting Java's RNG so any
scenario is reproducible across both backends, then diffing both the **RNG draw
trace** and the **resulting tables**.

This is the mission-state analogue of `scripts/ws_verify/` (which diffs the
websocket/REST sync payloads). See [[ws-sync-rust-vs-java-harness]].

---

## Why this is tractable: the entire combat randomness surface is 3 draws

Confirmed by audit of `business/.../mission/attack/`:

| Draw | Java site | Rust site (today) |
|------|-----------|-------------------|
| Targeting order | `AttackObtainedUnitBo.shuffleUnits` → `Collections.shuffle(units)` | `attack_mission_manager_bo.rs` `shuffle_indices` (xorshift64, nanos-seeded) |
| Capture probability | `HandleUnitCaptureListener:46` `Math.random()*100 < prob` | `next_unit_f64(rng_state)` (xorshift64, nanos-seeded) |
| Capture amount | `HandleUnitCaptureListener:48` `floor(Math.random()*floor(killed*pct)+1)` | same xorshift |

**Critical attacks are deterministic** (no RNG in `CriticalAttackBo` /
`AttackRuleBo`) — they only sort targets by descending critical score. So the only
thing standing between us and full reproducibility is these three draws plus the
*order* they are consumed in.

Today both backends seed from wall-clock and use different PRNGs, so outcomes are
non-reproducible even within one backend. The plan makes both consume one shared,
mission-derived seed through a byte-identical `java.util.Random`.

---

## Part 0 — Feature-parity audit (do FIRST; this is the "nothing missing" check)

Before touching RNG, produce a checklist mapping every Java attack path to its Rust
counterpart and flag gaps. Source of truth on the Java side:

- `AttackMissionManagerBo` — `buildAttackInformation`, `addUnit`, `startAttack`,
  `doAttack`, `attackTarget`/`addPointsAndUpdateCount`, `updatePoints`, the
  per-user post-combat emit block, conquest owner reassignment.
- `AttackObtainedUnitBo.create` — per-stack attack/shield/health computation
  **including improvement multipliers** (`ImprovementBo.findAsRational` of
  ATTACK/SHIELD/DEFENSE unit-type improvements). This is a silent-divergence risk:
  if Rust's improvement math differs, kills differ even with identical RNG.
- `HandleUnitCaptureListener` — `AfterUnitKilledCalculationListener` (capture roll)
  + `AfterAttackEndListener` (capture report fan-out, `unitCaptureInformation`).
- `AttackBypassShieldService` — own `bypassShield` flag **OR** active
  `TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD` rule owned by source targeting victim.
- `AttackEventEmitter` — `emitAttackEnd`, report construction.
- The attack-rule recursion (`AttackRuleBo.findAttackRule` up the unit-type parent
  chain) and `can_attack` filtering.
- Config gating: `MISSION_<TYPE>_TRIGGER_ATTACK` per mission type.

Rust counterparts to verify line-by-line: `attack_mission_manager_bo.rs`,
`mission_processor/{attack,conquest,counterattack}.rs`, `critical_attack_bo.rs`,
`attack_rule_bo.rs`. Output: a table in this doc, `✅ ported` / `⚠ differs` /
`❌ missing`, with the divergence noted. Known things to scrutinize:
- carrier units (`unitsStoringUnits` / `ownerUnit`) freeing on wipe,
- `OwgeElementSideDeletedException` handling in `saveWithChange`,
- the `usersWithDeletedMissions` vs `usersWithChangedCounts` emit split,
- alliance enemy resolution (`allianceBo.areEnemies`),
- points accrual (`earnedPoints`, `addPointsToUser`).

The trace + table diffs in Parts 4–5 will *empirically* catch anything the manual
audit misses — but the audit gives us a checklist to reason about coverage.

---

## Part 1 — `JavaRandom`: byte-exact port of `java.util.Random`

New module `owge-business/src/bo/java_random.rs`. `java.util.Random` is
**contractually specified** (stable across all JVMs):

```
seed0   = (seed ^ 0x5DEECE66D) & ((1<<48)-1)        // constructor scramble
next(b) = seed = (seed*0x5DEECE66D + 0xB) & mask48; return (seed >> (48-b)) as i32
nextInt(bound):
    if (bound & -bound) == bound  -> (bound * (next(31) as i64) >> 31) as i32  // pow2
    else rejection loop: bits=next(31); val=bits%bound; while bits-val+(bound-1)<0 retry
nextDouble() = (((next(26) as i64) << 27) + next(27)) as f64 / (1u64<<53) as f64
```

Plus `Collections.shuffle(list, rnd)`:
```
for i in (2..=size).rev(): swap(list, i-1, rnd.nextInt(i))   // i = size down to 2
```

**Verification:** a tiny throwaway Java program prints `new Random(SEED)` →
N `nextInt(bound)`, N `nextDouble()`, and a shuffle of `[0..k)`; assert the Rust
`JavaRandom` reproduces them exactly for several seeds/bounds. Commit the golden
vectors as a Rust unit test so it can never regress.

---

## Part 2 — Seeded deterministic mode, gated in BOTH backends

**Seed source:** the **mission id** (`mission.id`) — deterministic, shared by both
backends for the same scenario, stable across reruns. (Optionally XOR a fixed base
constant; mission id alone is sufficient.)

**Gate:** a config flag so production stays time-random. Proposed
`configuration` row `ATTACK_DETERMINISTIC_RNG` (default `FALSE`). When `TRUE`:
- **Java:** in `startAttack`, build `var rnd = new Random(seed)` once; thread it
  into `attackObtainedUnitBo.shuffleUnits(units, rnd)` and into
  `HandleUnitCaptureListener` (replace both `Math.random()` with `rnd.nextDouble()`).
  Threading: carry the `Random` on `AttackInformation` (it already flows through
  the listeners via `information`). When the flag is `FALSE`, keep
  `Collections.shuffle(units)` / `Math.random()` exactly as today.
- **Rust:** replace `rng_state: u64` (xorshift) with a `JavaRandom` seeded from the
  same mission id when the flag is set; keep `shuffle_indices` only as the
  fallback. The shuffle must call `JavaRandom::shuffle` on the same logical list.

This is the only production-code change, and it is inert unless the flag is on.

---

## Part 3 — Lock-step consumption (the real work)

Bit-exact outcomes require both engines to advance the seed with the **same calls
in the same order**. Three invariants, in decreasing ease:

1. **Shuffle input order must match.** `shuffle` output depends on the RNG sequence
   *and* the list's initial order. So `buildAttackInformation` must assemble
   `units` in the identical order in both backends: Java does
   `findInvolvedInAttack(targetPlanet)` then `findByMissionId(attackMission)`.
   Verify Rust's two queries use the same `ORDER BY` and the same add order
   (`addUnit`). This is the same path-dependent-ordering discipline ws-sync needed.
2. **Capture rolls must fire on the same (attacker→victim) pairs in the same
   order.** A draw only happens when a capture rule exists for the pair. Both
   engines must iterate attackers in shuffled order × scored targets in the same
   order, and evaluate the rule at the same point (Rust already documents it
   mirrors "Java's call order inside `addPointsAndUpdateCount`" — under this plan
   that becomes load-bearing for correctness).
3. **`nextInt`/`nextDouble` bit-exactness** from Part 1.

Part 4's trace makes any violation here immediately visible and localized.

---

## Part 4 — RNG trace instrumentation + trace diff (primary debugging tool)

When `ATTACK_DETERMINISTIC_RNG=TRUE` and a trace flag is on, both backends emit one
line per draw to a trace sink (stderr/file):

```
{"seq":0,"site":"shuffle","seed":<seed>,"bound":<i>,"result":<j>}
{"seq":1,"site":"capture_prob","attacker":<ouId>,"victim":<ouId>,"result":<double>}
{"seq":2,"site":"capture_amount","attacker":...,"victim":...,"killed":<n>,"result":<double>}
```

`scripts/mission_verify/trace_diff.py` aligns the two streams by `seq` and reports
the **first divergence** (which draw, which site, which pair). Because there are so
few draws, this pinpoints the exact combat step where the engines part ways — far
stronger than only diffing final tables. This is the workhorse during bring-up.

---

## Part 5 — Table-state diff harness (`scripts/mission_verify/`)

The mission analogue of `ws_verify`. Per scenario:

1. Apply the scenario seed SQL to both DBs (Java + Rust universes), with
   `ATTACK_DETERMINISTIC_RNG=TRUE` set in `configuration`.
2. Create the mission (REST `POST` to each backend, or seed the `missions` row),
   then force `scheduled_tasks.execution_time` into the past so the scheduler fires
   it (the technique already used in `ws_verify/README.md`).
3. Wait for completion, then dump the canonical tables from each DB.
4. Normalize and deep-diff.

**Tables in scope** (the full mission footprint):
`obtained_units`, `missions`, `mission_reports` (+`mission_information`),
`user_storage` (points / primary / secondary / energy), `planets` (owner/home on
conquest), `unlocked_relation`, `active_time_specials`, `scheduled_tasks`
(return-mission scheduling).

**Normalization:** strip surrogate ids and legit timestamps (`mission.id`,
`report.id`, created/updated, `execution_time`); sort id-keyed rows; **keep**
RNG-derived counts (captured counts, survivor counts) since they are now
deterministic. `mission_reports` JSON needs field-level normalization (drop
timestamps/ids, keep the combat numbers). Reuse `rest_sync_diff.py`'s normalize
helpers where possible.

---

## Part 6 — Scenario matrix (seed_*.sql; many already exist)

Existing seeds to reuse/extend: `seed_attack`, `seed_capture`, `seed_conquest`,
`seed_counterattack`, `seed_deploy`, `seed_gather`, `seed_interception`,
`seed_temporal_units`, `seed_reqtrigger`, `seed_build`, `seed_levelup`,
`seed_deleteuser`.

**Mission types that can drive an attack:** `ATTACK`, `CONQUEST`, `COUNTERATTACK`,
plus any type with `MISSION_<TYPE>_TRIGGER_ATTACK=TRUE` (`GATHER`, `DEPLOY`,
`ESTABLISH_BASE`, `EXPLORE`, `RETURN_MISSION` as configured).

**Combat variety to cover** (each should desync without Part 1, match with it):
- symmetric 10v10 (order-independent baseline),
- **asymmetric partial-kill** (the case that REQUIRES seeded RNG — different
  shuffle order changes which stack absorbs the partial hit),
- multi-stack / multi-user / alliance enemies,
- critical-attack scoring (deterministic, but exercises the sort),
- captures (probability + amount draws — `seed_capture` already forces them),
- shield bypass (own flag) and time-special bypass,
- carrier units holding units (`unitsStoringUnits`),
- conquest owner reassignment,
- overkill, exact-kill, and mission-emptying/deletion edges.

---

## Part 7 — Runner

`scripts/mission_verify/run_mission_parity.sh` — boots the Java compare image (as
`compare_rest_sync.sh` does) and the Rust backend, then for each scenario runs the
seed → fire → trace-diff → table-diff pipeline and prints a per-scenario,
per-table `✅ / 🔴` report plus the first RNG divergence if any.

---

## Open decisions (confirm before building)

1. **Seed source** = `mission.id` alone — OK? (deterministic + shared; reruns
   stable).
2. **Gate** = new `configuration.ATTACK_DETERMINISTIC_RNG` flag (production
   untouched) — OK, or prefer an env var?
3. **Topology** = two universes (Java `dc13` / Rust `dc14`, as the existing seeds
   assume) vs. run both backends sequentially against one shared DB snapshot. Two
   universes matches existing seeds; single-DB-sequential avoids divergent base
   data. Recommend: two universes seeded from the **same** snapshot.

## Implementation status (2026-06-23)

- **Part 0 — audit:** done → `ATTACK_FEATURE_AUDIT.md`. Port functionally complete;
  found **D0** (Rust summed every ancestor unit-type improvement unconditionally vs
  Java's `has_to_inherit_improvements` gate — outcome-affecting) and **D2** (capture
  type-chain lookup order). Both fixed.
- **Part 1 — `JavaRandom`:** done → `owge-business/src/bo/java_random.rs`, golden-tested
  against real JDK 21 (`cargo test -p owge-business java_random` green).
- **Part 2 — seeded gate:** done both backends. Flag = `configuration.ATTACK_DETERMINISTIC_RNG`
  (default FALSE, production untouched). Seed = `mission.id`. One RNG per attack.
- **Part 3 — lock-step ordering:** Rust defender query had **no `ORDER BY`** → different
  shuffle-input list than Java's category-ordered concatenation (planet-resident →
  DEPLOYED → CONQUEST≥10%, then attacker `findByMissionId`). Fixed in
  `mission_processor/attack.rs` + the defenders SQL. Final convergence pending the
  live trace-diff run.
- **Part 4 — RNG trace:** done both backends — one `@@RNG@@ {json}` line per primitive
  draw to stderr in deterministic mode. Schema as in Part 4 above.
- **Parts 5/6/7 — harness:** done → `scripts/mission_verify/` (`dump_mission_state.sh`,
  `table_diff.py`, `trace_diff.py`, `run_mission_parity.sh`, README) + new
  `scripts/seed_attack_asymmetric.sql`. Harness greps `"seq"` from Java `docker logs`
  and Rust stderr into trace files.
- **End-to-end PROVEN (2026-06-23):** ran `run_mission_parity.sh` on all scenarios.
  Same-seed reproducibility is demonstrated on the **seed-sensitive** scenario
  `seed_attack_partialkill.sql` (FIXED_MISSION_ID=900001): a negative control proves
  it depends on the shuffle (seed 900001 → survivor `src=1005`; seed 900007 →
  `src=1002`), and with the SAME fixed seed Java and Rust produce **byte-identical RNG
  draws** (`shuffle bound=3→2, bound=2→0`, incl. `result`) and **9/9 identical tables**.
  Verified manually (not just via the agent): Java container mission 900001 emits the
  same `@@RNG@@` lines and same `obtained_units` as Rust.
- **Bugs found + fixed (all Rust, Java is reference):** D0 (improvement inheritance
  ignored `has_to_inherit_improvements`), D2 (capture type-chain lookup order),
  Part-3 defender-query missing `ORDER BY` (wrong shuffle input), and two report
  serialization mismatches (user ordering; wiped-stack reports original not final count).
- **Residual nit (not correctness):** the harness's in-run Java trace capture is
  intermittently empty (stderr/`docker logs` flush timing) — the strict trace matches
  when captured with a settle wait; the table diff is the authoritative, reliably-green
  check. The dev DB `owge` needs broadened seed cleanup (user 1 had extra refs); the
  updated seeds handle it. `ATTACK_DETERMINISTIC_RNG` left `FALSE` (production default).

## Suggested build order

Part 0 (audit) → Part 1 (`JavaRandom` + golden tests) → Part 2 (gate both
backends) → Part 4 (trace) → Part 3 (fix ordering until traces match on one
scenario) → Part 5 (table diff) → Part 6/7 (scale to all scenarios).
