package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.business.rule.timespecial.ActiveTimeSpecialRuleFinderService;
import com.kevinguanchedarias.owgejava.business.rule.type.timespecial.TimeSpecialIsActiveHideUnitsTypeProviderBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.ActiveTimeSpecial;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.IntStream;

import static com.kevinguanchedarias.owgejava.entity.Rule.RULE_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.entity.Unit.UNIT_CACHE_TAG;
import static com.kevinguanchedarias.owgejava.entity.UnitType.UNIT_TYPE_CACHE_TAG;

@Service
@AllArgsConstructor
@Slf4j
public class HiddenUnitBo {
    private final TaggableCacheManager taggableCacheManager;
    private final ActiveTimeSpecialRuleFinderService activeTimeSpecialRuleFinderService;

    public void defineHidden(List<ObtainedUnit> data, List<ObtainedUnitDto> dtoVersion) {
        IntStream.range(0, dtoVersion.size())
                .forEach(i -> dtoVersion.get(i).getUnit().setIsInvisible(isHiddenUnit(data.get(i).getUser(), data.get(i).getUnit())));
    }

    public boolean isHiddenUnit(UserStorage user, Unit unit) {
        var cacheKey = getClass().getName() + "_defineHidden() " + user.getId() + "_" + unit.getId();
        if (taggableCacheManager.keyExists(cacheKey)) {
            return taggableCacheManager.findByKey(cacheKey);
        } else {
            var compute = isHiddenUnitInternal(user, unit);
            taggableCacheManager.saveEntry(cacheKey, compute, List.of(
                    RULE_CACHE_TAG,
                    ActiveTimeSpecial.ACTIVE_TIME_SPECIAL_BY_USER_CACHE_TAG + ":" + user.getId(),
                    UNIT_TYPE_CACHE_TAG,
                    UNIT_CACHE_TAG
            ));
            return compute;
        }
    }

    private boolean isHiddenUnitInternal(UserStorage user, Unit unit) {
        return Boolean.TRUE.equals(unit.getIsInvisible()) || activeTimeSpecialRuleFinderService.existsRuleMatchingUnitDestination(user, unit, TimeSpecialIsActiveHideUnitsTypeProviderBo.TIME_SPECIAL_IS_ACTIVE_HIDE_UNITS_ID);
    }
}
