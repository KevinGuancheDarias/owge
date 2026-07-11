package com.kevinguanchedarias.owgejava.configurations;

import com.github.kagkarlsson.scheduler.task.CompletionHandler;
import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
import com.kevinguanchedarias.owgejava.business.DbSchedulerTasksManagerService;
import com.kevinguanchedarias.owgejava.job.DbSchedulerRealizationJob;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DbSchedulerConfiguration {
    @Bean
    Task<Void> missionProcessingTask(DbSchedulerRealizationJob dbSchedulerRealizationJob) {
        return Tasks.custom(DbSchedulerRealizationJob.BASIC_ONE_TIME_TASK)
                .execute((instance, ctx) -> {
                    var retryAt = dbSchedulerRealizationJob.execute(Long.parseLong(instance.getId()));
                    if (retryAt == null) {
                        return new CompletionHandler.OnCompleteRemove<>();
                    }
                    // Retries must reschedule this same execution: scheduling a new one-time task with
                    // the same instance id while this execution is still picked is silently ignored by
                    // db-scheduler (createIfNotExists), and the instance row is removed on completion,
                    // so a retry scheduled from inside the execution would be lost
                    return (executionComplete, executionOperations) -> executionOperations.reschedule(executionComplete, retryAt);
                });
    }

    @Bean
    Task<Void> timeSpecialEffectEndTask(DbSchedulerTasksManagerService dbSchedulerTasksManagerService) {
        return Tasks.oneTime(DbSchedulerTasksManagerService.TIME_SPECIAL_EFFECT_END_TASK)
                .execute((instance, ctx) -> dbSchedulerTasksManagerService
                        .fire(DbSchedulerTasksManagerService.TIME_SPECIAL_EFFECT_END_EVENT, instance.getId()));
    }

    @Bean
    Task<Void> timeSpecialIsReadyTask(DbSchedulerTasksManagerService dbSchedulerTasksManagerService) {
        return Tasks.oneTime(DbSchedulerTasksManagerService.TIME_SPECIAL_IS_READY_TASK)
                .execute((instance, ctx) -> dbSchedulerTasksManagerService
                        .fire(DbSchedulerTasksManagerService.TIME_SPECIAL_IS_READY_EVENT, instance.getId()));
    }

    @Bean
    Task<Void> unitExpiredTask(DbSchedulerTasksManagerService dbSchedulerTasksManagerService) {
        return Tasks.oneTime(DbSchedulerTasksManagerService.UNIT_EXPIRED_TASK)
                .execute((instance, ctx) -> dbSchedulerTasksManagerService
                        .fire(DbSchedulerTasksManagerService.UNIT_EXPIRED_EVENT, instance.getId()));
    }
}
