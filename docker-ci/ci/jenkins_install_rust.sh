#!/bin/bash

##
# Rust-backend variant of jenkins_install.sh: installs the compiled `owge-rest`
# binary + the compiled Angular frontend into the docker build contexts and runs
# docker-compose.rust.yml. Mirrors jenkins_install.sh but swaps the Java war for
# the Rust binary and the Java stack for the Rust stack.
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
##
function checkExists(){
	if [ ! -$2 "$3" ]; then
		log error "required element $1 = $3 doesn't exists";
		exit 1;
	fi
}

# START env aliasing
gameRestBinFile="$OWGE_CI_INSTALL_GAME_REST_BIN";
gameFrontendNgDir="$OWGE_CI_INSTALL_FRONTEND_DIR";
owgeVersion="$OWGE_CI_VERSION";
universeId="$OWGE_UNIVERSE_ID";
# END env aliasing

# START check env
envFailureCheck 2 "$gameRestBinFile";
envFailureCheck 4 "$owgeVersion";
envFailureCheck 6 "$universeId";
envFailureCheck 7 "$OWGE_DB_URL";
envFailureCheck 8 "$OWGE_DB_USER"
envFailureCheck 9 "$OWGE_DB_PASS"

checkExists 2 f "$gameRestBinFile";
# END check env

# The Rust app takes a single JDBC-style URL. OWGE_DB_URL is "host:port/dbname".
# NOTE: a password with URL-reserved characters (@ : / etc.) would need encoding.
export OWGE_DB_JDBC_URL="mysql://${OWGE_DB_USER}:${OWGE_DB_PASS}@${OWGE_DB_URL}";

# START ask or set parameters
if [ -z "$1" ]; then
	while [ -z "$staticImgDir" ]; do
		echo -n "Please insert static images server directory: "; read staticImgDir;
	done
else
	staticImgDir="$1";
fi

checkDirectoryExists "$staticImgDir";

if [ -z "$2" ]; then
	while [ -z "$dynamicImgDir" ]; do
		echo -n "Please insert dynamic images server directory: "; read dynamicImgDir;
	done
else
	dynamicImgDir="$2";
fi

checkDirectoryExists "$dynamicImgDir";

COMPOSE_PROJECT_NAME="dc$universeId" OWGE_PORT="56000" STATIC_IMAGES_DIR="$staticImgDir" DYNAMIC_IMAGES_DIR="$dynamicImgDir" OWGE_CI_VERSION="$owgeVersion" docker-compose -f docker-compose.rust.yml down;
sleep 2;
log debug "Getting target port";
if ! owgePort=`getPortByUniverseId $universeId`; then
        echo "Error getting port: $owgePort";
        exit 1;
fi
# END ask or set parameters

# START program itself
localPath=`dirname $0`;
restContainer="$localPath/rust_rest_game";
gameFrontendContainer="$localPath/main_reverse_proxy";
log info "Jenkins (rust) install script started";
log debug "copying the rust binary to the rest container build context";
test ! -d "$restContainer/target" && mkdir "$restContainer/target";
cp "$gameRestBinFile" "$restContainer/target/owge-rest";
if ! [ $? -eq 0 ]; then
	echo -e "\e[31mFailed to copy the rust binary to $restContainer/target/\e[39m";
	exit 1;
fi

if [ -n "$gameFrontendNgDir" ]; then
	log debug "Copying frontend files";
	test -d "$gameFrontendContainer/target" && rm -r "$gameFrontendContainer/target";
	cp -rp "$gameFrontendNgDir" "$gameFrontendContainer/target";
fi

cd $localPath;
log debug "running docker-compose (rust)";
COMPOSE_PROJECT_NAME="dc$universeId" OWGE_PORT="$owgePort" STATIC_IMAGES_DIR="$staticImgDir" DYNAMIC_IMAGES_DIR="$dynamicImgDir" OWGE_CI_VERSION="$owgeVersion" OWGE_UNIVERSE_ID="$universeId" OWGE_DB_JDBC_URL="$OWGE_DB_JDBC_URL" docker-compose -f docker-compose.rust.yml up --build -d  |  grep -E "^(Creating|Successfully)";
cd -;
# END program itself
