ALTER TABLE `user_storage` DROP `max_energy`;

-- Remove unsigned
ALTER TABLE `improvements` CHANGE `more_upgrade_research_speed` `more_upgrade_research_speed` FLOAT NULL DEFAULT NULL, CHANGE `more_unit_build_speed` `more_unit_build_speed` FLOAT NULL DEFAULT NULL; 

ALTER TABLE `units` ADD `bypass_shield` BOOLEAN NOT NULL AFTER `speed_impact_group_id`;

ALTER TABLE `units` ADD `is_invisible` BOOLEAN NOT NULL AFTER `bypass_shield`;

ALTER TABLE `factions` ADD `custom_primary_gather_percentage` FLOAT UNSIGNED NULL AFTER `cloned_improvements`, ADD `custom_secondary_gather_percentage` FLOAT UNSIGNED NULL AFTER `custom_primary_gather_percentage`;

CREATE TABLE `factions_unit_types` (
  `id` int(10) unsigned NOT NULL AUTO_INCREMENT,
  `faction_id` smallint(5) unsigned NOT NULL,
  `unit_type_id` smallint(6) unsigned NOT NULL,
  `max_count` int(10) unsigned DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fk_fut_faction_id` (`faction_id`),
  KEY `fk_fut_unit_type_id` (`unit_type_id`),
  CONSTRAINT `fk_fut_faction_id` FOREIGN KEY (`faction_id`) REFERENCES `factions` (`id`),
  CONSTRAINT `fk_fut_unit_type_id` FOREIGN KEY (`unit_type_id`) REFERENCES `unit_types` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

CREATE TABLE `interceptable_speed_group` ( 
  `id` SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT ,
  `unit_id` SMALLINT(6) UNSIGNED NOT NULL ,
  `speed_impact_group_id` SMALLINT UNSIGNED NOT NULL ,
  PRIMARY KEY (`id`),
  INDEX (`unit_id`), 
  INDEX (`speed_impact_group_id`),
  CONSTRAINT `fk_isg_unit_id` FOREIGN KEY (`unit_id`) REFERENCES `units`(`id`), 
  CONSTRAINT `fk_isg_speed_impact_group_id` FOREIGN KEY (`speed_impact_group_id`) REFERENCES `speed_impact_groups`(`id`)
) ENGINE = InnoDB;

ALTER TABLE `missions` ADD `invisible` TINYINT NOT NULL AFTER `resolved`;
ALTER TABLE `speed_impact_groups` ADD `image_id` BIGINT UNSIGNED NULL AFTER `can_deploy`;
CREATE TABLE `audit` ( 
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT, 
  `action` ENUM('SUBSCRIBE_TO_WORLD','LOGIN','REGISTER_MISSION','ADD_PLANET_TO_LIST','BROWSE_COORDINATES', 'USER_INTERACTION', 'JOIN_ALLIANCE', 'ACCEPT_JOIN_ALLIANCE', 'ATTACK_INTERACTION') NOT NULL , 
  `action_detail` VARCHAR(100) NULL , 
  `user_id` INT UNSIGNED NOT NULL , 
  `related_user_id` INT UNSIGNED NULL , 
  `ip` CHAR(15) NULL , 
  `user_agent` VARCHAR(255) NULL , 
  `cookie` VARCHAR(50) NULL , 
  `is_tor` BOOLEAN NOT NULL,
  `creation_date` DATETIME NOT NULL,
  PRIMARY KEY (`id`),
  CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) REFERENCES `user_storage`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  CONSTRAINT `fk_audit_related_user` FOREIGN KEY (`user_id`) REFERENCES `user_storage`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT
) ENGINE = InnoDB;
ALTER TABLE `user_storage` ADD `last_multi_account_check` DATETIME NULL AFTER `can_alter_twitch_state`, ADD `multi_account_score` FLOAT NULL, `banned` tinyint NOT NULL AFTER `last_multi_account_check`;

CREATE TABLE `tor_ip_data` ( 
  `ip` CHAR(15) NOT NULL , 
  `last_checked_date` DATETIME NOT NULL , 
  `is_tor` BOOLEAN NOT NULL , 
  PRIMARY KEY (`ip`)
) ENGINE = MyIsam;


CREATE TABLE `critical_attack` ( 
  `id` SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT , 
  `name` VARCHAR(100) NOT NULL , 
  PRIMARY KEY (`id`)
) ENGINE = InnoDB;

CREATE TABLE `critical_attack_entries` ( 
  `id` INT UNSIGNED NOT NULL AUTO_INCREMENT , 
  `critical_attack_id` SMALLINT UNSIGNED NOT NULL , 
  `target` ENUM('UNIT','UNIT_TYPE') NOT NULL , 
  `reference_id` INT UNSIGNED NOT NULL , 
  `value` FLOAT NOT NULL , 
  PRIMARY KEY (`id`)
) ENGINE = InnoDB;

ALTER TABLE `unit_types` ADD `critical_attack_id` SMALLINT UNSIGNED NULL AFTER `speed_impact_group_id`;
ALTER TABLE `unit_types` ADD CONSTRAINT `unit_types_critical_attack` FOREIGN KEY (`critical_attack_id`) REFERENCES `critical_attack`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

ALTER TABLE `units` ADD `critical_attack_id` SMALLINT UNSIGNED NULL AFTER `speed_impact_group_id`;
ALTER TABLE `units` ADD CONSTRAINT `units_critical_attack` FOREIGN KEY (`critical_attack_id`) REFERENCES `critical_attack`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;