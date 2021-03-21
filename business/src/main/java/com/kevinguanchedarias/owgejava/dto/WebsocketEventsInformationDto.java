package com.kevinguanchedarias.owgejava.dto;

import java.time.Instant;

import com.kevinguanchedarias.owgejava.entity.WebsocketEventsInformation;
import com.kevinguanchedarias.owgejava.entity.embeddedid.EventNameUserId;

/**
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
public class WebsocketEventsInformationDto implements DtoFromEntity<WebsocketEventsInformation> {
	private String eventName;
	private Integer userId;
	private Instant lastSent;

	@Override
	public void dtoFromEntity(WebsocketEventsInformation entity) {
		EventNameUserId eventNameAndUserId = entity.getEventNameUserId();
		eventName = eventNameAndUserId.getEventName();
		userId = eventNameAndUserId.getUserId();
		lastSent = entity.getLastSent();
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

	/**
	 * @return the userId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Integer getUserId() {
		return userId;
	}

	/**
	 * @param userId the userId to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setUserId(Integer userId) {
		this.userId = userId;
	}

	/**
	 * @return the lastSent
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Instant getLastSent() {
		return lastSent;
	}

	/**
	 * @param lastSent the lastSent to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setLastSent(Instant lastSent) {
		this.lastSent = lastSent;
	}
}
