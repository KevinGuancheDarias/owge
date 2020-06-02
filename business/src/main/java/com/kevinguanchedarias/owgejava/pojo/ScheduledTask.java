package com.kevinguanchedarias.owgejava.pojo;

import java.io.Serializable;

/**
 * Represents a scheduled task
 *
 * @param <T>
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.8.1
 */
public class ScheduledTask implements Serializable {
	private static final long serialVersionUID = -6405371104931890932L;

	private String id;
	private String type;
	private Object content;

	/**
	 *
	 *
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public ScheduledTask() {

	}

	/**
	 * @param type
	 * @param content
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public ScheduledTask(String type, Object content) {
		super();
		this.type = type;
		this.content = content;
	}

	/**
	 * The id in the Schedule system
	 *
	 * @return the id
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public String getId() {
		return id;
	}

	/**
	 * @param id the id to set
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public void setId(String id) {
		this.id = id;
	}

	/**
	 * Type of event
	 *
	 * @return the type
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public String getType() {
		return type;
	}

	/**
	 * @param type the type to set
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public void setType(String type) {
		this.type = type;
	}

	/**
	 * The body of the event
	 *
	 * @return the content
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public Object getContent() {
		return content;
	}

	/**
	 * @param content the content to set
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 * @since 0.8.1
	 */
	public void setContent(Object content) {
		this.content = content;
	}

}
