/**
 * 
 */
package com.kevinguanchedarias.sgtjava.dto;

import com.kevinguanchedarias.sgtjava.entity.AllianceJoinRequest;
import com.kevinguanchedarias.sgtjava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.7.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class AllianceJoinRequestDto implements WithDtoFromEntityTrait<AllianceJoinRequest> {
	private Integer id;
	private UserStorageDto user;
	private AllianceDto alliance;

	@Override
	public void dtoFromEntity(AllianceJoinRequest joinRequest) {
		id = joinRequest.getId();
		user = new UserStorageDto();
		user.dtoFromEntity(joinRequest.getUser());
		alliance = new AllianceDto();
		alliance.dtoFromEntity(joinRequest.getAlliance());
	}

	/**
	 * @since 0.7.0
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @since 0.7.0
	 * @param id
	 *            the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @since 0.7.0
	 * @return the user
	 */
	public UserStorageDto getUser() {
		return user;
	}

	/**
	 * @since 0.7.0
	 * @param user
	 *            the user to set
	 */
	public void setUser(UserStorageDto user) {
		this.user = user;
	}

	/**
	 * @since 0.7.0
	 * @return the alliance
	 */
	public AllianceDto getAlliance() {
		return alliance;
	}

	/**
	 * @since 0.7.0
	 * @param alliance
	 *            the alliance to set
	 */
	public void setAlliance(AllianceDto alliance) {
		this.alliance = alliance;
	}

}
