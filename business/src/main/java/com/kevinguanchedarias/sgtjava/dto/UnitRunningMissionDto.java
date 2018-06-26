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
	private PlanetDto sourcePlanet;
	private PlanetDto targetPlanet;

	public UnitRunningMissionDto(Mission mission) {
		this(mission, mission.getInvolvedUnits());
	}

	public UnitRunningMissionDto(Mission mission, List<ObtainedUnit> involvedUnits) {
		super(mission);
		this.involvedUnits = findDtoService().convertEntireArray(ObtainedUnitDto.class, involvedUnits);
		sourcePlanet = findDtoService().dtoFromEntity(PlanetDto.class, mission.getSourcePlanet());
		targetPlanet = findDtoService().dtoFromEntity(PlanetDto.class, mission.getTargetPlanet());
	}

	/**
	 * Defines as undefined the source and the target planet, of each involved
	 * unit
	 * 
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void nullifyInvolvedUnitsPlanets() {
		this.involvedUnits.forEach(current -> {
			current.setSourcePlanet(null);
			current.setTargetPlanet(null);
		});
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

	public PlanetDto getSourcePlanet() {
		return sourcePlanet;
	}

	public void setSourcePlanet(PlanetDto sourcePlanet) {
		this.sourcePlanet = sourcePlanet;
	}

	public PlanetDto getTargetPlanet() {
		return targetPlanet;
	}

	public void setTargetPlanet(PlanetDto targetPlanet) {
		this.targetPlanet = targetPlanet;
	}

}
