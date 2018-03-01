-- Create default user with no password
INSERT INTO users (username,email,password,creation_date,last_login)VALUES('KevinGuancheDarias','kevin@kevinguanchedarias.com','$2a$10$AJiH4DXWg0z6Mf37u.XiBOf//7GrTxY59grCtXmE14XaeAegcskyK','2016-12-11','2016-12-11');
INSERT INTO configuration (name,value,privileged)VALUES('SYSTEM_EMAIL','system@sgt.test',1),('SYSTEM_PASSWORD','1234',1);