#!/bin/bash
# Runtime shim (podman|docker): wire the `docker` dispatch before use.
source "$(cd "$(dirname "$0")" && cd ../../.. && pwd)/ci/lib.sh";
mountdir=${1-/var/owge_data/account_db};
docker run -d --env MYSQL_ROOT_PASSWORD=1234 -p 3306:3306 -v $mountdir:/var/lib/mysql owge_mock_account_database
