Confirmed all the load-bearing crypto details:

- **HMAC**: `JwtService.buildToken` uses `secret.getBytes()` (platform-default charset, effectively UTF-8) → raw bytes, NOT base64-decoded. The verify path (`Jwts.parser().setSigningKey(String)` in JJWT 0.9.x) decodes the string with `Base64.decode` **only if** it's a `String` overload... actually in JJWT 0.9.x the `setSigningKey(String)` BASE64-decodes it. There's an asymmetry to flag: admin signs with raw `getBytes()` but the generic `setSigningKey(String)` parser path base64-decodes. This is a real compatibility trap worth calling out explicitly.
- **RSA**: PEM via BouncyCastle `PemReader` → `PemObject.getContent()` is the raw DER bytes; public key = X509 `SubjectPublicKeyInfo`, private key = PKCS8. Algorithm chosen from `JWT_HASHING_ALGO` config (default HS256).
- **Claims**: `{sub, iat, exp, data:{id,username,email}}`, `data.id` is a `Number`.

I have everything. Here is the roadmap.

---

# OWGE Java → Rust Port: Implementation Roadmap

axum + sqlx (MySQL) + socketioxide. This is a **port of an existing live system**: byte-for-byte JWT compatibility, exact JSON field names, and exact DB column types are hard constraints, because the Rust backend must coexist with the unchanged Angular frontend, the unchanged external account/SSO system, and existing universe databases.

## 0. Non-negotiable compatibility constraints (read first)

These shape every later decision:

1. **JWT secret is a raw UTF-8 byte secret, with an asymmetry to reproduce.** `JwtService.buildToken` signs HMAC with `secret.getBytes()` (raw UTF-8). But the kevinsuite *verify* path (`Jwts.parser().setSigningKey(String)`, JJWT 0.9.x) **Base64-decodes** the secret string before using it as the HMAC key. So *issuing* (admin token) and *verifying* (player + admin tokens) treat the same `JWT_SECRET` string differently. The player token is issued by the external account system, which also uses `signWith(algo, secret)` — verify against that side too before locking behavior. **Action: in M0, write a conformance test that takes a real token from the running Java stack and a `JWT_SECRET` value and confirms which interpretation (raw bytes vs base64) actually validates it; encode that as the canonical behavior.** Do not guess.
2. **JSON is camelCase, entity-shaped.** The frontend consumes DTOs with exact Java field names (`eventName`, `lastSent`, `myUnitMissions`, `terminationDate`, `primaryResource`, …). Every Rust response struct needs `#[serde(rename_all = "camelCase")]` and, where Java names are irregular, explicit `#[serde(rename = "...")]`.
3. **Unsigned integers everywhere, with logical-FK signedness mismatches.** Most PKs/FKs are `UNSIGNED` → `u16/u32/u64`, but `user_storage.id` is signed `int` (`i32`), `missions.source_planet/target_planet` are signed `bigint` (`i64`) referencing unsigned `planets.id` (`u64`). **Match the literal column type per field, not the referenced PK**, or sqlx decode panics. sqlx requires `mysql` driver unsigned-aware decoding (it has it natively; just use `u8/u16/u32/u64`).
4. **All times are UTC `NaiveDateTime`**; DB columns are `datetime`/`timestamp` with no tz. Use `chrono::NaiveDateTime`; treat as UTC. `timestamp(6)` (scheduled_tasks) needs microsecond precision. The frontend does its own clock-skew handling via `/open/clock`.
5. **tinyint flag ambiguity.** Only `tinyint(1)` decodes as `bool` in sqlx; bare `tinyint`/`tinyint UNSIGNED` decode as `i8`/`u8`. Many flags (`missions.resolved`, `missions.invisible`, `units.is_unique`, etc.) are bare `tinyint`. Decode as `i8`/`u8` and convert with a helper, or wrap in a newtype `DbBool(i8)`.
6. **`UserStorage.id` mirrors the external account id** (it is the JWT `data.id`); it is **not** auto-increment. Provisioning happens via `GET game/user/subscribe`.

---

## 1. Cargo workspace layout

```
owge-rs/
├── Cargo.toml                      # [workspace] members + shared deps in [workspace.dependencies]
├── crates/
│   ├── owge-business/              # lib: the engine (entities, repos, *Bo logic, mission system)
│   │   ├── src/
│   │   │   ├── lib.rs
│   │   │   ├── config.rs           # env + DB-backed configuration table loader
│   │   │   ├── error.rs            # OwgeError enum + IntoResponse-friendly mapping hook
│   │   │   ├── db/
│   │   │   │   ├── mod.rs          # MySqlPool factory, named-lock connection-pinning helper
│   │   │   │   └── lock.rs         # GET_LOCK/RELEASE_LOCK keyed serialization (planet_/user_)
│   │   │   ├── entities/           # one module per table: structs + insert structs
│   │   │   ├── repositories/       # sqlx query fns per aggregate (findByIdOrDie etc.)
│   │   │   ├── dto/                # response structs (camelCase serde) + From<Entity>
│   │   │   ├── security/
│   │   │   │   ├── jwt.rs          # decode/verify (SECRET + RSA), TokenUser
│   │   │   │   └── token_config.rs # TokenConfigLoader equivalent (mode selection)
│   │   │   ├── services/           # *Bo equivalents (alliance, faction, planet, unit, upgrade…)
│   │   │   ├── requirement/        # ObjectRelation / unlock / requirement engine
│   │   │   ├── mission/
│   │   │   │   ├── mod.rs
│   │   │   │   ├── scheduler.rs     # db-scheduler-equivalent poller over scheduled_tasks
│   │   │   │   ├── realization.rs   # DbSchedulerRealizationJob equivalent (dispatch+retry)
│   │   │   │   ├── time.rs          # MissionTimeManagerBo math
│   │   │   │   ├── interception.rs
│   │   │   │   ├── attack.rs        # AttackMissionManagerBo combat math
│   │   │   │   └── processor/       # one file per MissionProcessor
│   │   │   ├── websocket/
│   │   │   │   ├── mod.rs           # SocketIoService equivalent (targeting, send pipeline)
│   │   │   │   ├── events.rs        # event-name constants + emitter helpers
│   │   │   │   └── sync.rs          # SyncSource registry + findWantedData
│   │   │   └── tx.rs               # transaction + do_after_commit hook collection
│   │   └── Cargo.toml
│   └── owge-rest/                  # bin: the runnable web app (WAR replacement)
│       ├── src/
│       │   ├── main.rs            # build router, start axum + socketioxide + scheduler
│       │   ├── state.rs           # AppState { pool, config, socket_io, ... }
│       │   ├── middleware/
│       │   │   ├── auth.rs        # game/admin JWT extractors (axum FromRequestParts)
│       │   │   ├── cors.rs
│       │   │   └── ratelimit.rs   # WebsocketSyncRateLimitFilter (per-IP 60s window)
│       │   ├── routes/
│       │   │   ├── game/          # one module per game controller group
│       │   │   ├── admin/         # one module per admin controller group
│       │   │   └── open/          # clock, configuration, websocket-sync, sponsor
│       │   └── ws.rs              # socketioxide namespace + authentication handler
│       └── Cargo.toml
└── migrations/                    # reuse existing 02_schema.sql; sqlx offline metadata
```

Two crates only (matching `business` lib + `game-rest` bin). Sub-systems are modules, not crates, to keep compile-iteration fast early; promote to crates only if compile times demand it.

---

## 2. Crate selection

| Concern | Crate | Notes |
|---|---|---|
| HTTP server | `axum` 0.7 | matches the "thin controller" layering; `FromRequestParts` for auth/user extraction |
| Async runtime | `tokio` (full) | scheduler workers, socket server, http all on it |
| DB | `sqlx` 0.8, features `mysql, chrono, runtime-tokio-rustls, macros` | **unsigned ints supported natively**; use `query!`/`query_as!` with offline metadata (`sqlx prepare`) so CI builds without a DB |
| Decimal (qrtz only) | `rust_decimal` + sqlx `rust_decimal` feature | only needed if porting Quartz tables; defer (Quartz is out of scope for the mission loop) |
| Time | `chrono` | `NaiveDateTime` UTC; serde via `chrono` serde feature |
| JWT | `jsonwebtoken` 9 | supports HS256/384/512 + RS256; **see §4 for the secret-bytes and PEM caveats** |
| RSA PEM parsing | built into `jsonwebtoken` (`DecodingKey::from_rsa_pem` / `from_rsa_der`) | but kevinsuite uses raw X509/PKCS8 DER from `PemObject.getContent()`, not standard PEM headers — may need `rsa` + `pkcs8`/`spki` crates to reconstruct (see risks) |
| Socket.IO | `socketioxide` 0.14 + `tower`/`axum` integration | **must speak the same Socket.IO/Engine.IO protocol version as `socket.io-client` the frontend pins** — verify protocol v4 (EIO=4) compatibility early |
| JSON | `serde`, `serde_json` | camelCase rename everywhere |
| Config / env | `figment` or hand-rolled `std::env` + DB `configuration` table loader | OWGE reads many params from the DB `configuration` table at runtime, not just env |
| Errors | `thiserror` (library) + a single `OwgeError` → `IntoResponse` | mirror `SgtBackend*Exception` → HTTP status mapping |
| Logging | `tracing` + `tracing-subscriber` | replace the TRACE-level lock logging package |
| Password/crypto misc | `base64`, `hex`, `sha2` (image checksum), `bcrypt`/account-side N/A | image_store checksum is md5(char(32)); use `md-5` crate |
| Background jobs | hand-rolled poller over `sqlx` (do **not** adopt a generic queue crate) | the optimistic-version-claim + heartbeat-recovery semantics are load-bearing and must match the existing `scheduled_tasks` table |
| Retry/backoff | `backoff` or hand-rolled | for the `CannotAcquireLockException`-equivalent lock retry |
| Tests | `sqlx::test`, `wiremock` (account system), `tokio::test` | golden tests for JWT + JSON shape |

---

## 3. Phased milestones (ordered by dependency and value)

### M0 — Foundation & auth (gate for everything)
**Goal: a Rust process that authenticates the real frontend's JWT, serves the open endpoints, and issues an admin token byte-compatible with the Java one.**

- Workspace skeleton, `AppState`, `MySqlPool` from `OWGE_DB_URL/USER/PASS`, `tracing`.
- `OwgeError` enum + `IntoResponse`. Map the Java exception → status conventions: invalid input → 400, faction/entity-not-found → 404, access denied → 403, rate-limit → 429.
- **Config loader**: env first, then DB `configuration` table (`findConfigurationParam`, `findOrSetDefault`, `findAllNonPrivileged`, `findPrivilegedReadOnly`). Many later behaviors (`ZERO_BUILD_TIME`, `UNIVERSE_ID`, `JWT_SECRET`, `MISSION_SPEED_*`, `TWITCH_STATE`) read this.
- **JWT, both modes** (`security/jwt.rs`):
  - `SECRET` (HMAC, default/dev): `JWT_SECRET` from DB config. Verify with `jsonwebtoken` HS256/384/512. Resolve the raw-bytes-vs-base64 question with a conformance test against a real token (§0.1).
  - `RSA_KEY` (prod, `rsaKeys` profile): public key from `/var/owge_data/keys/`, X509 `SubjectPublicKeyInfo` DER. Private key PKCS8 DER for *signing admin tokens only*. Clock skew: 300s (RSA) / 3600s (SECRET) — `jsonwebtoken::Validation::leeway`.
  - `TokenUser { id: i64, username, email }` from `data` claim. Note `data.id` is a JSON `Number`; deserialize tolerantly (it may arrive as int).
- **Auth middleware/extractors**: `GameUser` and `AdminUser` extractors (axum `FromRequestParts`) that parse `Authorization: Bearer`, verify, and expose the principal. Skip `OPTIONS` (CORS preflight). STATELESS — no session.
- **`UserSessionService` equivalent**: `find_logged_in` (token-only lightweight user), `find_logged_in_with_details` (load `user_storage` by id + self-heal email/username drift).
- **Endpoints in M0:**
  - `POST game/adminLogin` → admin token (sign HMAC with `getBytes()` semantics; algo from `JWT_HASHING_ALGO`, default HS256; claims `{sub,iat,exp,data:{adminUser}}`).
  - `GET /open/clock` (current UTC `Date`), `GET open/configuration` (non-priv + priv-read-only), `GET /open/sponsor`.
  - `GET /open/websocket-sync/rule_change`, `/speed_group_change`.
- **CORS** config matching the frontend origin.
- **Deliverable test**: real frontend JWT validates; admin login round-trips against the Java verifier.

### M1 — Read-only game state (high value, low risk)
**Goal: a logged-in player can load the game read surface.** No mutation, no scheduler, no websocket yet.

- Entities + repositories + DTOs for: `user_storage`, `factions`, `units`, `unit_types`, `upgrades`, `upgrade_types`, `planets`, `galaxies`, `time_specials`, `alliances`, `ranking`, `object_relations`, `obtained_units`, `obtained_upgrades`.
- The **DtoFromEntity pattern** → idiomatic Rust `From<Entity> for Dto` (or a `to_dto`); no reflection. Replicate the lazy-guarding logic as explicit "load nested only if joined."
- Endpoints: `game/user/exists`, `game/faction/findVisible`, `game/time_special` (+`{id}`), `game/upgradeType/`, `game/ranking`, `game/galaxy/navigate`, `game/alliance` (+`{id}/members`, `listRequest`, `my-requests`), `game/unit/{unitId}/criticalAttack`, `game/twitch-state` (GET).
- **`GET game/websocket-sync?keys=`** read path: build the `SyncSource` registry and `findWantedData` returning `{data, lastSent}` per key (re-saving the `websocket_events_information` watermark). This is high-value because the frontend's entire offline cache depends on it; implementing the read side here (before websocket) lets the UI hydrate over HTTP.
- `WebsocketSyncRateLimitFilter` (per-IP, 60s fixed window, default 10/min, `429`; IP from `X-OWGE-RMT-IP`).

### M2 — CRUD / admin & remaining player mutations (breadth, mechanical)
**Goal: admin panel works; player non-mission mutations work.**

- **CRUD trait → Rust generic or macro.** The Java trait system (`WithRead`/`WithDelete`/`CrudRestServiceTrait`/`+Improvements`/`+Requirements`/`+RequirementGroups`/`Full`) is interface-default-method composition. In Rust, model each as a small generic helper function set (or a `crud_routes!` macro) parameterized over `(id type, entity, dto, supported_ops)`. Don't over-abstract: most admin controllers just need GET-all/GET-one/POST/PUT/DELETE plus a couple of sub-resources; a macro that emits the 5 standard handlers + opt-in `improvement`/`requirements`/`requirement-group` blocks is enough.
- **Improvement / Requirement / Unlock engine** (`ObjectRelation` indirection) — needed by admin sub-resources *and* by missions/subscribe later, so build it here: `object_relations`, `object_relation__object_relation`, `requirements`, `requirements_information`, `unlocked_relation`, `RequirementBo` triggers (`triggerFactionSelection`, `triggerHomeGalaxySelection`).
- **`GET game/user/subscribe`** (provisioning: faction validate, random spawn planet, seed resources, mark planet owned/home, fire requirement triggers, audit). Depends on requirement engine.
- Player mutations that aren't missions: `game/planet-list` (POST/DELETE), `game/planet/leave`, `game/report/*`, `game/system-message/mark-as-read`, `game/tutorial/*`, `game/time_special/activate`, `game/twitch-state` PUT, `game/track-browser/*`, `game/deliver-backdoor/ping-user`.
- All admin controllers (see §5 table).

### M3 — Missions + scheduler (the core game loop, highest complexity)
**Goal: build/research/fleet missions register, schedule, fire, process, resolve.** This is the heart; see Inventory §5.

- **Keyed serialization** (`db/lock.rs`): replicate MySQL `GET_LOCK`/`RELEASE_LOCK` for `planet_lock_<id>` / `user_lock_<id>`. **Critical: GET_LOCK is session-scoped — you must pin one pooled connection for the whole locked critical section** (`pool.acquire()` and hold it). Preserve: re-entrant per-execution tracking (task-local, not thread-local), globally-sorted acquisition order, up-front superset acquisition (`resolvePlanetsToLock` = source/target planets + all planets of both owners), 10s timeout × 5 attempts → retryable error.
  - *Recommended simplification for single-instance deploys*: an in-process keyed `tokio::Mutex` map with the same ordering semantics; keep the SQL named-lock impl behind a trait so multi-instance can swap it in. Document the choice.
- **Scheduler poller** (`mission/scheduler.rs`): tokio task, 3s interval, `SELECT ... WHERE execution_time <= now() AND picked = 0`, atomic claim via `UPDATE ... SET picked, picked_by, last_heartbeat, version+1 WHERE version = ?` (optimistic concurrency), spawn worker, delete row on success, heartbeat (5m) + stale-heartbeat re-claim. `task_name='mission-run'`, `task_instance = mission.id`, no payload. `execution_time = now + requiredTime − 2s`.
  - *Recommended simplification (per Inventory §5)*: insert the `scheduled_tasks` row in the **same transaction** as the mission (the Java code does it non-transactionally only as a historical artifact; same-tx is safer).
- **Realization/dispatch** (`mission/realization.rs`): load mission, skip if missing/resolved (idempotent), branch BUILD_UNIT/LEVEL_UP → `MissionBo` path vs unit missions → `UnitMissionBo` path. Catch-all → app retry: persisted `attemps` counter (cap 3), recompute `terminationDate`, re-schedule; on 3rd failure type-specific give-up (unit→return mission+resolved; build→delete units+mission; level-up→delete mission). Re-emit websocket channel on failure (after M4).
- **Time math** (`mission/time.rs`): per Inventory §5 — base time per type, travel penalty from slowest non-fixed unit speed (+SPEED improvement), `MISSION_SPEED_*` multipliers, move-cost weighted distance, divisor, `ZERO_BUILD_TIME`/`ZERO_UPGRADE_TIME` dev shortcuts (=3s). UTC throughout.
- **Processors** (`mission/processor/*`): explore, gather, establish_base, attack, counterattack (delegates to attack), conquest, deploy, return. Interception pre-pass (`mission/interception.rs`). Each: mutate, optionally register return mission, build report, set `resolved=true`. Two terminal conventions: unit missions stay (purged after 60 days by a nightly job); build/level-up deleted on completion.
- **Combat** (`mission/attack.rs`): the full `AttackMissionManagerBo` math (shuffle targeting, alliance-aware enemy filtering, attack rules, critical multipliers, shield bypass, kill-count `floor(attack/healthPerUnit)`, leftover-attack carryover, point accrual, unit-row deletion, carrier-freeing).
- Mission registration endpoints: `game/mission/*` (explore/gather/establishBase/attack/counterattack/conquest/deploy/cancel), `game/unit/build`, `game/unit/cancel`, `game/unit/delete`, `game/upgrade/registerLevelUp`, `game/upgrade/cancelUpgrade`.
- Nightly purge job (resolved missions > 60 days), `admin/system/run-hang-missions`.

### M4 — Websocket realtime (delivery layer; depends on M1–M3 for payloads)
**Goal: live `deliver_message` push + handshake, so the frontend gets real-time updates instead of only HTTP re-sync.**

- **socketioxide server** on `OWGE_WS_HOST:OWGE_WS_PORT` (default `0.0.0.0:7474`), separate listener from HTTP. **Verify Engine.IO/Socket.IO protocol parity with the frontend's pinned `socket.io-client`** before anything else (biggest unknown in this milestone).
- **Handshake** (`ws.rs`): on `authentication` event (payload is a JSON *string* `{value:<jwt>, protocol}`), verify JWT (reuse M0), bind `TokenUser` to the socket as `USER_TOKEN_KEY`. Success → emit `authentication` with `WebsocketMessage{status:"ok", value: eventsInfo[]}` where eventsInfo = all `websocket_events_information` rows for the user **plus** the synthetic `_universe_id:<UNIVERSE_ID>` entry. Failure → `WebsocketMessage{status:"error", value:"Invalid credentials"}` then disconnect.
- **Targeting** (`websocket/mod.rs`): no rooms — scan all connected clients, filter by bound `USER_TOKEN_KEY` id; `targetUserId==0` = broadcast to all authenticated. Multi-tab fan-out.
- **Send pipeline**: persist `websocket_events_information` watermark *first* (even for offline users; broadcast iterates all users), then async-emit `deliver_message` with `WebsocketMessage{eventName, value, status:"ok", lastSent}` off the request/tx thread. `sendOneTimeMessage` skips the watermark. `cache_clear` broadcast (`clearCache` bumps all watermarks). `warn_message`.
- **Envelope** `WebsocketMessage<T> { eventName, value, status, lastSent }` with camelCase serde; `status` only "ok"/"error".
- **after-commit emission**: wire the `do_after_commit` hook collection (`tx.rs`) so every mutating service flushes its websocket events only post-commit. Retrofit M2/M3 mutations to emit their events (`unit_obtained_change`, `unit_mission_change`, `missions_count_change`, `user_data_change`, `mission_report_*`, `*_unlocked_change`, `mission_gather_result`, etc.).
- Admin `admin/system/notify-updated-version`, `admin/cache/drop-all` (→ `cache_clear`), `game/deliver-backdoor/ping-user`.

### M5 — DROPPED (will not be implemented)
The original M5 (audit/anti-cheat: `audit`, `suspicions`, Tor checks) has been
**removed from the plan and will not be ported.** Auditing is disabled in the
live Java deployment too, so it is a deliberate no-op in the Rust port: the
`auditBo.*` call sites are left as inert stubs and `GET admin/users/{id}/suspicions`
returns `501` permanently. The other items once parked under M5 (image upload,
translatables/rule admin) were already completed under M2. Remaining genuine
follow-ups (golden-JSON diff tests, load/lock-contention testing, multi-instance
lock backend) are tracked outside this milestone.

---

## 4. Biggest compatibility risks (with mitigations)

1. **HMAC secret: raw bytes vs Base64-decoded (asymmetry).** Java *signs* admin tokens with `secret.getBytes()` (raw UTF-8) but the kevinsuite *verify* path (`Jwts.parser().setSigningKey(String)` in JJWT 0.9.x) Base64-decodes the secret. `jsonwebtoken::DecodingKey::from_secret(bytes)` and `EncodingKey::from_secret(bytes)` both take raw bytes. **You must determine empirically whether the live `JWT_SECRET` is a base64 string (→ decode before `from_secret`) or arbitrary text (→ raw `as_bytes()`), and whether sign and verify actually agree in the Java stack today.** Mitigation: a M0 conformance test that feeds a real production-issued token + the real secret and asserts validation; bake the winning interpretation into a single `decoding_key(secret)` fn. This is the single highest-risk item — a wrong choice silently rejects every player.

2. **RSA key format: X509 SubjectPublicKeyInfo + PKCS8, from BouncyCastle `PemObject.getContent()` (raw DER, header-stripped).** kevinsuite reads the PEM body bytes and feeds them to `X509EncodedKeySpec`/`PKCS8EncodedKeySpec` — i.e. it wants SPKI public DER and PKCS8 private DER. `jsonwebtoken::DecodingKey::from_rsa_pem` expects a standard `-----BEGIN PUBLIC KEY-----` (SPKI) PEM, which matches; `from_rsa_der` expects PKCS1, **not** SPKI — so prefer the PEM constructor, or convert SPKI→components with the `spki`/`rsa` crates. Private signing (admin RSA) needs PKCS8 → `EncodingKey::from_rsa_pem` (PKCS8 PEM ok) or reconstruct via `pkcs8`. Mitigation: test against the actual files in `/var/owge_data/keys/`; handle both PKCS1 and PKCS8/SPKI defensively.

3. **JWT algorithm is config-driven.** `JWT_HASHING_ALGO` (default HS256) selects the signature alg. Don't hardcode HS256; map the config string to `jsonwebtoken::Algorithm`. Validation must allow exactly the configured alg (never accept `none`, never accept alg confusion between HMAC and RSA — `jsonwebtoken` guards this but set `Validation::new(alg)` explicitly).

4. **Unsigned ints + mismatched logical-FK signedness.** Decode each column at its literal type (`user_storage.id` = `i32`; `planets.id` = `u64`; `missions.source_planet` = `i64`). A single wrong signedness → sqlx runtime decode error. Mitigation: generate struct fields straight from the schema inventory; add a `cargo sqlx prepare` step so `query!` checks types at compile time against the real DB.

5. **datetime timezone.** All `datetime`/`timestamp` are naive UTC. Use `NaiveDateTime` and never apply local tz. The JSON serialization must match Java's `Instant`/`Date` output the frontend expects (`WebsocketMessage.lastSent` is `Instant` serialized via `JavaTimeModule` → ISO-8601; `/open/clock` returns a `Date` → epoch-millis JSON number by default Jackson). **Check the exact wire format per field** (epoch-millis vs ISO string) — Jackson defaults differ for `Date` vs `java.time`; mirror with `chrono` serde (`serde(with = "chrono::serde::ts_milliseconds")` vs ISO) on a per-field basis.

6. **camelCase JSON, entity-shaped, irregular names.** `#[serde(rename_all = "camelCase")]` globally, plus explicit renames where Java getters are irregular (`myUnitMissions`, `eventName`, `_universe_id:` synthetic key, `secondValue`/`thirdValue`). Booleans that are bare `tinyint` in DB but `boolean`/`Boolean` in DTO must serialize as JSON `true/false`, not `0/1`. Round-trip every DTO against a captured Java response.

7. **tinyint(1) vs bare tinyint decode.** (§0.5) Decode bare `tinyint` as `i8`, convert to `bool` for DTO. A `DbBool` newtype with `Decode`/`Encode` keeps this in one place.

8. **Socket.IO protocol version parity.** socketioxide must match the frontend's `socket.io-client` major version (Engine.IO v4 handshake, packet framing). A mismatch fails silently at connect. Verify with the real frontend in M4 before building emitters.

9. **GET_LOCK requires connection pinning.** sqlx pools hand out arbitrary connections; an advisory lock taken on one connection is invisible to another. The locked critical section must hold a single acquired connection for its entire duration (and all queries inside it must use that connection). Easy to get subtly wrong; encapsulate in `db/lock.rs` so callers can't bypass it.

10. **JSON `data.id` as `Number`.** The JWT `data.id` may decode as int or float depending on issuer; deserialize into a tolerant numeric and convert to `i32`/`i64` (user id is signed `int`).

---

## 5. Controller-group → Rust module mapping (with effort)

Effort: **S** ≈ ≤0.5 day, **M** ≈ 1–2 days, **L** ≈ 3–5 days, **XL** ≈ 1–2 weeks. Assumes the shared infra (auth, CRUD macro, requirement engine, scheduler) from the milestone is already in place.

| Java controller group | Rust module | Milestone | Effort | Notes |
|---|---|---|---|---|
| `AdminLoginRestService` (`game/adminLogin`) | `routes/game/admin_login.rs` | M0 | S | admin token issuance; depends on JWT sign |
| `open/ClockRestService`, `ConfigurationRestService`, `SponsorRestService`, `OpenWebsocketSyncRestService` | `routes/open/*` | M0 | M | clock, config merge, sponsor list, rule/speed-group sync (cached, not rate-limited) |
| `WebsocketSyncRestService` (`game/websocket-sync`) | `websocket/sync.rs` + `routes/game/websocket_sync.rs` | M1 (read) | L | SyncSource registry + `findWantedData` + watermark re-save + rate-limit filter; central to frontend hydration |
| `UserRestService` (`exists`, `subscribe`) | `routes/game/user.rs` | M1 exists / M2 subscribe | M | subscribe needs requirement engine + spawn-planet logic |
| `FactionRestService`, `RankingRestService`, `GalaxyRestService`, `TimeSpecialRestService`, `UpgradeTypeRestService`, `TwitchStateRestService`(GET), `UnitRestService.criticalAttack` | `routes/game/{faction,ranking,galaxy,time_special,upgrade_type,twitch_state,unit}.rs` | M1 | M | read-only player surface |
| `AllianceRestService` | `routes/game/alliance.rs` | M2 | L | 12 endpoints, ownership checks, join-request lifecycle |
| `PlanetListRestService`, `PlanetRestService`, `ReportRestService`, `SystemMessageRestService`, `TutorialRestService`, `TrackBrowserRestService`, `DeliverBackdoorRestService`, `TimeSpecial.activate`, `TwitchState`(PUT) | `routes/game/*` | M2 | L | player non-mission mutations; some emit websocket (wire fully in M4) |
| `MissionRestService` (`game/mission/*`) | `routes/game/mission.rs` | M3 | L | thin registration endpoints over `UnitMissionBo` |
| `UnitRestService` (build/cancel/delete/findRunning), `UpgradeRestService` (registerLevelUp/cancel) | `routes/game/{unit,upgrade}.rs` | M3 | M | build/level-up registration over `MissionBo` |
| **Mission engine** (`MissionBo`, `UnitMissionBo`, processors, scheduler, time, interception, attack, locks) | `mission/*` | M3 | **XL** | the dominant effort; combat + scheduler + locks |
| `AdminConfigurationRestService`, `AdminAdminsRestService`, `AdminCacheRestService`, `AdminSystemRestService`, `AdminSuspicionsRestService`, `AdminSystemMessageRestService`, `DebugRestService` | `routes/admin/*` (bespoke) | M2 | M | non-CRUD admin endpoints |
| `AdminGalaxiesRestService`, `AdminUnitTypeRestService`, `AdminUpgradeTypeRestService`, `AdminTranslatableRestService`, `AdminTutorialSectionRestService`, `AdminImageStoreRestService` | `routes/admin/*` via CRUD macro | M2 | M | standard CRUD (+ a couple own methods); image store md5/base64 upload done in M2 |
| `AdminFactionRestService`, `AdminSpecialLocationRestService` (`CrudWithImprovements`) | `routes/admin/{faction,special_location}.rs` | M2 | L | CRUD + improvement sub-resource + image hook + own methods (faction unitTypes/spawn-locations) |
| `AdminUnitRestService`, `AdminUpgradeRestService`, `AdminTimeSpecialRestService` (`CrudWithFull`) | `routes/admin/*` | M2 | L | CRUD + improvements + requirements (needs requirement engine) + own methods |
| `AdminSpeedImpactGroupRestService` (`Crud + RequirementGroups`) | `routes/admin/speed_impact_group.rs` | M2 | M | CRUD + requirement-group nesting |
| `AdminAttackRuleRestService`, `AdminCriticalAttackRestService`, `AdminRuleRestService` | `routes/admin/*` | M2 (rules consumed by M3 combat) | M | attack-rule/critical-attack/generic-rule admin |
| `AdminGameUsersRestService` | `routes/admin/users.rs` | M2 | M | user list ported; `{id}/suspicions` permanently `501` (auditing dropped) |
| **Websocket server + emitters** (`SocketIoService`, `*EventEmitter`, `WebsocketMessage`, watermark) | `websocket/*`, `ws.rs` | M4 | **XL** | handshake, targeting, send pipeline, after-commit, all `*_change` emitters |
| Requirement/unlock/ObjectRelation engine (`RequirementBo`, `UnlockedRelationBo`, `WithUnlockableBo`) | `requirement/*` | M2 (built early) | L | shared dependency of subscribe, admin requirements, and combat unlock triggers |

**Critical-path summary:** M0 (auth) → M1 (read + sync) → M2 (requirement engine + CRUD/admin + subscribe) → M3 (mission engine, the XL item) → M4 (websocket, the second XL item). M2's requirement engine and M3's lock/scheduler primitives are the two pieces of shared infra that, if rushed, will force rework across many modules — build them deliberately and test them in isolation first.