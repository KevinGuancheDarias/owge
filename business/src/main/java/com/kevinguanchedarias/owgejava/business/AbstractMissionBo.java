package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.dto.MissionDto;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.entity.Planet;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.UserNotFoundException;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.MissionTypeRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.owgejava.util.ObtainedUnitUtil;
import lombok.SneakyThrows;
import org.apache.commons.lang3.RandomUtils;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Contains methods and properties shared between all MissionBo types
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public abstract class AbstractMissionBo implements BaseBo<Long, Mission, MissionDto> {
    public static final String UNIT_OBTAINED_CHANGE = "unit_obtained_change";

    private static final long serialVersionUID = 3252246009672348672L;

    private static final Integer MAX_ATTEMPS = 3;

    @Autowired
    protected MissionRepository missionRepository;

    @Autowired
    protected ObtainedUpgradeBo obtainedUpgradeBo;

    @Autowired
    protected ObjectRelationBo objectRelationBo;

    @Autowired
    protected UpgradeBo upgradeBo;

    @Autowired
    protected UserStorageBo userStorageBo;

    @Autowired
    protected MissionTypeRepository missionTypeRepository;

    @Autowired
    protected ImprovementBo improvementBo;

    @Autowired
    protected RequirementBo requirementBo;

    @Autowired
    protected UnlockedRelationBo unlockedRelationBo;

    @Autowired
    protected UnitBo unitBo;

    @Autowired
    @Lazy
    protected ObtainedUnitBo obtainedUnitBo;

    @Autowired
    protected PlanetBo planetBo;

    @Autowired
    protected transient ExceptionUtilService exceptionUtilService;

    @Autowired
    @Lazy
    protected UnitTypeBo unitTypeBo;

    @Autowired
    private MissionReportBo missionReportBo;

    @Autowired
    private transient ApplicationContext applicationContext;

    @Autowired
    private transient MissionSchedulerService missionSchedulerService;

    public abstract String getGroupName();

    public abstract Logger getLogger();

    @Override
    public JpaRepository<Mission, Long> getRepository() {
        return missionRepository;
    }

    /*
     * (non-Javadoc)
     *
     * @see com.kevinguanchedarias.owgejava.business.BaseBo#getDtoClass()
     */
    @Override
    public Class<MissionDto> getDtoClass() {
        return MissionDto.class;
    }

    /**
     * @return True if the mission will continue to run
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Transactional
    public boolean retryMissionIfPossible(Long missionId, MissionType missionType) {
        Mission mission = findById(missionId);
        mission.setUser(userStorageBo.findOneByMission(mission));
        if (mission.getAttemps() >= MAX_ATTEMPS) {
            if (missionType.isUnitMission() && missionType != MissionType.RETURN_MISSION
                    && missionType != MissionType.BUILD_UNIT) {
                findUnitMissionBoInstance().adminRegisterReturnMission(mission);
                resolveMission(mission);
            } else if (missionType == MissionType.BUILD_UNIT) {
                obtainedUnitBo.deleteByMissionId(mission.getId());
                delete(mission);
            } else if (missionType == MissionType.LEVEL_UP) {
                delete(mission);
            } else {
                throw new ProgrammingException("Should never ever happend");
            }
            return false;
        } else {
            mission.setAttemps(mission.getAttemps() + 1);
            mission.setTerminationDate(computeTerminationDate(mission.getRequiredTime()));
            handleMissionReportSave(mission, buildCommonErrorReport(mission, missionType));
            scheduleMission(mission);
            save(mission);
            return true;
        }
    }

    /**
     * Finds a <b>not resolved </b>mission by userId, mission type and target planet
     *
     * @param userId
     * @param type
     * @param targetPlanet
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.7.4
     */
    public Mission findOneByUserIdAndTypeAndTargetPlanet(Integer userId, MissionType type, Long targetPlanet) {
        List<Mission> missions = missionRepository.findByUserIdAndTypeCodeAndTargetPlanetIdAndResolvedFalse(userId,
                type.name(), targetPlanet);
        if (missions.isEmpty()) {
            return null;
        } else {
            return missions.get(0);
        }
    }

    /**
     * Counts the number of missions that a user has running
     *
     * @param userId
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public Integer countUserMissions(Integer userId) {
        return missionRepository.countByUserIdAndResolvedFalse(userId);
    }

    /**
     * Returns the max number of missions a user can run
     *
     * @param user
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public Integer findUserMaxAllowedMissions(UserStorage user) {
        return improvementBo.findUserImprovement(user).getMoreMisions().intValue() + 1;
    }

    /**
     * Returns all the running missions for the specified user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Transactional
    public List<UnitRunningMissionDto> findUserRunningMissions(Integer userId) {
        return missionRepository.findByUserIdAndResolvedFalse(userId).stream().map(mission -> {
            mission.setInvolvedUnits(obtainedUnitBo.findByMissionId(mission.getId()));
            return mission;
        }).map(UnitRunningMissionDto::new).map(current -> {
            if (current.getType() == MissionType.EXPLORE) {
                planetBo.cleanUpUnexplored(userId, current.getTargetPlanet());
            }
            return current;
        }).map(UnitRunningMissionDto::nullifyInvolvedUnitsPlanets).collect(Collectors.toList());
    }

    @Transactional
    public List<UnitRunningMissionDto> findEnemyRunningMissions(UserStorage user) {
        List<Planet> myPlanets = planetBo.findPlanetsByUser(user);
        return missionRepository.findByTargetPlanetInAndResolvedFalseAndInvisibleFalseAndUserNot(myPlanets, user)
                .stream().map(current -> {
                    UnitRunningMissionDto retVal = new UnitRunningMissionDto(current);
                    retVal.nullifyInvolvedUnitsPlanets();
                    if (!planetBo.isExplored(user, current.getSourcePlanet())) {
                        retVal.setSourcePlanet(null);
                        retVal.setUser(null);
                    }
                    ObtainedUnitUtil.handleInvisible(retVal.getInvolvedUnits());
                    return retVal;
                }).collect(Collectors.toList());
    }

    /**
     * Finds missions that couldn't execute with success
     *
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    public List<Mission> findHangMissions() {
        return missionRepository.findByTerminationDateNotNullAndTerminationDateLessThanAndResolvedFalse(new Date());
    }

    /**
     * Finds a mission by user id and mission type
     *
     * @param userId
     * @param type
     * @return
     * @author Kevin Guanche Darias
     */
    protected Mission findByUserIdAndTypeCode(Integer userId, MissionType type) {
        return missionRepository.findOneByUserIdAndTypeCode(userId, type.name());
    }

    /**
     * Checks if the user exists (in this universe), throws if not
     *
     * @param userId
     * @throws UserNotFoundException If user doesn't exists
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    protected void checkUserExists(Integer userId) {
        if (!userStorageBo.exists(userId)) {
            throw new UserNotFoundException("No user with id " + userId);
        }
    }

    /**
     * Checks if the input planet exists
     *
     * @param planetId
     * @throws PlanetNotFoundException
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    protected void checkPlanetExists(Long planetId) {
        if (!planetBo.exists(planetId)) {
            throw new PlanetNotFoundException("No such planet with id " + planetId);
        }
    }

    /**
     * @param type enum based mission type
     * @return persisted mission type
     * @author Kevin Guanche Darias
     */
    protected com.kevinguanchedarias.owgejava.entity.MissionType findMissionType(MissionType type) {
        com.kevinguanchedarias.owgejava.entity.MissionType retVal = missionTypeRepository.findOneByCode(type.name());
        if (retVal == null) {
            throw new SgtBackendInvalidInputException("No MissionType " + type.name() + " was found in the database");
        }
        return retVal;
    }

    /**
     * Returns the date that the mission will have according to the required time
     *
     * @param requiredTime ammount of time required (in seconds)
     * @author Kevin Guanche Darias
     */
    protected Date computeTerminationDate(Double requiredTime) {
        return Date.from(Instant.now().plusSeconds(requiredTime.intValue()));
    }

    /**
     * Defines the mission as resolved
     *
     * @param mission Mission to persist
     * @return Persisted entity
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    protected void resolveMission(Mission mission) {
        mission.setResolved(true);
    }

    /**
     * Returns true if the input mission is of the expected type
     *
     * @param mission input mission
     * @param type    expected type
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    protected boolean isOfType(Mission mission, MissionType type) {
        return MissionType.valueOf(mission.getType().getCode()).equals(type);
    }

    /**
     * Saves the MissionReport to the database
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    protected void handleMissionReportSave(Mission mission, UnitMissionReportBuilder builder) {
        MissionReport missionReport = new MissionReport("{}", mission);
        missionReport.setUser(mission.getUser());
        missionReport = missionReportBo.save(missionReport);
        missionReport.setReportDate(new Date());
        missionReport.setJsonBody(builder.withId(missionReport.getId()).buildJson());
        missionReport.setIsEnemy(false);
        mission.setReport(missionReport);
    }

    protected void handleMissionReportSave(Mission mission, UnitMissionReportBuilder builder, boolean isEnemy,
                                           List<UserStorage> users) {
        users.forEach(currentUser -> handleMissionReportSave(mission, builder, isEnemy, currentUser));
    }

    protected void handleMissionReportSave(Mission mission, UnitMissionReportBuilder builder, boolean isEnemy,
                                           UserStorage user) {
        MissionReport missionReport = new MissionReport("{}", mission);
        missionReport.setUser(user);
        missionReport = missionReportBo.save(missionReport);
        missionReport.setReportDate(new Date());
        missionReport.setJsonBody(builder.withId(missionReport.getId()).buildJson());
        missionReport.setIsEnemy(isEnemy);
        mission.setReport(missionReport);
    }

    protected void checkMissionLimitNotReached(UserStorage user) {
        if (countUserMissions(user.getId()) + 1 >= findUserMaxAllowedMissions(user)) {
            throw exceptionUtilService
                    .createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_MISSION_LIMIT_EXCEEDED")
                    .withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
        }
    }

    /**
     * Finds the last mission in the relation tree that is not a RETURN_MISSION
     *
     * @param missionId
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    protected Mission findLastMissionInRelationTree(Long missionId) {
        return findLastMissionInRelationTree(missionId,
                mission -> MissionType.RETURN_MISSION.name().equals(mission.getType().getCode()));
    }

    /**
     * Find the last mission in the relation tree that matches "false" to
     * <i>ignore</i> lambda
     *
     * @param missionId
     * @param ignore
     * @return
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    protected Mission findLastMissionInRelationTree(Long missionId, Predicate<Mission> ignore) {
        Mission mission = findById(missionId);
        if (mission != null && mission.getRelatedMission() != null) {
            if (ignore.test(mission)) {
                return findLastMissionInRelationTree(missionId, ignore);
            } else {
                return mission;
            }
        } else if (mission != null && mission.getRelatedMission() == null) {
            return mission;
        } else {
            throw new ProgrammingException("Passed null");
        }
    }

    /**
     * @param mission
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    protected void scheduleMission(Mission mission) {
        missionSchedulerService.scheduleMission(getGroupName(), mission);
    }

    /**
     * @param mission
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    protected void abortMissionJob(Mission mission) {
        missionSchedulerService.abortMissionJob(getGroupName(), mission);
    }

    /**
     * Wait for mission affecting planet.
     *
     * @param planet the planet
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.20
     */
    @SneakyThrows(InterruptedException.class)
    protected void waitForMissionAffectingPlanet(Planet planet) {
        Date offsetStart = new Date(new Date().getTime() - 2000);
        Date offset = new Date(offsetStart.getTime() + 2000);
        if (missionRepository.existsByTerminationDateBetweenAndTargetPlanetAndResolvedFalse(offsetStart, offset,
                planet)) {
            Thread.sleep(100 * RandomUtils.nextLong(10, 15));
            waitForMissionAffectingPlanet(planet);
        }
    }

    private UnitMissionReportBuilder buildCommonErrorReport(Mission mission, MissionType missionType) {
        UnitMissionReportBuilder reportBuilder = UnitMissionReportBuilder.create().withSenderUser(mission.getUser())
                .withId(mission.getId());
        if (missionType.isUnitMission()) {
            reportBuilder = reportBuilder.withSourcePlanet(mission.getSourcePlanet())
                    .withTargetPlanet(mission.getTargetPlanet())
                    .withInvolvedUnits(findUnitMissionBoInstance().findInvolvedInMission(mission));
        }
        reportBuilder.withErrorInformation("Mission with id " + mission.getId() + " failed, please contact an admin!");
        return reportBuilder;
    }

    private UnitMissionBo findUnitMissionBoInstance() {
        return applicationContext.getBean(UnitMissionBo.class);
    }
}
