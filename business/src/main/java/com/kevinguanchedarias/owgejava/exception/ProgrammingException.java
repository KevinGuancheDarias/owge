package com.kevinguanchedarias.owgejava.exception;

import org.apache.log4j.Logger;

import com.kevinguanchedarias.owgejava.util.StackTraceUtil;

public class ProgrammingException extends CommonException {
	private static final long serialVersionUID = -7298435329894498401L;
	private static final Logger LOG = Logger.getLogger(ProgrammingException.class);

	/**
	 * Default constructor for {@link ProgrammingException}, <br>
	 * <b>As of 0.8.0 logs the exception</b>
	 * 
	 * @param message
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public ProgrammingException(String message) {
		super(message);
		StackTraceElement stackTraceElement = new RuntimeException().getStackTrace()[1];
		LOG.fatal("[" + StackTraceUtil.formatTrace(stackTraceElement) + "] " + message);
	}

}
