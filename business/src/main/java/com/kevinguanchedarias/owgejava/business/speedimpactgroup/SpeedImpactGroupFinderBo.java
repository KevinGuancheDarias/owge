package com.kevinguanchedarias.owgejava.business.speedimpactgroup;

import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.unit.util.UnitTypeInheritanceFinderService;
import com.kevinguanchedarias.owgejava.dto.rule.RuleDto;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.owgejava.repository.SpeedImpactGroupRepository;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.business.ActiveTimeSpecialBo.ACTIVE_TIME_SPECIAL_CACHE_TAG_BY_USER;
import static com.kevinguanchedarias.owgejava.business.SpeedImpactGroupBo.SPEED_IMPACT_GROUP_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.business.UnitBo.UNIT_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.business.UnitTypeBo.UNIT_TYPE_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.business.rule.RuleBo.RULE_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveSwapSpeedImpactGroupProviderBo.TIME_SPECIAL_IS_ACTIVE_SWAP_SPEED_IMPACT_GROUP_ID;

@Service
@AllArgsConstructor
public class SpeedImpactGroupFinderBo {
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final RuleBo ruleBo;
    private final SpeedImpactGroupRepository speedImpactGroupRepository;
    private final UnitTypeInheritanceFinderService unitTypeInheritanceFinderService;

    @TaggableCacheable(tags = {
            UNIT_CACHE_TAG + "#:unit.id",
            UNIT_TYPE_CACHE_TAG
    })
    public SpeedImpactGroup findHisOrInherited(Unit unit) {
        if (unit.getSpeedImpactGroup() != null) {
            return unit.getSpeedImpactGroup();
        } else {
            return unitTypeInheritanceFinderService.findUnitTypeMatchingCondition(
                    unit.getType(),
                    unitType -> unitType.getSpeedImpactGroup() != null
            ).map(UnitType::getSpeedImpactGroup).orElse(null);
        }
    }

    @TaggableCacheable(
            tags = {
                    UNIT_CACHE_TAG + ":#unit.id",
                    UNIT_TYPE_CACHE_TAG,
                    ACTIVE_TIME_SPECIAL_CACHE_TAG_BY_USER + ":#user.id",
                    RULE_CACHE_TAG,
                    SPEED_IMPACT_GROUP_CACHE_TAG
            },
            keySuffix = "#user.id_#unit.id"
    )
    public SpeedImpactGroup findApplicable(UserStorage user, Unit unit) {
        return activeTimeSpecialRepository.findByUserIdAndState(user.getId(), TimeSpecialStateEnum.ACTIVE).stream()
                .flatMap(activeTimeSpecial ->
                        ruleBo.findByOriginTypeAndOriginId(
                                ObjectEnum.TIME_SPECIAL.name(), activeTimeSpecial.getTimeSpecial().getId().longValue()
                        ).stream()
                ).filter(ruleDto -> ruleBo.isWantedType(ruleDto, TIME_SPECIAL_IS_ACTIVE_SWAP_SPEED_IMPACT_GROUP_ID))
                .filter(ruleDto -> !ruleDto.getExtraArgs().isEmpty())
                .findFirst()
                .flatMap(this::ruleToEntity)
                .orElseGet(() -> findHisOrInherited(unit));
    }

    private Optional<SpeedImpactGroup> ruleToEntity(RuleDto ruleDto) {
        return speedImpactGroupRepository.findById(Integer.parseInt((String) ruleDto.getExtraArgs().get(0)));
    }

}
