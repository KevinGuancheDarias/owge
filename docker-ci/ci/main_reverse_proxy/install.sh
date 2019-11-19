#!/bin/bash
# @deprecated no longer used, as project is compiled by Jenkins
# @var $1 - Absoltute path to  angular 2 project
# @var $2 - if specified, will mean should do docker install (Not useful when using docker compose)

dockerImageName="kevinguanchedarias/owgejava_main_proxy";

if [ -z "$1" ]; then
        echo "No se ha especificado el directorio donde se encuentra el cdigo del frontend";
        exit 1;
fi

frontend="$1";
if [ ! -f "$frontend/angular.json" ]; then
	echo "Not a valid Angular CLI project";
	exit 1;
fi

launcherPath=$PWD;
compiledDestination="$frontend/dist";

if [ ! -d "$compiledDestination" ]; then
	echo "Compiled destination not found, should not used install.sh outside of launch_admin_rest.sh for now";
	exit 1;
fi
if [ ! -d "$compiledDestination/game-frontend" ] || [ ! -d "$compiledDestination/game-admin" ]; then
	echo "Compiled destination exists, but doesn't have game-frontend, or/nor game-admin";
	exit 1;
fi
echo "Copying compiled files to target, so is available to docker build";
sourceDestination="$launcherPath/target";
cd $launcherPath;
if [  ! -d "$launcherPath/config" ]; then
	echo "No existe el directorio ./config , lo que significa que no se está lanzando este comando en el directorio correcto";
	exit 1;
fi

test -d $sourceDestination && rm -r $sourceDestination;

cp -rp "$compiledDestination/game-frontend" "$sourceDestination";
cp -rp "$compiledDestination/game-admin" "$sourceDestination/admin";

# START docker fun if wanteed to
if [ ! -z "$2" ]; then
	echo "se ha solicitado compilación de Docker!";
	docker build -t $dockerImageName;
fi
