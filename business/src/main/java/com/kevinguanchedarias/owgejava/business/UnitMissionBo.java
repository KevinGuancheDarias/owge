package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.AttackRule;
import com.kevinguanchedarias.owgejava.entity.AttackRuleEntry;
import com.kevinguanchedarias.owgejava.entity.EntityWithMissionLimitation;
import com.kevinguanchedarias.owgejava.entity.InterceptableSpeedGroup;
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
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.enumerations.DeployMissionConfigurationEnum;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionSupportEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.OwgeElementSideDeletedException;
import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.SgtCorruptDatabaseException;
import com.kevinguanchedarias.owgejava.exception.UserNotFoundException;
import com.kevinguanchedarias.owgejava.pojo.GroupedImprovement;
import com.kevinguanchedarias.owgejava.pojo.InterceptedUnitsInformation;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.commons.lang3.text.WordUtils;
import org.apache.log4j.Logger;
import org.joda.time.Instant;
import org.joda.time.Interval;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
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

@Service
public class UnitMissionBo extends AbstractMissionBo {
    private static final String ENEMY_MISSION_CHANGE = "enemy_mission_change";

    private static final long serialVersionUID = 344402831344882216L;

    private static final Logger LOG = Logger.getLogger(UnitMissionBo.class);
    private static final String JOB_GROUP_NAME = "UnitMissions";
    private static final String MAX_PLANETS_MESSAGE = "I18N_MAX_PLANETS_EXCEEDED";

    @AllArgsConstructor
    public class AttackObtainedUnitWithScore {
        AttackObtainedUnit attackObtainedUnit;
        float score;
    }

    /**
     * Represents an ObtainedUnit, its full attack, and the pending attack is has
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public class AttackObtainedUnit {

        @EqualsAndHashCode.Include
        AttackUserInformation user;
        @EqualsAndHashCode.Include
        ObtainedUnit obtainedUnit;

        Double pendingAttack;
        boolean noAttack = false;
        Double availableShield;
        Double availableHealth;
        Long finalCount;

        private final Long initialCount;
        private Double totalShield;
        private Double totalHealth;

        public AttackObtainedUnit(ObtainedUnit obtainedUnit, GroupedImprovement userImprovement) {
            var unit = obtainedUnit.getUnit();
            var unitType = unit.getType();
            initialCount = obtainedUnit.getCount();
            finalCount = initialCount;
            double totalAttack = initialCount.doubleValue() * unit.getAttack();
            totalAttack += (totalAttack * improvementBo.findAsRational(
                    (double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.ATTACK, unitType)));
            pendingAttack = totalAttack;
            totalShield = initialCount.doubleValue() * ObjectUtils.firstNonNull(unit.getShield(), 0);
            totalShield += (totalShield * improvementBo.findAsRational(
                    (double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.SHIELD, unitType)));
            availableShield = totalShield;
            totalHealth = initialCount.doubleValue() * unit.getHealth();
            totalHealth += (totalHealth * improvementBo.findAsRational(
                    (double) userImprovement.findUnitTypeImprovement(ImprovementTypeEnum.DEFENSE, unitType)));
            availableHealth = totalHealth;
            this.obtainedUnit = obtainedUnit;
        }

        public Long getInitialCount() {
            return initialCount;
        }

        public Long getFinalCount() {
            return finalCount;
        }

        public ObtainedUnit getObtainedUnit() {
            return obtainedUnit;
        }

    }

    public class AttackUserInformation {
        Double earnedPoints = 0D;
        List<AttackObtainedUnit> units = new ArrayList<>();
        List<AttackObtainedUnit> attackableUnits;

        private final UserStorage user;
        private final GroupedImprovement userImprovement;

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

        /**
         * @return the units
         * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
         * @since 0.9.0
         */
        public List<AttackObtainedUnit> getUnits() {
            return units;
        }

    }

    public class AttackInformation {
        private final Mission attackMission;
        private final Map<Integer, AttackUserInformation> users = new HashMap<>();
        private final List<AttackObtainedUnit> units = new ArrayList<>();
        private final Set<Integer> usersWithDeletedMissions = new HashSet<>();
        private final Set<Integer> usersWithChangedCounts = new HashSet<>();
        private final Planet targetPlanet;
        private boolean isRemoved = false;
        private UnitMissionReportBuilder reportBuilder;

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
            var unit = new AttackObtainedUnit(unitEntity, user.userImprovement);
            unit.user = user;
            user.units.add(unit);
            units.add(unit);
        }

        public void startAttack() {
            Collections.shuffle(units);
            users.forEach((userId, user) -> user.attackableUnits = units.stream().filter(
                    unit -> !unit.user.user.getId().equals(user.user.getId()) && filterAlliance(user.user, unit.user.user))
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
         * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
         * @since 0.9.0
         */
        public Map<Integer, AttackUserInformation> getUsers() {
            return users;
        }

        public void setRemoved(boolean isRemoved) {
            this.isRemoved = isRemoved;
        }

        private void doAttack() {
            units.forEach(attackerUnit -> {
                List<AttackObtainedUnitWithScore> attackableByUnit = attackerUnit.user.attackableUnits.stream()
                        .filter(target -> {
                            var unitEntity = attackerUnit.obtainedUnit.getUnit();
                            var attackRule = ObjectUtils.firstNonNull(unitEntity.getAttackRule(),
                                    findAttackRule(unitEntity.getType()));
                            return canAttack(attackRule, target);
                        })
                        .map(target -> new AttackObtainedUnitWithScore(target, findScore(attackerUnit, target)))
                        .sorted((a, b) -> (int) (b.score - a.score))
                        .collect(Collectors.toList());
                for (AttackObtainedUnitWithScore target : attackableByUnit) {
                    attackTarget(attackerUnit, target);
                    if (attackerUnit.noAttack) {
                        break;
                    }
                }
            });
        }

        private float findScore(AttackObtainedUnit attacker, AttackObtainedUnit target) {
            var unit = attacker.obtainedUnit.getUnit();
            var criticalAttack = ObjectUtils.firstNonNull(unit.getCriticalAttack(), criticalAttackBo.findUsedCriticalAttack(unit.getType()));
            var criticalAttackRule = criticalAttackBo.findApplicableCriticalEntry(criticalAttack, target.obtainedUnit.getUnit());
            return criticalAttackRule == null
                    ? 1F
                    : criticalAttackRule.getValue();
        }

        private boolean canAttack(AttackRule attackRule, AttackObtainedUnit target) {
            if (attackRule != null && attackRule.getAttackRuleEntries() != null) {
                for (AttackRuleEntry ruleEntry : attackRule.getAttackRuleEntries()) {
                    if (ruleEntry.getTarget() == AttackableTargetEnum.UNIT) {
                        if (target.obtainedUnit.getUnit().getId().equals(ruleEntry.getReferenceId())) {
                            return ruleEntry.getCanAttack();
                        }
                    } else if (ruleEntry.getTarget() == AttackableTargetEnum.UNIT_TYPE) {
                        var unitType = findUnitTypeMatchingRule(ruleEntry,
                                target.obtainedUnit.getUnit().getType());
                        if (unitType != null) {
                            return ruleEntry.getCanAttack();
                        }
                    } else {
                        throw new ProgrammingException("unexpected code path");
                    }
                }
            }
            return true;
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

        private void attackTarget(AttackObtainedUnit source, AttackObtainedUnitWithScore targetWithScore) {
            var originalAttackValue = source.pendingAttack;
            var myAttack = source.pendingAttack * targetWithScore.score;
            boolean bypassShield = source.obtainedUnit.getUnit().getBypassShield();
            var target = targetWithScore.attackObtainedUnit;
            var victimHealth = bypassShield ? target.availableHealth
                    : target.availableHealth + target.availableShield;
            addPointsAndUpdateCount(myAttack, source, target);
            if (victimHealth > myAttack) {
                source.pendingAttack = 0D;
                source.noAttack = true;
                if (bypassShield) {
                    target.availableHealth -= myAttack;
                } else {
                    double attackDistributed = myAttack / 2;
                    target.availableShield -= attackDistributed;
                    target.availableHealth -= attackDistributed;
                }
                if (target.availableShield < 0.0D) {
                    target.availableHealth += target.availableShield;
                }
                if (!target.initialCount.equals(target.finalCount)) {
                    usersWithChangedCounts.add(target.user.getUser().getId());
                }
            } else {
                source.pendingAttack = myAttack - victimHealth;
                if (source.pendingAttack > originalAttackValue) {
                    source.pendingAttack = originalAttackValue;
                }
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
         * <p>
         * Notice, should be invoked after <b>removing the obtained unit</b>
         *
         * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
         */
        private void deleteMissionIfRequired(ObtainedUnit obtainedUnit) {
            var mission = obtainedUnit.getMission();
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
            double healthForEachUnit = Boolean.TRUE.equals(source.obtainedUnit.getUnit().getBypassShield())
                    ? victimUnit.totalHealth / victimUnit.initialCount
                    : (victimUnit.totalHealth + victimUnit.totalShield) / victimUnit.initialCount;
            long killedCount = (long) Math.floor(usedAttack / healthForEachUnit);
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
                var attackUserInformation = current.getValue();
                List<AttackObtainedUnit> userUnits = attackUserInformation.units;
                userStorageBo.addPointsToUser(attackUserInformation.getUser(), attackUserInformation.earnedPoints);
                userUnits.stream().filter(currentUnit -> !currentUnit.finalCount.equals(0L)
                        && !currentUnit.initialCount.equals(currentUnit.finalCount)).forEach(currentUnit -> {
                    long killed = currentUnit.initialCount - currentUnit.finalCount;
                    try {
                        obtainedUnitBo.trySave(currentUnit.obtainedUnit, -killed);
                        alteredUsers.add(attackUserInformation.getUser().getId());
                    } catch (OwgeElementSideDeletedException e) {
                        LOG.warn("Element side deleted", e);
                    }
                });
            });
            alteredUsers.addAll(usersWithChangedCounts);
            TransactionUtil.doAfterCommit(() -> alteredUsers.forEach(current -> {
                unitTypeBo.emitUserChange(current);
                socketIoService.sendMessage(current, UNIT_OBTAINED_CHANGE,
                        () -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(current)));

            }));
        }
    }

    @Autowired
    private ConfigurationBo configurationBo;

    @Autowired
    private transient SocketIoService socketIoService;

    @Autowired
    private ImageStoreBo imageStoreBo;

    @Autowired
    private transient PlanetListBo planetListBo;

    @Autowired
    private transient AsyncRunnerBo asyncRunnerBo;

    @Autowired
    private transient EntityManager entityManager;

    @Autowired
    private MissionBo missionBo;

    @Autowired
    private AuditBo auditBo;

    @Autowired
    private transient CriticalAttackBo criticalAttackBo;

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
        returnMission.setInvisible(Boolean.TRUE.equals(mission.getInvisible()));
        List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findByMissionId(mission.getId());
        missionRepository.saveAndFlush(returnMission);
        obtainedUnits.forEach(current -> current.setMission(returnMission));
        obtainedUnitBo.save(obtainedUnits);
        scheduleMission(returnMission);
        emitLocalMissionChangeAfterCommit(returnMission);
    }

    @Transactional
    public void proccessReturnMission(Mission mission) {
        Integer userId = mission.getUser().getId();
        List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findByMissionId(mission.getId());
        obtainedUnits.forEach(current -> obtainedUnitBo.moveUnit(current, userId, mission.getSourcePlanet().getId()));
        resolveMission(mission);
        emitLocalMissionChangeAfterCommit(mission);
        TransactionUtil.doAfterCommit(() -> {
            if (obtainedUnits.get(0).getMission() != null
                    && obtainedUnits.get(0).getMission().getInvolvedUnits() == null) {
                entityManager.refresh(obtainedUnits.get(0).getMission());
            }
            emitLocalMissionChange(mission, userId);
        });
        asyncRunnerBo
                .runAssyncWithoutContextDelayed(
                        () -> socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
                                () -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(userId))),
                        500);

    }

    @Transactional
    public UnitMissionReportBuilder processConquest(Mission mission, List<ObtainedUnit> involvedUnits) {
        UserStorage user = mission.getUser();
        Planet targetPlanet = mission.getTargetPlanet();
        UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(user, mission.getSourcePlanet(),
                targetPlanet, involvedUnits);
        boolean maxPlanets = planetBo.hasMaxPlanets(user);
        boolean areUnitsHavingToReturn = false;
        AttackInformation attackInformation = processAttack(mission, false);
        UserStorage oldOwner = targetPlanet.getOwner();
        boolean isOldOwnerDefeated;
        boolean isAllianceDefeated;
        if (oldOwner == null) {
            isOldOwnerDefeated = true;
            isAllianceDefeated = true;
        } else {
            isOldOwnerDefeated = !attackInformation.getUsers().containsKey(oldOwner.getId())
                    || attackInformation.getUsers().get(oldOwner.getId()).units.stream()
                    .noneMatch(current -> current.finalCount > 0L);
            isAllianceDefeated = isOldOwnerDefeated
                    && (oldOwner.getAlliance() == null || attackInformation.getUsers().entrySet().stream()
                    .filter(attackedUser -> attackedUser.getValue().getUser().getAlliance() != null
                            && attackedUser.getValue().getUser().getAlliance().equals(oldOwner.getAlliance()))
                    .allMatch(currentUser -> currentUser.getValue().units.stream()
                            .noneMatch(currentUserUnit -> currentUserUnit.finalCount > 0L)));
        }

        if (!isOldOwnerDefeated || !isAllianceDefeated || maxPlanets || planetBo.isHomePlanet(targetPlanet)) {
            if (!attackInformation.isMissionRemoved()) {
                adminRegisterReturnMission(mission);
                areUnitsHavingToReturn = true;
            }
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
            if (targetPlanet.getSpecialLocation() != null && oldOwner != null) {
                requirementBo.triggerSpecialLocation(oldOwner, targetPlanet.getSpecialLocation());
            }
            if (oldOwner != null) {
                planetBo.emitPlanetOwnedChange(oldOwner);
                findUnitBuildMission(targetPlanet).ifPresent(missionBo::adminCancelBuildMission);
                emitEnemyMissionsChange(oldOwner);
                UnitMissionReportBuilder enemyReportBuilder = UnitMissionReportBuilder
                        .create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
                        .withConquestInformation(true, "I18N_YOUR_PLANET_WAS_CONQUISTED");
                handleMissionReportSave(mission, enemyReportBuilder, true, oldOwner);
            }

        }
        resolveMission(mission);
        if (!areUnitsHavingToReturn) {
            emitLocalMissionChangeAfterCommit(mission);
        }
        return builder;
    }

    public Optional<Mission> findUnitBuildMission(Planet planet) {
        return missionRepository.findOneByResolvedFalseAndTypeCodeAndMissionInformationValue(MissionType.BUILD_UNIT.name(), planet.getId().doubleValue());
    }

    @Transactional
    public void proccessDeploy(Long missionId) {
        Mission mission = findById(missionId);
        if (mission != null) {
            UserStorage user = mission.getUser();
            Integer userId = user.getId();
            List<ObtainedUnit> alteredUnits = new ArrayList<>();
            findUnitsInvolved(missionId).forEach(current ->
                    alteredUnits.add(obtainedUnitBo.moveUnit(current, userId, mission.getTargetPlanet().getId()))
            );
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
                    launchedMission.ifPresent(unitRunningMissionDto -> unitRunningMissionDto
                            .setInvolvedUnits(obtainedUnitBo.toDto(alteredUnits)));
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
            var durationMillis = 0L;
            if (terminationMillis >= nowMillis) {
                var interval = new Interval(nowMillis, terminationMillis);
                durationMillis = (long) (interval.toDurationMillis() / 1000D);
            }
            adminRegisterReturnMission(mission, mission.getRequiredTime() - durationMillis);
        }
    }

    public List<ObtainedUnit> findInvolvedInMission(Mission mission) {
        return obtainedUnitBo.findByMissionId(mission.getId());
    }

    /**
     * finds user <b>not resolved</b> deployed mission, if none exists creates one
     * <br>
     * <b>IMPORTANT:</b> Will save the unit, because if the mission exists, has to
     * remove the firstDeploymentMission
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    @Transactional
    @Retryable(value = CannotAcquireLockException.class, backoff = @Backoff(delay = 500, random = true, maxDelay = 750, multiplier = 2))
    public void runUnitMission(Long missionId, MissionType missionType) {
        Mission mission = findById(missionId);
        List<ObtainedUnit> involvedUnits = findUnitsInvolved(missionId);
        List<ObtainedUnit> originallyInvolved = involvedUnits;
        boolean isMissionIntercepted = false;
        int totalInterceptedUnits;
        List<InterceptedUnitsInformation> interceptedUnits;
        if (!missionType.equals(MissionType.RETURN_MISSION)) {
            interceptedUnits = checkInterceptsSpeedImpactGroup(mission, involvedUnits);
            totalInterceptedUnits = interceptedUnits.stream().map(current -> current.getInterceptedUnits().size())
                    .reduce(Integer::sum).orElse(0);
            isMissionIntercepted = totalInterceptedUnits == involvedUnits.size();
            if (totalInterceptedUnits > 0) {
                deleteInterceptedUnits(interceptedUnits);
                involvedUnits = findUnitsInvolved(missionId);
            }
        } else {
            totalInterceptedUnits = 0;
            interceptedUnits = null;
        }

        if (!isMissionIntercepted) {
            UnitMissionReportBuilder reportBuilder = null;
            switch (missionType) {
                case EXPLORE:
                    reportBuilder = processExplore(mission, involvedUnits);
                    break;
                case RETURN_MISSION:
                    proccessReturnMission(mission);
                    break;
                case GATHER:
                    reportBuilder = processGather(mission, involvedUnits);
                    break;
                case ESTABLISH_BASE:
                    reportBuilder = processEstablishBase(mission, involvedUnits);
                    break;
                case ATTACK:
                    reportBuilder = processAttack(mission, true).reportBuilder;
                    break;
                case COUNTERATTACK:
                    reportBuilder = processCounterattack(mission);
                    break;
                case CONQUEST:
                    reportBuilder = processConquest(mission, involvedUnits);
                    break;
                case DEPLOY:
                    proccessDeploy(missionId);
                    break;
                default:
                    LOG.warn("Not an unit mission");
            }
            if (totalInterceptedUnits != 0 && reportBuilder != null) {
                reportBuilder.withInvolvedUnits(originallyInvolved);
                reportBuilder.withInterceptionInformation(interceptedUnits);
            }
            if (reportBuilder != null) {
                handleMissionReportSave(mission, reportBuilder);
            }
        } else {
            handleMissionInterception(mission, originallyInvolved, interceptedUnits);
            emitLocalMissionChangeAfterCommit(mission);
        }
    }

    /**
     * Parses the exploration of a planet
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private UnitMissionReportBuilder processExplore(Mission mission, List<ObtainedUnit> involvedUnits) {
        UserStorage user = mission.getUser();
        Planet targetPlanet = mission.getTargetPlanet();
        if (!planetBo.isExplored(user, targetPlanet)) {
            planetBo.defineAsExplored(user, targetPlanet);
        }
        List<ObtainedUnitDto> unitsInPlanet = obtainedUnitBo.explorePlanetUnits(mission, targetPlanet);
        adminRegisterReturnMission(mission);
        UnitMissionReportBuilder builder = UnitMissionReportBuilder
                .create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
                .withExploredInformation(unitsInPlanet);
        resolveMission(mission);
        return builder;
    }

    private boolean isEnemy(UserStorage asker, UserStorage target) {
        return !asker.getId().equals(target.getId())
                && filterAlliance(asker, target);
    }

    private UnitMissionReportBuilder processGather(Mission mission, List<ObtainedUnit> involvedUnits) {
        UserStorage user = mission.getUser();
        var faction = user.getFaction();
        var targetPlanet = mission.getTargetPlanet();
        boolean continueMission = triggerAttackIfRequired(mission, user, targetPlanet);
        if (continueMission) {
            adminRegisterReturnMission(mission);
            Long gathered = involvedUnits.stream()
                    .map(current -> ObjectUtils.firstNonNull(current.getUnit().getCharge(), 0) * current.getCount())
                    .reduce(0L, Long::sum);
            double withPlanetRichness = gathered * targetPlanet.findRationalRichness();
            var groupedImprovement = improvementBo.findUserImprovement(user);
            double withUserImprovement = withPlanetRichness
                    + (withPlanetRichness * improvementBo.findAsRational(groupedImprovement.getMoreChargeCapacity()));
            var customPrimary = faction.getCustomPrimaryGatherPercentage();
            var customSecondary = faction.getCustomSecondaryGatherPercentage();
            double primaryResource;
            double secondaryResource;
            if (customPrimary != null && customSecondary != null && customPrimary > 0 && customSecondary > 0) {
                primaryResource = withUserImprovement * (customPrimary / 100);
                secondaryResource = withUserImprovement * (customSecondary / 100);
            } else {
                primaryResource = withUserImprovement * 0.5;
                secondaryResource = withUserImprovement * 0.5;
            }
            user.addtoPrimary(primaryResource);
            user.addToSecondary(secondaryResource);
            UnitMissionReportBuilder builder = UnitMissionReportBuilder
                    .create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
                    .withGatherInformation(primaryResource, secondaryResource);
            TransactionUtil.doAfterCommit(() -> socketIoService.sendMessage(user, "mission_gather_result", () -> {
                Map<String, Double> content = new HashMap<>();
                content.put("primaryResource", primaryResource);
                content.put("secondaryResource", secondaryResource);
                return content;
            }));
            resolveMission(mission);
            return builder;
        } else {
            return null;
        }
    }

    private UnitMissionReportBuilder processEstablishBase(Mission mission, List<ObtainedUnit> involvedUnits) {
        UserStorage user = mission.getUser();
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
            resolveMission(mission);
            emitLocalMissionChangeAfterCommit(mission);
            return builder;
        } else {
            return null;
        }
    }

    private AttackInformation processAttack(Mission mission, boolean survivorsDoReturn) {
        var targetPlanet = mission.getTargetPlanet();
        var attackInformation = buildAttackInformation(targetPlanet, mission);
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
                attackInformation.users.values().stream().map(attackUserInformation -> attackUserInformation.user)
                        .filter(user -> !user.getId().equals(invoker.getId())).collect(Collectors.toList()));
        attackInformation.users.entrySet().stream()
                .map(userEntry -> userEntry.getValue().user)
                .filter(user -> !mission.getUser().getId().equals(user.getId()))
                .forEach(user -> auditBo.nonRequestAudit(AuditActionEnum.ATTACK_INTERACTION, null, mission.getUser(), user.getId()));
        if (attackInformation.isMissionRemoved()) {
            emitLocalMissionChangeAfterCommit(mission);
        }
        var owner = targetPlanet.getOwner();
        if (owner != null && !attackInformation.usersWithDeletedMissions.isEmpty()) {
            emitEnemyMissionsChange(owner);
        }
        attackInformation.units.stream().distinct().forEach(this::triggerUnitRequirementChange);
        attackInformation.reportBuilder = builder;
        return attackInformation;
    }

    private void triggerUnitRequirementChange(AttackObtainedUnit attackObtainedUnit) {
        var user = attackObtainedUnit.obtainedUnit.getUser();
        var unit = attackObtainedUnit.obtainedUnit.getUnit();
        if (attackObtainedUnit.finalCount.equals(0L)) {
            requirementBo.triggerUnitBuildCompletedOrKilled(user, unit);
        } else if (!attackObtainedUnit.finalCount.equals(attackObtainedUnit.initialCount)) {
            requirementBo.triggerUnitAmountChanged(user, unit);
        }

    }

    /**
     * Executes the counterattack logic <br>
     * <b>NOTICE: </b> For now the current implementation just calls the
     * processAttack()
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private UnitMissionReportBuilder processCounterattack(Mission mission) {
        return processAttack(mission, true).reportBuilder;
    }

    private void handleMissionInterception(Mission mission, List<ObtainedUnit> involved, List<InterceptedUnitsInformation> interceptedUnitsInformations) {
        resolveMission(mission);
        reportFullMissionInterception(mission, involved, interceptedUnitsInformations);
        deleteInterceptedUnits(interceptedUnitsInformations);
    }

    private void deleteInterceptedUnits(List<InterceptedUnitsInformation> interceptedUnitsInformations) {
        interceptedUnitsInformations.stream().map(interception -> List.copyOf(interception.getInterceptedUnits())).forEach(obtainedUnitBo::delete);
    }

    private void reportFullMissionInterception(Mission mission, List<ObtainedUnit> involved, List<InterceptedUnitsInformation> interceptedUnits) {
        UnitMissionReportBuilder builder = UnitMissionReportBuilder.create(mission.getUser(), mission.getSourcePlanet(),
                mission.getTargetPlanet(), involved).withInterceptionInformation(interceptedUnits);
        handleMissionReportSave(mission, builder);
    }

    private List<InterceptedUnitsInformation> checkInterceptsSpeedImpactGroup(Mission mission,
                                                                              List<ObtainedUnit> involvedUnits) {
        Set<ObtainedUnit> alreadyIntercepted = new HashSet<>();
        Map<Integer, InterceptedUnitsInformation> interceptedMap = new HashMap<>();
        List<ObtainedUnit> unitsWithInterception = obtainedUnitBo.findInvolvedInAttack(mission.getTargetPlanet())
                .stream().filter(current -> !current.getUnit().getInterceptableSpeedGroups().isEmpty())
                .collect(Collectors.toList());
        unitsWithInterception.forEach(unitWithInterception -> involvedUnits.stream().filter(
                involved -> canIntercept(unitWithInterception.getUnit().getInterceptableSpeedGroups(), involved))
                .filter(involved -> !alreadyIntercepted.contains(involved) && isEnemy(unitWithInterception.getUser(), involved.getUser()))
                .forEach(interceptedUnit -> {
                    UserStorage interceptorUser = unitWithInterception.getUser();
                    Integer interceptorUserId = interceptorUser.getId();
                    if (!interceptedMap.containsKey(interceptorUserId)) {
                        interceptedMap.put(interceptorUserId, new InterceptedUnitsInformation(
                                unitWithInterception.getUser(), unitWithInterception, new HashSet<>()));
                    }
                    interceptedMap.get(interceptorUserId).getInterceptedUnits().add(interceptedUnit);
                    alreadyIntercepted.add(interceptedUnit);
                }));
        return new ArrayList<>(interceptedMap.values());
    }

    private boolean canIntercept(List<InterceptableSpeedGroup> interceptableSpeedGroups, ObtainedUnit obtainedUnit) {
        SpeedImpactGroup speedImpactGroup = obtainedUnit.getUnit().getSpeedImpactGroup();
        return speedImpactGroup != null && interceptableSpeedGroups.stream().anyMatch(current -> current.getSpeedImpactGroup().getId()
                .equals(obtainedUnit.getUnit().getSpeedImpactGroup().getId()));
    }

    /**
     * Due to lack of support from Quartz to access spring context from the
     * EntityListener of {@link ImageStoreListener} we have to invoke the image URL
     * computation from here
     *
     * @author Kevin Guanche Darias
     * @since 0.9.0
     */
    private List<ObtainedUnit> findUnitsInvolved(Long missionId) {
        List<ObtainedUnit> retVal = obtainedUnitBo.findByMissionId(missionId);
        retVal.forEach(current -> imageStoreBo.computeImageUrl(current.getUnit().getImage()));
        return retVal;
    }

    /**
     * Executes modifications to <i>missionInformation</i> to define the logged in
     * user as the sender user
     *
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
        var user = userStorageBo.findLoggedIn();
        var isDeployMission = missionType.equals(MissionType.DEPLOY);
        if (!isDeployMission || !planetBo.isOfUserProperty(user.getId(), missionInformation.getTargetPlanetId())) {
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
        var mission = missionRepository.saveAndFlush((prepareMission(targetMissionInformation, missionType)));
        auditMissionRegistration(mission, isDeployMission);
        targetMissionInformation.getInvolvedUnits().forEach(current -> {
            var currentObtainedUnit = new ObtainedUnit();
            currentObtainedUnit.setMission(mission);
            var firstDeploymentMission = dbUnits.get(current.getId()).getFirstDeploymentMission();
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
        handleMissionTimeCalculation(obtainedUnits, mission, missionType);
        mission.setInvisible(
                obtainedUnits.stream().allMatch(current -> Boolean.TRUE.equals(current.getUnit().getIsInvisible())));
        save(mission);
        scheduleMission(mission);
        emitLocalMissionChangeAfterCommit(mission);
        TransactionUtil.doAfterCommit(() -> socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
                () -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(userId))));
    }

    /**
     * Alters the mission and adds the required time and the termination date
     */
    private void handleMissionTimeCalculation(List<ObtainedUnit> obtainedUnits, Mission mission, MissionType missionType) {
        if (!obtainedUnits.stream().allMatch(obtainedUnit -> obtainedUnit.getUnit().getSpeedImpactGroup() != null
                && obtainedUnit.getUnit().getSpeedImpactGroup().getIsFixed())) {
            var user = mission.getUser();
            Optional<Double> lowestSpeedOptional = obtainedUnits.stream().map(ObtainedUnit::getUnit)
                    .filter(unit -> unit.getSpeed() != null && unit.getSpeed() > 0.000D
                            && (unit.getSpeedImpactGroup() == null || !unit.getSpeedImpactGroup().getIsFixed()))
                    .map(Unit::getSpeed).reduce((a, b) -> a > b ? b : a);
            if (lowestSpeedOptional.isPresent()) {
                double lowestSpeed = lowestSpeedOptional.get();
                var unitType = obtainedUnits.stream()
                        .map(ObtainedUnit::getUnit)
                        .filter(unit -> lowestSpeed == unit.getSpeed())
                        .map(Unit::getType)
                        .findFirst()
                        .orElseThrow(() -> new ProgrammingException("Should never ever happend, you know"));
                var improvement = improvementBo.findUserImprovement(user);
                var speedWithImprovement = lowestSpeed + (lowestSpeed * improvementBo.findAsRational(
                        (double) improvement.findUnitTypeImprovement(ImprovementTypeEnum.SPEED, unitType)));
                double missionTypeTime = calculateRequiredTime(missionType);
                double requiredTime = calculateTimeUsingSpeed(mission, missionType, missionTypeTime, speedWithImprovement);
                mission.setRequiredTime(requiredTime);
                mission.setTerminationDate(computeTerminationDate(mission.getRequiredTime()));
            }
        }
    }

    private void auditMissionRegistration(Mission mission, boolean isDeploy) {
        var planetOwner = mission.getTargetPlanet().getOwner();
        if (planetOwner == null || planetOwner.getId().equals(mission.getUser().getId())) {
            auditBo.doAudit(AuditActionEnum.REGISTER_MISSION, mission.getType().getCode(), null);
        } else {
            auditBo.doAudit(AuditActionEnum.REGISTER_MISSION, mission.getType().getCode(), planetOwner.getId());
        }
        if (isDeploy) {
            obtainedUnitBo.findInPlanetOrInMissiontoPlanet(mission.getTargetPlanet()).stream()
                    .filter(unit -> !unit.getUser().getId().equals(mission.getUser().getId()))
                    .map(ObtainedUnit::getUser)
                    .distinct()
                    .forEach(unitUser -> auditBo.doAudit(AuditActionEnum.USER_INTERACTION, "DEPLOY", unitUser.getId()));
        }
    }

    private double calculateTimeUsingSpeed(Mission mission, MissionType missionType, double missionTypeTime,
                                           double lowestUnitSpeed) {
        int missionTypeDivisor = findMissionTypeDivisor(missionType);
        missionTypeDivisor = missionTypeDivisor == 0 ? 1 : missionTypeDivisor;
        int leftMultiplier = findSpeedLeftMultiplier(mission, missionType);
        float moveCost = calculateMoveCost(missionType, mission.getSourcePlanet(), mission.getTargetPlanet());
        double retVal = missionTypeTime + ((leftMultiplier * moveCost) * (100 - lowestUnitSpeed)) / missionTypeDivisor;
        return Math.max(missionTypeTime, retVal);
    }

    /**
     * Finds the speed left multiplier <b>also known as the "mission penalty"</b>
     * which depends of the mission type and if it's on different quadrant
     */
    private int findSpeedLeftMultiplier(Mission mission, MissionType missionType) {
        final String prefix = "MISSION_SPEED_";
        String missionTypeName = missionType.name();
        Long sourceQuadrant = mission.getSourcePlanet().getQuadrant();
        Long targetQuadrant = mission.getTargetPlanet().getQuadrant();
        Long sourceSector = mission.getSourcePlanet().getSector();
        Long targetSector = mission.getTargetPlanet().getSector();
        Integer sourceGalaxy = mission.getSourcePlanet().getGalaxy().getId();
        Integer targetGalaxy = mission.getTargetPlanet().getGalaxy().getId();
        int defaultMultiplier;
        String configurationName;
        if (sourceQuadrant.equals(targetQuadrant) && sourceSector.equals(targetSector)
                && sourceGalaxy.equals(targetGalaxy)) {
            configurationName = prefix + missionTypeName + "_SAME_Q";
            defaultMultiplier = 50;
        } else if (!sourceGalaxy.equals(targetGalaxy)) {
            configurationName = prefix + missionTypeName + "_DIFF_G";
            defaultMultiplier = 2000;
        } else if (!sourceSector.equals(targetSector)) {
            configurationName = prefix + missionTypeName + "_DIFF_S";
            defaultMultiplier = 200;
        } else {
            configurationName = prefix + missionTypeName + "_DIFF_Q";
            defaultMultiplier = 100;
        }
        return NumberUtils.toInt(
                configurationBo.findOrSetDefault(configurationName, String.valueOf(defaultMultiplier)).getValue(),
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
     * @return Database list of <i>ObtainedUnit</i> with the subtraction <b>already
     * applied</b>, whose key is the "unit" id (don't confuse with obtained
     * unit id)
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
        deletedMissions.stream().filter(mission -> unitsInMissionsAfterDelete.stream()
                .noneMatch(unit -> mission.getId().equals(unit.getMission() != null ? unit.getMission().getId() : null))

        ).forEach(this::resolveMission);
        return retVal;
    }

    /**
     * Checks if the current obtained unit can do deploy (if already deployed in
     * some cases, cannot)
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
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
     * @throws SgtBackendInvalidInputException If the deployment mission is
     *                                         <b>globally</b> disabled
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
     */
    private void checkDeployedAllowed(MissionType missionType) {
        if (missionType == MissionType.DEPLOY
                && configurationBo.findDeployMissionConfiguration().equals(DeployMissionConfigurationEnum.DISALLOWED)) {
            throw new SgtBackendInvalidInputException("The deployment mission is globally disabled");
        }
    }

    /**
     * Returns a copy of the object, used to make missionInformation immutable
     *
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private Double calculateRequiredTime(MissionType type) {
        return (double) configurationBo.findMissionBaseTimeByType(type);
    }

    /**
     * Emits a local mission change to the target user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private void emitLocalMissionChangeAfterCommit(Mission mission) {
        UserStorage user = mission.getUser();
        TransactionUtil.doAfterCommit(() -> emitLocalMissionChange(mission, user));
    }

    private void emitLocalMissionChange(Mission mission, UserStorage user) {
        emitLocalMissionChange(mission, user.getId());
    }

    private void emitLocalMissionChange(Mission mission, Integer userId) {
        entityManager.refresh(mission);
        if (Boolean.FALSE.equals(mission.getInvisible())) {
            emitEnemyMissionsChange(mission);
        }
        emitMissions(userId);
    }

    private void emitEnemyMissionsChange(UserStorage user) {
        socketIoService.sendMessage(user, ENEMY_MISSION_CHANGE, () -> findEnemyRunningMissions(user));

    }

    private AttackInformation buildAttackInformation(Planet targetPlanet, Mission attackMission) {
        AttackInformation retVal = new AttackInformation(attackMission, targetPlanet);
        obtainedUnitBo.findInvolvedInAttack(targetPlanet).forEach(unit -> {
            if (!attackMission.equals(unit.getMission())) {
                retVal.addUnit(unit);
            }
        });
        obtainedUnitBo.findByMissionId(attackMission.getId()).forEach(retVal::addUnit);
        return retVal;
    }

    /**
     * Defines the new owner for the targetPlanet
     *
     * @param owner         The new owner
     * @param involvedUnits The units used by the owner to conquest the planet
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

        TransactionUtil.doAfterCommit(() -> planetListBo.emitByChangedPlanet(targetPlanet));
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
        return (positionInQuadrant * planetDiff) + (quadrants * quadrantDiff) + (sectors * sectorDiff)
                + (!targetPlanet.getGalaxy().getId().equals(sourcePlanet.getGalaxy().getId()) ? galaxyDiff : 0);
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
     * @return True if should continue the mission
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private boolean triggerAttackIfRequired(Mission mission, UserStorage user, Planet targetPlanet) {
        boolean continueMission = true;
        if (isAttackTriggerEnabledForMission(MissionType.valueOf(mission.getType().getCode()))
                && obtainedUnitBo.areUnitsInvolved(user, targetPlanet)) {
            AttackInformation result = processAttack(mission, false);
            continueMission = !result.isMissionRemoved();
        }
        return continueMission;
    }

    private int findMissionTypeDivisor(MissionType missionType) {
        return Integer.parseInt(
                configurationBo.findOrSetDefault("MISSION_SPEED_DIVISOR_" + missionType.name(), "1").getValue());
    }

    private boolean isAttackTriggerEnabledForMission(MissionType missionType) {
        return Boolean.parseBoolean(configurationBo
                .findOrSetDefault("MISSION_" + missionType.name() + "_TRIGGER_ATTACK", "FALSE").getValue());
    }

    /**
     * If the user has an alliance, removes all those users that are not in the user
     * alliance
     *
     * @return True if:
     * <ul>
     *   <li>The <i>source</i> user has not an alliance</li>
     *   <li>The <i>target</i>> user has not an alliance</li>
     *   <li>The source alliance is different than the target alliance</li>
     *   </ul>
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.0
     */
    private boolean filterAlliance(UserStorage source, UserStorage target) {
        return source.getAlliance() == null || target.getAlliance() == null
                || !source.getAlliance().getId().equals(target.getAlliance().getId());
    }
}
