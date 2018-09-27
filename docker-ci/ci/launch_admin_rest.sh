#!/bin/bash
##
# This script is used to compile and mount the project into docker images
#
# @param $1 string Project version, for example 0.3.0 (should match a SGT version tag)
# @param $2 string Directory where the static files will be located
# @param $3 string Directory where the dynamic files will be located
# @param $4 int UniverseId number of the universeId
# @env [NO_COMPILE] boolean If should not compile the modules again
# @env [SGT_NOT_OPTIONAL] If specified, will compile SGT projects, even if they have been already compiled for specified version
# @todo In the future, create a folder for the specified version, and git clone
# @author Kevin Guanche Darias
##
echo -e "\e[34m\e[42mKevin Guanche Darias :: Modern SGT DevOps & CI/CD :: Universe launching tool\e[39m\e[49m";
sgtOptional="1";
if [ ! -z "$SGT_NOT_OPTIONAL" ]; then
	sgtOptional=""
fi

if [ -z "$1" ]; then
	echo "Project version not specified";
	exit 1;
fi

if [ -z "$2" ] ||[ ! -d "$2" ]; then
	echo "Directory for sgt-data/static not specified, aborting";
	exit 1;
fi

if [ -z "$3" ] || [ ! -d "$3" ]; then
	echo "Directory for sgt-data/dynamic not specified, aborting";
	exit 1;
fi

if [ -z "$4" ]; then
        echo "Universe id was NOT specified";
        exit 1;
fi
if ! [ "$4" -eq "$4" ]; then
	echo "FATAL: Universe id MUST be a number";
	exit 1;
fi
. ./lib.sh;
if ! gitVersionExists "$1"; then
	exit 1;
fi
echo "git checkingout tag v$1";
oldBranch=`gitGetCurrentBranch`;
oldDetachedHeadValue=`git config advice.detachedHead`;
git config advice.detachedHead false;
git checkout "v$1";
git config advice.detachedHead "$oldDetachedHeadValue";
##
# After the execution of compileMavenProject() contains where the compile file is located
##
globalCompiledMavenFile=

##
# After the execution of compileMavenProject() will contain the filename of the jar or the war file
##
globalMavenFilename=

function mavenRun () {
	_targetDirectory="$1";
	shift;
	 docker run -it --rm --volume "$_targetDirectory"://usr/src/app \
        --volume "$HOME"/.m2:/root/.m2 -w="/usr/src/app/" maven:3-jdk-8-alpine mvn $@
}

function nodeRun() {
	_targetDirectory="$1";
	if [ ! -d "$_targetDirectory" ]; then
		echo "FATAL, nodeRun failed, no such directory $_targetDirectory, aborting script execution";
		rollback;
	fi
	shift;
	docker run -it --rm --volume "$_targetDirectory"://home/node -w=/home/node node:8-alpine $@
}

##
# Compiles the specified maven project
#
# @param $1 string Target maven project directory
# @param [$2] string directory to copy the generated war
# @env [OPTIONAL] boolean If specified, will only compile if compile file doesn't exists
#
# @author Kevin Guanche Darias
##
function compileMavenProject () {
	_project=`mavenRun "$1" -q -Dexec.executable="echo" -Dexec.args='${project.artifactId}:${project.version}:${project.packaging}' --non-recursive exec:exec | head -n 1`;
	_project=`echo "$_project" | tr -cd '[[:alnum:]].:_-'`;
	_projectName=`echo "$_project" | cut -d ':' -f 1`;
	_projectVersion=`echo "$_project" | cut -d ':' -f 2`;
	_projectPackaging=`echo "$_project" | cut -d ':' -f 3`;
	_projectFile="$_projectName-$_projectVersion.$_projectPackaging"
	_doCompile="1";
	if [ ! -z "$OPTIONAL" ]; then
		_compiledFilePath=`find ~/.m2/repository -name "$_projectFile"`;
		if [ ! -z "$_compiledFilePath" ]; then
			_doCompile="0";
		fi
	fi
	if [ $_doCompile -eq 1 ]; then
		echo "Compiling $_projectName:$_projectVersion";
		mavenRun "$1" clean install &> /dev/null;
		_compiledFilePath=`find ~/.m2/repository -name "$_projectFile"`;
		if [ -z "$_compiledFilePath" ]; then
			echo "FATAL, compilation, when trying to compile $_project , aborting script execution";
			rollback;
		fi
	fi
	globalMavenFilename="$_projectFile";
	if [ ! -z "$2" ] && [ -d "$2" ]; then
		globalCompiledMavenFile="$2/$_projectFile";
		echo "Copying file $_projectFile  to $2";
		cp -p "$_compiledFilePath" "$2/";
	else
		globalCompiledMavenFile="$_compiledFilePath";
        fi

}

function compileAngularProject () {
	test -d "$1/dist" && rm -r "$1/dist";
	test -d "$1" && echo "Compiling Angular project in $1 to $2";
	if [ -d "$2" ]; then
		echo "FATAL, target directory for angular project alreadt exists, used $2, aborting...";
		rollback;
	fi
	cp -rp "$1" "$2";
	nodeRun "$2" npm install &> /dev/null;
	nodeRun "$2" npm run build -- -prod --build-optimizer &> /dev/null;
	if [ ! -d "$2/dist" ]; then
		echo "FATAL, Angular compilation failed, aborting script execution";
		rollback;
	fi

}

if [ ! -d "$kevinsuiteCommonBackend" ] || [ ! -d "$kevinsuiteRestBackend" ] ; then
	echo "Fatal: Missing kevinsuite lib, download it please, looking into $kevinsuiteRoot";
	rollback;
fi

if [ -z "$sgtOptional" ]; then
	echo "Remember, ideally, when using this script, all maven builds should have the OPTIONAL flag, study the correct versioning of the maven artifacts!";
fi
targetRoot="/tmp/shit";
if [ -z "$NO_COMPILE" ]; then
	test -d "$targetRoot" && rm -r "$targetRoot";
	mkdir "$targetRoot";
	OPTIONAL=1 SKIP_TESTS=1 compileMavenProject "$kevinsuiteCommonBackend";
	OPTIONAL=1 SKIP_TESTS=1 compileMavenProject "$kevinsuiteRestBackend";
	OPTIONAL="$sgtOptional" compileMavenProject "$PWD"/../../business "$targetRoot";
	OPTIONAL="$sgtOptional" compileMavenProject "$PWD"/../../account "$targetRoot";
	OPTIONAL="$sgtOptional" compileMavenProject "$PWD"/../../admin "$targetRoot";
	export SGT_CI_INSTALL_ADMIN_FILE="$globalCompiledMavenFile";
	export SGT_ADMIN_WAR_FILENAME="$globalMavenFilename";
	OPTIONAL="$sgtOptional" compileMavenProject "$PWD"/../../game-rest "$targetRoot";
	export SGT_CI_INSTALL_GAME_REST_FILE="$globalCompiledMavenFile";
	export SGT_REST_WAR_FILENAME="$globalMavenFilename";
	OPTIONAL="$sgtOptional" compileAngularProject "$PWD/../../game-frontend" "$targetRoot/frontend";
	export SGT_CI_INSTALL_FRONTEND_DIR="$targetRoot/frontend/dist";
else
	echo "Currently NO_COMPILE is buggy, and is work in progress :(  ...... aborting :/";
	rollback;
fi
export SGT_CI_VERSION="$1";
# START Dockerzation things
launcherPath="$PWD";
cd main_reverse_proxy;
chmod +x install.sh;
./install.sh "$targetRoot/frontend"
cd "$launcherPath";
echo "Executing jenkins install";
chmod +x jenkins_install.sh
SGT_UNIVERSE_ID="$4" ./jenkins_install.sh "$2" "$3";
echo "git checkingout again the previously branch: $oldBranch";
git checkout "$oldBranch";
