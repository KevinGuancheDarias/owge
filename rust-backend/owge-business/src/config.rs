//! Runtime configuration, assembled from environment variables (mirroring the
//! Spring `application.properties` / env-driven config) plus values read from
//! the `configuration` DB table at startup.

use std::env;

use jsonwebtoken::Algorithm;

use crate::error::{OwgeError, OwgeResult};
use crate::jwt::SecretEncoding;

/// Config codes stored in the `configuration` table (see `ConfigurationBo`).
pub const GAME_JWT_SECRET_CODE: &str = "JWT_SECRET";
pub const ADMIN_JWT_SECRET_CODE: &str = "ADMIN_JWT_SECRET";
pub const ADMIN_JWT_ALGO_CODE: &str = "ADMIN_JWT_ALGO";
pub const ADMIN_JWT_DURATION_CODE: &str = "ADMIN_JWT_DURATION_SECONDS";

/// Process-level configuration resolved purely from the environment. The
/// JWT *secrets* are resolved later from the DB (they live in the
/// `configuration` table), so they are not present here.
#[derive(Debug, Clone)]
pub struct EnvConfig {
    pub database_url: String,
    pub db_max_connections: u32,
    pub server_host: String,
    pub server_port: u16,
    /// Socket.io realtime server bind host/port. Mirrors `WebsocketConfiguration`
    /// (`OWGE_WS_HOST`/`OWGE_WS_PORT`, defaults `0.0.0.0:7474`). nginx proxies
    /// `location /websocket/` to this port at engine.io path `/socket.io`.
    pub ws_host: String,
    pub ws_port: u16,
    /// Active Spring-style profiles (`OWGE_PROFILES`, comma separated). The
    /// presence of `rsaKeys` flips the game login from HMAC to RSA, exactly as
    /// `SecurityBeansConfiguration.gameOwgeTokenConfigLoader` does.
    pub profiles: Vec<String>,
    /// Paths to the RSA key PEM files (production game login). Defaults match
    /// `SgtTokenConfigLoader`.
    pub rsa_public_key_path: String,
    pub rsa_private_key_path: String,
    /// Allowed JWT clock skew in seconds (`OWGE_CLOCK_SKEW`).
    pub clock_skew_seconds: u64,
    /// How the HMAC `JWT_SECRET` string is turned into key bytes for *game*
    /// (player) token verification. This is the #1 compatibility knob: OWGE's
    /// own `JwtService` signs with raw `secret.getBytes()`, but jjwt's
    /// `setSigningKey(String)` verify path (used by the external account system
    /// and kevinsuite) Base64-decodes. Pin this against a real production token
    /// per `docs/PORTING-ROADMAP.md`. Env: `OWGE_GAME_SECRET_ENCODING=raw|base64`.
    pub game_secret_encoding: SecretEncoding,
    /// CORS allowed origins (`OWGE_CORS_ORIGINS`, comma separated; `*` = any).
    pub cors_origins: Vec<String>,
    /// HTTP context path under which all REST routes are served
    /// (`OWGE_CONTEXT_PATH`), the Rust equivalent of the Java
    /// `server.servlet.context-path` / `OWGE_CONTEXT_PATH=/game_api`. Empty (the
    /// default) serves at the root; `/game_api` makes the app answer
    /// `/game_api/open/clock` etc., so the same nginx reverse proxy can front
    /// either backend unchanged. Does NOT affect the socket.io listener.
    pub context_path: String,
}

impl EnvConfig {
    pub fn from_env() -> OwgeResult<Self> {
        let database_url = env::var("OWGE_DB_JDBC_URL")
            .or_else(|_| env::var("DATABASE_URL"))
            .map_err(|_| {
                OwgeError::Common(
                    "Missing OWGE_DB_JDBC_URL / DATABASE_URL (mysql://user:pass@host:port/db)"
                        .into(),
                )
            })?;

        let profiles = env::var("OWGE_PROFILES")
            .unwrap_or_default()
            .split(',')
            .map(|s| s.trim().to_string())
            .filter(|s| !s.is_empty())
            .collect();

        Ok(Self {
            database_url,
            db_max_connections: parse_env("OWGE_DB_MAX_CONNECTIONS", 16),
            server_host: env::var("OWGE_SERVER_HOST").unwrap_or_else(|_| "0.0.0.0".into()),
            server_port: parse_env("OWGE_SERVER_PORT", 8080),
            ws_host: env::var("OWGE_WS_HOST").unwrap_or_else(|_| "0.0.0.0".into()),
            ws_port: parse_env("OWGE_WS_PORT", 7474),
            profiles,
            rsa_public_key_path: env::var("OWGE_RSA_PUBLIC_KEY")
                .unwrap_or_else(|_| "/var/owge_data/keys/public.key".into()),
            rsa_private_key_path: env::var("OWGE_RSA_PRIVATE_KEY")
                .unwrap_or_else(|_| "/var/owge_data/keys/private.key".into()),
            clock_skew_seconds: parse_env("OWGE_CLOCK_SKEW", 300),
            game_secret_encoding: match env::var("OWGE_GAME_SECRET_ENCODING")
                .unwrap_or_else(|_| "raw".into())
                .trim()
                .to_lowercase()
                .as_str()
            {
                "base64" => SecretEncoding::Base64,
                _ => SecretEncoding::Raw,
            },
            cors_origins: env::var("OWGE_CORS_ORIGINS")
                .unwrap_or_else(|_| "*".into())
                .split(',')
                .map(|s| s.trim().to_string())
                .filter(|s| !s.is_empty())
                .collect(),
            context_path: normalize_context_path(
                &env::var("OWGE_CONTEXT_PATH").unwrap_or_default(),
            ),
        })
    }

    pub fn has_profile(&self, profile: &str) -> bool {
        self.profiles.iter().any(|p| p == profile)
    }
}

/// Normalize `OWGE_CONTEXT_PATH`: empty or `/` => `""` (serve at root); otherwise
/// ensure a single leading slash and no trailing slash (axum `nest` requires a
/// non-empty prefix that does not end in `/`). E.g. `game_api`, `/game_api`, and
/// `/game_api/` all become `/game_api`.
fn normalize_context_path(raw: &str) -> String {
    let trimmed = raw.trim().trim_matches('/');
    if trimmed.is_empty() {
        String::new()
    } else {
        format!("/{trimmed}")
    }
}

/// Parse a JWT algorithm name as stored in `ADMIN_JWT_ALGO` (e.g. `HS256`),
/// matching `SignatureAlgorithm.valueOf(...)`.
pub fn parse_algorithm(name: &str) -> OwgeResult<Algorithm> {
    match name.trim().to_uppercase().as_str() {
        "HS256" => Ok(Algorithm::HS256),
        "HS384" => Ok(Algorithm::HS384),
        "HS512" => Ok(Algorithm::HS512),
        "RS256" => Ok(Algorithm::RS256),
        "RS384" => Ok(Algorithm::RS384),
        "RS512" => Ok(Algorithm::RS512),
        other => Err(OwgeError::Common(format!(
            "Unsupported JWT algorithm: {other}"
        ))),
    }
}

fn parse_env<T: std::str::FromStr>(key: &str, default: T) -> T {
    env::var(key)
        .ok()
        .and_then(|v| v.parse().ok())
        .unwrap_or(default)
}
