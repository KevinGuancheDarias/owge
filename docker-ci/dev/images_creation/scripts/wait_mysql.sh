#!/bin/sh
retries=120
while (test -z "`mysql -h "$MYSQL_HOST" -u $MYSQL_USER -p"$MYSQL_PASSWORD" $MYSQL_DB -e "SELECT 1 FROM configuration;" 2> /dev/null`"); do
    sleep 1
    counter=`expr $counter + 1`
    if [ $counter -gt $retries ]; then
        >&2 echo "We have been waiting for MySQL too long already; failing."
        exit 1
    fi;
    if [ $(( $counter % 10 )) -eq 0 ] ; then
        echo -e "\e[33mWarning:\e[39mTaking to long to connect to db mysql://$MYSQL_USER:$MYSQL_PASSWORD@$MYSQL_HOST/$MYSQL_DB";
    fi
done
$@