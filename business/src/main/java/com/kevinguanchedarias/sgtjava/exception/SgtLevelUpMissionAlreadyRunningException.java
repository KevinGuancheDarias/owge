package com.kevinguanchedarias.sgtjava.exception;

/**
 * This exception is thrown when there is a level up mission running, and user
 * Tries to run other
 * 
 * @author Kevin Guanche Darias
 *
 */
public class SgtLevelUpMissionAlreadyRunningException extends CommonException {
	private static final long serialVersionUID = -3887373140216678467L;

	public SgtLevelUpMissionAlreadyRunningException(String message) {
		super(message);
	}

}
