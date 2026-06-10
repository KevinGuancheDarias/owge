//! `UpgradeRestService` — the player-facing upgrade endpoints under
//! `game/upgrade` (`registerLevelUp`, `cancelUpgrade`). Both require a game user.
//!
//! ## Parity status (M3)
//! `registerLevelUp` (`MissionBo.registerLevelUpAnUpgrade` +
//! `findRunningLevelUpMission`) and `cancelUpgrade`
//! (`MissionBo.cancelUpgradeMission`) are LEVEL_UP mission registration/cancel,
//! the direct sibling of the BUILD_UNIT register/cancel pair. Both are wired to
//! the real `MissionBo` methods. The frontend ignores both response bodies
//! (`Observable<void>` / `Promise<void>`) — the running upgrade is driven by the
//! `running_upgrade_change` websocket sync — but the responses mirror Java for
//! fidelity (`RunningUpgradeDto` with a fresh `missionsCount`; `"{}"`).

use axum::extract::State;
use axum::routing::get;
use axum::{Json, Router};

use owge_business::OwgeError;
use owge_business::bo::MissionBo;
use owge_business::dto::mission::RunningUpgradeDto;

use crate::auth::GameUser;
use crate::http_error::ApiResult;
use crate::state::AppState;

pub fn routes() -> Router<AppState> {
    Router::new()
        .route("/game/upgrade/registerLevelUp", get(register_level_up))
        .route("/game/upgrade/cancelUpgrade", get(cancel_upgrade))
}

#[derive(serde::Deserialize)]
struct RegisterLevelUpQuery {
    /// `upgrades.id` = `smallint unsigned`; the Java param is boxed `Integer`.
    #[serde(rename = "upgradeId")]
    upgrade_id: u16,
}

/// `registerLevelUp` -> `MissionBo.registerLevelUpAnUpgrade` then return the
/// running level-up mission with the fresh unresolved-mission count.
async fn register_level_up(
    State(state): State<AppState>,
    GameUser(user): GameUser,
    axum::extract::Query(q): axum::extract::Query<RegisterLevelUpQuery>,
) -> ApiResult<Json<RunningUpgradeDto>> {
    let mut conn = state.db.acquire().await?;
    let user_id = user.id as i32;
    MissionBo::register_level_up_an_upgrade(&mut conn, user_id, q.upgrade_id).await?;
    let mut running = MissionBo::find_running_level_up_mission(&mut conn, user_id)
        .await?
        .ok_or_else(|| {
            OwgeError::Common(
                "Running level-up mission vanished right after registration".to_string(),
            )
        })?;
    running.missions_count = Some(MissionBo::count_unresolved_missions(&mut conn, user_id).await?);
    Ok(Json(running))
}

/// `cancelUpgrade` -> `MissionBo.cancelUpgradeMission`. Java returns the literal
/// `"{}"` body (a raw string via Spring's `StringHttpMessageConverter`).
async fn cancel_upgrade(
    State(state): State<AppState>,
    GameUser(user): GameUser,
) -> ApiResult<&'static str> {
    let mut conn = state.db.acquire().await?;
    MissionBo::cancel_upgrade_mission(&mut conn, user.id as i32).await?;
    Ok("{}")
}
