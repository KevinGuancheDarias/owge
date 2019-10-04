/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Requirement;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class RequirementDto implements WithDtoFromEntityTrait<Requirement> {
	private Integer id;
	private String code;
	private String description;

	/**
	 * @since 0.8.0
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @since 0.8.0
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @since 0.8.0
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @since 0.8.0
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @since 0.8.0
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @since 0.8.0
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

}
