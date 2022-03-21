package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.mission.MissionConfigurationBo;
import com.kevinguanchedarias.owgejava.dto.MissionDto;
import com.kevinguanchedarias.owgejava.dto.UnitRunningMissionDto;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.MissionReport;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.exception.UserNotFoundException;
import com.kevinguanchedarias.owgejava.pojo.websocket.MissionWebsocketMessage;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.MissionTypeRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.taggablecache.manager.TaggableCacheManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import java.io.Serial;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Contains methods and properties shared between all MissionBo types
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public abstract class AbstractMissionBo implements BaseBo<Long, Mission, MissionDto> {
    public static final String MISSION_CACHE_TAG = "mission";
    public static final String UNIT_OBTAINED_CHANGE = "unit_obtained_change";

    @Serial
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
    protected transient ObtainedUnitBo obtainedUnitBo;

    @Autowired
    protected PlanetBo planetBo;

    @Autowired
    protected transient ExceptionUtilService exceptionUtilService;

    @Autowired
    @Lazy
    protected UnitTypeBo unitTypeBo;

    @Autowired
    protected transient MissionConfigurationBo missionConfigurationBo;

    @Autowired
    protected transient SocketIoService socketIoService;

    @Autowired
    private MissionReportBo missionReportBo;

    @Autowired
    private transient ApplicationContext applicationContext;

    @Autowired
    private transient MissionSchedulerService missionSchedulerService;

    @Autowired
    private transient TaggableCacheManager taggableCacheManager;

    public abstract String getGroupName();

    @Override
    public TaggableCacheManager getTaggableCacheManager() {
        return taggableCacheManager;
    }

    @Override
    public String getCacheTag() {
        return MISSION_CACHE_TAG;
    }

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
     * @todo Think in a way to move this to a "MissionEventChangeEmitterService" service
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public void emitUnitMissions(Integer userId) {
        socketIoService.sendMessage(userId, "unit_mission_change",
                () -> new MissionWebsocketMessage(countUserMissions(userId), findUserRunningMissions(userId)));
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Transactional
    public void retryMissionIfPossible(Long missionId, MissionType missionType) {
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
        } else {
            mission.setAttemps(mission.getAttemps() + 1);
            mission.setTerminationDate(computeTerminationDate(mission.getRequiredTime()));
            handleMissionReportSave(mission, buildCommonErrorReport(mission, missionType));
            scheduleMission(mission);
            save(mission);
        }
    }

    /**
     * Counts the number of missions that a user has running
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    public Integer countUserMissions(Integer userId) {
        return missionRepository.countByUserIdAndResolvedFalse(userId);
    }

    /**
     * Returns the max number of missions a user can run
     *
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
    public List<UnitRunningMissionDto> findUserRunningMissions(Integer userId) {
        return missionRepository.findByUserIdAndResolvedFalse(userId).stream().map(mission -> {
            mission.setInvolvedUnits(obtainedUnitBo.findByMissionId(mission.getId()));
            return mission;
        }).map(UnitRunningMissionDto::new).map(current -> {
            if (current.getType() == MissionType.EXPLORE) {
                planetBo.cleanUpUnexplored(userId, current.getTargetPlanet());
            }
            return current;
        }).map(UnitRunningMissionDto::nullifyInvolvedUnitsPlanets).toList();
    }

    /**
     * Finds missions that couldn't execute with success
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    public List<Mission> findHangMissions() {
        return missionRepository.findByTerminationDateNotNullAndTerminationDateLessThanAndResolvedFalse(new Date());
    }

    /**
     * @param type enum based mission type
     * @return persisted mission type
     * @author Kevin Guanche Darias
     */
    public com.kevinguanchedarias.owgejava.entity.MissionType findMissionType(MissionType type) {
        return missionTypeRepository.findOneByCode(type.name())
                .orElseThrow(() -> new SgtBackendInvalidInputException("No MissionType " + type.name() + " was found in the database"));
    }

    /**
     * Finds a mission by user id and mission type
     *
     * @author Kevin Guanche Darias
     */
    protected Mission findByUserIdAndTypeCode(Integer userId, MissionType type) {
        return missionRepository.findOneByUserIdAndTypeCode(userId, type.name());
    }

    /**
     * Checks if the user exists (in this universe), throws if not
     *
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    protected void checkPlanetExists(Long planetId) {
        if (!planetBo.exists(planetId)) {
            throw new PlanetNotFoundException("No such planet with id " + planetId);
        }
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
        MissionReport missionReport = missionReportBo.create(builder, isEnemy, user);
        missionReport.setMission(mission);
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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    protected void scheduleMission(Mission mission) {
        missionSchedulerService.scheduleMission(getGroupName(), mission);
    }

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    protected void abortMissionJob(Mission mission) {
        missionSchedulerService.abortMissionJob(getGroupName(), mission);
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
