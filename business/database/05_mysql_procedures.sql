DELIMITER $$
CREATE
    DEFINER = `root`@`%` PROCEDURE `DELETE_RELATION`(IN `i_relation_id` INT UNSIGNED)
    NO SQL
BEGIN
    DELETE FROM requirements_information WHERE relation_id = i_relation_id;
    DELETE from unlocked_relation WHERE relation_id = i_relation_id;
    DELETE FROM `object_relations` WHERE `object_relations`.`id` = i_relation_id;
    COMMIT;
END$$
DELIMITER ;

DELIMITER $$
CREATE
    DEFINER = `root`@`localhost` PROCEDURE `FIND_BLOCKED_MISSIONS`()
    NO SQL
SELECT *
FROM `missions`
WHERE type >= 3
  AND resolved = 0
  AND termination_date < DATE_SUB(NOW(), INTERVAL 121 MINUTE)
ORDER BY id DESC$$
DELIMITER ;

DELIMITER $$
CREATE
    DEFINER = `root`@`localhost` PROCEDURE `FIND_ORPHAN_UNITS`()
    NO SQL
SELECT *
FROM obtained_units ou
         LEFT JOIN missions m ON m.id = ou.mission_id
WHERE ou.mission_id IS NOT NULL
  AND m.id IS NULL$$
DELIMITER ;

DELIMITER $$
CREATE
    DEFINER = `root`@`localhost` PROCEDURE `DELETE_BLOCKED_MISSIONS`()
    NO SQL
BEGIN
    DELETE
    FROM mission_information
    WHERE mission_id IN (SELECT id
                         FROM missions
                         WHERE type >= 3
                           AND resolved = 0
                           AND termination_date < DATE_SUB(NOW(), INTERVAL 121 MINUTE));

    DELETE
    FROM `missions`
    WHERE type >= 3
      AND resolved = 0
      AND termination_date < DATE_SUB(NOW(), INTERVAL 121 MINUTE);
END$$
DELIMITER ;

DELIMITER $$
CREATE
    DEFINER = `root`@`localhost` PROCEDURE `DELETE_ORPHAN_UNITS`()
    NO SQL
DELETE ou
FROM obtained_units ou
         LEFT JOIN missions m ON m.id = ou.mission_id
WHERE ou.mission_id IS NOT NULL
  AND m.id IS NULL$$
DELIMITER ;

DELIMITER $$
CREATE
    DEFINER = `root`@`localhost` PROCEDURE `DELETE_PLAYER`(IN `v_user_id` INT UNSIGNED)
    NOT DETERMINISTIC
    NO SQL
    SQL SECURITY DEFINER
BEGIN
    START TRANSACTION; DELETE FROM obtained_upgrades WHERE user_id = v_user_id;
    DELETE FROM unlocked_relation WHERE user_id = v_user_id; DELETE FROM explored_planets WHERE user = v_user_id;
    DELETE FROM websocket_events_information WHERE user_id = v_user_id;
    UPDATE planets set home = NULL, owner = NULL WHERE user_id = v_user_id;
    DELETE FROM mission_reports WHERE user_id = v_user_id; DELETE FROM user_storage WHERE id = v_user_id; COMMIT;
END
DELIMITER;


CREATE VIEW see_objects_without_relations AS
SELECT 'UNIT', u.id, name, ort.id AS relation_id
FROM units u
         LEFT JOIN object_relations ort ON ort.object_description = 'UNIT' AND ort.reference_id = u.id
UNION
SELECT 'TIME_SPECIAL', ts.id, name, ort.id AS relation_id
FROM time_specials ts
         LEFT JOIN object_relations ort ON ort.object_description = 'TIME_SPECIAL' AND ort.reference_id = ts.id
UNION
SELECT 'UPGRADE', up.id, name, ort.id AS relation_id
FROM upgrades up
         LEFT JOIN object_relations ort ON ort.object_description = 'UPGRADE' AND ort.reference_id = up.id

CREATE VIEW see_orphan_relations AS
SELECT object_description, ort.id
FROM object_relations ort
         LEFT JOIN units u ON u.id = ort.reference_id
WHERE object_description = 'UNIT'
  AND u.id IS NULL
UNION
SELECT object_description, ort.id
FROM object_relations ort
         LEFT JOIN upgrades up ON up.id = ort.reference_id
WHERE object_description = 'UPGRADE'
  AND up.id IS NULL
UNION
SELECT object_description, ort.id
FROM object_relations ort
         LEFT JOIN time_specials ts ON ts.id = ort.reference_id
WHERE object_description = 'TIME_SPECIAL'
  AND ts.id IS NULL;

CREATE VIEW see_orphan_relations AS
SELECT object_description, ort.id AS relation_id, ort.reference_id
FROM object_relations ort
         LEFT JOIN units u ON u.id = ort.reference_id
WHERE object_description = 'UNIT'
  AND u.id IS NULL
UNION
SELECT object_description, ort.id AS relation_id, ort.reference_id
FROM object_relations ort
         LEFT JOIN upgrades up ON up.id = ort.reference_id
WHERE object_description = 'UPGRADE'
  AND up.id IS NULL
UNION
SELECT object_description, ort.id AS relation_id, ort.reference_id
FROM object_relations ort
         LEFT JOIN time_specials ts ON ts.id = ort.reference_id
WHERE object_description = 'TIME_SPECIAL'
  AND ts.id IS NULL;

DELIMITER $$
CREATE
    DEFINER = `root`@`%` FUNCTION `DELETE_RELATION_FN`(`i_relation_id` INT) RETURNS int unsigned
    DETERMINISTIC
    SQL SECURITY INVOKER
BEGIN
    DELETE FROM requirements_information WHERE relation_id = i_relation_id;
    DELETE from unlocked_relation WHERE relation_id = i_relation_id;
    DELETE FROM `object_relations` WHERE `object_relations`.`id` = i_relation_id;
    RETURN 0;
END$$
DELIMITER ;