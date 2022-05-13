package com.kevinguanchedarias.owgejava.business.requirement;

import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.repository.ObjectRelationsRepository;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class TimeSpecialAvailableRequirementSourceBo implements RequirementSource {

    private final ObjectRelationsRepository objectRelationsRepository;
    private final UnlockedRelationRepository unlockedRelationRepository;

    @Override
    public boolean supports(String requirementType) {
        return RequirementTypeEnum.HAVE_SPECIAL_AVAILABLE.name().equals(requirementType);
    }

    @Override
    public boolean checkRequirementIsMet(RequirementInformation currentRequirement, UserStorage user) {
        var timeSpecialId = currentRequirement.getSecondValue();
        var relation = objectRelationsRepository.findOneByObjectDescriptionAndReferenceId(ObjectEnum.TIME_SPECIAL.name(), timeSpecialId.intValue());
        if (relation == null) {
            log.warn("Missing object relation for time special with id {}", timeSpecialId);
            return false;
        } else {
            return unlockedRelationRepository.existsByUserAndRelation(user, relation);
        }

    }
}
