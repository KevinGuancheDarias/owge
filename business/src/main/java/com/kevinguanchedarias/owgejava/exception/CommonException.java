package com.kevinguanchedarias.owgejava.exception;

import java.util.HashMap;
import java.util.Map;

import com.kevinguanchedarias.owgejava.pojo.GameBackendErrorPojo;

public class CommonException extends RuntimeException {

	private static final long serialVersionUID = -1921454839270502939L;

	private final String developerHint;
	private final Class<?> reporter;
	private final transient Map<String, Object> extra;

	/**
	 * Constructor from {@link GameBackendErrorPojo}
	 * 
	 * @param gameBackendErrorPojo
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public CommonException(GameBackendErrorPojo gameBackendErrorPojo) {
		super(gameBackendErrorPojo.getMessage());
		developerHint = gameBackendErrorPojo.getDeveloperHint();
		reporter = gameBackendErrorPojo.getReporter();
		extra = gameBackendErrorPojo.getExtra();
	}

	/**
	 * Constructor from {@link GameBackendErrorPojo}
	 * 
	 * @param gameBackendErrorPojo
	 * @param cause
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public CommonException(GameBackendErrorPojo gameBackendErrorPojo, Exception cause) {
		super(gameBackendErrorPojo.getMessage(), cause);
		developerHint = gameBackendErrorPojo.getDeveloperHint();
		reporter = gameBackendErrorPojo.getReporter();
		extra = gameBackendErrorPojo.getExtra();
	}

	/**
	 * Constructor that can add a <i>developerHint</i>
	 * 
	 * @param message
	 * @param developerHint
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public CommonException(String message, String developerHint) {
		super(message);
		this.developerHint = developerHint;
		reporter = null;
		extra = new HashMap<>();
	}

	/**
	 * Constructor that can add a <i>developerHint</i>
	 * 
	 * @param message
	 * @param developerHint
	 * @param e
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public CommonException(String message, String developerHint, Exception e) {
		super(message, e);
		this.developerHint = developerHint;
		this.reporter = null;
		extra = new HashMap<>();
	}

	public CommonException(String message) {
		super(message);
		developerHint = null;
		this.reporter = null;
		extra = new HashMap<>();
	}

	public CommonException(String message, Exception cause) {
		super(message, cause);
		developerHint = null;
		this.reporter = null;
		extra = new HashMap<>();
	}

	/**
	 * Adds "developerExtraHint" field to the "extra" section of the exception
	 * 
	 * @param hint
	 * @return
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public CommonException addExtraDeveloperHint(String hint) {
		extra.put("developerExtraHint", hint);
		return this;
	}

	/**
	 * @since 0.8.0
	 * @return the developerHint
	 */
	public String getDeveloperHint() {
		return developerHint;
	}

	/**
	 * @since 0.8.0
	 * @return the reporter
	 */
	public Class<?> getReporter() {
		return reporter;
	}

	/**
	 * @since 0.8.0
	 * @return the extra
	 */
	public Map<String, Object> getExtra() {
		return extra;
	}

}
