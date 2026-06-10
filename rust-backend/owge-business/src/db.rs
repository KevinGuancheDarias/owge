//! Database access. The Java engine uses Spring Data JPA repositories over a
//! MySQL/MariaDB schema (`business/database/02_schema.sql`); the Rust port uses
//! `sqlx` against the very same schema (no ORM, hand-written queries in the
//! `bo` layer).
//!
//! **Single-connection invariant.** MySQL advisory locks (`GET_LOCK`) and
//! transactions are *session* scoped, so every `bo` function takes
//! `conn: &mut MySqlConnection` and the pool exists only at entry points
//! (HTTP handlers, the socket.io auth handler, the scheduler pollers), each
//! acquiring exactly one connection per unit of work. Nothing below the entry
//! points may touch the pool — that's what makes it impossible for a query to
//! sneak onto a second session and bypass the caller's locks or transaction.
//!
//! **SeaORM is builder + decoder only.** Queries that need SeaORM's
//! join/decode machinery (nested `DerivePartialModel`, e.g. the
//! `obtained_unit_bo` doubly-joined select) build a [`sea_orm::Statement`] and
//! run it through [`sea_all`]/[`sea_one`] **on the caller's pinned
//! connection** — sea-orm 1.1 has no public single-connection executor (its
//! `DatabaseConnection` only wraps a pool), so its executor is deliberately
//! not used. This is also the incremental migration path towards SeaORM:
//! port a query to the SeaORM builder + a partial model, keep executing it
//! through these helpers on the same session.

use sea_orm::sea_query::Values;
use sea_orm::{DbBackend, FromQueryResult, QueryResult, SelectModel, Selector};
use sea_query_binder::SqlxValues;
use sqlx::MySqlConnection;
use sqlx::mysql::{MySqlPool, MySqlPoolOptions};

use crate::error::{OwgeError, OwgeResult};

/// The application connection pool. Only entry points hold it; everything
/// below works on a single acquired `&mut MySqlConnection`.
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

/// Run a SeaORM-built select on the caller's pinned connection and decode
/// every row through the partial model `M`. The bind glue
/// (`sqlx::query_with(&stmt.sql, SqlxValues(values))`) is byte-for-byte what
/// sea-orm's own MySQL driver does internally — only the executor differs.
pub async fn sea_all<M>(
    conn: &mut MySqlConnection,
    select: Selector<SelectModel<M>>,
) -> OwgeResult<Vec<M>>
where
    M: FromQueryResult,
{
    let stmt = select.into_statement(DbBackend::MySql);
    let values = SqlxValues(stmt.values.unwrap_or(Values(Vec::new())));
    let rows = sqlx::query_with(&stmt.sql, values)
        .fetch_all(&mut *conn)
        .await?;
    rows.into_iter()
        .map(|row| {
            M::from_query_result(&QueryResult::from(row), "")
                .map_err(|e| OwgeError::Common(e.to_string()))
        })
        .collect()
}

/// [`sea_all`] for at-most-one row.
pub async fn sea_one<M>(
    conn: &mut MySqlConnection,
    select: Selector<SelectModel<M>>,
) -> OwgeResult<Option<M>>
where
    M: FromQueryResult,
{
    let stmt = select.into_statement(DbBackend::MySql);
    let values = SqlxValues(stmt.values.unwrap_or(Values(Vec::new())));
    let row = sqlx::query_with(&stmt.sql, values)
        .fetch_optional(&mut *conn)
        .await?;
    row.map(|row| {
        M::from_query_result(&QueryResult::from(row), "")
            .map_err(|e| OwgeError::Common(e.to_string()))
    })
    .transpose()
}
