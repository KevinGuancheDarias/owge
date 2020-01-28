package com.kevinguanchedarias.owgejava.business;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import org.apache.log4j.Logger;

import com.kevinguanchedarias.owgejava.pojo.ScheduledTask;

/**
 * Abstract implementation that all services wanting to implement
 * {@link ScheduledTasksManagerService} should extend
 * 
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 */
public abstract class AbstractScheduledTasksManagerService implements ScheduledTasksManagerService {
	private static final Logger ABSTRACT_LOGGER = Logger.getLogger(AbstractScheduledTasksManagerService.class);

	protected Map<String, List<Consumer<ScheduledTask<Serializable>>>> handlers = new HashMap<>();

	@Override
	public void addHandler(String event, Consumer<ScheduledTask<Serializable>> consumer) {
		List<Consumer<ScheduledTask<Serializable>>> eventHandlers;
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
	 * @param event
	 * @param body  The body of the event
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	protected void fireHandlersForEvent(ScheduledTask<Serializable> task) {
		String event = task.getType();
		if (handlers.containsKey(event)) {
			handlers.get(event).forEach(current -> current.accept(task));
		} else {
			ABSTRACT_LOGGER.warn("No handler for event " + event);
		}
	}
}
