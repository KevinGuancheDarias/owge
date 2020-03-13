#!/bin/bash
source '../../../ci/lib.sh';
envFailureCheck "OWGE_DB_URL" "$OWGE_DB_URL"
envFailureCheck "OWGE_DB_USER" "$OWGE_DB_USER"
envFailureCheck "OWGE_DB_PASS" "$OWGE_DB_PASS"

defaultStaticDirectory="/var/owge_data/static";
defaultDynamicDirectory="/var/owge_data/dynamic";

staticDirectory="${1:-/var/owge_data/static}";
dynamicDirectory="${2:-/var/owge_data/static}";

dockerImage="owge_game_rest";
checkDockerImageExists "$dockerImage"
docker run -t -p 8080:8080 -v "$staticDirectory:/var/owge_data/static" -v "$dynamicDirectory:/var/owge_data/dynamic"\
    --env OWGE_DB_URL="$OWGE_DB_URL" --env OWGE_DB_USER="$OWGE_DB_USER" --env OWGE_DB_PASS="$OWGE_DB_PASS"\
    --env MYSQL_HOST="192.168.99.1" --env MYSQL_USER="$OWGE_DB_USER" --env MYSQL_PASSWORD="$OWGE_DB_PASS" --env MYSQL_DB="sgalactica_java"\
     "$dockerImage"
