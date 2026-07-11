# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What this is

OWGE (Open Web Game Engine) is an engine for non-graphical strategy web games (think OGame-style: planets, units, upgrades, missions). It is a multi-language monorepo with **no root aggregator** — each part builds independently:

- `business/` — the core game engine, a Spring Boot **library** (`com.kevinguanchedarias.owge:owgejava-backend`). Contains all entities, repositories, and game logic (`*Bo` "business objects"). Builds to a jar, not a runnable app.
- `game-rest/` — the runnable Spring Boot **web app** (`game-rest`, packaged as a WAR). Depends on `owgejava-backend` and exposes the REST API + websocket server. This is what actually runs in production.
- `game-frontend/` — Angular 11 client (player UI + admin UI) — see its own `README.md`/`CHANGELOG.md`.
- `mock_account/` — a small PHP (Yii) app that emulates the external account/SSO system in dev. OWGE delegates authentication to an external "account system"; this stands in for it.
- `docker-ci/` — build + deploy automation (CI scripts and per-universe Docker Compose). See "Deployment".
- `static/`, `dynamic/` — image asset roots served by nginx (static is shared, dynamic is per-universe).

### Critical external dependency: kevinsuite-java
`business` and `game-rest` depend on `com.kevinguanchedarias.owge:kevinsuite-java-*` (e.g. `kevinsuite-java-rest-commons`), which is **not on Maven Central** but **is published via [JitPack](https://jitpack.io)** (which builds it on demand from its GitHub repo). JitPack is declared as a `<repository>` in `business/pom.xml`, so an **online** build resolves kevinsuite automatically — no manual step needed. The only time you must provide it yourself is an **offline** build (`-o`) against a `~/.m2` that doesn't already have it. For that case (and in CI), it can be built from source and installed to `~/.m2` from `/public/kevinsuite-java/{common-backend,backend-rest-commons}` — which is what the deploy script does. Note `game-rest` has no JitPack repo of its own; it resolves kevinsuite transitively, so build/install `business` first (the JitPack repo comes from there).

## Build, run, and test

Java 21, Spring Boot 3.2.2 (Jakarta namespace). This host has **no local `mvn`/JDK** — builds run in Docker with the shared `~/.m2` mounted. The canonical pattern (used by CI too):

```bash
# Build/test the business engine (run from repo root)
docker run --rm -v /public/owge:/work -v /root/.m2:/root/.m2 -w /work/business \
  maven:3.9-eclipse-temurin-21 mvn -o test

# Run a single test class / method
docker run --rm -v /public/owge:/work -v /root/.m2:/root/.m2 -w /work/business \
  maven:3.9-eclipse-temurin-21 mvn -o test -Dtest='AttackMissionManagerBoTest'
docker run --rm -v /public/owge:/work -v /root/.m2:/root/.m2 -w /work/business \
  maven:3.9-eclipse-temurin-21 mvn -o test -Dtest='AttackMissionManagerBoTest#startAttack_should_work'
```
`-o` (offline) works because `~/.m2` is already populated; drop it if you need to fetch new deps. Build `game-rest` the same way with `-w /work/game-rest` (it needs `owgejava-backend` installed first via `mvn install` in `business`). Where a real `mvn` is on PATH, the same `mvn` goals apply directly.

Tests use **JUnit 5 + Mockito** (`mockito-inline`, `BDDMockito`). Most engine tests are pure unit tests with mocked collaborators and `*Mock` helper factories under `src/test/.../mock/`.

Frontend (run in `game-frontend/`, **Node 14** — Angular 11; pinned via `engines` + `.nvmrc`, and CI builds with the `node:14` image):
```bash
npm install
npm start           # ng serve (player frontend)
npm run startAdmin  # ng serve game-admin
npm run build       # prod build (player) ; npm run buildAdmin for admin
npm run lint        # tslint
```

## Architecture (the parts that span multiple files)

**Layering.** REST controllers in `game-rest` (`rest/{game,admin,open}`) are thin; all logic lives in `business` `*Bo` services. `game-rest` adds web concerns only: security/JWT filters, exception handlers, websocket wiring. When adding a feature, the entity + repository + `*Bo` go in `business`; only the endpoint goes in `game-rest`.

**The mission system is the core game loop.** Player actions (attack, explore, gather, conquest, deploy, establish base, return, counterattack) are *missions* with a delay. They are scheduled and executed asynchronously via **db-scheduler** (table `scheduled_tasks`, scheduler name `OWGE_BACKGROUND`, configured in `application.properties` + `DbSchedulerConfiguration`). `DbSchedulerRealizationJob` is the entry point that fires a due mission; `UnitMissionBo` dispatches to a `MissionProcessor` implementation (`business/mission/processor/*MissionProcessor`) per mission type. Combat math lives in `business/mission/attack/` (notably `AttackMissionManagerBo`). Quartz also exists (`QuartzConfiguration`) for other scheduled tasks, so don't assume "scheduler" == db-scheduler.

**ObjectRelation / unlockable system.** Game content types (units, upgrades, time specials, etc.) are modeled generically as `ObjectEntity` + `ObjectRelation` (`object_relations` table) + `ObjectRelationToObjectRelation`. Requirements (`RequirementBo`, `business/requirement/`) and unlocks (`UnlockedRelationBo`, `WithUnlockableBo`) are expressed against these relations rather than concrete types. Understanding this indirection is necessary before touching requirements/unlock logic — it's not a direct FK per content type.

**Real-time sync.** The server pushes state to clients over a **netty-socketio** websocket (not STOMP), configured in `WebsocketConfiguration` and driven by `SocketIoService` / `WebsocketSyncService` / `*EventEmitter` classes. Many `*Bo` methods emit websocket events after mutating state (often `transactionUtilService.doAfterCommit(...)`); keep emissions after commit.

**Concurrency & caching.** Mission execution serializes contended work with application-level **MySQL named locks** via `MysqlLockUtilService` (e.g. `planet_lock_<id>`) — its package logs at TRACE. Read caching uses the `taggable-cache` library with by-user cache tags (`@TaggableCacheEvictByTag`, `getByUserCacheTag()`); when a bulk/`@Modifying` update bypasses entity listeners, the cache tag must be evicted manually.

**Persistence gotchas.** `spring.jpa.open-in-view=false` and `hibernate.enable_lazy_load_no_trans=true`. Some helpers (`EntityRefreshUtilService.refresh`) fall back to `getReferenceById`, returning a **lazy proxy**; dereferencing a proxy whose row was deleted throws `EntityNotFoundException`. Be careful passing entities that may have been deleted earlier in the same transaction into save/update paths.

## Database

Schema and seed data are plain SQL in `business/database/` (`02_schema.sql`, `04_insert_data.sql`, `05_mysql_procedures.sql`), with versioned `migrations/v*.sql`. There is **no automatic migration tool** — new schema changes go in a `migrations/v<next>.sql` file and are applied manually/by deploy. A brand-new universe is initialized by the deploy script (schema + base data + a "world" `init.sql`). MySQL/MariaDB; some log tables (e.g. `tor_ip_data`) use MyISAM.

## Versioning & releases

A single version string spans both backends and the frontend. To bump it, run from `game-frontend/`:
```bash
npm run setVersion <X.Y.Z>   # no "v" prefix
```
This rewrites `business/pom.xml` and `game-rest/pom.xml` to `<X.Y.Z>-SNAPSHOT` (and the `<owge.version>` property where present) and `package.json` to `X.Y.Z`. Commit the result as-is — the poms intentionally stay on `-SNAPSHOT` while the version is in development (this is why deploy refuses an untagged/`-SNAPSHOT` version; see below).

The changelog date doubles as release state. In `game-frontend/CHANGELOG.md`, the in-progress version's header uses `(latest)` as its date (e.g. `v0.11.8 (latest)`); add user-facing entries under it as you go. When you tag the release, **replace `(latest)` with the datetime the tag was created** (e.g. `v0.11.8 (2026-05-31 22:13)`) and then create the `v<X.Y.Z>` git tag. So: untagged + `(latest)` = unreleased; dated header + matching git tag = released and deployable.

## Deployment (universes)

Each game world is a "universe" `dc<N>` deployed as its own Docker Compose project. Deploy from `docker-ci/ci/`:
```bash
OWGE_DB_URL=<host:port/dbname> OWGE_DB_USER=... OWGE_DB_PASS=... \
  ./launch_admin_rest.sh <version> /public/owge-data/static /public/owge-data/dynamic/<N> <N>
```
Key facts:
- **`<version>` must be an existing git tag** (`v<version>`); the script does `git checkout v$version` and rejects `-SNAPSHOT`. Tag + push before deploying a new version.
- The script builds kevinsuite, `business`, `game-rest`, and the Angular frontend (tests skipped), then `docker-compose up --build -d` (uses **docker-compose v1**) with `COMPOSE_PROJECT_NAME=dc<N>`.
- **Dynamic** images dir is per-universe (`/public/owge-data/dynamic/<N>`); **static** is shared (`/public/owge-data/static`). Passing the wrong dynamic dir makes all dynamic images 404.
- Published host port = `8110 + N` (e.g. dc12 → 8122).

## Conventions

- Per the user's global rule: **never add AI/assistant attribution** (no `Co-Authored-By` etc.) to commits or PRs.
- `CONTRIBUTING.md` documents the contributor convention of PRs into `master`/version branches rather than direct pushes; the repo owner commits to `master` directly. Match the existing commit-message style (`Fix:` / `Improvement:` / `Docs:` prefixes) and add a matching line to `game-frontend/CHANGELOG.md` under the latest version for user-facing changes.
- `business` service classes are named `*Bo`; prefer adding logic there over controllers.
