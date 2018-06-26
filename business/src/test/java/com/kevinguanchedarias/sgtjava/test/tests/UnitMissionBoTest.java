package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.internal.util.reflection.Whitebox;
import org.mockito.runners.MockitoJUnitRunner;
import org.quartz.SchedulerException;
import org.springframework.beans.BeanUtils;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.kevinguanchedarias.sgtjava.business.ConfigurationBo;
import com.kevinguanchedarias.sgtjava.business.MissionReportBo;
import com.kevinguanchedarias.sgtjava.business.ObtainedUnitBo;
import com.kevinguanchedarias.sgtjava.business.SocketIoService;
import com.kevinguanchedarias.sgtjava.business.UnitMissionBo;
import com.kevinguanchedarias.sgtjava.dto.MissionDto;
import com.kevinguanchedarias.sgtjava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.MissionReport;
import com.kevinguanchedarias.sgtjava.entity.MissionType;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.Unit;
import com.kevinguanchedarias.sgtjava.entity.UnitType;
import com.kevinguanchedarias.sgtjava.entity.UserImprovement;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.sgtjava.exception.UserNotFoundException;
import com.kevinguanchedarias.sgtjava.pojo.SelectedUnit;
import com.kevinguanchedarias.sgtjava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.sgtjava.repository.MissionRepository;
import com.kevinguanchedarias.sgtjava.repository.MissionTypeRepository;
import com.kevinguanchedarias.sgtjava.test.helper.MissionReportMockitoHelper;
import com.kevinguanchedarias.sgtjava.test.helper.MissionTypeMockitoHelper;
import com.kevinguanchedarias.sgtjava.test.helper.PlanetMockitoHelper;
import com.kevinguanchedarias.sgtjava.test.helper.UnitMockitoHelper;
import com.kevinguanchedarias.sgtjava.test.helper.UserMockitoHelper;
import com.kevinguanchedarias.sgtjava.util.DtoUtilService;

@RunWith(MockitoJUnitRunner.class)
public class UnitMissionBoTest extends TestCommon {

	private static final Logger LOG = Logger.getLogger(UnitMissionBoTest.class);

	private static final int CHARGE_DEFAULT_UNIT_AMOUNT = 20;
	private static final UserStorage LOGGED_USER;
	static {
		LOGGED_USER = new UserStorage();
		LOGGED_USER.setId(1);
		LOGGED_USER.setUsername("test_user");
	}

	private ObtainedUnitBo obtainedUnitBoMock;

	@Mock
	private MissionRepository missionRepositoryMock;

	@Mock
	private ConfigurationBo configurationBo;

	@Mock
	private MissionTypeRepository missionTyperepositoryMock;

	@Mock
	private SchedulerFactoryBean schedulerFactoryBeanMock;

	@Mock
	private SocketIoService socketIoService;

	@Mock
	private DtoUtilService dtoUtilServiceMock;

	@Mock
	private MissionReportBo missionReportBoMock;

	@Mock
	private MissionTypeMockitoHelper missionTypeMockitoHelper;

	@InjectMocks
	private UnitMissionBo unitMissionBo;

	private UserMockitoHelper userMockitoHelper;
	private PlanetMockitoHelper planetMockitoHelper;
	private UnitMockitoHelper unitMockitoHelper;
	private MissionReportMockitoHelper missionReportMockitoHelper;

	private MissionDto fakeMissionDto;

	/**
	 * Represents the information required for a return mission
	 *
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	class ReturnMissionInformation {
		private Planet sourcePlanet;
		private Planet targetPlanet;
		private Mission mission;
		private List<ObtainedUnit> obtainedUnits;

		public ReturnMissionInformation(Planet sourcePlanet, Planet targetPlanet, Mission mission,
				List<ObtainedUnit> obtainedUnits) {
			this.sourcePlanet = sourcePlanet;
			this.targetPlanet = targetPlanet;
			this.mission = mission;
			this.obtainedUnits = obtainedUnits;
		}

		public Planet getSourcePlanet() {
			return sourcePlanet;
		}

		public void setSourcePlanet(Planet sourcePlanet) {
			this.sourcePlanet = sourcePlanet;
		}

		public Planet getTargetPlanet() {
			return targetPlanet;
		}

		public void setTargetPlanet(Planet targetPlanet) {
			this.targetPlanet = targetPlanet;
		}

		public Mission getMission() {
			return mission;
		}

		public void setMission(Mission mission) {
			this.mission = mission;
		}

		public List<ObtainedUnit> getObtainedUnits() {
			return obtainedUnits;
		}

		public void setObtainedUnits(List<ObtainedUnit> obtainedUnits) {
			this.obtainedUnits = obtainedUnits;
		}

	}

	@Before
	public void init() {
		userMockitoHelper = new UserMockitoHelper(unitMissionBo);
		planetMockitoHelper = new PlanetMockitoHelper(unitMissionBo);
		unitMockitoHelper = new UnitMockitoHelper(unitMissionBo);
		missionReportMockitoHelper = new MissionReportMockitoHelper(missionReportBoMock);
		missionTypeMockitoHelper = new MissionTypeMockitoHelper(missionTyperepositoryMock);
		obtainedUnitBoMock = unitMockitoHelper.getObtainedUnitBoMock();
		mockScheduler(schedulerFactoryBeanMock);
		fakeMissionDto = new MissionDto();
		Mockito.when(dtoUtilServiceMock.dtoFromEntity(Mockito.any(), Mockito.any(Mission.class)))
				.thenReturn(fakeMissionDto);
	}

	@Test(expected = SgtBackendInvalidInputException.class)
	public void registerExploreShouldCheckInvoker() {
		userMockitoHelper.fakeLoggedIn(LOGGED_USER);
		UnitMissionInformation information = prepareValidUnitMissionInformation();
		information.setUserId(2);
		unitMissionBo.myRegisterExploreMission(information);
	}

	@Test
	public void myRegisterExplorerShouldDefineUserIdAsLoggedInWhenNull() {
		userMockitoHelper.fakeLoggedIn(LOGGED_USER);
		UnitMissionInformation information = prepareValidUnitMissionInformation();
		information.setUserId(null);
		Mission mission = new Mission();
		mission.setUser(LOGGED_USER);
		mission.setType(prepareValidMissionType());
		UnitRunningMissionDto retVal = new UnitRunningMissionDto(mission, new ArrayList<>());
		UnitMissionBo unitMissionBoSpy = Mockito.spy(unitMissionBo);
		doReturn(retVal).when(unitMissionBoSpy).adminRegisterExploreMission(information);
		assertEquals(retVal, unitMissionBoSpy.myRegisterExploreMission(information));
		assertEquals(LOGGED_USER.getId(), information.getUserId());
		Mockito.verify(unitMissionBoSpy, Mockito.times(1)).adminRegisterExploreMission(information);

	}

	@Test(expected = UserNotFoundException.class)
	public void registerExploreShouldCheckUserExists() {
		userMockitoHelper.fakeLoggedIn(LOGGED_USER);
		unitMissionBo.myRegisterExploreMission(prepareValidUnitMissionInformation());
	}

	@Test(expected = PlanetNotFoundException.class)
	public void registerExploreShouldCheckSourcePlanetExists() {
		userMockitoHelper.fakeLoggedIn(LOGGED_USER);
		userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
		unitMissionBo.myRegisterExploreMission(prepareValidUnitMissionInformation());
	}

	@Test(expected = PlanetNotFoundException.class)
	public void registerExploreShouldCheckTargetPlanetExists() {
		userMockitoHelper.fakeLoggedIn(LOGGED_USER);
		userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
		planetMockitoHelper.fakePlanetExists(1L, new Planet());
		unitMissionBo.myRegisterExploreMission(prepareValidUnitMissionInformation());
	}

	@Test
	public void registerExploreShouldCheckInvolvedUnitsNotEmpty() {
		wrapAssertExceptionMessage("involvedUnits can't be empty", message -> {
			userMockitoHelper.fakeLoggedIn(LOGGED_USER);
			userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
			planetMockitoHelper.fakePlanetExists(1L, new Planet());
			planetMockitoHelper.fakePlanetExists(2L, new Planet());
			UnitMissionInformation information = prepareValidUnitMissionInformation();
			information.setInvolvedUnits(new ArrayList<>());
			unitMissionBo.myRegisterExploreMission(information);
		});
	}

	@Test
	public void registerExploreShouldCheckInvolvedUnitExists() {
		wrapAssertExceptionMessage("No obtainedUnit for unit with id 1 was found in planet 1, nice try, dirty hacker!",
				message -> {
					userMockitoHelper.fakeLoggedIn(LOGGED_USER);
					userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
					planetMockitoHelper.fakePlanetExists(1L, new Planet());
					planetMockitoHelper.fakePlanetExists(2L, new Planet());
					unitMissionBo.myRegisterExploreMission(prepareValidUnitMissionInformation());
				});
	}

	@SuppressWarnings("unchecked")
	@Test
	public void registerExploreShouldRegisterMissionWhenAllIsOk() {
		ObtainedUnit involvedUnit = prepareObtainedUnit();
		Planet sourcePlanet = new Planet();
		Planet targetPlanet = new Planet();
		Mission savedMission = new Mission();
		savedMission.setId(4L);
		savedMission.setSourcePlanet(sourcePlanet);
		savedMission.setTargetPlanet(targetPlanet);
		savedMission.setTerminationDate(new Date());
		savedMission.setUser(LOGGED_USER);
		savedMission.setType(prepareValidMissionType());
		userMockitoHelper.fakeLoggedIn(LOGGED_USER);
		userMockitoHelper.fakeUserExists(LOGGED_USER.getId(), LOGGED_USER);
		planetMockitoHelper.fakePlanetExists(1L, sourcePlanet);
		planetMockitoHelper.fakePlanetExists(2L, targetPlanet);
		unitMockitoHelper.fakeFindUnit(1, involvedUnit.getUnit());
		unitMockitoHelper.fakeObtainedUnitExistsForGivenUnitAndPlanet(LOGGED_USER.getId(),
				involvedUnit.getUnit().getId(), 1L, involvedUnit);
		Mockito.when(missionTyperepositoryMock.findOneByCode("EXPLORE")).thenReturn(new MissionType());
		Mockito.when(missionRepositoryMock.saveAndFlush(Mockito.any(Mission.class))).thenReturn(savedMission);
		Mockito.when(missionRepositoryMock.findOne(1L)).thenReturn(savedMission);
		UnitMissionBo unitMissionBoSpy = Mockito.spy(unitMissionBo);
		doNothing().when(unitMissionBoSpy).adminRegisterReturnMission(savedMission);
		ArgumentCaptor<List<ObtainedUnit>> captor = unitMockitoHelper.captureObtainedUnitListSave();
		unitMissionBoSpy.myRegisterExploreMission(prepareValidUnitMissionInformation());
		Mockito.verify(missionRepositoryMock, Mockito.times(1)).saveAndFlush(Mockito.any(Mission.class));
		Mockito.verify(unitMockitoHelper.getObtainedUnitBoMock(), Mockito.times(1)).save(Mockito.anyList());
		ObtainedUnit savedUnit = captor.getValue().get(0);
		assertEquals(sourcePlanet, savedUnit.getTargetPlanet());
		assertEquals(targetPlanet, savedUnit.getSourcePlanet());
	}

	@Test
	public void processExploreShouldNotDefineAsExploredIfAlreadyDone() throws SchedulerException {
		Mission savedMission = new Mission();
		savedMission.setId(1L);
		savedMission.setRequiredTime(1D);
		UserStorage involvedUser = new UserStorage();
		Planet targetPlanet = new Planet();
		savedMission.setUser(involvedUser);
		savedMission.setTargetPlanet(targetPlanet);
		Mockito.when(missionRepositoryMock.findOne(1L)).thenReturn(savedMission);
		planetMockitoHelper.fakeExplored(involvedUser, targetPlanet);
		UnitMissionBo unitMissionBoSpy = Mockito.spy(unitMissionBo);
		doNothing().when(unitMissionBoSpy).adminRegisterReturnMission(savedMission);
		missionReportMockitoHelper.captureMissionReportSave();
		unitMissionBoSpy.processExplore(1L);
		Mockito.verify(planetMockitoHelper.getPlanetBoMock(), Mockito.times(1)).isExplored(involvedUser, targetPlanet);
		Mockito.verify(planetMockitoHelper.getPlanetBoMock(), Mockito.never()).defineAsExplored(involvedUser,
				targetPlanet);
		Mockito.verify(unitMissionBoSpy, Mockito.times(1)).adminRegisterReturnMission(savedMission);

	}

	@Test
	public void processExploreShouldDefineAsExplored() {
		Mission savedMission = new Mission();
		savedMission.setRequiredTime(1D);
		UserStorage involvedUser = new UserStorage();
		Planet targetPlanet = new Planet();
		savedMission.setUser(involvedUser);
		savedMission.setTargetPlanet(targetPlanet);
		Mockito.when(missionRepositoryMock.findOne(1L)).thenReturn(savedMission);
		UnitMissionBo unitMissionBoSpy = Mockito.spy(unitMissionBo);
		doNothing().when(unitMissionBoSpy).adminRegisterReturnMission(savedMission);
		missionReportMockitoHelper.captureMissionReportSave();
		unitMissionBoSpy.processExplore(1L);
		Mockito.verify(planetMockitoHelper.getPlanetBoMock(), Mockito.times(1)).isExplored(involvedUser, targetPlanet);
		Mockito.verify(planetMockitoHelper.getPlanetBoMock(), Mockito.times(1)).defineAsExplored(involvedUser,
				targetPlanet);
		Mockito.verify(unitMissionBoSpy, Mockito.times(1)).adminRegisterReturnMission(savedMission);
		Mockito.verify(missionRepositoryMock, Mockito.times(1)).saveAndFlush(savedMission);
		assertEquals(true, savedMission.getResolved());
		testMissionChangeEmmited(involvedUser, savedMission);
	}

	@Test
	public void processGatherShouldIncreaseUserResourcesAmount() {
		Mission savedMission = new Mission();
		savedMission.setId(1L);
		savedMission.setRequiredTime(1D);
		UserStorage involvedUser = new UserStorage();
		BeanUtils.copyProperties(LOGGED_USER, involvedUser);
		UserImprovement userImprovement = new UserImprovement();
		userImprovement.setMoreChargeCapacity(10F);
		involvedUser.setImprovements(userImprovement);
		involvedUser.setPrimaryResource(200D);
		involvedUser.setSecondaryResource(400D);
		Planet targetPlanet = new Planet();
		targetPlanet.setRichness(40);
		savedMission.setUser(involvedUser);
		savedMission.setTargetPlanet(targetPlanet);
		Mockito.when(missionRepositoryMock.findOne(1L)).thenReturn(savedMission);
		UnitMissionBo unitMissionBoSpy = Mockito.spy(unitMissionBo);
		doNothing().when(unitMissionBoSpy).adminRegisterReturnMission(savedMission);
		ArgumentCaptor<MissionReport> captor = missionReportMockitoHelper.captureMissionReportSave();
		unitMockitoHelper.fakeFindByMissionId(1L, prepareObtainedUnit());
		unitMissionBoSpy.processGather(1L);
		Map<String, Object> savedJsonMap = parseJson(captor.getValue().getJsonBody());
		Double expectedGatheredPrimary = 30.8D;
		Double expectedGatheredSecondary = 13.2D;
		Double expectedUserPrimary = 200D + expectedGatheredPrimary;
		Double expectedUserSecondary = 413.2D;
		assertEquals(expectedUserPrimary, involvedUser.getPrimaryResource(), 0.1D);
		assertEquals(expectedUserSecondary, involvedUser.getSecondaryResource(), 0.1D);
		assertEquals(expectedGatheredPrimary, (Double) savedJsonMap.get("gatheredPrimary"), 0.1D);
		assertEquals(expectedGatheredSecondary, (Double) savedJsonMap.get("gatheredSecondary"), 0.1D);

	}

	@Test
	public void proccessAttackShouldWorkWithoutExcedingAttack() {
		Mission mission = new Mission();
		mission.setId(1L);
		mission.setTargetPlanet(new Planet());
		Mockito.when(missionRepositoryMock.findOne(1L)).thenReturn(mission);
		List<ObtainedUnit> involvedUnits = new ArrayList<>();

		UserStorage user1 = new UserStorage();
		user1.setId(1);
		Unit unit1 = new Unit();
		unit1.setId(1);
		unit1.setShield(10);
		unit1.setHealth(100);
		unit1.setAttack(90);
		unit1.setPoints(10);
		unit1.setType(prepareValidUnitType(1));
		ObtainedUnit obtainedUnit1 = new ObtainedUnit();
		obtainedUnit1.setCount(4L);
		obtainedUnit1.setUnit(unit1);
		obtainedUnit1.setUser(user1);
		obtainedUnit1.setMission(mission);
		mission.setUser(user1);

		UserStorage user2 = new UserStorage();
		user2.setId(2);
		Unit unit2 = new Unit();
		unit2.setId(2);
		unit2.setShield(40);
		unit2.setHealth(60);
		unit2.setAttack(61);
		unit2.setPoints(15);
		unit2.setType(prepareValidUnitType(2));
		ObtainedUnit obtainedUnit2 = new ObtainedUnit();
		obtainedUnit2.setCount(4L);
		obtainedUnit2.setUnit(unit2);
		obtainedUnit2.setUser(user2);
		Mission mission2 = new Mission();
		mission2.setId(3L);
		obtainedUnit2.setMission(mission2);

		involvedUnits.add(obtainedUnit1);
		involvedUnits.add(obtainedUnit2);

		Mockito.when(obtainedUnitBoMock.findInvolvedInAttack(mission.getTargetPlanet(), mission))
				.thenReturn(involvedUnits);
		missionReportMockitoHelper.captureMissionReportSave();
		UnitMissionBo unitMissionBoSpy = Mockito.spy(unitMissionBo);
		doNothing().when(unitMissionBoSpy).adminRegisterReturnMission(mission);
		unitMissionBoSpy.processAttack(1L);
		Mockito.verify(obtainedUnitBoMock, Mockito.never()).existsByMission(Mockito.any());
		Mockito.verify(obtainedUnitBoMock, Mockito.never()).delete(Mockito.any());
		Mockito.verify(unitMissionBoSpy, Mockito.never()).delete(Mockito.any(Mission.class));
		Mockito.verify(unitMissionBoSpy, Mockito.times(1)).adminRegisterReturnMission(mission);
	}

	@Test
	public void proccessAttackShouldWorkWithExcedingAttack() {
		Mission mission = new Mission();
		mission.setId(1L);
		mission.setTargetPlanet(new Planet());
		Mockito.when(missionRepositoryMock.findOne(1L)).thenReturn(mission);
		List<ObtainedUnit> involvedUnits = new ArrayList<>();

		UserStorage user1 = new UserStorage();
		user1.setId(1);
		Unit unit1 = new Unit();
		unit1.setId(1);
		unit1.setShield(10);
		unit1.setHealth(100);
		unit1.setAttack(90000);
		unit1.setPoints(10);
		unit1.setType(prepareValidUnitType(1));
		ObtainedUnit obtainedUnit1 = new ObtainedUnit();
		obtainedUnit1.setCount(4L);
		obtainedUnit1.setUnit(unit1);
		obtainedUnit1.setUser(user1);
		obtainedUnit1.setMission(mission);
		mission.setUser(user1);

		UserStorage user2 = new UserStorage();
		user2.setId(2);
		Unit unit2 = new Unit();
		unit2.setId(2);
		unit2.setShield(40);
		unit2.setHealth(60);
		unit2.setAttack(61000);
		unit2.setPoints(15);
		unit2.setType(prepareValidUnitType(2));
		ObtainedUnit obtainedUnit2 = new ObtainedUnit();
		obtainedUnit2.setCount(4L);
		obtainedUnit2.setUnit(unit2);
		obtainedUnit2.setUser(user2);
		Mission mission2 = new Mission();
		mission2.setId(3L);
		obtainedUnit2.setMission(mission2);

		involvedUnits.add(obtainedUnit1);
		involvedUnits.add(obtainedUnit2);

		Mockito.when(obtainedUnitBoMock.findInvolvedInAttack(mission.getTargetPlanet(), mission))
				.thenReturn(involvedUnits);
		missionReportMockitoHelper.captureMissionReportSave();

		UnitMissionBo unitMissionBoSpy = Mockito.spy(unitMissionBo);
		doNothing().when(unitMissionBoSpy).adminRegisterReturnMission(mission);
		unitMissionBoSpy.processAttack(1L);
		Mockito.verify(obtainedUnitBoMock, Mockito.times(2)).existsByMission(Mockito.any());
		Mockito.verify(obtainedUnitBoMock, Mockito.times(2)).delete(Mockito.any());
		Mockito.verify(unitMissionBoSpy, Mockito.times(1)).delete(Mockito.any(Mission.class));
		Mockito.verify(unitMissionBoSpy, Mockito.never()).adminRegisterReturnMission(mission);
		Mockito.verify(missionReportBoMock, Mockito.times(2)).save(Mockito.any(MissionReport.class));
	}

	@Test
	public void registerReturnMissionShouldCreateValidReturnMission() throws SchedulerException {
		Mission originalMission = new Mission();
		originalMission.setId(4L);
		originalMission.setRequiredTime(1D);
		Planet originalSourcePlanet = new Planet();
		Planet originalTargetPlanet = new Planet();
		originalMission.setSourcePlanet(originalSourcePlanet);
		originalMission.setTargetPlanet(originalTargetPlanet);
		UserStorage user = new UserStorage();
		originalMission.setUser(user);

		String expectedTypeName = com.kevinguanchedarias.sgtjava.enumerations.MissionType.RETURN_MISSION.name();
		MissionType returnMissionType = prepareValidMissionType();
		List<ObtainedUnit> obtainedUnits = new ArrayList<>();
		obtainedUnits.add(prepareObtainedUnit());
		ArgumentCaptor<Mission> captor = ArgumentCaptor.forClass(Mission.class);
		Mockito.when(missionRepositoryMock.saveAndFlush(captor.capture())).thenAnswer(invocation -> {
			Mission passedMission = invocation.getArgumentAt(0, Mission.class);
			passedMission.setId(8L);
			return passedMission;
		});
		Mockito.when(obtainedUnitBoMock.findByMissionId(originalMission.getId())).thenReturn(obtainedUnits);
		Mockito.when(missionTyperepositoryMock.findOneByCode(expectedTypeName)).thenReturn(returnMissionType);

		Whitebox.setInternalState(unitMissionBo, "obtainedUnitBo", obtainedUnitBoMock);
		unitMissionBo.adminRegisterReturnMission(originalMission);

		Mission savedMission = captor.getValue();
		Mockito.verify(missionRepositoryMock, Mockito.times(1)).saveAndFlush(savedMission);
		Mockito.verify(obtainedUnitBoMock, Mockito.times(1)).findByMissionId(originalMission.getId());
		Mockito.verify(missionTyperepositoryMock, Mockito.times(1)).findOneByCode(expectedTypeName);
		Mockito.verify(obtainedUnitBoMock, Mockito.times(1)).save(obtainedUnits);
		testHasScheduledJob(schedulerFactoryBeanMock);

		assertEquals(returnMissionType, savedMission.getType());
		assertEquals(originalMission.getRequiredTime(), savedMission.getRequiredTime());
		assertNotNull(savedMission.getTerminationDate());
		assertEquals(originalMission.getSourcePlanet(), savedMission.getTargetPlanet());
		assertEquals(originalMission.getTargetPlanet(), savedMission.getSourcePlanet());
		assertEquals(originalMission.getUser(), savedMission.getUser());
		assertEquals(originalMission, savedMission.getRelatedMission());
		obtainedUnits.forEach(current -> assertEquals(savedMission, current.getMission()));
	}

	@Test
	public void processReturnMissionShouldUpdateUnitsPosition() {
		ReturnMissionInformation returnMissionInformation = doCommonReturnMissionHandling();
		ObtainedUnit obtainedUnit = returnMissionInformation.getObtainedUnits().get(0);
		Mission returnMission = returnMissionInformation.getMission();
		unitMissionBo.proccessReturnMission(1L);
		Mockito.verify(missionRepositoryMock, Mockito.times(1)).findOne(1L);
		Mockito.verify(obtainedUnitBoMock, Mockito.times(1)).findByMissionId(1L);
		assertNull(obtainedUnit.getMission());
		assertEquals(returnMissionInformation.getSourcePlanet(), obtainedUnit.getSourcePlanet());
		assertNull(obtainedUnit.getTargetPlanet());
		Mockito.verify(missionRepositoryMock, Mockito.times(1)).saveAndFlush(returnMission);
		assertEquals(true, returnMission.getResolved());
		testMissionChangeEmmited(LOGGED_USER, returnMission);
	}

	@SuppressWarnings("unchecked")
	@Test
	public void proccessReturnMissionShouldAppendToCountIfExistingInPlanet() {
		ReturnMissionInformation returnMissionInformation = doCommonReturnMissionHandling();
		ObtainedUnit obtainedUnit = returnMissionInformation.getObtainedUnits().get(0);
		obtainedUnit.setCount(10L);
		Unit unit = obtainedUnit.getUnit();
		Planet targetPlanet = returnMissionInformation.getTargetPlanet();
		ObtainedUnit existingUnit = new ObtainedUnit();
		BeanUtils.copyProperties(obtainedUnit, existingUnit);
		Long existingId = 2L;
		existingUnit.setId(existingId);
		existingUnit.setCount(8L);
		unitMockitoHelper.fakeObtainedUnitExistsForGivenUnitAndPlanet(1, unit.getId(), targetPlanet.getId(),
				existingUnit);
		Mockito.when(obtainedUnitBoMock.findHavingSameUnit(Mockito.anyList(), Mockito.any(ObtainedUnit.class)))
				.thenReturn(existingUnit);
		unitMissionBo.proccessReturnMission(1L);
		Mockito.verify(obtainedUnitBoMock, Mockito.times(1)).delete(obtainedUnit);
		ObtainedUnit savedUnit = existingUnit;
		assertEquals(existingId, savedUnit.getId());
		assertEquals(18L, (long) savedUnit.getCount());
	}

	private UnitMissionInformation prepareValidUnitMissionInformation() {
		UnitMissionInformation retVal = new UnitMissionInformation();
		retVal.setSourcePlanetId(1L);
		retVal.setTargetPlanetId(2L);
		retVal.setInvolvedUnits(prepareInvolvedUnits());
		retVal.setUserId(LOGGED_USER.getId());
		return retVal;
	}

	private List<SelectedUnit> prepareInvolvedUnits() {
		List<SelectedUnit> retVal = new ArrayList<>();
		SelectedUnit current = new SelectedUnit();
		current.setId(1);
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
		user.setId(LOGGED_USER.getId());
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
		unit.setCharge(CHARGE_DEFAULT_UNIT_AMOUNT);
		retVal.setUnit(unit);
		return retVal;
	}

	private void testMissionChangeEmmited(UserStorage targetUser, Mission missionToNotify) {
		Mockito.verify(socketIoService, Mockito.times(1)).sendMessage(targetUser, "local_mission_change",
				fakeMissionDto);
	}

	private ReturnMissionInformation doCommonReturnMissionHandling() {
		Mission returnMission = new Mission();
		returnMission.setId(1L);
		returnMission.setUser(LOGGED_USER);
		Planet originalMissionSourcePlanet = prepareValidPlanet(prepareValidGalaxy());
		originalMissionSourcePlanet.setId(4L);
		returnMission.setTargetPlanet(originalMissionSourcePlanet);
		Mockito.when(missionRepositoryMock.findOne(1L)).thenReturn(returnMission);
		ObtainedUnit obtainedUnit = prepareObtainedUnit();
		obtainedUnit.setMission(returnMission);
		Planet somePlanet = prepareValidPlanet(prepareValidGalaxy());
		obtainedUnit.setSourcePlanet(somePlanet);
		obtainedUnit.setTargetPlanet(somePlanet);
		List<ObtainedUnit> involvedUnits = new ArrayList<>();
		involvedUnits.add(obtainedUnit);
		Mockito.when(obtainedUnitBoMock.findByMissionId(1L)).thenReturn(involvedUnits);
		return new ReturnMissionInformation(originalMissionSourcePlanet, somePlanet, returnMission, involvedUnits);
	}
}
