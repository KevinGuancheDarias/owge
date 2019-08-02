-- phpMyAdmin SQL Dump
-- version 4.7.9
-- https://www.phpmyadmin.net/
--
-- Hôte : 192.168.122.167
-- Généré le :  jeu. 10 jan. 2019 à 14:35
-- Version du serveur :  5.7.19-log
-- Version de PHP :  7.2.2

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données :  `sgalactica_java`
--

-- --------------------------------------------------------

--
-- Structure de la table `admin_users`
--

CREATE TABLE `admin_users` (
  `id` int(10) UNSIGNED NOT NULL,
  `username` varchar(20) NOT NULL,
  `password` varchar(30) NOT NULL,
  `mail` varchar(100) NOT NULL,
  `enabled` tinyint(1) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `alliances`
--

CREATE TABLE `alliances` (
  `id` smallint(5) UNSIGNED NOT NULL,
  `name` varchar(100) NOT NULL,
  `description` text,
  `image` char(36) DEFAULT NULL,
  `owner_id` int(11) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `alliance_join_request`
--

CREATE TABLE `alliance_join_request` (
  `id` int(10) UNSIGNED NOT NULL,
  `alliance_id` smallint(5) UNSIGNED NOT NULL,
  `user_id` int(11) UNSIGNED NOT NULL,
  `request_date` datetime NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `aranking`
--

CREATE TABLE `aranking` (
  `PosicionTotal` int(5) NOT NULL,
  `PosicionMejoras` int(4) NOT NULL,
  `PosicionTropas` int(4) NOT NULL,
  `PosicionNaves` int(4) NOT NULL,
  `PosicionDefensas` int(4) NOT NULL,
  `alianzacd` int(7) NOT NULL,
  `PuntosTotales` int(11) NOT NULL,
  `PuntosMejoras` int(11) NOT NULL,
  `PuntosTropas` int(11) NOT NULL,
  `PuntosNaves` int(11) NOT NULL,
  `PuntosDefensas` int(11) NOT NULL,
  `Imagen320x240` char(255) NOT NULL,
  `Imagen640x240` char(255) NOT NULL
) ENGINE=MEMORY DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Structure de la table `carpetas`
--

CREATE TABLE `carpetas` (
  `cd` int(11) NOT NULL,
  `Nombre` char(15) COLLATE latin1_spanish_ci NOT NULL,
  `usercd` int(6) NOT NULL,
  `Borrable` tinyint(1) NOT NULL,
  `Movible` tinyint(1) NOT NULL,
  `Mensajes` int(2) NOT NULL COMMENT 'Número de mensajes que contiene'
) ENGINE=MyISAM DEFAULT CHARSET=latin1 COLLATE=latin1_spanish_ci;

-- --------------------------------------------------------

--
-- Structure de la table `configuration`
--

CREATE TABLE `configuration` (
  `name` varchar(30) NOT NULL,
  `display_name` varchar(400) DEFAULT NULL,
  `value` varchar(200) NOT NULL,
  `privileged` tinyint(4) NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `especialesderaza`
--

CREATE TABLE `especialesderaza` (
  `cd` int(11) NOT NULL,
  `Oculto` tinyint(4) NOT NULL DEFAULT '1',
  `rcd` int(11) NOT NULL,
  `Nombre` varchar(50) NOT NULL,
  `Descripcion` text NOT NULL,
  `Duracion` int(11) NOT NULL COMMENT 'Tiempo que dura el especial',
  `Recarga` int(11) NOT NULL COMMENT 'Tiempo que tarda en volver a estar disponible el especial',
  `Atributos` text NOT NULL,
  `cdImagen` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `explored_planets`
--

CREATE TABLE `explored_planets` (
  `id` bigint(20) NOT NULL,
  `user` int(11) UNSIGNED NOT NULL,
  `planet` bigint(20) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `factions`
--

CREATE TABLE `factions` (
  `id` smallint(5) UNSIGNED NOT NULL,
  `hidden` tinyint(4) DEFAULT NULL,
  `name` varchar(30) NOT NULL,
  `image` varchar(50) NULL,
  `description` text,
  `primary_resource_name` varchar(20) NOT NULL,
  `primary_resource_image` varchar(50) NULL,
  `secondary_resource_name` varchar(20) NOT NULL,
  `secondary_resource_image` varchar(50) NULL,
  `energy_name` varchar(20) NOT NULL,
  `energy_image` varchar(50) NULL,
  `initial_primary_resource` mediumint(8) UNSIGNED NOT NULL,
  `initial_secondary_resource` mediumint(8) UNSIGNED NOT NULL,
  `initial_energy` mediumint(8) UNSIGNED NOT NULL,
  `primary_resource_production` float NOT NULL COMMENT 'Per minut',
  `secondary_resource_production` float NOT NULL COMMENT 'Per minut',
  `max_planets` tinyint(3) UNSIGNED NOT NULL COMMENT 'Max number of planets',
  `improvement_id` smallint(6) UNSIGNED DEFAULT NULL,
  `cloned_improvements` tinyint(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `galaxies`
--

CREATE TABLE `galaxies` (
  `id` smallint(6) UNSIGNED NOT NULL,
  `name` varchar(30) CHARACTER SET utf8 NOT NULL,
  `sectors` int(11) UNSIGNED NOT NULL,
  `quadrants` int(11) UNSIGNED NOT NULL,
  `order_number` smallint(6) UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=latin1 COLLATE=latin1_spanish_ci;

-- --------------------------------------------------------

--
-- Structure de la table `improvements`
--

CREATE TABLE `improvements` (
  `id` smallint(6) UNSIGNED NOT NULL,
  `more_soldiers_production` smallint(6) DEFAULT NULL,
  `more_primary_resource_production` smallint(6) DEFAULT NULL,
  `more_secondary_resource_production` smallint(6) DEFAULT NULL,
  `more_energy_production` smallint(6) DEFAULT NULL,
  `more_charge_capacity` smallint(6) DEFAULT NULL,
  `more_missions_value` tinyint(4) DEFAULT NULL,
  `more_upgrade_research_speed` float UNSIGNED DEFAULT NULL,
  `more_unit_build_speed` float UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `improvements_unit_types`
--

CREATE TABLE `improvements_unit_types` (
  `id` smallint(5) UNSIGNED NOT NULL,
  `improvement_id` smallint(6) UNSIGNED NOT NULL,
  `type` enum('ATTACK','DEFENSE','SHIELD','AMOUNT') NOT NULL,
  `unit_type_id` smallint(6) UNSIGNED NOT NULL,
  `value` int(11) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='upgrades_unit_types';

-- --------------------------------------------------------

--
-- Structure de la table `mensajes`
--

CREATE TABLE `mensajes` (
  `cd` int(11) NOT NULL,
  `TipoMision` int(11) NOT NULL COMMENT '0 para ninguno,1 para Exploración, 2 para recolección, 3 para ataque',
  `Tiempo` int(11) NOT NULL,
  `Titulo` char(255) NOT NULL,
  `Contenido` text NOT NULL,
  `Destinatarios` text NOT NULL COMMENT 'En este caso se empieza por ,',
  `Destino` int(7) NOT NULL,
  `Carpetacd` int(3) NOT NULL,
  `Leido` tinyint(1) NOT NULL,
  `Notificado` int(4) NOT NULL DEFAULT '0',
  `Enviador` int(7) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `missions`
--

CREATE TABLE `missions` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `user_id` int(11) UNSIGNED DEFAULT NULL COMMENT 'If null is a core mission!',
  `type` smallint(5) UNSIGNED NOT NULL,
  `termination_date` datetime DEFAULT NULL,
  `required_time` double DEFAULT NULL,
  `primary_resource` double DEFAULT NULL,
  `secondary_resource` double DEFAULT NULL,
  `required_energy` double DEFAULT NULL,
  `source_planet` bigint(20) DEFAULT NULL,
  `target_planet` bigint(20) DEFAULT NULL,
  `related_mission` bigint(20) UNSIGNED DEFAULT NULL,
  `report_id` bigint(20) UNSIGNED DEFAULT NULL,
  `attemps` tinyint(3) UNSIGNED NOT NULL DEFAULT '1',
  `resolved` tinyint(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `mission_information`
--

CREATE TABLE `mission_information` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `mission_id` bigint(20) UNSIGNED NOT NULL,
  `relation_id` smallint(5) UNSIGNED DEFAULT NULL COMMENT 'Represents the relation id if applicable',
  `value` double DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Some missions may require having some information about the mission itself.  for example the level up upgrade mission needs the relation id';

-- --------------------------------------------------------

--
-- Structure de la table `mission_reports`
--

CREATE TABLE `mission_reports` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `json_body` mediumtext NOT NULL,
  `user_id` int(11) UNSIGNED NOT NULL,
  `report_date` datetime DEFAULT NULL,
  `user_aware_date` datetime DEFAULT NULL,
  `user_read_date` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `mission_types`
--

CREATE TABLE `mission_types` (
  `id` smallint(5) UNSIGNED NOT NULL,
  `code` varchar(50) NOT NULL,
  `description` varchar(200) DEFAULT NULL,
  `is_shared` tinyint(4) NOT NULL COMMENT 'If true will use the shared handling thread, instead of a dedicated one'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `objects`
--

CREATE TABLE `objects` (
  `description` varchar(12) NOT NULL,
  `repository` varchar(100) NOT NULL COMMENT 'Spring Data Repository related to this object'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Used to match objects with requirements';

-- --------------------------------------------------------

--
-- Structure de la table `object_relations`
--

CREATE TABLE `object_relations` (
  `id` smallint(6) UNSIGNED NOT NULL,
  `object_description` varchar(12) NOT NULL,
  `reference_id` smallint(6) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Has the mapping between objects table and the referenced tb';

-- --------------------------------------------------------

--
-- Structure de la table `obtained_units`
--

CREATE TABLE `obtained_units` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `user_id` int(11) UNSIGNED NOT NULL,
  `unit_id` smallint(6) UNSIGNED NOT NULL,
  `count` bigint(20) UNSIGNED NOT NULL,
  `source_planet` bigint(20) UNSIGNED DEFAULT NULL,
  `target_planet` bigint(20) UNSIGNED DEFAULT NULL,
  `mission_id` bigint(20) UNSIGNED DEFAULT NULL,
  `expiration` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `obtained_upgrades`
--

CREATE TABLE `obtained_upgrades` (
  `id` int(11) UNSIGNED NOT NULL,
  `user_id` int(11) UNSIGNED NOT NULL,
  `upgrade_id` smallint(6) UNSIGNED NOT NULL,
  `level` smallint(6) NOT NULL,
  `available` tinyint(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `planets`
--

CREATE TABLE `planets` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `name` varchar(20) NOT NULL,
  `galaxy_id` smallint(6) UNSIGNED NOT NULL,
  `sector` int(11) UNSIGNED NOT NULL,
  `quadrant` int(11) UNSIGNED NOT NULL,
  `planet_number` smallint(6) UNSIGNED NOT NULL,
  `owner` int(11) UNSIGNED DEFAULT NULL,
  `richness` smallint(6) UNSIGNED NOT NULL,
  `home` tinyint(4) DEFAULT '0',
  `special_location_id` smallint(5) UNSIGNED DEFAULT NULL
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
  `FIRED_TIME` bigint(13) NOT NULL,
  `SCHED_TIME` bigint(13) NOT NULL,
  `PRIORITY` int(11) NOT NULL,
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
  `LAST_CHECKIN_TIME` bigint(13) NOT NULL,
  `CHECKIN_INTERVAL` bigint(13) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `qrtz_simple_triggers`
--

CREATE TABLE `qrtz_simple_triggers` (
  `SCHED_NAME` varchar(120) NOT NULL,
  `TRIGGER_NAME` varchar(200) NOT NULL,
  `TRIGGER_GROUP` varchar(200) NOT NULL,
  `REPEAT_COUNT` bigint(7) NOT NULL,
  `REPEAT_INTERVAL` bigint(12) NOT NULL,
  `TIMES_TRIGGERED` bigint(10) NOT NULL
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
  `INT_PROP_1` int(11) DEFAULT NULL,
  `INT_PROP_2` int(11) DEFAULT NULL,
  `LONG_PROP_1` bigint(20) DEFAULT NULL,
  `LONG_PROP_2` bigint(20) DEFAULT NULL,
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
  `NEXT_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PREV_FIRE_TIME` bigint(13) DEFAULT NULL,
  `PRIORITY` int(11) DEFAULT NULL,
  `TRIGGER_STATE` varchar(16) NOT NULL,
  `TRIGGER_TYPE` varchar(8) NOT NULL,
  `START_TIME` bigint(13) NOT NULL,
  `END_TIME` bigint(13) DEFAULT NULL,
  `CALENDAR_NAME` varchar(200) DEFAULT NULL,
  `MISFIRE_INSTR` smallint(2) DEFAULT NULL,
  `JOB_DATA` blob
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `ranking`
--

CREATE TABLE `ranking` (
  `PosicionTotal` int(5) NOT NULL,
  `PosicionMejoras` int(4) NOT NULL,
  `PosicionTropas` int(4) NOT NULL,
  `PosicionNaves` int(4) NOT NULL,
  `PosicionDefensas` int(4) NOT NULL,
  `usercd` int(7) NOT NULL,
  `PuntosTotales` int(11) NOT NULL,
  `PuntosMejoras` int(11) NOT NULL,
  `PuntosTropas` int(11) NOT NULL,
  `PuntosNaves` int(11) NOT NULL,
  `PuntosDefensas` int(11) NOT NULL,
  `Imagen320x240` char(255) NOT NULL,
  `Imagen640x240` char(255) NOT NULL
) ENGINE=MEMORY DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Structure de la table `requirements`
--

CREATE TABLE `requirements` (
  `id` smallint(6) NOT NULL,
  `code` varchar(22) NOT NULL,
  `description` varchar(50) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `requirements_information`
--

CREATE TABLE `requirements_information` (
  `id` smallint(6) NOT NULL,
  `relation_id` smallint(6) UNSIGNED NOT NULL,
  `requirement_id` smallint(6) NOT NULL,
  `second_value` int(11) DEFAULT NULL,
  `third_value` int(11) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Stores which object has which requirement';

-- --------------------------------------------------------

--
-- Structure de la table `requisitosespecialesderaza`
--

CREATE TABLE `requisitosespecialesderaza` (
  `cdEspecial` int(4) NOT NULL,
  `mcd` int(4) NOT NULL,
  `Nivel` int(2) NOT NULL
) ENGINE=MyISAM DEFAULT CHARSET=latin1;

-- --------------------------------------------------------

--
-- Structure de la table `special_locations`
--

CREATE TABLE `special_locations` (
  `id` smallint(6) UNSIGNED NOT NULL,
  `name` varchar(30) NOT NULL,
  `image` char(36) DEFAULT NULL,
  `description` text NOT NULL,
  `galaxy_id` smallint(6) UNSIGNED NOT NULL,
  `planet_id` bigint(20) UNSIGNED DEFAULT NULL,
  `improvement_id` smallint(6) UNSIGNED NOT NULL,
  `cloned_improvements` tinyint(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `speed_impact_groups`
--

CREATE TABLE `speed_impact_groups` (
  `id` smallint(5) UNSIGNED NOT NULL,
  `name` varchar(50) NOT NULL,
  `mission_explore` double NOT NULL,
  `mission_gather` double NOT NULL,
  `mission_establish_base` double NOT NULL,
  `mission_attack` double NOT NULL,
  `mission_conquest` double NOT NULL,
  `mission_counterattack` double NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `units`
--

CREATE TABLE `units` (
  `id` smallint(6) UNSIGNED NOT NULL,
  `order_number` smallint(6) UNSIGNED DEFAULT NULL COMMENT 'El orden de la unidad',
  `name` char(40) CHARACTER SET latin1 COLLATE latin1_spanish_ci NOT NULL,
  `image` char(36) DEFAULT NULL,
  `points` int(11) UNSIGNED DEFAULT NULL,
  `description` text CHARACTER SET latin1 COLLATE latin1_spanish_ci,
  `time` int(11) DEFAULT NULL COMMENT 'El tiempo base para la mejora, en segundos',
  `primary_resource` int(11) UNSIGNED DEFAULT NULL,
  `secondary_resource` int(11) UNSIGNED DEFAULT NULL,
  `energy` smallint(6) UNSIGNED DEFAULT '0',
  `type` smallint(6) UNSIGNED DEFAULT NULL,
  `attack` smallint(6) UNSIGNED DEFAULT NULL,
  `health` smallint(6) UNSIGNED DEFAULT NULL,
  `shield` smallint(6) UNSIGNED DEFAULT NULL,
  `charge` smallint(6) UNSIGNED DEFAULT NULL,
  `is_unique` tinyint(3) UNSIGNED NOT NULL DEFAULT '0',
  `improvement_id` smallint(6) UNSIGNED NOT NULL,
  `cloned_improvements` tinyint(4) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `unit_types`
--

CREATE TABLE `unit_types` (
  `id` smallint(6) UNSIGNED NOT NULL,
  `name` varchar(20) NOT NULL,
  `max_count` bigint(20) DEFAULT NULL,
  `image` char(36) DEFAULT NULL,
  `parent_type` smallint(11) DEFAULT NULL,
  `can_explore` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_gather` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_establish_base` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_attack` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_counterattack` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_conquest` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY',
  `can_deploy` enum('NONE','OWNED_ONLY','ANY') NOT NULL DEFAULT 'ANY'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `unlocked_relation`
--

CREATE TABLE `unlocked_relation` (
  `id` bigint(20) NOT NULL,
  `user_id` int(11) UNSIGNED NOT NULL,
  `relation_id` smallint(6) UNSIGNED NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Any row here, means the user can use such thing';

-- --------------------------------------------------------

--
-- Structure de la table `upgrades`
--

CREATE TABLE `upgrades` (
  `id` smallint(6) UNSIGNED NOT NULL,
  `name` varchar(70) NOT NULL,
  `points` int(11) NOT NULL DEFAULT '0',
  `image` varchar(100) DEFAULT NULL,
  `description` text,
  `time` int(11) NOT NULL DEFAULT '60',
  `primary_resource` int(11) NOT NULL DEFAULT '100',
  `secondary_resource` int(11) NOT NULL DEFAULT '100',
  `type` smallint(6) UNSIGNED DEFAULT NULL COMMENT 'Null means invisible',
  `level_effect` float NOT NULL DEFAULT '20',
  `improvement_id` smallint(6) UNSIGNED DEFAULT NULL,
  `cloned_improvements` tinyint(1) NOT NULL COMMENT 'If improvements are cloned from other upgrade'
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `upgrade_types`
--

CREATE TABLE `upgrade_types` (
  `id` smallint(6) UNSIGNED NOT NULL,
  `name` varchar(20) NOT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `user_improvements`
--

CREATE TABLE `user_improvements` (
  `id` smallint(6) UNSIGNED NOT NULL,
  `user_id` int(10) UNSIGNED NOT NULL,
  `more_soldiers_production` smallint(6) DEFAULT NULL,
  `more_primary_resource_production` smallint(6) DEFAULT NULL,
  `more_secondary_resource_production` smallint(6) DEFAULT NULL,
  `more_energy_production` smallint(6) DEFAULT NULL,
  `more_charge_capacity` smallint(6) DEFAULT NULL,
  `more_missions_value` tinyint(4) DEFAULT NULL,
  `more_upgrade_research_speed` float UNSIGNED DEFAULT NULL,
  `more_unit_build_speed` float UNSIGNED DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

-- --------------------------------------------------------

--
-- Structure de la table `user_storage`
--

CREATE TABLE `user_storage` (
  `id` int(11) UNSIGNED NOT NULL COMMENT 'The id of the user as defined in the other database, no autoincremental, not even need to check it',
  `username` varchar(32) NOT NULL,
  `email` varchar(254) NOT NULL,
  `alliance_id` smallint(5) UNSIGNED DEFAULT NULL,
  `faction` smallint(5) UNSIGNED NOT NULL,
  `last_action` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `home_planet` bigint(20) UNSIGNED NOT NULL,
  `primary_resource` double UNSIGNED DEFAULT NULL,
  `secondary_resource` double UNSIGNED DEFAULT NULL,
  `energy` double UNSIGNED NOT NULL,
  `primary_resource_generation_per_second` double UNSIGNED DEFAULT NULL,
  `secondary_resource_generation_per_second` double UNSIGNED DEFAULT NULL,
  `max_energy` double UNSIGNED DEFAULT NULL,
  `points` double NOT NULL DEFAULT '0'
) ENGINE=InnoDB DEFAULT CHARSET=utf8 COMMENT='Has the users that has inscribed in this database';

-- --------------------------------------------------------

--
-- Structure de la table `websocket_messages_status`
--

CREATE TABLE `websocket_messages_status` (
  `id` bigint(20) UNSIGNED NOT NULL,
  `user_id` int(11) UNSIGNED DEFAULT NULL,
  `event_name` varchar(100) NOT NULL,
  `unwhiling_to_delivery` tinyint(4) NOT NULL,
  `socket_server_ack` tinyint(4) NOT NULL,
  `socket_not_found` tinyint(4) NOT NULL,
  `web_browser_ack` tinytext NOT NULL,
  `is_user_ack_required` tinytext NOT NULL,
  `user_ack` datetime DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=ascii;

--
-- Index pour les tables déchargées
--

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
-- Index pour la table `aranking`
--
ALTER TABLE `aranking`
  ADD KEY `PosicionMejoras` (`PosicionMejoras`);

--
-- Index pour la table `carpetas`
--
ALTER TABLE `carpetas`
  ADD PRIMARY KEY (`cd`);

--
-- Index pour la table `configuration`
--
ALTER TABLE `configuration`
  ADD PRIMARY KEY (`name`);

--
-- Index pour la table `especialesderaza`
--
ALTER TABLE `especialesderaza`
  ADD PRIMARY KEY (`cd`);

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
  ADD KEY `improvement_id` (`improvement_id`);

--
-- Index pour la table `galaxies`
--
ALTER TABLE `galaxies`
  ADD PRIMARY KEY (`id`);

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
-- Index pour la table `mensajes`
--
ALTER TABLE `mensajes`
  ADD PRIMARY KEY (`cd`),
  ADD KEY `Destino` (`Destino`);

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
-- Index pour la table `obtained_units`
--
ALTER TABLE `obtained_units`
  ADD PRIMARY KEY (`id`),
  ADD KEY `user_id` (`user_id`);

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
-- Index pour la table `requisitosespecialesderaza`
--
ALTER TABLE `requisitosespecialesderaza`
  ADD KEY `NoPrimaria` (`cdEspecial`);

--
-- Index pour la table `special_locations`
--
ALTER TABLE `special_locations`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`),
  ADD KEY `planet_id` (`planet_id`),
  ADD KEY `improvement_id` (`improvement_id`),
  ADD KEY `galaxy_id` (`galaxy_id`);

--
-- Index pour la table `speed_impact_groups`
--
ALTER TABLE `speed_impact_groups`
  ADD PRIMARY KEY (`id`);

--
-- Index pour la table `units`
--
ALTER TABLE `units`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`),
  ADD KEY `type` (`type`),
  ADD KEY `improvement_id` (`improvement_id`);

--
-- Index pour la table `unit_types`
--
ALTER TABLE `unit_types`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`);

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
  ADD KEY `improvements_id` (`improvement_id`);

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
-- Index pour la table `user_storage`
--
ALTER TABLE `user_storage`
  ADD PRIMARY KEY (`id`),
  ADD KEY `faction` (`faction`),
  ADD KEY `home_planet` (`home_planet`),
  ADD KEY `alliance_id` (`alliance_id`);

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
-- AUTO_INCREMENT pour la table `admin_users`
--
ALTER TABLE `admin_users`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2;

--
-- AUTO_INCREMENT pour la table `alliances`
--
ALTER TABLE `alliances`
  MODIFY `id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `alliance_join_request`
--
ALTER TABLE `alliance_join_request`
  MODIFY `id` int(10) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=5;

--
-- AUTO_INCREMENT pour la table `carpetas`
--
ALTER TABLE `carpetas`
  MODIFY `cd` int(11) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=12;

--
-- AUTO_INCREMENT pour la table `especialesderaza`
--
ALTER TABLE `especialesderaza`
  MODIFY `cd` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `explored_planets`
--
ALTER TABLE `explored_planets`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2188;

--
-- AUTO_INCREMENT pour la table `factions`
--
ALTER TABLE `factions`
  MODIFY `id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=35;

--
-- AUTO_INCREMENT pour la table `galaxies`
--
ALTER TABLE `galaxies`
  MODIFY `id` smallint(6) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=4;

--
-- AUTO_INCREMENT pour la table `improvements`
--
ALTER TABLE `improvements`
  MODIFY `id` smallint(6) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=734;

--
-- AUTO_INCREMENT pour la table `improvements_unit_types`
--
ALTER TABLE `improvements_unit_types`
  MODIFY `id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=431;

--
-- AUTO_INCREMENT pour la table `mensajes`
--
ALTER TABLE `mensajes`
  MODIFY `cd` int(11) NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `missions`
--
ALTER TABLE `missions`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=6814;

--
-- AUTO_INCREMENT pour la table `mission_information`
--
ALTER TABLE `mission_information`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=34;

--
-- AUTO_INCREMENT pour la table `mission_reports`
--
ALTER TABLE `mission_reports`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2914;

--
-- AUTO_INCREMENT pour la table `mission_types`
--
ALTER TABLE `mission_types`
  MODIFY `id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=13;

--
-- AUTO_INCREMENT pour la table `object_relations`
--
ALTER TABLE `object_relations`
  MODIFY `id` smallint(6) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=733;

--
-- AUTO_INCREMENT pour la table `obtained_units`
--
ALTER TABLE `obtained_units`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3638;

--
-- AUTO_INCREMENT pour la table `obtained_upgrades`
--
ALTER TABLE `obtained_upgrades`
  MODIFY `id` int(11) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=181;

--
-- AUTO_INCREMENT pour la table `planets`
--
ALTER TABLE `planets`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=3381;

--
-- AUTO_INCREMENT pour la table `requirements`
--
ALTER TABLE `requirements`
  MODIFY `id` smallint(6) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT pour la table `requirements_information`
--
ALTER TABLE `requirements_information`
  MODIFY `id` smallint(6) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=2352;

--
-- AUTO_INCREMENT pour la table `special_locations`
--
ALTER TABLE `special_locations`
  MODIFY `id` smallint(6) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `speed_impact_groups`
--
ALTER TABLE `speed_impact_groups`
  MODIFY `id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `units`
--
ALTER TABLE `units`
  MODIFY `id` smallint(6) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=374;

--
-- AUTO_INCREMENT pour la table `unit_types`
--
ALTER TABLE `unit_types`
  MODIFY `id` smallint(6) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=7;

--
-- AUTO_INCREMENT pour la table `unlocked_relation`
--
ALTER TABLE `unlocked_relation`
  MODIFY `id` bigint(20) NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=237;

--
-- AUTO_INCREMENT pour la table `upgrades`
--
ALTER TABLE `upgrades`
  MODIFY `id` smallint(6) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=412;

--
-- AUTO_INCREMENT pour la table `upgrade_types`
--
ALTER TABLE `upgrade_types`
  MODIFY `id` smallint(6) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=10;

--
-- AUTO_INCREMENT pour la table `user_improvements`
--
ALTER TABLE `user_improvements`
  MODIFY `id` smallint(6) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=16;

--
-- AUTO_INCREMENT pour la table `websocket_messages_status`
--
ALTER TABLE `websocket_messages_status`
  MODIFY `id` bigint(20) UNSIGNED NOT NULL AUTO_INCREMENT, AUTO_INCREMENT=8577;

--
-- Contraintes pour les tables déchargées
--

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
-- Contraintes pour la table `explored_planets`
--
ALTER TABLE `explored_planets`
  ADD CONSTRAINT `explored_planets_ibfk_1` FOREIGN KEY (`planet`) REFERENCES `planets` (`id`),
  ADD CONSTRAINT `explored_planets_ibfk_2` FOREIGN KEY (`user`) REFERENCES `user_storage` (`id`);

--
-- Contraintes pour la table `factions`
--
ALTER TABLE `factions`
  ADD CONSTRAINT `factions_ibfk_1` FOREIGN KEY (`improvement_id`) REFERENCES `improvements` (`id`);

--
-- Contraintes pour la table `improvements_unit_types`
--
ALTER TABLE `improvements_unit_types`
  ADD CONSTRAINT `improvements_unit_types_ibfk_1` FOREIGN KEY (`improvement_id`) REFERENCES `improvements` (`id`),
  ADD CONSTRAINT `improvements_unit_types_ibfk_2` FOREIGN KEY (`unit_type_id`) REFERENCES `unit_types` (`id`);

--
-- Contraintes pour la table `missions`
--
ALTER TABLE `missions`
  ADD CONSTRAINT `missions_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`),
  ADD CONSTRAINT `missions_ibfk_2` FOREIGN KEY (`type`) REFERENCES `mission_types` (`id`),
  ADD CONSTRAINT `missions_ibfk_3` FOREIGN KEY (`related_mission`) REFERENCES `missions` (`id`),
  ADD CONSTRAINT `missions_ibfk_4` FOREIGN KEY (`report_id`) REFERENCES `mission_reports` (`id`);

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
-- Contraintes pour la table `qrtz_blob_triggers`
--
ALTER TABLE `qrtz_blob_triggers`
  ADD CONSTRAINT `qrtz_blob_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`);

--
-- Contraintes pour la table `qrtz_cron_triggers`
--
ALTER TABLE `qrtz_cron_triggers`
  ADD CONSTRAINT `qrtz_cron_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`);

--
-- Contraintes pour la table `qrtz_simple_triggers`
--
ALTER TABLE `qrtz_simple_triggers`
  ADD CONSTRAINT `qrtz_simple_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`);

--
-- Contraintes pour la table `qrtz_simprop_triggers`
--
ALTER TABLE `qrtz_simprop_triggers`
  ADD CONSTRAINT `qrtz_simprop_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`,`TRIGGER_NAME`,`TRIGGER_GROUP`) REFERENCES `qrtz_triggers` (`SCHED_NAME`, `TRIGGER_NAME`, `TRIGGER_GROUP`);

--
-- Contraintes pour la table `qrtz_triggers`
--
ALTER TABLE `qrtz_triggers`
  ADD CONSTRAINT `qrtz_triggers_ibfk_1` FOREIGN KEY (`SCHED_NAME`,`JOB_NAME`,`JOB_GROUP`) REFERENCES `qrtz_job_details` (`SCHED_NAME`, `JOB_NAME`, `JOB_GROUP`);

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
  ADD CONSTRAINT `special_locations_ibfk_1` FOREIGN KEY (`planet_id`) REFERENCES `planets` (`id`),
  ADD CONSTRAINT `special_locations_ibfk_2` FOREIGN KEY (`improvement_id`) REFERENCES `improvements` (`id`),
  ADD CONSTRAINT `special_locations_ibfk_3` FOREIGN KEY (`galaxy_id`) REFERENCES `galaxies` (`id`);

--
-- Contraintes pour la table `units`
--
ALTER TABLE `units`
  ADD CONSTRAINT `units_ibfk_1` FOREIGN KEY (`type`) REFERENCES `unit_types` (`id`),
  ADD CONSTRAINT `units_ibfk_2` FOREIGN KEY (`improvement_id`) REFERENCES `improvements` (`id`);

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
  ADD CONSTRAINT `upgrades_ibfk_2` FOREIGN KEY (`improvement_id`) REFERENCES `improvements` (`id`);

--
-- Contraintes pour la table `user_storage`
--
ALTER TABLE `user_storage`
  ADD CONSTRAINT `user_storage_ibfk_1` FOREIGN KEY (`home_planet`) REFERENCES `planets` (`id`),
  ADD CONSTRAINT `user_storage_ibfk_2` FOREIGN KEY (`faction`) REFERENCES `factions` (`id`),
  ADD CONSTRAINT `user_storage_ibfk_3` FOREIGN KEY (`alliance_id`) REFERENCES `alliances` (`id`);

--
-- Contraintes pour la table `websocket_messages_status`
--
ALTER TABLE `websocket_messages_status`
  ADD CONSTRAINT `websocket_messages_status_ibfk_1` FOREIGN KEY (`user_id`) REFERENCES `user_storage` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
