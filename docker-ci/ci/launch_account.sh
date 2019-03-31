#!/bin/bash

##
# After the execution of compileMavenProject() contains where the compile file is located
##
globalCompiledMavenFile=

##
# After the execution of compileMavenProject() will contain the filename of the jar or the war file
##
globalMavenFilename=

if [ -z "$1" ]; then
	echo "Version must be specified, example version: 0.3.0";
	exit 1;
fi
owgeVersion="$1";
source ./lib.sh;
OPTIONAL=1 SKIP_TESTS=1 compileMavenProject "$kevinsuiteCommonBackend";
OPTIONAL=1 SKIP_TESTS=1 compileMavenProject "$kevinsuiteRestBackend";
compileMavenProject "$PWD"/../../business "$targetRoot";
compileMavenProject "$PWD"/../../account "account/target/";
containerName="owge-ci-docker-account-$owgeVersion";
imageName="owge_account:$owgeVersion";
cd account;
test ! -d target && mkdir target;
cp "$globalCompiledMavenProject" target/
OWGE_ACCOUNT_WAR_FILENAME="$globalMavenFilename" ./build.sh "$imageName";
cd -;
docker rm -f "$containerName";
sleep 2;
port=`getPort $owgeVersion`;
docker run  -d --name  "$containerName" --restart always -p $port:8081 "$imageName";
