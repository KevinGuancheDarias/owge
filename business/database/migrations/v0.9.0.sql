ALTER TABLE `unit_types` DROP `image`;
ALTER TABLE `unit_types` ADD `image_id` BIGINT(20) UNSIGNED NULL AFTER `max_count`;
ALTER TABLE `unit_types` ADD INDEX( `image_id`);
ALTER TABLE `unit_types` ADD FOREIGN KEY (`image_id`) REFERENCES `images_store`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `factions` DROP `image`, `primary_resource_image_id`, `secondary_resource_image_id`, `energy_image`;
ALTER TABLE `factions` ADD `image_id` BIGINT(20) UNSIGNED NULL AFTER `name`;
ALTER TABLE `factions` ADD INDEX( `image_id`);
ALTER TABLE `factions` ADD `primary_resource_image_id` BIGINT(20) UNSIGNED NULL AFTER `image`;
ALTER TABLE `factions` ADD INDEX( `primary_resource_image_id`);
ALTER TABLE `factions` ADD `secondary_resource_image_id` BIGINT(20) UNSIGNED NULL AFTER `primary_resource_image_id`;
ALTER TABLE `factions` ADD INDEX( `secondary_resource_image_id`);
ALTER TABLE `factions` ADD `energy_image_id` BIGINT(20) UNSIGNED NULL AFTER `secondary_resource_image_id`;
ALTER TABLE `factions` ADD INDEX( `energy_image_id`);
ALTER TABLE `factions` ADD FOREIGN KEY (`image_id`) REFERENCES `images_store`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `factions` ADD FOREIGN KEY (`primary_resource_image_id`) REFERENCES `images_store`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `factions` ADD FOREIGN KEY (`secondary_resource_image_id`) REFERENCES `images_store`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `factions` ADD FOREIGN KEY (`energy_image_id`) REFERENCES `images_store`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `upgrades` ADD `image_id` BIGINT(20) UNSIGNED NULL AFTER `image`;
ALTER TABLE `upgrades` DROP `image`;
ALTER TABLE `upgrades` ADD INDEX(`image_id`);
ALTER TABLE `upgrades` ADD FOREIGN KEY (`image_id`) REFERENCES `images_store`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `units` ADD `image_id` BIGINT(20) UNSIGNED NULL AFTER `image`;
ALTER TABLE `units` DROP `image`;
ALTER TABLE `units` ADD INDEX(`image_id`);
ALTER TABLE `units` ADD FOREIGN KEY (`image_id`) REFERENCES `images_store`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `special_locations` ADD `image_id` BIGINT(20) UNSIGNED NULL AFTER `image`;
ALTER TABLE `special_locations` DROP `image`;
ALTER TABLE `special_locations` ADD INDEX(`image_id`);
ALTER TABLE `special_locations` ADD FOREIGN KEY (`image_id`) REFERENCES `images_store`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `special_locations` CHANGE `improvement_id` `improvement_id` SMALLINT(6) UNSIGNED NULL;
ALTER TABLE `special_locations` DROP FOREIGN KEY `special_locations_ibfk_1`;
ALTER TABLE `special_locations` DROP `planet_id`;
ALTER TABLE `special_locations` CHANGE `galaxy_id` `galaxy_id` SMALLINT(6) UNSIGNED NULL;

ALTER TABLE `mission_reports` DROP `user_aware_date`;
ALTER TABLE `mission_reports` ADD `is_enemy` TINYINT(1) NULL DEFAULT '0' AFTER `report_date`;

CREATE TABLE `websocket_events_information` ( `event_name` VARCHAR(100) NOT NULL , `user_id`  INT(11) UNSIGNED NOT NULL , `last_sent` DATETIME NOT NULL ) ENGINE = InnoDB;
ALTER TABLE `websocket_events_information` ADD PRIMARY KEY( `event_name`, `user_id`);
ALTER TABLE `websocket_events_information` ADD CONSTRAINT `fk_user` FOREIGN KEY (`user_id`) REFERENCES `user_storage`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `admin_users` DROP `password`, DROP `mail`;
ALTER TABLE `admin_users` ADD `can_add_admins` TINYINT UNSIGNED NOT NULL AFTER `enabled`;
ALTER TABLE `admin_users` CHANGE `id` `id` INT(10) UNSIGNED NOT NULL;

CREATE TABLE `requirement_group` ( `id` INT UNSIGNED NOT NULL AUTO_INCREMENT , `object_relation_id` SMALLINT(6) UNSIGNED NOT NULL , PRIMARY KEY (`id`), INDEX (`object_relation_id`)) ENGINE = InnoDB;

ALTER TABLE `objects` CHANGE `description` `description` VARCHAR(18) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;
INSERT INTO `objects` (`description`, `repository`) 
                VALUES ('REQUIREMENT_GROUP', 'com.kevinguanchedarias.owgejava.repository.RequirementGroupRepository'),
                        ('SPEED_IMPACT_GROUP', 'com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository');

ALTER TABLE `speed_impact_groups` ADD `is_fixed` TINYINT NOT NULL AFTER `name`;

CREATE TABLE `object_relation__object_relation` ( `id` INT UNSIGNED NOT NULL , `master_relation_id` SMALLINT(6) UNSIGNED NOT NULL , `slave_relation_id` SMALLINT(6) UNSIGNED NOT NULL ) ENGINE = InnoDB;
ALTER TABLE `object_relation__object_relation` ADD PRIMARY KEY(`id`);
ALTER TABLE `object_relation__object_relation` CHANGE `id` `id` INT(10) UNSIGNED NOT NULL AUTO_INCREMENT;
ALTER TABLE `object_relation__object_relation` ADD INDEX( `master_relation_id`, `slave_relation_id`);
ALTER TABLE `object_relation__object_relation` ADD  CONSTRAINT `fk_object_relation__object_relation_master` FOREIGN KEY (`master_relation_id`) REFERENCES `object_relations`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;
ALTER TABLE `object_relation__object_relation` ADD  CONSTRAINT `fk_object_relation__object_relation_slave` FOREIGN KEY (`slave_relation_id`) REFERENCES `object_relations`(`id`) ON DELETE CASCADE ON UPDATE CASCADE;

ALTER TABLE `object_relations` CHANGE `object_description` `object_description` VARCHAR(18) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;

ALTER TABLE `unit_types` ADD `speed_impact_group_id` SMALLINT(5) UNSIGNED NULL AFTER `can_deploy`;
ALTER TABLE `unit_types` ADD INDEX( `speed_impact_group_id`);
ALTER TABLE `unit_types` ADD CONSTRAINT `unit_types__speed_impact` FOREIGN KEY (`speed_impact_group_id`) REFERENCES `speed_impact_groups`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `units` ADD `speed_impact_group_id` SMALLINT(5) UNSIGNED NULL AFTER `cloned_improvements`;
ALTER TABLE `units` ADD INDEX( `speed_impact_group_id`);
ALTER TABLE `units` ADD CONSTRAINT `units__speed_impact` FOREIGN KEY (`speed_impact_group_id`) REFERENCES `speed_impact_groups`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
