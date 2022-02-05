package com.kevinguanchedarias.owgejava.business.requirement;

import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@AllArgsConstructor
public class TimeSpecialEnabledRequirementSourceBo implements RequirementSource {
    public static final String REQUIREMENT_SOURCE_ID = "HAVE_SPECIAL_ENABLED";

    private ActiveTimeSpecialRepository repository;

    @Override
    public boolean supports(String requirementType) {
        return REQUIREMENT_SOURCE_ID.equals(requirementType);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    @Override
    public boolean checkRequirementIsMet(RequirementInformation currentRequirement, UserStorage user) {
        var timeSpecialId = currentRequirement.getSecondValue().intValue();
        return repository.findOneByTimeSpecialIdAndUserId(timeSpecialId, user.getId())
                .filter(activeTimeSpecial -> activeTimeSpecial.getState() == TimeSpecialStateEnum.ACTIVE)
                .isPresent();
    }
}
