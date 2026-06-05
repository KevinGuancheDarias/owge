-- Deploy-scenario seed (idempotent), both DBs. Owned-target path (merge).
--   user 1 owns home 1002 AND 1003. 10x unit 10 on 1002, 3x unit 10 already on 1003.
-- Deploy 10 from 1002 -> 1003 should land+merge: 1003 = 13x unit 10, 1002 = 0.
-- Deploy produces no report; compare obtained_units end state.

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
UPDATE planets SET owner=1, home=0 WHERE id=1003;

INSERT INTO obtained_units (user_id, unit_id, count, is_from_capture, source_planet) VALUES
  (1, 10, 10, 0, 1002),
  (1, 10,  3, 0, 1003);

COMMIT;

SELECT 'ou_before' AS t, id, count, source_planet, mission_id FROM obtained_units WHERE user_id=1 ORDER BY source_planet;
