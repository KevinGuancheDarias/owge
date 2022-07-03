package com.kevinguanchedarias.owgejava.entity.listener;

import com.kevinguanchedarias.owgejava.business.RequirementGroupBo;
import com.kevinguanchedarias.owgejava.entity.EntityWithRequirementGroups;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.PostLoad;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Component
public class EntityWithRequirementGroupsListener {
    private final RequirementGroupBo requirementGroupBo;

    @Lazy
    public EntityWithRequirementGroupsListener(RequirementGroupBo requirementGroupBo) {
        this.requirementGroupBo = requirementGroupBo;
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @PostLoad
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void loadRequirements(EntityWithRequirementGroups entityWithGroupRequirements) {
        entityWithGroupRequirements.setRequirementGroups(requirementGroupBo.findRequirements(entityWithGroupRequirements));
    }
}
