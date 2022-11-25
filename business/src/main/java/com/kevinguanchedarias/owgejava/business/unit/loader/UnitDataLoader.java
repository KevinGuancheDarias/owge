package com.kevinguanchedarias.owgejava.business.unit.loader;

import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;

public interface UnitDataLoader {
    /**
     * Allows to set extra information in the DTO
     */
    void addInformationToDto(ObtainedUnit obtainedUnit, ObtainedUnitDto targetDto);
}
