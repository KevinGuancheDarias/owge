package com.kevinguanchedarias.sgtjava.dto;

import java.util.Date;

import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.util.DtoUtilService;

public abstract class AbstractRunningMissionDto {
	private Long missionId;
	private Double requiredPrimary;
	private Double requiredSecondary;
	private Date terminationDate;

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
		terminationDate = mission.getTerminationDate();
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

	public Date getTerminationDate() {
		return terminationDate;
	}

	public void setTerminationDate(Date terminationDate) {
		this.terminationDate = terminationDate;
	}

}
