//! JWT minting + REST helpers. Claim shape MUST stay identical to
//! scripts/mint_jwt.py (the debugging cross-check): HS256 over
//! {sub, iat, exp, data:{id, username, email}} with the secret from the
//! `JWT_SECRET` configuration row.

use serde::Serialize;
use tokio::sync::OnceCell;

use crate::support::backends::Backend;

static JWT_SECRET: OnceCell<String> = OnceCell::const_new();

#[derive(Serialize)]
struct ClaimData {
    id: i64,
    username: String,
    email: String,
}

#[derive(Serialize)]
struct Claims {
    sub: i64,
    iat: u64,
    exp: u64,
    data: ClaimData,
}

pub async fn jwt_secret(db: &sqlx::MySqlPool) -> String {
    JWT_SECRET
        .get_or_init(|| async {
            sqlx::query_scalar::<_, String>(
                "SELECT value FROM configuration WHERE name = 'JWT_SECRET'",
            )
            .fetch_one(db)
            .await
            .expect("JWT_SECRET configuration row must exist")
        })
        .await
        .clone()
}

pub async fn mint_jwt(db: &sqlx::MySqlPool, user_id: i64) -> String {
    let secret = jwt_secret(db).await;
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_secs();
    // The claims MUST carry the seed user's real username/email: the backends
    // treat the token as the account system's source of truth and SYNC the
    // user entity from it (Java UserSessionService.setUsername) — a hardcoded
    // "rusttester" claim renamed user 2 inside Java's session and leaked into
    // every DTO carrying a username (caught by the counterattack scenario).
    let (username, email): (String, String) =
        sqlx::query_as("SELECT username, email FROM user_storage WHERE id = ?")
            .bind(user_id)
            .fetch_one(db)
            .await
            .expect("mint_jwt: user must exist in user_storage");
    let claims = Claims {
        sub: user_id,
        iat: now,
        exp: now + 86400,
        data: ClaimData {
            id: user_id,
            username,
            email,
        },
    };
    jsonwebtoken::encode(
        &jsonwebtoken::Header::default(), // HS256
        &claims,
        &jsonwebtoken::EncodingKey::from_secret(secret.as_bytes()),
    )
    .expect("JWT encoding cannot fail with HS256")
}

/// POST with Bearer auth. Returns (status, body-text); the CALLER decides
/// whether a non-2xx is a scenario failure (§6.3: mission Whens require 2xx).
pub async fn post_json(
    backend: &Backend,
    jwt: &str,
    path: &str,
    body: &serde_json::Value,
) -> (u16, String) {
    let url = format!("{}/{}", backend.base_url.trim_end_matches('/'), path);
    let resp = reqwest::Client::new()
        .post(&url)
        .bearer_auth(jwt)
        .json(body)
        .send()
        .await
        .unwrap_or_else(|e| panic!("POST {url} failed to send: {e}"));
    let status = resp.status().as_u16();
    let text = resp.text().await.unwrap_or_default();
    (status, text)
}

/// GET with Bearer auth (some mutating game endpoints are GETs, e.g.
/// game/upgrade/registerLevelUp).
pub async fn get_query(
    backend: &Backend,
    jwt: &str,
    path: &str,
    query: &[(&str, String)],
) -> (u16, String) {
    let url = format!("{}/{}", backend.base_url.trim_end_matches('/'), path);
    let resp = reqwest::Client::new()
        .get(&url)
        .bearer_auth(jwt)
        .query(query)
        .send()
        .await
        .unwrap_or_else(|e| panic!("GET {url} failed to send: {e}"));
    let status = resp.status().as_u16();
    let text = resp.text().await.unwrap_or_default();
    (status, text)
}

/// Mint an ADMIN JWT: HS256 over the `ADMIN_JWT_SECRET` configuration row
/// (seeded by the runner BEFORE the baseline dump, so restores keep it and
/// both backends' boot-time read stays valid). Same claim shape as
/// `AdminUserBo.createToken` ({sub, iat, exp, data}); the `/admin/**` filters
/// only validate the signature.
pub async fn mint_admin_jwt(db: &sqlx::MySqlPool) -> String {
    let secret: String =
        sqlx::query_scalar("SELECT value FROM configuration WHERE name = 'ADMIN_JWT_SECRET'")
            .fetch_one(db)
            .await
            .expect("ADMIN_JWT_SECRET configuration row must exist (runner seeds it)");
    let now = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .unwrap()
        .as_secs();
    let claims = serde_json::json!({
        "sub": 1,
        "iat": now,
        "exp": now + 86400,
        "data": {"id": 1, "username": "bdd_admin", "email": "bdd@admin.local", "enabled": true},
    });
    jsonwebtoken::encode(
        &jsonwebtoken::Header::default(), // HS256
        &claims,
        &jsonwebtoken::EncodingKey::from_secret(secret.as_bytes()),
    )
    .expect("admin JWT encoding cannot fail with HS256")
}

/// DELETE with Bearer auth and no body (e.g. game/alliance,
/// game/planet-list/{id}).
pub async fn delete_path(backend: &Backend, jwt: &str, path: &str) -> (u16, String) {
    let url = format!("{}/{}", backend.base_url.trim_end_matches('/'), path);
    let resp = reqwest::Client::new()
        .delete(&url)
        .bearer_auth(jwt)
        .send()
        .await
        .unwrap_or_else(|e| panic!("DELETE {url} failed to send: {e}"));
    let status = resp.status().as_u16();
    let text = resp.text().await.unwrap_or_default();
    (status, text)
}

/// POST with query params and empty body (e.g. game/planet/leave?planetId=N).
pub async fn post_query(
    backend: &Backend,
    jwt: &str,
    path: &str,
    query: &[(&str, String)],
) -> (u16, String) {
    let url = format!("{}/{}", backend.base_url.trim_end_matches('/'), path);
    let resp = reqwest::Client::new()
        .post(&url)
        .bearer_auth(jwt)
        .query(query)
        .header("Content-Length", "0")
        .send()
        .await
        .unwrap_or_else(|e| panic!("POST {url} failed to send: {e}"));
    let status = resp.status().as_u16();
    let text = resp.text().await.unwrap_or_default();
    (status, text)
}
