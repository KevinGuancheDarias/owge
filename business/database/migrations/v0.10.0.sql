ALTER TABLE `user_storage` DROP `max_energy`;

-- Remove unsigned
ALTER TABLE `improvements` CHANGE `more_upgrade_research_speed` `more_upgrade_research_speed` FLOAT NULL DEFAULT NULL, CHANGE `more_unit_build_speed` `more_unit_build_speed` FLOAT NULL DEFAULT NULL; 

ALTER TABLE `units` ADD `bypass_shield` BOOLEAN NOT NULL AFTER `speed_impact_group_id`;

ALTER TABLE `units` ADD `is_invisible` BOOLEAN NOT NULL AFTER `bypass_shield`;

ALTER TABLE `factions` ADD `custom_primary_gather_percentage` FLOAT UNSIGNED NULL AFTER `cloned_improvements`, ADD `custom_secondary_gather_percentage` FLOAT UNSIGNED NULL AFTER `custom_primary_gather_percentage`;
