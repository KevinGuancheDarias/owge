package com.kevinguanchedarias.owgejava.configurations;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

@Configuration
@EnableAsync
public class TaskExecutorConfiguration {

    public class ContextAwareCallable implements Runnable {

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

    public class ContextAwarePoolExecutor extends ThreadPoolTaskExecutor {
        private static final long serialVersionUID = 430481863356216895L;

        @Override
        public void execute(Runnable task) {
            super.execute(new ContextAwareCallable(task, RequestContextHolder.currentRequestAttributes()));
        }
    }

    @Bean(name = "contextAwareTaskExecutor")
    public TaskExecutor getContextAwareTaskExecutor() {
        var taskExecutor = new ContextAwarePoolExecutor();
        taskExecutor.setMaxPoolSize(30);
        taskExecutor.setCorePoolSize(20);
        taskExecutor.setQueueCapacity(1000);
        taskExecutor.setThreadNamePrefix("ContextAwareExecutor-");
        return taskExecutor;
    }
}