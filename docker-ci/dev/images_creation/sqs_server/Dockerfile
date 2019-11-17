FROM node:10
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

RUN npm_config_user=root npm install -g sqs-server@~1.1.0

EXPOSE 7474
ENV SQS_HOST=127.0.0.1
ENV SQS_PORT=7474
ENV SQS_QUEUE=not_used

VOLUME [ "/sqs-root" ]

WORKDIR /sqs-root
CMD [ "sqs-server"]