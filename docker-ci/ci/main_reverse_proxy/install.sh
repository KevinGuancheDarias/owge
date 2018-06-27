#!/bin/bash
# @deprecated no longer used, as project is compiled by Jenkins
# @var $1 - Absoltute path to  angular 2 project
# @var $2 - if specified, will mean should do docker install (Not useful when using docker compose)

dockerImageName="kevinguanchedarias/sgtjava_main_proxy";

if [ -z "$1" ]; then
        echo "No se ha especificado el directorio donde se encuentra el cdigo del frontend";
        exit 1;
fi

frontend="$1";
if [ ! -f "$frontend/.angular-cli.json" ]; then
	echo "No parece un proyecto válido debe haber angular cli";
	exit 1;
fi

launcherPath=$PWD;
compiledDestination="$frontend/dist";

if [ ! -d "$compiledDestination" ]; then
	if ! ng -v ; then
		echo "No se encontró Angular cli en el sistema operativo, lo que significa que no está instalado";
		exit 1;
	fi
	echo "Compilando proyecto angular";
	cd $frontend;
	test -d $compiledDestination && rm -r $compiledDestination;
	ng build  --prod
	if [ ! -d "$compiledDestination" ];then
		echo "Parece que la compilación no salió bien, ya que nose creo ./dist";
		exit 1;
	fi
else
	echo "Not recompiling frontend, as ithas already been compiled by other script";
fi
echo "Copying compiled files to target, so is available to docker build";
sourceDestination="$launcherPath/target";
cd $launcherPath;
if [  ! -d "$launcherPath/config" ]; then
	echo "No existe el directorio ./config , lo que significa que no se está lanzando este comando en el directorio correcto";
	exit 1;
fi

test -d $sourceDestination && rm -r $sourceDestination;

cp -rp "$compiledDestination" "$sourceDestination";

# START docker fun if wanteed to
if [ ! -z "$2" ]; then
	echo "se ha solicitado compilación de Docker!";
	docker build -t $dockerImageName;
fi
