package com.kevinguanchedarias.sgtjava.dto;

import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.Unit;

public class RunningUnitBuildDto extends AbstractRunningMissionDto {

	private UnitDto unit;
	private Long count;

	public RunningUnitBuildDto(Unit unit, Mission mission, Long count) {
		super(mission);
		this.unit = new UnitDto();
		this.unit.dtoFromEntity(unit);
		this.count = count;
	}

	public UnitDto getUnit() {
		return unit;
	}

	public void setUnit(UnitDto unit) {
		this.unit = unit;
	}

	public Long getCount() {
		return count;
	}

}
