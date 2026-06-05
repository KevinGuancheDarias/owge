# Runtime image for the Rust port of the OWGE game backend (owge-rest).
# The binary is compiled by launch_rust_rest.sh inside a rust:1-bookworm builder
# (so its glibc matches this bookworm-slim runtime) and copied into
# `target/owge-rest` before `docker-compose build`, mirroring how the Java
# container receives its pre-built war.
FROM debian:bookworm-slim
LABEL maintainer="Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

# 8080 = REST/HTTP, 7474 = socket.io realtime (matches the Java container ports
# the reverse proxy forwards to).
EXPOSE 8080
EXPOSE 7474

ARG binHolderDirectory=target

ENV TERM=xterm

# ca-certificates only: sqlx uses rustls (no libmysqlclient / no libssl needed).
RUN apt-get update \
    && apt-get install -y --no-install-recommends ca-certificates \
    && rm -rf /var/lib/apt/lists/*

RUN mkdir /app
COPY $binHolderDirectory/owge-rest /app/owge-rest
RUN chmod +x /app/owge-rest

CMD ["/app/owge-rest"]
