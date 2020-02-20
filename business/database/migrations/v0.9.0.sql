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