-- UNIT_CAPTURE parity seed (idempotent). Applied identically to
-- sgalactica_java_13 (Java) and sgalactica_java_14 (Rust).
--   Attacker user 1 'rusttester': home 1002, 10x unit 3 (Comandos Stargate) on 1002, explored 1003.
--   Defender user 2 'defender'  : home 1004, owns 1003, 5x unit 309 (Unas Salvaje, type 6) on 1003.
-- There is a UNIT_CAPTURE rule unit 3 -> unit 309 (rule id 12); the harness forces
-- its extra_args to '100#10' so a capture ALWAYS fires and the captured count is
-- deterministically 1 (floor(killed * 0.10) = 0 for killed < 10 -> floor(rand*0)+1 = 1),
-- making the otherwise-random outcome diffable bit-for-bit across both backends.
-- Unit 3's attack rule (38) permits attacking type 6 (via ancestor type 1), so the
-- attacker actually engages and kills some unit 309. Unit 309 has no capture rule as
-- origin, so the defender never captures back.

START TRANSACTION;

DELETE FROM scheduled_tasks WHERE task_name='mission-run';
DELETE FROM mission_reports WHERE user_id IN (1,2);
DELETE FROM obtained_units WHERE user_id IN (1,2);
DELETE FROM mission_information WHERE mission_id IN (SELECT id FROM missions WHERE user_id IN (1,2));
DELETE FROM missions WHERE user_id IN (1,2);
DELETE FROM explored_planets WHERE user IN (1,2);
DELETE FROM unlocked_relation WHERE user_id IN (1,2);
DELETE FROM active_time_specials WHERE user_id IN (1,2);
DELETE FROM websocket_events_information WHERE user_id IN (1,2);
UPDATE planets SET owner=NULL, home=NULL WHERE id IN (1002,1003,1004) OR owner IN (1,2);
DELETE FROM obtained_upgrades WHERE user_id IN (1,2);
DELETE FROM user_storage WHERE id IN (1,2);

INSERT INTO user_storage
  (id, username, email, faction, home_planet, energy, primary_resource,
   secondary_resource, has_skipped_tutorial, points, can_alter_twitch_state, banned)
VALUES
  (1, 'rusttester', 'rust@test.local', 1, 1002, 110, 24000, 16000, 1, 0, 0, 0),
  (2, 'defender',   'def@test.local',  1, 1004, 110, 24000, 16000, 1, 0, 0, 0);

UPDATE planets SET owner=1, home=1 WHERE id=1002;
UPDATE planets SET owner=2, home=0 WHERE id=1003;
UPDATE planets SET owner=2, home=1 WHERE id=1004;

INSERT INTO obtained_units (user_id, unit_id, count, is_from_capture, source_planet) VALUES
  (1, 3,   10, 0, 1002),
  (2, 309, 5,  0, 1003);

INSERT INTO explored_planets (user, planet) VALUES (1, 1003);

-- Force the UNIT_CAPTURE rule 12 (unit 3 -> unit 309) to '100#10' so the capture
-- outcome is SEED-INDEPENDENT and thus identical across the Java and Rust runs
-- (which use different mission-id seeds):
--   * prob = 100  -> capture ALWAYS fires regardless of the nextDouble prob roll.
--   * amount pct = 10 -> floor(killed * 0.10) = 0 for killed < 10, and the amount
--     draw floor(rand * 0 + 1) = 1 for ANY rand, so exactly 1 unit is captured.
-- Without this the 42% default probability makes capture fire-or-not depend on the
-- per-run seed, so the two backends would legitimately diverge.
UPDATE rules SET extra_args='100#10' WHERE id=12;

COMMIT;
