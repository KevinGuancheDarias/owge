package com.kevinguanchedarias.owgejava;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.SecurityFilterAutoConfiguration;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.ImportResource;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

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

}
