package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.trait.WithDtoFromEntityTrait;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementInformationDto implements WithDtoFromEntityTrait<RequirementInformation> {
    private Integer id;
    private ObjectRelationDto relation;
    private RequirementDto requirement;
    private Long secondValue;
    private Long thirdValue;

    /**
     * @param entity Source entity
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
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
     * @return the id
     * @since 0.8.0
     */
    public Integer getId() {
        return id;
    }

    /**
     * @param id the id to set
     * @since 0.8.0
     */
    public void setId(Integer id) {
        this.id = id;
    }

    /**
     * @return the relation
     * @since 0.8.0
     */
    public ObjectRelationDto getRelation() {
        return relation;
    }

    /**
     * @param relation the relation to set
     * @since 0.8.0
     */
    public void setRelation(ObjectRelationDto relation) {
        this.relation = relation;
    }

    /**
     * @return the requirement
     * @since 0.8.0
     */
    public RequirementDto getRequirement() {
        return requirement;
    }

    /**
     * @param requirement the requirement to set
     * @since 0.8.0
     */
    public void setRequirement(RequirementDto requirement) {
        this.requirement = requirement;
    }

    /**
     * @return the secondValue
     * @since 0.8.0
     */
    public Long getSecondValue() {
        return secondValue;
    }

    /**
     * @param secondValue the secondValue to set
     * @since 0.8.0
     */
    public void setSecondValue(Long secondValue) {
        this.secondValue = secondValue;
    }

    /**
     * @return the thirdValue
     * @since 0.8.0
     */
    public Long getThirdValue() {
        return thirdValue;
    }

    /**
     * @param thirdValue the thirdValue to set
     * @since 0.8.0
     */
    public void setThirdValue(Long thirdValue) {
        this.thirdValue = thirdValue;
    }

}
