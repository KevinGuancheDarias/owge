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

/// Render arbitrary rows as debug strings — used by failure messages, which
/// are the product here (§5.3): a red Then must show the ACTUAL rows.
pub async fn dump_rows(db: &MySqlPool, sql: &str) -> Vec<String> {
    use sqlx::{Column, Row};
    sqlx::query(sql)
        .fetch_all(db)
        .await
        .map(|rows| {
            rows.iter()
                .map(|r| {
                    r.columns()
                        .iter()
                        .map(|c| {
                            let v: Result<Option<String>, _> = r.try_get_unchecked(c.ordinal());
                            format!("{}={:?}", c.name(), v.unwrap_or(None))
                        })
                        .collect::<Vec<_>>()
                        .join(" ")
                })
                .collect()
        })
        .unwrap_or_default()
}
