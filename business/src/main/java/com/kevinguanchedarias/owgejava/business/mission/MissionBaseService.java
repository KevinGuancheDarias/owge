package com.kevinguanchedarias.owgejava.business.mission;

import com.kevinguanchedarias.owgejava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.owgejava.business.ImprovementBo;
import com.kevinguanchedarias.owgejava.business.MissionSchedulerService;
import com.kevinguanchedarias.owgejava.business.mission.report.MissionReportManagerBo;
import com.kevinguanchedarias.owgejava.business.mission.unit.registration.returns.ReturnMissionRegistrationBo;
import com.kevinguanchedarias.owgejava.business.unit.obtained.ObtainedUnitModificationBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.DocTypeEnum;
import com.kevinguanchedarias.owgejava.enumerations.GameProjectsEnum;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.exception.SgtBackendInvalidInputException;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.ObtainedUnitRepository;
import com.kevinguanchedarias.owgejava.util.ExceptionUtilService;
import com.kevinguanchedarias.owgejava.util.SpringRepositoryUtil;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@AllArgsConstructor
public class MissionBaseService {
    private static final Integer MAX_ATTEMPTS = 3;

    private final MissionRepository missionRepository;
    private final ReturnMissionRegistrationBo returnMissionRegistrationBo;
    private final ObtainedUnitModificationBo obtainedUnitModificationBo;
    private final MissionTimeManagerBo missionTimeManagerBo;
    private final MissionReportManagerBo missionReportManagerBo;
    private final ObtainedUnitRepository obtainedUnitRepository;
    private final ImprovementBo improvementBo;
    private final ExceptionUtilService exceptionUtilService;
    private final MissionSchedulerService missionSchedulerService;

    /**
     * Handles a failed mission execution, returning the instant at which the execution should
     * be retried, or null when the mission was given up (max attempts reached) <br>
     * <b>NOTICE:</b> the retry must NOT be scheduled from here: this runs inside the failing
     * db-scheduler execution, whose task instance still exists, so scheduling the same instance
     * id is silently ignored and the retry would be lost when the execution completes. The caller
     * (the db-scheduler task's completion handler) reschedules the current execution instead
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    @Transactional
    public Instant retryMissionIfPossible(Long missionId, MissionType missionType) {
        var mission = SpringRepositoryUtil.findByIdOrDie(missionRepository, missionId);
        if (mission.getAttemps() >= MAX_ATTEMPTS) {
            if (missionType.isUnitMission()) {
                returnMissionRegistrationBo.registerReturnMission(mission, null);
                mission.setResolved(true);
            } else if (missionType == MissionType.BUILD_UNIT) {
                obtainedUnitModificationBo.deleteByMissionId(mission.getId());
                missionRepository.delete(mission);
            } else if (missionType == MissionType.LEVEL_UP) {
                missionRepository.delete(mission);
            } else {
                throw new ProgrammingException("Should never ever happend");
            }
            return null;
        } else {
            mission.setAttemps(mission.getAttemps() + 1);
            mission.setTerminationDate(missionTimeManagerBo.computeTerminationDate(mission.getRequiredTime()));
            missionReportManagerBo.handleMissionReportSave(mission, buildCommonErrorReport(mission, missionType));
            missionRepository.save(mission);
            return missionSchedulerService.computeExecutionTime(mission);
        }
    }

    /**
     * Returns true if the input mission is of the expected type
     *
     * @param mission input mission
     * @param type    expected type
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     */
    public boolean isOfType(Mission mission, MissionType type) {
        return MissionType.valueOf(mission.getType().getCode()).equals(type);
    }

    public void checkMissionLimitNotReached(UserStorage user) {
        if (missionRepository.countByUserIdAndResolvedFalse(user.getId()) + 1 >= findUserMaxAllowedMissions(user)) {
            throw exceptionUtilService
                    .createExceptionBuilder(SgtBackendInvalidInputException.class, "I18N_ERR_MISSION_LIMIT_EXCEEDED")
                    .withDeveloperHintDoc(GameProjectsEnum.BUSINESS, getClass(), DocTypeEnum.EXCEPTIONS).build();
        }
    }

    private UnitMissionReportBuilder buildCommonErrorReport(Mission mission, MissionType missionType) {
        var reportBuilder = UnitMissionReportBuilder.create().withSenderUser(mission.getUser())
                .withId(mission.getId());
        if (missionType.isUnitMission()) {
            reportBuilder = reportBuilder.withSourcePlanet(mission.getSourcePlanet())
                    .withTargetPlanet(mission.getTargetPlanet())
                    .withInvolvedUnits(obtainedUnitRepository.findByMissionId(mission.getId()));
        }
        reportBuilder.withErrorInformation("Mission with id " + mission.getId() + " failed, please contact an admin!");
        return reportBuilder;
    }

    /**
     * Returns the max number of missions a user can run
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.0
     */
    private Integer findUserMaxAllowedMissions(UserStorage user) {
        return improvementBo.findUserImprovement(user).getMoreMissions().intValue() + 1;
    }
}
