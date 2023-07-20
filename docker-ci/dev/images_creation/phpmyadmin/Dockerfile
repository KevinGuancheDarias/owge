ARG OWGE_PHPMYADMIN_ARCH_IMAGE
FROM ${OWGE_PHPMYADMIN_ARCH_IMAGE}
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

COPY scripts/owge-docker-entrypoint.sh  /
RUN cat /owge-docker-entrypoint.sh /docker-entrypoint.sh > /docker-entrypoint.sh.tmp && mv /docker-entrypoint.sh.tmp /docker-entrypoint.sh && chmod +x /docker-entrypoint.sh
