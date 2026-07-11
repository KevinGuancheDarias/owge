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
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import lombok.AllArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@AllArgsConstructor
public class MissionRegistrationObtainedUnitLoader {
    private final MissionRegistrationPlanetExistsChecker missionRegistrationPlanetExistsChecker;
    private final PlanetRepository planetRepository;
    private final MissionRegistrationCanDeployChecker missionRegistrationCanDeployChecker;
    private final ObtainedUnitBo obtainedUnitBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
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
            List<StoredUnitWithItsCount> storedUnits;
            if (CollectionUtils.isEmpty(current.getStoredUnits())) {
                // When relaunching from a DEPLOYED position the already-nested units (linked in the
                // database by owner_unit_id) are not present in the request, so they must be carried
                // automatically; otherwise they would be left behind referencing a mission that gets
                // deleted once the holder leaves, making them uncontrollable for the player.
                storedUnits = loadAutoCarriedStoredUnits(currentObtainedUnit, current.getCount(), deletedMissions);
            } else {
                storedUnits = current.getStoredUnits().stream()
                        .map(storedUnit -> {
                            checkRepeatedUnitAndAdd(loadedUnits, storedUnit);
                            missionRegistrationCanStoreUnitChecker.checkCanStoreUnit(current.getId(), storedUnit.getId());
                            return handleSelectedUnit(missionInformation, userId, sourcePlanetId, deletedMissions, false, storedUnit);
                        })
                        .toList();
            }
            var unitWithItsStoredUnits = new UnitWithItsStoredUnits(currentObtainedUnit.obtainedUnit(), storedUnits);
            retVal.put(
                    new UnitInMap(current.getId(), current.getExpirationId()),
                    unitWithItsStoredUnits
            );
            checkTotalHeight(currentObtainedUnit, unitWithItsStoredUnits);
        });
        missionRegistrationOrphanMissionEraser.doMarkAsDeletedTheOrphanMissions(deletedMissions);

        return retVal;
    }

    /**
     * Builds the list of units nested inside a moving holder out of the database (owner_unit_id), used
     * when the request doesn't declare them explicitly (the typical case when relaunching a mission from
     * a DEPLOYED position, where the nesting already exists and the UI offers no way to redeclare it).
     * Each nested unit is carried proportionally (floored) to the fraction of holders being moved, and is
     * subtracted from its source obtained unit just like an explicitly requested stored unit.
     */
    private List<StoredUnitWithItsCount> loadAutoCarriedStoredUnits(
            StoredUnitWithItsCount holder, Long movedCount, Set<Mission> deletedMissions
    ) {
        var holderObtainedUnit = holder.obtainedUnit();
        var holderMission = holderObtainedUnit.getMission();
        if (holderMission == null
                || !MissionType.DEPLOYED.toString().equals(holderMission.getType().getCode())) {
            return List.of();
        }
        var holderTotalCount = holderObtainedUnit.getCount();
        List<StoredUnitWithItsCount> retVal = new ArrayList<>();
        obtainedUnitRepository.findByOwnerUnitId(holderObtainedUnit.getId()).forEach(storedUnit -> {
            var carriedCount = storedUnit.getCount() * movedCount / holderTotalCount;
            if (carriedCount > 0) {
                var unitAfterSubtraction = obtainedUnitBo.saveWithSubtraction(storedUnit, carriedCount, false);
                if (unitAfterSubtraction == null && storedUnit.getMission() != null
                        && storedUnit.getMission().getType().getCode().equals(MissionType.DEPLOYED.toString())) {
                    deletedMissions.add(storedUnit.getMission());
                }
                retVal.add(new StoredUnitWithItsCount(storedUnit, carriedCount));
            }
        });
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
