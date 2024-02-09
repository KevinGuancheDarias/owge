package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.RequirementInformationBo;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.PostLoad;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Component
public class UpgradeListener {
    private final RequirementInformationBo requirementInformationBo;

    @Lazy
    public UpgradeListener(RequirementInformationBo requirementInformationBo) {
        this.requirementInformationBo = requirementInformationBo;
    }

    @PostLoad
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadRequirements(Upgrade upgrade) {
        upgrade.setRequirements(requirementInformationBo.findRequirements(ObjectEnum.UPGRADE, upgrade.getId()));
    }
}
