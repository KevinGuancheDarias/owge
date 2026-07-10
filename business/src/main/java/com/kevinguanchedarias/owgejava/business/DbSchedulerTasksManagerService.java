package com.kevinguanchedarias.owgejava.business;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.github.kagkarlsson.scheduler.task.TaskWithoutDataDescriptor;
import com.kevinguanchedarias.owgejava.exception.SgtBackendSchedulerException;
import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Map;

/**
 * db-scheduler based task scheduling: the events ride the same
 * {@code scheduled_tasks} table as {@code mission-run}, with
 * {@code task_instance} carrying the domain entity id (the fired handlers only
 * need that id). Replaces the Quartz-based implementation, which was legacy
 * from before the db-scheduler lib was added.
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 1.0.0
 */
@Service
@Slf4j
public class DbSchedulerTasksManagerService extends AbstractScheduledTasksManagerService {
    public static final String TIME_SPECIAL_EFFECT_END_EVENT = "TIME_SPECIAL_EFFECT_END";
    public static final String TIME_SPECIAL_IS_READY_EVENT = "TIME_SPECIAL_IS_READY";
    public static final String UNIT_EXPIRED_EVENT = "UNIT_EXPIRED";

    public static final TaskWithoutDataDescriptor TIME_SPECIAL_EFFECT_END_TASK =
            new TaskWithoutDataDescriptor(TIME_SPECIAL_EFFECT_END_EVENT);
    public static final TaskWithoutDataDescriptor TIME_SPECIAL_IS_READY_TASK =
            new TaskWithoutDataDescriptor(TIME_SPECIAL_IS_READY_EVENT);
    public static final TaskWithoutDataDescriptor UNIT_EXPIRED_TASK =
            new TaskWithoutDataDescriptor(UNIT_EXPIRED_EVENT);

    private static final Map<String, TaskWithoutDataDescriptor> DESCRIPTORS_BY_EVENT = Map.of(
            TIME_SPECIAL_EFFECT_END_EVENT, TIME_SPECIAL_EFFECT_END_TASK,
            TIME_SPECIAL_IS_READY_EVENT, TIME_SPECIAL_IS_READY_TASK,
            UNIT_EXPIRED_EVENT, UNIT_EXPIRED_TASK
    );

    private final Scheduler scheduler;

    public DbSchedulerTasksManagerService(@Lazy Scheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @Override
    public String registerEvent(ScheduledTask task, long deliverAfterSeconds) {
        var instanceId = String.valueOf(task.getContent());
        task.setId(instanceId);
        log.debug("Scheduling event with id " + instanceId + " and of type " + task.getType());
        scheduler.schedule(
                findDescriptor(task.getType()).instance(instanceId),
                Instant.now().plusSeconds(deliverAfterSeconds)
        );
        return instanceId;
    }

    @Override
    public void cancelEvent(String id, String event) {
        try {
            scheduler.cancel(findDescriptor(event).instance(id));
        } catch (TaskInstanceNotFoundException e) {
            log.warn("Not able to cancel event {} with id {}, because it was already cancelled", event, id);
        }
    }

    /**
     * Entry point for the db-scheduler execution handlers (see
     * {@code DbSchedulerConfiguration}): rebuilds the {@link ScheduledTask} from
     * the fired row and dispatches it to the registered handlers.
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 1.0.0
     */
    public void fire(String event, String instanceId) {
        fireHandlersForEvent(new ScheduledTask(instanceId, event, Long.valueOf(instanceId)));
    }

    private TaskWithoutDataDescriptor findDescriptor(String event) {
        var descriptor = DESCRIPTORS_BY_EVENT.get(event);
        if (descriptor == null) {
            throw new SgtBackendSchedulerException("No db-scheduler task descriptor for event " + event);
        }
        return descriptor;
    }
}
