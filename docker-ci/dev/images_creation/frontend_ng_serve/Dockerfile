FROM node
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

RUN npm i -g @angular/cli@~1.7.4 \
    && npm i -g typescript@~2.5.3

ENV APP_ROOT /app
ADD package.json /app/

WORKDIR $APP_ROOT
RUN npm install	

COPY . /app

CMD [ "ng", "serve", "--host=0.0.0.0", "--disable-host-check", "--live-reload=false"]
EXPOSE 4200