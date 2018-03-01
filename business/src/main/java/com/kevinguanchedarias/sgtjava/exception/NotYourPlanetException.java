package com.kevinguanchedarias.sgtjava.exception;

public class NotYourPlanetException extends AccessDeniedException {
	private static final long serialVersionUID = 4593331660518689659L;

	public NotYourPlanetException(String message) {
		super(message);
	}

}
