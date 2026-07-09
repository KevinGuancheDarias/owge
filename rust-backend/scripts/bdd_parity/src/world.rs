use std::collections::{HashMap, HashSet};
use std::process::Child;

use crate::support::backends::Backend;

/// Typed per-scenario state (BDD-PARITY-PLAN.md §5.3).
#[derive(cucumber::World, Debug)]
#[world(init = Self::new)]
pub struct BddWorld {
    pub db: sqlx::MySqlPool,
    /// None when OWGE_BDD_BACKEND is unset/"none" (pure Given/Then scenarios).
    pub backend: Option<Backend>,
    /// User ids whose websocket traffic is captured from the first When on.
    pub captured_users: HashSet<i64>,
    /// user_id -> running ws_capture.js process.
    pub ws_procs: HashMap<i64, Child>,
    /// Mission ids created by When steps of this scenario.
    pub created_missions: Vec<i64>,
    /// Planets/users registered by Given steps — widen the layer-2 dump filter.
    pub registered_planets: HashSet<i64>,
    /// (http_status, body) of the last `attempts`-style REST call — read by the
    /// rejection Thens (§6.6 negative-path design).
    pub last_response: Option<(u16, String)>,
}

impl BddWorld {
    async fn new() -> anyhow::Result<Self> {
        Ok(Self {
            db: crate::support::db::connect().await?,
            backend: Backend::from_env()?,
            captured_users: HashSet::new(),
            ws_procs: HashMap::new(),
            created_missions: Vec::new(),
            registered_planets: HashSet::new(),
            last_response: None,
        })
    }

    /// §5.3 after-scenario: settle-wait, then stop captures. Layer-2 dumping
    /// is the runner script's job (it also owns the artifacts dir); we hand it
    /// the scenario's registered scope via scope.env.
    pub async fn after_scenario(&mut self) {
        if !self.ws_procs.is_empty() {
            tokio::time::sleep(std::time::Duration::from_secs(2)).await;
        }
        for (_user, mut child) in self.ws_procs.drain() {
            let _ = child.kill();
            let _ = child.wait();
        }
        self.write_scope_env();
    }

    /// Widen the layer-2 dump filter (dump_mission_state.sh USERS/PLANETS) to
    /// everything the scenario's Givens registered, merged with the defaults.
    fn write_scope_env(&self) {
        let Ok(dir) = std::env::var("OWGE_BDD_ARTIFACTS") else {
            return;
        };
        let mut users: Vec<i64> = self.captured_users.iter().copied().chain([1, 2]).collect();
        users.sort_unstable();
        users.dedup();
        let mut planets: Vec<i64> = self
            .registered_planets
            .iter()
            .copied()
            .chain([1002, 1003, 1004])
            .collect();
        planets.sort_unstable();
        planets.dedup();
        let join = |v: &[i64]| {
            v.iter()
                .map(|n| n.to_string())
                .collect::<Vec<_>>()
                .join(",")
        };
        let content = format!("USERS={}\nPLANETS={}\n", join(&users), join(&planets));
        let _ = std::fs::create_dir_all(&dir);
        let _ = std::fs::write(format!("{dir}/scope.env"), content);
    }
}
