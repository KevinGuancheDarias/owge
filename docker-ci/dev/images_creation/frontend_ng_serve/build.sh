#!/bin/bash
# Runtime shim (podman|docker): wire the `docker` dispatch before use.
source "$(cd "$(dirname "$0")" && cd ../../.. && pwd)/ci/lib.sh";
docker build -t owge_ng_serve  -f ./Dockerfile ../../../../game-frontend
