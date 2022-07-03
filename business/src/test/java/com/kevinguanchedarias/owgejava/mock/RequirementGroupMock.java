package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import lombok.experimental.UtilityClass;

@UtilityClass
public class RequirementGroupMock {
    public static final int REQUIREMENT_GROUP_ID = 41137;

    public static RequirementGroup givenRequirementGroup() {
        return RequirementGroup.builder()
                .id(REQUIREMENT_GROUP_ID)
                .build();
    }
}
