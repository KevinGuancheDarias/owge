# Attack feature-parity audit — Java reference vs Rust port

Part 0 of `ATTACK_PARITY_PLAN.md`. Read-only audit mapping every Java attack code
path to its Rust counterpart. No source was modified.

Sources audited:

- Java: `business/src/main/java/com/kevinguanchedarias/owgejava/business/`
  - `mission/attack/AttackMissionManagerBo.java`
  - `mission/attack/AttackObtainedUnitBo.java`
  - `mission/attack/AttackBypassShieldService.java`
  - `mission/attack/AttackEventEmitter.java`
  - `mission/attack/listener/HandleUnitCaptureListener.java`
  - `AttackRuleBo.java`, `CriticalAttackBo.java`
  - `rule/UnitRuleFinderService.java`, `rule/RuleBo.java`
  - `rule/timespecial/ActiveTimeSpecialRuleFinderService.java`
  - `AllianceBo.areEnemies`
  - `mission/processor/AttackMissionProcessor.java`
- Rust: `rust-backend/owge-business/src/bo/`
  - `attack_mission_manager_bo.rs` (combat core, 1659 lines)
  - `active_time_special_rule_finder_bo.rs`
  - `mission_processor/{attack,conquest,counterattack}.rs`

---

## (a) Summary verdict

**The Rust port is functionally complete: every Java attack behavior has a Rust
counterpart, and the per-stack combat math, kill counts, shield/health drain,
carry-over, capture, points, carrier-freeing, mission-deletion, requirement
triggers, and report JSON are faithfully ported.** No behavior is outright
`❌ missing`.

There is exactly **one outcome-affecting divergence** and it is the one the plan
already flags as the whole reason for the work: **RNG**. Both the shuffle
(targeting order) and the two capture rolls use a clock-seeded xorshift in Rust vs
`Collections.shuffle`/`Math.random()` in Java, so outcomes are not reproducible
across backends. That is by design pre-Part-1; it is listed below as `⚠` because it
*does* change table state until `JavaRandom` (Part 1/2) lands.

**One non-RNG real divergence in the core arithmetic was confirmed (D0):** the
unit-type improvement inheritance ignores the `has_to_inherit_improvements` flag, so
Rust can apply a parent-type ATTACK/SHIELD/DEFENSE bonus that Java suppresses — that
changes per-stack stats and therefore kills, independent of RNG. Beyond that I found
three latent ordering/edge differences that change table state only in narrow
multi-type-chain / NULL-extra_args configurations (D2–D4), plus cosmetic/emit-order
notes.

---

## (b) Behavior parity table

Legend: ✅ ported faithfully · ⚠ ported but differs · ❌ missing

| # | Behavior | Java site | Rust site | Status | Note |
|---|----------|-----------|-----------|--------|------|
| 1 | `buildAttackInformation` assembly: defenders then attacker stacks | `AttackMissionManagerBo.java:59-68` | `attack_mission_manager_bo.rs:268-326` | ✅ | Defenders via `SELECT_DEFENDERS_SQL` (planet-resident / DEPLOYED / CONQUEST≥10%), then `findByMissionId`. Add order preserved (load-bearing for shuffle input, Part 3). |
| 2 | `isAttackTriggerEnabledForMission` (`MISSION_<TYPE>_TRIGGER_ATTACK`) | `AttackMissionManagerBo.java:70-73` | gated in processors (`mission_processor/*`) | ✅ | Config gate lives in the processors that call `process_attack`, same as Java's `triggerAttackIfRequired`. |
| 3 | `addUnit` — per-user accumulator + `unitsStoringUnits` + improvement load | `AttackMissionManagerBo.java:76-93` | `attack_mission_manager_bo.rs:331-374` | ✅ | `findUserImprovement` is narrowed to ATTACK/SHIELD/DEFENSE unit-type improvements on the locked conn (documented; correct for combat). |
| 4 | `AttackObtainedUnit.create` — total attack/shield/health **with improvement multipliers** | `AttackObtainedUnitBo.java:20-45` | `attack_mission_manager_bo.rs:379-418` | ✅ | Formula matches exactly: `total = count*stat; total += total*asRational(imp)`, `asRational = pct/100`. Shield null→0. See D1 caveat (chain inheritance). |
| 5 | `shuffleUnits` (targeting order) | `AttackObtainedUnitBo.java:47-49` | `attack_mission_manager_bo.rs:434-435, 1486-1503` | ⚠ | **RNG divergence** — xorshift64 vs `Collections.shuffle(java.util.Random)`. Changes table state. This is Part-1 work. |
| 6 | `startAttack` enemy resolution (`areEnemies`) | `AttackMissionManagerBo.java:99-101` | `attack_mission_manager_bo.rs:438-460, 1537-1547` | ✅ | `are_enemies` logic identical (diff users + missing/different alliance). |
| 7 | `doAttack` — rule filter, critical scoring, descending sort, `noAttack` break | `AttackMissionManagerBo.java:150-171` | `attack_mission_manager_bo.rs:491-545` | ✅ | Score sort `b-a`; skips `final_count==0`; breaks on `no_attack`. Rust adds a `target_idx==attacker_idx` self-skip (D3). |
| 8 | `attackTarget` — bypass, victim health, survive/wipe branches, carry-over clamp | `AttackMissionManagerBo.java:173-213` | `attack_mission_manager_bo.rs:550-635` | ✅ | `myAttack = pendingAttack*score`; survive: shield/health split `/2`, neg-shield rollover; wipe: zero out, delete, clamp leftover to `originalAttackValue`. Matches line-for-line. |
| 9 | `addPointsAndUpdateCount` — killedCount + earnedPoints | `AttackMissionManagerBo.java:227-243` | `attack_mission_manager_bo.rs:688-725` | ✅ | `healthPerUnit` uses **own bypass flag only** (not time-special) on both sides; `floor(usedAttack/healthPerUnit)` clamped to `finalCount`; `earnedPoints += killed*points`. |
| 10 | `AttackBypassShieldService.bypassShields` (own flag OR time-special rule) | `AttackBypassShieldService.java:15-20` | `attack_mission_manager_bo.rs:642-666` | ✅ | Own `bypass_shield` OR `TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD` rule owned by source targeting victim unit. |
| 11 | `maybeUnsetHolderUnit` — free carried stacks on carrier wipe | `AttackMissionManagerBo.java:215-225` | `attack_mission_manager_bo.rs:670-683` + persist `1204-1213` | ✅ | In-memory `owner_unit_id=None`; persisted as `UPDATE owner_unit_id=NULL` in `update_points`, skipping ids that were themselves later wiped. |
| 12 | `deleteMissionIfRequired` — empty mission delete / `removed` flag | `AttackMissionManagerBo.java:252-262` | `attack_mission_manager_bo.rs:1129-1158` | ✅ | `existsByMission`→`COUNT(*)==0`; attack mission ⇒ `removed=true`; else delete + `usersWithDeletedMissions.add(owner)`. |
| 13 | `findCriticalScore` (own crit, else type chain, else 1) | `AttackMissionManagerBo.java:264-271`, `CriticalAttackBo.java:86-109` | `attack_mission_manager_bo.rs:1312-1373` | ✅ | `findApplicableCriticalEntry` + `findUsedCriticalAttack` parent-chain recursion ported faithfully; default `1F`. |
| 14 | `updatePoints` — addPointsToUser + saveWithChange survivors + altered users | `AttackMissionManagerBo.java:126-148` | `attack_mission_manager_bo.rs:1163-1216` | ✅ | Points UPDATE; survivors with changed count → `UPDATE count = count - killed`; `altered_users ∪= usersWithChangedCounts`. |
| 15 | `OwgeElementSideDeletedException` handling in saveWithChange | `AttackMissionManagerBo.java:135-140` | `attack_mission_manager_bo.rs:1184-1194` | ⚠ | See D4 — Rust has no equivalent guard; behavior differs only in a delete-during-tx edge. |
| 16 | `AttackRuleBo.findAttackRule` recursion + `canAttack` | `AttackRuleBo.java:86-129` | `attack_mission_manager_bo.rs:1253-1308, 1376-1406` | ✅ | Nearest-ancestor `attack_rule_id`; first matching entry (UNIT then UNIT_TYPE chain) decides; default `true`. |
| 17 | Capture roll `onAfterUnitKilledCalculation` (prob + amount draws) | `HandleUnitCaptureListener.java:31-50` | `attack_mission_manager_bo.rs:730-784` | ⚠ | Formula ported exactly (`Math.random()*100<prob`; `floor(rand*floor(killed*pct*0.01)+1)`). RNG source differs (item 5/D5). Parse-on-`#` matches `ARGS_DELIMITER`. |
| 18 | Capture rule lookup order (`findRule` + time-special fallback) | `UnitRuleFinderService.java:35-58`, `findRule` | `attack_mission_manager_bo.rs:792-895` | ⚠ | Steps 1-3 match. **D2:** step-4 (UNIT_TYPE×UNIT_TYPE) chain-walk order differs from Java's recursion. |
| 19 | `saveCaptured` / `moveUnit` stationing | `HandleUnitCaptureListener.java:69-99` | `attack_mission_manager_bo.rs:899-1055` | ✅ | Source/target from captor mission else own planet; owned-planet vs DEPLOYED foreign-planet branch; `is_from_capture=1`; merge-or-insert. |
| 20 | Capture report fan-out `onAttackEnd` (`unitCaptureInformation`) | `HandleUnitCaptureListener.java:52-67` | `attack_mission_manager_bo.rs:1062-1124` | ✅ | One report per distinct captor (first-seen order), `is_enemy=false`, `involvedUnits=[]`, entries `{unit, oldOwner{id,username}, capturedCount}`. |
| 21 | `usersWithDeletedMissions` vs `usersWithChangedCounts` split | `AttackMissionManagerBo.java:104-122` | collected `attack_mission_manager_bo.rs:256-261`, emitted by processor | ✅ | Both sets collected identically; the per-user emit *block* (emitUnitMissions/emitUserData/emitObtainedUnits/emitEnemyMissionsChange) is M4 — see D6/cosmetic. |
| 22 | Points accrual to user_storage (`addPointsToUser`) | `AttackMissionManagerBo.java:131` | `attack_mission_manager_bo.rs:1172-1179` | ✅ | `UPDATE user_storage SET points = points + earned`. Guarded by `earned != 0.0` (Java always calls; net-equivalent since 0 is a no-op). |
| 23 | `triggerUnitRequirementChange` (HAVE_UNIT/UNIT_AMOUNT) | `AttackMissionProcessor.java:90-104` | `attack_mission_manager_bo.rs:1224-1247` | ✅ | Wiped→buildCompletedOrKilled; shrunk→amountChanged. Java does `.distinct()` over units; Rust iterates per stack (D7 cosmetic). |
| 24 | `to_attack_information_json` report shape | `UnitMissionReportBuilder.withAttackInformation` | `attack_mission_manager_bo.rs:1455-1481` | ✅ | `{userInfo, earnedPoints, units:[{initialCount, finalCount, obtainedUnit}]}`, in `user_order`. |
| 25 | Conquest owner reassignment | (not in attack manager — `ConquestMissionProcessor`) | `mission_processor/conquest.rs:38-160` | ✅ | Correctly **outside** the attack manager in both; attack never reassigns owner. Old-owner capture, defeat calc, planet redefine all present. |

---

## (c) Prioritized gaps / differences

### Group 1 — WOULD change table state

**D0 (NEW — confirmed real bug, not RNG). Unit-type improvement inheritance ignores
`has_to_inherit_improvements`.**
Java `GroupedImprovement.findUnitTypeImprovement` (`pojo/GroupedImprovement.java:74-86`)
walks to a parent unit type **only when `unitType.getHasToInheritImprovements()` is
TRUE** at that level (and stops as soon as a level has it FALSE):

```java
Long retVal = <sum of improvements for this exact unitType id>;
if (Boolean.TRUE.equals(unitType.getHasToInheritImprovements()) && unitType.getParent() != null) {
    retVal += findUnitTypeImprovement(improvementTypeEnum, unitType.getParent());
}
```

Rust builds the **full** parent chain unconditionally (`unit_type_chain`,
`attack_mission_manager_bo.rs:1387-1406`, plain `parent_type` walk with no flag check)
and `find_unit_type_improvement_for_chain`
(`dto/user_improvement.rs:96-108`) sums improvements across **every** ancestor in that
chain. So when a unit type has `has_to_inherit_improvements = FALSE` (the default for
many types) but has a parent that carries an ATTACK/SHIELD/DEFENSE improvement, **Rust
applies the parent's bonus and Java does not** → different per-stack total
attack/shield/health → different kill counts **even with identical RNG**. This is
exactly the silent-divergence class the plan warns about (#1) and it is independent of
Part 1. **Should be fixed before bit-for-bit combat can pass on any scenario that has a
parent-type improvement and a non-inheriting child type.** The same flag-aware walk is
needed wherever `unit_type_chain` feeds improvements (only the combat stack here; the
rule/critical chain walks are correct because those *do* inherit unconditionally in
Java's `findUnitTypeMatchingRule`).

**D1 (the headline, already planned). RNG source mismatch.**
Shuffle: `attack_mission_manager_bo.rs:1486-1503` (clock-seeded xorshift64) vs
`AttackObtainedUnitBo.java:48` (`Collections.shuffle` / `java.util.Random`).
Capture prob+amount: `attack_mission_manager_bo.rs:773,779` (`next_unit_f64`,
clock-seeded xorshift, seeded at `:289-294`) vs `HandleUnitCaptureListener.java:46,48`
(`Math.random()`). This is the entire premise of Parts 1-3: until `JavaRandom` is
ported and gated, shuffle order and capture counts differ → which stack absorbs a
partial kill differs → survivor/captured counts differ. **Highest priority; already
the plan's main work.**

**D2. Capture-rule UNIT_TYPE×UNIT_TYPE lookup order differs.**
Java `UnitRuleFinderService.unitTypeVsUnitTypeOptional` recurses **victim (to)
parent chain first at each from-level, then advances the from chain** — i.e. for
`fromChain=[F0,F1]`, `toChain=[T0,T1]` it probes `(F0,T0),(F0,T1),(F1,T0),(F1,T1)`
*but via nested recursion that exhausts the `to` side before stepping `from`*.
Rust step 4 (`attack_mission_manager_bo.rs:834-842`) iterates `for f in attacker_chain
{ for t in victim_chain }` — attacker outer, victim inner. The two produce the
**same set** but a different **first match** when more than one `(UNIT_TYPE,
UNIT_TYPE)` capture rule exists across different ancestor combinations. With a single
capture rule per pair (the normal case) outcomes are identical. **Only diverges with
multiple overlapping type-chain capture rules.** Worth aligning the loop nesting to
Java's recursion order to be safe; low real-world likelihood.

Note: steps 2 and 3 (UNIT×UNIT_TYPE, UNIT_TYPE×UNIT) match Java's single-sided
recursion order correctly.

**D3. Self-target skip not present in Java.**
Rust `do_attack` skips `target_idx == attacker_idx`
(`attack_mission_manager_bo.rs:519-521`). Java has no such guard, but a unit is never
in its own `attackableUnits` because `areEnemies` requires different user ids
(`AllianceBo.areEnemies` / `:1543`). So the guard is **defensive and inert** — it can
only differ if a single stack were ever its own enemy, which `areEnemies` forbids. No
practical divergence; flagged for completeness.

**D4. `OwgeElementSideDeletedException` guard missing.**
Java wraps `saveWithChange` in try/catch (`AttackMissionManagerBo.java:135-140`); on a
proxy whose row was deleted earlier in the tx it logs and **skips marking the user
altered** without aborting. Rust `update_points` (`:1184-1194`) does a plain
`UPDATE ... WHERE id = ?`; if the row was already deleted the UPDATE simply affects 0
rows (no error, no abort). End-state is equivalent (the row is gone either way), and
Rust never adds that user to `altered_users` for a 0-row update only if you check
rows-affected — currently it adds unconditionally on a survivor whose `final_count!=0`,
but such a stack was *not* deleted, so the row exists. **Net: no table-state divergence
in realistic paths**, but the explicit guard is absent so a future deletion-ordering
change wouldn't be caught the way Java tolerates it. Low risk.

### Group 2 — cosmetic / emit-order only (no table-state effect)

- **D5. Capture rolls share one xorshift stream** (`info.rng_state`) separate from the
  shuffle PRNG. Under Part 1 both must become the *same* `JavaRandom` consumed in
  Java's order (shuffle draws, then capture prob/amount per pair in `addPointsAndUpdateCount`
  call order). Currently they are two independent clock seeds — folded into D1.
- **D6. Per-user post-combat websocket emit block** (`AttackMissionManagerBo.java:104-123`:
  emitUnitMissions / clearSourceCache / emitUserData / emitObtainedUnits /
  emitEnemyMissionsChange, plus the `usersWithChangedCounts.remove(userId)` dedup and the
  `targetPlanet.getOwner()` branch) is M4 in Rust — collected as `AttackEmitData`
  (`:224-237`) and scheduled by the processor post-commit. State is unaffected; only the
  realtime emissions are deferred. The `usersWithDeletedMissions`/`usersWithChangedCounts`
  split and `target_owner` capture are all present.
- **D7. `triggerUnitRequirementChange` dedup.** Java does
  `attackInformation.getUnits().stream().distinct()` (`AttackMissionProcessor.java:90`)
  before triggering; Rust iterates every stack (`:1229`). Since each `AttackObtainedUnit`
  is a distinct stack (distinct obtained_unit id), `.distinct()` removes nothing here —
  no effect.
- **D8. NULL `extra_args` capture rule.** Rust treats a matched rule whose `extra_args`
  is NULL or has `<2` `#`-parts as "no capture" and **falls through to the next lookup**
  is NOT what Java does — but Java would actually NPE/short-circuit: `findRule` returns
  the first matching rule, then `.filter(hasExtraArg(0)&&hasExtraArg(1))` runs
  `split("#")` on the (non-null in practice) args. Rust returning the first matched rule's
  args and then bailing on malformed args (`:762-770`) matches Java's "first rule wins,
  then validate" semantics; the only true difference is Rust *continuing the lookup* on a
  NULL-args match (`lookup_rule` inner `Option<String>` → treated as no match). In practice
  capture rules always carry `prob#pct` extra_args, so inert. Documented for the harness.

---

## Cross-checks performed

- **Improvement multiplier formula (silent-divergence risk #1):** the per-stack
  arithmetic is byte-identical — `count*stat`, then
  `+= total*asRational(findUnitTypeImprovement(type, unitType))`, `asRational = pct/100`,
  for ATTACK/SHIELD/DEFENSE; shield coalesces null→0. **BUT** the *value* fed in diverges:
  see **D0** — Rust's chain walk ignores `has_to_inherit_improvements`, so it can
  over-apply parent-type improvements that Java suppresses. Confirmed real divergence.
- **Carrier free on wipe (risk #2):** present and persisted (item 11).
- **usersWith* split (risk #3):** present (item 21).
- **Alliance enemies (risk #4):** identical (item 6).
- **Points (risk #5):** identical (items 9, 22).
- **Attack-rule recursion + can_attack (risk #6):** identical (item 16).
- **Shield bypass own-flag OR time-special (risk #7):** identical (item 10).
- **Capture lookup order / `#` parsing / formulas / report (risk #8):** ported; only
  D2 (type×type ordering) and D8 (NULL args) are edge differences.
- **OwgeElementSideDeletedException (risk #9):** D4 — no explicit guard, behavior
  equivalent in realistic paths.

---

## Settled during this audit (was the open improvement-inheritance question)

The improvement-inheritance question is **resolved and is now D0** (Group 1). Java's
`GroupedImprovement.findUnitTypeImprovement` (`pojo/GroupedImprovement.java:74-86`)
only folds in a parent type's improvement when that level's
`has_to_inherit_improvements` is TRUE; Rust's `unit_type_chain` walk
(`attack_mission_manager_bo.rs:1387-1406`) folds in **every** ancestor unconditionally.
This is a confirmed combat-outcome divergence independent of RNG. A Part-6 scenario
with a parent-type ATTACK/SHIELD/DEFENSE improvement on a non-inheriting child type
will reproduce it.
