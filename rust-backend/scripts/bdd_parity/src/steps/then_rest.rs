//! Rejection Thens for `attempts`-style Whens (plan §6.6 negative-path design).
//! HTTP status equality is the contract; error-body wording is only ever
//! matched loosely (Java GameBackendErrorPojo vs Rust OwgeError differ by
//! design).

use cucumber::then;

use crate::world::BddWorld;

fn last_response(world: &BddWorld) -> &(u16, String) {
    world
        .last_response
        .as_ref()
        .expect("no previous `attempts` request — rejection Thens need an attempts-style When")
}

#[then(expr = "the request is rejected with HTTP status {int}")]
async fn rejected_with_status(world: &mut BddWorld, expected: i64) {
    let (status, body) = last_response(world);
    assert_eq!(
        *status as i64, expected,
        "expected HTTP {expected}, got {status}; body: {body}"
    );
}

#[then(expr = "the request is rejected with error containing {string}")]
async fn rejected_with_error(world: &mut BddWorld, marker: String) {
    let (status, body) = last_response(world);
    assert!(
        !(200..300).contains(status),
        "expected a rejected request, got HTTP {status}; body: {body}"
    );
    assert!(
        body.contains(&marker),
        "expected error body to contain {marker:?}; HTTP {status}, body: {body}"
    );
}

#[then(expr = "the request succeeded")]
async fn request_succeeded(world: &mut BddWorld) {
    let (status, body) = last_response(world);
    assert!(
        (200..300).contains(status),
        "expected success, got HTTP {status}; body: {body}"
    );
}
