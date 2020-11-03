package com.kevinguanchedarias.owgejava.business;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.EntityManager;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.entity.AttackRuleEntry;
import com.kevinguanchedarias.owgejava.entity.EntityWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.Galaxy;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObjectRelation;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.SpeedImpactGroup;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.listener.ImageStoreListener;
import com.kevinguanchedarias.owgejava.enumerations.AttackableTargetEnum;
import com.kevinguanchedarias.owgejava.enumerations.DeployMissionConfigurationEnum;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtCorruptDatabaseException;
import com.kevinguanchedarias.owgejava.exception.UserNotFoundException;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;

@Service
public class UnitMissionBo extends AbstractMissionBo {
	private static final String ENEMY_MISSION_CHANGE = "enemy_mission_change";

	private static final long serialVersionUID = 344402831344882216L;

	private static final Logger LOG = Logger.getLogger(UnitMissionBo.class);
	private static final String JOB_GROUP_NAME = "UnitMissions";
	private static final String MAX_PLANETS_MESSAGE = "I18N_MAX_PLANETS_EXCEEDED";

	@Autowired
	private ImageStoreBo imageStoreBo;

	/**
	 * Represents an ObtainedUnit, its full attack, and the pending attack is has
	 *
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public class AttackObtainedUnit {
		AttackUserInformation user;
		Double pendingAttack;
		boolean noAttack = false;
		Double availableShield;
		Double availableHealth;
		Long finalCount;
		ObtainedUnit obtainedUnit;

		private Double totalAttack;
		private Long initialCount;
		private Double totalShield;
		private Double totalHealth;

		private Double initialHealth;

		public AttackObtainedUnit() {
			throw new ProgrammingException("Can't use AttackObtainedUnit");
		}

		public AttackObtainedUnit(ObtainedUnit obtainedUnit, GroupedImprovement userImprovement) {
			Unit unit = obtainedUnit.getUnit();
			UnitType unitType = unit.getType();
			initialCount = obtainedUnit.getCount();
			finalCount = initialCount;
			totalAttack = initialCount.doubleValue() * unit.getAttack();
			totalAttack += (totalAttack * improvementBo.findAsRational(
					(double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.ATTACK, unitType)));
			pendingAttack = totalAttack;
			totalShield = initialCount.doubleValue() * ObjectUtils.firstNonNull(unit.getShield(), 0);
			totalShield += (totalShield * improvementBo.findAsRational(
					(double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.SHIELD, unitType)));
			availableShield = totalShield;
			totalHealth = initialCount.doubleValue() * unit.getHealth();
			initialHealth = totalHealth;
			totalHealth += (totalHealth * improvementBo.findAsRational(
					(double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.DEFENSE, unitType)));
			availableHealth = totalHealth;
			this.obtainedUnit = obtainedUnit;
		}

		public Double getTotalAttack() {
			return totalAttack;
		}

		public void setTotalAttack(Double totalAttack) {
			this.totalAttack = totalAttack;
		}

		public Long getInitialCount() {
			return initialCount;
		}

		public void setInitialCount(Long initialCount) {
			this.initialCount = initialCount;
		}

		public Double getTotalShield() {
			return totalShield;
		}

		public void setTotalShield(Double totalShield) {
			this.totalShield = totalShield;
		}

		public Double getTotalHealth() {
			return totalHealth;
		}

		public void setTotalHealth(Double totalHealth) {
			this.totalHealth = totalHealth;
		}

		public Long getFinalCount() {
			return finalCount;
		}

		public ObtainedUnit getObtainedUnit() {
			return obtainedUnit;
		}

		public Double getInitialHealth() {
			return initialHealth;
		}

		public void setInitialHealth(Double initialHealth) {
			this.initialHealth = initialHealth;
		}

	}

	public class AttackUserInformation {
		AttackInformation attackInformationRef;
		Double earnedPoints = 0D;
		List<AttackObtainedUnit> units = new ArrayList<>();
		List<AttackObtainedUnit> attackableUnits;
		boolean isDefeated = false;
		boolean canAttack = true;

		private UserStorage user;
		private GroupedImprovement userImprovement;

		public AttackUserInformation(UserStorage user) {
			this.user = user;
			userImprovement = improvementBo.findUserImprovement(user);
		}

		public UserStorage getUser() {
			return user;
		}

		public Double getEarnedPoints() {
			return earnedPoints;
		}

		public GroupedImprovement getUserImprovement() {
			return userImprovement;
		}

		/**
		 * @return the units
		 * @since 0.9.0
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		public List<AttackObtainedUnit> getUnits() {
			return units;
		}

	}

	public class AttackInformation {
		private Mission attackMission;
		private boolean isRemoved = false;
		private Map<Integer, AttackUserInformation> users = new HashMap<>();
		private List<AttackObtainedUnit> units = new ArrayList<>();
		private Set<Integer> usersWithDeletedMissions = new HashSet<>();
		private Set<Integer> usersWithChangedCounts = new HashSet<>();
		private Planet targetPlanet;

		public AttackInformation() {
			throw new ProgrammingException(
					"Can't invoke constructor for " + this.getClass().getName() + " without arguments");
		}

		public AttackInformation(Mission attackMission, Planet targetPlanet) {
			this.attackMission = attackMission;
			this.targetPlanet = targetPlanet;
		}

		/**
		 * To have the expected behavior should be invoked after <i>startAttack()</i>
		 *
		 * @return true if the mission has been removed from the database
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		public boolean isMissionRemoved() {
			return isRemoved;
		}

		public void addUnit(ObtainedUnit unitEntity) {
			UserStorage userEntity = unitEntity.getUser();
			AttackUserInformation user;
			if (users.containsKey(userEntity.getId())) {
				user = users.get(userEntity.getId());
			} else {
				user = new AttackUserInformation(userEntity);
				users.put(userEntity.getId(), user);
			}
			AttackObtainedUnit unit = new AttackObtainedUnit(unitEntity, user.userImprovement);
			unit.user = user;
			user.units.add(unit);
			units.add(unit);
		}

		public void startAttack() {
			Collections.shuffle(units);
			users.forEach((userId, user) -> user.attackableUnits = units.stream().filter(
					unit -> !unit.user.user.getId().equals(user.user.getId()) && filterAlliance(user, unit.user))
					.collect(Collectors.toList()));
			doAttack();
			updatePoints();
			usersWithDeletedMissions.forEach(userId -> {
				emitMissions(userId);
				userStorageBo.emitUserData(userStorageBo.findById(userId));
				usersWithChangedCounts.remove(userId);
			});
			usersWithChangedCounts.forEach(userId -> {
				if (targetPlanet.getOwner() != null && targetPlanet.getOwner().getId().equals(userId)) {
					obtainedUnitBo.emitObtainedUnitChange(userId);
				}
				emitMissions(userId);
				userStorageBo.emitUserData(userStorageBo.findById(userId));
			});
		}

		/**
		 * @return the users
		 * @since 0.9.0
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		public Map<Integer, AttackUserInformation> getUsers() {
			return users;
		}

		public void setRemoved(boolean isRemoved) {
			this.isRemoved = isRemoved;
		}

		private void doAttack() {
			units.forEach(unit -> {
				List<AttackObtainedUnit> attackableByUnit = unit.user.attackableUnits.stream().filter(target -> {
					Unit unitEntity = unit.obtainedUnit.getUnit();
					AttackRule attackRule = ObjectUtils.firstNonNull(unitEntity.getAttackRule(),
							findAttackRule(unitEntity.getType()));
					return canAttack(attackRule, target);
				}).collect(Collectors.toList());
				for (AttackObtainedUnit target : attackableByUnit) {
					attackTarget(unit, target);
					if (unit.noAttack) {
						break;
					}
				}
			});
		}

		private boolean canAttack(AttackRule attackRule, AttackObtainedUnit target) {
			if (attackRule == null || attackRule.getAttackRuleEntries() == null) {
				return true;
			} else {
				for (AttackRuleEntry ruleEntry : attackRule.getAttackRuleEntries()) {
					if (ruleEntry.getTarget() == AttackableTargetEnum.UNIT) {
						if (target.obtainedUnit.getUnit().getId().equals(ruleEntry.getReferenceId())) {
							return ruleEntry.getCanAttack();
						}
					} else if (ruleEntry.getTarget() == AttackableTargetEnum.UNIT_TYPE) {
						UnitType unitType = findUnitTypeMatchingRule(ruleEntry,
								target.obtainedUnit.getUnit().getType());
						if (unitType != null) {
							return ruleEntry.getCanAttack();
						}
					} else {
						throw new ProgrammingException("unexpected code path");
					}
				}
				return true;
			}
		}

		private UnitType findUnitTypeMatchingRule(AttackRuleEntry ruleEntry, UnitType unitType) {
			if (ruleEntry.getReferenceId().equals(unitType.getId())) {
				return unitType;
			} else if (unitType.getParent() != null) {
				return findUnitTypeMatchingRule(ruleEntry, unitType.getParent());
			} else {
				return null;
			}
		}

		/**
		 * Discovers the attack rule, looking using recursion
		 *
		 * @param type
		 * @return
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		private AttackRule findAttackRule(UnitType type) {
			if (type.getAttackRule() != null) {
				return type.getAttackRule();
			} else if (type.getParent() != null) {
				return findAttackRule(type.getParent());
			} else {
				return null;
			}
		}

		private void attackTarget(AttackObtainedUnit source, AttackObtainedUnit target) {
			Double myAttack = source.pendingAttack;
			Double victimHealth = target.availableHealth + target.availableShield;
			addPointsAndUpdateCount(myAttack, source, target);
			if (victimHealth > myAttack) {
				source.pendingAttack = 0D;
				source.noAttack = true;
				double attackDistribruted = myAttack / 2;
				target.availableShield -= attackDistribruted;
				target.availableHealth -= attackDistribruted;
				if (target.availableShield < 0.0D) {
					target.availableHealth += target.availableShield;
				}
				if (!target.initialCount.equals(target.finalCount)) {
					usersWithChangedCounts.add(target.user.getUser().getId());
				}
			} else {
				source.pendingAttack = myAttack - victimHealth;
				target.availableHealth = 0D;
				target.availableShield = 0D;
				obtainedUnitBo.delete(target.obtainedUnit);
				deleteMissionIfRequired(target.obtainedUnit);
				usersWithChangedCounts.add(target.user.getUser().getId());
			}
			improvementBo.clearCacheEntriesIfRequired(target.obtainedUnit.getUnit(), obtainedUnitBo);

		}

		/**
		 * Deletes the mission from the system, when all units involved are death
		 *
		 * Notice, should be invoked after <b>removing the obtained unit</b>
		 *
		 * @param obtainedUnit
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		private void deleteMissionIfRequired(ObtainedUnit obtainedUnit) {
			Mission mission = obtainedUnit.getMission();
			if (mission != null && !obtainedUnitBo.existsByMission(mission)) {
				if (attackMission.getId().equals(mission.getId())) {
					setRemoved(true);
				} else {
					delete(mission);
					usersWithDeletedMissions.add(mission.getUser().getId());
				}
			}
		}

		private void addPointsAndUpdateCount(double usedAttack, AttackObtainedUnit source,
				AttackObtainedUnit victimUnit) {
			Double healthForEachUnit = (victimUnit.totalHealth + victimUnit.totalShield) / victimUnit.initialCount;
			Long killedCount = (long) Math.floor(usedAttack / healthForEachUnit);
			if (killedCount > victimUnit.finalCount) {
				killedCount = victimUnit.finalCount;
				victimUnit.finalCount = 0L;
			} else {
				victimUnit.finalCount -= killedCount;
			}
			source.user.earnedPoints += killedCount * victimUnit.obtainedUnit.getUnit().getPoints();
		}

		private void updatePoints() {
			Set<Integer> alteredUsers = new HashSet<>();
			users.entrySet().forEach(current -> {
				AttackUserInformation attackUserInformation = current.getValue();
				List<AttackObtainedUnit> userUnits = attackUserInformation.units;
				userStorageBo.addPointsToUser(attackUserInformation.getUser(), attackUserInformation.earnedPoints);
				obtainedUnitBo
						.save(userUnits.stream()
								.filter(currentUnit -> !currentUnit.finalCount.equals(0L)
										&& !currentUnit.initialCount.equals(currentUnit.finalCount))
								.map(currentUnit -> {
									currentUnit.obtainedUnit.setCount(currentUnit.finalCount);
									alteredUsers.add(attackUserInformation.getUser().getId());
									return currentUnit.obtainedUnit;
								}).collect(Collectors.toList()));
			});
			usersWithChangedCounts.forEach(alteredUsers::add);
			TransactionUtil.doAfterCommit(() -> alteredUsers.forEach(current -> {
				unitTypeBo.emitUserChange(current);
				socketIoService.sendMessage(current, UNIT_OBTAINED_CHANGE,
						() -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(current)));

			}));
		}

		/**
		 * If the user has an alliance, removes all those users that are not in the user
		 * alliance
		 *
		 * @param current
		 * @return
		 * @since 0.7.0
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		private boolean filterAlliance(AttackUserInformation source, AttackUserInformation target) {
			return source.user.getAlliance() == null || target.user.getAlliance() == null
					|| !source.user.getAlliance().getId().equals(target.user.getAlliance().getId());
		}
	}

	@Autowired
	private ConfigurationBo configurationBo;

	@Autowired
	private transient SocketIoService socketIoService;

	@Autowired
	private transient EntityManager entityManager;

	@Autowired
	private PlanetListBo planetListBo;

	@Override
	public String getGroupName() {
		return JOB_GROUP_NAME;
	}

	@Override
	public Logger getLogger() {
		return LOG;
	}

	/**
	 * Registers a explore mission <b>as logged in user</b>
	 *
	 * @param missionInformation <i>userId</i> is <b>ignored</b> in this method
	 *                           <b>immutable object</b>
	 * @return mission representation DTO
	 * @throws SgtBackendInvalidInputException When input information is not valid
	 * @throws UserNotFoundException           When user doesn't exists <b>(in this
	 *                                         universe)</b>
	 * @throws PlanetNotFoundException         When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void myRegisterExploreMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		adminRegisterExploreMission(missionInformation);
	}

	/**
	 * Registers a explore mission <b>as a admin</b>
	 *
	 * @param missionInformation
	 * @throws SgtBackendInvalidInputException When input information is not valid
	 * @throws UserNotFoundException           When user doesn't exists <b>(in this
	 *                                         universe)</b>
	 * @throws PlanetNotFoundException         When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void adminRegisterExploreMission(UnitMissionInformation missionInformation) {
		commonMissionRegister(missionInformation, MissionType.EXPLORE);
	}

	@Transactional
	public void myRegisterGatherMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		adminRegisterGatherMission(missionInformation);
	}

	@Transactional
	public void adminRegisterGatherMission(UnitMissionInformation missionInformation) {
		commonMissionRegister(missionInformation, MissionType.GATHER);
	}

	@Transactional
	public void myRegisterEstablishBaseMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		adminRegisterEstablishBase(missionInformation);
	}

	@Transactional
	public void adminRegisterEstablishBase(UnitMissionInformation missionInformation) {
		commonMissionRegister(missionInformation, MissionType.ESTABLISH_BASE);
	}

	@Transactional
	public void myRegisterAttackMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		adminRegisterAttackMission(missionInformation);
	}

	@Transactional
	public void adminRegisterAttackMission(UnitMissionInformation missionInformation) {
		commonMissionRegister(missionInformation, MissionType.ATTACK);
	}

	@Transactional
	public void myRegisterCounterattackMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		adminRegisterCounterattackMission(missionInformation);
	}

	@Transactional
	public void adminRegisterCounterattackMission(UnitMissionInformation missionInformation) {
		if (!planetBo.isOfUserProperty(missionInformation.getUserId(), missionInformation.getTargetPlanetId())) {
			throw new SgtBackendInvalidInputException(
					"TargetPlanet doesn't belong to sender user, try again dear Hacker, maybe next time you have some luck");
		}
		commonMissionRegister(missionInformation, MissionType.COUNTERATTACK);
	}

	@Transactional
	public void myRegisterConquestMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		adminRegisterConquestMission(missionInformation);
	}

	@Transactional
	public void adminRegisterConquestMission(UnitMissionInformation missionInformation) {
		if (planetBo.myIsOfUserProperty(missionInformation.getTargetPlanetId())) {
			throw new SgtBackendInvalidInputException(
					"Doesn't make sense to conquest your own planet... unless your population hates you, and are going to organize a rebelion");
		}
		if (planetBo.isHomePlanet(missionInformation.getTargetPlanetId())) {
			throw new SgtBackendInvalidInputException(
					"Can't steal a home planet to a user, would you like a bandit to steal in your own home??!");
		}
		if (planetBo.hasMaxPlanets(missionInformation.getUserId())) {
			throw new SgtBackendInvalidInputException(MAX_PLANETS_MESSAGE);
		}
		commonMissionRegister(missionInformation, MissionType.CONQUEST);
	}

	@Transactional
	public void myRegisterDeploy(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		adminRegisterDeploy(missionInformation);
	}

	@Transactional
	public void adminRegisterDeploy(UnitMissionInformation missionInformation) {
		if (missionInformation.getSourcePlanetId().equals(missionInformation.getTargetPlanetId())) {
			throw exceptionUtilService
					.createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_DEPLOY_ITSELF")
					.withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
		}
		commonMissionRegister(missionInformation, MissionType.DEPLOY);
	}

	/**
	 * Parses the exploration of a planet
	 *
	 * @param missionId
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void processExplore(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = findUnitsInvolved(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		if (!planetBo.isExplored(user, targetPlanet)) {
			planetBo.defineAsExplored(user, targetPlanet);
		}
		List<ObtainedUnit> unitsInPlanet = obtainedUnitBo.explorePlanetUnits(mission, targetPlanet);
		adminRegisterReturnMission(mission);
		UnitMissionReportBuilder builder = UnitMissionReportBuilder
				.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
				.withExploredInformation(unitsInPlanet);
		handleMissionReportSave(mission, builder);
		resolveMission(mission);
	}

	@Transactional
	public void processGather(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = findUnitsInvolved(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		boolean continueMission = triggerAttackIfRequired(mission, user, targetPlanet);
		if (continueMission) {
			adminRegisterReturnMission(mission);
			Long gathered = involvedUnits.stream()
					.map(current -> ObjectUtils.firstNonNull(current.getUnit().getCharge(), 0) * current.getCount())
					.reduce(0L, (sum, current) -> sum + current);
			Double withPlanetRichness = gathered * targetPlanet.findRationalRichness();
			GroupedImprovement groupedImprovement = improvementBo.findUserImprovement(user);
			Double withUserImprovement = withPlanetRichness
					+ (withPlanetRichness * improvementBo.findAsRational(groupedImprovement.getMoreChargeCapacity()));
			Double primaryResource = withUserImprovement * 0.5;
			Double secondaryResource = withUserImprovement * 0.5;
			user.addtoPrimary(primaryResource);
			user.addToSecondary(secondaryResource);
			UnitMissionReportBuilder builder = UnitMissionReportBuilder
					.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
					.withGatherInformation(primaryResource, secondaryResource);
			handleMissionReportSave(mission, builder);
			resolveMission(mission);

			TransactionUtil.doAfterCommit(() -> socketIoService.sendMessage(user, "mission_gather_result", () -> {
				Map<String, Double> content = new HashMap<>();
				content.put("primaryResource", primaryResource);
				content.put("secondaryResource", secondaryResource);
				return content;
			}));
		}
	}

	@Transactional
	public void processEstablishBase(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = findUnitsInvolved(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		if (triggerAttackIfRequired(mission, user, targetPlanet)) {
			UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(user, mission.getSourcePlanet(),
					targetPlanet, involvedUnits);
			UserStorage planetOwner = targetPlanet.getOwner();
			boolean hasMaxPlanets = planetBo.hasMaxPlanets(user);
			if (planetOwner != null || hasMaxPlanets) {
				adminRegisterReturnMission(mission);
				if (planetOwner != null) {
					builder.withEstablishBaseInformation(false, "I18N_ALREADY_HAS_OWNER");
				} else {
					builder.withEstablishBaseInformation(false, MAX_PLANETS_MESSAGE);
				}
			} else {
				builder.withEstablishBaseInformation(true);
				definePlanetAsOwnedBy(user, involvedUnits, targetPlanet);
			}
			handleMissionReportSave(mission, builder);
			resolveMission(mission);
			emitLocalMissionChangeAfterCommit(mission);
		}
	}

	@Transactional
	public AttackInformation processAttack(Long missionId, boolean survivorsDoReturn) {
		Mission mission = findById(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		AttackInformation attackInformation = buildAttackInformation(targetPlanet, mission);
		attackInformation.startAttack();
		if (survivorsDoReturn && !attackInformation.isMissionRemoved()) {
			adminRegisterReturnMission(mission);
		}
		resolveMission(mission);
		UnitMissionReportBuilder builder = UnitMissionReportBuilder
				.create(mission.getUser(), mission.getSourcePlanet(), targetPlanet, new ArrayList<>())
				.withAttackInformation(attackInformation);
		UserStorage invoker = mission.getUser();
		handleMissionReportSave(mission, builder, true,
				attackInformation.users.entrySet().stream().map(current -> current.getValue().user)
						.filter(user -> !user.getId().equals(invoker.getId())).collect(Collectors.toList()));
		handleMissionReportSave(mission, builder, false, invoker);
		if (attackInformation.isMissionRemoved()) {
			emitLocalMissionChangeAfterCommit(mission);
		}
		UserStorage owner = targetPlanet.getOwner();
		if (owner != null && !attackInformation.usersWithDeletedMissions.isEmpty()) {
			emitEnemyMissionsChange(owner);
		}

		return attackInformation;
	}

	/**
	 * Executes the counterattack logic <br>
	 * <b>NOTICE: </b> For now the current implementation just calls the
	 * processAttack()
	 *
	 * @param missionId
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void processCounterattack(Long missionId) {
		processAttack(missionId, true);
	}

	/**
	 * Creates a return mission from an existing mission
	 *
	 * @param mission Existing mission that will be returned
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void adminRegisterReturnMission(Mission mission) {
		adminRegisterReturnMission(mission, null);
	}

	/**
	 * Creates a return mission from an existing mission
	 *
	 * @param mission            Existing mission that will be returned
	 * @param customRequiredTime If not null will be used as the time for the return
	 *                           mission, else will use source mission time
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void adminRegisterReturnMission(Mission mission, Double customRequiredTime) {
		Mission returnMission = new Mission();
		returnMission.setStartingDate(new Date());
		returnMission.setType(findMissionType(MissionType.RETURN_MISSION));
		returnMission.setRequiredTime(mission.getRequiredTime());
		Double requiredTime = customRequiredTime == null ? mission.getRequiredTime() : customRequiredTime;
		returnMission.setTerminationDate(computeTerminationDate(requiredTime));
		returnMission.setSourcePlanet(mission.getSourcePlanet());
		returnMission.setTargetPlanet(mission.getTargetPlanet());
		returnMission.setUser(mission.getUser());
		returnMission.setRelatedMission(mission);
		List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findLockedByMissionId(mission.getId());
		missionRepository.saveAndFlush(returnMission);
		obtainedUnits.forEach(current -> current.setMission(returnMission));
		obtainedUnitBo.save(obtainedUnits);
		scheduleMission(returnMission);
		emitLocalMissionChangeAfterCommit(returnMission);
	}

	@Transactional
	public void proccessReturnMission(Long missionId) {
		Mission mission = missionRepository.findById(missionId).get();
		Integer userId = mission.getUser().getId();
		List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findLockedByMissionId(mission.getId());
		obtainedUnits.forEach(current -> obtainedUnitBo.moveUnit(current, userId, mission.getSourcePlanet().getId()));
		resolveMission(mission);
		emitLocalMissionChangeAfterCommit(mission);
		TransactionUtil.doAfterCommit(() -> socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
				() -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(userId))));
	}

	@Transactional
	public void processConquest(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = findUnitsInvolved(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(user, mission.getSourcePlanet(),
				targetPlanet, involvedUnits);
		boolean maxPlanets = planetBo.hasMaxPlanets(user);
		boolean areUnitsHavingToReturn = false;
		AttackInformation attackInformation = processAttack(missionId, false);
		UserStorage oldOwner = targetPlanet.getOwner();
		boolean isOldOwnerDefeated = attackInformation.getUsers().containsKey(oldOwner.getId())
				? attackInformation.getUsers().get(oldOwner.getId()).units.stream()
						.noneMatch(current -> current.finalCount > 0L)
				: true;
		boolean isAllianceDefeated = isOldOwnerDefeated
				&& (oldOwner.getAlliance() == null || attackInformation.getUsers().entrySet().stream()
						.filter(attackedUser -> attackedUser.getValue().getUser().getAlliance() != null
								&& attackedUser.getValue().getUser().getAlliance().equals(oldOwner.getAlliance()))
						.allMatch(currentUser -> currentUser.getValue().units.stream()
								.noneMatch(currentUserUnit -> currentUserUnit.finalCount > 0L)));
		if (!isOldOwnerDefeated || !isAllianceDefeated || maxPlanets || planetBo.isHomePlanet(targetPlanet)) {
			adminRegisterReturnMission(mission);
			areUnitsHavingToReturn = true;
			if (maxPlanets) {
				builder.withConquestInformation(false, MAX_PLANETS_MESSAGE);
			} else if (!isOldOwnerDefeated) {
				builder.withConquestInformation(false, "I18N_OWNER_NOT_DEFEATED");
			} else if (!isAllianceDefeated) {
				builder.withConquestInformation(false, "I18N_ALLIANCE_NOT_DEFEATED");
			} else {
				builder.withConquestInformation(false, "I18N_CANT_CONQUER_HOME_PLANET");
			}
		} else {
			definePlanetAsOwnedBy(user, involvedUnits, targetPlanet);
			builder.withConquestInformation(true, "I18N_PLANET_IS_NOW_OURS");
			if (targetPlanet.getSpecialLocation() != null) {
				requirementBo.triggerSpecialLocation(oldOwner, targetPlanet.getSpecialLocation());
			}
			planetBo.emitPlanetOwnedChange(oldOwner);
			emitEnemyMissionsChange(oldOwner);
			UnitMissionReportBuilder enemyReportBuilder = UnitMissionReportBuilder
					.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
					.withConquestInformation(true, "I18N_YOUR_PLANET_WAS_CONQUISTED");
			handleMissionReportSave(mission, enemyReportBuilder, true, oldOwner);

		}
		handleMissionReportSave(mission, builder);
		resolveMission(mission);
		if (!areUnitsHavingToReturn) {
			emitLocalMissionChangeAfterCommit(mission);
		}
	}

	@Transactional
	public void proccessDeploy(Long missionId) {
		Mission mission = findById(missionId);
		if (mission != null) {
			UserStorage user = mission.getUser();
			Integer userId = user.getId();
			List<ObtainedUnit> alteredUnits = new ArrayList<>();
			findUnitsInvolved(missionId).forEach(current -> alteredUnits
					.add(obtainedUnitBo.moveUnit(current, userId, mission.getTargetPlanet().getId())));
			resolveMission(mission);
			TransactionUtil.doAfterCommit(() -> {
				alteredUnits.forEach(unit -> {
					entityManager.refresh(unit);
					if (unit.getMission() != null && unit.getMission().getId() > missionId) {
						entityManager.refresh(unit.getMission());
					}
				});
				socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
						() -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(userId)));
				emitEnemyMissionsChange(mission);
				socketIoService.sendMessage(user, "unit_mission_change", () -> {
					List<UnitRunningMissionDto> missionsWorkarounds = findUserRunningMissions(user.getId());
					Optional<UnitRunningMissionDto> launchedMission = missionsWorkarounds.stream()
							.filter(current -> current.getMissionId().equals(missionId)).findFirst();
					if (launchedMission.isPresent()) {
						launchedMission.get().setInvolvedUnits(obtainedUnitBo.toDto(alteredUnits));
					}
					return new MissionWebsocketMessage(countUserMissions(user.getId()), missionsWorkarounds);
				});

			});
		}
	}

	@Transactional
	public void myCancelMission(Long missionId) {
		Mission mission = findById(missionId);
		if (mission == null) {
			throw new NotFoundException("No mission with id " + missionId + " was found");
		} else if (!mission.getUser().getId().equals(userStorageBo.findLoggedIn().getId())) {
			throw new SgtBackendInvalidInputException("You can't cancel other player missions");
		} else if (isOfType(mission, MissionType.RETURN_MISSION)) {
			throw new SgtBackendInvalidInputException("can't cancel return missions");
		} else {
			mission.setResolved(true);
			save(mission);
			long nowMillis = new Instant().getMillis();
			long terminationMillis = mission.getTerminationDate().getTime();
			long durationMillis = 0L;
			if (terminationMillis >= nowMillis) {
				Interval interval = new Interval(nowMillis, terminationMillis);
				durationMillis = (long) (interval.toDurationMillis() / 1000D);
			}
			adminRegisterReturnMission(mission, mission.getRequiredTime() - durationMillis);
		}
	}

	public List<ObtainedUnit> findInvolvedInMission(Mission mission) {
		return obtainedUnitBo.findLockedByMissionId(mission.getId());
	}

	/**
	 * finds user <b>not resolved</b> deployed mission, if none exists creates one
	 * <br>
	 * <b>IMPORTANT:</b> Will save the unit, because if the mission exists, has to
	 * remove the firstDeploymentMission
	 *
	 * @param origin
	 * @param unit
	 * @return
	 * @since 0.7.4
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public Mission findDeployedMissionOrCreate(ObtainedUnit unit) {
		UserStorage user = unit.getUser();
		Planet origin = unit.getSourcePlanet();
		Planet target = unit.getTargetPlanet();
		Mission existingMission = findOneByUserIdAndTypeAndTargetPlanet(user.getId(), MissionType.DEPLOYED,
				target.getId());
		if (existingMission != null) {
			unit.setFirstDeploymentMission(null);
			unit.setMission(existingMission);
			obtainedUnitBo.save(unit);
			return existingMission;
		} else {
			Mission deployedMission = new Mission();
			deployedMission.setType(findMissionType(MissionType.DEPLOYED));
			deployedMission.setUser(user);
			if (unit.getFirstDeploymentMission() == null) {
				deployedMission.setSourcePlanet(origin);
				deployedMission.setTargetPlanet(target);
				deployedMission = save(deployedMission);
				unit.setFirstDeploymentMission(deployedMission);
				obtainedUnitBo.save(unit);
			} else {
				Mission firstDeploymentMission = findById(unit.getFirstDeploymentMission().getId());
				deployedMission.setSourcePlanet(firstDeploymentMission.getSourcePlanet());
				deployedMission.setTargetPlanet(firstDeploymentMission.getTargetPlanet());
				deployedMission = save(deployedMission);
			}
			return deployedMission;
		}
	}

	/**
	 * Test if the given entity with mission limitations can do the mission
	 *
	 * @param user
	 * @param targetPlanet
	 * @param entityWithMissionLimitation
	 * @param missionType
	 * @return
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public boolean canDoMission(UserStorage user, Planet targetPlanet,
			EntityWithMissionLimitation<Integer> entityWithMissionLimitation, MissionType missionType) {
		String targetMethod = "getCan" + WordUtils.capitalizeFully(missionType.name(), '_').replaceAll("_", "");
		try {
			MissionSupportEnum missionSupport = ((MissionSupportEnum) entityWithMissionLimitation.getClass()
					.getMethod(targetMethod).invoke(entityWithMissionLimitation));
			switch (missionSupport) {
			case ANY:
				return true;
			case OWNED_ONLY:
				return planetBo.isOfUserProperty(user, targetPlanet);
			case NONE:
				return false;
			default:
				throw new SgtCorruptDatabaseException(
						"unsupported mission support was specified: " + missionSupport.name());
			}
		} catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException | NoSuchMethodException
				| SecurityException e) {
			throw new SgtBackendInvalidInputException(
					"Could not invoke method " + targetMethod + " maybe it is not supported mission", e);
		}
	}

	/**
	 *
	 * @param userId
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void emitMissions(Integer userId) {
		socketIoService.sendMessage(userId, "unit_mission_change",
				() -> new MissionWebsocketMessage(countUserMissions(userId), findUserRunningMissions(userId)));
	}

	/**
	 * Emits the specified mission to the <i>mission</i> target planet owner if any
	 * <br>
	 * As of 0.9.9 this method is now public
	 *
	 * @param mission
	 * @since 0.9.9
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public void emitEnemyMissionsChange(Mission mission) {
		UserStorage targetPlanetOwner = mission.getTargetPlanet().getOwner();
		if (targetPlanetOwner != null && !targetPlanetOwner.getId().equals(mission.getUser().getId())) {
			emitEnemyMissionsChange(targetPlanetOwner);
		}
	}

	/**
	 * Runs the mission
	 *
	 * @param missionId
	 * @param missionType
	 * @since 0.9.9
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void runUnitMission(Long missionId, MissionType missionType) {
		switch (missionType) {
		case EXPLORE:
			processExplore(missionId);
			break;
		case RETURN_MISSION:
			proccessReturnMission(missionId);
			break;
		case GATHER:
			processGather(missionId);
			break;
		case ESTABLISH_BASE:
			processEstablishBase(missionId);
			break;
		case ATTACK:
			processAttack(missionId, true);
			break;
		case COUNTERATTACK:
			processCounterattack(missionId);
			break;
		case CONQUEST:
			processConquest(missionId);
			break;
		case DEPLOY:
			proccessDeploy(missionId);
			break;
		default:
			LOG.warn("Not an unit mission");
		}

	}

	/**
	 * Due to lack of support from Quartz to access spring context from the
	 * EntityListener of {@link ImageStoreListener} we have to invoke the image URL
	 * computation from here
	 *
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 * @param missionId
	 * @return
	 */
	private List<ObtainedUnit> findUnitsInvolved(Long missionId) {
		List<ObtainedUnit> retVal = obtainedUnitBo.findLockedByMissionId(missionId);
		retVal.forEach(current -> imageStoreBo.computeImageUrl(current.getUnit().getImage()));
		return retVal;
	}

	/**
	 * Executes modifications to <i>missionInformation</i> to define the logged in
	 * user as the sender user
	 *
	 * @param missionInformation
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void myRegister(UnitMissionInformation missionInformation) {
		if (missionInformation.getUserId() == null) {
			missionInformation.setUserId(userStorageBo.findLoggedIn().getId());
		} else {
			checkInvokerIsTheLoggedUser(missionInformation.getUserId());
		}
	}

	private void commonMissionRegister(UnitMissionInformation missionInformation, MissionType missionType) {
		List<ObtainedUnit> obtainedUnits = new ArrayList<>();
		missionInformation.setMissionType(missionType);
		UserStorage user = userStorageBo.findLoggedIn();
		if (!missionType.equals(MissionType.DEPLOY)
				|| !planetBo.isOfUserProperty(user.getId(), missionInformation.getTargetPlanetId())) {
			checkMissionLimitNotReached(user);
		}
		UnitMissionInformation targetMissionInformation = copyMissionInformation(missionInformation);
		Integer userId = user.getId();
		targetMissionInformation.setUserId(userId);
		if (missionType != MissionType.EXPLORE
				&& !planetBo.isExplored(userId, missionInformation.getTargetPlanetId())) {
			throw new SgtBackendInvalidInputException(
					"Can't send this mission, because target planet is not explored ");
		}
		Map<Integer, ObtainedUnit> dbUnits = checkAndLoadObtainedUnits(missionInformation);
		Mission mission = missionRepository.saveAndFlush((prepareMission(targetMissionInformation, missionType)));
		targetMissionInformation.getInvolvedUnits().forEach(current -> {
			ObtainedUnit currentObtainedUnit = new ObtainedUnit();
			currentObtainedUnit.setMission(mission);
			Mission firstDeploymentMission = dbUnits.get(current.getId()).getFirstDeploymentMission();
			currentObtainedUnit.setFirstDeploymentMission(firstDeploymentMission);
			currentObtainedUnit.setCount(current.getCount());
			currentObtainedUnit.setUser(user);
			currentObtainedUnit.setUnit(unitBo.findById(current.getId()));
			currentObtainedUnit.setSourcePlanet(firstDeploymentMission == null ? mission.getSourcePlanet()
					: firstDeploymentMission.getSourcePlanet());
			currentObtainedUnit.setTargetPlanet(mission.getTargetPlanet());
			obtainedUnits.add(currentObtainedUnit);
		});
		List<UnitType> involvedUnitTypes = obtainedUnits.stream().map(current -> current.getUnit().getType())
				.collect(Collectors.toList());
		if (!unitTypeBo.canDoMission(user, mission.getTargetPlanet(), involvedUnitTypes, missionType)) {
			throw new SgtBackendInvalidInputException(
					"At least one unit type doesn't support the specified mission.... don't try it dear hacker, you can't defeat the system, but don't worry nobody can");
		}
		checkCrossGalaxy(missionType, obtainedUnits, mission.getSourcePlanet(), mission.getTargetPlanet());
		obtainedUnitBo.save(obtainedUnits);
		if (obtainedUnits.stream().noneMatch(obtainedUnit -> obtainedUnit.getUnit().getSpeedImpactGroup() != null
				&& obtainedUnit.getUnit().getSpeedImpactGroup().getIsFixed())) {
			Optional<Double> lowestSpeedOptional = obtainedUnits.stream().map(ObtainedUnit::getUnit)
					.filter(unit -> unit.getSpeed() != null && unit.getSpeed() > 0.000D
							&& (unit.getSpeedImpactGroup() == null || unit.getSpeedImpactGroup().getIsFixed() == false))
					.map(Unit::getSpeed).reduce((a, b) -> a > b ? b : a);
			if (lowestSpeedOptional.isPresent()) {
				double lowestSpeed = lowestSpeedOptional.get();
				double missionTypeTime = calculateRequiredTime(missionType);
				double requiredTime = calculateTimeUsingSpeed(mission, missionType, missionTypeTime, lowestSpeed);
				mission.setRequiredTime(requiredTime);
				mission.setTerminationDate(computeTerminationDate(mission.getRequiredTime()));
			}
		}
		save(mission);
		scheduleMission(mission);
		emitLocalMissionChangeAfterCommit(mission);
		TransactionUtil.doAfterCommit(() -> socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
				() -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(userId))));
	}

	private double calculateTimeUsingSpeed(Mission mission, MissionType missionType, double missionTypeTime,
			double lowestUnitSpeed) {
		int missionTypeDivisor = findMissionTypeDivisor(missionType);
		missionTypeDivisor = missionTypeDivisor == 0 ? 1 : missionTypeDivisor;
		int leftMultiplier = findSpeedLeftMultiplier(mission, missionType);
		float moveCost = calculateMoveCost(missionType, mission.getSourcePlanet(), mission.getTargetPlanet());
		return (missionTypeTime + ((leftMultiplier * moveCost) * (100 - lowestUnitSpeed) / missionTypeDivisor));
	}

	/**
	 * Finds the speed left multiplier <b>also known as the "mission penalty"</b>
	 * which depends of the mission type and if it's on different quadrant
	 *
	 * @param mission
	 * @param missionType
	 * @return
	 */
	private int findSpeedLeftMultiplier(Mission mission, MissionType missionType) {
		final String prefix = "MISSION_SPEED_";
		String missionTypeName = missionType.name();
		Long sourceQuadrant = mission.getSourcePlanet().getQuadrant();
		Long targetQuadrant = mission.getTargetPlanet().getQuadrant();
		Long sourceSector = mission.getSourcePlanet().getSector();
		Long targetSector = mission.getTargetPlanet().getSector();
		Galaxy sourceGalaxy = mission.getSourcePlanet().getGalaxy();
		Galaxy targetGalaxy = mission.getTargetPlanet().getGalaxy();
		Integer defaultMultiplier;
		String configurationName;
		if (sourceQuadrant.equals(targetQuadrant) && sourceSector.equals(targetSector)
				&& sourceGalaxy.equals(targetGalaxy)) {
			configurationName = prefix + missionTypeName + "_SAME_Q_PENALTY";
			defaultMultiplier = 50;
		} else if (!sourceGalaxy.equals(targetGalaxy)) {
			configurationName = prefix + missionTypeName + "_DIFF_G_PENALTY";
			defaultMultiplier = 2000;
		} else if (!sourceSector.equals(targetSector)) {
			configurationName = prefix + missionTypeName + "_DIFF_S_PENALTY";
			defaultMultiplier = 200;
		} else {
			configurationName = prefix + missionTypeName + "_DIFF_Q_PENALTY";
			defaultMultiplier = 100;
		}
		return NumberUtils.toInt(
				configurationBo.findOrSetDefault(configurationName, defaultMultiplier.toString()).getValue(),
				defaultMultiplier);
	}

	/**
	 * Will check if the input DTO is valid, the following validations will be done
	 * <br>
	 * <b>IMPORTANT:</b> This method is intended to be use as part of the mission
	 * registration process
	 * <ul>
	 * <li>Check if the user exists</li>
	 * <li>Check if the sourcePlanet exists</li>
	 * <li>Check if the targetPlanet exists</li>
	 * <li>Check for each selected unit if there is an associated obtainedUnit and
	 * if count is valid</li>
	 * <li>removes DEPLOYED mission if required</li>
	 * </ul>
	 *
	 * @param missionInformation
	 * @return Database list of <i>ObtainedUnit</i> with the subtraction <b>already
	 *         applied</b>, whose key is the "unit" id (don't confuse with obtained
	 *         unit id)
	 *
	 * @throws SgtBackendInvalidInputException when validation was not passed
	 * @throws UserNotFoundException           When user doesn't exists <b>(in this
	 *                                         universe)</b>
	 * @throws PlanetNotFoundException         When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Map<Integer, ObtainedUnit> checkAndLoadObtainedUnits(UnitMissionInformation missionInformation) {
		Map<Integer, ObtainedUnit> retVal = new HashMap<>();
		Integer userId = missionInformation.getUserId();
		Long sourcePlanetId = missionInformation.getSourcePlanetId();
		checkUserExists(userId);
		checkPlanetExists(sourcePlanetId);
		checkPlanetExists(missionInformation.getTargetPlanetId());
		checkDeployedAllowed(missionInformation.getMissionType());
		Set<Mission> deletedMissions = new HashSet<>();
		if (CollectionUtils.isEmpty(missionInformation.getInvolvedUnits())) {
			throw new SgtBackendInvalidInputException("involvedUnits can't be empty");
		}
		missionInformation.getInvolvedUnits().forEach(current -> {
			if (current.getCount() == null) {
				throw new SgtBackendInvalidInputException("No count was specified for unit " + current.getId());
			}
			ObtainedUnit currentObtainedUnit = findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(
					missionInformation.getUserId(), current.getId(), sourcePlanetId,
					!planetBo.isOfUserProperty(userId, sourcePlanetId));
			checkUnitCanDeploy(currentObtainedUnit, missionInformation);
			ObtainedUnit unitAfterSubstraction = obtainedUnitBo.saveWithSubtraction(currentObtainedUnit,
					current.getCount(), false);
			if (unitAfterSubstraction == null && currentObtainedUnit.getMission() != null
					&& currentObtainedUnit.getMission().getType().getCode().equals(MissionType.DEPLOYED.toString())) {
				deletedMissions.add(currentObtainedUnit.getMission());
			}
			retVal.put(current.getId(), currentObtainedUnit);
		});
		List<ObtainedUnit> unitsInMissionsAfterDelete = obtainedUnitBo
				.findByMissionIn(deletedMissions.stream().map(Mission::getId).collect(Collectors.toList()));
		deletedMissions.stream().filter(
				mission -> unitsInMissionsAfterDelete.stream().noneMatch(unit -> mission.equals(unit.getMission()))

		).forEach(this::resolveMission);
		return retVal;
	}

	/**
	 * Checks if the current obtained unit can do deploy (if already deployed in
	 * some cases, cannot)
	 *
	 * @param currentObtainedUnit
	 * @param missionType
	 * @since 0.7.4
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void checkUnitCanDeploy(ObtainedUnit currentObtainedUnit, UnitMissionInformation missionInformation) {
		MissionType unitMissionType = obtainedUnitBo.resolveMissionType(currentObtainedUnit);
		boolean isOfUserProperty = planetBo.isOfUserProperty(missionInformation.getUserId(),
				missionInformation.getTargetPlanetId());
		switch (configurationBo.findDeployMissionConfiguration()) {
		case ONLY_ONCE_RETURN_SOURCE:
		case ONLY_ONCE_RETURN_DEPLOYED:
			if (!isOfUserProperty && unitMissionType == MissionType.DEPLOYED
					&& missionInformation.getMissionType() == MissionType.DEPLOY) {
				throw new SgtBackendInvalidInputException("You can't do a deploy mission after a deploy mission");
			}
			break;
		default:
			break;
		}
	}

	/**
	 * Checks if the DEPLOY mission is allowed
	 *
	 * @param missionType
	 * @throws SgtBackendInvalidInputException If the deployment mission is
	 *                                         <b>globally</b> disabled
	 * @since 0.7.4
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void checkDeployedAllowed(MissionType missionType) {
		if (missionType == MissionType.DEPLOY
				&& configurationBo.findDeployMissionConfiguration().equals(DeployMissionConfigurationEnum.DISALLOWED)) {
			throw new SgtBackendInvalidInputException("The deployment mission is globally disabñed");
		}
	}

	/**
	 * Returns a copy of the object, used to make missionInformation immutable
	 *
	 * @param missionInformation
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private UnitMissionInformation copyMissionInformation(UnitMissionInformation missionInformation) {
		UnitMissionInformation retVal = new UnitMissionInformation();
		BeanUtils.copyProperties(missionInformation, retVal);
		return retVal;
	}

	/**
	 * Checks if the input Unit <i>id</i> exists, and returns the associated
	 * ObtainedUnit
	 *
	 * @param id
	 * @param isDeployedMission If true will search for a deployed obtained unit,
	 *                          else for an obtained unit with a <i>null<i> mission
	 * @return the expected obtained id
	 * @throws NotFoundException If obtainedUnit doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private ObtainedUnit findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMission(Integer userId, Integer unitId,
			Long planetId, boolean isDeployedMission) {
		ObtainedUnit retVal = isDeployedMission
				? obtainedUnitBo.findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(userId, unitId, planetId)
				: obtainedUnitBo.findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNull(userId, unitId, planetId);

		if (retVal == null) {
			throw new NotFoundException("No obtainedUnit for unit with id " + unitId + " was found in planet "
					+ planetId + ", nice try, dirty hacker!");
		}
		return retVal;
	}

	/**
	 * Checks if the logged in user is the creator of the mission
	 *
	 * @param invoker The creator of the mission
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void checkInvokerIsTheLoggedUser(Integer invoker) {
		if (!invoker.equals(userStorageBo.findLoggedIn().getId())) {
			throw new SgtBackendInvalidInputException("Invoker is not the logged in user");
		}
	}

	/**
	 * Prepares a mission to be scheduled
	 *
	 * @param missionInformation
	 * @param type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Mission prepareMission(UnitMissionInformation missionInformation, MissionType type) {
		Mission retVal = new Mission();
		retVal.setStartingDate(new Date());
		Double requiredTime = calculateRequiredTime(type);
		retVal.setMissionInformation(null);
		retVal.setType(findMissionType(type));
		retVal.setUser(userStorageBo.findById(missionInformation.getUserId()));
		retVal.setRequiredTime(requiredTime);
		Long sourcePlanetId = missionInformation.getSourcePlanetId();
		Long targetPlanetId = missionInformation.getTargetPlanetId();
		if (sourcePlanetId != null) {
			retVal.setSourcePlanet(planetBo.findLockedById(sourcePlanetId));
		}
		if (targetPlanetId != null) {
			retVal.setTargetPlanet(planetBo.findLockedById(targetPlanetId));
		}

		retVal.setTerminationDate(computeTerminationDate(requiredTime));
		return retVal;
	}

	/**
	 * Calculates time required to complete the mission
	 *
	 *
	 * @param type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Double calculateRequiredTime(MissionType type) {
		return (double) configurationBo.findMissionBaseTimeByType(type);
	}

	/**
	 * Emits a local mission change to the target user
	 *
	 * @param mission
	 * @param transactionAffected When specified, will search in the result of find
	 *                            user missions one with the same id, and replace it
	 *                            with that <br>
	 *                            This is require because, entity relations may not
	 *                            has been populated, as transaction is not done
	 * @param user
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void emitLocalMissionChangeAfterCommit(Mission mission) {
		UserStorage user = mission.getUser();
		TransactionUtil.doAfterCommit(() -> {
			emitLocalMissionChange(mission, user);
		});
	}

	private void emitLocalMissionChange(Mission mission, UserStorage user) {
		entityManager.refresh(mission);
		emitEnemyMissionsChange(mission);
		emitMissions(user.getId());
	}

	private void emitEnemyMissionsChange(UserStorage user) {
		socketIoService.sendMessage(user, ENEMY_MISSION_CHANGE, () -> findEnemyRunningMissions(user));

	}

	private AttackInformation buildAttackInformation(Planet targetPlanet, Mission attackMission) {
		AttackInformation retVal = new AttackInformation(attackMission, targetPlanet);
		obtainedUnitBo.findInvolvedInAttack(targetPlanet).forEach(retVal::addUnit);
		obtainedUnitBo.findByMissionId(attackMission.getId()).forEach(retVal::addUnit);
		return retVal;
	}

	/**
	 * Defines the new owner for the targetPlanet
	 *
	 * @param owner         The new owner
	 * @param involvedUnits The units used by the owner to conquest the planet
	 * @param targetPlanet
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void definePlanetAsOwnedBy(UserStorage owner, List<ObtainedUnit> involvedUnits, Planet targetPlanet) {
		targetPlanet.setOwner(owner);
		involvedUnits.forEach(current -> {
			current.setSourcePlanet(targetPlanet);
			current.setTargetPlanet(null);
			current.setMission(null);
		});
		planetBo.save(targetPlanet);
		obtainedUnitBo.findByUserIdAndTargetPlanetAndMissionTypeCode(owner.getId(), targetPlanet, MissionType.DEPLOYED)
				.forEach(units -> {
					Mission mission = units.getMission();
					obtainedUnitBo.moveUnit(units, owner.getId(), targetPlanet.getId());
					if (mission != null) {
						delete(mission);
					}

				});
		if (targetPlanet.getSpecialLocation() != null) {
			requirementBo.triggerSpecialLocation(owner, targetPlanet.getSpecialLocation());
		}

		planetListBo.emitByChangedPlanet(targetPlanet);
		planetBo.emitPlanetOwnedChange(owner);
		socketIoService.sendMessage(owner, UNIT_OBTAINED_CHANGE,
				() -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(owner.getId())));
	}

	private float calculateMoveCost(MissionType missionType, Planet sourcePlanet, Planet targetPlanet) {
		final String prefix = "MISSION_SPEED_";
		String missionTypeName = missionType.name();
		long positionInQuadrant = Math.abs(sourcePlanet.getPlanetNumber() - targetPlanet.getPlanetNumber());
		long quadrants = Math.abs(sourcePlanet.getQuadrant() - targetPlanet.getQuadrant());
		long sectors = Math.abs(sourcePlanet.getSector() - targetPlanet.getSector());
		float planetDiff = NumberUtils.toFloat(
				configurationBo.findOrSetDefault(prefix + missionTypeName + "_P_MOVE_COST", "0.01").getValue(), 0.01f);
		float quadrantDiff = NumberUtils.toFloat(
				configurationBo.findOrSetDefault(prefix + missionTypeName + "_Q_MOVE_COST", "0.02").getValue(), 0.02f);
		float sectorDiff = NumberUtils.toFloat(
				configurationBo.findOrSetDefault(prefix + missionTypeName + "_S_MOVE_COST", "0.03").getValue(), 0.03f);
		float galaxyDiff = NumberUtils.toFloat(
				configurationBo.findOrSetDefault(prefix + missionTypeName + "_G_MOVE_COST", "0.15").getValue(), 0.15f);
		return 1 + (positionInQuadrant * planetDiff) + (quadrants * quadrantDiff) + (sectors * sectorDiff)
				+ (targetPlanet.getGalaxy().equals(sourcePlanet.getGalaxy()) ? galaxyDiff : 0);
	}

	private void checkCrossGalaxy(MissionType missionType, List<ObtainedUnit> units, Planet sourcePlanet,
			Planet targetPlanet) {
		UserStorage user = units.get(0).getUser();
		if (!sourcePlanet.getGalaxy().getId().equals(targetPlanet.getGalaxy().getId())) {
			units.forEach(unit -> {
				SpeedImpactGroup speedGroup = unit.getUnit().getSpeedImpactGroup();
				speedGroup = speedGroup == null ? unit.getUnit().getType().getSpeedImpactGroup() : speedGroup;
				if (speedGroup != null) {
					if (!canDoMission(user, targetPlanet, speedGroup, missionType)) {
						throw new SgtBackendInvalidInputException(
								"This speed group doesn't support this mission outside of the galaxy");
					}
					ObjectRelation relation = objectRelationBo
							.findOneByObjectTypeAndReferenceId(ObjectEnum.SPEED_IMPACT_GROUP, speedGroup.getId());
					if (relation == null) {
						LOG.warn("Unexpected null objectRelation for SPEED_IMPACT_GROUP with id " + speedGroup.getId());
					} else if (!unlockedRelationBo.isUnlocked(user, relation)) {
						throw new SgtBackendInvalidInputException(
								"Don't try it.... you can't do cross galaxy missions, and you know it");
					}
				}
			});
		}
	}

	/**
	 *
	 * @param missionId
	 * @param user
	 * @param targetPlanet
	 * @return True if should continue the mission
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private boolean triggerAttackIfRequired(Mission mission, UserStorage user, Planet targetPlanet) {
		boolean continueMission = true;
		if (isAttackTriggerEnabledForMission(MissionType.valueOf(mission.getType().getCode()))
				&& obtainedUnitBo.areUnitsInvolved(user, targetPlanet)) {
			AttackInformation result = processAttack(mission.getId(), false);
			continueMission = !result.isMissionRemoved();
		}
		return continueMission;
	}

	private int findMissionTypeDivisor(MissionType missionType) {
		return Integer.valueOf(
				configurationBo.findOrSetDefault("MISSION_SPEED_DIVISOR_" + missionType.name(), "1").getValue());
	}

	private boolean isAttackTriggerEnabledForMission(MissionType missionType) {
		return Boolean.parseBoolean(configurationBo
				.findOrSetDefault("MISSION_" + missionType.name() + "_TRIGGER_ATTACK", "FALSE").getValue());
	}
}
