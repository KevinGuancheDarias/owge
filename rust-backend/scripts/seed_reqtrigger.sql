-- REQUIREMENT RE-TRIGGER scenario seed (idempotent), both DBs — UPGRADE_LEVEL path.
--   user 1, faction 8 "Ori" (satisfies BEEN_RACE(8)), home 1002, fat resources.
--   Relation 247 = UNIT 123 "Prior", gated by:
--     BEEN_RACE(8) + UPGRADE_LEVEL(91,1) + UPGRADE_LEVEL(95,1) + UPGRADE_LEVEL(96,1).
--   Pre-seed upgrades 91 & 96 at level 1 (met); upgrade 95 at level 0 (available, NOT met).
--   Initially relation 247 NOT unlocked.
-- Then level upgrade 95 to 1 via the LEVEL_UP mission -> on completion MissionBo calls
--   triggerLevelUpCompleted(95) -> re-evals relations gated by UPGRADE_LEVEL(95) ->
--   relation 247 now has all reqs met -> unlocked_relation gains (user 1, relation 247).
-- Verify: unlocked_relation for user 1 IDENTICAL across both backends after the level-up.

START TRANSACTION;
DELETE FROM scheduled_tasks WHERE task_name='mission-run';
DELETE FROM mission_reports WHERE user_id IN (1,2);
DELETE FROM mission_information WHERE mission_id IN (SELECT id FROM missions WHERE user_id IN (1,2));
DELETE FROM obtained_units WHERE user_id IN (1,2);
DELETE FROM obtained_upgrades WHERE user_id IN (1,2);
DELETE FROM missions WHERE user_id IN (1,2);
DELETE FROM explored_planets WHERE user IN (1,2);
DELETE FROM unlocked_relation WHERE user_id IN (1,2);
DELETE FROM websocket_events_information WHERE user_id IN (1,2);
UPDATE planets SET owner=NULL, home=NULL WHERE id IN (1002,1003,1004);
DELETE FROM user_storage WHERE id IN (1,2);
INSERT INTO user_storage (id, username, email, faction, home_planet, energy, primary_resource, secondary_resource, has_skipped_tutorial, points, can_alter_twitch_state, banned)
VALUES (1,'rusttester','rust@test.local',8,1002,5000,100000,100000,1,0,0,0);
UPDATE planets SET owner=1, home=1 WHERE id=1002;
INSERT INTO obtained_upgrades (user_id, upgrade_id, level, available) VALUES
  (1, 95, 0, 1),
  (1, 91, 1, 1),
  (1, 96, 1, 1);
COMMIT;
SELECT 'pre-unlocked' AS t, GROUP_CONCAT(relation_id ORDER BY relation_id) FROM unlocked_relation WHERE user_id=1;
SELECT 'obtained_upg' AS t, upgrade_id, level, available FROM obtained_upgrades WHERE user_id=1 ORDER BY upgrade_id;
