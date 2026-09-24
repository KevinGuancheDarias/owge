#!/bin/bash
# Runtime shim (podman|docker): wire the `docker` dispatch before use.
source "$(cd "$(dirname "$0")" && cd ../../.. && pwd)/ci/lib.sh";
mountdir=${1-/var/owge_data/db};
if [ -z "$INTERACTIVE" ]; then
	mode="-d";
else
	mode="-it";
fi
docker run $mode \
	-p 3306:3306\
	--env MYSQL_ROOT_PASSWORD=1234\
	owge_database;
\ #	-v $mountdir:/var/lib/mysql \
	owge_database;
