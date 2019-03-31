package com.kevinguanchedarias.owgejava.business;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import com.kevinguanchedarias.owgejava.entity.User;
import com.kevinguanchedarias.owgejava.exception.UserLoginException;
import com.kevinguanchedarias.owgejava.pojo.TokenPojo;
import com.kevinguanchedarias.owgejava.repository.UserRepository;

import io.jsonwebtoken.SignatureAlgorithm;

@Service
public class UserBo {
	private static final Logger LOGGER = Logger.getLogger(UserBo.class);

	public static final String JWT_SECRET_DB_CODE = "JWT_SECRET";
	public static final String JWT_HASHING_ALGO = "JWT_ALGO";
	public static final String JWT_DURATION_CODE = "JWT_DURATION_SECONDS";

	public static final String EXCEPTION_INVALID_LOGIN_USER = "El usuario no existe";
	public static final String EXCEPTION_INVALID_LOGIN_CREDENTIALS = "Contraseña incorrecta";

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private ConfigurationBo configurationBo;

	@Autowired
	private JwtService jwtService;

	/**
	 * Will generate JWT token, if success
	 * 
	 * @param email
	 *            The user's email
	 * @param password
	 *            The user's password
	 * @return JWT token signed with private RSA key
	 * @author Kevin Guanche Darias
	 */
	public TokenPojo login(String email, String password) {
		if (configurationBo.isSystemEmail(email) && configurationBo.isSystemPassword(password)) {
			User systemUser = new User();
			systemUser.setId(0);
			systemUser.setEmail(email);
			systemUser.setUsername("system");
			systemUser.setPassword("realPassword??? (:");
			return createToken(systemUser);
		} else {
			User user = userRepository.findByEmail(email);
			if (user == null) {
				throw new UserLoginException(EXCEPTION_INVALID_LOGIN_USER);
			}

			if (BCrypt.checkpw(password, user.getPassword())) {
				return createToken(user);
			} else {
				LOGGER.info("Intento de inicio de sesión erroneo para el usuario : " + email);
				throw new UserLoginException(EXCEPTION_INVALID_LOGIN_CREDENTIALS);
			}
		}
	}

	/**
	 * Will register the user specified by the User object
	 * 
	 * @param user
	 * @return registered user
	 * @author Kevin Guanche Darias
	 */
	public User register(User user) {
		user.checkValid();
		user.setPassword(cryptPassword(user.getPassword()));
		return userRepository.save(user);
	}

	/**
	 * 
	 * @param password
	 * @return Password hash
	 * @author Kevin Guanche Darias
	 */
	private String cryptPassword(String password) {
		BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
		return passwordEncoder.encode(password);
	}

	private String findJwtSecret() {
		return configurationBo.findConfigurationParam(JWT_SECRET_DB_CODE).getValue();
	}

	private SignatureAlgorithm findSigningAlgo() {
		return SignatureAlgorithm.valueOf(configurationBo.findConfigurationParam(JWT_HASHING_ALGO).getValue());
	}

	private Date genTokenExpitarion() {
		Integer seconds = Integer.valueOf(configurationBo.findConfigurationParam(JWT_DURATION_CODE).getValue());
		return new Date((new Date().getTime()) + (seconds * 1000));
	}

	private void removeSentitiveData(User user) {
		user.setPassword(null);
		user.setFirstName(null);
		user.setLastName(null);
	}

	/**
	 * Will generate the token
	 * 
	 * @param userId
	 * @return
	 * @author Kevin Guanche Darias
	 */
	private TokenPojo createToken(User user) {
		removeSentitiveData(user);

		Map<String, Object> claims = new HashMap<>();
		claims.put("sub", user.getId());
		claims.put("iat", new Date());
		claims.put("exp", genTokenExpitarion());
		claims.put("data", user);

		TokenPojo token = new TokenPojo();
		token.setToken(jwtService.buildToken(claims, findSigningAlgo(), findJwtSecret()));
		token.setUserId(user.getId());
		return token;
	}
}
