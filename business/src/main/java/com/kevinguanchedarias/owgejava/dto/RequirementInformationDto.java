/**
 * 
 */
package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class RequirementInformationDto implements WithDtoFromEntityTrait<RequirementInformation> {
	private Integer id;
	private ObjectRelationDto relation;
	private RequirementDto requirement;
	private Long secondValue;
	private Long thirdValue;

	/**
	 * 
	 * @since 0.8.0
	 * @param entity Source entity
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Override
	public void dtoFromEntity(RequirementInformation entity) {
		WithDtoFromEntityTrait.super.dtoFromEntity(entity);
		requirement = new RequirementDto();
		requirement.dtoFromEntity(entity.getRequirement());
		relation = new ObjectRelationDto();
		relation.dtoFromEntity(entity.getRelation());
	}

	/**
	 * @since 0.8.0
	 * @return the id
	 */
	public Integer getId() {
		return id;
	}

	/**
	 * @since 0.8.0
	 * @param id the id to set
	 */
	public void setId(Integer id) {
		this.id = id;
	}

	/**
	 * @since 0.8.0
	 * @return the relation
	 */
	public ObjectRelationDto getRelation() {
		return relation;
	}

	/**
	 * @since 0.8.0
	 * @param relation the relation to set
	 */
	public void setRelation(ObjectRelationDto relation) {
		this.relation = relation;
	}

	/**
	 * @since 0.8.0
	 * @return the requirement
	 */
	public RequirementDto getRequirement() {
		return requirement;
	}

	/**
	 * @since 0.8.0
	 * @param requirement the requirement to set
	 */
	public void setRequirement(RequirementDto requirement) {
		this.requirement = requirement;
	}

	/**
	 * @since 0.8.0
	 * @return the secondValue
	 */
	public Long getSecondValue() {
		return secondValue;
	}

	/**
	 * @since 0.8.0
	 * @param secondValue the secondValue to set
	 */
	public void setSecondValue(Long secondValue) {
		this.secondValue = secondValue;
	}

	/**
	 * @since 0.8.0
	 * @return the thirdValue
	 */
	public Long getThirdValue() {
		return thirdValue;
	}

	/**
	 * @since 0.8.0
	 * @param thirdValue the thirdValue to set
	 */
	public void setThirdValue(Long thirdValue) {
		this.thirdValue = thirdValue;
	}

}
