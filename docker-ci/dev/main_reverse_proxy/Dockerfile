FROM nginx
MAINTAINER Kevin Guanche Darias

ARG host_machine=192.168.99.1

EXPOSE 80

ENV CONF_PATH /tmp/config
ENV WWW_PATH /tmp/target
ENV TERM xterm

COPY config $CONF_PATH
COPY target $WWW_PATH

RUN rm /etc/nginx/conf.d/default.conf
RUN mv $CONF_PATH/nginx/conf.d/proxy_settings.conf /tmp/
RUN sed -i -e "s/DEV_MACHINE_IP_BOUNDARY/$host_machine/g" /tmp/proxy_settings.conf
RUN mv /tmp/proxy_settings.conf /etc/nginx/conf.d/
RUN mv $CONF_PATH/nginx/include /etc/nginx/

RUN rm -r /usr/share/nginx/html
RUN mv $WWW_PATH /usr/share/nginx/html
