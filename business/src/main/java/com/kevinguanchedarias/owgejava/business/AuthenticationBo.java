/**
 *
 */
package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.SecurityContextService;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.Serial;
import java.io.Serializable;

/**
 * Has methods to interact with the authentication system
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Service
@Slf4j
public class AuthenticationBo implements Serializable {
    @Serial
    private static final long serialVersionUID = 5427846503367637891L;


    @Autowired(required = false)
    private transient SecurityContextService securityContextService;

    @PostConstruct
    public void init() {
        if (securityContextService == null) {
            securityContextService = new SecurityContextService();
            log.warn("Had to spawn an entire {}"
                            + " because it's not defined as a bean, define a singletone bean for performance and convenience purposes",
                    SecurityContextService.class.getName()
            );
        }
    }

    /**
     * Finds the currently logged in user token
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public TokenUser findTokenUser() {
        return securityContextService != null && securityContextService.getAuthentication() != null
                ? (TokenUser) securityContextService.getAuthentication().getDetails()
                : null;
    }
}
