package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.unit.loader.UnitDataLoader;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import org.springframework.stereotype.Service;

@Service
public class FakeUnitDataLoader implements UnitDataLoader {
    @Override
    public void addInformationToDto(ObtainedUnit obtainedUnit, ObtainedUnitDto targetDto) {
        // It's fake
    }
}
