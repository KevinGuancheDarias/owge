#!/bin/bash
# Run the Rust backend against the dc12 CLONE database (sgalactica_java_12_base_fast).
# Credentials come from .env.local (gitignored). The active universe DB
# (sgalactica_java_12) is never touched.
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f .env.local ]]; then
  echo "Missing .env.local (DB credentials). See README.md." >&2
  exit 1
fi
# shellcheck disable=SC1091
source .env.local
exec cargo run -p owge-rest "$@"
