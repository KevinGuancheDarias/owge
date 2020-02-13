#!/bin/bash
docker rm -f owge_dev &> /dev/null;
docker run -d -t -p 80:80 --restart always --name owge_dev -v  "/$HOME/var/owge_data":"/var/owge_data" kevinguanchedarias/owge_dev_nginx
