package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.RequirementInformationBo;
import com.kevinguanchedarias.owgejava.entity.RequirementGroup;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import javax.persistence.PostLoad;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Lazy
@Component
public class RequirementGroupListener {

    private final RequirementInformationBo requirementInformationBo;

    @Lazy
    public RequirementGroupListener(RequirementInformationBo requirementInformationBo) {
        this.requirementInformationBo = requirementInformationBo;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostLoad
    public void computeRequirements(RequirementGroup requirementGroup) {
        requirementGroup.setRequirements(
                requirementInformationBo.findRequirements(ObjectEnum.REQUIREMENT_GROUP, requirementGroup.getId()));
    }
}
