package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.owgejava.business.UpgradeBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.pojo.ResourceRequirementsPojo;

@RunWith(MockitoJUnitRunner.class)
public class UpgradeBoTest extends TestCommon {

	@InjectMocks
	private UpgradeBo upgradeBo;

	private ObtainedUpgrade obtainedUpgrade;
	private Upgrade upgrade;
	private Integer userId = 1;
	private UserStorage user;

	@Before
	public void init() {
		user = new UserStorage();
		user.setId(userId);

		upgrade = new Upgrade();
		upgrade.setPrimaryResource(100);
		upgrade.setSecondaryResource(120);
		upgrade.setTime(30L);
		upgrade.setLevelEffect(2.0F);

		obtainedUpgrade = new ObtainedUpgrade();
		obtainedUpgrade.setAvailable(true);
		obtainedUpgrade.setLevel(0);
		obtainedUpgrade.setUpgrade(upgrade);
		obtainedUpgrade.setUserId(user);
	}

	/**
	 * Requirements should exactly match the upgrade values!
	 * 
	 * @author Kevin Guanche Darias
	 */
	@Test
	public void requirementsShouldNotBeDifferentWhenLevelIsZero() {
		ResourceRequirementsPojo requiredResources = upgradeBo.calculateRequirementsAreMet(obtainedUpgrade);
		assertEquals(upgrade.getPrimaryResource().intValue(), requiredResources.getRequiredPrimary().intValue());
		assertEquals(upgrade.getSecondaryResource().intValue(), requiredResources.getRequiredSecondary().intValue());
		assertEquals(upgrade.getTime().intValue(), requiredResources.getRequiredTime().intValue());

	}

	@Test
	public void shouldBeHighetWhenLevelOne() {
		obtainedUpgrade.setLevel(1);
		ResourceRequirementsPojo requiredResources = upgradeBo.calculateRequirementsAreMet(obtainedUpgrade);
		assertEquals(300, requiredResources.getRequiredPrimary().intValue());
		assertEquals(360, requiredResources.getRequiredSecondary().intValue());
		assertEquals(90, requiredResources.getRequiredTime().intValue());
	}

	@Test
	public void shouldIncreaseEvenMoreWhenLevelTwo() {
		obtainedUpgrade.setLevel(2);
		ResourceRequirementsPojo requiredResources = upgradeBo.calculateRequirementsAreMet(obtainedUpgrade);
		assertEquals(900, requiredResources.getRequiredPrimary().intValue());
		assertEquals(1080, requiredResources.getRequiredSecondary().intValue());
		assertEquals(270, requiredResources.getRequiredTime().intValue());
	}
}
