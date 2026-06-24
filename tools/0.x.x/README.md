# OWGE maintenance tools (0.x.x)

Standalone PHP CLI scripts for operating an OWGE universe (its MySQL/MariaDB
database) outside of the game backend. They talk to the database directly and
have **no** dependency on the Java backend or the frontend.

All scripts take the database coordinates as positional arguments and read
optional switches from environment variables. PHP 7.4+ with the `mysqli`
extension is required.

| Script | Purpose |
| --- | --- |
| `export_world.php` | Dump the configuration + content of a universe as a portable `INSERT` script ("world template"). |
| `reset_universe.php` | Wipe **all player data** from a universe and re-randomize special locations ("universe reset"). |
| `ramdomize_special_locations.php` | (Re)assign each special location to a random eligible planet. |
| `import_classic_sgt.php` | One-off importer for the legacy "classic SGT" data. |

---

## `reset_universe.php`

Returns a **running** (or stopped) universe to a pristine, never-played state:
every player, mission, obtained unit/upgrade, alliance, message, ranking, etc.
is deleted, every planet is released (no owner, no home, no special location),
and special locations are re-randomized. **All admin-authored configuration and
content is kept** — factions, units, upgrades, planets, galaxies, translations,
tutorial, sponsors, requirements, admin accounts, and so on.

### Usage

```bash
php reset_universe.php <dbhost> <dbuser> <dbpassword> <targetdatabase>
```

By default it asks for an interactive confirmation (you must type `RESET`).

### Environment switches

| Env var | Effect |
| --- | --- |
| `NOOP` | Dry run: performs the deletes inside a transaction, prints the per-table row counts, then **rolls back**. Nothing is deleted. Also forwarded to the randomizer. |
| `FORCE=1` | Skip the interactive `RESET` confirmation (for automation). |
| `SKIP_RANDOMIZE=1` | Wipe player data but do **not** run `ramdomize_special_locations.php` afterwards. |
| `MYSQL_PORT` | Database port (default `3306`). |

### What it deletes

Only the player/user-generated tables are cleared (a hard-coded whitelist in the
script — keep it in sync with `business/database/02_schema.sql`):

- **Account / progress:** `user_storage`, `user_improvements`,
  `user_read_system_messages`, `visited_tutorial_entries`, `unlocked_relation`,
  `ranking`, `planet_list`
- **Obtained content:** `obtained_units`, `obtained_unit_temporal_information`,
  `obtained_upgrades`, `stored_units`, `active_time_specials`
- **Missions / combat:** `missions`, `mission_information`, `mission_reports`,
  `explored_planets`, `scheduled_tasks` (the db-scheduler jobs that fire pending
  missions — must be cleared too)
- **Alliances:** `alliances`, `alliance_join_request`
- **Legacy messaging:** `mensajes`, `carpetas`
- **Realtime websocket per-user state:** `websocket_events_information`,
  `websocket_messages_status`
- **Per-user security / auditing:** `audit`, `suspicions`, `track_browser`

It then runs `UPDATE planets SET owner = NULL, home = 0, special_location_id = NULL`
to release every planet, and finally delegates to
`ramdomize_special_locations.php` (unless `SKIP_RANDOMIZE=1`).

Everything not in that list is left untouched.

### ⚠️ After running on a live universe

As with the randomizer, if the universe is already running you **must** force a
cache reset from the admin panel afterwards, and ideally restart the `game-rest`
backend so no stale in-memory / db-scheduler state remains.

### ⚠️ Randomizer host limitation

`reset_universe.php` connects using the `<dbhost>` you pass, but the
`ramdomize_special_locations.php` step it delegates to **always connects to
`127.0.0.1`** (a limitation of that script). So when resetting a database that is
not on localhost, either run the reset from the database host, or pass
`SKIP_RANDOMIZE=1` and run the randomizer separately against the right host.

---

## `reset_universe.php` vs `export_world.php`

Both scripts share the same notion of "what is configuration/content vs what is
player data", but they use it in opposite directions:

- `export_world.php` **copies out** a curated set of content tables
  (`TABLES_TO_EXPORT`) to build a reusable world template. It is a read-only
  export; it never modifies the source database. While dumping `planets` it
  nulls `owner`/`home` so the exported world ships unowned.
- `reset_universe.php` **deletes** the player tables in place and keeps
  *everything else*, then re-randomizes special locations. It mutates the
  database.

### Important: reset preserves *more* than export exports

`export_world.php`'s `TABLES_TO_EXPORT` is a **curated** list — it intentionally
does not include every config table (a fresh universe gets some of those from
the schema seed / `init.sql` instead). `reset_universe.php` is the opposite: it
preserves **every** table except the explicit player-data whitelist.

So a number of configuration/content tables are **kept by the reset but are NOT
part of `export_world.php`'s output**, for example:

- `objects` (the `ObjectEntity` repository registry)
- `mission_types`
- `requirements` (note: export only ships `requirements_information` and
  `requirement_group`, not `requirements` itself)
- `requisitosespecialesderaza` (legacy special-race requirements)
- `system_messages`
- `sponsors`
- `admin_users` (admin accounts are preserved across a reset)
- `tor_ip_data`
- the Quartz runtime tables (`qrtz_*`)

In short: **anything `export_world.php` exports is also preserved by
`reset_universe.php`, plus the extra config tables above that export omits.**
The reset never touches configuration/content — it only removes players.

---

## `ramdomize_special_locations.php`

Clears `planets.special_location_id` and then assigns each row of
`special_locations` to a random eligible (unowned, unassigned) planet — within
the location's `galaxy_id` if it has one, otherwise anywhere.

```bash
php ramdomize_special_locations.php <dbuser> <dbpassword> <targetdatabase>
```

| Env var | Effect |
| --- | --- |
| `NOOP` | Do everything then `ROLLBACK` (dry run). |

> Connects to `127.0.0.1` only. Do **not** run against a live universe without
> forcing a cache reset in the admin panel afterwards.

---

## `export_world.php`

Dumps the curated content tables of a universe as an `INSERT` SQL script on
stdout (redirect it to a file). Read-only.

```bash
php export_world.php <dbhost> <dbuser> <dbpassword> <sourcedatabase> > world.sql
```

| Env var | Effect |
| --- | --- |
| `MYSQL_PORT` | Database port (default `3306`). |
| `DUMP_TARGET_SQL` | Show the queries issued against the target database. |
