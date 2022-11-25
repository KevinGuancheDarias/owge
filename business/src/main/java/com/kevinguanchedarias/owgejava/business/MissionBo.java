package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionTimeManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionTypeBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.unit.ObtainedUnitEventEmitter;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitImprovementCalculationService;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitModificationBo;
import com.kevinguanchedarias.owgejava.business.user.UserEnergyServiceBo;
import com.kevinguanchedarias.owgejava.business.user.UserEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.dto.RunningUnitBuildDto;
import com.kevinguanchedarias.owgejava.dto.RunningUpgradeDto;
import com.kevinguanchedarias.owgejava.entity.*;
import com.kevinguanchedarias.owgejava.entity.Mission.MissionIdAndTerminationDateProjection;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementChangeEnum;
import com.kevinguanchedarias.owgejava.enumerations.ImprovementTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.enumerations.ObjectEnum;
import com.kevinguanchedarias.owgejava.exception.*;
import com.kevinguanchedarias.owgejava.pojo.ResourceRequirementsPojo;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUpgradeRepository;
import com.kevinguanchedarias.owgejava.util.TransactionUtil;
import lombok.AllArgsConstructor;
import org.apache.log4j.Logger;
import org.hibernate.Hibernate;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.PostConstruct;
import javax.persistence.EntityManager;
import java.io.Serial;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

@Service
@AllArgsConstructor
public class MissionBo extends AbstractMissionBo {
    public static final String ENEMY_MISSION_CHANGE = "enemy_mission_change";
    public static final String UNIT_BUILD_MISSION_CHANGE = "unit_build_mission_change";
    public static final String MISSIONS_COUNT_CHANGE = "missions_count_change";
    public static final String MISSION_NOT_FOUND = "Mission doesn't exists, maybe it was cancelled";
    public static final String RUNNING_UPGRADE_CHANGE = "running_upgrade_change";

    @Serial
    private static final long serialVersionUID = 5505953709078785322L;

    private static final Logger LOG = Logger.getLogger(MissionBo.class);
    private static final String JOB_GROUP_NAME = "Missions";

    private static final int DAYS = 60;

    private final transient EntityManager entityManager;
    private final transient ConfigurationBo configurationBo;
    private final transient AsyncRunnerBo asyncRunnerBo;
    private final transient TransactionUtilService transactionUtilService;
    private final transient PlanetLockUtilService planetLockUtilService;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ObtainedUpgradeRepository obtainedUpgradeRepository;
    private final transient UserEventEmitterBo userEventEmitterBo;
    private final transient UserEnergyServiceBo userEnergyServiceBo;
    private final transient MissionTypeBo missionTypeBo;
    private final transient ObtainedUnitEventEmitter obtainedUnitEventEmitter;
    private final transient MissionTimeManagerBo missionTimeManagerBo;
    private final ObtainedUnitModificationBo obtainedUnitModificationBo;
    private final ObtainedUnitBo obtainedUnitBo;
    private final ObtainedUnitImprovementCalculationService obtainedUnitImprovementCalculationService;

    @PostConstruct
    public void init() {
        improvementBo.addChangeListener(ImprovementChangeEnum.UNIT_IMPROVEMENTS, (userId, improvement) -> {
            if (improvement.getUnitTypesUpgrades().stream()
                    .anyMatch(current -> ImprovementTypeEnum.AMOUNT.name().equals(current.getType()))) {
                unitTypeBo.emitUserChange(userId);
            }
        });
        improvementBo.addChangeListener(ImprovementChangeEnum.MORE_ENERGY, (userId, improvement) ->
                transactionUtilService.doAfterCommit(() ->
                        userEventEmitterBo.emitMaxEnergyChange(userId)
                )
        );
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onApplicationReady() {
        deleteOldMissions();
    }

    @Scheduled(cron = "0 0 2 * * *")
    public void deleteOldMissions() {
        var limitDate = new Date(new Date().getTime() - (86400000L * DAYS));
        transactionUtilService.runWithRequired(() ->
                missionRepository.findByResolvedTrueAndTerminationDateLessThan(limitDate)
                        .forEach(mission -> {
                            mission.getLinkedRelated().forEach(linked -> linked.setRelatedMission(null));
                            missionRepository.delete(mission);
                        })
        );
    }

    @Override
    public String getGroupName() {
        return JOB_GROUP_NAME;
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

        var user = userStorageBo.findById(userId);
        checkMissionLimitNotReached(user);
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

        userStorageBo.save(user);
        missionRepository.save(mission);
        scheduleMission(mission);
        transactionUtilService.doAfterCommit(() -> {
            entityManager.refresh(mission);
            emitRunningUpgrade(user);
            emitMissionCountChange(userId);
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
        var mission = findById(missionId);
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
        planetBo.myCheckIsOfUserProperty(planetId);
        checkUnitBuildMissionDoesNotExists(userId, planetId);
        var relation = objectRelationBo.findOne(ObjectEnum.UNIT,
                unitId);
        checkUnlockedUnit(userId, relation);
        var user = userStorageBo.findById(userId);
        checkMissionLimitNotReached(user);
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

        userStorageBo.save(user);
        missionRepository.save(mission);

        var obtainedUnit = new ObtainedUnit();
        obtainedUnit.setMission(mission);
        obtainedUnit.setCount(finalCount);
        obtainedUnit.setUnit(unit);
        obtainedUnit.setUser(user);
        obtainedUnitRepository.save(obtainedUnit);

        scheduleMission(mission);

        transactionUtilService.doAfterCommit(() -> {
            entityManager.refresh(obtainedUnit);
            entityManager.refresh(mission);
            emitMissionCountChange(userId);
            emitUnitBuildChange(userId);
            unitTypeBo.emitUserChange(userId);
        });

        return new RunningUnitBuildDto(unit, mission, planetBo.findById(planetId), finalCount);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    public void emitUnitBuildChange(Integer userId) {
        socketIoService.sendMessage(userId, UNIT_BUILD_MISSION_CHANGE, () -> findBuildMissions(userId));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    public void emitMissionCountChange(Integer userId) {
        socketIoService.sendMessage(userId, MISSIONS_COUNT_CHANGE, () -> countUserMissions(userId));
    }

    public RunningUpgradeDto findRunningLevelUpMission(Integer userId) {
        var mission = findByUserIdAndTypeCode(userId, MissionType.LEVEL_UP);
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

    public RunningUnitBuildDto findRunningUnitBuild(Integer userId, Double planetId) {
        var mission = findByUserIdAndTypeCodeAndMissionInformationValue(userId, MissionType.BUILD_UNIT, planetId);
        if (mission != null) {
            var missionInformation = mission.getMissionInformation();
            var unit = (Unit) objectRelationBo.unboxObjectRelation(missionInformation.getRelation());
            return new RunningUnitBuildDto(unit, mission, planetBo.findById(planetId.longValue()),
                    obtainedUnitRepository.findByMissionId(mission.getId()).get(0).getCount());
        } else {
            return null;
        }
    }

    /**
     * Finds all build missions for given user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public List<RunningUnitBuildDto> findBuildMissions(Integer userId) {
        return missionRepository.findByUserIdAndTypeCodeAndResolvedFalse(userId, MissionType.BUILD_UNIT.name()).stream()
                .map(mission -> {
                    var missionInformation = mission.getMissionInformation();
                    var unit = (Unit) objectRelationBo.unboxObjectRelation(missionInformation.getRelation());
                    var planet = planetBo.findById(missionInformation.getValue().longValue());
                    List<ObtainedUnit> findByMissionId = obtainedUnitRepository.findByMissionId(mission.getId());
                    return new RunningUnitBuildDto(unit, mission, planet,
                            findByMissionId.isEmpty() ? 0 : findByMissionId.get(0).getCount());
                }).toList();
    }

    public MissionIdAndTerminationDateProjection findOneByReportId(Long reportId) {
        return missionRepository.findOneByReportId(reportId);
    }

    /**
     * Should be invoked from the context
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void cancelMission(Long missionId) {
        cancelMission(findById(missionId));
    }

    /**
     * Should be invoked from the context
     *
     * @author Kevin Guanche Darias
     */
    @Transactional
    public void cancelMission(Mission mission) {
        if (mission == null) {
            throw new MissionNotFoundException("The mission was not found, or was not passed to cancelMission()");
        }
        var missionUser = userStorageBo.findOneByMission(mission);
        var loggedInUser = userStorageBo.findLoggedIn();
        var type = MissionType.valueOf(mission.getType().getCode());
        if (missionUser == null) {
            if (type == MissionType.BROADCAST_MESSAGE) {
                throw new SgtBackendNotImplementedException("This feature has not been implemented");
            } else {
                throw new CommonException("No such mission type " + mission.getType().getCode());
            }
        } else if (missionUser.getId().equals(loggedInUser.getId())) {
            switch (type) {
                case BUILD_UNIT:
                    adminCancelBuildMission(mission);
                    break;
                case LEVEL_UP:
                    missionUser.addtoPrimary(mission.getPrimaryResource());
                    missionUser.addToSecondary(mission.getSecondaryResource());
                    userStorageBo.save(missionUser);
                    emitUserAftercommit(missionUser.getId());
                    break;
                default:
                    throw new CommonException("No such mission type " + mission.getType().getCode());
            }
        } else {
            throw new CommonException(
                    "unexpected executed condition!, maybe some dirty Kenpachi tried to cancel mission of other player!");
        }
        missionRepository.delete(mission);
        abortMissionJob(mission);
    }

    @Transactional
    public void adminCancelBuildMission(Mission mission) {
        UserStorage missionUser = mission.getUser();
        obtainedUnitModificationBo.deleteByMissionId(mission.getId());
        missionUser.addtoPrimary(mission.getPrimaryResource());
        missionUser.addToSecondary(mission.getSecondaryResource());
        userStorageBo.save(missionUser);
        transactionUtilService.doAfterCommit(() -> {
            socketIoService.sendMessage(missionUser, UNIT_BUILD_MISSION_CHANGE,
                    () -> findBuildMissions(missionUser.getId()));
            emitUser(missionUser.getId());
        });
    }

    @Transactional
    public void cancelUpgradeMission(Integer userId) {
        cancelMission(findByUserIdAndTypeCode(userId, MissionType.LEVEL_UP));
        socketIoService.sendMessage(userId, RUNNING_UPGRADE_CHANGE, () -> null);
        emitMissionCountChange(userId);
    }

    @Transactional
    public void processBuildUnit(Long missionId) {
        var missionBeforeLock = findById(missionId);
        if (missionBeforeLock != null) {
            planetLockUtilService.doInsideLockById(
                    List.of(missionBeforeLock.getMissionInformation().getValue().longValue()),
                    () -> {
                        var mission = findById(missionId);
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
                            emitUnitBuildChange(userId);
                            emitMissionCountChange(userId);
                        });
                        asyncRunnerBo.runAssyncWithoutContextDelayed(
                                () -> obtainedUnitEventEmitter.emitObtainedUnits(user)
                                , 500
                        );
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
    @Transactional
    public void runMission(Long missionId, MissionType missionType) {
        switch (missionType) {
            case BUILD_UNIT -> processBuildUnit(missionId);
            case LEVEL_UP -> processLevelUpAnUpgrade(missionId);
            default -> LOG.warn("Not a upgrade level mission nor unit build");
        }
    }

    private void emitUserAftercommit(Integer userId) {
        TransactionUtil.doAfterCommit(() -> emitUser(userId));
    }

    private void emitUser(Integer userId) {
        unitTypeBo.emitUserChange(userId);
        emitMissionCountChange(userId);
    }

    /**
     * Finds a mission by user id, mission type, and value inside MissionInformation
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private Mission findByUserIdAndTypeCodeAndMissionInformationValue(Integer userId, MissionType type, Double value) {
        return missionRepository.findByUserIdAndTypeCodeAndMissionInformationValue(userId, type.name(), value);
    }

    /**
     * Checks that there is not another upgrade mission running, if it's doing, will
     * throw an exception
     *
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
     * @author Kevin Guanche Darias
     */
    private void checkUnitBuildMissionDoesNotExists(Integer userId, Long planetId) {
        if (findRunningUnitBuild(userId, (double) planetId) != null) {
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
}
