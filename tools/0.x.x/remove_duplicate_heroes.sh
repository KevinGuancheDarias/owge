#!/usr/bin/env bash
#
# remove_duplicate_heroes.sh
#
# Detects and (optionally) removes duplicated "heroes" — i.e. units whose
# definition has is_unique = 1 — that a single user ended up owning more than
# once because of a bug.
#
# A unique unit MUST exist exactly once per user, as a single obtained_units
# row with count = 1. A hero can be duplicated in two ways, and this script
# handles BOTH:
#
#   1) A single obtained_units row with count > 1.
#   2) Several obtained_units rows (each count = 1) for the same (user, unit),
#      typically because the same hero was "built" / ended up on several
#      planets / missions.
#
# For every (user_id, unit_id) group of a unique unit the script keeps exactly
# ONE row (forcing its count back to 1) and deletes the rest. The kept row is
# chosen to be the most "settled" copy, in this preference order:
#
#       1. a copy idle on a planet            (mission_id IS NULL)
#       2. a copy garrisoned on a planet      (DEPLOYED mission)
#       3. anything else (e.g. in transit on a GATHER mission)
#   tie-break: the lowest obtained_units.id   (the oldest copy)
#
# Deleting a copy that is attached to a running mission is safe here because
# every such mission also carries many other units, so it is never left empty;
# the deleted copy simply does not come back when the mission resolves, while
# the kept copy remains under the player's control.
#
# DRY RUN IS ON BY DEFAULT. Nothing is modified unless you pass --apply.
#
# Connection is taken from the same env vars the deploy scripts use:
#   OWGE_DB_URL  = host:port/dbname   (e.g. 172.20.0.1:3306/sgalactica_java_12)
#   OWGE_DB_USER = db user
#   OWGE_DB_PASS = db password
#
# Usage:
#   OWGE_DB_URL=172.20.0.1:3306/sgalactica_java_12 OWGE_DB_USER=u OWGE_DB_PASS=p \
#       ./remove_duplicate_heroes.sh            # dry run: only reports
#   ... ./remove_duplicate_heroes.sh --apply    # actually delete / fix counts
#
set -euo pipefail

APPLY=0
for arg in "$@"; do
  case "$arg" in
    --apply) APPLY=1 ;;
    --dry-run) APPLY=0 ;;
    -h|--help)
      sed -n '2,40p' "$0"; exit 0 ;;
    *) echo "Unknown argument: $arg" >&2; exit 2 ;;
  esac
done

: "${OWGE_DB_URL:?Set OWGE_DB_URL=host:port/dbname}"
: "${OWGE_DB_USER:?Set OWGE_DB_USER}"
: "${OWGE_DB_PASS:?Set OWGE_DB_PASS}"

DB_HOST="${OWGE_DB_URL%%:*}"
_rest="${OWGE_DB_URL#*:}"
DB_PORT="${_rest%%/*}"
DB_NAME="${_rest#*/}"

# Password via a temp defaults file so it never appears on the command line.
CNF="$(mktemp)"
trap 'rm -f "$CNF"' EXIT
cat > "$CNF" <<EOF
[client]
host=${DB_HOST}
port=${DB_PORT}
user=${OWGE_DB_USER}
password=${OWGE_DB_PASS}
database=${DB_NAME}
EOF
chmod 600 "$CNF"

mysql_run() { mysql --defaults-extra-file="$CNF" "$@"; }

# ---------------------------------------------------------------------------
# Shared prelude: build a TEMPORARY TABLE with the keep/delete decision for
# every row that belongs to a duplicated unique-unit group. Everything below
# runs in a SINGLE mysql session so the temp table survives.
# ---------------------------------------------------------------------------
read -r -d '' PRELUDE <<'SQL' || true
DROP TEMPORARY TABLE IF EXISTS dup_decisions;
CREATE TEMPORARY TABLE dup_decisions AS
SELECT t.*,
       CASE WHEN t.rn = 1 THEN 'KEEP' ELSE 'DELETE' END AS action
FROM (
  SELECT
    ou.id            AS obtained_unit_id,
    ou.user_id,
    ou.unit_id,
    ou.count,
    ou.source_planet,
    ou.target_planet,
    ou.mission_id,
    COALESCE(mt.code, 'NONE') AS mission_code,
    ROW_NUMBER() OVER (
        PARTITION BY ou.user_id, ou.unit_id
        ORDER BY CASE WHEN ou.mission_id IS NULL THEN 0
                      WHEN mt.code = 'DEPLOYED'  THEN 1
                      ELSE 2 END,
                 ou.id
    ) AS rn,
    COUNT(*)      OVER (PARTITION BY ou.user_id, ou.unit_id) AS grp_rows,
    SUM(ou.count) OVER (PARTITION BY ou.user_id, ou.unit_id) AS grp_total
  FROM obtained_units ou
  JOIN units u       ON u.id = ou.unit_id
  LEFT JOIN missions m       ON m.id  = ou.mission_id
  LEFT JOIN mission_types mt ON mt.id = m.type
  WHERE u.is_unique = 1
) t
WHERE t.grp_rows > 1 OR t.grp_total > 1;
SQL

# ---------------------------------------------------------------------------
# Report (always runs).
# ---------------------------------------------------------------------------
read -r -d '' REPORT <<'SQL' || true
SELECT '==== USERS WITH DUPLICATED HEROES ====' AS '';
SELECT d.user_id                              AS user_id,
       us.username                            AS user_name,
       COUNT(DISTINCT d.unit_id)              AS duplicated_heroes,
       COUNT(*)                               AS total_rows,
       SUM(d.action = 'DELETE')               AS rows_to_delete
FROM dup_decisions d
JOIN user_storage us ON us.id = d.user_id
GROUP BY d.user_id, us.username
ORDER BY d.user_id;

SELECT '==== DETAIL: every duplicated hero copy (planet + unit + action) ====' AS '';
SELECT d.user_id                              AS user_id,
       us.username                            AS user_name,
       d.unit_id                              AS unit_id,
       un.name                                AS unit_name,
       d.count                                AS unit_count,
       d.source_planet                        AS planet_id,
       p.name                                 AS planet_name,
       d.mission_code                         AS location,
       d.action                               AS action
FROM dup_decisions d
JOIN user_storage us ON us.id = d.user_id
JOIN units un        ON un.id = d.unit_id
LEFT JOIN planets p  ON p.id  = d.source_planet
ORDER BY d.user_id, d.unit_id, d.action DESC, d.obtained_unit_id;

SELECT '==== WHAT WOULD CHANGE ====' AS '';
SELECT
  SUM(action = 'DELETE')                              AS rows_to_delete,
  SUM(action = 'KEEP' AND count <> 1)                 AS kept_rows_count_reset_to_1,
  COUNT(DISTINCT CONCAT(user_id, '-', unit_id))       AS affected_hero_groups
FROM dup_decisions;
SQL

echo "=========================================================================="
echo " Duplicate hero cleanup  —  db: ${DB_NAME}@${DB_HOST}:${DB_PORT}"
echo " Mode: $([ "$APPLY" -eq 1 ] && echo 'APPLY (changes WILL be written)' || echo 'DRY RUN (no changes)')"
echo "=========================================================================="

if [ "$APPLY" -eq 0 ]; then
  # Dry run: build temp table + report only, inside one session.
  printf '%s\n%s\n' "$PRELUDE" "$REPORT" | mysql_run --table
  echo
  echo ">> DRY RUN complete. No rows were modified. Re-run with --apply to execute."
else
  read -r -d '' MUTATE <<'SQL' || true
START TRANSACTION;

DELETE ou FROM obtained_units ou
JOIN dup_decisions d ON d.obtained_unit_id = ou.id
WHERE d.action = 'DELETE';

UPDATE obtained_units ou
JOIN dup_decisions d ON d.obtained_unit_id = ou.id
SET ou.count = 1
WHERE d.action = 'KEEP' AND ou.count <> 1;

COMMIT;

SELECT '==== VERIFICATION: remaining duplicated heroes (should be empty) ====' AS '';
SELECT ou.user_id, ou.unit_id, COUNT(*) AS rows_cnt, SUM(ou.count) AS total_count
FROM obtained_units ou
JOIN units u ON u.id = ou.unit_id
WHERE u.is_unique = 1
GROUP BY ou.user_id, ou.unit_id
HAVING rows_cnt > 1 OR total_count > 1;
SQL
  printf '%s\n%s\n%s\n' "$PRELUDE" "$REPORT" "$MUTATE" | mysql_run --table
  echo
  echo ">> APPLY complete. Duplicates removed and kept-copy counts normalized to 1."
fi
