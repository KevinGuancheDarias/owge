-- delete-account cascade seed (idempotent), both DBs.
-- Populates user 1 across the tables UserDeleteService's listeners touch, so a
-- subsequent DELETE /admin/users/1 must empty every one of them and remove the
-- user_storage row. (Alliance/missions/reports/suspicions/audit/time-specials
-- left empty — the cascade must still run cleanly and delete the user.)
START TRANSACTION;
DELETE FROM obtained_units WHERE user_id IN (1,2);
DELETE FROM obtained_upgrades WHERE user_id IN (1,2);
DELETE FROM unlocked_relation WHERE user_id IN (1,2);
DELETE FROM websocket_events_information WHERE user_id IN (1,2);
DELETE FROM explored_planets WHERE user IN (1,2);
DELETE FROM planet_list WHERE user_id IN (1,2);
UPDATE planets SET owner=NULL, home=NULL WHERE id IN (1002,1003,1004);
DELETE FROM user_storage WHERE id IN (1,2);
INSERT INTO user_storage (id, username, email, faction, home_planet, energy, primary_resource, secondary_resource, has_skipped_tutorial, points, can_alter_twitch_state, banned)
VALUES (1,'rusttester','rust@test.local',1,1002,110,24000,16000,1,0,0,0);
UPDATE planets SET owner=1, home=1 WHERE id=1002;
INSERT INTO unlocked_relation (user_id, relation_id) VALUES (1, 278);
INSERT INTO obtained_upgrades (user_id, upgrade_id, level, available) VALUES (1, 1, 1, 1);
INSERT INTO obtained_units (user_id, unit_id, count, source_planet, is_from_capture) VALUES (1, 10, 5, 1002, 0);
INSERT INTO websocket_events_information (event_name, user_id, last_sent) VALUES ('unit_obtained_change', 1, NOW());
INSERT INTO explored_planets (user, planet) VALUES (1, 1003);
COMMIT;
-- snapshot counts
SELECT 'unlocked_relation' t, COUNT(*) n FROM unlocked_relation WHERE user_id=1
UNION ALL SELECT 'obtained_upgrades', COUNT(*) FROM obtained_upgrades WHERE user_id=1
UNION ALL SELECT 'obtained_units', COUNT(*) FROM obtained_units WHERE user_id=1
UNION ALL SELECT 'websocket_events', COUNT(*) FROM websocket_events_information WHERE user_id=1
UNION ALL SELECT 'explored_planets', COUNT(*) FROM explored_planets WHERE user=1
UNION ALL SELECT 'planets_owned', COUNT(*) FROM planets WHERE owner=1
UNION ALL SELECT 'user_storage', COUNT(*) FROM user_storage WHERE id=1;
