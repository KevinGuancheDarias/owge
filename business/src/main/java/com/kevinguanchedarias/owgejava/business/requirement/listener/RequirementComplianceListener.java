package com.kevinguanchedarias.owgejava.business.requirement.listener;

import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;

/**
 * Runs when the specified unlockedRelation has been obtained or lost
 */
public interface RequirementComplianceListener {
    default void relationObtained(UnlockedRelation unlockedRelation) {
        // By default, do nothing
    }

    default void relationLost(UnlockedRelation unlockedRelation) {
        // By default, do nothing
    }
}
