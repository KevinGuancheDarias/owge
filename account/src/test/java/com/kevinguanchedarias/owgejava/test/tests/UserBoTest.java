package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Date;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import com.kevinguanchedarias.owgejava.business.ConfigurationBo;
import com.kevinguanchedarias.owgejava.business.JwtService;
import com.kevinguanchedarias.owgejava.business.UserBo;
import com.kevinguanchedarias.owgejava.entity.Configuration;
import com.kevinguanchedarias.owgejava.entity.User;
import com.kevinguanchedarias.owgejava.exception.InvalidInputException;
import com.kevinguanchedarias.owgejava.exception.UserLoginException;
import com.kevinguanchedarias.owgejava.repository.UserRepository;

import io.jsonwebtoken.SignatureAlgorithm;

@RunWith(MockitoJUnitRunner.class)
public class UserBoTest extends TestCommon {

	private static final String DEFAULT_USER_EMAIL = "kevin@kevinguanchedarias.com";
	private static final String DEFAULT_USER_PASSWORD = "1234";

	@Mock
	private UserRepository userRepositoryMock;

	@Mock
	private ConfigurationBo configurationBoMock;

	@Mock
	private JwtService jwtServiceMock;

	@InjectMocks
	private UserBo userBo;

	@Test
	public void shouldReportAccountDoesNotExists() {
		try {
			userBo.login("kddfjdhjfhjf@djfijdifff.com", "jsjd");
			throw new RuntimeException("should throw exception!");
		} catch (UserLoginException e) {
			assertEquals(UserBo.EXCEPTION_INVALID_LOGIN_USER, e.getMessage());
		}
	}

	@Test
	public void shouldReportInvalidPassword() {
		try {
			User fakeUser = new User();
			fakeUser.setPassword(new BCryptPasswordEncoder().encode("1234"));
			Mockito.when(userRepositoryMock.findByEmail(DEFAULT_USER_EMAIL)).thenReturn(fakeUser);
			userBo.login(DEFAULT_USER_EMAIL, "jdfhdhf");
			throw new RuntimeException("should throw exception!");
		} catch (UserLoginException e) {
			assertEquals(UserBo.EXCEPTION_INVALID_LOGIN_CREDENTIALS, e.getMessage());
		}
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldCreateTokenWhenValidUserRemovingSensitiveData() {
		User fakeUser = new User();
		fakeUser.setId(74);
		fakeUser.setPassword(new BCryptPasswordEncoder().encode(DEFAULT_USER_PASSWORD));
		fakeUser.setFirstName("Expect null in token");
		fakeUser.setLastName("Should be null in token");
		Configuration fakeExpire = new Configuration("lol", "20");
		Configuration fakeAlgo = new Configuration("lal", "HS256");
		Configuration fakeSecret = new Configuration("lul", "1234");
		Mockito.when(configurationBoMock.findConfigurationParam(UserBo.JWT_DURATION_CODE)).thenReturn(fakeExpire);
		Mockito.when(configurationBoMock.findConfigurationParam(UserBo.JWT_HASHING_ALGO)).thenReturn(fakeAlgo);
		Mockito.when(configurationBoMock.findConfigurationParam(UserBo.JWT_SECRET_DB_CODE)).thenReturn(fakeSecret);
		Mockito.when(userRepositoryMock.findByEmail(DEFAULT_USER_EMAIL)).thenReturn(fakeUser);
		Answer<String> buildTokenAnswer = invocation -> {
			Map<String, Object> claims = (Map<String, Object>) invocation.getArguments()[0];
			SignatureAlgorithm algo = (SignatureAlgorithm) invocation.getArguments()[1];
			String secret = (String) invocation.getArguments()[2];
			User userToTokenize = (User) claims.get("data");
			assertEquals(fakeUser.getId(), claims.get("sub"));
			assertTrue(claims.get("iat") instanceof Date);
			assertTrue(claims.get("exp") instanceof Date);
			assertEquals(fakeUser, userToTokenize);
			assertEquals(fakeAlgo.getValue(), algo.getValue());
			assertEquals(fakeSecret.getValue(), secret);
			assertNull(userToTokenize.getFirstName());
			assertNull(userToTokenize.getLastName());
			assertNull(userToTokenize.getPassword());
			return "generated";
		};
		Mockito.when(jwtServiceMock.buildToken(Mockito.anyMapOf(String.class, Object.class), Mockito.any(),
				Mockito.anyString())).then(buildTokenAnswer);
		assertEquals("generated", userBo.login(DEFAULT_USER_EMAIL, DEFAULT_USER_PASSWORD).getToken());
		Mockito.verify(jwtServiceMock, Mockito.times(1)).buildToken(Mockito.anyMapOf(String.class, Object.class),
				Mockito.any(), Mockito.anyString());
	}

	@Test(expected = InvalidInputException.class)
	public void shouldNotRegisterUserWhenNotValid() {
		userBo.register(new User());
	}

	@Test
	public void shouldRegisterUserTestingItIsValidAndCryptingPassword() {
		User fakeUserSpy = Mockito.spy(prepareValidUser());
		Mockito.when(userRepositoryMock.save(fakeUserSpy)).thenReturn(fakeUserSpy);
		User savedUser = userBo.register(fakeUserSpy);
		Mockito.verify(fakeUserSpy, Mockito.times(1)).checkValid();
		assertNotEquals("1234", savedUser.getPassword());
		assertTrue(fakeUserSpy == savedUser);
	}
}
