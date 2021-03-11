FROM mysql:8
LABEL maintainer "Kevin Guanche Darias <kevin@kevinguanchedarias.com>"

# COPY *.sql /tmp/ (Asterisk is evil as Docker would use a cached version of the files, even if there are changes) /tmp/
COPY 00_create_db.sql /tmp/
COPY 01_use_db.sql /tmp/
COPY 02_schema.sql /tmp/
COPY 03_use_db.sql /tmp/
COPY 04_insert_data.sql /tmp/
COPY 05_mysql_procedures.sql /tmp/
RUN echo "[mysqld]" > /etc/mysql/conf.d/mysqld.cnf
RUN echo "lower_case_table_names=1" >> /etc/mysql/conf.d/mysqld.cnf
RUN echo "innodb_use_native_aio=0" >> /etc/mysql/conf.d/mysqld.cnf
RUN echo "default-authentication-plugin=mysql_native_password" >> /etc/mysql/conf.d/mysqld.cnf
RUN cat /tmp/*.sql > /docker-entrypoint-initdb.d/init.sql

VOLUME [ "/var/lib/mysql" ]
