package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
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
@Lazy
public class WebsocketSyncService {
    private static final Logger LOG = Logger.getLogger(WebsocketSyncService.class);

    @Autowired(required = false)
    private List<SyncSource> syncSources;

    @Autowired
    private AsyncRunnerBo asyncRunnerBo;

    @Autowired
    private UserStorageBo userStorageBo;

    @Autowired
    private WebsocketEventsInformationBo websocketEventsInformationBo;

    private final Map<String, Function<UserStorage, Object>> handlers = new HashMap<>();

    @PostConstruct
    public void init() {
        if (syncSources != null) {
            syncSources.forEach(source -> source.findSyncHandlers().forEach((handler, lambda) -> {
                if (handlers.containsKey(handler)) {
                    throw new ProgrammingException("There is already a handler for " + handler);
                } else {
                    handlers.put(handler, lambda);
                }
            }));
        } else {
            LOG.warn("No sync sources has been specified");
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
        UserStorage loggedUser = userStorageBo.findLoggedIn();
        keys.stream()
                .filter(handlers::containsKey)
                .forEach(key -> {
                    var handler = handlers.get(key);
                    var data = handler.apply(loggedUser);

                    Map<String, Object> pair = new HashMap<>();
                    Instant date = Instant.now().truncatedTo(ChronoUnit.SECONDS);
                    pair.put("data", data);
                    pair.put("lastSent", date);
                    Integer userId = loggedUser.getId();
                    websocketEventsInformationBo.save(key, userId, date);
                    retVal.put(key, pair);

                });
        return retVal;
    }

}
