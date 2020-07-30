package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.kevinsuite.commons.convert.EntityPojoConverterUtil;
import com.kevinguanchedarias.owgejava.entity.Planet;

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
	private SpecialLocationDto specialLocation;

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
			specialLocation = new SpecialLocationDto();
			specialLocation.dtoFromEntity(entity.getSpecialLocation());
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

	/**
	 * @return the specialLocation
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SpecialLocationDto getSpecialLocation() {
		return specialLocation;
	}

	/**
	 * @param specialLocation the specialLocation to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setSpecialLocation(SpecialLocationDto specialLocation) {
		this.specialLocation = specialLocation;
	}

}
