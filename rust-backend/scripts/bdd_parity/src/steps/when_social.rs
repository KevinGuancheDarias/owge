//! When steps for the "social" surfaces: alliances and planet lists.
//! Same conventions as when_actions.rs: the asserting form requires 2xx, the
//! `attempts` form stores the response for the rejection Thens, and every
//! step gives post-commit emissions a settle second.

use std::time::Duration;

use cucumber::when;

use crate::support::rest;
use crate::world::BddWorld;

fn backend(world: &BddWorld) -> &crate::support::backends::Backend {
    world
        .backend
        .as_ref()
        .expect("this scenario has a When step — OWGE_BDD_BACKEND must be java|rust")
}

async fn settle() {
    tokio::time::sleep(Duration::from_secs(1)).await;
}

#[when(regex = r#"^user (\d+) (creates|attempts to create) an alliance named "([^"]+)"$"#)]
async fn user_creates_alliance(world: &mut BddWorld, user: i64, mode: String, name: String) {
    world.captured_users.insert(user);
    super::when_actions::ensure_ws_captures(world).await;
    let jwt = rest::mint_jwt(&world.db, user).await;
    let body = serde_json::json!({"name": name, "description": "bdd-parity alliance"});
    let (status, response) = rest::post_json(backend(world), &jwt, "game/alliance", &body).await;
    world.last_response = Some((status, response.clone()));
    if mode == "creates" {
        assert!(
            (200..300).contains(&status),
            "POST game/alliance returned HTTP {status}: {response}"
        );
    }
    settle().await;
}

#[when(expr = "user {int} requests to join the alliance owned by user {int}")]
async fn user_requests_join(world: &mut BddWorld, user: i64, owner: i64) {
    world.captured_users.insert(user);
    super::when_actions::ensure_ws_captures(world).await;
    let alliance_id: Option<i64> = sqlx::query_scalar(
        "SELECT CAST(id AS SIGNED) FROM alliances WHERE owner_id = ?",
    )
    .bind(owner)
    .fetch_optional(&world.db)
    .await
    .expect("find alliance by owner");
    let Some(alliance_id) = alliance_id else {
        panic!("user {owner} owns no alliance to join");
    };
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) = rest::post_json(
        backend(world),
        &jwt,
        "game/alliance/requestJoin",
        &serde_json::json!({"allianceId": alliance_id}),
    )
    .await;
    world.last_response = Some((status, response.clone()));
    assert!(
        (200..300).contains(&status),
        "POST game/alliance/requestJoin returned HTTP {status}: {response}"
    );
    settle().await;
}

#[when(regex = r"^user (\d+) (accepts|rejects) the join request of user (\d+)$")]
async fn user_handles_join_request(world: &mut BddWorld, user: i64, mode: String, requester: i64) {
    world.captured_users.insert(user);
    super::when_actions::ensure_ws_captures(world).await;
    let request_id: Option<i64> = sqlx::query_scalar(
        "SELECT CAST(MAX(id) AS SIGNED) FROM alliance_join_request WHERE user_id = ?",
    )
    .bind(requester)
    .fetch_one(&world.db)
    .await
    .expect("find join request by requester");
    let Some(request_id) = request_id else {
        panic!("user {requester} has no pending alliance join request");
    };
    let path = if mode == "accepts" {
        "game/alliance/acceptJoinRequest"
    } else {
        "game/alliance/rejectJoinRequest"
    };
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) = rest::post_json(
        backend(world),
        &jwt,
        path,
        &serde_json::json!({"joinRequestId": request_id}),
    )
    .await;
    world.last_response = Some((status, response.clone()));
    assert!(
        (200..300).contains(&status),
        "POST {path} returned HTTP {status}: {response}"
    );
    settle().await;
}

#[when(regex = r"^user (\d+) (leaves|attempts to leave) their alliance$")]
async fn user_leaves_alliance(world: &mut BddWorld, user: i64, mode: String) {
    world.captured_users.insert(user);
    super::when_actions::ensure_ws_captures(world).await;
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) =
        rest::post_json(backend(world), &jwt, "game/alliance/leave", &serde_json::json!({}))
            .await;
    world.last_response = Some((status, response.clone()));
    if mode == "leaves" {
        assert!(
            (200..300).contains(&status),
            "POST game/alliance/leave returned HTTP {status}: {response}"
        );
    }
    settle().await;
}

#[when(regex = r"^user (\d+) (deletes|attempts to delete) their alliance$")]
async fn user_deletes_alliance(world: &mut BddWorld, user: i64, mode: String) {
    world.captured_users.insert(user);
    super::when_actions::ensure_ws_captures(world).await;
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) = rest::delete_path(backend(world), &jwt, "game/alliance").await;
    world.last_response = Some((status, response.clone()));
    if mode == "deletes" {
        assert!(
            (200..300).contains(&status),
            "DELETE game/alliance returned HTTP {status}: {response}"
        );
    }
    settle().await;
}

#[when(regex = r#"^user (\d+) adds planet (\d+) to their planet list as "([^"]+)"$"#)]
async fn user_adds_planet_list(world: &mut BddWorld, user: i64, planet: i64, name: String) {
    world.captured_users.insert(user);
    world.registered_planets.insert(planet);
    super::when_actions::ensure_ws_captures(world).await;
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) = rest::post_json(
        backend(world),
        &jwt,
        "game/planet-list",
        &serde_json::json!({"planetId": planet, "name": name}),
    )
    .await;
    world.last_response = Some((status, response.clone()));
    assert!(
        (200..300).contains(&status),
        "POST game/planet-list returned HTTP {status}: {response}"
    );
    settle().await;
}

#[when(expr = "user {int} removes planet {int} from their planet list")]
async fn user_removes_planet_list(world: &mut BddWorld, user: i64, planet: i64) {
    world.captured_users.insert(user);
    super::when_actions::ensure_ws_captures(world).await;
    let jwt = rest::mint_jwt(&world.db, user).await;
    let (status, response) =
        rest::delete_path(backend(world), &jwt, &format!("game/planet-list/{planet}")).await;
    world.last_response = Some((status, response.clone()));
    assert!(
        (200..300).contains(&status),
        "DELETE game/planet-list/{planet} returned HTTP {status}: {response}"
    );
    settle().await;
}
