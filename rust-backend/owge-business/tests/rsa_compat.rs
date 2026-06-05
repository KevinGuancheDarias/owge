//! Proves the `RSA_KEY` verification path (production game login) accepts an
//! RS256 token signed by a PKCS#8 private key and verified against the matching
//! SPKI ("BEGIN PUBLIC KEY") public key — the exact PEM formats the Java
//! backend's `SgtTokenConfigLoader` loads from `/var/owge_data/keys`.

use jsonwebtoken::{encode, Algorithm, EncodingKey, Header};
use owge_business::jwt::{decode_token, TokenConfig};
use serde_json::json;

const PRIVATE_PEM: &str = include_str!("fixtures/test_rsa_private.pem");
const PUBLIC_PEM: &str = include_str!("fixtures/test_rsa_public.pem");

#[test]
fn rsa_token_is_verified_against_public_key() {
    // Mint a token the way the external account system would: `data` holds the
    // user, signed RS256 with the private key.
    let now = chrono::Utc::now().timestamp();
    let claims = json!({
        "sub": 7,
        "iat": now,
        "exp": now + 3600,
        "data": { "id": 7, "username": "player7", "email": "p7@example.com" }
    });
    let key = EncodingKey::from_rsa_pem(PRIVATE_PEM.as_bytes()).expect("load private key");
    let token = encode(&Header::new(Algorithm::RS256), &claims, &key).expect("sign RS256");

    let cfg = TokenConfig::rsa(PUBLIC_PEM, 300);
    let user = decode_token(&cfg, &token).expect("verify with public key");
    assert_eq!(user.id, 7);
    assert_eq!(user.username, "player7");
    assert_eq!(user.email.as_deref(), Some("p7@example.com"));
}

#[test]
fn rsa_rejects_token_signed_by_a_different_key() {
    // A token signed with an HMAC secret must not pass RSA verification.
    let now = chrono::Utc::now().timestamp();
    let claims = json!({
        "exp": now + 3600,
        "data": { "id": 1, "username": "x" }
    });
    let key = EncodingKey::from_secret(b"not-the-rsa-key");
    let token = encode(&Header::new(Algorithm::HS256), &claims, &key).expect("sign");

    let cfg = TokenConfig::rsa(PUBLIC_PEM, 300);
    assert!(decode_token(&cfg, &token).is_err());
}
