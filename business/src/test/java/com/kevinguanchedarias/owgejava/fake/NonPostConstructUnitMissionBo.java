package com.kevinguanchedarias.owgejava.fake;

import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionBaseService;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionInterceptionManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.processor.MissionProcessor;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.UnitMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetExplorationService;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@Primary
public class NonPostConstructUnitMissionBo extends UnitMissionBo {

    public NonPostConstructUnitMissionBo(
            PlanetLockUtilService planetLockUtilService, UnitMissionRegistrationBo unitMissionRegistrationBo,
            ReturnMissionRegistrationBo returnMissionRegistrationBo, PlanetRepository planetRepository,
            MissionEventEmitterBo missionEventEmitterBo, MissionInterceptionManagerBo missionInterceptionManagerBo,
            List<MissionProcessor> missionProcessors, PlanetBo planetBo, PlanetExplorationService planetExplorationService,
            MissionRepository missionRepository, UserStorageBo userStorageBo, ExceptionUtilService exceptionUtilService,
            MissionReportManagerBo missionReportManagerBo, MissionBaseService missionBaseService
    ) {
        super(planetLockUtilService, unitMissionRegistrationBo, returnMissionRegistrationBo, planetRepository, missionEventEmitterBo, missionInterceptionManagerBo, missionProcessors, planetBo, planetExplorationService, missionRepository, userStorageBo, exceptionUtilService, missionReportManagerBo, missionBaseService, null);
    }

    @Override
    public void init() {
        // Skip init
    }

    public void invokeRealInit() {
        super.init();
    }
}
