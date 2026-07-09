use sqlx::mysql::MySqlPoolOptions;
use sqlx::MySqlPool;

/// Shared dev DB (the mission_verify topology). Overridable for CI.
pub fn url() -> String {
    std::env::var("OWGE_BDD_DB_URL")
        .unwrap_or_else(|_| "mysql://root:1234@127.0.0.1:3306/owge".into())
}

pub async fn connect() -> anyhow::Result<MySqlPool> {
    Ok(MySqlPoolOptions::new()
        .max_connections(2)
        .connect(&url())
        .await?)
}
