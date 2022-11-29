package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetExplorationService;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@AllArgsConstructor
public class ExploreMissionProcessor implements MissionProcessor {
    private final PlanetExplorationService planetExplorationService;
    private final ObtainedUnitBo obtainedUnitBo;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;

    @Override
    public boolean supports(MissionType missionType) {
        return missionType == MissionType.EXPLORE;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UnitMissionReportBuilder process(Mission mission, List<ObtainedUnit> involvedUnits) {
        UserStorage user = mission.getUser();
        Planet targetPlanet = mission.getTargetPlanet();
        if (!planetExplorationService.isExplored(user, targetPlanet)) {
            planetExplorationService.defineAsExplored(user, targetPlanet);
        }
        List<ObtainedUnitDto> unitsInPlanet = obtainedUnitBo.explorePlanetUnits(mission, targetPlanet);
        returnMissionRegistrationBo.registerReturnMission(mission, null);
        UnitMissionReportBuilder builder = UnitMissionReportBuilder
                .create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
                .withExploredInformation(unitsInPlanet);
        mission.setResolved(true);
        return builder;
    }
}
