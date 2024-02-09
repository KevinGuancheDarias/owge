package com.kevinguanchedarias.owgejava.configuration;

import com.kevinguanchedarias.owgejava.exception.CommonException;
import lombok.AllArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

@Configuration
@Order(104)
@AllArgsConstructor
class GameSecurityConfiguration {
    private final SecurityBeansConfiguration securityBeansConfiguration;


    @Bean
    public SecurityFilterChain gameSecurityFilterChain(HttpSecurity http) throws Exception {
        http.csrf().disable();
        http.authorizeHttpRequests(authz -> {
            authz.requestMatchers("/**");
            try {
                authz.requestMatchers("/game/**").authenticated().and().cors().disable();
            } catch (Exception e) {
                throw new CommonException("Not able to disable cors", e);
            }
        });
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

        http.addFilterBefore(securityBeansConfiguration.getGameBootJwtAuthenticationFilter(),
                        BasicAuthenticationFilter.class).exceptionHandling()
                .authenticationEntryPoint(securityBeansConfiguration.getAuthenticationEntryPoint());
        return http.build();
    }
}