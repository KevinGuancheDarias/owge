package com.kevinguanchedarias.owgejava.job;

import com.github.kagkarlsson.scheduler.task.TaskWithoutDataDescriptor;
import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionBaseService;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.MysqlInformationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class DbSchedulerRealizationJob {
    public static final TaskWithoutDataDescriptor BASIC_ONE_TIME_TASK = new TaskWithoutDataDescriptor("mission-run");
    private final MissionRepository missionRepository;
    private final UnitMissionBo unitMissionBo;
    private final MissionBaseService missionBaseService;
    private final MissionEventEmitterBo missionEventEmitterBo;
    private final MysqlInformationRepository mysqlInformationRepository;

    @Autowired
    @Lazy
    private MissionBo missionBo;

    public void execute(Long missionId) {
        Thread.currentThread().setName("OWGE_BACKGROUND_" + missionId);
        var mission = missionRepository.findById(missionId).orElse(null);
        if (mission != null && !mission.getResolved()) {
            var missionType = MissionType.valueOf(mission.getType().getCode());
            try {
                log.debug("Executing mission id {} of type {}", missionId, MissionType.valueOf(mission.getType().getCode()));
                if (missionType == MissionType.BUILD_UNIT || missionType == MissionType.LEVEL_UP) {
                    missionBo.runMission(missionId, missionType);
                } else {
                    unitMissionBo.runUnitMission(missionId, missionType);
                }
            } catch (Exception e) {
                log.error("Unexpected fatal exception when executing mission {}", missionId, e);
                missionBaseService.retryMissionIfPossible(missionId, missionType);
                var user = mission.getUser();
                if (missionType.isUnitMission()) {
                    missionEventEmitterBo.emitUnitMissions(user.getId());
                    missionEventEmitterBo.emitEnemyMissionsChange(mission);
                } else if (missionType == MissionType.LEVEL_UP) {
                    missionBo.emitRunningUpgrade(user);
                } else if (missionType == MissionType.BUILD_UNIT) {
                    missionEventEmitterBo.emitUnitBuildChange(user.getId());
                } else {
                    throw new ProgrammingException("It's impossible!!!!");
                }
                maybeLogPessimistic(e);
            }
        }
    }

    private void maybeLogPessimistic(Exception e) {
        if (e instanceof PessimisticLockingFailureException) {
            log.error(
                    "Error Information {} {}",
                    mysqlInformationRepository.findInnoDbStatus(),
                    mysqlInformationRepository.findFullProcessInformation()
                    , e
            );
        }
    }

}
