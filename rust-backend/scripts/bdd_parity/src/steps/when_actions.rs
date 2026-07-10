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
        "EXPLORE" => "explorePlanet",
        "GATHER" => "gather",
        other => panic!("unknown mission type {other:?} — extend mission_verb()"),
    }
}

/// Nudge THIS mission's db-scheduler task into the past and poll
/// missions.resolved (§3 technique #1; nudges only our task_instance so
/// follow-up missions the execution creates are never fired prematurely —
/// the lesson mission_verify learned the hard way).
async fn fire_and_await_mission(world: &BddWorld, mission_id: i64) {
    // Rewind the mission's own termination_date too: a genuinely "resolved"
    // mission has its termination in the past. Without this, a later cancel
    // computes remaining-time from a termination still ~requiredTime in the
    // future, making the cancel-return's required_time equal the WALL-CLOCK
    // elapsed between registration and cancel — nondeterministic across passes
    // (java 5 vs rust 3.025 in the cancel-resolved-explore scenario). With the
    // rewind both backends clamp remaining to 0 and reuse the full
    // required_time, deterministically.
    sqlx::query(
        "UPDATE missions SET termination_date = DATE_SUB(NOW(6), INTERVAL 5 SECOND) \
         WHERE id = ?",
    )
    .bind(mission_id)
    .execute(&world.db)
    .await
    .expect("rewind missions.termination_date");
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
    crate::support::db::dump_rows(&world.db, sql).await
}

/// Shared body of the mission-registration When: POST, then (if 2xx) nudge the
/// created mission to resolution. `expect_success` distinguishes the asserting
/// W1 form from the `attempts` negative-path form (§6.6).
async fn register_mission(
    world: &mut BddWorld,
    user: i64,
    mission_type: &str,
    source: i64,
    target: i64,
    count: i64,
    unit: i64,
    expect_success: bool,
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
    let verb = mission_verb(mission_type);
    let (status, response) =
        rest::post_json(backend(world), &jwt, &format!("game/mission/{verb}"), &body).await;
    world.last_response = Some((status, response.clone()));

    if !expect_success {
        return; // the rejection Thens judge the stored response
    }
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
    register_mission(world, user, &mission_type, source, target, count, unit, true).await;
}

#[when(regex = r"^user (\d+) attempts an? ([A-Z_]+) mission from planet (\d+) to planet (\d+) with (\d+) units? of id (\d+)$")]
async fn user_attempts_mission(
    world: &mut BddWorld,
    user: i64,
    mission_type: String,
    source: i64,
    target: i64,
    count: i64,
    unit: i64,
) {
    register_mission(world, user, &mission_type, source, target, count, unit, false).await;
}

#[when(regex = r"^user (\d+) (builds|attempts to build) (\d+) units? of id (\d+) on planet (\d+)$")]
async fn user_builds_units(
    world: &mut BddWorld,
    user: i64,
    mode: String,
    count: i64,
    unit: i64,
    planet: i64,
) {
    world.captured_users.insert(user);
    world.registered_planets.insert(planet);
    ensure_ws_captures(world).await;
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) = rest::post_query(
        backend(world),
        &jwt,
        "game/unit/build",
        &[
            ("planetId", planet.to_string()),
            ("unitId", unit.to_string()),
            ("count", count.to_string()),
        ],
    )
    .await;
    world.last_response = Some((status, response.clone()));
    if mode == "builds" {
        assert!(
            (200..300).contains(&status),
            "POST game/unit/build returned HTTP {status}: {response}"
        );
        // FREEZE the build task — same D9 race as level-up: ZERO_BUILD_TIME
        // collapses required_time to 3 s and DELAY_HANDLE is 2 s, so the task
        // is due at +1 s and auto-fires MID-SCENARIO on whichever backend's
        // scheduler polls first (the Then raced completion: java lost, rust
        // won — flaky by construction). The completion nudge rewinds
        // execution_time, so freezing is transparent to it.
        let mission_id: Option<i64> = sqlx::query_scalar(
            "SELECT CAST(MAX(id) AS SIGNED) FROM missions WHERE user_id = ? AND resolved = 0",
        )
        .bind(user)
        .fetch_one(&world.db)
        .await
        .expect("find registered build mission id");
        let mission_id = mission_id.expect("build succeeded but no unresolved mission");
        sqlx::query(
            "UPDATE scheduled_tasks \
             SET execution_time = DATE_ADD(NOW(6), INTERVAL 1 HOUR) \
             WHERE task_name = 'mission-run' AND task_instance = ?",
        )
        .bind(mission_id.to_string())
        .execute(&world.db)
        .await
        .expect("freeze build scheduled task");
    }
    tokio::time::sleep(Duration::from_secs(1)).await;
}

#[when(expr = "user {int} cancels their build mission on planet {int}")]
async fn user_cancels_build(world: &mut BddWorld, user: i64, planet: i64) {
    world.captured_users.insert(user);
    ensure_ws_captures(world).await;
    // GET game/unit/cancel wants the mission id — resolve the unresolved
    // BUILD_UNIT mission on this planet (mirrors MissionFinderBo.findRunningUnitBuild)
    let mission_id: Option<i64> = sqlx::query_scalar(
        "SELECT CAST(MAX(m.id) AS SIGNED) FROM missions m \
         JOIN mission_types mt ON mt.id = m.type \
         JOIN mission_information mi ON mi.mission_id = m.id \
         WHERE m.user_id = ? AND mt.code = 'BUILD_UNIT' AND m.resolved = 0 AND mi.value = ?",
    )
    .bind(user)
    .bind(planet as f64)
    .fetch_one(&world.db)
    .await
    .expect("find running BUILD_UNIT mission");
    let Some(mission_id) = mission_id else {
        panic!("user {user} has no unresolved BUILD_UNIT mission on planet {planet}");
    };
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) = rest::get_query(
        backend(world),
        &jwt,
        "game/unit/cancel",
        &[("missionId", mission_id.to_string())],
    )
    .await;
    world.last_response = Some((status, response.clone()));
    assert!(
        (200..300).contains(&status),
        "GET game/unit/cancel returned HTTP {status}: {response}"
    );
    tokio::time::sleep(Duration::from_secs(1)).await;
}

#[when(regex = r"^user (\d+) (registers|attempts to register) a LEVEL_UP mission for upgrade (\d+)$")]
async fn user_registers_level_up(world: &mut BddWorld, user: i64, mode: String, upgrade: i64) {
    world.captured_users.insert(user);
    ensure_ws_captures(world).await;
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) = rest::get_query(
        backend(world),
        &jwt,
        "game/upgrade/registerLevelUp",
        &[("upgradeId", upgrade.to_string())],
    )
    .await;
    world.last_response = Some((status, response.clone()));
    if mode == "registers" {
        assert!(
            (200..300).contains(&status),
            "GET game/upgrade/registerLevelUp returned HTTP {status}: {response}"
        );
        // FREEZE the level-up task: ZERO_UPGRADE_TIME collapses required_time
        // to 3 s and DELAY_HANDLE is 2 s, so the task is due at +1 s and would
        // auto-fire MID-SCENARIO on whichever backend's scheduler polls first
        // (the D9 nondeterminism: Java completed + hard-deleted the mission
        // before its dump, Rust hadn't). A "running" mission must stay running
        // until an explicit completes/cancel step; the completion nudge
        // rewinds execution_time anyway, so freezing is transparent to it.
        // (two queries: an inline CAST-to-CHAR subquery hits MySQL error 1267 —
        // collation mix between the cast and the task_instance column; a bound
        // parameter, like the nudge uses, does not)
        let mission_id: Option<i64> = sqlx::query_scalar(
            "SELECT CAST(MAX(id) AS SIGNED) FROM missions WHERE user_id = ? AND resolved = 0",
        )
        .bind(user)
        .fetch_one(&world.db)
        .await
        .expect("find registered level-up mission id");
        let mission_id = mission_id.expect("registerLevelUp succeeded but no unresolved mission");
        sqlx::query(
            "UPDATE scheduled_tasks \
             SET execution_time = DATE_ADD(NOW(6), INTERVAL 1 HOUR) \
             WHERE task_name = 'mission-run' AND task_instance = ?",
        )
        .bind(mission_id.to_string())
        .execute(&world.db)
        .await
        .expect("freeze level-up scheduled task");
    }
}

#[when(regex = r"^user (\d+) (cancels|attempts to cancel) the running upgrade mission$")]
async fn user_cancels_upgrade(world: &mut BddWorld, user: i64, mode: String) {
    world.captured_users.insert(user);
    ensure_ws_captures(world).await;
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) =
        rest::get_query(backend(world), &jwt, "game/upgrade/cancelUpgrade", &[]).await;
    world.last_response = Some((status, response.clone()));
    if mode == "cancels" {
        assert!(
            (200..300).contains(&status),
            "GET game/upgrade/cancelUpgrade returned HTTP {status}: {response}"
        );
    }
    tokio::time::sleep(Duration::from_secs(1)).await;
}

#[when(regex = r"^user (\d+) (activates|attempts to activate) time special (\d+)$")]
async fn user_activates_time_special(
    world: &mut BddWorld,
    user: i64,
    mode: String,
    time_special: i64,
) {
    world.captured_users.insert(user);
    ensure_ws_captures(world).await;
    let jwt = rest::mint_jwt(&world.db, user).await;
    // @RequestBody Integer — the body is the bare JSON number
    let (status, response) = rest::post_json(
        backend(world),
        &jwt,
        "game/time_special/activate",
        &serde_json::json!(time_special),
    )
    .await;
    world.last_response = Some((status, response.clone()));
    if mode == "activates" {
        assert!(
            (200..300).contains(&status),
            "POST game/time_special/activate returned HTTP {status}: {response}"
        );
    }
    tokio::time::sleep(Duration::from_secs(1)).await;
}

#[when(regex = r"^user (\d+) (cancels|attempts to cancel) their latest mission$")]
async fn user_cancels_latest_mission(world: &mut BddWorld, user: i64, mode: String) {
    world.captured_users.insert(user);
    ensure_ws_captures(world).await;
    let mission_id = *world
        .created_missions
        .last()
        .expect("no mission was created by a previous When of this scenario");
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) = rest::post_query(
        backend(world),
        &jwt,
        "game/mission/cancel",
        &[("id", mission_id.to_string())],
    )
    .await;
    world.last_response = Some((status, response.clone()));
    if mode == "cancels" {
        assert!(
            (200..300).contains(&status),
            "POST game/mission/cancel returned HTTP {status}: {response}"
        );
    }
    tokio::time::sleep(Duration::from_secs(1)).await;
}

/// Generic completion nudge for the LATEST unresolved mission of a type —
/// polls for resolved=1 OR row deletion (BUILD_UNIT and some types hard-delete
/// on completion instead of resolving, §6.6).
#[when(regex = r"^the ([A-Z_]+) mission of user (\d+) completes$")]
async fn mission_of_type_completes(world: &mut BddWorld, mission_type: String, user: i64) {
    ensure_ws_captures(world).await;
    let mission_id: Option<i64> = sqlx::query_scalar(
        "SELECT CAST(MAX(m.id) AS SIGNED) FROM missions m \
         JOIN mission_types mt ON mt.id = m.type \
         WHERE m.user_id = ? AND mt.code = ? AND m.resolved = 0",
    )
    .bind(user)
    .bind(&mission_type)
    .fetch_one(&world.db)
    .await
    .expect("find latest unresolved mission of type");
    let Some(mission_id) = mission_id else {
        panic!("user {user} has no unresolved {mission_type} mission to complete");
    };
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
                .expect("poll mission");
        match resolved {
            None | Some(1) => {
                tokio::time::sleep(Duration::from_secs(2)).await;
                return;
            }
            _ => {}
        }
        if std::time::Instant::now() >= deadline {
            let rows =
                dump_rows(world, &format!("SELECT * FROM missions WHERE id = {mission_id}")).await;
            panic!(
                "{mission_type} mission {mission_id} neither resolved nor deleted within \
                 {MISSION_RESOLVE_TIMEOUT:?}; missions row: {rows:#?}"
            );
        }
        tokio::time::sleep(Duration::from_secs(1)).await;
    }
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
