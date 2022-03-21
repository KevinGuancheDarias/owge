package com.kevinguanchedarias.owgejava.mock;


import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.job.RealizationJob;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ApplicationContext;

import static com.kevinguanchedarias.owgejava.mock.MissionMock.EXPLORE_MISSION_ID;
import static com.kevinguanchedarias.owgejava.mock.MissionMock.givenExploreMission;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(OutputCaptureExtension.class)
class RealizationJobTest {
    private static final long MISSION_ID = 192;

    private RealizationJob realizationJob;
    private JobExecutionContext jobExecutionContextMock;
    private MissionBo missionBoMock;
    private UnitMissionBo unitMissionBoMock;

    @BeforeEach
    public void setup() throws SchedulerException {
        jobExecutionContextMock = mock(JobExecutionContext.class);

        missionBoMock = mock(MissionBo.class);
        unitMissionBoMock = mock(UnitMissionBo.class);
        realizationJob = new RealizationJob();
        realizationJob.setMissionId(MISSION_ID);
        var schedulerMock = mock(Scheduler.class);
        var schedulerContextMock = mock(SchedulerContext.class);
        var applicationContextMock = mock(ApplicationContext.class);

        given(jobExecutionContextMock.getScheduler()).willReturn(schedulerMock);
        given(schedulerMock.getContext()).willReturn(schedulerContextMock);
        given(schedulerContextMock.get("applicationContext")).willReturn(applicationContextMock);
        given(applicationContextMock.getBean(MissionBo.class)).willReturn(missionBoMock);
        given(applicationContextMock.getBean(UnitMissionBo.class)).willReturn(unitMissionBoMock);
    }

    @Test
    void executeInternal_should_execute_unit_mission(CapturedOutput capturedOutput) throws JobExecutionException {
        var unitMission = givenExploreMission();
        realizationJob.setMissionId(EXPLORE_MISSION_ID);
        given(missionBoMock.findById(EXPLORE_MISSION_ID)).willReturn(unitMission);

        realizationJob.execute(jobExecutionContextMock);

        verify(missionBoMock, times(1)).findById(EXPLORE_MISSION_ID);
        verify(unitMissionBoMock, times(1)).runUnitMission(EXPLORE_MISSION_ID, MissionType.EXPLORE);
        assertThat(capturedOutput.getOut()).contains("Executing mission id " + EXPLORE_MISSION_ID);
    }

}
