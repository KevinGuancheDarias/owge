package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.mission.attack.AttackMissionManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.checker.CrossGalaxyMissionChecker;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.Unit;
import com.kevinguanchedarias.owgejava.entity.UnitType;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.entity.listener.ImageStoreListener;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.enumerations.DeployMissionConfigurationEnum;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.UserNotFoundException;
import com.kevinguanchedarias.owgejava.pojo.InterceptedUnitsInformation;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackUserInformation;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import javax.persistence.EntityManager;
import java.io.Serial;
import java.time.Instant;
import java.util.ArrayList;
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
    public static final String JOB_GROUP_NAME = "UnitMissions";

    @Serial
    private static final long serialVersionUID = 344402831344882216L;

    private static final Logger LOG = Logger.getLogger(UnitMissionBo.class);
    private static final String MAX_PLANETS_MESSAGE = "I18N_MAX_PLANETS_EXCEEDED";

    @Autowired
    private ConfigurationBo configurationBo;

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

    @Autowired
    private transient AttackMissionManagerBo attackMissionManagerBo;

    @Autowired
    private ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    private AllianceBo allianceBo;

    @Autowired
    private transient TransactionUtilService transactionUtilService;

    @Autowired
    private SpeedImpactGroupBo speedImpactGroupBo;

    @Autowired
    private transient HiddenUnitBo hiddenUnitBo;

    @Autowired
    private transient PlanetLockUtilService planetLockUtilService;

    @Autowired
    private transient CrossGalaxyMissionChecker crossGalaxyMissionChecker;

    @Override
    public String getGroupName() {
        return JOB_GROUP_NAME;
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
        planetLockUtilService.doInsideLock(List.of(mission.getSourcePlanet(), mission.getTargetPlanet()),
                () -> {
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
        );
    }

    @Transactional
    public void processReturnMission(Mission mission) {
        planetLockUtilService.doInsideLock(List.of(mission.getSourcePlanet(), mission.getTargetPlanet()), () -> {
            Integer userId = mission.getUser().getId();
            List<ObtainedUnit> obtainedUnits = obtainedUnitBo.findByMissionId(mission.getId());
            obtainedUnits.forEach(current -> obtainedUnitBo.moveUnit(current, userId, mission.getSourcePlanet().getId()));
            resolveMission(mission);
            emitLocalMissionChangeAfterCommit(mission);
            asyncRunnerBo
                    .runAssyncWithoutContextDelayed(
                            () -> socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
                                    () -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(userId))),
                            500);
        });
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
                    || attackInformation.getUsers().get(oldOwner.getId()).getUnits().stream()
                    .noneMatch(current -> current.getFinalCount() > 0L);
            isAllianceDefeated = isOldOwnerDefeated
                    && (oldOwner.getAlliance() == null || attackInformation.getUsers().entrySet().stream()
                    .filter(attackedUser -> attackedUser.getValue().getUser().getAlliance() != null
                            && attackedUser.getValue().getUser().getAlliance().equals(oldOwner.getAlliance()))
                    .allMatch(currentUser -> currentUser.getValue().getUnits().stream()
                            .noneMatch(currentUserUnit -> currentUserUnit.getFinalCount() > 0L)));
        }

        if (!isOldOwnerDefeated || !isAllianceDefeated || maxPlanets || planetBo.isHomePlanet(targetPlanet)) {
            if (!attackInformation.isRemoved()) {
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
                missionBo.emitEnemyMissionsChange(oldOwner);
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
    public void processDeploy(Mission mission) {
        long missionId = mission.getId();
        UserStorage user = mission.getUser();
        Integer userId = user.getId();
        List<ObtainedUnit> alteredUnits = new ArrayList<>();
        findUnitsInvolved(missionId).forEach(current ->
                alteredUnits.add(obtainedUnitBo.moveUnit(current, userId, mission.getTargetPlanet().getId()))
        );

        var deployedMission = alteredUnits.get(0).getMission();
        if (deployedMission != null) {
            deployedMission.setInvisible(deployedMission.getInvolvedUnits().stream().allMatch(hiddenUnitBo::isHiddenUnit));
        }

        resolveMission(mission);
        transactionUtilService.doAfterCommit(() -> {
            alteredUnits.forEach(entityManager::refresh);
            if (user.equals(mission.getTargetPlanet().getOwner())) {
                socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
                        () -> obtainedUnitBo.toDto(obtainedUnitBo.findDeployedInUserOwnedPlanets(userId)));
            }
            emitLocalMissionChange(mission, user);
        });

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
            long nowMillis = Instant.now().toEpochMilli();
            long terminationMillis = mission.getTerminationDate().getTime();
            var durationMillis = 0L;
            if (terminationMillis >= nowMillis) {
                durationMillis = (long) ((terminationMillis - nowMillis) / 1000D);
            }
            adminRegisterReturnMission(mission, mission.getRequiredTime() - durationMillis);
        }
    }

    public List<ObtainedUnit> findInvolvedInMission(Mission mission) {
        return obtainedUnitBo.findByMissionId(mission.getId());
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
            missionBo.emitEnemyMissionsChange(targetPlanetOwner);
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
        planetLockUtilService.doInsideLock(
                List.of(mission.getSourcePlanet(), mission.getTargetPlanet()),
                () -> doRunUnitMission(findById(missionId), missionType)
        );
    }

    private void doRunUnitMission(Mission mission, MissionType missionType) {
        var missionId = mission.getId();
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
                    processReturnMission(mission);
                    break;
                case GATHER:
                    reportBuilder = processGather(mission, involvedUnits);
                    break;
                case ESTABLISH_BASE:
                    reportBuilder = processEstablishBase(mission, involvedUnits);
                    break;
                case ATTACK:
                    reportBuilder = processAttack(mission, true).getReportBuilder();
                    break;
                case COUNTERATTACK:
                    reportBuilder = processCounterattack(mission);
                    break;
                case CONQUEST:
                    reportBuilder = processConquest(mission, involvedUnits);
                    break;
                case DEPLOY:
                    processDeploy(mission);
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
                && allianceBo.areEnemies(asker, target);
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
        AttackInformation attackInformation = attackMissionManagerBo.buildAttackInformation(targetPlanet, mission);
        attackMissionManagerBo.startAttack(attackInformation);
        if (survivorsDoReturn && !attackInformation.isRemoved()) {
            adminRegisterReturnMission(mission);
        }
        resolveMission(mission);
        UnitMissionReportBuilder builder = UnitMissionReportBuilder
                .create(mission.getUser(), mission.getSourcePlanet(), targetPlanet, new ArrayList<>())
                .withAttackInformation(attackInformation);
        UserStorage invoker = mission.getUser();
        handleMissionReportSave(mission, builder, true,
                attackInformation.getUsers().values().stream().map(AttackUserInformation::getUser)
                        .filter(user -> !user.getId().equals(invoker.getId())).collect(Collectors.toList()));
        attackInformation.getUsers().entrySet().stream()
                .map(userEntry -> userEntry.getValue().getUser())
                .filter(user -> !mission.getUser().getId().equals(user.getId()))
                .forEach(user -> auditBo.nonRequestAudit(AuditActionEnum.ATTACK_INTERACTION, null, mission.getUser(), user.getId()));
        var owner = targetPlanet.getOwner();
        if (attackInformation.isRemoved() || owner != null && !attackInformation.getUsersWithDeletedMissions().isEmpty()) {
            emitLocalMissionChangeAfterCommit(mission);
        }
        attackInformation.getUnits().stream().distinct().forEach(this::triggerUnitRequirementChange);
        attackInformation.setReportBuilder(builder);
        return attackInformation;
    }

    private void triggerUnitRequirementChange(AttackObtainedUnit attackObtainedUnit) {
        var user = attackObtainedUnit.getObtainedUnit().getUser();
        var unit = attackObtainedUnit.getObtainedUnit().getUnit();
        if (attackObtainedUnit.getFinalCount().equals(0L)) {
            requirementBo.triggerUnitBuildCompletedOrKilled(user, unit);
        } else if (!attackObtainedUnit.getFinalCount().equals(attackObtainedUnit.getInitialCount())) {
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
        return processAttack(mission, true).getReportBuilder();
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
                .stream().filter(current -> !CollectionUtils.isEmpty(current.getUnit().getInterceptableSpeedGroups()))
                .toList();
        unitsWithInterception.forEach(unitWithInterception -> involvedUnits.stream().filter(
                        involved -> speedImpactGroupBo.canIntercept(unitWithInterception.getUnit().getInterceptableSpeedGroups(), involved.getUnit()))
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
        planetLockUtilService.doInsideLockById(
                List.of(missionInformation.getSourcePlanetId(), missionInformation.getTargetPlanetId()),
                () -> doCommonMissionRegister(missionInformation, targetMissionInformation, missionType, user, isDeployMission)
        );
    }

    private void doCommonMissionRegister(
            UnitMissionInformation missionInformation,
            UnitMissionInformation targetMissionInformation,
            MissionType missionType,
            UserStorage user,
            boolean isDeployMission
    ) {
        List<ObtainedUnit> obtainedUnits = new ArrayList<>();
        Integer userId = user.getId();
        Map<Integer, ObtainedUnit> dbUnits = checkAndLoadObtainedUnits(missionInformation);
        var mission = missionRepository.saveAndFlush((prepareMission(targetMissionInformation, missionType)));
        boolean isEnemyPlanet = mission.getSourcePlanet().getOwner() != null && !user.equals(mission.getSourcePlanet().getOwner());
        auditMissionRegistration(mission, isDeployMission);
        List<Mission> alteredVisibilityMissions = new ArrayList<>();
        targetMissionInformation.getInvolvedUnits().forEach(current -> {
            var currentObtainedUnit = new ObtainedUnit();
            var dbUnit = dbUnits.get(current.getId());
            if (isEnemyPlanet) {
                alteredVisibilityMissions.add(dbUnit.getMission());
            }
            currentObtainedUnit.setMission(mission);
            var firstDeploymentMission = dbUnit.getFirstDeploymentMission();
            currentObtainedUnit.setFirstDeploymentMission(firstDeploymentMission);
            currentObtainedUnit.setCount(current.getCount());
            currentObtainedUnit.setUser(user);
            currentObtainedUnit.setUnit(unitBo.findById(current.getId()));
            currentObtainedUnit.setExpirationId(dbUnit.getExpirationId());
            currentObtainedUnit.setSourcePlanet(firstDeploymentMission == null ? mission.getSourcePlanet()
                    : firstDeploymentMission.getSourcePlanet());
            currentObtainedUnit.setTargetPlanet(mission.getTargetPlanet());
            obtainedUnits.add(currentObtainedUnit);
        });
        List<UnitType> involvedUnitTypes = obtainedUnits.stream().map(current -> current.getUnit().getType())
                .toList();
        if (!unitTypeBo.canDoMission(user, mission.getTargetPlanet(), involvedUnitTypes, missionType)) {
            throw new SgtBackendInvalidInputException(
                    "At least one unit type doesn't support the specified mission.... don't try it dear hacker, you can't defeat the system, but don't worry nobody can");
        }
        crossGalaxyMissionChecker.checkCrossGalaxy(missionType, obtainedUnits, mission.getSourcePlanet(), mission.getTargetPlanet());
        obtainedUnitBo.save(obtainedUnits);
        handleMissionTimeCalculation(obtainedUnits, mission, missionType);
        handleCustomDuration(mission, missionInformation.getWantedTime());
        mission.setInvisible(
                obtainedUnits.stream().allMatch(hiddenUnitBo::isHiddenUnit)
        );
        save(mission);
        scheduleMission(mission);
        emitLocalMissionChangeAfterCommit(mission);
        if (user.equals(mission.getSourcePlanet().getOwner())) {
            transactionUtilService.doAfterCommit(() -> socketIoService.sendMessage(userId, UNIT_OBTAINED_CHANGE,
                    () -> obtainedUnitBo.findCompletedAsDto(user)));
        }
        if (isEnemyPlanet) {
            alteredVisibilityMissions.stream()
                    .filter(current -> {
                        var oldValue = Boolean.TRUE.equals(current.getInvisible());
                        var newValue = obtainedUnitRepository.findByMissionId(current.getId()).stream().allMatch(hiddenUnitBo::isHiddenUnit);
                        current.setInvisible(newValue);
                        return oldValue != newValue;
                    })
                    .forEach(missionRepository::save);
            missionBo.emitEnemyMissionsChange(mission.getSourcePlanet().getOwner());
        }
    }

    private void handleCustomDuration(Mission mission, Long customDuration) {
        if (customDuration != null && customDuration > mission.getRequiredTime()) {
            mission.setRequiredTime(customDuration.doubleValue());
            mission.setTerminationDate(computeTerminationDate(mission.getRequiredTime()));
        }
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
                    missionInformation.getUserId(), current.getId(), sourcePlanetId, current.getExpirationId(),
                    !planetBo.isOfUserProperty(userId, sourcePlanetId));
            checkUnitCanDeploy(currentObtainedUnit, missionInformation);
            ObtainedUnit unitAfterSubtraction = obtainedUnitBo.saveWithSubtraction(currentObtainedUnit,
                    current.getCount(), false);
            if (unitAfterSubtraction == null && currentObtainedUnit.getMission() != null
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
                                                                                Long planetId, Long expirationId, boolean isDeployedMission) {

        ObtainedUnit retVal;
        if (expirationId == null) {
            retVal = isDeployedMission
                    ? obtainedUnitBo.findOneByUserIdAndUnitIdAndTargetPlanetAndMissionDeployed(userId, unitId, planetId)
                    : obtainedUnitBo.findOneByUserIdAndUnitIdAndSourcePlanetAndMissionIsNull(userId, unitId, planetId);
        } else {
            retVal = isDeployedMission
                    ? obtainedUnitRepository.findOneByUserIdAndUnitIdAndTargetPlanetIdAndExpirationIdAndMissionTypeCode(
                    userId, unitId, planetId, expirationId, MissionType.DEPLOYED.name())
                    : obtainedUnitRepository.findOneByUserIdAndUnitIdAndSourcePlanetIdAndExpirationIdAndMissionIsNull(userId, unitId, planetId, expirationId);
        }


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
        return (double) missionConfigurationBo.findMissionBaseTimeByType(type);
    }

    /**
     * Emits a local mission change to the target user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private void emitLocalMissionChangeAfterCommit(Mission mission) {
        UserStorage user = mission.getUser();
        transactionUtilService.doAfterCommit(() -> emitLocalMissionChange(mission, user));
    }

    private void emitLocalMissionChange(Mission mission, UserStorage user) {
        emitLocalMissionChange(mission, user.getId());
    }

    private void emitLocalMissionChange(Mission mission, Integer userId) {
        entityManager.refresh(mission);
        if (Boolean.FALSE.equals(mission.getInvisible())) {
            emitEnemyMissionsChange(mission);
        }
        emitUnitMissions(userId);
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

        transactionUtilService.doAfterCommit(() -> planetListBo.emitByChangedPlanet(targetPlanet));
        planetBo.emitPlanetOwnedChange(owner);
        missionBo.emitEnemyMissionsChange(owner);
        socketIoService.sendMessage(owner, AbstractMissionBo.UNIT_OBTAINED_CHANGE,
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

    private int findMissionTypeDivisor(MissionType missionType) {
        return Integer.parseInt(
                configurationBo.findOrSetDefault("MISSION_SPEED_DIVISOR_" + missionType.name(), "1").getValue());
    }

    private boolean isAttackTriggerEnabledForMission(MissionType missionType) {
        return Boolean.parseBoolean(configurationBo
                .findOrSetDefault("MISSION_" + missionType.name() + "_TRIGGER_ATTACK", "FALSE").getValue());
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
            continueMission = !result.isRemoved();
        }
        return continueMission;
    }
}
