/**
 * 
 */
package com.kevinguanchedarias.owgejava.pojo;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.ObjectUtils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.kevinguanchedarias.owgejava.exception.CommonException;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;
import com.kevinguanchedarias.owgejava.util.StackTraceUtil;

/**
 * Kevinsuite's CommonException doesn't have a developer hint, and I'm not
 * whiling to modify the original implementation
 * 
 * @since 0.8.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 */
public class GameBackendErrorPojo extends BackendErrorPojo {
	private String developerHint;
	private Class<?> reporter;
	private Map<String, Object> extra = new HashMap<>();

	public GameBackendErrorPojo() {

	}

	/**
	 * @param developerHint
	 * @param reporter
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public GameBackendErrorPojo(String message, String developerHint, Class<?> reporter) {
		this.setMessage(message);
		this.developerHint = developerHint;
		this.reporter = reporter;
	}

	/**
	 * @param e
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public GameBackendErrorPojo(Exception e) {
		super(e);
	}

	/**
	 * 
	 * @param e
	 * @since 0.8.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	@SuppressWarnings("unchecked")
	public GameBackendErrorPojo(CommonException e) {
		this((Exception) e);
		extra = ObjectUtils.firstNonNull(e.getExtra(), extra);
		if (e.getDeveloperHint() != null) {
			developerHint = e.getDeveloperHint();
		} else if (ProgrammingException.class.isInstance(e)) {
			developerHint = "Looks like, you, as developer has made a mistake :O D: ";
			StackTraceElement[] trace = e.getStackTrace();
			extra.put("noobTrace", Arrays.stream(trace, 1, 5).map(StackTraceUtil::formatTrace).toArray());
		}
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
	 * @param developerHint the developerHint to set
	 */
	public void setDeveloperHint(String developerHint) {
		this.developerHint = developerHint;
	}

	/**
	 * Class that reports the error
	 * 
	 * @since 0.8.0
	 * @return the reporter
	 */
	public String getReporterAsString() {
		return reporter == null ? "" : reporter.getName();
	}

	@JsonIgnore
	public Class<?> getReporter() {
		return reporter;
	}

	/**
	 * @since 0.8.0
	 * @param reporter the reporter to set
	 */
	public void setReporter(Class<?> reporter) {
		this.reporter = reporter;
	}

	/**
	 * @since 0.8.0
	 * @return the extra
	 */
	public Map<String, Object> getExtra() {
		return extra;
	}

	/**
	 * Extra information that anyone can add to a class
	 * 
	 * @since 0.8.0
	 * @param extra the extra to set
	 */
	public void setExtra(Map<String, Object> extra) {
		this.extra = extra;
	}
}
