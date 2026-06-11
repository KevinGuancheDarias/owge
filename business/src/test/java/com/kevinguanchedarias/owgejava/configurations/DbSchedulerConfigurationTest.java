package com.kevinguanchedarias.owgejava.configurations;

import com.github.kagkarlsson.scheduler.task.ExecutionComplete;
import com.github.kagkarlsson.scheduler.task.ExecutionContext;
import com.github.kagkarlsson.scheduler.task.ExecutionOperations;
import com.kevinguanchedarias.owgejava.job.DbSchedulerRealizationJob;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

class DbSchedulerConfigurationTest {
    private static final long MISSION_ID = 192;

    private final DbSchedulerRealizationJob dbSchedulerRealizationJob = mock(DbSchedulerRealizationJob.class);
    private final DbSchedulerConfiguration dbSchedulerConfiguration = new DbSchedulerConfiguration();

    @Test
    @SuppressWarnings("unchecked")
    void missionProcessingTask_should_remove_the_execution_when_no_retry_is_required() {
        given(dbSchedulerRealizationJob.execute(MISSION_ID)).willReturn(null);
        var task = dbSchedulerConfiguration.missionProcessingTask(dbSchedulerRealizationJob);
        var executionOperations = (ExecutionOperations<Void>) mock(ExecutionOperations.class);

        var completionHandler = task.execute(
                DbSchedulerRealizationJob.BASIC_ONE_TIME_TASK.instance(String.valueOf(MISSION_ID)),
                mock(ExecutionContext.class)
        );
        completionHandler.complete(mock(ExecutionComplete.class), executionOperations);

        verify(dbSchedulerRealizationJob, times(1)).execute(MISSION_ID);
        verify(executionOperations, times(1)).stop();
    }

    @Test
    @SuppressWarnings("unchecked")
    void missionProcessingTask_should_reschedule_the_execution_when_a_retry_is_required() {
        var retryAt = Instant.now().plusSeconds(60);
        given(dbSchedulerRealizationJob.execute(MISSION_ID)).willReturn(retryAt);
        var task = dbSchedulerConfiguration.missionProcessingTask(dbSchedulerRealizationJob);
        var executionOperations = (ExecutionOperations<Void>) mock(ExecutionOperations.class);
        var executionComplete = mock(ExecutionComplete.class);

        var completionHandler = task.execute(
                DbSchedulerRealizationJob.BASIC_ONE_TIME_TASK.instance(String.valueOf(MISSION_ID)),
                mock(ExecutionContext.class)
        );
        completionHandler.complete(executionComplete, executionOperations);

        verify(dbSchedulerRealizationJob, times(1)).execute(MISSION_ID);
        verify(executionOperations, times(1)).reschedule(executionComplete, retryAt);
        verify(executionOperations, never()).stop();
    }
}
