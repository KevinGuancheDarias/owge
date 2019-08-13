/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 * DTO for {@link Configuration}
 *
 * @since 0.7.4
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class ConfigurationDto extends CommonDto<String> implements WithDtoFromEntityTrait<Configuration> {
	private String name;
	private String displayName;
	private String value;

	/**
	 * @since 0.7.4
	 * @return the name
	 */
	@Override
	public String getName() {
		return name;
	}

	/**
	 * @since 0.7.4
	 * @param name
	 *            the name to set
	 */
	@Override
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @since 0.7.4
	 * @return the displayName
	 */
	public String getDisplayName() {
		return displayName;
	}

	/**
	 * @since 0.7.4
	 * @param displayName
	 *            the displayName to set
	 */
	public void setDisplayName(String displayName) {
		this.displayName = displayName;
	}

	/**
	 * @since 0.7.4
	 * @return the value
	 */
	public String getValue() {
		return value;
	}

	/**
	 * @since 0.7.4
	 * @param value
	 *            the value to set
	 */
	public void setValue(String value) {
		this.value = value;
	}

}
