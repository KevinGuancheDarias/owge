package com.kevinguanchedarias.owgejava.business.unit;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.projection.ObtainedUnitBasicInfoProjection;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.jdbc.StoredUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class StoredUnitBo {
    private final StoredUnitRepository storedUnitRepository;
    private final ObtainedUnitRepository obtainedUnitRepository;

    public List<ObtainedUnitBasicInfoProjection> findStoredUnits(ObtainedUnit obtainedUnit) {
        return storedUnitRepository.findByOwnerObtainedUnitId(obtainedUnit)
                .stream()
                .map(storedUnit -> obtainedUnitRepository.findBaseInfo(storedUnit.getTargetObtainedUnitId()))
                .toList();
    }
}
