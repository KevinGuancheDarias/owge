FROM php:7.3-cli
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

RUN docker-php-ext-install mysqli mbstring

RUN apt-get update && \
    apt-get install -y --no-install-recommends git zip
RUN curl --silent --show-error https://getcomposer.org/installer | php

RUN mv composer.phar /bin/composer
COPY . /app
WORKDIR /app
RUN composer install
RUN ln -s /app/console.php /bin/console

EXPOSE 8080
CMD [ "php", "-S", "0.0.0.0:8080",  "-t", "/app/web" ]