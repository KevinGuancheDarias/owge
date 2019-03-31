package com.kevinguanchedarias.owgejava.exception;

public class SocketIoException extends CommonException {
	private static final long serialVersionUID = 4543237633183995859L;

	public SocketIoException(String message) {
		super(message);
	}

	public SocketIoException(String message, Exception cause) {
		super(message, cause);
	}

}
