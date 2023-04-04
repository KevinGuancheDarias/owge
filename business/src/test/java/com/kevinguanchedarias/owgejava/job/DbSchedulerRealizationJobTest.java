package com.kevinguanchedarias.owgejava.job;

import com.github.kagkarlsson.scheduler.Scheduler;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.PessimisticLockingFailureException;

import javax.sql.DataSource;
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
@SpringBootTest(
        classes = DbSchedulerRealizationJob.class,
        webEnvironment = SpringBootTest.WebEnvironment.NONE
)
@MockBean({
        MissionRepository.class,
        MissionBo.class,
        UnitMissionBo.class,
        MissionBaseService.class,
        MissionEventEmitterBo.class,
        MysqlInformationRepository.class,
        DataSource.class,
        Scheduler.class
})
class DbSchedulerRealizationJobTest {
    private static final long MISSION_ID = 192;

    private final DbSchedulerRealizationJob dbSchedulerRealizationJob;
    private final MissionRepository missionRepository;
    private final MissionBo missionBo;
    private final UnitMissionBo unitMissionBo;
    private final MissionBaseService missionBaseService;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final MysqlInformationRepository mysqlInformationRepository;

    @Autowired
    DbSchedulerRealizationJobTest(
            DbSchedulerRealizationJob dbSchedulerRealizationJob,
            MissionRepository missionRepository,
            MissionBo missionBo,
            UnitMissionBo unitMissionBo,
            MissionBaseService missionBaseService,
            MissionEventEmitterBo missionEventEmitterBo,
            MysqlInformationRepository mysqlInformationRepository
    ) {
        this.dbSchedulerRealizationJob = dbSchedulerRealizationJob;
        this.missionRepository = missionRepository;
        this.missionBo = missionBo;
        this.unitMissionBo = unitMissionBo;
        this.missionBaseService = missionBaseService;
        this.missionEventEmitterBo = missionEventEmitterBo;
        this.mysqlInformationRepository = mysqlInformationRepository;
    }

    @Test
    void executeInternal_should_execute_unit_mission(CapturedOutput capturedOutput) {
        var unitMission = givenExploreMission();
        given(missionRepository.findById(EXPLORE_MISSION_ID)).willReturn(Optional.of(unitMission));

        dbSchedulerRealizationJob.execute(EXPLORE_MISSION_ID);

        verify(unitMissionBo, times(1)).runUnitMission(EXPLORE_MISSION_ID, MissionType.EXPLORE);
        assertThat(capturedOutput.getOut()).contains("Executing mission id " + EXPLORE_MISSION_ID);
    }

    @ParameterizedTest
    @CsvSource({
            "LEVEL_UP",
            "BUILD_UNIT"
    })
    void executeInternal_should_execute_non_unit_mission(MissionType missionType) {
        var mission = givenBuildMission();
        mission.setId(MISSION_ID);
        mission.setType(givenMissinType(missionType));
        given(missionRepository.findById(MISSION_ID)).willReturn(Optional.of(mission));

        dbSchedulerRealizationJob.execute(MISSION_ID);

        verify(missionBo, times(1)).runMission(MISSION_ID, missionType);
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
    ) {
        var mission = givenBuildMission();
        var user = givenUser1();
        mission.setUser(user);
        mission.setId(MISSION_ID);
        mission.setType(givenMissinType(missionType));
        given(missionRepository.findById(MISSION_ID)).willReturn(Optional.of(mission));
        doThrow(e).when(missionBo).runMission(MISSION_ID, missionType);
        given(mysqlInformationRepository.findInnoDbStatus()).willReturn(mock(MysqlEngineInformation.class));
        given(mysqlInformationRepository.findFullProcessInformation()).willReturn(List.of());

        dbSchedulerRealizationJob.execute(MISSION_ID);

        assertThat(capturedOutput.getOut()).contains("fatal exception when ");
        verify(missionBaseService, times(1)).retryMissionIfPossible(MISSION_ID, missionType);
        verify(missionBo, times(timesEmitRunningUpgrade)).emitRunningUpgrade(user);
        verify(missionEventEmitterBo, times(timesEmitUnitBuildChange)).emitUnitBuildChange(USER_ID_1);
        verify(mysqlInformationRepository, times(timesLogSqlInformation)).findInnoDbStatus();
        verify(mysqlInformationRepository, times(timesLogSqlInformation)).findFullProcessInformation();
    }

    @Test
    void executeInternal_should_handle_exception_when_unit_mission() {
        var mission = givenExploreMission();
        var user = givenUser1();
        mission.setId(MISSION_ID);
        mission.setUser(user);
        given(missionRepository.findById(MISSION_ID)).willReturn(Optional.of(mission));
        doThrow(new CommonException("OOPS")).when(unitMissionBo).runUnitMission(MISSION_ID, MissionType.EXPLORE);

        dbSchedulerRealizationJob.execute(MISSION_ID);

        verify(missionEventEmitterBo, times(1)).emitUnitMissions(USER_ID_1);
        verify(missionEventEmitterBo, times(1)).emitEnemyMissionsChange(mission);
    }

    @Test
    void executeInternal_should_do_nothing_on_empty_or_resolved_mission() {
        var mission = givenExploreMission();
        mission.setResolved(true);
        mission.setId(MISSION_ID);
        given(missionRepository.findById(MISSION_ID)).willReturn(Optional.of(mission)).willReturn(Optional.empty());

        dbSchedulerRealizationJob.execute(MISSION_ID);

        verifyNoInteractions(missionBo, unitMissionBo, missionBaseService, missionEventEmitterBo);
    }

    @Test
    void executeInternal_should_fail_to_handle_exception_on_surprising_condition() {
        var mission = givenDeployedMission();
        mission.setId(MISSION_ID);
        given(missionRepository.findById(MISSION_ID)).willReturn(Optional.of(mission));
        doThrow(new CommonException("OOPS")).when(unitMissionBo).runUnitMission(MISSION_ID, MissionType.DEPLOYED);

        assertThatThrownBy(() -> dbSchedulerRealizationJob.execute(MISSION_ID))
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
