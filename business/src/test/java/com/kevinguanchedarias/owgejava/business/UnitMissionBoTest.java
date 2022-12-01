package com.kevinguanchedarias.owgejava.business;


import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.mission.*;
import com.kevinguanchedarias.owgejava.business.mission.checker.CrossGalaxyMissionChecker;
import com.kevinguanchedarias.owgejava.business.mission.processor.MissionProcessor;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.UnitMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetExplorationService;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.UnitInterceptionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitModificationBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.fake.NonPostConstructUnitMissionBo;
import com.kevinguanchedarias.owgejava.mock.UnitMissionMock;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.test.answer.InvokeRunnableLambdaAnswer;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionInterceptionInformationMock.INTERCEPTION_INFORMATION_INVOLVED;
import static com.kevinguanchedarias.owgejava.mock.MissionInterceptionInformationMock.givenMissionInterceptionInformation;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static com.kevinguanchedarias.owgejava.mock.MissionTypeMock.givenMissinType;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser2;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.NONE,
        classes = NonPostConstructUnitMissionBo.class
)
@MockBean({
        MissionRepository.class,
        UserStorageBo.class,
        ImprovementBo.class,
        ExceptionUtilService.class,
        SocketIoService.class,
        MissionReportBo.class,
        MissionSchedulerService.class,
        CriticalAttackBo.class,
        TaggableCacheManager.class,
        PlanetLockUtilService.class,
        CrossGalaxyMissionChecker.class,
        PlanetRepository.class,
        MissionEventEmitterBo.class,
        MissionTimeManagerBo.class,
        ObtainedUnitModificationBo.class,
        UnitMissionRegistrationBo.class,
        ReturnMissionRegistrationBo.class,
        MissionReportManagerBo.class,
        MissionUnitsFinderBo.class,
        MissionInterceptionManagerBo.class,
        UnitInterceptionFinderBo.class,
        PlanetBo.class,
        PlanetExplorationService.class,
        MissionBaseService.class
})
class UnitMissionBoTest {
    private final UnitMissionBo unitMissionBo;
    private final MissionRepository missionRepository;
    private final UserStorageBo userStorageBo;
    private final PlanetLockUtilService planetLockUtilService;
    private final MissionInterceptionManagerBo missionInterceptionManagerBo;
    private final MissionReportManagerBo missionReportManagerBo;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final MissionBaseService missionBaseService;
    private MissionProcessor exploreMissionProcessor;

    @Autowired
    public UnitMissionBoTest(
            UnitMissionBo unitMissionBo,
            MissionRepository missionRepository,
            UserStorageBo userStorageBo,
            PlanetLockUtilService planetLockUtilService,
            MissionInterceptionManagerBo missionInterceptionManagerBo,
            MissionReportManagerBo missionReportManagerBo,
            MissionEventEmitterBo missionEventEmitterBo,
            MissionBaseService missionBaseService
    ) {
        this.unitMissionBo = unitMissionBo;
        this.missionRepository = missionRepository;
        this.userStorageBo = userStorageBo;
        this.planetLockUtilService = planetLockUtilService;
        this.missionInterceptionManagerBo = missionInterceptionManagerBo;
        this.missionReportManagerBo = missionReportManagerBo;
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.missionBaseService = missionBaseService;
    }

    @BeforeEach
    public void before() {
        exploreMissionProcessor = mock(MissionProcessor.class);
        unitMissionBo.missionProcessorMap = new HashMap<>();
        unitMissionBo.missionProcessorMap.put(MissionType.EXPLORE, exploreMissionProcessor);
    }

    @Test
    void adminRegisterCounterattackMission_shouuld_throw_when_planet_is_not_owned() {
        var information = UnitMissionMock.givenUnitMissionInformation(MissionType.COUNTERATTACK);
        Assertions.assertThatThrownBy(() -> unitMissionBo.adminRegisterCounterattackMission(information))
                .isInstanceOf(SgtBackendInvalidInputException.class)
                .hasMessageContaining("try again dear Hacker");
    }

    @ParameterizedTest
    @MethodSource("myCancelMission_should_throw_arguments")
    void myCancelMission_should_throw(
            boolean isReturnMission, Class<RuntimeException> exceptionClass, String message, Mission mission
    ) {
        given(missionRepository.findById(EXPLORE_MISSION_ID)).willReturn(Optional.ofNullable(mission));
        given(userStorageBo.findLoggedIn()).willReturn(givenUser1());
        given(missionBaseService.isOfType(mission, MissionType.RETURN_MISSION)).willReturn(isReturnMission);

        assertThatThrownBy(() -> unitMissionBo.myCancelMission(EXPLORE_MISSION_ID))
                .isInstanceOf(exceptionClass)
                .hasMessageContaining(message);

    }

    @ParameterizedTest
    @MethodSource("runUnitMission_should_work_arguments")
    void runUnitMission_should_work(
            boolean isIntercepted,
            int timesAppend,
            int timesHandleInterceptionAndEmitLocal,
            UnitMissionReportBuilder unitMissionReportBuilder,
            int timesHandleReport
    ) {
        var mission = givenExploreMission();
        var missionType = MissionType.EXPLORE;
        var sourcePlanet = mission.getSourcePlanet();
        var targetPlanet = mission.getTargetPlanet();
        var interceptionInformation = givenMissionInterceptionInformation().toBuilder().isMissionIntercepted(isIntercepted).build();
        given(missionRepository.findById(EXPLORE_MISSION_ID)).willReturn(Optional.of(mission));
        doAnswer(new InvokeRunnableLambdaAnswer(1)).when(planetLockUtilService).doInsideLock(eq(List.of(sourcePlanet, targetPlanet)), any());
        given(missionInterceptionManagerBo.loadInformation(mission, missionType)).willReturn(interceptionInformation);
        given(exploreMissionProcessor.process(mission, INTERCEPTION_INFORMATION_INVOLVED)).willReturn(unitMissionReportBuilder);

        unitMissionBo.runUnitMission(EXPLORE_MISSION_ID, missionType);

        verify(missionInterceptionManagerBo, times(timesAppend)).maybeAppendDataToMissionReport(mission, unitMissionReportBuilder, interceptionInformation);
        verify(missionReportManagerBo, times(timesHandleReport)).handleMissionReportSave(mission, unitMissionReportBuilder);
        verify(missionInterceptionManagerBo, times(timesHandleInterceptionAndEmitLocal)).handleMissionInterception(mission, interceptionInformation);
        verify(missionEventEmitterBo, times(timesHandleInterceptionAndEmitLocal)).emitLocalMissionChangeAfterCommit(mission);
    }

    private static Stream<Arguments> myCancelMission_should_throw_arguments() {
        var otherUserMission = givenExploreMission();
        otherUserMission.setUser(givenUser2());
        var returnMission = givenExploreMission();
        returnMission.setType(givenMissinType(MissionType.RETURN_MISSION));
        returnMission.setUser(givenUser1());
        return Stream.of(
                Arguments.of(false, NotFoundException.class, "No mission with id ", null),
                Arguments.of(false, SgtBackendInvalidInputException.class, "other player missions", otherUserMission),
                Arguments.of(true, SgtBackendInvalidInputException.class, "cancel return missions", returnMission)
        );
    }

    private static Stream<Arguments> runUnitMission_should_work_arguments() {
        var report = mock(UnitMissionReportBuilder.class);
        return Stream.of(
                Arguments.of(true, 0, 1, null, 0),
                Arguments.of(false, 1, 0, report, 1),
                Arguments.of(false, 1, 0, null, 0)
        );
    }
}
