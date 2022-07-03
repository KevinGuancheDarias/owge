package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.RequirementBo;
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

    private final RequirementBo requirementBo;

    @Lazy
    public RequirementGroupListener(RequirementBo requirementBo) {
        this.requirementBo = requirementBo;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostLoad
    public void computeRequirements(RequirementGroup requirementGroup) {
        requirementGroup.setRequirements(
                requirementBo.findRequirements(ObjectEnum.REQUIREMENT_GROUP, requirementGroup.getId()));
    }
}
