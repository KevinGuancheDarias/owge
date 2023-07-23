package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.*;
import com.kevinguanchedarias.owgejava.business.mission.cancel.MissionCancelBuildService;
import com.kevinguanchedarias.owgejava.business.planet.PlanetCheckerService;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitImprovementCalculationService;
import com.kevinguanchedarias.owgejava.business.user.UserEnergyServiceBo;
import com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.owgejava.dto.RunningUpgradeDto;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.*;
import com.kevinguanchedarias.owgejava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.owgejava.repository.UserStorageRepository;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import javax.persistence.EntityManager;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@AllArgsConstructor
public class MissionBo implements UserDeleteListener {
    public static final int MISSION_USER_DELETE_ORDER = 3;
    public static final String UNIT_BUILD_MISSION_CHANGE = "unit_build_mission_change";
    public static final String MISSION_NOT_FOUND = "Mission doesn't exists, maybe it was cancelled";
    public static final String RUNNING_UPGRADE_CHANGE = "running_upgrade_change";

    private static final Logger LOG = Logger.getLogger(MissionBo.class);
    private static final int DAYS = 60;

    private final EntityManager entityManager;
    private final ConfigurationBo configurationBo;
    private final AsyncRunnerBo asyncRunnerBo;
    private final TransactionUtilService transactionUtilService;
    private final PlanetLockUtilService planetLockUtilService;
    private final ObtainedUpgradeRepository obtainedUpgradeRepository;
    private final UserEnergyServiceBo userEnergyServiceBo;
    private final MissionTypeBo missionTypeBo;
    private final ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final MissionTimeManagerBo missionTimeManagerBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;
    private final UnitTypeBo unitTypeBo;
    private final SocketIoService socketIoService;
    private final ObjectRelationBo objectRelationBo;
    private final RequirementBo requirementBo;
    private final ObtainedUpgradeBo obtainedUpgradeBo;
    private final PlanetBo planetBo;
    private final UpgradeBo upgradeBo;
    private final UnitBo unitBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final PlanetCheckerService planetCheckerService;
    private final MissionFinderBo missionFinderBo;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final MissionCancelBuildService missionCancelBuildService;
    private final MissionRepository missionRepository;
    private final UserSessionService userSessionService;
    private final UserStorageRepository userStorageRepository;
    private final ImprovementBo improvementBo;
    private final MissionSchedulerService missionSchedulerService;
    private final MissionBaseService missionBaseService;
    private final UserEventEmitterBo userEventEmitterBo;

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        deleteOldMissions();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void deleteOldMissions() {
        var limitDate = LocalDateTime.now(ZoneOffset.UTC).minusDays(DAYS);
        transactionUtilService.runWithRequired(() ->
                missionRepository.findByResolvedTrueAndTerminationDateLessThan(limitDate)
                        .forEach(mission -> {
                            mission.getLinkedRelated().forEach(linked -> linked.setRelatedMission(null));
                            missionRepository.delete(mission);
                        })
        );
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
        var obtainedUpgrade = obtainedUpgradeRepository.findOneByUserIdAndUpgradeId(userId, upgradeId);
        checkUpgradeIsAvailable(obtainedUpgrade);

        var user = SpringRepositoryUtil.findByIdOrDie(userStorageRepository, userId);
        missionBaseService.checkMissionLimitNotReached(user);
        ResourceRequirementsPojo resourceRequirements = upgradeBo.calculateRequirementsAreMet(obtainedUpgrade);
        if (!resourceRequirements.canRun(user, userEnergyServiceBo)) {
            throw new SgtMissionRegistrationException("No enough resources!");
        }
        if (configurationBo.findOrSetDefault("ZERO_UPGRADE_TIME", "TRUE").getValue().equals("TRUE")) {
            resourceRequirements.setRequiredTime(3D);
        } else {
            resourceRequirements
                    .setRequiredTime(improvementBo.computeImprovementValue(resourceRequirements.getRequiredTime(),
                            improvementBo.findUserImprovement(user).getMoreUpgradeResearchSpeed(), false));
        }
        var relation = objectRelationBo.findOne(ObjectEnum.UPGRADE,
                obtainedUpgrade.getUpgrade().getId());

        var missionInformation = new MissionInformation();
        missionInformation.setRelation(relation);
        missionInformation.setValue(obtainedUpgrade.getLevel() + 1);

        var mission = new Mission();
        mission.setStartingDate(LocalDateTime.now(ZoneOffset.UTC));
        mission.setMissionInformation(missionInformation);
        attachRequirementsToMission(mission, resourceRequirements);
        mission.setType(missionTypeBo.find(MissionType.LEVEL_UP));
        mission.setUser(user);
        missionInformation.setMission(mission);

        substractResources(user, mission);

        userStorageRepository.save(user);
        missionRepository.save(mission);
        missionSchedulerService.scheduleMission(mission);
        transactionUtilService.doAfterCommit(() -> {
            entityManager.refresh(mission);
            emitRunningUpgrade(user);
            emitMissionCountChange(userId);
            userEventEmitterBo.emitUserData(user);
        });
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    public void emitRunningUpgrade(UserStorage user) {
        socketIoService.sendMessage(user, RUNNING_UPGRADE_CHANGE, () -> findRunningLevelUpMission(user.getId()));
    }

    /**
     * Process the effects of leveling up an upgrade
     *
     * @param missionId Id of the mission to process
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void processLevelUpAnUpgrade(Long missionId) {
        var mission = missionRepository.findById(missionId).orElse(null);
        if (mission != null) {
            var missionInformation = mission.getMissionInformation();
            var upgrade = (Upgrade) objectRelationBo.unboxObjectRelation(missionInformation.getRelation());
            UserStorage user = mission.getUser();
            Integer userId = user.getId();
            var obtainedUpgrade = obtainedUpgradeRepository.findOneByUserIdAndUpgradeId(userId, upgrade.getId());
            obtainedUpgrade.setLevel(missionInformation.getValue().intValue());
            obtainedUpgradeRepository.save(obtainedUpgrade);
            requirementBo.triggerLevelUpCompleted(user, upgrade.getId());
            improvementBo.clearSourceCache(user, obtainedUpgradeBo);
            improvementBo.triggerChange(userId, obtainedUpgrade.getUpgrade().getImprovement());
            missionRepository.delete(mission);
            transactionUtilService.doAfterCommit(() -> {
                entityManager.refresh(obtainedUpgrade);
                socketIoService.sendMessage(user, RUNNING_UPGRADE_CHANGE, () -> null);
                obtainedUpgradeBo.emitObtainedChange(userId);
                emitMissionCountChange(userId);
            });
        } else {
            LOG.debug(MISSION_NOT_FOUND);
        }
    }

    /**
     * Creates a mission of type unit build
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public RunningUnitBuildDto registerBuildUnit(Integer userId, Long planetId, Integer unitId, Long count) {
        planetCheckerService.myCheckIsOfUserProperty(planetId);
        checkUnitBuildMissionDoesNotExists(userId, planetId);
        var relation = objectRelationBo.findOne(ObjectEnum.UNIT,
                unitId);
        checkUnlockedUnit(userId, relation);
        var user = SpringRepositoryUtil.findByIdOrDie(userStorageRepository, userId);
        missionBaseService.checkMissionLimitNotReached(user);
        var unit = unitBo.findByIdOrDie(unitId);
        Long finalCount = Boolean.TRUE.equals(unit.getIsUnique()) ? 1 : count;
        unitBo.checkIsUniqueBuilt(user, unit);
        ResourceRequirementsPojo resourceRequirements = unitBo.calculateRequirements(unit, finalCount);
        if (!resourceRequirements.canRun(user, userEnergyServiceBo)) {
            throw new SgtMissionRegistrationException("No enough resources!");
        }
        resourceRequirements
                .setRequiredTime(improvementBo.computeImprovementValue(resourceRequirements.getRequiredTime(),
                        improvementBo.findUserImprovement(user).getMoreUnitBuildSpeed(), false));
        unitTypeBo.checkWouldReachUnitTypeLimit(user, unit.getType().getId(), finalCount);
        var missionInformation = new MissionInformation();
        missionInformation.setRelation(relation);
        missionInformation.setValue(planetId.doubleValue());

        var mission = new Mission();
        mission.setStartingDate(LocalDateTime.now(ZoneOffset.UTC));
        mission.setMissionInformation(missionInformation);
        if (configurationBo.findOrSetDefault("ZERO_BUILD_TIME", "TRUE").getValue().equals("TRUE")) {
            resourceRequirements.setRequiredTime(3D);
        }
        attachRequirementsToMission(mission, resourceRequirements);

        mission.setType(missionTypeBo.find(MissionType.BUILD_UNIT));
        mission.setUser(user);
        missionInformation.setMission(mission);

        substractResources(user, mission);

        userStorageRepository.save(user);
        missionRepository.save(mission);

        var obtainedUnit = new ObtainedUnit();
        obtainedUnit.setMission(mission);
        obtainedUnit.setCount(finalCount);
        obtainedUnit.setUnit(unit);
        obtainedUnit.setUser(user);
        obtainedUnitRepository.save(obtainedUnit);

        missionSchedulerService.scheduleMission(mission);

        transactionUtilService.doAfterCommit(() -> {
            entityManager.refresh(obtainedUnit);
            entityManager.refresh(mission);
            emitMissionCountChange(userId);
            missionEventEmitterBo.emitUnitBuildChange(userId);
            unitTypeBo.emitUserChange(userId);
            userEventEmitterBo.emitUserData(user);
        });

        return new RunningUnitBuildDto(unit, mission, planetBo.findById(planetId), finalCount);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    private void emitMissionCountChange(Integer userId) {
        missionEventEmitterBo.emitMissionCountChange(userId);
    }

    public RunningUpgradeDto findRunningLevelUpMission(Integer userId) {
        var mission = missionRepository.findOneByUserIdAndTypeCode(userId, MissionType.LEVEL_UP.name());
        if (mission != null) {
            var missionInformation = mission.getMissionInformation();
            var upgrade = (Upgrade) objectRelationBo.unboxObjectRelation(missionInformation.getRelation());
            if (upgrade.getImprovement() != null) {
                Hibernate.initialize(upgrade.getImprovement());
            }
            return new RunningUpgradeDto(upgrade, mission);
        } else {
            return null;
        }
    }

    @Transactional
    public void cancelUpgradeMission(Integer userId) {
        cancelMission(missionRepository.findOneByUserIdAndTypeCode(userId, MissionType.LEVEL_UP.name()));
        socketIoService.sendMessage(userId, RUNNING_UPGRADE_CHANGE, () -> null);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void processBuildUnit(Long missionId) {
        var missionBeforeLock = missionRepository.findById(missionId).orElse(null);
        if (missionBeforeLock != null) {
            planetLockUtilService.doInsideLockById(
                    List.of(missionBeforeLock.getMissionInformation().getValue().longValue()),
                    () -> {
                        LOG.debug("Process build mission " + missionId);
                        var mission = SpringRepositoryUtil.findByIdOrDie(missionRepository, missionId);
                        Long sourcePlanetId = mission.getMissionInformation().getValue().longValue();
                        var sourcePlanet = planetBo.findById(sourcePlanetId);
                        AtomicReference<Boolean> shouldClearImprovementsCache = new AtomicReference<>(false);
                        UserStorage user = mission.getUser();
                        Integer userId = user.getId();
                        obtainedUnitRepository.findByMissionId(missionId).forEach(current -> {
                            if (current.getUnit().getImprovement() != null) {
                                shouldClearImprovementsCache.set(true);
                            }
                            current.setSourcePlanet(sourcePlanet);
                            obtainedUnitBo.moveUnit(current, userId, sourcePlanetId);
                            requirementBo.triggerUnitBuildCompletedOrKilled(user, current.getUnit());
                        });
                        missionRepository.delete(mission);
                        transactionUtilService.doAfterCommit(() -> {
                            if (Boolean.TRUE.equals(shouldClearImprovementsCache.get())) {
                                improvementBo.clearSourceCache(user, obtainedUnitImprovementCalculationService);
                            }
                            missionEventEmitterBo.emitUnitBuildChange(userId);
                            emitMissionCountChange(userId);
                        });
                        asyncRunnerBo.runAsyncWithoutContextDelayed(
                                () -> obtainedUnitEventEmitter.emitObtainedUnits(user)
                                , 500
                        );
                        LOG.debug("End build mission " + mission);
                    }
            );
        } else {
            LOG.debug(MISSION_NOT_FOUND);
        }
    }

    @Transactional
    public void cancelBuildUnit(Long missionId) {
        cancelMission(missionId);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void runMission(Long missionId, MissionType missionType) {
        switch (missionType) {
            case BUILD_UNIT -> processBuildUnit(missionId);
            case LEVEL_UP -> processLevelUpAnUpgrade(missionId);
            default -> LOG.warn("Not a upgrade level mission nor unit build");
        }
    }

    @Override
    public int order() {
        return MISSION_USER_DELETE_ORDER;
    }

    @Override
    public void doDeleteUser(UserStorage user) {
        missionRepository.deleteByUserAndTypeCodeIn(user, List.of(MissionType.LEVEL_UP.name(), MissionType.BUILD_UNIT.name()));
    }

    /**
     * Should be invoked from the context
     *
     * @author Kevin Guanche Darias
     */
    private void cancelMission(Long missionId) {
        cancelMission(missionRepository.findById(missionId).orElse(null));
    }

    /**
     * Should be invoked from the context
     *
     * @author Kevin Guanche Darias
     */
    private void cancelMission(Mission mission) {
        if (mission == null) {
            throw new MissionNotFoundException("The mission was not found, or was not passed to cancelMission()");
        }
        var missionUser = mission.getUser();
        var loggedInUser = userSessionService.findLoggedIn();
        if (missionUser != null && missionUser.getId().equals(loggedInUser.getId())) {
            var missionType = missionTypeBo.resolve(mission);
            if (missionType == MissionType.BUILD_UNIT) {
                missionCancelBuildService.cancel(mission);
            } else {
                missionUser.addtoPrimary(mission.getPrimaryResource());
                missionUser.addToSecondary(mission.getSecondaryResource());
                userStorageRepository.save(missionUser);
                emitUserAfterCommit(missionUser.getId());
            }
        } else {
            throw new CommonException(
                    "unexpected executed condition!, maybe some dirty Kenpachi tried to cancel mission of other player!");
        }
        missionRepository.delete(mission);
        abortMissionJob(mission);
    }

    private void emitUserAfterCommit(Integer userId) {
        transactionUtilService.doAfterCommit(() -> emitUser(userId));
    }

    private void emitUser(Integer userId) {
        unitTypeBo.emitUserChange(userId);
        emitMissionCountChange(userId);
    }


    /**
     * Checks that there is not another upgrade mission running, if it's doing, will
     * throw an exception
     *
     * @author Kevin Guanche Darias
     */
    private void checkUpgradeMissionDoesNotExists(Integer userId) {
        if (missionRepository.findOneByUserIdAndTypeCode(userId, MissionType.LEVEL_UP.name()) != null) {
            throw new SgtLevelUpMissionAlreadyRunningException("There is already an upgrade going");
        }
    }

    /**
     * Checks that there is not another unit recluit mission running in <b>target
     * planet</b>
     *
     * @author Kevin Guanche Darias
     */
    private void checkUnitBuildMissionDoesNotExists(Integer userId, Long planetId) {
        if (missionFinderBo.findRunningUnitBuild(userId, (double) planetId) != null) {
            throw new SgtBackendUnitBuildAlreadyRunningException("I18N_ERR_BUILD_MISSION_ALREADY_PRESENT");
        }
    }

    /**
     * Checks that the selected obtained upgrade is available else, throws an
     * exception
     *
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
     * @author Kevin Guanche Darias
     */
    private void checkUnlockedUnit(Integer userId, ObjectRelation relation) {
        objectRelationBo.checkIsUnlocked(userId, relation.getId());
    }

    /**
     * Copies resource requirements object to mission and fills the ime and date
     *
     * @author Kevin Guanche Darias
     */
    private void attachRequirementsToMission(Mission mission, ResourceRequirementsPojo requirements) {
        mission.setPrimaryResource(requirements.getRequiredPrimary());
        mission.setSecondaryResource(requirements.getRequiredSecondary());
        mission.setRequiredTime(requirements.getRequiredTime());
        mission.setTerminationDate(missionTimeManagerBo.computeTerminationDate(requirements.getRequiredTime()));
    }

    /**
     * Substracts the resources of the mission to the logged in user
     *
     * @author Kevin Guanche Darias
     */
    private void substractResources(UserStorage user, Mission mission) {
        user.setPrimaryResource(user.getPrimaryResource() - mission.getPrimaryResource());
        user.setSecondaryResource(user.getSecondaryResource() - mission.getSecondaryResource());
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    private void abortMissionJob(Mission mission) {
        missionSchedulerService.abortMissionJob(mission);
    }
}
