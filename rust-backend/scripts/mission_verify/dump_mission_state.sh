#!/usr/bin/env bash
# dump_mission_state.sh <db> <out.json>
#
# Dumps the canonical "mission footprint" tables of OWGE to a normalised JSON
# document so two runs (Java vs Rust) can be table-diffed (see table_diff.py).
#
# The mission footprint is every table a mission can mutate:
#   obtained_units, missions, mission_reports, mission_information,
#   user_storage (resource/points cols only), planets (owner/home),
#   unlocked_relation, active_time_specials, scheduled_tasks.
#
# Rows are filtered to the test users (1,2) and their planets (1002,1003,1004)
# where the table has a user/planet column; tables without one are dumped whole
# but they are small in a test universe.
#
# All queries are issued through `docker exec <DB_CONTAINER> mysql` so no local
# mysql client is required (matches compare_rest_sync.sh's approach).
#
# Usage:
#   dump_mission_state.sh owge /tmp/java_state.json
#   DB_CONTAINER=owge_backend_developer-db-1 dump_mission_state.sh owge out.json
set -euo pipefail

DB_NAME="${1:?usage: dump_mission_state.sh <db> <out.json>}"
OUT="${2:?usage: dump_mission_state.sh <db> <out.json>}"

DB_CONTAINER="${DB_CONTAINER:-owge_backend_developer-db-1}"
DB_PASS="${DB_PASS:-1234}"
DB_USER="${DB_USER:-root}"

# Test scope. Override via env if a scenario uses other ids.
USERS="${USERS:-1,2}"
PLANETS="${PLANETS:-1002,1003,1004}"

# Run a SQL query in the DB container, batch/tab-separated, with a header row.
run_sql() {
  # --default-character-set=utf8mb4: the DB stores text as utf8mb4 but the client
  # default is latin1, which would return multi-byte chars as raw latin1 bytes and
  # produce invalid-UTF-8 (surrogate) JSON. Force utf8mb4 so the stream is valid.
  docker exec -i "$DB_CONTAINER" \
    mysql -u"$DB_USER" -p"$DB_PASS" --default-character-set=utf8mb4 --batch --raw "$DB_NAME" -e "$1" 2>/dev/null
}

# Each entry: <table-key>=<SQL>. The SELECT column list defines the JSON keys.
declare -A QUERIES
QUERIES[obtained_units]="SELECT id,user_id,unit_id,count,source_planet,target_planet,mission_id,first_deployment_mission,is_from_capture,expiration_id,owner_unit_id FROM obtained_units WHERE user_id IN ($USERS) ORDER BY user_id,unit_id,source_planet,id;"
QUERIES[missions]="SELECT id,user_id,type,required_time,primary_resource,secondary_resource,required_energy,source_planet,target_planet,related_mission,report_id,attemps,resolved,invisible FROM missions WHERE user_id IN ($USERS) ORDER BY user_id,type,id;"
QUERIES[mission_reports]="SELECT id,user_id,is_enemy,json_body FROM mission_reports WHERE user_id IN ($USERS) ORDER BY user_id,id;"
QUERIES[mission_information]="SELECT mi.id,mi.mission_id,mi.relation_id,mi.value FROM mission_information mi JOIN missions m ON m.id=mi.mission_id WHERE m.user_id IN ($USERS) ORDER BY mi.mission_id,mi.relation_id,mi.id;"
QUERIES[user_storage]="SELECT id,points,primary_resource,secondary_resource,energy,alliance_id FROM user_storage WHERE id IN ($USERS) ORDER BY id;"
QUERIES[alliances]="SELECT id,name,description,owner_id FROM alliances ORDER BY id;"
# request_date is wall-clock (differs between passes by construction) — excluded
QUERIES[alliance_join_request]="SELECT id,alliance_id,user_id FROM alliance_join_request WHERE user_id IN ($USERS) ORDER BY user_id,alliance_id,id;"
QUERIES[planet_list]="SELECT user_id,planet_id,name FROM planet_list WHERE user_id IN ($USERS) ORDER BY user_id,planet_id;"
QUERIES[planets]="SELECT id,owner,home FROM planets WHERE id IN ($PLANETS) ORDER BY id;"
QUERIES[unlocked_relation]="SELECT id,user_id,relation_id FROM unlocked_relation WHERE user_id IN ($USERS) ORDER BY user_id,relation_id,id;"
QUERIES[active_time_specials]="SELECT id,user_id,time_special_id,state FROM active_time_specials WHERE user_id IN ($USERS) ORDER BY user_id,time_special_id,id;"
# scheduled_tasks has no user column; dump task identity only (count + names of
# pending tasks per name is what matters for return-mission scheduling parity).
QUERIES[scheduled_tasks]="SELECT task_name,task_instance FROM scheduled_tasks ORDER BY task_name,task_instance;"

# Emit a single JSON object {table: [ {col: val,...}, ... ]} via python, fed the
# tab-separated dumps on stdin as 'TABLE\n<header>\n<rows...>\n\f' blocks.
#
# The parser is passed as -c (NOT a stdin heredoc): the tab-separated blocks are
# what we pipe on stdin, so stdin must stay the data channel.
read -r -d '' PARSER <<'PY' || true
import sys, json

out_path = sys.argv[1]
raw = sys.stdin.read()
tables = {}
cur = None
header = None
for block in raw.split("\f"):
    block = block.strip("\n")
    if not block.strip():
        continue
    lines = block.split("\n")
    name = None
    rows_lines = []
    for ln in lines:
        if ln.startswith("@@TABLE "):
            name = ln[len("@@TABLE "):].strip()
        else:
            rows_lines.append(ln)
    if name is None:
        continue
    rows = []
    if rows_lines:
        header = rows_lines[0].split("\t")
        for dl in rows_lines[1:]:
            if dl == "":
                continue
            vals = dl.split("\t")
            # tolerate ragged rows (NULL printed as the literal 'NULL' by --batch)
            row = {}
            for i, col in enumerate(header):
                v = vals[i] if i < len(vals) else None
                if v == "NULL":
                    v = None
                row[col] = v
            rows.append(row)
    tables[name] = rows

with open(out_path, "w") as fh:
    json.dump(tables, fh, indent=2, ensure_ascii=False)
print(f"wrote {out_path}: " + ", ".join(f"{k}={len(v)}" for k, v in sorted(tables.items())))
PY

{
  for key in "${!QUERIES[@]}"; do
    printf '@@TABLE %s\n' "$key"
    run_sql "${QUERIES[$key]}"
    printf '\f\n'
  done
} | python3 -c "$PARSER" "$OUT"
