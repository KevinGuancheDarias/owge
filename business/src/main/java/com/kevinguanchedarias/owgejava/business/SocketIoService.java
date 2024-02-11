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
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.annotation.DependsOn;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;

@Service
@DependsOn("bootJacksonConfigurationService")
@Slf4j
public class SocketIoService {
    public static final String AUTHENTICATION = "authentication";
    public static final String USER_TOKEN_KEY = "user_token";

    private static final String EVENT_WARN_MESSAGE = "warn_message";

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

    protected SocketIOServer server;
    protected final ObjectMapper mapper;

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
        log.info("Starting websocket at ws://" + websocketConfiguration.getHostname() + ":"
                + websocketConfiguration.getPort());
        server.start();
    }

    @PreDestroy
    public void destroy() {
        log.debug("Closing websocket connection");
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
        var userSockets = findClientSockets(targetUserId);
        Map<Integer, WebsocketEventsInformation> savedInformation = new HashMap<>();
        if (targetUserId == 0) {
            userStorageRepository.findAll().forEach(user -> {
                var saved = new WebsocketEventsInformation(eventName, user.getId());
                savedInformation.put(user.getId(), saved);
                websocketEventsInformationBo.save(saved);
            });
        } else {
            var saved = websocketEventsInformationBo
                    .save(new WebsocketEventsInformation(eventName, targetUserId));
            savedInformation.put(targetUserId, saved);
        }
        handleSendMessage(userSockets, eventName, messageContent, notConnectedAction, savedInformation);
    }

    /**
     * Sends a message to all sockets from related target user, if any
     *
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

    /**
     * Sends the message one time only without saving to the db the event information
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public <T> void sendOneTimeMessage(
            int targetUserId,
            String eventName,
            Supplier<T> messageContent,
            Runnable notConnectedAction
    ) {
        var userSockets = findClientSockets(targetUserId);
        handleSendMessage(userSockets, eventName, messageContent, notConnectedAction, null);
    }

    private <T> void handleSendMessage(
            List<SocketIOClient> userSockets,
            String eventName,
            Supplier<T> messageContent,
            Runnable notConnectedAction,
            Map<Integer, WebsocketEventsInformation> savedInformation
    ) {
        if (!userSockets.isEmpty()) {
            T sendValue = messageContent.get();
            asyncRunnerBo.runAsyncWithoutContext(() -> userSockets.forEach(client -> {
                if (TransactionSynchronizationManager.isActualTransactionActive()) {
                    log.warn("Should never happened, if everything is nice!!!");
                }
                TokenUser user = client.get(USER_TOKEN_KEY);
                log.trace("Sending message to socket, event: {}, user: {}", eventName, user.getId());
                var message = savedInformation != null
                        ? new WebsocketMessage<>(savedInformation.get(user.getId()), sendValue)
                        : new WebsocketMessage<>(eventName, sendValue);
                client.sendEvent("deliver_message", message);
            }));
        } else if (notConnectedAction != null) {
            notConnectedAction.run();
        }
    }

    private List<SocketIOClient> findClientSockets(int targetUserId) {
        return server.getAllClients().stream()
                .filter(client -> client.get(USER_TOKEN_KEY) != null
                        && (targetUserId == 0 || ((TokenUser) client.get(USER_TOKEN_KEY)).getId().equals(targetUserId)))
                .toList();
    }

    private void registerUnauthenticatedEvents() {
        server.addConnectListener(
                client -> log.debug("Client connected from " + client.getRemoteAddress().toString()));
        server.addEventListener(AUTHENTICATION, String.class, (client, data, ack) -> {
            var token = mapper.readValue(data, new TypeReference<Map<String, String>>() {
            }).get("value");
            if (StringUtils.isEmpty(token)) {
                sendError(client, AUTHENTICATION, "invalid token sent from client");
            } else {
                log.trace("Authenticating using token " + token);
                var authenticatedToken = authenticationFilters.stream()
                        .map(current -> current.findUserFromToken(token)).filter(Objects::nonNull).findFirst();
                if (authenticatedToken.isPresent()) {
                    var tokenUser = authenticatedToken.get();
                    client.set(USER_TOKEN_KEY, tokenUser);
                    var eventsInfo = websocketEventsInformationBo
                            .toDto(websocketEventsInformationBo.findByUserId((Integer) tokenUser.getId()));
                    var universeIdInfo = new WebsocketEventsInformationDto();
                    universeIdInfo.setEventName(
                            "_universe_id:" + configurationBo.findConfigurationParam("UNIVERSE_ID").getValue());
                    eventsInfo.add(universeIdInfo);
                    client.sendEvent(AUTHENTICATION, new WebsocketMessage<>(AUTHENTICATION, eventsInfo));
                } else {
                    sendError(client, AUTHENTICATION, "Invalid credentials");
                }
            }
        });
    }

    private void sendError(SocketIOClient client, String event, String text) {
        log.warn(text);
        client.sendEvent(event, new WebsocketMessage<>(event, text, "error"));
        client.disconnect();
    }

}
