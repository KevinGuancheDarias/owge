package com.kevinguanchedarias.sgtjava.business;

import java.util.Date;
import java.util.List;

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
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.transaction.annotation.Transactional;

import com.kevinguanchedarias.sgtjava.builder.UnitMissionReportBuilder;
import com.kevinguanchedarias.sgtjava.entity.Mission;
import com.kevinguanchedarias.sgtjava.entity.MissionReport;
import com.kevinguanchedarias.sgtjava.entity.UserStorage;
import com.kevinguanchedarias.sgtjava.enumerations.MissionType;
import com.kevinguanchedarias.sgtjava.exception.PlanetNotFoundException;
import com.kevinguanchedarias.sgtjava.exception.SgtBackendInvalidInputException;
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

	private static final Integer MAX_ATTEMPS = 3;

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

	@Autowired
	private MissionReportBo missionReportBo;

	@Autowired
	private transient ApplicationContext applicationContext;

	public abstract String getGroupName();

	public abstract Logger getLogger();

	@Override
	public JpaRepository<Mission, Number> getRepository() {
		return missionRepository;
	}

	@Transactional
	public void retryMissionIfPossible(Long missionId) {
		Mission mission = findById(missionId);
		MissionType missionType = MissionType.valueOf(mission.getType().getCode());
		mission.setUser(userStorageBo.findOneByMission(mission));
		if (mission.getAttemps() >= MAX_ATTEMPS) {
			if (missionType.isUnitMission() && missionType != MissionType.RETURN_MISSION
					&& missionType != MissionType.BUILD_UNIT) {
				findUnitMissionBoInstance().adminRegisterReturnMission(mission);
			} else if (missionType == MissionType.BUILD_UNIT) {
				obtainedUnitBo.deleteByMissionId(mission.getId());
			}
			resolveMission(mission);
		} else {
			mission.setAttemps(mission.getAttemps() + 1);
			mission.setTerminationDate(computeTerminationDate(mission.getRequiredTime()));
			hanleMissionReportSave(mission, buildCommonErrorReport(mission, missionType));
			scheduleMission(mission);
			save(mission);
		}
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
		com.kevinguanchedarias.sgtjava.entity.MissionType retVal = missionTypeRepository.findOneByCode(type.name());
		if (retVal == null) {
			throw new SgtBackendInvalidInputException("No MissionType " + type.name() + " was found in the database");
		}
		return retVal;
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
		return new TriggerKey("trigger_" + mission.getId() + "_" + mission.getAttemps(), getGroupName());
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

	/**
	 * Returns true if the input mission is of the expected type
	 * 
	 * @param mission
	 *            input mission
	 * @param type
	 *            expected type
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	protected boolean isOfType(Mission mission, MissionType type) {
		return MissionType.valueOf(mission.getType().getCode()).equals(type);
	 * Saves the MissionReport to the database
	 * 
	 * @param mission
	 * @param builder
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	protected void hanleMissionReportSave(Mission mission, UnitMissionReportBuilder builder) {
		MissionReport missionReport = new MissionReport("{}", mission);
		missionReport.setUser(mission.getUser());
		missionReport = missionReportBo.save(missionReport);
		missionReport.setReportDate(new Date());
		missionReport.setJsonBody(builder.withId(missionReport.getId()).buildJson());
		mission.setReport(missionReport);
	}

	protected void hanleMissionReportSave(Mission mission, UnitMissionReportBuilder builder, List<UserStorage> users) {
		users.forEach(currentUser -> {
			MissionReport missionReport = new MissionReport("{}", mission);
			missionReport.setUser(currentUser);
			missionReport = missionReportBo.save(missionReport);
			missionReport.setReportDate(new Date());
			missionReport.setJsonBody(builder.withId(missionReport.getId()).buildJson());
			mission.setReport(missionReport);
		});
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
