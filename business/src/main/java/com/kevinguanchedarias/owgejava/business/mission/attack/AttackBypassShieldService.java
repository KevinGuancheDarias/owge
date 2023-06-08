package com.kevinguanchedarias.owgejava.business.mission.attack;

import com.kevinguanchedarias.owgejava.business.rule.timespecial.ActiveTimeSpecialRuleFinderService;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class AttackBypassShieldService {
    public static final String RULE_TYPE = "TIME_SPECIAL_IS_ENABLED_BYPASS_SHIELD";

    private final ActiveTimeSpecialRuleFinderService activeTimeSpecialRuleFinderService;

    public boolean bypassShields(ObtainedUnit source, ObtainedUnit target) {
        var user = source.getUser();
        return Boolean.TRUE.equals(source.getUnit().getBypassShield()) || activeTimeSpecialRuleFinderService.existsRuleMatchingUnitDestination(
                user, target.getUnit(), RULE_TYPE
        );
    }
}
