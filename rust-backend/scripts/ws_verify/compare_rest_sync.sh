#!/usr/bin/env bash
# Autonomous Rust-vs-Java diff of the REST /game/websocket-sync endpoint.
#
# Assumes:
#   - the dev MySQL `db` container is running (universe data in DB `owge`),
#   - the Rust backend is already running on http://127.0.0.1:$RUST_PORT,
#   - the Java image `owge-java-compare:latest` has been built
#     (docker build -f docker-ci/dev/images_creation/Dockerfile_for_war_files
#      --target game-rest-and-admin -t owge-java-compare:latest .).
#
# It starts the Java backend in a container on the DB's docker network, mints a
# shared HS256 JWT (dev HMAC secret from the `configuration` table), runs the
# per-key diff, and tears the Java container down.
set -euo pipefail

RUST_PORT="${RUST_PORT:-8080}"
JAVA_HOST_PORT="${JAVA_HOST_PORT:-8099}"
DB_CONTAINER="${DB_CONTAINER:-owge_backend_developer-db-1}"
DB_NAME="${DB_NAME:-owge}"
DB_PASS="${DB_PASS:-1234}"
JAVA_CONTAINER="owge-java-compare-run"
HERE="$(cd "$(dirname "$0")" && pwd)"

cleanup() { docker rm -f "$JAVA_CONTAINER" >/dev/null 2>&1 || true; }
trap cleanup EXIT

# 1. DB network + HMAC secret straight from the running DB.
NET=$(docker inspect "$DB_CONTAINER" \
  --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')
DB_SVC=$(docker inspect "$DB_CONTAINER" --format '{{index .Config.Hostname}}')
SECRET=$(docker exec "$DB_CONTAINER" mysql -uroot -p"$DB_PASS" "$DB_NAME" -N -s \
  -e "SELECT value FROM configuration WHERE name='JWT_SECRET';" 2>/dev/null)
echo "DB network=$NET  jwt-secret=${SECRET:0:8}…"

# 2. Start the Java backend on that network (reaches the DB as host 'db').
cleanup
docker run -d --name "$JAVA_CONTAINER" --network "$NET" \
  -e OWGE_DB_URL="db:3306/$DB_NAME" -e OWGE_DB_USER=root -e OWGE_DB_PASS="$DB_PASS" \
  -e MYSQL_HOST=db -e MYSQL_DB="$DB_NAME" -e MYSQL_USER=root -e MYSQL_PASSWORD="$DB_PASS" \
  -e OWGE_CONTEXT_PATH=/game_api -e OWGE_CORS_CUSTOM_ORIGIN='*' \
  -e OWGE_WS_SYNC_RATELIMIT_PER_MINUTE=0 \
  -p "$JAVA_HOST_PORT:8080" \
  owge-java-compare:latest >/dev/null
echo "Java container started; waiting for it to come up on :$JAVA_HOST_PORT …"

JAVA_BASE="http://127.0.0.1:$JAVA_HOST_PORT/game_api"
for i in $(seq 1 90); do
  code=$(curl -s -o /dev/null -w '%{http_code}' "$JAVA_BASE/open/clock" || true)
  [ "$code" = "200" ] && { echo "Java up after ${i}s"; break; }
  sleep 2
  if [ "$i" = "90" ]; then
    echo "Java did NOT come up. Last logs:" >&2
    docker logs --tail 40 "$JAVA_CONTAINER" >&2
    exit 1
  fi
done

# 3. Mint a shared token for user 1.
JWT=$(SECRET="$SECRET" python3 - <<'PY'
import os, time, jwt
s = os.environ["SECRET"]
now = int(time.time())
print(jwt.encode({"sub":1,"iat":now,"exp":now+86400,
                  "data":{"id":1,"username":"kevin","email":"kevin@kevin.kevin"}},
                 s, algorithm="HS256"))
PY
)

# 4. Diff every key.
python3 "$HERE/rest_sync_diff.py" \
  --java-base "$JAVA_BASE" \
  --rust-base "http://127.0.0.1:$RUST_PORT" \
  --jwt "$JWT" \
  --json /tmp/rest_sync_diff.json
