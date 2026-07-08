# Universe Wiki — static-site generator plan

Goal: a per-universe, auto-regenerating **static HTML wiki** that documents everything the
universe offers — units (with attack rules / critical attacks / interception), upgrades,
time specials, special locations, galaxies, factions — including the *derived* knowledge
players can't see in-game easily: how a unit is unlocked, what a special location unlocks,
requirement chains, improvement effects, etc.

Status: **implemented** (2026-07-08) as the `owge-wiki-gen` workspace crate; verified
against the dev universe (544 units / 125 upgrades / 316 time specials / 87 special
locations, ~8 s full build in debug). Usage:

```bash
owge-wiki-gen once  --db mysql://user:pass@host:3306/db --out /path/wiki [--universe "Name"]
owge-wiki-gen watch --db ... --out ... [--interval 30] [--universe "Name"]
```

Implementation notes that refine the plan below (the rest of this doc is the original
design, kept for rationale):

- **Reuse over reimplementation** (Kevin's call): the generator loads everything through
  `owge-business` bo functions — `UnitBo/UpgradeBo/...::find_all`,
  `RequirementBo::find_requirements`, `RequirementGroupBo::find_groups` (OR-group
  semantics), `ImprovementBo::find_for_entity_shallow`,
  `UnitBo::find_used_critical_attack`, `AttackMissionManagerBo::find_attack_rule` and
  `UnitInterceptionFinderBo::find_his_or_inherited_speed_impact_group` /
  `find_interceptable_group_ids` (those three were widened from private to `pub` for
  this). Only presentation-only reads (`factions_unit_types`, `faction_spawn_location`,
  raw `units` FK columns) and the checksum watch are plain SQL in the crate.
- Extra sections beyond the plan: **unit types** (single page, `#t<id>` anchors),
  **galaxies** (single page, `#g<id>`), **travel groups** = speed impact groups
  (single page, `#s<id>`, with unlock requirements), an interception matrix on unit
  pages (can intercept / intercepted by), and a client-side search box
  (build-time `search-index.js` + ~30 lines of JS).
- Images are linked via the engine's own `/dynamic/<file>` URLs (`OWGE_DYNAMIC_URL`
  env overrides), so the wiki resolves images when served next to the game's nginx.
- Watch mode verified end-to-end: rename a unit → "change detected, waiting to settle"
  → one stable poll later → rebuild + atomic dir-swap republish. NULL checksum aborts
  the process by design.
- **Faction-grouped list pages**: units, upgrades and time specials are grouped by
  their BEEN_RACE requirement (scanning direct requirements and every OR-group; an
  entity unlockable by several factions appears under each), with an "Any faction"
  bucket last. Units and time specials are further split per faction into
  "Unlocked by default" (only BEEN_RACE/UPGRADE_LEVEL requirements) vs "Requires
  further unlocks" (any other requirement code present). Special locations keep a
  flat list.
- **Bilingual UI (en/es)**, same scope as game-frontend i18n: the wiki chrome (nav,
  headings, requirement phrasing, badges) is translated via the `src/i18n.rs` string
  table; entity names/descriptions stay in the DB's language (out of scope for now, per
  Kevin). Output is `<out>/en/…` + `<out>/es/…` (each subtree self-contained, per-language
  search index) with a browser-language redirect at `<out>/index.html` and a per-page
  language switcher in the header. Spanish terms deliberately reuse the game frontend's
  translation files (Mejoras, Especiales de tiempo, Tipos de unidad, Grupos de
  velocidad, Atributos, Atraviesa escudos, Sin asignar…).

## Decision summary

| Question | Decision |
|---|---|
| Framework | **No SSG framework** (Zola/Cobalt are Markdown-file-oriented and can't express the `object_relations` graph). Custom generator binary. |
| Where | New workspace member `rust-backend/owge-wiki-gen`, depending on `owge-business` (reuse entities, query layer, requirement/unlock semantics). |
| Templates | **Askama** (compile-time checked against typed structs; renamed field = compile error, not a blank wiki page). `minijinja` is the fallback if runtime-editable templates ever become a requirement. |
| Output | Plain HTML written to a per-universe directory, served by the existing nginx (same model as `static/` / `dynamic/<N>`). Zero runtime backend dependency. |
| Regeneration strategy | **Full rebuild every time** — config tables are hundreds of rows, a complete rebuild is sub-second, and it keeps the generator stateless. No incremental builds. |
| Change detection | **Poll `CHECKSUM TABLE`** on the watched tables every 30–60 s (option 1 below). No coupling to the Java admin writers, catches manual SQL edits too. |

## Change detection: `CHECKSUM TABLE`

One statement checksums all watched tables in a single round trip:

```sql
CHECKSUM TABLE
  units, unit_types, upgrades, upgrade_types, time_specials, special_locations,
  galaxies, factions, factions_unit_types, improvements, improvements_unit_types,
  objects, object_relations, object_relation__object_relation,
  requirements, requirements_information, requirement_group,
  interceptable_speed_group, speed_impact_groups, critical_attacks, critical_attack_entries,
  attack_rules, attack_rule_entries, configuration;
```

(Trim/extend the list to whatever the resolve layer actually reads — the list above is a
starting point; verify each name against `SHOW TABLES` before committing it.)

The watcher keeps the last `(table, checksum)` map in memory; any difference ⇒ regenerate,
then store the new map. Debounce: after detecting a change, wait one extra poll interval of
stability before rebuilding, so an admin mid-edit-session doesn't trigger a rebuild per save.

### ⚠️ The `NULL` checksum gotcha (root-caused 2026-07-08)

`CHECKSUM TABLE some_table` returns a **row with `Checksum = NULL` instead of hard-failing
when the table does not exist** (it also raises error 1146, but in batch mode / multi-table
statements / some drivers the resultset still arrives and the error is easy to miss).

That is exactly what happened here: the schema uses **plural table names** (`units`,
`upgrades`, `special_locations`, `galaxies`, `time_specials`, …) and the singular form was
being checksummed. Verified against the dev DB (MySQL **8.4.9**, container
`owge_backend_developer-db-1`, db `owge`):

```
CHECKSUM TABLE unit;   -- → NULL  (+ ERROR 1146 table doesn't exist)
CHECKSUM TABLE units;  -- → 2791246653  ✓
```

InnoDB is fine with the default (full-scan) mode. Only `CHECKSUM TABLE … QUICK` returns
NULL by design on InnoDB (QUICK needs MyISAM live checksums) — **never use `QUICK`**.

**Watcher rule:** treat a `NULL` checksum as a fatal misconfiguration (log + exit or alert),
never as "no change". A silently-NULL table would otherwise mean its edits never trigger a
rebuild.

### Fallback if checksums ever misbehave

Order-independent row hash, no `CHECKSUM TABLE` involved (column list can be generated from
`information_schema.columns` at startup):

```sql
SELECT COUNT(*) AS cnt,
       COALESCE(BIT_XOR(CRC32(CONCAT_WS('|', id, name, ...))), 0) AS h
FROM units;
```

Not needed today; documented only so the alternative isn't re-researched.

## Docker environments (2026-07-08)

**Dev** (`docker-ci/dev`): every launcher option now adds a `wiki_generator`
service (`profiles/wiki_generator.docker-compose.yml`, image
`images_creation/wiki_generator/Dockerfile`). It runs `owge-wiki-gen watch`
with `--out /var/owge_data/dynamic/wiki` — **inside the same dynamic-images
volume nginx mounts**, so its root-absolute `/dynamic/<file>` image URLs
resolve. The canonical URL is the clean **`/wiki/`** (a dedicated nginx
location with `alias`, directory indexes, and — in production — a 1h cache
instead of `/dynamic/`'s 365d, so regenerated pages aren't pinned stale in
browsers); `/dynamic/wiki/index.html` also works since the files physically
live there (that path has no directory indexes — `index fake` — which is why
the wiki's internal links are all explicit `…/index.html`). With a dockerized
database the launcher wires `mysql://root:1234@db:3306/owge` automatically;
with a local database (launcher option 5) `_withWikiGenerator` prompts for
ip / port / user / password / database. The image gates startup on the same
"`SELECT 1 FROM configuration`" readiness wait as the Rust backend image —
required because the watcher deliberately dies on NULL checksums, which is
what a half-initialized schema looks like.

**CI** (`docker-ci/ci`): the `main_reverse_proxy` config already carries the
`/wiki/` location (short cache, directory indexes), so the URL is ready; the
wiki *service* itself is deliberately not deployed yet — that's a
production-rollout decision. When wanted, it's the same recipe: one more
service running `owge-wiki-gen watch` with the universe's `OWGE_DB_*`
credentials and `--out ${DYNAMIC_IMAGES_DIR}/wiki`, plus a binary build step
in `jenkins_install_rust.sh`. Related fix found during this work: `rust:1`
images no longer ship rustfmt, which the `MysqlTemplate` derive shells out to —
`launch_rust_rest.sh` and both dev Rust Dockerfiles now `rustup component add
rustfmt` before building (previously they only worked thanks to a stale local
base image).

## Crate layout

```
rust-backend/
  owge-wiki-gen/
    Cargo.toml            # deps: owge-business, askama, sqlx/sea-orm (via workspace), tokio, clap
    templates/            # askama templates
      base.html           #   shared layout/nav
      index.html          #   universe overview
      unit.html  unit_list.html
      upgrade.html  upgrade_list.html
      time_special.html  special_location.html  galaxy.html  faction.html
    src/
      main.rs             # CLI: `once` (build & exit) | `watch` (checksum poll loop)
      watch.rs            # CHECKSUM TABLE poll + debounce + NULL-is-fatal rule
      model/              # resolved *view* of the universe, decoupled from DB entities:
                          #   WikiUnit { unit, unlocked_by: Vec<UnlockSource>, unlocks: …,
                          #              attack_rules_explained, interception, improvements }
      resolve/            # the real work: walk objects/object_relations/requirements_information
                          #   and INVERT the graph both ways:
                          #   - per content item: "how do I get this?" (requirement chains,
                          #     incl. HAVE_SPECIAL_LOCATION, faction, upgrade-level reqs)
                          #   - per source: "what does this unlock?" (special location page
                          #     lists units/upgrades it grants)
      render/             # askama render + write files, stable URLs: /wiki/unit/<id>-<slug>.html
```

Run modes:

- `owge-wiki-gen once --db <url> --out <dir>` — one build (deploy-time, manual).
- `owge-wiki-gen watch --db <url> --out <dir> --interval 30` — daemon; add a service to the
  per-universe compose file, output dir mounted where nginx serves it.

Atomic publish: render into `<out>.tmp/`, then swap (`rename` dir or symlink flip
`current -> build-<ts>`), so nginx never serves a half-written site.

## Resolve layer — where the complexity lives

This is the same `objects` / `object_relations` / `requirements_information` indirection the
engine uses (see CLAUDE.md "ObjectRelation / unlockable system"). The wiki must present it
inverted in both directions:

- **Unit/upgrade/time-special page:** full requirement chain, rendered human-readably
  ("Requires *Laser Research* level 3 **and** owning special location *Black Hole*"), incl.
  `requirement_group` OR-groups and second-level relations
  (`object_relation__object_relation`).
- **Special location page:** everything whose requirements reference it via
  `HAVE_SPECIAL_LOCATION` — i.e. "conquering this planet unlocks: …".
  Note: the Rust backend currently has a bug *granting* these unlocks at runtime
  (`docs/BUG-SPECIAL-LOCATION-UNLOCK.md`); the wiki only *reads* the declared requirements,
  so it's unaffected — but wiki pages that look wrong are a cheap parity-bug detector.
- **Unit page combat section:** explain `attack_rules` / `attack_rule_entries` (can/can't
  attack which unit types), `critical_attacks` multipliers, speed-impact/interception.
- **Faction page:** initial resources/energy, faction-restricted unit types, spawn behavior.

Deliberately excluded: anything player-state (obtained_units, missions, reports). The wiki
is universe *configuration* only.

## Open items (decide when starting implementation)

- Exact watched-table list (derive from what `resolve/` reads; keep the SQL and the code in
  one place so they can't drift).
- Whether images (unit/upgrade pictures from the dynamic assets dir) are linked by URL or
  copied into the wiki output for full self-containment.
- i18n: probably out of scope for v1 (game content itself is single-language per universe).
- Search: a build-time-generated JSON index + a few lines of client-side JS is enough; no
  server component.
