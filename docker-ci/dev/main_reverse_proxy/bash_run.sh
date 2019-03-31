#!/bin/bash
docker stop owge_dev
docker rm owge_dev
docker run -d -t -p 80:80 --name owge_dev -v  /var/owge_data:/var/owge_data kevinguanchedarias/owgejava_dev_nginx
