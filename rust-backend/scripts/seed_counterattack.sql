-- Counterattack-scenario seed (idempotent), both DBs.
--   user 1 'rusttester': owns home 1002 AND target 1003, 10x unit 10 on 1002.
--   user 2 'defender'  : home 1004, 10x unit 10 stationed on 1003 (the enemy
--                        present on user 1's planet).
-- Counterattack requires the target (1003) to belong to the sender (user 1).
-- Symmetric 10v10 -> expect 5/5 survivors; user 1 survivors return to 1002.

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
UPDATE planets SET owner=1, home=0 WHERE id=1003;
UPDATE planets SET owner=2, home=1 WHERE id=1004;

INSERT INTO obtained_units (user_id, unit_id, count, is_from_capture, source_planet) VALUES
  (1, 10, 10, 0, 1002),
  (2, 10, 10, 0, 1003);

COMMIT;

SELECT 'planets' AS t, id, owner FROM planets WHERE id IN (1002,1003);
SELECT 'ou' AS t, user_id, count, source_planet FROM obtained_units WHERE user_id IN (1,2);
