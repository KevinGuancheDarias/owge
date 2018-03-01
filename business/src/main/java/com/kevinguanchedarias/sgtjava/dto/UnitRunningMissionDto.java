package com.kevinguanchedarias.sgtjava.dto;

import java.util.List;

import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;

/**
 * This pojo represents a mission where units are involved, such as explore,
 * gather. and such
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class UnitRunningMissionDto extends AbstractRunningMissionDto {
	private List<ObtainedUnitDto> involvedUnits;
	private Boolean invisible = false;

	public UnitRunningMissionDto(Mission mission, List<ObtainedUnit> involvedUnits) {
		super(mission);
		this.involvedUnits = findDtoService().convertEntireArray(ObtainedUnitDto.class, involvedUnits);
	}

	public List<ObtainedUnitDto> getInvolvedUnits() {
		return involvedUnits;
	}

	public void setInvolvedUnits(List<ObtainedUnitDto> involvedUnits) {
		this.involvedUnits = involvedUnits;
	}

	public Boolean getInvisible() {
		return invisible;
	}

	public void setInvisible(Boolean invisible) {
		this.invisible = invisible;
	}

}
