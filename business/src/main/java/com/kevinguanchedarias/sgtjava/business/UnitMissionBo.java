package com.kevinguanchedarias.sgtjava.business;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import com.kevinguanchedarias.sgtjava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.sgtjava.dto.MissionDto;
import com.kevinguanchedarias.sgtjava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.MissionReport;
import com.kevinguanchedarias.sgtjava.entity.ObtainedUnit;
import com.kevinguanchedarias.sgtjava.entity.Planet;
import com.kevinguanchedarias.sgtjava.entity.Unit;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.MissionType;
import com.kevinguanchedarias.sgtjava.exception.NotFoundException;
import com.kevinguanchedarias.sgtjava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.sgtjava.exception.ProgrammingException;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.sgtjava.exception.UserNotFoundException;
import com.kevinguanchedarias.sgtjava.pojo.DeliveryQueueEntry;
import com.kevinguanchedarias.sgtjava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.sgtjava.util.DtoUtilService;

@Service
public class UnitMissionBo extends AbstractMissionBo {
	private static final long serialVersionUID = 344402831344882216L;

	private static final Logger LOG = Logger.getLogger(UnitMissionBo.class);
	private static final String JOB_GROUP_NAME = "UnitMissions";
	private static final String MAX_PLANETS_MESSAGE = "You already have the max planets, you can have";

	/**
	 * Represents an ObtainedUnit, its full attack, and the pending attack is
	 * has
	 *
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public class AttackObtainedUnit {
		Double pendingAttack;
		Double availableShield;
		Double availableHealth;
		Long finalCount;
		ObtainedUnit obtainedUnit;

		private Double totalAttack;
		private Long initialCount;
		private Double totalShield;
		private Double totalHealth;

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

		/**
		 * Notice: Setter with logic, check it
		 * 
		 * @param obtainedUnit
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		public void setObtainedUnit(ObtainedUnit obtainedUnit) {
			Unit unit = obtainedUnit.getUnit();
			initialCount = obtainedUnit.getCount();
			finalCount = initialCount;
			totalAttack = initialCount.doubleValue() * unit.getAttack();
			pendingAttack = totalAttack;
			totalShield = initialCount.doubleValue() * unit.getShield();
			availableShield = totalShield;
			totalHealth = initialCount.doubleValue() * unit.getHealth();
			availableHealth = totalHealth;
			this.obtainedUnit = obtainedUnit;
		}

	}

	public class AttackUserInformation {
		AttackInformation attackInformationRef;
		private UserStorage user;
		Double earnedPoints = 0D;
		List<AttackObtainedUnit> unitsWithAvailableAttack = new ArrayList<>();
		List<AttackObtainedUnit> unitsWithoutAttack = new ArrayList<>();
		boolean isDefeated = false;
		boolean canAttack = true;

		public AttackUserInformation(UserStorage user) {
			this.setUser(user);
		}

		/**
		 * List of users that the current user can attack
		 * 
		 * @return
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		public List<AttackUserInformation> findVictims(List<AttackUserInformation> allUsers) {
			return allUsers.stream()
					.filter(current -> !current.isDefeated && !current.getUser().getId().equals(getUser().getId()))
					.collect(Collectors.toList());
		}

		public List<AttackObtainedUnit> findAllUnits() {
			List<AttackObtainedUnit> retVal = new ArrayList<>();
			retVal.addAll(unitsWithAvailableAttack);
			retVal.addAll(unitsWithoutAttack);
			return retVal;
		}

		public List<AttackObtainedUnit> findUnitsWithLife() {
			return findAllUnits().stream().filter(current -> current.availableHealth >= 0D)
					.collect(Collectors.toList());
		}

		public void attackVictim(AttackUserInformation targetVictim, List<AttackUserInformation> allUsers) {
			unitsWithAvailableAttack.stream().filter(current -> {
				List<AttackObtainedUnit> victimUnitsWithLife = targetVictim.findUnitsWithLife();
				final AtomicInteger count = new AtomicInteger(victimUnitsWithLife.size());
				victimUnitsWithLife.stream().filter(victimUnit -> {
					Double myAttack = current.pendingAttack;
					Double victimShield = victimUnit.availableShield;
					if (victimShield > myAttack) {
						current.pendingAttack = 0D;
						victimUnit.availableShield -= myAttack;
					} else {
						myAttack -= victimUnit.availableShield;
						victimUnit.availableShield = 0D;
						Double victimHealth = victimUnit.availableHealth;
						addPointsAndUpdateCount(myAttack, victimUnit);
						if (victimHealth > myAttack) {
							current.pendingAttack = 0D;
							victimUnit.availableHealth -= myAttack;
						} else {
							current.pendingAttack = myAttack - victimHealth;
							victimUnit.availableHealth = 0D;
							obtainedUnitBo.delete(victimUnit.obtainedUnit);
							deleteMissionIfRequired(victimUnit.obtainedUnit);
							count.decrementAndGet();
						}
					}
					if (current.pendingAttack == 0D) {
						unitsWithoutAttack.add(current);
					}
					return current.pendingAttack == 0D;
				}).findFirst();
				if (count.get() == 0) {
					targetVictim.isDefeated = true;
				}
				return count.get() == 0;
			}).findFirst();
			updateUnitsWithAvailableAttack();
			updateCanAttack(allUsers);
		}

		public UserStorage getUser() {
			return user;
		}

		public void setUser(UserStorage user) {
			this.user = user;
		}

		public Double getEarnedPoints() {
			return earnedPoints;
		}

		/**
		 * Deletes the mission from the system, when all units involved ade
		 * death
		 * 
		 * Notice, should be invoked after <b>removing the obtained unit</b>
		 * 
		 * @param obtainedUnit
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		private void deleteMissionIfRequired(ObtainedUnit obtainedUnit) {
			Mission mission = obtainedUnit.getMission();
			if (mission != null && !obtainedUnitBo.existsByMission(mission)) {
				if (attackInformationRef.attackMission.getId().equals(mission.getId())) {
					attackInformationRef.setRemoved(true);
				} else {
					delete(mission);
				}
			}
		}

		private void updateUnitsWithAvailableAttack() {
			unitsWithAvailableAttack = unitsWithAvailableAttack.stream().filter(current -> current.pendingAttack > 0D)
					.collect(Collectors.toList());
		}

		private void updateCanAttack(List<AttackUserInformation> allUsers) {
			canAttack = !unitsWithAvailableAttack.isEmpty() && !findVictims(allUsers).isEmpty();
		}

		private void addPointsAndUpdateCount(double usedAttack, AttackObtainedUnit victimUnit) {
			Double healthForEachUnit = victimUnit.totalHealth / victimUnit.initialCount;
			Long killedCount = (long) Math.floor(usedAttack / healthForEachUnit);
			if (killedCount > victimUnit.finalCount) {
				killedCount = victimUnit.finalCount;
				victimUnit.finalCount = 0L;
			} else {
				victimUnit.finalCount -= killedCount;
			}
			earnedPoints += killedCount * victimUnit.obtainedUnit.getUnit().getPoints();
		}
	}

	public class AttackInformation {
		private Mission attackMission;
		private boolean isRemoved = false;
		private List<AttackUserInformation> users = new ArrayList<>();

		public AttackInformation() {
			throw new ProgrammingException(
					"Can't invoke constructor for " + this.getClass().getName() + " without arguments");
		}

		public AttackInformation(Mission attackMission) {
			this.attackMission = attackMission;
		}

		/**
		 * To have the expected behavior sohuld be invoked after
		 * <i>startAttack()</i>
		 * 
		 * @return true if the mission has been removed from the database
		 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
		 */
		public boolean isMissionRemoved() {
			return isRemoved;
		}

		public void startAttack() {
			doAttack();
			updatePoints();
		}

		public void addUnitToUser(ObtainedUnit unitToAdd) {

			AttackUserInformation attackUserInformation = users.stream()
					.filter(current -> current.getUser().getId().equals(unitToAdd.getUser().getId())).findFirst()
					.orElse(null);
			if (attackUserInformation == null) {
				attackUserInformation = new AttackUserInformation(unitToAdd.getUser());
				attackUserInformation.attackInformationRef = this;
				users.add(attackUserInformation);
			}
			AttackObtainedUnit attackObtainedUnit = new AttackObtainedUnit();
			attackObtainedUnit.setObtainedUnit(unitToAdd);
			attackUserInformation.unitsWithAvailableAttack.add(attackObtainedUnit);
		}

		public List<AttackUserInformation> getUsers() {
			return users;
		}

		public void setUsers(List<AttackUserInformation> users) {
			this.users = users;
		}

		public Mission getAttackMission() {
			return attackMission;
		}

		public void setRemoved(boolean isRemoved) {
			this.isRemoved = isRemoved;
		}

		private void doAttack() {
			Collections.shuffle(users);
			List<AttackUserInformation> attackerUsers = users.stream().filter(current -> current.canAttack)
					.collect(Collectors.toList());
			if (!attackerUsers.isEmpty()) {
				AttackUserInformation attackerUser = attackerUsers.get(0);
				List<AttackUserInformation> victims = attackerUser.findVictims(users);
				if (victims.isEmpty()) {
					attackerUser.canAttack = false;
				} else {
					victims.stream().filter(current -> {
						attackerUser.attackVictim(current, users);
						return !attackerUser.canAttack;
					}).findFirst();
				}
				doAttack();
			}

		}

		private void updatePoints() {
			users.forEach(current -> userStorageBo.addPointsToUser(current.getUser(), current.earnedPoints));
		}

	}

	@Autowired
	private ConfigurationBo configurationBo;

	@Autowired
	private SocketIoService socketIoService;

	@Autowired
	private MissionReportBo missionReportBo;

	private DtoUtilService dtoUtilService = new DtoUtilService();

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
	 * @param missionInformation
	 *            <i>userId</i> is <b>ignored</b> in this method <b>immutable
	 *            object</b>
	 * @return mission representation DTO
	 * @throws SgtBackendInvalidInputException
	 *             When input information is not valid
	 * @throws UserNotFoundException
	 *             When user doesn't exists <b>(in this universe)</b>
	 * @throws PlanetNotFoundException
	 *             When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public UnitRunningMissionDto myRegisterExploreMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterExploreMission(missionInformation);
	}

	/**
	 * Registers a explore mission <b>as a admin</b>
	 * 
	 * @param missionInformation
	 * @return mission representation DTO
	 * @throws SgtBackendInvalidInputException
	 *             When input information is not valid
	 * @throws UserNotFoundException
	 *             When user doesn't exists <b>(in this universe)</b>
	 * @throws PlanetNotFoundException
	 *             When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public UnitRunningMissionDto adminRegisterExploreMission(UnitMissionInformation missionInformation) {
		return commonMissionRegister(missionInformation, MissionType.EXPLORE);
	}

	@Transactional
	public UnitRunningMissionDto myRegisterGatherMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterGatherMission(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterGatherMission(UnitMissionInformation missionInformation) {
		return commonMissionRegister(missionInformation, MissionType.GATHER);
	}

	public UnitRunningMissionDto myRegisterEstablishBaseMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterEstablishBase(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterEstablishBase(UnitMissionInformation missionInformation) {
		return commonMissionRegister(missionInformation, MissionType.ESTABLISH_BASE);
	}

	@Transactional
	public UnitRunningMissionDto myRegisterAttackMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterAttackMission(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterAttackMission(UnitMissionInformation missionInformation) {
		return commonMissionRegister(missionInformation, MissionType.ATTACK);
	}

	@Transactional
	public UnitRunningMissionDto myRegisterCounterattackMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterCounterattackMission(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterCounterattackMission(UnitMissionInformation missionInformation) {
		if (!planetBo.isOfUserProperty(missionInformation.getUserId(), missionInformation.getTargetPlanetId())) {
			throw new SgtBackendInvalidInputException(
					"TargetPlanet doesn't belong to sender user, try again dear Hacker, maybe next time you have some luck");
		}
		return commonMissionRegister(missionInformation, MissionType.COUNTERATTACK);
	}

	@Transactional
	public UnitRunningMissionDto myRegisterConquestMission(UnitMissionInformation missionInformation) {
		myRegister(missionInformation);
		return adminRegisterConquestMission(missionInformation);
	}

	@Transactional
	public UnitRunningMissionDto adminRegisterConquestMission(UnitMissionInformation missionInformation) {
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
		return commonMissionRegister(missionInformation, MissionType.CONQUEST);
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
		List<ObtainedUnit> involvedUnits = obtainedUnitBo.findByMissionId(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		if (!planetBo.isExplored(user, targetPlanet)) {
			planetBo.defineAsExplored(user, targetPlanet);
		}
		List<ObtainedUnit> unitsInPlanet = obtainedUnitBo.explorePlanetUnits(targetPlanet);
		adminRegisterReturnMission(mission);
		UnitMissionReportBuilder builder = UnitMissionReportBuilder
				.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
				.withExploredInformation(unitsInPlanet);
		hanleMissionReportSave(mission, builder);
		resolveMission(mission);
		socketIoService.sendMessage(user, "explore_report", builder.build());
		emitLocalMissionChange(mission, user);
	}

	@Transactional
	public void processGather(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = obtainedUnitBo.findByMissionId(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		adminRegisterReturnMission(mission);
		Long gathered = involvedUnits.stream()
				.map(current -> ObjectUtils.firstNonNull(current.getUnit().getCharge(), 0) * current.getCount())
				.reduce(0L, (sum, current) -> sum + current);
		Double withPlanetRichness = gathered * targetPlanet.findRationalRichness();
		Double withUserImprovement = withPlanetRichness
				+ (withPlanetRichness * user.getImprovements().findRationalChargeCapacity());
		Double primaryResource = withUserImprovement * 0.7;
		Double secondaryResource = withUserImprovement * 0.3;
		user.addtoPrimary(primaryResource);
		user.addToSecondary(secondaryResource);
		UnitMissionReportBuilder builder = UnitMissionReportBuilder
				.create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
				.withGatherInformation(primaryResource, secondaryResource);
		hanleMissionReportSave(mission, builder);
		resolveMission(mission);
		socketIoService.sendMessage(user, "gather_report", builder.build());
		emitLocalMissionChange(mission, user);
	}

	@Transactional
	public void processEstablishBase(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = obtainedUnitBo.findByMissionId(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(user, mission.getSourcePlanet(),
				targetPlanet, involvedUnits);
		UserStorage planetOwner = targetPlanet.getOwner();
		boolean hasMaxPlanets = planetBo.hasMaxPlanets(user);
		if (planetOwner != null || hasMaxPlanets) {
			adminRegisterReturnMission(mission);
			if (planetOwner != null) {
				builder.withEstablishBaseInformation(false, "The planet already belongs to a user");
			} else {
				builder.withEstablishBaseInformation(false, MAX_PLANETS_MESSAGE);
			}
		} else {
			builder.withEstablishBaseInformation(true);
			definePlanetAsOwnedBy(user, involvedUnits, targetPlanet);
		}
		hanleMissionReportSave(mission, builder);
		resolveMission(mission);
		socketIoService.sendMessage(user, "establish_base_report", builder.build());
		emitLocalMissionChange(mission, user);
	}

	@Transactional
	public void processAttack(Long missionId) {
		Mission mission = findById(missionId);
		List<ObtainedUnit> involvedUnits = obtainedUnitBo.findByMissionId(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		AttackInformation attackInformation = buildAttackInformation(targetPlanet, mission);
		attackInformation.startAttack();
		if (!attackInformation.isMissionRemoved()) {
			adminRegisterReturnMission(mission);
		}
		resolveMission(mission);
		UnitMissionReportBuilder builder = UnitMissionReportBuilder
				.create(mission.getUser(), mission.getSourcePlanet(), targetPlanet, involvedUnits)
				.withAttackInformation(attackInformation);
		hanleMissionReportSave(mission, builder,
				attackInformation.users.stream().map(current -> current.user).collect(Collectors.toList()));
		emitLocalMissionChange(mission, mission.getUser());
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
		processAttack(missionId);
	}

	/**
	 * Creates a return mission from an existing mission
	 * 
	 * @param mission
	 *            Existing mission that will be returned
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void adminRegisterReturnMission(Mission mission) {
		adminRegisterReturnMission(mission, null);
	}

	/**
	 * Creates a return mission from an existing mission
	 * 
	 * @param mission
	 *            Existing mission that will be returned
	 * @param customRequiredTime
	 *            If not null will be used as the time for the return mission,
	 *            else will use source mission time
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional
	public void adminRegisterReturnMission(Mission mission, Double customRequiredTime) {
		Mission returnMission = new Mission();
		returnMission.setType(findMissionType(MissionType.RETURN_MISSION));
		returnMission.setRequiredTime(mission.getRequiredTime());
		Double requiredTime = customRequiredTime == null ? mission.getRequiredTime() : customRequiredTime;
		returnMission.setTerminationDate(computeTerminationDate(requiredTime));
		returnMission.setSourcePlanet(mission.getTargetPlanet());
		returnMission.setTargetPlanet(mission.getSourcePlanet());
		returnMission.setUser(mission.getUser());
		returnMission.setRelatedMission(mission);
		List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findByMissionId(mission.getId());
		missionRepository.saveAndFlush(returnMission);
		obtainedUnits.forEach(current -> current.setMission(returnMission));
		obtainedUnitBo.save(obtainedUnits);
		scheduleMission(returnMission);
	}

	@Transactional
	public void proccessReturnMission(Long missionId) {
		Mission mission = missionRepository.findOne(missionId);
		List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findByMissionId(mission.getId());
		List<ObtainedUnit> inPlanet = obtainedUnitBo.findByUserIdAndSourcePlanetAndMissionIdIsNull(mission.getUser(),
				mission.getTargetPlanet());
		obtainedUnits.forEach(current -> {
			ObtainedUnit existingUnit = obtainedUnitBo.findHavingSameUnit(inPlanet, current);
			if (existingUnit == null) {
				current.setMission(null);
				current.setSourcePlanet(mission.getTargetPlanet());
				current.setTargetPlanet(null);
			} else {
				existingUnit.addCount(current.getCount());
				obtainedUnitBo.delete(current);
			}
		});
		resolveMission(mission);
		emitLocalMissionChange(mission, mission.getUser());
	}

	@Transactional
	public void processConquest(Long missionId) {
		Mission mission = findById(missionId);
		UserStorage user = mission.getUser();
		List<ObtainedUnit> involvedUnits = obtainedUnitBo.findByMissionId(missionId);
		Planet targetPlanet = mission.getTargetPlanet();
		UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(user, mission.getSourcePlanet(),
				targetPlanet, involvedUnits);
		boolean maxPlanets = planetBo.hasMaxPlanets(user);
		if (maxPlanets || planetBo.isHomePlanet(targetPlanet)) {
			adminRegisterReturnMission(mission);
			if (maxPlanets) {
				builder.withConquestInformation(false, MAX_PLANETS_MESSAGE);
			} else {
				builder.withConquestInformation(false, "This is a home planet now, can't conquest it");
			}
		} else {
			obtainedUnitBo.deleteBySourcePlanetIdAndMissionIdNull(targetPlanet);
			definePlanetAsOwnedBy(user, involvedUnits, targetPlanet);
			builder.withConquestInformation(true);
		}
		hanleMissionReportSave(mission, builder);
		resolveMission(mission);
		socketIoService.sendMessage(user, "conquest_report", builder.build());
		emitLocalMissionChange(mission, user);
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
			Interval interval = new Interval(new Instant().getMillis(), mission.getTerminationDate().getTime());
			adminRegisterReturnMission(mission,
					Double.valueOf(mission.getRequiredTime() - (interval.toDurationMillis() / 1000D)));
		}
	}

	/**
	 * Executes modifications to <i>missionInformation</i> to define the logged
	 * in user as the sender user
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

	private UnitRunningMissionDto commonMissionRegister(UnitMissionInformation missionInformation,
			MissionType missionType) {
		List<ObtainedUnit> obtainedUnits = new ArrayList<>();
		UserStorage user = userStorageBo.findLoggedIn();
		UnitMissionInformation targetMissionInformation = copyMissionInformation(missionInformation);
		targetMissionInformation.setUserId(user.getId());
		if (missionType != MissionType.EXPLORE
				&& !planetBo.isExplored(user.getId(), missionInformation.getTargetPlanetId())) {
			throw new SgtBackendInvalidInputException(
					"Can't send this mission, because target planet is not explored ");
		}
		checkAndLoadObtainedUnits(missionInformation);
		Mission mission = missionRepository.saveAndFlush((prepareMission(targetMissionInformation, missionType)));
		targetMissionInformation.getInvolvedUnits().forEach(current -> {
			ObtainedUnit currentObtainedUnit = new ObtainedUnit();
			currentObtainedUnit.setMission(mission);
			currentObtainedUnit.setCount(current.getCount());
			currentObtainedUnit.setUser(user);
			currentObtainedUnit.setUnit(unitBo.findById(current.getId()));
			currentObtainedUnit.setSourcePlanet(mission.getTargetPlanet());
			currentObtainedUnit.setTargetPlanet(mission.getSourcePlanet());
			obtainedUnits.add(currentObtainedUnit);
		});
		obtainedUnitBo.save(obtainedUnits);
		scheduleMission(mission);
		return new UnitRunningMissionDto(mission, obtainedUnits);
	}

	/**
	 * Will check if the input DTO is valid, the following validations will be
	 * done <br>
	 * <b>IMPORTANT:</b> This method is intended to be use as part of the
	 * mission registration process
	 * <ul>
	 * <li>Check if the user exists</li>
	 * <li>Check if the sourcePlanet exists</li>
	 * <li>Check if the targetPlanet exists</li>
	 * <li>Check for each selected unit if there is an associated obtainedUnit
	 * and if count is valid</li>
	 * </ul>
	 * 
	 * @param missionInformation
	 * @return Database list of <i>ObtainedUnit</i> with the subtraction
	 *         <b>already applied</b>
	 * @throws SgtBackendInvalidInputException
	 *             when validation was not passed
	 * @throws UserNotFoundException
	 *             When user doesn't exists <b>(in this universe)</b>
	 * @throws PlanetNotFoundException
	 *             When the planet doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private List<ObtainedUnit> checkAndLoadObtainedUnits(UnitMissionInformation missionInformation) {
		List<ObtainedUnit> retVal = new ArrayList<>();
		checkUserExists(missionInformation.getUserId());
		checkPlanetExists(missionInformation.getSourcePlanetId());
		checkPlanetExists(missionInformation.getTargetPlanetId());
		if (CollectionUtils.isEmpty(missionInformation.getInvolvedUnits())) {
			throw new SgtBackendInvalidInputException("involvedUnits can't be empty");
		}
		missionInformation.getInvolvedUnits().forEach(current -> {
			if (current.getCount() == null) {
				throw new SgtBackendInvalidInputException("No count was specified for unit " + current.getId());
			}
			ObtainedUnit currentObtainedUnit = findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMissionIdIsNull(
					missionInformation.getUserId(), current.getId(), missionInformation.getSourcePlanetId());
			retVal.add(obtainedUnitBo.saveWithSubtraction(currentObtainedUnit, current.getCount()));
		});
		return retVal;
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
	 * @return the expected obtained id
	 * @throws NotFoundException
	 *             If obtainedUnit doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private ObtainedUnit findObtainedUnitByUserIdAndUnitIdAndPlanetIdAndMissionIdIsNull(Integer userId, Integer unitId,
			Long planetId) {
		ObtainedUnit retVal = obtainedUnitBo.findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIdIsNull(userId, unitId,
				planetId);
		if (retVal == null) {
			throw new NotFoundException("No obtainedUnit for unit with id " + unitId + " was found in planet "
					+ planetId + ", nice try, dirty hacker!");
		}
		return retVal;
	}

	/**
	 * Checks if the logged in user is the creator of the mission
	 * 
	 * @param invoker
	 *            The creator of the mission
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
		Double requiredTime = calculateRequiredTime(type);
		retVal.setMissionInformation(null);
		retVal.setType(findMissionType(type));
		retVal.setUser(userStorageBo.findById(missionInformation.getUserId()));
		retVal.setRequiredTime(requiredTime);
		Long sourcePlanetId = missionInformation.getSourcePlanetId();
		Long targetPlanetId = missionInformation.getTargetPlanetId();
		if (sourcePlanetId != null) {
			retVal.setSourcePlanet(planetBo.findById(sourcePlanetId));
		}
		if (targetPlanetId != null) {
			retVal.setTargetPlanet(planetBo.findById(targetPlanetId));
		}

		retVal.setTerminationDate(computeTerminationDate(requiredTime));
		return retVal;
	}

	/**
	 * Calculates time required to complete the mission
	 * 
	 * @todo In the future calculate the units speed
	 * 
	 * @param type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private Double calculateRequiredTime(MissionType type) {
		return Double.valueOf(configurationBo.findMissionBaseTimeByType(type));
	}

	/**
	 * Emits a local mission change to the target user
	 * 
	 * @param mission
	 * @param user
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private CompletableFuture<DeliveryQueueEntry> emitLocalMissionChange(Mission mission, UserStorage user) {
		return socketIoService.sendMessage(user, "local_mission_change",
				dtoUtilService.dtoFromEntity(MissionDto.class, mission));
	}

	/**
	 * Saves the MissionReport to the database
	 * 
	 * @param mission
	 * @param builder
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	private void hanleMissionReportSave(Mission mission, UnitMissionReportBuilder builder) {
		MissionReport missionReport = new MissionReport("{}", mission);
		missionReport.setUser(mission.getUser());
		missionReport = missionReportBo.save(missionReport);
		missionReport.setJsonBody(builder.withId(missionReport.getId()).buildJson());
		mission.setReport(missionReport);
	}

	private void hanleMissionReportSave(Mission mission, UnitMissionReportBuilder builder, List<UserStorage> users) {
		users.forEach(currentUser -> {
			MissionReport missionReport = new MissionReport("{}", mission);
			missionReport.setUser(currentUser);
			missionReport = missionReportBo.save(missionReport);
			missionReport.setReportDate(new Date());
			missionReport.setJsonBody(builder.withId(missionReport.getId()).buildJson());
			mission.setReport(missionReport);
		});
	}

	private AttackInformation buildAttackInformation(Planet targetPlanet, Mission attackMission) {
		AttackInformation retVal = new AttackInformation(attackMission);
		obtainedUnitBo.findInvolvedInAttack(targetPlanet, attackMission).forEach(retVal::addUnitToUser);
		return retVal;
	}

	/**
	 * Defines the new owner for the targetPlanet
	 * 
	 * @param owner
	 *            The new owner
	 * @param involvedUnits
	 *            The units used by the owner to conquest the planet
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
	}
}
