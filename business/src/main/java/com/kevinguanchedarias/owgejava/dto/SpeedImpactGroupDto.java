package com.kevinguanchedarias.owgejava.dto;

import java.util.List;
import java.util.stream.Collectors;

import com.kevinguanchedarias.owgejava.dto.base.DtoWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class SpeedImpactGroupDto extends DtoWithMissionLimitation implements WithDtoFromEntityTrait<SpeedImpactGroup> {
	private Integer id;
	private String name;
	private Boolean isFixed = false;
	private Double missionExplore = 0D;
	private Double missionGather = 0D;
	private Double missionEstablishBase = 0D;
	private Double missionAttack = 0D;
	private Double missionConquest = 0D;
	private Double missionCounterattack = 0D;
	private List<RequirementGroupDto> requirementsGroups;

	@Override
	public void dtoFromEntity(SpeedImpactGroup entity) {
		WithDtoFromEntityTrait.super.dtoFromEntity(entity);
		id = entity.getId();
		if (entity.getRequirementGroups() != null) {
			requirementsGroups = entity.getRequirementGroups().stream().map(current -> {
				RequirementGroupDto dto = new RequirementGroupDto();
				dto.dtoFromEntity(current);
				return dto;
			}).collect(Collectors.toList());
		}
	}

	/**
	 * @return the id
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @return the name
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the isFixed
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Boolean getIsFixed() {
		return isFixed;
	}

	/**
	 * @param isFixed the isFixed to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setIsFixed(Boolean isFixed) {
		this.isFixed = isFixed;
	}

	/**
	 * @return the missionExplore
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double getMissionExplore() {
		return missionExplore;
	}

	/**
	 * @param missionExplore the missionExplore to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setMissionExplore(Double missionExplore) {
		this.missionExplore = missionExplore;
	}

	/**
	 * @return the missionGather
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double getMissionGather() {
		return missionGather;
	}

	/**
	 * @param missionGather the missionGather to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setMissionGather(Double missionGather) {
		this.missionGather = missionGather;
	}

	/**
	 * @return the missionEstablishBase
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double getMissionEstablishBase() {
		return missionEstablishBase;
	}

	/**
	 * @param missionEstablishBase the missionEstablishBase to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setMissionEstablishBase(Double missionEstablishBase) {
		this.missionEstablishBase = missionEstablishBase;
	}

	/**
	 * @return the missionAttack
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double getMissionAttack() {
		return missionAttack;
	}

	/**
	 * @param missionAttack the missionAttack to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setMissionAttack(Double missionAttack) {
		this.missionAttack = missionAttack;
	}

	/**
	 * @return the missionConquest
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double getMissionConquest() {
		return missionConquest;
	}

	/**
	 * @param missionConquest the missionConquest to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setMissionConquest(Double missionConquest) {
		this.missionConquest = missionConquest;
	}

	/**
	 * @return the missionCounterattack
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Double getMissionCounterattack() {
		return missionCounterattack;
	}

	/**
	 * @param missionCounterattack the missionCounterattack to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setMissionCounterattack(Double missionCounterattack) {
		this.missionCounterattack = missionCounterattack;
	}

	/**
	 * @return the requirementsGroups
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public List<RequirementGroupDto> getRequirementsGroups() {
		return requirementsGroups;
	}

	/**
	 * @param requirementsGroups the requirementsGroups to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setRequirementsGroups(List<RequirementGroupDto> requirementsGroups) {
		this.requirementsGroups = requirementsGroups;
	}

}
