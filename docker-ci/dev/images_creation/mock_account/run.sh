#!/bin/bash
source '../../../ci/lib.sh';

envFailureCheck "MYSQL_HOST" "$MYSQL_HOST"
envFailureCheck "MYSQL_USER" "$MYSQL_USER"
envFailureCheck "MYSQL_PASSWORD" "$MYSQL_PASSWORD"
envFailureCheck "MYSQL_DB" "$MYSQL_DB"

if [ $# -gt 0 ]; then
    mode='-it';
    command="$@";
else
    mode='-d';
    command="";
fi
`winPtyPrefix`  docker run $mode -p 8087:8080 --rm \
    --env OWGE_INTERACTIVE="$OWGE_INTERACTIVE" \
    --env MYSQL_HOST="$MYSQL_HOST" \
    --env MYSQL_USER="$MYSQL_USER" \
    --env MYSQL_PASSWORD="$MYSQL_PASSWORD" \
    --env MYSQL_DB="$MYSQL_DB" \
    owge_mock_account $command;
