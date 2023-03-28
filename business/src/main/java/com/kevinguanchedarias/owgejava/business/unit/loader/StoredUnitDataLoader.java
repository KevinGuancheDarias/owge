package com.kevinguanchedarias.owgejava.business.unit.loader;

import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.util.DtoUtilService;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class StoredUnitDataLoader implements UnitDataLoader {

    private final List<UnitDataLoader> unitDataLoaders;
    private final DtoUtilService dtoUtilService;

    private final ObtainedUnitRepository obtainedUnitRepository;

    @Override
    @Transactional
    public void addInformationToDto(ObtainedUnit obtainedUnit, ObtainedUnitDto targetDto) {
        targetDto.setStoredUnits(CollectionUtils.emptyIfNull(obtainedUnitRepository.findByOwnerUnitId(obtainedUnit.getId())).stream()
                .map(this::mapStoredUnit)
                .toList()
        );
    }

    private ObtainedUnitDto mapStoredUnit(ObtainedUnit obtainedUnit) {
        var retVal = dtoUtilService.dtoFromEntity(ObtainedUnitDto.class, obtainedUnit);
        unitDataLoaders.forEach(unitDataLoader -> unitDataLoader.addInformationToDto(obtainedUnit, retVal));
        return retVal;
    }
}
