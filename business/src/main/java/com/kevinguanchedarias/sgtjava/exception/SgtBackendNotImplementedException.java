package com.kevinguanchedarias.sgtjava.exception;

/**
 * Used to notify that there are some features, that has not been implemented
 * 
 * @author Kevin Guanche Darias
 *
 */
public class SgtBackendNotImplementedException extends CommonException {
	private static final long serialVersionUID = 4679029540396339601L;

	public SgtBackendNotImplementedException(String message) {
		super(message);
	}

}
