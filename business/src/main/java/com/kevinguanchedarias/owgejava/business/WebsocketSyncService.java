package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.6
 */
@Service
@RequiredArgsConstructor
@Lazy
@Slf4j
public class WebsocketSyncService {

    private final List<SyncSource> syncSources;
    private final UserSessionService userSessionService;
    private final WebsocketEventsInformationBo websocketEventsInformationBo;

    private final Map<String, Function<UserStorage, Object>> handlers = new HashMap<>();

    @PostConstruct
    public void init() {
        if (syncSources != null && !syncSources.isEmpty()) {
            syncSources.forEach(source -> source.findSyncHandlers().forEach((handler, lambda) -> {
                if (handlers.containsKey(handler)) {
                    throw new ProgrammingException("There is already a handler for " + handler);
                } else {
                    handlers.put(handler, lambda);
                }
            }));
        } else {
            log.warn("No sync sources has been specified");
        }
    }

    /**
     * Executes the find query to return all the wanted data
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.6
     */
    @Transactional
    public Map<String, Object> findWantedData(List<String> keys) {
        Map<String, Object> retVal = new HashMap<>();
        var loggedUser = userSessionService.findLoggedIn();
        keys.stream()
                .filter(handlers::containsKey)
                .forEach(key -> {
                    var handler = handlers.get(key);
                    var data = handler.apply(loggedUser);

                    Map<String, Object> pair = new HashMap<>();
                    var date = Instant.now().truncatedTo(ChronoUnit.SECONDS);
                    pair.put("data", data);
                    pair.put("lastSent", date);
                    var userId = loggedUser.getId();
                    websocketEventsInformationBo.save(key, userId, date);
                    retVal.put(key, pair);

                });
        return retVal;
    }

}
