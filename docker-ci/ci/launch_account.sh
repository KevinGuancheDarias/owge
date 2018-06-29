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
sgtVersion="$1";
source ./lib.sh;
compileMavenProject "$PWD"/../../account "account/target/";
containerName="sgt-ci-docker-account-$sgtVersion";
imageName="sgt_account:$sgtVersion";
cd account;
test ! -d target && mkdir target;
cp "$globalCompiledMavenProject" target/
SGT_ACCOUNT_WAR_FILENAME="$globalMavenFilename" ./build.sh "$imageName";
cd -;
docker rm -f "$containerName";
sleep 2;
port=`getPort $sgtVersion`;
docker run  -it --name  "$containerName" --restart always -p $port:8081 "$imageName";
