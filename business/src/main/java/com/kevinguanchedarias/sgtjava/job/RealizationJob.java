package com.kevinguanchedarias.sgtjava.job;

import org.apache.log4j.Logger;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;

import com.kevinguanchedarias.sgtjava.business.MissionBo;
import com.kevinguanchedarias.sgtjava.business.UnitMissionBo;
import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.enumerations.MissionType;
import com.kevinguanchedarias.sgtjava.exception.CommonException;

public class RealizationJob extends QuartzJobBean {
	private static final Logger LOG = Logger.getLogger(RealizationJob.class);

	private Long missionId;
	private MissionBo missionBo;
	private UnitMissionBo unitMissionBo;

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
		injectSpringBeans(context);
		Mission mission = missionBo.findById(missionId);
		if (mission != null && !mission.getResolved()) {
			try {
				LOG.debug("Executing mission id " + mission.getId() + " of type "
						+ MissionType.valueOf(mission.getType().getCode()));
				switch (MissionType.valueOf(mission.getType().getCode())) {
				case BUILD_UNIT:
					missionBo.processBuildUnit(missionId);
					break;
				case LEVEL_UP:
					missionBo.processLevelUpAnUpgrade(missionId);
					break;
				case EXPLORE:
					unitMissionBo.processExplore(missionId);
					break;
				case RETURN_MISSION:
					unitMissionBo.proccessReturnMission(missionId);
					break;
				case GATHER:
					unitMissionBo.processGather(missionId);
					break;
				default:
					throw new CommonException("Unimplemented mission type " + mission.getType().getCode());
				}
			} catch (Exception e) {
				LOG.error("Unexpected fatal ecxception", e);
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
