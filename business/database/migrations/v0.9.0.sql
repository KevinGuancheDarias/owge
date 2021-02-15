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
ALTER TABLE `speed_impact_groups` ADD `can_explore` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
ALTER TABLE `speed_impact_groups` ADD `can_gather` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
ALTER TABLE `speed_impact_groups` ADD `can_establish_base` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
ALTER TABLE `speed_impact_groups` ADD `can_attack` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
ALTER TABLE `speed_impact_groups` ADD `can_counterattack` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
ALTER TABLE `speed_impact_groups` ADD `can_conquest` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
ALTER TABLE `speed_impact_groups` ADD `can_deploy` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY'

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
ALTER TABLE `units` ADD `speed` DOUBLE NULL AFTER `is_unique`;

CREATE TABLE `attack_rules` ( `id` SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT , `name` VARCHAR(100) NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;
CREATE TABLE `attack_rule_entries` ( `id` SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT , `attack_rule_id` SMALLINT UNSIGNED NOT NULL , `target` ENUM('UNIT','UNIT_TYPE') NOT NULL , `reference_id` SMALLINT UNSIGNED NOT NULL , `can_attack` TINYINT NOT NULL , PRIMARY KEY (`id`), INDEX (`attack_rule_id`)) ENGINE = InnoDB;
ALTER TABLE `attack_rule_entries` ADD CONSTRAINT `fk_attack_rule` FOREIGN KEY (`attack_rule_id`) REFERENCES `attack_rules`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `unit_types` ADD `attack_rule_id` SMALLINT UNSIGNED NULL AFTER `name`;
ALTER TABLE `units` ADD `attack_rule_id` SMALLINT UNSIGNED NULL AFTER `name`;
ALTER TABLE `unit_types` ADD CONSTRAINT `unit_types__attack_rules` FOREIGN KEY (`attack_rule_id`) REFERENCES `attack_rules`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
ALTER TABLE `units` ADD CONSTRAINT `units__attack_rules` FOREIGN KEY (`attack_rule_id`) REFERENCES `attack_rules`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `unit_types` ADD `share_max_count` SMALLINT(6) UNSIGNED NULL COMMENT 'The count of this unit type will apply to referenced' AFTER `max_count`;
ALTER TABLE `unit_types` ADD CONSTRAINT `unit_types_share_count` FOREIGN KEY (`share_max_count`) REFERENCES `unit_types`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `unit_types` CHANGE `parent_type` `parent_type` SMALLINT(6) UNSIGNED NULL DEFAULT NULL;
ALTER TABLE `unit_types` ADD CONSTRAINT `unit_types_parent_type` FOREIGN KEY (`parent_type`) REFERENCES `unit_types`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `units` CHANGE `improvement_id` `improvement_id` SMALLINT(6) UNSIGNED NULL;
ALTER TABLE `units` ADD `display_in_requirements` BOOLEAN NULL DEFAULT FALSE AFTER `name`;
ALTER TABLE `unit_types` ADD `has_to_inherit_improvements` BOOLEAN NOT NULL DEFAULT FALSE COMMENT 'If true applied benefits to parent unit type will also apply to this' AFTER `speed_impact_group_id`;

ALTER TABLE `units` ADD `can_fast_explore` BOOLEAN NOT NULL DEFAULT FALSE AFTER `is_unique`;

ALTER TABLE `galaxies` ADD `num_planets` INT UNSIGNED NOT NULL DEFAULT '20' AFTER `quadrants`;

CREATE TABLE `tutorial_sections` ( `id` SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT , `name` VARCHAR(100) NOT NULL , `description` TEXT NULL , `frontend_router_path` VARCHAR(150) NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;

CREATE TABLE `tutorial_sections_available_html_symbols` ( `id` INT UNSIGNED NOT NULL AUTO_INCREMENT , `name` VARCHAR(50) NOT NULL , `identifier` VARCHAR(150) NOT NULL COMMENT 'The identifier to use in the Frontend ngDirective' , `tutorial_section_id` SMALLINT UNSIGNED NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;

ALTER TABLE `tutorial_sections_available_html_symbols` ADD CONSTRAINT `fk_section_id` FOREIGN KEY (`tutorial_section_id`) REFERENCES `tutorial_sections`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE `tutorial_sections_entries` ( `id` INT UNSIGNED NOT NULL AUTO_INCREMENT , `order_num` SMALLINT UNSIGNED NULL , `section_available_html_symbol_id` INT UNSIGNED NOT NULL , `event` ENUM('CLICK','ANY_KEY_OR_CLICK') NOT NULL , `text_id` INT UNSIGNED NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;

ALTER TABLE `tutorial_sections_entries` ADD CONSTRAINT `fk_tse_symbol_id` FOREIGN KEY (`section_available_html_symbol_id`) REFERENCES `tutorial_sections_available_html_symbols`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE `translatables` ( `id` INT UNSIGNED NOT NULL AUTO_INCREMENT , `name` VARCHAR(100) NOT NULL , `default_lang_code` CHAR(2) NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;

CREATE TABLE `translatables_translations` ( `id` INT UNSIGNED NOT NULL AUTO_INCREMENT , `translatable_id` INT UNSIGNED NOT NULL , `lang_code` CHAR(2) NOT NULL , `value` LONGTEXT NOT NULL , PRIMARY KEY (`id`), INDEX (`translatable_id`)) ENGINE = InnoDB;

ALTER TABLE `translatables_translations` ADD CONSTRAINT `fk_translatable_id` FOREIGN KEY (`translatable_id`) REFERENCES `translatables`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

CREATE TABLE `visited_tutorial_entries` ( `id` BIGINT NOT NULL AUTO_INCREMENT , `user_id` INT(11) UNSIGNED NOT NULL , `entry_id` INT UNSIGNED NOT NULL , PRIMARY KEY (`id`), INDEX (`user_id`)) ENGINE = InnoDB;

ALTER TABLE `visited_tutorial_entries` ADD CONSTRAINT `fk_vts_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_storage`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `user_storage` ADD `has_skipped_tutorial` BOOLEAN NOT NULL AFTER `max_energy`;

ALTER TABLE `configuration` CHANGE `name` `name` VARCHAR(50) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;

-- v0.9.5
ALTER TABLE `user_storage` ADD `can_alter_twitch_state` BOOLEAN NOT NULL AFTER `points`;

-- v0.9.6
ALTER TABLE `missions` ADD `starting_date` DATETIME NOT NULL AFTER `required_time`;
UPDATE missions SET starting_date = '1970-01-01 00:00:00';

-- v0.9.16
CREATE TABLE `system_messages` ( `id` SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT , `content` TEXT NOT NULL , `creation_date` DATETIME NOT NULL , PRIMARY KEY (`id`)) ENGINE = InnoDB;
CREATE TABLE `user_read_system_messages` ( `id` INT UNSIGNED NOT NULL AUTO_INCREMENT , `user_id` INT(11) UNSIGNED NOT NULL , `message_id` SMALLINT(5) UNSIGNED NOT NULL , PRIMARY KEY (`id`), INDEX (`user_id`), INDEX (`message_id`)) ENGINE = InnoDB;
ALTER TABLE `user_read_system_messages` ADD CONSTRAINT `fk_ursm_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_storage`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT; ALTER TABLE `user_read_system_messages` ADD CONSTRAINT `fk_ursm_message_id` FOREIGN KEY (`message_id`) REFERENCES `system_messages`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;