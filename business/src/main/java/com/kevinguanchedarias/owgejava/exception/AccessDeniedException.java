package com.kevinguanchedarias.owgejava.exception;

public class AccessDeniedException extends RuntimeException {
	private static final long serialVersionUID = -1023035697857057773L;

	public AccessDeniedException(String message) {
		super(message);
	}
}
