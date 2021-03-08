package com.kevinguanchedarias.owgejava.pojo;

import lombok.Value;

/**
 * The type Mysql process information.
 * 
 * @since 0.9.20
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Value
public class MysqlProcessInformation {
    Long id;
    String user;
    String host;
    String db;
    String command;
    Long time;
    String state;
    String info;
}
