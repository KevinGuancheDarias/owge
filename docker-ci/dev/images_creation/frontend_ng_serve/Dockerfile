FROM node:10 as owge-frontends
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

ENV APP_ROOT /app
ADD package.json /app/

WORKDIR $APP_ROOT
RUN npm install	
COPY . /app

# Game Frontend
FROM owge-frontends as game-frontend 
CMD [ "npm", "run", "ngServeDocker", "--live-reload=false"]
EXPOSE 4200

# Game Admin
FROM owge-frontends as game-admin 
CMD [ "npm", "run", "ngServeDockerAdmin", "--live-reload=false"]
EXPOSE 4200