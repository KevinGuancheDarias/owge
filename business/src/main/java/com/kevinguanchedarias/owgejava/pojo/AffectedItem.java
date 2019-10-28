/**
 * 
 */
package com.kevinguanchedarias.owgejava.pojo;

import com.kevinguanchedarias.owgejava.exception.NotFoundException;

/**
 * Represents the affectedItem of a not existing item in a
 * {@link NotFoundException}
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class AffectedItem {
	private final Object id;
	private final Class<?> type;

	/**
	 * Create a new instance from the defined {@link NotFoundException}
	 * 
	 * @param e
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public AffectedItem(NotFoundException e) {
		id = e.getAffectedItemId();
		type = e.getAffectedItemType();
	}

	/**
	 * Create a new instance from args
	 * 
	 * @param type
	 * @param id
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public AffectedItem(Class<?> type, Number id) {
		this.type = type;
		this.id = id;
	}

	/**
	 * @since 0.8.0
	 * @return the id
	 */
	public Object getId() {
		return id;
	}

	/**
	 * @since 0.8.0
	 * @return the type
	 */
	public Class<?> getType() {
		return type;
	}

}
