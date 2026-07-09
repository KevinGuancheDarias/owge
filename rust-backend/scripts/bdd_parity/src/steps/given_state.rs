//! Given steps: deterministic SQL seeding (BDD-PARITY-PLAN.md §6.2).
//! DELETE-then-INSERT with explicit ids only — Givens run identically in the
//! java and rust passes (§5.1 simplification), so they MUST be idempotent
//! and NOW()-free in any column the differ doesn't strip.

use cucumber::given;

use crate::world::BddWorld;

#[given(expr = "the standard test universe")]
async fn standard_test_universe(_world: &mut BddWorld) {
    // No-op marker: the runner restored the baseline before this pass (§5.2).
}

/// Find-or-create the `object_relations` row for (OBJ, reference_id) — the
/// indirection every requirement/unlock hangs off (CLAUDE.md "ObjectRelation").
pub async fn find_or_create_relation(
    db: &sqlx::MySqlPool,
    object_description: &str,
    reference_id: i64,
) -> i64 {
    let existing: Option<i64> = sqlx::query_scalar(
        "SELECT CAST(id AS SIGNED) FROM object_relations \
         WHERE object_description = ? AND reference_id = ?",
    )
    .bind(object_description)
    .bind(reference_id)
    .fetch_optional(db)
    .await
    .expect("query object_relations");
    if let Some(id) = existing {
        return id;
    }
    let inserted = sqlx::query(
        "INSERT INTO object_relations (object_description, reference_id) VALUES (?, ?)",
    )
    .bind(object_description)
    .bind(reference_id)
    .execute(db)
    .await
    .expect("insert object_relations");
    inserted.last_insert_id() as i64
}

/// DELETE-then-INSERT the HAVE_SPECIAL_LOCATION requirement on a relation —
/// mirrors what the admin panel writes (§6.2).
async fn gate_by_special_location(db: &sqlx::MySqlPool, relation_id: i64, second_value: i64) {
    sqlx::query(
        "DELETE FROM requirements_information WHERE relation_id = ? AND requirement_id = \
         (SELECT id FROM requirements WHERE code = 'HAVE_SPECIAL_LOCATION')",
    )
    .bind(relation_id)
    .execute(db)
    .await
    .expect("clear requirements_information");
    sqlx::query(
        "INSERT INTO requirements_information (relation_id, requirement_id, second_value, third_value) \
         SELECT ?, id, ?, NULL FROM requirements WHERE code = 'HAVE_SPECIAL_LOCATION'",
    )
    .bind(relation_id)
    .bind(second_value)
    .execute(db)
    .await
    .expect("insert requirements_information");
}

#[given(expr = "planet {int} has special location {int} and no owner")]
async fn planet_has_special_location(world: &mut BddWorld, planet: i64, special_location: i64) {
    sqlx::query(
        "INSERT IGNORE INTO special_locations (id, name, description, cloned_improvements) \
         VALUES (?, CONCAT('BDD SL ', ?), 'bdd-parity seeded', 0)",
    )
    .bind(special_location)
    .bind(special_location)
    .execute(&world.db)
    .await
    .expect("Given: insert special_locations");
    sqlx::query("UPDATE planets SET special_location_id = ?, owner = NULL WHERE id = ?")
        .bind(special_location)
        .bind(planet)
        .execute(&world.db)
        .await
        .expect("Given: assign special location to planet");
    world.registered_planets.insert(planet);
}

#[given(expr = "unit {int} exists gated by requirement HAVE_SPECIAL_LOCATION with second value {int}")]
async fn unit_gated_by_special_location(world: &mut BddWorld, unit: i64, second_value: i64) {
    // Fresh unit cloned from baseline unit 10 (all NOT NULL columns satisfied,
    // improvement shared — never mutated by these scenarios).
    sqlx::query("DELETE FROM obtained_units WHERE unit_id = ?")
        .bind(unit)
        .execute(&world.db)
        .await
        .expect("Given: clear obtained_units of gated unit");
    sqlx::query("DELETE FROM units WHERE id = ?")
        .bind(unit)
        .execute(&world.db)
        .await
        .expect("Given: delete previous gated unit");
    sqlx::query(
        "INSERT INTO units (id, order_number, name, display_in_requirements, attack_rule_id, \
           image_id, points, description, time, primary_resource, secondary_resource, energy, \
           type, attack, health, shield, charge, is_unique, can_fast_explore, speed, \
           improvement_id, cloned_improvements, speed_impact_group_id, critical_attack_id, \
           bypass_shield, is_invisible, stored_weight, storage_capacity) \
         SELECT ?, order_number, CONCAT('BDD gated unit ', ?), display_in_requirements, \
           attack_rule_id, image_id, points, 'bdd-parity seeded', time, primary_resource, \
           secondary_resource, energy, type, attack, health, shield, charge, is_unique, \
           can_fast_explore, speed, improvement_id, cloned_improvements, speed_impact_group_id, \
           critical_attack_id, bypass_shield, is_invisible, stored_weight, storage_capacity \
         FROM units WHERE id = 10",
    )
    .bind(unit)
    .bind(unit)
    .execute(&world.db)
    .await
    .expect("Given: clone gated unit from baseline unit 10");
    let relation = find_or_create_relation(&world.db, "UNIT", unit).await;
    gate_by_special_location(&world.db, relation, second_value).await;
}

#[given(
    expr = "time special {int} exists gated by requirement HAVE_SPECIAL_LOCATION with second value {int}"
)]
async fn time_special_gated_by_special_location(
    world: &mut BddWorld,
    time_special: i64,
    second_value: i64,
) {
    sqlx::query("DELETE FROM time_specials WHERE id = ?")
        .bind(time_special)
        .execute(&world.db)
        .await
        .expect("Given: delete previous gated time special");
    sqlx::query(
        "INSERT INTO time_specials (id, name, description, duration, recharge_time, \
           improvement_id, cloned_improvements) \
         VALUES (?, CONCAT('BDD gated TS ', ?), 'bdd-parity seeded', 60, 60, NULL, 0)",
    )
    .bind(time_special)
    .bind(time_special)
    .execute(&world.db)
    .await
    .expect("Given: insert gated time special");
    let relation = find_or_create_relation(&world.db, "TIME_SPECIAL", time_special).await;
    gate_by_special_location(&world.db, relation, second_value).await;
}

#[given(expr = "planet {int} is owned by user {int}")]
async fn planet_owned_by(world: &mut BddWorld, planet: i64, user: i64) {
    sqlx::query("UPDATE planets SET owner = ? WHERE id = ?")
        .bind(user)
        .bind(planet)
        .execute(&world.db)
        .await
        .expect("Given: set planet owner");
    world.captured_users.insert(user);
    world.registered_planets.insert(planet);
}

#[given(expr = "user {int} has an unlocked relation for object {word} reference {int}")]
async fn user_has_unlocked_relation(
    world: &mut BddWorld,
    user: i64,
    object: String,
    reference: i64,
) {
    let relation = find_or_create_relation(&world.db, &object, reference).await;
    sqlx::query("DELETE FROM unlocked_relation WHERE user_id = ? AND relation_id = ?")
        .bind(user)
        .bind(relation)
        .execute(&world.db)
        .await
        .expect("Given: clear unlocked_relation");
    sqlx::query("INSERT INTO unlocked_relation (user_id, relation_id) VALUES (?, ?)")
        .bind(user)
        .bind(relation)
        .execute(&world.db)
        .await
        .expect("Given: insert unlocked_relation");
    world.captured_users.insert(user);
}

#[given(expr = "configuration {string} is {string}")]
async fn configuration_is(world: &mut BddWorld, name: String, value: String) {
    sqlx::query(
        "INSERT INTO configuration (name, value) VALUES (?, ?) \
         ON DUPLICATE KEY UPDATE value = VALUES(value)",
    )
    .bind(name)
    .bind(value)
    .execute(&world.db)
    .await
    .expect("Given: upsert configuration");
}

#[given(expr = "user {int} has {int} primary resource and {int} secondary resource")]
async fn user_has_resources(world: &mut BddWorld, user: i64, primary: i64, secondary: i64) {
    sqlx::query("UPDATE user_storage SET primary_resource = ?, secondary_resource = ? WHERE id = ?")
        .bind(primary)
        .bind(secondary)
        .bind(user)
        .execute(&world.db)
        .await
        .expect("Given: set user resources");
    world.captured_users.insert(user);
}

#[given(regex = r"^user (\d+) has obtained upgrade (\d+) at level (\d+) (available|unavailable)$")]
async fn user_has_obtained_upgrade(
    world: &mut BddWorld,
    user: i64,
    upgrade: i64,
    level: i64,
    availability: String,
) {
    sqlx::query("DELETE FROM obtained_upgrades WHERE user_id = ? AND upgrade_id = ?")
        .bind(user)
        .bind(upgrade)
        .execute(&world.db)
        .await
        .expect("Given: clear obtained_upgrades");
    sqlx::query(
        "INSERT INTO obtained_upgrades (user_id, upgrade_id, level, available) VALUES (?, ?, ?, ?)",
    )
    .bind(user)
    .bind(upgrade)
    .bind(level)
    .bind(availability == "available")
    .execute(&world.db)
    .await
    .expect("Given: insert obtained_upgrades");
    world.captured_users.insert(user);
}

#[given(expr = "user {int} has no unlocked relation for object {word} reference {int}")]
async fn user_has_no_unlocked_relation(
    world: &mut BddWorld,
    user: i64,
    object: String,
    reference: i64,
) {
    sqlx::query(
        "DELETE ur FROM unlocked_relation ur \
         JOIN object_relations orl ON orl.id = ur.relation_id \
         WHERE ur.user_id = ? AND orl.object_description = ? AND orl.reference_id = ?",
    )
    .bind(user)
    .bind(&object)
    .bind(reference)
    .execute(&world.db)
    .await
    .expect("Given: delete unlocked_relation");
    world.captured_users.insert(user);
}

#[given(expr = "user {int} has explored planet {int}")]
async fn user_has_explored_planet(world: &mut BddWorld, user: i64, planet: i64) {
    sqlx::query("DELETE FROM explored_planets WHERE user = ? AND planet = ?")
        .bind(user)
        .bind(planet)
        .execute(&world.db)
        .await
        .expect("Given: clear explored_planets");
    sqlx::query("INSERT INTO explored_planets (user, planet) VALUES (?, ?)")
        .bind(user)
        .bind(planet)
        .execute(&world.db)
        .await
        .expect("Given: insert explored_planets");
    world.registered_planets.insert(planet);
}

#[given(regex = r"^user (\d+) has (\d+) units? of id (\d+) on planet (\d+)$")]
async fn user_has_units_on_planet(
    world: &mut BddWorld,
    user: i64,
    count: i64,
    unit: i64,
    planet: i64,
) {
    sqlx::query(
        "DELETE FROM obtained_units WHERE user_id = ? AND unit_id = ? AND source_planet = ?",
    )
    .bind(user)
    .bind(unit)
    .bind(planet)
    .execute(&world.db)
    .await
    .expect("Given: delete previous obtained_units");

    sqlx::query(
        "INSERT INTO obtained_units \
         (user_id, unit_id, count, is_from_capture, source_planet, target_planet, mission_id) \
         VALUES (?, ?, ?, 0, ?, NULL, NULL)",
    )
    .bind(user)
    .bind(unit)
    .bind(count)
    .bind(planet)
    .execute(&world.db)
    .await
    .expect("Given: insert obtained_units stack");

    world.captured_users.insert(user);
    world.registered_planets.insert(planet);
}
