-- phpMyAdmin SQL Dump
-- version 5.1.0
-- https://www.phpmyadmin.net/
--
-- Hôte : db:3306
-- Généré le : mer. 05 juil. 2023 à 14:40
-- Version du serveur :  8.0.23
-- Version de PHP : 7.4.15

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données : `owge`
--

-- --------------------------------------------------------

--
-- Structure de la table `active_time_specials`
--

CREATE TABLE `active_time_specials` (
  `id` bigint UNSIGNED NOT NULL,
  `user_id` int NOT NULL,
  `time_special_id` smallint UNSIGNED NOT NULL,
  `state` enum('ACTIVE','RECHARGE') NOT NULL COMMENT 'possible states for the time special',
  `activation_date` datetime NOT NULL,
  `expiring_date` datetime NOT NULL,
  `ready_date` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `admin_users`
--

CREATE TABLE `admin_users` (
  `id` int UNSIGNED NOT NULL,
  `username` varchar(20) NOT NULL,
  `enabled` tinyint(1) NOT NULL,
  `can_add_admins` tinyint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `alliances`
--

CREATE TABLE `alliances` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text,
  `image` char(36) DEFAULT NULL,
  `owner_id` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `alliance_join_request`
--

CREATE TABLE `alliance_join_request` (
  `id` int UNSIGNED NOT NULL,
  `alliance_id` smallint UNSIGNED NOT NULL,
  `user_id` int NOT NULL,
  `request_date` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `attack_rules`
--

CREATE TABLE `attack_rules` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `attack_rule_entries`
--

CREATE TABLE `attack_rule_entries` (
  `id` smallint UNSIGNED NOT NULL,
  `attack_rule_id` smallint UNSIGNED NOT NULL,
  `target` enum('UNIT','UNIT_TYPE') NOT NULL,
  `reference_id` smallint UNSIGNED NOT NULL,
  `can_attack` tinyint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `audit`
--

CREATE TABLE `audit` (
  `id` bigint UNSIGNED NOT NULL,
  `action` enum('SUBSCRIBE_TO_WORLD','LOGIN','REGISTER_MISSION','ADD_PLANET_TO_LIST','BROWSE_COORDINATES','USER_INTERACTION','JOIN_ALLIANCE','ACCEPT_JOIN_ALLIANCE','ATTACK_INTERACTION') CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `action_detail` varchar(100) DEFAULT NULL,
  `user_id` int NOT NULL,
  `related_user_id` int DEFAULT NULL,
  `user_agent` varchar(255) DEFAULT NULL,
  `cookie` varchar(50) DEFAULT NULL,
  `is_tor` tinyint(1) NOT NULL,
  `creation_date` datetime NOT NULL,
  `ipv4` char(15) DEFAULT NULL,
  `ipv6` char(39) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `carpetas`
--

CREATE TABLE `carpetas` (
  `cd` int NOT NULL,
  `Nombre` char(15) CHARACTER SET latin1 COLLATE latin1_spanish_ci NOT NULL,
  `usercd` int NOT NULL,
  `Borrable` tinyint(1) NOT NULL,
  `Movible` tinyint(1) NOT NULL,
  `Mensajes` int NOT NULL COMMENT 'Número de mensajes que contiene'
) ENGINE=MyISAM DEFAULT CHARSET=latin1 COLLATE=latin1_spanish_ci;

-- --------------------------------------------------------

--
-- Structure de la table `configuration`
--

CREATE TABLE `configuration` (
  `name` varchar(50) NOT NULL,
  `display_name` varchar(400) DEFAULT NULL,
  `value` varchar(200) NOT NULL,
  `privileged` tinyint NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `critical_attack`
--

CREATE TABLE `critical_attack` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(100) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `critical_attack_entries`
--

CREATE TABLE `critical_attack_entries` (
  `id` int UNSIGNED NOT NULL,
  `critical_attack_id` smallint UNSIGNED NOT NULL,
  `target` enum('UNIT','UNIT_TYPE') NOT NULL,
  `reference_id` int UNSIGNED NOT NULL,
  `value` float NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `explored_planets`
--

CREATE TABLE `explored_planets` (
  `id` bigint NOT NULL,
  `user` int NOT NULL,
  `planet` bigint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `factions`
--

CREATE TABLE `factions` (
  `id` smallint UNSIGNED NOT NULL,
  `hidden` tinyint DEFAULT NULL,
  `name` varchar(30) NOT NULL,
  `image_id` bigint UNSIGNED DEFAULT NULL,
  `primary_resource_image_id` bigint UNSIGNED DEFAULT NULL,
  `secondary_resource_image_id` bigint UNSIGNED DEFAULT NULL,
  `energy_image_id` bigint UNSIGNED DEFAULT NULL,
  `description` text,
  `primary_resource_name` varchar(20) NOT NULL,
  `primary_resource_image` varchar(50) DEFAULT NULL,
  `secondary_resource_name` varchar(20) NOT NULL,
  `secondary_resource_image` varchar(50) DEFAULT NULL,
  `energy_name` varchar(20) NOT NULL,
  `energy_image` varchar(50) DEFAULT NULL,
  `initial_primary_resource` mediumint UNSIGNED NOT NULL,
  `initial_secondary_resource` mediumint UNSIGNED NOT NULL,
  `initial_energy` mediumint UNSIGNED NOT NULL,
  `primary_resource_production` float NOT NULL COMMENT 'Per minut',
  `secondary_resource_production` float NOT NULL COMMENT 'Per minut',
  `max_planets` tinyint UNSIGNED NOT NULL COMMENT 'Max number of planets',
  `improvement_id` smallint UNSIGNED DEFAULT NULL,
  `cloned_improvements` tinyint NOT NULL,
  `custom_primary_gather_percentage` float UNSIGNED DEFAULT NULL,
  `custom_secondary_gather_percentage` float UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `factions_unit_types`
--

CREATE TABLE `factions_unit_types` (
  `id` int UNSIGNED NOT NULL,
  `faction_id` smallint UNSIGNED NOT NULL,
  `unit_type_id` smallint UNSIGNED NOT NULL,
  `max_count` int UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `faction_spawn_location`
--

CREATE TABLE `faction_spawn_location` (
  `id` smallint UNSIGNED NOT NULL,
  `faction_id` smallint UNSIGNED NOT NULL,
  `galaxy_id` smallint UNSIGNED NOT NULL,
  `sector_range_start` int UNSIGNED DEFAULT NULL,
  `sector_range_end` int UNSIGNED DEFAULT NULL,
  `quadrant_range_start` int UNSIGNED DEFAULT NULL,
  `quadrant_range_end` int UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `galaxies`
--

CREATE TABLE `galaxies` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(30) CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `sectors` int UNSIGNED NOT NULL,
  `quadrants` int UNSIGNED NOT NULL,
  `num_planets` int UNSIGNED NOT NULL DEFAULT '20',
  `order_number` smallint UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_spanish_ci;

-- --------------------------------------------------------

--
-- Structure de la table `images_store`
--

CREATE TABLE `images_store` (
  `id` bigint UNSIGNED NOT NULL,
  `checksum` char(32) CHARACTER SET ascii COLLATE ascii_general_ci NOT NULL,
  `filename` varchar(500) NOT NULL,
  `display_name` varchar(50) DEFAULT NULL,
  `description` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `improvements`
--

CREATE TABLE `improvements` (
  `id` smallint UNSIGNED NOT NULL,
  `more_soldiers_production` smallint DEFAULT NULL,
  `more_primary_resource_production` smallint DEFAULT NULL,
  `more_secondary_resource_production` smallint DEFAULT NULL,
  `more_energy_production` smallint DEFAULT NULL,
  `more_charge_capacity` smallint DEFAULT NULL,
  `more_missions_value` tinyint DEFAULT NULL,
  `more_upgrade_research_speed` float DEFAULT NULL,
  `more_unit_build_speed` float DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `improvements_unit_types`
--

CREATE TABLE `improvements_unit_types` (
  `id` smallint UNSIGNED NOT NULL,
  `improvement_id` smallint UNSIGNED NOT NULL,
  `type` enum('ATTACK','DEFENSE','SHIELD','AMOUNT','SPEED') CHARACTER SET utf8 COLLATE utf8_general_ci NOT NULL,
  `unit_type_id` smallint UNSIGNED NOT NULL,
  `value` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='upgrades_unit_types';

-- --------------------------------------------------------

--
-- Structure de la table `interceptable_speed_group`
--

CREATE TABLE `interceptable_speed_group` (
  `id` smallint UNSIGNED NOT NULL,
  `unit_id` smallint UNSIGNED NOT NULL,
  `speed_impact_group_id` smallint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `mensajes`
--

CREATE TABLE `mensajes` (
  `cd` int NOT NULL,
  `TipoMision` int NOT NULL COMMENT '0 para ninguno,1 para Exploración, 2 para recolección, 3 para ataque',
  `Tiempo` int NOT NULL,
  `Titulo` char(255) NOT NULL,
  `Contenido` text NOT NULL,
  `Destinatarios` text NOT NULL COMMENT 'En este caso se empieza por ,',
  `Destino` int NOT NULL,
  `Carpetacd` int NOT NULL,
  `Leido` tinyint(1) NOT NULL,
  `Notificado` int NOT NULL DEFAULT '0',
  `Enviador` int NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `missions`
--

CREATE TABLE `missions` (
  `id` bigint UNSIGNED NOT NULL,
  `user_id` int DEFAULT NULL COMMENT 'If null is a core mission!',
  `type` smallint UNSIGNED NOT NULL,
  `termination_date` datetime DEFAULT NULL,
  `required_time` double DEFAULT NULL,
  `starting_date` datetime NOT NULL,
  `primary_resource` double DEFAULT NULL,
  `secondary_resource` double DEFAULT NULL,
  `required_energy` double DEFAULT NULL,
  `source_planet` bigint DEFAULT NULL,
  `target_planet` bigint DEFAULT NULL,
  `related_mission` bigint UNSIGNED DEFAULT NULL,
  `report_id` bigint UNSIGNED DEFAULT NULL,
  `attemps` tinyint UNSIGNED NOT NULL DEFAULT '1',
  `resolved` tinyint NOT NULL,
  `invisible` tinyint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `mission_information`
--

CREATE TABLE `mission_information` (
  `id` bigint UNSIGNED NOT NULL,
  `mission_id` bigint UNSIGNED NOT NULL,
  `relation_id` smallint UNSIGNED DEFAULT NULL COMMENT 'Represents the relation id if applicable',
  `value` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Some missions may require having some information about the mission itself.  for example the level up upgrade mission needs the relation id';

-- --------------------------------------------------------

--
-- Structure de la table `mission_reports`
--

CREATE TABLE `mission_reports` (
  `id` bigint UNSIGNED NOT NULL,
  `json_body` mediumtext NOT NULL,
  `user_id` int NOT NULL,
  `report_date` datetime DEFAULT NULL,
  `is_enemy` tinyint(1) DEFAULT '0',
  `user_read_date` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `mission_types`
--

CREATE TABLE `mission_types` (
  `id` smallint UNSIGNED NOT NULL,
  `code` varchar(50) NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `is_shared` tinyint NOT NULL COMMENT 'If true will use the shared handling thread, instead of a dedicated one'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `objects`
--

CREATE TABLE `objects` (
  `description` varchar(18) NOT NULL,
  `repository` varchar(100) NOT NULL COMMENT 'Spring Data Repository related to this object'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Used to match objects with requirements';

-- --------------------------------------------------------

--
-- Structure de la table `object_relations`
--

CREATE TABLE `object_relations` (
  `id` smallint UNSIGNED NOT NULL,
  `object_description` varchar(18) NOT NULL,
  `reference_id` smallint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Has the mapping between objects table and the referenced tb';

-- --------------------------------------------------------

--
-- Structure de la table `object_relation__object_relation`
--

CREATE TABLE `object_relation__object_relation` (
  `id` int UNSIGNED NOT NULL,
  `master_relation_id` smallint UNSIGNED NOT NULL,
  `slave_relation_id` smallint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `obtained_units`
--

CREATE TABLE `obtained_units` (
  `id` bigint UNSIGNED NOT NULL,
  `user_id` int NOT NULL,
  `unit_id` smallint UNSIGNED NOT NULL,
  `count` bigint UNSIGNED NOT NULL,
  `source_planet` bigint UNSIGNED DEFAULT NULL,
  `target_planet` bigint UNSIGNED DEFAULT NULL,
  `mission_id` bigint UNSIGNED DEFAULT NULL,
  `first_deployment_mission` bigint UNSIGNED DEFAULT NULL COMMENT 'Has the id of the first deployment executed mission',
  `is_from_capture` tinyint NOT NULL,
  `expiration_id` int UNSIGNED DEFAULT NULL,
  `owner_unit_id` bigint UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `obtained_unit_temporal_information`
--

CREATE TABLE `obtained_unit_temporal_information` (
  `id` int UNSIGNED NOT NULL,
  `duration` int UNSIGNED NOT NULL,
  `expiration` timestamp NOT NULL,
  `relation_id` smallint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `obtained_upgrades`
--

CREATE TABLE `obtained_upgrades` (
  `id` int UNSIGNED NOT NULL,
  `user_id` int NOT NULL,
  `upgrade_id` smallint UNSIGNED NOT NULL,
  `level` smallint NOT NULL,
  `available` tinyint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `planets`
--

CREATE TABLE `planets` (
  `id` bigint UNSIGNED NOT NULL,
  `name` varchar(20) NOT NULL,
  `galaxy_id` smallint UNSIGNED NOT NULL,
  `sector` int UNSIGNED NOT NULL,
  `quadrant` int UNSIGNED NOT NULL,
  `planet_number` smallint UNSIGNED NOT NULL,
  `owner` int DEFAULT NULL,
  `richness` smallint UNSIGNED NOT NULL,
  `home` tinyint DEFAULT '0',
  `special_location_id` smallint UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `planet_list`
--

CREATE TABLE `planet_list` (
  `user_id` int NOT NULL,
  `planet_id` bigint UNSIGNED NOT NULL,
  `name` varchar(150) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_blob_triggers`
--

CREATE TABLE `qrtz_blob_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `BLOB_DATA` blob
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_calendars`
--

CREATE TABLE `qrtz_calendars` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `CALENDAR_NAME` varchar(200) NOT NULL,
  `CALENDAR` blob NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_cron_triggers`
--

CREATE TABLE `qrtz_cron_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `CRON_EXPRESSION` varchar(120) NOT NULL,
  `TIME_ZONE_ID` varchar(80) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_fired_triggers`
--

CREATE TABLE `qrtz_fired_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `ENTRY_ID` varchar(95) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `INSTANCE_NAME` varchar(200) NOT NULL,
  `FIRED_TIME` bigint NOT NULL,
  `SCHED_TIME` bigint NOT NULL,
  `PRIORITY` int NOT NULL,
  `STATE` varchar(16) NOT NULL,
  `JOB_NAME` varchar(200) DEFAULT NULL,
  `JOB_GROUP` varchar(200) DEFAULT NULL,
  `IS_NONCONCURRENT` varchar(1) DEFAULT NULL,
  `REQUESTS_RECOVERY` varchar(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_job_details`
--

CREATE TABLE `qrtz_job_details` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `JOB_CLASS_NAME` varchar(250) NOT NULL,
  `IS_DURABLE` varchar(1) NOT NULL,
  `IS_NONCONCURRENT` varchar(1) NOT NULL,
  `IS_UPDATE_DATA` varchar(1) NOT NULL,
  `REQUESTS_RECOVERY` varchar(1) NOT NULL,
  `JOB_DATA` blob
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_locks`
--

CREATE TABLE `qrtz_locks` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `LOCK_NAME` varchar(40) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_paused_trigger_grps`
--

CREATE TABLE `qrtz_paused_trigger_grps` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_scheduler_state`
--

CREATE TABLE `qrtz_scheduler_state` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `INSTANCE_NAME` varchar(200) NOT NULL,
  `LAST_CHECKIN_TIME` bigint NOT NULL,
  `CHECKIN_INTERVAL` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_simple_triggers`
--

CREATE TABLE `qrtz_simple_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `REPEAT_COUNT` bigint NOT NULL,
  `REPEAT_INTERVAL` bigint NOT NULL,
  `TIMES_TRIGGERED` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_simprop_triggers`
--

CREATE TABLE `qrtz_simprop_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `STR_PROP_1` varchar(512) DEFAULT NULL,
  `STR_PROP_2` varchar(512) DEFAULT NULL,
  `STR_PROP_3` varchar(512) DEFAULT NULL,
  `INT_PROP_1` int DEFAULT NULL,
  `INT_PROP_2` int DEFAULT NULL,
  `LONG_PROP_1` bigint DEFAULT NULL,
  `LONG_PROP_2` bigint DEFAULT NULL,
  `DEC_PROP_1` decimal(13,4) DEFAULT NULL,
  `DEC_PROP_2` decimal(13,4) DEFAULT NULL,
  `BOOL_PROP_1` varchar(1) DEFAULT NULL,
  `BOOL_PROP_2` varchar(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_triggers`
--

CREATE TABLE `qrtz_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `JOB_NAME` varchar(200) NOT NULL,
  `JOB_GROUP` varchar(200) NOT NULL,
  `DESCRIPTION` varchar(250) DEFAULT NULL,
  `NEXT_FIRE_TIME` bigint DEFAULT NULL,
  `PREV_FIRE_TIME` bigint DEFAULT NULL,
  `PRIORITY` int DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint NOT NULL,
  `END_TIME` bigint DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) DEFAULT NULL,
  `MISFIRE_INSTR` smallint DEFAULT NULL,
  `JOB_DATA` blob
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `ranking`
--

CREATE TABLE `ranking` (
  `PosicionTotal` int NOT NULL,
  `PosicionMejoras` int NOT NULL,
  `PosicionTropas` int NOT NULL,
  `PosicionNaves` int NOT NULL,
  `PosicionDefensas` int NOT NULL,
  `usercd` int NOT NULL,
  `PuntosTotales` int NOT NULL,
  `PuntosMejoras` int NOT NULL,
  `PuntosTropas` int NOT NULL,
  `PuntosNaves` int NOT NULL,
  `PuntosDefensas` int NOT NULL,
  `Imagen320x240` char(255) NOT NULL,
  `Imagen640x240` char(255) NOT NULL
) ENGINE=MEMORY DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Structure de la table `requirements`
--

CREATE TABLE `requirements` (
  `id` smallint NOT NULL,
  `code` varchar(24) NOT NULL,
  `description` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `requirements_information`
--

CREATE TABLE `requirements_information` (
  `id` smallint NOT NULL,
  `relation_id` smallint UNSIGNED NOT NULL,
  `requirement_id` smallint NOT NULL,
  `second_value` int DEFAULT NULL,
  `third_value` int DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Stores which object has which requirement';

-- --------------------------------------------------------

--
-- Structure de la table `requirement_group`
--

CREATE TABLE `requirement_group` (
  `id` int UNSIGNED NOT NULL,
  `name` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `requisitosespecialesderaza`
--

CREATE TABLE `requisitosespecialesderaza` (
  `cdEspecial` int NOT NULL,
  `mcd` int NOT NULL,
  `Nivel` int NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Structure de la table `rules`
--

CREATE TABLE `rules` (
  `id` smallint UNSIGNED NOT NULL,
  `type` varchar(50) NOT NULL,
  `origin_type` varchar(50) NOT NULL,
  `origin_id` smallint NOT NULL,
  `destination_type` varchar(50) NOT NULL,
  `destination_id` smallint NOT NULL,
  `extra_args` varchar(100) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `scheduled_tasks`
--

CREATE TABLE `scheduled_tasks` (
  `task_name` varchar(40) NOT NULL,
  `task_instance` varchar(40) NOT NULL,
  `task_data` blob,
  `execution_time` timestamp(6) NOT NULL,
  `picked` tinyint(1) NOT NULL,
  `picked_by` varchar(50) DEFAULT NULL,
  `last_success` timestamp(6) NULL DEFAULT NULL,
  `last_failure` timestamp(6) NULL DEFAULT NULL,
  `consecutive_failures` int DEFAULT NULL,
  `last_heartbeat` timestamp(6) NULL DEFAULT NULL,
  `version` bigint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Doublure de structure pour la vue `see_orphan_relations`
-- (Voir ci-dessous la vue réelle)
--
CREATE TABLE `see_orphan_relations` (
`object_description` varchar(18)
,`relation_id` smallint unsigned
,`reference_id` smallint
);

-- --------------------------------------------------------

--
-- Structure de la table `special_locations`
--

CREATE TABLE `special_locations` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(30) NOT NULL,
  `image_id` bigint UNSIGNED DEFAULT NULL,
  `description` text NOT NULL,
  `galaxy_id` smallint UNSIGNED DEFAULT NULL,
  `improvement_id` smallint UNSIGNED DEFAULT NULL,
  `cloned_improvements` tinyint NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `speed_impact_groups`
--

CREATE TABLE `speed_impact_groups` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(50) NOT NULL,
  `is_fixed` tinyint NOT NULL,
  `mission_explore` double NOT NULL,
  `mission_gather` double NOT NULL,
  `mission_establish_base` double NOT NULL,
  `mission_attack` double NOT NULL,
  `mission_conquest` double NOT NULL,
  `mission_counterattack` double NOT NULL,
  `can_explore` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
  `can_gather` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
  `can_establish_base` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
  `can_attack` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
  `can_counterattack` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
  `can_conquest` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
  `can_deploy` enum('NONE','OWNED_ONLY','ANY') DEFAULT 'ANY',
  `image_id` bigint UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `sponsors`
--

CREATE TABLE `sponsors` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(200) NOT NULL,
  `description` text,
  `image_id` bigint UNSIGNED DEFAULT NULL,
  `url` varchar(200) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `type` enum('COMPANY','INDIVIDUAL') NOT NULL,
  `expiration_date` date DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `stored_units`
--

CREATE TABLE `stored_units` (
  `id` int UNSIGNED NOT NULL,
  `owner_obtained_unit_id` int UNSIGNED NOT NULL,
  `target_obtained_unit_id` int UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `suspicions`
--

CREATE TABLE `suspicions` (
  `id` bigint UNSIGNED NOT NULL,
  `source` enum('BROWSER','IP','BROWSER_AND_IP') NOT NULL,
  `user_id` int NOT NULL,
  `audit_id` bigint UNSIGNED NOT NULL,
  `created_at` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `system_messages`
--

CREATE TABLE `system_messages` (
  `id` smallint UNSIGNED NOT NULL,
  `content` text NOT NULL,
  `creation_date` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `time_specials`
--

CREATE TABLE `time_specials` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text,
  `image_id` bigint UNSIGNED DEFAULT NULL,
  `duration` bigint UNSIGNED NOT NULL COMMENT 'Duration <b>in seconds</b> of the time special',
  `recharge_time` bigint UNSIGNED NOT NULL COMMENT 'Time to wait <b>in seconds</b> to be able to use the time special again',
  `improvement_id` smallint UNSIGNED DEFAULT NULL,
  `cloned_improvements` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `tor_ip_data`
--

CREATE TABLE `tor_ip_data` (
  `ip` varchar(39) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL,
  `last_checked_date` datetime NOT NULL,
  `is_tor` tinyint(1) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- --------------------------------------------------------

--
-- Structure de la table `translatables`
--

CREATE TABLE `translatables` (
  `id` int UNSIGNED NOT NULL,
  `name` varchar(100) NOT NULL,
  `default_lang_code` char(2) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `translatables_translations`
--

CREATE TABLE `translatables_translations` (
  `id` int UNSIGNED NOT NULL,
  `translatable_id` int UNSIGNED NOT NULL,
  `lang_code` char(2) NOT NULL,
  `value` longtext NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `tutorial_sections`
--

CREATE TABLE `tutorial_sections` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` mediumtext,
  `frontend_router_path` varchar(150) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `tutorial_sections_available_html_symbols`
--

CREATE TABLE `tutorial_sections_available_html_symbols` (
  `id` int UNSIGNED NOT NULL,
  `name` varchar(50) NOT NULL,
  `identifier` varchar(150) NOT NULL COMMENT 'The identifier to use in the Frontend ngDirective',
  `tutorial_section_id` smallint UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `tutorial_sections_entries`
--

CREATE TABLE `tutorial_sections_entries` (
  `id` int UNSIGNED NOT NULL,
  `order_num` smallint UNSIGNED DEFAULT NULL,
  `section_available_html_symbol_id` int UNSIGNED NOT NULL,
  `event` enum('CLICK','ANY_KEY_OR_CLICK') NOT NULL,
  `text_id` int UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `units`
--

CREATE TABLE `units` (
  `id` smallint UNSIGNED NOT NULL,
  `order_number` smallint UNSIGNED DEFAULT NULL COMMENT 'El orden de la unidad',
  `name` char(40) CHARACTER SET latin1 COLLATE latin1_spanish_ci NOT NULL,
  `display_in_requirements` tinyint(1) DEFAULT '0',
  `attack_rule_id` smallint UNSIGNED DEFAULT NULL,
  `image_id` bigint UNSIGNED DEFAULT NULL,
  `points` int UNSIGNED DEFAULT NULL,
  `description` text CHARACTER SET latin1 COLLATE latin1_spanish_ci,
  `time` int DEFAULT NULL COMMENT 'El tiempo base para la mejora, en segundos',
  `primary_resource` int UNSIGNED DEFAULT NULL,
  `secondary_resource` int UNSIGNED DEFAULT NULL,
  `energy` smallint UNSIGNED DEFAULT '0',
  `type` smallint UNSIGNED DEFAULT NULL,
  `attack` smallint UNSIGNED DEFAULT NULL,
  `health` smallint UNSIGNED DEFAULT NULL,
  `shield` smallint UNSIGNED DEFAULT NULL,
  `charge` smallint UNSIGNED DEFAULT NULL,
  `is_unique` tinyint UNSIGNED NOT NULL DEFAULT '0',
  `can_fast_explore` tinyint(1) NOT NULL DEFAULT '0',
  `speed` double DEFAULT '0',
  `improvement_id` smallint UNSIGNED NOT NULL,
  `cloned_improvements` tinyint NOT NULL,
  `speed_impact_group_id` smallint UNSIGNED DEFAULT NULL,
  `critical_attack_id` smallint UNSIGNED DEFAULT NULL,
  `bypass_shield` tinyint NOT NULL,
  `is_invisible` tinyint(1) NOT NULL,
  `stored_weight` int UNSIGNED NOT NULL DEFAULT '1',
  `storage_capacity` int UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `unit_types`
--

CREATE TABLE `unit_types` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(20) NOT NULL,
  `attack_rule_id` smallint UNSIGNED DEFAULT NULL,
  `max_count` bigint DEFAULT NULL,
  `share_max_count` smallint UNSIGNED DEFAULT NULL COMMENT 'The count of this unit type will apply to referenced',
  `image_id` bigint UNSIGNED DEFAULT NULL,
  `parent_type` smallint UNSIGNED DEFAULT NULL,
  `can_explore` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_gather` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_establish_base` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_attack` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_counterattack` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_conquest` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_deploy` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `speed_impact_group_id` smallint UNSIGNED DEFAULT NULL,
  `critical_attack_id` smallint UNSIGNED DEFAULT NULL,
  `has_to_inherit_improvements` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'If true applied benefits to parent unit type will also apply to this'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `unlocked_relation`
--

CREATE TABLE `unlocked_relation` (
  `id` bigint NOT NULL,
  `user_id` int NOT NULL,
  `relation_id` smallint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Any row here, means the user can use such thing';

-- --------------------------------------------------------

--
-- Structure de la table `upgrades`
--

CREATE TABLE `upgrades` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(70) NOT NULL,
  `points` int NOT NULL DEFAULT '0',
  `image_id` bigint UNSIGNED DEFAULT NULL,
  `description` text,
  `time` int NOT NULL DEFAULT '60',
  `primary_resource` int NOT NULL DEFAULT '100',
  `secondary_resource` int NOT NULL DEFAULT '100',
  `type` smallint UNSIGNED DEFAULT NULL COMMENT 'Null means invisible',
  `level_effect` float NOT NULL DEFAULT '20',
  `improvement_id` smallint UNSIGNED DEFAULT NULL,
  `cloned_improvements` tinyint(1) NOT NULL COMMENT 'If improvements are cloned from other upgrade'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `upgrade_types`
--

CREATE TABLE `upgrade_types` (
  `id` smallint UNSIGNED NOT NULL,
  `name` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `user_improvements`
--

CREATE TABLE `user_improvements` (
  `id` smallint UNSIGNED NOT NULL,
  `user_id` int NOT NULL,
  `more_soldiers_production` smallint DEFAULT NULL,
  `more_primary_resource_production` smallint DEFAULT NULL,
  `more_secondary_resource_production` smallint DEFAULT NULL,
  `more_energy_production` smallint DEFAULT NULL,
  `more_charge_capacity` smallint DEFAULT NULL,
  `more_missions_value` tinyint DEFAULT NULL,
  `more_upgrade_research_speed` float UNSIGNED DEFAULT NULL,
  `more_unit_build_speed` float UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `user_read_system_messages`
--

CREATE TABLE `user_read_system_messages` (
  `id` int UNSIGNED NOT NULL,
  `user_id` int NOT NULL,
  `message_id` smallint UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `user_storage`
--

CREATE TABLE `user_storage` (
  `id` int NOT NULL COMMENT 'The id of the user as defined in the other database, no autoincremental, not even need to check it',
  `username` varchar(32) NOT NULL,
  `email` varchar(254) NOT NULL,
  `alliance_id` smallint UNSIGNED DEFAULT NULL,
  `faction` smallint UNSIGNED NOT NULL,
  `last_action` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `home_planet` bigint UNSIGNED NOT NULL,
  `primary_resource` double UNSIGNED DEFAULT NULL,
  `secondary_resource` double UNSIGNED DEFAULT NULL,
  `energy` double UNSIGNED NOT NULL,
  `primary_resource_generation_per_second` double UNSIGNED DEFAULT NULL,
  `secondary_resource_generation_per_second` double UNSIGNED DEFAULT NULL,
  `has_skipped_tutorial` tinyint(1) NOT NULL,
  `points` double NOT NULL DEFAULT '0',
  `can_alter_twitch_state` tinyint(1) NOT NULL,
  `last_multi_account_check` datetime DEFAULT NULL,
  `multi_account_score` float DEFAULT NULL,
  `banned` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Has the users that has inscribed in this database';

-- --------------------------------------------------------

--
-- Structure de la table `visited_tutorial_entries`
--

CREATE TABLE `visited_tutorial_entries` (
  `id` bigint NOT NULL,
  `user_id` int NOT NULL,
  `entry_id` int UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `websocket_events_information`
--

CREATE TABLE `websocket_events_information` (
  `event_name` varchar(100) NOT NULL,
  `user_id` int NOT NULL,
  `last_sent` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `websocket_messages_status`
--

CREATE TABLE `websocket_messages_status` (
  `id` bigint UNSIGNED NOT NULL,
  `user_id` int DEFAULT NULL,
  `event_name` varchar(100) NOT NULL,
  `unwhiling_to_delivery` tinyint NOT NULL,
  `socket_server_ack` tinyint NOT NULL,
  `socket_not_found` tinyint NOT NULL,
  `web_browser_ack` tinytext NOT NULL,
  `is_user_ack_required` tinytext NOT NULL,
  `user_ack` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

-- --------------------------------------------------------

--
-- Structure de la vue `see_orphan_relations`
--
DROP TABLE IF EXISTS `see_orphan_relations`;

CREATE ALGORITHM=UNDEFINED DEFINER=`root`@`%` SQL SECURITY DEFINER VIEW `see_orphan_relations`  AS SELECT `ort`.`object_description` AS `object_description`, `ort`.`id` AS `relation_id`, `ort`.`reference_id` AS `reference_id` FROM (`object_relations` `ort` left join `units` `u` on((`u`.`id` = `ort`.`reference_id`))) WHERE ((`ort`.`object_description` = 'UNIT') AND (`u`.`id` is null)) ;

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `active_time_specials`
--
ALTER TABLE `active_time_specials`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `time_special_id` (`time_special_id`);

--
-- Index pour la table `admin_users`
--
ALTER TABLE `admin_users`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `alliances`
--
ALTER TABLE `alliances`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `owner_id` (`owner_id`);

--
-- Index pour la table `alliance_join_request`
--
ALTER TABLE `alliance_join_request`
  ADD PRIMARY KEY (`id`),
  ADD KEY `alliance_id` (`alliance_id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `attack_rules`
--
ALTER TABLE `attack_rules`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `attack_rule_entries`
--
ALTER TABLE `attack_rule_entries`
  ADD PRIMARY KEY (`id`),
  ADD KEY `attack_rule_id` (`attack_rule_id`);

--
-- Index pour la table `audit`
--
ALTER TABLE `audit`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_audit_related_user` (`user_id`),
  ADD KEY `ipv4` (`ipv4`),
  ADD KEY `ipv6` (`ipv6`);

--
-- Index pour la table `configuration`
--
ALTER TABLE `configuration`
  ADD PRIMARY KEY (`name`);

--
-- Index pour la table `critical_attack`
--
ALTER TABLE `critical_attack`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `critical_attack_entries`
--
ALTER TABLE `critical_attack_entries`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `explored_planets`
--
ALTER TABLE `explored_planets`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user` (`user`),
  ADD KEY `planet` (`planet`);

--
-- Index pour la table `factions`
--
ALTER TABLE `factions`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`),
  ADD KEY `improvement_id` (`improvement_id`),
  ADD KEY `image_id` (`image_id`),
  ADD KEY `primary_resource_image_id` (`primary_resource_image_id`),
  ADD KEY `secondary_resource_image_id` (`secondary_resource_image_id`),
  ADD KEY `energy_image_id` (`energy_image_id`);

--
-- Index pour la table `factions_unit_types`
--
ALTER TABLE `factions_unit_types`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_fut_faction_id` (`faction_id`),
  ADD KEY `fk_fut_unit_type_id` (`unit_type_id`);

--
-- Index pour la table `faction_spawn_location`
--
ALTER TABLE `faction_spawn_location`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `galaxies`
--
ALTER TABLE `galaxies`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `images_store`
--
ALTER TABLE `images_store`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `checksum` (`checksum`);

--
-- Index pour la table `improvements`
--
ALTER TABLE `improvements`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `improvements_unit_types`
--
ALTER TABLE `improvements_unit_types`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `improvements_id` (`improvement_id`,`type`,`unit_type_id`),
  ADD KEY `unit_type_id` (`unit_type_id`);

--
-- Index pour la table `interceptable_speed_group`
--
ALTER TABLE `interceptable_speed_group`
  ADD PRIMARY KEY (`id`),
  ADD KEY `unit_id` (`unit_id`),
  ADD KEY `speed_impact_group_id` (`speed_impact_group_id`);

--
-- Index pour la table `missions`
--
ALTER TABLE `missions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `type` (`type`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `related_mission` (`related_mission`),
  ADD KEY `report` (`report_id`);

--
-- Index pour la table `mission_information`
--
ALTER TABLE `mission_information`
  ADD PRIMARY KEY (`id`),
  ADD KEY `mission_id` (`mission_id`),
  ADD KEY `relation_id` (`relation_id`);

--
-- Index pour la table `mission_reports`
--
ALTER TABLE `mission_reports`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `mission_types`
--
ALTER TABLE `mission_types`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `objects`
--
ALTER TABLE `objects`
  ADD PRIMARY KEY (`description`);

--
-- Index pour la table `object_relations`
--
ALTER TABLE `object_relations`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `id_object` (`object_description`,`reference_id`);

--
-- Index pour la table `object_relation__object_relation`
--
ALTER TABLE `object_relation__object_relation`
  ADD PRIMARY KEY (`id`),
  ADD KEY `master_relation_id` (`master_relation_id`,`slave_relation_id`),
  ADD KEY `fk_object_relation__object_relation_slave` (`slave_relation_id`);

--
-- Index pour la table `obtained_units`
--
ALTER TABLE `obtained_units`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `first_deployment_mission` (`first_deployment_mission`),
  ADD KEY `owner_unit_id` (`owner_unit_id`);

--
-- Index pour la table `obtained_unit_temporal_information`
--
ALTER TABLE `obtained_unit_temporal_information`
  ADD PRIMARY KEY (`id`),
  ADD KEY `relation_id` (`relation_id`);

--
-- Index pour la table `obtained_upgrades`
--
ALTER TABLE `obtained_upgrades`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `upgrade_id` (`upgrade_id`);

--
-- Index pour la table `planets`
--
ALTER TABLE `planets`
  ADD PRIMARY KEY (`id`),
  ADD KEY `galaxy_id` (`galaxy_id`,`sector`,`quadrant`),
  ADD KEY `owner` (`owner`),
  ADD KEY `special_location_id` (`special_location_id`);

--
-- Index pour la table `planet_list`
--
ALTER TABLE `planet_list`
  ADD PRIMARY KEY (`user_id`,`planet_id`),
  ADD KEY `fk_planet_list_planet` (`planet_id`);

--
-- Index pour la table `qrtz_blob_triggers`
--
ALTER TABLE `qrtz_blob_triggers`
  ADD PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  ADD KEY `SCHED_NAME` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`);

--
-- Index pour la table `qrtz_calendars`
--
ALTER TABLE `qrtz_calendars`
  ADD PRIMARY KEY (`SCHED_NAME`,`CALENDAR_NAME`);

--
-- Index pour la table `qrtz_cron_triggers`
--
ALTER TABLE `qrtz_cron_triggers`
  ADD PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`);

--
-- Index pour la table `qrtz_fired_triggers`
--
ALTER TABLE `qrtz_fired_triggers`
  ADD PRIMARY KEY (`SCHED_NAME`,`ENTRY_ID`),
  ADD KEY `IDX_QRTZ_FT_TRIG_INST_NAME` (`SCHED_NAME`,`INSTANCE_NAME`),
  ADD KEY `IDX_QRTZ_FT_INST_JOB_REQ_RCVRY` (`SCHED_NAME`,`INSTANCE_NAME`,`REQUESTS_RECOVERY`),
  ADD KEY `IDX_QRTZ_FT_J_G` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  ADD KEY `IDX_QRTZ_FT_JG` (`SCHED_NAME`,`JOB_GROUP`),
  ADD KEY `IDX_QRTZ_FT_T_G` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  ADD KEY `IDX_QRTZ_FT_TG` (`SCHED_NAME`,`TRIGGER_GROUP`);

--
-- Index pour la table `qrtz_job_details`
--
ALTER TABLE `qrtz_job_details`
  ADD PRIMARY KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  ADD KEY `IDX_QRTZ_J_REQ_RECOVERY` (`SCHED_NAME`,`REQUESTS_RECOVERY`),
  ADD KEY `IDX_QRTZ_J_GRP` (`SCHED_NAME`,`JOB_GROUP`);

--
-- Index pour la table `qrtz_locks`
--
ALTER TABLE `qrtz_locks`
  ADD PRIMARY KEY (`SCHED_NAME`,`LOCK_NAME`);

--
-- Index pour la table `qrtz_paused_trigger_grps`
--
ALTER TABLE `qrtz_paused_trigger_grps`
  ADD PRIMARY KEY (`SCHED_NAME`,`TRIGGER_GROUP`);

--
-- Index pour la table `qrtz_scheduler_state`
--
ALTER TABLE `qrtz_scheduler_state`
  ADD PRIMARY KEY (`SCHED_NAME`,`INSTANCE_NAME`);

--
-- Index pour la table `qrtz_simple_triggers`
--
ALTER TABLE `qrtz_simple_triggers`
  ADD PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`);

--
-- Index pour la table `qrtz_simprop_triggers`
--
ALTER TABLE `qrtz_simprop_triggers`
  ADD PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`);

--
-- Index pour la table `qrtz_triggers`
--
ALTER TABLE `qrtz_triggers`
  ADD PRIMARY KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`),
  ADD KEY `IDX_QRTZ_T_J` (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`),
  ADD KEY `IDX_QRTZ_T_JG` (`SCHED_NAME`,`JOB_GROUP`),
  ADD KEY `IDX_QRTZ_T_C` (`SCHED_NAME`,`CALENDAR_NAME`),
  ADD KEY `IDX_QRTZ_T_G` (`SCHED_NAME`,`TRIGGER_GROUP`),
  ADD KEY `IDX_QRTZ_T_STATE` (`SCHED_NAME`,`TRIGGER_STATE`),
  ADD KEY `IDX_QRTZ_T_N_STATE` (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  ADD KEY `IDX_QRTZ_T_N_G_STATE` (`SCHED_NAME`,`TRIGGER_GROUP`,`TRIGGER_STATE`),
  ADD KEY `IDX_QRTZ_T_NEXT_FIRE_TIME` (`SCHED_NAME`,`NEXT_FIRE_TIME`),
  ADD KEY `IDX_QRTZ_T_NFT_ST` (`SCHED_NAME`,`TRIGGER_STATE`,`NEXT_FIRE_TIME`),
  ADD KEY `IDX_QRTZ_T_NFT_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`),
  ADD KEY `IDX_QRTZ_T_NFT_ST_MISFIRE` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_STATE`),
  ADD KEY `IDX_QRTZ_T_NFT_ST_MISFIRE_GRP` (`SCHED_NAME`,`MISFIRE_INSTR`,`NEXT_FIRE_TIME`,`TRIGGER_GROUP`,`TRIGGER_STATE`);

--
-- Index pour la table `requirements`
--
ALTER TABLE `requirements`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `requirements_information`
--
ALTER TABLE `requirements_information`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `relation_id` (`relation_id`,`requirement_id`,`second_value`),
  ADD KEY `requirement_id` (`requirement_id`);

--
-- Index pour la table `requirement_group`
--
ALTER TABLE `requirement_group`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `rules`
--
ALTER TABLE `rules`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `scheduled_tasks`
--
ALTER TABLE `scheduled_tasks`
  ADD PRIMARY KEY (`task_name`,`task_instance`),
  ADD KEY `execution_time_idx` (`execution_time`),
  ADD KEY `last_heartbeat_idx` (`last_heartbeat`);

--
-- Index pour la table `special_locations`
--
ALTER TABLE `special_locations`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`),
  ADD KEY `improvement_id` (`improvement_id`),
  ADD KEY `galaxy_id` (`galaxy_id`),
  ADD KEY `image_id` (`image_id`);

--
-- Index pour la table `speed_impact_groups`
--
ALTER TABLE `speed_impact_groups`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `sponsors`
--
ALTER TABLE `sponsors`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_sponsors_image_id` (`image_id`);

--
-- Index pour la table `stored_units`
--
ALTER TABLE `stored_units`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `suspicions`
--
ALTER TABLE `suspicions`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`,`audit_id`);

--
-- Index pour la table `system_messages`
--
ALTER TABLE `system_messages`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `time_specials`
--
ALTER TABLE `time_specials`
  ADD PRIMARY KEY (`id`),
  ADD KEY `image_id` (`image_id`);

--
-- Index pour la table `tor_ip_data`
--
ALTER TABLE `tor_ip_data`
  ADD PRIMARY KEY (`ip`);

--
-- Index pour la table `translatables`
--
ALTER TABLE `translatables`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `translatables_translations`
--
ALTER TABLE `translatables_translations`
  ADD PRIMARY KEY (`id`),
  ADD KEY `translatable_id` (`translatable_id`);

--
-- Index pour la table `tutorial_sections`
--
ALTER TABLE `tutorial_sections`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `tutorial_sections_available_html_symbols`
--
ALTER TABLE `tutorial_sections_available_html_symbols`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_section_id` (`tutorial_section_id`);

--
-- Index pour la table `tutorial_sections_entries`
--
ALTER TABLE `tutorial_sections_entries`
  ADD PRIMARY KEY (`id`),
  ADD KEY `fk_tse_symbol_id` (`section_available_html_symbol_id`);

--
-- Index pour la table `units`
--
ALTER TABLE `units`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`),
  ADD KEY `type` (`type`),
  ADD KEY `improvement_id` (`improvement_id`),
  ADD KEY `image_id` (`image_id`),
  ADD KEY `speed_impact_group_id` (`speed_impact_group_id`),
  ADD KEY `units__attack_rules` (`attack_rule_id`),
  ADD KEY `units_critical_attack` (`critical_attack_id`);

--
-- Index pour la table `unit_types`
--
ALTER TABLE `unit_types`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`),
  ADD KEY `image_id` (`image_id`),
  ADD KEY `speed_impact_group_id` (`speed_impact_group_id`),
  ADD KEY `unit_types__attack_rules` (`attack_rule_id`),
  ADD KEY `unit_types_share_count` (`share_max_count`),
  ADD KEY `unit_types_parent_type` (`parent_type`),
  ADD KEY `unit_types_critical_attack` (`critical_attack_id`);

--
-- Index pour la table `unlocked_relation`
--
ALTER TABLE `unlocked_relation`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `user_id` (`user_id`,`relation_id`),
  ADD KEY `relation_id` (`relation_id`);

--
-- Index pour la table `upgrades`
--
ALTER TABLE `upgrades`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`),
  ADD KEY `type` (`type`),
  ADD KEY `improvements_id` (`improvement_id`),
  ADD KEY `image_id` (`image_id`);

--
-- Index pour la table `upgrade_types`
--
ALTER TABLE `upgrade_types`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`);

--
-- Index pour la table `user_improvements`
--
ALTER TABLE `user_improvements`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `user_read_system_messages`
--
ALTER TABLE `user_read_system_messages`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`),
  ADD KEY `message_id` (`message_id`);

--
-- Index pour la table `user_storage`
--
ALTER TABLE `user_storage`
  ADD PRIMARY KEY (`id`),
  ADD KEY `faction` (`faction`),
  ADD KEY `home_planet` (`home_planet`),
  ADD KEY `alliance_id` (`alliance_id`);

--
-- Index pour la table `visited_tutorial_entries`
--
ALTER TABLE `visited_tutorial_entries`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- Index pour la table `websocket_events_information`
--
ALTER TABLE `websocket_events_information`
  ADD PRIMARY KEY (`event_name`,`user_id`),
  ADD KEY `fk_user` (`user_id`);

--
-- Index pour la table `websocket_messages_status`
--
ALTER TABLE `websocket_messages_status`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `active_time_specials`
--
ALTER TABLE `active_time_specials`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `alliances`
--
ALTER TABLE `alliances`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `alliance_join_request`
--
ALTER TABLE `alliance_join_request`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `attack_rules`
--
ALTER TABLE `attack_rules`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `attack_rule_entries`
--
ALTER TABLE `attack_rule_entries`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `audit`
--
ALTER TABLE `audit`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `critical_attack`
--
ALTER TABLE `critical_attack`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `critical_attack_entries`
--
ALTER TABLE `critical_attack_entries`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `explored_planets`
--
ALTER TABLE `explored_planets`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `factions`
--
ALTER TABLE `factions`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `factions_unit_types`
--
ALTER TABLE `factions_unit_types`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `faction_spawn_location`
--
ALTER TABLE `faction_spawn_location`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `galaxies`
--
ALTER TABLE `galaxies`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `images_store`
--
ALTER TABLE `images_store`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `improvements`
--
ALTER TABLE `improvements`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `improvements_unit_types`
--
ALTER TABLE `improvements_unit_types`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `interceptable_speed_group`
--
ALTER TABLE `interceptable_speed_group`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `missions`
--
ALTER TABLE `missions`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `mission_information`
--
ALTER TABLE `mission_information`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `mission_reports`
--
ALTER TABLE `mission_reports`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `mission_types`
--
ALTER TABLE `mission_types`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `object_relations`
--
ALTER TABLE `object_relations`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `object_relation__object_relation`
--
ALTER TABLE `object_relation__object_relation`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `obtained_units`
--
ALTER TABLE `obtained_units`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `obtained_unit_temporal_information`
--
ALTER TABLE `obtained_unit_temporal_information`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `obtained_upgrades`
--
ALTER TABLE `obtained_upgrades`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `planets`
--
ALTER TABLE `planets`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `requirements`
--
ALTER TABLE `requirements`
  MODIFY `id` smallint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `requirements_information`
--
ALTER TABLE `requirements_information`
  MODIFY `id` smallint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `requirement_group`
--
ALTER TABLE `requirement_group`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `rules`
--
ALTER TABLE `rules`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `special_locations`
--
ALTER TABLE `special_locations`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `speed_impact_groups`
--
ALTER TABLE `speed_impact_groups`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `sponsors`
--
ALTER TABLE `sponsors`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `stored_units`
--
ALTER TABLE `stored_units`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `suspicions`
--
ALTER TABLE `suspicions`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `system_messages`
--
ALTER TABLE `system_messages`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `time_specials`
--
ALTER TABLE `time_specials`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `translatables`
--
ALTER TABLE `translatables`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `translatables_translations`
--
ALTER TABLE `translatables_translations`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `tutorial_sections`
--
ALTER TABLE `tutorial_sections`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `tutorial_sections_available_html_symbols`
--
ALTER TABLE `tutorial_sections_available_html_symbols`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `tutorial_sections_entries`
--
ALTER TABLE `tutorial_sections_entries`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `units`
--
ALTER TABLE `units`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `unit_types`
--
ALTER TABLE `unit_types`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `unlocked_relation`
--
ALTER TABLE `unlocked_relation`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `upgrades`
--
ALTER TABLE `upgrades`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `upgrade_types`
--
ALTER TABLE `upgrade_types`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `user_improvements`
--
ALTER TABLE `user_improvements`
  MODIFY `id` smallint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `user_read_system_messages`
--
ALTER TABLE `user_read_system_messages`
  MODIFY `id` int UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `visited_tutorial_entries`
--
ALTER TABLE `visited_tutorial_entries`
  MODIFY `id` bigint NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `websocket_messages_status`
--
ALTER TABLE `websocket_messages_status`
  MODIFY `id` bigint UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `active_time_specials`
--
ALTER TABLE `active_time_specials`
  ADD CONSTRAINT `active_time_specials_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`),
  ADD CONSTRAINT `active_time_specials_ibfk_2` FOREIGN KEY (`time_special_id`) REFERENCES `time_specials` (`id`);

--
-- Contraintes pour la table `alliances`
--
ALTER TABLE `alliances`
  ADD CONSTRAINT `alliances_ibfk_1` FOREIGN KEY (`owner_id`) REFERENCES `user_storage` (`id`);

--
-- Contraintes pour la table `alliance_join_request`
--
ALTER TABLE `alliance_join_request`
  ADD CONSTRAINT `alliance_join_request_ibfk_1` FOREIGN KEY (`alliance_id`) REFERENCES `alliances` (`id`),
  ADD CONSTRAINT `alliance_join_request_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`);

--
-- Contraintes pour la table `attack_rule_entries`
--
ALTER TABLE `attack_rule_entries`
  ADD CONSTRAINT `fk_attack_rule` FOREIGN KEY (`attack_rule_id`) REFERENCES `attack_rules` (`id`);

--
-- Contraintes pour la table `audit`
--
ALTER TABLE `audit`
  ADD CONSTRAINT `fk_audit_related_user` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `fk_audit_user` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

--
-- Contraintes pour la table `explored_planets`
--
ALTER TABLE `explored_planets`
  ADD CONSTRAINT `explored_planets_ibfk_1` FOREIGN KEY (`planet`) REFERENCES `planets` (`id`),
  ADD CONSTRAINT `explored_planets_ibfk_2` FOREIGN KEY (`user`) REFERENCES `user_storage` (`id`);

--
-- Contraintes pour la table `factions`
--
ALTER TABLE `factions`
  ADD CONSTRAINT `factions_ibfk_1` FOREIGN KEY (`improvement_id`) REFERENCES `improvements` (`id`),
  ADD CONSTRAINT `factions_ibfk_2` FOREIGN KEY (`image_id`) REFERENCES `images_store` (`id`),
  ADD CONSTRAINT `factions_ibfk_3` FOREIGN KEY (`primary_resource_image_id`) REFERENCES `images_store` (`id`),
  ADD CONSTRAINT `factions_ibfk_4` FOREIGN KEY (`secondary_resource_image_id`) REFERENCES `images_store` (`id`),
  ADD CONSTRAINT `factions_ibfk_5` FOREIGN KEY (`energy_image_id`) REFERENCES `images_store` (`id`);

--
-- Contraintes pour la table `factions_unit_types`
--
ALTER TABLE `factions_unit_types`
  ADD CONSTRAINT `fk_fut_faction_id` FOREIGN KEY (`faction_id`) REFERENCES `factions` (`id`),
  ADD CONSTRAINT `fk_fut_unit_type_id` FOREIGN KEY (`unit_type_id`) REFERENCES `unit_types` (`id`);

--
-- Contraintes pour la table `improvements_unit_types`
--
ALTER TABLE `improvements_unit_types`
  ADD CONSTRAINT `improvements_unit_types_ibfk_1` FOREIGN KEY (`improvement_id`) REFERENCES `improvements` (`id`),
  ADD CONSTRAINT `improvements_unit_types_ibfk_2` FOREIGN KEY (`unit_type_id`) REFERENCES `unit_types` (`id`);

--
-- Contraintes pour la table `interceptable_speed_group`
--
ALTER TABLE `interceptable_speed_group`
  ADD CONSTRAINT `fk_isg_speed_impact_group_id` FOREIGN KEY (`speed_impact_group_id`) REFERENCES `speed_impact_groups` (`id`),
  ADD CONSTRAINT `fk_isg_unit_id` FOREIGN KEY (`unit_id`) REFERENCES `units` (`id`);

--
-- Contraintes pour la table `mission_information`
--
ALTER TABLE `mission_information`
  ADD CONSTRAINT `mission_information_ibfk_2` FOREIGN KEY (`relation_id`) REFERENCES `object_relations` (`id`),
  ADD CONSTRAINT `mission_information_ibfk_3` FOREIGN KEY (`mission_id`) REFERENCES `missions` (`id`);

--
-- Contraintes pour la table `mission_reports`
--
ALTER TABLE `mission_reports`
  ADD CONSTRAINT `mission_reports_ibfk_2` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`);

--
-- Contraintes pour la table `object_relations`
--
ALTER TABLE `object_relations`
  ADD CONSTRAINT `object_relations_ibfk_1` FOREIGN KEY (`object_description`) REFERENCES `objects` (`description`);

--
-- Contraintes pour la table `object_relation__object_relation`
--
ALTER TABLE `object_relation__object_relation`
  ADD CONSTRAINT `fk_object_relation__object_relation_master` FOREIGN KEY (`master_relation_id`) REFERENCES `object_relations` (`id`) ON DELETE CASCADE ON UPDATE CASCADE,
  ADD CONSTRAINT `fk_object_relation__object_relation_slave` FOREIGN KEY (`slave_relation_id`) REFERENCES `object_relations` (`id`) ON DELETE CASCADE ON UPDATE CASCADE;

--
-- Contraintes pour la table `obtained_units`
--
ALTER TABLE `obtained_units`
  ADD CONSTRAINT `obtained_units_ibfk_1` FOREIGN KEY (`first_deployment_mission`) REFERENCES `missions` (`id`);

--
-- Contraintes pour la table `obtained_upgrades`
--
ALTER TABLE `obtained_upgrades`
  ADD CONSTRAINT `obtained_upgrades_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`),
  ADD CONSTRAINT `obtained_upgrades_ibfk_2` FOREIGN KEY (`upgrade_id`) REFERENCES `upgrades` (`id`);

--
-- Contraintes pour la table `planets`
--
ALTER TABLE `planets`
  ADD CONSTRAINT `planets_ibfk_1` FOREIGN KEY (`galaxy_id`) REFERENCES `galaxies` (`id`),
  ADD CONSTRAINT `planets_ibfk_2` FOREIGN KEY (`special_location_id`) REFERENCES `special_locations` (`id`),
  ADD CONSTRAINT `planets_ibfk_3` FOREIGN KEY (`owner`) REFERENCES `user_storage` (`id`);

--
-- Contraintes pour la table `planet_list`
--
ALTER TABLE `planet_list`
  ADD CONSTRAINT `fk_panet_list_user` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`),
  ADD CONSTRAINT `fk_planet_list_planet` FOREIGN KEY (`planet_id`) REFERENCES `planets` (`id`);

--
-- Contraintes pour la table `qrtz_blob_triggers`
--
ALTER TABLE `qrtz_blob_triggers`
  ADD CONSTRAINT `QRTZ_BLOB_TRIGGERS_IBFK_1` FOREIGN KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`);

--
-- Contraintes pour la table `qrtz_cron_triggers`
--
ALTER TABLE `qrtz_cron_triggers`
  ADD CONSTRAINT `QRTZ_CRON_TRIGGERS_IBFK_1` FOREIGN KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`);

--
-- Contraintes pour la table `qrtz_simple_triggers`
--
ALTER TABLE `qrtz_simple_triggers`
  ADD CONSTRAINT `QRTZ_SIMPLE_TRIGGERS_IBFK_1` FOREIGN KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`);

--
-- Contraintes pour la table `qrtz_simprop_triggers`
--
ALTER TABLE `qrtz_simprop_triggers`
  ADD CONSTRAINT `QRTZ_SIMPROP_TRIGGERS_IBFK_1` FOREIGN KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`);

--
-- Contraintes pour la table `qrtz_triggers`
--
ALTER TABLE `qrtz_triggers`
  ADD CONSTRAINT `QRTZ_TRIGGERS_IBFK_1` FOREIGN KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`) REFERENCES `qrtz_job_details` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`);

--
-- Contraintes pour la table `requirements_information`
--
ALTER TABLE `requirements_information`
  ADD CONSTRAINT `requirements_information_ibfk_1` FOREIGN KEY (`requirement_id`) REFERENCES `requirements` (`id`),
  ADD CONSTRAINT `requirements_information_ibfk_2` FOREIGN KEY (`relation_id`) REFERENCES `object_relations` (`id`);

--
-- Contraintes pour la table `special_locations`
--
ALTER TABLE `special_locations`
  ADD CONSTRAINT `special_locations_ibfk_2` FOREIGN KEY (`improvement_id`) REFERENCES `improvements` (`id`),
  ADD CONSTRAINT `special_locations_ibfk_3` FOREIGN KEY (`galaxy_id`) REFERENCES `galaxies` (`id`),
  ADD CONSTRAINT `special_locations_ibfk_4` FOREIGN KEY (`image_id`) REFERENCES `images_store` (`id`);

--
-- Contraintes pour la table `sponsors`
--
ALTER TABLE `sponsors`
  ADD CONSTRAINT `fk_sponsors_image_id` FOREIGN KEY (`image_id`) REFERENCES `images_store` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT;

--
-- Contraintes pour la table `time_specials`
--
ALTER TABLE `time_specials`
  ADD CONSTRAINT `time_specials_ibfk_1` FOREIGN KEY (`image_id`) REFERENCES `images_store` (`id`);

--
-- Contraintes pour la table `translatables_translations`
--
ALTER TABLE `translatables_translations`
  ADD CONSTRAINT `fk_translatable_id` FOREIGN KEY (`translatable_id`) REFERENCES `translatables` (`id`);

--
-- Contraintes pour la table `tutorial_sections_available_html_symbols`
--
ALTER TABLE `tutorial_sections_available_html_symbols`
  ADD CONSTRAINT `fk_section_id` FOREIGN KEY (`tutorial_section_id`) REFERENCES `tutorial_sections` (`id`);

--
-- Contraintes pour la table `tutorial_sections_entries`
--
ALTER TABLE `tutorial_sections_entries`
  ADD CONSTRAINT `fk_tse_symbol_id` FOREIGN KEY (`section_available_html_symbol_id`) REFERENCES `tutorial_sections_available_html_symbols` (`id`);

--
-- Contraintes pour la table `units`
--
ALTER TABLE `units`
  ADD CONSTRAINT `units__attack_rules` FOREIGN KEY (`attack_rule_id`) REFERENCES `attack_rules` (`id`),
  ADD CONSTRAINT `units__speed_impact` FOREIGN KEY (`speed_impact_group_id`) REFERENCES `speed_impact_groups` (`id`),
  ADD CONSTRAINT `units_critical_attack` FOREIGN KEY (`critical_attack_id`) REFERENCES `critical_attack` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `units_ibfk_1` FOREIGN KEY (`type`) REFERENCES `unit_types` (`id`),
  ADD CONSTRAINT `units_ibfk_2` FOREIGN KEY (`improvement_id`) REFERENCES `improvements` (`id`),
  ADD CONSTRAINT `units_ibfk_3` FOREIGN KEY (`image_id`) REFERENCES `images_store` (`id`);

--
-- Contraintes pour la table `unit_types`
--
ALTER TABLE `unit_types`
  ADD CONSTRAINT `unit_types__attack_rules` FOREIGN KEY (`attack_rule_id`) REFERENCES `attack_rules` (`id`),
  ADD CONSTRAINT `unit_types__speed_impact` FOREIGN KEY (`speed_impact_group_id`) REFERENCES `speed_impact_groups` (`id`),
  ADD CONSTRAINT `unit_types_critical_attack` FOREIGN KEY (`critical_attack_id`) REFERENCES `critical_attack` (`id`) ON DELETE RESTRICT ON UPDATE RESTRICT,
  ADD CONSTRAINT `unit_types_ibfk_1` FOREIGN KEY (`image_id`) REFERENCES `images_store` (`id`),
  ADD CONSTRAINT `unit_types_parent_type` FOREIGN KEY (`parent_type`) REFERENCES `unit_types` (`id`),
  ADD CONSTRAINT `unit_types_share_count` FOREIGN KEY (`share_max_count`) REFERENCES `unit_types` (`id`);

--
-- Contraintes pour la table `unlocked_relation`
--
ALTER TABLE `unlocked_relation`
  ADD CONSTRAINT `unlocked_relation_ibfk_3` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`),
  ADD CONSTRAINT `unlocked_relation_ibfk_4` FOREIGN KEY (`relation_id`) REFERENCES `object_relations` (`id`);

--
-- Contraintes pour la table `upgrades`
--
ALTER TABLE `upgrades`
  ADD CONSTRAINT `upgrades_ibfk_1` FOREIGN KEY (`type`) REFERENCES `upgrade_types` (`id`),
  ADD CONSTRAINT `upgrades_ibfk_2` FOREIGN KEY (`improvement_id`) REFERENCES `improvements` (`id`),
  ADD CONSTRAINT `upgrades_ibfk_3` FOREIGN KEY (`image_id`) REFERENCES `images_store` (`id`);

--
-- Contraintes pour la table `user_read_system_messages`
--
ALTER TABLE `user_read_system_messages`
  ADD CONSTRAINT `fk_ursm_message_id` FOREIGN KEY (`message_id`) REFERENCES `system_messages` (`id`),
  ADD CONSTRAINT `fk_ursm_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`);

--
-- Contraintes pour la table `user_storage`
--
ALTER TABLE `user_storage`
  ADD CONSTRAINT `user_storage_ibfk_1` FOREIGN KEY (`home_planet`) REFERENCES `planets` (`id`),
  ADD CONSTRAINT `user_storage_ibfk_2` FOREIGN KEY (`faction`) REFERENCES `factions` (`id`),
  ADD CONSTRAINT `user_storage_ibfk_3` FOREIGN KEY (`alliance_id`) REFERENCES `alliances` (`id`);

--
-- Contraintes pour la table `visited_tutorial_entries`
--
ALTER TABLE `visited_tutorial_entries`
  ADD CONSTRAINT `fk_vts_user_id` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`);

--
-- Contraintes pour la table `websocket_events_information`
--
ALTER TABLE `websocket_events_information`
  ADD CONSTRAINT `fk_user` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`);

--
-- Contraintes pour la table `websocket_messages_status`
--
ALTER TABLE `websocket_messages_status`
  ADD CONSTRAINT `websocket_messages_status_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
