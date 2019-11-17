#!/bin/bash
source '../../../ci/lib.sh';

docker run -i --rm  -p 4200:4200 owge_ng_serve $@