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

staticDirectory="${1:-~/var/owge_data/static}";
dynamicDirectory="${2:-~/var/owge_data/static}";

if ! (echo "$EXTRA_COMPOSE_OPTIONS" |  grep -wE "(\-it)|(\-i)|(\-t)" &> /dev/null); then
    EXTRA_COMPOSE_OPTIONS="$EXTRA_COMPOSE_OPTIONS -d";
else 
    EXTRA_COMPOSE_OPTIONS="`echo $EXTRA_COMPOSE_OPTIONS | sed s/-it//`"
fi
function _menu () {
    if [ `_findAccountsSystems | wc -l` -ge 1 ]; then
        _extraMockAccountCommands="\e[32maccount. \e[39m\e[32m(Mock account is UP 🔥!)\e[36m enter mock account commandline  \e[39m";
    fi
    [ -z "$1" ] || echo;
    echo -e "OWGE launcher :: By Kevin Guanche Darias & contributors\n"
    echo -e "Passed compose up options:\e[32m${EXTRA_COMPOSE_OPTIONS}. \e[36mPass environment variable EXTRA_COMPOSE_OPTIONS to change values. Ie: \e[32m$ EXTRA_COMPOSE_OPTIONS=--build -it ./owge_launcher.sh\e[39m"
    echo -e "Passed compose build options:\e[32m${EXTRA_COMPOSE_BUILD_OPTIONS}. \e[36mPass environment variable EXTRA_COMPOSE_OPTIONS to change values. Ie: \e[32m$ EXTRA_COMPOSE_BUILD_OPTIONS=--no-cache -it ./owge_launcher.sh\e[39m"
    echo -e "     \e[32m1\e[39m. \e[36mLaunch all dockerized\e[39m (just for the sake of seeing some great black magic going under the hood)
     \e[32m2\e[39m. \e[36mFrontend developer mode\e[39m (launchs everything in docker, but not the Frontend ng serve)
     \e[32m3\e[39m. \e[36mBackend developer mode\e[39m (launchs database, nginx, the accounts system and the frontend)
     \e[32m4\e[39m. \e[36mFullstack developer mode\e[39m (launchs database, nginx and the accounts system) \e[33mWarning: \e[35madvanced users\e[39m
     \e[32m5\e[39m. \e[36mFullstack with local database\e[39m (Only launchs the account system and nginx) \e[33mWarning: \e[35mpro guys\e[39m
     $_extraMockAccountCommands
     \e[32m6\e[39m. \e[31m\U2665 \e[36mDonate \e[31m\U2665\e[39m
     \e[32m7\e[39m. \e[36mSee something nice \e[33m???\e[36m x') \e[39m
     \e[32m8\e[39m. \e[36mexit\e[39m";

    _port=

    function __promptPort () {
        echo -n "listen port: ";
        _port=`promptParam | tail -n 1`;
    }

    read -p "Insert option: " selectedOption;
    trimmedOption="`echo $selectedOption | tr -d ' '`";
    case "$trimmedOption" in
        1)
            __promptPort;
            echo "Port $_port";
            _accountPort=`expr $_port + 1`;
            echo "Account port $_accountPort";
            echo "Static directory: $staticDirectory";
            echo "Dynamic directory: $dynamicDirectory";

            (
                export OWGE_PORT="$_port";
                export OWGE_MOCK_ACCOUNT_PORT="$_accountPort";
                export STATIC_IMAGES_DIR="$staticDirectory";
                export DYNAMIC_IMAGES_DIR="$dynamicDirectory";
                line="--file ./profiles/all_dockerized.docker-compose.yml -p owge_all_dockerized";
                docker-compose $line build $EXTRA_COMPOSE_BUILD_OPTIONS
                docker-compose  $line up $EXTRA_COMPOSE_OPTIONS

            )
            _menu
            ;;
        2)
            ;;
        3)
            ;;
        4)
            ;;
        5)
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
                    _matchs=`docker ps | grep "$_container" | grep -v db_mock_account | wc -l`;
                    if  [ "$_matchs" -gt 1 ]; then
                        echo -e "\e[31m More than one account system matched\e[39m";
                        _container="";
                    elif [ "$_matchs" -eq 0 ]; then
                        echo -e "\e[31m No account system matched";
                        _container="";
                    else
                        _container=`docker ps | grep "$_container" | grep -v db_mock_account | cut -d ' ' -f 1`;
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