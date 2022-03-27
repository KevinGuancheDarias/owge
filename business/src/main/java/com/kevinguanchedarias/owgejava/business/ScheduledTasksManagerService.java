package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;

import java.util.function.Consumer;

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
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    void addHandler(String event, Consumer<ScheduledTask> consumer);

    /**
     * Registers an event that will run after certain time
     *
     * @return The id that represents the job
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    String registerEvent(ScheduledTask task, long deliverAfterSeconds);

    /**
     * Cancels an event before it even fires
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    void cancelEvent(String id, String event);

}
