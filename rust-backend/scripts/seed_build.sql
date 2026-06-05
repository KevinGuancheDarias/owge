-- BUILD_UNIT-scenario seed (idempotent), both DBs.
--   user 1 owns home 1002, resources 24000/16000, energy 110, no units.
--   unit 138 (Kino, cost 60/40, energy 3, type 4 -> share root type 1 max 10)
--   unlocked via unlocked_relation(user 1, relation 278).
-- Build 5x unit 138: expect primary -300, secondary -200; mission BUILD_UNIT(3)
--   + mission_information(relation 278, value 1002) + obtained_unit(138, 5).

START TRANSACTION;
DELETE FROM scheduled_tasks WHERE task_name='mission-run';
DELETE FROM mission_reports WHERE user_id IN (1,2);
DELETE FROM mission_information WHERE mission_id IN (SELECT id FROM missions WHERE user_id IN (1,2));
DELETE FROM obtained_units WHERE user_id IN (1,2);
DELETE FROM missions WHERE user_id IN (1,2);
DELETE FROM explored_planets WHERE user IN (1,2);
DELETE FROM unlocked_relation WHERE user_id IN (1,2);
DELETE FROM websocket_events_information WHERE user_id IN (1,2);
UPDATE planets SET owner=NULL, home=NULL WHERE id IN (1002,1003,1004);
DELETE FROM user_storage WHERE id IN (1,2);
INSERT INTO user_storage (id, username, email, faction, home_planet, energy, primary_resource, secondary_resource, has_skipped_tutorial, points, can_alter_twitch_state, banned)
VALUES (1,'rusttester','rust@test.local',1,1002,110,24000,16000,1,0,0,0);
UPDATE planets SET owner=1, home=1 WHERE id=1002;
INSERT INTO unlocked_relation (user_id, relation_id) VALUES (1, 278);
COMMIT;
SELECT 'user' AS t, primary_resource, secondary_resource FROM user_storage WHERE id=1;
SELECT 'unlocked' AS t, user_id, relation_id FROM unlocked_relation WHERE user_id=1;
