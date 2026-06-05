-- LEVEL_UP-scenario seed (idempotent), both DBs.
--   user 1 owns home 1002, resources 24000/16000.
--   obtained_upgrade: user 1, upgrade 1 (Reclutamiento, cost 490/330, time 280,
--     level_effect 0.5), level 2, available=1. object_relation(UPGRADE,1)=relation 1.
-- Register LEVEL_UP of upgrade 1: calculateRequirementsAreMet grows the base cost
--   by level_effect once per owned level (2 iterations):
--     primary  490 -> 735    -> 1102.5
--     secondary 330 -> 495   -> 742.5
--     time      280 -> 420   -> 630
--   ZERO_UPGRADE_TIME=FALSE and no research-speed improvement -> requiredTime stays 630.
--   Expect user primary 24000-1102.5=22897.5, secondary 16000-742.5=15257.5;
--   mission LEVEL_UP(1) + mission_information(relation 1, value 3 = level+1).

START TRANSACTION;
DELETE FROM scheduled_tasks WHERE task_name='mission-run';
DELETE FROM mission_reports WHERE user_id IN (1,2);
DELETE FROM mission_information WHERE mission_id IN (SELECT id FROM missions WHERE user_id IN (1,2));
DELETE FROM obtained_units WHERE user_id IN (1,2);
DELETE FROM obtained_upgrades WHERE user_id IN (1,2);
DELETE FROM missions WHERE user_id IN (1,2);
DELETE FROM unlocked_relation WHERE user_id IN (1,2);
DELETE FROM websocket_events_information WHERE user_id IN (1,2);
UPDATE planets SET owner=NULL, home=NULL WHERE id IN (1002,1003,1004);
DELETE FROM user_storage WHERE id IN (1,2);
INSERT INTO user_storage (id, username, email, faction, home_planet, energy, primary_resource, secondary_resource, has_skipped_tutorial, points, can_alter_twitch_state, banned)
VALUES (1,'rusttester','rust@test.local',1,1002,110,24000,16000,1,0,0,0);
UPDATE planets SET owner=1, home=1 WHERE id=1002;
INSERT INTO obtained_upgrades (user_id, upgrade_id, level, available) VALUES (1, 1, 2, 1);
COMMIT;
SELECT 'user' AS t, primary_resource, secondary_resource FROM user_storage WHERE id=1;
SELECT 'obtained_upgrade' AS t, upgrade_id, level, available FROM obtained_upgrades WHERE user_id=1;
