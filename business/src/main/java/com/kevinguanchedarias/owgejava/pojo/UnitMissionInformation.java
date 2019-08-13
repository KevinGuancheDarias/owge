package com.kevinguanchedarias.owgejava.pojo;

import java.util.List;

import com.kevinguanchedarias.owgejava.enumerations.MissionType;

/**
 * Represents the required information to register an "unit based mission"
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class UnitMissionInformation {
	private Integer userId;
	private Long sourcePlanetId;
	private Long targetPlanetId;
	private MissionType missionType;
	private List<SelectedUnit> involvedUnits;

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

	/**
	 * @since 0.7.4
	 * @return the missionType
	 */
	public MissionType getMissionType() {
		return missionType;
	}

	/**
	 * @since 0.7.4
	 * @param missionType
	 *            the missionType to set
	 */
	public void setMissionType(MissionType missionType) {
		this.missionType = missionType;
	}

	public List<SelectedUnit> getInvolvedUnits() {
		return involvedUnits;
	}

	public void setInvolvedUnits(List<SelectedUnit> involvedUnits) {
		this.involvedUnits = involvedUnits;
	}

}
