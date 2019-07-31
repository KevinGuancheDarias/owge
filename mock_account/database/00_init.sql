-- phpMyAdmin SQL Dump
-- version 4.9.0.1
-- https://www.phpmyadmin.net/
--
-- Hôte : 192.168.99.1
-- Généré le :  lun. 15 juil. 2019 à 09:22
-- Version du serveur :  5.7.19-log
-- Version de PHP :  7.2.19

CREATE DATABASE `owge_account`;
USE `owge_account`;

SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données :  `owge_account`
--

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

--
-- Déchargement des données de la table `configuration`
--

INSERT INTO `configuration` (`name`, `display_name`, `value`, `privileged`) VALUES
('JWT_ALGO', NULL, 'HS256', 1),
('JWT_DURATION_SECONDS', NULL, '604800', 1),
('JWT_SECRET', NULL, 'REALLY_LONG_SECRET_THAT_I_USE_WHEN_IM_BORED', 1),
('SYSTEM_EMAIL', NULL, 'owge-system@kevinguanchedarias.com', 1),
('SYSTEM_PASSWORD', NULL, '1234', 1);

-- --------------------------------------------------------

--
-- Structure de la table `universes`
--

CREATE TABLE `universes` (
  `id` smallint(5) UNSIGNED NOT NULL,
  `name` varchar(50) NOT NULL,
  `description` varchar(500) DEFAULT NULL,
  `creator` int(10) UNSIGNED DEFAULT NULL,
  `creation_date` datetime NOT NULL,
  `public` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Can be acceded by anyone',
  `official` tinyint(1) NOT NULL DEFAULT '0' COMMENT 'Should be listed after the login form',
  `target_database` varchar(50) NOT NULL,
  `rest_base_url` varchar(200) NOT NULL COMMENT 'full or relative (accounts base) URL path to universe',
  `frontend_url` varchar(200) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Déchargement des données de la table `configuration`
--

INSERT INTO `universes` (`id`,`name`, `description`,`creation_date`, `public`, `official`, `target_database`, `rest_base_url`, `frontend_url`) VALUES
(0, 'OWGE Mock Universe', 'It is a mock universe, intended for development', '2019-07-15', 1, 1, 'not_used', '/game_api/', '/');

-- --------------------------------------------------------

--
-- Structure de la table `users`
--

CREATE TABLE `users` (
  `id` int(110) UNSIGNED NOT NULL,
  `username` varchar(32) NOT NULL,
  `email` varchar(254) NOT NULL,
  `password` char(60) NOT NULL,
  `activated` tinyint(1) DEFAULT NULL,
  `creation_date` datetime NOT NULL,
  `last_login` datetime NOT NULL,
  `first_name` varchar(50) DEFAULT NULL,
  `last_name` varchar(50) DEFAULT NULL,
  `notifications` tinyint(1) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8;

--
-- Index pour les tables déchargées
--

--
-- Index pour la table `configuration`
--
ALTER TABLE `configuration`
  ADD PRIMARY KEY (`name`);

--
-- Index pour la table `universes`
--
ALTER TABLE `universes`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `name` (`name`),
  ADD KEY `creator` (`creator`);

--
-- Index pour la table `users`
--
ALTER TABLE `users`
  ADD PRIMARY KEY (`id`),
  ADD UNIQUE KEY `email` (`email`),
  ADD UNIQUE KEY `username` (`username`);

--
-- AUTO_INCREMENT pour les tables déchargées
--

--
-- AUTO_INCREMENT pour la table `universes`
--
ALTER TABLE `universes`
  MODIFY `id` smallint(5) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- AUTO_INCREMENT pour la table `users`
--
ALTER TABLE `users`
  MODIFY `id` int(110) UNSIGNED NOT NULL AUTO_INCREMENT;

--
-- Contraintes pour les tables déchargées
--

--
-- Contraintes pour la table `universes`
--
ALTER TABLE `universes`
  ADD CONSTRAINT `universes_ibfk_1` FOREIGN KEY (`creator`) REFERENCES `users` (`id`);
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
