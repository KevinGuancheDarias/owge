//! The change watcher: polls `CHECKSUM TABLE` over every table the resolve
//! layer reads and rebuilds the site when the set of checksums changes and has
//! stayed stable for one extra poll (debounce, so an admin mid-edit-session
//! doesn't trigger a rebuild per save).
//!
//! A `NULL` checksum is **fatal**, never "no change": MySQL answers a
//! nonexistent table with a NULL-checksum row (plus an easily-missed error
//! 1146), so treating it as benign would silently stop rebuilds for that
//! table. See `docs/WIKI-GENERATOR-PLAN.md`.

use std::collections::BTreeMap;
use std::path::Path;
use std::time::Duration;

use anyhow::{Context, Result, bail};
use sqlx::{Connection, MySqlConnection, Row};
use tracing::{info, warn};

use crate::{build, render};

/// Every table the resolve layer reads, directly or through a bo.
const WATCHED_TABLES: &[&str] = &[
    "configuration",
    "units",
    "unit_types",
    "upgrades",
    "upgrade_types",
    "time_specials",
    "special_locations",
    "galaxies",
    "factions",
    "factions_unit_types",
    "faction_spawn_location",
    "improvements",
    "improvements_unit_types",
    "objects",
    "object_relations",
    "object_relation__object_relation",
    "requirements",
    "requirements_information",
    "requirement_group",
    "attack_rules",
    "attack_rule_entries",
    "rules",
    "critical_attack",
    "critical_attack_entries",
    "speed_impact_groups",
    "interceptable_speed_group",
    "images_store",
];

type Checksums = BTreeMap<String, u64>;

async fn fetch_checksums(conn: &mut MySqlConnection) -> Result<Checksums> {
    let sql = format!("CHECKSUM TABLE {}", WATCHED_TABLES.join(", "));
    let rows = sqlx::query(&sql).fetch_all(&mut *conn).await?;
    let mut out = Checksums::new();
    for row in rows {
        let table: String = row.try_get(0)?;
        let checksum: Option<u64> = match row.try_get::<Option<u64>, _>(1) {
            Ok(v) => v,
            Err(_) => row.try_get::<Option<i64>, _>(1)?.map(|v| v as u64),
        };
        let Some(checksum) = checksum else {
            bail!(
                "CHECKSUM TABLE returned NULL for `{table}` — the table does not exist \
                 (or cannot be checksummed). Refusing to continue: treating this as \
                 'no change' would silently stop rebuilds. Fix WATCHED_TABLES / the schema."
            );
        };
        out.insert(table, checksum);
    }
    Ok(out)
}

async fn connect(db_url: &str) -> Result<MySqlConnection> {
    MySqlConnection::connect(db_url)
        .await
        .context("connecting to the universe database")
}

async fn rebuild(db_url: &str, universe: &str, out: &Path) -> Result<()> {
    let mut conn = connect(db_url).await?;
    let sites = build::build_all_languages(&mut conn, universe).await?;
    let refs: Vec<_> = sites.iter().map(|(l, s)| (*l, s)).collect();
    render::write_site(&refs, out)?;
    Ok(())
}

/// Initial build, then poll forever. Connection/build errors are retried on
/// the next tick; a NULL checksum aborts the process (see module docs).
pub async fn run(db_url: &str, universe: &str, out: &Path, interval_secs: u64) -> Result<()> {
    info!("initial build");
    let mut last_built: Option<Checksums> = None;
    match connect(db_url).await {
        Ok(mut conn) => {
            let sums = fetch_checksums(&mut conn).await?; // NULL here is fatal
            let sites = build::build_all_languages(&mut conn, universe).await?;
            let refs: Vec<_> = sites.iter().map(|(l, s)| (*l, s)).collect();
            render::write_site(&refs, out)?;
            info!("site published");
            last_built = Some(sums);
        }
        Err(e) => warn!("initial build failed, will retry: {e:#}"),
    }

    let mut previous: Option<Checksums> = None;
    loop {
        tokio::time::sleep(Duration::from_secs(interval_secs)).await;
        let sums = match poll(db_url).await {
            Ok(s) => s,
            Err(e) => {
                if e.to_string().contains("CHECKSUM TABLE returned NULL") {
                    return Err(e);
                }
                warn!("checksum poll failed, will retry: {e:#}");
                continue;
            }
        };
        if last_built.as_ref() == Some(&sums) {
            previous = Some(sums);
            continue;
        }
        if previous.as_ref() == Some(&sums) {
            // Changed vs the last build and stable across two polls → rebuild.
            info!("configuration change settled, rebuilding");
            match rebuild(db_url, universe, out).await {
                Ok(()) => {
                    info!("site republished");
                    last_built = Some(sums.clone());
                }
                Err(e) => warn!("rebuild failed, will retry: {e:#}"),
            }
        } else {
            info!("configuration change detected, waiting for it to settle");
        }
        previous = Some(sums);
    }
}

async fn poll(db_url: &str) -> Result<Checksums> {
    let mut conn = connect(db_url).await?;
    fetch_checksums(&mut conn).await
}
