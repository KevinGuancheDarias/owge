ALTER TABLE `improvements_unit_types`
    CHANGE `type` `type` ENUM ('ATTACK','DEFENSE','SHIELD','AMOUNT','SPEED') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL;

CREATE TABLE `faction_spawn_location`
(
    `id`                   SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT,
    `faction_id`           SMALLINT UNSIGNED NOT NULL,
    `galaxy_id`            SMALLINT UNSIGNED NOT NULL,
    `sector_range_start`   INT UNSIGNED      NULL,
    `sector_range_end`     INT UNSIGNED      NULL,
    `quadrant_range_start` INT UNSIGNED      NULL,
    `quadrant_range_end`   INT UNSIGNED      NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

CREATE TABLE `rules`
(
    `id`             SMALLINT UNSIGNED NOT NULL AUTO_INCREMENT PRIMARY KEY,
    type             VARCHAR(50)       NOT NULL,
    origin_type      VARCHAR(50)       NOT NULL,
    origin_id        SMALLINT          NOT NULL,
    destination_type VARCHAR(50)       NOT NULL,
    destination_id   SMALLINT          NOT NULL,
    extra_args       VARCHAR(100)
) CHARACTER SET utf8
  COLLATE utf8_general_ci;

ALTER TABLE `obtained_units`
    ADD `is_from_capture` TINYINT NOT NULL AFTER `first_deployment_mission`;

CREATE TABLE `obtained_unit_temporal_information`
(
    `id`         INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `duration`   INT UNSIGNED NOT NULL,
    `expiration` TIMESTAMP    NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

ALTER TABLE `obtained_units`
    CHANGE `expiration` `expiration_id` INT UNSIGNED NULL DEFAULT NULL;
ALTER TABLE `requirements`
    CHANGE `code` `code` VARCHAR(24) NOT NULL;
INSERT INTO requirements (code, description)
VALUES ('UPGRADE_LEVEL_LOWER_THAN', 'Have upgrade lower than');

ALTER TABLE `units`
    ADD `stored_weight`    INT UNSIGNED NOT NULL DEFAULT '1' AFTER `is_invisible`,
    ADD `storage_capacity` INT UNSIGNED NULL AFTER `stored_weight`;

ALTER TABLE `obtained_units`
    ADD `owner_unit_id` BIGINT UNSIGNED NULL AFTER `expiration_id`,
    ADD INDEX (`owner_unit_id`);

CREATE TABLE `stored_units`
(
    `id`                      INT UNSIGNED NOT NULL AUTO_INCREMENT,
    `owner_obtained_unit_id`  INT UNSIGNED NOT NULL,
    `target_obtained_unit_id` INT UNSIGNED NOT NULL,
    PRIMARY KEY (`id`)
) ENGINE = InnoDB;

ALTER TABLE `audit`
    ADD `ipv4` CHAR(15) NULL AFTER `creation_date`,
    ADD `ipv6` CHAR(39) NULL AFTER `ipv4`,
    ADD INDEX (`ipv4`),
    ADD INDEX (`ipv6`);

UPDATE `audit`
SET ipv4 = ip;

ALTER TABLE audit
    DROP COLUMN ip;

CREATE TABLE scheduled_tasks
(
    task_name            varchar(40)  not null,
    task_instance        varchar(40)  not null,
    task_data            blob,
    execution_time       timestamp(6) not null,
    picked               BOOLEAN      not null,
    picked_by            varchar(50),
    last_success         timestamp(6) null,
    last_failure         timestamp(6) null,
    consecutive_failures INT,
    last_heartbeat       timestamp(6) null,
    version              BIGINT       not null,
    PRIMARY KEY (task_name, task_instance),
    INDEX execution_time_idx (execution_time),
    INDEX last_heartbeat_idx (last_heartbeat)
);

ALTER TABLE `obtained_unit_temporal_information`
    ADD `relation_id` SMALLINT UNSIGNED NOT NULL AFTER `expiration`,
    ADD INDEX (`relation_id`);

ALTER TABLE `tor_ip_data`
    CHANGE `ip` `ip` VARCHAR(39) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL;

CREATE TABLE `suspicions`
(
    `id`         BIGINT UNSIGNED AUTO_INCREMENT         NOT NULL,
    `source`     ENUM ('BROWSER','IP','BROWSER_AND_IP') NOT NULL,
    `user_id`    INT UNSIGNED                           NOT NULL,
    `audit_id`   BIGINT UNSIGNED                        NOT NULL,
    `created_at` DATETIME                               NOT NULL,
    PRIMARY KEY (`id`),
    INDEX (`user_id`, `audit_id`)
) ENGINE = InnoDB;

ALTER TABLE upgrades
    ADD `order_number` smallint unsigned DEFAULT NULL COMMENT 'The upgrade order' AFTER id;

CREATE TABLE track_browser
(
    id           BIGINT UNSIGNED AUTO_INCREMENT NOT NULL,
    method       VARCHAR(8)                     NOT NULL,
    json_content TEXT                           NOT NULL,
    created_at   DATETIME                       NOT NULL,
    PRIMARY KEY (id)
) ENGINE = InnoDB

UPDATE units SET attack = 0 WHERE attack is null;
ALTER TABLE `units` CHANGE `attack` `attack` SMALLINT UNSIGNED NOT NULL DEFAULT '0';