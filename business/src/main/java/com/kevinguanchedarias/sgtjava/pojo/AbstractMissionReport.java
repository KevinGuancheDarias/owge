package com.kevinguanchedarias.sgtjava.pojo;

import java.util.HashMap;
import java.util.Map;

import com.kevinguanchedarias.sgtjava.exception.ProgrammingException;

public abstract class AbstractMissionReport {

	private static final Map<String, Boolean> VALID_EVENTS;
	static {
		VALID_EVENTS = new HashMap<>();
		VALID_EVENTS.put("mission_explore", true);
	}

	public AbstractMissionReport() {
		checkEventName();
	}

	/**
	 * Returns the Socket IO event name for the report
	 * 
	 * @return
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public abstract String getEventName();

	/**
	 * Checks if the socket io event name is valid
	 * 
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public final void checkEventName() {
		if (VALID_EVENTS.get(getEventName()) == null) {
			throw new ProgrammingException("Invalid event name " + getEventName());
		}

	}
}
