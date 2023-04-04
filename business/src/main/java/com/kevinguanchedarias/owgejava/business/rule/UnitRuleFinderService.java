package com.kevinguanchedarias.owgejava.business.rule;

import com.kevinguanchedarias.owgejava.business.rule.itemtype.UnitRuleItemTypeProviderBo;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.util.Optional.ofNullable;

@Service
@AllArgsConstructor
public class UnitRuleFinderService {
    public static final String UNIT_TYPE = "UNIT_TYPE";
    private final RuleRepository ruleRepository;
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final TaggableCacheManager taggableCacheManager;

    @TaggableCacheable(
            tags = {
                    Rule.RULE_CACHE_TAG, Unit.UNIT_CACHE_TAG, UnitType.UNIT_TYPE_CACHE_TAG
            },
            keySuffix = "#ruleTypeId from_unit #from.id #to.id"
    )
    public Optional<Rule> findRule(String ruleTypeId, Unit from, Unit to) {
        var fromUnitType = from.getType();
        var toUnitType = to.getType();
        return unitVsUnitOptional(ruleTypeId, from, to)
                .or(() -> unitVsUnitTypeOptional(ruleTypeId, from, toUnitType))
                .or(() -> unitTypeVsUnitOptional(ruleTypeId, fromUnitType, to))
                .or(() -> unitTypeVsUnitTypeOptional(ruleTypeId, fromUnitType, toUnitType));
    }

    @TaggableCacheable(
            tags = {
                    Rule.RULE_CACHE_TAG,
                    Unit.UNIT_CACHE_TAG,
                    UnitType.UNIT_TYPE_CACHE_TAG,
                    ActiveTimeSpecial.ACTIVE_TIME_SPECIAL_BY_USER_CACHE_TAG + ":" + "#user.id"
            },
            keySuffix = "#ruleTypeId #user.id #to.id"
    )
    public Optional<Rule> findRuleByActiveTimeSpecialsAndTargetUnit(String ruleTypeId, UserStorage user, Unit to) {
        return activeTimeSpecialRepository.findByUserIdAndState(user.getId(), TimeSpecialStateEnum.ACTIVE).stream()
                .map(ActiveTimeSpecial::getTimeSpecial)
                .map(timeSpecial -> findRule(ruleTypeId, timeSpecial, to))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst();
    }

    private Optional<Rule> unitVsUnitOptional(String ruleTypeId, Unit from, Unit to) {
        return ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                ruleTypeId,
                UnitRuleItemTypeProviderBo.PROVIDER_ID,
                from.getId().longValue(),
                UnitRuleItemTypeProviderBo.PROVIDER_ID,
                to.getId().longValue()
        );
    }

    private Optional<Rule> unitVsUnitTypeOptional(String ruleTypeId, Unit from, UnitType toUnitType) {
        return ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                ruleTypeId,
                "UNIT",
                from.getId().longValue(),
                UNIT_TYPE,
                toUnitType.getId().longValue()
        ).or(() ->
                ofNullable(toUnitType.getParent())
                        .flatMap(toParentType -> unitVsUnitTypeOptional(ruleTypeId, from, toParentType))
        );

    }

    private Optional<Rule> unitTypeVsUnitOptional(String ruleTypeId, UnitType fromUnitType, Unit to) {
        return ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                ruleTypeId,
                UNIT_TYPE,
                fromUnitType.getId().longValue(),
                "UNIT",
                to.getId().longValue()
        ).or(() ->
                ofNullable(fromUnitType.getParent())
                        .flatMap(fromParent -> unitTypeVsUnitOptional(ruleTypeId, fromParent, to))
        );
    }

    private Optional<Rule> unitTypeVsUnitTypeOptional(String ruleTypeId, UnitType fromUnitType, UnitType toUnitType) {
        return ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                ruleTypeId,
                UNIT_TYPE,
                fromUnitType.getId().longValue(),
                UNIT_TYPE,
                toUnitType.getId().longValue()
        ).or(() ->
                ofNullable(toUnitType.getParent())
                        .flatMap(toParent -> unitTypeVsUnitTypeOptional(ruleTypeId, fromUnitType, toParent))
        ).or(() ->
                ofNullable(fromUnitType.getParent())
                        .flatMap(fromParent -> unitTypeVsUnitTypeOptional(ruleTypeId, fromParent, toUnitType))
        );
    }

    private Optional<Rule> timeSpecialVsUnit(String ruleTypeId, TimeSpecial from, Unit to) {
        return ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                ruleTypeId,
                ObjectEnum.TIME_SPECIAL.name(),
                from.getId().longValue(),
                "UNIT",
                to.getId().longValue()
        );
    }

    private Optional<Rule> timeSpecialVsUnitType(String ruleTypeId, TimeSpecial from, UnitType toUnitType) {
        return ruleRepository.findOneByTypeAndOriginTypeAndOriginIdAndDestinationTypeAndDestinationId(
                ruleTypeId,
                ObjectEnum.TIME_SPECIAL.name(),
                from.getId().longValue(),
                UNIT_TYPE,
                toUnitType.getId().longValue()
        ).or(() ->
                ofNullable(toUnitType.getParent())
                        .flatMap(toParentType -> timeSpecialVsUnitType(ruleTypeId, from, toParentType))
        );
    }

    private Optional<Rule> findRule(String ruleTypeId, TimeSpecial from, Unit to) {
        return taggableCacheManager.computeIfAbsent(
                UnitRuleFinderService.class.getName() + "_findRule from_time_special" + ruleTypeId + " " + from.getId() + " " + to.getId(),
                List.of(Rule.RULE_CACHE_TAG, Unit.UNIT_CACHE_TAG, UnitType.UNIT_TYPE_CACHE_TAG, TimeSpecial.TIME_SPECIAL_CACHE_TAG),
                () -> timeSpecialVsUnit(ruleTypeId, from, to)
                        .or(() -> timeSpecialVsUnitType(ruleTypeId, from, to.getType()))
        );
    }
}
