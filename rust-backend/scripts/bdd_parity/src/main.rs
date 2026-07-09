//! BDD parity harness driver — see rust-backend/docs/BDD-PARITY-PLAN.md.
//!
//! One invocation = ONE backend pass over the selected features
//! (OWGE_BDD_BACKEND=java|rust|none). The runner script loops backends,
//! resets the DB between passes and diffs the layer-2 artifacts (§5.1).

mod steps;
mod support;
mod world;

use cucumber::World as _;
use world::BddWorld;

#[tokio::main]
async fn main() {
    let features = std::env::var("OWGE_BDD_FEATURES").unwrap_or_else(|_| "features".into());
    BddWorld::cucumber()
        .fail_on_skipped()
        .max_concurrent_scenarios(1) // one shared DB: scenarios must never interleave (§5)
        .after(|_feature, _rule, _scenario, _ev, world| {
            Box::pin(async move {
                if let Some(w) = world {
                    w.after_scenario().await;
                }
            })
        })
        .run_and_exit(features)
        .await;
}
