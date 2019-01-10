/**
 * 
 */
package com.kevinguanchedarias.sgtjava.dto;

import com.kevinguanchedarias.sgtjava.entity.Alliance;
import com.kevinguanchedarias.sgtjava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class AllianceDto extends CommonDto<Integer> implements WithDtoFromEntityTrait<Alliance> {

	private UserStorageDto owner;

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
