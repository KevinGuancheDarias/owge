//! Shared application state — the Rust analogue of Spring's singleton bean
//! graph. Cloned cheaply into every handler (`Arc` inside the pool, plain
//! copies of the small config structs).

use std::sync::Arc;

use owge_business::bo::admin_user_bo::AdminJwtSettings;
use owge_business::config::{
    self, ADMIN_JWT_ALGO_CODE, ADMIN_JWT_DURATION_CODE, ADMIN_JWT_SECRET_CODE, EnvConfig,
    GAME_JWT_SECRET_CODE,
};
use owge_business::db::{self, Db};
use owge_business::jwt::TokenConfig;
use owge_business::{OwgeError, OwgeResult};

use owge_business::bo::ConfigurationBo;

/// The two JWT token configurations, one per auth scope, resolved at boot —
/// the Rust equivalent of the `gameOwgeTokenConfigLoader` /
/// `adminOwgeTokenConfigLoader` beans.
#[derive(Clone)]
pub struct AppState {
    pub db: Db,
    /// Verifies inbound `/game/**` tokens (HMAC in dev, RSA under `rsaKeys`).
    pub game_token: TokenConfig,
    /// Verifies inbound `/admin/**` tokens (always HMAC).
    pub admin_token: TokenConfig,
    /// Settings for *minting* admin tokens at `/game/adminLogin`.
    pub admin_jwt: AdminJwtSettings,
    pub env: Arc<EnvConfig>,
}

impl AppState {
    /// Build the pool and resolve all JWT material from env + the
    /// `configuration` table, reproducing the Spring security bean wiring.
    pub async fn bootstrap(env: EnvConfig) -> OwgeResult<Self> {
        let db = db::create_pool(&env.database_url, env.db_max_connections).await?;
        let mut conn = db.acquire().await?;

        // --- Game token config: RSA under the `rsaKeys` profile, else HMAC. ---
        let game_token = if env.has_profile("rsaKeys") {
            let pem = std::fs::read_to_string(&env.rsa_public_key_path).map_err(|e| {
                OwgeError::Common(format!(
                    "Cannot read RSA public key {}: {e}",
                    env.rsa_public_key_path
                ))
            })?;
            TokenConfig::rsa(pem, env.clock_skew_seconds)
        } else {
            let secret = ConfigurationBo::find_value(&mut conn, GAME_JWT_SECRET_CODE).await?;
            // Dev game tokens are HMAC; algorithm is fixed HS256 on the verify
            // side (jsonwebtoken keys the validator by algorithm). The secret
            // byte interpretation is operator-pinned (see EnvConfig docs / the
            // raw-vs-base64 compatibility note).
            let mut cfg = TokenConfig::secret(
                secret,
                jsonwebtoken::Algorithm::HS256,
                env.clock_skew_seconds,
            );
            cfg.secret_encoding = env.game_secret_encoding;
            cfg
        };

        // --- Admin token config (always HMAC). ---
        let admin_secret = ConfigurationBo::find_or_set_default(
            &mut conn,
            ADMIN_JWT_SECRET_CODE,
            &gen_random_secret(),
        )
        .await?
        .value;
        let admin_algo_name =
            ConfigurationBo::find_or_set_default(&mut conn, ADMIN_JWT_ALGO_CODE, "HS256")
                .await?
                .value;
        let admin_algo = config::parse_algorithm(&admin_algo_name)?;
        let admin_duration: i64 =
            ConfigurationBo::find_or_set_default(&mut conn, ADMIN_JWT_DURATION_CODE, "86400")
                .await?
                .value
                .parse()
                .map_err(|_| {
                    OwgeError::Common("ADMIN_JWT_DURATION_SECONDS is not an integer".into())
                })?;
        drop(conn);

        let admin_token =
            TokenConfig::secret(admin_secret.clone(), admin_algo, env.clock_skew_seconds);
        let admin_jwt = AdminJwtSettings {
            secret: admin_secret,
            algorithm: admin_algo,
            duration_seconds: admin_duration,
        };

        Ok(Self {
            db,
            game_token,
            admin_token,
            admin_jwt,
            env: Arc::new(env),
        })
    }
}

/// Mirrors `AdminTokenConfigLoader.genRandomTokenSecret` — a throwaway default
/// secret only used the very first time the row is missing.
fn gen_random_secret() -> String {
    // Deterministic-but-unique-enough seed without pulling an rng crate; the
    // value is immediately persisted and reused thereafter.
    let nanos = std::time::SystemTime::now()
        .duration_since(std::time::UNIX_EPOCH)
        .map(|d| d.as_nanos())
        .unwrap_or(0);
    format!("{}", 5000 + (nanos % 10000))
}
