package com.kevinguanchedarias.owgejava.test.stub;

import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.pojo.GameBackendErrorPojo;

/**
 *
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class TestBuilderException extends CommonException {
	private static final long serialVersionUID = -3556556102376208557L;

	/**
	 * @param gameBackendErrorPojo
	 * @param cause
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TestBuilderException(GameBackendErrorPojo gameBackendErrorPojo, Exception cause) {
		super(gameBackendErrorPojo, cause);
		// TODO Auto-generated constructor stub
	}

	/**
	 * @param gameBackendErrorPojo
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public TestBuilderException(GameBackendErrorPojo gameBackendErrorPojo) {
		super(gameBackendErrorPojo);
		// TODO Auto-generated constructor stub
	}

}
