DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `DELETE_BLOCKED_MISSIONS`()
    NO SQL
BEGIN
    DELETE FROM mission_information WHERE mission_id IN ( SELECT id FROM missions WHERE type >= 3 AND resolved=0 AND termination_date < DATE_SUB(NOW(), INTERVAL 121 MINUTE ));
	
    DELETE FROM `missions`
	WHERE type >= 3 AND resolved=0 AND termination_date < DATE_SUB(NOW(), INTERVAL 121 MINUTE );
END$$
DELIMITER ;

DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `DELETE_ORPHAN_UNITS`()
    NO SQL
DELETE ou FROM obtained_units ou 
	LEFT JOIN missions m ON m.id = ou.mission_id
    WHERE ou.mission_id IS NOT NULL AND m.id IS NULL$$
DELIMITER ;

DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `FIND_ORPHAN_UNITS`()
    NO SQL
SELECT * FROM obtained_units ou 
	LEFT JOIN missions m ON m.id = ou.mission_id
    WHERE ou.mission_id IS NOT NULL AND m.id IS NULL$$
DELIMITER ;

DELIMITER $$
CREATE DEFINER=`root`@`localhost` PROCEDURE `FIND_BLOCKED_MISSIONS`()
    NO SQL
SELECT * FROM `missions` WHERE type >= 3 AND resolved=0 AND termination_date < DATE_SUB(NOW(), INTERVAL 121 MINUTE ) ORDER BY id DESC$$
DELIMITER ;
