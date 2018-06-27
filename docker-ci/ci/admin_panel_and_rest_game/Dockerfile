FROM tomcat:8-jre8
MAINTAINER Kevin Guanche Darias
ARG warHolderDirectory
ARG SGT_ADMIN_WAR_FILENAME
ARG SGT_REST_WAR_FILENAME

ENV TERM xterm

COPY config /tmp/config
COPY $warHolderDirectory /tmp/war_location_folder

RUN echo VALUES ARE: $SGT_ADMIN_WAR_FILENAME , $SGT_REST_WAR_FILENAME;
RUN ls /tmp/war_location_folder;
RUN mv /tmp/config/tomcat/context.xml /usr/local/tomcat/conf/context.xml
RUN rm -r /usr/local/tomcat/webapps/ROOT
RUN mkdir -p /usr/local/tomcat/webapps/ROOT
RUN mv /tmp/war_location_folder/$SGT_ADMIN_WAR_FILENAME /usr/local/tomcat/webapps/admin.war
RUN mv /tmp/war_location_folder/$SGT_REST_WAR_FILENAME /usr/local/tomcat/webapps/game_api.war
#RUN apt update && apt install -y nano

