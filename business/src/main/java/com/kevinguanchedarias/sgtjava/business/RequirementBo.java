package com.kevinguanchedarias.sgtjava.business;

import java.io.Serializable;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.dao.RequirementInformationDao;
import com.kevinguanchedarias.sgtjava.entity.ObjectRelation;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.sgtjava.entity.Requirement;
import com.kevinguanchedarias.sgtjava.entity.RequirementInformation;
import com.kevinguanchedarias.sgtjava.entity.Unit;
import com.kevinguanchedarias.sgtjava.entity.UnlockedRelation;
import com.kevinguanchedarias.sgtjava.entity.Upgrade;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.sgtjava.enumerations.RequirementType;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.sgtjava.repository.RequirementRepository;
import com.kevinguanchedarias.sgtjava.repository.UnlockedRelationRepository;

@Component
@Transactional
public class RequirementBo implements Serializable {
	private static final long serialVersionUID = -7069590234333605969L;

	@Autowired
	private UserStorageBo userStorageBo;

	@Autowired
	private RequirementRepository requirementRepository;

	@Autowired
	private RequirementInformationDao requirementDao;

	@Autowired
	private UnlockedRelationRepository unlockedRelationRepository;

	@Autowired
	private ObtainedUpradeBo obtainedUpgradeBo;

	@Autowired
	private UpgradeBo upgradeBo;

	@Autowired
	private ObjectRelationBo objectRelationBo;

	@Autowired
	private ObtainedUnitBo obtainedUnitBo;

	public List<Requirement> findAll() {
		return requirementRepository.findAll();
	}

	/**
	 * Will return requirement for specified object type
	 * 
	 * @param targetObject
	 *            - Type of object
	 * @param referenceId
	 *            - Id on the target entity, for example id of an upgrade, or an
	 *            unit
	 * @author Kevin Guanche Darias
	 */
	public List<RequirementInformation> getRequirements(RequirementTargetObject targetObject, Integer referenceId) {
		return requirementDao.getRequirements(targetObject, referenceId);
	}

	/**
	 * Checks if requirements are met for all objects, and fills the
	 * unlocked_relation table <b>EXPENSIVE METHOD!</b>
	 * 
	 * @param userId
	 *            user to which the requirements are going to be checked
	 */
	@Transactional
	public void processRequirementsForAllRequirementTypes(UserStorage user) {
		processRelationList(requirementDao.findAllObjectRelations(), user);
	}

	/**
	 * Checks requirements when race has been selected
	 * 
	 * @param user
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void triggerFactionSelection(UserStorage user) {
		processRelationList(requirementDao.findObjectRelationsHavingRequirementType(RequirementType.BEEN_RACE), user);
	}

	/**
	 * Chacks requirements when galaxy has been assigned
	 * 
	 * @param user
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void triggerHomeGalaxySelection(UserStorage user) {
		processRelationList(requirementDao.findObjectRelationsHavingRequirementType(RequirementType.HOME_GALAXY), user);
	}

	/**
	 * Checks requirements when level up mission has been completed!
	 * 
	 * @param user
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void triggerLevelUpCompleted(UserStorage user) {
		processRelationList(requirementDao.findObjectRelationsHavingRequirementType(RequirementType.UPGRADE_LEVEL),
				user);
	}

	/**
	 * Checks requirements that has dependency on having this unit<br>
	 * Following requirements are checked:
	 * <ul>
	 * <li>HAVE_UNIT</li>
	 * <li>UNIT_AMOUNT</li>
	 * </ul>
	 * 
	 * @param user
	 * @param unit
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void triggerUnitBuildCompleted(UserStorage user, Unit unit) {
		long count = obtainedUnitBo.countByUserAndUnitId(user, unit.getId());
		processRelationList(
				requirementDao.findByRequirementTypeAndSecondValue(RequirementType.HAVE_UNIT, unit.getId().longValue()),
				user);
		processRelationList(requirementDao.findByRequirementTypeAndSecondValueAndThirdValueGreaterThanEqual(
				RequirementType.UNIT_AMOUNT, unit.getId().longValue(), count), user);
	}

	/**
	 * Checks if all users met the new requirements of the changed relation
	 * 
	 * @param relation
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void triggerRelationChanged(ObjectRelation relation) {
		ObjectRelation withSessionRelation = objectRelationBo.refresh(relation);
		List<UserStorage> users = userStorageBo.findAll();
		for (UserStorage user : users) {
			processRelation(withSessionRelation, user);
		}
	}

	/**
	 * Process the list of relations, and add or remove then from
	 * unlocked_relation table if requirements are met or not
	 * 
	 * @param relations
	 * @param user
	 * @author Kevin Guanche Darias
	 */
	private void processRelationList(List<ObjectRelation> relations, UserStorage user) {
		for (ObjectRelation currentRelation : relations) {
			processRelation(currentRelation, user);
		}
	}

	/**
	 * Process single relation change
	 * 
	 * @param relation
	 *            relation persisted entity
	 * @param user
	 * @author Kevin Guanche Darias
	 */
	private void processRelation(ObjectRelation relation, UserStorage user) {
		if (checkRequirementsAreMet(relation, user)) {
			registerObtainedRelation(relation, user);
		} else {
			unregisterLossedRelation(relation, user);
		}
	}

	/**
	 * Will check that all requirements are met for given relation and user
	 * 
	 * @param objectRelation
	 * @param user
	 * @return True if object can be used
	 * @author Kevin Guanche Darias
	 */
	private Boolean checkRequirementsAreMet(ObjectRelation objectRelation, UserStorage user) {
		for (RequirementInformation currentRequirement : objectRelation.getRequirements()) {
			boolean status;
			switch (RequirementType.valueOf(currentRequirement.getRequirement().getCode())) {
			case UPGRADE_LEVEL:
				status = checkUpgradeLevelRequirement(currentRequirement, user.getId());
				break;
			case HAVE_UNIT:
				status = checkHaveUnitRequirement(currentRequirement, user);
				break;
			case UNIT_AMOUNT:
				status = checkUnitAmountRequirement(currentRequirement, user);
				break;
			case BEEN_RACE:
				status = checkBeenFactionRequirement(currentRequirement, user.getId());
				break;
			case HOME_GALAXY:
				status = checkBeenGalaxyRequirement(currentRequirement, user);
				break;
			default:
				throw new SgtBackendNotImplementedException(
						"Not implemented requirement type: " + currentRequirement.getRequirement().getCode());
			}
			if (!status) {
				return false;
			}
		}
		return true;
	}

	private boolean checkUpgradeLevelRequirement(RequirementInformation requirementInformation, Integer userId) {
		Upgrade upgrade = upgradeBo.findById(requirementInformation.getSecondValue().intValue());
		int level;
		ObtainedUpgrade obtainedUpgrade = obtainedUpgradeBo.findByUserAndUpgrade(userId, upgrade.getId());
		if (obtainedUpgrade == null) {
			level = 0;
		} else {
			level = obtainedUpgrade.getLevel();
		}
		return level >= requirementInformation.getThirdValue();
	}

	private boolean checkHaveUnitRequirement(RequirementInformation requirementInformation, UserStorage user) {
		return obtainedUnitBo.findOneByUserIdAndUnitId(user.getId(),
				requirementInformation.getSecondValue().intValue()) != null;
	}

	private boolean checkUnitAmountRequirement(RequirementInformation requirementInformation, UserStorage user) {
		return obtainedUnitBo.countByUserAndUnitId(user,
				requirementInformation.getSecondValue().intValue()) >= requirementInformation.getThirdValue();
	}

	private boolean checkBeenFactionRequirement(RequirementInformation requirement, Integer userId) {
		return userStorageBo.isOfFaction(requirement.getSecondValue().intValue(), userId);
	}

	private boolean checkBeenGalaxyRequirement(RequirementInformation requirement, UserStorage user) {
		return user.getHomePlanet().getGalaxy().getId().equals(requirement.getSecondValue().intValue());
	}

	/**
	 * Will register the obtained object <br />
	 * <b>NOTICE: If relation already exists in unlocked_relation table will
	 * just do nothing</b><br />
	 * If relation is an upgrade, will save it to the ObtainedUpgrades!
	 * 
	 * @param relation
	 * @param user
	 * @author Kevin Guanche Darias
	 */
	private void registerObtainedRelation(ObjectRelation relation, UserStorage user) {
		if (findUnlockedObjectRelation(relation.getId(), user.getId()) == null) {
			UnlockedRelation unlockedRelation = new UnlockedRelation();
			unlockedRelation.setRelation(relation);
			unlockedRelation.setUser(user);
			unlockedRelationRepository.save(unlockedRelation);
			if (RequirementTargetObject.UPGRADE.name().equals(relation.getObject().getDescription())) {
				if (obtainedUpgradeBo.userHasUpgrade(user.getId(), relation.getReferenceId())) {
					alterObtainedUpgradeAvailability(
							obtainedUpgradeBo.findUserObtainedUpgrade(user.getId(), relation.getReferenceId()), true);
				} else {
					registerObtainedUpgrade(user, relation.getReferenceId());
				}
			}
		}
	}

	private void unregisterLossedRelation(ObjectRelation relation, UserStorage user) {
		UnlockedRelation unlockedRelation = findUnlockedObjectRelation(relation.getId(), user.getId());
		if (unlockedRelation != null) {
			unlockedRelationRepository.delete(unlockedRelation.getId());
		}

		if (RequirementTargetObject.UPGRADE.name().equals(relation.getObject().getDescription())
				&& obtainedUpgradeBo.userHasUpgrade(user.getId(), relation.getReferenceId())) {
			alterObtainedUpgradeAvailability(
					obtainedUpgradeBo.findUserObtainedUpgrade(user.getId(), relation.getReferenceId()), false);
		}
	}

	private UnlockedRelation findUnlockedObjectRelation(Integer relationId, Integer userId) {
		return unlockedRelationRepository.findOneByUserIdAndRelationId(userId, relationId);
	}

	private void registerObtainedUpgrade(UserStorage user, Integer upgradeId) {
		ObtainedUpgrade obtainedUpgrade = new ObtainedUpgrade();
		obtainedUpgrade.setLevel(0);
		obtainedUpgrade.setUpgrade(upgradeBo.findById(upgradeId));
		obtainedUpgrade.setUserId(user);
		obtainedUpgrade.setAvailable(true);
		obtainedUpgradeBo.save(obtainedUpgrade);
	}

	private void alterObtainedUpgradeAvailability(ObtainedUpgrade obtainedUpgrade, Boolean available) {
		obtainedUpgrade.setAvailable(available);
		obtainedUpgradeBo.save(obtainedUpgrade);
	}
}
