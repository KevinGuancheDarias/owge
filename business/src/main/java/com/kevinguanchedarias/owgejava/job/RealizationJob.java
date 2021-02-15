package com.kevinguanchedarias.owgejava.job;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.dao.PessimisticLockingFailureException;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.kevinguanchedarias.owgejava.business.MissionBo;
import com.kevinguanchedarias.owgejava.business.UnitMissionBo;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.entity.UserStorage;
import com.kevinguanchedarias.owgejava.enumerations.MissionType;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.repository.MysqlInformationRepository;

public class RealizationJob extends QuartzJobBean {
	private static final Logger LOG = Logger.getLogger(RealizationJob.class);

	private Long missionId;
	private MissionBo missionBo;
	private UnitMissionBo unitMissionBo;
	private MysqlInformationRepository mysqlInformationRepository;

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
	protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
		Thread.currentThread().setName("OWGE_BACKGROUND_" + missionId);
		injectSpringBeans(context);
		Mission mission = missionBo.findById(missionId);
		if (mission != null && !mission.getResolved()) {
			MissionType missionType = MissionType.valueOf(mission.getType().getCode());
			try {
				LOG.debug("Executing mission id " + mission.getId() + " of type "
						+ MissionType.valueOf(mission.getType().getCode()));
				switch (missionType) {
				case BUILD_UNIT:
				case LEVEL_UP:
					missionBo.runMission(missionId, missionType);
					break;
				default:
					unitMissionBo.runUnitMission(missionId, missionType);
				}
			} catch (Exception e) {
				LOG.error("Unexpected fatal exception when executing mission " + missionId, e);
				missionBo.retryMissionIfPossible(missionId, missionType);
				UserStorage user = mission.getUser();
				if (missionType.isUnitMission()) {
					unitMissionBo.emitMissions(user.getId());
					unitMissionBo.emitEnemyMissionsChange(mission);
				} else if (missionType == MissionType.LEVEL_UP) {
					missionBo.emitRunningUpgrade(user);
				} else if (missionType == MissionType.BUILD_UNIT) {
					missionBo.emitUnitBuildChange(user.getId());
				} else {
					throw new ProgrammingException("It's impossible!!!!");
				}
				if (e instanceof PessimisticLockingFailureException || e instanceof CannotAcquireLockException) {
					LOG.error(mysqlInformationRepository.findInnoDbStatus());
				}
			}
		}
	}

	private void injectSpringBeans(JobExecutionContext context) {
		try {
			SchedulerContext schedulercontext = context.getScheduler().getContext();
			ApplicationContext applicationContext = (ApplicationContext) schedulercontext.get("applicationContext");
			missionBo = applicationContext.getBean(MissionBo.class);
			unitMissionBo = applicationContext.getBean(UnitMissionBo.class);
		} catch (SchedulerException e) {
			LOG.error("Unexpected exception", e);
			throw new CommonException("Could not get application context inside job parser", e);
		}
	}

}
