package com.kevinguanchedarias.owgejava.filter;


import com.kevinguanchedarias.kevinsuite.commons.rest.security.JwtAuthenticationFilter;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;
import lombok.extern.slf4j.Slf4j;

/**
 * This class exposes the token validation logic
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Slf4j
public class OwgeJwtAuthenticationFilter extends JwtAuthenticationFilter {

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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public TokenUser findUserFromToken(String jwtToken) {
        try {
            return decodeTokenIfPossible(jwtToken);
        } catch (Exception e) {
            log.trace("Invalud authentication :O", e);
            return null;
        }
    }
}
