package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.dto.DtoFromEntity;

/**
 *
 * @since 0.10.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class FactionUnitTypeDto implements DtoFromEntity<FactionUnitType> {

	private Integer id;
	private Integer factionId;
	private Integer unitTypeId;
	private Long maxCount;

	@Override
	public void dtoFromEntity(FactionUnitType entity) {
		id = entity.getId();
		factionId = entity.getFaction().getId();
		unitTypeId = entity.getUnitType().getId();
		maxCount = entity.getMaxCount();
	}

	/**
	 * @return the id
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the factionId
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getFactionId() {
		return factionId;
	}

	/**
	 * @param factionId the factionId to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setFactionId(Integer factionId) {
		this.factionId = factionId;
	}

	/**
	 * @return the unitTypeId
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getUnitTypeId() {
		return unitTypeId;
	}

	/**
	 * @param unitTypeId the unitTypeId to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setUnitTypeId(Integer unitTypeId) {
		this.unitTypeId = unitTypeId;
	}

	/**
	 * @return the maxCount
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Long getMaxCount() {
		return maxCount;
	}

	/**
	 * @param maxCount the maxCount to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setMaxCount(Long maxCount) {
		this.maxCount = maxCount;
	}

}
