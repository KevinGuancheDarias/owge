package com.kevinguanchedarias.owgejava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ImportResource;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.fasterxml.jackson.annotation.JsonInclude.Include;

@SpringBootApplication(exclude = { SecurityAutoConfiguration.class, SecurityFilterAutoConfiguration.class,
		UserDetailsServiceAutoConfiguration.class })
@EnableWebSecurity
@EnableWebMvc
@EnableCaching
@ImportResource("META-INF/quartz-context.xml")
public class OwgeRestApplication {

	public static void main(String[] args) {
		SpringApplication.run(OwgeRestApplication.class, args);
	}

	@Bean
	@Primary
	public Jackson2ObjectMapperBuilder objectMapper() {
		Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
		builder.serializationInclusion(Include.NON_NULL);
		return builder;
	}
	
	@Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer()  {
        return bean -> bean.setTaskExecutor(null);
    }
	
}
