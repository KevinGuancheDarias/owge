package com.kevinguanchedarias.sgtjava.test.tests;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.sgtjava.entity.Upgrade;
import com.kevinguanchedarias.sgtjava.entity.UpgradeType;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.repository.ObtainedUpgradeRepository;

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
