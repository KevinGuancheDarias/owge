package com.kevinguanchedarias.owgejava.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader;
import com.kevinguanchedarias.owgejava.security.SgtTokenConfigLoader;

@Profile("rsaKeys")
@Order(102)
class RsaProfileSecurityConfiguration {
	@Bean
	public TokenConfigLoader gameOwgeTokenConfigLoader() {
		return new SgtTokenConfigLoader();
	}
}