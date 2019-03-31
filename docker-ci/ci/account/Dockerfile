FROM tomcat:8-jre8
MAINTAINER Kevin Guanche Darias
ARG warHolderDirectory
ARG OWGE_ACCOUNT_WAR_FILENAME
EXPOSE 8081
ENV TERM xterm

COPY config /tmp/config
COPY $warHolderDirectory /tmp/war_location_folder

RUN mv /tmp/config/tomcat/context.xml /usr/local/tomcat/conf/context.xml
RUN mv /tmp/config/tomcat/server.xml /usr/local/tomcat/conf/server.xml
RUN rm -r /usr/local/tomcat/webapps/ROOT
#RUN mkdir -p /usr/local/tomcat/webapps/ROOT
RUN mv /tmp/war_location_folder/$OWGE_ACCOUNT_WAR_FILENAME /usr/local/tomcat/webapps/ROOT.war
#RUN apt update && apt install -y nano

