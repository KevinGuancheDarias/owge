package com.kevinguanchedarias.owgejava.configuration;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.cors.CorsConfigurator;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.cors.CorsFilter;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.cors.SimpleCorsConfigurator;
import com.kevinguanchedarias.owgejava.filter.OwgeNullSecurityContextRepository;

@Configuration
class CorsFilterConfiguration extends WebSecurityConfigurerAdapter {
	@Value("${${OWGE_CORS_CUSTOM_ORIGIN:}")
	private String customOrigin;

	@Override
	protected void configure(HttpSecurity http) throws Exception {
		CorsFilter filter = new CorsFilter();
		filter.setCorsConfigurator(createFilterConfiguration());
		http.addFilterBefore(filter, org.springframework.web.filter.CorsFilter.class).sessionManagement()
				.sessionCreationPolicy(SessionCreationPolicy.STATELESS).and().securityContext()
				.securityContextRepository(new OwgeNullSecurityContextRepository()).and().csrf().disable();

	}

	private CorsConfigurator createFilterConfiguration() {
		SimpleCorsConfigurator corsConfigurator = new SimpleCorsConfigurator();
		String[] originStrings = { "http://owgejava_ci.kevinguanchedarias.com", "http://localhost:4200",
				"http://localhost", "http://192.168.99.100" };
		List<String> originList = new ArrayList<>(Arrays.asList(originStrings));
		if (!StringUtils.isEmpty(customOrigin)) {
			originList.add(customOrigin);
		}
		List<String> rootOriginList = new ArrayList<>();
		rootOriginList.add("kevinguanchedarias.com");
		List<String> methodList = new ArrayList<>();
		methodList.add("GET");
		methodList.add("POST");
		methodList.add("PUT");
		methodList.add("DELETE");
		corsConfigurator.setOriginList(originList);
		corsConfigurator.setRootOriginList(rootOriginList);
		corsConfigurator.setMethodList(methodList);
		return corsConfigurator;
	}
}