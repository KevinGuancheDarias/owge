package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.InterceptableSpeedGroup;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.10.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class InterceptableSpeedGroupDto implements WithDtoFromEntityTrait<InterceptableSpeedGroup> {
	private Integer id;
	private UnitDto unit;
	private SpeedImpactGroupDto speedImpactGroup;

	@Override
	public void dtoFromEntity(InterceptableSpeedGroup interceptableSpeedGroup) {
		WithDtoFromEntityTrait.super.dtoFromEntity(interceptableSpeedGroup);
		speedImpactGroup = new SpeedImpactGroupDto();
		speedImpactGroup.dtoFromEntity(interceptableSpeedGroup.getSpeedImpactGroup());
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
	 * @return the unit
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public UnitDto getUnit() {
		return unit;
	}

	/**
	 * @param unit the unit to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setUnit(UnitDto unit) {
		this.unit = unit;
	}

	/**
	 * @return the speedImpactGroup
	 * @since 0.10.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SpeedImpactGroupDto getSpeedImpactGroup() {
		return speedImpactGroup;
	}

	/**
	 * @param speedImpactGroup the speedImpactGroup to set
	 * @author Kevin Guanche Darias
	 * @since 0.10.0
	 */
	public void setSpeedImpactGroup(SpeedImpactGroupDto speedImpactGroup) {
		this.speedImpactGroup = speedImpactGroup;
	}

}
