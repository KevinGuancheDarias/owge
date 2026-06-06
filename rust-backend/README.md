# OWGE Rust backend (port of `business` + `game-rest`)

A work-in-progress Rust reimplementation of the OWGE engine (`business`) and web
app (`game-rest`), exposing the **same HTTP API and socket.io websocket API** so
the existing Angular frontend and deployed universes can talk to it unchanged.

- **Web framework:** [axum](https://github.com/tokio-rs/axum)
- **Database:** [sqlx](https://github.com/launchbadge/sqlx) over the *existing*
  MySQL/MariaDB schema (`business/database/02_schema.sql`) — no migrations, no
  ORM, hand-written SQL in the `Bo` layer.
- **Auth:** dual JWT verification — HMAC (`SECRET`) **and** RSA
  (`RSA_KEY`) — bug-for-bug compatible with the Java/`kevinsuite-java` scheme.
- **Realtime:** socket.io (planned via `socketioxide`, milestone M4).

> Status: **M0–M2 complete** (foundation, read-only game state, and the admin /
> non-mission write side, including the Alliance subsystem and the
> requirement/improvement write engine). M3 (missions + scheduler) is next; see
> the milestone plan below.

## Workspace layout

```
rust-backend/
├── owge-business/        # lib crate — the engine (= Java `business` jar)
│   ├── src/
│   │   ├── jwt.rs         # ★ dual-mode JWT (SECRET-HMAC + RSA_KEY) — tested
│   │   ├── config.rs      # env + `configuration` table settings
│   │   ├── db.rs          # sqlx MySQL pool
│   │   ├── error.rs       # OwgeError ≈ Java exception hierarchy
│   │   ├── model/         # sqlx::FromRow entities (= JPA @Entity)
│   │   └── bo/            # *Bo services (all game logic lives here)
│   └── tests/rsa_compat.rs
└── owge-rest/            # bin crate — the web app (= Java `game-rest` WAR)
    └── src/
        ├── main.rs        # boot, CORS, serve
        ├── state.rs       # AppState (= Spring bean graph) + JWT bootstrap
        ├── auth.rs        # GameUser / AdminUser extractors (= JWT filters)
        ├── http_error.rs  # OwgeError -> JSON {exceptionType,message}
        └── routes/        # thin controllers (open / game / admin)
```

The split mirrors the Java monorepo exactly: entity + repository + `*Bo` logic
lives in `owge-business`; only the HTTP endpoint lives in `owge-rest`.

## Authentication — how it matches the Java backend

The Java backend (`kevinsuite-java` `JwtAuthenticationFilter` +
`TokenConfigLoader` implementations) verifies tokens two ways, chosen per scope:

| Scope | Java loader | Method | Rust equivalent |
|-------|-------------|--------|-----------------|
| `/game/**` prod | `SgtTokenConfigLoader` | `RSA_KEY` (public key PEM) | `TokenConfig::rsa(public_pem)` |
| `/game/**` dev  | `DevelopmentSgtTokenConfigLoader` | `SECRET` (HMAC) | `TokenConfig::secret(...)` |
| `/admin/**`     | `AdminTokenConfigLoader` | `SECRET` (HMAC) | `TokenConfig::secret(...)` |

Selection follows the same trigger: the **`rsaKeys` profile**
(`OWGE_PROFILES=rsaKeys`) switches the game login to RSA, reading the public key
from `OWGE_RSA_PUBLIC_KEY` (default `/var/owge_data/keys/public.key`). The token
shape is identical: a `data` claim holding `{ id, username, email }`, validated
for signature + expiry (with `OWGE_CLOCK_SKEW` leeway).

**Compatibility gotchas captured in code (`jwt.rs`):**

1. **HMAC secret bytes.** OWGE's `JwtService.buildToken` signs admin tokens with
   `secret.getBytes()` (**raw** UTF-8 bytes). Some jjwt `setSigningKey(String)`
   versions instead **Base64-decode** the secret. `SecretEncoding` makes this
   explicit; the default is `Raw` (matching how OWGE mints admin tokens). If an
   environment's existing tokens were minted via the Base64 path, set the game
   loader to `SecretEncoding::Base64`. *(Verify against a real production token
   when cutting over — this is the #1 interop risk.)*
2. **RSA PEM formats.** The Java side loads a PKCS#8 private key and an X.509/SPKI
   (`BEGIN PUBLIC KEY`) public key. `jsonwebtoken::DecodingKey::from_rsa_pem`
   accepts the SPKI public key directly; proven by `tests/rsa_compat.rs`.
3. **Unsigned ints / `tinyint(1)` / `double` / `datetime`** map to `u32/u64`,
   `bool`, `f64`, `chrono::NaiveDateTime` respectively in the `model` structs.
4. **JSON field naming** uses `#[serde(rename = "camelCase")]` to match Jackson.

## Build & run

```bash
cd rust-backend
cargo build
cargo test -p owge-business        # JWT (HMAC + RSA) compatibility tests
./run-local.sh                     # runs against the dc12 CLONE (see below)
./watch-local.sh                   # runs in watch mode (requires cargo-watch)

# Or run against any OWGE database directly:
OWGE_DB_JDBC_URL='mysql://owge:owge@127.0.0.1:3306/owge_dc1' \
OWGE_SERVER_PORT=8080 \
  cargo run -p owge-rest
# RSA game login (production parity):
OWGE_PROFILES=rsaKeys OWGE_RSA_PUBLIC_KEY=/var/owge_data/keys/public.key ...
```

### Development database — a clone, not the live universe

Dev runs against a **clone** of the dc12 universe so the live game DB is never
touched. The clone lives in `sgalactica_java_12_base_fast` (the `OWGEu4` user
lacks global `CREATE`, so the clone reuses that granted-but-empty schema name).
Re-create it any time:

```bash
export MYSQL_PWD=<dc12 db pass>
mysql  -h127.0.0.1 -uOWGEu4 -e "DROP DATABASE IF EXISTS sgalactica_java_12_base_fast; \
                                CREATE DATABASE sgalactica_java_12_base_fast CHARACTER SET utf8;"
mysqldump -h127.0.0.1 -uOWGEu4 --single-transaction --quick --routines sgalactica_java_12 \
  | mysql -h127.0.0.1 -uOWGEu4 sgalactica_java_12_base_fast
```

DB credentials live in `.env.local` (gitignored), consumed by `run-local.sh`.
The M0 endpoints are verified end-to-end against this clone (open endpoints,
game-JWT auth, `adminLogin`).

### Environment variables

| Var | Default | Purpose |
|-----|---------|---------|
| `OWGE_DB_JDBC_URL` / `DATABASE_URL` | — (required) | `mysql://user:pass@host:port/db` |
| `OWGE_DB_MAX_CONNECTIONS` | 16 | pool size |
| `OWGE_SERVER_HOST` / `OWGE_SERVER_PORT` | `0.0.0.0` / `8080` | bind address |
| `OWGE_PROFILES` | — | `rsaKeys` enables RSA game login |
| `OWGE_RSA_PUBLIC_KEY` / `OWGE_RSA_PRIVATE_KEY` | `/var/owge_data/keys/*.key` | RSA PEM paths |
| `OWGE_CLOCK_SKEW` | 300 | JWT leeway seconds |
| `OWGE_GAME_SECRET_ENCODING` | `raw` | `raw` or `base64` — how the game HMAC `JWT_SECRET` becomes key bytes (the #1 interop knob; pin against a real token) |
| `OWGE_CORS_ORIGINS` | `*` | comma-separated allowed origins |

## Implemented so far

**M0 — foundation** (verified against the dc12 clone):
- `GET  /open/clock`, `GET /open/configuration`
- `POST /game/adminLogin` — full `AdminUserBo.login` (lookup, enabled check, username sync, mint)
- `GET  /game/faction/findVisible`

**M1 — read-only game state** (verified against the dc12 clone):
- `GET /game/user/exists`
- `GET /game/websocket-sync?keys=…` — the frontend hydration path; **15 sync
  handlers** wired into the dispatch registry (`websocket/sync.rs`):
  `user_data_change`, `planet_owned_change`, `planet_user_list_change`,
  `unit_type_change`, `unit_obtained_change`, `upgrade_types_change`,
  `obtained_upgrades_change`, `running_upgrade_change` (M3-stub),
  `time_special_change`, `system_message_change`, `tutorial_entries_change`,
  `visited_tutorial_entry_change`, `speed_impact_group_unlocked_change`,
  `unit_unlocked_change`/`unit_requirements_change` (M2-stub). Each records the
  `websocket_events_information` watermark.
- `GET /game/ranking`, `GET /game/galaxy/navigate`, `GET /game/time_special`(+`/{id}`),
  `GET /game/tutorial/entries`
- Mission-system sync keys (`unit_mission_change`, `enemy_mission_change`,
  `missions_count_change`, `mission_report_change`, `unit_build_mission_change`)
  arrive with the mission engine in **M3**.

**M2 — admin CRUD** (standard controllers done; verified vs the clone) — all
behind the `AdminUser` extractor, exercised with a real admin token from
`POST /game/adminLogin`:
- `admin/galaxy` — full `CrudRestServiceTrait` + `{id}/has-players` (verified
  create→update→delete cycle).
- `admin/unit_type` — CRUD + `{id}/attackRule` / `{id}/criticalAttack` unset.
- `admin/upgrade_type` — CRUD (verified create→update→delete).
- `admin/translatable` — CRUD + nested `{id}/translations` sub-resource.
- `admin/tutorial_section` — CRUD + `entries` / `availableHtmlSymbols`.
- `admin/configuration` — list + save-by-name (POST forbids existing key;
  PUT updates; `MISSION_TIME_*` ≥10 guard verified) + delete.
- `admin/system-message`.
- **Content-type & combat admin CRUD** (verified vs clone): `admin/unit` (543),
  `admin/upgrade` (125), `admin/faction` (15), `admin/special-location` (85),
  `admin/image_store` (1139), plus `admin/attack-rule`, `admin/critical-attack`,
  `admin/rules` (write routes; their GET-list reads are a known gap → 405).
  CrudWithFull controllers expose `{id}/requirements` (read **and write** — see
  the requirement/improvement write engine below). Image **upload** stays a
  deliberate `501` (M5).
- **Alliance subsystem** — `game/alliance` (11 endpoints, `AllianceBo`): `findAll`,
  `{id}/members` (email blanked, as Java does), `POST`/`PUT` save (create gates on
  `DISABLED_FEATURE_ALLIANCE`; update is owner-only, name+description), `DELETE`
  (owner-only; unsets members + purges join requests), `listRequest`/`my-requests`,
  `requestJoin`, `acceptJoinRequest` (with the `ALLIANCE_MAX_SIZE[_PERCENTAGE]`
  full-alliance check), `rejectJoinRequest`, `leave`. Auditing (M5) and websocket
  emits (M4, none on these paths) are the only omissions.
- **Requirement / improvement write engine** (admin write side; SQL verified vs
  the clone in rolled-back transactions). `ObjectRelationBo::find_object_relation_or_create`
  + `RequirementBo::add_requirement_from_dto`/`delete_requirement_information` back
  `POST`/`DELETE admin/{unit,upgrade}/{id}/requirements`. `ImprovementBo` backs
  `GET`/`PUT {id}/improvement` and the `{id}/improvement/unitTypeImprovements`
  sub-resources (list/add/delete with the `improvements_unit_types` engine) for
  `admin/{unit,faction,upgrade,special-location}`. `RequirementGroupBo` backs the
  `admin/speed-impact-group/{id}/requirement-group…` sub-resources (group row +
  its `REQUIREMENT_GROUP` object relation, linked as a slave of the parent's
  relation; per-group requirements via the same add path). `FactionBo` backs
  `PUT admin/faction/{id}/unitTypes` and `/spawn-locations` (rewrite
  `factions_unit_types` / `faction_spawn_location`). Cache eviction +
  `user_improvements_change` emission are M4 (`// TODO(M4)` at the call sites);
  one stricter input check (`AMOUNT` improvement requires a unit type with a max
  count) is left as a TODO.
- **Requirement engine** (read): `RequirementBo::find_faction_unit_level_requirements`
  fills the last M1 sync key `unit_requirements_change` (verified: 20 units with
  resolved upgrade-level requirements), and `find_requirements` backs the admin
  `{id}/requirements` sub-resource.
- **Player non-mission mutations** (verified vs clone): `POST/DELETE
  game/planet-list`, `POST game/system-message/mark-as-read`,
  `POST game/tutorial/visited-entries`, `POST game/track-browser/{warn,error}`,
  `GET/PUT game/twitch-state` (PUT enforces `can_alter_twitch_state`; the
  `twitch_state_change` emit is deferred to M4), `POST game/report/mark-as-read`
  and `POST game/report/mark-as-read-before-date/{date}`.
- **Bespoke admin endpoints** (verified vs clone): `GET admin/debug`;
  `admin/admin-user` (`GET` list, `PUT {id}` add-admin, `DELETE {id}`);
  `admin/users` (`GET with-suspicions` with per-user suspicion counts,
  `GET {id}`); `admin/speed-impact-group` full CRUD **+ the
  `CrudWithRequirementGroups` sub-resources** (now ported — see the write engine
  above). `admin/users` `DELETE {id}` (`UserDeleteService` cascade) and
  `GET {id}/suspicions` (embeds `AuditDto`) are deferred to **M5** (auditing) →
  `501`.
- **Unlock engine** (`ObjectRelation` indirection) — **both sides ported.**
  *Read:* `UnlockedRelationBo::find_unlocked_reference_ids(user, object_type)`
  resolves `unlocked_relation → object_relations(<TYPE>)` to concrete entity ids
  (fills `unit_unlocked_change`, `speed_impact_group_unlocked_change`).
  *Write — the requirement-trigger engine* (`bo/requirement_engine.rs`): a
  faithful port of `RequirementBo`'s write side —
  `processRelationList`/`processRelation`/`checkRequirementsAreMet`/
  `registerObtainedRelation`/`unregisterLostRelation`, **all** requirement-type
  checks (`UPGRADE_LEVEL`, `HAVE_UNIT`, `UNIT_AMOUNT`, `BEEN_RACE`, `HOME_GALAXY`,
  `HAVE_SPECIAL_LOCATION`, `HAVE_SPECIAL_AVAILABLE`, `HAVE_SPECIAL_ENABLED`,
  `UPGRADE_LEVEL_LOWER_THAN`), requirement-group OR-semantics (a master unlocks
  when any group slave is unlocked), and the `obtained_upgrades` side effects.
  Every function takes a `&mut MySqlConnection` so a trigger runs inside the
  caller's transaction. The `triggerFactionSelection`/`triggerHomeGalaxySelection`
  triggers are exposed; the remaining triggers (level-up / unit-build / special
  location / time-special) are a thin wrapper away once M3 calls them.
- **`GET game/user/subscribe`** (provisioning) — **done + verified vs clone.**
  `UserStorageBo::subscribe` validates the faction, picks a random free spawn
  planet in a faction spawn galaxy (universe-wide fallback), seeds starting
  resources, marks the planet owned + home, inserts the `user_storage` row, and
  fires the faction/home-galaxy triggers — all in one transaction. Verified: a
  fresh faction-1 user's unlock set **exactly matches** an independent SQL
  computation (12 base upgrades unlocked, 0 units — units gate on upgrade levels
  the fresh user lacks), idempotent on re-subscribe.
- **M2 is closed.** The admin write side (requirements, improvements,
  requirement groups, faction overrides) and the Alliance subsystem above are
  done; the engine SQL is verified against the clone. The endpoints that still
  return `501` are all blocked on a *later* milestone, not on M2:
  - `game/planet/leave`, `game/time_special/activate` — **M3** (mission engine /
    db-scheduler: the running-build guard and the `TIME_SPECIAL_EFFECT_END` task).
    Their requirement-trigger needs (`triggerSpecialLocation`/
    `triggerTimeSpecialStateChange`) are already unblocked.
  - `game/report/findMy` (deprecated) — **M3** (needs the mission join).
  - `admin/image_store` upload, `admin/users` `DELETE {id}` / `{id}/suspicions` —
    **M5** (image storage / auditing).
  - `admin/cache/drop-all`, `admin/system/*` — depend on the cache layer
    (taggable-cache) + **M3/M4**.
  - `admin/rules` `type-descriptor`/`item-type-descriptor` — need the rule
    type-provider registry. Note `admin/attack-rule` and `admin/critical-attack`
    have **no** GET endpoints in the Java controllers, so the absence of GET there
    is parity, not a gap.

## Milestone plan

- **M0 — Foundation** ✅ config, db pool, error type, dual-mode JWT + auth
  extractors, open endpoints, admin login.
- **M1 — Read-only game state:** user data, planets, obtained units/upgrades,
  unit/upgrade/faction catalogs, rankings, the websocket-sync REST snapshot.
- **M2 — Admin CRUD + non-mission writes** ✅ the generic CRUD-trait controllers
  (units, upgrades, factions, system messages, …), the requirement/improvement/
  requirement-group write engine, faction overrides, player non-mission
  mutations, `user/subscribe`, and the **Alliance** subsystem. (Image *upload*
  defers to M5.)
- **M3 — Missions + scheduler:** the async game loop — port the db-scheduler
  `scheduled_tasks` runner, mission processors, combat math, MySQL named locks.
- **M4 — Realtime:** socket.io server (`socketioxide`), the `authentication`
  handshake, per-user rooms, and all `*EventEmitter` deltas + the
  `websocket_events_information` last-known-value sync.

A detailed, inventory-driven roadmap (every controller → module mapping, all
~75 tables, every websocket event) is generated under `docs/` — see
`PORTING-ROADMAP.md`.
```
