package com.kevinguanchedarias.owgejava.dto;

import java.util.Date;

import com.kevinguanchedarias.owgejava.entity.Mission;

public class MissionDto implements DtoFromEntity<Mission> {

	private Long id;
	private Date terminationDate;
	private Boolean resolved;

	@Override
	public void dtoFromEntity(Mission entity) {
		id = entity.getId();
		terminationDate = entity.getTerminationDate();
		resolved = entity.getResolved();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public Date getTerminationDate() {
		return terminationDate;
	}

	public void setTerminationDate(Date terminationDate) {
		this.terminationDate = terminationDate;
	}

	public Boolean getResolved() {
		return resolved;
	}

	public void setResolved(Boolean resolved) {
		this.resolved = resolved;
	}

}
