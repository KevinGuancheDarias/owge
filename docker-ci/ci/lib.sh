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
# Returns the HTTP port for the specified SGT version
# Notice: It ensures only a free port is returned
#
# @param $1 string Version, should look like x.x.x
# @author Kevin Guanche Darias
# @returns stdout<int> Port number
##
function getPort () {
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
