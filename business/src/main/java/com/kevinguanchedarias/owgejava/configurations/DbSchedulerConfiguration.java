package com.kevinguanchedarias.owgejava.configurations;

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
        return Tasks.oneTime(DbSchedulerRealizationJob.BASIC_ONE_TIME_TASK)
                .execute((instance, ctx) -> dbSchedulerRealizationJob.execute(Long.parseLong(instance.getId())));
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
