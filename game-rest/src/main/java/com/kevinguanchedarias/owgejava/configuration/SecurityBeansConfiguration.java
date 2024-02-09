package com.kevinguanchedarias.owgejava.configuration;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.*;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.event.ResourceAutoUpdateEventHandler;
import com.kevinguanchedarias.owgejava.filter.BootJwtAuthenticationFilter;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.security.AdminTokenConfigLoader;
import com.kevinguanchedarias.owgejava.security.DevelopmentSgtTokenConfigLoader;
import com.kevinguanchedarias.owgejava.security.SgtTokenConfigLoader;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.security.web.util.matcher.OrRequestMatcher;

import java.util.Arrays;

@Configuration
@RequiredArgsConstructor
@Order(101)
@Getter
class SecurityBeansConfiguration {

    private final Environment environment;

    private final AuthenticationEntryPoint authenticationEntryPoint = new RestAuthenticationEntryPoint();
    private final BootJwtAuthenticationFilter adminBootJwtAuthenticationFilter = new BootJwtAuthenticationFilter("/admin/**");
    private final BootJwtAuthenticationFilter gameBootJwtAuthenticationFilter = new BootJwtAuthenticationFilter("/game/**");
    private final JwtAuthenticationProvider jwtAuthenticationProvider = new JwtAuthenticationProvider();

    @Bean
    public AuthenticationEntryPoint restAuthenticationEntryPoint() {
        return authenticationEntryPoint;
    }

    @Bean
    public AuthenticationSuccessHandler jwtAuthenticationSuccessHandler() {
        return new JwtAuthenticationSuccessHandler();
    }

    @Bean
    public TokenConfigLoader adminOwgeTokenConfigLoader() {
        return new AdminTokenConfigLoader();
    }

    /**
     * Can't use profile annotation, as Spring Boot 2.29 doesn't support proper
     * overriding
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Bean
    public TokenConfigLoader gameOwgeTokenConfigLoader() {
        if (Arrays.asList(environment.getActiveProfiles()).contains("rsaKeys")) {
            return new SgtTokenConfigLoader();
        } else {
            return new DevelopmentSgtTokenConfigLoader();
        }

    }

    @Bean
    public FilterEventHandler owgeResourceAutoUpdateEventHandler(
            UserStorageBo userStorageBo, UserSessionService userSessionService, UserStorageRepository userStorageRepository
    ) {
        return new ResourceAutoUpdateEventHandler(userStorageBo, userStorageRepository, userSessionService);
    }

    @Bean
    public BootJwtAuthenticationFilter gameBootJwtAuthenticationFilter(
            AuthenticationSuccessHandler authenticationSuccessHandler,
            @Qualifier("gameOwgeTokenConfigLoader") TokenConfigLoader tokenConfigLoader,
            FilterEventHandler filterEventHandler, AuthenticationManager authenticationManager) {
        gameBootJwtAuthenticationFilter.setAuthenticationManager(authenticationManager);
        gameBootJwtAuthenticationFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        gameBootJwtAuthenticationFilter.setTokenConfigLoader(tokenConfigLoader);
        gameBootJwtAuthenticationFilter.setConvertExceptionToJson(true);
        gameBootJwtAuthenticationFilter.setFilterEventHandler(filterEventHandler);
        gameBootJwtAuthenticationFilter
                .setRequiresAuthenticationRequestMatcher(new OrRequestMatcher(new AntPathRequestMatcher("/game/**")));
        return gameBootJwtAuthenticationFilter;
    }

    @Bean
    public BootJwtAuthenticationFilter adminBootJwtAuthenticationFilter(
            AuthenticationSuccessHandler authenticationSuccessHandler,
            @Qualifier("adminOwgeTokenConfigLoader") TokenConfigLoader tokenConfigLoader, AuthenticationManager authenticationManager) {
        adminBootJwtAuthenticationFilter.setAuthenticationManager(authenticationManager);
        adminBootJwtAuthenticationFilter.setAuthenticationSuccessHandler(authenticationSuccessHandler);
        adminBootJwtAuthenticationFilter.setTokenConfigLoader(tokenConfigLoader);
        adminBootJwtAuthenticationFilter.setConvertExceptionToJson(true);
        adminBootJwtAuthenticationFilter
                .setRequiresAuthenticationRequestMatcher(new OrRequestMatcher(new AntPathRequestMatcher("/admin/**")));
        return adminBootJwtAuthenticationFilter;
    }

    @Bean
    public FilterRegistrationBean<BootJwtAuthenticationFilter> runLastGameAuth(
            @Qualifier("gameBootJwtAuthenticationFilter") BootJwtAuthenticationFilter filter) {
        FilterRegistrationBean<BootJwtAuthenticationFilter> filterRegistrationBean = new FilterRegistrationBean<>(
                filter);
        filterRegistrationBean.setOrder(Ordered.LOWEST_PRECEDENCE);
        return filterRegistrationBean;
    }

    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity httpSecurity) throws Exception {
        var auth = httpSecurity.getSharedObject(AuthenticationManagerBuilder.class);
        auth.authenticationProvider(jwtAuthenticationProvider);
        return auth.build();
    }
}