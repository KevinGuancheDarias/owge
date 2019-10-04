package com.kevinguanchedarias.owgejava.exception;

import com.kevinguanchedarias.owgejava.pojo.GameBackendErrorPojo;

public class SgtBackendInvalidInputException extends CommonException {
	private static final long serialVersionUID = 7803777181397697297L;

	public SgtBackendInvalidInputException(String message) {
		super(message);
	}

	public SgtBackendInvalidInputException(String message, Exception e) {
		super(message, e);
	}

	/**
	 * @param gameBackendErrorPojo
	 * @param cause
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SgtBackendInvalidInputException(GameBackendErrorPojo gameBackendErrorPojo, Exception cause) {
		super(gameBackendErrorPojo, cause);
	}

	/**
	 * @param gameBackendErrorPojo
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public SgtBackendInvalidInputException(GameBackendErrorPojo gameBackendErrorPojo) {
		super(gameBackendErrorPojo);
	}

}
