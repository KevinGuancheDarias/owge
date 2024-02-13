kevinsuiteRoot="/public/kevinsuite-java";
kevinsuiteCommonBackend="$kevinsuiteRoot/common-backend";
kevinsuiteRestBackend="$kevinsuiteRoot/backend-rest-commons";

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
		>&2 echo  -ne "\e[31mError: \e[39m";
                >&2 echo "Environment is not properly configured, missing $1, $3";
		exit 1;
	fi
}

function envInfoCheck() {
        if [ -z "$2" ]; then
                _hint="";
                test -n "$3" && _hint="Hint: $3";
		echo -ne "\e[32mInfo: \e[39m";
                echo "Optional $1 not specified, $3";
		exit 1;
        fi
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
	if ! echo "$1" | grep -E "^[0-9]{1,3}\.[0-9]{1,3}\.[0-9]{1,3}$" &> /dev/null; then
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
# @param $3 string (Optional) Test command, if invalid will ask again, notice "€" will be replaced by the user input value
# @param $4 string (Optional) When specified, represents a "hint" to show when the test command fails
# @return variable _output
# @author Kevin Guanche Darias
##
function promptWithDefault () {
        export _output="";
        echo -ne "$1 [\e[33m$2\e[39m]: ";
        read _value;
        while [ -z "$_output" ]; do
                if [ -n "$3" ] && [ -n "$_value" ] && ! (echo "$_value" | xargs -I€ bash -c "$3"); then
                        _command="`echo "$_value" | xargs -I€ echo "$3"`";
                        log debug "promptWithDefault test command has been specified ($_command) and input has an INVALID string";
                        >&2 echo -ne "\e[31mInvalid input \e[39m";
                        >&2 test -n "$4" && echo -ne "Hint: \e[35m$4\e[39m";
                        export _output="";
                        echo
                        echo -ne "$1 [\e[33m$2\e[39m]: ";
                        read _value;
                elif [ -z "$_value" ]; then
                        log debug "promptWithDefault empty value";
                        export _output="$2";
                else
                        log debug "promptWithDefault value defined";
                        export _output="$_value";
                fi
        done
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
# Logs a message, if no DEBUG_LEVELS is specified, will only DEBUG errors 
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
                _log_levels="$DEBUG_LEVELS";
        else
                _log_levels="error,warning";
        fi
        if echo "$_log_levels" | grep "$1" &> /dev/null; then
                if [ "$1" == "warning" ]; then
                        _defaultColor="3";
                elif [ "$1" == "error" ]; then
                        _defaultColor="1";
                else 
                        _defaultColor="2";
                fi
                echo -e "\e[3${3-$_defaultColor}m${1^^} [`date -Ins | cut -d + -f 1`]: $2\e[39m";
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
                        export output="`dockerNetTools 'ip route show' | grep default | cut -d \  -f 3;`";
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
function waitForSimple () {
        _waitingMessage="$1";
        shift;
        _attemps=${attemps-5};
        _i=0;
        log debug "Running waitFor command: $@";
        while ! "$@" &> /dev/null; do
                echo -e "$_waitingMessage";
                sleep ${delay-1};
                _i=`expr $_i + 1`;
                if [ "$_i" -eq "$_attemps" ]; then
                        "$@";
                        >&2 echo -e "\e[31mAborting due to error";
                        exit 1;
                fi
        done
}


function ctrl_c() {
        log debug "** Trapped CTRL-C"
        if [ ${script_in_query-0} -eq 1 ]; then
                log warning "An outgoing mysql query is executing, please wait"
                script_aborted=1
        else
                trap INT;
                exit 1;
        fi

}

##
# Runs a MySQL command, against the known to be the mysql connection
# 
# @param string $1 the command itself If not specified, will read from stdin
# @env OWGE_DB_URL
# @env OWGE_DB_USER
# @env OWGE_DB_PASS
##
function mysql_run() {
        if [ -n "$script_aborted" ]; then
                log error "Can't run MySQL command as script is aborted";
                return 1;
        fi
	_mysql_host_and_port=`echo "$OWGE_DB_URL" | cut -d '/' -f 1`;
	_mysql_db=`echo "$OWGE_DB_URL" | cut -d '/' -f 2`;
	_mysql_host=`echo "$_mysql_host_and_port" | cut -d ':' -f 1`;
	_mysql_port=`echo "$_mysql_host_and_port" | cut -d ':' -f 2`;
        if [ -z "$1" ]; then
                _command='cat';
                _filename=/tmp/`date +%s`.tmp;
                cat > $_filename
                _concat="< $_filename";
        else
                _command="echo $1";
                _concat="";
        fi
	( 
                if [ -z  "$_concat" ]; then
                        $_command | MYSQL_PWD="$OWGE_DB_PASS" mysql -h "$_mysql_host" -P "$_mysql_port" -u "$OWGE_DB_USER" $_mysql_db;
                else
                        $_command | MYSQL_PWD="$OWGE_DB_PASS" mysql -h "$_mysql_host" -P "$_mysql_port" -u "$OWGE_DB_USER" $_mysql_db < $_filename;
                fi
        ) &
        _pid=$!;
        _count=0;
        script_in_query=1;
        while [ -d /proc/$_pid ]; do
                _count=$(($_count + 1));
                if [ $(($_count % 5 )) -eq 0 ]; then
                        log warning "The mysql query is taking long time";
                fi
                sleep 1;
        done
        wait $_pid;
        _result=$?;
        script_in_query=0;
        if [ ${script_aborted-0} -eq 1 ]; then
                log debug "Killing as wanted";
                trap INT;
                exit 1;
        fi
        return $_result;
}

##
# Returns with error if mysql query fails (empty result)
#
# @param string $1 The query to run
# @param string $2 extra filter
#
##
function check_mysql_query_fails() {
	_extra_filter="${2-cat}";
	test `mysql_run "$1" | $_extra_filter | wc -l` -eq 0;
}

export log_has_displayed_levels=${log_has_displayed_levels-0};
(
        if [ $log_has_displayed_levels -eq 0 ]; then
                _levels=${DEBUG_LEVELS-error};
                for level in ${_levels//,/ }; do
                        log $level "Level $level is enabled";
                done
        fi
)
export log_has_displayed_levels=1;
