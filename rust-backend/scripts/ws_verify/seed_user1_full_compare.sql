-- Seed data so user 1 (rusttester) exercises EVERY websocket-sync payload path,
-- for the Rust-vs-Java parity harness (compare_rest_sync.sh).
-- Idempotent-ish: uses fixed high ids (9001+ / 201+); re-running after a wipe is fine,
-- re-running twice will fail on duplicate PKs (harmless).
--
-- Covers:
--   1. planet_list entry whose planet has a special location (planet 1006 / SL 202)
--   2. owned planet with a special location                  (planet 1005 / SL 201)
--   3. DEPLOYED mission from special (1005) to special (1006), with deployed units
--   4. a unit with ALL attributes set (attack rule, critical attack, speed impact
--      group, image, storage, charge, invisible=0, bypass_shield, is_unique=1) and a
--      fully-populated improvement incl. per-unit-type entries; obtained + unlocked
--   5. an unlocked time special (TS 59, relation 410 already unlocked for user 1)
--      whose improvement gets every field + per-unit-type entries
--   6. one mission report of each builder kind (attack already exists as id 238):
--      explore, gather, establish_base, conquest, error, interception, unit capture

-- ---------------------------------------------------------------------------
-- Improvements (fully populated) for the 2 special locations + the test unit
-- ---------------------------------------------------------------------------
INSERT INTO improvements (id, more_soldiers_production, more_primary_resource_production,
  more_secondary_resource_production, more_energy_production, more_charge_capacity,
  more_missions_value, more_upgrade_research_speed, more_unit_build_speed) VALUES
 (9001, 5, 10, 15, 20, 25, 2, 7.5, 12.5),
 (9002, 3,  6,  9, 12, 15, 1, 4.5,  8.25),
 (9003, 11, 22, 33, 44, 55, 3, 16.5, 21.75);

INSERT INTO improvements_unit_types (improvement_id, type, unit_type_id, value) VALUES
 (9001,'ATTACK',4,10),(9001,'DEFENSE',4,20),(9001,'SHIELD',4,30),(9001,'AMOUNT',4,5),(9001,'SPEED',4,15),
 (9002,'ATTACK',10,6),(9002,'DEFENSE',10,12),(9002,'SHIELD',10,18),(9002,'AMOUNT',10,3),(9002,'SPEED',10,9),
 (9003,'ATTACK',4,10),(9003,'DEFENSE',4,20),(9003,'SHIELD',4,30),(9003,'AMOUNT',4,5),(9003,'SPEED',4,15);

-- ---------------------------------------------------------------------------
-- Special locations
-- ---------------------------------------------------------------------------
INSERT INTO special_locations (id, name, image_id, description, galaxy_id, improvement_id, cloned_improvements) VALUES
 (201, 'Zona Especial Alfa', 1, 'Special location Alfa for backend parity tests', 1, 9001, 0),
 (202, 'Zona Especial Beta', 2, 'Special location Beta for backend parity tests', 1, 9002, 0);

-- Owned planet with special location (1005 is already owned by user 1, non-home)
UPDATE planets SET special_location_id = 201 WHERE id = 1005;

-- Planet-list planet with special location (unowned 1006, explored so it is not masked)
UPDATE planets SET special_location_id = 202 WHERE id = 1006;
INSERT INTO planet_list (user_id, planet_id, name) VALUES (1, 1006, 'Beta especial');
INSERT INTO explored_planets (user, planet) VALUES (1, 1006);

-- ---------------------------------------------------------------------------
-- DEPLOYED mission from special planet to special planet, with deployed units
-- ---------------------------------------------------------------------------
INSERT INTO missions (id, user_id, type, termination_date, required_time, starting_date,
  primary_resource, secondary_resource, required_energy, source_planet, target_planet,
  related_mission, report_id, attemps, resolved, invisible)
VALUES (900200, 1, 12, NULL, NULL, NOW(), NULL, NULL, NULL, 1005, 1006, NULL, NULL, 1, 0, 0);

INSERT INTO obtained_units (id, user_id, unit_id, count, source_planet, target_planet,
  mission_id, first_deployment_mission, is_from_capture, expiration_id, owner_unit_id)
VALUES (9002, 1, 10, 5, 1005, 1006, 900200, 900200, 0, NULL, NULL);

-- ---------------------------------------------------------------------------
-- Unit with every attribute set, unique, fully-populated improvement (9003)
-- ---------------------------------------------------------------------------
INSERT INTO units (id, order_number, name, display_in_requirements, attack_rule_id, image_id,
  points, description, time, primary_resource, secondary_resource, energy, type, attack,
  health, shield, charge, is_unique, can_fast_explore, speed, improvement_id,
  cloned_improvements, speed_impact_group_id, critical_attack_id, bypass_shield,
  is_invisible, stored_weight, storage_capacity)
VALUES (9001, 999, 'Unidad Total de Pruebas', 1, 1, 3, 1234,
  'Unit with every attribute set, used to compare Java vs Rust backend serialization',
  3600, 11111, 22222, 333, 4, 444, 555, 66, 77, 1, 1, 8.5, 9003, 0, 1, 1, 1, 0, 10, 5000);

-- Owned by user 1 on his home planet
INSERT INTO obtained_units (id, user_id, unit_id, count, source_planet, target_planet,
  mission_id, first_deployment_mission, is_from_capture, expiration_id, owner_unit_id)
VALUES (9001, 1, 9001, 1, 1002, NULL, NULL, NULL, 0, NULL, NULL);

-- Unlocked for user 1 so it also appears in unit_unlocked_change
INSERT INTO object_relations (id, object_description, reference_id) VALUES (9001, 'UNIT', 9001);
INSERT INTO unlocked_relation (id, user_id, relation_id) VALUES (9001, 1, 9001);

-- ---------------------------------------------------------------------------
-- Unlocked time special with ALL improvements set
-- (TS 59 'Refuerzos del Pentagono' is already unlocked for user 1 via relation 410;
--  its improvement is 417 -> fill every field + per-unit-type entries)
-- ---------------------------------------------------------------------------
UPDATE improvements SET
  more_soldiers_production = 8,
  more_primary_resource_production = 100,
  more_secondary_resource_production = 100,
  more_energy_production = 40,
  more_charge_capacity = 60,
  more_missions_value = 2,
  more_upgrade_research_speed = 25,
  more_unit_build_speed = 10
WHERE id = 417;

INSERT INTO improvements_unit_types (improvement_id, type, unit_type_id, value) VALUES
 (417,'ATTACK',10,12),(417,'DEFENSE',10,18),(417,'SHIELD',10,24),(417,'AMOUNT',10,4),(417,'SPEED',10,9);

-- ---------------------------------------------------------------------------
-- One mission report of each builder kind (attack already exists, id 238)
-- ---------------------------------------------------------------------------
-- EXPLORE (also linked from a resolved EXPLORE mission to exercise the
-- missionId/missionDate join in mission_report_change)
INSERT INTO mission_reports (id, json_body, user_id, report_date, is_enemy, user_read_date) VALUES
(9001, '{"senderUser":{"id":1,"username":"rusttester","canAlterTwitchState":false},"sourcePlanet":{"id":1002,"name":"VS1C1N1","sector":1,"quadrant":1,"planetNumber":1,"ownerId":1,"ownerName":"rusttester","richness":20,"home":true,"galaxyId":1,"galaxyName":"Via Bug"},"targetPlanet":{"id":1006,"name":"VS1C1N5","sector":1,"quadrant":1,"planetNumber":5,"richness":30,"home":false,"galaxyId":1,"galaxyName":"Via Bug"},"involvedUnits":[{"id":274,"count":1,"unit":{"id":10,"name":"X-302","points":90,"typeId":10,"typeName":"Caza Pesado","attack":280,"health":545,"shield":0,"charge":0,"isUnique":false,"canFastExplore":false,"speed":7.0,"clonedImprovements":false}}],"unitsInPlanet":[]}', 1, NOW(), 0, NULL),
-- GATHER
(9002, '{"senderUser":{"id":1,"username":"rusttester","canAlterTwitchState":false},"sourcePlanet":{"id":1002,"name":"VS1C1N1","sector":1,"quadrant":1,"planetNumber":1,"ownerId":1,"ownerName":"rusttester","richness":20,"home":true,"galaxyId":1,"galaxyName":"Via Bug"},"targetPlanet":{"id":1007,"name":"VS1C1N6","sector":1,"quadrant":1,"planetNumber":6,"richness":30,"home":false,"galaxyId":1,"galaxyName":"Via Bug"},"involvedUnits":[{"id":274,"count":1,"unit":{"id":10,"name":"X-302","typeId":10,"typeName":"Caza Pesado","isUnique":false}}],"gatheredPrimary":1234.5,"gatheredSecondary":678.9}', 1, NOW(), 0, NOW()),
-- ESTABLISH_BASE
(9003, '{"senderUser":{"id":1,"username":"rusttester","canAlterTwitchState":false},"sourcePlanet":{"id":1002,"name":"VS1C1N1","sector":1,"quadrant":1,"planetNumber":1,"ownerId":1,"ownerName":"rusttester","richness":20,"home":true,"galaxyId":1,"galaxyName":"Via Bug"},"targetPlanet":{"id":1005,"name":"VS1C1N4","sector":1,"quadrant":1,"planetNumber":4,"ownerId":1,"ownerName":"rusttester","richness":30,"home":false,"galaxyId":1,"galaxyName":"Via Bug"},"involvedUnits":[],"establishBaseStatus":true,"establishBaseStatusStr":""}', 1, NOW(), 0, NOW()),
-- CONQUEST
(9004, '{"senderUser":{"id":1,"username":"rusttester","canAlterTwitchState":false},"sourcePlanet":{"id":1002,"name":"VS1C1N1","sector":1,"quadrant":1,"planetNumber":1,"ownerId":1,"ownerName":"rusttester","richness":20,"home":true,"galaxyId":1,"galaxyName":"Via Bug"},"targetPlanet":{"id":1008,"name":"VS1C1N7","sector":1,"quadrant":1,"planetNumber":7,"richness":30,"home":false,"galaxyId":1,"galaxyName":"Via Bug"},"involvedUnits":[],"conquestStatus":true,"conquestStatusStr":"I18N_PLANET_CONQUISTED"}', 1, NOW(), 0, NULL),
-- ERROR
(9005, '{"senderUser":{"id":1,"username":"rusttester","canAlterTwitchState":false},"sourcePlanet":{"id":1002,"name":"VS1C1N1","sector":1,"quadrant":1,"planetNumber":1,"ownerId":1,"ownerName":"rusttester","richness":20,"home":true,"galaxyId":1,"galaxyName":"Via Bug"},"targetPlanet":{"id":1009,"name":"VS1C1N8","sector":1,"quadrant":1,"planetNumber":8,"richness":30,"home":false,"galaxyId":1,"galaxyName":"Via Bug"},"involvedUnits":[],"errorText":"I18N_ERR_MISSION_FAILED"}', 1, NOW(), 0, NOW()),
-- INTERCEPTION
(9006, '{"senderUser":{"id":1,"username":"rusttester","canAlterTwitchState":false},"sourcePlanet":{"id":1002,"name":"VS1C1N1","sector":1,"quadrant":1,"planetNumber":1,"ownerId":1,"ownerName":"rusttester","richness":20,"home":true,"galaxyId":1,"galaxyName":"Via Bug"},"targetPlanet":{"id":1010,"name":"VS1C1N9","sector":1,"quadrant":1,"planetNumber":9,"richness":30,"home":false,"galaxyId":1,"galaxyName":"Via Bug"},"involvedUnits":[],"interceptionInfo":[{"interceptorUser":"otheruser","interceptorUnit":{"id":500,"count":2,"unit":{"id":16,"name":"Destacamento de Stargate","typeId":14,"isUnique":false}},"units":[{"id":274,"count":1,"unit":{"id":10,"name":"X-302","typeId":10,"isUnique":false}}]}]}', 1, NOW(), 0, NULL),
-- UNIT CAPTURE
(9007, '{"senderUser":{"id":1,"username":"rusttester","canAlterTwitchState":false},"sourcePlanet":{"id":1002,"name":"VS1C1N1","sector":1,"quadrant":1,"planetNumber":1,"ownerId":1,"ownerName":"rusttester","richness":20,"home":true,"galaxyId":1,"galaxyName":"Via Bug"},"targetPlanet":{"id":1003,"name":"VS1C1N2","sector":1,"quadrant":1,"planetNumber":2,"richness":30,"home":false,"galaxyId":1,"galaxyName":"Via Bug"},"involvedUnits":[],"unitCaptureInformation":[{"unit":{"id":10,"name":"X-302","typeId":10,"typeName":"Caza Pesado","isUnique":false},"oldOwner":{"id":2,"username":"enemyuser","canAlterTwitchState":false},"capturedCount":3}]}', 1, NOW(), 0, NULL);

-- Resolved EXPLORE mission pointing at the explore report (missionId join path)
INSERT INTO missions (id, user_id, type, termination_date, required_time, starting_date,
  primary_resource, secondary_resource, required_energy, source_planet, target_planet,
  related_mission, report_id, attemps, resolved, invisible)
VALUES (900201, 1, 4, NOW(), 60, NOW(), NULL, NULL, NULL, 1002, 1006, NULL, 9001, 1, 1, 0);
