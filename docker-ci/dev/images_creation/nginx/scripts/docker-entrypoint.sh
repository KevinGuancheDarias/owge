#!/bin/bash
(
    if [ -z "$OWGE_REST_CONTEXT_PATH" ]; then
        echo -e "\e[33mWarning:\e[39m Context path for REST ommited, will use \e[32mgame_api\e[39m";
        export OWGE_REST_CONTEXT_PATH="game_api";
    fi
    if [ -z "$OWGE_ADMIN_CONTEXT_PATH" ]; then
        echo -e "\e[33mWarning:\e[39m Context path for ADMIN ommited, will use \e[32madmin\e[39m";
        export OWGE_ADMIN_CONTEXT_PATH="admin";
    fi
    envsubst '${OWGE_BACKEND_SERVER} ${OWGE_WS_SERVER} ${OWGE_FRONTEND_SERVER} ${OWGE_ADMIN_FRONTEND_SERVER} ${OWGE_ACCOUNT_SERVER} ${OWGE_NGINX_PHPMYADMIN_SERVER} ${OWGE_REST_CONTEXT_PATH} ${OWGE_ADMIN_CONTEXT_PATH}' < /etc/nginx/conf.d/proxy_settings.template > /etc/nginx/conf.d/proxy_settings.conf
)
exec "$@"