package com.kevinguanchedarias.owgejava.business.mission.checker;

import com.kevinguanchedarias.owgejava.business.ObjectRelationBo;
import com.kevinguanchedarias.owgejava.business.UnlockedRelationBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@AllArgsConstructor
@Slf4j
public class CrossGalaxyMissionChecker {
    private final EntityCanDoMissionChecker entityCanDoMissionChecker;
    private final ObjectRelationBo objectRelationBo;
    private final UnlockedRelationBo unlockedRelationBo;

    public void checkCrossGalaxy(MissionType missionType, List<ObtainedUnit> units, Planet sourcePlanet,
                                 Planet targetPlanet) {
        var user = units.get(0).getUser();
        if (!sourcePlanet.getGalaxy().getId().equals(targetPlanet.getGalaxy().getId())) {
            units
                    .stream().filter(obtainedUnit -> obtainedUnit.getOwnerUnit() == null)
                    .forEach(unit -> {
                        var speedGroup = unit.getUnit().getSpeedImpactGroup();
                        speedGroup = speedGroup == null ? unit.getUnit().getType().getSpeedImpactGroup() : speedGroup;
                        doCheckSpeedImpactIfNotNull(speedGroup, user, targetPlanet, missionType);
                    });
        }
    }

    private void doCheckSpeedImpactIfNotNull(
            SpeedImpactGroup speedGroup,
            UserStorage user,
            Planet targetPlanet,
            MissionType missionType
    ) {
        if (speedGroup != null) {
            if (!entityCanDoMissionChecker.canDoMission(user, targetPlanet, speedGroup, missionType)) {
                throw new SgtBackendInvalidInputException(
                        "This speed group doesn't support this mission outside of the galaxy");
            }
            var relation = objectRelationBo
                    .findOne(ObjectEnum.SPEED_IMPACT_GROUP, speedGroup.getId());
            if (relation == null) {
                log.warn("Unexpected null objectRelation for SPEED_IMPACT_GROUP with id " + speedGroup.getId());
            } else if (!unlockedRelationBo.isUnlocked(user, relation)) {
                throw new SgtBackendInvalidInputException(
                        "Don't try it.... you can't do cross galaxy missions, and you know it");
            }
        }
    }
}
