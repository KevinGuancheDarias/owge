package com.kevinguanchedarias.owgejava.mock;

import com.kevinguanchedarias.owgejava.dto.RequirementGroupDto;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import lombok.experimental.UtilityClass;
import org.springframework.beans.BeanUtils;

@UtilityClass
public class RequirementGroupMock {
    public static final int REQUIREMENT_GROUP_ID = 41137;
    public static final String REQUIREMENT_GROUP_NAME = "REQUIREMENT_GROUP_NAME";

    public static RequirementGroup givenRequirementGroup() {
        return RequirementGroup.builder()
                .id(REQUIREMENT_GROUP_ID)
                .name(REQUIREMENT_GROUP_NAME)
                .build();
    }

    public static RequirementGroupDto givenRequirementGroupDto() {
        var retVal = new RequirementGroupDto();
        BeanUtils.copyProperties(givenRequirementGroup(), retVal);
        return retVal;
    }
}
