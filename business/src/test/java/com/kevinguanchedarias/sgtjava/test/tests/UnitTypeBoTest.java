package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import com.kevinguanchedarias.sgtjava.business.PlanetBo;
import com.kevinguanchedarias.sgtjava.business.UnitTypeBo;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.UnitType;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.MissionSupportEnum;
import com.kevinguanchedarias.sgtjava.enumerations.MissionType;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;

@RunWith(MockitoJUnitRunner.class)
public class UnitTypeBoTest {

	@Mock
	private PlanetBo planetBo;

	@InjectMocks
	private UnitTypeBo unitTypeBo;

	private UnitType unitTypeMock;
	private UserStorage userMock;
	private Planet planetMock;

	@Before
	public void init() {
		unitTypeMock = mock(UnitType.class);
		userMock = mock(UserStorage.class);
		planetMock = mock(Planet.class);
	}

	@Test
	public void canDoMissionProperlyHandlesNoneValue() {
		doReturn(MissionSupportEnum.NONE).when(unitTypeMock).getCanEstablishBase();
		assertFalse(unitTypeBo.canDoMission(userMock, planetMock, unitTypeMock, MissionType.ESTABLISH_BASE));

		verify(unitTypeMock).getCanEstablishBase();
		verify(planetBo, never()).isOfUserProperty(userMock, planetMock);
	}

	@Test
	public void canDoMissionProperlyHandlesOwnedOnlyValue() {
		doReturn(MissionSupportEnum.OWNED_ONLY).when(unitTypeMock).getCanAttack();
		doReturn(false).when(planetBo).isOfUserProperty(userMock, planetMock);
		assertFalse(unitTypeBo.canDoMission(userMock, planetMock, unitTypeMock, MissionType.ATTACK));

		verify(unitTypeMock).getCanAttack();
		verify(planetBo).isOfUserProperty(userMock, planetMock);

		doReturn(true).when(planetBo).isOfUserProperty(userMock, planetMock);
		assertTrue(unitTypeBo.canDoMission(userMock, planetMock, unitTypeMock, MissionType.ATTACK));
	}

	@Test
	public void canDoMissionProperlyHandlesAnyValue() {
		doReturn(MissionSupportEnum.ANY).when(unitTypeMock).getCanConquest();
		assertTrue(unitTypeBo.canDoMission(userMock, planetMock, unitTypeMock, MissionType.CONQUEST));

		verify(unitTypeMock).getCanConquest();
		verify(planetBo, never()).isOfUserProperty(userMock, planetMock);
	}

	@Test(expected = SgtBackendInvalidInputException.class)
	public void canDoMissionProperlyThrowsOnUnsupportedMission() {
		assertTrue(unitTypeBo.canDoMission(userMock, planetMock, unitTypeMock, MissionType.LEVEL_UP));

	}

	@Test
	public void canDoMissionProperlyHandlesCollections() {
		UnitType unitTypeMock2 = mock(UnitType.class);
		List<UnitType> unitTypes = new ArrayList<>();
		unitTypes.add(unitTypeMock);
		unitTypes.add(unitTypeMock2);

		doReturn(MissionSupportEnum.ANY).when(unitTypeMock).getCanConquest();
		doReturn(MissionSupportEnum.ANY).when(unitTypeMock2).getCanConquest();

		assertTrue(unitTypeBo.canDoMission(userMock, planetMock, unitTypes, MissionType.CONQUEST));

		doReturn(MissionSupportEnum.NONE).when(unitTypeMock).getCanConquest();

		assertFalse(unitTypeBo.canDoMission(userMock, planetMock, unitTypes, MissionType.CONQUEST));
		
		doReturn(MissionSupportEnum.NONE).when(unitTypeMock2).getCanConquest();

		assertFalse(unitTypeBo.canDoMission(userMock, planetMock, unitTypes, MissionType.CONQUEST));
	}
}
