//! `owge-wiki-gen` — static game-wiki generator for one universe.
//!
//! ```text
//! owge-wiki-gen once  --db mysql://user:pass@host:3306/dbname --out /path/to/wiki [--universe "Name"]
//! owge-wiki-gen watch --db ... --out ... [--interval 30] [--universe "Name"]
//! ```
//!
//! `--db` falls back to the `DATABASE_URL` environment variable. Image URLs
//! come from the engine's own `ImageStore` mapping and honor `OWGE_DYNAMIC_URL`
//! (default `/dynamic`), so pages served next to the game's nginx resolve
//! images with no extra configuration.

mod build;
mod i18n;
mod render;
mod view;
mod watch;

use std::path::PathBuf;

use anyhow::{Context, Result, bail};
use sqlx::Connection;
use tracing::info;

struct Args {
    mode: Mode,
    db: String,
    out: PathBuf,
    universe: String,
    interval: u64,
}

enum Mode {
    Once,
    Watch,
}

fn usage() -> ! {
    eprintln!(
        "usage: owge-wiki-gen <once|watch> --db <mysql-url> --out <dir> \
         [--universe <name>] [--interval <secs>]\n\
         --db defaults to $DATABASE_URL"
    );
    std::process::exit(2);
}

fn parse_args() -> Result<Args> {
    let mut argv = std::env::args().skip(1);
    let mode = match argv.next().as_deref() {
        Some("once") => Mode::Once,
        Some("watch") => Mode::Watch,
        _ => usage(),
    };
    let mut db = std::env::var("DATABASE_URL").ok();
    let mut out: Option<PathBuf> = None;
    let mut universe = "OWGE Universe".to_string();
    let mut interval = 30u64;
    while let Some(flag) = argv.next() {
        let mut value = || {
            argv.next()
                .with_context(|| format!("missing value for {flag}"))
        };
        match flag.as_str() {
            "--db" => db = Some(value()?),
            "--out" => out = Some(PathBuf::from(value()?)),
            "--universe" => universe = value()?,
            "--interval" => {
                interval = value()?
                    .parse()
                    .context("--interval must be a number of seconds")?
            }
            _ => usage(),
        }
    }
    let Some(db) = db else {
        bail!("no database URL: pass --db or set DATABASE_URL");
    };
    let Some(out) = out else {
        bail!("no output directory: pass --out");
    };
    Ok(Args {
        mode,
        db,
        out,
        universe,
        interval: interval.max(5),
    })
}

/// `write_site` wants borrowed sites; the builder returns owned ones.
fn as_refs<'a>(
    sites: &'a [(&'static i18n::LangDef, view::Site)],
) -> Vec<(&'static i18n::LangDef, &'a view::Site)> {
    sites.iter().map(|(l, s)| (*l, s)).collect()
}

#[tokio::main]
async fn main() -> Result<()> {
    tracing_subscriber::fmt()
        .with_env_filter(
            tracing_subscriber::EnvFilter::try_from_default_env()
                .unwrap_or_else(|_| "info".into()),
        )
        .init();
    let args = parse_args()?;
    match args.mode {
        Mode::Once => {
            let mut conn = sqlx::MySqlConnection::connect(&args.db)
                .await
                .context("connecting to the universe database")?;
            let sites = build::build_all_languages(&mut conn, &args.universe).await?;
            render::write_site(&as_refs(&sites), &args.out)?;
            let site = &sites[0].1;
            info!(
                units = site.units.len(),
                upgrades = site.upgrades.len(),
                time_specials = site.time_specials.len(),
                special_locations = site.special_locations.len(),
                languages = sites.len(),
                out = %args.out.display(),
                "wiki generated"
            );
        }
        Mode::Watch => {
            watch::run(&args.db, &args.universe, &args.out, args.interval).await?;
        }
    }
    Ok(())
}
