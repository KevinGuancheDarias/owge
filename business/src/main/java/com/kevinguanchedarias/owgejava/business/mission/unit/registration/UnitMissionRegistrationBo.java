package com.kevinguanchedarias.owgejava.business.mission.unit.registration;

import com.kevinguanchedarias.owgejava.business.MissionSchedulerService;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTimeManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.checker.CrossGalaxyMissionChecker;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationCanDeployChecker;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationUnitTypeChecker;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.checker.MissionRegistrationUserExistsChecker;
import com.kevinguanchedarias.owgejava.business.planet.PlanetUtilService;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class UnitMissionRegistrationBo {
    private final MissionRegistrationObtainedUnitLoader missionRegistrationObtainedUnitLoader;
    private final MissionRegistrationCanDeployChecker missionRegistrationCanDeployChecker;
    private final MissionRegistrationUserExistsChecker missionRegistrationUserExistsChecker;
    private final MissionRepository missionRepository;
    private final MissionRegistrationPreparer missionRegistrationPreparer;
    private final PlanetUtilService planetUtilService;
    private final MissionRegistrationAuditor missionRegistrationAuditor;
    private final MissionRegistrationUnitManager missionRegistrationUnitManager;
    private final MissionRegistrationUnitTypeChecker missionRegistrationUnitTypeChecker;
    private final CrossGalaxyMissionChecker crossGalaxyMissionChecker;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final MissionTimeManagerBo missionTimeManagerBo;
    private final MissionRegistrationInvisibleManager missionRegistrationInvisibleManager;
    private final MissionSchedulerService missionSchedulerService;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;

    public void doCommonMissionRegister(
            UnitMissionInformation missionInformation,
            UnitMissionInformation targetMissionInformation,
            MissionType missionType,
            UserStorage user,
            boolean isDeployMission
    ) {
        missionRegistrationUserExistsChecker.checkUserExists(user.getId());
        missionRegistrationCanDeployChecker.checkDeployedAllowed(missionType);
        var dbUnits = missionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits(missionInformation);
        var mission = missionRepository.saveAndFlush((missionRegistrationPreparer.prepareMission(targetMissionInformation, missionType)));
        boolean isEnemyPlanet = planetUtilService.isEnemyPlanet(user, mission.getSourcePlanet());
        missionRegistrationAuditor.auditMissionRegistration(mission, isDeployMission);
        var unitManagementResult = missionRegistrationUnitManager.manageUnitsRegistration(
                targetMissionInformation, dbUnits, isEnemyPlanet, user, mission
        );
        missionRegistrationUnitTypeChecker.checkUnitsCanDoMission(unitManagementResult.getUnits(), user, mission, missionType);
        crossGalaxyMissionChecker.checkCrossGalaxy(
                missionType, unitManagementResult.getUnits(), mission.getSourcePlanet(), mission.getTargetPlanet()
        );
        obtainedUnitRepository.saveAll(unitManagementResult.getUnits());
        missionTimeManagerBo.handleMissionTimeCalculation(unitManagementResult.getUnits(), mission, missionType);
        missionTimeManagerBo.handleCustomDuration(mission, missionInformation.getWantedTime());
        missionRegistrationInvisibleManager.handleDefineMissionAsInvisible(mission, unitManagementResult.getUnits());
        missionRepository.save(mission);
        missionSchedulerService.scheduleMission(mission);
        missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission);
        if (user.equals(mission.getSourcePlanet().getOwner())) {
            obtainedUnitEventEmitter.emitObtainedUnitsAfterCommit(user);
        }
        if (isEnemyPlanet) {
            missionRegistrationInvisibleManager.maybeUpdateMissionsVisibility(unitManagementResult.getAlteredVisibilityMissions());
            missionEventEmitterBo.emitEnemyMissionsChange(mission.getSourcePlanet().getOwner());
        }
    }
}
