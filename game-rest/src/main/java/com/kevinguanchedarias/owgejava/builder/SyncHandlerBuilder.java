package com.kevinguanchedarias.owgejava.builder;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

import com.kevinguanchedarias.owgejava.interfaces.SyncSource;

/**
 * Eases the creation of {@link SyncSource} maps
 *
 * @since 0.9.6
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class SyncHandlerBuilder {

	protected Map<String, Supplier<Object>> map = new HashMap<>();

	/**
	 *
	 * @return
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static SyncHandlerBuilder create() {
		return new SyncHandlerBuilder();
	}

	/**
	 *
	 * @param handler
	 * @param lambda
	 * @return
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SyncHandlerBuilder withHandler(String handler, Supplier<Object> lambda) {
		map.put(handler, lambda);
		return this;
	}

	/**
	 *
	 * @return
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Map<String, Supplier<Object>> build() {
		return map;
	}

	protected SyncHandlerBuilder() {

	}
}
