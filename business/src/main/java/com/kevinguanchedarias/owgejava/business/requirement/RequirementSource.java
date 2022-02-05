package com.kevinguanchedarias.owgejava.business.requirement;

import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;

public interface RequirementSource {
    boolean supports(String requirementType);

    boolean checkRequirementIsMet(RequirementInformation currentRequirement, UserStorage user);
}
