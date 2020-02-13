#!/bin/bash
source '../../../ci/lib.sh';

envFailureCheck "DATABASE_SERVERS" "$DATABASE_SERVERS";
envFailureCheck "PHPMYADMIN_PORT" "$PHPMYADMIN_PORT";

winpty docker run -it --rm -p "$PHPMYADMIN_PORT:80" --env "DATABASE_SERVERS=$DATABASE_SERVERS" owge_phpmyadmin $@