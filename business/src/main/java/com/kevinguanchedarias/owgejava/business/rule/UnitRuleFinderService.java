package com.kevinguanchedarias.owgejava.business.rule;

import com.kevinguanchedarias.owgejava.business.rule.itemtype.UnitRuleItemTypeProviderBo;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.repository.RuleRepository;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static java.util.Optional.ofNullable;

@Service
@AllArgsConstructor
public class UnitRuleFinderService {
    public static final String UNIT_TYPE = "UNIT_TYPE";
    private RuleRepository ruleRepository;

    @TaggableCacheable(
            tags = {
                    Rule.RULE_CACHE_TAG, Unit.UNIT_CACHE_TAG, UnitType.UNIT_TYPE_CACHE_TAG
            },
            keySuffix = "#ruleTypeId #from.id #to.id"
    )
    public Optional<Rule> findRule(String ruleTypeId, Unit from, Unit to) {
        var fromUnitType = from.getType();
        var toUnitType = to.getType();
        return unitVsUnitOptional(ruleTypeId, from, to)
                .or(() -> unitVsUnitTypeOptional(ruleTypeId, from, toUnitType))
                .or(() -> unitTypeVsUnitOptional(ruleTypeId, fromUnitType, to))
                .or(() -> unitTypeVsUnitTypeOptional(ruleTypeId, fromUnitType, toUnitType));
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
}
