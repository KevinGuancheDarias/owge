ALTER TABLE `unit_types` DROP `image`;
ALTER TABLE `unit_types` ADD `image_id` BIGINT(20) UNSIGNED NULL AFTER `max_count`;
ALTER TABLE `unit_types` ADD INDEX( `image_id`);
ALTER TABLE `unit_types` ADD FOREIGN KEY (`image_id`) REFERENCES `images_store`(`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;
