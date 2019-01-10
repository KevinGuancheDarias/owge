/**
 * 
 */
package com.kevinguanchedarias.sgtjava.dto;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public abstract class CommonDto<K extends Number> {
	private K id;
	private String name;
	private String description;

	/**
	 * @since 0.7.0
	 * @return the id
	 */
	public K getId() {
		return id;
	}

	/**
	 * @since 0.7.0
	 * @param id
	 *            the id to set
	 */
	public void setId(K id) {
		this.id = id;
	}

	/**
	 * @since 0.7.0
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @since 0.7.0
	 * @param name
	 *            the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @since 0.7.0
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @since 0.7.0
	 * @param description
	 *            the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

}
