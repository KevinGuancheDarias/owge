-- ASYMMETRIC PARTIAL-KILL attack seed (idempotent). Applied identically to both
-- backends (shared `owge` DB via the mission_verify snapshot/restore topology;
-- the dc13/dc14 universes referenced by the other seeds do not exist here).
--
-- THE POINT OF THIS SCENARIO (the one case that ONLY matches with seeded RNG):
--   Attacker user 1 sends TWO UNEQUAL stacks of the SAME unit (unit 10, X-302)
--   at the defender's planet 1003, from two different source planets so they stay
--   as two distinct obtained_units rows / two distinct targeting entries:
--       * 7x unit 10 from home 1002
--       * 2x unit 10 from 1005
--   The defender (user 2) holds a force on 1003 sized to kill MOST but NOT ALL of
--   the 9 attacking units, leaving a single fractional survivor.
--   `AttackObtainedUnitBo.shuffleUnits` randomises the order the attacker stacks
--   are processed as targets, so WHICH stack absorbs the partial hit (i.e. whether
--   the surviving unit belongs to the 7-stack or the 2-stack, and therefore which
--   source_planet it returns to) is RNG-dependent.
--   * Without seeded RNG: Java's Collections.shuffle and Rust's xorshift pick
--     different orders -> obtained_units survivor rows DIVERGE between backends.
--   * With ATTACK_DETERMINISTIC_RNG=TRUE (mission-id-seeded java.util.Random in
--     both): identical shuffle -> identical survivor stack -> table_diff matches.
--
--   Both sides use unit 10 (attack 280, health 545, shield 0) so the combat math
--   is simple and the only variable is the shuffle order. Defender force is sized
--   to wipe 8 of the 9 attackers (~8*545 = 4360 dmg needed; 16x280 = 4480).
--
-- Test users 1 (attacker) and 2 (defender); planets 1002/1003/1004/1005.

START TRANSACTION;

DELETE FROM scheduled_tasks WHERE task_name='mission-run';
DELETE FROM mission_reports WHERE user_id IN (1,2);
DELETE FROM obtained_units WHERE user_id IN (1,2);
DELETE FROM mission_information WHERE mission_id IN (SELECT id FROM missions WHERE user_id IN (1,2));
DELETE FROM missions WHERE user_id IN (1,2);
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

-- attacker owns home 1002 and a second base 1005; defender owns target 1003 and home 1004
UPDATE planets SET owner=1, home=1 WHERE id=1002;
UPDATE planets SET owner=1, home=0 WHERE id=1005;
UPDATE planets SET owner=2, home=0 WHERE id=1003;
UPDATE planets SET owner=2, home=1 WHERE id=1004;

-- two UNEQUAL attacker stacks (kept distinct via different source_planet),
-- and a defender force big enough to leave exactly one fractional survivor.
INSERT INTO obtained_units (user_id, unit_id, count, is_from_capture, source_planet) VALUES
  (1, 10, 7,  0, 1002),
  (1, 10, 2,  0, 1005),
  (2, 10, 16, 0, 1003);

-- attacker has explored the target (precondition for a non-explore mission)
INSERT INTO explored_planets (user, planet) VALUES (1, 1003);

COMMIT;

SELECT 'users'   AS t, id, username, home_planet FROM user_storage WHERE id IN (1,2);
SELECT 'planets' AS t, id, owner, home FROM planets WHERE id IN (1002,1003,1004,1005);
SELECT 'ou'      AS t, id, user_id, unit_id, count, source_planet FROM obtained_units WHERE user_id IN (1,2) ORDER BY user_id, source_planet;
SELECT 'explored' AS t, user, planet FROM explored_planets WHERE user=1;
