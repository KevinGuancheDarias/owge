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
    let claims = Claims {
        sub: user_id,
        iat: now,
        exp: now + 86400,
        data: ClaimData {
            id: user_id,
            username: "rusttester".into(),
            email: "rust@test.local".into(),
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
