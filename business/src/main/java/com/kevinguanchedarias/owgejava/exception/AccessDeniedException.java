package com.kevinguanchedarias.owgejava.exception;

public class AccessDeniedException extends RuntimeException {
	private static final long serialVersionUID = -1023035697857057773L;

	/**
	 * Used to simplify the creation of thrown exceptions on the Crud Traits magic
	 * 
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public static AccessDeniedException fromUnsupportedOperation() {
		return new AccessDeniedException("The specified operation is NOT supported by the target endpoint");
	}

	public AccessDeniedException(String message) {
		super(message);
	}
}
