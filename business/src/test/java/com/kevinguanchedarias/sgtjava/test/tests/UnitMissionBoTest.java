package com.kevinguanchedarias.sgtjava.test.tests;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SchedulerFactory;
import org.quartz.Trigger;

import com.kevinguanchedarias.sgtjava.business.ConfigurationBo;
import com.kevinguanchedarias.sgtjava.business.ObtainedUnitBo;
import com.kevinguanchedarias.sgtjava.business.UnitMissionBo;
import com.kevinguanchedarias.sgtjava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.MissionType;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.Unit;
import com.kevinguanchedarias.sgtjava.entity.UnitType;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.exception.CommonException;
import com.kevinguanchedarias.sgtjava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.sgtjava.exception.UserNotFoundException;
import com.kevinguanchedarias.sgtjava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.sgtjava.repository.MissionRepository;
import com.kevinguanchedarias.sgtjava.repository.MissionTypeRepository;
import com.kevinguanchedarias.sgtjava.test.helper.PlanetMockitoHelper;
import com.kevinguanchedarias.sgtjava.test.helper.UnitMockitoHelper;
import com.kevinguanchedarias.sgtjava.test.helper.UserMockitoHelper;

@RunWith(MockitoJUnitRunner.class)
public class UnitMissionBoTest {

	private static final Logger LOG = Logger.getLogger(UnitMissionBoTest.class);

	private static final UserStorage LOGGED_USER;
	static {
		LOGGED_USER = new UserStorage();
		LOGGED_USER.setId(1);
		LOGGED_USER.setUsername("test_user");
	}

	@Mock
	private ObtainedUnitBo obtainedUnitBoMock;

	@Mock
	private MissionRepository missionRepositoryMock;

	@Mock
	private ConfigurationBo configurationBo;

	@Mock
	private MissionTypeRepository missionTyperepositoryMock;

	@Mock
	private SchedulerFactory schedulerFactoryMock;

	@InjectMocks
	private UnitMissionBo unitMissionBo;

	private UserMockitoHelper userMockitoHelper;
	private PlanetMockitoHelper planetMockitoHelper;
	private UnitMockitoHelper unitMockitoHelper;

	@Before
	public void init() {
		userMockitoHelper = new UserMockitoHelper(unitMissionBo);
		planetMockitoHelper = new PlanetMockitoHelper(unitMissionBo);
		unitMockitoHelper = new UnitMockitoHelper(unitMissionBo);
	}

	@Test(expected = SgtBackendInvalidInputException.class)
	public void shouldCheckInvoker() {
		userMockitoHelper.fakeLoggedIn(LOGGED_USER);
		UnitMissionInformation information = prepareValidMissionInformation();
		information.setUserId(2);
		unitMissionBo.myRegisterExploreMission(information);
	}

	@Test(expected = UserNotFoundException.class)
	public void shouldCheckUserExists() {
		userMockitoHelper.fakeLoggedIn(LOGGED_USER);
		unitMissionBo.myRegisterExploreMission(prepareValidMissionInformation());
	}

	@Test(expected = PlanetNotFoundException.class)
	public void shouldCheckSourcePlanetExists() {
		userMockitoHelper.fakeLoggedIn(LOGGED_USER);
		userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
		unitMissionBo.myRegisterExploreMission(prepareValidMissionInformation());
	}

	@Test(expected = PlanetNotFoundException.class)
	public void shouldCheckTargetPlanetExists() {
		userMockitoHelper.fakeLoggedIn(LOGGED_USER);
		userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
		planetMockitoHelper.fakePlanetExists(1L, new Planet());
		unitMissionBo.myRegisterExploreMission(prepareValidMissionInformation());
	}

	@Test
	public void shouldCheckInvolvedUnitsNotEmpty() {
		wrapAssertExceptionMessage("involvedUnits can't be empty", message -> {
			userMockitoHelper.fakeLoggedIn(LOGGED_USER);
			userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
			planetMockitoHelper.fakePlanetExists(1L, new Planet());
			planetMockitoHelper.fakePlanetExists(2L, new Planet());
			UnitMissionInformation information = prepareValidMissionInformation();
			information.setInvolvedUnits(new ArrayList<>());
			unitMissionBo.myRegisterExploreMission(information);
		});
	}

	@Test
	public void shouldCheckInvolvedUnitExists() {
		wrapAssertExceptionMessage("No obtainedUnit with id 1 was found, nice try, dirty hacker!", message -> {
			userMockitoHelper.fakeLoggedIn(LOGGED_USER);
			userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
			planetMockitoHelper.fakePlanetExists(1L, new Planet());
			planetMockitoHelper.fakePlanetExists(2L, new Planet());
			unitMissionBo.myRegisterExploreMission(prepareValidMissionInformation());
		});
	}

	@Test
	public void shouldCheckInvolvedUnitNotInMission() {
		wrapAssertExceptionMessage("obtainedUnit already involved in mission", message -> {
			userMockitoHelper.fakeLoggedIn(LOGGED_USER);
			userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
			planetMockitoHelper.fakePlanetExists(1L, new Planet());
			planetMockitoHelper.fakePlanetExists(2L, new Planet());
			ObtainedUnit obtainedUnit = prepareObtainedUnit();
			obtainedUnit.setMission(new Mission());
			unitMockitoHelper.fakeObtainedUnitExists(1L, obtainedUnit);
			unitMissionBo.myRegisterExploreMission(prepareValidMissionInformation());
			Scheduler schedulerMock = Mockito.mock(Scheduler.class);
			try {
				Mockito.when(schedulerFactoryMock.getScheduler()).thenReturn(schedulerMock);
				Mockito.verify(schedulerFactoryMock, Mockito.times(1)).getScheduler();
				Mockito.verify(schedulerMock, Mockito.times(1)).addJob(Mockito.any(), Mockito.anyBoolean(),
						Mockito.anyBoolean());
				Mockito.verify(schedulerMock, Mockito.times(1)).scheduleJob(Mockito.any(Trigger.class));
			} catch (SchedulerException e) {
				throw new CommonException("Testing error", e);
			}
		});
	}

	@Test
	public void shouldCheckInvolvedUnitBelongsToInvoker() {
		wrapAssertExceptionMessage("obtainedUnit doesn't belong to invoker user", message -> {
			userMockitoHelper.fakeLoggedIn(LOGGED_USER);
			userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
			planetMockitoHelper.fakePlanetExists(1L, new Planet());
			planetMockitoHelper.fakePlanetExists(2L, new Planet());
			ObtainedUnit obtainedUnit = prepareObtainedUnit();
			obtainedUnit.getUser().setId(2);
			unitMockitoHelper.fakeObtainedUnitExists(1L, obtainedUnit);
			unitMissionBo.myRegisterExploreMission(prepareValidMissionInformation());

		});
	}

	@Test
	public void shouldCheckInvolvedUnitBelongsToSourcePlanet() {
		wrapAssertExceptionMessage("obtainedUnit doesn't belong to sourcePlanet", message -> {
			userMockitoHelper.fakeLoggedIn(LOGGED_USER);
			userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
			planetMockitoHelper.fakePlanetExists(1L, new Planet());
			planetMockitoHelper.fakePlanetExists(2L, new Planet());
			ObtainedUnit obtainedUnit = prepareObtainedUnit();
			obtainedUnit.getSourcePlanet().setId(4L);
			unitMockitoHelper.fakeObtainedUnitExists(1L, obtainedUnit);
			unitMissionBo.myRegisterExploreMission(prepareValidMissionInformation());
		});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void shouldRegisterMissionWhenAllIsOk() {
		wrapAssertExceptionMessage("obtainedUnit doesn't belong to sourcePlanet", message -> {
			userMockitoHelper.fakeLoggedIn(LOGGED_USER);
			userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
			planetMockitoHelper.fakePlanetExists(1L, new Planet());
			planetMockitoHelper.fakePlanetExists(2L, new Planet());
			unitMockitoHelper.fakeObtainedUnitExists(1L, prepareObtainedUnit());
			Mockito.when(missionTyperepositoryMock.findOneByCode("EXPLORE")).thenReturn(new MissionType());
			Mockito.when(missionRepositoryMock.saveAndFlush(Mockito.any(Mission.class))).thenReturn(new Mission());
			unitMissionBo.myRegisterExploreMission(prepareValidMissionInformation());
			Mockito.verify(missionRepositoryMock, Mockito.times(1)).saveAndFlush(Mockito.any(Mission.class));
			Mockito.verify(unitMockitoHelper.getObtainedUnitBoMock(), Mockito.times(1)).save(Mockito.anyList());

		});
	}

	private UnitMissionInformation prepareValidMissionInformation() {
		UnitMissionInformation retVal = new UnitMissionInformation();
		retVal.setSourcePlanetId(1L);
		retVal.setTargetPlanetId(2L);
		retVal.setInvolvedUnits(prepareInvolvedUnits());
		retVal.setUserId(LOGGED_USER.getId());
		return retVal;
	}

	private List<ObtainedUnitDto> prepareInvolvedUnits() {
		List<ObtainedUnitDto> retVal = new ArrayList<>();
		ObtainedUnitDto current = new ObtainedUnitDto();
		current.setId(1L);
		current.setCount(40L);
		retVal.add(current);
		return retVal;
	}

	private void wrapAssertExceptionMessage(String message, Consumer<String> consumer) {
		try {
			consumer.accept(message);
		} catch (Exception e) {
			if (!e.getMessage().equals(message)) {
				LOG.error("Expected exception message: \"" + message + "\", but was: \"" + e.getMessage() + "\"");
				throw e;
			}
		}
	}

	private ObtainedUnit prepareObtainedUnit() {
		ObtainedUnit retVal = new ObtainedUnit();
		retVal.setId(1L);
		retVal.setCount(5L);
		UserStorage user = new UserStorage();
		user.setId(1);
		Planet planet = new Planet();
		planet.setId(1L);
		retVal.setSourcePlanet(planet);
		retVal.setUser(user);
		UnitType unitType = new UnitType();
		unitType.setId(1);
		unitType.setName("Test type");
		Unit unit = new Unit();
		unit.setId(1);
		unit.setType(unitType);
		retVal.setUnit(unit);
		return retVal;
	}
}
