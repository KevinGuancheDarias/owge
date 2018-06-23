package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.joda.time.Interval;
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
import org.quartz.TriggerKey;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.kevinguanchedarias.sgtjava.business.MissionBo;
import com.kevinguanchedarias.sgtjava.business.ObjectRelationBo;
import com.kevinguanchedarias.sgtjava.business.ObtainedUnitBo;
import com.kevinguanchedarias.sgtjava.business.ObtainedUpradeBo;
import com.kevinguanchedarias.sgtjava.business.PlanetBo;
import com.kevinguanchedarias.sgtjava.business.RequirementBo;
import com.kevinguanchedarias.sgtjava.business.UnitBo;
import com.kevinguanchedarias.sgtjava.business.UnlockedRelationBo;
import com.kevinguanchedarias.sgtjava.business.UpgradeBo;
import com.kevinguanchedarias.sgtjava.business.UserImprovementBo;
import com.kevinguanchedarias.sgtjava.business.UserStorageBo;
import com.kevinguanchedarias.sgtjava.entity.Improvement;
import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.MissionInformation;
import com.kevinguanchedarias.sgtjava.entity.MissionType;
import com.kevinguanchedarias.sgtjava.entity.ObjectRelation;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.Unit;
import com.kevinguanchedarias.sgtjava.entity.UnlockedRelation;
import com.kevinguanchedarias.sgtjava.entity.Upgrade;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendTargetNotUnlocked;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendUnitBuildAlreadyRunningException;
import com.kevinguanchedarias.sgtjava.exception.SgtLevelUpMissionAlreadyRunningException;
import com.kevinguanchedarias.sgtjava.exception.SgtMissionRegistrationException;
import com.kevinguanchedarias.sgtjava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.sgtjava.repository.MissionRepository;
import com.kevinguanchedarias.sgtjava.repository.MissionTypeRepository;

@RunWith(MockitoJUnitRunner.class)
public class MissionBoTest extends TestCommon {
	private static final Double REQUIRED_PRIMARY = 100D;
	private static final Double REQUIRED_SECONDARY = 120D;
	private static final Double REQUIRED_TIME = 40D;

	private static final Integer UPGRADE_ID = 1;
	private static final Integer USER_ID = 1;
	private static final Integer LEVEL_OF_OBTAINED_UPGRADE = 2;
	private static final Integer UNIT_ID = 1;
	private static final Long PLANET_ID = 1L;

	@Mock
	private MissionRepository missionRepositoryMock;

	@Mock
	private ObtainedUpradeBo obtainedUpgradeBoMock;

	@Mock
	private ObjectRelationBo objectRelationBoMock;

	@Mock
	private UpgradeBo upgradeBoMock;

	@Mock
	private UserStorageBo userStorageBoMock;

	@Mock
	private MissionTypeRepository missionTypeRepositoryMock;

	@Mock
	private ResourceRequirementsPojo resourceRequirementsMock;

	@Mock
	private UserImprovementBo userImprovementBoMock;

	@Mock
	private RequirementBo requirementBoMock;

	@Mock
	private UnlockedRelationBo unlockedRelationBoMock;

	@Mock
	private UnitBo unitBoMock;

	@Mock
	private ObtainedUnitBo obtainedUnitBo;

	@Mock
	private PlanetBo planetBo;

	@InjectMocks
	private MissionBo missionBo;

	private ObtainedUpgrade obtainedUpgrade;
	private UserStorage user;
	private ObjectRelation relationUpgrade;
	private ObjectRelation relationUnit;
	private MissionType missionType;
	private List<Mission> missionStorage;
	private Double currentUserPrimaryResource;
	private Double currentUserSecondaryResource;

	@Before
	public void init() {
		Whitebox.setInternalState(missionBo, "schedulerFactory", null);
		missionStorage = new ArrayList<>();
		user = new UserStorage();
		user.setId(USER_ID);
		user.setPrimaryResource(300D);
		user.setSecondaryResource(300D);

		Upgrade upgrade = new Upgrade();
		upgrade.setId(UPGRADE_ID);

		obtainedUpgrade = new ObtainedUpgrade();
		obtainedUpgrade.setUpgrade(upgrade);
		obtainedUpgrade.setUserId(user);
		obtainedUpgrade.setLevel(LEVEL_OF_OBTAINED_UPGRADE);
		obtainedUpgrade.setAvailable(true);

		Mockito.when(resourceRequirementsMock.getRequiredPrimary()).thenReturn(REQUIRED_PRIMARY);
		Mockito.when(resourceRequirementsMock.getRequiredSecondary()).thenReturn(REQUIRED_SECONDARY);
		Mockito.when(resourceRequirementsMock.getRequiredTime()).thenReturn(REQUIRED_TIME);
		Mockito.when(resourceRequirementsMock.canRun(user)).thenReturn(true);

		relationUpgrade = new ObjectRelation();
		relationUpgrade.setObject(prepareValidObjectEntity(RequirementTargetObject.UPGRADE));

		relationUnit = new ObjectRelation();
		relationUnit.setObject(prepareValidObjectEntity(RequirementTargetObject.UNIT));
		relationUnit.setId(1);

		missionType = prepareValidMissionType();

		Mockito.when(obtainedUpgradeBoMock.findByUserAndUpgrade(USER_ID, UPGRADE_ID)).thenReturn(obtainedUpgrade);
		Mockito.when(userStorageBoMock.findById(USER_ID)).thenReturn(user);
		Mockito.when(userStorageBoMock.findLoggedIn()).thenReturn(user);
		Mockito.when(upgradeBoMock.calculateRequirementsAreMet(obtainedUpgrade)).thenReturn(resourceRequirementsMock);
		Mockito.when(
				objectRelationBoMock.findOneByObjectTypeAndReferenceId(RequirementTargetObject.UPGRADE, UPGRADE_ID))
				.thenReturn(relationUpgrade);
		Mockito.when(objectRelationBoMock.findOneByObjectTypeAndReferenceId(RequirementTargetObject.UNIT, 1))
				.thenReturn(relationUnit);
		Mockito.when(missionTypeRepositoryMock
				.findOneByCode(com.kevinguanchedarias.sgtjava.enumerations.MissionType.LEVEL_UP.name()))
				.thenReturn(missionType);
		Mockito.when(unitBoMock.findByIdOrDie(1)).thenReturn(prepareValidUnit(UNIT_ID, 1));
		mockRepositorySaveAction(Mission.class, missionRepositoryMock, missionStorage);

	}

	@Test(expected = SgtLevelUpMissionAlreadyRunningException.class)
	public void registerLevelUpgradeShouldThrowWhenThereIsAnUpgradeMissionAlreadyRunningForThisUser() {
		Mockito.when(missionRepositoryMock.findOneByUserIdAndTypeCode(USER_ID,
				com.kevinguanchedarias.sgtjava.enumerations.MissionType.LEVEL_UP.name())).thenReturn(new Mission());
		missionBo.registerLevelUpAnUpgrade(USER_ID, UPGRADE_ID);
	}

	@Test(expected = SgtMissionRegistrationException.class)
	public void registerLevelUpgradeShouldThrowWhenObtainedUpgradeIsNotAvailable() {
		obtainedUpgrade.setAvailable(false);
		missionBo.registerLevelUpAnUpgrade(USER_ID, UPGRADE_ID);
	}

	@Test(expected = SgtMissionRegistrationException.class)
	public void registerLevelUpgradeShouldThrowWhenDoesNotHaveEnoughResources() {
		Mockito.when(resourceRequirementsMock.canRun(user)).thenReturn(false);
		missionBo.registerLevelUpAnUpgrade(USER_ID, UPGRADE_ID);
	}

	@Test
	public void registerLevelUpgradeMissionInformationShouldHaveLevelOfObtainedUpgradePlusOne() {
		missionBo.registerLevelUpAnUpgrade(USER_ID, UPGRADE_ID);
		MissionInformation savedMissionInformation = missionStorage.get(0).getMissionInformation();
		assertEquals(relationUpgrade, savedMissionInformation.getRelation());
		assertEquals(LEVEL_OF_OBTAINED_UPGRADE + 1, savedMissionInformation.getValue().intValue());
	}

	@Test
	public void registerLevelUpgradeShouldProperlySetTerminationDate() {
		missionBo.registerLevelUpAnUpgrade(USER_ID, UPGRADE_ID);
		Date terminationDate = missionStorage.get(0).getTerminationDate();
		Interval interval = new Interval((new Date()).getTime(), terminationDate.getTime());
		long secondsDifference = interval.toDuration().getStandardSeconds();
		assertTrue(secondsDifference == REQUIRED_TIME
				|| (secondsDifference <= REQUIRED_TIME && (REQUIRED_TIME - secondsDifference) < 2));
	}

	@Test
	public void registerLevelUpgradeShouldUpdateUserResources() {
		missionBo.registerLevelUpAnUpgrade(USER_ID, UPGRADE_ID);
		assertNotNull(missionStorage.get(0));
		assertEquals(200, user.getPrimaryResource().intValue());
		assertEquals(180, user.getSecondaryResource().intValue());
	}

	@Test
	public void shouldNotProcessMissionWhenItHasBeenRemoved() {
		missionBo.processLevelUpAnUpgrade(3L);
		Mockito.verify(objectRelationBoMock, Mockito.never()).unboxObjectRelation(Mockito.any(ObjectRelation.class));
		Mockito.verify(obtainedUpgradeBoMock, Mockito.never()).save(Mockito.any(ObtainedUpgrade.class));
	}

	@Test
	public void shouldProcessMissionWhenItExists() {
		Mission fakeSavedMission = prepareValidMissionWithInformation(relationUpgrade);
		Upgrade fakeRelatedUpgrade = new Upgrade();
		fakeRelatedUpgrade.setId(1);
		Improvement improvement = new Improvement();
		fakeRelatedUpgrade.setImprovement(improvement);

		Mockito.when(missionRepositoryMock.findOne(1L)).thenReturn(fakeSavedMission);
		Mockito.when(objectRelationBoMock.unboxObjectRelation(relationUpgrade)).thenReturn(fakeRelatedUpgrade);
		missionBo.processLevelUpAnUpgrade(1L);
		Mockito.verify(objectRelationBoMock).unboxObjectRelation(Mockito.any(ObjectRelation.class));
		Mockito.verify(obtainedUpgradeBoMock).save(Mockito.any(ObtainedUpgrade.class));
		Mockito.verify(missionRepositoryMock).delete(Mockito.any(Mission.class));
		Mockito.verify(userImprovementBoMock).addImprovements(improvement, user, 1L);
		Mockito.verify(requirementBoMock).triggerLevelUpCompleted(user);
	}

	@Test
	public void shouldCancelLevelUpMission() throws SchedulerException {
		Mission fakeSavedMission = prepareMissionForCommonCancelMission(
				com.kevinguanchedarias.sgtjava.enumerations.MissionType.LEVEL_UP);

		SchedulerFactoryBean schedulerFactoryMock = mockScheduler();
		Mockito.when(missionRepositoryMock.findOne(1L)).thenReturn(fakeSavedMission);
		Whitebox.setInternalState(missionBo, "schedulerFactory", schedulerFactoryMock);

		missionBo.cancelMission(1L);

		assertEquals((currentUserPrimaryResource + fakeSavedMission.getPrimaryResource()),
				user.getPrimaryResource().doubleValue(), 0D);
		assertEquals(currentUserSecondaryResource + fakeSavedMission.getSecondaryResource(),
				user.getSecondaryResource().doubleValue(), 0D);
		Mockito.verify(userStorageBoMock).save(user);

		Mockito.verify(schedulerFactoryMock.getScheduler()).unscheduleJob(Mockito.any(TriggerKey.class));
	}

	@Test(expected = SgtBackendUnitBuildAlreadyRunningException.class)
	public void registerUnitBuildShouldThrowWhenOneMissionAlreadyRunningOnThatPlanet() {
		Mockito.when(missionRepositoryMock.findByUserIdAndTypeCodeAndMissionInformationValue(USER_ID, "BUILD_UNIT", 1D))
				.thenReturn(prepareValidMissionWithInformation(relationUnit));
		Mockito.when(objectRelationBoMock.unboxObjectRelation(relationUnit)).thenReturn(prepareValidUnit(1, 1));
		missionBo.registerBuildUnit(USER_ID, 1L, 1, 1L);
	}

	@Test(expected = SgtBackendTargetNotUnlocked.class)
	public void registerUnitBuildShouldThrowWhenRelationIsNotUnlocked() {
		missionBo.registerBuildUnit(USER_ID, 1L, 1, 1L);
	}

	@Test(expected = SgtMissionRegistrationException.class)
	public void registerUnitBuildShouldCheckRequirements() {
		Mockito.when(unlockedRelationBoMock.findOneByUserIdAndRelationId(USER_ID, relationUnit.getId()))
				.thenReturn(new UnlockedRelation());
		ResourceRequirementsPojo resourceRequirementsPojoMock = Mockito.mock(ResourceRequirementsPojo.class);
		Mockito.when(unitBoMock.calculateRequirements(Mockito.any(Unit.class), Mockito.anyLong()))
				.thenReturn(resourceRequirementsPojoMock);
		missionBo.registerBuildUnit(USER_ID, 1L, 1, 1L);
	}

	@Test
	public void registerUnitShouldSubstractResourcesAndRegister() {
		double userPrimary = REQUIRED_PRIMARY + 10;
		double userSecondary = REQUIRED_SECONDARY + 10;
		long count = 2L;
		ArgumentCaptor<ObtainedUnit> captor = ArgumentCaptor.forClass(ObtainedUnit.class);
		user.setPrimaryResource(userPrimary);
		user.setSecondaryResource(userSecondary);

		Mockito.when(unlockedRelationBoMock.findOneByUserIdAndRelationId(USER_ID, relationUnit.getId()))
				.thenReturn(new UnlockedRelation());
		Mockito.when(unitBoMock.calculateRequirements(Mockito.any(Unit.class), Mockito.anyLong()))
				.thenReturn(resourceRequirementsMock);
		Mockito.when(obtainedUnitBo.save(captor.capture())).thenReturn(null);
		Mockito.when(missionTypeRepositoryMock.findOneByCode("BUILD_UNIT")).thenReturn(missionType);
		missionBo.registerBuildUnit(USER_ID, 1L, UNIT_ID, count);

		assertTrue(user.getPrimaryResource() < userPrimary);
		assertTrue(user.getSecondaryResource() < userSecondary);
		Mockito.verify(userStorageBoMock).save(user);
		Mockito.verify(missionRepositoryMock).save(Mockito.any(Mission.class));
		Mission savedMission = missionStorage.get(0);
		MissionInformation savedMissionInformation = savedMission.getMissionInformation();
		assertEquals(relationUnit, savedMissionInformation.getRelation());
		assertEquals(PLANET_ID.longValue(), savedMissionInformation.getValue().longValue());
		assertEquals(REQUIRED_PRIMARY, savedMission.getPrimaryResource());
		assertEquals(REQUIRED_SECONDARY, savedMission.getSecondaryResource());
		assertEquals(savedMission, savedMissionInformation.getMission());
		assertEquals(count, captor.getValue().getCount().longValue());
	}

	@Test
	public void processUnitBuildShouldDoNothingWhenMissionDoesNotLongerExists() {
		missionBo.processBuildUnit(1L);
		Mockito.verify(obtainedUnitBo, Mockito.never()).findByMissionId(1L);
	}

	@Test
	public void processUnitBuildShouldSetSourcePlanetToMissionInformationValueUnsetMissionAndSave() {
		int planetId = 4;
		Planet planet = new Planet();
		planet.setId(Long.valueOf(planetId));

		MissionInformation missionInformation = new MissionInformation();
		missionInformation.setValue(planetId);

		Mission mission = new Mission();
		mission.setId(1L);
		mission.setMissionInformation(missionInformation);
		mission.setUser(user);

		Unit unit = new Unit();
		unit.setId(UNIT_ID);
		;

		ObtainedUnit oUnit1 = new ObtainedUnit();
		oUnit1.setId(1L);
		oUnit1.setUnit(unit);
		ObtainedUnit oUnit2 = new ObtainedUnit();
		oUnit2.setId(2L);
		oUnit2.setUnit(unit);

		List<ObtainedUnit> obtainedUnitList = new ArrayList<ObtainedUnit>();
		obtainedUnitList.add(oUnit1);
		obtainedUnitList.add(oUnit2);

		Mockito.when(missionRepositoryMock.findOne(1L)).thenReturn(mission);
		Mockito.when(obtainedUnitBo.findByMissionId(1L)).thenReturn(obtainedUnitList);
		Mockito.when(planetBo.findByIdOrDie(planet.getId())).thenReturn(planet);

		missionBo.processBuildUnit(1L);

		obtainedUnitList.forEach(current -> {
			assertEquals(planet, current.getSourcePlanet());
			assertNull(current.getMission());
			Mockito.verify(obtainedUnitBo).saveWithAdding(USER_ID, current);
		});
		Mockito.verify(requirementBoMock, Mockito.times(obtainedUnitList.size())).triggerUnitBuildCompleted(user, unit);
		Mockito.verify(missionRepositoryMock).delete(mission);
	}

	@Test
	public void shouldProperlyCancelBuildUnitMission() {
		Mission mission = prepareMissionForCommonCancelMission(
				com.kevinguanchedarias.sgtjava.enumerations.MissionType.BUILD_UNIT);

		missionBo.cancelMission(mission);
		Mockito.verify(obtainedUnitBo).deleteByMissionId(mission.getId());
		assertEquals((currentUserPrimaryResource + mission.getPrimaryResource()),
				user.getPrimaryResource().doubleValue(), 0D);
		assertEquals(currentUserSecondaryResource + mission.getSecondaryResource(),
				user.getSecondaryResource().doubleValue(), 0D);
		Mockito.verify(userStorageBoMock).save(user);
	}

	private Mission prepareValidMissionWithInformation(ObjectRelation relation) {
		Mission retVal = new Mission();
		retVal.setUser(user);
		MissionInformation fakeMissionInformaton = new MissionInformation();
		fakeMissionInformaton.setRelation(relation);
		fakeMissionInformaton.setValue(20);
		retVal.setMissionInformation(fakeMissionInformaton);
		return retVal;
	}

	private Mission prepareMissionForCommonCancelMission(com.kevinguanchedarias.sgtjava.enumerations.MissionType type) {
		Mission retVal = new Mission();
		MissionType missionType = new MissionType();
		missionType.setCode(type.name());
		currentUserPrimaryResource = user.getPrimaryResource();
		currentUserSecondaryResource = user.getSecondaryResource();
		retVal.setId(1L);
		retVal.setType(missionType);
		retVal.setUser(user);
		retVal.setPrimaryResource(100D);
		retVal.setSecondaryResource(200D);
		return retVal;
	}
}
