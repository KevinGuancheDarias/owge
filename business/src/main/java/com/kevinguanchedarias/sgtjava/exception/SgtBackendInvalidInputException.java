package com.kevinguanchedarias.sgtjava.exception;

import java.io.IOException;

public class SgtBackendInvalidInputException extends CommonException {
	private static final long serialVersionUID = 7803777181397697297L;

	public SgtBackendInvalidInputException(String message) {
		super(message);
	}

	public SgtBackendInvalidInputException(String message, IOException e) {
		super(message, e);
	}

}
