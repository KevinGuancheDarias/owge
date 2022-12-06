package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.builder.ExceptionBuilder;
import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.MissionSchedulerService;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitModificationBo;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.*;
import static com.kevinguanchedarias.owgejava.mock.ObtainedUnitMock.givenObtainedUnit1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@SpringBootTest(
        classes = MissionBaseService.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        MissionRepository.class,
        UserStorageRepository.class,
        ReturnMissionRegistrationBo.class,
        ObtainedUnitModificationBo.class,
        MissionTimeManagerBo.class,
        MissionReportManagerBo.class,
        ObtainedUnitRepository.class,
        ImprovementBo.class,
        ExceptionUtilService.class,
        MissionSchedulerService.class
})
class MissionBaseServiceTest {
    private static final String GROUP = "FOO";
    private final MissionBaseService missionBaseService;
    private final MissionRepository missionRepository;
    private final UserStorageRepository userStorageRepository;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final ObtainedUnitModificationBo obtainedUnitModificationBo;
    private final MissionTimeManagerBo missionTimeManagerBo;
    private final MissionReportManagerBo missionReportManagerBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ImprovementBo improvementBo;
    private final ExceptionUtilService exceptionUtilService;
    private final MissionSchedulerService missionSchedulerService;

    @Autowired
    MissionBaseServiceTest(
            MissionBaseService missionBaseService,
            MissionRepository missionRepository,
            UserStorageRepository userStorageRepository,
            ReturnMissionRegistrationBo returnMissionRegistrationBo,
            ObtainedUnitModificationBo obtainedUnitModificationBo,
            MissionTimeManagerBo missionTimeManagerBo,
            MissionReportManagerBo missionReportManagerBo,
            ObtainedUnitRepository obtainedUnitRepository,
            ImprovementBo improvementBo,
            ExceptionUtilService exceptionUtilService,
            MissionSchedulerService missionSchedulerService

    ) {
        this.missionBaseService = missionBaseService;
        this.missionRepository = missionRepository;
        this.userStorageRepository = userStorageRepository;
        this.returnMissionRegistrationBo = returnMissionRegistrationBo;
        this.obtainedUnitModificationBo = obtainedUnitModificationBo;
        this.missionTimeManagerBo = missionTimeManagerBo;
        this.missionReportManagerBo = missionReportManagerBo;
        this.obtainedUnitRepository = obtainedUnitRepository;
        this.improvementBo = improvementBo;
        this.exceptionUtilService = exceptionUtilService;
        this.missionSchedulerService = missionSchedulerService;
    }

    @Test
    void retryMissionIfPossible_should_throw_when_unexpected_mission_type() {
        var mission = givenDeployedMission();
        mission.setAttemps(5);
        given(missionRepository.findById(DEPLOYED_MISSION_ID)).willReturn(Optional.of(mission));

        assertThatThrownBy(() -> missionBaseService.retryMissionIfPossible(DEPLOYED_MISSION_ID, MissionType.DEPLOYED, GROUP))
                .isInstanceOf(ProgrammingException.class);
    }

    @ParameterizedTest
    @CsvSource({
            "GATHER,1,0,0,true",
            "BUILD_UNIT,0,1,1,false",
            "LEVEL_UP,0,0,1,false"
    })
    void retryMissionIfPossible_should_handle_passed_max_attempts(
            MissionType missionType,
            int timesRegisterReturnMission,
            int timesDeletedByMissionId,
            int timesDeleteMission,
            boolean isResolved
    ) {
        var mission = givenExploreMission();
        long missionId = 192;
        mission.setId(missionId);
        mission.setType(givenMissionType(missionType));
        mission.setAttemps(5);
        given(missionRepository.findById(missionId)).willReturn(Optional.of(mission));

        missionBaseService.retryMissionIfPossible(missionId, missionType, GROUP);

        verify(returnMissionRegistrationBo, times(timesRegisterReturnMission)).registerReturnMission(mission, null);
        verify(obtainedUnitModificationBo, times(timesDeletedByMissionId)).deleteByMissionId(missionId);
        verify(missionRepository, times(timesDeleteMission)).delete(mission);
        assertThat(mission.getResolved()).isEqualTo(isResolved);
    }

    @ParameterizedTest
    @CsvSource({
            "GATHER,1",
            "LEVEL_UP,0"
    })
    void retryMissionIfPossible_should__should_add_new_attempt(MissionType missionType, int timesIfUnitMission) {
        double requiredTime = 29;
        var mission = givenExploreMission();
        long missionId = 1420;
        var user = givenUser1();
        mission.setId(missionId);
        mission.setUser(user);
        mission.setAttemps(2);
        mission.setRequiredTime(requiredTime);
        mission.setType(givenMissionType(missionType));
        var terminationDate = LocalDateTime.now();
        var involvedUnits = List.of(givenObtainedUnit1());
        var reportBuilderMock = mock(UnitMissionReportBuilder.class);
        given(missionRepository.findById(missionId)).willReturn(Optional.of(mission));
        given(missionTimeManagerBo.computeTerminationDate(requiredTime)).willReturn(terminationDate);
        given(reportBuilderMock.withSenderUser(user)).willReturn(reportBuilderMock);
        given(reportBuilderMock.withId(missionId)).willReturn(reportBuilderMock);
        given(reportBuilderMock.withSourcePlanet(mission.getSourcePlanet())).willReturn(reportBuilderMock);
        given(reportBuilderMock.withTargetPlanet(mission.getTargetPlanet())).willReturn(reportBuilderMock);
        given(obtainedUnitRepository.findByMissionId(missionId)).willReturn(involvedUnits);
        given(reportBuilderMock.withInvolvedUnits(involvedUnits)).willReturn(reportBuilderMock);
        try (var mockedStatic = mockStatic(UnitMissionReportBuilder.class)) {
            mockedStatic.when(UnitMissionReportBuilder::create)
                    .thenReturn(reportBuilderMock);

            missionBaseService.retryMissionIfPossible(missionId, missionType, GROUP);

            assertThat(mission.getAttemps()).isEqualTo(3);
            assertThat(mission.getTerminationDate()).isSameAs(terminationDate);
            verify(reportBuilderMock, times(timesIfUnitMission)).withInvolvedUnits(involvedUnits);
            verify(reportBuilderMock, times(1)).withErrorInformation(contains("please contact an admin"));
            verify(missionReportManagerBo, times(1)).handleMissionReportSave(mission, reportBuilderMock);
            verify(missionSchedulerService, times(1)).scheduleMission(GROUP, mission);
            verify(missionRepository, times(1)).save(mission);
        }
    }

    @Test
    void isOfType_works() {
        assertThat(missionBaseService.isOfType(givenExploreMission(), MissionType.EXPLORE)).isTrue();
        assertThat(missionBaseService.isOfType(givenAttackMission(), MissionType.EXPLORE)).isFalse();
    }

    @ParameterizedTest
    @CsvSource({
            "3,2",
            "10,3"
    })
    void checkMissionLimitNotReached_should_work(float maxMissions, int runningMissions) {
        var user = givenUser1();
        var groupedImprovementMock = mock(GroupedImprovement.class);
        given(missionRepository.countByUserIdAndResolvedFalse(user.getId())).willReturn(runningMissions);
        given(improvementBo.findUserImprovement(user)).willReturn(groupedImprovementMock);
        given(groupedImprovementMock.getMoreMisions()).willReturn(maxMissions);

        missionBaseService.checkMissionLimitNotReached(user);

        verifyNoInteractions(exceptionUtilService);
    }

    @Test
    void checkMissionLimitNotReached_should_throw() {
        var user = givenUser1();
        float maxMissions = 1;
        var groupedImprovementMock = mock(GroupedImprovement.class);
        var exceptionBuilderMock = mock(ExceptionBuilder.class);
        var exceptionMock = mock(CommonException.class);
        given(missionRepository.countByUserIdAndResolvedFalse(user.getId())).willReturn(2);
        given(improvementBo.findUserImprovement(user)).willReturn(groupedImprovementMock);
        given(groupedImprovementMock.getMoreMisions()).willReturn(maxMissions);
        given(exceptionUtilService.createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_MISSION_LIMIT_EXCEEDED"))
                .willReturn(exceptionBuilderMock);
        given(exceptionBuilderMock.withDeveloperHintDoc(GameProjectsEnum.BUSINESS, MissionBaseService.class, DocTypeEnum.EXCEPTIONS))
                .willReturn(exceptionBuilderMock);
        given(exceptionBuilderMock.build()).willReturn(exceptionMock);

        assertThatThrownBy(() -> missionBaseService.checkMissionLimitNotReached(user))
                .isSameAs(exceptionMock);
    }
}