package com.kevinguanchedarias.owgejava.business;

import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Abstract implementation that all services wanting to implement
 * {@link ScheduledTasksManagerService} should extend
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 */
@Slf4j
public abstract class AbstractScheduledTasksManagerService implements ScheduledTasksManagerService {

    protected Map<String, List<Consumer<ScheduledTask>>> handlers = new HashMap<>();

    @Override
    public void addHandler(String event, Consumer<ScheduledTask> consumer) {
        List<Consumer<ScheduledTask>> eventHandlers;
        if (handlers.containsKey(event)) {
            eventHandlers = handlers.get(event);
        } else {
            eventHandlers = new ArrayList<>();
            eventHandlers.add(consumer);
        }
        handlers.put(event, eventHandlers);
    }

    /**
     * Fires all the handlers for the registered event with the specified
     * information
     *
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.8.1
     */
    protected void fireHandlersForEvent(ScheduledTask task) {
        String event = task.getType();
        if (handlers.containsKey(event)) {
            handlers.get(event).forEach(current -> current.accept(task));
        } else {
            log.warn("No handler for event " + event);
        }
    }
}
