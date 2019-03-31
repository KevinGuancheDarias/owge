package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.owgejava.business.UserImprovementBo;
import com.kevinguanchedarias.owgejava.entity.Improvement;
import com.kevinguanchedarias.owgejava.entity.UserImprovement;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.repository.UserImprovementRepository;

@RunWith(MockitoJUnitRunner.class)
public class UserImprovementBoTest {

	@Mock
	private UserImprovementRepository userImprovementRepositoryMock;

	@InjectMocks
	private UserImprovementBo userImprovementBo;

	private UserStorage user;
	private UserImprovement userImprovement;
	private Improvement improvement;

	@Before
	public void init() {
		user = new UserStorage();
		user.setId(1);

		userImprovement = new UserImprovement();
		userImprovement.setId(1);
		userImprovement.setUser(user);
		userImprovement.setMorePrimaryResourceProduction(20.0F);
		userImprovement.setMoreSecondaryResourceProduction(30.0F);
		userImprovement.setMoreEnergyProduction(40.0F);
		userImprovement.setMoreChargeCapacity(50.0F);
		userImprovement.setMoreMisions(60.0F);
		Mockito.when(userImprovementRepositoryMock.findOneByUserId(1)).thenReturn(userImprovement);

		improvement = new Improvement();
		improvement.setMorePrimaryResourceProduction(2.0F);
		improvement.setMoreSecondaryResourceProduction(3.0F);
		improvement.setMoreEnergyProduction(4.0F);
		improvement.setMoreChargeCapacity(5.0F);
		improvement.setMoreMisions(6.0F);
	}

	@Test
	public void shouldReturnTheInstanceIfExists() {
		assertTrue(userImprovement == userImprovementBo.findUserImprovements(user));
	}

	@Test
	public void shouldReturnAnewNotPersistedInstanceIfNotExists() {
		Mockito.when(userImprovementRepositoryMock.findOneByUserId(1)).thenReturn(null);
		UserImprovement retVal = userImprovementBo.findUserImprovements(user);
		assertFalse(userImprovement == retVal);
		assertNull(retVal.getId());
		assertEquals(user, retVal.getUser());

	}

	@Test
	public void shouldAddMoreSoldiers() {
		userImprovementBo.addImprovements(improvement, user);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldAddMorePr() {
		userImprovementBo.addImprovements(improvement, user);
		assertEquals(22F, userImprovement.getMorePrimaryResourceProduction(), 1);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldAddMoreSr() {
		userImprovementBo.addImprovements(improvement, user);
		assertEquals(33F, userImprovement.getMoreSecondaryResourceProduction(), 1);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldAddMoreEnergy() {
		userImprovementBo.addImprovements(improvement, user);
		assertEquals(44F, userImprovement.getMoreEnergyProduction(), 1);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldAddMoreCharge() {
		userImprovementBo.addImprovements(improvement, user);
		assertEquals(55F, userImprovement.getMoreChargeCapacity(), 1);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldAddMoreMissions() {
		userImprovementBo.addImprovements(improvement, user);
		assertEquals(66F, userImprovement.getMoreMisions(), 1);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldSubtractMoreSoldiers() {
		userImprovementBo.subtractImprovements(improvement, user);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldSubtractMorePr() {
		userImprovementBo.subtractImprovements(improvement, user);
		assertEquals(18F, userImprovement.getMorePrimaryResourceProduction(), 1);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldSubtractMoreSr() {
		userImprovementBo.subtractImprovements(improvement, user);
		assertEquals(27F, userImprovement.getMoreSecondaryResourceProduction(), 1);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldSubtractMoreEnergy() {
		userImprovementBo.subtractImprovements(improvement, user);
		assertEquals(36F, userImprovement.getMoreEnergyProduction(), 1);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldSubtractMoreCharge() {
		userImprovementBo.subtractImprovements(improvement, user);
		assertEquals(45F, userImprovement.getMoreChargeCapacity(), 1);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}

	@Test
	public void shouldSubtractMoreMissions() {
		userImprovementBo.subtractImprovements(improvement, user);
		assertEquals(54F, userImprovement.getMoreMisions(), 1);
		Mockito.verify(userImprovementRepositoryMock).save(userImprovement);
	}
}
