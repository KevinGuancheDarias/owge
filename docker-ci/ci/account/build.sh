#!/bin/bash
if [ -z "$SGT_ACCOUNT_WAR_FILENAME" ]; then
	echo "War filename was not passed";
	exit 1;
fi

if [ -z "$1" ]; then
	echo "No docker image name was passed";
	exit 1;
fi
docker build -t "$1" --build-arg warHolderDirectory=target --build-arg SGT_ACCOUNT_WAR_FILENAME .
