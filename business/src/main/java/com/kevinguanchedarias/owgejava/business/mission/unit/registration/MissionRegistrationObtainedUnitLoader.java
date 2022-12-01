package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationCanDeployChecker;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationPlanetExistsChecker;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Service
@AllArgsConstructor
public class MissionRegistrationObtainedUnitLoader {
    private final MissionRegistrationPlanetExistsChecker missionRegistrationPlanetExistsChecker;
    private final PlanetRepository planetRepository;
    private final MissionRegistrationCanDeployChecker missionRegistrationCanDeployChecker;
    private final ObtainedUnitBo obtainedUnitBo;
    private final MissionRegistrationOrphanMissionEraser missionRegistrationOrphanMissionEraser;

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<Integer, ObtainedUnit> checkAndLoadObtainedUnits(UnitMissionInformation missionInformation) {
        Map<Integer, ObtainedUnit> retVal = new HashMap<>();
        var userId = missionInformation.getUserId();
        var sourcePlanetId = missionInformation.getSourcePlanetId();
        var targetPlanetId = missionInformation.getTargetPlanetId();
        missionRegistrationPlanetExistsChecker.checkPlanetExists(sourcePlanetId);
        missionRegistrationPlanetExistsChecker.checkPlanetExists(targetPlanetId);
        Set<Mission> deletedMissions = new HashSet<>();
        if (CollectionUtils.isEmpty(missionInformation.getInvolvedUnits())) {
            throw new SgtBackendInvalidInputException("involvedUnits can't be empty");
        }
        missionInformation.getInvolvedUnits().forEach(current -> {
            if (current.getCount() == null) {
                throw new SgtBackendInvalidInputException("No count was specified for unit " + current.getId());
            }
            var currentObtainedUnit = obtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(
                    missionInformation.getUserId(), current.getId(), sourcePlanetId, current.getExpirationId(),
                    !planetRepository.isOfUserProperty(userId, sourcePlanetId));
            missionRegistrationCanDeployChecker.checkUnitCanDeploy(currentObtainedUnit, missionInformation);
            var unitAfterSubtraction = obtainedUnitBo.saveWithSubtraction(currentObtainedUnit,
                    current.getCount(), false);
            if (unitAfterSubtraction == null && currentObtainedUnit.getMission() != null
                    && currentObtainedUnit.getMission().getType().getCode().equals(MissionType.DEPLOYED.toString())) {
                deletedMissions.add(currentObtainedUnit.getMission());
            }
            retVal.put(current.getId(), currentObtainedUnit);
        });
        missionRegistrationOrphanMissionEraser.doMarkAsDeletedOrphanMissions(deletedMissions);

        return retVal;
    }
}
