FROM tomcat:7-jre8
MAINTAINER Kevin Guanche Darias
ARG warLocationFolder
ENV TERM xterm

COPY config /tmp/config
COPY $warLocationFolder /tmp/war_location_folder

RUN mv /tmp/config/tomcat/context.xml /usr/local/tomcat/conf/context.xml
RUN rm -r /usr/local/tomcat/webapps/ROOT
RUN mkdir -p /usr/local/tomcat/webapps/ROOT
RUN mv /tmp/war_location_folder/sgtjava-admin.war /usr/local/tomcat/webapps/admin.war
RUN mv /tmp/war_location_folder/sgtjava-game-rest.war /usr/local/tomcat/webapps/game_api.war
#RUN apt update && apt install -y nano

