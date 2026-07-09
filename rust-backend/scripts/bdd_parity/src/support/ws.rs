//! ws_capture.js subprocess management + captured-frame queries (§5.3/§6.5).
//!
//! One capture process per (scenario, user), started before the first When,
//! writing one JSON line per received event to
//! $OWGE_BDD_ARTIFACTS/ws_user<id>.jsonl. Node 14 only — hence the nvm shim.

use std::path::PathBuf;
use std::process::{Child, Command, Stdio};
use std::time::Duration;

use crate::support::backends::Backend;

fn scripts_dir() -> PathBuf {
    // target/debug/bdd_parity -> crate root is CARGO_MANIFEST_DIR at build time
    PathBuf::from(env!("CARGO_MANIFEST_DIR")).parent().unwrap().to_path_buf()
}

pub fn artifacts_dir() -> Option<PathBuf> {
    std::env::var("OWGE_BDD_ARTIFACTS").ok().map(PathBuf::from)
}

pub fn capture_file(user_id: i64) -> Option<PathBuf> {
    artifacts_dir().map(|d| d.join(format!("ws_user{user_id}.jsonl")))
}

/// Spawn ws_capture.js for one user; caller stores the Child in the World.
pub fn start_capture(backend: &Backend, user_id: i64, jwt: &str) -> Child {
    let out = capture_file(user_id).expect("OWGE_BDD_ARTIFACTS must be set to capture ws");
    std::fs::create_dir_all(out.parent().unwrap()).expect("create artifacts dir");
    let script = scripts_dir().join("ws_verify/ws_capture.js");
    let cmd = format!(
        "source ~/.nvm/nvm.sh >/dev/null 2>&1; nvm use 14 >/dev/null 2>&1; \
         exec node {} {} {} {} 3600",
        script.display(),
        backend.ws_origin,
        backend.ws_path,
        jwt,
    );
    let stdout = std::fs::File::create(&out).expect("create ws capture file");
    let stderr = std::fs::File::create(out.with_extension("stderr")).expect("stderr file");
    Command::new("bash")
        .arg("-c")
        .arg(cmd)
        .current_dir("/tmp/wsclient")
        // require() resolves from the SCRIPT's dir (ws_verify/), not cwd —
        // NODE_PATH points it at the socket.io-client@2.4.0 install
        .env("NODE_PATH", "/tmp/wsclient/node_modules")
        .stdout(Stdio::from(stdout))
        .stderr(Stdio::from(stderr))
        .spawn()
        .expect("spawn ws_capture.js")
}

/// Wait until the capture emitted its `authentication` frame — before that,
/// deliveries can be missed (§5.3).
pub async fn wait_authenticated(user_id: i64, timeout: Duration) {
    let path = capture_file(user_id).expect("artifacts dir");
    let deadline = std::time::Instant::now() + timeout;
    loop {
        if let Ok(content) = std::fs::read_to_string(&path) {
            if content.contains("\"authentication\"") {
                return;
            }
        }
        assert!(
            std::time::Instant::now() < deadline,
            "ws capture for user {user_id} did not authenticate within {timeout:?} \
             (stderr: {:?})",
            std::fs::read_to_string(path.with_extension("stderr")).unwrap_or_default()
        );
        tokio::time::sleep(Duration::from_millis(250)).await;
    }
}

/// All `deliver` frames for an event name, in arrival order (flush-safe: reads
/// the file, never talks to the process).
pub fn deliver_frames(user_id: i64, event_name: &str) -> Vec<serde_json::Value> {
    let Some(path) = capture_file(user_id) else {
        return Vec::new();
    };
    let Ok(content) = std::fs::read_to_string(&path) else {
        return Vec::new();
    };
    content
        .lines()
        .filter_map(|l| serde_json::from_str::<serde_json::Value>(l).ok())
        .filter(|v| v["kind"] == "deliver" && v["payload"]["eventName"] == event_name)
        .collect()
}

/// Poll (§5.3: emissions are post-commit + async) until a deliver frame for
/// `event_name` satisfies `pred` on its payload.value, or time out. Returns
/// the LAST matching frame's value (full-list push semantics: latest = final
/// state).
pub async fn wait_frame(
    user_id: i64,
    event_name: &str,
    timeout: Duration,
    pred: impl Fn(&serde_json::Value) -> bool,
) -> Option<serde_json::Value> {
    let deadline = std::time::Instant::now() + timeout;
    loop {
        let frames = deliver_frames(user_id, event_name);
        if let Some(hit) = frames
            .iter()
            .rev()
            .find(|f| pred(&f["payload"]["value"]))
        {
            return Some(hit["payload"]["value"].clone());
        }
        if std::time::Instant::now() >= deadline {
            return None;
        }
        tokio::time::sleep(Duration::from_millis(500)).await;
    }
}
