-- MISSION INTERCEPTION scenario seed (idempotent), both DBs.
--   Attacker user 1 (faction 1, home 1002): 10x unit 5 (Doctor Daniel Jackson,
--     own speed_impact_group_id=1), has explored 1003. Sends ATTACK -> 1003.
--   Defender user 2 (faction 2, home 1004, owns 1003): 5x unit 346 (Campo de Fuerza),
--     sitting on 1003 (mission NULL, source_planet=1003). Unit 346 declares
--     interceptable_speed_group {1}.
-- On ATTACK fire, doRunUnitMission interception check: the attacker's involved
--   units have applicable SIG 1, which is in the defender's interceptable set, and
--   the two users are enemies (no alliances) -> FULL interception. The attack never
--   reaches combat: attacker stack removed, interception report persisted, mission resolved.
-- Verify: identical post-fire DB state across both backends.

START TRANSACTION;
DELETE FROM scheduled_tasks WHERE task_name='mission-run';
DELETE FROM mission_reports WHERE user_id IN (1,2);
DELETE FROM mission_information WHERE mission_id IN (SELECT id FROM missions WHERE user_id IN (1,2));
DELETE FROM obtained_units WHERE user_id IN (1,2);
DELETE FROM obtained_upgrades WHERE user_id IN (1,2);
DELETE FROM missions WHERE user_id IN (1,2);
DELETE FROM explored_planets WHERE user IN (1,2);
DELETE FROM unlocked_relation WHERE user_id IN (1,2);
DELETE FROM active_time_specials WHERE user_id IN (1,2);
DELETE FROM websocket_events_information WHERE user_id IN (1,2);
UPDATE planets SET owner=NULL, home=NULL WHERE id IN (1002,1003,1004);
DELETE FROM user_storage WHERE id IN (1,2);

INSERT INTO user_storage
  (id, username, email, faction, home_planet, energy, primary_resource,
   secondary_resource, has_skipped_tutorial, points, can_alter_twitch_state, banned)
VALUES
  (1, 'rusttester', 'rust@test.local', 1, 1002, 10000, 1000000, 1000000, 1, 0, 0, 0),
  (2, 'defender',   'def@test.local',  2, 1004, 10000, 1000000, 1000000, 1, 0, 0, 0);

UPDATE planets SET owner=1, home=1 WHERE id=1002;
UPDATE planets SET owner=2, home=0 WHERE id=1003;
UPDATE planets SET owner=2, home=1 WHERE id=1004;

-- attacker SIG-1 stack on home 1002; defender interceptor on contested 1003
INSERT INTO obtained_units (user_id, unit_id, count, is_from_capture, source_planet) VALUES
  (1, 5,   10, 0, 1002),
  (2, 346, 5,  0, 1003);

INSERT INTO explored_planets (user, planet) VALUES (1, 1003);

COMMIT;
SELECT 'ou' AS t, id, user_id, unit_id, count, source_planet, mission_id FROM obtained_units WHERE user_id IN (1,2) ORDER BY user_id;
