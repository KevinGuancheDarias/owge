@echo off
docker stop owge_dev
docker rm owge_dev
docker run -d -t -p 80:80 --name owge_dev --restart always -v G:\var\owge_data:/var/owge_data kevinguanchedarias/owgejava_dev_nginx
