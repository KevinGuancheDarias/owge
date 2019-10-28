/**
 * 
 */
package com.kevinguanchedarias.owgejava.exception;

/**
 * Thrown when the backend configuration is wrong <br>
 * Usually is thrown at boot-time
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class InvalidConfigurationException extends CommonException {
	private static final long serialVersionUID = -1184031022292066529L;

	/**
	 * @param cause
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public InvalidConfigurationException(Exception cause) {
		super("Invalid configuation", cause);
	}

}
