package com.kevinguanchedarias.owgejava.business;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.enumerations.TokenVerificationMethod;

/**
 * Configures the backend to work in the KGDW server
 *
 * @deprecated As of 0.8.0 This class has been moved to
 *             <b>com.kevinguanchedarias.owgejava.security</b>
 * @since 0.3.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Deprecated
public class SgtTokenConfigLoader implements TokenConfigLoader {

	@Autowired
	private ConfigurationBo configurationBo;

	/**
	 * 
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SgtTokenConfigLoader() {
		Logger.getLogger(getClass())
				.warn("Using this class from com.kevinguanchedarias.owgejava.business is deprecated");
	}

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
