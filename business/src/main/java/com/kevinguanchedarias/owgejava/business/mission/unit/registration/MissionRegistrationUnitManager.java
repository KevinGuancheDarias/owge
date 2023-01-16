package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.UnitInMap;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.mission.MissionRegistrationUnitManagementResult;
import com.kevinguanchedarias.owgejava.repository.UnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
            Map<UnitInMap, ObtainedUnit> dbUnits,
            boolean isEnemyPlanet,
            UserStorage user,
            Mission mission
    ) {
        List<ObtainedUnit> obtainedUnits = new ArrayList<>();
        List<Mission> alteredVisibilityMissions = new ArrayList<>();

        targetMissionInformation.getInvolvedUnits().forEach(current -> {
            var currentObtainedUnit = new ObtainedUnit();
            var dbUnit = dbUnits.get(new UnitInMap(current.getId(), current.getExpirationId()));
            if (isEnemyPlanet) {
                alteredVisibilityMissions.add(dbUnit.getMission());
            }
            currentObtainedUnit.setMission(mission);
            var firstDeploymentMission = dbUnit.getFirstDeploymentMission();
            currentObtainedUnit.setFirstDeploymentMission(firstDeploymentMission);
            currentObtainedUnit.setCount(current.getCount());
            currentObtainedUnit.setUser(user);
            currentObtainedUnit.setUnit(unitRepository.getById(current.getId()));
            currentObtainedUnit.setExpirationId(dbUnit.getExpirationId());
            currentObtainedUnit.setSourcePlanet(firstDeploymentMission == null ? mission.getSourcePlanet()
                    : firstDeploymentMission.getSourcePlanet());
            currentObtainedUnit.setTargetPlanet(mission.getTargetPlanet());
            obtainedUnits.add(currentObtainedUnit);
        });
        return MissionRegistrationUnitManagementResult.builder()
                .alteredVisibilityMissions(Collections.unmodifiableList(alteredVisibilityMissions))
                .units(Collections.unmodifiableList(obtainedUnits))
                .build();
    }
}
