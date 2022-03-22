package com.kevinguanchedarias.owgejava.business.unit.util;

import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.UnitTypeRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.function.Predicate;

@Service
@AllArgsConstructor
public class UnitTypeInheritanceFinderService {
    private final UnitTypeRepository unitTypeRepository;

    public Optional<UnitType> findUnitTypeMatchingCondition(UnitType unitType, Predicate<UnitType> predicate) {
        if (predicate.test(unitType)) {
            return Optional.of(unitType);
        } else if (unitType.getParent() != null) {
            return findUnitTypeMatchingCondition(unitType.getParent(), predicate);
        } else {
            return Optional.empty();
        }
    }

    public Optional<UnitType> findUnitTypeMatchingCondition(Integer unitTypeId, Predicate<UnitType> predicate) {
        return findUnitTypeMatchingCondition(
                unitTypeRepository.findById(unitTypeId)
                        .orElseThrow(() -> new SgtBackendInvalidInputException("No unit type with id " + unitTypeId + " exists")),
                predicate
        );
    }
}
