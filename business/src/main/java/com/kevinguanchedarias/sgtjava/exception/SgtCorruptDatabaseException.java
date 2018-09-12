package com.kevinguanchedarias.sgtjava.exception;

/**
 * Exceptions thrown when the database has bad information
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class SgtCorruptDatabaseException extends CommonException {
	private static final long serialVersionUID = -225470132358840261L;

	public SgtCorruptDatabaseException(String message) {
		super(message);
	}

}
