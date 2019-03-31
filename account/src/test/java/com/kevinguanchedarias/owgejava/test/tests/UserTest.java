package com.kevinguanchedarias.owgejava.test.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.entity.User;
import com.kevinguanchedarias.owgejava.exception.InvalidInputException;
import com.kevinguanchedarias.owgejava.repository.UserRepository;

@ContextConfiguration(locations = { "file:src/test/resources/test-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class UserTest extends TestCommon {

	@Autowired
	private UserRepository userRepository;

	@Test(expected = InvalidInputException.class)
	public void shouldNotInsertUniverseDueToNullValues() {
		User user = new User();
		checkNullSaveException(userRepository, user, null);
		user.checkValid();
	}

	@Test(expected = InvalidInputException.class)
	public void shouldNotInsertUniverseDueToNullUsername() {
		User user = prepareValidUser();
		user.setUsername(null);
		checkNullSaveException(userRepository, user, "username");
		user.checkValid();
	}

	@Test(expected = InvalidInputException.class)
	public void shouldNotInsertUniverseDueToNullEmail() {
		User user = prepareValidUser();
		user.setEmail(null);
		checkNullSaveException(userRepository, user, "email");
		user.checkValid();
	}

	@Test(expected = InvalidInputException.class)
	public void shouldNotInsertUniverseDueToNullPassword() {
		User user = prepareValidUser();
		user.setPassword(null);
		checkNullSaveException(userRepository, user, "password");
		user.checkValid();
	}

	@Test(expected = InvalidInputException.class)
	public void shouldNotInsertUniverseDueToNullCreationDate() {
		User user = prepareValidUser();
		user.setCreationDate(null);
		checkNullSaveException(userRepository, user, "creationDate");
		user.checkValid();
	}

	@Test(expected = InvalidInputException.class)
	public void shouldNotInsertUniverseDueToNullLastLogin() {
		User user = prepareValidUser();
		user.setLastLogin(null);
		checkNullSaveException(userRepository, user, "lastLogin");
		user.checkValid();
	}

	@Test(expected = InvalidInputException.class)
	public void shouldNotSaveBecauseSystemIsReservedKeyword() {
		User user = prepareValidUser();
		user.setUsername("system");
		user.checkValid();
	}

	@Test
	public void shouldSave() {
		User user = prepareValidUser();
		userRepository.save(user);
	}
}
