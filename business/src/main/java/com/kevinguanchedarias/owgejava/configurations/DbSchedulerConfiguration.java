package com.kevinguanchedarias.owgejava.configurations;

import com.github.kagkarlsson.scheduler.task.Task;
import com.github.kagkarlsson.scheduler.task.helper.Tasks;
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
}
