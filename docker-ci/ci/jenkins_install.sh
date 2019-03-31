#!/bin/bash

##
# This script will install the required information into target dirs before calling  docker-compose<br />
#
# @author Kevin Guanche Darias
##
source ./lib.sh

##
# Will check if given env-var is defined
#
# @param $1 env var number
# @param $2 current value of the env var
# @author Kevin Guanche Darias
##
function envFailureCheck(){
	if [ -z "$2" ]; then
		echo "Environment is not properly configured, missing $1";
		exit 1;
	fi
}

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
		echo "required element $1 = $3 doesn't exists";
		exit 1;
	fi
}

# START env aliasing
adminWarFile="$OWGE_CI_INSTALL_ADMIN_FILE";
gameRestWarFile="$OWGE_CI_INSTALL_GAME_REST_FILE";
gameFrontendNgDir="$OWGE_CI_INSTALL_FRONTEND_DIR";
owgeVersion="$OWGE_CI_VERSION";
gameRestFilename="$OWGE_REST_WAR_FILENAME";
gameAdminFilename="$OWGE_ADMIN_WAR_FILENAME";
universeId="$OWGE_UNIVERSE_ID";
# END env aliasing

# START check env
envFailureCheck 1 "$adminWarFile";
envFailureCheck 2 "$gameRestWarFile";
envFailureCheck 3 "$gameFrontendNgDir";
envFailureCheck 4 "$owgeVersion";
envFailureCheck 5 "$gameRestFilename";
envFailureCheck 6 "$gameAdminFilename";
envFailureCheck 7 "$universeId";


checkExists 1 f "$adminWarFile";
checkExists 2 f "$gameRestWarFile";
checkExists 3 d "$gameFrontendNgDir";
# END check env

# START ask or set parameters
if [ -z "$1" ]; then
	while [ -z "$staticImgDir" ]; do
		echo -n "Please insert static images server directory: "; read staticImgDir;
	done
else
	staticImgDir="$1";
fi

if [ ! -d "$staticImgDir" ]; then
	echo "Directory $staticImgDir doesn't exists!";
	exit 1;
fi
# END check param 1
if [ -z "$2" ]; then
	while [ -z "$dynamicImgDir" ]; do
		echo -n "Please insert dynamic images server directory: "; read dynamicImgDir;
	done
else
	dynamicImgDir="$2";
fi

if [ ! -d "$dynamicImgDir" ]; then
	echo "Directory $dynamicImgDir doesn't exists";
	exit 1;
fi
COMPOSE_PROJECT_NAME="dc$universeId" OWGE_PORT="56000" STATIC_IMAGES_DIR="$staticImgDir" DYNAMIC_IMAGES_DIR="$dynamicImgDir" OWGE_CI_VERSION="$owgeVersion" OWGE_REST_WAR_FILENAME="$gameRestFilename" OWGE_ADMIN_WAR_FILENAME="$gameAdminFilename" docker-compose down;
sleep 2;
if ! owgePort=`getPortByUniverseId $universeId`; then
        echo "Error getting port: $owgePort";
        exit 1;
fi

# END ask or set parameters

# START program itself
localPath=`dirname $0`;
tomcatContainer="$localPath/admin_panel_and_rest_game";
gameFrontendContainer="$localPath/main_reverse_proxy";

test ! -d "$tomcatContainer/target" && mkdir "$tomcatContainer/target";
cp "$adminWarFile" "$tomcatContainer/target/";
cp "$gameRestWarFile" "$tomcatContainer/target/";
test -d "$gameFrontendContainer/target" && rm -r "$gameFrontendContainer/target";
cp -rp "$gameFrontendNgDir" "$gameFrontendContainer/target";

cd $localPath;
COMPOSE_PROJECT_NAME="dc$universeId" OWGE_PORT="$owgePort" STATIC_IMAGES_DIR="$staticImgDir" DYNAMIC_IMAGES_DIR="$dynamicImgDir" OWGE_CI_VERSION="$owgeVersion" OWGE_REST_WAR_FILENAME="$gameRestFilename" OWGE_ADMIN_WAR_FILENAME="$gameAdminFilename" OWGE_UNIVERSE_ID="$universeId" docker-compose up -d  --build | grep -E "^(Creating|Successfully)";
cd -;
# END program itself
