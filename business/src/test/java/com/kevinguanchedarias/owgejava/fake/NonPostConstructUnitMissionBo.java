package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionInterceptionManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.processor.MissionProcessor;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.UnitMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetExplorationService;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@Primary
public class NonPostConstructUnitMissionBo extends UnitMissionBo {
    public NonPostConstructUnitMissionBo(
            PlanetLockUtilService planetLockUtilService, UnitMissionRegistrationBo unitMissionRegistrationBo,
            ReturnMissionRegistrationBo returnMissionRegistrationBo, PlanetRepository planetRepository,
            MissionEventEmitterBo missionEventEmitterBo, MissionInterceptionManagerBo missionInterceptionManagerBo,
            List<MissionProcessor> missionProcessors, PlanetBo planetBo, PlanetExplorationService planetExplorationService,
            ObtainedUnitRepository obtainedUnitRepository, Map<MissionType, MissionProcessor> missionProcessorMap
    ) {
        super(
                planetLockUtilService, unitMissionRegistrationBo, returnMissionRegistrationBo, planetRepository, missionEventEmitterBo,
                missionInterceptionManagerBo, missionProcessors, planetBo, planetExplorationService, obtainedUnitRepository, missionProcessorMap
        );
    }

    @Override
    public void init() {
        // Skip init
    }
}
