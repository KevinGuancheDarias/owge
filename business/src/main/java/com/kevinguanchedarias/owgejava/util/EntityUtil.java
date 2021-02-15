package com.kevinguanchedarias.owgejava.util;

import com.kevinguanchedarias.owgejava.entity.EntityWithId;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;

/**
 *
 * @since 0.9.16
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class EntityUtil {

	/**
	 * Throws if the if is <b>not</b> null
	 *
	 * @param entity
	 * @since 0.9.16
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static void requireNullId(EntityWithId<?> entity) {
		if (entity.getId() != null) {
			throw new SgtBackendInvalidInputException("The passed entity can't have an id");
		}
	}

	private EntityUtil() {
		// An util class doesn't have constructor
	}
}
