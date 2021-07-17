package com.kevinguanchedarias.owgejava.configurations;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

@Component
public class WebsocketConfiguration extends Configuration {
    @Value("${OWGE_WS_HOST:0.0.0.0}")
    private String host;

    @Value("${OWGE_WS_PORT:7474}")
    private String port;

    @PostConstruct
    public void init() {
        setHostname(host);
        setPort(Integer.parseInt(port));
        setRandomSession(true);
        setJsonSupport(new JacksonJsonSupport(new JavaTimeModule()));
    }
}
