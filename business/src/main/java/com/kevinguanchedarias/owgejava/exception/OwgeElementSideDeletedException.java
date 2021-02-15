package com.kevinguanchedarias.owgejava.exception;

/**
 * This exception is thrown when tried to save an entity but that has been
 * deleted by other process
 *
 * @since 0.9.19
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class OwgeElementSideDeletedException extends CommonException {
	private static final long serialVersionUID = -4314686177498588774L;

	public OwgeElementSideDeletedException(String message) {
		super(message);
	}

}
