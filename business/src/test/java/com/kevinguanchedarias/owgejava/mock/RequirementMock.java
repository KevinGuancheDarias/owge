package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.dto.RequirementDto;
import com.kevinguanchedarias.owgejava.dto.RequirementInformationDto;
import com.kevinguanchedarias.owgejava.entity.Requirement;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import lombok.experimental.UtilityClass;

import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelation;
import static com.kevinguanchedarias.owgejava.mock.ObjectRelationMock.givenObjectRelationDto;

@UtilityClass
public class RequirementMock {
    public static final int REQUIREMENT_ID = 1;
    public static final String REQUIREMENT_CODE = RequirementTypeEnum.UPGRADE_LEVEL_LOWER_THAN.name();
    public static final String REQUIREMENT_DESCRIPTION = "The Description";
    public static final long REQUIREMENT_INFORMATION_SECOND_VALUE = 9731;
    public static final long REQUIREMENT_INFORMATION_THIRD_VALUE = 1379;

    public static RequirementInformation givenRequirementInformation(long secondValue) {
        return RequirementInformation.builder()
                .secondValue(secondValue)
                .relation(givenObjectRelation())
                .build();
    }

    public static RequirementInformation givenRequirementInformation(long secondValue, RequirementTypeEnum requirementTypeEnum) {
        var retVal = givenRequirementInformation(secondValue);
        retVal.setRequirement(givenRequirement(requirementTypeEnum));
        return retVal;
    }

    public static RequirementInformation givenRequirementInformation(long secondValue, long thirdValue, RequirementTypeEnum requirementTypeEnum) {
        var retVal = givenRequirementInformation(secondValue, requirementTypeEnum);
        retVal.setThirdValue(thirdValue);
        return retVal;
    }

    public static Requirement givenRequirement(RequirementTypeEnum requirementTypeEnum) {
        return Requirement.builder()
                .id(requirementTypeEnum.getValue())
                .code(requirementTypeEnum.name())
                .build();
    }

    public static RequirementInformationDto givenRequirementInformationDto(Integer id) {
        return RequirementInformationDto.builder()
                .id(id)
                .requirement(givenRequirementDto())
                .relation(givenObjectRelationDto())
                .secondValue(REQUIREMENT_INFORMATION_SECOND_VALUE)
                .thirdValue(REQUIREMENT_INFORMATION_THIRD_VALUE)
                .build();
    }

    public static RequirementDto givenRequirementDto() {
        return RequirementDto.builder()
                .id(REQUIREMENT_ID)
                .code(REQUIREMENT_CODE)
                .description(REQUIREMENT_DESCRIPTION)
                .build();

    }
}
