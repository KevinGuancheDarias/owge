package com.kevinguanchedarias.owgejava.configurations;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;

@Configuration
@ImportResource("META-INF/quartz-context.xml")
public class QuartzConfiguration {
}
