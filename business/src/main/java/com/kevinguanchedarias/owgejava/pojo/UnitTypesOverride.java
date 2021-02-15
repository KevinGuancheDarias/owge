package com.kevinguanchedarias.owgejava.pojo;

import com.kevinguanchedarias.owgejava.dto.UnitTypeDto;

/**
 *
 * @since 0.10.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class UnitTypesOverride extends UnitTypeDto {
	private Long overrideMaxCount;

	/**
	 * @return the overrideMaxCount
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getOverrideMaxCount() {
		return overrideMaxCount;
	}

	/**
	 * @param overrideMaxCount the overrideMaxCount to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setOverrideMaxCount(Long overrideMaxCount) {
		this.overrideMaxCount = overrideMaxCount;
	}

}
