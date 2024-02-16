package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.UnitInMap;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.mission.MissionRegistrationUnitManagementResult;
import com.kevinguanchedarias.owgejava.pojo.storedunit.UnitWithItsStoredUnits;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
@AllArgsConstructor
public class MissionRegistrationUnitManager {
    private UnitRepository unitRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public MissionRegistrationUnitManagementResult manageUnitsRegistration(
            UnitMissionInformation targetMissionInformation,
            Map<UnitInMap, UnitWithItsStoredUnits> dbUnits,
            boolean isEnemyPlanet,
            UserStorage user,
            Mission mission
    ) {
        List<ObtainedUnit> obtainedUnits = new ArrayList<>();
        List<Mission> alteredVisibilityMissions = new ArrayList<>();

        targetMissionInformation.getInvolvedUnits().forEach(current -> {
            var processingUnit = dbUnits.get(new UnitInMap(current.getId(), current.getExpirationId()));
            var dbUnit = processingUnit.obtainedUnit();
            if (isEnemyPlanet) {
                alteredVisibilityMissions.add(dbUnit.getMission());
            }
            var currentObtainedUnit = configureObtainedUnit(current.getCount(), user, mission, dbUnit, null);
            if (!CollectionUtils.isEmpty(processingUnit.storedUnits())) {
                processingUnit.storedUnits().forEach(
                        storedUnitWithItsCount -> obtainedUnits.add(configureObtainedUnit(
                                storedUnitWithItsCount.count(), user, mission, storedUnitWithItsCount.obtainedUnit(), currentObtainedUnit
                        ))
                );
            }

            obtainedUnits.add(currentObtainedUnit);
        });
        return MissionRegistrationUnitManagementResult.builder()
                .alteredVisibilityMissions(Collections.unmodifiableList(alteredVisibilityMissions))
                .units(Collections.unmodifiableList(obtainedUnits))
                .build();
    }

    private ObtainedUnit configureObtainedUnit(
            Long count, UserStorage user, Mission mission, ObtainedUnit dbUnit, ObtainedUnit ownerUnit
    ) {
        var retVal = new ObtainedUnit();
        retVal.setMission(mission);
        retVal.setCount(count);
        retVal.setUser(user);
        retVal.setUnit(unitRepository.getReferenceById(dbUnit.getUnit().getId()));
        retVal.setExpirationId(dbUnit.getExpirationId());
        retVal.setSourcePlanet(mission.getSourcePlanet());
        retVal.setTargetPlanet(mission.getTargetPlanet());
        retVal.setOwnerUnit(ownerUnit);
        return retVal;
    }
}
