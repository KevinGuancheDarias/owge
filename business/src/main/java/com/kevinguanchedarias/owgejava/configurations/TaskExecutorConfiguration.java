package com.kevinguanchedarias.owgejava.configurations;

import com.kevinguanchedarias.owgejava.business.mysql.MysqlLockState;
import com.kevinguanchedarias.owgejava.context.OwgeContextHolder;
import org.jetbrains.annotations.NotNull;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import java.io.Serial;

@Configuration
@EnableAsync
public class TaskExecutorConfiguration {

    @Bean(name = "contextAwareTaskExecutor")
    public TaskExecutor getContextAwareTaskExecutor() {
        var taskExecutor = new ContextAwarePoolExecutor();
        taskExecutor.setMaxPoolSize(30);
        taskExecutor.setCorePoolSize(20);
        taskExecutor.setQueueCapacity(1000);
        taskExecutor.setThreadNamePrefix("ContextAwareExecutor-");
        return taskExecutor;
    }

    public static class ContextAwareCallable implements Runnable {

        private final Runnable task;
        private final RequestAttributes context;

        public ContextAwareCallable(Runnable task, RequestAttributes context) {
            this.task = task;
            this.context = context;
        }

        @Override
        public void run() {
            if (context != null) {
                RequestContextHolder.setRequestAttributes(context);
            }
            task.run();
        }
    }

    public static class ContextAwarePoolExecutor extends ThreadPoolTaskExecutor {
        @Serial
        private static final long serialVersionUID = 430481863356216895L;

        @Override
        public void execute(@NotNull Runnable task) {
            var mysqlLockState = MysqlLockState.get();
            var owgeContext = OwgeContextHolder.get();

            Runnable taskWithFullContext = () -> {
                // This is run inside a thread in the pool
                MysqlLockState.set(mysqlLockState);
                owgeContext.ifPresent(OwgeContextHolder::set);
                task.run();
            };
            super.execute(new ContextAwareCallable(taskWithFullContext, RequestContextHolder.currentRequestAttributes()));
        }
    }
}