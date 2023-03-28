package com.kevinguanchedarias.owgejava.pojo.storedunit;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;

import java.util.List;

public record UnitWithItsStoredUnits(
        ObtainedUnit obtainedUnit, List<StoredUnitWithItsCount> storedUnits
) {
}
