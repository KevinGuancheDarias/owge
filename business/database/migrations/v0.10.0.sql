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
