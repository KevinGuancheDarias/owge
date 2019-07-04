source '../../../ci/lib.sh';
envFailureCheck "OWGE_DATABASE_URL" "$OWGE_DATABASE_URL"
envFailureCheck "OWGE_DATABASE_USER" "$OWGE_DATABASE_USER"
envFailureCheck "OWGE_DATABASE_PASSWORD" "$OWGE_DATABASE_PASSWORD"

defaultStaticDirectory="/var/owge_data/static";
defaultDynamicDirectory="/var/owge_data/dynamic";

staticDirectory="${1:-/var/owge_data/static}";
dynamicDirectory="${2:-/var/owge_data/static}";

dockerImage="owge_game_rest";
checkDockerImageExists "$dockerImage"
docker run -d -p 8080:8080 -v "$staticDirectory:/var/owge_data/static" -v "$dynamicDirectory:/var/owge_data/dynamic" --env CATALINA_OPTS="-Dowge.databaseUrl=${OWGE_DATABASE_URL} -Dowge.databaseUser=${OWGE_DATABASE_USER} -Dowge.databasePassword=${OWGE_DATABASE_PASSWORD}" "$dockerImage"