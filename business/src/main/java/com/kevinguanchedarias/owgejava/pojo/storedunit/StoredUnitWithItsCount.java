package com.kevinguanchedarias.owgejava.pojo.storedunit;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;

public record StoredUnitWithItsCount(ObtainedUnit obtainedUnit, Long count) {
}
