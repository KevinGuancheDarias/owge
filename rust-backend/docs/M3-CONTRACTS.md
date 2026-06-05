# M3 (mission engine) — porting contracts for fan-out agents

You are porting part of the OWGE Java mission engine to Rust. The Java source is
under `/public/owge/business/src/main/java/com/kevinguanchedarias/owgejava/`.
Your Rust target is `/public/owge/rust-backend/owge-business` (engine) and
`owge-rest` (endpoints). Read the referenced Java file(s) yourself.

## Hard rules (do not deviate)

1. **Write ONLY the file(s) you are assigned.** Do NOT edit `lib.rs`, any
   `mod.rs`, or another agent's file. Instead, at the very end of your reply,
   return a `WIRING` block listing the exact lines the orchestrator must add
   (module declarations, `pub use`, route `.merge(...)`). The orchestrator owns
   all wiring.
2. **sqlx signedness is load-bearing.** Decode each column at its *literal* MySQL
   type or sqlx panics at runtime. Known traps:
   - `missions.id`/`related_mission`/`report_id` = `bigint unsigned` → `u64`;
     `missions.source_planet`/`target_planet` = **signed** `bigint` → `i64`;
     `missions.user_id` = signed `int` → `i32`; `missions.type` = `smallint
     unsigned` → `u16`; `attemps` = `tinyint unsigned` → `u8`;
     `resolved`/`invisible` = bare `tinyint` → `i8`.
   - `obtained_units.id`/`count` = `u64`, `user_id` = `i32`, `unit_id` = `u16`,
     `source_planet`/`target_planet`/`mission_id`/`owner_unit_id` = `Option<u64>`.
   - `planets.id` = `u64`, `owner` = `Option<i32>`, `galaxy_id` = `u16`,
     `sector`/`quadrant` = `u32`, `planet_number` = `u16`.
   - `units.is_unique`/`speed_impact_groups.is_fixed` = `tinyint unsigned` → `u8`;
     `object_relations.reference_id` = **signed** `smallint` → `i16`.
   - bare `tinyint` → `i8`/`u8` then `!= 0`; only `tinyint(1)` decodes as `bool`.
3. **JSON is camelCase.** Every response/DTO struct uses
   `#[serde(rename_all = "camelCase")]` plus explicit `#[serde(rename=...)]`
   where the Java getter is irregular.
4. **Bo style.** Each `Bo` is a unit struct (`pub struct FooBo;`) with `pub async
   fn` associated functions taking `db: &Db` (pool) for autonomous reads, or
   `conn: &mut sqlx::MySqlConnection` when the work must join the caller's
   transaction (mutations inside a locked section). Prefer `&mut MySqlConnection`
   for anything a mission processor calls, so it runs on the pinned locked
   connection. Match the existing files for tone/structure.
5. **Times are UTC `NaiveDateTime`.** Use `chrono::Utc::now().naive_utc()` for
   "now". DB columns are naive; never apply a local tz.
6. Keep parity faithful. Where a dependency genuinely isn't ported yet, leave a
   `// TODO(M3): ...` and a minimal safe behavior rather than inventing logic —
   and call it out in your reply's `NOTES`.

## Foundation already built (use it, don't redefine)

- `owge_business::model::mission::{Mission, MissionInformation, MissionReport, MissionType}`
  — fields above. `MissionType::{from_value(u16), value(), code(), from_code(), is_unit_mission()}`.
  `Mission::mission_type() -> Option<MissionType>`, `Mission::is_resolved()`.
- `owge_business::lock` — `acquire(&mut conn, &[String])`, `release(&mut conn, &[String])`,
  `planet_lock_key(u64) -> String`, `user_lock_key(i32) -> String`. The mission
  runner acquires the planet superset up front on ONE pinned connection; nested
  calls reuse it (MySQL same-session reentrancy).
- `owge_business::bo::mission_scheduler_bo::{MissionSchedulerService, MissionDispatch}`.
  `MissionSchedulerService::{new(db), schedule_mission(mission_id, required_time_secs),
  abort_mission_job(mission_id), spawn_poller(Arc<dyn MissionDispatch>, worker_id)}`.
  `#[async_trait] trait MissionDispatch { async fn run_mission(&self, mission_id: u64); }`.
- `owge_business::builder::UnitMissionReportBuilder` — the shared report API.
  Methods: `create()`, `create_with(...)`, `with_id(u64)`, `with_sender_user(i32,&str)`,
  `with_source_planet(&PlanetDto)`, `with_target_planet(&PlanetDto)`,
  `with_involved_units(&[ObtainedUnitDto])`, `with_explored_information(&[ObtainedUnitDto])`,
  `with_gather_information(f64,f64)`, `with_establish_base_information(bool,&str)`,
  `with_conquest_information(bool,&str)`, `with_error_information(&str)`,
  `with_attack_information(Value)`, `with_interception_information(Value)`,
  `with_unit_capture_information(Value)`, `with_raw(&str,Value)`,
  `build() -> Map`, `build_json() -> OwgeResult<String>`. Processors RETURN an
  `Option<UnitMissionReportBuilder>` (None = no report).

## Existing Bos/types you may call (read the file for exact signatures)

`db::Db`, `error::{OwgeError, OwgeResult}` (variants: NotFound, AccessDenied,
Unauthorized, InvalidInput, Conflict, Common). `bo::ConfigurationBo`
(`find_or_set_default(db,name,default) -> Configuration{value:String}`,
`find_value`). `bo::ImprovementBo` (admin/config side only — the runtime
`find_user_improvement` aggregate is part of THIS milestone, see the time-math
agent). `bo::ObtainedUnitBo`, `bo::PlanetBo`, `bo::UnitBo`, `bo::UpgradeBo`,
`bo::AttackRuleBo`, `bo::CriticalAttackBo`, `bo::RequirementBo`,
`bo::requirement_engine`, `bo::UnlockedRelationBo`, `bo::UserStorageBo`.
`dto::{PlanetDto, obtained_unit::ObtainedUnitDto}`. `model::{Planet, ObtainedUnit,
Unit, UserStorage, ...}`.

## Cross-module Rust API contract (frozen — code against these names)

These signatures are agreed across agents so disjoint files integrate. If you
own the file, implement exactly this surface; if you call it, assume it exists.

- `bo::mission_configuration_bo::MissionConfigurationBo::find_mission_base_time(db, MissionType) -> OwgeResult<i64>`
- `bo::mission_time_manager_bo::MissionTimeManagerBo`:
  - `compute_termination_date(required_time: f64) -> NaiveDateTime`
  - `calculate_required_time(db, MissionType) -> OwgeResult<f64>`
  - `handle_mission_time_calculation(conn, units: &[ObtainedUnit], mission: &mut Mission, MissionType) -> OwgeResult<()>`
  - `handle_custom_duration(mission: &mut Mission, custom: Option<i64>)`
- `bo::improvement_bo::ImprovementBo::find_user_improvement(db, user_id: i32) -> OwgeResult<UserImprovementDto>`
  where `UserImprovementDto` exposes `more_missions: f64`, `find_unit_type_improvement(ImprovementType, unit_type_id: u16) -> f64`, plus attack/defense/etc. Put the type in `dto::improvement`.
- `pojo::unit_mission_information::{UnitMissionInformation, SelectedUnit}` — registration input (see `pojo/UnitMissionInformation.java`, `pojo/SelectedUnit.java`). `UnitMissionInformation { user_id: Option<i32>, source_planet_id: Option<i64>, target_planet_id: i64, mission_type: Option<MissionType>, wanted_time: Option<i64>, involved_units: Vec<SelectedUnit> }`.
- `bo::unit_mission_registration_bo::UnitMissionRegistrationBo::do_common_mission_register(conn, info: &UnitMissionInformation, mission_type: MissionType, user: &UserStorage, is_deploy: bool) -> OwgeResult<Mission>`
- `bo::return_mission_registration_bo::ReturnMissionRegistrationBo::register_return_mission(conn, source_mission: &Mission, custom_duration: Option<f64>) -> OwgeResult<()>`
- `bo::attack_mission_manager_bo::AttackMissionManagerBo::process_attack(conn, mission: &Mission, involved_units: &[ObtainedUnit]) -> OwgeResult<Value>` (returns the `attackInformation` JSON for the report).
- `bo::mission_interception_manager_bo::MissionInterceptionManagerBo` — `load_information(conn, mission, MissionType) -> OwgeResult<InterceptionInformation>` with `{ is_mission_intercepted: bool, involved_units: Vec<ObtainedUnit> }`.
- Processors live in `bo::mission_processor::{explore,gather,establish_base,attack,counterattack,conquest,deploy,return_mission}` each: `pub async fn process(conn, mission: &Mission, involved_units: &[ObtainedUnit], deps...) -> OwgeResult<Option<UnitMissionReportBuilder>>`. Keep dependencies as explicit args (`db`/`conn`) — no DI container.
- `bo::mission_report_manager_bo::MissionReportManagerBo::handle_mission_report_save(conn, mission: &Mission, report: UnitMissionReportBuilder) -> OwgeResult<()>` — inserts `mission_reports`, links `missions.report_id`, sets `resolved=1`.

## WIRING block format (end of every reply)

```
WIRING
model/mod.rs: pub mod foo;  +  pub use foo::Foo;
bo/mod.rs: pub mod foo_bo;  +  pub use foo_bo::FooBo;
routes/game/mod.rs: .merge(foo::routes())
NOTES: <anything stubbed / parity TODO / signature you had to assume>
```
