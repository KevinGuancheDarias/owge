//! Then steps over captured websocket frames (BDD-PARITY-PLAN.md §6.5).
//!
//! These events are FULL-LIST pushes (unit_unlocked_change carries ALL
//! currently-unlocked units), so "no item has id X" is meaningful — but only
//! after a frame for the event actually arrived. All assertions poll the
//! capture file (post-commit emission is async, §5.3).

use std::time::Duration;

use cucumber::then;

use crate::support::ws;
use crate::world::BddWorld;

const FRAME_TIMEOUT: Duration = Duration::from_secs(10);

fn value_has_item_with_id(value: &serde_json::Value, id: i64) -> bool {
    value
        .as_array()
        .map(|items| items.iter().any(|it| it["id"] == serde_json::json!(id)))
        .unwrap_or(false)
}

#[then(expr = "user {int} received websocket event {string}")]
async fn received_event(_world: &mut BddWorld, user: i64, event: String) {
    let frame = ws::wait_frame(user, &event, FRAME_TIMEOUT, |_| true).await;
    assert!(
        frame.is_some(),
        "user {user} never received websocket event {event:?} within {FRAME_TIMEOUT:?}"
    );
}

#[then(expr = "user {int} received websocket event {string} where some item has id {int}")]
async fn received_event_with_item(_world: &mut BddWorld, user: i64, event: String, id: i64) {
    let hit = ws::wait_frame(user, &event, FRAME_TIMEOUT, |v| {
        value_has_item_with_id(v, id)
    })
    .await;
    if hit.is_none() {
        let frames = ws::deliver_frames(user, &event);
        panic!(
            "user {user}: no {event:?} frame with an item of id {id} within {FRAME_TIMEOUT:?}; \
             received {} frame(s) of that event, last payload: {}",
            frames.len(),
            frames
                .last()
                .map(|f| f["payload"]["value"].to_string())
                .unwrap_or_else(|| "<none>".into())
        );
    }
}

#[then(expr = "user {int} received websocket event {string} where no item has id {int}")]
async fn received_event_without_item(_world: &mut BddWorld, user: i64, event: String, id: i64) {
    // the event must have ARRIVED (full-list semantics) — then its latest
    // frame must lack the id
    let last = ws::wait_frame(user, &event, FRAME_TIMEOUT, |_| true).await;
    let Some(value) = last else {
        panic!(
            "user {user} never received websocket event {event:?} within {FRAME_TIMEOUT:?} \
             (required even for the negative item assertion — full-list push semantics)"
        );
    };
    // poll a little longer in case a later frame (the final state) still lands
    let settled = ws::wait_frame(user, &event, Duration::from_secs(3), |v| {
        !value_has_item_with_id(v, id)
    })
    .await;
    assert!(
        settled.is_some(),
        "user {user}: latest {event:?} frame still contains an item with id {id}: {value}"
    );
}

#[then(expr = "user {int} received no websocket event {string}")]
async fn received_no_event(_world: &mut BddWorld, user: i64, event: String) {
    // negative: only meaningful after the settle window; the after-scenario
    // hook waits 2 s before killing captures, we wait here explicitly too
    tokio::time::sleep(Duration::from_secs(3)).await;
    let frames = ws::deliver_frames(user, &event);
    assert!(
        frames.is_empty(),
        "user {user} received {} unexpected {event:?} frame(s); last payload: {}",
        frames.len(),
        frames
            .last()
            .map(|f| f["payload"]["value"].to_string())
            .unwrap_or_default()
    );
}
