kevinsuiteRoot="/public/kevinsuite-java";
kevinsuiteCommonBackend="$kevinsuiteRoot/common-backend";
kevinsuiteRestBackend="$kevinsuiteRoot/backend-rest-commons";

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
                exit 1;
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
# @env [SKIP_TESTS] If should skip test, if not specified, will run all the tests (as we should always do)
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
	_skipTests=""
	if [ ! -z "$SKIP_TESTS" ]; then
		_skipTests="-DskipTests";
	fi
        if [ $_doCompile -eq 1 ]; then
                echo "Compiling $_projectName:$_projectVersion";
                mavenRun "$1" clean install "$_skipTests"  &> /dev/null;
                _compiledFilePath=`find ~/.m2/repository -name "$_projectFile"`;
                if [ -z "$_compiledFilePath" ]; then
                        echo "FATAL, compilation, when trying to compile $_project , aborting script execution";
                        exit 1;
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

##
# Returns the HTTP port for the specified OWGE version
# Notice: It ensures only a free port is returned
#
# @deprecated It's better to determine the port by giving universeId, use getPortByUniverseId instead
# @param $1 string Version, should look like x.x.x
# @author Kevin Guanche Darias
# @returns stdout<int> Port number
##
function getPort () {
	echo -e "\[33mWarning: getPort function is deprecated\e[39m";
	if ! echo "$1" | grep -E "^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]$" &> /dev/null; then
		echo "FATAL, malformed version passed to getPort(), exit()ing script";
		return 1;
	fi
	major=`echo $1 | cut -d . -f 1`;
	minor=`echo $1 | cut -d . -f 2`;
	path=`echo $1 | cut -d . -f 3`;
	port=$(( 8080 + ( $major * 100 )  + ( $minor * 10 ) + $path));
	while nc -z 127.0.0.1 $port ; do
		port=$(( $port + 1 ));
	done
	echo "$port";
}

##
# Returns the HTTP port for the specified OWGE universe
#
# @param $1 int Unirverse id
# @returns stdout<int> Port number
# @author Kevin Guanche Darias
##
function getPortByUniverseId () {
	if ! [ "$1" -eq "$1" ] 2> /dev/null; then
		echo "FATAL, param passed to getPortByUniverseId is not a number, passed: $1";
		return 1;
	fi;
	_basePort=8110;
	_port=$(( $_basePort + $1 ));
	if nc -z 127.0.0.1 $_port; then
		echo "FATAL, port $_port is already in use";
		return 1;
	fi
	echo "$_port";
}

function compileAngularProject () {
        test -d "$1/dist" && rm -r "$1/dist";
        test -d "$1" && echo "Compiling Angular project in $1 to $2";
        if [ -d "$2" ]; then
                echo "FATAL, target directory for angular project alreadt exists, used $2, aborting...";
                exit 1;
        fi
        cp -rp "$1" "$2";
        nodeRun "$2" npm install &> /dev/null;
        nodeRun "$2" npm run build &> /dev/null;
        if [ ! -d "$2/dist" ]; then
                echo "FATAL, Angular compilation failed, aborting script execution";
                exit 1;
        fi

}

function gitGetCurrentBranch () {
	git branch | grep '^*' | cut -c 3-;
}

function gitVersionExists () {
	if ! echo "$1" | grep -E "^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]$" &> /dev/null; then
                echo "FATAL, malformed version passed to gitVersionExists(), exit()ing script";
                return 1;
        fi
	major=`echo $1 | cut -d . -f 1`;
        minor=`echo $1 | cut -d . -f 2`;
        patch=`echo $1 | cut -d . -f 3`;
	if [[ $major -eq 0 && $minor -eq 3 && $patch -lt 5 ]] || [[ $major -eq 0 && $minor -eq 4 && $patch -lt 1 ]] || [[ $major -eq 0 && $minor -lt 3 ]]; then
		echo "Unsupported version v$1, only versions equal or grater than 0.3.5, 0.4.1, or, having minor equal or greater to 5 can be used";
		return 1;
	elif git tag | grep v$1 &> /dev/null; then
		return 0;
	else
		echo "NO SUCH version exists, v$1 is not a git tag";
		return 1;
	fi

}

function rollback () {
	git checkout "$oldBranch";
	exit 1;
}
