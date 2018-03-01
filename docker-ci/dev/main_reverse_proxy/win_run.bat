@echo off
docker stop sgt_dev
docker rm sgt_dev
docker run -d -t -p 80:80 --name sgt_dev --restart always -v G:\var\sgt_data:/var/sgt_data kevinguanchedarias/sgtjava_dev_nginx
