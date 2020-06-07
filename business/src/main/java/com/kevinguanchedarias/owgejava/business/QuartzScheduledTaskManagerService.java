package com.kevinguanchedarias.owgejava.business;

import java.util.Date;
import java.util.UUID;

import javax.annotation.PostConstruct;

import org.apache.log4j.Logger;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerContext;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.google.gson.Gson;
import com.kevinguanchedarias.owgejava.exception.SgtBackendSchedulerException;
import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;

/**
 * Quartz based task scheduling
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 */
@Service
public class QuartzScheduledTaskManagerService extends AbstractScheduledTasksManagerService {

	private static final Logger LOG = Logger.getLogger(QuartzScheduledTaskManagerService.class);

	private Gson gson;

	public static class JobHandler extends QuartzJobBean {
		private String task;

		/**
		 * @return the task
		 */
		public String getTask() {
			return task;
		}

		/**
		 * @param task the task to set
		 */
		public void setTask(String task) {
			this.task = task;
		}

		@Override
		protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
			try {
				SchedulerContext schedulercontext = context.getScheduler().getContext();
				ApplicationContext applicationContext = (ApplicationContext) schedulercontext.get("applicationContext");
				QuartzScheduledTaskManagerService service = applicationContext
						.getBean(QuartzScheduledTaskManagerService.class);
				service.fireHandlersForEvent(new Gson().fromJson(task, ScheduledTask.class));
			} catch (SchedulerException e) {
				throw new SgtBackendSchedulerException("Could not get application context inside job parser", e);
			}
		}

	}

	@Autowired(required = false)
	protected SchedulerFactoryBean schedulerFactory;

	@PostConstruct
	public void init() {
		gson = new Gson();
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	@Override
	public String registerEvent(ScheduledTask task, long deliverAfterSeconds) {
		return doSchedule(task, deliverAfterSeconds);
	}

	@Override
	public void cancelEvent(String id, String event) {
		try {
			schedulerFactory.getScheduler().unscheduleJob(genTriggerKey(id, event));
		} catch (SchedulerException e) {
			throw new SgtBackendSchedulerException("Not able to remove the job", e);
		}
	}

	private String doSchedule(ScheduledTask task, long deliverAfterSeconds) {
		if (schedulerFactory != null) {
			String jobName = UUID.randomUUID().toString();
			Scheduler scheduler = schedulerFactory.getScheduler();
			JobKey jobKey = new JobKey(jobName, task.getType());
			TriggerKey triggerKey = genTriggerKey(jobName, task.getType());
			JobDataMap jobData = new JobDataMap();
			task.setId(jobName);
			jobData.put("eventUuid", jobName);
			jobData.put("task", gson.toJson(task));
			LOG.debug("Scheduling event with id " + jobName + " and of type " + task.getType());
			JobDetail jobDetail = JobBuilder.newJob(JobHandler.class).withIdentity(jobKey).setJobData(jobData).build();
			Date startAt = new Date(new Date().getTime() + (deliverAfterSeconds * 1000));
			SimpleTrigger trigger = (SimpleTrigger) TriggerBuilder.newTrigger().withIdentity(triggerKey)
					.forJob(jobDetail).startAt(startAt).forJob(jobKey).build();
			try {
				scheduler.addJob(jobDetail, true, true);
				scheduler.scheduleJob(trigger);
				return jobName;
			} catch (SchedulerException e) {
				throw new SgtBackendSchedulerException("Couldn't store job: " + jobName, e);
			}
		} else {
			throw new SgtBackendSchedulerException("Null factory should never happend");
		}
	}

	private TriggerKey genTriggerKey(String uuid, String eventType) {
		return new TriggerKey("trigger_" + uuid, eventType);
	}
}
