package com.kevinguanchedarias.sgtjava.exception;

public class SgtBackendInvalidInputException extends CommonException {
	private static final long serialVersionUID = 7803777181397697297L;

	public SgtBackendInvalidInputException(String message) {
		super(message);
	}

	public SgtBackendInvalidInputException(String message, Exception e) {
		super(message, e);
	}

}
