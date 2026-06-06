#!/bin/bash
# Run the Rust backend in watch mode using cargo-watch.
# Recompiles and restarts on file changes.
set -euo pipefail
cd "$(dirname "$0")"
if [[ ! -f .env.local ]]; then
  echo "Missing .env.local (DB credentials). See README.md." >&2
  exit 1
fi

if ! command -v cargo-watch &> /dev/null; then
  echo "cargo-watch is not installed. Install it with: cargo install cargo-watch" >&2
  exit 1
fi
# shellcheck disable=SC1091
source .env.local
exec cargo watch -x 'run -p owge-rest'
