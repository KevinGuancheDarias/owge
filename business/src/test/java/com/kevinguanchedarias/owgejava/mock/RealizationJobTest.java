package com.kevinguanchedarias.owgejava.mock;


import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionBaseService;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.job.RealizationJob;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.*;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;

import java.util.Optional;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static org.assertj.core.api.Assertions.assertThat;
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

    @BeforeEach
    public void setup() throws SchedulerException {
        jobExecutionContextMock = mock(JobExecutionContext.class);

        missionBoMock = mock(MissionBo.class);
        unitMissionBoMock = mock(UnitMissionBo.class);
        realizationJob = new RealizationJob();
        realizationJob.setMissionId(MISSION_ID);
        missionBaseServiceMock = mock(MissionBaseService.class);
        missionRepositoryMock = mock(MissionRepository.class);

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
}
