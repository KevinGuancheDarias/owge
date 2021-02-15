package com.kevinguanchedarias.owgejava.interfaces;

import java.util.Map;
import java.util.function.Function;

import com.kevinguanchedarias.owgejava.builder.SyncHandlerBuilder;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

/**
 * Instead of having multiple rest endpoints for the data, use a sync source
 * <br>
 * <b>Hint: </b> Use {@link SyncHandlerBuilder} to ease the creation of sync
 * sources
 *
 * @since 0.9.6
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public interface SyncSource {
	/**
	 *
	 * @return Map of handlerKey => lambda that fetches the information
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	Map<String, Function<UserStorage, Object>> findSyncHandlers();
}
