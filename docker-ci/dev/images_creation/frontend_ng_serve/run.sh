#!/bin/bash
source '../../../ci/lib.sh';

#gameFrontend=${1-${BASH_SOURCE%/*}/../../../../game-frontend}

#gameFrontend="/`findAbsoluteDir "$gameFrontend"`";

# -v "$gameFrontend:/app"
docker run -i --rm  -p 4200:4200 owge_ng_serve $@