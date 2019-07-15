FROM mysql:5.7
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

COPY *.sql /tmp/
RUN echo "lower_case_table_names=1" >> /etc/mysql/mysql.conf.d/mysqld.cnf
RUN cat /tmp/*.sql > /docker-entrypoint-initdb.d/init.sql