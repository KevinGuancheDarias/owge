# Dev image for the universe wiki generator (owge-wiki-gen watch). Compiles the
# workspace from source in a rust builder like backend_rest_rust, then runs the
# generator in a slim runtime that first waits for an initialized MySQL (the
# same `SELECT 1 FROM configuration` readiness gate) — the watcher treats a
# NULL `CHECKSUM TABLE` (missing table) as fatal by design, so it must not
# start against a database that is still loading its schema.
#
# Build context is rust-backend/ (see wiki_generator.docker-compose.yml).
#
# The generator writes into the SAME dynamic-images volume nginx serves
# (`--out /var/owge_data/dynamic/wiki`), so the wiki is reachable at
# /dynamic/wiki/index.html and its /dynamic/<file> image URLs resolve with no
# extra nginx configuration.
FROM rust:1-bookworm AS builder
# The MysqlTemplate derive macro pipes its generated code through rustfmt and
# panics when it is missing (current rust:1 images don't ship it by default).
RUN rustup component add rustfmt
WORKDIR /build
COPY . .
RUN cargo build --release --bin owge-wiki-gen

FROM debian:bookworm-slim
LABEL maintainer="Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

# ca-certificates for rustls; default-mysql-client only for the startup DB wait.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates default-mysql-client \
    && rm -rf /var/lib/apt/lists/*

COPY --from=builder /build/target/release/owge-wiki-gen /app/owge-wiki-gen

# Wait for an initialized DB, then run the watcher. Connection settings come
# from the environment:
#   OWGE_WIKI_DB_URL   mysql://user:pass@host:port/db (the watcher's connection)
#   OWGE_WIKI_UNIVERSE display name on the generated pages
#   OWGE_WIKI_INTERVAL checksum poll seconds
#   MYSQL_HOST/_PORT/_USER/_PASSWORD/_DB  only for the readiness wait
RUN printf '%s\n' \
    '#!/bin/sh' \
    ': "${MYSQL_HOST:=db}"; : "${MYSQL_PORT:=3306}"; : "${MYSQL_USER:=root}"; : "${MYSQL_PASSWORD:=1234}"; : "${MYSQL_DB:=owge}"' \
    ': "${OWGE_WIKI_DB_URL:=mysql://root:1234@db:3306/owge}"' \
    ': "${OWGE_WIKI_UNIVERSE:=OWGE Universe}"' \
    ': "${OWGE_WIKI_INTERVAL:=30}"' \
    'echo "Waiting for an initialized MySQL at $MYSQL_HOST:$MYSQL_PORT ..."' \
    'i=0' \
    'until mysql -h "$MYSQL_HOST" -P "$MYSQL_PORT" -u "$MYSQL_USER" -p"$MYSQL_PASSWORD" "$MYSQL_DB" -e "SELECT 1 FROM configuration;" >/dev/null 2>&1; do' \
    '  i=$((i+1)); [ "$i" -gt 300 ] && { echo "Timed out waiting for MySQL"; exit 1; }' \
    '  sleep 1' \
    'done' \
    'echo "MySQL ready; starting owge-wiki-gen watch"' \
    'exec /app/owge-wiki-gen watch --db "$OWGE_WIKI_DB_URL" --out /var/owge_data/dynamic/wiki --universe "$OWGE_WIKI_UNIVERSE" --interval "$OWGE_WIKI_INTERVAL"' \
    > /wait_and_run.sh \
    && chmod +x /wait_and_run.sh

CMD ["/wait_and_run.sh"]
