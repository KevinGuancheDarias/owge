# Launching a universe on the RUST backend

`launch_rust_rest.sh` is the Rust-backend equivalent of `launch_admin_rest.sh`.
It builds the Rust port (`rust-backend/owge-rest`) + the Angular frontend and
brings a universe up as an nginx reverse proxy in front of the Rust backend.

Same contract as the Java launcher:

```bash
OWGE_DB_URL=<host:port/dbname> OWGE_DB_USER=... OWGE_DB_PASS=... \
  ./launch_rust_rest.sh <version> /public/owge-data/static /public/owge-data/dynamic/<N> <N>
```

- `<version>` is an existing OWGE git tag (`v<version>`). It pins the frontend +
  the DB schema SQL. **`rust-backend/` is git-untracked, so the Rust backend is
  built from the current working tree**, not the tag.
- Published host port = `8110 + N` (same as Java).
- Fresh/empty DB → initialized from `business/database/{02_schema,04_insert_data}.sql`
  + `$OWGE_WORLD_DIR/init.sql` (set `OWGE_WORLD_DIR`), identical to the Java path.

## Files (Rust-specific)

| File | Role |
|---|---|
| `launch_rust_rest.sh` | orchestrator — `cargo build --release` (in `rust:1-bookworm`) + Angular build + install |
| `jenkins_install_rust.sh` | copies the binary + frontend, composes `OWGE_DB_JDBC_URL`, runs the stack |
| `docker-compose.rust.yml` | nginx proxy (**shared image + config, unchanged**) + the Rust backend |
| `rust_rest_game/Dockerfile` | `debian:bookworm-slim` runtime for the `owge-rest` binary |

The **nginx reverse proxy is reused verbatim** from the Java stack (same image,
same `main_reverse_proxy/config/nginx/conf.d/proxy_settings.conf`). No
Rust-specific nginx config exists.

## How it differs from the Java stack

- No kevinsuite/Maven/war: the backend is the single `owge-rest` binary.
- The Rust app serves under the same `/game_api` context path as the Java app via
  the new `OWGE_CONTEXT_PATH` env var (defaults to root; set to `/game_api` in the
  compose), so the unchanged nginx config front-ends either backend. In the
  compose the Rust backend keeps the service name `admin_panel_and_rest_game` so
  the proxy upstream resolves; its image (`rust_rest_game:<version>`) identifies
  the real backend in `docker ps`.
- The Rust app takes one `OWGE_DB_JDBC_URL` (`mysql://user:pass@host:port/db`,
  composed from `OWGE_DB_URL`/`OWGE_DB_USER`/`OWGE_DB_PASS`) instead of the split
  Java vars. A DB password with URL-reserved characters would need encoding.
- Backend container ports: `8080` (REST) + `7474` (socket.io), same as the Java
  container, so the reverse proxy forwards identically.
