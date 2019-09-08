/**
 * 
 */
package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.SecurityContextService;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;

/**
 * Has methods to interact with the authentication system
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class AuthenticationBo implements Serializable {
	private static final long serialVersionUID = 5427846503367637891L;

	private static final Logger LOGGER = Logger.getLogger(AuthenticationBo.class);

	@Autowired(required = false)
	private transient SecurityContextService securityContextService;

	@PostConstruct
	public void init() {
		if (securityContextService == null) {
			securityContextService = new SecurityContextService();
			LOGGER.warn("Had to spawn an entire " + SecurityContextService.class.getName()
					+ " because it's not defined as a bean, define a singletone bean for performance and convenience purposes");
		}
	}

	/**
	 * Finds the currently logged in user token
	 * 
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TokenUser findTokenUser() {
		return (TokenUser) securityContextService.getAuthentication().getDetails();
	}
}
