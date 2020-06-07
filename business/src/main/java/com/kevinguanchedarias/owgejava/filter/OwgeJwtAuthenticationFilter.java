package com.kevinguanchedarias.owgejava.filter;

import org.apache.log4j.Logger;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.JwtAuthenticationFilter;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;

/**
 * This class exposes the token validation logic
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class OwgeJwtAuthenticationFilter extends JwtAuthenticationFilter {
	private static final Logger LOG = Logger.getLogger(OwgeJwtAuthenticationFilter.class);

	public OwgeJwtAuthenticationFilter() {
		super();
	}

	public OwgeJwtAuthenticationFilter(boolean useAntMatcher) {
		super(useAntMatcher);
	}

	/**
	 * Finds the user for given token, returns null if token is expired or invalid
	 *
	 * @param jwtToken
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TokenUser findUserFromToken(String jwtToken) {
		try {
			return decodeTokenIfPossible(jwtToken);
		} catch (Exception e) {
			LOG.trace("Invalud authentication :O", e);
			return null;
		}
	}
}
