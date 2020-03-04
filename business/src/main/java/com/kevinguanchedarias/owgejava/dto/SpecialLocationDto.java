package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;

/**
 * 
 * @author Kevin Guanche Darias
 * @since 0.9.0
 *
 */
public class SpecialLocationDto extends CommonDtoWithImageStore<Integer, SpecialLocation>
		implements DtoWithImprovements {

	private ImprovementDto improvement;
	private Integer galaxyId;
	private String galaxyName;
	private Long assignedPlanetId;
	private String assignedPlanetName;

	@Override
	public void dtoFromEntity(SpecialLocation entity) {
		super.dtoFromEntity(entity);
		DtoWithImprovements.super.dtoFromEntity(entity);
		Galaxy galaxy = entity.getGalaxy();
		if (galaxy != null) {
			galaxyId = galaxy.getId();
			galaxyName = galaxy.getName();
		}
		Planet assignedPlanet = entity.getAssignedPlanet();
		if (assignedPlanet != null) {
			assignedPlanetId = assignedPlanet.getId();
			assignedPlanetName = assignedPlanet.getName();
		}
	}

	/**
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @return the improvement
	 */
	@Override
	public ImprovementDto getImprovement() {
		return improvement;
	}

	/**
	 * @param improvement the improvement to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	@Override
	public void setImprovement(ImprovementDto improvement) {
		this.improvement = improvement;
	}

	/**
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @return the galaxyId
	 */
	public Integer getGalaxyId() {
		return galaxyId;
	}

	/**
	 * @param galaxyId the galaxyId to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setGalaxyId(Integer galaxyId) {
		this.galaxyId = galaxyId;
	}

	/**
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @return the galaxyName
	 */
	public String getGalaxyName() {
		return galaxyName;
	}

	/**
	 * @param galaxyName the galaxyName to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setGalaxyName(String galaxyName) {
		this.galaxyName = galaxyName;
	}

	/**
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @return the assignedPlanetId
	 */
	public Long getAssignedPlanetId() {
		return assignedPlanetId;
	}

	/**
	 * @param assignedPlanetId the assignedPlanetId to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setAssignedPlanetId(Long assignedPlanetId) {
		this.assignedPlanetId = assignedPlanetId;
	}

	/**
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @return the assignedPlanetName
	 */
	public String getAssignedPlanetName() {
		return assignedPlanetName;
	}

	/**
	 * @param assignedPlanetName the assignedPlanetName to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setAssignedPlanetName(String assignedPlanetName) {
		this.assignedPlanetName = assignedPlanetName;
	}

}
