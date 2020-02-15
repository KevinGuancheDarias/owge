package com.kevinguanchedarias.owgejava.dto;

import java.util.Date;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

public abstract class AbstractRunningMissionDto {
	private static final int INTENTIONAL_DELAY_MS = 2000;
	private static final int NEVER_ENDING_MISSION_SYMBOL = -1;

	private Long missionId;
	private Double requiredPrimary;
	private Double requiredSecondary;
	private Long pendingMillis;
	private MissionType type;
	private Integer missionsCount;

	public AbstractRunningMissionDto() {
		throw new UnsupportedOperationException("Can't create a RunningMissionDto from an empty constructor");
	}

	protected DtoUtilService findDtoService() {
		return new DtoUtilService();
	}

	protected AbstractRunningMissionDto(Mission mission) {
		missionId = mission.getId();
		requiredPrimary = mission.getPrimaryResource();
		requiredSecondary = mission.getSecondaryResource();
		pendingMillis = mission.getTerminationDate() == null ? NEVER_ENDING_MISSION_SYMBOL
				: (mission.getTerminationDate().getTime() - new Date().getTime() + INTENTIONAL_DELAY_MS);
		type = MissionType.valueOf(mission.getType().getCode());
	}

	public Long getMissionId() {
		return missionId;
	}

	public void setMissionId(Long missionId) {
		this.missionId = missionId;
	}

	public Double getRequiredPrimary() {
		return requiredPrimary;
	}

	public void setRequiredPrimary(Double requiredPrimary) {
		this.requiredPrimary = requiredPrimary;
	}

	public Double getRequiredSecondary() {
		return requiredSecondary;
	}

	public void setRequiredSecondary(Double requiredSecondary) {
		this.requiredSecondary = requiredSecondary;
	}

	/**
	 * @return the pendingMillis
	 * @since 0.8.1
	 */
	public Long getPendingMillis() {
		return pendingMillis;
	}

	/**
	 * @param pendingMillis the pendingMillis to set
	 * @since 0.8.1
	 */
	public void setPendingMillis(Long pendingMillis) {
		this.pendingMillis = pendingMillis;
	}

	public MissionType getType() {
		return type;
	}

	public void setType(MissionType type) {
		this.type = type;
	}

	/**
	 * @return the missionsCount
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getMissionsCount() {
		return missionsCount;
	}

	/**
	 * @param missionsCount the missionsCount to set
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void setMissionsCount(Integer missionsCount) {
		this.missionsCount = missionsCount;
	}

}
