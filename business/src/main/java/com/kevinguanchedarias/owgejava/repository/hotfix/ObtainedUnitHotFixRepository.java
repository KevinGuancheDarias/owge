package com.kevinguanchedarias.owgejava.repository.hotfix;

import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class ObtainedUnitHotFixRepository {
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final EntityManager entityManager;

    @Transactional(propagation = Propagation.MANDATORY)
    public ObtainedUnit findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(
            Integer userId, Integer unitId, Long sourcePlanet
    ) {
        return handleResult(this.obtainedUnitRepository.findByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdIsNullAndMissionIsNull(
                userId, unitId, sourcePlanet
        ));
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public ObtainedUnit findOneByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdIsNullAndMissionTypeCode(
            Integer userId, Integer unitId, Long planetId, String missionTypeCode
    ) {
        return handleResult(obtainedUnitRepository.findByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdIsNullAndMissionTypeCode(
                userId, unitId, planetId, missionTypeCode
        ));
    }

    private long calculateCount(List<ObtainedUnit> obtainedUnits) {
        return obtainedUnits.stream()
                .map(ObtainedUnit::getCount)
                .reduce(0L, Long::sum);
    }

    private ObtainedUnit handleResult(List<ObtainedUnit> result) {
        if (result.size() == 1) {
            return result.getFirst();
        } else if (result.isEmpty()) {
            return null;
        } else {
            log.warn("Workaround unit duplicated units {}", result);
            var entity = result.getFirst();
            obtainedUnitRepository.deleteAll(result);
            entityManager.detach(entity);
            entity.setId(null);
            entity.setCount(calculateCount(result));
            return obtainedUnitRepository.save(entity);
        }
    }
}
