#!/usr/bin/env php

<?php
const TABLES_TO_EXPORT = [
    'attack_rules',
    'attack_rule_entries',
    'configuration',
    'critical_attack',
    'critical_attack_entries',
    'factions',
    'factions_unit_types',
    'faction_spawn_location',
    'galaxies',
    'images_store',
    'improvements',
    'improvements_unit_types',
    'interceptable_speed_group',
    'object_relations',
    'object_relation__object_relation',
    'planets',
    'requirements_information',
    'requirement_group',
    'rules',
    'special_locations',
    'speed_impact_groups',
    'translatables',
    'translatables_translations',
    'time_specials',
    'tutorial_sections',
    'tutorial_sections_available_html_symbols',
    'tutorial_sections_entries',
    'units',
    'unit_types',
    'upgrades',
    'upgrade_types'
];

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

echo '-- Kevin Guanche Darias :: OWGE world exporter' . PHP_EOL;

if($argc < 5) {
    echo 'Missing parameters, usage: ' . basename(__FILE__) . ' dbhost dbuser dbpassword sourcedatabase' . PHP_EOL;
    echo 'Env: DUMP_TARGET_SQL to display the queries to the target database';
    exit(1);
}

function applyFilters(string $table, array $row, string $column) {
    $retVal = $row[$column];
    if($table === 'planets') {
        if(($column === 'owner' && $row[$column]) || $column === 'home') {
            $retVal = null;
        }
    }
    $retVal = is_null($retVal) ? 'NULL' : $retVal;
    return $retVal;
}

list($_, $host, $user, $password, $sourceDb) = $argv;
$connetion = new mysqli($host, $user, $password, $sourceDb, getenv('MYSQL_PORT') ?: 3306);
$connetion->set_charset('utf8');

$output = <<<SQL_MARKER
    SET FOREIGN_KEY_CHECKS=0;
    SET SQL_MODE = "NO_AUTO_VALUE_ON_ZERO";
    SET AUTOCOMMIT = 0;
    START TRANSACTION;
    SET time_zone = "+00:00";    
    
    
    SQL_MARKER;
foreach(TABLES_TO_EXPORT as $table) {
    $result = $connetion->query("SELECT * FROM $table LIMIT 1");
    if($result->num_rows) {
        $columns = $result->fetch_assoc();
        $columns = array_keys($columns);
        $implodedColumns = implode(',', $columns);
        $result = $connetion->query("SELECT * FROM $table");
        $output .= "INSERT INTO $table ($implodedColumns)VALUES" . PHP_EOL;
        $rows = $result->num_rows;
        for($i = 1; $row = $result->fetch_assoc(); $i++) {
            $output .= str_repeat(' ', 13 + strlen($table));
            $values = implode('\',\'',array_map(
                fn($column) => $connetion->real_escape_string(applyFilters($table, $row, $column)), 
                $columns
            ));
            $values = str_replace("'NULL'",'NULL', "('$values')");
            $output .= $values;
            $output .= ($rows !== $i) ? ',' : ';';
            $output .= PHP_EOL;
        }
    } else {
        $output .= "-- Table $table is empty" . PHP_EOL;
    }

}
echo <<<SQL
    /*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
    /*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
    /*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
    /*!40101 SET NAMES utf8mb4 */;
    
    $output
    SET FOREIGN_KEY_CHECKS=1;
    COMMIT;
    
    /*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
    /*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
    /*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
    SQL;
?>