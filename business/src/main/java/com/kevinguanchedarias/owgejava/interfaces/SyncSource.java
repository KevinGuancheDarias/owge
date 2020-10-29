package com.kevinguanchedarias.owgejava.interfaces;

import java.util.Map;
import java.util.function.Supplier;

/**
 * Instead of having multiple rest endpoints for the data, use a sync source
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
	Map<String, Supplier<Object>> findSyncHandlers();
}
