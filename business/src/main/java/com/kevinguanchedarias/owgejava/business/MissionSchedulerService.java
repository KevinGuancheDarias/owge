package com.kevinguanchedarias.owgejava.business;

import org.apache.log4j.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.exception.SgtBackendSchedulerException;
import com.kevinguanchedarias.owgejava.job.RealizationJob;

/**
 * <b>NOTICE:</b> Due to changes in the way Spring Boot handles Quartz jobs, the
 * manager has to have his own transaction
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Service
public class MissionSchedulerService {

	private static final Logger LOG = Logger.getLogger(MissionSchedulerService.class);

	@Autowired(required = false)
	protected SchedulerFactoryBean schedulerFactory;

	/**
	 * Schedules a mission <br>
	 *
	 *
	 * @param groupName
	 * @param mission
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void scheduleMission(String groupName, Mission mission) {
		if (schedulerFactory != null) {
			String jobName = mission.getId().toString();
			Scheduler scheduler = schedulerFactory.getScheduler();
			JobKey jobKey = new JobKey(jobName, groupName);
			TriggerKey triggerKey = genTriggerKey(groupName, mission);
			JobDataMap jobData = new JobDataMap();
			jobData.put("missionId", mission.getId().toString());
			JobDetail jobDetail = JobBuilder.newJob(RealizationJob.class).withIdentity(jobKey).setJobData(jobData)
					.build();
			SimpleTrigger trigger = (SimpleTrigger) TriggerBuilder.newTrigger().withIdentity(triggerKey)
					.forJob(jobDetail).startAt(mission.getTerminationDate()).forJob(jobKey).build();
			try {
				scheduler.addJob(jobDetail, true, true);
				scheduler.scheduleJob(trigger);
			} catch (SchedulerException e) {
				LOG.error(e);
				throw new SgtBackendSchedulerException("Couldn't store job: " + jobName, e);
			}
		}
	}

	/**
	 * Unschedules the mission<br>
	 *
	 * @param groupName
	 * @param mission
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public void abortMissionJob(String groupName, Mission mission) {
		if (schedulerFactory != null) {
			try {
				schedulerFactory.getScheduler().unscheduleJob(genTriggerKey(groupName, mission));
			} catch (SchedulerException e) {
				LOG.error("Couldn't remove job", e);
				throw new SgtBackendSchedulerException("Couldn't remove job", e);
			}
		}
	}

	protected TriggerKey genTriggerKey(String groupName, Mission mission) {
		return new TriggerKey("trigger_" + mission.getId() + "_" + mission.getAttemps(), groupName);
	}
}
