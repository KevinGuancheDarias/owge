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
import com.kevinguanchedarias.owgejava.pojo.UnitInMap;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.mission.MissionRegistrationUnitManagementResult;
import com.kevinguanchedarias.owgejava.pojo.storedunit.UnitWithItsStoredUnits;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenGatherMission;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UnitMock.UNIT_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = UnitMissionRegistrationBo.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        MissionRegistrationObtainedUnitLoader.class,
        MissionRegistrationCanDeployChecker.class,
        MissionRegistrationUserExistsChecker.class,
        MissionRepository.class,
        MissionRegistrationPreparer.class,
        PlanetUtilService.class,
        MissionRegistrationAuditor.class,
        MissionRegistrationUnitManager.class,
        MissionRegistrationUnitTypeChecker.class,
        CrossGalaxyMissionChecker.class,
        ObtainedUnitRepository.class,
        MissionTimeManagerBo.class,
        MissionRegistrationInvisibleManager.class,
        MissionSchedulerService.class,
        MissionEventEmitterBo.class,
        ObtainedUnitEventEmitter.class
})
class UnitMissionRegistrationBoTest {
    private final UnitMissionRegistrationBo unitMissionRegistrationBo;
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

    @Autowired
    UnitMissionRegistrationBoTest(
            UnitMissionRegistrationBo unitMissionRegistrationBo,
            MissionRegistrationObtainedUnitLoader missionRegistrationObtainedUnitLoader,
            MissionRegistrationCanDeployChecker missionRegistrationCanDeployChecker,
            MissionRegistrationUserExistsChecker missionRegistrationUserExistsChecker,
            MissionRepository missionRepository,
            MissionRegistrationPreparer missionRegistrationPreparer,
            PlanetUtilService planetUtilService,
            MissionRegistrationAuditor missionRegistrationAuditor,
            MissionRegistrationUnitManager missionRegistrationUnitManager,
            MissionRegistrationUnitTypeChecker missionRegistrationUnitTypeChecker,
            CrossGalaxyMissionChecker crossGalaxyMissionChecker,
            ObtainedUnitRepository obtainedUnitRepository,
            MissionTimeManagerBo missionTimeManagerBo,
            MissionRegistrationInvisibleManager missionRegistrationInvisibleManager,
            MissionSchedulerService missionSchedulerService,
            MissionEventEmitterBo missionEventEmitterBo,
            ObtainedUnitEventEmitter obtainedUnitEventEmitter
    ) {
        this.unitMissionRegistrationBo = unitMissionRegistrationBo;
        this.missionRegistrationObtainedUnitLoader = missionRegistrationObtainedUnitLoader;
        this.missionRegistrationCanDeployChecker = missionRegistrationCanDeployChecker;
        this.missionRegistrationUserExistsChecker = missionRegistrationUserExistsChecker;
        this.missionRepository = missionRepository;
        this.missionRegistrationPreparer = missionRegistrationPreparer;
        this.planetUtilService = planetUtilService;
        this.missionRegistrationAuditor = missionRegistrationAuditor;
        this.missionRegistrationUnitManager = missionRegistrationUnitManager;
        this.missionRegistrationUnitTypeChecker = missionRegistrationUnitTypeChecker;
        this.crossGalaxyMissionChecker = crossGalaxyMissionChecker;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.missionTimeManagerBo = missionTimeManagerBo;
        this.missionRegistrationInvisibleManager = missionRegistrationInvisibleManager;
        this.missionSchedulerService = missionSchedulerService;
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.obtainedUnitEventEmitter = obtainedUnitEventEmitter;
    }

    @ParameterizedTest
    @MethodSource("doCommonMissionRegister_should_work_arguments")
    void doCommonMissionRegister_should_work(
            boolean isEnemyPlanet,
            int timesWhenEnemy,
            UserStorage planetOwner,
            int timesEmitObtainedUnitsAfterCommit,
            boolean isDeployMission
    ) {
        var mission = givenExploreMission();
        var alteredVisibilityMissions = List.of(givenGatherMission());
        var user = givenUser1();
        var sourcePlanet = mission.getSourcePlanet();
        sourcePlanet.setOwner(planetOwner);
        var targetPlanet = mission.getTargetPlanet();
        mission.setUser(user);
        var missionType = MissionType.EXPLORE;
        var missionInformationMock = mock(UnitMissionInformation.class);
        var targetMissionInformationMock = mock(UnitMissionInformation.class);
        var ouDb = givenObtainedUnit1();
        var dbUnits = Map.of(new UnitInMap(UNIT_ID_1, null), new UnitWithItsStoredUnits(ouDb, null));
        var managedOuList = List.of(ouDb.toBuilder().id(49L).build());
        long expectedWantedTime = 190;
        given(missionRegistrationObtainedUnitLoader.checkAndLoadObtainedUnits(missionInformationMock)).willReturn(dbUnits);
        given(missionRegistrationPreparer.prepareMission(targetMissionInformationMock, missionType)).willReturn(mission);
        given(missionRepository.saveAndFlush(mission)).willReturn(mission);
        given(planetUtilService.isEnemyPlanet(user, sourcePlanet)).willReturn(isEnemyPlanet);
        given(missionRegistrationUnitManager.manageUnitsRegistration(targetMissionInformationMock, dbUnits, isEnemyPlanet, user, mission))
                .willReturn(MissionRegistrationUnitManagementResult.builder()
                        .units(managedOuList).
                        alteredVisibilityMissions(alteredVisibilityMissions)
                        .build()
                );
        given(missionInformationMock.getWantedTime()).willReturn(expectedWantedTime);

        unitMissionRegistrationBo.doCommonMissionRegister(missionInformationMock, targetMissionInformationMock, missionType, user, isDeployMission);

        verify(missionRegistrationUserExistsChecker, times(1)).checkUserExists(USER_ID_1);
        verify(missionRegistrationCanDeployChecker, times(1)).checkDeployedAllowed(missionType);
        verify(missionRegistrationAuditor, times(1)).auditMissionRegistration(mission, isDeployMission);
        verify(missionRegistrationUnitTypeChecker, times(1)).checkUnitsCanDoMission(managedOuList, user, mission, missionType);
        verify(crossGalaxyMissionChecker, times(1)).checkCrossGalaxy(missionType, managedOuList, sourcePlanet, targetPlanet);
        verify(obtainedUnitRepository, times(1)).saveAll(managedOuList);
        verify(missionTimeManagerBo, times(1)).handleMissionTimeCalculation(managedOuList, mission, missionType);
        verify(missionTimeManagerBo, times(1)).handleCustomDuration(mission, expectedWantedTime);
        verify(missionRegistrationInvisibleManager, times(1)).handleDefineMissionAsInvisible(mission, managedOuList);
        verify(missionRepository, times(1)).save(mission);
        verify(missionSchedulerService, times(1)).scheduleMission(mission);
        verify(missionEventEmitterBo, times(1)).emitLocalMissionChangeAfterCommit(mission);
        verify(obtainedUnitEventEmitter, times(timesEmitObtainedUnitsAfterCommit)).emitObtainedUnitsAfterCommit(user);
        verify(missionRegistrationInvisibleManager, times(timesWhenEnemy)).maybeUpdateMissionsVisibility(alteredVisibilityMissions);
        verify(missionEventEmitterBo, times(timesWhenEnemy)).emitEnemyMissionsChange(planetOwner);
    }

    private static Stream<Arguments> doCommonMissionRegister_should_work_arguments() {
        var user1 = givenUser1();
        var user2 = givenUser2();
        return Stream.of(
                Arguments.of(false, 0, user1, 1, true),
                Arguments.of(true, 1, user2, 0, true),
                Arguments.of(true, 1, user2, 0, false)
        );
    }
}
