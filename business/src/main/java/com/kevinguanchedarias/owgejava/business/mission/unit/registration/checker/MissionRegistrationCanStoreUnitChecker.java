package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;


import com.kevinguanchedarias.owgejava.business.rule.UnitRuleFinderService;
import com.kevinguanchedarias.owgejava.business.rule.type.unit.UnitStoresUnitRuleTypeProviderBo;
import com.kevinguanchedarias.owgejava.entity.Rule;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import com.kevinguanchedarias.taggablecache.aspect.TaggableCacheable;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class MissionRegistrationCanStoreUnitChecker {
    private final UnitRuleFinderService unitRuleFinderService;
    private final UnitRepository unitRepository;

    @TaggableCacheable(
            tags = {
                    Rule.RULE_CACHE_TAG, Unit.UNIT_CACHE_TAG
            },
            keySuffix = "#futureOwnerId #unitIdToStore"
    )
    public void checkCanStoreUnit(Integer futureOwnerId, Integer unitIdToStore) {
        checkCanStoreUnit(
                SpringRepositoryUtil.findByIdOrDie(unitRepository, futureOwnerId),
                SpringRepositoryUtil.findByIdOrDie(unitRepository, unitIdToStore)
        );
    }

    public void checkCanStoreUnit(Unit futureOwner, Unit unitToStore) {
        if (unitRuleFinderService.findRule(UnitStoresUnitRuleTypeProviderBo.PROVIDER_ID, futureOwner, unitToStore).isEmpty()) {
            throw new SgtBackendInvalidInputException("I18N_CANT_STORE_UNIT");
        }
    }
}
