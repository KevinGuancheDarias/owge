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
	private Integer userId;
	private String username;

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
		userId = entity.getUser().getId();
		username = entity.getUser().getUsername();
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

	/**
	 * @return the userId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	/**
	 * @return the username
	 * @since 0.9.10
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getUsername() {
		return username;
	}

	/**
	 * @param username the username to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.10
	 */
	public void setUsername(String username) {
		this.username = username;
	}

}
