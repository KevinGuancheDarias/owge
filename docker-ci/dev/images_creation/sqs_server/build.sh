#!/bin/bash
# Runtime shim (podman|docker): wire the `docker` dispatch before use.
source "$(cd "$(dirname "$0")" && cd ../../.. && pwd)/ci/lib.sh";
docker build -t owge_sqs_server  -f ./Dockerfile .
