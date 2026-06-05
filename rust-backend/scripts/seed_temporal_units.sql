-- Temporal-units (phase-4) seed (idempotent), both DBs.
--   user 1 owns home 1002. Time special 648 "Supergate" (duration 300s,
--   recharge 864000s) is unlocked (object_relation TIME_SPECIAL/648 = relation 819).
--   It has ONE TIME_SPECIAL_IS_ENABLED_TEMPORAL_UNITS rule -> UNIT 491 (Supergate),
--   extra_args 21601#1  => grant 1x unit 491 expiring 21601s after activation.
-- Activate ts 648 -> expect:
--   active_time_specials(user 1, ts 648, ACTIVE, expiring=+300s)
--   obtained_unit_temporal_information(duration 21601, expiration=+21601s, relation_id 819)
--   obtained_units(user 1, unit 491, count 1, source_planet 1002, expiration_id=<new>)
--   one UNIT_EXPIRED scheduled task (Rust) / QRTZ trigger (Java) firing +21601s.

START TRANSACTION;
DELETE FROM obtained_units WHERE user_id = 1;
DELETE FROM active_time_specials WHERE user_id = 1;
DELETE FROM obtained_unit_temporal_information WHERE relation_id = 819;
DELETE FROM scheduled_tasks WHERE task_name IN ('UNIT_EXPIRED','TIME_SPECIAL_EFFECT_END','TIME_SPECIAL_IS_READY');
DELETE FROM obtained_upgrades WHERE user_id = 1;
DELETE FROM unlocked_relation WHERE user_id = 1;
DELETE FROM websocket_events_information WHERE user_id = 1;
INSERT INTO user_storage (id, username, email, faction, home_planet, energy, primary_resource, secondary_resource, has_skipped_tutorial, points, can_alter_twitch_state, banned)
VALUES (1, 'rusttester', 'rust@test.local', 8, 1002, 50000, 24000, 16000, 1, 0, 0, 0)
ON DUPLICATE KEY UPDATE username=VALUES(username), email=VALUES(email), faction=VALUES(faction),
  home_planet=VALUES(home_planet), energy=VALUES(energy), primary_resource=VALUES(primary_resource),
  secondary_resource=VALUES(secondary_resource), banned=0;
UPDATE planets SET owner = 1, home = 1 WHERE id = 1002;
-- Unlock the time special for the user (relation 819 = object_relation TIME_SPECIAL/648).
INSERT INTO unlocked_relation (user_id, relation_id) VALUES (1, 819);
COMMIT;
