package com.kevinguanchedarias.owgejava.test.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.support.GenericApplicationContext;

import static org.mockito.Mockito.spy;

/**
 * Because is not possible to mock {@link org.springframework.context.ApplicationEventPublisher}
 * ensure it's a spy so one can doNothing() and verify
 */
@Configuration
public class SpyEventPublisherConfiguration {
    @Bean
    @Primary
    GenericApplicationContext genericApplicationContext(final GenericApplicationContext genericApplicationContext) {
        return spy(genericApplicationContext);
    }
}
