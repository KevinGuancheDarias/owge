package com.kevinguanchedarias.owgejava.configurations;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.listener.ExceptionListenerAdapter;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import io.netty.channel.ChannelHandlerContext;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;


@Component
@Slf4j
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
        setExceptionListener(new ExceptionListenerAdapter() {
            @Override
            public void onEventException(Exception e, List<Object> args, com.corundumstudio.socketio.SocketIOClient client) {
                log.error("Websocket event exception for client {}: {}", client.getSessionId(), e.getMessage(), e);
            }

            @Override
            public void onDisconnectException(Exception e, com.corundumstudio.socketio.SocketIOClient client) {
                log.error("Websocket disconnect exception for client {}: {}", client.getSessionId(), e.getMessage(), e);
            }

            @Override
            public void onConnectException(Exception e, com.corundumstudio.socketio.SocketIOClient client) {
                log.error("Websocket connect exception for client {}: {}", client.getSessionId(), e.getMessage(), e);
            }

            @Override
            public void onPongException(Exception e, com.corundumstudio.socketio.SocketIOClient client) {
                log.error("Websocket pong exception for client {}: {}", client.getSessionId(), e.getMessage(), e);
            }

            @Override
            public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) {
                log.error("Websocket pipeline exception on channel {}: {}", ctx.channel().id(), e.getMessage(), e);
                return false;
            }
        });
    }
}
