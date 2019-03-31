package com.kevinguanchedarias.owgejava.exception;

public class CommonException extends RuntimeException {

	private static final long serialVersionUID = -1921454839270502939L;

	public CommonException(String message) {
		super(message);
	}

	public CommonException(String message, Exception cause) {
		super(message, cause);
	}
}
