package com.kevinguanchedarias.owgejava.entity;

import java.io.Serializable;

public interface EntityWithId<K> extends Serializable {
	public K getId();

	/**
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setId(K id);
}
