package com.kevinguanchedarias.owgejava.security;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.enumerations.TokenVerificationMethod;
import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;

/**
 * Configures the backend to work in the KGDW server
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class SgtTokenConfigLoader implements TokenConfigLoader {

	@Autowired
	private ConfigurationBo configurationBo;

	@Override
	public String getTokenSecret() {
		return configurationBo.findConfigurationParam(UserStorageBo.JWT_SECRET_DB_CODE).getValue();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader
	 * #getVerificationMethod()
	 */
	@Override
	public TokenVerificationMethod getVerificationMethod() {
		return TokenVerificationMethod.RSA_KEY;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader
	 * #getPrivateKey()
	 */
	@Override
	public String getPrivateKey() {
		return "/var/owge_data/keys/private.key";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader
	 * #getPublicKey()
	 */
	@Override
	public String getPublicKey() {
		return "/var/owge_data/keys/public.key";
	}

}
