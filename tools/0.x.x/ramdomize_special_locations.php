#!/usr/bin/env php

<?php
mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

echo "\e[34mKevin Guanche Darias :: OWGE special locations ramdomizer\e[39m" . PHP_EOL . PHP_EOL;

echo "\e[33mWARNING: Do NOT use if universe is already running,if doing, force cache reset in the admin panel\e[39m" . PHP_EOL . PHP_EOL;
if($argc < 4) {
    echo 'Missing parameters, usage: ' . basename(__FILE__) . ' dbuser dbpassword targetdatabase' . PHP_EOL;
    echo 'Env: NOOP to do nothing' . PHP_EOL;
    exit(1);
}

list($_, $user, $password, $sourceDb) = $argv;
$connetion = new mysqli('127.0.0.1', $user, $password, $sourceDb);
$connetion->set_charset('utf8');
$connetion->query('START TRANSACTION');
$connetion->query('UPDATE planets SET special_location_id = NULL');
$result = $connetion->query('SELECT * FROM special_locations');
$err = false;
while($specialLocation = $result->fetch_object()) {
    $galaxyId = boolval($specialLocation->galaxy_id) ? +$specialLocation->galaxy_id : 0;
    $galaxyWhere = $galaxyId ? "galaxy_id = $galaxyId" : '1=1';
    $planet = $connetion->query(
        <<<SQL_MARKER
        SELECT id, name, galaxy_id 
        FROM planets 
        WHERE $galaxyWhere AND owner IS NULL AND special_location_id IS NULL
        ORDER BY RAND() LIMIT 1
        SQL_MARKER
    );
    if($planet) {
        list($planetId,$planetName, $planetGalaxy) = $planet->fetch_row();
        $wantedOrRandom = $galaxyId ? 'wanted' : 'random';
        $galaxyName = $connetion->query("SELECT name FROM galaxies WHERE id = $planetGalaxy")->fetch_array()[0];
        echo "Will move special \e[32m$specialLocation->name\e[39m TO \e[33m$wantedOrRandom\e[39m galaxy \e[32m$galaxyName\e[39m($planetGalaxy)\e[35m => \e[32m$planetName\e[39m($planetId)";
        $connetion->query("UPDATE planets SET special_location_id = $specialLocation->id WHERE id = $planetId");
        if(!getenv('NOOP')) {
            echo PHP_EOL;
        } else {
            echo " \e[41m DOING NOTHING (NOOP)\e[49m\e[39m" . PHP_EOL;
        }
    } else {
        $err = true;
    }
}
if($err) {
    echo "\e[31mAn unexpected error occured\e[39m";
    $connetion->query('ROLLBACK');
} else if(getenv('NOOP')) {
    $connetion->query('ROLLBACK');
} else {
    $connetion->query('COMMIT');
}
