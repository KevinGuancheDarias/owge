//! Then steps asserting table state (BDD-PARITY-PLAN.md §6.4). On failure the
//! message contains the ACTUAL rows — failure output is the product here.

use cucumber::then;
use sqlx::Row;

use crate::world::BddWorld;

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
