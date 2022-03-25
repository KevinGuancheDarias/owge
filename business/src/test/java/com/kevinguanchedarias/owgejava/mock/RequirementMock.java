package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.Requirement;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import lombok.experimental.UtilityClass;

import java.util.Arrays;
import java.util.List;

@UtilityClass
public class RequirementMock {
    public static RequirementInformation givenRequirementInformation(long secondValue) {
        return RequirementInformation.builder()
                .secondValue(secondValue)
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

    public static List<Requirement> givenAllRequirements() {
        return Arrays.stream(RequirementTypeEnum.values())
                .map(current -> Requirement.builder().id(current.getValue()).code(current.name()).build())
                .toList();
    }
}
