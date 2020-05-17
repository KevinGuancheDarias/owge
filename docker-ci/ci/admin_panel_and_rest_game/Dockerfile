FROM  maven:3-jdk-11-openj9
LABEL Autor Kevin Guanche Darias

ARG warHolderDirectory
ARG OWGE_REST_WAR_FILENAME
ARG OWGE_UNIVERSE_ID
ARG OWGE_DB_URL
ARG OWGE_DB_USER
ARG OWGE_DB_PASS

ENV TERM xterm

COPY $warHolderDirectory /tmp/war_location_folder

RUN mkdir /app
RUN mv /tmp/war_location_folder/$OWGE_REST_WAR_FILENAME /app/rest.jar
