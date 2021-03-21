package com.kevinguanchedarias.owgejava.entity;

import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.Table;

import com.kevinguanchedarias.owgejava.entity.embeddedid.EventNameUserId;

/**
 * Represents the last send events for given user and type
 *
 * @since 0.9.0
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 *
 */
@Entity
@Table(name = "websocket_events_information")
public class WebsocketEventsInformation implements Serializable {
	private static final long serialVersionUID = 3216511685876136585L;

	@EmbeddedId
	private EventNameUserId eventNameUserId;
	@Column(name = "last_sent", nullable = false)
	private Instant lastSenT = dateWithoutMs();

	/**
	 *
	 *
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public WebsocketEventsInformation() {

	}

	/**
	 *
	 * @param eventName
	 * @param userId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public WebsocketEventsInformation(String eventName, Integer userId) {
		eventNameUserId = new EventNameUserId();
		eventNameUserId.setEventName(eventName);
		eventNameUserId.setUserId(userId);
	}

	/**
	 * @return the eventNameUserId
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public EventNameUserId getEventNameUserId() {
		return eventNameUserId;
	}

	/**
	 * @param eventNameUserId the eventNameUserId to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setEventNameUserId(EventNameUserId eventNameUserId) {
		this.eventNameUserId = eventNameUserId;
	}

	/**
	 * @return the lastSenT
	 * @since 0.9.0
	 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
	 */
	public Instant getLastSent() {
		return lastSenT;
	}

	/**
	 * @param lastSenT the lastSenT to set
	 * @author Kevin Guanche Darias
	 * @since 0.9.0
	 */
	public void setLastSent(Instant lastSenT) {
		this.lastSenT = lastSenT;
	}

	private Instant dateWithoutMs() {
		return Instant.now().truncatedTo(ChronoUnit.SECONDS);
	}
}
