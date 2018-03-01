FROM nginx
MAINTAINER Kevin Guanche Darias
ENV CONF_PATH /tmp/config
ENV WWW_PATH /tmp/target
ENV TERM xterm

COPY config $CONF_PATH
COPY target $WWW_PATH

RUN rm /etc/nginx/conf.d/default.conf
RUN mv $CONF_PATH/nginx/conf.d/proxy_settings.conf /etc/nginx/conf.d/
RUN mv $CONF_PATH/nginx/include /etc/nginx/

RUN rm -r /usr/share/nginx/html
RUN mv $WWW_PATH /usr/share/nginx/html
