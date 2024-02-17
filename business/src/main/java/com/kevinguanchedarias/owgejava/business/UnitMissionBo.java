package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.business.mission.MissionBaseService;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionInterceptionManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.processor.MissionProcessor;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.UnitMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.planet.PlanetExplorationService;
import com.kevinguanchedarias.owgejava.business.planet.PlanetLockUtilService;
import com.kevinguanchedarias.owgejava.business.user.UserSessionService;
import com.kevinguanchedarias.owgejava.business.user.listener.UserDeleteListener;
import com.kevinguanchedarias.owgejava.business.util.TransactionUtilService;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.*;
import com.kevinguanchedarias.owgejava.pojo.UnitMissionInformation;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.PlanetRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.EnumUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.*;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
public class UnitMissionBo implements UserDeleteListener {
    public static final int UNIT_MISSION_USER_DELETE_ORDER = MissionBo.MISSION_USER_DELETE_ORDER + 1;

    private final PlanetLockUtilService planetLockUtilService;
    private final UnitMissionRegistrationBo unitMissionRegistrationBo;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final PlanetRepository planetRepository;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final MissionInterceptionManagerBo missionInterceptionManagerBo;
    private final List<MissionProcessor> missionProcessors;
    private final PlanetBo planetBo;
    private final PlanetExplorationService planetExplorationService;
    private final MissionRepository missionRepository;
    private final UserSessionService userSessionService;
    private final ExceptionUtilService exceptionUtilService;
    private final MissionReportManagerBo missionReportManagerBo;
    private final MissionBaseService missionBaseService;
    private final TransactionUtilService transactionUtilService;

    protected Map<MissionType, MissionProcessor> missionProcessorMap;

    @PostConstruct
    public void init() {
        missionProcessorMap = Arrays.stream(MissionType.values())
                .filter(MissionType::isUnitMission)
                .collect(Collectors.toMap(missionType -> missionType, this::findFirstSupporter));
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
    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void adminRegisterExploreMission(UnitMissionInformation missionInformation) {
        commonMissionRegister(missionInformation, MissionType.EXPLORE);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void myRegisterGatherMission(UnitMissionInformation missionInformation) {
        myRegister(missionInformation);
        adminRegisterGatherMission(missionInformation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void adminRegisterGatherMission(UnitMissionInformation missionInformation) {
        commonMissionRegister(missionInformation, MissionType.GATHER);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void myRegisterEstablishBaseMission(UnitMissionInformation missionInformation) {
        myRegister(missionInformation);
        adminRegisterEstablishBase(missionInformation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void adminRegisterEstablishBase(UnitMissionInformation missionInformation) {
        commonMissionRegister(missionInformation, MissionType.ESTABLISH_BASE);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void myRegisterAttackMission(UnitMissionInformation missionInformation) {
        myRegister(missionInformation);
        adminRegisterAttackMission(missionInformation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void adminRegisterAttackMission(UnitMissionInformation missionInformation) {
        commonMissionRegister(missionInformation, MissionType.ATTACK);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void myRegisterCounterattackMission(UnitMissionInformation missionInformation) {
        myRegister(missionInformation);
        adminRegisterCounterattackMission(missionInformation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void adminRegisterCounterattackMission(UnitMissionInformation missionInformation) {
        if (!planetRepository.isOfUserProperty(missionInformation.getUserId(), missionInformation.getTargetPlanetId())) {
            throw new SgtBackendInvalidInputException(
                    "TargetPlanet doesn't belong to sender user, try again dear Hacker, maybe next time you have some luck");
        }
        commonMissionRegister(missionInformation, MissionType.COUNTERATTACK);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void myRegisterConquestMission(UnitMissionInformation missionInformation) {
        myRegister(missionInformation);
        adminRegisterConquestMission(missionInformation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
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

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void myRegisterDeploy(UnitMissionInformation missionInformation) {
        myRegister(missionInformation);
        adminRegisterDeploy(missionInformation);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void adminRegisterDeploy(UnitMissionInformation missionInformation) {
        if (missionInformation.getSourcePlanetId().equals(missionInformation.getTargetPlanetId())) {
            throw exceptionUtilService
                    .createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_DEPLOY_ITSELF")
                    .withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
        }
        commonMissionRegister(missionInformation, MissionType.DEPLOY);
    }

    @Transactional(isolation = Isolation.READ_COMMITTED)
    public void myCancelMission(Long missionId) {
        var mission = missionRepository.findById(missionId).orElse(null);
        if (mission == null) {
            throw new NotFoundException("No mission with id " + missionId + " was found");
        } else if (!mission.getUser().getId().equals(userSessionService.findLoggedIn().getId())) {
            throw new SgtBackendInvalidInputException("You can't cancel other player missions");
        } else if (missionBaseService.isOfType(mission, MissionType.RETURN_MISSION)) {
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
            returnMissionRegistrationBo.registerReturnMission(mission, mission.getRequiredTime() - durationMillis);
        }
    }

    /**
     * Runs the mission
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.9
     */
    @Transactional(isolation = Isolation.READ_COMMITTED)
    @Retryable(retryFor = CannotAcquireLockException.class, backoff = @Backoff(delay = 500, random = true, maxDelay = 750, multiplier = 2))
    public void runUnitMission(Long missionId, MissionType missionType) {
        transactionUtilService.clearStatus();
        var mission = SpringRepositoryUtil.findByIdOrDie(missionRepository, missionId);
        planetLockUtilService.doInsideLock(
                List.of(mission.getSourcePlanet(), mission.getTargetPlanet()),
                () -> doRunUnitMission(mission, missionType)
        );
    }

    @Override
    public int order() {
        return UNIT_MISSION_USER_DELETE_ORDER;
    }

    @Override
    public void doDeleteUser(UserStorage user) {
        var missions = missionRepository.findByUser(user).stream()
                .filter(mission -> EnumUtils.isValidEnum(MissionType.class, mission.getType().getCode()))
                .filter(mission -> MissionType.valueOf(mission.getType().getCode()).isUnitMission())
                .toList();
        var affectedPlanets = missions.stream()
                .map(mission -> List.of(mission.getSourcePlanet(), mission.getTargetPlanet()))
                .flatMap(Collection::stream)
                .filter(Objects::nonNull)
                .collect(Collectors.toUnmodifiableSet())
                .stream()
                .toList();
        planetLockUtilService.doInsideLock(affectedPlanets, () -> {
            missionRepository.deleteAll(missions);
            transactionUtilService.doAfterCommit(
                    () -> missionEventEmitterBo.emitEnemyMissionsChange(missions)
            );
        });
    }

    private void doRunUnitMission(Mission mission, MissionType missionType) {
        var interceptionInformation = missionInterceptionManagerBo.loadInformation(mission, missionType);
        if (!interceptionInformation.isMissionIntercepted()) {
            var involvedUnits = interceptionInformation.getInvolvedUnits();
            var reportBuilder = missionProcessorMap.get(missionType).process(mission, involvedUnits);
            missionInterceptionManagerBo.maybeAppendDataToMissionReport(mission, reportBuilder, interceptionInformation);
            if (reportBuilder != null) {
                missionReportManagerBo.handleMissionReportSave(mission, reportBuilder);
            }
        } else {
            missionInterceptionManagerBo.handleMissionInterception(mission, interceptionInformation);
            missionEventEmitterBo.emitLocalMissionChangeAfterCommit(mission);
        }
    }

    /**
     * Executes modifications to <i>missionInformation</i> to define the logged in
     * user as the sender user
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    private void myRegister(UnitMissionInformation missionInformation) {
        missionInformation.setUserId(userSessionService.findLoggedIn().getId());
    }

    private void commonMissionRegister(UnitMissionInformation missionInformation, MissionType missionType) {
        missionInformation.setMissionType(missionType);
        var user = userSessionService.findLoggedIn();
        var isDeployMission = missionType.equals(MissionType.DEPLOY);
        if (!isDeployMission || !planetRepository.isOfUserProperty(user.getId(), missionInformation.getTargetPlanetId())) {
            missionBaseService.checkMissionLimitNotReached(user);
        }
        var targetMissionInformation = copyMissionInformation(missionInformation);
        var userId = user.getId();
        targetMissionInformation.setUserId(userId);
        if (missionType != MissionType.EXPLORE
                && !planetExplorationService.isExplored(userId, missionInformation.getTargetPlanetId())) {
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
        var retVal = new UnitMissionInformation();
        BeanUtils.copyProperties(missionInformation, retVal);
        return retVal;
    }

    private MissionProcessor findFirstSupporter(MissionType missionType) {
        return missionProcessors.stream().filter(missionProcessor -> missionProcessor.supports(missionType)).findFirst().orElseThrow(() -> new ProgrammingException("No support for mission: " + missionType));
    }
}
