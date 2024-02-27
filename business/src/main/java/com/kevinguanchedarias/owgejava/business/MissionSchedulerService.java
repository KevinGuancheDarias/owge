package com.kevinguanchedarias.owgejava.business;

import com.github.kagkarlsson.scheduler.Scheduler;
import com.github.kagkarlsson.scheduler.exceptions.TaskInstanceNotFoundException;
import com.kevinguanchedarias.owgejava.entity.Mission;
import com.kevinguanchedarias.owgejava.job.DbSchedulerRealizationJob;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * <b>NOTICE:</b> Due to changes in the way Spring Boot handles Quartz jobs, the
 * manager has to have his own transaction
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Service
@Slf4j
@AllArgsConstructor
public class MissionSchedulerService {
    private static final long DELAY_HANDLE = 2;

    @Lazy
    private final Scheduler scheduler;

    /**
     * Schedules a mission <br>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void scheduleMission(Mission mission) {
        scheduler.schedule(
                DbSchedulerRealizationJob.BASIC_ONE_TIME_TASK.instance(mission.getId().toString()),
                Instant.now().plusSeconds(mission.getRequiredTime().longValue() - DELAY_HANDLE)
        );
    }

    /**
     * Unschedules the mission<br>
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void abortMissionJob(Mission mission) {
        try {
            scheduler.cancel(DbSchedulerRealizationJob.BASIC_ONE_TIME_TASK.instance(mission.getId().toString()));
        } catch (TaskInstanceNotFoundException e) {
            log.warn("Not able to cancel mission id {}, because, it was already cancelled", mission.getId());
        }
    }
}
