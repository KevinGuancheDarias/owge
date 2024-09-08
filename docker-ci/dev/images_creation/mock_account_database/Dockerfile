FROM mysql:8
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

COPY *.sql /tmp/
RUN echo "[mysqld]" > /etc/mysql/conf.d/mysqld.cnf
RUN echo "lower_case_table_names=1" >> /etc/mysql/conf.d/mysqld.cnf
RUN echo "innodb_use_native_aio=0" >> /etc/mysql/conf.d/mysqld.cnf
RUN echo "mysql_native_password=ON" >> /etc/mysql/conf.d/mysqld.cnf
RUN cat /tmp/*.sql > /docker-entrypoint-initdb.d/init.sql