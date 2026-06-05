-- Gather-scenario seed (idempotent). Applied identically to sgalactica_java_13
-- (Java) and sgalactica_java_14 (Rust).
--   user 1 'rusttester': home 1002, 10x unit 28 (Tel'tak, charge 85) on 1002,
--   has explored 1003 (unowned, richness 60). No defender, no trigger-attack.
-- Expected gather: 10*85=850 * richness(0.60)=510; faction 1 split 60/40 ->
--   primary +306, secondary +204 (no improvements).

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
  (1, 'rusttester', 'rust@test.local', 1, 1002, 110, 24000, 16000, 1, 0, 0, 0);

UPDATE planets SET owner=1, home=1 WHERE id=1002;

INSERT INTO obtained_units (user_id, unit_id, count, is_from_capture, source_planet)
VALUES (1, 28, 10, 0, 1002);

INSERT INTO explored_planets (user, planet) VALUES (1, 1003);

COMMIT;

SELECT 'user'   AS t, id, username, primary_resource, secondary_resource FROM user_storage WHERE id=1;
SELECT 'ou'     AS t, id, unit_id, count, source_planet FROM obtained_units WHERE user_id=1;
SELECT 'planet' AS t, id, owner, richness FROM planets WHERE id=1003;
