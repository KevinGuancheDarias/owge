package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.business.rule.RuleBo;
import com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveHideUnitsTypeProviderBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.enumerations.TimeSpecialStateEnum;
import com.kevinguanchedarias.owgejava.repository.ActiveTimeSpecialRepository;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

import static com.kevinguanchedarias.owgejava.business.ActiveTimeSpecialBo.ACTIVE_TIME_SPECIAL_CACHE_TAG_BY_USER;
import static com.kevinguanchedarias.owgejava.business.UnitBo.UNIT_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.business.UnitTypeBo.UNIT_TYPE_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.business.rule.RuleBo.RULE_CACHE_TAG;

@Service
@AllArgsConstructor
@Slf4j
public class HiddenUnitBo {
    private final ActiveTimeSpecialRepository activeTimeSpecialRepository;
    private final RuleBo ruleBo;
    private final TaggableCacheManager taggableCacheManager;

    public void defineHidden(List<ObtainedUnit> data, List<ObtainedUnitDto> dtoVersion) {
        IntStream.range(0, dtoVersion.size()).forEach(i -> dtoVersion.get(i).getUnit().setIsInvisible(isHiddenUnit(data.get(i))));
    }

    public boolean isHiddenUnit(ObtainedUnit obtainedUnit) {
        var cacheKey = getClass().getName() + "_defineHidden() " + obtainedUnit.hashCode();
        if (taggableCacheManager.keyExists(cacheKey)) {
            return (boolean) taggableCacheManager.findByKey(cacheKey);
        } else {
            var compute = isHiddenUnitInternal(obtainedUnit);
            taggableCacheManager.saveEntry(cacheKey, compute, List.of(
                    RULE_CACHE_TAG,
                    ACTIVE_TIME_SPECIAL_CACHE_TAG_BY_USER + ":" + obtainedUnit.getUser().getId(),
                    UNIT_TYPE_CACHE_TAG,
                    UNIT_CACHE_TAG
            ));
            return compute;
        }
    }

    private boolean isHiddenUnitInternal(ObtainedUnit obtainedUnit) {
        if (Boolean.TRUE.equals(obtainedUnit.getUnit().getIsInvisible())) {
            return true;
        } else {
            var user = obtainedUnit.getUser();
            var activeTimeSpecials = activeTimeSpecialRepository.findByUserIdAndState(user.getId(), TimeSpecialStateEnum.ACTIVE);

            return activeTimeSpecials.stream()
                    .flatMap(activeTimeSpecial ->
                            ruleBo.findByOriginTypeAndOriginId(
                                    ObjectEnum.TIME_SPECIAL.name(), activeTimeSpecial.getTimeSpecial().getId().longValue()
                            ).stream()
                    )
                    .filter(ruleDto -> ruleBo.isWantedType(ruleDto, TimeSpecialIsActiveHideUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID))
                    .anyMatch(ruleDto -> ruleBo.isWantedUnitDestination(ruleDto, obtainedUnit.getUnit()));
        }
    }


}
