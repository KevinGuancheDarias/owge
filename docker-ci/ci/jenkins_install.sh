#!/bin/bash

##
# This script will install the required information into target dirs before calling  docker-compose<br />
#
# @author Kevin Guanche Darias
##
source ./lib.sh

##
# Will check that the file (or directory) exists
#
# @param $1 check number
# @param $2 type , example $1='d' for directory
# @param $3 target path
# @author Kevin Guanche Darias
##
function checkExists(){
	if [ ! -$2 "$3" ]; then
		log error "required element $1 = $3 doesn't exists";
		exit 1;
	fi
}

# START env aliasing
gameRestWarFile="$OWGE_CI_INSTALL_GAME_REST_FILE";
gameFrontendNgDir="$OWGE_CI_INSTALL_FRONTEND_DIR";
owgeVersion="$OWGE_CI_VERSION";
gameRestFilename="$OWGE_REST_WAR_FILENAME";
universeId="$OWGE_UNIVERSE_ID";
# END env aliasing

# START check env
envFailureCheck 2 "$gameRestWarFile";
envFailureCheck 4 "$owgeVersion";
envFailureCheck 5 "$gameRestFilename";
envFailureCheck 6 "$universeId";
envFailureCheck 7 "$OWGE_DB_URL";
envFailureCheck 8 "$OWGE_DB_USER"
envFailureCheck 9 "$OWGE_DB_PASS"

checkExists 2 f "$gameRestWarFile";
# END check env

# START ask or set parameters
if [ -z "$1" ]; then
	while [ -z "$staticImgDir" ]; do
		echo -n "Please insert static images server directory: "; read staticImgDir;
	done
else
	staticImgDir="$1";
fi

checkDirectoryExists "$staticImgDir";

# END check param 1
if [ -z "$2" ]; then
	while [ -z "$dynamicImgDir" ]; do
		echo -n "Please insert dynamic images server directory: "; read dynamicImgDir;
	done
else
	dynamicImgDir="$2";
fi

checkDirectoryExists "$dynamicImgDir";

COMPOSE_PROJECT_NAME="dc$universeId" OWGE_PORT="56000" STATIC_IMAGES_DIR="$staticImgDir" DYNAMIC_IMAGES_DIR="$dynamicImgDir" OWGE_CI_VERSION="$owgeVersion" OWGE_REST_WAR_FILENAME="$gameRestFilename" docker-compose down;
sleep 2;
log debug "Getting target port";
if ! owgePort=`getPortByUniverseId $universeId`; then
        echo "Error getting port: $owgePort";
        exit 1;
fi

# END ask or set parameters

# START program itself
localPath=`dirname $0`;
tomcatContainer="$localPath/admin_panel_and_rest_game";
gameFrontendContainer="$localPath/main_reverse_proxy";
log info "Jenkins install script started";
log debug "copying content to rest container working directory";
test ! -d "$tomcatContainer/target" && mkdir "$tomcatContainer/target";
cp "$gameRestWarFile" "$tomcatContainer/target/";
cp -rp "$localPath/../dev/images_creation/scripts" "$tomcatContainer/target/";
if ! [ $? -eq 0 ]; then
	echo -e "\e[31mFailed to copy scripts to target $tomcatContainer/target/\e[39m";
	exit 1;
fi

if [ -n "$gameFrontendNgDir" ]; then
	log debug "Copying frontend files";
	test -d "$gameFrontendContainer/target" && rm -r "$gameFrontendContainer/target";
	cp -rp "$gameFrontendNgDir" "$gameFrontendContainer/target";
fi

cd $localPath;
log debug "running docker-compose";
COMPOSE_PROJECT_NAME="dc$universeId" OWGE_PORT="$owgePort" STATIC_IMAGES_DIR="$staticImgDir" DYNAMIC_IMAGES_DIR="$dynamicImgDir" OWGE_CI_VERSION="$owgeVersion" OWGE_REST_WAR_FILENAME="$gameRestFilename" OWGE_UNIVERSE_ID="$universeId" docker-compose up --build -d  |  grep -E "^(Creating|Successfully)";
cd -;
# END program itself
