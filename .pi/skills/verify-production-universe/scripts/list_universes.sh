#!/usr/bin/env bash
# list_universes.sh — Fetch the official universe list (or one universe's restBaseUrl).
#
# Usage:
#   ./list_universes.sh            -> print the full JSON list
#   ./list_universes.sh <id>       -> print ONLY the restBaseUrl for that universe ID
#
# Contract:
#   stdout : JSON list (no arg) or the restBaseUrl (with arg) — nothing else
#   stderr : diagnostics
#   env    : OWGE_AI_KGDW_URL
#   exit   : 0 ok · 1 env/usage error · 3 universe ID not found
set -euo pipefail

if [ -z "${OWGE_AI_KGDW_URL:-}" ]; then
  echo "ERROR: need OWGE_AI_KGDW_URL" >&2
  exit 1
fi

list=$(curl -sS "$OWGE_AI_KGDW_URL/universe/findOfficials") || {
  echo "ERROR: failed to fetch universe list" >&2
  exit 1
}

if [ -z "${1:-}" ]; then
  printf '%s\n' "$list" | jq
  exit 0
fi

base=$(echo "$list" | jq -r --arg id "$1" '.[] | select((.id|tostring) == ($id|tostring)) | .restBaseUrl')
if [ -z "$base" ] || [ "$base" = "null" ]; then
  echo "ERROR: universe ID '$1' not found. Available:" >&2
  echo "$list" | jq -r '.[] | "  - ID: \(.id), Name: \(.name), restBaseUrl: \(.restBaseUrl)"' >&2
  exit 3
fi

printf '%s\n' "$base"
