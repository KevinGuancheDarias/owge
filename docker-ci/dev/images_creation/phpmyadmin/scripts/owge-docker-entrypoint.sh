#!/bin/bash
if [ -z "$DATABASE_SERVERS" ]; then
    >&2 echo -e "\e[31mError building, no DATABASE_SERVERS has been added format = Server name:ip:port:user:password|Second_serverr:ip:port:user:password supports spaces\e[39m";
    exit 1;
fi

function _cutField () {
    echo "$line" | cut -d ':' -f $1;
}

_targetFile="/etc/phpmyadmin/config.user.inc.php";
echo "<?php
        \$i = 0;
" > $_targetFile;
echo "$DATABASE_SERVERS" | sed -e 's/|/\n/g' | while read line; do
    echo -e "\e[32mAdding database server: $line\e[39m";
    if  echo "$line" | grep -E "*.:*.:*.:*.:" &> /dev/null; then
        name="`_cutField 1`";
        ip="`_cutField 2`";
        port="`_cutField 3`";
        user="`_cutField 4`";
        password="`_cutField 5`";
        echo -e\
        "
        // Adding OWGE server with information $line
        \$i++;
        \$cfg['Servers'][\$i]['verbose'] = '$name';
        \$cfg['Servers'][\$i]['host'] = '$ip';
        \$cfg['Servers'][\$i]['port'] = '$port';
        \$cfg['Servers'][\$i]['user'] = '$user';
        \$cfg['Servers'][\$i]['password'] = '$password';
        \$cfg['Servers'][\$i]['socket'] = '';
        \$cfg['Servers'][\$i]['connect_type'] = 'tcp';
        \$cfg['Servers'][\$i]['extension'] = 'mysqli';
        \$cfg['Servers'][\$i]['auth_type'] = 'config';
        \$cfg['Servers'][\$i]['AllowNoPassword'] = false;
        " >> $_targetFile;
    else
        1>&2 echo -e "\e[33mInvalid server, ignoring $line\e[39m":
    fi
done
