package com.kevinguanchedarias.owgejava.business;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;
import com.kevinguanchedarias.owgejava.configurations.WebsocketConfiguration;
import com.kevinguanchedarias.owgejava.dto.WebsocketEventsInformationDto;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.WebsocketEventsInformation;
import com.kevinguanchedarias.owgejava.filter.OwgeJwtAuthenticationFilter;
import com.kevinguanchedarias.owgejava.pojo.WebsocketMessage;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

@Service
@DependsOn("bootJacksonConfigurationService")
@Slf4j
public class SocketIoService {

    private static final String EVENT_WARN_MESSAGE = "warn_message";
    private static final String AUTHENTICATION = "authentication";
    private static final Logger LOCAL_LOGGER = Logger.getLogger(SocketIoService.class);
    private static final String USER_TOKEN_KEY = "user_token";

    @Autowired
    private WebsocketConfiguration websocketConfiguration;

    @Autowired
    private WebsocketEventsInformationBo websocketEventsInformationBo;

    @Autowired
    private ConfigurationBo configurationBo;

    @Autowired
    private UserStorageRepository userStorageRepository;

    @Autowired
    private AsyncRunnerBo asyncRunnerBo;

    @Autowired
    @Lazy
    private List<OwgeJwtAuthenticationFilter> authenticationFilters;

    private SocketIOServer server;
    private final ObjectMapper mapper;

    public SocketIoService(ObjectMapper springMapper) {
        mapper = springMapper.copy();
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
     * @param targetUserId       If 0 will broadcast to all connected users
     * @param notConnectedAction Action to run if the user is not connected
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.2
     */
    public <T> void sendMessage(int targetUserId, String eventName, Supplier<T> messageContent,
                                Runnable notConnectedAction) {
        List<SocketIOClient> userSockets = server.getAllClients().stream()
                .filter(client -> client.get(USER_TOKEN_KEY) != null
                        && (targetUserId == 0 || ((TokenUser) client.get(USER_TOKEN_KEY)).getId().equals(targetUserId)))
                .collect(Collectors.toList());
        Map<Integer, WebsocketEventsInformation> savedInformation = new HashMap<>();
        if (targetUserId == 0) {
            userStorageRepository.findAll().forEach(user -> {
                WebsocketEventsInformation saved = new WebsocketEventsInformation(eventName, user.getId());
                savedInformation.put(user.getId(), saved);
                websocketEventsInformationBo.save(saved);
            });
        } else {
            WebsocketEventsInformation saved = websocketEventsInformationBo
                    .save(new WebsocketEventsInformation(eventName, targetUserId));
            savedInformation.put(targetUserId, saved);
        }
        if (!userSockets.isEmpty()) {
            T sendValue = messageContent.get();
            asyncRunnerBo.runAssyncWithoutContext(() -> userSockets.forEach(client -> {
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    LOCAL_LOGGER.warn("Should never happend, if everything is nice!!!");
                }
                TokenUser user = client.get(USER_TOKEN_KEY);
                log.trace("Sending message to socket, event: {}, user: {}", eventName, user.getId());
                client.sendEvent("deliver_message",
                        new WebsocketMessage<>(savedInformation.get(user.getId()), sendValue));
            }));
        } else if (notConnectedAction != null) {
            notConnectedAction.run();
        }
    }

    /**
     * Sends a message to all sockets from related target user, if any
     *
     * @param <T>
     * @param targetUserId
     * @param eventName
     * @param messageContent
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public <T> void sendMessage(int targetUserId, String eventName, Supplier<T> messageContent) {
        sendMessage(targetUserId, eventName, messageContent, null);
    }

    /**
     * Sends a message to all sockets from related target user, if any
     *
     * @param notConnectedAction Action to run if the user is not connected
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.2
     */
    public <T> void sendMessage(UserStorage user, String eventName, Supplier<T> messageContent,
                                Runnable notConnectedAction) {
        sendMessage(user.getId(), eventName, messageContent, notConnectedAction);
    }

    /**
     * Sends a message to all sockets from related target user, if any
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public <T> void sendMessage(UserStorage user, String eventName, Supplier<T> messageContent) {
        sendMessage(user == null ? 0 : user.getId(), eventName, messageContent, null);
    }

    public void sendWarning(UserStorage user, String i18nWarningText) {
        sendMessage(user, EVENT_WARN_MESSAGE, () -> i18nWarningText);
    }

    /**
     * Removes all the cache entries
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public void clearCache() {
        websocketEventsInformationBo.clear();
        server.getAllClients().forEach(client -> client.sendEvent("cache_clear", "null"));
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
                    List<WebsocketEventsInformationDto> eventsInfo = websocketEventsInformationBo
                            .toDto(websocketEventsInformationBo.findByUserId((Integer) tokenUser.getId()));
                    WebsocketEventsInformationDto universeIdInfo = new WebsocketEventsInformationDto();
                    universeIdInfo.setEventName(
                            "_universe_id:" + configurationBo.findConfigurationParam("UNIVERSE_ID").getValue());
                    eventsInfo.add(universeIdInfo);
                    client.sendEvent(AUTHENTICATION, new WebsocketMessage<>(AUTHENTICATION, eventsInfo));
                } else {
                    sendError(client, AUTHENTICATION, "Invalid credentials", true);
                }
            }
        });
    }

    private void sendError(SocketIOClient client, String event, String text, boolean close) {
        LOCAL_LOGGER.warn(text);
        client.sendEvent(event, new WebsocketMessage<>(event, text, "error"));
        if (close) {
            client.disconnect();
        }
    }

}
