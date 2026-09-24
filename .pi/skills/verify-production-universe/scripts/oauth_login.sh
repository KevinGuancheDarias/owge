#!/usr/bin/env bash
# oauth_login.sh — Authenticate the test account and print ONLY the JWT access token to stdout.
#
# Contract:
#   stdout : the access token (nothing else — safe to $())
#   stderr : diagnostics
#   env    : OWGE_AI_USERNAME, OWGE_AI_PASSWORD, OWGE_AI_KGDW_URL
#   exit   : 0 ok · 1 env/usage error · 2 auth failed
set -euo pipefail

if [ -z "${OWGE_AI_USERNAME:-}" ] || [ -z "${OWGE_AI_PASSWORD:-}" ] || [ -z "${OWGE_AI_KGDW_URL:-}" ]; then
  echo "ERROR: need OWGE_AI_USERNAME, OWGE_AI_PASSWORD, OWGE_AI_KGDW_URL" >&2
  exit 1
fi

resp=$(curl -sS -X POST "$OWGE_AI_KGDW_URL/oauth/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=1e39e154-8ec1-4c72-81ed-48b47a2a7dd2" \
  -d "client_secret=1234" \
  -d "username=$OWGE_AI_USERNAME" \
  -d "password=$OWGE_AI_PASSWORD") || {
  echo "ERROR: token request failed: $resp" >&2
  exit 2;
}

token=$(echo "$resp" | jq -r '.access_token // .token // empty')
if [ -z "$token" ] || [ "$token" = "null" ]; then
  echo "ERROR: no token in response: $resp" >&2
  exit 2
fi

printf '%s\n' "$token"
