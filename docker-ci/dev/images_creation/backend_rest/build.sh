#!/bin/bash
source '../../../ci/lib.sh';

docker build -t owge_game_rest \
    --target game-rest \
    -f ../Dockerfile_for_war_files ../../../..