package com.kevinguanchedarias.owgejava.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.JwtAuthenticationProvider;

@Configuration
@Order(104)
class GameSecurityConfiguration extends WebSecurityConfigurerAdapter {
	@Autowired
	private SecurityBeansConfiguration securityBeansConfiguration;

	@Override
	protected void configure(AuthenticationManagerBuilder auth) throws Exception {
		auth.authenticationProvider(new JwtAuthenticationProvider());
	}

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		http.csrf().disable();
		http.antMatcher("/**");
		http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS);

		http.addFilterBefore(securityBeansConfiguration.getGameBootJwtAuthenticationFilter(),
				BasicAuthenticationFilter.class).exceptionHandling()
				.authenticationEntryPoint(securityBeansConfiguration.getAuthenticationEntryPoint());
		http.antMatcher("/game/**").authorizeRequests().anyRequest().authenticated();

	}
}