package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.owgejava.business.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.PlanetBo;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;

@RunWith(MockitoJUnitRunner.class)
public class ObtainedUnitBoTest {

	private static final Integer USER_ID = 1;
	private static final Integer UNIT_ID = 1;
	private static final Long OBTAINED_UNIT_ID = 4L;
	private static final Long SOURCE_PLANET_ID = 1L;

	@Mock
	private ObtainedUnitRepository repositoryMock;

	@Mock
	private PlanetBo planetBoMock;

	@InjectMocks
	private ObtainedUnitBo obtainedUnitBo;

	private UserStorage user;
	private ObtainedUnit obtainedUnit;
	private Planet sourcePlanet;

	@Before
	public void init() {
		user = new UserStorage();
		user.setId(USER_ID);

		Unit unit = new Unit();
		unit.setId(UNIT_ID);

		obtainedUnit = new ObtainedUnit();
		obtainedUnit.setUnit(unit);
		obtainedUnit.setId(OBTAINED_UNIT_ID);
		obtainedUnit.setCount(1L);

		sourcePlanet = new Planet();
		sourcePlanet.setId(SOURCE_PLANET_ID);
		Mockito.when(planetBoMock.isOfUserProperty(USER_ID, SOURCE_PLANET_ID)).thenReturn(true);
	}

	@Test(expected = SgtBackendNotImplementedException.class)
	public void shouldThrowBecauseNonDeployedInLocalPlanetSavingIsNotSupported() {
		Planet otherPlanet = new Planet();
		otherPlanet.setId(2L);
		obtainedUnit.setSourcePlanet(otherPlanet);
		obtainedUnitBo.saveWithAdding(USER_ID, obtainedUnit);
	}

	@Test
	public void shouldSaveAddedOneWhenUnitDoesNotExistsInDatabase() {
		obtainedUnit.setSourcePlanet(sourcePlanet);

		obtainedUnitBo.saveWithAdding(USER_ID, obtainedUnit);
		Mockito.verify(repositoryMock).save(obtainedUnit);
		Mockito.verify(repositoryMock, Mockito.never()).delete(Mockito.any(ObtainedUnit.class));
	}

	@Test
	public void shouldSaveExistingOneWhenUnitExistsInDatabaseDeletingAddedIfHasId() {
		obtainedUnit.setSourcePlanet(sourcePlanet);

		ObtainedUnit existingOne = new ObtainedUnit();
		existingOne.setId(2L);
		existingOne.setCount(1L);
		Mockito.when(repositoryMock.findOneByUserIdAndUnitIdAndSourcePlanetIdAndIdNotAndMissionNull(USER_ID, UNIT_ID,
				SOURCE_PLANET_ID, obtainedUnit.getId())).thenReturn(existingOne);
		obtainedUnitBo.saveWithAdding(USER_ID, obtainedUnit);
		Mockito.verify(repositoryMock).save(existingOne);
		Mockito.verify(repositoryMock).delete(obtainedUnit);

	}

	@Test
	public void shouldSaveExistingOneWhenUnitExistsInDatabaseNotDeletingIfHasNotId() {
		obtainedUnit.setSourcePlanet(sourcePlanet);
		obtainedUnit.setId(null);

		ObtainedUnit existingOne = new ObtainedUnit();
		existingOne.setId(2L);
		existingOne.setCount(1L);
		Mockito.when(repositoryMock.findOneByUserIdAndUnitIdAndSourcePlanetIdAndIdNotAndMissionNull(USER_ID, UNIT_ID,
				SOURCE_PLANET_ID, obtainedUnit.getId())).thenReturn(existingOne);
		obtainedUnitBo.saveWithAdding(USER_ID, obtainedUnit);
		Mockito.verify(repositoryMock).save(existingOne);
		Mockito.verify(repositoryMock, Mockito.never()).delete(obtainedUnit);
	}

	@Test(expected = SgtBackendInvalidInputException.class)
	public void shouldNotSaveWithSubstractionBecauseCountIsInvalid() {
		obtainedUnit.setCount(2L);
		obtainedUnitBo.saveWithSubtraction(obtainedUnit, 4L, false);
	}

	@Test
	public void shouldProperlySubtractFromObtainedUnitUpdatingDbsCount() {
		obtainedUnit.setCount(6L);
		obtainedUnitBo.saveWithSubtraction(obtainedUnit, 4L, false);
		assertEquals(2L, (long) obtainedUnit.getCount());
		Mockito.verify(repositoryMock, Mockito.times(1)).save(obtainedUnit);

	}

	@Test
	public void shouldRemoveFromDatabaseWhenCountIsEqual() {
		long count = 2L;
		obtainedUnit.setCount(count);
		obtainedUnitBo.saveWithSubtraction(obtainedUnit, count, false);
		Mockito.verify(repositoryMock, Mockito.never()).save(Mockito.any(ObtainedUnit.class));
		Mockito.verify(repositoryMock, Mockito.times(1)).delete(obtainedUnit);

	}

	@Test
	public void testFindHavingSameUnit() {
		Unit searchedUnit = new Unit();
		searchedUnit.setId(1);
		Unit boilerPlatetUnit = new Unit();
		boilerPlatetUnit.setId(2);
		Unit boilerPlateUnit2 = new Unit();
		boilerPlateUnit2.setId(3);
		ObtainedUnit expectedObtainedUnit = new ObtainedUnit();
		expectedObtainedUnit.setUnit(searchedUnit);
		ObtainedUnit searchObtainedUnit = new ObtainedUnit();
		searchObtainedUnit.setUnit(searchedUnit);
		ObtainedUnit boilerPlateObtainedUnit = new ObtainedUnit();
		boilerPlateObtainedUnit.setUnit(boilerPlatetUnit);
		ObtainedUnit boilerPlateObtainedUnit2 = new ObtainedUnit();
		boilerPlateObtainedUnit2.setUnit(boilerPlateUnit2);
		List<ObtainedUnit> storage = new ArrayList<>();
		storage.add(boilerPlateObtainedUnit);
		storage.add(boilerPlateObtainedUnit2);
		assertNull(obtainedUnitBo.findHavingSameUnit(storage, searchObtainedUnit));
		storage.add(expectedObtainedUnit);
		assertEquals(expectedObtainedUnit, (obtainedUnitBo.findHavingSameUnit(storage, searchObtainedUnit)));
	}

	@Test
	public void notNullFindConsumeEnergyByUser() {
		assertNotNull(obtainedUnitBo.findConsumeEnergyByUser(user));
	}

}
