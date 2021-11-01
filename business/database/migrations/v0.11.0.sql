ALTER TABLE `improvements_unit_types` CHANGE `type` `type` ENUM('ATTACK','DEFENSE','SHIELD','AMOUNT','SPEED') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;

CREATE TABLE `faction_spawn_location` ( `id` SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT , `faction_id` SMALLINT UNSIGNED NOT NULL , `galaxy_id` SMALLINT UNSIGNED NOT NULL , `sector_range_start` INT UNSIGNED NULL , `sector_range_end` INT UNSIGNED NULL , `quadrant_range_start` INT UNSIGNED NULL , `quadrant_range_end` INT UNSIGNED NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;
