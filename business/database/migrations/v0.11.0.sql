ALTER TABLE `improvements_unit_types` CHANGE `type` `type` ENUM('ATTACK','DEFENSE','SHIELD','AMOUNT','SPEED') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;

CREATE TABLE `faction_spawn_location`
(
    `id`                   SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `faction_id`           SMALLINT UNSIGNED NOT NULL,
    `galaxy_id`            SMALLINT UNSIGNED NOT NULL,
    `sector_range_start`   INT UNSIGNED NULL,
    `sector_range_end`     INT UNSIGNED NULL,
    `quadrant_range_start` INT UNSIGNED NULL,
    `quadrant_range_end`   INT UNSIGNED NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

CREATE TABLE `rules`
(
    `id`             SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type             VARCHAR(50) NOT NULL,
    origin_type      VARCHAR(50) NOT NULL,
    origin_id        SMALLINT    NOT NULL,
    destination_type VARCHAR(50) NOT NULL,
    destination_id   SMALLINT    NOT NULL,
    extra_args       VARCHAR(100)
) CHARACTER SET utf8 COLLATE utf8_general_ci;

ALTER TABLE `obtained_units`
    ADD `is_from_capture` TINYINT NOT NULL AFTER `first_deployment_mission`;

CREATE TABLE `obtained_unit_temporal_information`
(
    `id`         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `duration`   INT UNSIGNED NOT NULL,
    `expiration` TIMESTAMP NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

ALTER TABLE `obtained_units` CHANGE `expiration` `expiration_id` INT UNSIGNED NULL DEFAULT NULL;
ALTER TABLE `requirements` CHANGE `code` `code` VARCHAR (24) NOT NULL;
INSERT INTO requirements (code, description)
VALUES ('UPGRADE_LEVEL_LOWER_THAN', 'Have upgrade lower than')

ALTER TABLE `units`
    ADD `stored_weight` INT UNSIGNED NOT NULL DEFAULT '1' AFTER `is_invisible`, ADD `storage_capacity` INT UNSIGNED NULL AFTER `stored_weight`;
CREATE TABLE `stored_units`
(
    `id`                      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `owner_obtained_unit_id`  INT UNSIGNED NOT NULL,
    `target_obtained_unit_id` INT UNSIGNED NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

