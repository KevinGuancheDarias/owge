FROM mysql:5.7
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

COPY *.sql /tmp/
RUN echo "[mysqld]" > /etc/mysql/conf.d/mysqld.cnf
RUN echo "lower_case_table_names=1" >> /etc/mysql/conf.d/mysqld.cnf
RUN echo "innodb_use_native_aio=0" >> /etc/mysql/conf.d/mysqld.cnf
RUN echo "default-authentication-plugin=mysql_native_password" >> /etc/mysql/conf.d/mysqld.cnf
RUN cat /tmp/*.sql > /docker-entrypoint-initdb.d/init.sql

VOLUME [ "/var/lib/mysql" ]