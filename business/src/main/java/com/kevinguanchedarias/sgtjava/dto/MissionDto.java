package com.kevinguanchedarias.sgtjava.dto;

import com.kevinguanchedarias.sgtjava.entity.Mission;

public class MissionDto implements DtoFromEntity<Mission> {

	private Long id;

	@Override
	public void dtoFromEntity(Mission entity) {
		id = entity.getId();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

}
