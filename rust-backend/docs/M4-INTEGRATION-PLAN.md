# M4 integration plan — exact emit call-sites (from Java)

Core (DONE + verified): socket.io server on OWGE_WS_PORT (socketio.rs), handshake,
`websocket::emitter::{send_message,send_one_time_message,broadcast_cache_clear}`.
Wrappers authored by the fan-out (mission_event_emitter, user_event_emitter,
obtained_unit_event_emitter, realtime_emitter, running_mission_finder_bo).

Emit AFTER tx.commit + lock release. The register/run helpers run on a borrowed
`conn` (no pool) — emit in the CALLER which has `&Db`.

## Tier 1 — wire now + verify

### Registration (unit_mission_bo.rs register helper, after run_locked)
Java UnitMissionRegistrationBo.doCommonMissionRegister tail:
- `emit_local_mission_change(db, mission.id, user.id)`  (enemy if !invisible + unit_missions)
- if user owns source planet: `ObtainedUnitEventEmitter::emit_obtained_units(db, user.id)`
- if isEnemyPlanet (source owned by someone != user): `emit_enemy_missions_change(db, source_owner)`
(Change the run_locked closure to RETURN the Mission so the caller can emit.)

### Cancel unit mission (my_cancel_mission, after run_locked)
ReturnMissionRegistrationBo emits emitLocalMissionChangeAfterCommit(returnMission).
- `emit_local_mission_change(db, return_mission.id, user_id)` (return id from closure) — or at
  minimum `emit_unit_missions(db, user_id)`.

### Firing path (do_run_unit_mission, after `tx.commit()` line 430)
Per-processor in Java, but the common owner event is emitLocalMissionChange:
- `emit_local_mission_change(db, mission.id, mission.user_id)`
Processor extras (Tier 2): return/deploy emitObtainedUnits; attack multi-user; explore
planet_explored_event; gather mission_gather_result; conquest planet_owned_change.

### Build-unit register (mission_bo.rs register_build_unit, after commit)
Java MissionBo build register tail: emitMissionCountChange + emitUnitBuildChange +
unitTypeBo.emitUserChange + emitUserData. So:
- `emit_mission_count_change(db,uid)` ; `emit_unit_build_change(db,uid)` ;
  `emit_unit_type_change(db,uid)` ; `emit_user_data(db,uid)`

### Build-unit completion (mission_bo.rs run_mission BUILD_UNIT branch, after commit)
Java processBuildUnit: emitUnitBuildChange + emitMissionCountChange + emitObtainedUnits +
emitUser(unitType+missionCount). So:
- `emit_unit_build_change` + `emit_mission_count_change` + `emit_obtained_units` + `emit_unit_type_change`

### Cancel build (mission_bo.rs cancel_build_unit, after commit)
Java MissionCancelBuildService: UNIT_BUILD_MISSION_CHANGE + emitMissionCountChange:
- `emit_unit_build_change` + `emit_mission_count_change`

### Level-up register (mission_bo.rs register_level_up_an_upgrade, after commit)
Java: emitRunningUpgrade + emitMissionCountChange + emitUserData:
- `emit_running_upgrade(db,uid)` (running_upgrade_change = find_running_level_up_mission) +
  `emit_mission_count_change` + `emit_user_data`

### Level-up completion (mission_bo.rs run_mission LEVEL_UP branch, after commit)
Java processLevelUp: RUNNING_UPGRADE_CHANGE->null + obtainedUpgradeBo.emitObtainedChange +
emitMissionCountChange:
- `emit_running_upgrade` (returns null when none) + `emit_obtained_upgrades` (obtained_upgrades_change =
  UpgradeBo::find_obtained_dtos) + `emit_mission_count_change`

### Cancel upgrade (mission_bo.rs cancel_upgrade_mission, after commit)
Java: RUNNING_UPGRADE_CHANGE->null + emitMissionCountChange:
- `emit_running_upgrade` + `emit_mission_count_change`

`emit_running_upgrade` and `emit_obtained_upgrades` are small inline wrappers I add in
mission_bo (or mission_event_emitter): send_message(uid,"running_upgrade_change",
MissionBo::find_running_level_up_mission) and (uid,"obtained_upgrades_change",
UpgradeBo::find_obtained_dtos).

## Tier 2 — DONE (5th session, 2026-06-05) except the requirement-trigger cross-cut

Firing-path emits are routed through a `DeferredEmit` queue: processors push while
running inside the tx (on the borrowed conn), `unit_mission_bo::do_run_unit_mission`
drains them AFTER commit. Enum + `run(db)` live in `mission_processor/mod.rs`;
`dispatch` + all 8 `process()` + `attack::{process,process_attack,trigger_attack_if_required}`
thread `emits: &mut Vec<DeferredEmit>`.

- ✅ Attack/Counterattack/Conquest (DeferredEmit::Attack): the full
  `startAttack` + `updatePoints` + `AttackMissionProcessor` per-user block.
  `attack_mission_manager_bo` now tracks `users_with_deleted_missions` /
  `users_with_changed_counts` / `altered_users` / `target_owner` and returns them
  as `AttackEmitData`. RUNTIME-VERIFIED on Rust (symmetric 10v10): attacker gets
  {unit_type,unit_obtained,unit_mission,user_data}; target-owner defender gets
  {unit_type,unit_obtained×2,enemy_mission,unit_mission,user_data} — matches Java.
- ✅ Explore processor (DeferredEmit::PlanetExplored): one-time planet_explored_event
  with the planet DTO, only on a *new* exploration.
- ✅ Gather processor (DeferredEmit::GatherResult): one-time mission_gather_result.
  RUNTIME-VERIFIED on Rust (153.00000607967377 / 102.00000151991844).
- ✅ Conquest success (DeferredEmit::ConquestSuccess + LocalMissionChange): new owner
  planet_owned/enemy_mission/unit_obtained + per-list-holder planet_user_list_change
  (find_planet_list_holders) + old owner planet_owned/enemy_mission; conditional
  emitLocalMissionChange unless survivors returning.
- ✅ time_special activate (active_time_special_bo): emit_time_special_change +
  emitIfActivationAffectingUnits (obtained_units when a TIME_SPECIAL→UNIT/UNIT_TYPE
  rule exists).
- ✅ deleteAccount (user_storage_bo): send_account_deleted(uid).
- ✅ twitch (routes/game/mod.rs): emit_twitch_state_change broadcast (boolean value).
- ✅ Deploy/Return processors: already wired in emit_after_run (Tier-1, 4th session).

### Still TODO — the requirement-trigger emit cross-cut + report sync key
- Requirement triggers (requirement_bo trigger_* / requirement_engine
  process_relation_list): on unlock/lock fire emit_unit_unlocked_change /
  emit_speed_impact_group_unlocked_change / emit_time_special_change /
  emit_unit_requirements_change to the affected user. These run INSIDE the firing
  tx (on conn) from MANY sites (build/levelup completion, attack, deploy/return,
  unit/delete, time_special activate); to emit post-commit they need to feed a
  collector (DeferredEmit-like) that bubbles up. Cross-cutting — own follow-up.
  Includes time_special activate's triggerTimeSpecialStateChange unlock emits
  (the direct time_special_change + obtained-units ARE done above).
- mission_report_change sync key still `None` in sync.rs; report_mark_as_read /
  markAsReadBeforeDate routes still TODO emit the count.

### Latent bug fixed this session
- `running_mission_finder_bo::load_user_planet_ids` read `planets.id` (bigint
  UNSIGNED) as i64 → 500 on EVERY enemy-missions emit. The 4th-session gather test
  never hit it (unowned target). Fixed with `CAST(id AS SIGNED)`. This finder backs
  emit_enemy_missions_change, fired by the attack target-owner branch + any
  enemy-owned-target registration/firing.
