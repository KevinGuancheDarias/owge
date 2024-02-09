/**
 *
 */
package com.kevinguanchedarias.owgejava.security;

import jakarta.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Autowired;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.enumerations.TokenVerificationMethod;
import com.kevinguanchedarias.owgejava.business.AdminUserBo;
import com.kevinguanchedarias.owgejava.business.ConfigurationBo;

/**
 * Has the configuration for the admin panel login
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class AdminTokenConfigLoader implements TokenConfigLoader {

    @Autowired
    private ConfigurationBo configurationBo;

    private String tokenSecret;

    @PostConstruct
    public void init() {
        tokenSecret = configurationBo.findOrSetDefault(AdminUserBo.JWT_SECRET_DB_CODE, genRandomTokenSecret())
                .getValue();
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader
     * #getTokenSecret()
     */
    @Override
    public String getTokenSecret() {
        return tokenSecret;
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
        return TokenVerificationMethod.SECRET;
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
        return null;
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
        return null;
    }

    private String genRandomTokenSecret() {
        Double number = Double.valueOf(Math.random() * 10000 + 5000);
        return String.valueOf(number.intValue());
    }
}
