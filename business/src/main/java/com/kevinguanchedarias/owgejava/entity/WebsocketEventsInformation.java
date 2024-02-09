package com.kevinguanchedarias.owgejava.entity;

import com.kevinguanchedarias.owgejava.entity.embeddedid.EventNameUserId;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.io.Serial;
import java.io.Serializable;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Represents the last send events for given user and type
 *
 * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
 * @since 0.9.0
 */
@Entity
@Table(name = "websocket_events_information")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WebsocketEventsInformation implements Serializable {
    @Serial
    private static final long serialVersionUID = 3216511685876136585L;

    @EmbeddedId
    private EventNameUserId eventNameUserId;

    @Column(nullable = false)
    private Instant lastSent = dateWithoutMs();

    /**
     * @author Kevin Guanche Darias <kevin@kevinguanchedarias.com>
     * @since 0.9.0
     */
    public WebsocketEventsInformation(String eventName, Integer userId) {
        eventNameUserId = new EventNameUserId();
        eventNameUserId.setEventName(eventName);
        eventNameUserId.setUserId(userId);
    }

    private Instant dateWithoutMs() {
        return Instant.now().truncatedTo(ChronoUnit.SECONDS);
    }
}
