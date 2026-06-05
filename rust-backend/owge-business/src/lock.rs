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
//! 2. **Globally-sorted acquisition.** Keys are acquired in ascending order in a
//!    single statement so two sessions sharing keys can never deadlock by
//!    requesting them in opposite orders (the Java
//!    `MysqlLockUtilService.doInsideLock` invariant). Re-acquiring a key already
//!    held by the *same* session is fine: MySQL named locks are re-entrant per
//!    session (each `GET_LOCK` returns 1 immediately), so the up-front superset
//!    acquisition in the mission runner makes nested requirement-engine locks
//!    no-ops.
//!
//! Timeout (10s) and attempt budget (5) match `MysqlLockUtilService`.

use sqlx::{MySqlConnection, Row};

use crate::error::{OwgeError, OwgeResult};

pub const PLANET_LOCK_KEY_PREFIX: &str = "planet_lock_";
pub const USER_LOCK_KEY_PREFIX: &str = "user_lock_";

pub const TIMEOUT_SECONDS: i32 = 10;
pub const MAX_LOCK_ATTEMPTS: u32 = 5;

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
        match try_acquire(conn, &sorted).await {
            Ok(true) => return Ok(()),
            Ok(false) => {
                // Partial acquisition: release whatever we got and retry.
                let _ = release(conn, keys).await;
                if attempt == MAX_LOCK_ATTEMPTS {
                    return Err(surrender(&sorted, "could not obtain all GET_LOCKs"));
                }
            }
            Err(e) => {
                // A deadlock surfaces as a DB error mentioning "Deadlock"; retry.
                let is_deadlock = e.to_string().contains("Deadlock");
                let _ = release(conn, keys).await;
                if !is_deadlock || attempt == MAX_LOCK_ATTEMPTS {
                    return Err(if is_deadlock {
                        surrender(&sorted, "deadlock")
                    } else {
                        e
                    });
                }
            }
        }
    }
    Err(surrender(&sorted, "exhausted attempts"))
}

/// Build `SELECT GET_LOCK(?,10), GET_LOCK(?,10), ...` and confirm every lock
/// resolved to 1. Returns `Ok(true)` only when the full set was acquired.
async fn try_acquire(conn: &mut MySqlConnection, sorted: &[&String]) -> OwgeResult<bool> {
    let parts = std::iter::repeat(format!("GET_LOCK(?,{TIMEOUT_SECONDS})"))
        .take(sorted.len())
        .collect::<Vec<_>>()
        .join(", ");
    let sql = format!("SELECT {parts}");
    let mut q = sqlx::query(&sql);
    for key in sorted {
        q = q.bind(*key);
    }
    let row = q.fetch_one(&mut *conn).await?;
    let mut total = 0i64;
    for i in 0..sorted.len() {
        // GET_LOCK returns 1 (acquired), 0 (timeout), or NULL (error).
        let v: Option<i64> = row.try_get(i)?;
        total += v.unwrap_or(0);
    }
    Ok(total == sorted.len() as i64)
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
