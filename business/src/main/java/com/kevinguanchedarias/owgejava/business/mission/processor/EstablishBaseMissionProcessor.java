package com.kevinguanchedarias.owgejava.business.mission.processor;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
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
public class EstablishBaseMissionProcessor implements MissionProcessor {
    public static final String MAX_PLANETS_MESSAGE = "I18N_MAX_PLANETS_EXCEEDED";

    private final AttackMissionProcessor attackMissionProcessor;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final PlanetBo planetBo;
    private final MissionEventEmitterBo missionEventEmitterBo;

    @Override
    public boolean supports(MissionType missionType) {
        return missionType == MissionType.ESTABLISH_BASE;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public UnitMissionReportBuilder process(Mission mission, List<ObtainedUnit> involvedUnits) {
        UserStorage user = mission.getUser();
        Planet targetPlanet = mission.getTargetPlanet();
        if (attackMissionProcessor.triggerAttackIfRequired(mission, user, targetPlanet)) {
            UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(user, mission.getSourcePlanet(),
                    targetPlanet, involvedUnits);
            UserStorage planetOwner = targetPlanet.getOwner();
            boolean hasMaxPlanets = planetBo.hasMaxPlanets(user);
            if (planetOwner != null || hasMaxPlanets) {
                returnMissionRegistrationBo.registerReturnMission(mission, null);
                if (planetOwner != null) {
                    builder.withEstablishBaseInformation(false, "I18N_ALREADY_HAS_OWNER");
                } else {
                    builder.withEstablishBaseInformation(false, MAX_PLANETS_MESSAGE);
                }
            } else {
                builder.withEstablishBaseInformation(true);
                planetBo.definePlanetAsOwnedBy(user, involvedUnits, targetPlanet);
            }
            mission.setResolved(true);
            missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission);
            return builder;
        } else {
            return null;
        }
    }
}
