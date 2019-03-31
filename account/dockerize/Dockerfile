FROM tomcat:7-jre8
MAINTAINER Kevin Guanche Darias
ARG warLocationFolder=target
ENV TERM xterm

COPY config /tmp/config
COPY $warLocationFolder /tmp/war_location_folder

RUN mv /tmp/config/tomcat/context.xml /usr/local/tomcat/conf/context.xml
RUN rm -r /usr/local/tomcat/webapps/ROOT
RUN mv /tmp/war_location_folder/owgejava-account.war /usr/local/tomcat/webapps/ROOT.war