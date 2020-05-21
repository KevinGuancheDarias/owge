package com.kevinguanchedarias.owgejava.business;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;
import com.kevinguanchedarias.owgejava.configurations.WebsocketConfiguration;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.filter.OwgeJwtAuthenticationFilter;
import com.kevinguanchedarias.owgejava.pojo.WebsocketMessage;

@Service
public class SocketIoService {

	private static final String AUTHENTICATION = "authentication";
	private static final Logger LOCAL_LOGGER = Logger.getLogger(SocketIoService.class);
	private static final String USER_TOKEN_KEY = "user_token";

	@Autowired
	private WebsocketConfiguration websocketConfiguration;

	@Autowired
	@Lazy
	private List<OwgeJwtAuthenticationFilter> authenticationFilters;

	private SocketIOServer server;
	private ObjectMapper mapper = new ObjectMapper();

	public SocketIoService() {
		mapper.registerModule(new JsonOrgModule());
	}

	@PostConstruct
	public void init() {
		server = new SocketIOServer(websocketConfiguration);
		registerUnauthenticatedEvents();
	}

	@EventListener(ApplicationReadyEvent.class)
	public void onContextReady() {
		LOCAL_LOGGER.info("Starting websocket at ws://" + websocketConfiguration.getHostname() + ":"
				+ websocketConfiguration.getPort());
		server.start();
	}

	@PreDestroy
	public void destroy() {
		LOCAL_LOGGER.debug("Closing websocket connection");
		server.stop();
	}

	/**
	 * Sends a message to all sockets from related target user, if any
	 *
	 * @param <T>
	 * @param targetUser
	 * @param eventNamepost
	 * @param clazz
	 * @param messageContent
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public <T> void sendMessage(Integer targetUserId, String eventName, T messageContent) {
		server.getAllClients().stream().filter(client -> client.get(USER_TOKEN_KEY) != null
				&& ((TokenUser) client.get(USER_TOKEN_KEY)).getId().equals(targetUserId)).forEach(client -> {
					LOCAL_LOGGER.trace("Sending message to socket");
					client.sendEvent("deliver_message", new WebsocketMessage<>(eventName, messageContent));
				});
	}

	/**
	 * Sends a message to all sockets from related target user, if any
	 *
	 * @param <T>
	 * @param user
	 * @param eventName
	 * @param messageContent
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public <T> void sendMessage(UserStorage user, String eventName, T messageContent) {
		sendMessage(user.getId(), eventName, messageContent);
	}

	private void registerUnauthenticatedEvents() {
		server.addConnectListener(
				client -> LOCAL_LOGGER.debug("Client connected from " + client.getRemoteAddress().toString()));
		server.addEventListener(AUTHENTICATION, String.class, (client, data, ack) -> {
			String token = mapper.readValue(data, new TypeReference<Map<String, String>>() {
			}).get("value");
			if (StringUtils.isEmpty(token)) {
				sendError(client, AUTHENTICATION, "invalid token sent from client", true);
			} else {
				LOCAL_LOGGER.trace("Authenticating using token " + token);
				Optional<TokenUser> authenticatedToken = authenticationFilters.stream()
						.map(current -> current.findUserFromToken(token)).filter(Objects::nonNull).findFirst();
				if (authenticatedToken.isPresent()) {
					TokenUser tokenUser = authenticatedToken.get();
					client.set(USER_TOKEN_KEY, tokenUser);
					sendOk(client, AUTHENTICATION);
				} else {
					sendError(client, AUTHENTICATION, "Invalid credentials", true);
				}
			}
		});
	}

	private void sendError(SocketIOClient client, String event, String text, boolean close) {
		LOCAL_LOGGER.warn(text);
		client.sendEvent(event, new WebsocketMessage<String>(text, null, "error"));
		if (close) {
			client.disconnect();
		}
	}

	private void sendOk(SocketIOClient client, String event) {
		LOCAL_LOGGER.trace("Sending ok to event " + event);
		client.sendEvent(event, new WebsocketMessage<String>());
	}
}
