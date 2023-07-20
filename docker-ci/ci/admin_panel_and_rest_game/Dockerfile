#FROM adoptopenjdk/openjdk11-openj9:alpine
FROM eclipse-temurin:17-jdk-focal
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

EXPOSE 8080

ARG warHolderDirectory
ARG OWGE_REST_WAR_FILENAME
ARG OWGE_UNIVERSE_ID

ENV TERM xterm

COPY $warHolderDirectory /tmp/war_location_folder

RUN cp -rp /tmp/war_location_folder/scripts /scripts
RUN chmod +x /scripts/*.sh
RUN mkdir /app
RUN mv /tmp/war_location_folder/$OWGE_REST_WAR_FILENAME /app/rest.jar

# Add "-Xlog:gc" to see the GC info
CMD ["java", "-XX:+UseShenandoahGC","-Xmx220m", "-Xms100m", "-jar", "-Dspring.profiles.active=rsaKeys", "/app/rest.jar"]