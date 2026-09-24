#!/usr/bin/env bash
# check_universe.sh — Verify a universe's REST backend is alive via GET /game/user/exists.
#
# Usage:  ./check_universe.sh <token> <universe_id>
#
# Resolves the universe's restBaseUrl (reuses list_universes.sh), then calls
#   GET <restBaseUrl>/game/user/exists   with   Authorization: Bearer <token>
#
# Contract:
#   stdout : on success  -> body (JSON boolean: user registered in universe? true/false)
#            on failure  -> "HTTP <code> <body>"
#   stderr : restBaseUrl used + diagnostics
#   exit   : 0 alive(200) · 1 env/usage error · 3 universe ID not found · 4 universe unhealthy
set -euo pipefail

dir=$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)

token="${1:-}"
universe_id="${2:-}"
if [ -z "$token" ] || [ -z "$universe_id" ]; then
  echo "ERROR: usage: $0 <token> <universe_id>" >&2
  exit 1
fi

base=$("$dir/list_universes.sh" "$universe_id") || exit $?

http_code=$(curl -sS -o /tmp/owge_exists_body.$$ -w '%{http_code}' \
  "$base/game/user/exists" \
  -H "Authorization: Bearer $token") || {
  rm -f /tmp/owge_exists_body.$$
  echo "ERROR: request to $base/game/user/exists failed" >&2
  exit 4
}
body=$(cat /tmp/owge_exists_body.$$); rm -f /tmp/owge_exists_body.$$

if [ "$http_code" != "200" ]; then
  echo "restBaseUrl: $base" >&2
  echo "HTTP $http_code $body"
  exit 4
fi

echo "restBaseUrl: $base" >&2
printf '%s\n' "$body"
