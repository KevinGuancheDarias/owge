#!/bin/bash
##
# This script is used to compile and mount the project into docker images
#
# @param $1 string Project version, for example 0.3.0 (should match a OWGE version tag)
# @param $2 string Directory where the static files will be located
# @param $3 string Directory where the dynamic files will be located
# @param $4 int UniverseId number of the universeId
# @env [NO_COMPILE] boolean If should not compile the modules again
# @env [OWGE_NOT_OPTIONAL] If specified, will compile OWGE projects, even if they have been already compiled for specified version
# @todo In the future, create a folder for the specified version, and git clone
# @author Kevin Guanche Darias
##
echo -e "\e[34m\e[42mKevin Guanche Darias :: Modern OWGE DevOps & CI/CD :: Universe launching tool\e[39m\e[49m";
owgeOptional="1";
if [ ! -z "$OWGE_NOT_OPTIONAL" ]; then
	owgeOptional=""
fi

if [ -z "$1" ]; then
	echo "Project version not specified";
	exit 1;
fi

if [ -z "$2" ] ||[ ! -d "$2" ]; then
	echo "Directory for owge-data/static not specified, aborting";
	exit 1;
fi

if [ -z "$3" ] || [ ! -d "$3" ]; then
	echo "Directory for owge-data/dynamic not specified, aborting";
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

envFailureCheck "OWGE_DB_URL" "$OWGE_DB_URL";
envFailureCheck "OWGE_DB_USER" "$OWGE_DB_USER";
envFailureCheck "OWGE_DB_PASS" "$OWGE_DB_PASS";

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
        --volume "$HOME"/.m2:/root/.m2 -w="/usr/src/app/" maven:3-jdk-11-openj9 mvn $@
}

function nodeRun() {
	_targetDirectory="$1";
	if [ ! -d "$_targetDirectory" ]; then
		echo "FATAL, nodeRun failed, no such directory $_targetDirectory, aborting script execution";
		rollback;
	fi
	shift;
	docker run -it --rm --env NG_CLI_ANALYTICS=ci --volume "$_targetDirectory"://home/node -w=/home/node node:10 $@
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
	log debug "invoked: $@";
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
		log info "Compiling $_projectName:$_projectVersion";
		_skipTests="";
		test -n "$SKIP_TESTS" && _skipTests="-DskipTests";
		mavenRun "$1" clean install "$_skipTests" &> /dev/null;
		_compiledFilePath=`find ~/.m2/repository -name "$_projectFile"`;
		if [ -z "$_compiledFilePath" ]; then
			log error "FATAL, compilation, when trying to compile $_project , aborting script execution";
			rollback;
		fi
	fi
	globalMavenFilename="$_projectFile";
	if [ ! -z "$2" ] && [ -d "$2" ]; then
		globalCompiledMavenFile="$2/$_projectFile";
		log "debug" "Copying file $_projectFile  to $2";
		cp -p "$_compiledFilePath" "$2/";
	else
		globalCompiledMavenFile="$_compiledFilePath";
    fi

}

##
# @param string $1 Source directory
# @param string $2 Target directory
##
function compileAngularProject () {
	test -d "$1/dist" && rm -r "$1/dist";
	test -d "$1" && log info "Compiling Angular project in $1 to $2";
	if [ -d "$2" ]; then
		log error "FATAL, target directory for angular project alreadt exists, used $2, aborting...";
		rollback;
	fi
	cp -rp "$1" "$2";
	log debug "Running npm install";
	nodeRun "$2" npm install &> /dev/null;
	log debug "Running npm run build";
	nodeRun "$2" npm run build &> /dev/null;
	log debug "Running npm run buildAdmin";
	nodeRun "$2" npm run buildAdmin &> /dev/null
	if [ ! -d "$2/dist" ]; then
		log error "FATAL, Angular compilation failed, aborting script execution";
		rollback;
	fi

}

# START program itself

no_db_query=`echo "$OWGE_DB_URL" | cut -d '/' -f 1`/
if OWGE_DB_URL=$no_db_query check_mysql_query_fails "SELECT 1"; then
	log error "Invalid mysql server, or invalid credentials"
	exit 1;
fi

mysql_db=`echo "$OWGE_DB_URL" | cut -d '/' -f 2`;
if OWGE_DB_URL=$no_db_query check_mysql_query_fails "SHOW DATABASES" "grep $mysql_db"; then
	log error "No such database $mysql_db"
	exit 1;
fi

business_dir="$PWD"/../../business;
trap ctrl_c INT
if check_mysql_query_fails "SHOW TABLES"; then
	envFailureCheck "OWGE_WORLD_DIR" "$OWGE_WORLD_DIR" "when the db needs intialization this env var is required";
	if ! [ -d  "$OWGE_WORLD_DIR" ]\
		|| ! [ -f "$OWGE_WORLD_DIR/init.sql" ] || ! [ -d "$OWGE_WORLD_DIR/dynamic" ]; then

		log error "Invalid initialization world directory";
		exit 1;
	fi
	log info "Will create the tables and initialize the world";
	if ! mysql_run < "$business_dir/database/02_schema.sql"; then
		log error "Failed to create database structure";
		mysql_run "SHOW TABLES" | while read line; do
			log debug "Deleting table $line";
			mysql_run "SET FOREIGN_KEY_CHECKS=0; DROP TABLE $line";
		done
		exit 1;
	fi
	log debug "Created the tables";
	if ! mysql_run < "$business_dir/database/04_insert_data.sql"; then
		log error "Failed to insert required content";
		exit 1;
	fi
	log debug "inserted the base content";

	if ! mysql_run < "$OWGE_WORLD_DIR/init.sql"; then
		log error "Insert world content failed =/";
		exit 1;
	fi
	log debug "inserted the world content";
	if ! cp -rp "$OWGE_WORLD_DIR/dynamic/"* $3/; then
		log error "Couldn't copy files from to the dynamic folder";
		exit 1;
	fi
	log debug "copied the content";
fi
trap INT;

log debug "Testing if kevinsuite java is present";
if [ ! -d "$kevinsuiteCommonBackend" ] || [ ! -d "$kevinsuiteRestBackend" ] ; then
	echo "Fatal: Missing kevinsuite lib, download it please, looking into $kevinsuiteRoot";
	rollback;
fi

if [ -z "$owgeOptional" ]; then
	log info "Remember, ideally, when using this script, all maven builds should have the OPTIONAL flag, study the correct versioning of the maven artifacts!";
fi
targetRoot="/tmp/shit";
if [ -z "$NO_COMPILE" ]; then
	test -d "$targetRoot" && rm -r "$targetRoot";
	mkdir "$targetRoot";
	OPTIONAL=1 SKIP_TESTS=1 compileMavenProject "$kevinsuiteCommonBackend";
	OPTIONAL=1 SKIP_TESTS=1 compileMavenProject "$kevinsuiteRestBackend";
	OPTIONAL="$owgeOptional" SKIP_TESTS=1 compileMavenProject "$business_dir" "$targetRoot";
	export OWGE_CI_INSTALL_ADMIN_FILE="$globalCompiledMavenFile";
	OPTIONAL="$owgeOptional" SKIP_TESTS=1 compileMavenProject "$PWD"/../../game-rest "$targetRoot";
	export OWGE_CI_INSTALL_GAME_REST_FILE="$globalCompiledMavenFile";
	export OWGE_REST_WAR_FILENAME="$globalMavenFilename";
	OPTIONAL="$owgeOptional" compileAngularProject "$PWD/../../game-frontend" "$targetRoot/frontend";
else
	echo "Currently NO_COMPILE is buggy, and is work in progress :(  ...... aborting :/";
	rollback;
fi
export OWGE_CI_VERSION="$1";
# START Dockerzation things
launcherPath="$PWD";
log debug "Invoke main_reverse_proxy/install.sh";
cd main_reverse_proxy;
chmod +x install.sh;
./install.sh "$targetRoot/frontend";
_err=$?;
cd "$launcherPath";
if [ "$_err" == "0" ]; then
	log info "Executing jenkins install";
	chmod +x jenkins_install.sh
	OWGE_UNIVERSE_ID="$4" ./jenkins_install.sh "$2" "$3";
else 
	log error "Failed to install the frontend to the nginx target directory";
fi
log debug "git checkingout again the previously branch: $oldBranch";
git checkout "$oldBranch";
