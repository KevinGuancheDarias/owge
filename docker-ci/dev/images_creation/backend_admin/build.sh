#!/bin/bash
source '../../../ci/lib.sh';
envFailureCheck "OWGE_DATABASE_URL" "$OWGE_DATABASE_URL"
envFailureCheck "OWGE_DATABASE_USER" "$OWGE_DATABASE_USER"
envFailureCheck "OWGE_DATABASE_PASSWORD" "$OWGE_DATABASE_PASSWORD"
docker build -t owge_game_admin \
    --target game-admin \   
    -f ../Dockerfile_for_war_files ../../../..