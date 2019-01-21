package com.kevinguanchedarias.sgtjava.business;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.enumerations.TokenVerificationMethod;

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
		return "C:\\xampp\\htdocs\\drupal\\private.key";
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
		return "C:\\xampp\\htdocs\\drupal\\public.key";
	}

}
