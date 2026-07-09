package com.kevinguanchedarias.owgejava.configurations;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;


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
        setJsonSupport(new JacksonJsonSupport(new JavaTimeModule()) {
            @Override
            protected void init(ObjectMapper objectMapper) {
                super.init(objectMapper);
                // Same LocalDateTime-as-ISO-string rule as the REST mapper
                // (BootJacksonConfigurationService): the frontend never
                // consumes the Jackson [y,m,d,…] array form, and the Rust
                // backend emits ISO — websocket pushes must match.
                objectMapper.configOverride(LocalDateTime.class)
                        .setFormat(JsonFormat.Value.forShape(JsonFormat.Shape.STRING));
            }
        });
    }
}
