/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import org.hibernate.Hibernate;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;
import com.kevinguanchedarias.owgejava.entity.Alliance;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class AllianceDto extends CommonDto<Integer> implements WithDtoFromEntityTrait<Alliance> {

	@JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
	@JsonIdentityReference(alwaysAsId = true)
	private UserStorageDto owner;

	@Override
	public void dtoFromEntity(Alliance alliance) {
		if (Hibernate.isInitialized(alliance.getOwner())) {
			owner = new UserStorageDto();
			owner.dtoFromEntity(alliance.getOwner());
		}
		WithDtoFromEntityTrait.super.dtoFromEntity(alliance);
	}

	/**
	 * @since 0.7.0
	 * @return the owner
	 */
	public UserStorageDto getOwner() {
		return owner;
	}

	/**
	 * @since 0.7.0
	 * @param owner
	 *            the owner to set
	 */
	public void setOwner(UserStorageDto owner) {
		this.owner = owner;
	}

}
