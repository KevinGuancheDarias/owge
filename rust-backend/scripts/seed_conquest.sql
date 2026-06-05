-- Conquest-scenario seed (idempotent), both DBs.
--   Attacker user 1: home 1002, 10x unit 10 (X-302), explored 1003.
--   Defender user 2: home 1004, owns NON-HOME 1003 with a WEAK 2x unit 10 force.
-- Expected: attacker overwhelms (defender wiped), planet 1003 -> user 1,
--   conquest report I18N_PLANET_IS_NOW_OURS.

START TRANSACTION;

DELETE FROM scheduled_tasks WHERE task_name='mission-run';
DELETE FROM mission_reports WHERE user_id IN (1,2);
DELETE FROM obtained_units WHERE user_id IN (1,2);
DELETE FROM missions WHERE user_id IN (1,2);
DELETE FROM explored_planets WHERE user IN (1,2);
DELETE FROM websocket_events_information WHERE user_id IN (1,2);
UPDATE planets SET owner=NULL, home=NULL WHERE id IN (1002,1003,1004);
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
  (1, 10, 10, 0, 1002),
  (2, 10,  2, 0, 1003);

INSERT INTO explored_planets (user, planet) VALUES (1, 1003);

COMMIT;

SELECT 'planet1003' AS t, id, owner, home FROM planets WHERE id=1003;
SELECT 'ou' AS t, user_id, unit_id, count, source_planet FROM obtained_units WHERE user_id IN (1,2);
