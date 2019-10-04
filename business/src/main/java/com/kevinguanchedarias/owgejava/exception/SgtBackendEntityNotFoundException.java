package com.kevinguanchedarias.owgejava.exception;

/**
 * 
 * 
 * @deprecated Use {@link NotFoundException#fromAffected(Class, Number)} instead
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
@Deprecated(since = "0.8.0")
public class SgtBackendEntityNotFoundException extends NotFoundException {
	private static final long serialVersionUID = 8520196326273985456L;

	public SgtBackendEntityNotFoundException(String message) {
		super(message);
	}

}
