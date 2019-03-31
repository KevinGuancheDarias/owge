/**
 * 
 */
package com.kevinguanchedarias.owgejava.rest.trait;

import java.util.Map;

import javax.servlet.http.HttpServletRequest;

import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public interface BaseRestServiceTrait {
	public default void checkPost(Object id, HttpServletRequest request) {
		if (id != null && request.getMethod().equals("POST")) {
			throw new SgtBackendInvalidInputException("Post request can't contain an id");
		}
	}

	/**
	 * Checks if the map contains the specified key
	 * 
	 * @param inputMap
	 *            Map, usually comes from JSON
	 * @param key
	 * @since 0.7.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@SuppressWarnings("rawtypes")
	public default void checkMapEntry(Map inputMap, String key) {
		if (inputMap.get(key) == null) {
			throw new SgtBackendInvalidInputException("No value for key " + key);
		}
	}

}
