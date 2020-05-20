package com.kevinguanchedarias.owgejava.business;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.owgejava.dto.RunningUpgradeDto;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.Mission.MissionIdAndTerminationDateProjection;
import com.kevinguanchedarias.owgejava.entity.MissionInformation;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.ObtainedUpgrade;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.Upgrade;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.enumerations.RequirementTargetObject;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.exception.MissionNotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendNotImplementedException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendUnitBuildAlreadyRunningException;
import com.kevinguanchedarias.owgejava.exception.SgtLevelUpMissionAlreadyRunningException;
import com.kevinguanchedarias.owgejava.exception.SgtMissionRegistrationException;
import com.kevinguanchedarias.owgejava.pojo.ResourceRequirementsPojo;

@Service
public class MissionBo extends AbstractMissionBo {
	private static final long serialVersionUID = 5505953709078785322L;

	private static final Logger LOG = Logger.getLogger(MissionBo.class);
	private static final String JOB_GROUP_NAME = "Missions";
	private static final String MISSION_NOT_FOUND = "Mission doesn't exists, maybe it was cancelled";

	@Override
	public String getGroupName() {
		return JOB_GROUP_NAME;
	}

	@Override
	public Logger getLogger() {
		return LOG;
	}

	/**
	 * Registers a level up mission
	 *
	 * @param userId    user that has requested level up
	 * @param upgradeId the id of the upgrade that the user wants to level up
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void registerLevelUpAnUpgrade(Integer userId, Integer upgradeId) {
		checkUpgradeMissionDoesNotExists(userId);
		ObtainedUpgrade obtainedUpgrade = obtainedUpgradeBo.findByUserAndUpgrade(userId, upgradeId);
		checkUpgradeIsAvailable(obtainedUpgrade);

		UserStorage user = userStorageBo.findById(userId);
		checkCanDoMisison(user);
		ResourceRequirementsPojo resourceRequirements = upgradeBo.calculateRequirementsAreMet(obtainedUpgrade);
		if (!resourceRequirements.canRun(user, userStorageBo)) {
			throw new SgtMissionRegistrationException("No enough resources!");
		}
		resourceRequirements.setRequiredTime(resourceRequirements.getRequiredTime() * 2
				+ improvementBo.computePlusPercertage((float) -resourceRequirements.getRequiredTime(),
						improvementBo.findUserImprovement(user).getMoreUpgradeResearchSpeed()));
		ObjectRelation relation = objectRelationBo.findOneByObjectTypeAndReferenceId(RequirementTargetObject.UPGRADE,
				obtainedUpgrade.getUpgrade().getId());

		MissionInformation missionInformation = new MissionInformation();
		missionInformation.setRelation(relation);
		missionInformation.setValue(obtainedUpgrade.getLevel() + 1);

		Mission mission = new Mission();
		mission.setMissionInformation(missionInformation);
		attachRequirementstoMission(mission, resourceRequirements);
		mission.setType(findMissionType(MissionType.LEVEL_UP));
		mission.setUser(user);
		missionInformation.setMission(mission);

		substractResources(user, mission);

		userStorageBo.save(user);
		missionRepository.save(mission);
		scheduleMission(mission);
	}

	/**
	 * Process the effects of leveling up an upgrade
	 *
	 * @param missionId Id of the mission to process
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void processLevelUpAnUpgrade(Long missionId) {
		Mission mission = findById(missionId);
		if (mission != null) {
			MissionInformation missionInformation = mission.getMissionInformation();
			Upgrade upgrade = objectRelationBo.unboxObjectRelation(missionInformation.getRelation());
			ObtainedUpgrade obtainedUpgrade = obtainedUpgradeBo.findByUserAndUpgrade(mission.getUser().getId(),
					upgrade.getId());
			obtainedUpgrade.setLevel(missionInformation.getValue().intValue());
			obtainedUpgradeBo.save(obtainedUpgrade);
			requirementBo.triggerLevelUpCompleted(mission.getUser());
			improvementBo.clearSourceCache(mission.getUser(), obtainedUpgradeBo);
			delete(mission);
		} else {
			LOG.debug(MISSION_NOT_FOUND);
		}
	}

	/**
	 * Creates a mission of type unit build
	 *
	 * @param userId
	 * @param planetId
	 * @param unitId
	 * @param finalCount
	 * @author Kevin Guanche Darias
	 */
	public RunningUnitBuildDto registerBuildUnit(Integer userId, Long planetId, Integer unitId, Long count) {
		planetBo.myCheckIsOfUserProperty(planetId);
		checkUnitBuildMissionDoesNotExists(userId, planetId);
		ObjectRelation relation = objectRelationBo.findOneByObjectTypeAndReferenceId(RequirementTargetObject.UNIT,
				unitId);
		checkUnlockedUnit(userId, relation);
		UserStorage user = userStorageBo.findById(userId);
		checkCanDoMisison(user);
		Unit unit = unitBo.findByIdOrDie(unitId);
		Long finalCount = Boolean.TRUE.equals(unit.getIsUnique()) ? 1 : count;
		unitBo.checkIsUniqueBuilt(user, unit);
		ResourceRequirementsPojo resourceRequirements = unitBo.calculateRequirements(unit, finalCount);
		if (!resourceRequirements.canRun(user, userStorageBo)) {
			throw new SgtMissionRegistrationException("No enough resources!");
		}
		resourceRequirements.setRequiredTime(resourceRequirements.getRequiredTime() * 2
				+ improvementBo.computePlusPercertage((float) -resourceRequirements.getRequiredTime(),
						improvementBo.findUserImprovement(user).getMoreUnitBuildSpeed()));
		obtainedUnitBo.checkWouldReachUnitTypeLimit(user, unit.getType().getId(), finalCount);
		MissionInformation missionInformation = new MissionInformation();
		missionInformation.setRelation(relation);
		missionInformation.setValue(planetId.doubleValue());

		Mission mission = new Mission();
		mission.setMissionInformation(missionInformation);
		attachRequirementstoMission(mission, resourceRequirements);
		mission.setType(findMissionType(MissionType.BUILD_UNIT));
		mission.setUser(user);
		missionInformation.setMission(mission);

		substractResources(user, mission);

		userStorageBo.save(user);
		missionRepository.save(mission);

		ObtainedUnit obtainedUnit = new ObtainedUnit();
		obtainedUnit.setMission(mission);
		obtainedUnit.setCount(finalCount);
		obtainedUnit.setUnit(unit);
		obtainedUnit.setUser(user);
		obtainedUnitBo.save(obtainedUnit);

		scheduleMission(mission);

		return new RunningUnitBuildDto(unit, mission, finalCount);
	}

	public RunningUpgradeDto findRunningLevelUpMission(Integer userId) {
		Mission mission = findByUserIdAndTypeCode(userId, MissionType.LEVEL_UP);
		if (mission != null) {
			MissionInformation missionInformation = mission.getMissionInformation();
			Upgrade upgrade = objectRelationBo.unboxObjectRelation(missionInformation.getRelation());
			return new RunningUpgradeDto(upgrade, mission);
		} else {
			return null;
		}
	}

	public RunningUnitBuildDto findRunningUnitBuild(Integer userId, Double planetId) {
		Mission mission = findByUserIdAndTypeCodeAndMissionInformationValue(userId, MissionType.BUILD_UNIT, planetId);
		if (mission != null) {
			MissionInformation missionInformation = mission.getMissionInformation();
			Unit unit = objectRelationBo.unboxObjectRelation(missionInformation.getRelation());
			return new RunningUnitBuildDto(unit, mission,
					obtainedUnitBo.findByMissionId(mission.getId()).get(0).getCount());
		} else {
			return null;
		}
	}

	public MissionIdAndTerminationDateProjection findOneByReportId(Long reportId) {
		return missionRepository.findOneByReportId(reportId);
	}

	public boolean existsByTargetPlanet(Long planetId) {
		return missionRepository.countByTargetPlanetIdAndResolvedFalse(planetId) > 0;
	}

	public boolean existsByTargetPlanetAndType(Long planetId, MissionType type) {
		return missionRepository.countByTargetPlanetIdAndTypeCodeAndResolvedFalse(planetId, type.name()) > 0;
	}

	/**
	 * Should be invoked from the context
	 *
	 * @param missionId
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void cancelMission(Long missionId) {
		cancelMission(findById(missionId));
	}

	/**
	 * Should be invoked from the context
	 *
	 * @param mission
	 * @author Kevin Guanche Darias
	 */
	@Transactional
	public void cancelMission(Mission mission) {
		if (mission == null) {
			throw new MissionNotFoundException("The mission was not found, or was not passed to cancelMission()");
		}
		UserStorage missionUser = userStorageBo.findOneByMission(mission);
		UserStorage loggedInUser = userStorageBo.findLoggedIn();
		MissionType type = MissionType.valueOf(mission.getType().getCode());
		if (missionUser == null) {
			if (type == MissionType.BROADCAST_MESSAGE) {
				throw new SgtBackendNotImplementedException("This feature has not been implemented");
			} else {
				throw new CommonException("No such mission type " + mission.getType().getCode());
			}
		} else if (missionUser.getId().equals(loggedInUser.getId())) {
			switch (type) {
			case BUILD_UNIT:
				obtainedUnitBo.deleteByMissionId(mission.getId());
				missionUser.addtoPrimary(mission.getPrimaryResource());
				missionUser.addToSecondary(mission.getSecondaryResource());
				userStorageBo.save(missionUser);
				break;
			case LEVEL_UP:
				missionUser.addtoPrimary(mission.getPrimaryResource());
				missionUser.addToSecondary(mission.getSecondaryResource());
				userStorageBo.save(missionUser);
				break;
			default:
				throw new CommonException("No such mission type " + mission.getType().getCode());
			}
		} else {
			throw new CommonException(
					"unexpected executed condition!, maybe some dirty Kenpachi tried to cancel mission of other player!");
		}
		delete(mission);
		abortMissionJob(mission);
	}

	@Transactional
	public void cancelUpgradeMission(Integer userId) {
		cancelMission(findByUserIdAndTypeCode(userId, MissionType.LEVEL_UP));
	}

	@Transactional
	public void processBuildUnit(Long missionId) {
		Mission mission = findById(missionId);
		final List<Boolean> shouldClearImprovementsCache = new ArrayList<>(1);
		shouldClearImprovementsCache.add(false);
		if (mission != null) {
			Long sourcePlanetId = mission.getMissionInformation().getValue().longValue();
			obtainedUnitBo.findByMissionId(missionId).forEach(current -> {
				if (current.getUnit().getImprovement() != null) {
					shouldClearImprovementsCache.remove(0);
					shouldClearImprovementsCache.add(true);
				}
				current.setSourcePlanet(planetBo.findById(sourcePlanetId));
				obtainedUnitBo.moveUnit(current, mission.getUser().getId(), sourcePlanetId);
				requirementBo.triggerUnitBuildCompleted(mission.getUser(), current.getUnit());
			});
			delete(mission);
			if (Boolean.TRUE.equals(shouldClearImprovementsCache.get(0))) {
				improvementBo.clearSourceCache(mission.getUser(), obtainedUnitBo);
			}
		} else {
			LOG.debug(MISSION_NOT_FOUND);
		}
	}

	@Transactional
	public void cancelBuildUnit(Long missionId) {
		cancelMission(missionId);
	}

	/**
	 * Returns all running missions by the logged in user
	 *
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public List<UnitRunningMissionDto> myFindUserRunningMissions() {
		return findUserRunningMissions(userStorageBo.findLoggedIn().getId());
	}

	@Transactional
	public List<UnitRunningMissionDto> myFindEnemyRunningMissions() {
		return findEnemyRunningMissions(userStorageBo.findLoggedIn());
	}

	/**
	 * Finds a mission by user id, mission type, and value inside MissionInformation
	 *
	 * @param userId
	 * @param type
	 * @param value
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Mission findByUserIdAndTypeCodeAndMissionInformationValue(Integer userId, MissionType type, Double value) {
		return missionRepository.findByUserIdAndTypeCodeAndMissionInformationValue(userId, type.name(), value);
	}

	/**
	 * Checks that there is not another upgrade mission running, if it's doing, will
	 * throw an exception
	 *
	 * @param userId
	 * @param type
	 * @throws SgtLevelUpMissionAlreadyRunningException
	 * @author Kevin Guanche Darias
	 */
	private void checkUpgradeMissionDoesNotExists(Integer userId) {
		if (findByUserIdAndTypeCode(userId, MissionType.LEVEL_UP) != null) {
			throw new SgtLevelUpMissionAlreadyRunningException("There is already an upgrade going");
		}
	}

	/**
	 * Checks that there is not another unit recluit mission running in <b>target
	 * planet</b>
	 *
	 * @param userId
	 * @param planetId
	 * @throws SgtBackendUnitBuildAlreadyRunningException
	 * @author Kevin Guanche Darias
	 */
	private void checkUnitBuildMissionDoesNotExists(Integer userId, Long planetId) {
		if (findRunningUnitBuild(userId, (double) planetId) != null) {
			throw new SgtBackendUnitBuildAlreadyRunningException("Ya hay una unidad reclutándose en este planeta");
		}
	}

	/**
	 * Checks that the selected obtained upgrade is available else, throws an
	 * exception
	 *
	 * @param obtainedUpgrade
	 * @throws SgtMissionRegistrationException target upgrade is not available
	 * @author Kevin Guanche Darias
	 */
	private void checkUpgradeIsAvailable(ObtainedUpgrade obtainedUpgrade) {
		if (!obtainedUpgrade.isAvailable()) {
			throw new SgtMissionRegistrationException(
					"Can't register mission, of type LEVEL_UP, when upgrade is not available!");
		}
	}

	/**
	 * Checks if relation is unlocked
	 *
	 * @param userId
	 * @param relation
	 * @author Kevin Guanche Darias
	 */
	private void checkUnlockedUnit(Integer userId, ObjectRelation relation) {
		objectRelationBo.checkIsUnlocked(userId, relation.getId());
	}

	/**
	 * Copies resource requirements object to mission and fills the ime and date
	 *
	 * @param mission
	 * @param requirements
	 * @author Kevin Guanche Darias
	 */
	private void attachRequirementstoMission(Mission mission, ResourceRequirementsPojo requirements) {
		mission.setPrimaryResource(requirements.getRequiredPrimary());
		mission.setSecondaryResource(requirements.getRequiredSecondary());
		mission.setRequiredTime(requirements.getRequiredTime());
		mission.setTerminationDate(computeTerminationDate(requirements.getRequiredTime()));
	}

	/**
	 * Substracts the resources of the mission to the logged in user
	 *
	 * @param user
	 * @param mission
	 * @author Kevin Guanche Darias
	 */
	private void substractResources(UserStorage user, Mission mission) {
		user.setPrimaryResource(user.getPrimaryResource() - mission.getPrimaryResource());
		user.setSecondaryResource(user.getSecondaryResource() - mission.getSecondaryResource());
	}
}
