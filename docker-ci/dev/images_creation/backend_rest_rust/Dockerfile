# Dev "all dockerized" image for the RUST backend (owge-rest). Compiles the
# workspace from source in a rust builder, then runs the binary in a slim
# runtime that first waits for the dockerized MySQL to be initialized — the same
# readiness gate the Java backend uses (wait for `SELECT 1 FROM configuration`).
#
# Build context is rust-backend/ (see backend_rust.docker-compose.yml), so
# `COPY . .` is the workspace (minus target/, per rust-backend/.dockerignore).
FROM rust:1-bookworm AS builder
WORKDIR /build
COPY . .
RUN cargo build --release --bin owge-rest

FROM debian:bookworm-slim
LABEL maintainer="Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

EXPOSE 8080
EXPOSE 7474

# ca-certificates for rustls; default-mysql-client only for the startup DB wait.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates default-mysql-client \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/release/owge-rest /app/owge-rest

# Inline wait-for-DB wrapper (mirrors images_creation/scripts/wait_mysql.sh): the
# universe DB container loads schema + base data on first init, so we block until
# the `configuration` table answers, then exec the backend.
RUN printf '%s\n' \
    '#!/bin/sh' \
    ': "${MYSQL_HOST:=db}"; : "${MYSQL_USER:=root}"; : "${MYSQL_PASSWORD:=1234}"; : "${MYSQL_DB:=owge}"' \
    'echo "Waiting for an initialized MySQL at $MYSQL_HOST ..."' \
    'i=0' \
    'until mysql -h "$MYSQL_HOST" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" -e "SELECT 1 FROM configuration;" >/dev/null 2>&1; do' \
    '  i=$((i+1)); [ "$i" -gt 180 ] && { echo "Timed out waiting for MySQL"; exit 1; }' \
    '  sleep 1' \
    'done' \
    'echo "MySQL ready; starting owge-rest"' \
    'exec "$@"' \
    > /wait_and_run.sh \
    && chmod +x /wait_and_run.sh

CMD ["/wait_and_run.sh", "/app/owge-rest"]
