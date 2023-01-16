package com.kevinguanchedarias.owgejava.business.util;

import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitImprovementCalculationService;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collection;

@Service
@AllArgsConstructor
public class UnitImprovementUtilService {
    private final ImprovementBo improvementBo;
    private final ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;

    public void maybeTriggerClearImprovement(UserStorage user, Collection<ObtainedUnit> obtainedUnits) {
        if (obtainedUnits.stream().anyMatch(ou -> ou.getUnit().getImprovement() != null)) {
            improvementBo.clearSourceCache(user, obtainedUnitImprovementCalculationService);
        }
    }
}
