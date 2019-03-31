package com.kevinguanchedarias.owgejava.exception;

public class SgtBackendSchedulerException extends CommonException {
	private static final long serialVersionUID = 1423002000568296439L;

	public SgtBackendSchedulerException(String message) {
		super(message);
	}

	public SgtBackendSchedulerException(String message, Exception cause) {
		super(message, cause);
	}
}
