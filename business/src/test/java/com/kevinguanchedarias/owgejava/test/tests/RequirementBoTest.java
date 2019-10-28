package com.kevinguanchedarias.owgejava.test.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

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
import com.kevinguanchedarias.owgejava.business.ObtainedUpgradeBo;
import com.kevinguanchedarias.owgejava.business.RequirementBo;
import com.kevinguanchedarias.owgejava.business.UpgradeBo;
import com.kevinguanchedarias.owgejava.business.UserStorageBo;
import com.kevinguanchedarias.owgejava.dao.RequirementInformationDao;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.ObjectEntity;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.Requirement;
import com.kevinguanchedarias.owgejava.entity.RequirementInformation;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnlockedRelation;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTypeEnum;
import com.kevinguanchedarias.owgejava.repository.RequirementRepository;
import com.kevinguanchedarias.owgejava.repository.UnlockedRelationRepository;

@RunWith(MockitoJUnitRunner.class)
public class RequirementBoTest extends TestCommon {

	@Mock
	private UserStorageBo userStorageBoMock;

	@Mock
	private RequirementRepository requirementRepositoryMock;

	@Mock
	private RequirementInformationDao requirementDaoMock;

	@Mock
	private UnlockedRelationRepository unlockedRelationRepositoryMock;

	@Mock
	private ObtainedUpgradeBo obtainedUpgradeBoMock;

	@Mock
	private UpgradeBo upgradeBo;

	@Mock
	private ObtainedUnitBo obtainedUnitBoMock;

	@InjectMocks
	private RequirementBo requirementBo;

	private List<UnlockedRelation> unlockedRelationsStore;

	private UserStorage user;

	private Integer selectedFaction = 1;

	@Before
	public void init() {
		unlockedRelationsStore = new ArrayList<>();
		mockRepositorySaveAction(UnlockedRelation.class, unlockedRelationRepositoryMock, unlockedRelationsStore);
		mockRepositoryDeleteAction(UnlockedRelation.class, unlockedRelationRepositoryMock, unlockedRelationsStore);
		user = new UserStorage();
		user.setId(1);
		Galaxy homeGalaxy = prepareValidGalaxy();
		homeGalaxy.setId(1);
		Planet homePlanet = prepareValidPlanet(homeGalaxy);
		homePlanet.setId(1L);
		user.setHomePlanet(homePlanet);

		Mockito.when(userStorageBoMock.isOfFaction(selectedFaction, user.getId())).thenReturn(true);
		// Whitebox.setInternalState(target, field, value);
	}

	@Test
	public void triggerFactionSelectionShouldInsertUnlockedRelationOnlyForValid() {
		int validRelationId = 1;
		List<ObjectRelation> relations = new ArrayList<>();

		ObjectRelation validRelation = prepareRelationWithOneRequirement(validRelationId, 1L,
				RequirementTypeEnum.BEEN_RACE);
		relations.add(validRelation);

		ObjectRelation relationHavingOtherFactionAsReq = prepareRelationWithOneRequirement(2, 3L,
				RequirementTypeEnum.BEEN_RACE);
		relations.add(relationHavingOtherFactionAsReq);

		ObjectRelation relationHavingValidFactionButInvalidGalaxy = prepareRelationWithOneRequirement(3, 1L,
				RequirementTypeEnum.BEEN_RACE);
		relationHavingValidFactionButInvalidGalaxy.getRequirements()
				.add(prepareRequirementInformation(RequirementTypeEnum.HOME_GALAXY, 44L, null));

		Mockito.when(requirementDaoMock.findObjectRelationsHavingRequirementType(RequirementTypeEnum.BEEN_RACE))
				.thenReturn(relations);

		requirementBo.triggerFactionSelection(user);
		assertEquals(1, unlockedRelationsStore.size());
		assertEquals(validRelation.getId(), unlockedRelationsStore.get(0).getRelation().getId());
	}

	@Test
	public void triggerHomeGalaxySelectionShouldInsertUnlockedRelationsOnlyForValid() {
		int validRelationId = 1;
		List<ObjectRelation> relations = new ArrayList<>();

		ObjectRelation validRelation = prepareRelationWithOneRequirement(validRelationId,
				Long.valueOf(user.getHomePlanet().getGalaxy().getId()), RequirementTypeEnum.HOME_GALAXY);
		ObjectRelation invalidRelation = prepareRelationWithOneRequirement(2, 329L, RequirementTypeEnum.HOME_GALAXY);

		Mockito.when(requirementDaoMock.findObjectRelationsHavingRequirementType(RequirementTypeEnum.HOME_GALAXY))
				.thenReturn(relations);

		relations.add(validRelation);
		relations.add(invalidRelation);

		requirementBo.triggerHomeGalaxySelection(user);
		assertEquals(1, unlockedRelationsStore.size());
		assertEquals(validRelation.getId(), unlockedRelationsStore.get(0).getRelation().getId());
	}

	@Test
	public void triggerLevelUpShouldInsertUnlockedRelationsOnlyForValid() {
		int validRelationId = 1;
		long upgradeId = 20;
		long validLevel = 4L;
		List<ObjectRelation> relations = new ArrayList<>();

		Upgrade upgrade = new Upgrade();
		upgrade.setId(1);
		Mockito.when(upgradeBo.findById(Mockito.any(Integer.class))).thenReturn(upgrade);

		ObtainedUpgrade obtainedUpgrade = new ObtainedUpgrade();
		obtainedUpgrade.setLevel(4);
		Mockito.when(obtainedUpgradeBoMock.findByUserAndUpgrade(1, 1)).thenReturn(obtainedUpgrade);

		ObjectRelation validRelation = prepareRelationWithOneRequirement(validRelationId, upgradeId, validLevel,
				RequirementTypeEnum.UPGRADE_LEVEL, RequirementTargetObject.UNIT);
		ObjectRelation invalidRelation = prepareRelationWithOneRequirement(4, 20L, 7L,
				RequirementTypeEnum.UPGRADE_LEVEL, RequirementTargetObject.UNIT);

		Mockito.when(requirementDaoMock.findObjectRelationsHavingRequirementType(RequirementTypeEnum.UPGRADE_LEVEL))
				.thenReturn(relations);

		relations.add(validRelation);
		relations.add(invalidRelation);

		requirementBo.triggerLevelUpCompleted(user);
		assertEquals(1, unlockedRelationsStore.size());
		assertEquals(validRelation.getId(), unlockedRelationsStore.get(0).getRelation().getId());
	}

	@Test
	public void shouldTestAllReqsOfSelectedRelation() {
		Long validFaction = 1L;
		List<ObjectRelation> relations = new ArrayList<>();

		ObjectRelation relationHavingMultipleValidReq = prepareRelationWithOneRequirement(1, validFaction,
				RequirementTypeEnum.BEEN_RACE);
		relationHavingMultipleValidReq.getRequirements()
				.add(prepareRequirementInformation(RequirementTypeEnum.HOME_GALAXY, 1L, null));
		ObjectRelation relationHavingValidFactionButInvalidGalaxy = prepareRelationWithOneRequirement(2, validFaction,
				RequirementTypeEnum.BEEN_RACE);
		relationHavingValidFactionButInvalidGalaxy.getRequirements()
				.add(prepareRequirementInformation(RequirementTypeEnum.HOME_GALAXY, 44L, null));
		ObjectRelation relationHavingValidGalaxyButInvalidFaction = prepareRelationWithOneRequirement(3, 44L,
				RequirementTypeEnum.BEEN_RACE);
		relationHavingValidGalaxyButInvalidFaction.getRequirements().add(prepareRequirementInformation(
				RequirementTypeEnum.HOME_GALAXY, Long.valueOf(user.getHomePlanet().getGalaxy().getId()), null));

		relations.add(relationHavingMultipleValidReq);
		relations.add(relationHavingValidFactionButInvalidGalaxy);
		relations.add(relationHavingValidGalaxyButInvalidFaction);
		Mockito.when(requirementDaoMock.findObjectRelationsHavingRequirementType(RequirementTypeEnum.BEEN_RACE))
				.thenReturn(relations);

		requirementBo.triggerFactionSelection(user);
		assertEquals(1, unlockedRelationsStore.size());
		assertEquals(relationHavingMultipleValidReq.getId(), unlockedRelationsStore.get(0).getRelation().getId());
	}

	@Test
	public void shouldRemoveFromUnlockedRelationsWhenRelationRequirementsAreNotLongerMet() {
		List<ObjectRelation> relations = new ArrayList<>();
		ObjectRelation validRelation = prepareRelationWithOneRequirement(1, 1L, RequirementTypeEnum.BEEN_RACE);
		unlockedRelationsStore
				.add(prepareUnlockedRelationWithOneRequirement(2, 2L, RequirementTypeEnum.HOME_GALAXY, user));
		unlockedRelationsStore
				.add(prepareUnlockedRelationWithOneRequirement(3, 2L, RequirementTypeEnum.BEEN_RACE, user));
		UnlockedRelation multipleOne = prepareUnlockedRelationWithOneRequirement(4, 2L, RequirementTypeEnum.BEEN_RACE,
				user);
		multipleOne.getRelation().getRequirements()
				.add(prepareRequirementInformation(RequirementTypeEnum.BEEN_RACE, 1L, null));
		unlockedRelationsStore.add(multipleOne);
		assertEquals(3, unlockedRelationsStore.size());

		relations.add(validRelation);
		relations.add(unlockedRelationsStore.get(0).getRelation());
		relations.add(unlockedRelationsStore.get(1).getRelation());
		relations.add(unlockedRelationsStore.get(2).getRelation());
		assertEquals(4, relations.size());

		Mockito.when(unlockedRelationRepositoryMock.findOneByUserIdAndRelationId(1, 2))
				.thenReturn(unlockedRelationsStore.get(0));
		Mockito.when(unlockedRelationRepositoryMock.findOneByUserIdAndRelationId(1, 3))
				.thenReturn(unlockedRelationsStore.get(1));
		Mockito.when(unlockedRelationRepositoryMock.findOneByUserIdAndRelationId(1, 4))
				.thenReturn(unlockedRelationsStore.get(2));
		Mockito.when(requirementDaoMock.findObjectRelationsHavingRequirementType(RequirementTypeEnum.BEEN_RACE))
				.thenReturn(relations);

		requirementBo.triggerFactionSelection(user);
		assertEquals(1, unlockedRelationsStore.size());
		assertEquals(validRelation.getId(), unlockedRelationsStore.get(0).getRelation().getId());
	}

	@Test
	public void shouldNotCallSaveObtainedUpgradeWhenTypeIsNotAnUpgrade() {
		List<ObjectRelation> relations = new ArrayList<>();

		ObjectRelation relation = prepareRelationWithOneRequirement(1, 1L, null, RequirementTypeEnum.BEEN_RACE,
				RequirementTargetObject.UNIT);

		relations.add(relation);
		Mockito.when(requirementDaoMock.findObjectRelationsHavingRequirementType(RequirementTypeEnum.BEEN_RACE))
				.thenReturn(relations);
		requirementBo.triggerFactionSelection(user);
		Mockito.verify(obtainedUpgradeBoMock, Mockito.never()).save(Mockito.any(ObtainedUpgrade.class));
	}

	@Test
	public void shouldCallSaveObtainedUpgradeWhenUseralreadyHasItSettingAvailableToTrueIfItWasNot() {
		List<ObjectRelation> relations = new ArrayList<>();
		ObjectRelation relation = prepareRelationWithOneRequirement(1, 1L, RequirementTypeEnum.BEEN_RACE);
		relation.setReferenceId(1);
		relations.add(relation);
		ObtainedUpgrade obtainedUpgrade = new ObtainedUpgrade();
		obtainedUpgrade.setId(1L);
		obtainedUpgrade.setUserId(user);
		Mockito.when(obtainedUpgradeBoMock.findUserObtainedUpgrade(1, 1)).thenReturn(obtainedUpgrade);
		Mockito.when(requirementDaoMock.findObjectRelationsHavingRequirementType(RequirementTypeEnum.BEEN_RACE))
				.thenReturn(relations);
		Mockito.when(obtainedUpgradeBoMock.userHasUpgrade(1, 1)).thenReturn(true);
		requirementBo.triggerFactionSelection(user);
		assertTrue(obtainedUpgrade.getAvailable());
		Mockito.verify(obtainedUpgradeBoMock).save(Mockito.any(ObtainedUpgrade.class));
	}

	@Test
	public void shouldSetObtainedUpgradeToUnavailableWhenReqsAreNotLongerMet() {
		List<ObjectRelation> relations = new ArrayList<>();
		ObjectRelation relation = prepareRelationWithOneRequirement(1, 4L, RequirementTypeEnum.BEEN_RACE);
		relation.setReferenceId(1);
		relations.add(relation);
		ObtainedUpgrade obtainedUpgrade = new ObtainedUpgrade();
		obtainedUpgrade.setId(1L);
		obtainedUpgrade.setUserId(user);
		obtainedUpgrade.setAvailable(true);
		Mockito.when(obtainedUpgradeBoMock.findUserObtainedUpgrade(1, 1)).thenReturn(obtainedUpgrade);
		Mockito.when(requirementDaoMock.findObjectRelationsHavingRequirementType(RequirementTypeEnum.BEEN_RACE))
				.thenReturn(relations);
		Mockito.when(obtainedUpgradeBoMock.userHasUpgrade(1, 1)).thenReturn(true);
		requirementBo.triggerFactionSelection(user);
		assertFalse(obtainedUpgrade.getAvailable());
		Mockito.verify(obtainedUpgradeBoMock).save(Mockito.any(ObtainedUpgrade.class));
	}

	@Test
	public void shouldCallSaveObtainedUpgradeWhenRelationIsAnUpgradeAndUserDoesNotHaveIt() {
		List<ObjectRelation> relations = new ArrayList<>();
		ObjectRelation relation = prepareRelationWithOneRequirement(1, 1L, RequirementTypeEnum.BEEN_RACE);
		relation.setReferenceId(1);
		relations.add(relation);
		Mockito.when(requirementDaoMock.findObjectRelationsHavingRequirementType(RequirementTypeEnum.BEEN_RACE))
				.thenReturn(relations);
		Mockito.when(obtainedUpgradeBoMock.userHasUpgrade(1, 1)).thenReturn(false);
		requirementBo.triggerFactionSelection(user);
		Mockito.verify(obtainedUpgradeBoMock).save(Mockito.any(ObtainedUpgrade.class));
	}

	@Test
	public void onTriggerUnitBuildShouldUnlockedBecauseUnitIsAvailable() {
		int unitId = 1;
		Unit unit = new Unit();
		unit.setId(unitId);
		ObtainedUnit obtainedUnit = new ObtainedUnit();
		obtainedUnit.setId(Long.valueOf(unitId));
		obtainedUnit.setUnit(unit);

		ObjectRelation relation = prepareRelationWithOneRequirement(1, unit.getId().longValue(),
				RequirementTypeEnum.HAVE_UNIT);

		List<ObjectRelation> relations = new ArrayList<>();
		relations.add(relation);

		Mockito.when(requirementDaoMock.findByRequirementTypeAndSecondValue(RequirementTypeEnum.HAVE_UNIT,
				unit.getId().longValue())).thenReturn(relations);
		Mockito.when(obtainedUnitBoMock.findOneByUserIdAndUnitId(user.getId(), unit.getId())).thenReturn(obtainedUnit);
		requirementBo.triggerUnitBuildCompleted(user, unit);

		Mockito.verify(obtainedUnitBoMock).findOneByUserIdAndUnitId(user.getId(), unit.getId());
		Mockito.verify(unlockedRelationRepositoryMock).save(Mockito.any(UnlockedRelation.class));

	}

	@Test
	public void onTriggerUnitBuildShouldUnlockBecauseUnitAmountIsGreaterOrEqual() {
		long fakeCount = 2L;
		int unitId = 1;
		Unit unit = new Unit();
		unit.setId(unitId);
		ObtainedUnit obtainedUnit = new ObtainedUnit();
		obtainedUnit.setId(Long.valueOf(unitId));
		obtainedUnit.setUnit(unit);

		ObjectRelation relation = prepareRelationWithOneRequirement(1, unit.getId().longValue(), fakeCount,
				RequirementTypeEnum.UNIT_AMOUNT, RequirementTargetObject.UNIT);

		List<ObjectRelation> relations = new ArrayList<>();
		relations.add(relation);

		Mockito.when(obtainedUnitBoMock.countByUserAndUnitId(user, unitId)).thenReturn(fakeCount);
		Mockito.when(requirementDaoMock.findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(
				RequirementTypeEnum.UNIT_AMOUNT, unit.getId().longValue(), fakeCount)).thenReturn(relations);
		Mockito.when(obtainedUnitBoMock.findOneByUserIdAndUnitId(user.getId(), unit.getId())).thenReturn(obtainedUnit);
		requirementBo.triggerUnitBuildCompleted(user, unit);

		Mockito.verify(obtainedUnitBoMock, Mockito.times(2)).countByUserAndUnitId(user, unitId);
		Mockito.verify(unlockedRelationRepositoryMock).save(Mockito.any(UnlockedRelation.class));
	}

	@Test
	public void onTriggerUnitBuildShouldNotUnlockBecauseUnitAmountIsNotGreaterOrEqual() {
		long requirementValue = 2L;
		long userValue = 1L;
		int unitId = 1;
		Unit unit = new Unit();
		unit.setId(unitId);
		ObtainedUnit obtainedUnit = new ObtainedUnit();
		obtainedUnit.setId(Long.valueOf(unitId));
		obtainedUnit.setUnit(unit);

		UnlockedRelation unlockedRelation = new UnlockedRelation();
		unlockedRelation.setId(1L);

		ObjectRelation relation = prepareRelationWithOneRequirement(1, unit.getId().longValue(), requirementValue,
				RequirementTypeEnum.UNIT_AMOUNT, RequirementTargetObject.UNIT);

		List<ObjectRelation> relations = new ArrayList<>();
		relations.add(relation);

		Mockito.when(obtainedUnitBoMock.countByUserAndUnitId(user, unitId)).thenReturn(userValue);
		Mockito.when(requirementDaoMock.findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(
				RequirementTypeEnum.UNIT_AMOUNT, unit.getId().longValue(), userValue)).thenReturn(relations);
		Mockito.when(obtainedUnitBoMock.findOneByUserIdAndUnitId(user.getId(), unit.getId())).thenReturn(obtainedUnit);
		Mockito.when(unlockedRelationRepositoryMock.findOneByUserIdAndRelationId(user.getId(), relation.getId()))
				.thenReturn(unlockedRelation);
		requirementBo.triggerUnitBuildCompleted(user, unit);

		Mockito.verify(obtainedUnitBoMock, Mockito.times(2)).countByUserAndUnitId(user, unitId);
		Mockito.verify(unlockedRelationRepositoryMock, Mockito.never()).save(Mockito.any(UnlockedRelation.class));
		Mockito.verify(unlockedRelationRepositoryMock).delete(unlockedRelation.getId());
	}

	private Requirement emulateRequirement(RequirementTypeEnum type) {
		Requirement requirement = new Requirement();
		requirement.setId(type.getValue());
		requirement.setCode(type.name());
		return requirement;
	}

	private RequirementInformation prepareRequirementInformation(RequirementTypeEnum type, Long secondValue,
			Long thirdValue) {
		RequirementInformation retVal = new RequirementInformation();
		retVal.setRequirement(emulateRequirement(type));
		retVal.setSecondValue(secondValue);
		retVal.setThirdValue(thirdValue);
		return retVal;
	}

	private UnlockedRelation prepareUnlockedRelationWithOneRequirement(Integer relationId, Long secondValue,
			RequirementTypeEnum type, UserStorage user) {
		UnlockedRelation retVal = new UnlockedRelation();
		retVal.setRelation(prepareRelationWithOneRequirement(relationId, secondValue, type));
		retVal.setUser(user);
		retVal.setId(Long.valueOf(relationId));
		return retVal;
	}

	private ObjectRelation prepareRelationWithOneRequirement(Integer relationId, Long secondValue, Long thirdValue,
			RequirementTypeEnum type, RequirementTargetObject target) {
		ObjectRelation retVal = new ObjectRelation();
		ObjectEntity object = new ObjectEntity();
		object.setDescription(target.name());
		object.setRepository("lieOne");
		retVal.setObject(object);
		retVal.setId(relationId);
		RequirementInformation requirement = new RequirementInformation();
		requirement.setSecondValue(secondValue);
		requirement.setThirdValue(thirdValue);
		requirement.setRequirement(emulateRequirement(type));
		List<RequirementInformation> requirementsForRelation = new ArrayList<>();
		requirementsForRelation.add(requirement);
		retVal.setRequirements(requirementsForRelation);

		return retVal;
	}

	private ObjectRelation prepareRelationWithOneRequirement(Integer relationId, Long secondValue,
			RequirementTypeEnum type) {
		return prepareRelationWithOneRequirement(relationId, secondValue, null, type, RequirementTargetObject.UPGRADE);
	}

}