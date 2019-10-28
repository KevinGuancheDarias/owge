package com.kevinguanchedarias.owgejava.business;

import java.util.Map;

import org.springframework.stereotype.Service;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/**
 * This class is intended to remove dependency in static {@link Jwts} so we can
 * run unit test
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Service
public class JwtService {

	/**
	 * Creates a JWT token
	 * 
	 * @param claims
	 *            Add properties to the token, such as iat, exp, sub
	 * @param algo
	 *            Signing algorithm used
	 * @param secret
	 *            Secret to use to checksum the token
	 * @return JWT token as string
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String buildToken(Map<String, Object> claims, SignatureAlgorithm algo, String secret) {
		return Jwts.builder().setClaims(claims).signWith(algo, secret.getBytes()).compact();
	}
}
