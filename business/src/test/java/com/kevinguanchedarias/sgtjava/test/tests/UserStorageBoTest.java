package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.springframework.beans.BeanUtils;

import com.kevinguanchedarias.kevinsuite.commons.rest.security.SecurityContextService;
import com.kevinguanchedarias.kevinsuite.commons.rest.security.TokenUser;
import com.kevinguanchedarias.sgtjava.business.FactionBo;
import com.kevinguanchedarias.sgtjava.business.PlanetBo;
import com.kevinguanchedarias.sgtjava.business.RequirementBo;
import com.kevinguanchedarias.sgtjava.business.UserImprovementBo;
import com.kevinguanchedarias.sgtjava.business.UserStorageBo;
import com.kevinguanchedarias.sgtjava.entity.Faction;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.UserImprovement;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.exception.SgtFactionNotFoundException;
import com.kevinguanchedarias.sgtjava.repository.UserStorageRepository;

@RunWith(MockitoJUnitRunner.class)
public class UserStorageBoTest extends TestCommon {

	@Mock
	private UserStorageRepository userStorageRepository;

	@Mock
	private SecurityContextService securityContextService;

	@Mock
	private FactionBo factionBo;

	@Mock
	private PlanetBo planetBo;

	@Mock
	private RequirementBo requirementBo;

	@Mock
	private UserImprovementBo userImprovementBoMock;

	@InjectMocks
	private UserStorageBo userStorageBo;

	@Before
	public void init() {
		// By default always return faction exists
		Mockito.when(factionBo.exists(Mockito.any(Number.class))).thenReturn(true);
	}

	@Test
	public void shouldSayUserDoesntExists() {
		Mockito.when(userStorageRepository.exists(Mockito.any(Integer.class))).thenReturn(false);

		assertFalse(userStorageBo.exists(4));
	}

	@Test
	public void shouldSayUserExists() {
		Mockito.when(userStorageRepository.exists(Mockito.any(Integer.class))).thenReturn(true);

		assertTrue(userStorageBo.exists(1));
	}

	@Test
	public void shouldReturnProperUserData() {
		Integer validId = 2;
		String validUsername = "Kevin";
		String validEmail = "Papote";

		TokenUser tokenUser = new TokenUser();
		tokenUser.setId(validId);
		tokenUser.setUsername(validUsername);
		tokenUser.setEmail(validEmail);
		fakeFinLoggedInUser(tokenUser);

		UserStorage returnedUser = userStorageBo.findLoggedIn();

		assertNotNull(returnedUser);
		assertEquals(tokenUser.getId(), returnedUser.getId());
		assertEquals(tokenUser.getUsername(), returnedUser.getUsername());
		assertEquals(tokenUser.getEmail(), returnedUser.getEmail());
	}

	@Test(expected = SgtFactionNotFoundException.class)
	public void shouldThrowExceptionWhenSubscribingBecauseFactionDoesNotExists() {
		fakeFinLoggedInUser(null);
		Mockito.when(factionBo.exists(Mockito.any(Number.class))).thenReturn(false);
		userStorageBo.subscribe(1);
	}

	@Test
	public void shouldReturnFalseWhenSubscribingBecauseUserIsAlreadySubscribed() {
		fakeFinLoggedInUser(null);
		Mockito.when(userStorageRepository.exists(Mockito.anyInt())).thenReturn(true);
		assertFalse(userStorageBo.subscribe(1));
	}

	@Test
	public void shouldReturnTrueWhenSubscribingBecauseUserExists() {
		fakeFinLoggedInUser(null);
		Mockito.when(factionBo.findById(Mockito.any(Number.class))).thenReturn(prepareValidFaction());
		Mockito.when(planetBo.findRandomPlanet(null)).thenReturn(new Planet());
		Mockito.when(userStorageRepository.exists(Mockito.anyInt())).thenReturn(false);
		Mockito.when(userStorageRepository.save(Mockito.any(UserStorage.class))).thenReturn(prepareValidUserStorage(1));
		assertTrue(userStorageBo.subscribe(1));
	}

	@Test
	public void shouldDefineMandatoryEntitiesWhenSubscribing() {
		fakeFinLoggedInUser(null);
		Mockito.when(factionBo.findById(Mockito.any(Number.class))).thenReturn(prepareValidFaction());
		Mockito.when(planetBo.findRandomPlanet(null)).thenReturn(new Planet());
		Mockito.when(userStorageRepository.exists(Mockito.anyInt())).thenReturn(false);
		Mockito.when(userStorageRepository.save(Mockito.any(UserStorage.class))).thenReturn(prepareValidUserStorage(1));
		assertTrue(userStorageBo.subscribe(1));
		Mockito.verify(userImprovementBoMock, Mockito.times(1)).findUserImprovements(Mockito.any(UserStorage.class));
	}

	@Test
	public void shouldNotFillTransientValuesWhenNotRequestedTo() {
		UserStorage user = genSimpleUserStorageDetails();
		Faction faction = prepareValidFaction();
		user.setFaction(faction);

		userStorageBo.findLoggedInWithDetails(false);

		assertNull(user.getComputedPrimaryResourceGenerationPerSecond());
		assertNull(user.getComputedSecondaryResourceGenerationPerSecond());
		assertNull(user.getComputedMaxEnergy());
	}

	@Test
	public void shouldFillTransientValuesWhenRequested() {
		Faction faction = prepareValidFaction();
		UserImprovement improvement = new UserImprovement();
		improvement.setMorePrimaryResourceProduction(10F);
		improvement.setMoreSecondaryResourceProduction(20F);
		improvement.setMoreEnergyProduction(30F);

		UserStorage user = genSimpleUserStorageDetails();
		user.setFaction(faction);
		user.setImprovements(improvement);

		userStorageBo.findLoggedInWithDetails(true);

		assertNotNull(user.getComputedPrimaryResourceGenerationPerSecond());
		assertNotNull(user.getComputedSecondaryResourceGenerationPerSecond());
		assertNotNull(user.getComputedMaxEnergy());
	}

	@Test
	public void shouldNotUpdateBaseInformationWhenNotChanged() {
		UserStorage detailsUser = genSimpleUserStorageDetails();
		userStorageBo.findLoggedInWithDetails(true);
		Mockito.verify(userStorageRepository, Mockito.never()).save(detailsUser);
	}

	@Test
	public void shouldUpdateBaseInformationWhenEmailChanged() {
		TokenUser simpleUser = fakeFinLoggedInUser(null);
		UserStorage detailsUser = new UserStorage();
		BeanUtils.copyProperties(simpleUser, detailsUser);
		simpleUser.setEmail("lol@dot.com");
		Mockito.when(userStorageRepository.findOne(simpleUser.getId())).thenReturn(detailsUser);
		userStorageBo.findLoggedInWithDetails(true);
		Mockito.verify(userStorageRepository).save(detailsUser);
	}

	@Test
	public void shouldUpdateBaseInformationWhenUsernameChanged() {
		TokenUser simpleUser = fakeFinLoggedInUser(null);
		UserStorage detailsUser = new UserStorage();
		BeanUtils.copyProperties(simpleUser, detailsUser);
		simpleUser.setUsername("lol@dot.com");
		Mockito.when(userStorageRepository.findOne(simpleUser.getId())).thenReturn(detailsUser);
		userStorageBo.findLoggedInWithDetails(true);
		Mockito.verify(userStorageRepository).save(detailsUser);
	}

	@Test
	public void shouldUpdateResources() {
		double generationPerSecond = 0.5;
		TokenUser simpleUser = fakeFinLoggedInUser(null);
		UserStorage detailsUser = new UserStorage();
		BeanUtils.copyProperties(simpleUser, detailsUser);

		Faction faction = prepareValidFaction();
		faction.setPrimaryResourceProduction((float) generationPerSecond);
		faction.setSecondaryResourceProduction((float) generationPerSecond);

		UserImprovement improvement = new UserImprovement();

		detailsUser.setFaction(faction);
		detailsUser.setImprovements(improvement);
		detailsUser.setLastAction(new DateTime().minusMinutes(1).toDate());
		detailsUser.setPrimaryResource(0.0);
		detailsUser.setSecondaryResource(0.0);

		Mockito.when(userStorageRepository.findOne(simpleUser.getId())).thenReturn(detailsUser);
		userStorageBo.triggerResourcesUpdate();
		assertEquals(30, detailsUser.getPrimaryResource().longValue());
		assertEquals(30, detailsUser.getSecondaryResource().longValue());
	}

	/**
	 * Will fake the request to {@link SecurityContextService}
	 * 
	 * @param tokenUser
	 *            If null will create a valid one
	 * @return - The user used to mock
	 * @author Kevin Guanche Darias
	 */
	private TokenUser fakeFinLoggedInUser(TokenUser tokenUser) {
		TokenUser retVal = tokenUser;
		if (retVal == null) {
			retVal = new TokenUser();
			retVal.setId(1);
			retVal.setUsername("Paco");
			retVal.setEmail("Milano");
		}

		Mockito.when(securityContextService.getAuthentication()).thenReturn(retVal);
		return retVal;
	}

	private UserStorage prepareValidUserStorage(Integer id) {
		UserStorage user = new UserStorage();
		user.setId(id);
		user.setEmail("user" + id + "@lol.com");
		user.setUsername("user" + id);
		return user;
	}

	private UserStorage genSimpleUserStorageDetails() {
		TokenUser simpleUser = fakeFinLoggedInUser(null);
		UserStorage detailsUser = new UserStorage();
		BeanUtils.copyProperties(simpleUser, detailsUser);

		Mockito.when(userStorageRepository.findOne(simpleUser.getId())).thenReturn(detailsUser);

		return detailsUser;
	}
}
