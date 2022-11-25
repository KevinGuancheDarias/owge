package com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker;

import com.kevinguanchedarias.owgejava.business.UnitTypeBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
public class MissionRegistrationUnitTypeChecker {
    private final UnitTypeBo unitTypeBo;

    public void checkUnitsCanDoMission(
            List<ObtainedUnit> obtainedUnits,
            UserStorage user,
            Mission mission,
            MissionType missionType
    ) {
        List<UnitType> involvedUnitTypes = obtainedUnits.stream().map(current -> current.getUnit().getType())
                .toList();
        if (!unitTypeBo.canDoMission(user, mission.getTargetPlanet(), involvedUnitTypes, missionType)) {
            throw new SgtBackendInvalidInputException(
                    "At least one unit type doesn't support the specified mission.... don't try it dear hacker, you can't defeat the system, but don't worry nobody can");
        }
    }
}
