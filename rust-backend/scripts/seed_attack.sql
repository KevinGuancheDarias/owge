-- Attack-scenario seed (idempotent). Applied identically to sgalactica_java_13
-- (Java) and sgalactica_java_14 (Rust) for the ATTACK parity test.
--   Attacker user 1 'rusttester' : home 1002, 10x unit 10 (X-302) on 1002, has explored 1003.
--   Defender user 2 'defender'   : home 1004, owns 1003, 10x unit 10 (X-302) on 1003.
-- Both sides use exactly the same units (unit 10), symmetric 10v10.

START TRANSACTION;

DELETE FROM scheduled_tasks WHERE task_name='mission-run';
DELETE FROM mission_reports WHERE user_id IN (1,2);
DELETE FROM obtained_units WHERE user_id IN (1,2);
DELETE FROM missions WHERE user_id IN (1,2);
DELETE FROM explored_planets WHERE user IN (1,2);
DELETE FROM websocket_events_information WHERE user_id IN (1,2);
DELETE FROM unlocked_relation WHERE user_id IN (1,2);
DELETE FROM active_time_specials WHERE user_id IN (1,2);
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

-- attacker's stack on home 1002, defender's stack on contested 1003
INSERT INTO obtained_units (user_id, unit_id, count, is_from_capture, source_planet) VALUES
  (1, 10, 10, 0, 1002),
  (2, 10, 10, 0, 1003);

-- attacker has explored the target planet (precondition for non-explore missions)
INSERT INTO explored_planets (user, planet) VALUES (1, 1003);

COMMIT;

SELECT 'users'  AS t, id, username, home_planet FROM user_storage WHERE id IN (1,2);
SELECT 'planets' AS t, id, owner, home FROM planets WHERE id IN (1002,1003,1004);
SELECT 'ou' AS t, id, user_id, unit_id, count, source_planet, mission_id FROM obtained_units WHERE user_id IN (1,2);
SELECT 'explored' AS t, user, planet FROM explored_planets WHERE user=1;
