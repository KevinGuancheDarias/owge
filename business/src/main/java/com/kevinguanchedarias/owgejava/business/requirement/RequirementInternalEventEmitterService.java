package com.kevinguanchedarias.owgejava.business.requirement;

import com.kevinguanchedarias.owgejava.business.requirement.listener.RequirementComplianceListener;
import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class RequirementInternalEventEmitterService {
    private final List<RequirementComplianceListener> requirementComplianceListeners;

    public void doNotifyObtainedRelation(UnlockedRelation unlockedRelation) {
        requirementComplianceListeners.forEach(
                requirementComplianceListener -> requirementComplianceListener.relationObtained(unlockedRelation)
        );
    }

    public void doNotifyLostRelation(UnlockedRelation unlockedRelation) {
        requirementComplianceListeners.forEach(
                requirementComplianceListener -> requirementComplianceListener.relationLost(unlockedRelation)
        );
    }
}
