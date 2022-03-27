package com.kevinguanchedarias.owgejava;

import com.kevinguanchedarias.taggablecache.configuration.TaggableCacheDefaultConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@SpringBootApplication(exclude = {SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
        UserDetailsServiceAutoConfiguration.class})
@EnableWebSecurity
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableRetry
@Import(TaggableCacheDefaultConfiguration.class)
@EnableJdbcRepositories(basePackages = "com.kevinguanchedarias.owgejava.repository.jdbc")
public class OwgeRestApplication {

    public static void main(String[] args) {
        SpringApplication.run(OwgeRestApplication.class, args);
    }

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return bean -> bean.setTaskExecutor(null);
    }

}
