package com.kevinguanchedarias.sgtjava.dto;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.sgtjava.entity.Planet;

public class PlanetDto implements DtoFromEntity<Planet> {
	private Long id;
	private String name;
	private Long sector;
	private Long quadrant;
	private Integer planetNumber;
	private Integer ownerId;
	private String ownerName;
	private Integer richness;
	private Boolean home;
	private Integer galaxyId;
	private String galaxyName;
	private Integer specialLocationId;
	private String specialLocationName;

	@Override
	public void dtoFromEntity(Planet entity) {
		EntityPojoConverterUtil.convertFromTo(this, entity);
		if (entity.getOwner() != null) {
			ownerId = entity.getOwner().getId();
			ownerName = entity.getOwner().getUsername();
		}
		if (entity.getGalaxy() != null) {
			galaxyId = entity.getGalaxy().getId();
			galaxyName = entity.getGalaxy().getName();
		}
		if (entity.getSpecialLocation() != null) {
			specialLocationId = entity.getSpecialLocation().getId();
			specialLocationName = entity.getSpecialLocation().getName();
		}
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Long getSector() {
		return sector;
	}

	public void setSector(Long sector) {
		this.sector = sector;
	}

	public Long getQuadrant() {
		return quadrant;
	}

	public void setQuadrant(Long quadrant) {
		this.quadrant = quadrant;
	}

	public Integer getPlanetNumber() {
		return planetNumber;
	}

	public void setPlanetNumber(Integer planetNumber) {
		this.planetNumber = planetNumber;
	}

	public Integer getRichness() {
		return richness;
	}

	public void setRichness(Integer richness) {
		this.richness = richness;
	}

	public Boolean getHome() {
		return home;
	}

	public void setHome(Boolean home) {
		this.home = home;
	}

	public Integer getGalaxyId() {
		return galaxyId;
	}

	public void setGalaxyId(Integer galaxyId) {
		this.galaxyId = galaxyId;
	}

	public String getGalaxyName() {
		return galaxyName;
	}

	public void setGalaxyName(String galaxyName) {
		this.galaxyName = galaxyName;
	}

	public Integer getSpecialLocationId() {
		return specialLocationId;
	}

	public void setSpecialLocationId(Integer specialLocationId) {
		this.specialLocationId = specialLocationId;
	}

	public String getSpecialLocationName() {
		return specialLocationName;
	}

	public void setSpecialLocationName(String specialLocationName) {
		this.specialLocationName = specialLocationName;
	}

	public Integer getOwnerId() {
		return ownerId;
	}

	public void setOwnerId(Integer ownerId) {
		this.ownerId = ownerId;
	}

	public String getOwnerName() {
		return ownerName;
	}

	public void setOwnerName(String ownerName) {
		this.ownerName = ownerName;
	}
}
