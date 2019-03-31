package com.kevinguanchedarias.owgejava.dto;

import java.util.Date;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;

public class ObtainedUnitDto implements DtoFromEntity<ObtainedUnit> {
	private Long id;
	private UnitDto unit;
	private Long count;
	private PlanetDto sourcePlanet;
	private PlanetDto targetPlanet;
	private MissionDto mission;
	private Date expiration;

	@Override
	public void dtoFromEntity(ObtainedUnit entity) {
		id = entity.getId();
		unit = new UnitDto();
		unit.dtoFromEntity(entity.getUnit());
		count = entity.getCount();
		sourcePlanet = DtoUtilService.staticDtoFromEntity(PlanetDto.class, entity.getSourcePlanet());
		targetPlanet = DtoUtilService.staticDtoFromEntity(PlanetDto.class, entity.getTargetPlanet());
		mission = DtoUtilService.staticDtoFromEntity(MissionDto.class, entity.getMission());
		expiration = entity.getExpiration();
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
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

	public void setCount(Long count) {
		this.count = count;
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

	public MissionDto getMission() {
		return mission;
	}

	public void setMission(MissionDto mission) {
		this.mission = mission;
	}

	public Date getExpiration() {
		return expiration;
	}

	public void setExpiration(Date expiration) {
		this.expiration = expiration;
	}

}
