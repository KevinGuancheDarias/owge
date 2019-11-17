#!/bin/bash
if [ ! -f "./owge_launcher.sh" ]; then
    echo -e "\e[31mError: Must launch from the script folder\e[39m";
    exit ;
fi

source '../ci/lib.sh';


if ! commandExists "docker"; then
    echo -e "\e[31mDocker MUST be installed\e[39m";
    exit 1;
fi

function _findAccountsSystems() {
    docker ps | grep mock_account | grep -v db_mock_account
}

function _findAccountsSystemsIds () {
    _findAccountsSystems | cut -d ' ' -f 1
}

owgeDataRoot="${1-$HOME/var/owge_data}";

if ! (echo "$EXTRA_COMPOSE_OPTIONS" |  grep -wE "(\-it)|(\-i)|(\-t)" &> /dev/null); then
    EXTRA_COMPOSE_OPTIONS="$EXTRA_COMPOSE_OPTIONS -d";
else 
    EXTRA_COMPOSE_OPTIONS="`echo $EXTRA_COMPOSE_OPTIONS | sed s/-it//`"
fi

if [ -z "$EXTRA_COMPOSE_BUILD_OPTIONS" ]; then
    _emptyWarning="\e[33m(when empty, \"docker-compose build\" may not be invoked)\e[39m";
else
    _emptyWarning="";
fi
log debug "Debug level is enabled";
log info "Info level is enabled";
log warning "Warning level is enabled";
log error "Error level if enabled";

if [ -n "$DOCKER_HOST" ] &&  [ `docker-machine ssh default 'mount | grep /c/Users | grep vboxsf | wc -l'` -eq  0 ]; then
    log warning "The vbox users folder is not mounted, trying to mount";
    docker-machine.exe ssh default 'sudo mkdir -p //c/Users';
    # -o -o umask=0022,gid=50,uid=1000
    if  ! docker-machine.exe ssh default 'sudo mount -t vboxsf  Users //c/Users' &> /dev/null; then
        >&2 echo -e "\e[31mAborting, /c/Users is not mounted in the vbox vm, and can't mount it, maybe because shared folder doesn't exists in virtual machine config";
        exit 1;
    fi
fi

function _menu () {
    function __findRunningComposerProject() {
        if [ `docker ps --filter "label=com.docker.compose.project" -q | wc -l ` -ge 1 ] && docker ps --filter "label=com.docker.compose.project" -q | xargs docker inspect --format='{{index .Config.Labels "com.docker.compose.project"}}'| uniq | grep "$1" &> /dev/null; then
            if [ `docker ps --filter "label=com.docker.compose.project" -q | xargs docker inspect --format='{{index .Config.Labels "com.docker.compose.project"}}' | grep "$1" | wc -l` -eq "$2" ]; then
                echo -e "\e[32m(UP)\e[39m";
            else 
                echo -e "\e[33m(UNHEALTHY)\e[39m";
            fi
        else
            echo -e "\e[31m(DOWN)\e[39m";
        fi
    }

    if [ `_findAccountsSystems | wc -l` -ge 1 ]; then
        _extraMockAccountCommands="\e[32maccount. \e[39m\e[32m(Mock account is UP 🔥!)\e[36m enter mock account commandline  \e[39m";
    fi
    [ -z "$1" ] || echo;
    echo -e "OWGE launcher :: By Kevin Guanche Darias & contributors\n"
    echo -e "Passed compose up options:\e[32m${EXTRA_COMPOSE_OPTIONS}. \e[36mPass environment variable EXTRA_COMPOSE_OPTIONS to change values. Ie: \e[32m$ EXTRA_COMPOSE_OPTIONS=--build -it ./owge_launcher.sh\e[39m"
    echo -e "Passed compose build options:\e[32m${EXTRA_COMPOSE_BUILD_OPTIONS}. $_emptyWarning  \e[36mPass environment variable EXTRA_COMPOSE_BUILD_OPTIONS to change values. Ie: \e[32m$ EXTRA_COMPOSE_BUILD_OPTIONS=--no-cache -it ./owge_launcher.sh\e[39m"
    echo -e "     `__findRunningComposerProject owge_all_dockerized 9` \e[32m1\e[39m. \e[36mLaunch all dockerized\e[39m (just for the sake of seeing some great black magic going under the hood)
     `__findRunningComposerProject owge_frontend_developer 7` \e[32m2\e[39m. \e[36mFrontend developer mode\e[39m (launchs everything in docker, but not the Frontend ng serve)
     `__findRunningComposerProject owge_backend_developer 8` \e[32m3\e[39m. \e[36mBackend developer mode\e[39m (launchs database, nginx, the accounts system and the frontend)
     `__findRunningComposerProject owge_fullstack_developer 2` \e[32m4\e[39m. \e[36mFullstack developer mode\e[39m (launchs database, nginx and the accounts system) \e[33mWarning: \e[35madvanced users\e[39m
     `__findRunningComposerProject owge_fullstack_without_db 2` \e[32m5\e[39m. \e[36mFullstack with local database\e[39m (Only launchs the account system and nginx) \e[33mWarning: \e[35mpro guys\e[39m
     $_extraMockAccountCommands
     \e[32m6\e[39m. \e[31m\U2665 \e[36mDonate \e[31m\U2665\e[39m
     \e[32m7\e[39m. \e[36mSee something nice \e[33m???\e[36m x') \e[39m
     \e[32m8\e[39m. \e[36mexit\e[39m";

    _port=

    function __promptPort () {
        echo -n "listen port: ";
        _port=`promptParam "^[0-9]{2,5}$" | tail -n 1`;
    }

    read -p "Insert option: " selectedOption;
    trimmedOption="`echo $selectedOption | tr -d ' '`";
    case "$trimmedOption" in
        1)
            (
                _doLaunch "owge_all_dockerized" _withAllDockerized;
                _menu
            )
            ;;
        2)
            (
                export OWGE_BACKEND_SERVER="rest_and_admin:8080";
                _doLaunch "owge_frontend_developer" _withFrontend _withBackend _withDatabase;
            )
            ;;

        3)
            (
                export OWGE_FRONTEND_SERVER="frontend:4200";
                _doLaunch "owge_backend_developer" _withFrontend _withBackend _withDatabase _withExportedDatabase;
            )
            ;;
        4)
            (
                _doLaunch "owge_fullstack_developer" _withFrontend _withBackend _withDatabase _withExportedDatabase;
            )
            ;;
        5)
            (
                _doLaunch "owge_fullstack_without_db" _withFrontend _withBackend;
            )
            ;;
        account)
            _noAccount="";
            _container="";
            if [ `_findAccountsSystems | wc -l` -eq 1 ]; then
                _container=`_findAccountsSystemsIds`;
            elif [ `_findAccountsSystems | wc -l` -gt 1 ]; then
                _findAccountsSystems;
                echo -e "\e[33mWarning: \e[39mMore than one account system exists, please choose one, by typing the name, or the container id";

                while [ -z "$_container" ]; do
                    echo -n "Insert container id or name (partial_match accepted): ";
                    _container=`promptParam | tail -n 1`;
                    _matchs=`_findAccountsSystems | grep "$_container" | grep -v db_mock_account | wc -l`;
                    if  [ "$_matchs" -gt 1 ]; then
                        echo -e "\e[31m More than one account system matched\e[39m";
                        _container="";
                    elif [ "$_matchs" -eq 0 ]; then
                        echo -e "\e[31m No account system matched";
                        _container="";
                    else
                        _container=`_findAccountsSystems | grep "$_container" | grep -v db_mock_account | cut -d ' ' -f 1`;
                    fi
                done
            else
                echo -e "\e[31mAccount system not available";
                _noAccount=true
            fi
            
            if [ -z "$_noAccount" ]; then
                echo "Waiting for DB to load (if required)" ;
                _attemps=0;
                _testDbCommand="docker exec --env "OWGE_IGNORE_WARNINGS=1" $_container console owge:db:test_ready";
                while ! $_testDbCommand &> /dev/null ; do
                    sleep 1;
                    _attemps=`expr $_attemps + 1`;
                    if [ "$_attemps" -ge 5 ]; then
                        echo -e "\e[31mError connecting to database, giving up after multiple attemps. See below for last error\e[39m";
                        $_testDbCommand;
                        exit 1;
                    fi
                done
                `winPtyPrefix` docker exec -it --env "OWGE_INTERACTIVE=1" $_container console;
            fi
            _menu;
            ;;
        6)
            echo -e "\e[31m              ******       ******
            **********   **********
          ************* *************
         *****************************
         *****************************
         *********** THANKS **********
          ***************************
            ***********************
              *******************
                ***************
                  ***********
                    *******
                      ***
                       *\e[39m";
            echo -e "           Donations keep project alive, used to pay the servers, and to pay the coffe. This is opensource, and will forever be opensource
             \e[31m\U2665\e[39m  Patreon:  https://www.patreon.com/bePatron?u=13416760
             \e[31m\U2665\e[39m  Paypal:  https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=XYSG7NDRN5HM6&source=url";
            ;;
        7)
            _moo;
            _menu 1;
            ;;
        8)
            exit 0;
            ;;
        *)
            echo -e "\e[31mInvalid option, try again\e[39m";
            _menu
    esac
}

##
# Launchs the docker composition
#
# @param $1 string The profile name, for example owge_all_dockerized
##
function _doLaunch() {
    if [ -z "$1" ]; then
        echo "... Programming error... the programmer probably had a noob day";
        exit 1;
    fi
    (
        _profileName="$1";
        _profileRoot="$owgeDataRoot/$_profileName";
        __promptPort;
        if [ ! -d "$_profileRoot" ]; then
            log info "Creating profile root directory $_profileRoot";
            mkdir -p $_profileRoot;
        fi
        _staticDirectory="$_profileRoot/static";
        if [ -d "$_staticDirectory" ]; then
            echo "Deleting $_staticDirectory";
            rm -r --interactive=once $_staticDirectory;
        fi
        cp -rp ../../static $_staticDirectory;
        _dynamicDirectory="$_profileRoot/dynamic";
        if [ ! -d "$_dynamicDirectory" ]; then
            log info "Creating directory $_dynamicDirectory";
            mkdir $_dynamicDirectory;
        fi
        log debug "Profile: \e[36m$_profileName\e[39m";
        log debug "Port \e[36m$_port\e[39m";
        _accountPort=`expr $_port + 1`;
        log debug "Account port \e[36m$_accountPort\e[39m";
        log debug "Static directory: \e[36m$_staticDirectory\e[39m";
        log debug "Dynamic directory: \e[36m$_dynamicDirectory\e[39m";
        _owgeDatabaseDir="$_profileRoot/db";
        _owgeMockAccountDatabaseDir="$_profileRoot/account_db";
        log debug "Database directory \e[36m$_owgeDatabaseDir\e[39m";
        log debug "Account database directory \e[36m$_owgeMockAccountDatabaseDir\e[39m";
        _availableWorlds="`ls ./test_worlds | xargs -I€ echo -e "\e[32m  * \e[36m€\e[39m"`";
        echo "$_availableWorlds" | cat;
        promptWithDefault "Select testworld from above list" "spanish_politics" "test -d ./test_worlds/€" "The specified test_world profile doesn't exists, insert one of the following: \n$_availableWorlds";
         _testWorld="$_output";
        (
            shift;
            export _launchLine="-f ./profiles/mock_account.docker-compose.yml -f ./profiles/phpmyadmin.docker-compose.yml";
            export OWGE_PORT="$_port";
            export STATIC_IMAGES_DIR="$_staticDirectory";
            export DYNAMIC_IMAGES_DIR="$_dynamicDirectory";
            export OWGE_DATABASE_DIR="$_owgeDatabaseDir";
            export OWGE_MOCK_ACCOUNT_DATABASE_DIR="$_owgeMockAccountDatabaseDir";
            export OWGE_MOCK_ACCOUNT_PORT="$_accountPort";
            export OWGE_ACCOUNT_SERVER="mock_account:8080";
            export OWGE_FRONTEND_ROOT="${BASH_SOURCE%/*}/../../../game-frontend";
            export OWGE_PHPMYADMIN_SERVERS="Mock Account Database:db_mock_account:3306:root:1234";
            export OWGE_NGINX_PHPMYADMIN_SERVER="phpmyadmin:80";
            export OWGE_ADMIN_FRONTEND_SERVER="admin_frontend:4200";

            # Launch profiles
            for profile in $@; do
                log debug "Loading profile $profile" 5;
                $profile;
            done
            # End launch profiles
            export _launchLine="$_launchLine -f ./profiles/nginx.docker-compose.yml -p $_profileName";
            test -n "$EXTRA_COMPOSE_BUILD_OPTIONS" && docker-compose $_launchLine build $EXTRA_COMPOSE_BUILD_OPTIONS
            docker-compose  $_launchLine up $EXTRA_COMPOSE_OPTIONS
            _createTestWorldIfWanted
        )
    )
}

function _withAllDockerized() {
    export OWGE_BACKEND_SERVER="rest_and_admin:8080";
    export OWGE_FRONTEND_SERVER="frontend:4200";
    _withFrontend;
    _withBackend;
    _withDatabase;
}

function _withFrontend() {
    if [ -z "$OWGE_FRONTEND_SERVER" ]; then
        dockerFindHostIp;
        _default="$output:4200";
        echo -ne "frontend server [\e[33m$_default\e[39m]: ";
        read _value;
        test -z "$_value" && export OWGE_FRONTEND_SERVER="$_default";
        echo -e "Please cd into game-frontend and run \e[32mnpm run ngServeDocker\e[39m";
        if [ -n "$OWGE_FRONTEND_SERVER" ]; then
            isPortOpen "192.168.99.1" 4200
            attemps=30 waitForSimple "Waiting for ng serve in \e[32m$OWGE_FRONTEND_SERVER\e[39m" isPortOpen `echo "$OWGE_FRONTEND_SERVER" | sed 's/:/ /'`;
            export OWGE_ADMIN_FRONTEND_SERVER="$OWGE_FRONTEND_SERVER";
        fi
    else 
        export _launchLine="$_launchLine -f ./profiles/frontend.docker-compose.yml";
    fi
    _value="";
}

function _withBackend() {
    _withSqs;
    if [ -z "$OWGE_BACKEND_SERVER" ]; then
        dockerFindHostIp;
        promptWithDefault "Backend Server" "$output:8080";
        export OWGE_BACKEND_SERVER="$_output";
        promptWithDefault "Backend contextpath" "owgejava-game-rest";
        export OWGE_REST_CONTEXT_PATH="$_output";
        promptWithDefault "Admin contextpath" "owgejava-admin";
        export OWGE_ADMIN_CONTEXT_PATH="$_output";
    else 
        export _launchLine="$_launchLine -f ./profiles/backend.docker-compose.yml";
    fi
    _value="";
}

function _withDatabase() {
    export OWGE_PHPMYADMIN_SERVERS="Universe database ($_profileName):db:3306:root:1234|$OWGE_PHPMYADMIN_SERVERS";
    export _launchLine="$_launchLine -f ./profiles/backend_database.docker-compose.yml";
}

function _withSqs() {
    export _launchLine="$_launchLine -f ./profiles/sqs_server.docker-compose.yml";
}

function _withExportedDatabase () {
    promptPort "backend MySQL" 1;
    export OWGE_DB_PORT="$_output";
    export _launchLine="$_launchLine -f ./profiles/exported-backend-database.docker-compose.yml";
}

function _createTestWorldIfWanted () {
    if [ -n "$_testWorld" ]; then
        sleep 3;
        _container=`docker ps | grep ${_profileName}_db_1 | cut -d ' ' -f 1`;
        if [ -n "$_container" ]; then
            attemps=30 waitFor "Waiting for universe database" "docker exec --env MYSQL_PWD=1234 -i "$_container" mysql -u root -e 'SELECT * FROM configuration' owge | wc -l |  xargs -I € test "€" -gt 0";
            if [ `docker exec --env MYSQL_PWD=1234 -i "$_container" mysql -u root -e "SELECT * FROM upgrade_types" owge | wc -l` -eq 0 ]; then
                log info "Creating universe $_testWorld";
                _worldDirectory="./test_worlds/$_testWorld";
                log debug "Worl directory = $_worldDirectory";
                if [ ! -d "$_worldDirectory" ]; then
                    >&2 echo -e "\[e31mNo such world is available\e[39m";
                    exit 1;
                fi
                if ! ( docker exec --env MYSQL_PWD=1234 -i "$_container" mysql -u root owge < "$_worldDirectory/init.sql" ); then
                    >&2 echo -e "\e[31mFailed to execute $_worldDirectory/init.sql\e[39m";
                    exit 1;
                fi
                log info "Copying dynamic folder from world $_testWorld";
                cp -rp "$_worldDirectory/dynamic/"* "$_dynamicDirectory";
            fi
        else
            >&2 echo -e "\e[31mCouldn't connect to database, not detected";
            exit 1;
        fi
    fi        
}
function _moo () {
    echo  -e "\e[36m                 (__)
                 (oo)
           /------\/
          / |    ||
         *  /\---/\
            
            \e[32m~~   ~~
        
        \e[39m...Have you mooed today?..., ok ... now... insert an actual option haha";

}
_menu;