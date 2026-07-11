//! MySQL application-level named locks — the Rust port of
//! `MysqlLockUtilService` + `PlanetLockUtilService`
//! (`com.kevinguanchedarias.owgejava.business.mysql` / `...business.planet`).
//!
//! Mission execution serialises contended work with MySQL `GET_LOCK` /
//! `RELEASE_LOCK` keyed by planet (`planet_lock_<id>`). Two hard rules carry
//! over from the Java engine and from the roadmap (§4.9):
//!
//! 1. **Connection pinning.** `GET_LOCK` is *session scoped* — a lock taken on
//!    one connection is invisible to another. The whole locked critical section
//!    must therefore run on **one** pinned `MySqlConnection` (acquire it once,
//!    take the locks on it, run every query of the section through `&mut conn`,
//!    then release). Callers thread that same `&mut conn` into the
//!    requirement-engine and `Bo` methods (which already take
//!    `&mut MySqlConnection`).
//! 2. **Globally-sorted acquisition, ONE key per statement.** Keys are acquired
//!    in ascending order, one `GET_LOCK` statement at a time, stopping at the
//!    first unavailable key and releasing the acquired prefix before backing
//!    off (the hotfixed Java `MysqlLockUtilService.acquireInOrder`, 17f266a2).
//!    The old multi-expression form, `SELECT GET_LOCK(a,t), GET_LOCK(b,t), …`,
//!    kept evaluating after a failed GET_LOCK, so a session that timed out on
//!    an early key still acquired — and held — the later ones; that silently
//!    broke the global ascending order and allowed real user-level lock
//!    deadlock cycles and lock convoys under same-planet mission traffic
//!    (seen on dc12, mission 365832). Stopping at the first failure means a
//!    session never holds a key greater than one it is waiting for, which
//!    makes such cycles impossible. Re-acquiring a key already held by the
//!    *same* session is fine: MySQL named locks are re-entrant per session
//!    (each `GET_LOCK` returns 1 immediately), so the up-front superset
//!    acquisition in the mission runner makes nested requirement-engine locks
//!    no-ops.
//!
//! Timeout (3s), attempt budget (5) and the 200ms×attempt retry backoff match
//! the hotfixed `MysqlLockUtilService` (mission executions hold the locks for
//! well under a second, so 3s is already a generous wait for a single holder).

use sqlx::MySqlConnection;

use crate::error::{OwgeError, OwgeResult};

pub const PLANET_LOCK_KEY_PREFIX: &str = "planet_lock_";
pub const USER_LOCK_KEY_PREFIX: &str = "user_lock_";

pub const TIMEOUT_SECONDS: i32 = 3;
pub const MAX_LOCK_ATTEMPTS: u32 = 5;
const RETRY_BACKOFF_MS: u64 = 200;

/// `planet_lock_<id>` — the key guarding a single planet's mutations.
pub fn planet_lock_key(planet_id: u64) -> String {
    format!("{PLANET_LOCK_KEY_PREFIX}{planet_id}")
}

/// `user_lock_<id>` — the key serialising per-user work (e.g. unique-unit
/// build registration, matching the Java per-user lock).
pub fn user_lock_key(user_id: i32) -> String {
    format!("{USER_LOCK_KEY_PREFIX}{user_id}")
}

/// Acquire every key in `keys` (deduplicated, ascending) on `conn` in one
/// statement. Retries the whole set on a partial acquisition or a detected
/// deadlock, up to [`MAX_LOCK_ATTEMPTS`]; then fails loudly with
/// [`OwgeError::Conflict`] (the `CannotAcquireLockException` analogue) rather
/// than running the protected section unprotected.
///
/// On success the caller **must** later call [`release`] with the same keys
/// (typically right after `COMMIT`).
pub async fn acquire(conn: &mut MySqlConnection, keys: &[String]) -> OwgeResult<()> {
    let mut sorted: Vec<&String> = keys.iter().collect();
    sorted.sort();
    sorted.dedup();
    if sorted.is_empty() {
        return Ok(());
    }

    for attempt in 1..=MAX_LOCK_ATTEMPTS {
        match acquire_in_order(conn, &sorted).await {
            Ok(None) => return Ok(()),
            Ok(Some(failed_key)) => {
                // acquire_in_order already released the acquired prefix.
                if attempt == MAX_LOCK_ATTEMPTS {
                    return Err(surrender(
                        &sorted,
                        &format!("failed acquiring key {failed_key}"),
                    ));
                }
                tracing::warn!(
                    "Not able to obtain lock {failed_key} (wanted keys = {sorted:?}), \
                     attempt {attempt}/{MAX_LOCK_ATTEMPTS}"
                );
                tokio::time::sleep(std::time::Duration::from_millis(
                    RETRY_BACKOFF_MS * attempt as u64,
                ))
                .await;
            }
            Err(e) => return Err(e),
        }
    }
    Err(surrender(&sorted, "exhausted attempts"))
}

/// Acquire the (already sorted) keys strictly ONE `GET_LOCK` statement at a
/// time, stopping at the first key that can't be acquired and releasing the
/// acquired prefix before returning (the 17f266a2 shape — see the module doc
/// for why the multi-expression statement caused real deadlock cycles).
///
/// Returns `None` when every key was acquired, otherwise the key that failed.
/// MySQL reporting a deadlock on a key is treated as a normal failed attempt
/// (should no longer happen now that the acquisition order can't be violated).
async fn acquire_in_order(
    conn: &mut MySqlConnection,
    sorted: &[&String],
) -> OwgeResult<Option<String>> {
    let mut acquired: Vec<String> = Vec::with_capacity(sorted.len());
    for key in sorted {
        // GET_LOCK returns 1 (acquired), 0 (timeout), or NULL (error).
        let result: Result<Option<i64>, sqlx::Error> =
            sqlx::query_scalar(&format!("SELECT GET_LOCK(?,{TIMEOUT_SECONDS})"))
                .bind(*key)
                .fetch_one(&mut *conn)
                .await;
        let lock_result = match result {
            Ok(v) => v.unwrap_or(0),
            Err(e) if e.to_string().contains("Deadlock") => {
                tracing::debug!("Handling deadlock for key {key}");
                0
            }
            Err(e) => {
                if !acquired.is_empty() {
                    let _ = release(conn, &acquired).await;
                }
                return Err(e.into());
            }
        };
        if lock_result == 1 {
            acquired.push((*key).clone());
        } else {
            if !acquired.is_empty() {
                release(conn, &acquired).await?;
            }
            return Ok(Some((*key).clone()));
        }
    }
    Ok(None)
}

/// Release every key (idempotent — `RELEASE_LOCK` on an unheld key returns 0).
pub async fn release(conn: &mut MySqlConnection, keys: &[String]) -> OwgeResult<()> {
    let mut sorted: Vec<&String> = keys.iter().collect();
    sorted.sort();
    sorted.dedup();
    if sorted.is_empty() {
        return Ok(());
    }
    let parts = std::iter::repeat("RELEASE_LOCK(?)".to_string())
        .take(sorted.len())
        .collect::<Vec<_>>()
        .join(", ");
    let sql = format!("SELECT {parts}");
    let mut q = sqlx::query(&sql);
    for key in &sorted {
        q = q.bind(*key);
    }
    q.fetch_one(&mut *conn).await?;
    Ok(())
}

fn surrender(keys: &[&String], reason: &str) -> OwgeError {
    OwgeError::Conflict(format!(
        "Could not acquire required MySQL user-level locks after {MAX_LOCK_ATTEMPTS} attempts \
         ({reason}), keys = {keys:?}"
    ))
}
