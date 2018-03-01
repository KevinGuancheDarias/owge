package com.kevinguanchedarias.sgtjava.pojo;

import java.util.List;

import com.kevinguanchedarias.sgtjava.dto.ObtainedUnitDto;

/**
 * Represents the required information to register an "unit based mission"
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class UnitMissionInformation {
	private Integer userId;
	private Long sourcePlanetId;
	private Long targetPlanetId;
	private List<ObtainedUnitDto> involvedUnits;

	public Integer getUserId() {
		return userId;
	}

	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	public Long getSourcePlanetId() {
		return sourcePlanetId;
	}

	public void setSourcePlanetId(Long sourcePlanetId) {
		this.sourcePlanetId = sourcePlanetId;
	}

	public Long getTargetPlanetId() {
		return targetPlanetId;
	}

	public void setTargetPlanetId(Long targetPlanetId) {
		this.targetPlanetId = targetPlanetId;
	}

	public List<ObtainedUnitDto> getInvolvedUnits() {
		return involvedUnits;
	}

	public void setInvolvedUnits(List<ObtainedUnitDto> involvedUnits) {
		this.involvedUnits = involvedUnits;
	}

}
