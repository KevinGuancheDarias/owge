package com.kevinguanchedarias.owgejava.dto;

import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RequirementInformationDto implements DtoFromEntity<RequirementInformation> {
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
        id = entity.getId();
        secondValue = entity.getSecondValue();
        thirdValue = entity.getThirdValue();
        requirement = new RequirementDto();
        requirement.dtoFromEntity(entity.getRequirement());
        relation = new ObjectRelationDto();
        relation.dtoFromEntity(entity.getRelation());
    }
}
