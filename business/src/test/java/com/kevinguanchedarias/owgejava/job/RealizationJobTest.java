package com.kevinguanchedarias.owgejava.job;

import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionBaseService;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.pojo.MysqlEngineInformation;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.MysqlInformationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.quartz.*;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.PessimisticLockingFailureException;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.*;
import static com.kevinguanchedarias.owgejava.mock.MissionTypeMock.givenMissinType;
import static com.kevinguanchedarias.owgejava.mock.UserMock.USER_ID_1;
import static com.kevinguanchedarias.owgejava.mock.UserMock.givenUser1;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(OutputCaptureExtension.class)
class RealizationJobTest {
    private static final long MISSION_ID = 192;

    private RealizationJob realizationJob;
    private JobExecutionContext jobExecutionContextMock;
    private MissionBo missionBoMock;
    private MissionBaseService missionBaseServiceMock;
    private MissionRepository missionRepositoryMock;
    private UnitMissionBo unitMissionBoMock;
    private MissionEventEmitterBo missionEventEmitterBoMock;
    private MysqlInformationRepository mysqlInformationRepository;

    @BeforeEach
    public void setup() throws SchedulerException {
        jobExecutionContextMock = mock(JobExecutionContext.class);

        missionBoMock = mock(MissionBo.class);
        unitMissionBoMock = mock(UnitMissionBo.class);
        realizationJob = new RealizationJob();
        realizationJob.setMissionId(MISSION_ID);
        missionBaseServiceMock = mock(MissionBaseService.class);
        missionRepositoryMock = mock(MissionRepository.class);
        missionEventEmitterBoMock = mock(MissionEventEmitterBo.class);
        mysqlInformationRepository = mock(MysqlInformationRepository.class);

        var schedulerMock = mock(Scheduler.class);
        var schedulerContextMock = mock(SchedulerContext.class);
        var applicationContextMock = mock(ApplicationContext.class);

        given(jobExecutionContextMock.getScheduler()).willReturn(schedulerMock);
        given(schedulerMock.getContext()).willReturn(schedulerContextMock);
        given(schedulerContextMock.get("applicationContext")).willReturn(applicationContextMock);
        given(applicationContextMock.getBean(MissionBo.class)).willReturn(missionBoMock);
        given(applicationContextMock.getBean(UnitMissionBo.class)).willReturn(unitMissionBoMock);
        given(applicationContextMock.getBean(MissionBaseService.class)).willReturn(missionBaseServiceMock);
        given(applicationContextMock.getBean(MissionRepository.class)).willReturn(missionRepositoryMock);
        given(applicationContextMock.getBean(MissionEventEmitterBo.class)).willReturn(missionEventEmitterBoMock);
        given(applicationContextMock.getBean(MysqlInformationRepository.class)).willReturn(mysqlInformationRepository);
    }

    @Test
    void executeInternal_should_execute_unit_mission(CapturedOutput capturedOutput) throws JobExecutionException {
        var unitMission = givenExploreMission();
        realizationJob.setMissionId(EXPLORE_MISSION_ID);
        given(missionRepositoryMock.findById(EXPLORE_MISSION_ID)).willReturn(Optional.of(unitMission));

        realizationJob.execute(jobExecutionContextMock);

        verify(unitMissionBoMock, times(1)).runUnitMission(EXPLORE_MISSION_ID, MissionType.EXPLORE);
        assertThat(capturedOutput.getOut()).contains("Executing mission id " + EXPLORE_MISSION_ID);
    }

    @ParameterizedTest
    @CsvSource({
            "LEVEL_UP",
            "BUILD_UNIT"
    })
    void executeInternal_should_execute_non_unit_mission(MissionType missionType) throws JobExecutionException {
        var mission = givenBuildMission();
        mission.setId(MISSION_ID);
        mission.setType(givenMissinType(missionType));
        given(missionRepositoryMock.findById(MISSION_ID)).willReturn(Optional.of(mission));

        realizationJob.execute(jobExecutionContextMock);

        verify(missionBoMock, times(1)).runMission(MISSION_ID, missionType);
    }

    @ParameterizedTest
    @MethodSource("executeInternal_should_handle_exception_arguments")
    void executeInternal_should_handle_exception_when_non_unit_mission(
            MissionType missionType,
            Exception e,
            int timesLogSqlInformation,
            int timesEmitRunningUpgrade,
            int timesEmitUnitBuildChange,
            CapturedOutput capturedOutput
    ) throws JobExecutionException {
        var mission = givenBuildMission();
        var user = givenUser1();
        mission.setUser(user);
        mission.setId(MISSION_ID);
        mission.setType(givenMissinType(missionType));
        given(missionRepositoryMock.findById(MISSION_ID)).willReturn(Optional.of(mission));
        doThrow(e).when(missionBoMock).runMission(MISSION_ID, missionType);
        given(mysqlInformationRepository.findInnoDbStatus()).willReturn(mock(MysqlEngineInformation.class));
        given(mysqlInformationRepository.findFullProcessInformation()).willReturn(List.of());

        realizationJob.execute(jobExecutionContextMock);

        assertThat(capturedOutput.getOut()).contains("fatal exception when ");
        verify(missionBaseServiceMock, times(1)).retryMissionIfPossible(MISSION_ID, missionType, MissionBo.JOB_GROUP_NAME);
        verify(missionBoMock, times(timesEmitRunningUpgrade)).emitRunningUpgrade(user);
        verify(missionEventEmitterBoMock, times(timesEmitUnitBuildChange)).emitUnitBuildChange(USER_ID_1);
        verify(mysqlInformationRepository, times(timesLogSqlInformation)).findInnoDbStatus();
        verify(mysqlInformationRepository, times(timesLogSqlInformation)).findFullProcessInformation();
    }

    @Test
    void executeInternal_should_handle_exception_when_unit_mission() throws JobExecutionException {
        var mission = givenExploreMission();
        var user = givenUser1();
        mission.setId(MISSION_ID);
        mission.setUser(user);
        given(missionRepositoryMock.findById(MISSION_ID)).willReturn(Optional.of(mission));
        doThrow(new CommonException("OOPS")).when(unitMissionBoMock).runUnitMission(MISSION_ID, MissionType.EXPLORE);

        realizationJob.execute(jobExecutionContextMock);

        verify(missionEventEmitterBoMock, times(1)).emitUnitMissions(USER_ID_1);
        verify(missionEventEmitterBoMock, times(1)).emitEnemyMissionsChange(mission);
    }

    @Test
    void executeInternal_should_do_nothing_on_empty_or_resolved_mission() {
        var mission = givenExploreMission();
        mission.setResolved(true);
        mission.setId(MISSION_ID);
        given(missionRepositoryMock.findById(MISSION_ID)).willReturn(Optional.of(mission)).willReturn(Optional.empty());

        realizationJob.executeInternal(jobExecutionContextMock);
        realizationJob.executeInternal(jobExecutionContextMock);

        verifyNoInteractions(missionBoMock, unitMissionBoMock, missionBaseServiceMock, missionEventEmitterBoMock);
    }

    @Test
    void executeInternal_should_fail_to_handle_exception_on_surprising_condition() {
        var mission = givenDeployedMission();
        mission.setId(MISSION_ID);
        given(missionRepositoryMock.findById(MISSION_ID)).willReturn(Optional.of(mission));
        doThrow(new CommonException("OOPS")).when(unitMissionBoMock).runUnitMission(MISSION_ID, MissionType.DEPLOYED);

        assertThatThrownBy(() -> realizationJob.executeInternal(jobExecutionContextMock))
                .isInstanceOf(ProgrammingException.class);
    }

    private static Stream<Arguments> executeInternal_should_handle_exception_arguments() {
        var pessimisticLockingFailureException = mock(PessimisticLockingFailureException.class);
        var commonException = mock(CommonException.class);
        var stackTraceArray = new StackTraceElement[]{new StackTraceElement("foo", "bar", "foo.file", 1)};
        given(pessimisticLockingFailureException.getStackTrace())
                .willReturn(stackTraceArray);
        given(commonException.getStackTrace()).willReturn(stackTraceArray);
        return Stream.of(
                Arguments.of(MissionType.LEVEL_UP, commonException, 0, 1, 0),
                Arguments.of(MissionType.LEVEL_UP, pessimisticLockingFailureException, 1, 1, 0),
                Arguments.of(MissionType.BUILD_UNIT, commonException, 0, 0, 1)
        );
    }
}
