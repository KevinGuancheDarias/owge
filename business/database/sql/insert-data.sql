-- START TEMPORARY Default admin
INSERT INTO admin_users (id,username,password,mail,enabled) VALUES(1,'KevinGuancheDarias','StrongestPaul','kevin@kevinguanchedarias.com',1);
-- END TEMPORARY Default admin

-- START Prepare  objects
INSERT INTO objects (description, repository) VALUES ('RACE_SPECIAL', 'invalid.RaceSpecial');
INSERT INTO objects (description, repository) VALUES ('UNIT', 'com.kevinguanchedarias.sgtjava.repository.UnitRepository');
INSERT INTO objects (description, repository) VALUES ('UPGRADE', 'com.kevinguanchedarias.sgtjava.repository.UpgradeRepository');
-- END Prepare objects

-- START Prepare requirements
INSERT INTO requirements (id,code, description) VALUES (1, 'HAVE_SPECIAL_LOCATION', 'Tener lugar especial');
INSERT INTO requirements (id,code, description) VALUES (2, 'HAVE_UNIT', 'Tener unidad');
INSERT INTO requirements (id,code, description) VALUES (3, 'BEEN_RACE', 'Pertenecer a raza');
INSERT INTO requirements (id,code, description) VALUES (4, 'UPGRADE_LEVEL', 'Nivel de mejora');
INSERT INTO requirements (id,code, description) VALUES (5, 'WORST_PLAYER', 'Peor jugador');
INSERT INTO requirements (id,code, description) VALUES (6, 'UNIT_AMOUNT', 'Cantidad unidad');
INSERT INTO requirements (id,code, description) VALUES (7, 'HOME_GALAXY', 'Galaxia original');
INSERT INTO requirements (id,code, description) VALUES (8, 'HAVE_SPECIAL_AVAILABLE', 'Tener especial disponible');
INSERT INTO requirements (id,code, description) VALUES (9, 'HAVE_SPECIAL_ENABLED', 'Tener especial habilitado');
-- END Prepare requirements

-- START Prepare mission types
INSERT INTO mission_types (id,code,description,is_shared) VALUES (1,'LEVEL_UP','Sube de nivel una mejora',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (2,'BROADCAST_MESSAGE','Envía un mensaje a todos los usuarios conectados',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (3,'BUILD_UNIT','Construye una unidad',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (4,'EXPLORE','Explores a planet',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (5,'RETURN_MISSION','Returns the unit to the source planet',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (6,'GATHER','Gather resources from specified planet',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (7,'ESTABLISH_BASE','Establish a base in a new planet',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (8,'ATTACK','Attacks an user',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (9,'COUNTERATTACK','Counterattacks a own planet',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (10,'CONQUEST','Conquest the planet owned by other user',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (11,'DEPLOY','Deploys the unit in the target planet',0);
INSERT INTO mission_types (id,code,description,is_shared) VALUES (12,'DEPLOYED','The unit is deployed',0);