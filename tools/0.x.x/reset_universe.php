#!/usr/bin/env php

<?php
/*
 * Kevin Guanche Darias :: OWGE universe reset
 *
 * Wipes ALL user/player generated data from a (running) universe, leaving the
 * configuration + game content tables (the same set "export_world.php" exports)
 * untouched, and finally re-randomizes the special locations by delegating to
 * "ramdomize_special_locations.php".
 *
 * The result is a pristine universe (no players, no missions, no obtained units,
 * planets released to no owner) ready to be played from scratch, while every
 * piece of admin authored content (factions, units, upgrades, planets, galaxies,
 * translations, tutorial, sponsors, admin users, ...) is preserved.
 *
 * NOTE: as with the randomizer, if the universe is already running you MUST
 *       force a cache reset from the admin panel afterwards, and ideally restart
 *       the game-rest backend so no stale in-memory/db-scheduler state remains.
 */

mysqli_report(MYSQLI_REPORT_ERROR | MYSQLI_REPORT_STRICT);

/*
 * Player/user generated tables. Everything NOT listed here is considered
 * configuration/content and is left intact. Keep this list in sync with the
 * schema (business/database/02_schema.sql) when new user tables are added.
 */
const USER_TABLES = [
    // --- account / progress ---
    'user_storage',
    'user_improvements',
    'user_read_system_messages',
    'visited_tutorial_entries',
    'unlocked_relation',
    'ranking',
    'planet_list',
    // --- obtained content ---
    'obtained_units',
    'obtained_unit_temporal_information',
    'obtained_upgrades',
    'stored_units',
    'active_time_specials',
    // --- missions / combat ---
    'missions',
    'mission_information',
    'mission_reports',
    'explored_planets',
    'scheduled_tasks', // db-scheduler jobs (pending mission executions)
    // --- alliances ---
    'alliances',
    'alliance_join_request',
    // --- legacy messaging ---
    'mensajes',
    'carpetas',
    // --- realtime websocket per-user state ---
    'websocket_events_information',
    'websocket_messages_status',
    // --- security / auditing (per-user) ---
    'audit',
    'suspicions',
    'track_browser',
];

echo "\e[34mKevin Guanche Darias :: OWGE universe reset\e[39m" . PHP_EOL . PHP_EOL;
echo "\e[33mWARNING: This DELETES every player from the universe. If the universe is\e[39m" . PHP_EOL;
echo "\e[33m         running, force a cache reset in the admin panel afterwards.\e[39m" . PHP_EOL . PHP_EOL;

if ($argc < 5) {
    echo 'Missing parameters, usage: ' . basename(__FILE__) . ' dbhost dbuser dbpassword targetdatabase' . PHP_EOL;
    echo 'Env: NOOP        => dry run, prints what would be deleted then rolls back (also passed to the randomizer)' . PHP_EOL;
    echo 'Env: FORCE=1     => skip the interactive confirmation' . PHP_EOL;
    echo 'Env: MYSQL_PORT  => database port (default 3306)' . PHP_EOL;
    echo 'Env: SKIP_RANDOMIZE=1 => do not run ramdomize_special_locations.php afterwards' . PHP_EOL;
    exit(1);
}

list($_, $host, $user, $password, $targetDb) = $argv;
$isNoop = (bool) getenv('NOOP');

if (!$isNoop && !getenv('FORCE')) {
    echo "About to reset universe \e[32m$targetDb\e[39m on \e[32m$host\e[39m." . PHP_EOL;
    echo 'Type "RESET" (uppercase) to continue: ';
    $answer = rtrim((string) fgets(STDIN));
    if ($answer !== 'RESET') {
        echo "\e[31mAborted.\e[39m" . PHP_EOL;
        exit(1);
    }
}

$connection = new mysqli($host, $user, $password, $targetDb, getenv('MYSQL_PORT') ?: 3306);
$connection->set_charset('utf8');

$connection->query('SET FOREIGN_KEY_CHECKS=0');
$connection->query('START TRANSACTION');

foreach (USER_TABLES as $table) {
    $connection->query("DELETE FROM `$table`");
    $affected = $connection->affected_rows;
    echo "Cleared \e[32m$table\e[39m (\e[33m$affected\e[39m rows)" . PHP_EOL;
}

// Release every planet: no owner, no home flag. special_location_id is reset by
// the randomizer below, but null it here too so a SKIP_RANDOMIZE run is coherent.
$connection->query('UPDATE planets SET owner = NULL, home = 0, special_location_id = NULL');
echo "Released \e[32mplanets\e[39m (owner/home/special_location cleared, \e[33m{$connection->affected_rows}\e[39m rows)" . PHP_EOL;

$connection->query('SET FOREIGN_KEY_CHECKS=1');

if ($isNoop) {
    $connection->query('ROLLBACK');
    echo PHP_EOL . "\e[41m NOOP: rolled back, nothing was deleted \e[49m" . PHP_EOL;
} else {
    $connection->query('COMMIT');
    echo PHP_EOL . "\e[32mUser data wiped.\e[39m" . PHP_EOL;
}

if (getenv('SKIP_RANDOMIZE')) {
    echo "\e[33mSKIP_RANDOMIZE set, not running the special locations randomizer.\e[39m" . PHP_EOL;
    exit(0);
}

// Delegate to the existing randomizer (single source of truth for that logic).
echo PHP_EOL . '-- Running special locations randomizer --' . PHP_EOL;
$randomizer = __DIR__ . '/ramdomize_special_locations.php';
$cmd = sprintf(
    '%s %s %s %s %s',
    escapeshellarg(PHP_BINARY),
    escapeshellarg($randomizer),
    escapeshellarg($user),
    escapeshellarg($password),
    escapeshellarg($targetDb)
);
passthru($cmd, $exitCode);
exit($exitCode);
?>
