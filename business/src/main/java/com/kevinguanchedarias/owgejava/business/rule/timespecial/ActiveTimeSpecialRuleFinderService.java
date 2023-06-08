package com.kevinguanchedarias.owgejava.business.rule.timespecial;

import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.stream.Stream;

@Service
@AllArgsConstructor
public class ActiveTimeSpecialRuleFinderService {
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final RuleBo ruleBo;

    /**
     * Returns the rules that are activate because the associated time specials are activated
     *
     * @param wantedType The type of rule, usually a constant of TypeProvider, for example
     *                   {@link com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveHideUnitsTypeProviderBo#TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID}
     */
    @TaggableCacheable(tags = {
            TimeSpecial.TIME_SPECIAL_CACHE_TAG,
            ActiveTimeSpecial.ACTIVE_TIME_SPECIAL_BY_USER_CACHE_TAG + ":#user.id",
            Rule.RULE_CACHE_TAG
    })
    public Stream<RuleDto> findActiveRules(UserStorage user, String wantedType) {
        return activeTimeSpecialRepository.findByUserIdAndState(user.getId(), TimeSpecialStateEnum.ACTIVE).stream()
                .flatMap(activeTimeSpecial ->
                        ruleBo.findByOriginTypeAndOriginId(
                                ObjectEnum.TIME_SPECIAL.name(), activeTimeSpecial.getTimeSpecial().getId().longValue()
                        ).stream()
                )
                .filter(ruleDto -> ruleBo.isWantedType(ruleDto, wantedType));
    }

    /**
     * @param wantedType see {@link ActiveTimeSpecialRuleFinderService#findActiveRules(UserStorage, String)}
     * @return True if there's an active time special rule enabled that affects that unit
     */
    @TaggableCacheable(tags = {
            TimeSpecial.TIME_SPECIAL_CACHE_TAG,
            ActiveTimeSpecial.ACTIVE_TIME_SPECIAL_BY_USER_CACHE_TAG + ":#user.id",
            Rule.RULE_CACHE_TAG
    })
    public boolean existsRuleMatchingUnitDestination(UserStorage user, Unit unit, String wantedType) {
        return findActiveRules(user, wantedType)
                .anyMatch(ruleDto -> ruleBo.isWantedUnitDestination(ruleDto, unit));
    }
}
