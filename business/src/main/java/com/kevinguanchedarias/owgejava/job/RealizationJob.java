package com.kevinguanchedarias.owgejava.job;

import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.business.mission.MissionBaseService;
import com.kevinguanchedarias.owgejava.business.mission.MissionEventEmitterBo;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.repository.MissionRepository;
import com.kevinguanchedarias.owgejava.repository.MysqlInformationRepository;
import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.scheduling.quartz.QuartzJobBean;

public class RealizationJob extends QuartzJobBean {
    private static final Logger LOG = Logger.getLogger(RealizationJob.class);

    private Long missionId;
    private MissionRepository missionRepository;
    private MissionBaseService missionBaseService;
    private MissionBo missionBo;
    private UnitMissionBo unitMissionBo;
    private MysqlInformationRepository mysqlInformationRepository;
    private MissionEventEmitterBo missionEventEmitterBo;

    public Long getMissionId() {
        return missionId;
    }

    public void setMissionId(Long missionId) {
        this.missionId = missionId;
    }

    /**
     * Will be called at time of resolving a mission
     *
     * @author Kevin Guanche Darias
     */
    @Override
    protected void executeInternal(JobExecutionContext context) {
        Thread.currentThread().setName("OWGE_BACKGROUND_" + missionId);
        injectSpringBeans(context);
        var mission = missionRepository.findById(missionId).orElse(null);
        if (mission != null && !mission.getResolved()) {
            var missionType = MissionType.valueOf(mission.getType().getCode());
            try {
                LOG.debug("Executing mission id " + mission.getId() + " of type "
                        + MissionType.valueOf(mission.getType().getCode()) + "scheduled for " + context.getScheduledFireTime());
                if (missionType == MissionType.BUILD_UNIT || missionType == MissionType.LEVEL_UP) {
                    missionBo.runMission(missionId, missionType);
                } else {
                    unitMissionBo.runUnitMission(missionId, missionType);
                }
            } catch (Exception e) {
                LOG.error("Unexpected fatal exception when executing mission " + missionId, e);
                missionBaseService.retryMissionIfPossible(missionId, missionType, MissionBo.JOB_GROUP_NAME);
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
            LOG.error(
                    "Error Information " +
                            mysqlInformationRepository.findInnoDbStatus().toString() +
                            mysqlInformationRepository.findFullProcessInformation().toString()
                    , e
            );
        }
    }

    private void injectSpringBeans(JobExecutionContext context) {
        try {
            var schedulerContext = context.getScheduler().getContext();
            var applicationContext = (ApplicationContext) schedulerContext.get("applicationContext");
            missionBo = applicationContext.getBean(MissionBo.class);
            missionRepository = applicationContext.getBean(MissionRepository.class);
            missionBaseService = applicationContext.getBean(MissionBaseService.class);
            unitMissionBo = applicationContext.getBean(UnitMissionBo.class);
            mysqlInformationRepository = applicationContext.getBean(MysqlInformationRepository.class);
            missionEventEmitterBo = applicationContext.getBean(MissionEventEmitterBo.class);
        } catch (SchedulerException e) {
            LOG.error("Unexpected exception", e);
            throw new CommonException("Could not get application context inside job parser", e);
        }
    }

}
