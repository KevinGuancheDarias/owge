package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationCanDeployChecker;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationCanStoreUnitChecker;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationPlanetExistsChecker;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.SelectedUnit;
import com.kevinguanchedarias.owgejava.pojo.UnitInMap;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.storedunit.StoredUnitWithItsCount;
import com.kevinguanchedarias.owgejava.pojo.storedunit.UnitWithItsStoredUnits;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
    private final MissionRegistrationCanStoreUnitChecker missionRegistrationCanStoreUnitChecker;

    @Transactional(propagation = Propagation.MANDATORY)
    public Map<UnitInMap, UnitWithItsStoredUnits> checkAndLoadObtainedUnits(UnitMissionInformation missionInformation) {
        Map<UnitInMap, UnitWithItsStoredUnits> retVal = new HashMap<>();
        Set<UnitInMap> loadedUnits = new HashSet<>();
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
            checkRepeatedUnitAndAdd(loadedUnits, current);
            var currentObtainedUnit = handleSelectedUnit(missionInformation, userId, sourcePlanetId, deletedMissions, true, current);
            var unitWithItsStoredUnits = new UnitWithItsStoredUnits(currentObtainedUnit.obtainedUnit(), CollectionUtils.emptyIfNull(current.getStoredUnits()).stream()
                    .map(
                            storedUnit -> {
                                checkRepeatedUnitAndAdd(loadedUnits, storedUnit);
                                missionRegistrationCanStoreUnitChecker.checkCanStoreUnit(current.getId(), storedUnit.getId());
                                return handleSelectedUnit(missionInformation, userId, sourcePlanetId, deletedMissions, false, storedUnit);
                            }
                    )
                    .toList()
            );
            retVal.put(
                    new UnitInMap(current.getId(), current.getExpirationId()),
                    unitWithItsStoredUnits
            );
            checkTotalHeight(currentObtainedUnit, unitWithItsStoredUnits);
        });
        missionRegistrationOrphanMissionEraser.doMarkAsDeletedTheOrphanMissions(deletedMissions);

        return retVal;
    }

    private void checkTotalHeight(StoredUnitWithItsCount storedUnitWithItsCount, UnitWithItsStoredUnits unitWithItsStoredUnits) {
        var maxSupportedWeight = storedUnitWithItsCount.count() * ObjectUtils.firstNonNull(
                storedUnitWithItsCount.obtainedUnit().getUnit().getStorageCapacity(), 0L
        );
        var storedWeight = unitWithItsStoredUnits.storedUnits().stream()
                .map(currentStoredUnit -> currentStoredUnit.obtainedUnit().getUnit().getStoredWeight() * currentStoredUnit.count())
                .reduce(Long::sum)
                .orElse(0L);
        if (storedWeight > maxSupportedWeight) {
            throw new SgtBackendInvalidInputException("I18N_ERR_MAX_WEIGHT_OVERPASSED");
        }
    }

    private void checkRepeatedUnitAndAdd(Set<UnitInMap> loadedUnits, SelectedUnit selectedUnit) {
        var unitInMap = new UnitInMap(selectedUnit.getId(), selectedUnit.getExpirationId());
        if (loadedUnits.contains(unitInMap)) {
            throw new SgtBackendInvalidInputException("I18N_ERR_REPEATED_UNIT");
        }
        loadedUnits.add(unitInMap);
    }

    private StoredUnitWithItsCount handleSelectedUnit(
            UnitMissionInformation missionInformation,
            Integer userId,
            Long sourcePlanetId,
            Set<Mission> deletedMissions,
            boolean checkCanDeploy,
            SelectedUnit current
    ) {
        if (current.getCount() == null) {
            throw new SgtBackendInvalidInputException("No count was specified for unit " + current.getId());
        }
        var currentObtainedUnit = obtainedUnitBo.findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(
                missionInformation.getUserId(), current.getId(), sourcePlanetId, current.getExpirationId(),
                !planetRepository.isOfUserProperty(userId, sourcePlanetId));
        if (checkCanDeploy) {
            missionRegistrationCanDeployChecker.checkUnitCanDeploy(currentObtainedUnit, missionInformation);
        }
        var unitAfterSubtraction = obtainedUnitBo.saveWithSubtraction(currentObtainedUnit,
                current.getCount(), false);
        if (unitAfterSubtraction == null && currentObtainedUnit.getMission() != null
                && currentObtainedUnit.getMission().getType().getCode().equals(MissionType.DEPLOYED.toString())) {
            deletedMissions.add(currentObtainedUnit.getMission());
        }
        return new StoredUnitWithItsCount(currentObtainedUnit, current.getCount());
    }
}
