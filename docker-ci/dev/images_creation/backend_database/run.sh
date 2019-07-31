#!/bin/bash
mountdir=${1-/var/owge_data/db};
if [ -z "$INTERACTIVE" ]; then
	mode="-d";
else
	mode="-it";
fi
winpty docker run $mode \
	-p 3306:3306\
	--env MYSQL_ROOT_PASSWORD=1234\
	owge_database;
\ #	-v $mountdir:/var/lib/mysql \
	owge_database;
