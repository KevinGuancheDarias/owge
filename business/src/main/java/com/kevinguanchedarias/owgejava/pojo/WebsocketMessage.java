package com.kevinguanchedarias.owgejava.pojo;

import java.util.Date;

import com.kevinguanchedarias.owgejava.entity.WebsocketEventsInformation;
import com.kevinguanchedarias.owgejava.exception.ProgrammingException;

public class WebsocketMessage<T> {

	private String eventName;
	private T value;

	private String status;
	private Date lastSent;

	public WebsocketMessage() {
		this.status = "ok";
	}

	public WebsocketMessage(T value) {
		this.value = value;
		this.status = "ok";
	}

	public WebsocketMessage(String eventName, T value) {
		this.eventName = eventName;
		this.value = value;
		this.status = "ok";
	}

	public WebsocketMessage(WebsocketEventsInformation information, T value) {
		eventName = information.getEventNameUserId().getEventName();
		lastSent = information.getLastSenT();
		this.value = value;
		status = "ok";
	}

	public WebsocketMessage(String eventName, T value, String status) {
		this.eventName = eventName;
		this.value = value;
		setStatus(status);
	}

	/**
	 * @return the eventName
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public String getEventName() {
		return eventName;
	}

	/**
	 * @param eventName the eventName to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setEventName(String eventName) {
		this.eventName = eventName;
	}

	public T getValue() {
		return value;
	}

	public void setValue(T value) {
		this.value = value;
	}

	public String getStatus() {
		return status;
	}

	public void setStatus(String status) {
		if (status != "ok" && status != "error") {
			throw new ProgrammingException("invalid value supplied to status");
		}
		this.status = status;
	}

	/**
	 * @return the lastSent
	 * @since 0.9.6
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Date getLastSent() {
		return lastSent;
	}

	/**
	 * @param lastSent the lastSent to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.6
	 */
	public void setLastSent(Date lastSent) {
		this.lastSent = lastSent;
	}

}
