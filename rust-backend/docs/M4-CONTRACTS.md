# M4 (socket.io realtime) — frozen contracts for the emitter fan-out

The transport + handshake + emit pipeline are DONE and runtime-verified
bit-for-bit vs Java/dc13. Your job: port the `*EventEmitter` finders + wrapper
functions as **new files only**, reusing existing finders. Do NOT edit shared
files; return wiring lines for the orchestrator to apply.

## The frozen emit API (already built — call it, don't change it)

`owge_business::websocket::emitter`:

```rust
// Persist the per-user websocket_events_information watermark, then push
// `deliver_message` to connected sockets only. target_user_id == 0 => broadcast
// (watermark for ALL users; push to every connected client). `compute` is an
// async closure evaluated lazily and ONLY when a recipient is connected.
pub async fn send_message<F, Fut>(db: &Db, target_user_id: i32, event_name: &str, compute: F)
    -> OwgeResult<()>
where F: FnOnce() -> Fut, Fut: Future<Output = OwgeResult<serde_json::Value>>;

// One-time push, no watermark (Java sendOneTimeMessage).
pub async fn send_one_time_message(target_user_id: i32, event_name: &str, value: Value)
    -> OwgeResult<()>;

pub async fn broadcast_cache_clear() -> OwgeResult<()>;
```

Canonical wrapper shape (this is what every `emit_*` becomes):

```rust
use crate::websocket::emitter;
pub async fn emit_obtained_units(db: &Db, user_id: i32) -> OwgeResult<()> {
    emitter::send_message(db, user_id, "unit_obtained_change", || async {
        Ok(serde_json::to_value(ObtainedUnitBo::find_completed_dtos(db, user_id).await?)?)
    }).await
}
```

Notes:
- `Db` = `crate::db::Db`. `OwgeResult` = `crate::error::OwgeResult`.
- Emits must run AFTER the surrounding tx commits — but that's a CALL-SITE
  concern (orchestrator). Your wrapper just calls `send_message`.
- Event name strings are the EXACT Java constants (snake_case `*_change`).

## Existing finders to reuse (all `(db, user_id)` unless noted)

| event name | finder (returns) |
|---|---|
| user_data_change | `UserStorageBo::find_data` -> `Option<UserStorageDto>` (unwrap; Java always has a user) |
| unit_obtained_change | `ObtainedUnitBo::find_completed_dtos` -> `Vec<ObtainedUnitDto>` |
| unit_type_change | `UnitTypeBo::find_unit_types_with_user_info` -> Vec |
| unit_unlocked_change | `UnitBo::find_unlocked_by_user` -> `Vec<UnitDto>` |
| unit_requirements_change | `RequirementBo::find_faction_unit_level_requirements` |
| speed_impact_group_unlocked_change | `SpeedImpactGroupBo::find_cross_galaxy_unlocked` |
| time_special_change | `TimeSpecialBo::find_user_status_dtos` |
| obtained_upgrades_change | `UpgradeBo::find_obtained_dtos` |
| upgrade_types_change | `UpgradeBo::find_upgrade_types` (no user_id) |
| running_upgrade_change | `MissionBo::find_running_level_up_mission` |
| system_message_change | `SystemMessageBo::find_read_by_user` |
| planet_owned_change | `PlanetBo::find_owned_dtos` -> `Vec<PlanetDto>` |
| planet_user_list_change | `PlanetListBo::find_by_user_id` -> `Vec<PlanetListDto>` |
| missions_count_change | `MissionBo::count_unresolved_missions` -> i32 |

For `user_max_energy_change` (UserEventEmitterBo.emitMaxEnergyChange): the max
energy number. Reuse the exact logic in `user_storage_bo.rs::find_data` (around
line 448-490): `compute_improvement_value(faction.initial_energy,
more_energy_production, true)` via `crate::bo::mission_bo::compute_improvement_value`
(pub(crate)) and `UserImprovementBo::find_user_improvement`. Extract a
`UserStorageBo::find_max_energy(db, user_id) -> OwgeResult<f64>` if convenient.

## Java reference event-emitter classes (read these for exact semantics)

- `business/.../business/mission/MissionEventEmitterBo.java`
- `business/.../business/unit/ObtainedUnitEventEmitter.java`
- `business/.../business/user/UserEventEmitterBo.java`
- `business/.../business/mission/RunningMissionFinderBo.java`
- `business/.../business/mission/MissionFinderBo.java` (findBuildMissions)

## Ground rules (from prior fan-out lessons — STRICT)

1. Create ONLY the new files assigned to you. Do NOT touch `lib.rs`,
   `bo/mod.rs`, `dto/mod.rs`, `Cargo.toml`, `websocket/sync.rs`, or any existing
   `bo/*.rs` / processor file. Those are shared; concurrent edits corrupt them.
2. Do NOT run `cargo build`/`test` (racy with siblings). Make it compile *by
   inspection*; the orchestrator builds once and fixes up.
3. sqlx signedness must match columns exactly (see existing code: smallint =>
   i16, tinyint unsigned => u8, int unsigned => u32, int signed => i32, missions
   source/target_planet are SIGNED i64). When in doubt, copy an existing query.
4. Return, as your final message: (a) the list of files you created; (b) the
   exact `pub mod` / re-export lines to add and to which mod file; (c) suggested
   call-site wiring as `file:function -> emit_xxx(db, user_id)` bullets (the
   orchestrator applies these after tx.commit). Keep it terse.
