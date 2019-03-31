package com.kevinguanchedarias.owgejava.exception;

/**
 * Thrown when the websocket server, did something it should not, often because
 * of a coding error
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class SocketServerProgrammingException extends CommonException {
	private static final long serialVersionUID = -2081382312529641130L;

	public SocketServerProgrammingException(String message) {
		super(message);
	}

}
