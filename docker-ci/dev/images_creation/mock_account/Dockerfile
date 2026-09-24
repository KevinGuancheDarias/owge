FROM php:7.3-cli
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

RUN docker-php-ext-install mysqli mbstring

# composer needs the `unzip` CLI (no PHP zip ext in this image) to unpack the
# dist archives. Debian 11 (bullseye) is EOL, so its -security packages no longer
# exist on security.debian.org / deb.debian.org (they 404). The base image's
# frozen snapshot (20220316T000000Z) still serves the plain, non-security
# `unzip 6.0-26`; we pin to it explicitly so apt never reaches for the 404'ing
# security version.
RUN printf '%s\n' \
        'deb http://snapshot.debian.org/archive/debian/20220316T000000Z bullseye main' \
        'deb http://snapshot.debian.org/archive/debian/20220316T000000Z bullseye-updates main' \
        > /etc/apt/sources.list.d/owge-snapshot.list \
    && sed -i '/^deb /d' /etc/apt/sources.list \
    && apt-get -o Acquire::Check-Valid-Until=false update \
    && apt-get -o Acquire::Check-Valid-Until=false install -y --no-install-recommends unzip=6.0-26 \
    && rm -rf /var/lib/apt/lists/*

RUN curl --silent --show-error https://getcomposer.org/installer | php

RUN mv composer.phar /bin/composer
COPY . /app
WORKDIR /app
RUN composer install
RUN ln -s /app/console.php /bin/console

EXPOSE 8080
CMD [ "php", "-S", "0.0.0.0:8080",  "-t", "/app/web" ]