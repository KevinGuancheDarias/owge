FROM nginx
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

ARG host_machine=10.74.32.252

EXPOSE 80

ENV CONF_PATH /tmp/config
ENV WWW_PATH /tmp/target
ENV TERM xterm

RUN mkdir /var/www;

COPY config $CONF_PATH
COPY target $WWW_PATH
COPY scripts/docker-entrypoint.sh  /

RUN rm /etc/nginx/conf.d/default.conf
RUN mv $CONF_PATH/nginx/conf.d/proxy_settings.template /etc/nginx/conf.d/
RUN mv $CONF_PATH/nginx/include /etc/nginx/

RUN rm -r /usr/share/nginx/html
RUN mv $WWW_PATH /usr/share/nginx/html

VOLUME [ "/var/owge_data/static" ]
VOLUME [ "/var/owge_data/dynamic" ]


ENTRYPOINT ["/docker-entrypoint.sh"]
CMD ["nginx", "-g", "daemon off;"]