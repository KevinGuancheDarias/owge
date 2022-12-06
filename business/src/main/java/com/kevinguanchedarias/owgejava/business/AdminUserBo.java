package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenConfigLoader;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;
import com.kevinguanchedarias.owgejava.dto.AdminUserDto;
import com.kevinguanchedarias.owgejava.entity.AdminUser;
import com.kevinguanchedarias.owgejava.exception.AccessDeniedException;
import com.kevinguanchedarias.owgejava.pojo.TokenPojo;
import com.kevinguanchedarias.owgejava.repository.AdminUserRepository;
import io.jsonwebtoken.SignatureAlgorithm;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.Serial;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.0
 */
@Service
@RequiredArgsConstructor
public class AdminUserBo implements BaseBo<Integer, AdminUser, AdminUserDto> {
    @Serial
    private static final long serialVersionUID = -5545554818842439920L;
    
    public static final String ADMIN_USER_CACHE_TAG = "admin_user";
    public static final String JWT_SECRET_DB_CODE = "ADMIN_JWT_SECRET";
    public static final String JWT_HASHING_ALGO = "ADMIN_JWT_ALGO";
    public static final String JWT_DURATION_CODE = "ADMIN_JWT_DURATION_SECONDS";

    private final transient AdminUserRepository adminUserRepository;

    private final AuthenticationBo authenticationBo;

    private final ConfigurationBo configurationBo;

    private final transient JwtService jwtService;

    @SuppressWarnings("SpringQualifierCopyableLombok")
    @Autowired
    @Qualifier("adminOwgeTokenConfigLoader")
    private transient TokenConfigLoader tokenConfigLoader;

    private String adminJwtSecret;
    private SignatureAlgorithm adminJwtAlgo;
    private Integer adminJwtDuration;

    @PostConstruct
    public void init() {
        adminJwtSecret = tokenConfigLoader.getTokenSecret();
        adminJwtAlgo = SignatureAlgorithm
                .valueOf(configurationBo.findOrSetDefault(JWT_HASHING_ALGO, "HS256").getValue());
        adminJwtDuration = Integer.valueOf(configurationBo.findOrSetDefault(JWT_DURATION_CODE, "86400").getValue());
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getRepository()
     */
    @Override
    public JpaRepository<AdminUser, Integer> getRepository() {
        return adminUserRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<AdminUserDto> getDtoClass() {
        return AdminUserDto.class;
    }

    /**
     * Login by using the Game credentials <br>
     * <b>NOTICE: </b> If username or email is different will update it
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public TokenPojo login() {
        TokenUser tokenUser = authenticationBo.findTokenUser();
        AdminUser adminUser = findById(tokenUser.getId().intValue());
        if (adminUser == null) {
            throw new AccessDeniedException("ERR_NO_SUCH_USER");
        } else if (Boolean.FALSE.equals(adminUser.getEnabled())) {
            throw new AccessDeniedException("ERR_USER_NOT_ENABLED");
        }
        if (isUserChanged(tokenUser, adminUser)) {
            adminUser.setUsername(tokenUser.getUsername());
            adminUserRepository.save(adminUser);
        }
        return createToken(adminUser);
    }

    /**
     * Adds a admin user to the system
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public AdminUser addAdmin(Integer accountUserId, String username) {
        AdminUser existing = findById(accountUserId);
        if (existing == null) {
            AdminUser adminUser = new AdminUser();
            adminUser.setId(accountUserId);
            adminUser.setUsername(username);
            adminUser.setEnabled(true);
            adminUserRepository.save(adminUser);
            return adminUser;
        } else {
            return existing;
        }
    }

    /**
     * Will generate the token
     *
     * @author Kevin Guanche Darias
     */
    private TokenPojo createToken(AdminUser user) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", user.getId());
        claims.put("iat", new Date());
        claims.put("exp", genTokenExpitarion());
        claims.put("data", user);

        TokenPojo token = new TokenPojo();
        token.setToken(jwtService.buildToken(claims, adminJwtAlgo, adminJwtSecret));
        token.setUserId(user.getId());
        return token;
    }

    private Date genTokenExpitarion() {
        return new Date((new Date().getTime()) + (adminJwtDuration * 1000));
    }

    private boolean isUserChanged(TokenUser tokenUser, AdminUser adminUser) {
        return !tokenUser.getUsername().equals(adminUser.getUsername());
    }
}
