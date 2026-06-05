//! Database access. The Java engine uses Spring Data JPA repositories over a
//! MySQL/MariaDB schema (`business/database/02_schema.sql`); the Rust port uses
//! `sqlx` against the very same schema (no ORM, hand-written queries in the
//! `bo` layer).

use sqlx::mysql::{MySqlPool, MySqlPoolOptions};

use crate::error::OwgeResult;

/// A pooled MySQL handle, shared across the application (the `Bo` layer borrows
/// it per query, mirroring Spring's connection pool).
pub type Db = MySqlPool;

/// Build the connection pool from a `mysql://user:pass@host:port/db` URL.
/// Equivalent to Spring's `spring.datasource.url` wiring.
pub async fn create_pool(database_url: &str, max_connections: u32) -> OwgeResult<Db> {
    let pool = MySqlPoolOptions::new()
        .max_connections(max_connections)
        .connect(database_url)
        .await?;
    Ok(pool)
}
