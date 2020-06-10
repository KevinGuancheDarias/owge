package com.kevinguanchedarias.owgejava.configurations;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.Configuration;

@Component
public class WebsocketConfiguration extends Configuration {
	@Value("${OWGE_WS_HOST:0.0.0.0}")
	private String host;

	@Value("${OWGE_WS_PORT:7474}")
	private String port;

	@Value("${OWGE_WS_PING_TIMEOUT:5000}")
	private String pingTimeout;

	@Value("${OWGE_WS_PING_INTERVAL:2500}")
	private String pingInterval;

	@PostConstruct
	public void init() {
		setHostname(host);
		setPort(Integer.valueOf(port));
		setRandomSession(true);
	}
}
