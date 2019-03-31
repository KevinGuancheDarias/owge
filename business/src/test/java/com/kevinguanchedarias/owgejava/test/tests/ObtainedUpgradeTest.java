package com.kevinguanchedarias.owgejava.test.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UpgradeType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;

@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class ObtainedUpgradeTest extends TestCommon {

	@Autowired
	private ObtainedUpgradeRepository obtainedUpgradeRepository;

	@Test
	public void shouldSave() {
		UserStorage user = persistValidUserStorage(1);
		UpgradeType type = persistValidUpgradeType();
		Upgrade upgrade = persistValidUpgrade(type);

		ObtainedUpgrade obtainedUpgrade = prepareValidObtainedUpgrade(user, upgrade);

		obtainedUpgrade = obtainedUpgradeRepository.save(obtainedUpgrade);
	}

	@Test(expected = JpaSystemException.class)
	public void shouldNotSave() {
		obtainedUpgradeRepository.save(new ObtainedUpgrade());
	}
}
