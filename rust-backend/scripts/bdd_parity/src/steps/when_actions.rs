//! When steps: REST calls + scheduler nudges (BDD-PARITY-PLAN.md §6.3).
//! The FIRST When of a scenario starts the ws captures (§5.3).

use std::time::Duration;

use cucumber::when;

use crate::support::{rest, ws};
use crate::world::BddWorld;

const MISSION_RESOLVE_TIMEOUT: Duration = Duration::from_secs(40);

fn backend(world: &BddWorld) -> &crate::support::backends::Backend {
    world
        .backend
        .as_ref()
        .expect("this scenario has a When step — OWGE_BDD_BACKEND must be java|rust")
}

/// Start ws_capture.js for every registered user (idempotent; called by every
/// When). Waits for each capture's `authentication` frame so no delivery is
/// missed.
async fn ensure_ws_captures(world: &mut BddWorld) {
    if ws::artifacts_dir().is_none() {
        return; // bare `cargo run` debugging without the runner
    }
    let users: Vec<i64> = world
        .captured_users
        .iter()
        .copied()
        .filter(|u| !world.ws_procs.contains_key(u))
        .collect();
    for user in &users {
        let jwt = rest::mint_jwt(&world.db, *user).await;
        let child = ws::start_capture(backend(world), *user, &jwt);
        world.ws_procs.insert(*user, child);
    }
    for user in &users {
        ws::wait_authenticated(*user, Duration::from_secs(15)).await;
    }
}

fn mission_verb(mission_type: &str) -> &'static str {
    match mission_type {
        "ESTABLISH_BASE" => "establishBase",
        "CONQUEST" => "conquest",
        "DEPLOY" => "deploy",
        "ATTACK" => "attack",
        "COUNTERATTACK" => "counterattack",
        "EXPLORE" => "explore",
        "GATHER" => "gather",
        other => panic!("unknown mission type {other:?} — extend mission_verb()"),
    }
}

/// Nudge THIS mission's db-scheduler task into the past and poll
/// missions.resolved (§3 technique #1; nudges only our task_instance so
/// follow-up missions the execution creates are never fired prematurely —
/// the lesson mission_verify learned the hard way).
async fn fire_and_await_mission(world: &BddWorld, mission_id: i64) {
    let nudge = "UPDATE scheduled_tasks \
                 SET execution_time = DATE_SUB(NOW(6), INTERVAL 5 SECOND), picked = 0 \
                 WHERE task_name = 'mission-run' AND task_instance = ?";
    let deadline = std::time::Instant::now() + MISSION_RESOLVE_TIMEOUT;
    loop {
        sqlx::query(nudge)
            .bind(mission_id.to_string())
            .execute(&world.db)
            .await
            .expect("nudge scheduled_tasks");
        let resolved: Option<i64> =
            sqlx::query_scalar("SELECT CAST(resolved AS SIGNED) FROM missions WHERE id = ?")
                .bind(mission_id)
                .fetch_optional(&world.db)
                .await
                .expect("poll missions.resolved");
        if resolved == Some(1) {
            // let the backend flush post-commit ws emissions / report rows
            tokio::time::sleep(Duration::from_secs(2)).await;
            return;
        }
        if std::time::Instant::now() >= deadline {
            let mission: Vec<String> = dump_rows(
                world,
                &format!("SELECT * FROM missions WHERE id = {mission_id}"),
            )
            .await;
            let task: Vec<String> = dump_rows(
                world,
                &format!(
                    "SELECT * FROM scheduled_tasks WHERE task_instance = '{mission_id}'"
                ),
            )
            .await;
            panic!(
                "mission {mission_id} did not resolve within {MISSION_RESOLVE_TIMEOUT:?}\n\
                 missions row: {mission:#?}\nscheduled_tasks row: {task:#?}"
            );
        }
        tokio::time::sleep(Duration::from_secs(1)).await;
    }
}

async fn dump_rows(world: &BddWorld, sql: &str) -> Vec<String> {
    use sqlx::{Column, Row};
    sqlx::query(sql)
        .fetch_all(&world.db)
        .await
        .map(|rows| {
            rows.iter()
                .map(|r| {
                    r.columns()
                        .iter()
                        .map(|c| {
                            let v: Result<Option<String>, _> = r.try_get_unchecked(c.ordinal());
                            format!("{}={:?}", c.name(), v.unwrap_or(None))
                        })
                        .collect::<Vec<_>>()
                        .join(" ")
                })
                .collect()
        })
        .unwrap_or_default()
}

#[when(regex = r"^user (\d+) runs an? ([A-Z_]+) mission from planet (\d+) to planet (\d+) with (\d+) units? of id (\d+)$")]
async fn user_runs_mission(
    world: &mut BddWorld,
    user: i64,
    mission_type: String,
    source: i64,
    target: i64,
    count: i64,
    unit: i64,
) {
    world.captured_users.insert(user);
    ensure_ws_captures(world).await;

    let jwt = rest::mint_jwt(&world.db, user).await;
    let body = serde_json::json!({
        "userId": user,
        "sourcePlanetId": source,
        "targetPlanetId": target,
        "involvedUnits": [{"id": unit, "count": count}],
    });
    let verb = mission_verb(&mission_type);
    let (status, response) =
        rest::post_json(backend(world), &jwt, &format!("game/mission/{verb}"), &body).await;
    assert!(
        (200..300).contains(&status),
        "POST game/mission/{verb} returned HTTP {status}: {response}"
    );

    let mission_id: i64 = sqlx::query_scalar(
        "SELECT CAST(MAX(id) AS SIGNED) FROM missions WHERE user_id = ? AND resolved = 0",
    )
    .bind(user)
    .fetch_one(&world.db)
    .await
    .expect("read back created mission id");
    world.created_missions.push(mission_id);

    fire_and_await_mission(world, mission_id).await;
}

#[when(expr = "user {int} leaves planet {int}")]
async fn user_leaves_planet(world: &mut BddWorld, user: i64, planet: i64) {
    world.captured_users.insert(user);
    ensure_ws_captures(world).await;

    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) = rest::post_query(
        backend(world),
        &jwt,
        "game/planet/leave",
        &[("planetId", planet.to_string())],
    )
    .await;
    assert!(
        (200..300).contains(&status),
        "POST game/planet/leave returned HTTP {status}: {response}"
    );
    // synchronous endpoint — give post-commit emissions a moment
    tokio::time::sleep(Duration::from_secs(1)).await;
}
