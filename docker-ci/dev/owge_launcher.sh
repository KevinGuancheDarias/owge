#!/bin/bash
if [ ! -f "./owge_launcher.sh" ]; then
    echo -e "\e[31mError: Must launch from the script folder\e[39m";
    exit ;
fi

source '../ci/lib.sh';

defaultStaticDirectory="/var/owge_data/static";
defaultDynamicDirectory="/var/owge_data/dynamic";

staticDirectory="${1:-/var/owge_data/static}";
dynamicDirectory="${2:-/var/owge_data/static}";

if ! (echo "$EXTRA_COMPOSE_OPTIONS" |  grep -wE "(\-it)|(\-i)|(\-t)" &> /dev/null); then
    EXTRA_COMPOSE_OPTIONS="$EXTRA_COMPOSE_OPTIONS -d";
fi
function _menu () {
    [ -z "$1" ] || echo;
    echo -e "OWGE launcher :: By Kevin Guanche Darias & contributors\n"
    echo -e "Passed compose options:\e[32m${EXTRA_COMPOSE_OPTIONS}. \e[36mPass environment variable EXTRA_COMPOSE_OPTIONS to change values. Ie: \e[32m$ EXTRA_COMPOSE_OPTIONS=--build -it ./owge_launcher.sh\e[39m"
    echo -e "     \e[32m1\e[39m. \e[36mLaunch all dockerized\e[39m (just for the sake of seeing some great black magic going under the hood)
     \e[32m2\e[39m. \e[36mFrontend developer mode\e[39m (launchs everything in docker, but not the Frontend ng serve)
     \e[32m3\e[39m. \e[36mBackend developer mode\e[39m (launchs database, nginx, the accounts system and the frontend)
     \e[32m4\e[39m. \e[36mFullstack developer mode\e[39m (launchs database, nginx and the accounts system) \e[33mWarning: \e[35madvanced users\e[39m
     \e[32m5\e[39m. \e[36mFullstack with local database\e[39m (Only launchs the account system and nginx) \e[33mWarning: \e[35mpro guys\e[39m
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
            OWGE_PORT="$_port"\
            STATIC_IMAGES_DIR="$staticDirectory"\
            DYNAMIC_IMAGES_DIR="$dynamicDirectory"\
             docker-compose --file ./profiles/all_dockerized.docker-compose.yml -p owge_all_dockerized up $EXTRA_COMPOSE_OPTIONS
            ;;
        2)
            ;;
        3)
            ;;
        4)
            ;;
        5)
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