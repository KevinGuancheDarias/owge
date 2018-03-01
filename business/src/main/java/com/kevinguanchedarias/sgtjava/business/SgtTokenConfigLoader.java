package com.kevinguanchedarias.sgtjava.business;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader;

public class SgtTokenConfigLoader implements TokenConfigLoader {

	@Autowired
	private ConfigurationBo configurationBo;

	@Override
	public String getTokenSecret() {
		return configurationBo.findConfigurationParam(UserStorageBo.JWT_SECRET_DB_CODE).getValue();
	}

}
