package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.business.mission.checker.EntityCanDoMissionChecker;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.SpeedImpactGroupFinderBo;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MissionRegistrationUnitTypeChecker {
    private final UnitTypeBo unitTypeBo;
    private final SpeedImpactGroupFinderBo speedImpactGroupFinderBo;
    private final EntityCanDoMissionChecker entityCanDoMissionChecker;

    public void checkUnitsCanDoMission(
            List<ObtainedUnit> obtainedUnits,
            UserStorage user,
            Mission mission,
            MissionType missionType
    ) {
        var involvedUnitTypes = obtainedUnits.stream().map(current -> current.getUnit().getType())
                .toList();
        var involvedSpeedImpactGroupsStream = obtainedUnits.stream()
                .filter(current -> current.getOwnerUnit() == null)
                .map(ObtainedUnit::getUnit)
                .map(current -> Pair.of(speedImpactGroupFinderBo.findApplicable(user, current), current))
                .map(current -> Pair.of((EntityWithMissionLimitation<Integer>) current.getLeft(), current.getRight()));
        if (!unitTypeBo.canDoMission(user, mission.getTargetPlanet(), involvedUnitTypes, missionType)) {
            throw new SgtBackendInvalidInputException(
                    "At least one unit type doesn't support the specified mission.... don't try it dear hacker, you can't defeat the system, but don't worry nobody can");
        }
        var unitsThatCannotGoToMission = involvedSpeedImpactGroupsStream
                .filter(current -> !entityCanDoMissionChecker.canDoMission(user, mission.getTargetPlanet(), current.getLeft(), missionType))
                .map(current -> current.getRight().getName() + "(" + ((SpeedImpactGroup) current.getLeft()).getName() + ")")
                .toList();
        if (!unitsThatCannotGoToMission.isEmpty()) {
            throw new SgtBackendInvalidInputException(
                    "At least one unit speed group doesn't support the specified mission : " + String.join(",", unitsThatCannotGoToMission)
            );
        }
    }
}
