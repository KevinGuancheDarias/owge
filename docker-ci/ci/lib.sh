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
# Will check if given env-var is defined
#
# @param $1 string env var name
# @param $2 string current value of the env var
# @param $3 string Additional optional hint
# @author Kevin Guanche Darias
##
function envFailureCheck(){
	if [ -z "$2" ]; then
                _hint="";
                test -n "$3" && _hint="Hint: $3";
		>&2 echo  ne "\e[31mError: \e[39m";
                >&2 echo "Environment is not properly configured, missing $1, $3";
		exit 1;
	fi
}

function envInfoCheck() {
        if [ -z "$2"]; then
                _hint="";
                test -n "$3" && _hint="Hint: $3";
		echo -ne "\e[32mInfo: \e[39m";
                echo "Optional $1 not specified, $3";
		exit 1;
        fi
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

function checkDirectoryExists () {
        if [ ! -d "$1" ]; then
	        echo "Directory $1 doesn't exists!";
	        exit 1;
        fi
}

##
# Exits with 1 if specified script has less arguments than expected
#
# @param $1 int Number of passed arguments (usually is $#)
# @param $2 int Number of expected arguments
# @param $3 string Additional error information
# @author Kevin Guanche Darias
##
function checkRequiredArguments () {
        if [ "$1" -lt "$2" ]; then
                echo "Err, the script requires $2 params, but specified $1. $3";
                exit 1;
        fi
}

##
# Exits with 1 if specified docker image doesn't exists
#
# @param $1 string Docker image name
# @author Kevin Guanche Darias
##
function checkDockerImageExists () {
        if [[ "$(docker images -q  "$1" 2> /dev/null)" == "" ]]; then
                echo "Image not found, must first compile it, please invoke build.sh";
                exit 1;
        fi
}

##
# Displays a deprecated log warning
#
# @param string $1 
# @author Kevin Guanche Darias
##
function deprecated () {
        if [ -n "$1" ]; then
                _useInstead=", use: $1 instead";
        fi
        log warning "$0 is deprecated";
}

##
# Prompts for a param, 
#
# @param $1 string Optional value, for use as grep expression, for example ^[0-9]{3,4}$
# @deprecated
# @author Kevin Guanche Darias
##
function promptParam () {
        deprecated $0 promptValue;
        param=
	while [ -z "$param" ]; do
		read param;
                if [ -n "$1" ] && [ -n "$param" ] && ! (echo "$param" | grep -E "$1"); then
                        echo -e "\e[31mInvalid value, retry\e[39m";
                        param="";
                fi
	done
        echo;
        echo "$param" | tr -d ' ';
}

##
# User Prompt
#
# @param $1 Display message;
# @param $2 string Optional value, for use as grep expression, for example ^[0-9]{3,4}$
# @return variable _output
# @author Kevin Guanche Darias
##
function promptValue () {
        if [ -z "$1" ]; then
                >&2 echo -e "\e[31mProgramming error.. noob programmer";
                exit 1;
        fi
        param=
	while [ -z "$param" ]; do
                echo -ne "$1: ";
		read param;
                if [ -n "$2" ] && [ -n "$param" ] && ! (echo "$param" | grep -E "$2"); then
                        echo -e "\e[31mInvalid value, retry\e[39m";
                        param="";
                fi
	done
        export _output="`echo $param`";
}

##
# Prompts for a port, if it is in use, will prompt again
#
# @param $1 string Name of the service
# @paramm $2 bool If not empty will test the Docker machine port, and not the host port (relevant for Docker Toolbox)
# @return variable _output
# @author Kevin Guanche Darias
##
function promptPort () {
        dockerFindHostIp "$2";
        _ip="$output";
        _port="";
        while [ -z "$_port" ]; do
                promptValue "Insert $1 port" "^[0-9]{2,5}$";
                if isPortOpen "$_ip" "$_output" ; then
                        echo -e "\e[31mPort $_output already in use\e[39m";
                else
                        _port="$_output";
                fi
        done
        export _output="`echo $_port`";
}

##
# Prompts once, if no response, will return the default
#
# @param $1 string Prompt message
# @param $2 string Default value
# @return variable _output
# @author Kevin Guanche Darias
##
function promptWithDefault () {
        echo -ne "$1 [\e[33m$2\e[39m]: ";
        read _value;
        if [ -z "$_value"]; then
                export _output="$2";
        else
                export _output="$_value";
        fi
        log debug "Returning value $_output";
}

##
# Checks if a command exists
#
# @param $1 string The command name
# @see https://github.com/thetrompf/yarn/blob/8b9c5f3c7238d63bce3b347472f3205cee01ddcf/bin/yarn#L10
##
function commandExists () {
        command -v "$1" >/dev/null 2>&1;
}

function isWinPty () {
        commandExists "winpty" && test -t 1;
}

function winPtyPrefix () {
        commandExists "winpty" && echo "winpty";
}

##
# Finds the absolute dir for given relative dir
# 
# @param $1 string relative dir
# @see https://stackoverflow.com/a/31605674
##
function findAbsoluteDir () {
        echo "$(cd "$(dirname "$1")"; pwd)/$(basename "$1")"
}

##
# Logs a message 
#
# @param string $1 log level, for example  "debug"
# @param string $2 log message
# @param string $3 color, for example: 1 = red, 2 = green
#
# @env DEBUG_LEVELS specified levels, separated by spaces. Ex: DEBUG_LEVELS="warning error", usually valid values are debug, info, warning, error
#
# @author Kevin Guanche Darias
##
function log () {
        if [ -n "$DEBUG_LEVELS" ]; then
                if echo "$DEBUG_LEVELS" | grep "$1" &> /dev/null; then
                        if [ "$1" == "warning" ]; then
                                _defaultColor="3";
                        elif [ "$1" == "error" ]; then
                                _defaultColor="1";
                        else 
                                _defaultColor="2";
                        fi
                        echo -e "\e[3${3-$_defaultColor}m$1: \e[39m$2";
                fi
        fi
}

function  dockerNetTools() {
        docker run --rm -i amouat/network-utils $@;
}

##
# Finds the IP of the system running docker (recommended when wanting to support Docker toolbox)
#
# @param $1 bool If non-empty will return docker machine IP when running in Docker toolbox 
# @return variable output
#
# @author Kevin Guanche Darias
##
function dockerFindHostIp () {
        log info "While the default Host or IP may look \"strange\", it's the internal IP used by Docker";
        if [ -z "$DOCKER_HOST" ]; then
                if docker run --rm -i alpine ping -c 2 host.docker.internal &> /dev/null; then
                        log debug "The user is using Windows 10/Mac docker";
                        export output="host.docker.internal";

                else
                        log debug "The user is using Linux native docker";
                        export output="`dockerNetTools bash -c 'ip route show | grep ^default' | cut -d ' ' -f 3;`";
                fi
        else
                log debug "The user is using docker toolbox";
                if [ -z "$1" ]; then
                        export output=`echo $DOCKER_HOST | cut -d ':' -f 2 | cut -c 3- | sed -e 's/\.[0-9]*$/.1/g'`;
                else 
                        export output=`echo $DOCKER_HOST | cut -d ':' -f 2 | cut -c 3-`;
                        log debug "Docker-Machine IP is $output";
                fi
        fi
}

function isPortOpen () {
        dockerNetTools nmap $1 -p $2 | grep "tcp open " &> /dev/null
}

##
# Waits till an specified command returns true
#
# @param $1 string $Waiting message
# @param ...$2 string Command
#
# @env attemps Number of attemps, defaults to 5
# @env delay Delay in second, defaults to 1 
#
# @author Kevin Guanche Darias
##
function waitFor () {
        _waitingMessage="$1";
        shift;
        _attemps=${attemps-5};
        _i=0;
        log debug "Running waitFor command: $@";
        while ! bash -c "$@" &> /dev/null; do
                echo -e "$_waitingMessage";
                sleep ${delay-1};
                _i=`expr $_i + 1`;
                if [ "$_i" -eq "$_attemps" ]; then
                        bash -c "$@";
                        >&2 echo -e "\e[31mAborting due to error";
                        exit 1;
                fi
        done
}