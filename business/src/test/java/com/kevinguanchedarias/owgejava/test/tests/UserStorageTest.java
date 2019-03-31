package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.entity.Faction;
import com.kevinguanchedarias.owgejava.entity.UserImprovement;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;

@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class UserStorageTest extends TestCommon {

	@Autowired
	private UserStorageRepository repository;

	private UserStorage user;

	@Before
	public void init() {
		user = prepareValidUser(1);
	}

	@Test
	public void shouldNotSaveBecauseFactionIsNull() {
		user.setFaction(null);
		checkNullSaveException(repository, user, "faction");
	}

	@Test
	public void shouldNotSaveBecauseHomePlanetIsNull() {
		user.setHomePlanet(null);
		checkNullSaveException(repository, user, "homePlanet");
	}

	@Test
	public void shouldNotSaveBecauseEnergyIsNull() {
		user.setEnergy(null);
		checkNullSaveException(repository, user, "energy");
	}

	@Test
	public void shouldSave() {
		repository.save(user);
	}

	@Test
	public void shouldProperlyComputeTransient() {
		user = new UserStorage();
		Faction faction = new Faction();
		faction.setPrimaryResourceProduction(2F);
		faction.setSecondaryResourceProduction(4F);
		faction.setInitialEnergy(100);

		UserImprovement improvement = new UserImprovement();
		improvement.setMorePrimaryResourceProduction(10F);
		improvement.setMoreSecondaryResourceProduction(20F);
		improvement.setMoreEnergyProduction(30F);

		user.setFaction(faction);
		user.setImprovements(improvement);

		user.fillTransientValues();
		assertEquals(2.2F, user.getComputedPrimaryResourceGenerationPerSecond(), 0.1F);
		assertEquals(4.8F, user.getComputedSecondaryResourceGenerationPerSecond(), 0.1F);
	}
}
