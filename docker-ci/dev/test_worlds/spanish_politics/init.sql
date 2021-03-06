-- phpMyAdmin SQL Dump
-- version 4.9.0.1
-- https://www.phpmyadmin.net/
--
-- Hôte : db:3306
-- Généré le :  sam. 27 juil. 2019 à 17:41
-- Version du serveur :  5.7.27
-- Version de PHP :  7.2.19

SET FOREIGN_KEY_CHECKS=0;
SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
SET AUTOCOMMIT = 0;
START TRANSACTION;
SET time_zone = "+00:00";


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8mb4 */;

--
-- Base de données :  `owge`
--

--
-- Déchargement des données de la table `factions`
--

INSERT INTO `factions` (`id`, `hidden`, `name`, `image`, `description`, `primary_resource_name`, `primary_resource_image`, `secondary_resource_name`, `secondary_resource_image`, `energy_name`, `energy_image`, `initial_primary_resource`, `initial_secondary_resource`, `initial_energy`, `primary_resource_production`, `secondary_resource_production`, `max_planets`, `improvement_id`, `cloned_improvements`) VALUES
(1, 0, 'PP', '3bc2b9bc23bbe5b135bef93da31c5ad0.png', 'El PP es un partido de \"ultra\" derecha conservador, y son todos mala gente\r\n- Alguien de izquierdas', 'Euros', 'bd23e87e6ba4b3b024f00efe4a7d048a.jpg', 'Sobres', '8411cee8676e82eae383a3e60980fcd7.jpg', 'Endesa', NULL, 100, 100, 500, 0.001, 0.001, 1, 1, 0),
(2, 0, 'PSOE', '47852152d9b3620bd9794987b9eb8e0a.png', 'El PSOE es el partido de los ERES y de los impuestos.\r\n\r\n- Alguien del PP', 'Euros', 'bd23e87e6ba4b3b024f00efe4a7d048a.jpg', 'Prostitutas', '6fc79d354ef684334c4155fc20aa2f50.png', 'Placas Solares', NULL, 200, 200, 300, 0.002, 0.002, 2, 2, 0),
(3, 1, 'Unidas Podemos ', 'c0266b4e6d15630e99d04c02084f5d3d.png', 'Los perroflautas de podemos son personas que no han trabajado en la vida\r\n\r\n- Un facha (o eso dicen)', 'Euros', NULL, 'Okupas', NULL, 'Subvenciones', NULL, 50, 50, 750, 0.001, 0.001, 1, 3, 0),
(4, 1, 'Ciudadanos', '7045d18b489a1320be37a2f905a72aaa.jpg', 'Los de Ciudadanos son el partido de la veleta... son de izquierdas o de derechas según donde sople el viento', 'Euros', NULL, 'Veleros', NULL, 'Viento', NULL, 150, 150, 450, 0.0005, 0.0005, 4, 4, 0),
(5, 1, 'Vox', '92f1141e5f76d29fe0e7329e0a1538b7.png', 'Vox es la ultraderecha, son fachas y homófobos\r\n\r\n- Un medio de comunicación cualquiera', 'Euros', NULL, 'Patriotas', NULL, 'Endesa', NULL, 100, 100, 500, 0.001, 0.001, 2, 5, 0);

--
-- Déchargement des données de la table `galaxies`
--

INSERT INTO `galaxies` (`id`, `name`, `sectors`, `quadrants`, `order_number`) VALUES
(1, 'España', 5, 5, 0);

--
-- Déchargement des données de la table `improvements`
--

INSERT INTO `improvements` (`id`, `more_soldiers_production`, `more_primary_resource_production`, `more_secondary_resource_production`, `more_energy_production`, `more_charge_capacity`, `more_missions_value`, `more_upgrade_research_speed`, `more_unit_build_speed`) VALUES
(1, 0, 0, 0, 0, 0, 0, 0, 0),
(2, 0, 0, 0, 0, 0, 0, 0, 0),
(3, 0, 0, 0, 0, 0, 0, 0, 0),
(4, 0, 0, 0, 0, 0, 0, 0, 0),
(5, 0, 0, 0, 0, 0, 0, 0, 0),
(6, 0, 10, 10, 0, 0, 0, 0, 0),
(7, 0, 10, 10, 0, 0, 0, 0, 0),
(8, 0, 10, 10, 0, 0, 0, 0, 0),
(9, 0, 10, 10, 0, 0, 0, 0, 0),
(10, 0, 10, 10, 0, 0, 0, 0, 0),
(11, 0, 0, 0, 0, 0, 0, 0, 0),
(12, 0, 0, 0, 0, 0, 0, 10, 10),
(13, 0, 0, 0, 0, 0, 0, 0, 0),
(14, 0, 0, 0, 0, 0, 0, 0, 0);

--
-- Déchargement des données de la table `object_relations`
--

INSERT INTO `object_relations` (`id`, `object_description`, `reference_id`) VALUES
(6, 'UNIT', 1),
(7, 'UNIT', 2),
(8, 'UNIT', 3),
(9, 'UNIT', 4),
(1, 'UPGRADE', 1),
(2, 'UPGRADE', 2),
(3, 'UPGRADE', 3),
(4, 'UPGRADE', 4),
(5, 'UPGRADE', 5);

--
-- Déchargement des données de la table `planets`
--

INSERT INTO `planets` (`id`, `name`, `galaxy_id`, `sector`, `quadrant`, `planet_number`, `owner`, `richness`, `home`, `special_location_id`) VALUES
(1, 'ES1C1N1', 1, 1, 1, 1, NULL, 100, NULL, NULL),
(2, 'ES1C1N2', 1, 1, 1, 2, NULL, 30, NULL, NULL),
(3, 'ES1C1N3', 1, 1, 1, 3, NULL, 40, NULL, NULL),
(4, 'ES1C1N4', 1, 1, 1, 4, NULL, 100, NULL, NULL),
(5, 'ES1C1N5', 1, 1, 1, 5, NULL, 80, NULL, NULL),
(6, 'ES1C1N6', 1, 1, 1, 6, NULL, 60, NULL, NULL),
(7, 'ES1C1N7', 1, 1, 1, 7, NULL, 100, NULL, NULL),
(8, 'ES1C1N8', 1, 1, 1, 8, NULL, 10, NULL, NULL),
(9, 'ES1C1N9', 1, 1, 1, 9, NULL, 70, NULL, NULL),
(10, 'ES1C1N10', 1, 1, 1, 10, NULL, 50, NULL, NULL),
(11, 'ES1C1N11', 1, 1, 1, 11, NULL, 90, NULL, NULL),
(12, 'ES1C1N12', 1, 1, 1, 12, NULL, 90, NULL, NULL),
(13, 'ES1C1N13', 1, 1, 1, 13, NULL, 20, NULL, NULL),
(14, 'ES1C1N14', 1, 1, 1, 14, NULL, 80, NULL, NULL),
(15, 'ES1C1N15', 1, 1, 1, 15, NULL, 60, NULL, NULL),
(16, 'ES1C1N16', 1, 1, 1, 16, NULL, 60, NULL, NULL),
(17, 'ES1C1N17', 1, 1, 1, 17, NULL, 10, NULL, NULL),
(18, 'ES1C1N18', 1, 1, 1, 18, NULL, 50, NULL, NULL),
(19, 'ES1C1N19', 1, 1, 1, 19, NULL, 70, NULL, NULL),
(20, 'ES1C1N20', 1, 1, 1, 20, NULL, 60, NULL, NULL),
(21, 'ES1C2N1', 1, 1, 2, 1, NULL, 90, NULL, NULL),
(22, 'ES1C2N2', 1, 1, 2, 2, NULL, 30, NULL, NULL),
(23, 'ES1C2N3', 1, 1, 2, 3, NULL, 10, NULL, NULL),
(24, 'ES1C2N4', 1, 1, 2, 4, NULL, 40, NULL, NULL),
(25, 'ES1C2N5', 1, 1, 2, 5, NULL, 30, NULL, NULL),
(26, 'ES1C2N6', 1, 1, 2, 6, NULL, 80, NULL, NULL),
(27, 'ES1C2N7', 1, 1, 2, 7, NULL, 30, NULL, NULL),
(28, 'ES1C2N8', 1, 1, 2, 8, NULL, 100, NULL, NULL),
(29, 'ES1C2N9', 1, 1, 2, 9, NULL, 100, NULL, NULL),
(30, 'ES1C2N10', 1, 1, 2, 10, NULL, 10, NULL, NULL),
(31, 'ES1C2N11', 1, 1, 2, 11, NULL, 50, NULL, NULL),
(32, 'ES1C2N12', 1, 1, 2, 12, NULL, 50, NULL, NULL),
(33, 'ES1C2N13', 1, 1, 2, 13, NULL, 90, NULL, NULL),
(34, 'ES1C2N14', 1, 1, 2, 14, NULL, 60, NULL, NULL),
(35, 'ES1C2N15', 1, 1, 2, 15, NULL, 30, NULL, NULL),
(36, 'ES1C2N16', 1, 1, 2, 16, NULL, 10, NULL, NULL),
(37, 'ES1C2N17', 1, 1, 2, 17, NULL, 30, NULL, NULL),
(38, 'ES1C2N18', 1, 1, 2, 18, NULL, 90, NULL, NULL),
(39, 'ES1C2N19', 1, 1, 2, 19, NULL, 80, NULL, NULL),
(40, 'ES1C2N20', 1, 1, 2, 20, NULL, 90, NULL, NULL),
(41, 'ES1C3N1', 1, 1, 3, 1, NULL, 80, NULL, NULL),
(42, 'ES1C3N2', 1, 1, 3, 2, NULL, 60, NULL, NULL),
(43, 'ES1C3N3', 1, 1, 3, 3, NULL, 100, NULL, NULL),
(44, 'ES1C3N4', 1, 1, 3, 4, NULL, 70, NULL, NULL),
(45, 'ES1C3N5', 1, 1, 3, 5, NULL, 30, NULL, NULL),
(46, 'ES1C3N6', 1, 1, 3, 6, NULL, 90, NULL, NULL),
(47, 'ES1C3N7', 1, 1, 3, 7, NULL, 60, NULL, NULL),
(48, 'ES1C3N8', 1, 1, 3, 8, NULL, 40, NULL, NULL),
(49, 'ES1C3N9', 1, 1, 3, 9, NULL, 100, NULL, NULL),
(50, 'ES1C3N10', 1, 1, 3, 10, NULL, 100, NULL, NULL),
(51, 'ES1C3N11', 1, 1, 3, 11, NULL, 60, NULL, NULL),
(52, 'ES1C3N12', 1, 1, 3, 12, NULL, 80, NULL, NULL),
(53, 'ES1C3N13', 1, 1, 3, 13, NULL, 40, NULL, NULL),
(54, 'ES1C3N14', 1, 1, 3, 14, NULL, 20, NULL, NULL),
(55, 'ES1C3N15', 1, 1, 3, 15, NULL, 90, NULL, NULL),
(56, 'ES1C3N16', 1, 1, 3, 16, NULL, 50, NULL, NULL),
(57, 'ES1C3N17', 1, 1, 3, 17, NULL, 90, NULL, NULL),
(58, 'ES1C3N18', 1, 1, 3, 18, NULL, 60, NULL, NULL),
(59, 'ES1C3N19', 1, 1, 3, 19, NULL, 100, NULL, NULL),
(60, 'ES1C3N20', 1, 1, 3, 20, NULL, 90, NULL, NULL),
(61, 'ES1C4N1', 1, 1, 4, 1, NULL, 70, NULL, NULL),
(62, 'ES1C4N2', 1, 1, 4, 2, NULL, 50, NULL, NULL),
(63, 'ES1C4N3', 1, 1, 4, 3, NULL, 40, NULL, NULL),
(64, 'ES1C4N4', 1, 1, 4, 4, NULL, 10, NULL, NULL),
(65, 'ES1C4N5', 1, 1, 4, 5, NULL, 50, NULL, NULL),
(66, 'ES1C4N6', 1, 1, 4, 6, NULL, 20, NULL, NULL),
(67, 'ES1C4N7', 1, 1, 4, 7, NULL, 40, NULL, NULL),
(68, 'ES1C4N8', 1, 1, 4, 8, NULL, 80, NULL, NULL),
(69, 'ES1C4N9', 1, 1, 4, 9, NULL, 80, NULL, NULL),
(70, 'ES1C4N10', 1, 1, 4, 10, NULL, 20, NULL, NULL),
(71, 'ES1C4N11', 1, 1, 4, 11, NULL, 60, NULL, NULL),
(72, 'ES1C4N12', 1, 1, 4, 12, NULL, 100, NULL, NULL),
(73, 'ES1C4N13', 1, 1, 4, 13, NULL, 60, NULL, NULL),
(74, 'ES1C4N14', 1, 1, 4, 14, NULL, 20, NULL, NULL),
(75, 'ES1C4N15', 1, 1, 4, 15, NULL, 30, NULL, NULL),
(76, 'ES1C4N16', 1, 1, 4, 16, NULL, 70, NULL, NULL),
(77, 'ES1C4N17', 1, 1, 4, 17, NULL, 60, NULL, NULL),
(78, 'ES1C4N18', 1, 1, 4, 18, NULL, 80, NULL, NULL),
(79, 'ES1C4N19', 1, 1, 4, 19, NULL, 30, NULL, NULL),
(80, 'ES1C4N20', 1, 1, 4, 20, NULL, 100, NULL, NULL),
(81, 'ES1C5N1', 1, 1, 5, 1, NULL, 10, NULL, NULL),
(82, 'ES1C5N2', 1, 1, 5, 2, NULL, 40, NULL, NULL),
(83, 'ES1C5N3', 1, 1, 5, 3, NULL, 40, NULL, NULL),
(84, 'ES1C5N4', 1, 1, 5, 4, NULL, 100, NULL, NULL),
(85, 'ES1C5N5', 1, 1, 5, 5, NULL, 80, NULL, NULL),
(86, 'ES1C5N6', 1, 1, 5, 6, NULL, 70, NULL, NULL),
(87, 'ES1C5N7', 1, 1, 5, 7, NULL, 50, NULL, NULL),
(88, 'ES1C5N8', 1, 1, 5, 8, NULL, 40, NULL, NULL),
(89, 'ES1C5N9', 1, 1, 5, 9, NULL, 60, NULL, NULL),
(90, 'ES1C5N10', 1, 1, 5, 10, NULL, 70, NULL, NULL),
(91, 'ES1C5N11', 1, 1, 5, 11, NULL, 40, NULL, NULL),
(92, 'ES1C5N12', 1, 1, 5, 12, NULL, 10, NULL, NULL),
(93, 'ES1C5N13', 1, 1, 5, 13, NULL, 90, NULL, NULL),
(94, 'ES1C5N14', 1, 1, 5, 14, NULL, 90, NULL, NULL),
(95, 'ES1C5N15', 1, 1, 5, 15, NULL, 30, NULL, NULL),
(96, 'ES1C5N16', 1, 1, 5, 16, NULL, 30, NULL, NULL),
(97, 'ES1C5N17', 1, 1, 5, 17, NULL, 70, NULL, NULL),
(98, 'ES1C5N18', 1, 1, 5, 18, NULL, 90, NULL, NULL),
(99, 'ES1C5N19', 1, 1, 5, 19, NULL, 100, NULL, NULL),
(100, 'ES1C5N20', 1, 1, 5, 20, NULL, 90, NULL, NULL),
(101, 'ES2C1N1', 1, 2, 1, 1, NULL, 10, NULL, NULL),
(102, 'ES2C1N2', 1, 2, 1, 2, NULL, 20, NULL, NULL),
(103, 'ES2C1N3', 1, 2, 1, 3, NULL, 20, NULL, NULL),
(104, 'ES2C1N4', 1, 2, 1, 4, NULL, 30, NULL, NULL),
(105, 'ES2C1N5', 1, 2, 1, 5, NULL, 70, NULL, NULL),
(106, 'ES2C1N6', 1, 2, 1, 6, NULL, 100, NULL, NULL),
(107, 'ES2C1N7', 1, 2, 1, 7, NULL, 70, NULL, NULL),
(108, 'ES2C1N8', 1, 2, 1, 8, NULL, 90, NULL, NULL),
(109, 'ES2C1N9', 1, 2, 1, 9, NULL, 70, NULL, NULL),
(110, 'ES2C1N10', 1, 2, 1, 10, NULL, 50, NULL, NULL),
(111, 'ES2C1N11', 1, 2, 1, 11, NULL, 20, NULL, NULL),
(112, 'ES2C1N12', 1, 2, 1, 12, NULL, 50, NULL, NULL),
(113, 'ES2C1N13', 1, 2, 1, 13, NULL, 40, NULL, NULL),
(114, 'ES2C1N14', 1, 2, 1, 14, NULL, 60, NULL, NULL),
(115, 'ES2C1N15', 1, 2, 1, 15, NULL, 60, NULL, NULL),
(116, 'ES2C1N16', 1, 2, 1, 16, NULL, 40, NULL, NULL),
(117, 'ES2C1N17', 1, 2, 1, 17, NULL, 50, NULL, NULL),
(118, 'ES2C1N18', 1, 2, 1, 18, NULL, 40, NULL, NULL),
(119, 'ES2C1N19', 1, 2, 1, 19, NULL, 90, NULL, NULL),
(120, 'ES2C1N20', 1, 2, 1, 20, NULL, 100, NULL, NULL),
(121, 'ES2C2N1', 1, 2, 2, 1, NULL, 20, NULL, NULL),
(122, 'ES2C2N2', 1, 2, 2, 2, NULL, 80, NULL, NULL),
(123, 'ES2C2N3', 1, 2, 2, 3, NULL, 20, NULL, NULL),
(124, 'ES2C2N4', 1, 2, 2, 4, NULL, 10, NULL, NULL),
(125, 'ES2C2N5', 1, 2, 2, 5, NULL, 90, NULL, NULL),
(126, 'ES2C2N6', 1, 2, 2, 6, NULL, 20, NULL, NULL),
(127, 'ES2C2N7', 1, 2, 2, 7, NULL, 80, NULL, NULL),
(128, 'ES2C2N8', 1, 2, 2, 8, NULL, 60, NULL, NULL),
(129, 'ES2C2N9', 1, 2, 2, 9, NULL, 80, NULL, NULL),
(130, 'ES2C2N10', 1, 2, 2, 10, NULL, 10, NULL, NULL),
(131, 'ES2C2N11', 1, 2, 2, 11, NULL, 10, NULL, NULL),
(132, 'ES2C2N12', 1, 2, 2, 12, NULL, 50, NULL, NULL),
(133, 'ES2C2N13', 1, 2, 2, 13, NULL, 100, NULL, NULL),
(134, 'ES2C2N14', 1, 2, 2, 14, NULL, 50, NULL, NULL),
(135, 'ES2C2N15', 1, 2, 2, 15, NULL, 70, NULL, NULL),
(136, 'ES2C2N16', 1, 2, 2, 16, NULL, 60, NULL, NULL),
(137, 'ES2C2N17', 1, 2, 2, 17, NULL, 10, NULL, NULL),
(138, 'ES2C2N18', 1, 2, 2, 18, NULL, 30, NULL, NULL),
(139, 'ES2C2N19', 1, 2, 2, 19, NULL, 20, NULL, NULL),
(140, 'ES2C2N20', 1, 2, 2, 20, NULL, 20, NULL, NULL),
(141, 'ES2C3N1', 1, 2, 3, 1, NULL, 40, NULL, NULL),
(142, 'ES2C3N2', 1, 2, 3, 2, NULL, 10, NULL, NULL),
(143, 'ES2C3N3', 1, 2, 3, 3, NULL, 80, NULL, NULL),
(144, 'ES2C3N4', 1, 2, 3, 4, NULL, 30, NULL, NULL),
(145, 'ES2C3N5', 1, 2, 3, 5, NULL, 100, NULL, NULL),
(146, 'ES2C3N6', 1, 2, 3, 6, NULL, 60, NULL, NULL),
(147, 'ES2C3N7', 1, 2, 3, 7, NULL, 50, NULL, NULL),
(148, 'ES2C3N8', 1, 2, 3, 8, NULL, 90, NULL, NULL),
(149, 'ES2C3N9', 1, 2, 3, 9, NULL, 30, NULL, NULL),
(150, 'ES2C3N10', 1, 2, 3, 10, NULL, 90, NULL, NULL),
(151, 'ES2C3N11', 1, 2, 3, 11, NULL, 100, NULL, NULL),
(152, 'ES2C3N12', 1, 2, 3, 12, NULL, 70, NULL, NULL),
(153, 'ES2C3N13', 1, 2, 3, 13, NULL, 100, NULL, NULL),
(154, 'ES2C3N14', 1, 2, 3, 14, NULL, 70, NULL, NULL),
(155, 'ES2C3N15', 1, 2, 3, 15, NULL, 50, NULL, NULL),
(156, 'ES2C3N16', 1, 2, 3, 16, NULL, 30, NULL, NULL),
(157, 'ES2C3N17', 1, 2, 3, 17, NULL, 30, NULL, NULL),
(158, 'ES2C3N18', 1, 2, 3, 18, NULL, 80, NULL, NULL),
(159, 'ES2C3N19', 1, 2, 3, 19, NULL, 90, NULL, NULL),
(160, 'ES2C3N20', 1, 2, 3, 20, NULL, 50, NULL, NULL),
(161, 'ES2C4N1', 1, 2, 4, 1, NULL, 10, NULL, NULL),
(162, 'ES2C4N2', 1, 2, 4, 2, NULL, 40, NULL, NULL),
(163, 'ES2C4N3', 1, 2, 4, 3, NULL, 70, NULL, NULL),
(164, 'ES2C4N4', 1, 2, 4, 4, NULL, 100, NULL, NULL),
(165, 'ES2C4N5', 1, 2, 4, 5, NULL, 30, NULL, NULL),
(166, 'ES2C4N6', 1, 2, 4, 6, NULL, 50, NULL, NULL),
(167, 'ES2C4N7', 1, 2, 4, 7, NULL, 90, NULL, NULL),
(168, 'ES2C4N8', 1, 2, 4, 8, NULL, 80, NULL, NULL),
(169, 'ES2C4N9', 1, 2, 4, 9, NULL, 60, NULL, NULL),
(170, 'ES2C4N10', 1, 2, 4, 10, NULL, 20, NULL, NULL),
(171, 'ES2C4N11', 1, 2, 4, 11, NULL, 60, NULL, NULL),
(172, 'ES2C4N12', 1, 2, 4, 12, NULL, 60, NULL, NULL),
(173, 'ES2C4N13', 1, 2, 4, 13, NULL, 30, NULL, NULL),
(174, 'ES2C4N14', 1, 2, 4, 14, NULL, 60, NULL, NULL),
(175, 'ES2C4N15', 1, 2, 4, 15, NULL, 50, NULL, NULL),
(176, 'ES2C4N16', 1, 2, 4, 16, NULL, 50, NULL, NULL),
(177, 'ES2C4N17', 1, 2, 4, 17, NULL, 20, NULL, NULL),
(178, 'ES2C4N18', 1, 2, 4, 18, NULL, 70, NULL, NULL),
(179, 'ES2C4N19', 1, 2, 4, 19, NULL, 90, NULL, NULL),
(180, 'ES2C4N20', 1, 2, 4, 20, NULL, 80, NULL, NULL),
(181, 'ES2C5N1', 1, 2, 5, 1, NULL, 50, NULL, NULL),
(182, 'ES2C5N2', 1, 2, 5, 2, NULL, 30, NULL, NULL),
(183, 'ES2C5N3', 1, 2, 5, 3, NULL, 100, NULL, NULL),
(184, 'ES2C5N4', 1, 2, 5, 4, NULL, 100, NULL, NULL),
(185, 'ES2C5N5', 1, 2, 5, 5, NULL, 90, NULL, NULL),
(186, 'ES2C5N6', 1, 2, 5, 6, NULL, 20, NULL, NULL),
(187, 'ES2C5N7', 1, 2, 5, 7, NULL, 100, NULL, NULL),
(188, 'ES2C5N8', 1, 2, 5, 8, NULL, 20, NULL, NULL),
(189, 'ES2C5N9', 1, 2, 5, 9, NULL, 50, NULL, NULL),
(190, 'ES2C5N10', 1, 2, 5, 10, NULL, 10, NULL, NULL),
(191, 'ES2C5N11', 1, 2, 5, 11, NULL, 10, NULL, NULL),
(192, 'ES2C5N12', 1, 2, 5, 12, NULL, 50, NULL, NULL),
(193, 'ES2C5N13', 1, 2, 5, 13, NULL, 80, NULL, NULL),
(194, 'ES2C5N14', 1, 2, 5, 14, NULL, 80, NULL, NULL),
(195, 'ES2C5N15', 1, 2, 5, 15, NULL, 50, NULL, NULL),
(196, 'ES2C5N16', 1, 2, 5, 16, NULL, 100, NULL, NULL),
(197, 'ES2C5N17', 1, 2, 5, 17, NULL, 50, NULL, NULL),
(198, 'ES2C5N18', 1, 2, 5, 18, NULL, 50, NULL, NULL),
(199, 'ES2C5N19', 1, 2, 5, 19, NULL, 30, NULL, NULL),
(200, 'ES2C5N20', 1, 2, 5, 20, NULL, 40, NULL, NULL),
(201, 'ES3C1N1', 1, 3, 1, 1, NULL, 70, NULL, NULL),
(202, 'ES3C1N2', 1, 3, 1, 2, NULL, 60, NULL, NULL),
(203, 'ES3C1N3', 1, 3, 1, 3, NULL, 100, NULL, NULL),
(204, 'ES3C1N4', 1, 3, 1, 4, NULL, 60, NULL, NULL),
(205, 'ES3C1N5', 1, 3, 1, 5, NULL, 70, NULL, NULL),
(206, 'ES3C1N6', 1, 3, 1, 6, NULL, 80, NULL, NULL),
(207, 'ES3C1N7', 1, 3, 1, 7, NULL, 30, NULL, NULL),
(208, 'ES3C1N8', 1, 3, 1, 8, NULL, 30, NULL, NULL),
(209, 'ES3C1N9', 1, 3, 1, 9, NULL, 70, NULL, NULL),
(210, 'ES3C1N10', 1, 3, 1, 10, NULL, 100, NULL, NULL),
(211, 'ES3C1N11', 1, 3, 1, 11, NULL, 90, NULL, NULL),
(212, 'ES3C1N12', 1, 3, 1, 12, NULL, 20, NULL, NULL),
(213, 'ES3C1N13', 1, 3, 1, 13, NULL, 50, NULL, NULL),
(214, 'ES3C1N14', 1, 3, 1, 14, NULL, 80, NULL, NULL),
(215, 'ES3C1N15', 1, 3, 1, 15, NULL, 80, NULL, NULL),
(216, 'ES3C1N16', 1, 3, 1, 16, NULL, 40, NULL, NULL),
(217, 'ES3C1N17', 1, 3, 1, 17, NULL, 60, NULL, NULL),
(218, 'ES3C1N18', 1, 3, 1, 18, NULL, 70, NULL, NULL),
(219, 'ES3C1N19', 1, 3, 1, 19, NULL, 60, NULL, NULL),
(220, 'ES3C1N20', 1, 3, 1, 20, NULL, 90, NULL, NULL),
(221, 'ES3C2N1', 1, 3, 2, 1, NULL, 40, NULL, NULL),
(222, 'ES3C2N2', 1, 3, 2, 2, NULL, 90, NULL, NULL),
(223, 'ES3C2N3', 1, 3, 2, 3, NULL, 20, NULL, NULL),
(224, 'ES3C2N4', 1, 3, 2, 4, NULL, 40, NULL, NULL),
(225, 'ES3C2N5', 1, 3, 2, 5, NULL, 90, NULL, NULL),
(226, 'ES3C2N6', 1, 3, 2, 6, NULL, 30, NULL, NULL),
(227, 'ES3C2N7', 1, 3, 2, 7, NULL, 40, NULL, NULL),
(228, 'ES3C2N8', 1, 3, 2, 8, NULL, 10, NULL, NULL),
(229, 'ES3C2N9', 1, 3, 2, 9, NULL, 70, NULL, NULL),
(230, 'ES3C2N10', 1, 3, 2, 10, NULL, 30, NULL, NULL),
(231, 'ES3C2N11', 1, 3, 2, 11, NULL, 90, NULL, NULL),
(232, 'ES3C2N12', 1, 3, 2, 12, NULL, 60, NULL, NULL),
(233, 'ES3C2N13', 1, 3, 2, 13, NULL, 10, NULL, NULL),
(234, 'ES3C2N14', 1, 3, 2, 14, NULL, 10, NULL, NULL),
(235, 'ES3C2N15', 1, 3, 2, 15, NULL, 90, NULL, NULL),
(236, 'ES3C2N16', 1, 3, 2, 16, NULL, 30, NULL, NULL),
(237, 'ES3C2N17', 1, 3, 2, 17, NULL, 90, NULL, NULL),
(238, 'ES3C2N18', 1, 3, 2, 18, NULL, 30, NULL, NULL),
(239, 'ES3C2N19', 1, 3, 2, 19, NULL, 80, NULL, NULL),
(240, 'ES3C2N20', 1, 3, 2, 20, NULL, 70, NULL, NULL),
(241, 'ES3C3N1', 1, 3, 3, 1, NULL, 10, NULL, NULL),
(242, 'ES3C3N2', 1, 3, 3, 2, NULL, 70, NULL, NULL),
(243, 'ES3C3N3', 1, 3, 3, 3, NULL, 10, NULL, NULL),
(244, 'ES3C3N4', 1, 3, 3, 4, NULL, 50, NULL, NULL),
(245, 'ES3C3N5', 1, 3, 3, 5, NULL, 100, NULL, NULL),
(246, 'ES3C3N6', 1, 3, 3, 6, NULL, 20, NULL, NULL),
(247, 'ES3C3N7', 1, 3, 3, 7, NULL, 60, NULL, NULL),
(248, 'ES3C3N8', 1, 3, 3, 8, NULL, 80, NULL, NULL),
(249, 'ES3C3N9', 1, 3, 3, 9, NULL, 30, NULL, NULL),
(250, 'ES3C3N10', 1, 3, 3, 10, NULL, 40, NULL, NULL),
(251, 'ES3C3N11', 1, 3, 3, 11, NULL, 80, NULL, NULL),
(252, 'ES3C3N12', 1, 3, 3, 12, NULL, 60, NULL, NULL),
(253, 'ES3C3N13', 1, 3, 3, 13, NULL, 10, NULL, NULL),
(254, 'ES3C3N14', 1, 3, 3, 14, NULL, 100, NULL, NULL),
(255, 'ES3C3N15', 1, 3, 3, 15, NULL, 90, NULL, NULL),
(256, 'ES3C3N16', 1, 3, 3, 16, NULL, 70, NULL, NULL),
(257, 'ES3C3N17', 1, 3, 3, 17, NULL, 100, NULL, NULL),
(258, 'ES3C3N18', 1, 3, 3, 18, NULL, 50, NULL, NULL),
(259, 'ES3C3N19', 1, 3, 3, 19, NULL, 50, NULL, NULL),
(260, 'ES3C3N20', 1, 3, 3, 20, NULL, 100, NULL, NULL),
(261, 'ES3C4N1', 1, 3, 4, 1, NULL, 10, NULL, NULL),
(262, 'ES3C4N2', 1, 3, 4, 2, NULL, 20, NULL, NULL),
(263, 'ES3C4N3', 1, 3, 4, 3, NULL, 80, NULL, NULL),
(264, 'ES3C4N4', 1, 3, 4, 4, NULL, 50, NULL, NULL),
(265, 'ES3C4N5', 1, 3, 4, 5, NULL, 20, NULL, NULL),
(266, 'ES3C4N6', 1, 3, 4, 6, NULL, 90, NULL, NULL),
(267, 'ES3C4N7', 1, 3, 4, 7, NULL, 20, NULL, NULL),
(268, 'ES3C4N8', 1, 3, 4, 8, NULL, 80, NULL, NULL),
(269, 'ES3C4N9', 1, 3, 4, 9, NULL, 100, NULL, NULL),
(270, 'ES3C4N10', 1, 3, 4, 10, NULL, 20, NULL, NULL),
(271, 'ES3C4N11', 1, 3, 4, 11, NULL, 50, NULL, NULL),
(272, 'ES3C4N12', 1, 3, 4, 12, NULL, 70, NULL, NULL),
(273, 'ES3C4N13', 1, 3, 4, 13, NULL, 40, NULL, NULL),
(274, 'ES3C4N14', 1, 3, 4, 14, NULL, 70, NULL, NULL),
(275, 'ES3C4N15', 1, 3, 4, 15, NULL, 60, NULL, NULL),
(276, 'ES3C4N16', 1, 3, 4, 16, NULL, 100, NULL, NULL),
(277, 'ES3C4N17', 1, 3, 4, 17, NULL, 70, NULL, NULL),
(278, 'ES3C4N18', 1, 3, 4, 18, NULL, 80, NULL, NULL),
(279, 'ES3C4N19', 1, 3, 4, 19, NULL, 10, NULL, NULL),
(280, 'ES3C4N20', 1, 3, 4, 20, NULL, 30, NULL, NULL),
(281, 'ES3C5N1', 1, 3, 5, 1, NULL, 100, NULL, NULL),
(282, 'ES3C5N2', 1, 3, 5, 2, NULL, 70, NULL, NULL),
(283, 'ES3C5N3', 1, 3, 5, 3, NULL, 90, NULL, NULL),
(284, 'ES3C5N4', 1, 3, 5, 4, NULL, 90, NULL, NULL),
(285, 'ES3C5N5', 1, 3, 5, 5, NULL, 100, NULL, NULL),
(286, 'ES3C5N6', 1, 3, 5, 6, NULL, 50, NULL, NULL),
(287, 'ES3C5N7', 1, 3, 5, 7, NULL, 80, NULL, NULL),
(288, 'ES3C5N8', 1, 3, 5, 8, NULL, 20, NULL, NULL),
(289, 'ES3C5N9', 1, 3, 5, 9, NULL, 50, NULL, NULL),
(290, 'ES3C5N10', 1, 3, 5, 10, NULL, 40, NULL, NULL),
(291, 'ES3C5N11', 1, 3, 5, 11, NULL, 50, NULL, NULL),
(292, 'ES3C5N12', 1, 3, 5, 12, NULL, 70, NULL, NULL),
(293, 'ES3C5N13', 1, 3, 5, 13, NULL, 70, NULL, NULL),
(294, 'ES3C5N14', 1, 3, 5, 14, NULL, 40, NULL, NULL),
(295, 'ES3C5N15', 1, 3, 5, 15, NULL, 20, NULL, NULL),
(296, 'ES3C5N16', 1, 3, 5, 16, NULL, 20, NULL, NULL),
(297, 'ES3C5N17', 1, 3, 5, 17, NULL, 20, NULL, NULL),
(298, 'ES3C5N18', 1, 3, 5, 18, NULL, 50, NULL, NULL),
(299, 'ES3C5N19', 1, 3, 5, 19, NULL, 50, NULL, NULL),
(300, 'ES3C5N20', 1, 3, 5, 20, NULL, 70, NULL, NULL),
(301, 'ES4C1N1', 1, 4, 1, 1, NULL, 70, NULL, NULL),
(302, 'ES4C1N2', 1, 4, 1, 2, NULL, 30, NULL, NULL),
(303, 'ES4C1N3', 1, 4, 1, 3, NULL, 40, NULL, NULL),
(304, 'ES4C1N4', 1, 4, 1, 4, NULL, 10, NULL, NULL),
(305, 'ES4C1N5', 1, 4, 1, 5, NULL, 30, NULL, NULL),
(306, 'ES4C1N6', 1, 4, 1, 6, NULL, 30, NULL, NULL),
(307, 'ES4C1N7', 1, 4, 1, 7, NULL, 20, NULL, NULL),
(308, 'ES4C1N8', 1, 4, 1, 8, NULL, 50, NULL, NULL),
(309, 'ES4C1N9', 1, 4, 1, 9, NULL, 70, NULL, NULL),
(310, 'ES4C1N10', 1, 4, 1, 10, NULL, 80, NULL, NULL),
(311, 'ES4C1N11', 1, 4, 1, 11, NULL, 90, NULL, NULL),
(312, 'ES4C1N12', 1, 4, 1, 12, NULL, 60, NULL, NULL),
(313, 'ES4C1N13', 1, 4, 1, 13, NULL, 20, NULL, NULL),
(314, 'ES4C1N14', 1, 4, 1, 14, NULL, 80, NULL, NULL),
(315, 'ES4C1N15', 1, 4, 1, 15, NULL, 40, NULL, NULL),
(316, 'ES4C1N16', 1, 4, 1, 16, NULL, 10, NULL, NULL),
(317, 'ES4C1N17', 1, 4, 1, 17, NULL, 30, NULL, NULL),
(318, 'ES4C1N18', 1, 4, 1, 18, NULL, 50, NULL, NULL),
(319, 'ES4C1N19', 1, 4, 1, 19, NULL, 20, NULL, NULL),
(320, 'ES4C1N20', 1, 4, 1, 20, NULL, 50, NULL, NULL),
(321, 'ES4C2N1', 1, 4, 2, 1, NULL, 80, NULL, NULL),
(322, 'ES4C2N2', 1, 4, 2, 2, NULL, 40, NULL, NULL),
(323, 'ES4C2N3', 1, 4, 2, 3, NULL, 30, NULL, NULL),
(324, 'ES4C2N4', 1, 4, 2, 4, NULL, 50, NULL, NULL),
(325, 'ES4C2N5', 1, 4, 2, 5, NULL, 20, NULL, NULL),
(326, 'ES4C2N6', 1, 4, 2, 6, NULL, 70, NULL, NULL),
(327, 'ES4C2N7', 1, 4, 2, 7, NULL, 10, NULL, NULL),
(328, 'ES4C2N8', 1, 4, 2, 8, NULL, 20, NULL, NULL),
(329, 'ES4C2N9', 1, 4, 2, 9, NULL, 20, NULL, NULL),
(330, 'ES4C2N10', 1, 4, 2, 10, NULL, 60, NULL, NULL),
(331, 'ES4C2N11', 1, 4, 2, 11, NULL, 50, NULL, NULL),
(332, 'ES4C2N12', 1, 4, 2, 12, NULL, 90, NULL, NULL),
(333, 'ES4C2N13', 1, 4, 2, 13, NULL, 50, NULL, NULL),
(334, 'ES4C2N14', 1, 4, 2, 14, NULL, 100, NULL, NULL),
(335, 'ES4C2N15', 1, 4, 2, 15, NULL, 30, NULL, NULL),
(336, 'ES4C2N16', 1, 4, 2, 16, NULL, 20, NULL, NULL),
(337, 'ES4C2N17', 1, 4, 2, 17, NULL, 30, NULL, NULL),
(338, 'ES4C2N18', 1, 4, 2, 18, NULL, 10, NULL, NULL),
(339, 'ES4C2N19', 1, 4, 2, 19, NULL, 40, NULL, NULL),
(340, 'ES4C2N20', 1, 4, 2, 20, NULL, 100, NULL, NULL),
(341, 'ES4C3N1', 1, 4, 3, 1, NULL, 70, NULL, NULL),
(342, 'ES4C3N2', 1, 4, 3, 2, NULL, 20, NULL, NULL),
(343, 'ES4C3N3', 1, 4, 3, 3, NULL, 20, NULL, NULL),
(344, 'ES4C3N4', 1, 4, 3, 4, NULL, 10, NULL, NULL),
(345, 'ES4C3N5', 1, 4, 3, 5, NULL, 90, NULL, NULL),
(346, 'ES4C3N6', 1, 4, 3, 6, NULL, 30, NULL, NULL),
(347, 'ES4C3N7', 1, 4, 3, 7, NULL, 80, NULL, NULL),
(348, 'ES4C3N8', 1, 4, 3, 8, NULL, 40, NULL, NULL),
(349, 'ES4C3N9', 1, 4, 3, 9, NULL, 70, NULL, NULL),
(350, 'ES4C3N10', 1, 4, 3, 10, NULL, 100, NULL, NULL),
(351, 'ES4C3N11', 1, 4, 3, 11, NULL, 70, NULL, NULL),
(352, 'ES4C3N12', 1, 4, 3, 12, NULL, 100, NULL, NULL),
(353, 'ES4C3N13', 1, 4, 3, 13, NULL, 80, NULL, NULL),
(354, 'ES4C3N14', 1, 4, 3, 14, NULL, 40, NULL, NULL),
(355, 'ES4C3N15', 1, 4, 3, 15, NULL, 60, NULL, NULL),
(356, 'ES4C3N16', 1, 4, 3, 16, NULL, 80, NULL, NULL),
(357, 'ES4C3N17', 1, 4, 3, 17, NULL, 60, NULL, NULL),
(358, 'ES4C3N18', 1, 4, 3, 18, NULL, 60, NULL, NULL),
(359, 'ES4C3N19', 1, 4, 3, 19, NULL, 50, NULL, NULL),
(360, 'ES4C3N20', 1, 4, 3, 20, NULL, 20, NULL, NULL),
(361, 'ES4C4N1', 1, 4, 4, 1, NULL, 10, NULL, NULL),
(362, 'ES4C4N2', 1, 4, 4, 2, NULL, 70, NULL, NULL),
(363, 'ES4C4N3', 1, 4, 4, 3, NULL, 30, NULL, NULL),
(364, 'ES4C4N4', 1, 4, 4, 4, NULL, 40, NULL, NULL),
(365, 'ES4C4N5', 1, 4, 4, 5, NULL, 60, NULL, NULL),
(366, 'ES4C4N6', 1, 4, 4, 6, NULL, 20, NULL, NULL),
(367, 'ES4C4N7', 1, 4, 4, 7, NULL, 10, NULL, NULL),
(368, 'ES4C4N8', 1, 4, 4, 8, NULL, 40, NULL, NULL),
(369, 'ES4C4N9', 1, 4, 4, 9, NULL, 40, NULL, NULL),
(370, 'ES4C4N10', 1, 4, 4, 10, NULL, 20, NULL, NULL),
(371, 'ES4C4N11', 1, 4, 4, 11, NULL, 40, NULL, NULL),
(372, 'ES4C4N12', 1, 4, 4, 12, NULL, 50, NULL, NULL),
(373, 'ES4C4N13', 1, 4, 4, 13, NULL, 100, NULL, NULL),
(374, 'ES4C4N14', 1, 4, 4, 14, NULL, 10, NULL, NULL),
(375, 'ES4C4N15', 1, 4, 4, 15, NULL, 100, NULL, NULL),
(376, 'ES4C4N16', 1, 4, 4, 16, NULL, 90, NULL, NULL),
(377, 'ES4C4N17', 1, 4, 4, 17, NULL, 20, NULL, NULL),
(378, 'ES4C4N18', 1, 4, 4, 18, NULL, 60, NULL, NULL),
(379, 'ES4C4N19', 1, 4, 4, 19, NULL, 60, NULL, NULL),
(380, 'ES4C4N20', 1, 4, 4, 20, NULL, 80, NULL, NULL),
(381, 'ES4C5N1', 1, 4, 5, 1, NULL, 10, NULL, NULL),
(382, 'ES4C5N2', 1, 4, 5, 2, NULL, 60, NULL, NULL),
(383, 'ES4C5N3', 1, 4, 5, 3, NULL, 60, NULL, NULL),
(384, 'ES4C5N4', 1, 4, 5, 4, NULL, 50, NULL, NULL),
(385, 'ES4C5N5', 1, 4, 5, 5, NULL, 70, NULL, NULL),
(386, 'ES4C5N6', 1, 4, 5, 6, NULL, 10, NULL, NULL),
(387, 'ES4C5N7', 1, 4, 5, 7, NULL, 70, NULL, NULL),
(388, 'ES4C5N8', 1, 4, 5, 8, NULL, 20, NULL, NULL),
(389, 'ES4C5N9', 1, 4, 5, 9, NULL, 70, NULL, NULL),
(390, 'ES4C5N10', 1, 4, 5, 10, NULL, 80, NULL, NULL),
(391, 'ES4C5N11', 1, 4, 5, 11, NULL, 20, NULL, NULL),
(392, 'ES4C5N12', 1, 4, 5, 12, NULL, 20, NULL, NULL),
(393, 'ES4C5N13', 1, 4, 5, 13, NULL, 80, NULL, NULL),
(394, 'ES4C5N14', 1, 4, 5, 14, NULL, 90, NULL, NULL),
(395, 'ES4C5N15', 1, 4, 5, 15, NULL, 100, NULL, NULL),
(396, 'ES4C5N16', 1, 4, 5, 16, NULL, 40, NULL, NULL),
(397, 'ES4C5N17', 1, 4, 5, 17, NULL, 60, NULL, NULL),
(398, 'ES4C5N18', 1, 4, 5, 18, NULL, 50, NULL, NULL),
(399, 'ES4C5N19', 1, 4, 5, 19, NULL, 100, NULL, NULL),
(400, 'ES4C5N20', 1, 4, 5, 20, NULL, 40, NULL, NULL),
(401, 'ES5C1N1', 1, 5, 1, 1, NULL, 90, NULL, NULL),
(402, 'ES5C1N2', 1, 5, 1, 2, NULL, 70, NULL, NULL),
(403, 'ES5C1N3', 1, 5, 1, 3, NULL, 40, NULL, NULL),
(404, 'ES5C1N4', 1, 5, 1, 4, NULL, 60, NULL, NULL),
(405, 'ES5C1N5', 1, 5, 1, 5, NULL, 90, NULL, NULL),
(406, 'ES5C1N6', 1, 5, 1, 6, NULL, 90, NULL, NULL),
(407, 'ES5C1N7', 1, 5, 1, 7, NULL, 30, NULL, NULL),
(408, 'ES5C1N8', 1, 5, 1, 8, NULL, 70, NULL, NULL),
(409, 'ES5C1N9', 1, 5, 1, 9, NULL, 60, NULL, NULL),
(410, 'ES5C1N10', 1, 5, 1, 10, NULL, 80, NULL, NULL),
(411, 'ES5C1N11', 1, 5, 1, 11, NULL, 50, NULL, NULL),
(412, 'ES5C1N12', 1, 5, 1, 12, NULL, 30, NULL, NULL),
(413, 'ES5C1N13', 1, 5, 1, 13, NULL, 50, NULL, NULL),
(414, 'ES5C1N14', 1, 5, 1, 14, NULL, 90, NULL, NULL),
(415, 'ES5C1N15', 1, 5, 1, 15, NULL, 80, NULL, NULL),
(416, 'ES5C1N16', 1, 5, 1, 16, NULL, 10, NULL, NULL),
(417, 'ES5C1N17', 1, 5, 1, 17, NULL, 20, NULL, NULL),
(418, 'ES5C1N18', 1, 5, 1, 18, NULL, 90, NULL, NULL),
(419, 'ES5C1N19', 1, 5, 1, 19, NULL, 50, NULL, NULL),
(420, 'ES5C1N20', 1, 5, 1, 20, NULL, 60, NULL, NULL),
(421, 'ES5C2N1', 1, 5, 2, 1, NULL, 20, NULL, NULL),
(422, 'ES5C2N2', 1, 5, 2, 2, NULL, 60, NULL, NULL),
(423, 'ES5C2N3', 1, 5, 2, 3, NULL, 40, NULL, NULL),
(424, 'ES5C2N4', 1, 5, 2, 4, NULL, 30, NULL, NULL),
(425, 'ES5C2N5', 1, 5, 2, 5, NULL, 10, NULL, NULL),
(426, 'ES5C2N6', 1, 5, 2, 6, NULL, 40, NULL, NULL),
(427, 'ES5C2N7', 1, 5, 2, 7, NULL, 70, NULL, NULL),
(428, 'ES5C2N8', 1, 5, 2, 8, NULL, 50, NULL, NULL),
(429, 'ES5C2N9', 1, 5, 2, 9, NULL, 80, NULL, NULL),
(430, 'ES5C2N10', 1, 5, 2, 10, NULL, 70, NULL, NULL),
(431, 'ES5C2N11', 1, 5, 2, 11, NULL, 70, NULL, NULL),
(432, 'ES5C2N12', 1, 5, 2, 12, NULL, 50, NULL, NULL),
(433, 'ES5C2N13', 1, 5, 2, 13, NULL, 60, NULL, NULL),
(434, 'ES5C2N14', 1, 5, 2, 14, NULL, 10, NULL, NULL),
(435, 'ES5C2N15', 1, 5, 2, 15, NULL, 90, NULL, NULL),
(436, 'ES5C2N16', 1, 5, 2, 16, NULL, 90, NULL, NULL),
(437, 'ES5C2N17', 1, 5, 2, 17, NULL, 30, NULL, NULL),
(438, 'ES5C2N18', 1, 5, 2, 18, NULL, 100, NULL, NULL),
(439, 'ES5C2N19', 1, 5, 2, 19, NULL, 70, NULL, NULL),
(440, 'ES5C2N20', 1, 5, 2, 20, NULL, 20, NULL, NULL),
(441, 'ES5C3N1', 1, 5, 3, 1, NULL, 90, NULL, NULL),
(442, 'ES5C3N2', 1, 5, 3, 2, NULL, 100, NULL, NULL),
(443, 'ES5C3N3', 1, 5, 3, 3, NULL, 60, NULL, NULL),
(444, 'ES5C3N4', 1, 5, 3, 4, NULL, 30, NULL, NULL),
(445, 'ES5C3N5', 1, 5, 3, 5, NULL, 70, NULL, NULL),
(446, 'ES5C3N6', 1, 5, 3, 6, NULL, 30, NULL, NULL),
(447, 'ES5C3N7', 1, 5, 3, 7, NULL, 10, NULL, NULL),
(448, 'ES5C3N8', 1, 5, 3, 8, NULL, 40, NULL, NULL),
(449, 'ES5C3N9', 1, 5, 3, 9, NULL, 80, NULL, NULL),
(450, 'ES5C3N10', 1, 5, 3, 10, NULL, 50, NULL, NULL),
(451, 'ES5C3N11', 1, 5, 3, 11, NULL, 80, NULL, NULL),
(452, 'ES5C3N12', 1, 5, 3, 12, NULL, 60, NULL, NULL),
(453, 'ES5C3N13', 1, 5, 3, 13, NULL, 20, NULL, NULL),
(454, 'ES5C3N14', 1, 5, 3, 14, NULL, 10, NULL, NULL),
(455, 'ES5C3N15', 1, 5, 3, 15, NULL, 70, NULL, NULL),
(456, 'ES5C3N16', 1, 5, 3, 16, NULL, 10, NULL, NULL),
(457, 'ES5C3N17', 1, 5, 3, 17, NULL, 40, NULL, NULL),
(458, 'ES5C3N18', 1, 5, 3, 18, NULL, 10, NULL, NULL),
(459, 'ES5C3N19', 1, 5, 3, 19, NULL, 50, NULL, NULL),
(460, 'ES5C3N20', 1, 5, 3, 20, NULL, 30, NULL, NULL),
(461, 'ES5C4N1', 1, 5, 4, 1, NULL, 70, NULL, NULL),
(462, 'ES5C4N2', 1, 5, 4, 2, NULL, 70, NULL, NULL),
(463, 'ES5C4N3', 1, 5, 4, 3, NULL, 80, NULL, NULL),
(464, 'ES5C4N4', 1, 5, 4, 4, NULL, 90, NULL, NULL),
(465, 'ES5C4N5', 1, 5, 4, 5, NULL, 30, NULL, NULL),
(466, 'ES5C4N6', 1, 5, 4, 6, NULL, 10, NULL, NULL),
(467, 'ES5C4N7', 1, 5, 4, 7, NULL, 40, NULL, NULL),
(468, 'ES5C4N8', 1, 5, 4, 8, NULL, 50, NULL, NULL),
(469, 'ES5C4N9', 1, 5, 4, 9, NULL, 90, NULL, NULL),
(470, 'ES5C4N10', 1, 5, 4, 10, NULL, 90, NULL, NULL),
(471, 'ES5C4N11', 1, 5, 4, 11, NULL, 80, NULL, NULL),
(472, 'ES5C4N12', 1, 5, 4, 12, NULL, 90, NULL, NULL),
(473, 'ES5C4N13', 1, 5, 4, 13, NULL, 60, NULL, NULL),
(474, 'ES5C4N14', 1, 5, 4, 14, NULL, 40, NULL, NULL),
(475, 'ES5C4N15', 1, 5, 4, 15, NULL, 70, NULL, NULL),
(476, 'ES5C4N16', 1, 5, 4, 16, NULL, 70, NULL, NULL),
(477, 'ES5C4N17', 1, 5, 4, 17, NULL, 20, NULL, NULL),
(478, 'ES5C4N18', 1, 5, 4, 18, NULL, 30, NULL, NULL),
(479, 'ES5C4N19', 1, 5, 4, 19, NULL, 30, NULL, NULL),
(480, 'ES5C4N20', 1, 5, 4, 20, NULL, 90, NULL, NULL),
(481, 'ES5C5N1', 1, 5, 5, 1, NULL, 90, NULL, NULL),
(482, 'ES5C5N2', 1, 5, 5, 2, NULL, 90, NULL, NULL),
(483, 'ES5C5N3', 1, 5, 5, 3, NULL, 10, NULL, NULL),
(484, 'ES5C5N4', 1, 5, 5, 4, NULL, 40, NULL, NULL),
(485, 'ES5C5N5', 1, 5, 5, 5, NULL, 50, NULL, NULL),
(486, 'ES5C5N6', 1, 5, 5, 6, NULL, 100, NULL, NULL),
(487, 'ES5C5N7', 1, 5, 5, 7, NULL, 50, NULL, NULL),
(488, 'ES5C5N8', 1, 5, 5, 8, NULL, 60, NULL, NULL),
(489, 'ES5C5N9', 1, 5, 5, 9, NULL, 100, NULL, NULL),
(490, 'ES5C5N10', 1, 5, 5, 10, NULL, 50, NULL, NULL),
(491, 'ES5C5N11', 1, 5, 5, 11, NULL, 60, NULL, NULL),
(492, 'ES5C5N12', 1, 5, 5, 12, NULL, 10, NULL, NULL),
(493, 'ES5C5N13', 1, 5, 5, 13, NULL, 30, NULL, NULL),
(494, 'ES5C5N14', 1, 5, 5, 14, NULL, 70, NULL, NULL),
(495, 'ES5C5N15', 1, 5, 5, 15, NULL, 90, NULL, NULL),
(496, 'ES5C5N16', 1, 5, 5, 16, NULL, 30, NULL, NULL),
(497, 'ES5C5N17', 1, 5, 5, 17, NULL, 20, NULL, NULL),
(498, 'ES5C5N18', 1, 5, 5, 18, NULL, 90, NULL, NULL),
(499, 'ES5C5N19', 1, 5, 5, 19, NULL, 100, NULL, NULL),
(500, 'ES5C5N20', 1, 5, 5, 20, NULL, 20, NULL, NULL);

--
-- Déchargement des données de la table `requirements_information`
--

INSERT INTO `requirements_information` (`id`, `relation_id`, `requirement_id`, `second_value`, `third_value`) VALUES
(1, 1, 3, 1, NULL),
(2, 2, 3, 2, NULL),
(3, 3, 3, 3, NULL),
(4, 4, 3, 4, NULL),
(5, 5, 3, 5, NULL),
(6, 6, 4, 1, 1),
(7, 7, 4, 1, 5),
(8, 8, 4, 2, 1),
(9, 9, 4, 2, 5),
(10, 6, 3, 1, NULL),
(11, 7, 3, 1, NULL),
(12, 8, 3, 2, NULL),
(13, 9, 3, 2, NULL);

--
-- Déchargement des données de la table `units`
--

INSERT INTO `units` (`id`, `order_number`, `name`, `image`, `points`, `description`, `time`, `primary_resource`, `secondary_resource`, `energy`, `type`, `attack`, `health`, `shield`, `charge`, `is_unique`, `improvement_id`, `cloned_improvements`) VALUES
(1, NULL, 'Viejo de derecha', '362d38355d4d1209d2c99da3ef402e75.png', 10, 'El típico viejo que dice \"Yooooo, de todaa la vidaa he votadoo al PP\"', 200, 1000, 1000, NULL, 2, 1, 3, 0, NULL, 0, 11, 0),
(2, NULL, 'Mariano Rajoy', '66540377585c9dfcc42c70fcb79d08a5.png', 100, 'Mariano Rajoy gracias a su \"gran gestión\" de los recursos del país.. mejora el PIB Español', 3600, 5000, 5000, NULL, 1, 100, 100, 5, NULL, 1, 12, 0),
(3, NULL, 'Viejo de izquierdas', 'd4413f62a54da8d2eddab58a186c7db6.png', 10, 'El típico anciano que siempre ha votado a la izquierda... porque su familia era republicana blablabla', 200, 1000, 1000, NULL, 2, 1, 2, 1, NULL, 0, 13, 0),
(4, NULL, 'Zapatero', '2d5ae400c3240a4f84c4c8b6d15847d2.jpg', 100, '', 3600, 5000, 5000, NULL, 1, 100, 100, 5, NULL, 1, 14, 0);

--
-- Déchargement des données de la table `unit_types`
--

INSERT INTO `unit_types` (`id`, `name`, `max_count`, `image`, `parent_type`, `can_explore`, `can_gather`, `can_establish_base`, `can_attack`, `can_counterattack`, `can_conquest`, `can_deploy`) VALUES
(1, 'Político', 10, NULL, NULL, 'NONE', 'NONE', 'ANY', 'ANY', 'ANY', 'ANY', 'ANY'),
(2, 'Ciudadano', NULL, NULL, NULL, 'ANY', 'ANY', 'NONE', 'ANY', 'ANY', 'NONE', 'ANY');

--
-- Déchargement des données de la table `upgrades`
--

INSERT INTO `upgrades` (`id`, `name`, `points`, `image`, `description`, `time`, `primary_resource`, `secondary_resource`, `type`, `level_effect`, `improvement_id`, `cloned_improvements`) VALUES
(1, 'Sobres', 20, 'e5f0f021d0dea0641962d19ab4f9240c.png', 'Los del PP saben manejar sobres mejor que Correos', 60, 100, 100, 1, 20, 6, 0),
(2, 'ERES', 20, '69621ab42e41abf15d07cfb53481e6fb.jpg', 'Los del PSOE usan ERES pa\' robar dinero público', 60, 120, 120, 1, 20, 7, 0),
(3, 'Subvenciones', 20, 'd05cca6a0e00201bf185ab832a360aea.jpg', 'Los de Podemos les gusta mucho las subvenciones', 100, 100, 100, 1, 20, 8, 0),
(4, 'Centrismo', 8, 'ab56bcf706f7ae9fe45282999567daea.jpg', 'Ciudadanos utiliza su posición de centro para no representar a nadie', 60, 100, 100, 1, 20, 9, 0),
(5, 'Patriotismo', 14, 'f115da79b53e22327bb385062b326d84.jpg', 'Los de Vox son gente muy patriota', 80, 100, 100, NULL, 20, 10, 0);

--
-- Déchargement des données de la table `upgrade_types`
--

INSERT INTO `upgrade_types` (`id`, `name`) VALUES
(1, 'Económicas');
SET FOREIGN_KEY_CHECKS=1;
COMMIT;

/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
