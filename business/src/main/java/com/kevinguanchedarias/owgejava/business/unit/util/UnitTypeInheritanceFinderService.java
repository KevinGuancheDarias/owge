package com.kevinguanchedarias.owgejava.business.unit.util;

import com.kevinguanchedarias.owgejava.entity.UnitType;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Predicate;

@Service
public class UnitTypeInheritanceFinderService {
    public Optional<UnitType> findUnitTypeMatchingCondition(UnitType unitType, Predicate<UnitType> predicate) {
        if (predicate.test(unitType)) {
            return Optional.of(unitType);
        } else if (unitType.getParent() != null) {
            return findUnitTypeMatchingCondition(unitType.getParent(), predicate);
        } else {
            return Optional.empty();
        }
    }
}
