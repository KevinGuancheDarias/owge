package com.kevinguanchedarias.sgtjava.business;

import java.util.Date;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
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
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.enumerations.MissionType;
import com.kevinguanchedarias.sgtjava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendSchedulerException;
import com.kevinguanchedarias.sgtjava.exception.UserNotFoundException;
import com.kevinguanchedarias.sgtjava.job.RealizationJob;
import com.kevinguanchedarias.sgtjava.repository.MissionRepository;
import com.kevinguanchedarias.sgtjava.repository.MissionTypeRepository;

/**
 * Contains methods and properties shared between all MissionBo types
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public abstract class AbstractMissionBo implements BaseBo<Mission> {
	private static final long serialVersionUID = 3252246009672348672L;

	@Autowired
	protected MissionRepository missionRepository;

	@Autowired
	protected ObtainedUpradeBo obtainedUpgradeBo;

	@Autowired
	protected ObjectRelationBo objectRelationBo;

	@Autowired
	protected UpgradeBo upgradeBo;

	@Autowired
	protected UserStorageBo userStorageBo;

	@Autowired
	protected MissionTypeRepository missionTypeRepository;

	@Autowired
	protected UserImprovementBo userImprovementBo;

	@Autowired
	protected RequirementBo requirementBo;

	@Autowired
	protected UnlockedRelationBo unlockedRelationBo;

	@Autowired
	protected UnitBo unitBo;

	@Autowired
	protected ObtainedUnitBo obtainedUnitBo;

	@Autowired
	protected PlanetBo planetBo;

	@Autowired(required = false)
	protected transient SchedulerFactoryBean schedulerFactory;

	public abstract String getGroupName();

	public abstract Logger getLogger();

	@Override
	public JpaRepository<Mission, Number> getRepository() {
		return missionRepository;
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
	 * @throws UserNotFoundException
	 *             If user doesn't exists
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	protected void checkUserExists(Integer userId) {
		if (userStorageBo.findById(userId) == null) {
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
	 * @param type
	 *            enum based mission type
	 * @return persisted mission type
	 * @author Kevin Guanche Darias
	 */
	protected com.kevinguanchedarias.sgtjava.entity.MissionType findMissionType(MissionType type) {
		return missionTypeRepository.findOneByCode(type.name());
	}

	/**
	 * Returns the date that the mission will have according to the required
	 * time
	 * 
	 * @param requiredTime
	 *            ammount of time required (in seconds)
	 * @return
	 * @author Kevin Guanche Darias
	 */
	protected Date computeTerminationDate(Double requiredTime) {
		return (new DateTime()).plusSeconds(requiredTime.intValue()).toDate();
	}

	protected void scheduleMission(Mission mission) {
		if (schedulerFactory != null) {
			String jobName = mission.getId().toString();
			Scheduler scheduler = schedulerFactory.getScheduler();
			JobKey jobKey = new JobKey(jobName, getGroupName());
			TriggerKey triggerKey = genTriggerKey(mission);
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
				getLogger().error(e);
				throw new SgtBackendSchedulerException("Couldn't store job: " + jobName, e);
			}
		}
	}

	protected TriggerKey genTriggerKey(Mission mission) {
		return new TriggerKey("trigger_" + mission.getId().toString(), getGroupName());
	}

	/**
	 * Defines the mission as resolved and saves it to the database
	 * 
	 * @param mission
	 *            Mission to persist
	 * @return Persisted entity
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	protected Mission resolveMission(Mission mission) {
		mission.setResolved(true);
		return missionRepository.saveAndFlush(mission);
	}
}
