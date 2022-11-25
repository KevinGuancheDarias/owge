package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionInterceptionManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionUnitsFinderBo;
import com.kevinguanchedarias.owgejava.business.mission.attack.AttackMissionManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.UnitMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.speedimpactgroup.UnitInterceptionFinderBo;
import com.kevinguanchedarias.owgejava.business.unit.HiddenUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.ObtainedUnitDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.ObtainedUnit;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.AuditActionEnum;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.NotFoundException;
import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.UserNotFoundException;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackInformation;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackObtainedUnit;
import com.kevinguanchedarias.owgejava.pojo.attack.AttackUserInformation;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.io.Serial;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
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
    private transient AttackMissionManagerBo attackMissionManagerBo;

    @Autowired
    private ObtainedUnitRepository obtainedUnitRepository;

    @Autowired
    private transient TransactionUtilService transactionUtilService;

    @Autowired
    private transient HiddenUnitBo hiddenUnitBo;

    @Autowired
    private transient PlanetLockUtilService planetLockUtilService;

    @Autowired
    private transient UnitInterceptionFinderBo unitInterceptionFinderBo;

    @Autowired
    private transient UnitMissionRegistrationBo unitMissionRegistrationBo;

    @Autowired
    private transient ObtainedUnitEventEmitter obtainedUnitEventEmitter;

    @Autowired
    private transient ReturnMissionRegistrationBo returnMissionRegistrationBo;

    @Autowired
    private PlanetRepository planetRepository;

    @Autowired
    private MissionEventEmitterBo missionEventEmitterBo;

    @Autowired
    private ObtainedUnitBo obtainedUnitBo;

    @Autowired
    private MissionUnitsFinderBo missionUnitsFinderBo;

    @Autowired
    private MissionInterceptionManagerBo missionInterceptionManagerBo;

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
        if (!planetRepository.isOfUserProperty(missionInformation.getUserId(), missionInformation.getTargetPlanetId())) {
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
        planetLockUtilService.doInsideLock(
                List.of(mission.getSourcePlanet(), mission.getTargetPlanet()),
                () -> returnMissionRegistrationBo.doRegisterReturnMission(mission, customRequiredTime)
        );
    }

    @Transactional
    public void processReturnMission(Mission mission) {
        planetLockUtilService.doInsideLock(List.of(mission.getSourcePlanet(), mission.getTargetPlanet()), () -> {
            Integer userId = mission.getUser().getId();
            List<ObtainedUnit> obtainedUnits = obtainedUnitRepository.findByMissionId(mission.getId());
            obtainedUnits.forEach(current -> obtainedUnitBo.moveUnit(current, userId, mission.getSourcePlanet().getId()));
            resolveMission(mission);
            emitLocalMissionChangeAfterCommit(mission);
            asyncRunnerBo
                    .runAssyncWithoutContextDelayed(
                            () -> obtainedUnitEventEmitter.emitObtainedUnits(mission.getUser()),
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
                missionEventEmitterBo.emitEnemyMissionsChange(oldOwner);
                UnitMissionReportBuilder enemyReportBuilder = UnitMissionReportBuilder
                        .create(user, mission.getSourcePlanet(), targetPlanet, involvedUnits)
                        .withConquestInformation(true, "I18N_YOUR_PLANET_WAS_CONQUISTED");
                missionReportManagerBo.handleMissionReportSave(mission, enemyReportBuilder, true, oldOwner);
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
        missionUnitsFinderBo.findUnitsInvolved(missionId).forEach(current ->
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
                obtainedUnitEventEmitter.emitObtainedUnits(user);
            }
            missionEventEmitterBo.emitLocalMissionChange(mission, user.getId());
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
            missionRepository.save(mission);
            long nowMillis = Instant.now().toEpochMilli();
            long terminationMillis = mission.getTerminationDate().toInstant(ZoneOffset.UTC).toEpochMilli();
            var durationMillis = 0L;
            if (terminationMillis >= nowMillis) {
                durationMillis = (long) ((terminationMillis - nowMillis) / 1000D);
            }
            adminRegisterReturnMission(mission, mission.getRequiredTime() - durationMillis);
        }
    }

    public List<ObtainedUnit> findInvolvedInMission(Mission mission) {
        return obtainedUnitRepository.findByMissionId(mission.getId());
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
        var interceptionInformation = missionInterceptionManagerBo.loadInformation(mission, missionType);
        if (!interceptionInformation.isMissionIntercepted()) {
            var involvedUnits = interceptionInformation.getInvolvedUnits();
            UnitMissionReportBuilder reportBuilder = null;
            switch (missionType) {
                case EXPLORE -> reportBuilder = processExplore(mission, involvedUnits);
                case RETURN_MISSION -> processReturnMission(mission);
                case GATHER -> reportBuilder = processGather(mission, involvedUnits);
                case ESTABLISH_BASE -> reportBuilder = processEstablishBase(mission, involvedUnits);
                case ATTACK -> reportBuilder = processAttack(mission, true).getReportBuilder();
                case COUNTERATTACK -> reportBuilder = processCounterattack(mission);
                case CONQUEST -> reportBuilder = processConquest(mission, involvedUnits);
                case DEPLOY -> processDeploy(mission);
                default -> LOG.warn("Not an unit mission");
            }
            missionInterceptionManagerBo.maybeAppendDataToMissionReport(mission, reportBuilder, interceptionInformation);
            if (reportBuilder != null) {
                missionReportManagerBo.handleMissionReportSave(mission, reportBuilder);
            }
        } else {
            missionInterceptionManagerBo.handleMissionInterception(mission, interceptionInformation);
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
        missionReportManagerBo.handleMissionReportSave(mission, builder, true,
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
        if (!isDeployMission || !planetRepository.isOfUserProperty(user.getId(), missionInformation.getTargetPlanetId())) {
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
                () -> unitMissionRegistrationBo.doCommonMissionRegister(
                        missionInformation, targetMissionInformation, missionType, user, isDeployMission
                )
        );
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
     * Emits a local mission change to the target user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private void emitLocalMissionChangeAfterCommit(Mission mission) {
        UserStorage user = mission.getUser();
        transactionUtilService.doAfterCommit(() -> missionEventEmitterBo.emitLocalMissionChange(mission, user.getId()));
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
        planetRepository.save(targetPlanet);
        obtainedUnitBo.findByUserIdAndTargetPlanetAndMissionTypeCode(owner.getId(), targetPlanet, MissionType.DEPLOYED)
                .forEach(units -> {
                    Mission mission = units.getMission();
                    obtainedUnitBo.moveUnit(units, owner.getId(), targetPlanet.getId());
                    if (mission != null) {
                        missionRepository.delete(mission);
                    }

                });
        if (targetPlanet.getSpecialLocation() != null) {
            requirementBo.triggerSpecialLocation(owner, targetPlanet.getSpecialLocation());
        }

        transactionUtilService.doAfterCommit(() -> planetListBo.emitByChangedPlanet(targetPlanet));
        planetBo.emitPlanetOwnedChange(owner);
        missionEventEmitterBo.emitEnemyMissionsChange(owner);
        obtainedUnitEventEmitter.emitObtainedUnits(owner);
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
