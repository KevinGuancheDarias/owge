package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.ArrayList;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.entity.ExploredPlanet;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpecialLocation;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.SgtBackendUniverseIsFull;
import com.kevinguanchedarias.owgejava.repository.ExploredPlanetRepository;
import com.kevinguanchedarias.owgejava.repository.GalaxyRepository;
import com.kevinguanchedarias.owgejava.repository.SpecialLocationRepository;
import com.kevinguanchedarias.owgejava.test.helper.UserMockitoHelper;

@ActiveProfiles("dev")
@ContextConfiguration(locations = { "file:src/test/resources/dao-context.xml",
		"file:src/test/resources/embedded-database-context.xml" })
@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
public class PlanetBoTest extends TestCommon {

	@Autowired
	private PlanetBo planetBo;

	@Autowired
	private GalaxyRepository galaxyRepository;

	@Autowired
	private SpecialLocationRepository specialLocationRepository;

	private UserStorage user;

	private ExploredPlanetRepository explorePlanetRepositoryMock;

	@Before
	public void init() {
		user = persistValidUserStorage(1);
		explorePlanetRepositoryMock = Mockito.mock(ExploredPlanetRepository.class);
	}

	@Test(expected = SgtBackendUniverseIsFull.class)
	public void shouldThrowUniverseFullBecauseIsEmpty() {
		planetBo.findRandomPlanet(1);
	}

	@Test(expected = SgtBackendUniverseIsFull.class)
	public void shouldReturnUniverseFullTooBecauseThereAreNotFreePlanets() {
		Galaxy galaxy = prepareGalaxy(300L, user, false);
		Galaxy galaxy2 = prepareGalaxy(100L, null, true);
		galaxy.getPlanets().addAll(galaxy2.getPlanets());
		galaxy = galaxyRepository.save(galaxy);

		planetBo.findRandomPlanet(galaxy.getId());
	}

	@Test
	public void shouldReturnOnePlanet() {
		Galaxy galaxy = prepareGalaxy(1L, null, false);
		galaxy = galaxyRepository.save(galaxy);
		galaxy = galaxyRepository.findOne(galaxy.getId());
		assertNotNull(planetBo.findRandomPlanet(galaxy.getId()));
	}

	@Test
	public void shouldReturnOnePlanetToo() {
		Galaxy galaxy = prepareGalaxy(200L, null, false);
		galaxy = galaxyRepository.save(galaxy);
		galaxy = galaxyRepository.findOne(galaxy.getId());
		assertNotNull(planetBo.findRandomPlanet(galaxy.getId()));
	}

	@Test
	public void shouldUseRandomGalaxyWhenNullIsPassed() {
		Galaxy galaxy = prepareGalaxy(200L, null, false);
		galaxy = galaxyRepository.save(galaxy);
		assertNotNull(planetBo.findRandomPlanet(null));
	}

	@Test
	public void shouldDefinePlanetAsExplored() {
		ArgumentCaptor<ExploredPlanet> captor = ArgumentCaptor.forClass(ExploredPlanet.class);
		Whitebox.setInternalState(planetBo, "exploredPlanetRepository", explorePlanetRepositoryMock);
		Mockito.when(explorePlanetRepositoryMock.save(captor.capture())).thenReturn(null);
		UserStorage user = prepareValidUser(1);
		Planet planet = preparePlanet("1234", null);
		planetBo.defineAsExplored(user, planet);
		ExploredPlanet savedPlanet = captor.getValue();
		assertEquals(user, savedPlanet.getUser());
		assertEquals(planet, savedPlanet.getPlanet());
		Mockito.verify(explorePlanetRepositoryMock, Mockito.times(1)).save(savedPlanet);
	}

	@Test
	public void shouldInvokeDefinePlanetAsExploredAsLoggedInUser() {
		UserMockitoHelper userMockitoHelper = new UserMockitoHelper(planetBo);
		UserStorage user = prepareValidUser(1);
		Planet planet = preparePlanet("1234", null);
		userMockitoHelper.fakeLoggedIn(user);
		PlanetBo planetBoSpy = Mockito.spy(planetBo);
		planetBoSpy.myDefineAsExplored(planet);
		Mockito.verify(planetBoSpy, Mockito.times(1)).defineAsExplored(user, planet);
	}

	/**
	 * Prepares a persistent entire galaxy with number of planets <br />
	 * NOTICE: This doesn't generated a fully valid galaxy (with valid sectors
	 * and quadrants)
	 * 
	 * @param numberOfPlanets
	 * @param owner
	 * @param addSpecial
	 *            - Should consider the planet a "special location"
	 * @return
	 * @author Kevin Guanche Darias
	 */
	private Galaxy prepareGalaxy(Long numberOfPlanets, UserStorage owner, Boolean addSpecial) {
		Galaxy galaxy = new Galaxy();
		galaxy.setName("Vía test");
		galaxy.setSectors(1L);
		galaxy.setQuadrants(1L);
		galaxyRepository.save(galaxy);

		galaxy.setPlanets(new ArrayList<>());
		for (long i = 0; i < numberOfPlanets; i++) {
			Planet currentPlanet = preparePlanet("I" + i, owner);
			currentPlanet.setGalaxy(galaxy);
			if (addSpecial) {
				SpecialLocation currentSpecialLocation = new SpecialLocation();
				currentSpecialLocation.setName("some");
				specialLocationRepository.save(currentSpecialLocation);
				currentPlanet.setSpecialLocation(currentSpecialLocation);
			}
			galaxy.getPlanets().add(currentPlanet);
		}
		return galaxyRepository.save(galaxy);
	}

	private Planet preparePlanet(String name, UserStorage owner) {
		Planet planet = new Planet();
		planet.setName(name);
		planet.setSector(1L);
		planet.setQuadrant(1L);
		planet.setOwner(owner);
		return planet;
	}
}
