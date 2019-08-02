#!/bin/bash
#!/bin/bash
source '../../../ci/lib.sh';

envFailureCheck '$1' "$1" "The dynamic images volume path"
envFailureCheck "OWGE_BACKEND_SERVER" "$OWGE_BACKEND_SERVER" The IP or hostname of the backend server and the port
envFailureCheck "OWGE_FRONTEND_SERVER" "$OWGE_FRONTEND_SERVER" The IP or hostname of the frontend server and the port
envFailureCheck "OWGE_ACCOUNT_SERVER" "$OWGE_ACCOUNT_SERVER" The IP or hostname of the frontend server and the port

envInfoCheck "OWGE_NGINX_PHPMYADMIN_SERVER" "$OWGE_NGINX_PHPMYADMIN_SERVER" "Specify a PHPMyAdmin to use when browsing /phpmyadmin"

staticPath=${2-${BASH_SOURCE%/*}/../../../../static}

checkDirectoryExists "$staticPath";

dynamicPath="/`findAbsoluteDir "$1"`";
staticPath="/`findAbsoluteDir "$staticPath"`";
(
    docker run -d  -p 80:80 --rm \
        -v "$dynamicPath:/var/owge_data/dynamic"\
        -v "$staticPath:/var/owge_data/static"\
        --env "OWGE_BACKEND_SERVER=$OWGE_BACKEND_SERVER"\
        --env "OWGE_FRONTEND_SERVER=$OWGE_FRONTEND_SERVER"\
        --env "OWGE_ACCOUNT_SERVER=$OWGE_ACCOUNT_SERVER"\
        --env "OWGE_NGINX_PHPMYADMIN_SERVER=${OWGE_NGINX_PHPMYADMIN_SERVER-no_phpmyadmin_server_specified}"
        owge_nginx
)
