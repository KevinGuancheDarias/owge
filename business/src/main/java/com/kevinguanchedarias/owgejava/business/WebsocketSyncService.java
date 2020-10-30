package com.kevinguanchedarias.owgejava.business;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Function;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.interfaces.SyncSource;

/**
 *
 * @since 0.9.6
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
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

	private Map<String, Function<UserStorage, Object>> handlers = new HashMap<>();

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
	 * @param keys
	 * @return
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public Map<String, Object> findWantedData(List<String> keys) {
		Map<String, Object> retVal = new HashMap<>();
		Map<String, CompletableFuture<Object>> runningHandlers = new HashMap<>();
		keys.forEach(key -> {
			if (handlers.containsKey(key)) {
				runningHandlers.put(key, asyncRunnerBo.runAssync(userStorageBo.findLoggedIn(), handlers.get(key)));
			} else {
				throw new SgtBackendInvalidInputException("Invalid key specified, specified: " + key + ", of allowed: "
						+ String.join(",", handlers.keySet()));
			}
		});
		Collection<CompletableFuture<Object>> values = runningHandlers.values();
		CompletableFuture.allOf(values.toArray(new CompletableFuture[values.size()])).join();
		runningHandlers.forEach((key, handler) -> {
			try {
				retVal.put(key, handler.get());
			} catch (InterruptedException | ExecutionException e) {
				LOG.error(e);
				Thread.currentThread().interrupt();
			}
		});
		return retVal;
	}

}
