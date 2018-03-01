package com.kevinguanchedarias.sgtjava.test.tests;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.entity.UserImprovement;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.repository.UserImprovementRepository;

@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class UserImprovementTest extends TestCommon {

	@Autowired
	private UserImprovementRepository repository;

	private UserStorage user;

	@Before
	public void init() {
		user = persistValidUserStorage(1);
	}

	@Test
	public void shouldNotSaveBecauseUserIsNull() {
		UserImprovement improvement = new UserImprovement();
		checkNullSaveException(repository, improvement, "user");
	}

	@Test
	public void shouldSave() {
		repository.save(prepareValidUserImprovement());
	}

	private UserImprovement prepareValidUserImprovement() {
		UserImprovement retVal = new UserImprovement();
		retVal.setUser(user);
		return retVal;
	}
}
