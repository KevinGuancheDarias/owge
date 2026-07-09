#!/usr/bin/env bash
# run_bdd_parity.sh — BDD parity runner (BDD-PARITY-PLAN.md §5.1/§7).
#
# For each selected scenario:
#   RESET(baseline) -> driver(java) -> dump -> RESET -> driver(rust) -> dump -> DIFF
# Verdicts per scenario: JAVA_SPEC, RUST_SPEC (driver exit codes), PARITY
# (table_diff.py + sorted ws-deliver diff empty).
#
# Topology = mission_verify's shared-DB sequential model, with one deliberate
# hardening: ONLY ONE BACKEND IS AWAKE AT A TIME (Java container paused during
# the Rust pass and vice versa; Rust restarted fresh after every restore).
# Both backends poll the same scheduled_tasks table with the same task name, so
# a concurrently-idle backend can steal the other pass's nudged mission —
# mission_verify works around instances of that race; we remove it wholesale.
#
# Usage:
#   ./run_bdd_parity.sh [--feature <substring>] [--scenario <substring>]
#                       [--backend java|rust|both] [--keep-going]
#
#   --backend java   = "is the spec right?" mode (§7): no Rust pass, no diff.
#   --keep-going     = don't stop at first red scenario.
set -uo pipefail

HERE="$(cd "$(dirname "$0")" && pwd)"
SCRIPTS="$(cd "$HERE/.." && pwd)"                  # rust-backend/scripts
MISSION_VERIFY="$SCRIPTS/mission_verify"
RUN_ID="$(date +%Y%m%d_%H%M%S)"
WORK="${WORK:-/tmp/bdd_parity_runs/$RUN_ID}"
mkdir -p "$WORK"
# mirror all output to a stable path so humans can `tail -f` any run
exec > >(tee "$WORK/run.log" /tmp/bdd_parity_runs/latest.log) 2>&1

# --- environment (same knobs as mission_verify) -------------------------------
DB_CONTAINER="${DB_CONTAINER:-owge_backend_developer-db-1}"
DB_NAME="${DB_NAME:-owge}"
DB_USER="${DB_USER:-root}"
DB_PASS="${DB_PASS:-1234}"
JAVA_CONTAINER="owge-java-bdd"
JAVA_HOST_PORT="${JAVA_HOST_PORT:-18080}"
JAVA_WS_HOST_PORT="${JAVA_WS_HOST_PORT:-17474}"  # container netty-socketio :7474
RUST_PORT="${RUST_PORT:-8080}"
RUST_WS_PORT="${RUST_WS_PORT:-7474}"
RUST_BIN="${RUST_BIN:-$(cd "$HERE/../.." && pwd)/target/debug/owge-rest}"
DRIVER="$HERE/target/debug/bdd_parity"

FILTER_FEATURE=""
FILTER_SCENARIO=""
BACKENDS="both"
KEEP_GOING=0
while [ "$#" -gt 0 ]; do
  case "$1" in
    --feature)  FILTER_FEATURE="$2"; shift 2 ;;
    --scenario) FILTER_SCENARIO="$2"; shift 2 ;;
    --backend)  BACKENDS="$2"; shift 2 ;;
    --keep-going) KEEP_GOING=1; shift ;;
    *) echo "unknown arg: $1" >&2; exit 2 ;;
  esac
done

RUST_PID=""
cleanup() {
  docker rm -f "$JAVA_CONTAINER" >/dev/null 2>&1 || true
  [ -n "$RUST_PID" ] && kill "$RUST_PID" >/dev/null 2>&1 || true
  # leave the dev DB as we found it — otherwise the last scenario's residue
  # becomes part of the NEXT run's baseline dump (self-inflicted pollution)
  if [ -s "${BASELINE:-}" ]; then
    docker exec -i "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" <"$BASELINE" 2>/dev/null || true
    mysql_q "DELETE FROM scheduled_tasks WHERE task_name='mission-run';" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

mysql_exec() { docker exec -i "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -s 2>/dev/null; }
mysql_q()    { docker exec -i "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" -N -s -e "$1" 2>/dev/null; }

set_flags() {
  mysql_q "INSERT INTO configuration (name,value) VALUES ('ATTACK_DETERMINISTIC_RNG','TRUE') ON DUPLICATE KEY UPDATE value='TRUE';" || true
}

# --- baseline: full-DB dump taken ONCE at setup (§5.2: full restore beats
# table-scoped snapshots for correctness; optimize only if it gets slow) ------
BASELINE="$WORK/baseline.sql"
take_baseline() {
  echo "taking full-DB baseline -> $BASELINE"
  docker exec "$DB_CONTAINER" mysqldump -u"$DB_USER" -p"$DB_PASS" \
    --routines --triggers --add-drop-table "$DB_NAME" >"$BASELINE" 2>/dev/null
  [ -s "$BASELINE" ] || { echo "baseline dump FAILED"; exit 1; }
}
restore_baseline() {
  docker exec -i "$DB_CONTAINER" mysql -u"$DB_USER" -p"$DB_PASS" "$DB_NAME" <"$BASELINE" 2>/dev/null
  # §5.2 db-scheduler hygiene: no stale mission task may survive a reset
  mysql_q "DELETE FROM scheduled_tasks WHERE task_name='mission-run';" >/dev/null 2>&1 || true
  set_flags
}

# --- backend lifecycle (one awake at a time) ----------------------------------
NET=$(docker inspect "$DB_CONTAINER" --format '{{range $k,$v := .NetworkSettings.Networks}}{{$k}}{{end}}')
JAVA_BASE="http://127.0.0.1:$JAVA_HOST_PORT/game_api"
RUST_BASE="http://127.0.0.1:$RUST_PORT"

boot_java() {
  docker rm -f "$JAVA_CONTAINER" >/dev/null 2>&1 || true
  docker run -d --name "$JAVA_CONTAINER" --network "$NET" \
    -e OWGE_DB_URL="db:3306/$DB_NAME" -e OWGE_DB_USER="$DB_USER" -e OWGE_DB_PASS="$DB_PASS" \
    -e MYSQL_HOST=db -e MYSQL_DB="$DB_NAME" -e MYSQL_USER="$DB_USER" -e MYSQL_PASSWORD="$DB_PASS" \
    -e OWGE_CONTEXT_PATH=/game_api -e OWGE_CORS_CUSTOM_ORIGIN='*' \
    -e OWGE_WS_SYNC_RATELIMIT_PER_MINUTE=0 \
    -e ATTACK_DETERMINISTIC_RNG=TRUE -e OWGE_ATTACK_RNG_TRACE=TRUE \
    -p "$JAVA_HOST_PORT:8080" -p "$JAVA_WS_HOST_PORT:7474" \
    owge-java-compare:latest >/dev/null
  echo -n "waiting for Java :$JAVA_HOST_PORT "
  for _ in $(seq 1 90); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' "$JAVA_BASE/open/clock" || true)" = "200" ] \
      && { echo "up"; return 0; }
    sleep 2
  done
  echo "FAILED"; docker logs --tail 40 "$JAVA_CONTAINER" >&2; return 1
}
java_awake() { docker unpause "$JAVA_CONTAINER" >/dev/null 2>&1 || true; }
java_asleep() { docker pause "$JAVA_CONTAINER" >/dev/null 2>&1 || true; }

start_rust() {
  OWGE_DB_JDBC_URL="mysql://$DB_USER:$DB_PASS@127.0.0.1:3306/$DB_NAME" \
    OWGE_SERVER_PORT="$RUST_PORT" OWGE_WS_PORT="$RUST_WS_PORT" \
    ATTACK_DETERMINISTIC_RNG=TRUE OWGE_ATTACK_RNG_TRACE=TRUE \
    "$RUST_BIN" >>"$WORK/rust.stderr" 2>&1 &
  RUST_PID=$!
  for _ in $(seq 1 60); do
    [ "$(curl -s -o /dev/null -w '%{http_code}' "$RUST_BASE/open/clock" || true)" = "200" ] \
      && return 0
    sleep 1
  done
  echo "Rust FAILED to start"; tail -20 "$WORK/rust.stderr" >&2; return 1
}
stop_rust() {
  [ -n "$RUST_PID" ] && { kill "$RUST_PID" >/dev/null 2>&1 || true; wait "$RUST_PID" 2>/dev/null || true; RUST_PID=""; }
}

# --- one driver pass ----------------------------------------------------------
# $1 backend (java|rust)  $2 feature file  $3 scenario name  $4 artifacts dir
driver_pass() {
  local backend="$1" feature="$2" scenario="$3" art="$4"
  mkdir -p "$art"
  local name_regex
  name_regex=$(printf '%s' "$scenario" | sed -e 's/[][\.*^$()+?{}|]/\\&/g')
  OWGE_BDD_BACKEND="$backend" \
    OWGE_BDD_FEATURES="$feature" \
    OWGE_BDD_ARTIFACTS="$art" \
    OWGE_BDD_JAVA_BASE="$JAVA_BASE" \
    OWGE_BDD_RUST_BASE="$RUST_BASE" \
    OWGE_BDD_JAVA_WS="http://127.0.0.1:$JAVA_WS_HOST_PORT" \
    OWGE_BDD_RUST_WS="http://127.0.0.1:$RUST_WS_PORT" \
    "$DRIVER" --name "^${name_regex}\$" >"$art/driver.log" 2>&1
}

dump_state() { # $1 artifacts dir (reads scope.env written by the driver)
  local art="$1" USERS="1,2" PLANETS="1002,1003,1004"
  [ -f "$art/scope.env" ] && . "$art/scope.env"
  USERS="$USERS" PLANETS="$PLANETS" DB_CONTAINER="$DB_CONTAINER" \
    "$MISSION_VERIFY/dump_mission_state.sh" "$DB_NAME" "$art/tables.json" >/dev/null
}

# --- scenario enumeration ------------------------------------------------------
declare -a SCN_FILES SCN_NAMES
while IFS=: read -r file name; do
  [ -n "$name" ] || continue
  case "$file" in *"$FILTER_FEATURE"*) : ;; *) continue ;; esac
  case "$name" in *"$FILTER_SCENARIO"*) : ;; *) continue ;; esac
  SCN_FILES+=("$file"); SCN_NAMES+=("$name")
done < <(grep -H '^[[:space:]]*Scenario\( Outline\)\?:' "$HERE"/features/*.feature \
          | sed 's/:[[:space:]]*Scenario\( Outline\)\?:[[:space:]]*/:/')
[ "${#SCN_FILES[@]}" -gt 0 ] || { echo "no scenarios match the filters"; exit 2; }

# --- global setup ---------------------------------------------------------------
echo "artifacts: $WORK"
( cd "$HERE" && cargo build 2>&1 | tail -2 )
[ -x "$DRIVER" ] || { echo "driver binary missing: $DRIVER"; exit 1; }
set_flags
take_baseline
if [ "$BACKENDS" != "rust" ]; then boot_java || exit 1; java_asleep; fi

# --- main loop -------------------------------------------------------------------
OVERALL=0
SUMMARY=""
for i in "${!SCN_FILES[@]}"; do
  feature="${SCN_FILES[$i]}"; scenario="${SCN_NAMES[$i]}"
  fbase=$(basename "$feature" .feature)
  sslug=$(printf '%s' "$scenario" | tr -c 'A-Za-z0-9' '_' | cut -c1-60)
  ART="$WORK/$fbase/$sslug"
  echo
  echo "▶ $fbase :: $scenario"

  JAVA_SPEC="-" RUST_SPEC="-" PARITY="-"

  # INVARIANT: the Java container must be AWAKE during every restore — a
  # paused JVM freezes mid-connection and its metadata locks deadlock the
  # baseline's DROP TABLEs (observed: 40+ min hang). Java sleeps ONLY while
  # the Rust driver runs (the anti-task-stealing window).
  if [ "$BACKENDS" = "java" ] || [ "$BACKENDS" = "both" ]; then
    java_awake
    sleep 1
    restore_baseline
    if driver_pass java "$feature" "$scenario" "$ART/java"; then JAVA_SPEC="✅"; else JAVA_SPEC="🔴"; fi
    dump_state "$ART/java"
    [ "$JAVA_SPEC" = "🔴" ] && sed -n '/✘/,+6p' "$ART/java/driver.log" | sed 's/^/  JAVA  /'
  fi

  if [ "$BACKENDS" = "rust" ] || [ "$BACKENDS" = "both" ]; then
    java_awake
    sleep 1
    restore_baseline
    java_asleep
    start_rust || { echo "  RUST  backend failed to start"; OVERALL=1; continue; }
    if driver_pass rust "$feature" "$scenario" "$ART/rust"; then RUST_SPEC="✅"; else RUST_SPEC="🔴"; fi
    dump_state "$ART/rust"
    stop_rust
    [ "$RUST_SPEC" = "🔴" ] && sed -n '/✘/,+6p' "$ART/rust/driver.log" | sed 's/^/  RUST  /'
  fi

  if [ "$BACKENDS" = "both" ]; then
    PARITY="✅"
    if ! python3 "$MISSION_VERIFY/table_diff.py" "$ART/java/tables.json" "$ART/rust/tables.json" \
         --a-label Java --b-label Rust >"$ART/table.diff" 2>&1; then
      PARITY="🔴"; sed 's/^/  TABLE /' "$ART/table.diff"
    fi
    # ws deliver frames: sorted-multiset diff per captured user (§5.5).
    # NORMALIZATION (not suppression, plan §5.4): wall-clock-derived VALUES
    # (terminationDate/startingDate/pendingMillis/…) always differ between the
    # sequential Java and Rust passes by construction. Values are replaced with
    # placeholders that PRESERVE the serialization format (<TS-ARR> vs <TS-STR>)
    # so a Jackson-array vs ISO-string divergence still shows as a diff.
    norm_ws() { python3 "$HERE/normalize_ws.py"; }
    for jf in "$ART"/java/ws_user*.jsonl; do
      [ -f "$jf" ] || continue
      rf="$ART/rust/$(basename "$jf")"
      grep '"kind":"deliver"' "$jf" 2>/dev/null | norm_ws | sort >"$jf.sorted" || true
      grep '"kind":"deliver"' "${rf}" 2>/dev/null | norm_ws | sort >"$jf.rust.sorted" || true
      if ! diff -u "$jf.sorted" "$jf.rust.sorted" >"$jf.diff" 2>&1; then
        PARITY="🔴"; echo "  WS    $(basename "$jf"): differs (see $jf.diff)"
      fi
    done
  fi

  echo "  JAVA_SPEC  $JAVA_SPEC"
  echo "  RUST_SPEC  $RUST_SPEC"
  echo "  PARITY     $PARITY"
  SUMMARY="$SUMMARY
$fbase :: $scenario | $JAVA_SPEC | $RUST_SPEC | $PARITY"
  case "$JAVA_SPEC$RUST_SPEC$PARITY" in *🔴*) OVERALL=1; [ "$KEEP_GOING" = "1" ] || break ;; esac
done

echo
echo "==================== SUMMARY (scenario | JAVA_SPEC | RUST_SPEC | PARITY)"
echo "$SUMMARY"
echo
echo "artifacts: $WORK"
exit $OVERALL
