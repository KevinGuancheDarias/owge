-- FIXED_MISSION_ID=900001
-- ============================================================================
-- SEED-SENSITIVE PARTIAL-KILL attack seed (idempotent), with a PRE-SEEDED mission
-- row of a FIXED id so BOTH backends execute the SAME mission id => the SAME
-- JavaRandom seed (seed = mission.id). This is the scenario that PROVES same-seed
-- bit-for-bit attack reproducibility across Java and Rust.
--
-- WHY A FIXED MISSION ID (the whole point):
--   seed = mission.id. The normal harness path creates the mission via REST POST,
--   so Java's mission gets one auto-increment id and Rust's gets another => the two
--   engines seed their RNG differently and `result`/`seed` cannot be compared.
--   Here we INSERT the `missions` row (and its db-scheduler `scheduled_tasks` row)
--   with a FIXED id 900001 BEFORE the harness snapshot. The harness then SKIPS the
--   REST POST and just nudges this pre-seeded task's execution_time into the past to
--   fire it on each backend. Snapshot-after-seed => both Java and Rust restore and
--   fire mission id 900001 => identical seed => identical shuffle => comparable
--   `result`/`seed` traces.
--   The harness recognises the `-- FIXED_MISSION_ID=<n>` marker on line 1 and may
--   override 900001 with another id (the negative control uses a SECOND id to prove
--   the outcome actually depends on the seed).
--
-- WHY THIS SCENARIO IS SEED-SENSITIVE (the shuffle decides who survives):
--   Attacker user 1 sends TWO UNEQUAL stacks of the SAME unit (unit 10, X-302) at
--   the defender's planet 1003, as two distinct obtained_units rows kept apart by
--   their source_planet:
--       * 7x unit 10 from home 1002
--       * 2x unit 10 from base 1005
--   The defender (user 2) holds 16x unit 10 on 1003. Unit 10 = attack 280,
--   health 545, shield 0. Defender total attack = 16*280 = 4480, which kills
--   floor(4480/545) = 8 of the 9 attacking units, leaving exactly ONE survivor.
--   `AttackObtainedUnitBo.shuffleUnits` randomises the order the attacker stacks
--   are processed, so WHICH stack the defender's leftover damage lands on (and thus
--   which stack the single survivor belongs to, and which source_planet it returns
--   to) depends entirely on the shuffle:
--       * defender hits the 7-stack first -> kills 7, leftover kills 1 of the
--         2-stack -> survivor is in the 2-stack (source_planet 1005).
--       * defender hits the 2-stack first -> kills 2, leftover kills 6 of the
--         7-stack -> survivor is in the 7-stack (source_planet 1002).
--   So the surviving obtained_units row (its count + source_planet) is a direct
--   function of the RNG seed. Same seed => same survivor on both backends.
--
-- Test users 1 (attacker) / 2 (defender); planets 1002/1003/1004/1005.
-- ============================================================================

SET @MISSION_ID = 900001;

START TRANSACTION;

-- Broadened cleanup (mirrors the updated seed_attack.sql), plus the fixed mission.
DELETE FROM scheduled_tasks WHERE task_name='mission-run';
DELETE FROM mission_reports WHERE user_id IN (1,2);
DELETE FROM obtained_units WHERE user_id IN (1,2);
DELETE FROM mission_information WHERE mission_id IN (SELECT id FROM missions WHERE user_id IN (1,2));
DELETE FROM mission_information WHERE mission_id = @MISSION_ID;
DELETE FROM missions WHERE user_id IN (1,2);
DELETE FROM missions WHERE id = @MISSION_ID;
DELETE FROM explored_planets WHERE user IN (1,2);
DELETE FROM unlocked_relation WHERE user_id IN (1,2);
DELETE FROM active_time_specials WHERE user_id IN (1,2);
DELETE FROM websocket_events_information WHERE user_id IN (1,2);
UPDATE planets SET owner=NULL, home=NULL WHERE id IN (1002,1003,1004,1005) OR owner IN (1,2);
DELETE FROM obtained_upgrades WHERE user_id IN (1,2);
DELETE FROM user_storage WHERE id IN (1,2);

INSERT INTO user_storage
  (id, username, email, faction, home_planet, energy, primary_resource,
   secondary_resource, has_skipped_tutorial, points, can_alter_twitch_state, banned)
VALUES
  (1, 'rusttester', 'rust@test.local', 1, 1002, 110, 24000, 16000, 1, 0, 0, 0),
  (2, 'defender',   'def@test.local',  1, 1004, 110, 24000, 16000, 1, 0, 0, 0);

-- attacker owns home 1002 and a second base 1005; defender owns target 1003 + home 1004
UPDATE planets SET owner=1, home=1 WHERE id=1002;
UPDATE planets SET owner=1, home=0 WHERE id=1005;
UPDATE planets SET owner=2, home=0 WHERE id=1003;
UPDATE planets SET owner=2, home=1 WHERE id=1004;

-- The PRE-SEEDED attack mission, fixed id, type 8 (ATTACK), user 1, 1002 -> 1003.
-- required_time is large enough that the scheduler will not fire it until the
-- harness explicitly nudges execution_time into the past.
INSERT INTO missions
  (id, user_id, type, starting_date, required_time, source_planet, target_planet,
   attemps, resolved, invisible)
VALUES
  (@MISSION_ID, 1, 8, UTC_TIMESTAMP(), 3600, 1002, 1003, 1, 0, 0);

-- Two UNEQUAL attacker stacks ATTACHED TO THE MISSION (mission_id + target_planet
-- set, exactly as a REST-POSTed attack would leave them), kept distinct by
-- source_planet so they remain two targeting entries. Defender's 16-unit force on
-- 1003 leaves exactly one fractional survivor.
INSERT INTO obtained_units (user_id, unit_id, count, is_from_capture, source_planet, target_planet, mission_id) VALUES
  (1, 10, 7,  0, 1002, 1003, @MISSION_ID),
  (1, 10, 2,  0, 1005, 1003, @MISSION_ID);
INSERT INTO obtained_units (user_id, unit_id, count, is_from_capture, source_planet) VALUES
  (2, 10, 16, 0, 1003);

-- attacker has explored the target (precondition consistency with the other seeds)
INSERT INTO explored_planets (user, planet) VALUES (1, 1003);

-- The db-scheduler row for this mission. task_name='mission-run',
-- task_instance = the mission id as a string, task_data NULL (Java uses
-- TaskWithoutDataDescriptor; Rust parses task_instance as the mission id). picked=0,
-- version=1. execution_time is in the FUTURE; the harness nudges it into the past
-- to fire the mission on each backend.
INSERT INTO scheduled_tasks
  (task_name, task_instance, task_data, execution_time, picked, version)
VALUES
  ('mission-run', CAST(@MISSION_ID AS CHAR), NULL,
   DATE_ADD(NOW(6), INTERVAL 3600 SECOND), 0, 1);

COMMIT;

SELECT 'mission' AS t, id, user_id, type, source_planet, target_planet, resolved FROM missions WHERE id=@MISSION_ID;
SELECT 'ou'      AS t, id, user_id, unit_id, count, source_planet, mission_id FROM obtained_units WHERE user_id IN (1,2) ORDER BY user_id, source_planet;
SELECT 'task'    AS t, task_name, task_instance, picked, version FROM scheduled_tasks WHERE task_name='mission-run';
