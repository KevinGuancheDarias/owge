/// The backend under test for this driver pass. The driver never links
/// backend code — this is just addresses (BDD-PARITY-PLAN.md §2.1/§2.2).
#[derive(Debug, Clone, Copy, PartialEq, Eq)]
pub enum BackendKind {
    Java,
    Rust,
}

#[derive(Debug, Clone)]
pub struct Backend {
    pub kind: BackendKind,
    /// REST base, e.g. http://127.0.0.1:18080/game_api (Java) /
    /// http://127.0.0.1:8080 (Rust)
    pub base_url: String,
    /// socket.io ORIGIN — both backends run netty-socketio/socketioxide on a
    /// SEPARATE listener (OWGE_WS_PORT, default 7474 inside each). The
    /// /websocket/ prefix seen in production is an nginx rewrite; direct
    /// connections use the default /socket.io context on this origin.
    pub ws_origin: String,
    pub ws_path: String,
}

impl Backend {
    /// OWGE_BDD_BACKEND=java|rust selects the target; unset/"none" means the
    /// scenario must not perform REST/ws work (pure Given/Then smoke).
    pub fn from_env() -> anyhow::Result<Option<Backend>> {
        let kind = std::env::var("OWGE_BDD_BACKEND").unwrap_or_else(|_| "none".into());
        match kind.as_str() {
            "none" | "" => Ok(None),
            "java" => Ok(Some(Backend {
                kind: BackendKind::Java,
                base_url: std::env::var("OWGE_BDD_JAVA_BASE")
                    .unwrap_or_else(|_| "http://127.0.0.1:18080/game_api".into()),
                ws_origin: std::env::var("OWGE_BDD_JAVA_WS")
                    .unwrap_or_else(|_| "http://127.0.0.1:17474".into()),
                ws_path: "/socket.io".into(),
            })),
            "rust" => Ok(Some(Backend {
                kind: BackendKind::Rust,
                base_url: std::env::var("OWGE_BDD_RUST_BASE")
                    .unwrap_or_else(|_| "http://127.0.0.1:8080".into()),
                ws_origin: std::env::var("OWGE_BDD_RUST_WS")
                    .unwrap_or_else(|_| "http://127.0.0.1:7474".into()),
                ws_path: "/socket.io".into(),
            })),
            other => anyhow::bail!("OWGE_BDD_BACKEND must be java|rust|none, got {other:?}"),
        }
    }
}
