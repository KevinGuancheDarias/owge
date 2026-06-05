//! JWT verification and issuance, faithfully reproducing the dual-mode scheme
//! of the Java backend (`kevinsuite-java` `JwtAuthenticationFilter` +
//! OWGE `JwtService` / `*TokenConfigLoader`).
//!
//! Two verification methods, selected per token-config-loader, exactly like
//! `TokenVerificationMethod`:
//!
//! * [`VerificationMethod::Secret`] — symmetric HMAC. Used by the admin panel
//!   always, and by the game login on dev machines
//!   (`DevelopmentSgtTokenConfigLoader`).
//! * [`VerificationMethod::RsaKey`] — asymmetric RSA, verifying against a public
//!   key PEM. Used by the game login in production (`SgtTokenConfigLoader`,
//!   active under the `rsaKeys` Spring profile).
//!
//! ## Compatibility note on the HMAC secret bytes
//!
//! jjwt is inconsistent about how a `String` secret is turned into key bytes:
//! `JwtService.buildToken` signs with `secret.getBytes()` (**raw** UTF-8
//! bytes), whereas kevinsuite's verifier uses `parser.setSigningKey(String)`
//! which, depending on the jjwt version, may **Base64-decode** the secret
//! first. To stay bug-for-bug compatible regardless of which path issued a
//! token, [`SecretEncoding`] makes the interpretation explicit; the default is
//! [`SecretEncoding::Raw`], matching `JwtService` (the path OWGE itself uses to
//! mint admin tokens).

use base64::Engine;
use jsonwebtoken::{
    decode, encode, Algorithm, DecodingKey, EncodingKey, Header, Validation,
};
use serde::{Deserialize, Serialize};

use crate::error::{OwgeError, OwgeResult};

/// Mirror of `com.kevinguanchedarias...enumerations.TokenVerificationMethod`.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum VerificationMethod {
    Secret,
    RsaKey,
}

/// How a `String` HMAC secret is converted to key bytes. See module docs.
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum SecretEncoding {
    /// `secret.getBytes()` — raw UTF-8 bytes (matches OWGE `JwtService`).
    Raw,
    /// Base64-decode the secret first (matches some jjwt `setSigningKey`
    /// String-overload versions).
    Base64,
}

/// Equivalent of an OWGE `TokenConfigLoader` implementation: everything needed
/// to verify (and, for HMAC, issue) tokens for one authentication scope.
#[derive(Debug, Clone)]
pub struct TokenConfig {
    pub method: VerificationMethod,
    /// HMAC shared secret (used when `method == Secret`).
    pub secret: Option<String>,
    pub secret_encoding: SecretEncoding,
    /// RSA public key in PEM form (PKCS#1 or SPKI; used when `method == RsaKey`).
    pub public_key_pem: Option<String>,
    /// RSA private key in PEM form (PKCS#8) — only needed to *issue* RSA tokens.
    pub private_key_pem: Option<String>,
    /// Signature algorithm. Game RSA tokens are typically `RS256`; admin HMAC
    /// tokens default to `HS256` (configurable via `ADMIN_JWT_ALGO`).
    pub algorithm: Algorithm,
    /// Allowed clock skew in seconds (`OWGE_CLOCK_SKEW`, default 300 in prod,
    /// 3600 in dev).
    pub leeway_seconds: u64,
}

impl TokenConfig {
    /// An HMAC/secret config (admin panel, or dev game login).
    pub fn secret(secret: impl Into<String>, algorithm: Algorithm, leeway_seconds: u64) -> Self {
        Self {
            method: VerificationMethod::Secret,
            secret: Some(secret.into()),
            secret_encoding: SecretEncoding::Raw,
            public_key_pem: None,
            private_key_pem: None,
            algorithm,
            leeway_seconds,
        }
    }

    /// An RSA config (production game login). Verification only needs the
    /// public key; pass `private_key_pem` if this process must also issue.
    pub fn rsa(public_key_pem: impl Into<String>, leeway_seconds: u64) -> Self {
        Self {
            method: VerificationMethod::RsaKey,
            secret: None,
            secret_encoding: SecretEncoding::Raw,
            public_key_pem: Some(public_key_pem.into()),
            private_key_pem: None,
            algorithm: Algorithm::RS256,
            leeway_seconds,
        }
    }

    fn hmac_key_bytes(&self) -> OwgeResult<Vec<u8>> {
        let secret = self
            .secret
            .as_ref()
            .ok_or_else(|| OwgeError::Common("JWT secret not configured".into()))?;
        Ok(match self.secret_encoding {
            SecretEncoding::Raw => secret.as_bytes().to_vec(),
            SecretEncoding::Base64 => base64::engine::general_purpose::STANDARD
                .decode(secret)
                .map_err(|e| OwgeError::Common(format!("JWT secret is not valid base64: {e}")))?,
        })
    }

    fn decoding_key(&self) -> OwgeResult<DecodingKey> {
        match self.method {
            VerificationMethod::Secret => Ok(DecodingKey::from_secret(&self.hmac_key_bytes()?)),
            VerificationMethod::RsaKey => {
                let pem = self.public_key_pem.as_ref().ok_or_else(|| {
                    OwgeError::Common("RSA public key not configured".into())
                })?;
                DecodingKey::from_rsa_pem(pem.as_bytes()).map_err(Into::into)
            }
        }
    }
}

/// The authenticated principal extracted from a token's `data` claim — the
/// Rust equivalent of kevinsuite's `TokenUser`.
#[derive(Debug, Clone, Serialize, Deserialize)]
pub struct TokenUser {
    pub id: i64,
    pub username: String,
    #[serde(default)]
    pub email: Option<String>,
}

/// The `data` object inside the JWT. The account system / admin login may nest
/// extra fields (faction, etc.); we only bind the three kevinsuite reads.
#[derive(Debug, Deserialize)]
struct DataClaim {
    id: serde_json::Value,
    username: String,
    #[serde(default)]
    email: Option<String>,
}

#[derive(Debug, Deserialize)]
struct OwgeClaims {
    data: DataClaim,
}

/// Claims emitted when OWGE itself mints a token (admin login). Matches
/// `AdminUserBo.createToken`: `sub`, `iat`, `exp`, `data`.
#[derive(Debug, Serialize)]
struct IssuedClaims<'a> {
    sub: i64,
    iat: i64,
    exp: i64,
    data: IssuedData<'a>,
}

#[derive(Debug, Serialize)]
struct IssuedData<'a> {
    id: i64,
    username: &'a str,
    email: Option<&'a str>,
}

/// Verify a token and extract its [`TokenUser`], reproducing
/// `JwtAuthenticationFilter.decodeTokenIfPossible` +
/// `getTokenClaimsIfNotExpired`.
pub fn decode_token(config: &TokenConfig, token: &str) -> OwgeResult<TokenUser> {
    let mut validation = Validation::new(config.algorithm);
    validation.leeway = config.leeway_seconds;
    // The Java filter only validates expiry + signature; it does not check aud.
    validation.validate_aud = false;

    let key = config.decoding_key()?;
    let data = decode::<OwgeClaims>(token, &key, &validation)
        .map_err(|e| OwgeError::Unauthorized(format!("Invalid token: {e}")))?
        .claims
        .data;

    // The id arrives as a JSON number (admin) or possibly a string; coerce.
    let id = match data.id {
        serde_json::Value::Number(n) => n
            .as_i64()
            .ok_or_else(|| OwgeError::Unauthorized("Token id is not an integer".into()))?,
        serde_json::Value::String(s) => s
            .parse::<i64>()
            .map_err(|_| OwgeError::Unauthorized("Token id is not an integer".into()))?,
        _ => return Err(OwgeError::Unauthorized("Token missing numeric id".into())),
    };

    Ok(TokenUser {
        id,
        username: data.username,
        email: data.email,
    })
}

/// Issue an HMAC-signed token, reproducing
/// `JwtService.buildToken(claims, algo, secret)` which signs with
/// `secret.getBytes()` (raw bytes) — used by the admin login.
pub fn build_hmac_token(
    secret: &str,
    algorithm: Algorithm,
    user: &TokenUser,
    issued_at: i64,
    expires_at: i64,
) -> OwgeResult<String> {
    if !matches!(
        algorithm,
        Algorithm::HS256 | Algorithm::HS384 | Algorithm::HS512
    ) {
        return Err(OwgeError::Common(format!(
            "build_hmac_token called with non-HMAC algorithm {algorithm:?}"
        )));
    }
    let claims = IssuedClaims {
        sub: user.id,
        iat: issued_at,
        exp: expires_at,
        data: IssuedData {
            id: user.id,
            username: &user.username,
            email: user.email.as_deref(),
        },
    };
    let key = EncodingKey::from_secret(secret.as_bytes());
    encode(&Header::new(algorithm), &claims, &key).map_err(Into::into)
}

#[cfg(test)]
mod tests {
    use super::*;

    fn user() -> TokenUser {
        TokenUser {
            id: 42,
            username: "kevin".into(),
            email: Some("kevin@example.com".into()),
        }
    }

    #[test]
    fn hmac_round_trip_raw_bytes() {
        // Reproduces: sign with secret.getBytes(), then verify. This is the
        // path OWGE uses for admin tokens.
        let secret = "super-secret-value";
        let token = build_hmac_token(secret, Algorithm::HS256, &user(), 1_700_000_000, 4_102_444_800)
            .expect("sign");

        let cfg = TokenConfig::secret(secret, Algorithm::HS256, 300);
        let decoded = decode_token(&cfg, &token).expect("verify");
        assert_eq!(decoded.id, 42);
        assert_eq!(decoded.username, "kevin");
        assert_eq!(decoded.email.as_deref(), Some("kevin@example.com"));
    }

    #[test]
    fn expired_token_is_rejected() {
        let secret = "another-secret";
        // exp well in the past, issued_at older still.
        let token =
            build_hmac_token(secret, Algorithm::HS256, &user(), 1_600_000_000, 1_600_000_300)
                .expect("sign");
        let cfg = TokenConfig::secret(secret, Algorithm::HS256, 0);
        let err = decode_token(&cfg, &token).expect_err("must reject expired");
        assert!(matches!(err, OwgeError::Unauthorized(_)));
    }

    #[test]
    fn hmac_base64_secret_encoding() {
        // Reproduces the jjwt `signWith(algo, String)` / `setSigningKey(String)`
        // path: the configured secret string is Base64, and the *decoded* bytes
        // are the HMAC key. This is the path the external account system + the
        // kevinsuite verifier use, selectable via SecretEncoding::Base64.
        use jsonwebtoken::{encode, EncodingKey, Header};
        use serde_json::json;

        let secret_b64 = "c3VwZXItc2VjcmV0LWtleS1tYXRlcmlhbA=="; // base64 of some key
        let key_bytes = base64::engine::general_purpose::STANDARD
            .decode(secret_b64)
            .unwrap();
        let claims = json!({
            "exp": 4_102_444_800i64,
            "data": { "id": 9, "username": "b64user", "email": "b@x.io" }
        });
        let token = encode(
            &Header::new(Algorithm::HS256),
            &claims,
            &EncodingKey::from_secret(&key_bytes),
        )
        .unwrap();

        let mut cfg = TokenConfig::secret(secret_b64, Algorithm::HS256, 300);
        cfg.secret_encoding = SecretEncoding::Base64;
        let user = decode_token(&cfg, &token).expect("verify with base64-decoded key");
        assert_eq!(user.id, 9);
        assert_eq!(user.username, "b64user");

        // And the Raw interpretation must NOT validate the same token.
        let raw_cfg = TokenConfig::secret(secret_b64, Algorithm::HS256, 300);
        assert!(decode_token(&raw_cfg, &token).is_err());
    }

    #[test]
    fn wrong_secret_is_rejected() {
        let token =
            build_hmac_token("right", Algorithm::HS256, &user(), 1_700_000_000, 4_102_444_800)
                .expect("sign");
        let cfg = TokenConfig::secret("wrong", Algorithm::HS256, 300);
        assert!(decode_token(&cfg, &token).is_err());
    }
}
