#!/bin/bash
docker stop sgt_dev
docker rm sgt_dev
docker run -d -t -p 80:80 --name sgt_dev -v  /var/sgt_data:/var/sgt_data kevinguanchedarias/sgtjava_dev_nginx
