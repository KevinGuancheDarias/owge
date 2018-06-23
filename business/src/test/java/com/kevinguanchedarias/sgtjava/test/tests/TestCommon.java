package com.kevinguanchedarias.sgtjava.test.tests;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.Field;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.orm.jpa.JpaSystemException;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsonorg.JsonOrgModule;
import com.kevinguanchedarias.sgtjava.entity.Faction;
import com.kevinguanchedarias.sgtjava.entity.Galaxy;
import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.MissionInformation;
import com.kevinguanchedarias.sgtjava.entity.MissionType;
import com.kevinguanchedarias.sgtjava.entity.ObjectEntity;
import com.kevinguanchedarias.sgtjava.entity.ObjectRelation;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.Unit;
import com.kevinguanchedarias.sgtjava.entity.UnitType;
import com.kevinguanchedarias.sgtjava.entity.Upgrade;
import com.kevinguanchedarias.sgtjava.entity.UpgradeType;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.sgtjava.exception.CommonException;
import com.kevinguanchedarias.sgtjava.repository.FactionRepository;
import com.kevinguanchedarias.sgtjava.repository.GalaxyRepository;
import com.kevinguanchedarias.sgtjava.repository.MissionRepository;
import com.kevinguanchedarias.sgtjava.repository.MissionTypeRepository;
import com.kevinguanchedarias.sgtjava.repository.PlanetRepository;
import com.kevinguanchedarias.sgtjava.repository.UpgradeRepository;
import com.kevinguanchedarias.sgtjava.repository.UpgradeTypeRepository;
import com.kevinguanchedarias.sgtjava.repository.UserStorageRepository;

public abstract class TestCommon {

	@Autowired
	private FactionRepository factionRepository;

	@Autowired
	private GalaxyRepository galaxyRepository;

	@Autowired
	private PlanetRepository planetRepository;

	@Autowired
	private UpgradeTypeRepository upgradeTypeRepository;

	@Autowired
	private UpgradeRepository upgradeRepository;

	@Autowired
	private UserStorageRepository userStorageRepository;

	@Autowired
	private MissionTypeRepository missionTypeRepository;
	@Autowired
	private MissionRepository missionRepository;

	protected ObjectEntity prepareValidObjectEntity(RequirementTargetObject type) {
		ObjectEntity retVal = new ObjectEntity();
		retVal.setDescription(type.name());
		return retVal;
	}

	protected ObjectRelation prepareValidObjectRelation(RequirementTargetObject type, Integer refId) {
		ObjectRelation retVal = new ObjectRelation();
		retVal.setObject(prepareValidObjectEntity(type));
		retVal.setReferenceId(refId);
		return retVal;
	}

	protected Faction prepareValidFaction() {
		Faction faction = new Faction();
		faction.setName("Valid faction!");
		faction.setHidden(false);
		faction.setInitialPrimaryResource(100);
		faction.setInitialSecondaryResource(100);
		faction.setInitialEnergy(100);
		faction.setPrimaryResourceProduction(100.0F);
		faction.setSecondaryResourceProduction(100.0F);
		faction.setMaxPlanets(1);

		return faction;
	}

	protected Galaxy prepareValidGalaxy() {
		Galaxy galaxy = new Galaxy();
		galaxy.setName("Test galaxy!");
		return galaxy;
	}

	/**
	 * @param galaxy
	 *            - SHOULD be a persistent one!
	 * @return
	 * @author Kevin Guanche Darias
	 */
	protected Planet prepareValidPlanet(Galaxy galaxy) {
		Planet planet = new Planet();
		planet.setName(galaxy.getName().substring(0, 1) + "01_TEST!");
		planet.setGalaxy(galaxy);
		return planet;
	}

	protected UserStorage prepareValidUser(Integer userId) {
		Galaxy galaxy = prepareValidGalaxy();
		galaxy = galaxyRepository.save(galaxy);

		Planet planet = prepareValidPlanet(galaxy);
		planet = planetRepository.save(planet);

		Faction faction = prepareValidFaction();
		faction = factionRepository.save(faction);

		UserStorage user = new UserStorage();
		user.setId(userId);
		user.setHomePlanet(planet);
		user.setFaction(faction);
		user.setEnergy(1.0);
		return user;
	}

	protected ObtainedUpgrade prepareValidObtainedUpgrade(UserStorage user, Upgrade upgrade) {
		ObtainedUpgrade obtainedUpgrade = new ObtainedUpgrade();
		obtainedUpgrade.setUserId(user);
		obtainedUpgrade.setUpgrade(upgrade);
		obtainedUpgrade.setLevel(1);
		obtainedUpgrade.setAvailable(true);
		return obtainedUpgrade;
	}

	protected Mission prepareValidMission() {
		Mission mission = new Mission();
		mission.setType(missionTypeRepository
				.findOne(com.kevinguanchedarias.sgtjava.enumerations.MissionType.LEVEL_UP.getValue()));
		mission.setTerminationDate(new Date());
		return mission;
	}

	protected MissionType prepareValidMissionType() {
		MissionType missionType = new MissionType();
		missionType.setCode("SOME_CODE");
		missionType.setDescription("lolazo");
		missionType.setIsShared(false);
		return missionType;
	}

	protected MissionInformation prepareValidMissionInformation() {
		MissionInformation missionInformation = new MissionInformation();
		missionInformation.setMission(persistValidMission());
		return missionInformation;
	}

	protected UnitType prepareValidUnitType(int id) {
		UnitType retVal = new UnitType();
		retVal.setId(id);
		retVal.setName("some_name");
		return retVal;
	}

	protected Unit prepareValidUnit(int id, int typeId) {
		Unit retVal = new Unit();
		retVal.setId(id);
		retVal.setName("UnitName");
		retVal.setType(prepareValidUnitType(typeId));
		return retVal;
	}

	protected UpgradeType persistValidUpgradeType() {
		UpgradeType upgradeType = new UpgradeType();
		upgradeType.setName("TestType");
		return upgradeTypeRepository.save(upgradeType);
	}

	/**
	 * 
	 * @param type
	 *            SHOULD BE a persistent one!
	 * @return
	 * @author Kevin Guanche Darias
	 */
	protected Upgrade persistValidUpgrade(UpgradeType type) {
		Upgrade upgrade = new Upgrade();
		upgrade.setName("Test upgrade!");
		upgrade.setType(type);
		return upgradeRepository.save(upgrade);
	}

	/**
	 * 
	 * @param model
	 *            Model faction to save
	 * @param times
	 *            number of factions to save
	 * @return id of last saved one (useful if one)
	 * @author Kevin Guanche Darias
	 */
	protected Number saveFaction(Faction model, int times) {
		for (int i = 0; i < times; i++) {
			Faction currentModel = new Faction();
			BeanUtils.copyProperties(model, currentModel);
			factionRepository.save(model);
		}
		return model.getId();
	}

	protected UserStorage persistValidUserStorage(Integer userId) {
		return userStorageRepository.save(prepareValidUser(userId));
	}

	protected Mission persistValidMission() {
		return missionRepository.save(prepareValidMission());
	}

	/**
	 * Emulates Spring data saving calls
	 * 
	 * @param targetEntity
	 *            class of the target entity
	 * @param repository
	 *            repository of the target entity
	 * @param storage
	 *            Store saved items here
	 * @author Kevin Guanche Darias
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected <E> void mockRepositorySaveAction(Class<E> targetEntity, JpaRepository repository, List<E> storage) {
		Mockito.when(repository.save(Mockito.any(targetEntity))).then(new Answer<E>() {
			@Override
			public E answer(InvocationOnMock invocation) throws Throwable {
				E addedItem = (E) invocation.getArguments()[0];
				Field field = addedItem.getClass().getDeclaredField("id");
				field.setAccessible(true);
				field.set(addedItem, Long.valueOf(storage.size() + 1));

				storage.add(addedItem);
				return addedItem;
			}
		});
	}

	/**
	 * Emulates Spring data deleting calls
	 * 
	 * @param targetEntity
	 *            class of the target entity
	 * @param repository
	 *            repository of the target entity
	 * @param storage
	 *            Remove items from here
	 * @author Kevin Guanche Darias
	 */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	protected <E> void mockRepositoryDeleteAction(Class<E> targetEntity, JpaRepository repository, List<E> storage) {
		Mockito.doAnswer(new Answer<E>() {
			@Override
			public E answer(InvocationOnMock invocation) throws Throwable {
				Field field = targetEntity.getDeclaredField("id");
				field.setAccessible(true);
				Object id = invocation.getArguments()[0];
				for (Iterator<E> iter = storage.iterator(); iter.hasNext();) {
					E currentItem = iter.next();
					Object currentId = field.get(currentItem);
					if (id.equals(currentId)) {
						iter.remove();
						break;
					}
				}

				return null;
			}
		}).when(repository).delete(Mockito.any(Serializable.class));
		;
	}

	/**
	 * Will assert that Hibernate reports null values
	 * 
	 * @param sourceRepository
	 * @param entity
	 * @param field
	 *            - If null will check any field
	 * @author Kevin Guanche Darias
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	protected void checkNullSaveException(JpaRepository sourceRepository, Serializable entity, String field) {
		try {
			sourceRepository.save(entity);
			throw new RuntimeException("Should throw PropertyValueException exception");
		} catch (JpaSystemException e) {
			if (field != null) {
				assertTrue(e.getMessage().endsWith("." + field));
			} else {
				assertTrue(e.getMessage().indexOf("not-null property references a null or") != -1);
			}
		}

	}

	protected SchedulerFactoryBean mockScheduler() {
		SchedulerFactoryBean schedulerFactoryBeanMock = Mockito.mock(SchedulerFactoryBean.class);
		return mockScheduler(schedulerFactoryBeanMock);
	}

	protected SchedulerFactoryBean mockScheduler(SchedulerFactoryBean schedulerFactoryBeanMock) {
		Scheduler schedulerMock = Mockito.mock(Scheduler.class);
		Mockito.when(schedulerFactoryBeanMock.getScheduler()).thenReturn(schedulerMock);
		return schedulerFactoryBeanMock;
	}

	protected void testHasScheduledJob(SchedulerFactoryBean schedulerFactoryBeanMock) throws SchedulerException {
		Scheduler schedulerMock = schedulerFactoryBeanMock.getScheduler();
		Mockito.verify(schedulerMock, Mockito.times(1)).addJob(Mockito.any(JobDetail.class), Mockito.anyBoolean(),
				Mockito.anyBoolean());
		Mockito.verify(schedulerMock, Mockito.times(1)).scheduleJob(Mockito.any(Trigger.class));
	}

	protected Map<String, Object> parseJson(String json) {
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(new JsonOrgModule());
		try {
			return mapper.readValue(json, new TypeReference<HashMap<String, Object>>() {
			});
		} catch (IOException e) {
			throw new CommonException("Couldn't parse JSON", e);
		}
	}
}
