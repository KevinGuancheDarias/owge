package com.kevinguanchedarias.owgejava.business.requirement;

import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.owgejava.repository.UpgradeRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
@Slf4j
public class UpgradeLevelLowerThanRequirementSource implements RequirementSource {

    private final UpgradeRepository upgradeRepository;
    private final ObtainedUpgradeRepository obtainedUpgradeRepository;

    @Override
    public boolean supports(String requirementType) {
        return RequirementTypeEnum.UPGRADE_LEVEL_LOWER_THAN.name().equals(requirementType);
    }

    @Override
    public boolean checkRequirementIsMet(RequirementInformation currentRequirement, UserStorage user) {
        var upgradeId = currentRequirement.getSecondValue().intValue();
        var upgradeOpt = upgradeRepository.findById(upgradeId);
        if (upgradeOpt.isEmpty()) {
            log.warn("Upgrade with id {} doesn't exists for requirements {}", upgradeId, currentRequirement);
            return false;
        } else {
            int level;
            var upgrade = upgradeOpt.get();
            var userId = user.getId();
            var obtainedUpgrade = obtainedUpgradeRepository.findOneByUserIdAndUpgradeId(userId, upgrade.getId());
            if (obtainedUpgrade == null) {
                level = 0;
            } else {
                level = obtainedUpgrade.getLevel();
            }
            return obtainedUpgrade != null && level < currentRequirement.getThirdValue();
        }
    }
}
