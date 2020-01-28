package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;
import java.util.function.Consumer;

import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;

/**
 * A manager for scheduled task
 * 
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 */
public interface ScheduledTasksManagerService {

	/**
	 * Adds a new handler that runs when a task is ready
	 * 
	 * @param <T>      Type of the consumer (deduced by JVM)
	 * @param event    The identifier of the task to listen to
	 * @param consumer
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public void addHandler(String event, Consumer<ScheduledTask<Serializable>> consumer);

	/**
	 * Registers an event that will run after certain time
	 * 
	 * @param <T>
	 * @param task
	 * @param deliverAfterSeconds
	 * @return The id that represents the job
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public <T extends Serializable> String registerEvent(ScheduledTask<T> task, long deliverAfterSeconds);

	/**
	 * Cancels an event before it even fires
	 * 
	 * @param id
	 * @param event
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public void cancelEvent(String id, String event);

}
