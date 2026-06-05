//! Port of `MissionEventEmitterBo`
//! (`com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo`).
//!
//! Async wrapper functions that push mission-related websocket events after a
//! transaction commits, using the frozen `emitter::send_message` API.
//!
//! ## Event-name constants (exact Java `MissionEventEmitterBo` + `MissionBo`)
//! - `"unit_mission_change"` — `UNIT_MISSION_CHANGE`
//! - `"enemy_mission_change"` — `ENEMY_MISSION_CHANGE`
//! - `"missions_count_change"` — `MISSIONS_COUNT_CHANGE`
//! - `"unit_build_mission_change"` — `UNIT_BUILD_MISSION_CHANGE` (from `MissionBo`)
//!
//! ## Call-site contract
//! All `emit_*` functions must be called **after** the surrounding DB
//! transaction commits (`tx.commit().await?`). They read through the pool
//! and will not see uncommitted writes.

use crate::bo::running_mission_finder_bo::RunningMissionFinderBo;
use crate::db::Db;
use crate::dto::mission_websocket::MissionWebsocketMessage;
use crate::error::OwgeResult;
use crate::websocket::emitter;

pub struct MissionEventEmitter;

impl MissionEventEmitter {
    /// `emitUnitMissions(userId)` — pushes `unit_mission_change` with a
    /// [`MissionWebsocketMessage`] `{ count, myUnitMissions }`.
    pub async fn emit_unit_missions(db: &Db, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(db, user_id, "unit_mission_change", || async {
            let count = RunningMissionFinderBo::count_user_running_missions(db, user_id).await?;
            let my_unit_missions =
                RunningMissionFinderBo::find_user_running_missions(db, user_id).await?;
            Ok(serde_json::to_value(MissionWebsocketMessage {
                count,
                my_unit_missions,
            })?)
        })
        .await
    }

    /// `emitEnemyMissionsChange(user)` — pushes `enemy_mission_change` with the
    /// list of enemy missions targeting a planet owned by `user_id`.
    pub async fn emit_enemy_missions_change(db: &Db, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(db, user_id, "enemy_mission_change", || async {
            let missions =
                RunningMissionFinderBo::find_enemy_running_missions(db, user_id).await?;
            Ok(serde_json::to_value(missions)?)
        })
        .await
    }

    /// `emitMissionCountChange(userId)` — pushes `missions_count_change` with
    /// the integer count of unresolved missions for the user.
    pub async fn emit_mission_count_change(db: &Db, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(db, user_id, "missions_count_change", || async {
            let count = RunningMissionFinderBo::count_user_running_missions(db, user_id).await?;
            Ok(serde_json::to_value(count)?)
        })
        .await
    }

    /// `emitUnitBuildChange(userId)` — pushes `unit_build_mission_change` with
    /// the list of running BUILD_UNIT missions for the user.
    pub async fn emit_unit_build_change(db: &Db, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(db, user_id, "unit_build_mission_change", || async {
            let builds = RunningMissionFinderBo::find_build_missions(db, user_id).await?;
            Ok(serde_json::to_value(builds)?)
        })
        .await
    }

    /// `MissionBo.emitRunningUpgrade(user)` — pushes `running_upgrade_change`
    /// with the user's running LEVEL_UP mission (or `null` when none, which is
    /// what Java's explicit `() -> null` emits after completion/cancel).
    pub async fn emit_running_upgrade(db: &Db, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(db, user_id, "running_upgrade_change", || async move {
            Ok(serde_json::to_value(
                crate::bo::MissionBo::find_running_level_up_mission(db, user_id).await?,
            )?)
        })
        .await
    }

    /// `obtainedUpgradeBo.emitObtainedChange(userId)` — pushes
    /// `obtained_upgrades_change` with the user's obtained-upgrade DTOs.
    pub async fn emit_obtained_upgrades(db: &Db, user_id: i32) -> OwgeResult<()> {
        emitter::send_message(db, user_id, "obtained_upgrades_change", || async move {
            Ok(serde_json::to_value(
                crate::bo::UpgradeBo::find_obtained_dtos(db, user_id).await?,
            )?)
        })
        .await
    }

    /// `emitLocalMissionChange(mission, userId)` — reload the mission by id;
    /// if `invisible == false`, look up the target planet owner and emit
    /// `enemy_mission_change` to them (when they differ from the mission owner);
    /// then emit `unit_mission_change` to the mission's user.
    ///
    /// Mirrors `MissionEventEmitterBo.emitLocalMissionChange`.
    pub async fn emit_local_mission_change(
        db: &Db,
        mission_id: u64,
        user_id: i32,
    ) -> OwgeResult<()> {
        // Reload the mission (entityRefreshUtilService.refresh).
        let mission = load_mission_by_id(db, mission_id).await?;
        let Some(mission) = mission else {
            // Mission no longer exists (resolved+deleted) — still emit to user.
            Self::emit_unit_missions(db, user_id).await?;
            return Ok(());
        };

        // if (!mission.getInvisible()) emitEnemyMissionsChange(mission)
        if mission.invisible == 0 {
            if let Some(tp_id) = mission.target_planet {
                // Look up the target planet owner.
                let owner_id: Option<Option<i32>> =
                    sqlx::query_scalar("SELECT owner FROM planets WHERE id = ?")
                        .bind(tp_id as u64)
                        .fetch_optional(db)
                        .await?;
                if let Some(Some(owner_id)) = owner_id {
                    // Only emit to the enemy if they're a different user.
                    if owner_id != user_id {
                        Self::emit_enemy_missions_change(db, owner_id).await?;
                    }
                }
            }
        }

        Self::emit_unit_missions(db, user_id).await
    }
}

// ---------------------------------------------------------------------------
// SQL helpers
// ---------------------------------------------------------------------------

/// Load a single mission by id (SELECT_MISSION ends with `WHERE id = ?`).
async fn load_mission_by_id(
    db: &Db,
    mission_id: u64,
) -> OwgeResult<Option<crate::model::mission::Mission>> {
    Ok(sqlx::query_as::<_, crate::model::mission::Mission>(
        crate::bo::mission_base_service_bo::SELECT_MISSION,
    )
    .bind(mission_id)
    .fetch_optional(db)
    .await?)
}
