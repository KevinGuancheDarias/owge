#!/usr/bin/env bash
# run_mission_parity.sh [scenario-seed.sql ...]
#
# Mission-state parity harness — the table/RNG analogue of ws_verify's
# compare_rest_sync.sh. For each scenario it runs the SAME mission on the Java
# backend and the Rust backend against the SAME shared `owge` DB, sequentially,
# using a snapshot/restore so the two engines never collide on the same rows,
# then diffs the resulting table state (table_diff.py) and RNG trace
# (trace_diff.py).
#
# TOPOLOGY (important): both backends point at the one dev `owge` DB. Per
# scenario:
#   1. set configuration.ATTACK_DETERMINISTIC_RNG='TRUE'
#   2. apply the scenario seed SQL
#   3. SNAPSHOT the in-scope tables (mysqldump of just those tables)
#   4. run the scenario on JAVA   -> capture table state + RNG trace
#   5. RESTORE the snapshot (back to the post-seed, pre-mission state)
#   6. run the scenario on RUST   -> capture table state + RNG trace
#   7. table_diff.py JAVA RUST  +  trace_diff.py JAVA RUST
#
# Triggering a mission: the seeds only set up units/planets, not the mission row.
# We CREATE the mission with a REST POST to the backend under test (game/mission
# /<verb>, mirroring the frontend), which inserts the `missions` row AND the
# db-scheduler `scheduled_tasks` row. We then force that task's execution_time
# into the past (the technique documented in ws_verify/README.md) so the
# OWGE_BACKGROUND scheduler fires it immediately, and poll until the mission
# resolves. (Alternative: seed the missions row directly; REST POST is used here
# because it exercises buildAttackInformation/addUnit ordering identically to
# production and works the same on both backends.)
#
# The RNG trace + ATTACK_DETERMINISTIC_RNG flag come from a DIFFERENT agent's
# combat-code changes and may not be wired yet: trace capture is best-effort and
# trace_diff.py prints "no trace emitted yet" when absent — the table-diff path
# still runs and gates.
#
# Usage:
#   run_mission_parity.sh                       # loop over all seed_*.sql
#   run_mission_parity.sh seed_attack_asymmetric.sql
#   SEED_DIR=/path RUST_PORT=8080 run_mission_parity.sh seed_attack.sql
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SEED_DIR="${SEED_DIR:-$(cd "$HERE/.." && pwd)}"   # rust-backend/scripts
WORK="${WORK:-$(mktemp -d)}"

# --- environment ------------------------------------------------------------
RUST_PORT="${RUST_PORT:-8080}"
RUST_BIN="${RUST_BIN:-$(cd "$HERE/../.." && pwd)/target/debug/owge-rest}"
JAVA_HOST_PORT="${JAVA_HOST_PORT:-8099}"
JAVA_CONTAINER="owge-java-mission-parity"
DB_CONTAINER="${DB_CONTAINER:-owge_backend_developer-db-1}"
DB_NAME="${DB_NAME:-owge}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-1234}"
USERS="${USERS:-1,2}"
POLL_SECONDS="${POLL_SECONDS:-40}"

# in-scope tables snapshotted/restored between the Java and Rust runs
SNAP_TABLES="obtained_units missions mission_reports mission_information \
user_storage planets unlocked_relation active_time_specials scheduled_tasks \
explored_planets websocket_events_information"

RUST_PID=""
cleanup() {
  docker rm -f "$JAVA_CONTAINER" >/dev/null 2>&1 || true
  [ -n "$RUST_PID" ] && kill "$RUST_PID" >/dev/null 2>&1 || true
}
trap cleanup EXIT

mysql_exec() { docker exec -i "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -s 2>/dev/null; }
mysql_q()    { docker exec -i "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -s -e "$1" 2>/dev/null; }

# --- map a seed filename to its mission verb / type -------------------------
# (used both for the REST POST path and the scheduler-fire LIKE filter)
seed_to_mission() {
  case "$1" in
    *asymmetric*|*attack*|*capture*|*interception*) echo "attack ATTACK" ;;
    *conquest*)       echo "conquest CONQUEST" ;;
    *counterattack*)  echo "counterattack COUNTERATTACK" ;;
    *gather*)         echo "gather GATHER" ;;
    *deploy*)         echo "deploy DEPLOY" ;;
    *)                echo "attack ATTACK" ;;   # default: attack-driving
  esac
}

# --- boot Java container (same approach as compare_rest_sync.sh) ------------
NET=$(docker inspect "$DB_CONTAINER" \
  --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')
SECRET=$(mysql_q "SELECT value FROM configuration WHERE name='JWT_SECRET';")
echo "DB network=$NET  jwt-secret=${SECRET:0:8}…  work=$WORK"

boot_java() {
  docker rm -f "$JAVA_CONTAINER" >/dev/null 2>&1 || true
  docker run -d --name "$JAVA_CONTAINER" --network "$NET" \
    -e OWGE_DB_URL="db:3306/$DB_NAME" -e OWGE_DB_USER="$DB_USER" -e OWGE_DB_PASS="$DB_PASS" \
    -e MYSQL_HOST=db -e MYSQL_DB="$DB_NAME" -e MYSQL_USER="$DB_USER" -e MYSQL_PASSWORD="$DB_PASS" \
    -e OWGE_CONTEXT_PATH=/game_api -e OWGE_CORS_CUSTOM_ORIGIN='*' \
    -e OWGE_WS_SYNC_RATELIMIT_PER_MINUTE=0 \
    -e ATTACK_DETERMINISTIC_RNG=TRUE -e OWGE_ATTACK_RNG_TRACE=TRUE \
    -p "$JAVA_HOST_PORT:8080" \
    owge-java-compare:latest >/dev/null
  JAVA_BASE="http://127.0.0.1:$JAVA_HOST_PORT/game_api"
  echo -n "  waiting for Java :$JAVA_HOST_PORT "
  for i in $(seq 1 90); do
    code=$(curl -s -o /dev/null -w '%{http_code}' "$JAVA_BASE/open/clock" || true)
    [ "$code" = "200" ] && { echo "up (${i}s)"; return 0; }
    sleep 2
  done
  echo "FAILED"; docker logs --tail 40 "$JAVA_CONTAINER" >&2; return 1
}

# Start the Rust backend, appending its stderr to $WORK/rust.stderr (append so the
# per-scenario @@RNG@@ offset tracking keeps working across restarts).
start_rust() {
  RUST_BASE="http://127.0.0.1:$RUST_PORT"
  OWGE_DB_JDBC_URL="mysql://$DB_USER:$DB_PASS@127.0.0.1:3306/$DB_NAME" \
    OWGE_SERVER_PORT="$RUST_PORT" \
    ATTACK_DETERMINISTIC_RNG=TRUE OWGE_ATTACK_RNG_TRACE=TRUE \
    "$RUST_BIN" >>"$WORK/rust.stderr" 2>&1 &
  RUST_PID=$!
  for i in $(seq 1 60); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' "$RUST_BASE/open/clock" || true)" = "200" ] \
      && { echo "  Rust up (${i}s)"; return 0; }
    sleep 1
  done
  echo "  Rust FAILED to start"; tail -20 "$WORK/rust.stderr" >&2; return 1
}

ensure_rust() {
  RUST_BASE="http://127.0.0.1:$RUST_PORT"
  if [ "$(curl -s -o /dev/null -w '%{http_code}' "$RUST_BASE/open/clock" || true)" = "200" ]; then
    echo "  Rust already up on :$RUST_PORT"; return 0
  fi
  echo "  starting Rust backend ($RUST_BIN)…"
  start_rust
}

# Kill and restart Rust on a clean (already-restored) DB. The Rust backend runs
# continuous background DB pollers; running them THROUGH a snapshot restore (table
# wipe + reload) can wedge its connection pool / HTTP handler. Restarting it AFTER
# the restore gives it a consistent DB view and avoids that race.
restart_rust() {
  if [ -n "${RUST_PID:-}" ]; then
    kill "$RUST_PID" >/dev/null 2>&1 || true
    wait "$RUST_PID" 2>/dev/null || true
    RUST_PID=""
  fi
  # ensure the port is actually free before rebinding
  for i in $(seq 1 20); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' "$RUST_BASE/open/clock" || true)" = "200" ] || break
    sleep 1
  done
  echo "  restarting Rust backend on clean DB…"
  start_rust
}

mint_jwt() {  # HS256, like compare_rest_sync.sh (Java dev secret)
  SECRET="$SECRET" JWT_UID="$1" python3 - <<'PY'
import os, time, jwt
s = os.environ["SECRET"]; uid = int(os.environ["JWT_UID"]); now = int(time.time())
print(jwt.encode({"sub": uid, "iat": now, "exp": now + 86400,
                  "data": {"id": uid, "username": "rusttester",
                           "email": "rust@test.local"}},
                 s, algorithm="HS256"))
PY
}

set_flag()   { mysql_q "INSERT INTO configuration (name,value) VALUES ('ATTACK_DETERMINISTIC_RNG','TRUE') ON DUPLICATE KEY UPDATE value='TRUE';" || true; }

snapshot()   { docker exec "$DB_CONTAINER" mysqldump -u"$DB_USER" -p"$DB_PASS" \
                 --no-create-info --skip-extended-insert --complete-insert \
                 "$DB_NAME" $SNAP_TABLES >"$1" 2>/dev/null; }
restore()    {
  # wipe the in-scope tables then reload the snapshot
  local del=""
  for t in $SNAP_TABLES; do del="$del DELETE FROM $t WHERE 1;"; done
  printf 'SET FOREIGN_KEY_CHECKS=0;\n%s\nSET FOREIGN_KEY_CHECKS=1;\n' "$del" | mysql_exec
  docker exec -i "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" <"$1" 2>/dev/null
}

# Create the mission via REST and fire its scheduled task, then wait for resolve.
# $1 base url  $2 verb  $3 mission-type  $4 jwt
run_scenario_on() {
  local base="$1" verb="$2" mtype="$3" jwt="$4"
  # Assemble the POST body: involvedUnits from the seeded obtained_units (user 1,
  # the sender), source/target planets from the seed.
  local body src tgt units_json
  src=$(mysql_q "SELECT source_planet FROM obtained_units WHERE user_id=1 ORDER BY id LIMIT 1;")
  case "$mtype" in
    CONQUEST|ATTACK) tgt=$(mysql_q "SELECT id FROM planets WHERE owner=2 AND home=0 LIMIT 1;") ;;
    COUNTERATTACK)   tgt=$(mysql_q "SELECT id FROM planets WHERE owner=1 AND home=0 LIMIT 1;") ;;
    GATHER|DEPLOY)   tgt=$(mysql_q "SELECT planet FROM explored_planets WHERE user=1 LIMIT 1;") ;;
    *)               tgt=$(mysql_q "SELECT id FROM planets WHERE owner=2 AND home=0 LIMIT 1;") ;;
  esac
  # involvedUnits is keyed by UNIT TYPE id (obtained_units.unit_id), not the
  # obtained_units surrogate id, and only the units actually departing from the
  # chosen source_planet take part (a stack on a different planet is a separate
  # mission). Group by unit_id so multiple rows of the same type on the source
  # planet collapse into one involved entry with the summed count.
  units_json=$(mysql_q "SELECT CONCAT('{\"id\":',unit_id,',\"count\":',SUM(count),'}') FROM obtained_units WHERE user_id=1 AND source_planet=$src AND mission_id IS NULL GROUP BY unit_id;" \
                | paste -sd, -)
  body=$(python3 - "$src" "$tgt" "$units_json" <<'PY'
import sys, json
src, tgt, units = sys.argv[1], sys.argv[2], sys.argv[3]
involved = json.loads("[" + units + "]") if units else []
print(json.dumps({"userId": 1,
                  "sourcePlanetId": int(src) if src else None,
                  "targetPlanetId": int(tgt) if tgt else None,
                  "involvedUnits": involved}))
PY
)
  echo "    POST $base/game/mission/$verb  (src=$src tgt=$tgt units=[$units_json])"
  local code
  code=$(curl -s -o "$WORK/post.out" -w '%{http_code}' -X POST \
    -H "Authorization: Bearer $jwt" -H "Content-Type: application/json" \
    --data "$body" "$base/game/mission/$verb" || true)
  if [ "$code" != "200" ] && [ "$code" != "201" ] && [ "$code" != "204" ]; then
    echo "    ⚠️  mission POST returned HTTP $code: $(head -c 200 "$WORK/post.out")"
  fi
  # The mission type code id we are driving (e.g. ATTACK). We poll for THIS
  # mission to resolve — not "all missions resolved", because an attack that
  # captures spawns a DEPLOYED mission (resolved=0 forever) and a RETURN mission
  # (resolved=0 until its own future execution_time). Waiting on "all resolved"
  # would loop forever AND keep nudging `mission-run`, prematurely firing those
  # follow-up missions and desyncing the Java vs Rust runs. So: nudge until the
  # driving mission resolves, then STOP (leaving follow-ups pending as production
  # would), and give Java a moment to flush its combat report + RNG trace.
  local mtid
  mtid=$(mysql_q "SELECT id FROM mission_types WHERE code='${mtype}' LIMIT 1;")
  # Capture the scheduled_task instance(s) that exist RIGHT NOW (i.e. the task for
  # the mission we just POSTed). We only ever nudge THESE — never tasks created
  # later by the attack itself (the RETURN mission's task). Otherwise the poll
  # would prematurely fire the follow-up RETURN mission, and a one-iteration race
  # made Java fire it but not Rust, desyncing `missions.resolved` / scheduled_tasks
  # for the capture scenario even though combat was identical.
  local drive_instances
  drive_instances=$(mysql_q "SELECT task_instance FROM scheduled_tasks WHERE task_name='mission-run';" | paste -sd"','" -)
  local in_clause=""
  [ -n "$drive_instances" ] && in_clause="AND task_instance IN ('$drive_instances')"
  mysql_q "UPDATE scheduled_tasks SET execution_time=DATE_SUB(NOW(), INTERVAL 5 SECOND), picked=0 WHERE task_name='mission-run' $in_clause;" || true
  local resolved_ok=0
  for i in $(seq 1 "$POLL_SECONDS"); do
    local pending
    pending=$(mysql_q "SELECT COUNT(*) FROM missions WHERE user_id IN ($USERS) AND type=${mtid:-0} AND resolved=0;")
    if [ "${pending:-1}" = "0" ]; then resolved_ok=1; break; fi
    mysql_q "UPDATE scheduled_tasks SET execution_time=DATE_SUB(NOW(), INTERVAL 5 SECOND), picked=0 WHERE task_name='mission-run' $in_clause;" >/dev/null 2>&1 || true
    sleep 1
  done
  [ "$resolved_ok" = "1" ] || echo "    ⚠️  ${mtype} mission did not resolve within ${POLL_SECONDS}s"
  # Record the driving mission id (the seed value its RNG draws are tagged with) so
  # the caller can capture this scenario's trace by `"seed":<id>` rather than by a
  # line offset — robust to stderr/log buffering across the inter-run Rust restart,
  # which was misattributing draws to the wrong scenario window.
  LAST_MISSION_ID=$(mysql_q "SELECT MAX(id) FROM missions WHERE user_id IN ($USERS) AND type=${mtid:-0};")
  # let the backend finish writing the report row + flush the RNG trace to stderr
  sleep 2
}

# Fire a PRE-SEEDED fixed-id mission (the same `missions.id` on both backends, so
# the JavaRandom seed = mission.id is IDENTICAL across Java and Rust => `result`/
# `seed` are comparable; this is the same-seed reproducibility path). No REST POST:
# the seed SQL already inserted the `missions` row AND its `scheduled_tasks` row, so
# we just nudge THAT task's execution_time into the past and poll until it resolves.
# $1 = the fixed mission id.
run_fixed_mission_on() {
  local mid="$1"
  echo "    FIXED mission id=$mid — nudging pre-seeded scheduled_task (no REST POST)"
  # Only ever nudge THIS instance (never a follow-up RETURN-mission task the attack
  # itself creates), to keep the Java and Rust runs in lock-step exactly as the
  # REST-POST path does.
  local nudge="UPDATE scheduled_tasks SET execution_time=DATE_SUB(NOW(6), INTERVAL 5 SECOND), picked=0 WHERE task_name='mission-run' AND task_instance='$mid';"
  mysql_q "$nudge" || true
  local resolved_ok=0
  for i in $(seq 1 "$POLL_SECONDS"); do
    local r
    r=$(mysql_q "SELECT resolved FROM missions WHERE id=$mid;")
    if [ "${r:-0}" = "1" ]; then resolved_ok=1; break; fi
    mysql_q "$nudge" >/dev/null 2>&1 || true
    sleep 1
  done
  [ "$resolved_ok" = "1" ] || echo "    ⚠️  fixed mission $mid did not resolve within ${POLL_SECONDS}s"
  LAST_MISSION_ID="$mid"
  # let the backend finish writing the report row + flush the RNG trace to stderr
  sleep 2
}

# Read the `-- FIXED_MISSION_ID=<n>` marker (line 1) of a seed, echo the id or "".
seed_fixed_mission_id() {
  sed -n 's/^-- *FIXED_MISSION_ID=\([0-9]\+\).*/\1/p' "$1" | head -1
}

# --- main loop --------------------------------------------------------------
SEEDS=()
if [ "$#" -gt 0 ]; then
  for s in "$@"; do
    [ -f "$s" ] && SEEDS+=("$s") || SEEDS+=("$SEED_DIR/$s")
  done
else
  for s in "$SEED_DIR"/seed_*.sql; do
    case "$s" in
      *seed_build*|*seed_levelup*|*seed_reqtrigger*|*seed_temporal_units*|*seed_deleteuser*) continue ;;  # non-mission/combat seeds
    esac
    SEEDS+=("$s")
  done
fi

set_flag
boot_java || exit 1
ensure_rust || exit 1
JWT=$(mint_jwt 1)

OVERALL=0
for seed in "${SEEDS[@]}"; do
  name=$(basename "$seed" .sql)
  read -r verb mtype <<<"$(seed_to_mission "$name")"
  echo
  echo "######################################################################"
  echo "# SCENARIO $name   (mission=$mtype verb=$verb)"
  echo "######################################################################"

  echo "  applying seed…"
  if ! docker exec -i "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" <"$seed" >/dev/null 2>"$WORK/seed.err"; then
    echo "  ⚠️  seed failed to apply: $(head -c 200 "$WORK/seed.err") — skipping"
    OVERALL=1; continue
  fi
  set_flag

  # FIXED-SEED PATH: if the seed declares `-- FIXED_MISSION_ID=<n>` it has already
  # inserted the missions row AND its scheduled_tasks row with that fixed id, so we
  # skip the REST POST entirely and fire THAT mission on both backends. Because the
  # snapshot is taken AFTER the seed, both Java and Rust restore and fire the SAME
  # mission id => SAME JavaRandom seed => the RNG `result`/`seed` are comparable and
  # the trace diff is run with --strict.
  FIXED_MID=$(seed_fixed_mission_id "$seed")
  if [ -n "$FIXED_MID" ]; then
    echo "  >>> FIXED-SEED scenario: mission id $FIXED_MID (strict trace, no REST POST)"
  fi

  SNAP="$WORK/$name.snapshot.sql"
  snapshot "$SNAP"

  # --- Java run ---
  # Both the Java container log and the Rust stderr ACCUMULATE across scenarios,
  # so capture only the draws emitted DURING this scenario: for Java use
  # `docker logs --since <marker>`; for Rust record the stderr line count first
  # and tail from there.
  echo "  --- Java run ---"
  LAST_MISSION_ID=""
  if [ -n "$FIXED_MID" ]; then
    run_fixed_mission_on "$FIXED_MID"
  else
    run_scenario_on "$JAVA_BASE" "$verb" "$mtype" "$JWT"
  fi
  java_mid="$LAST_MISSION_ID"
  "$HERE/dump_mission_state.sh" "$DB_NAME" "$WORK/$name.java.json" >/dev/null
  # ROBUST trace capture: this scenario's draws are exactly the @@RNG@@ lines whose
  # JSON carries `"seed":<this mission id>`, so we grep by seed rather than by a line
  # offset. This is immune to the stderr/log flush lag and the inter-run Rust restart
  # that previously misattributed buffered draws to the wrong scenario window. The
  # Java container log accumulates across scenarios but each mission id is unique, so
  # the seed filter isolates this run. Settle/flush wait until the count for THIS
  # seed stops growing (up to 12s) since the strict trace is now load-bearing.
  capture_trace_by_seed() { # $1=raw-stream-cmd  $2=seed  $3=outfile
    local prev=-1 now
    for _ in $(seq 1 12); do
      now=$(eval "$1" 2>/dev/null | grep '@@RNG@@' | grep -c "\"seed\":$2\b")
      [ "$now" = "$prev" ] && [ "$now" -gt 0 ] && break
      prev=$now; sleep 1
    done
    eval "$1" 2>/dev/null | grep '@@RNG@@' | grep "\"seed\":$2\b" >"$3" || true
  }
  capture_trace_by_seed "docker logs $JAVA_CONTAINER 2>&1" "$java_mid" "$WORK/$name.java.trace"

  # --- restore, then Rust run ---
  echo "  --- restore snapshot ---"
  restore "$SNAP"
  # The restored snapshot reloads the post-seed `mission-run` scheduled_task(s). For
  # the REST-POST path that is a STALE task (the POST will create a fresh one), so we
  # drop it to stop the Rust poller firing it pre-POST. For the FIXED-SEED path the
  # restored task IS the one we want to fire on Rust (same id as Java just ran) — so
  # keep it; it is back in its FUTURE-execution_time post-seed state and will only
  # fire when run_fixed_mission_on nudges it.
  if [ -z "$FIXED_MID" ]; then
    mysql_q "DELETE FROM scheduled_tasks WHERE task_name='mission-run';" >/dev/null 2>&1 || true
  fi
  # Assert the deterministic flag BEFORE Rust restarts so it is already TRUE when
  # Rust first reads `configuration`, then restart Rust on the clean DB (its
  # pollers don't survive a restore — see restart_rust).
  set_flag
  restart_rust || { echo "    ⚠️  Rust failed to restart"; OVERALL=1; continue; }
  set_flag
  echo "  --- Rust run ---"
  LAST_MISSION_ID=""
  if [ -n "$FIXED_MID" ]; then
    run_fixed_mission_on "$FIXED_MID"
  else
    run_scenario_on "$RUST_BASE" "$verb" "$mtype" "$JWT"
  fi
  rust_mid="$LAST_MISSION_ID"
  "$HERE/dump_mission_state.sh" "$DB_NAME" "$WORK/$name.rust.json" >/dev/null
  # Same seed-keyed capture for Rust (its stderr accumulates across the inter-run
  # restart; the seed filter isolates this scenario's draws regardless of buffering).
  capture_trace_by_seed "cat $WORK/rust.stderr" "$rust_mid" "$WORK/$name.rust.trace"

  # --- diffs ---
  echo
  echo "  === TABLE DIFF ($name) ==="
  python3 "$HERE/table_diff.py" "$WORK/$name.java.json" "$WORK/$name.rust.json" \
    --a-label Java --b-label Rust --json "$WORK/$name.tablediff.json"
  tdr=$?
  echo
  echo "  === RNG TRACE DIFF ($name) ==="
  # STRICT (compare seed+result too) only on the fixed-seed path, where both engines
  # ran the SAME mission id and so the seed is identical; lenient otherwise.
  STRICT_FLAG=""
  [ -n "$FIXED_MID" ] && STRICT_FLAG="--strict"
  python3 "$HERE/trace_diff.py" "$WORK/$name.java.trace" "$WORK/$name.rust.trace" \
    --a-label Java --b-label Rust $STRICT_FLAG
  trr=$?

  if [ "$tdr" != "0" ] || [ "$trr" -gt 1 ]; then OVERALL=1; fi
done

echo
echo "######################################################################"
echo "# DONE. Artifacts in $WORK"
echo "######################################################################"
exit $OVERALL
