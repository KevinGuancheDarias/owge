#!/bin/bash
mountdir=${1-/var/owge_data/account_db};
winpty docker run -d --env MYSQL_ROOT_PASSWORD=1234 -p 3306:3306 -v $mountdir:/var/lib/mysql owge_mock_account_database