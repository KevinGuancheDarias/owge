package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RequirementMock {
    public static RequirementInformation givenRequirementInformation(long secondValue) {
        return RequirementInformation.builder()
                .secondValue(secondValue)
                .build();
    }
}
