//! Then steps asserting table state (BDD-PARITY-PLAN.md §6.4). On failure the
//! message contains the ACTUAL rows — failure output is the product here.

use cucumber::then;
use sqlx::Row;

use crate::world::BddWorld;

/// Whitelisted tables for the generic escape hatch (§6.4 + §6.6).
const TABLE_WHITELIST: &[&str] = &[
    "unlocked_relation",
    "obtained_units",
    "obtained_upgrades",
    "missions",
    "planets",
    "active_time_specials",
    "planet_list",
    "visited_tutorial_entries",
    "user_read_system_messages",
    "mission_reports",
    "system_messages",
    "user_storage",
    "obtained_unit_temporal_information",
    "explored_planets",
    "alliances",
    "alliance_join_requests",
];

/// Parse the `<col>=<val> and <col> is null and …` predicate chain into a safe
/// WHERE clause. Columns/values are strictly validated then interpolated —
/// no user input reaches this, only feature-file text.
fn build_where(table: &str, conditions: &str) -> (String, String) {
    assert!(
        TABLE_WHITELIST.contains(&table),
        "table {table:?} is not in the escape-hatch whitelist {TABLE_WHITELIST:?}"
    );
    let col_ok = |c: &str| {
        !c.is_empty() && c.chars().all(|ch| ch.is_ascii_lowercase() || ch == '_')
    };
    // missions.type_code is a pseudo-column resolved through mission_types
    let (from, qualify): (String, fn(&str) -> String) = if table == "missions" {
        (
            "missions m JOIN mission_types mt ON mt.id = m.type".into(),
            |c| {
                if c == "type_code" {
                    "mt.code".into()
                } else {
                    format!("m.{c}")
                }
            },
        )
    } else {
        (table.to_string(), |c| c.to_string())
    };
    let mut clauses = Vec::new();
    for part in conditions.split(" and ") {
        let part = part.trim();
        if let Some(col) = part.strip_suffix(" is not null") {
            assert!(col_ok(col.trim()), "bad column in {part:?}");
            clauses.push(format!("{} IS NOT NULL", qualify(col.trim())));
        } else if let Some(col) = part.strip_suffix(" is null") {
            assert!(col_ok(col.trim()), "bad column in {part:?}");
            clauses.push(format!("{} IS NULL", qualify(col.trim())));
        } else if let Some((col, val)) = part.split_once('=') {
            let (col, val) = (col.trim(), val.trim());
            assert!(col_ok(col), "bad column in {part:?}");
            let quoted = if val.chars().all(|c| c.is_ascii_digit() || c == '-' || c == '.') {
                val.to_string()
            } else {
                let inner = val.trim_matches('"').trim_matches('\'');
                assert!(
                    inner
                        .chars()
                        .all(|c| c.is_ascii_alphanumeric() || "_- .:".contains(c)),
                    "bad value in {part:?}"
                );
                format!("'{inner}'")
            };
            clauses.push(format!("{} = {}", qualify(col), quoted));
        } else {
            panic!("cannot parse predicate {part:?} (want col=val / col is [not] null)");
        }
    }
    (from, clauses.join(" AND "))
}

async fn count_rows(world: &BddWorld, table: &str, conditions: &str) -> i64 {
    let (from, where_) = build_where(table, conditions);
    sqlx::query_scalar(&format!("SELECT COUNT(*) FROM {from} WHERE {where_}"))
        .fetch_one(&world.db)
        .await
        .unwrap_or_else(|e| panic!("escape-hatch count on {table} failed: {e}"))
}

async fn dump_table_slice(world: &BddWorld, table: &str) -> Vec<String> {
    crate::support::db::dump_rows(&world.db, &format!("SELECT * FROM {table} LIMIT 20")).await
}

#[then(regex = r"^table ([a-z_]+) has a row where (.+)$")]
async fn table_has_row(world: &mut BddWorld, table: String, conditions: String) {
    let n = count_rows(world, &table, &conditions).await;
    if n == 0 {
        let rows = dump_table_slice(world, &table).await;
        panic!("expected a {table} row where {conditions}; first rows of {table}: {rows:#?}");
    }
}

#[then(regex = r"^table ([a-z_]+) has no row where (.+)$")]
async fn table_has_no_row(world: &mut BddWorld, table: String, conditions: String) {
    let n = count_rows(world, &table, &conditions).await;
    assert_eq!(n, 0, "expected NO {table} row where {conditions}, found {n}");
}

#[then(regex = r"^table ([a-z_]+) has (\d+) rows? where (.+)$")]
async fn table_has_n_rows(world: &mut BddWorld, table: String, expected: i64, conditions: String) {
    let n = count_rows(world, &table, &conditions).await;
    if n != expected {
        let rows = dump_table_slice(world, &table).await;
        panic!(
            "expected exactly {expected} {table} row(s) where {conditions}, found {n}; \
             first rows of {table}: {rows:#?}"
        );
    }
}

#[then(expr = "user {int} has primary resource {int} and secondary resource {int}")]
async fn user_has_resources(world: &mut BddWorld, user: i64, primary: i64, secondary: i64) {
    let (p, s): (f64, f64) = sqlx::query_as(
        "SELECT primary_resource, secondary_resource FROM user_storage WHERE id = ?",
    )
    .bind(user)
    .fetch_one(&world.db)
    .await
    .expect("Then: query user resources");
    assert!(
        (p - primary as f64).abs() < 0.01 && (s - secondary as f64).abs() < 0.01,
        "expected user {user} resources ({primary}, {secondary}), actual ({p}, {s})"
    );
}

#[then(expr = "planet {int} is owned by user {int}")]
async fn planet_owned_by(world: &mut BddWorld, planet: i64, user: i64) {
    let owner: Option<i64> =
        sqlx::query_scalar("SELECT CAST(owner AS SIGNED) FROM planets WHERE id = ?")
            .bind(planet)
            .fetch_one(&world.db)
            .await
            .expect("Then: query planet owner");
    assert_eq!(
        owner,
        Some(user),
        "expected planet {planet} to be owned by user {user}, actual owner: {owner:?}"
    );
}

#[then(expr = "planet {int} has no owner")]
async fn planet_has_no_owner(world: &mut BddWorld, planet: i64) {
    let owner: Option<i64> =
        sqlx::query_scalar("SELECT CAST(owner AS SIGNED) FROM planets WHERE id = ?")
            .bind(planet)
            .fetch_one(&world.db)
            .await
            .expect("Then: query planet owner");
    assert_eq!(
        owner, None,
        "expected planet {planet} to have no owner, actual owner: {owner:?}"
    );
}

async fn unlocked_relation_rows(world: &BddWorld, user: i64) -> Vec<String> {
    sqlx::query(
        "SELECT orl.object_description AS obj, CAST(orl.reference_id AS SIGNED) AS refid \
         FROM unlocked_relation ur JOIN object_relations orl ON orl.id = ur.relation_id \
         WHERE ur.user_id = ? ORDER BY orl.object_description, orl.reference_id",
    )
    .bind(user)
    .fetch_all(&world.db)
    .await
    .expect("Then: dump unlocked_relation")
    .iter()
    .map(|r| {
        format!(
            "{} {}",
            r.get::<String, _>("obj"),
            r.get::<i64, _>("refid")
        )
    })
    .collect()
}

async fn count_unlocked(world: &BddWorld, user: i64, object: &str, reference: i64) -> i64 {
    sqlx::query_scalar(
        "SELECT COUNT(*) FROM unlocked_relation ur \
         JOIN object_relations orl ON orl.id = ur.relation_id \
         WHERE ur.user_id = ? AND orl.object_description = ? AND orl.reference_id = ?",
    )
    .bind(user)
    .bind(object)
    .bind(reference)
    .fetch_one(&world.db)
    .await
    .expect("Then: count unlocked_relation")
}

#[then(expr = "table unlocked_relation has a row for user {int} and object {word} reference {int}")]
async fn unlocked_relation_has_row(world: &mut BddWorld, user: i64, object: String, reference: i64) {
    let n = count_unlocked(world, user, &object, reference).await;
    if n == 0 {
        let rows = unlocked_relation_rows(world, user).await;
        panic!(
            "expected an unlocked_relation row for user {user} and {object} {reference}; \
             actual unlocked relations of user {user}: {rows:#?}"
        );
    }
}

#[then(expr = "table unlocked_relation has no row for user {int} and object {word} reference {int}")]
async fn unlocked_relation_has_no_row(
    world: &mut BddWorld,
    user: i64,
    object: String,
    reference: i64,
) {
    let n = count_unlocked(world, user, &object, reference).await;
    assert_eq!(
        n, 0,
        "expected NO unlocked_relation row for user {user} and {object} {reference}, found {n}"
    );
}

#[then(regex = r"^user (\d+) has (\d+) units? of id (\d+) on planet (\d+)$")]
async fn user_has_units_on_planet(
    world: &mut BddWorld,
    user: i64,
    expected: i64,
    unit: i64,
    planet: i64,
) {
    let rows = sqlx::query(
        "SELECT id, user_id, unit_id, count, source_planet, target_planet, mission_id \
         FROM obtained_units WHERE user_id = ? AND unit_id = ? AND source_planet = ?",
    )
    .bind(user)
    .bind(unit)
    .bind(planet)
    .fetch_all(&world.db)
    .await
    .expect("Then: query obtained_units");

    // Schema types: id/count/planets/mission are BIGINT UNSIGNED, user_id INT,
    // unit_id SMALLINT UNSIGNED — sqlx decodes them strictly.
    let total: i64 = rows.iter().map(|r| r.get::<u64, _>("count") as i64).sum();
    let actual: Vec<String> = rows
        .iter()
        .map(|r| {
            format!(
                "id={} user={} unit={} count={} source={:?} target={:?} mission={:?}",
                r.get::<u64, _>("id"),
                r.get::<i32, _>("user_id"),
                r.get::<u16, _>("unit_id"),
                r.get::<u64, _>("count"),
                r.get::<Option<u64>, _>("source_planet"),
                r.get::<Option<u64>, _>("target_planet"),
                r.get::<Option<u64>, _>("mission_id"),
            )
        })
        .collect();

    assert_eq!(
        total, expected,
        "expected user {user} to have {expected} units of id {unit} on planet {planet}; \
         actual matching obtained_units rows: {actual:#?}"
    );
}
