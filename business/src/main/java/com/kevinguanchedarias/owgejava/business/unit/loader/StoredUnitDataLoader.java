package com.kevinguanchedarias.owgejava.business.unit.loader;

import com.kevinguanchedarias.owgejava.business.unit.StoredUnitBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class StoredUnitDataLoader implements UnitDataLoader {

    private final StoredUnitBo storedUnitBo;

    @Override
    public void addInformationToDto(ObtainedUnit obtainedUnit, ObtainedUnitDto targetDto) {
        targetDto.setStoredUnits(storedUnitBo.findStoredUnits(obtainedUnit));
    }
}
