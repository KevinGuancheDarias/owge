package com.kevinguanchedarias.sgtjava.fake;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;

/**
 * This class does nothing, but spring needs an authentication provider to use a
 * Spring Security Filter
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class AuthenticationProviderFake implements AuthenticationProvider {

	@Override
	public Authentication authenticate(Authentication authentication) {
		return authentication;
	}

	@Override
	public boolean supports(Class<?> authentication) {
		return true;
	}

}
