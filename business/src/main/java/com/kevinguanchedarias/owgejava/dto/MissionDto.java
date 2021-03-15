package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Mission;
import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class MissionDto implements DtoFromEntity<Mission> {

	private Long id;
	private Date terminationDate;
	private Boolean resolved;
	private Boolean invisible;

	@Override
	public void dtoFromEntity(Mission entity) {
		id = entity.getId();
		terminationDate = entity.getTerminationDate();
		resolved = entity.getResolved();
		invisible = entity.getInvisible();
	}
}
